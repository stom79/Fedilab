package app.fedilab.android.peertube.helper;

/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static app.fedilab.android.peertube.worker.NotificationsWorker.FETCH_NOTIFICATION_CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.data.AccountData;

public class NotificationHelper {


    /**
     * Sends notification with intent
     *
     * @param context Context
     * @param intent  Intent associated to the notifcation
     * @param icon    Bitmap profile picture
     * @param title   String title of the notification
     * @param message String message for the notification
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    public static void notify_user(Context context, AccountData.Account account, Intent intent, Bitmap icon, String title, String message) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = (int) System.currentTimeMillis();
        PendingIntent pIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_ONE_SHOT);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, FETCH_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_tubelab).setTicker(message)
                .setWhen(System.currentTimeMillis());
        notificationBuilder.setGroup(account.getAcct())
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setContentText(message);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel;
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            channel = new NotificationChannel(FETCH_NOTIFICATION_CHANNEL_ID, context.getString(R.string.fetch_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setLargeIcon(icon);
        notificationManager.notify(notificationId, notificationBuilder.build());

        Notification summaryNotification =
                new NotificationCompat.Builder(context, FETCH_NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(context.getApplicationContext().getString(R.string.fetch_notification_channel_name))
                        .setContentIntent(pIntent)
                        .setLargeIcon(icon)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_notification_tubelab)
                        .setGroup(account.getAcct())
                        .setGroupSummary(true)
                        .build();
        notificationManager.notify(0, summaryNotification);
    }


}
