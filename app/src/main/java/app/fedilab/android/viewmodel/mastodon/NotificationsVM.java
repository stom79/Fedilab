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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.endpoints.MastodonNotificationsService;
import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.Notifications;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.PushSubscription;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.TimelineHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();


    private MutableLiveData<Notifications> notificationsMutableLiveData;
    private MutableLiveData<Notification> notificationMutableLiveData;
    private MutableLiveData<Void> voidMutableLiveData;
    private MutableLiveData<PushSubscription> pushSubscriptionMutableLiveData;

    public NotificationsVM(@NonNull Application application) {
        super(application);
    }

    private static void sortDesc(List<Notification> notificationList) {
        Collections.sort(notificationList, (obj1, obj2) -> obj2.id.compareToIgnoreCase(obj1.id));
    }

    private static void addFetchMoreNotifications(List<Notification> notificationList, List<Notification> timelineNotifications, TimelinesVM.TimelineParams timelineParams) throws DBException {
        if (notificationList != null && notificationList.size() > 0 && timelineNotifications != null && timelineNotifications.size() > 0) {
            sortDesc(notificationList);
            if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.REFRESH || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.FETCH_NEW) {
                //When refreshing/scrolling to TOP, if last statuses fetched has a greater id from newest in cache, there is potential hole
                if (notificationList.get(notificationList.size() - 1).id.compareToIgnoreCase(timelineNotifications.get(0).id) > 0) {
                    notificationList.get(notificationList.size() - 1).isFetchMore = true;
                    notificationList.get(notificationList.size() - 1).positionFetchMore = Notification.PositionFetchMore.TOP;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.TOP && timelineParams.fetchingMissing) {
                if (!timelineNotifications.contains(notificationList.get(0))) {
                    notificationList.get(0).isFetchMore = true;
                    notificationList.get(0).positionFetchMore = Notification.PositionFetchMore.BOTTOM;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.BOTTOM && timelineParams.fetchingMissing) {
                if (!timelineNotifications.contains(notificationList.get(notificationList.size() - 1))) {
                    notificationList.get(notificationList.size() - 1).isFetchMore = true;
                    notificationList.get(notificationList.size() - 1).positionFetchMore = Notification.PositionFetchMore.TOP;
                }
            }
        }
    }

    private MastodonNotificationsService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

    /**
     * Get notifications for the authenticated account
     *
     * @return {@link LiveData} containing a {@link Notifications}
     */
    public LiveData<Notifications> getNotifications(List<Notification> notificationList, TimelinesVM.TimelineParams timelineParams) {
        notificationsMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(timelineParams.instance);
        new Thread(() -> {
            Notifications notifications = new Notifications();
            Call<List<Notification>> notificationsCall = mastodonNotificationsService.getNotifications(timelineParams.token, timelineParams.excludeType, null, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
            if (notificationsCall != null) {
                try {
                    Response<List<Notification>> notificationsResponse = notificationsCall.execute();
                    if (notificationsResponse.isSuccessful()) {
                        List<Notification> notFiltered = notificationsResponse.body();
                        notifications.notifications = TimelineHelper.filterNotification(getApplication().getApplicationContext(), notFiltered);
                        notifications.pagination = MastodonHelper.getPagination(notificationsResponse.headers());
                        if (notifications.notifications != null && notifications.notifications.size() > 0) {
                            addFetchMoreNotifications(notifications.notifications, notificationList, timelineParams);
                            if (notFiltered != null && notFiltered.size() > 0) {
                                for (Notification notification : notFiltered) {
                                    StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
                                    StatusCache statusCache = new StatusCache();
                                    statusCache.instance = timelineParams.instance;
                                    statusCache.user_id = timelineParams.userId;
                                    statusCache.notification = notification;
                                    statusCache.slug = notification.type;
                                    statusCache.type = Timeline.TimeLineEnum.NOTIFICATION;
                                    statusCache.status_id = notification.id;
                                    try {
                                        statusCacheDAO.insertOrUpdate(statusCache, notification.type);
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> notificationsMutableLiveData.setValue(notifications);
            mainHandler.post(myRunnable);
        }).start();

        return notificationsMutableLiveData;
    }

    public LiveData<Notifications> getNotificationCache(List<Notification> timelineNotification, TimelinesVM.TimelineParams timelineParams) {
        notificationsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
            Notifications notifications = new Notifications();
            List<Notification> notificationsDb;
            try {
                notificationsDb = statusCacheDAO.getNotifications(timelineParams.excludeType, timelineParams.instance, timelineParams.userId, timelineParams.maxId, timelineParams.minId, timelineParams.sinceId);
                if (notificationsDb != null && notificationsDb.size() > 0) {
                    if (timelineNotification != null) {
                        List<Notification> notPresentNotifications = new ArrayList<>();
                        for (Notification notification : notificationsDb) {
                            if (!timelineNotification.contains(notification)) {
                                notification.cached = true;
                                notPresentNotifications.add(notification);
                            }
                        }
                        //Only not already present statuses are added
                        notificationsDb = notPresentNotifications;
                    }
                    notifications.notifications = TimelineHelper.filterNotification(getApplication().getApplicationContext(), notificationsDb);
                    if (notifications.notifications.size() > 0) {
                        addFetchMoreNotifications(notifications.notifications, timelineNotification, timelineParams);
                        notifications.pagination = new Pagination();
                        notifications.pagination.min_id = notifications.notifications.get(0).id;
                        notifications.pagination.max_id = notifications.notifications.get(notifications.notifications.size() - 1).id;
                    }
                }
            } catch (DBException e) {
                e.printStackTrace();
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
                    }
                } catch (Exception e) {
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
     * @param user_id  String - UserId for the api call
     * @param instance String - Instance for the api call
     * @param token    String - Token of the authenticated account
     */
    public LiveData<Void> clearNotification(@NonNull String user_id, @NonNull String instance, String token) {
        voidMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            Call<Void> voidCall = mastodonNotificationsService.clearAllNotifications(token);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                    new StatusCache(getApplication().getApplicationContext()).deleteNotifications(user_id, instance);
                } catch (Exception e) {
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
                } catch (Exception e) {
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
                } catch (Exception e) {
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
                } catch (Exception e) {
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
                } catch (Exception e) {
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
                } catch (Exception e) {
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
