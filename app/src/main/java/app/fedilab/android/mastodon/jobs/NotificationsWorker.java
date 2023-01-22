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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.NotificationsHelper;

public class NotificationsWorker extends Worker {

    private static final int FETCH_NOTIFICATION_CHANNEL_ID = 4;
    private static final String CHANNEL_ID = "fedilab_notifications";
    private final NotificationManager notificationManager;

    public NotificationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Notifications";
            String channelDescription = "Fetched notifications";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notifChannel.setDescription(channelDescription);
            notifChannel.setSound(null, null);
            notifChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notifChannel);
            if (notificationManager.getNotificationChannel("notifications") != null) {
                notificationManager.deleteNotificationChannel("notifications");
            }

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.notifications))
                .setContentText(getApplicationContext().getString(R.string.fetch_notifications))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return Futures.immediateFuture(new ForegroundInfo(FETCH_NOTIFICATION_CHANNEL_ID, notificationBuilder.build()));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Notifications";
            String channelDescription = "Fetched notifications";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notifChannel.setSound(null, null);
            notifChannel.setShowBadge(false);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.notifications))
                .setContentText(getApplicationContext().getString(R.string.fetch_notifications))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSilent(true)
                .setPriority(Notification.PRIORITY_LOW);
        return new ForegroundInfo(FETCH_NOTIFICATION_CHANNEL_ID, notificationBuilder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(createForegroundInfo());
        try {
            List<BaseAccount> accounts = new Account(getApplicationContext()).getAll();
            for (BaseAccount account : accounts) {
                try {
                    NotificationsHelper.task(getApplicationContext(), account.user_id + "@" + account.instance);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        return Result.success(new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.notifications)).build());
    }
}
