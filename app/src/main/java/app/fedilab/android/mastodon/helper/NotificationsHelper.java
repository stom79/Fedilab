package app.fedilab.android.mastodon.helper;
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
import static app.fedilab.android.mastodon.helper.LogoHelper.getMainLogo;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonNotificationsService;
import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.api.Notifications;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
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
        BaseAccount accountDb = new Account(context).getUniqAccount(slugArray[0], slugArray[1]);
        if (accountDb == null) {
            return;
        }
        String last_notifid;
        if (since_ids.containsKey(slug)) {
            last_notifid = since_ids.get(slug);
        } else {
            last_notifid = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + slug, null);
            since_ids.put(slug, last_notifid);
        }

        //Check which notifications the user wants to see
        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        boolean notif_status = prefs.getBoolean(context.getString(R.string.SET_NOTIF_STATUS), true);
        boolean notif_updates = prefs.getBoolean(context.getString(R.string.SET_NOTIF_UPDATE), true);
        boolean notif_signup = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_SIGNUP), true);
        boolean notif_report = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_REPORT), true);

        //User disagree with all notifications
        if (!notif_follow && !notif_fav && !notif_mention && !notif_share && !notif_poll && !notif_status && !notif_updates && !notif_signup && !notif_report)
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
                        }
                        notifications.pagination = MastodonHelper.getPagination(notificationsResponse.headers());
                    }
                } catch (Exception e) {
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
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

    public static void onRetrieveNotifications(Context context, Notifications newNotifications, final BaseAccount account) {
        if (newNotifications == null || newNotifications.notifications == null || newNotifications.notifications.size() == 0 || account == null) {
            return;
        }
        List<Notification> notificationsReceived = newNotifications.notifications;
        String key = account.user_id + "@" + account.instance;
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        boolean notif_follow = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FOLLOW), true);
        boolean notif_mention = prefs.getBoolean(context.getString(R.string.SET_NOTIF_MENTION), true);
        boolean notif_share = prefs.getBoolean(context.getString(R.string.SET_NOTIF_SHARE), true);
        boolean notif_poll = prefs.getBoolean(context.getString(R.string.SET_NOTIF_POLL), true);
        boolean notif_fav = prefs.getBoolean(context.getString(R.string.SET_NOTIF_FAVOURITE), true);
        boolean notif_status = prefs.getBoolean(context.getString(R.string.SET_NOTIF_STATUS), true);
        boolean notif_update = prefs.getBoolean(context.getString(R.string.SET_NOTIF_UPDATE), true);
        boolean notif_signup = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_SIGNUP), true);
        boolean notif_report = prefs.getBoolean(context.getString(R.string.SET_NOTIF_ADMIN_REPORT), true);

        final String max_id = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + key, null);

        final List<Notification> notifications = new ArrayList<>();
        int pos = 0;
        for (Notification notif : notificationsReceived) {
            if (max_id == null || Helper.compareTo(notif.id, max_id) > 0) {
                notifications.add(pos, notif);
                pos++;
            }
        }

        if (notifications.size() == 0) {
            return;
        }
        //No previous notifications in cache, so no notification will be sent

        for (Notification notification : notifications) {
            String notificationUrl;
            String title = null;
            String message = null;
            String targeted_account = null;
            Helper.NotifType notifType = Helper.NotifType.MENTION;
            switch (notification.type) {
                case "mention":
                    if (notif_mention) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            title = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_mention));
                        else
                            title = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_mention));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "status":
                    notifType = Helper.NotifType.STATUS;
                    if (notif_status) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            title = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_status));
                        else
                            title = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_status));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "reblog":
                    notifType = Helper.NotifType.BOOST;
                    if (notif_share) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            title = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_reblog));
                        else
                            title = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_reblog));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "favourite":
                    notifType = Helper.NotifType.FAV;
                    if (notif_fav) {
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            title = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_favourite));
                        else
                            title = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_favourite));
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "follow_request":
                    notifType = Helper.NotifType.FOLLLOW;
                    if (notif_follow) {
                        title = context.getString(R.string.channel_notif_follow);
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_follow_request));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_follow_request));
                        targeted_account = notification.account.id;
                    }
                    break;
                case "follow":
                    notifType = Helper.NotifType.FOLLLOW;
                    if (notif_follow) {
                        title = context.getString(R.string.channel_notif_follow);
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_follow));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_follow));
                        targeted_account = notification.account.id;
                    }
                    break;
                case "poll":
                    notifType = Helper.NotifType.POLL;
                    if (notif_poll) {
                        if (notification.account.id != null && notification.account.id.equals(BaseMainActivity.currentUserID))
                            title = context.getString(R.string.notif_poll_self);
                        else
                            title = context.getString(R.string.notif_poll);
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "update":
                    notifType = Helper.NotifType.UPDATE;
                    if (notif_update) {
                        title = context.getString(R.string.notif_update_push);
                        if (notification.status != null) {
                            if (notification.status.spoiler_text != null && notification.status.spoiler_text.length() > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.spoiler_text)).toString();
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    message = new SpannableString(Html.fromHtml(notification.status.content, FROM_HTML_MODE_LEGACY)).toString();
                                else
                                    message = new SpannableString(Html.fromHtml(notification.status.content)).toString();
                            }
                        }
                    }
                    break;
                case "admin.sign_up":
                    notifType = Helper.NotifType.SIGN_UP;
                    if (notif_signup) {
                        title = context.getString(R.string.notif_sign_up);
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_signed_up));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_signed_up));
                        targeted_account = notification.account.id;
                    }
                    break;
                case "admin.report":
                    notifType = Helper.NotifType.REPORT;
                    if (notif_report) {
                        title = context.getString(R.string.notif_report);
                        if (notification.account.display_name != null && notification.account.display_name.length() > 0)
                            message = String.format("%s %s", notification.account.display_name, context.getString(R.string.notif_reported));
                        else
                            message = String.format("@%s %s", notification.account.acct, context.getString(R.string.notif_reported));
                        targeted_account = notification.account.id;
                    }
                    break;
                default:
            }
            if (message != null) {
                //Some others notification
                final Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Helper.INTENT_ACTION, Helper.NOTIFICATION_INTENT);
                intent.putExtra(Helper.PREF_USER_ID, account.user_id);
                if (targeted_account != null)
                    intent.putExtra(Helper.INTENT_TARGETED_ACCOUNT, targeted_account);
                intent.putExtra(Helper.PREF_USER_INSTANCE, account.instance);
                notificationUrl = notification.account.avatar;
                Handler mainHandler = new Handler(Looper.getMainLooper());
                final String finalNotificationUrl = notificationUrl;
                Helper.NotifType finalNotifType = notifType;
                String finalMessage = message;
                String finalTitle = title;
                StatusAdapter.sendAction(context, Helper.ARG_REFRESH_NOTFICATION, null, null);
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
                                String lastNotif = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, null);
                                if (lastNotif == null || Helper.compareTo(notification.id, lastNotif) > 0) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    since_ids.put(account.user_id + "@" + account.instance, lastNotif);
                                    editor.putString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, notifications.get(0).id);
                                    editor.apply();
                                    Helper.notify_user(context, account, intent, BitmapFactory.decodeResource(context.getResources(),
                                            getMainLogo(context)), finalNotifType, finalTitle, finalMessage);
                                }
                                return false;
                            }
                        })
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                String lastNotif = prefs.getString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, null);
                                if (lastNotif == null || Helper.compareTo(notification.id, lastNotif) > 0) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString(context.getString(R.string.LAST_NOTIFICATION_ID) + account.user_id + "@" + account.instance, notifications.get(0).id);
                                    editor.apply();
                                    since_ids.put(account.user_id + "@" + account.instance, lastNotif);
                                    Helper.notify_user(context, account, intent, resource, finalNotifType, finalTitle, finalMessage);
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
}
