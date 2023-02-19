package app.fedilab.android.mastodon.helper;
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


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonNotificationsService;
import app.fedilab.android.mastodon.client.entities.api.PushSubscription;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class PushNotifications {


    public static void registerPushNotifications(Context context, String endpoint, String slug) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        ECDHFedilab ecdh = null;
        try {
            ecdh = new ECDHFedilab(context, slug);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ecdh == null) {
            return;
        }

        String pubKey = ecdh.getPublicKey();
        String auth = ecdh.getAuthKey();


        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        boolean notif_status = prefs.getBoolean(context.getString(R.string.SET_NOTIF_STATUS), true);
        boolean notif_updates = prefs.getBoolean(context.getString(R.string.SET_NOTIF_UPDATE), true);
        boolean notif_signup = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_SIGNUP), true);
        boolean notif_report = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_REPORT), true);
        new Thread(() -> {
            String[] slugArray = slug.split("@");
            BaseAccount accountDb = null;
            try {
                accountDb = new Account(context).getUniqAccount(slugArray[0], slugArray[1]);
            } catch (DBException e) {
                e.printStackTrace();
            }

            if (accountDb == null) {
                return;
            }
            MastodonNotificationsService mastodonNotificationsService = init(context, accountDb.instance);
            PushSubscription pushSubscription;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.pushSubscription(
                    accountDb.token,
                    endpoint,
                    pubKey,
                    auth,
                    notif_follow,
                    notif_fav,
                    notif_share,
                    notif_mention,
                    notif_poll,
                    notif_status,
                    notif_updates,
                    notif_signup,
                    notif_report, "all");
            if (pushSubscriptionCall != null) {
                try {
                    Response<PushSubscription> pushSubscriptionResponse = pushSubscriptionCall.execute();
                    if (pushSubscriptionResponse.isSuccessful()) {
                        pushSubscription = pushSubscriptionResponse.body();
                        if (pushSubscription != null) {
                            pushSubscription.server_key = pushSubscription.server_key.replace('/', '_');
                            pushSubscription.server_key = pushSubscription.server_key.replace('+', '-');
                            SharedPreferences.Editor prefsEditor = PreferenceManager
                                    .getDefaultSharedPreferences(context).edit();
                            prefsEditor.putString("server_key" + slug, pushSubscription.server_key);
                            prefsEditor.apply();
                        }
                    }
                } catch (Exception e) {
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
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

}
