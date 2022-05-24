package app.fedilab.android.jobs;
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.client.endpoints.MastodonStatusesService;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.ScheduledBoost;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScheduleBoostWorker extends Worker {

    private static final int NOTIFICATION_INT_CHANNEL_ID = 2;
    private static final String CHANNEL_ID = "schedule_boost";
    private final NotificationManager notificationManager;

    public ScheduleBoostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        if (Build.VERSION.SDK_INT >= 26) {
            String channelName = "Boost messages";
            String channelDescription = "Schedule boosts channel";
            NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            notifChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notifChannel);

        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getApplicationContext().getString(R.string.schedule_boost))
                .setContentText(getApplicationContext().getString(R.string.schedule_boost))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return new ForegroundInfo(NOTIFICATION_INT_CHANNEL_ID, notificationBuilder.build());
    }


    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(getApplicationContext()))
                .build();
    }

    private MastodonStatusesService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(getOkHttpClient())
                .build();
        return retrofit.create(MastodonStatusesService.class);
    }

    @NonNull
    @Override
    public Result doWork() {

        setForegroundAsync(createForegroundInfo());

        String instance = getInputData().getString(Helper.ARG_INSTANCE);
        String token = getInputData().getString(Helper.ARG_TOKEN);
        String statusId = getInputData().getString(Helper.ARG_STATUS_ID);
        String userID = getInputData().getString(Helper.ARG_USER_ID);
        Data outputData = new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.toast_error)).build();
        if (instance != null) {
            MastodonStatusesService mastodonStatusesService = init(instance);
            Call<Status> statusCall = mastodonStatusesService.reblog(token, statusId, null);
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        try {
                            new ScheduledBoost(getApplicationContext()).removeScheduled(instance, userID, statusId);
                            outputData = new Data.Builder().putString("WORK_RESULT", getApplicationContext().getString(R.string.notif_reblog)).build();
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.success(outputData);
    }
}
