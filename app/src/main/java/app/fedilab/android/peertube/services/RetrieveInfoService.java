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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.LinkedHashMap;

import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.helper.EmojiHelper;
import app.fedilab.android.peertube.helper.NetworkStateReceiver;


public class RetrieveInfoService extends Service implements NetworkStateReceiver.NetworkStateReceiverListener {

    private NetworkStateReceiver networkStateReceiver;


    public void onCreate() {
        super.onCreate();
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        ContextCompat.registerReceiver(RetrieveInfoService.this, networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);

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
            try {
                unregisterReceiver(networkStateReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void networkAvailable() {
        Thread thread = new Thread() {

            @Override
            public void run() {
                EmojiHelper.fillMapEmoji(getApplicationContext());
                if (peertubeInformation == null || peertubeInformation.getCategories() == null || peertubeInformation.getCategories().isEmpty()) {
                    peertubeInformation = new PeertubeInformation();
                    peertubeInformation.setCategories(new LinkedHashMap<>());
                    peertubeInformation.setLanguages(new LinkedHashMap<>());
                    peertubeInformation.setLicences(new LinkedHashMap<>());
                    peertubeInformation.setPrivacies(new LinkedHashMap<>());
                    peertubeInformation.setPlaylistPrivacies(new LinkedHashMap<>());
                    peertubeInformation.setTranslations(new LinkedHashMap<>());
                    peertubeInformation = new RetrofitPeertubeAPI(RetrieveInfoService.this).getPeertubeInformation();
                }
            }
        };
        thread.start();
    }

    @Override
    public void networkUnavailable() {

    }
}
