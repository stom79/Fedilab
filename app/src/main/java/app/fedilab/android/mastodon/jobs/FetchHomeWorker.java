package app.fedilab.android.mastodon.jobs;
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class FetchHomeWorker extends Worker {

    private static final int FETCH_HOME_CHANNEL_ID = 5;
    private static final String CHANNEL_ID = "fedilab_home";
    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplicationContext().getApplicationContext()))
            .build();
    private final NotificationManager notificationManager;


    public FetchHomeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void setRepeatHome(Context context, BaseAccount account) {
        WorkManager.getInstance(context).cancelAllWorkByTag(Helper.WORKER_REFRESH_HOME + account.user_id + account.instance);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(context.getString(R.string.SET_FETCH_HOME_DELAY_VALUE) + account.user_id + account.instance, "60");
        PeriodicWorkRequest notificationPeriodic = new PeriodicWorkRequest.Builder(NotificationsWorker.class, Long.parseLong(value), TimeUnit.MINUTES)
                .addTag(Helper.WORKER_REFRESH_NOTIFICATION)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(Helper.WORKER_REFRESH_HOME + account.user_id + account.instance, ExistingPeriodicWorkPolicy.REPLACE, notificationPeriodic);
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Notifications";
            String channelDescription = "Fetched notifications";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notifChannel.setDescription(channelDescription);
            notifChannel.setSound(null, null);
            notifChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notifChannel);
            if (notificationManager.getNotificationChannel("notifications") != null) {
                notificationManager.deleteNotificationChannel("notifications");
            }

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.notifications))
                .setContentText(getApplicationContext().getString(R.string.fetch_notifications))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return Futures.immediateFuture(new ForegroundInfo(FETCH_HOME_CHANNEL_ID, notificationBuilder.build()));
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Notifications";
            String channelDescription = "Fetched notifications";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            notifChannel.setSound(null, null);
            notifChannel.setShowBadge(false);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.notifications))
                .setContentText(getApplicationContext().getString(R.string.fetch_notifications))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSilent(true)
                .setPriority(Notification.PRIORITY_LOW);
        return new ForegroundInfo(FETCH_HOME_CHANNEL_ID, notificationBuilder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(createForegroundInfo());
        try {
            List<BaseAccount> accounts = new Account(getApplicationContext()).getCrossAccounts();
            for (BaseAccount account : accounts) {
                try {
                    fetchHome(getApplicationContext(), account);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        return Result.success(new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.notifications)).build());
    }

    private void fetchHome(Context context, BaseAccount account) throws IOException {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean fetch_home = prefs.getBoolean(context.getString(R.string.SET_FETCH_HOME) + account.user_id + account.instance, false);

        if (fetch_home) {
            int max_calls = 5;
            int status_per_page = 80;
            //Browse last 400 home messages
            boolean canContinue = true;
            int call = 0;
            String max_id = null;
            MastodonTimelinesService mastodonTimelinesService = init(account.instance);
            while (canContinue && call < max_calls) {
                Call<List<Status>> homeCall = mastodonTimelinesService.getHome(account.token, account.instance, max_id, null, status_per_page, null);
                if (homeCall != null) {
                    Response<List<Status>> homeResponse = homeCall.execute();
                    if (homeResponse.isSuccessful()) {
                        List<Status> statusList = homeResponse.body();
                        if (statusList != null && statusList.size() > 0) {
                            for (Status status : statusList) {
                                StatusCache statusCacheDAO = new StatusCache(getApplicationContext());
                                StatusCache statusCache = new StatusCache();
                                statusCache.instance = account.instance;
                                statusCache.user_id = account.user_id;
                                statusCache.status = status;
                                statusCache.type = Timeline.TimeLineEnum.HOME;
                                statusCache.status_id = status.id;
                                try {
                                    int inserted = statusCacheDAO.insertOrUpdate(statusCache, Timeline.TimeLineEnum.HOME.getValue());
                                    //We reached already cached messages
                                    if (inserted == 0) {
                                        canContinue = false;
                                        break;
                                    }
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }
                            Pagination pagination = MastodonHelper.getPagination(homeResponse.headers());
                            if (pagination.max_id != null) {
                                max_id = pagination.max_id;
                            } else {
                                canContinue = false;
                            }
                        } else {
                            canContinue = false;
                        }
                    } else {
                        canContinue = false;
                    }
                }
                call++;
            }

        }
    }

    private MastodonTimelinesService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }
}
