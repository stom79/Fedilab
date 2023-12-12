package app.fedilab.android.peertube.services;
/* Copyright 2023 Thomas Schneider
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

import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.LinkedHashMap;
import java.util.Objects;

import app.fedilab.android.R;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.helper.EmojiHelper;
import app.fedilab.android.peertube.helper.NetworkStateReceiver;


public class RetrieveInfoService extends Service implements NetworkStateReceiver.NetworkStateReceiverListener {

    static String NOTIFICATION_CHANNEL_ID = "update_info_peertube";
    private NetworkStateReceiver networkStateReceiver;


    public void onCreate() {
        super.onCreate();
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        ContextCompat.registerReceiver(RetrieveInfoService.this, networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION),  ContextCompat.RECEIVER_NOT_EXPORTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);

            ((NotificationManager) Objects.requireNonNull(getSystemService(Context.NOTIFICATION_SERVICE))).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_channel_name))
                    .setAutoCancel(true).build();

            startForeground(1, notification);

        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentText(getString(R.string.notification_channel_name))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            Notification notification = builder.build();

            startForeground(1, notification);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Thread thread = new Thread() {
            @Override
            public void run() {
                EmojiHelper.fillMapEmoji(getApplicationContext());
                peertubeInformation = new PeertubeInformation();
                peertubeInformation.setCategories(new LinkedHashMap<>());
                peertubeInformation.setLanguages(new LinkedHashMap<>());
                peertubeInformation.setLicences(new LinkedHashMap<>());
                peertubeInformation.setPrivacies(new LinkedHashMap<>());
                peertubeInformation.setPlaylistPrivacies(new LinkedHashMap<>());
                peertubeInformation.setTranslations(new LinkedHashMap<>());
                peertubeInformation = new RetrofitPeertubeAPI(RetrieveInfoService.this).getPeertubeInformation();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> stopForeground(true);
                mainHandler.post(myRunnable);
            }
        };
        thread.start();
        return START_NOT_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkStateReceiver != null) {
            networkStateReceiver.removeListener(this);
            unregisterReceiver(networkStateReceiver);
        }
    }

    @Override
    public void networkAvailable() {
        Thread thread = new Thread() {

            @Override
            public void run() {
                EmojiHelper.fillMapEmoji(getApplicationContext());
                if (peertubeInformation == null || peertubeInformation.getCategories() == null || peertubeInformation.getCategories().size() == 0) {
                    peertubeInformation = new PeertubeInformation();
                    peertubeInformation.setCategories(new LinkedHashMap<>());
                    peertubeInformation.setLanguages(new LinkedHashMap<>());
                    peertubeInformation.setLicences(new LinkedHashMap<>());
                    peertubeInformation.setPrivacies(new LinkedHashMap<>());
                    peertubeInformation.setPlaylistPrivacies(new LinkedHashMap<>());
                    peertubeInformation.setTranslations(new LinkedHashMap<>());
                    peertubeInformation = new RetrofitPeertubeAPI(RetrieveInfoService.this).getPeertubeInformation();
                }
                stopForeground(true);
            }
        };
        thread.start();
    }

    @Override
    public void networkUnavailable() {

    }
}
