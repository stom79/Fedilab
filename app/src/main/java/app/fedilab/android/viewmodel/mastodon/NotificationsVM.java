package app.fedilab.android.viewmodel.mastodon;
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.mastodon.MastodonNotificationsService;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Notifications;
import app.fedilab.android.client.mastodon.entities.PushSubscription;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.TimelineHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();


    private MutableLiveData<Notifications> notificationsMutableLiveData;
    private MutableLiveData<Notification> notificationMutableLiveData;
    private MutableLiveData<Void> voidMutableLiveData;
    private MutableLiveData<PushSubscription> pushSubscriptionMutableLiveData;

    public NotificationsVM(@NonNull Application application) {
        super(application);
    }

    private MastodonNotificationsService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

    /**
     * Get notifications for the authenticated account
     *
     * @param instance     String - Instance for the api call
     * @param token        String - Token of the authenticated account
     * @param maxId        String - max id for pagination
     * @param sinceId      String - since id for pagination
     * @param minId        String - min id for pagination
     * @param limit        int - result fetched
     * @param exlude_types List<String> - type of notifications to exclude in reply
     * @param account_id   String - target notifications from an account
     * @return {@link LiveData} containing a {@link Notifications}
     */
    public LiveData<Notifications> getNotifications(@NonNull String instance, String token,
                                                    String maxId,
                                                    String sinceId,
                                                    String minId,
                                                    int limit,
                                                    List<String> exlude_types,
                                                    String account_id) {
        notificationsMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Notifications notifications = new Notifications();
            Call<List<Notification>> notificationsCall = mastodonNotificationsService.getNotifications(token, exlude_types, account_id, maxId, sinceId, minId, limit);
            if (notificationsCall != null) {
                try {
                    Response<List<Notification>> notificationsResponse = notificationsCall.execute();
                    if (notificationsResponse.isSuccessful()) {
                        List<Notification> notFilteredNotifications = notificationsResponse.body();
                        notifications.notifications = TimelineHelper.filterNotification(getApplication().getApplicationContext(), notFilteredNotifications);
                        for (Notification notification : notifications.notifications) {
                            if (notification != null) {
                                notification.status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), notification.status);
                            }
                        }
                        notifications.pagination = MastodonHelper.getPaginationNotification(notifications.notifications);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> notificationsMutableLiveData.setValue(notifications);
            mainHandler.post(myRunnable);
        }).start();

        return notificationsMutableLiveData;
    }


    /**
     * Get a notification for the authenticated account by its id
     *
     * @param instance        String - Instance for the api call
     * @param token           String - Token of the authenticated account
     * @param notification_id String - id of the notification
     * @return {@link LiveData} containing a {@link Notification}
     */
    public LiveData<Notification> getSingleNotification(@NonNull String instance, String token,
                                                        String notification_id) {
        notificationMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Notification notification = null;
            Call<Notification> notificationCall = mastodonNotificationsService.getNotification(token, notification_id);
            if (notificationCall != null) {
                try {
                    Response<Notification> notificationResponse = notificationCall.execute();
                    if (notificationResponse.isSuccessful()) {
                        notification = notificationResponse.body();
                        if (notification != null) {
                            notification.status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), notification.status);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Notification finalNotification = notification;
            Runnable myRunnable = () -> notificationMutableLiveData.setValue(finalNotification);
            mainHandler.post(myRunnable);
        }).start();

        return notificationMutableLiveData;
    }

    /**
     * Get a notification for the authenticated account by its id
     *
     * @param instance String - Instance for the api call
     * @param token    String - Token of the authenticated account
     */
    public LiveData<Void> clearNotification(@NonNull String instance, String token) {
        voidMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Call<Void> voidCall = mastodonNotificationsService.clearAllNotifications(token);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> voidMutableLiveData.setValue(null);
            mainHandler.post(myRunnable);
        }).start();

        return voidMutableLiveData;
    }


    /**
     * Get a notification for the authenticated account by its id
     *
     * @param instance        String - Instance for the api call
     * @param token           String - Token of the authenticated account
     * @param notification_id String - id of the notification
     */
    public LiveData<Void> dismissNotification(@NonNull String instance, String token, String notification_id) {
        voidMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Call<Void> voidCall = mastodonNotificationsService.dismissNotification(token, notification_id);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> voidMutableLiveData.setValue(null);
            mainHandler.post(myRunnable);
        }).start();

        return voidMutableLiveData;
    }


    /**
     * Subscribe to push notifications
     *
     * @param instance    String - server instance
     * @param token       String
     * @param endpoint    String - Endpoint URL that is called when a notification event occurs.
     * @param keys_p256dh String - User agent public key. Base64 encoded string of public key of ECDH key using prime256v1 curve.
     * @param keys_auth   String  - Auth secret. Base64 encoded string of 16 bytes of random data.
     * @param follow      boolean - Receive follow notifications?
     * @param favourite   boolean - Receive favourite notifications?
     * @param reblog      boolean - Receive reblog notifications?
     * @param mention     boolean - Receive mention notifications?
     * @param poll        boolean - Receive poll notifications?
     * @return {@link LiveData} containing a {@link PushSubscription}
     */
    public LiveData<PushSubscription> pushSubscription(@NonNull String instance, String token,
                                                       String endpoint,
                                                       String keys_p256dh,
                                                       String keys_auth,
                                                       boolean follow,
                                                       boolean favourite,
                                                       boolean reblog,
                                                       boolean mention,
                                                       boolean poll) {
        pushSubscriptionMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            PushSubscription pushSubscription = null;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.pushSubscription(token, endpoint, keys_p256dh, keys_auth, follow, favourite, reblog, mention, poll);
            if (pushSubscriptionCall != null) {
                try {
                    Response<PushSubscription> pushSubscriptionResponse = pushSubscriptionCall.execute();
                    if (pushSubscriptionResponse.isSuccessful()) {
                        pushSubscription = pushSubscriptionResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            PushSubscription finalPushSubscription = pushSubscription;
            Runnable myRunnable = () -> pushSubscriptionMutableLiveData.setValue(finalPushSubscription);
            mainHandler.post(myRunnable);
        }).start();

        return pushSubscriptionMutableLiveData;
    }


    /**
     * Get push notifications
     *
     * @param instance String - server instance
     * @param token    String
     * @return {@link LiveData} containing a {@link PushSubscription}
     */
    public LiveData<PushSubscription> getPushSubscription(@NonNull String instance, String token) {
        pushSubscriptionMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            PushSubscription pushSubscription = null;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.getPushSubscription(token);
            if (pushSubscriptionCall != null) {
                try {
                    Response<PushSubscription> pushSubscriptionResponse = pushSubscriptionCall.execute();
                    if (pushSubscriptionResponse.isSuccessful()) {
                        pushSubscription = pushSubscriptionResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            PushSubscription finalPushSubscription = pushSubscription;
            Runnable myRunnable = () -> pushSubscriptionMutableLiveData.setValue(finalPushSubscription);
            mainHandler.post(myRunnable);
        }).start();

        return pushSubscriptionMutableLiveData;
    }


    /**
     * Subscribe to push notifications
     *
     * @param instance  String - server instance
     * @param token     String
     * @param follow    boolean - Receive follow notifications?
     * @param favourite boolean - Receive favourite notifications?
     * @param reblog    boolean - Receive reblog notifications?
     * @param mention   boolean - Receive mention notifications?
     * @param poll      boolean - Receive poll notifications?
     * @return {@link LiveData} containing a {@link PushSubscription}
     */
    public LiveData<PushSubscription> updatePushSubscription(@NonNull String instance, String token,
                                                             boolean follow,
                                                             boolean favourite,
                                                             boolean reblog,
                                                             boolean mention,
                                                             boolean poll) {
        pushSubscriptionMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            PushSubscription pushSubscription = null;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.updatePushSubscription(token, follow, favourite, reblog, mention, poll);
            if (pushSubscriptionCall != null) {
                try {
                    Response<PushSubscription> pushSubscriptionResponse = pushSubscriptionCall.execute();
                    if (pushSubscriptionResponse.isSuccessful()) {
                        pushSubscription = pushSubscriptionResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            PushSubscription finalPushSubscription = pushSubscription;
            Runnable myRunnable = () -> pushSubscriptionMutableLiveData.setValue(finalPushSubscription);
            mainHandler.post(myRunnable);
        }).start();

        return pushSubscriptionMutableLiveData;
    }

    /**
     * Delete push notifications
     *
     * @param instance String - Instance for the api call
     * @param token    String - Token of the authenticated account
     */
    public LiveData<Void> deletePushsubscription(@NonNull String instance, String token) {
        voidMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Call<Void> voidCall = mastodonNotificationsService.deletePushsubscription(token);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> voidMutableLiveData.setValue(null);
            mainHandler.post(myRunnable);
        }).start();

        return voidMutableLiveData;
    }

}
