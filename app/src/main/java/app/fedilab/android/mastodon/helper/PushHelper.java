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


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.unifiedpush.android.connector.UnifiedPush;

import java.net.IDN;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonInstanceService;
import app.fedilab.android.mastodon.client.entities.api.InstanceV2;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.jobs.NotificationsWorker;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PushHelper {


    public static void startStreaming(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String typeOfNotification = prefs.getString(context.getString(R.string.SET_NOTIFICATION_TYPE), "PUSH_NOTIFICATIONS");
        switch (typeOfNotification) {
            case "PUSH_NOTIFICATIONS":
                new Thread(() -> {
                    List<BaseAccount> accounts = new Account(context).getPushNotificationAccounts();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        List<String> distributors = UnifiedPush.getDistributors(context);
                        if (distributors.isEmpty()) {
                            AlertDialog.Builder alert = new MaterialAlertDialogBuilder(context);
                            alert.setTitle(R.string.no_distributors_found);
                            final TextView message = new TextView(context);
                            String link = "https://fedilab.app/docs/fedilab/faq/";
                            final SpannableString s =
                                    new SpannableString(context.getString(R.string.no_distributors_explanation, link));
                            Linkify.addLinks(s, Linkify.WEB_URLS);
                            message.setText(s);
                            message.setPadding(30, 20, 30, 10);
                            message.setMovementMethod(LinkMovementMethod.getInstance());
                            alert.setView(message);
                            alert.setPositiveButton(R.string.close, (dialog, whichButton) -> dialog.dismiss());
                            alert.setNegativeButton(R.string.disable, (dialog, whichButton) -> {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(context.getString(R.string.SET_NOTIFICATION_TYPE), "REPEAT_NOTIFICATIONS");
                                editor.apply();
                                dialog.dismiss();
                            });
                            alert.show();
                        } else {
                            registerAppWithDialog(context, accounts);
                        }
                    };
                    mainHandler.post(myRunnable);
                }).start();
                //Cancel scheduled jobs
                WorkManager.getInstance(context).cancelAllWorkByTag(Helper.WORKER_REFRESH_NOTIFICATION);
                break;
            case "REPEAT_NOTIFICATIONS":
                setRepeat(context);
                break;
            case "NO_NOTIFICATIONS":
                WorkManager.getInstance(context).cancelAllWorkByTag(Helper.WORKER_REFRESH_NOTIFICATION);
                new Thread(() -> {
                    List<BaseAccount> accounts = new Account(context).getPushNotificationAccounts();
                    for (BaseAccount account : accounts) {
                        ((Activity) context).runOnUiThread(() -> UnifiedPush.unregister(context, account.user_id + "@" + account.instance));
                    }
                }).start();
                break;
        }
    }

    public static void setRepeat(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(Helper.WORKER_REFRESH_NOTIFICATION);
        new Thread(() -> {
            List<BaseAccount> accounts = new Account(context).getPushNotificationAccounts();
            if (accounts != null) {
                for (BaseAccount account : accounts) {
                    ((Activity) context).runOnUiThread(() -> UnifiedPush.unregister(context, account.user_id + "@" + account.instance));
                }
            }
        }).start();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(context.getString(R.string.SET_NOTIFICATION_DELAY_VALUE), "15");
        PeriodicWorkRequest notificationPeriodic = new PeriodicWorkRequest.Builder(NotificationsWorker.class, Long.parseLong(value), TimeUnit.MINUTES)
                .addTag(Helper.WORKER_REFRESH_NOTIFICATION)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(Helper.WORKER_REFRESH_NOTIFICATION, ExistingPeriodicWorkPolicy.REPLACE, notificationPeriodic);
    }


    private static void registerAppWithDialog(Context context, List<BaseAccount> accounts) {
        if (accounts == null) {
            return;
        }
        List<String> distributors = UnifiedPush.getDistributors(context);
        if (!distributors.isEmpty()) {
            if (distributors.size() == 1) {
                UnifiedPush.saveDistributor(context, distributors.get(0));
            }
            final OkHttpClient okHttpClient = Helper.myOkHttpClient(context.getApplicationContext());
            for (BaseAccount account : accounts) {

                new Thread(()->{
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://" + (account.instance != null ? IDN.toASCII(account.instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
                            .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                            .client(okHttpClient)
                            .build();
                    MastodonInstanceService mastodonInstanceService = retrofit.create(MastodonInstanceService.class);
                    Call<InstanceV2> instanceV2Call = mastodonInstanceService.instanceV2();
                    String vapid = null;
                    if (instanceV2Call != null) {
                        try {
                            Response<InstanceV2> instanceResponse = instanceV2Call.execute();
                            if (instanceResponse.isSuccessful()) {
                                InstanceV2 instanceV2 = instanceResponse.body();
                                if (instanceV2 != null && instanceV2.configuration.vapId != null) {
                                    vapid = instanceV2.configuration.vapId.publicKey;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    String finalVapid = vapid!=null?vapid.replaceAll("=",""):null;
                    Runnable myRunnable = () -> {
                        try {
                            UnifiedPush.register(context, account.user_id + "@" + account.instance, null, finalVapid);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    };
                    mainHandler.post(myRunnable);
                }).start();
            }
        }
    }
}
