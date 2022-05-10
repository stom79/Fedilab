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

import static app.fedilab.android.helper.Helper.NotifType.TOOT;
import static app.fedilab.android.helper.Helper.notify_user;

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
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.entities.PostState;
import app.fedilab.android.client.entities.StatusDraft;
import app.fedilab.android.client.mastodon.MastodonStatusesService;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Poll;
import app.fedilab.android.client.mastodon.entities.ScheduledStatus;
import app.fedilab.android.client.mastodon.entities.Status;
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
    public PostMessageService(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public PostMessageService() {
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
        Status firstSendMessage = null;
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

            List<Status> statuses = statusDraft.statusDraftList;
            String in_reply_to_status = null;
            if (statusDraft.statusReplyList != null && statusDraft.statusReplyList.size() > 0) {
                in_reply_to_status = statusDraft.statusReplyList.get(statusDraft.statusReplyList.size() - 1).id;
            }
            totalMediaSize = 0;
            totalBitRead = 0;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PostMessageService.this);
            boolean watermark = sharedPreferences.getBoolean(getString(R.string.SET_WATERMARK), false);
            String watermarkText = sharedPreferences.getString(getString(R.string.SET_WATERMARK_TEXT) + MainActivity.currentUserID + MainActivity.currentInstance, null);
            for (int i = startingPosition; i < statuses.size(); i++) {
                if (statuses.get(i).media_attachments != null && statuses.get(i).media_attachments.size() > 0) {
                    for (Attachment attachment : statuses.get(i).media_attachments) {
                        totalMediaSize += attachment.size;
                    }
                }
            }
            if (watermarkText == null) {
                try {
                    Account account = new Account(PostMessageService.this).getAccountByToken(token);
                    watermarkText = account.mastodon_account.username + "@" + account.instance;
                } catch (DBException e) {
                    e.printStackTrace();
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
                        if (attachment.id != null) {
                            attachmentIds.add(attachment.id);
                        } else {
                            MultipartBody.Part fileMultipartBody;
                            if (watermark && attachment.mimeType != null && attachment.mimeType.contains("image")) {
                                fileMultipartBody = Helper.getMultipartBodyWithWM(PostMessageService.this, watermarkText, "file", attachment);
                            } else {
                                fileMultipartBody = Helper.getMultipartBody("file", attachment);
                            }
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
                if (scheduledDate == null) {
                    statusCall = mastodonStatusesService.createStatus(null, token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), statuses.get(i).language);
                    try {
                        Response<Status> statusResponse = statusCall.execute();

                        if (statusResponse.isSuccessful()) {
                            Status statusReply = statusResponse.body();
                            if (statusReply != null) {
                                StatusAdapter.sendAction(this, Helper.ARG_STATUS_POSTED, statusReply, null);
                            }
                            if (firstSendMessage == null && statusReply != null) {
                                firstSendMessage = statusReply;
                            }
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
                                        new StatusDraft(PostMessageService.this).removeDraft(statusDraft);
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
                } else {
                    Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.createScheduledStatus(null, token, statuses.get(i).text, attachmentIds, poll_options, poll_expire_in,
                            poll_multiple, poll_hide_totals, in_reply_to_status, statuses.get(i).sensitive, statuses.get(i).spoiler_text, statuses.get(i).visibility.toLowerCase(), scheduledDate, statuses.get(i).language);
                    try {
                        Response<ScheduledStatus> statusResponse = scheduledStatusCall.execute();

                        if (statusResponse.isSuccessful()) {
                            ScheduledStatus statusReply = statusResponse.body();
                            if (statusReply != null) {
                                in_reply_to_status = statusReply.id;
                                statusDraft.state.posts_successfully_sent = i;
                                statusDraft.state.posts.get(i).id = statusReply.id;
                                statusDraft.state.posts.get(i).in_reply_to_id = statusReply.params.in_reply_to_id;
                                try {
                                    new StatusDraft(getApplicationContext()).updatePostState(statusDraft);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                                if (!error && i >= statusDraft.statusDraftList.size()) {
                                    try {
                                        new StatusDraft(PostMessageService.this).removeDraft(statusDraft);
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
                messageSent++;
                if (messageSent > messageToSend) {
                    messageSent = messageToSend;
                }
            }
        }

        if (scheduledDate == null && token != null && firstSendMessage != null) {
            Account account;
            try {
                account = new Account(PostMessageService.this).getAccountByToken(token);
                final Intent pendingIntent = new Intent(PostMessageService.this, ContextActivity.class);
                pendingIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                pendingIntent.putExtra(Helper.ARG_STATUS, firstSendMessage);
                pendingIntent.putExtra(Helper.PREF_INSTANCE, account.instance);
                String text = firstSendMessage.content;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
                else
                    text = Html.fromHtml(text).toString();
                if (text.length() > 255) {
                    text = text.substring(0, 254);
                    text = String.format(Locale.getDefault(), "%s…", text);
                }
                notify_user(PostMessageService.this, account, pendingIntent, BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher), TOOT, getString(R.string.message_has_been_sent), text);
            } catch (DBException e) {
                e.printStackTrace();
            }

        }
        notificationManager.cancel(NOTIFICATION_INT_CHANNEL_ID);
    }

}
