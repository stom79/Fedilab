package app.fedilab.android.services;
/* Copyright 2021 Thomas Schneider
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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.endpoints.MastodonStatusesService;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Poll;
import app.fedilab.android.client.entities.api.ScheduledStatus;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.PostState;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.drawer.StatusAdapter;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PostMessageService extends IntentService {

    private static final int NOTIFICATION_INT_CHANNEL_ID = 1;
    public static String CHANNEL_ID = "post_messages";

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    /**
     * @param name - String
     * @deprecated
     */
    public PostMessageService(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public PostMessageService() {
        super("PostMessageService");
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
                .addConverterFactory(GsonConverterFactory.create())
                .client(getOkHttpClient(context))
                .build();
        return retrofit.create(MastodonStatusesService.class);
    }

    static void publishMessage(Context context, DataPost dataPost) {
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
                if (post.id == null) {
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
            String watermarkText = sharedPreferences.getString(context.getString(R.string.SET_WATERMARK_TEXT) + MainActivity.currentUserID + MainActivity.currentInstance, null);
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        totalMediaSize += attachment.size;
                    }
                }
            }
            if (watermarkText == null) {
                try {
                    Account account = new Account(context).getAccountByToken(dataPost.token);
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
                            Call<Attachment> attachmentCall = mastodonStatusesService.postMedia(dataPost.token, fileMultipartBody, null, attachment.description, null);

                            if (attachmentCall != null) {
                                try {
                                    Response<Attachment> attachmentResponse = attachmentCall.execute();
                                    if (attachmentResponse.isSuccessful()) {
                                        Attachment attachmentReply = attachmentResponse.body();
                                        if (attachmentReply != null) {
                                            attachmentIds.add(attachmentReply.id);
                                        }
                                    }
                                } catch (IOException e) {
                                    error = true;
                                    e.printStackTrace();
                                }
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
                if (dataPost.scheduledDate == null) {
                    statusCall = mastodonStatusesService.createStatus(null, dataPost.token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), statuses.get(i).language);
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
                                dataPost.statusDraft.state.posts.get(i).id = statusReply.id;
                                dataPost.statusDraft.state.posts.get(i).in_reply_to_id = statusReply.in_reply_to_id;
                                try {
                                    new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                                if (!error && i >= dataPost.statusDraft.statusDraftList.size()) {
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
                    } catch (IOException e) {
                        e.printStackTrace();
                        error = true;
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
                                dataPost.statusDraft.state.posts.get(i).id = statusReply.id;
                                dataPost.statusDraft.state.posts.get(i).in_reply_to_id = statusReply.params.in_reply_to_id;
                                try {
                                    new StatusDraft(context.getApplicationContext()).updatePostState(dataPost.statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                                if (!error && i >= dataPost.statusDraft.statusDraftList.size()) {
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
                    } catch (IOException e) {
                        e.printStackTrace();
                        error = true;
                    }
                }
                dataPost.messageSent++;
                if (dataPost.messageSent > dataPost.messageToSend) {
                    dataPost.messageSent = dataPost.messageToSend;
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

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Post messages";
            String channelDescription = "Post messages in background";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        notificationBuilder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getString(R.string.post_message))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        startForeground(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        StatusDraft statusDraft = null;
        String token = null, instance = null;
        String scheduledDate = null;
        if (intent != null && intent.getExtras() != null) {
            Bundle b = intent.getExtras();
            statusDraft = (StatusDraft) b.getSerializable(Helper.ARG_STATUS_DRAFT);
            token = b.getString(Helper.ARG_TOKEN);
            instance = b.getString(Helper.ARG_INSTANCE);
            scheduledDate = b.getString(Helper.ARG_SCHEDULED_DATE);
        }
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
        dataPost.statusDraft = statusDraft;
        dataPost.scheduledDate = scheduledDate;
        dataPost.notificationBuilder = notificationBuilder;
        dataPost.notificationManager = notificationManager;
        publishMessage(PostMessageService.this, dataPost);
        notificationManager.cancel(NOTIFICATION_INT_CHANNEL_ID);
    }

    static class DataPost {
        String instance;
        String token;
        StatusDraft statusDraft;
        int messageToSend;
        int messageSent;
        NotificationCompat.Builder notificationBuilder;
        String scheduledDate;
        NotificationManager notificationManager;
        IntentService service;
    }

}
