package app.fedilab.android.mastodon.jobs;
/* Copyright 2022 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */

import static android.content.Context.NOTIFICATION_SERVICE;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonStatusesService;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.params.StatusParams;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.CamelTag;
import app.fedilab.android.mastodon.client.entities.app.PostState;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyFile;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.NoteCreateRequest;
import app.fedilab.android.misskey.client.entities.NoteCreateResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ComposeWorker extends Worker {

    private static final int NOTIFICATION_INT_CHANNEL_ID = 1;
    public static String CHANNEL_ID = "post_messages";
    private final NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public ComposeWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }

    private static MastodonStatusesService init(Context context, String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(Helper.myPostOkHttpClient(context))
                .build();
        return retrofit.create(MastodonStatusesService.class);
    }

    private static MisskeyService initMisskey(Context context, String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(Helper.myPostOkHttpClient(context))
                .build();
        return retrofit.create(MisskeyService.class);
    }

    public static void publishMessage(Context context, DataPost dataPost) {
        BaseAccount account = null;
        try {
            account = new Account(context).getAccountByToken(dataPost.token);
        } catch (DBException e) {
            e.printStackTrace();
        }
        if (account != null && account.api == Account.API.MISSKEY) {
            publishMisskeyMessage(context, dataPost, account);
            return;
        }

        long totalMediaSize;
        long totalBitRead;
        MastodonStatusesService mastodonStatusesService = init(context, dataPost.instance);
        boolean error = false;
        Status firstSendMessage = null;
        if (dataPost.statusDraft != null && dataPost.statusDraft.statusDraftList != null && !dataPost.statusDraft.statusDraftList.isEmpty()) {
            //If state is null, it is created (typically when submitting the status the first time)
            if (dataPost.statusDraft.state == null) {
                dataPost.statusDraft.state = new PostState();
                dataPost.statusDraft.state.posts = new ArrayList<>();
                dataPost.statusDraft.state.number_of_posts = dataPost.statusDraft.statusDraftList.size();
                for (Status status : dataPost.statusDraft.statusDraftList) {
                    PostState.Post post = new PostState.Post();
                    post.number_of_media = status.media_attachments != null ? status.media_attachments.size() : 0;
                    dataPost.statusDraft.state.posts.add(post);
                }
            }
            //Check if previous messages in thread have already been published (ie: when resending after a fail)
            int startingPosition = 0;
            for (PostState.Post post : dataPost.statusDraft.state.posts) {
                if (post.id == null || post.id.startsWith("@fedilab_compose_")) {
                    break;
                }
                startingPosition++;
            }

            List<Status> statuses = dataPost.statusDraft.statusDraftList;
            String in_reply_to_status = null;
            if (dataPost.statusDraft.statusReplyList != null && !dataPost.statusDraft.statusReplyList.isEmpty()) {
                in_reply_to_status = dataPost.statusDraft.statusReplyList.get(dataPost.statusDraft.statusReplyList.size() - 1).id;
            }
            totalMediaSize = 0;
            totalBitRead = 0;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean watermark = sharedPreferences.getBoolean(context.getString(R.string.SET_WATERMARK), false);
            String watermarkText = sharedPreferences.getString(context.getString(R.string.SET_WATERMARK_TEXT) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (statuses.get(i).media_attachments != null && !statuses.get(i).media_attachments.isEmpty()) {
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        totalMediaSize += attachment.size;
                    }
                }
            }
            if (watermarkText == null || watermarkText.trim().isEmpty()) {
                if (account != null && account.mastodon_account != null) {
                    watermarkText = account.mastodon_account.username + "@" + account.instance;
                }
            }
            dataPost.messageToSend = statuses.size() - startingPosition;
            dataPost.messageSent = 0;
            List<StatusParams.MediaParams> media_attributes = null;

            for (int i = startingPosition; i < statuses.size(); i++) {
                if (dataPost.notificationBuilder != null) {
                    dataPost.notificationBuilder.setProgress(100, dataPost.messageSent * 100 / dataPost.messageToSend, true);
                    dataPost.notificationBuilder.setContentText(context.getString(R.string.post_message_text, dataPost.messageSent, dataPost.messageToSend));
                    dataPost.notificationManager.notify(NOTIFICATION_INT_CHANNEL_ID, dataPost.notificationBuilder.build());
                }
                //post media first
                List<String> attachmentIds = null;
                if (statuses.get(i).media_attachments != null && !statuses.get(i).media_attachments.isEmpty()) {
                    attachmentIds = new ArrayList<>();
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        if (attachment.id != null) {
                            if (media_attributes == null) {
                                media_attributes = new ArrayList<>();
                            }
                            StatusParams.MediaParams mediaParams = new StatusParams.MediaParams();
                            mediaParams.id = attachment.id;
                            mediaParams.description = attachment.description;
                            mediaParams.focus = attachment.focus;
                            attachmentIds.add(attachment.id);
                            media_attributes.add(mediaParams);
                        } else {
                            MultipartBody.Part fileMultipartBody;
                            if (watermark && attachment.mimeType != null && attachment.mimeType.contains("image")) {
                                fileMultipartBody = Helper.getMultipartBodyWithWM(context, watermarkText, "file", attachment);
                            } else {
                                fileMultipartBody = Helper.getMultipartBody("file", attachment);
                            }
                            String replyId = null;
                            int retry = 0;
                            while (replyId == null && retry < 3) {
                                replyId = postAttachment(mastodonStatusesService, dataPost, fileMultipartBody, attachment);
                                retry++;
                            }
                            if (replyId == null) {
                                error = true;
                            } else {
                                attachmentIds.add(replyId);
                            }
                        }
                    }
                }
                List<String> poll_options = null;
                Integer poll_expire_in = null;
                Boolean poll_multiple = null;
                Boolean poll_hide_totals = null;
                if (statuses.get(i).poll != null) {
                    poll_options = new ArrayList<>();
                    for (Poll.PollItem pollItem : statuses.get(i).poll.options) {
                        poll_options.add(pollItem.title);
                    }
                    poll_expire_in = statuses.get(i).poll.expire_in;
                    poll_multiple = statuses.get(i).poll.multiple;
                    poll_hide_totals = false;
                }
                Call<Status> statusCall;
                if (error) {
                    Bundle args = new Bundle();
                    args.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                    Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                    args.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, context.getString(R.string.media_cannot_be_uploaded));
                    args.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                    new CachedBundle(context).insertBundle(args, account, bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intentBD.putExtras(bundle);
                        intentBD.setPackage(BuildConfig.APPLICATION_ID);
                        context.sendBroadcast(intentBD);
                    });
                    return;
                }
                if (statuses.get(i).local_only) {
                    statuses.get(i).text += " \uD83D\uDC41";
                }
                //Record tags
                if (statuses.get(i).text != null && !statuses.get(i).text.isEmpty()) {
                    Matcher matcher = Helper.hashtagPattern.matcher(statuses.get(i).text);
                    while (matcher.find()) {
                        int matchStart = matcher.start(1);
                        int matchEnd = matcher.end();
                        //Get cached tags
                        if (matchStart >= 0 && matchEnd < statuses.get(i).text.length()) {
                            String tag = statuses.get(i).text.substring(matchStart, matchEnd);
                            tag = tag.replace("#", "");
                            if (!tag.isEmpty()) {
                                try {
                                    new CamelTag(context).insert(tag);
                                } catch (DBException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }

                if (dataPost.scheduledDate == null) {
                    if (dataPost.statusEditId == null) {
                        String visibility = statuses.get(i).visibility != null ? statuses.get(i).visibility.toLowerCase() : null;
                        String quoteApprovalPolicy = statuses.get(i).quote_approval_policy != null ? statuses.get(i).quote_approval_policy.toLowerCase() : null;
                        statusCall = mastodonStatusesService.createStatus(null, dataPost.token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                                poll_multiple, poll_hide_totals, statuses.get(i).quote_id == null ? in_reply_to_status : null, statuses.get(i).sensitive, statuses.get(i).spoilerChecked ? statuses.get(i).spoiler_text : null, visibility, statuses.get(i).language, quoteApprovalPolicy, statuses.get(i).quote_id, statuses.get(i).quote_id, statuses.get(i).content_type, statuses.get(i).local_only);
                    } else { //Status is edited
                        StatusParams statusParams = new StatusParams();
                        statusParams.status = statuses.get(i).text;
                        statusParams.media_ids = attachmentIds;
                        if(poll_options != null) {
                            statusParams.pollParams = new StatusParams.PollParams();
                            statusParams.pollParams.poll_options = poll_options;
                            statusParams.pollParams.poll_expire_in = poll_expire_in;
                            statusParams.pollParams.poll_multiple = poll_multiple;
                            statusParams.pollParams.poll_hide_totals = poll_hide_totals;
                        }
                        statusParams.in_reply_to_id =  statuses.get(i).quote_id == null ? in_reply_to_status : null;
                        statusParams.sensitive =  statuses.get(i).sensitive;
                        statusParams.spoiler_text = statuses.get(i).spoilerChecked ? statuses.get(i).spoiler_text : null;
                        statusParams.visibility = statuses.get(i).visibility != null ? statuses.get(i).visibility.toLowerCase() : null;
                        String quote_id = statuses.get(i).quote_id;
                        if (quote_id != null) {
                            statusParams.quoted_status_id = quote_id.toLowerCase();
                        }
                        String quote_approval_policy = statuses.get(i).quote_approval_policy;
                        if(quote_approval_policy != null) {
                            statusParams.quote_approval_policy = quote_approval_policy.toLowerCase();
                        }
                        statusParams.language = statuses.get(i).language;
                        statusParams.media_attributes = media_attributes;
                        statusCall = mastodonStatusesService.updateStatus(null, dataPost.token,
                                dataPost.statusEditId,
                                statusParams);
                    }
                    try {
                        Response<Status> statusResponse = statusCall.execute();
                        if (statusResponse.isSuccessful()) {
                            Status statusReply = statusResponse.body();
                            if (statusReply != null) {
                                if (dataPost.statusEditId == null) {
                                    StatusAdapter.sendAction(context, Helper.ARG_STATUS_POSTED, statusReply, null);
                                }
                            }
                            if (firstSendMessage == null && statusReply != null) {
                                firstSendMessage = statusReply;
                            }
                            if (statusReply != null) {
                                in_reply_to_status = statusReply.id;
                                dataPost.statusDraft.state.posts_successfully_sent = i;
                                if (dataPost.statusDraft.state.posts.size() > i + 1) {
                                    dataPost.statusDraft.state.posts.get(i + 1).id = statusReply.id;
                                    dataPost.statusDraft.state.posts.get(i + 1).in_reply_to_id = statusReply.in_reply_to_id;
                                }

                                try {
                                    new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                                if (i >= dataPost.statusDraft.statusDraftList.size()) {
                                    try {
                                        new StatusDraft(context).removeDraft(dataPost.statusDraft);
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                    if (dataPost.redraftStatusId != null) {
                                        try {
                                            mastodonStatusesService.deleteStatus(dataPost.token, dataPost.redraftStatusId).execute();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        dataPost.redraftStatusId = null;
                                    }
                                    if (dataPost.service != null) {
                                        dataPost.service.stopSelf();
                                    }
                                }
                            }
                        } else if (statusResponse.errorBody() != null) {
                            Bundle args = new Bundle();
                            args.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                            Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                            args.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                            String err = statusResponse.errorBody().string();
                            if (err.contains("{\"error\":\"")) {
                                err = err.replaceAll("\\{\"error\":\"(.*)\"\\}", "$1");
                            }
                            args.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, err);
                            new CachedBundle(context).insertBundle(args, account, bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intentBD.putExtras(bundle);
                                intentBD.setPackage(BuildConfig.APPLICATION_ID);
                                context.sendBroadcast(intentBD);
                            });
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Bundle args = new Bundle();
                        args.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                        args.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                        Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                        args.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, e.getMessage());
                        new CachedBundle(context).insertBundle(args, account, bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intentBD.putExtras(bundle);
                            intentBD.setPackage(BuildConfig.APPLICATION_ID);
                            context.sendBroadcast(intentBD);
                        });
                        return;
                    }
                } else {
                    Call<Void> voidCall = mastodonStatusesService.deleteScheduledStatus(dataPost.token, dataPost.scheduledId);
                    if (voidCall != null) {
                        try {
                            voidCall.execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    String scheduledVisibility = statuses.get(i).visibility != null ? statuses.get(i).visibility.toLowerCase() : null;
                    Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.createScheduledStatus(null, dataPost.token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, statuses.get(i).quote_id == null ? in_reply_to_status : null, statuses.get(i).sensitive, statuses.get(i).spoilerChecked ? statuses.get(i).spoiler_text : null, scheduledVisibility, dataPost.scheduledDate, statuses.get(i).language);
                    try {
                        Response<ScheduledStatus> statusResponse = scheduledStatusCall.execute();

                        if (statusResponse.isSuccessful()) {
                            ScheduledStatus statusReply = statusResponse.body();
                            if (statusReply != null) {
                                in_reply_to_status = statusReply.id;
                                dataPost.statusDraft.state.posts_successfully_sent = i;
                                if (dataPost.statusDraft.state.posts.size() > i + 1) {
                                    dataPost.statusDraft.state.posts.get(i + 1).id = statusReply.id;
                                    dataPost.statusDraft.state.posts.get(i + 1).in_reply_to_id = statusReply.params.in_reply_to_id;
                                }
                                try {
                                    new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        error = true;
                    }
                }
                dataPost.messageSent++;
                if (dataPost.messageSent > dataPost.messageToSend) {
                    dataPost.messageSent = dataPost.messageToSend;
                }
                if (i >= (statuses.size() - 1)) {
                    try {
                        new StatusDraft(context).removeDraft(dataPost.statusDraft);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    if (dataPost.service != null) {
                        dataPost.service.stopSelf();
                    }
                }
            }
        }

        if (dataPost.scheduledDate == null && dataPost.token != null && firstSendMessage != null) {
            Bundle args = new Bundle();
            args.putBoolean(Helper.RECEIVE_NEW_MESSAGE, true);
            args.putString(Helper.ARG_EDIT_STATUS_ID, dataPost.statusEditId);
            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
            args.putSerializable(Helper.RECEIVE_STATUS_ACTION, firstSendMessage);
            new CachedBundle(context).insertBundle(args, account, bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intentBD.putExtras(bundle);
                intentBD.setPackage(BuildConfig.APPLICATION_ID);
                context.sendBroadcast(intentBD);
            });
        }
    }

    private static String postAttachment(MastodonStatusesService mastodonStatusesService, DataPost dataPost, MultipartBody.Part fileMultipartBody, Attachment attachment) {

        RequestBody descriptionBody = null;
        RequestBody focusBody = null;
        if (attachment.description != null && attachment.description.trim().length() > 0) {
            descriptionBody = RequestBody.create(MediaType.parse("text/plain"), attachment.description);
        }
        if (attachment.focus != null && attachment.focus.trim().length() > 0) {
            focusBody = RequestBody.create(MediaType.parse("text/plain"), attachment.focus);
        }
        Call<Attachment> attachmentCall = mastodonStatusesService.postMedia(dataPost.token, fileMultipartBody, null, descriptionBody, focusBody);

        if (attachmentCall != null) {
            try {
                Response<Attachment> attachmentResponse = attachmentCall.execute();
                if (attachmentResponse.isSuccessful()) {
                    Attachment attachmentReply = attachmentResponse.body();
                    if (attachmentReply != null) {
                        return attachmentReply.id;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String serialize(StatusDraft statusDraft) {
        Gson gson = new Gson();
        try {
            return gson.toJson(statusDraft);
        } catch (Exception e) {
            return null;
        }
    }

    public static StatusDraft restore(String serialized) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serialized, StatusDraft.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void publishMisskeyMessage(Context context, DataPost dataPost, BaseAccount account) {
        MisskeyService misskeyService = initMisskey(context, dataPost.instance);

        if (dataPost.statusDraft == null || dataPost.statusDraft.statusDraftList == null || dataPost.statusDraft.statusDraftList.isEmpty()) {
            return;
        }

        if (dataPost.statusDraft.state == null) {
            dataPost.statusDraft.state = new PostState();
            dataPost.statusDraft.state.posts = new ArrayList<>();
            dataPost.statusDraft.state.number_of_posts = dataPost.statusDraft.statusDraftList.size();
            for (Status status : dataPost.statusDraft.statusDraftList) {
                PostState.Post post = new PostState.Post();
                post.number_of_media = status.media_attachments != null ? status.media_attachments.size() : 0;
                dataPost.statusDraft.state.posts.add(post);
            }
        }

        int startingPosition = 0;
        for (PostState.Post post : dataPost.statusDraft.state.posts) {
            if (post.id == null || post.id.startsWith("@fedilab_compose_")) {
                break;
            }
            startingPosition++;
        }

        List<Status> statuses = dataPost.statusDraft.statusDraftList;
        String in_reply_to_status = null;
        if (dataPost.statusDraft.statusReplyList != null && !dataPost.statusDraft.statusReplyList.isEmpty()) {
            in_reply_to_status = dataPost.statusDraft.statusReplyList.get(dataPost.statusDraft.statusReplyList.size() - 1).id;
        }

        Status firstSendMessage = null;

        for (int i = startingPosition; i < statuses.size(); i++) {
            List<String> fileIds = null;

            if (statuses.get(i).media_attachments != null && !statuses.get(i).media_attachments.isEmpty()) {
                fileIds = new ArrayList<>();
                for (Attachment attachment : statuses.get(i).media_attachments) {
                    if (attachment.id != null) {
                        fileIds.add(attachment.id);
                    } else {
                        MultipartBody.Part fileMultipartBody = Helper.getMultipartBody("file", attachment);
                        try {
                            RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), dataPost.token);
                            RequestBody sensitiveBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(statuses.get(i).sensitive));
                            RequestBody commentBody = attachment.description != null ?
                                    RequestBody.create(MediaType.parse("text/plain"), attachment.description) : null;

                            Response<MisskeyFile> response = misskeyService.uploadFile(tokenBody, fileMultipartBody, sensitiveBody, commentBody).execute();
                            if (response.isSuccessful() && response.body() != null) {
                                fileIds.add(response.body().id);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            NoteCreateRequest request = new NoteCreateRequest(dataPost.token);
            request.text = statuses.get(i).text;
            request.visibility = NoteCreateRequest.mapVisibility(statuses.get(i).visibility);
            request.cw = statuses.get(i).spoilerChecked ? statuses.get(i).spoiler_text : null;
            request.replyId = statuses.get(i).quote_id == null ? in_reply_to_status : null;
            request.fileIds = fileIds;
            request.localOnly = statuses.get(i).local_only;

            if (dataPost.scheduledDate != null) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(Helper.SCHEDULE_DATE_FORMAT, java.util.Locale.getDefault());
                    java.util.Date date = sdf.parse(dataPost.scheduledDate);
                    if (date != null) {
                        request.scheduledAt = date.getTime();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (statuses.get(i).poll != null && statuses.get(i).poll.options != null && !statuses.get(i).poll.options.isEmpty()) {
                NoteCreateRequest.PollRequest pollRequest = new NoteCreateRequest.PollRequest();
                pollRequest.choices = new java.util.ArrayList<>();
                for (app.fedilab.android.mastodon.client.entities.api.Poll.PollItem option : statuses.get(i).poll.options) {
                    if (option.title != null && !option.title.trim().isEmpty()) {
                        pollRequest.choices.add(option.title);
                    }
                }
                pollRequest.multiple = statuses.get(i).poll.multiple;
                if (statuses.get(i).poll.expire_in > 0) {
                    pollRequest.expiredAfter = (long) statuses.get(i).poll.expire_in * 1000L;
                }
                request.poll = pollRequest;
            }

            try {
                Response<NoteCreateResponse> response = misskeyService.createNote(request).execute();
                if (response.isSuccessful()) {
                    if (request.scheduledAt != null) {
                        // Scheduled note: no createdNote in response, just mark as successful
                        dataPost.statusDraft.state.posts_successfully_sent = i;
                        try {
                            new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                        if (i >= dataPost.statusDraft.statusDraftList.size() - 1) {
                            try {
                                new StatusDraft(context).removeDraft(dataPost.statusDraft);
                            } catch (DBException e) {
                                e.printStackTrace();
                            }
                            if (dataPost.service != null) {
                                dataPost.service.stopSelf();
                            }
                        }
                    } else if (response.body() != null && response.body().createdNote != null) {
                        Status statusReply = response.body().createdNote.toStatus(dataPost.instance);
                        if (dataPost.statusEditId == null) {
                            StatusAdapter.sendAction(context, Helper.ARG_STATUS_POSTED, statusReply, null);
                        }
                        if (firstSendMessage == null) {
                            firstSendMessage = statusReply;
                        }
                        in_reply_to_status = statusReply.id;
                        dataPost.statusDraft.state.posts_successfully_sent = i;

                        try {
                            new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                        if (i >= dataPost.statusDraft.statusDraftList.size() - 1) {
                            try {
                                new StatusDraft(context).removeDraft(dataPost.statusDraft);
                            } catch (DBException e) {
                                e.printStackTrace();
                            }
                            if (dataPost.redraftStatusId != null) {
                                try {
                                    misskeyService.deleteNote(new MisskeyRequest.NoteIdRequest(dataPost.token, dataPost.redraftStatusId)).execute();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                dataPost.redraftStatusId = null;
                            }
                            if (dataPost.service != null) {
                                dataPost.service.stopSelf();
                            }
                        }
                    }
                } else {
                    Bundle args = new Bundle();
                    args.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                    Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                    args.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                    String err = response.errorBody() != null ? response.errorBody().string() : context.getString(R.string.toast_error);
                    args.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, err);
                    new CachedBundle(context).insertBundle(args, account, bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intentBD.putExtras(bundle);
                        intentBD.setPackage(BuildConfig.APPLICATION_ID);
                        context.sendBroadcast(intentBD);
                    });
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Bundle args = new Bundle();
                args.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                args.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                args.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, e.getMessage());
                new CachedBundle(context).insertBundle(args, account, bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intentBD.putExtras(bundle);
                    intentBD.setPackage(BuildConfig.APPLICATION_ID);
                    context.sendBroadcast(intentBD);
                });
                return;
            }
        }

        if (firstSendMessage != null) {
            try {
                new StatusDraft(context).removeDraft(dataPost.statusDraft);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        String statusDraftId = inputData.getString(Helper.ARG_STATUS_DRAFT_ID);
        StatusDraft statusDraft = null;
        if (statusDraftId != null) {
            try {
                statusDraft = new StatusDraft(getApplicationContext()).geStatusDraft(statusDraftId);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }
        String token = inputData.getString(Helper.ARG_TOKEN);
        String scheduledId = inputData.getString(Helper.ARG_SCHEDULED_ID);
        String instance = inputData.getString(Helper.ARG_INSTANCE);
        String userId = inputData.getString(Helper.ARG_USER_ID);
        String scheduledDate = inputData.getString(Helper.ARG_SCHEDULED_DATE);
        String editMessageId = inputData.getString(Helper.ARG_EDIT_STATUS_ID);
        String redraftStatusId = inputData.getString(Helper.ARG_REDRAFT_STATUS_ID);
        //Should not be null, but a simple security
        if (token == null) {
            token = BaseMainActivity.currentToken;
        }
        if (instance == null) {
            instance = BaseMainActivity.currentInstance;
        }
        DataPost dataPost = new DataPost();
        dataPost.instance = instance;
        dataPost.token = token;
        dataPost.scheduledId = scheduledId;
        dataPost.userId = userId;
        dataPost.statusDraft = statusDraft;
        dataPost.scheduledDate = scheduledDate;
        dataPost.notificationBuilder = notificationBuilder;
        dataPost.notificationManager = notificationManager;
        dataPost.statusEditId = editMessageId;
        dataPost.redraftStatusId = redraftStatusId;
        // Mark the Worker as important
        setForegroundAsync(createForegroundInfo());
        publishMessage(getApplicationContext(), dataPost);
        return Result.success();
    }


    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        // Build a notification using bytesRead and contentLength
        Context context = getApplicationContext();
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(context.getString(R.string.post_message))
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Futures.immediateFuture(new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC));
        } else {
            return Futures.immediateFuture(new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build()));
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        // Build a notification using bytesRead and contentLength
        Context context = getApplicationContext();
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(context.getString(R.string.post_message))
                .setOngoing(true)
                .setSilent(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            return new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        String channelName = "Post messages";
        String channelDescription = "Post messages in background";
        NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        notifChannel.setDescription(channelDescription);
        notificationManager.createNotificationChannel(notifChannel);
    }

    public static class DataPost {
        public String instance;
        public String token;
        public String userId;
        public String scheduledId;
        public String statusEditId;
        public String redraftStatusId;
        public StatusDraft statusDraft;
        public int messageToSend;
        public int messageSent;
        public NotificationCompat.Builder notificationBuilder;
        public String scheduledDate;
        public NotificationManager notificationManager;
        public IntentService service;
    }
}
