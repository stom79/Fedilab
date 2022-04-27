package app.fedilab.android.helper;
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


import static app.fedilab.android.helper.ECDH.kp_private;
import static app.fedilab.android.helper.ECDH.kp_public;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.MastodonNotificationsService;
import app.fedilab.android.client.mastodon.entities.PushSubscription;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class PushNotifications {


    public static void registerPushNotifications(Context context, String endpoint, String slug) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String strPub = prefs.getString(kp_public + slug, "");
        String strPriv = prefs.getString(kp_private + slug, "");
        ECDH ecdh = null;
        try {
            ecdh = ECDH.getInstance(slug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ecdh == null) {
            return;
        }
        if (strPub.trim().isEmpty() || strPriv.trim().isEmpty()) {
            ecdh.newPair(context);
        }
        String pubKey = ecdh.getPublicKey(context);
        byte[] randBytes = new byte[16];
        new Random().nextBytes(randBytes);
        String auth = ECDH.base64Encode(randBytes);


        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        MastodonNotificationsService mastodonNotificationsService = init(context, BaseMainActivity.currentInstance);
        ECDH finalEcdh = ecdh;
        new Thread(() -> {
            PushSubscription pushSubscription;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.pushSubscription(
                    BaseMainActivity.currentToken,
                    endpoint,
                    pubKey,
                    auth,
                    notif_follow,
                    notif_fav,
                    notif_share,
                    notif_mention,
                    notif_poll);
            if (pushSubscriptionCall != null) {
                try {
                    Response<PushSubscription> pushSubscriptionResponse = pushSubscriptionCall.execute();
                    if (pushSubscriptionResponse.isSuccessful()) {
                        pushSubscription = pushSubscriptionResponse.body();
                        if (pushSubscription != null) {
                            finalEcdh.saveServerKey(context, pushSubscription.server_key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {

            };
            mainHandler.post(myRunnable);
        }).start();


    }

    public static String getToken(Context context, String slug) {
        return context.getSharedPreferences("unifiedpush.connector", Context.MODE_PRIVATE).getString(
                slug + "/unifiedpush.connector", null);
    }

    private static MastodonNotificationsService init(@NonNull Context context, @NonNull String instance) {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context.getApplicationContext()))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

}
