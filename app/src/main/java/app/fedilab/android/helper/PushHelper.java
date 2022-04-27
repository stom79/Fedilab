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
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.unifiedpush.android.connector.UnifiedPush;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.jobs.NotificationsWorker;

public class PushHelper {

    public static void startStreaming(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String typeOfNotification = prefs.getString(context.getString(R.string.SET_NOTIFICATION_TYPE), "PUSH_NOTIFICATIONS");
        switch (typeOfNotification) {
            case "PUSH_NOTIFICATIONS":
                new Thread(() -> {
                    List<Account> accounts = new Account(context).getPushNotificationAccounts();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        List<String> distributors = UnifiedPush.getDistributors(context, new ArrayList<>());
                        if (distributors.size() == 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(context, Helper.dialogStyle());
                            alert.setTitle(R.string.no_distributors_found);
                            final TextView message = new TextView(context);
                            String link = "https://fedilab.app/wiki/features/push-notifications/";
                            final SpannableString s =
                                    new SpannableString(context.getString(R.string.no_distributors_explanation, link));
                            Linkify.addLinks(s, Linkify.WEB_URLS);
                            message.setText(s);
                            message.setPadding(30, 20, 30, 10);
                            message.setMovementMethod(LinkMovementMethod.getInstance());
                            alert.setView(message);
                            alert.setPositiveButton(R.string.close, (dialog, whichButton) -> dialog.dismiss());
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
                new Thread(() -> {
                    List<Account> accounts = new Account(context).getPushNotificationAccounts();
                    for (Account account : accounts) {
                        ((Activity) context).runOnUiThread(() -> {
                            UnifiedPush.unregisterApp(context, account.user_id + "@" + account.instance);
                        });
                    }
                }).start();
                new PeriodicWorkRequest.Builder(NotificationsWorker.class, 20, TimeUnit.MINUTES)
                        .addTag(Helper.WORKER_REFRESH_NOTIFICATION)
                        .build();
                break;
            case "NO_NOTIFICATIONS":
                WorkManager.getInstance(context).cancelAllWorkByTag(Helper.WORKER_REFRESH_NOTIFICATION);
                new Thread(() -> {
                    List<Account> accounts = new Account(context).getPushNotificationAccounts();
                    for (Account account : accounts) {
                        ((Activity) context).runOnUiThread(() -> {
                            UnifiedPush.unregisterApp(context, account.user_id + "@" + account.instance);
                        });
                    }
                }).start();
                break;
        }
    }


    private static void registerAppWithDialog(Context context, List<Account> accounts) {

        List<String> distributors = UnifiedPush.getDistributors(context, new ArrayList<>());
        if (distributors.size() == 1 || !UnifiedPush.getDistributor(context).isEmpty()) {
            if (distributors.size() == 1) {
                UnifiedPush.saveDistributor(context, distributors.get(0));
            }
            for (Account account : accounts) {
                UnifiedPush.registerApp(context, account.user_id + "@" + account.instance, new ArrayList<>(), "");
            }
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(context, Helper.dialogStyle());
        alert.setTitle(R.string.select_distributors);
        String[] distributorsStr = distributors.toArray(new String[0]);
        alert.setSingleChoiceItems(distributorsStr, -1, (dialog, item) -> {
            String distributor = distributorsStr[item];
            UnifiedPush.saveDistributor(context, distributor);
            for (Account account : accounts) {
                UnifiedPush.registerApp(context, account.user_id + "@" + account.instance, new ArrayList<>(), "");
            }
            dialog.dismiss();
        });
        alert.show();
    }
}
