package app.fedilab.android.jobs;
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
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.endpoints.MastodonStatusesService;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Poll;
import app.fedilab.android.client.entities.api.ScheduledStatus;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.PostState;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.drawer.StatusAdapter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
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

    private static OkHttpClient getOkHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context.getApplicationContext()))
                .build();
    }

    private static MastodonStatusesService init(Context context, @NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(getOkHttpClient(context))
                .build();
        return retrofit.create(MastodonStatusesService.class);
    }

    public static void publishMessage(Context context, DataPost dataPost) {
        long totalMediaSize;
        long totalBitRead;
        MastodonStatusesService mastodonStatusesService = init(context, dataPost.instance);
        boolean error = false;
        Status firstSendMessage = null;
        if (dataPost.statusDraft != null && dataPost.statusDraft.statusDraftList != null && dataPost.statusDraft.statusDraftList.size() > 0) {
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
            if (dataPost.statusDraft.statusReplyList != null && dataPost.statusDraft.statusReplyList.size() > 0) {
                in_reply_to_status = dataPost.statusDraft.statusReplyList.get(dataPost.statusDraft.statusReplyList.size() - 1).id;
            }
            totalMediaSize = 0;
            totalBitRead = 0;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean watermark = sharedPreferences.getBoolean(context.getString(R.string.SET_WATERMARK), false);
            String watermarkText = sharedPreferences.getString(context.getString(R.string.SET_WATERMARK_TEXT) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        totalMediaSize += attachment.size;
                    }
                }
            }
            if (watermarkText == null || watermarkText.trim().length() == 0) {
                try {
                    BaseAccount account = new Account(context).getAccountByToken(dataPost.token);
                    watermarkText = account.mastodon_account.username + "@" + account.instance;
                } catch (DBException e) {
                    e.printStackTrace();
                }

            }
            dataPost.messageToSend = statuses.size() - startingPosition;
            dataPost.messageSent = 0;
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (dataPost.notificationBuilder != null) {
                    dataPost.notificationBuilder.setProgress(100, dataPost.messageSent * 100 / dataPost.messageToSend, true);
                    dataPost.notificationBuilder.setContentText(context.getString(R.string.post_message_text, dataPost.messageSent, dataPost.messageToSend));
                    dataPost.notificationManager.notify(NOTIFICATION_INT_CHANNEL_ID, dataPost.notificationBuilder.build());
                }
                //post media first
                List<String> attachmentIds = null;
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    attachmentIds = new ArrayList<>();
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        if (attachment.id != null) {
                            attachmentIds.add(attachment.id);
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
                    Bundle b = new Bundle();
                    b.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                    Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                    b.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, context.getString(R.string.media_cannot_be_uploaded));
                    b.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                    intentBD.putExtras(b);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentBD);
                    return;
                }
                String language = sharedPreferences.getString(context.getString(R.string.SET_COMPOSE_LANGUAGE) + dataPost.userId + dataPost.instance, null);
                if (dataPost.scheduledDate == null) {
                    statusCall = mastodonStatusesService.createStatus(null, dataPost.token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), language);
                    try {
                        Response<Status> statusResponse = statusCall.execute();
                        if (statusResponse.isSuccessful()) {
                            Status statusReply = statusResponse.body();
                            if (statusReply != null) {
                                StatusAdapter.sendAction(context, Helper.ARG_STATUS_POSTED, statusReply, null);
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
                                    if (dataPost.service != null) {
                                        dataPost.service.stopSelf();
                                    }
                                }
                            }
                        } else if (statusResponse.errorBody() != null) {
                            Bundle b = new Bundle();
                            b.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                            Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                            b.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                            b.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, statusResponse.errorBody().string());
                            intentBD.putExtras(b);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBD);
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Bundle b = new Bundle();
                        b.putBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, true);
                        b.putSerializable(Helper.ARG_STATUS_DRAFT, dataPost.statusDraft);
                        Intent intentBD = new Intent(Helper.INTENT_COMPOSE_ERROR_MESSAGE);
                        b.putSerializable(Helper.RECEIVE_ERROR_MESSAGE, e.getMessage());
                        intentBD.putExtras(b);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBD);
                        return;
                    }
                } else {
                    Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.createScheduledStatus(null, dataPost.token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), dataPost.scheduledDate, statuses.get(i).language);
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
            Bundle b = new Bundle();
            b.putBoolean(Helper.RECEIVE_NEW_MESSAGE, true);
            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
            b.putSerializable(Helper.RECEIVE_STATUS_ACTION, firstSendMessage);
            intentBD.putExtras(b);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentBD);
        }
    }

    private static String postAttachment(MastodonStatusesService mastodonStatusesService, DataPost dataPost, MultipartBody.Part fileMultipartBody, Attachment attachment) {

        RequestBody descriptionBody = null;
        if (attachment.description != null && attachment.description.trim().length() > 0) {
            descriptionBody = RequestBody.create(MediaType.parse("text/plain"), attachment.description);
        }
        Call<Attachment> attachmentCall = mastodonStatusesService.postMedia(dataPost.token, fileMultipartBody, null, descriptionBody, attachment.focus);

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
        String instance = inputData.getString(Helper.ARG_INSTANCE);
        String userId = inputData.getString(Helper.ARG_USER_ID);
        String scheduledDate = inputData.getString(Helper.ARG_SCHEDULED_DATE);
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
        dataPost.userId = userId;
        dataPost.statusDraft = statusDraft;
        dataPost.scheduledDate = scheduledDate;
        dataPost.notificationBuilder = notificationBuilder;
        dataPost.notificationManager = notificationManager;
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
        return Futures.immediateFuture(new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build()));
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
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH);

        return new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
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
        public StatusDraft statusDraft;
        public int messageToSend;
        public int messageSent;
        public NotificationCompat.Builder notificationBuilder;
        public String scheduledDate;
        public NotificationManager notificationManager;
        public IntentService service;
    }
}
