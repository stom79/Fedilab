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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.services.PostMessageService;

public class NotificationsWorker extends Worker {

    private static final int FETCH_NOTIFICATION_CHANNEL_ID = 4;
    private static final String CHANNEL_ID = "notifications";
    private final NotificationManager notificationManager;

    public NotificationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Notification";
            String channelDescription = "Fetched notifications";
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
        return new ForegroundInfo(FETCH_NOTIFICATION_CHANNEL_ID, notificationBuilder.build());
    }

    @NonNull
    @Override
    public Result doWork() {

        setForegroundAsync(createForegroundInfo());
        Data outputData;
        String instance = getInputData().getString(Helper.ARG_INSTANCE);
        String token = getInputData().getString(Helper.ARG_TOKEN);
        String statusDraftId = getInputData().getString(Helper.ARG_STATUS_DRAFT_ID);
        String userId = getInputData().getString(Helper.ARG_USER_ID);
        StatusDraft statusDraft;
        try {
            statusDraft = new StatusDraft(getApplicationContext()).geStatusDraft(statusDraftId);
            Intent intent = new Intent(getApplicationContext(), PostMessageService.class);
            intent.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
            intent.putExtra(Helper.ARG_INSTANCE, instance);
            intent.putExtra(Helper.ARG_TOKEN, token);
            intent.putExtra(Helper.ARG_USER_ID, userId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(intent);
            } else {
                getApplicationContext().startService(intent);
            }
        } catch (DBException e) {
            e.printStackTrace();
            outputData = new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.toast_error)).build();
            return Result.failure(outputData);
        }

        return Result.success(new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.toot_sent)).build());
    }
}
