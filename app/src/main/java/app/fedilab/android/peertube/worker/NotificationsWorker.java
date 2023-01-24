package app.fedilab.android.peertube.worker;
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

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import java.util.List;
import java.util.concurrent.ExecutionException;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.peertube.activities.PeertubeActivity;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.activities.ShowAccountActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.NotificationData;
import app.fedilab.android.peertube.client.entities.Actor;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.NotificationSettings;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.fragment.DisplayNotificationsFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.NotificationHelper;

public class NotificationsWorker extends Worker {

    public static String FETCH_NOTIFICATION_CHANNEL_ID = "fetch_notification_peertube";
    public static int pendingNotificationID = 1;
    private final NotificationManager notificationManager;

    public NotificationsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }


    @NonNull
    @Override
    public Result doWork() {
        Context applicationContext = getApplicationContext();
        List<BaseAccount> accounts = new Account(applicationContext).getPeertubeAccounts();
        if (accounts == null || accounts.size() == 0) {
            return Result.success();
        }
        setForegroundAsync(createForegroundInfo());
        fetchNotification();
        return Result.success();
    }


    @SuppressWarnings({"SwitchStatementWithoutDefaultBranch", "DuplicateBranchesInSwitch"})
    private void fetchNotification() {
        List<BaseAccount> accounts = new Account(getApplicationContext()).getPeertubeAccounts();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedpreferences.edit();
        for (BaseAccount account : accounts) {
            RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(getApplicationContext(), account.instance, account.token);
            APIResponse apiResponse = retrofitPeertubeAPI.getNotifications();
            if (apiResponse == null) {
                return;
            }
            try {
                UserMe userMe = retrofitPeertubeAPI.verifyCredentials();
                if (userMe != null) {
                    List<NotificationData.Notification> notifications = apiResponse.getPeertubeNotifications();
                    NotificationSettings notificationSettings = userMe.getNotificationSettings();
                    if (apiResponse.getPeertubeNotifications() != null && apiResponse.getPeertubeNotifications().size() > 0) {
                        String last_read = sharedpreferences.getString(Helper.LAST_NOTIFICATION_READ + account.user_id + account.instance, null);
                        editor.putString(Helper.LAST_NOTIFICATION_READ + account.user_id + account.instance, apiResponse.getPeertubeNotifications().get(0).getId());
                        editor.apply();
                        if (last_read != null) {
                            for (NotificationData.Notification notification : notifications) {
                                String title = "";
                                String message = "";
                                FutureTarget<Bitmap> futureBitmap = Glide.with(getApplicationContext())
                                        .asBitmap()
                                        .load("https://" + account.instance + account.peertube_account.getAvatar()).submit();
                                Bitmap icon;
                                try {
                                    icon = futureBitmap.get();
                                } catch (Exception e) {
                                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                            R.drawable.missing_peertube);
                                }

                                Intent intent = null;
                                if (notification.getId().compareTo(last_read) > 0) {
                                    switch (notification.getType()) {
                                        case DisplayNotificationsFragment.NEW_VIDEO_FROM_SUBSCRIPTION:
                                            if (notificationSettings.getNewVideoFromSubscription() == 1 || notificationSettings.getNewVideoFromSubscription() == 3) {
                                                if (notification.getVideo().getChannel().getAvatar() != null) {
                                                    FutureTarget<Bitmap> futureBitmapChannel = Glide.with(getApplicationContext())
                                                            .asBitmap()
                                                            .load("https://" + account.instance + notification.getVideo().getChannel().getAvatar().getPath()).submit();
                                                    try {
                                                        icon = futureBitmapChannel.get();
                                                    } catch (Exception e) {
                                                        icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                                                R.drawable.missing_peertube);
                                                    }

                                                } else {
                                                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                                            R.drawable.missing_peertube);
                                                }
                                                title = getApplicationContext().getString(R.string.new_video);
                                                message = getApplicationContext().getString(R.string.peertube_video_from_subscription, notification.getVideo().getChannel().getDisplayName(), notification.getVideo().getName());
                                                intent = new Intent(getApplicationContext(), PeertubeActivity.class);
                                                Bundle b = new Bundle();
                                                b.putSerializable("video", notification.getVideo());
                                                b.putString("peertube_instance", notification.getVideo().getChannel().getHost());
                                                b.putBoolean("isMyVideo", false);
                                                b.putString("video_id", notification.getVideo().getId());
                                                b.putString("video_uuid", notification.getVideo().getUuid());
                                                intent.putExtras(b);
                                            }
                                            break;
                                        case DisplayNotificationsFragment.NEW_COMMENT_ON_MY_VIDEO:
                                            if (notificationSettings.getNewCommentOnMyVideo() == 1 || notificationSettings.getNewCommentOnMyVideo() == 3) {
                                                if (notification.getComment().getAccount().getAvatar() != null) {
                                                    FutureTarget<Bitmap> futureBitmapChannel = Glide.with(getApplicationContext())
                                                            .asBitmap()
                                                            .load("https://" + account.instance + notification.getComment().getAccount().getAvatar().getPath()).submit();
                                                    try {
                                                        icon = futureBitmapChannel.get();
                                                    } catch (Exception e) {
                                                        icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                                                R.drawable.missing_peertube);
                                                    }

                                                } else {
                                                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                                            R.drawable.missing_peertube);
                                                }
                                                title = getApplicationContext().getString(R.string.new_comment);
                                                message = getApplicationContext().getString(R.string.peertube_comment_on_video, notification.getComment().getAccount().getDisplayName(), notification.getComment().getAccount().getUsername());
                                                intent = new Intent(getApplicationContext(), PeertubeActivity.class);
                                                Bundle b = new Bundle();
                                                b.putSerializable("video", notification.getVideo());
                                                b.putString("peertube_instance", notification.getVideo().getChannel().getHost());
                                                b.putBoolean("isMyVideo", false);
                                                b.putString("video_id", notification.getVideo().getId());
                                                b.putString("video_uuid", notification.getVideo().getUuid());
                                                intent.putExtras(b);
                                            }

                                            break;
                                        case DisplayNotificationsFragment.NEW_ABUSE_FOR_MODERATORS:

                                            break;
                                        case DisplayNotificationsFragment.BLACKLIST_ON_MY_VIDEO:
                                            if (notificationSettings.getBlacklistOnMyVideo() == 1 || notificationSettings.getBlacklistOnMyVideo() == 3) {
                                                title = getApplicationContext().getString(R.string.new_blacklist);
                                                message = getApplicationContext().getString(R.string.peertube_video_blacklist, notification.getVideo().getName());
                                            }
                                            break;
                                        case DisplayNotificationsFragment.UNBLACKLIST_ON_MY_VIDEO:
                                            if (notificationSettings.getBlacklistOnMyVideo() == 1 || notificationSettings.getBlacklistOnMyVideo() == 3) {
                                                title = getApplicationContext().getString(R.string.new_blacklist);
                                                message = getApplicationContext().getString(R.string.peertube_video_unblacklist, notification.getVideo().getName());
                                            }
                                            break;
                                        case DisplayNotificationsFragment.MY_VIDEO_PUBLISHED:
                                            if (notificationSettings.getMyVideoPublished() == 1 || notificationSettings.getMyVideoPublished() == 3) {
                                                title = getApplicationContext().getString(R.string.new_my_video_published);
                                                message = getApplicationContext().getString(R.string.peertube_video_published, notification.getVideo().getName());
                                            }
                                            break;
                                        case DisplayNotificationsFragment.MY_VIDEO_IMPORT_SUCCESS:
                                            if (notificationSettings.getMyVideoPublished() == 1 || notificationSettings.getMyVideoPublished() == 3) {
                                                message = getApplicationContext().getString(R.string.peertube_video_import_success, notification.getVideo().getName());
                                                title = getApplicationContext().getString(R.string.new_my_video_error);
                                            }
                                            break;
                                        case DisplayNotificationsFragment.MY_VIDEO_IMPORT_ERROR:
                                            if (notificationSettings.getMyVideoPublished() == 1 || notificationSettings.getMyVideoPublished() == 3) {
                                                message = getApplicationContext().getString(R.string.peertube_video_import_error, notification.getVideo().getName());
                                                title = getApplicationContext().getString(R.string.new_my_video_error);
                                            }
                                            break;
                                        case DisplayNotificationsFragment.NEW_USER_REGISTRATION:

                                            break;
                                        case DisplayNotificationsFragment.NEW_FOLLOW:
                                            if (notificationSettings.getNewFollow() == 1 || notificationSettings.getNewFollow() == 3) {
                                                if (notification.getVideo().getChannel().getAvatar() != null) {
                                                    FutureTarget<Bitmap> futureBitmapChannel = Glide.with(getApplicationContext())
                                                            .asBitmap()
                                                            .load("https://" + account.instance + notification.getVideo().getChannel().getAvatar().getPath()).submit();
                                                    icon = futureBitmapChannel.get();

                                                } else {
                                                    icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                                            R.drawable.missing_peertube);
                                                }
                                                title = getApplicationContext().getString(R.string.new_video);
                                                String type = notification.getActorFollow().getFollowing().getType();
                                                if (type != null && type.compareTo("channel") == 0) {
                                                    message = getApplicationContext().getString(R.string.peertube_follow_channel, notification.getActorFollow().getFollower().getDisplayName(), notification.getActorFollow().getFollowing().getDisplayName());
                                                } else {
                                                    message = getApplicationContext().getString(R.string.peertube_follow_account, notification.getActorFollow().getFollower().getDisplayName());
                                                }
                                                Bundle b = new Bundle();
                                                Actor actor = notification.getActorFollow().getFollower();
                                                AccountData.PeertubeAccount accountAction = new AccountData.PeertubeAccount();
                                                accountAction.setAvatar(actor.getAvatar());
                                                accountAction.setDisplayName(actor.getDisplayName());
                                                accountAction.setHost(actor.getHost());
                                                accountAction.setUsername(actor.getName());
                                                intent = new Intent(getApplicationContext(), ShowAccountActivity.class);
                                                b.putSerializable("account", accountAction);
                                                b.putString("accountAcct", accountAction.getUsername() + "@" + accountAction.getHost());
                                                intent.putExtras(b);
                                            }
                                            break;
                                        case DisplayNotificationsFragment.COMMENT_MENTION:

                                            break;
                                        case DisplayNotificationsFragment.VIDEO_AUTO_BLACKLIST_FOR_MODERATORS:

                                            break;
                                        case DisplayNotificationsFragment.NEW_INSTANCE_FOLLOWER:

                                            break;
                                        case DisplayNotificationsFragment.AUTO_INSTANCE_FOLLOWING:

                                            break;
                                        case DisplayNotificationsFragment.MY_VIDEO_REPPORT_SUCCESS:

                                            break;
                                        case DisplayNotificationsFragment.ABUSE_NEW_MESSAGE:

                                            break;
                                    }
                                    if (message != null && icon != null && title != null) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                            message = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY).toString();
                                        else
                                            message = Html.fromHtml(message).toString();
                                        NotificationHelper.notify_user(getApplicationContext(), account.peertube_account, intent, icon, title, message);
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }

            } catch (Error | InterruptedException | ExecutionException error) {
                error.printStackTrace();
            }

        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {

        String title = getApplicationContext().getString(R.string.fetch_notification_channel_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FETCH_NOTIFICATION_CHANNEL_ID,
                    getApplicationContext().getString(R.string.fetch_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
        Intent myIntent = new Intent(getApplicationContext(), PeertubeMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                myIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), FETCH_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setTicker(title)
                .setProgress(100, 0, false)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setSound(null)
                .setAutoCancel(true)
                .setOngoing(true);
        return new ForegroundInfo(pendingNotificationID, notificationBuilder.build());
    }
}
