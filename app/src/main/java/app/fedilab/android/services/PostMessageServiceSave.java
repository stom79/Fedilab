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
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.PostState;
import app.fedilab.android.client.entities.StatusDraft;
import app.fedilab.android.client.mastodon.MastodonStatusesService;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Poll;
import app.fedilab.android.client.mastodon.entities.ScheduledStatus;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PostMessageServiceSave extends IntentService {

    private static final int NOTIFICATION_INT_CHANNEL_ID = 1;
    public static String CHANNEL_ID = "post_messages";
    private long totalMediaSize;
    private long totalBitRead;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int messageToSend;
    private int messageSent;

    /**
     * @param name - String
     * @deprecated
     */
    public PostMessageServiceSave(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public PostMessageServiceSave() {
        super("PostMessageService");
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
                .setContentText(getString(R.string.post_message_text, messageSent, messageToSend))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        startForeground(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
    }


    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(getApplication().getApplicationContext()))
                .build();
    }

    private MastodonStatusesService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(getOkHttpClient())
                .build();
        return retrofit.create(MastodonStatusesService.class);
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
        MastodonStatusesService mastodonStatusesService = init(instance);
        boolean error = false;
        if (statusDraft != null && statusDraft.statusDraftList != null && statusDraft.statusDraftList.size() > 0) {
            //If state is null, it is created (typically when submitting the status the first time)
            if (statusDraft.state == null) {
                statusDraft.state = new PostState();
                statusDraft.state.posts = new ArrayList<>();
                statusDraft.state.number_of_posts = statusDraft.statusDraftList.size();
                for (Status status : statusDraft.statusDraftList) {
                    PostState.Post post = new PostState.Post();
                    post.number_of_media = status.media_attachments != null ? status.media_attachments.size() : 0;
                    statusDraft.state.posts.add(post);
                }
            }
            //Check if previous messages in thread have already been published (ie: when resending after a fail)
            int startingPosition = 0;
            for (PostState.Post post : statusDraft.state.posts) {
                if (post.id == null) {
                    break;
                }
                startingPosition++;
            }
            String in_reply_to_status = null;
            List<Status> statuses = statusDraft.statusDraftList;
            totalMediaSize = 0;
            totalBitRead = 0;
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        totalMediaSize += attachment.size;
                    }
                }
            }
            messageToSend = statuses.size() - startingPosition;
            messageSent = 0;
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (notificationBuilder != null) {
                    notificationBuilder.setProgress(100, messageSent * 100 / messageToSend, true);
                    notificationBuilder.setContentText(getString(R.string.post_message_text, messageSent, messageToSend));
                    notificationManager.notify(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
                }
                //post media first
                List<String> attachmentIds = null;
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    attachmentIds = new ArrayList<>();
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        MultipartBody.Part fileMultipartBody;
                        fileMultipartBody = Helper.getMultipartBody("file", attachment);
                        Call<Attachment> attachmentCall = mastodonStatusesService.postMedia(token, fileMultipartBody, null, attachment.description, null);
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
                if (scheduledDate == null) {
                    Call<Status> statusCall = mastodonStatusesService.createStatus(null, token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), statuses.get(i).language);
                    if (statusCall != null) {
                        try {
                            Response<Status> statusResponse = statusCall.execute();

                            if (statusResponse.isSuccessful()) {
                                Status statusReply = statusResponse.body();
                                if (statusReply != null) {
                                    in_reply_to_status = statusReply.id;
                                    statusDraft.state.posts_successfully_sent = i;
                                    statusDraft.state.posts.get(i).id = statusReply.id;
                                    statusDraft.state.posts.get(i).in_reply_to_id = statusReply.in_reply_to_id;
                                    try {
                                        new StatusDraft(getApplicationContext()).updatePostState(statusDraft);
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                    if (!error && i >= statusDraft.statusDraftList.size()) {
                                        try {
                                            new StatusDraft(PostMessageServiceSave.this).removeDraft(statusDraft);
                                        } catch (DBException e) {
                                            e.printStackTrace();
                                        }
                                        stopSelf();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            error = true;
                        }
                    }
                } else {
                    //Even if we use the loop, there is always one status to schedule from server side.
                    Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.createScheduledStatus(null, token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), scheduledDate, statuses.get(i).language);
                    if (scheduledStatusCall != null) {
                        try {
                            Response<ScheduledStatus> statusResponse = scheduledStatusCall.execute();
                            if (statusResponse.isSuccessful()) {
                                try {
                                    new StatusDraft(PostMessageServiceSave.this).removeDraft(statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                                stopSelf();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            error = true;
                        }
                    }
                }
                messageSent++;
                if (messageSent > messageToSend) {
                    messageSent = messageToSend;
                }
            }
        }
    }

}
