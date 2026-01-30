package app.fedilab.android.misskey.viewmodel;
/* Copyright 2026 Thomas Schneider
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.api.Notifications;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyNotification;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisskeyNotificationsVM extends AndroidViewModel {

    private final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<Notifications> notificationsMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;

    public MisskeyNotificationsVM(@NonNull Application application) {
        super(application);
    }

    private MisskeyService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) + "/api/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MisskeyService.class);
    }

    public LiveData<Notifications> getNotifications(
            @NonNull String instance,
            String token,
            String maxId,
            String sinceId,
            Integer limit,
            String[] types,
            String[] excludeTypes) {
        notificationsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NotificationsRequest request = new MisskeyRequest.NotificationsRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.includeTypes = types;
            request.excludeTypes = excludeTypes;

            Notifications notifications = new Notifications();
            notifications.notifications = new ArrayList<>();
            notifications.pagination = new Pagination();

            try {
                Response<List<MisskeyNotification>> response = misskeyService.getNotifications(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyNotification misskeyNotification : response.body()) {
                        Notification notification = misskeyNotification.toNotification(instance);
                        if (notification.type != null) {
                            notifications.notifications.add(notification);
                        }
                    }
                    if (!response.body().isEmpty()) {
                        notifications.pagination.max_id = response.body().get(response.body().size() - 1).id;
                        notifications.pagination.min_id = response.body().get(0).id;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> notificationsMutableLiveData.setValue(notifications));
        }).start();
        return notificationsMutableLiveData;
    }

    public LiveData<Boolean> markAllAsRead(@NonNull String instance, String token) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.markAllNotificationsAsRead(request).execute();
                success = response.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            boolean finalSuccess = success;
            mainHandler.post(() -> booleanMutableLiveData.setValue(finalSuccess));
        }).start();
        return booleanMutableLiveData;
    }
}
