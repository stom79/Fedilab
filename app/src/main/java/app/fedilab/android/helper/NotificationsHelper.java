package app.fedilab.android.helper;
/* Copyright 2021 Thomas Schneider
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

import static android.text.Html.FROM_HTML_MODE_LEGACY;
import static app.fedilab.android.helper.Helper.notify_user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.mastodon.MastodonNotificationsService;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Notifications;
import app.fedilab.android.exception.DBException;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NotificationsHelper {

    public static HashMap<String, String> since_ids = new HashMap<>();

    public static void task(Context context, String slug) throws DBException {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String[] slugArray = slug.split("@");
        Account accountDb = new Account(context).getUniqAccount(slugArray[0], slugArray[1]);
        if (accountDb == null) {
            return;
        }
        String last_notifid = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + slug, null);
        if (since_ids.containsKey(slug)) {
            last_notifid = since_ids.get(slug);
        }

        //Check which notifications the user wants to see
        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        //User disagree with all notifications
        if (!notif_follow && !notif_fav && !notif_mention && !notif_share && !notif_poll)
            return; //Nothing is done

        MastodonNotificationsService mastodonNotificationsService = init(context, slugArray[1]);
        String finalLast_notifid = last_notifid;
        new Thread(() -> {
            Notifications notifications = new Notifications();
            Call<List<Notification>> notificationsCall;
            if (finalLast_notifid != null) {
                notificationsCall = mastodonNotificationsService.getNotifications(accountDb.token, null, null, null, finalLast_notifid, null, 30);
            } else {
                notificationsCall = mastodonNotificationsService.getNotifications(accountDb.token, null, null, null, null, null, 5);
            }
            if (notificationsCall != null) {
                try {
                    Response<List<Notification>> notificationsResponse = notificationsCall.execute();
                    if (notificationsResponse.isSuccessful()) {
                        notifications.notifications = notificationsResponse.body();
                        if (notifications.notifications != null) {
                            if (notifications.notifications.size() > 0) {
                                since_ids.put(slug, notifications.notifications.get(0).id);
                            }
                            for (Notification notification : notifications.notifications) {
                                if (notification != null && notification.status != null) {
                                    notification.status = SpannableHelper.convertStatus(context.getApplicationContext(), notification.status);
                                }
                            }
                        }
                        notifications.pagination = MastodonHelper.getPagination(notificationsResponse.headers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> onRetrieveNotifications(context, notifications, accountDb);
            mainHandler.post(myRunnable);
        }).start();

    }


    private static MastodonNotificationsService init(Context context, @NonNull String instance) {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

    public static void onRetrieveNotifications(Context context, Notifications newNotifications, final Account account) {
        List<Notification> notificationsReceived = newNotifications.notifications;
        if (notificationsReceived == null || notificationsReceived.size() == 0 || account == null)
            return;
        String key = account.user_id + "@" + account.instance;
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        boolean notif_status = prefs.getBoolean(context.getString(R.string.SET_NOTIF_STATUS), true);
        final String max_id = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + key, null);
        final List<Notification> notifications = new ArrayList<>();
        int pos = 0;
        for (Notification notif : notificationsReceived) {
            if (max_id == null || notif.id.compareTo(max_id) > 0) {
                notifications.add(pos, notif);
                pos++;
            }
        }
        if (notifications.size() == 0)
            return;
        //No previous notifications in cache, so no notification will be sent
        int newFollows = 0;
        int newAdds = 0;
        int newMentions = 0;
        int newShare = 0;
        int newPolls = 0;
        int newStatus = 0;
        String notificationUrl;
        String message = null;
        String targeted_account = null;
        Helper.NotifType notifType = Helper.NotifType.MENTION;
        for (Notification notification : notifications) {
            switch (notification.type) {
                case "mention":
                    notifType = Helper.NotifType.MENTION;
                    if (notif_mention) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_mention));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_mention));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY));
                                else
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.spoiler_text));
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY));
                                else
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.content));
                            }
                        }
                        newFollows++;
                    }
                    break;
                case "status":
                    notifType = Helper.NotifType.STATUS;
                    if (notif_status) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_status));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_status));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY));
                                else
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.spoiler_text));
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY));
                                else
                                    message = "\n" + new SpannableString(Html.fromHtml(notification.status.content));
                            }
                        }
                        newStatus++;
                    }
                    break;
                case "reblog":
                    notifType = Helper.NotifType.BOOST;
                    if (notif_share) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_reblog));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_reblog));
                        newShare++;
                    }
                    break;
                case "favourite":
                    notifType = Helper.NotifType.FAV;
                    if (notif_fav) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_favourite));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_favourite));
                        newAdds++;
                    }
                    break;
                case "follow_request":
                    notifType = Helper.NotifType.FOLLLOW;
                    if (notif_follow) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_follow_request));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_follow_request));
                        targeted_account = notification.account.id;
                        newFollows++;
                    }
                    break;
                case "follow":
                    notifType = Helper.NotifType.FOLLLOW;
                    if (notif_follow) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_follow));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_follow));
                        targeted_account = notification.account.id;
                        newFollows++;
                    }
                    break;
                case "poll":
                    notifType = Helper.NotifType.POLL;
                    if (notif_poll) {
                        if (notification.account.id != null && notification.account.id.equals(MainActivity.currentUserID))
                            message = context.getString(R.string.notif_poll_self);
                        else
                            message = context.getString(R.string.notif_poll);
                        newPolls++;
                    }
                    break;
                default:
            }

        }

        int allNotifCount = newFollows + newAdds + newMentions + newShare + newPolls + newStatus;
        if (allNotifCount > 0) {
            //Some others notification
            final Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Helper.INTENT_ACTION, Helper.NOTIFICATION_INTENT);
            intent.putExtra(Helper.PREF_KEY_ID, account.user_id);
            if (targeted_account != null && notifType == Helper.NotifType.FOLLLOW)
                intent.putExtra(Helper.INTENT_TARGETED_ACCOUNT, targeted_account);
            intent.putExtra(Helper.PREF_INSTANCE, account.instance);
            notificationUrl = notifications.get(0).account.avatar;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            final String finalNotificationUrl = notificationUrl;
            Helper.NotifType finalNotifType = notifType;
            String finalMessage = message;
            String finalMessage1 = message;
            Runnable myRunnable = () -> Glide.with(context)
                    .asBitmap()
                    .load(finalNotificationUrl != null ? finalNotificationUrl : R.drawable.fedilab_logo_bubbles)
                    .listener(new RequestListener<Bitmap>() {

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                            notify_user(context, account, intent, BitmapFactory.decodeResource(context.getResources(),
                                    R.mipmap.ic_launcher), finalNotifType, context.getString(R.string.top_notification), finalMessage1);
                            String lastNotif = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, null);
                            if (lastNotif == null || notifications.get(0).id.compareTo(lastNotif) > 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, notifications.get(0).id);
                                editor.apply();
                            }
                            return false;
                        }
                    })
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                            notify_user(context, account, intent, resource, finalNotifType, context.getString(R.string.top_notification), finalMessage);
                            String lastNotif = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, null);
                            if (lastNotif == null || notifications.get(0).id.compareTo(lastNotif) > 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, notifications.get(0).id);
                                editor.apply();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            mainHandler.post(myRunnable);

        }
    }
}
