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

import static app.fedilab.android.mastodon.jobs.ComposeWorker.publishMessage;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;

public class ScheduleThreadWorker extends Worker {

    private static final int NOTIFICATION_INT_CHANNEL_ID = 3;
    private static final String CHANNEL_ID = "scheduled_thread";
    private final NotificationManager notificationManager;

    public ScheduleThreadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Scheduled threads";
            String channelDescription = "Scheduled threads channel";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.scheduled_toots))
                .setContentText(getApplicationContext().getString(R.string.scheduled_toots))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return Futures.immediateFuture(new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build()));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Scheduled threads";
            String channelDescription = "Scheduled threads channel";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.scheduled_toots))
                .setContentText(getApplicationContext().getString(R.string.scheduled_toots))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(createForegroundInfo());
        String instance = getInputData().getString(Helper.ARG_INSTANCE);
        String token = getInputData().getString(Helper.ARG_TOKEN);
        String userId = getInputData().getString(Helper.ARG_USER_ID);
        String statusDraftId = getInputData().getString(Helper.ARG_STATUS_DRAFT_ID);
        StatusDraft statusDraft;
        try {
            statusDraft = new StatusDraft(getApplicationContext()).geStatusDraft(statusDraftId);
            ComposeWorker.DataPost dataPost = new ComposeWorker.DataPost();
            dataPost.instance = instance;
            dataPost.token = token;
            dataPost.userId = userId;
            dataPost.statusDraft = statusDraft;
            dataPost.scheduledDate = null;
            dataPost.notificationManager = notificationManager;
            // Mark the Worker as important
            setForegroundAsync(createForegroundInfo());
            publishMessage(getApplicationContext(), dataPost);
            return Result.success();
        } catch (DBException e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}
