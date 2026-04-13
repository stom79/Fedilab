package app.fedilab.android.mastodon.viewmodel.mastodon;
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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;


import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.preference.PreferenceManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonNotificationsService;
import app.fedilab.android.mastodon.client.entities.api.GroupedNotificationsResults;
import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.api.NotificationGroup;
import app.fedilab.android.mastodon.client.entities.api.NotificationPolicy;
import app.fedilab.android.mastodon.client.entities.api.NotificationRequest;
import app.fedilab.android.mastodon.client.entities.api.NotificationRequests;
import app.fedilab.android.mastodon.client.entities.api.Notifications;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.PushSubscription;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.TimelineHelper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsVM extends AndroidViewModel {

    private static final Map<String, Boolean> v2UnsupportedInstances = new HashMap<>();
    private static final List<String> GROUPED_TYPES = Arrays.asList("favourite", "reblog", "follow");
    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());


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
                if (!timelineNotifications.contains(notificationList.get(notificationList.size() - 1))) {
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

    private MastodonNotificationsService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonNotificationsService.class);
    }

    private MastodonNotificationsService initV2(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
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
        new Thread(() -> {
            Notifications notifications = new Notifications();
            boolean v2Success = false;

            // Try grouped notifications API (v2) if the instance supports it and aggregation is enabled
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext());
            boolean aggregateNotification = sharedpreferences.getBoolean(getApplication().getString(R.string.SET_AGGREGATE_NOTIFICATION), true);
            if (aggregateNotification && !Boolean.TRUE.equals(v2UnsupportedInstances.get(timelineParams.instance))) {
                try {
                    MastodonNotificationsService serviceV2 = initV2(timelineParams.instance);
                    Call<GroupedNotificationsResults> groupedCall = serviceV2.getGroupedNotifications(
                            timelineParams.token, timelineParams.excludeType, null,
                            timelineParams.maxId, timelineParams.sinceId, timelineParams.minId,
                            timelineParams.limit, GROUPED_TYPES);
                    if (groupedCall != null) {
                        Response<GroupedNotificationsResults> groupedResponse = groupedCall.execute();
                        if (groupedResponse.isSuccessful() && groupedResponse.body() != null) {
                            List<Notification> converted = NotificationGroup.fromGroupedResults(groupedResponse.body());
                            notifications.notifications = TimelineHelper.filterNotification(getApplication().getApplicationContext(), converted);
                            notifications.pagination = MastodonHelper.getPagination(groupedResponse.headers());
                            notifications.groupedByServer = true;
                            v2Success = true;
                            if (notifications.notifications != null && !notifications.notifications.isEmpty()) {
                                addFetchMoreNotifications(notifications.notifications, notificationList, timelineParams);
                                cacheNotifications(converted, timelineParams);
                            }
                        } else if (groupedResponse.code() == 404 || groupedResponse.code() == 410) {
                            v2UnsupportedInstances.put(timelineParams.instance, true);
                        }
                    }
                } catch (Exception e) {
                    v2UnsupportedInstances.put(timelineParams.instance, true);
                }
            }

            // Fallback to v1
            if (!v2Success) {
                MastodonNotificationsService mastodonNotificationsService = init(timelineParams.instance);
                Call<List<Notification>> notificationsCall = mastodonNotificationsService.getNotifications(timelineParams.token, timelineParams.excludeType, null, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                if (notificationsCall != null) {
                    try {
                        Response<List<Notification>> notificationsResponse = notificationsCall.execute();
                        if (notificationsResponse.isSuccessful()) {
                            List<Notification> notFiltered = notificationsResponse.body();
                            notifications.notifications = TimelineHelper.filterNotification(getApplication().getApplicationContext(), notFiltered);
                            notifications.pagination = MastodonHelper.getPagination(notificationsResponse.headers());
                            if (notifications.notifications != null && !notifications.notifications.isEmpty()) {
                                addFetchMoreNotifications(notifications.notifications, notificationList, timelineParams);
                                cacheNotifications(notFiltered, timelineParams);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> notificationsMutableLiveData.setValue(notifications);
            mainHandler.post(myRunnable);
        }).start();

        return notificationsMutableLiveData;
    }

    private void cacheNotifications(List<Notification> notificationsToCache, TimelinesVM.TimelineParams timelineParams) {
        if (notificationsToCache != null && !notificationsToCache.isEmpty()) {
            for (Notification notification : notificationsToCache) {
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

    public LiveData<Notifications> getNotificationCache(List<Notification> timelineNotification, TimelinesVM.TimelineParams timelineParams) {
        notificationsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
            Notifications notifications = new Notifications();
            List<Notification> notificationsDb;
            try {
                notificationsDb = statusCacheDAO.getNotifications(timelineParams.excludeType, timelineParams.instance, timelineParams.userId, timelineParams.maxId, timelineParams.minId, timelineParams.sinceId);
                if (notificationsDb != null && notificationsDb.size() > 0) {
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext());
                    boolean aggregateNotification = sharedpreferences.getBoolean(getApplication().getString(R.string.SET_AGGREGATE_NOTIFICATION), true);
                    if (aggregateNotification) {
                        notificationsDb = deduplicateByGroupKey(notificationsDb);
                    }
                    for (Notification notification : notificationsDb) {
                        if (notification.group_key != null) {
                            notifications.groupedByServer = true;
                            break;
                        }
                    }
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

    private List<Notification> deduplicateByGroupKey(List<Notification> notifications) {
        Map<String, Notification> groupKeyMap = new HashMap<>();
        List<Notification> result = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notification.group_key != null) {
                Notification existing = groupKeyMap.get(notification.group_key);
                if (existing == null || Helper.compareTo(notification.id, existing.id) > 0) {
                    groupKeyMap.put(notification.group_key, notification);
                }
            } else {
                result.add(notification);
            }
        }
        result.addAll(groupKeyMap.values());
        sortDesc(result);
        return result;
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
                                                       boolean poll,
                                                       boolean status,
                                                       boolean updates,
                                                       boolean signup,
                                                       boolean report
    ) {
        pushSubscriptionMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            PushSubscription pushSubscription = null;
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.pushSubscription(token, endpoint, keys_p256dh, keys_auth, true, follow, favourite, reblog, mention, poll, status, updates, signup, report, "all");
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
            Call<PushSubscription> pushSubscriptionCall = mastodonNotificationsService.updatePushSubscription(token, follow, favourite, reblog, mention, poll, "all");
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

    /**
     * Get notification policy
     *
     * @param instance String - Instance for the api call
     * @param token    String - Token of the authenticated account
     * @return {@link LiveData} containing a {@link NotificationPolicy}
     */
    public LiveData<NotificationPolicy> getNotificationPolicy(@NonNull String instance, String token) {
        MutableLiveData<NotificationPolicy> notificationPolicyMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = initV2(instance);
        new Thread(() -> {
            NotificationPolicy notificationPolicy = null;
            Call<NotificationPolicy> notificationPolicyCall = mastodonNotificationsService.getNotificationPolicy(token);
            if (notificationPolicyCall != null) {
                try {
                    retrofit2.Response<NotificationPolicy> response = notificationPolicyCall.execute();
                    if (response.isSuccessful()) {
                        notificationPolicy = response.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            NotificationPolicy finalNotificationPolicy = notificationPolicy;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> notificationPolicyMutableLiveData.setValue(finalNotificationPolicy));
        }).start();
        return notificationPolicyMutableLiveData;
    }

    /**
     * Update notification policy
     *
     * @param instance             String - Instance for the api call
     * @param token                String - Token of the authenticated account
     * @param for_not_following    String - accept, filter, or drop
     * @param for_not_followers    String - accept, filter, or drop
     * @param for_new_accounts     String - accept, filter, or drop
     * @param for_private_mentions String - accept, filter, or drop
     * @param for_limited_accounts String - accept, filter, or drop
     * @return {@link LiveData} containing a {@link NotificationPolicy}
     */
    public LiveData<NotificationPolicy> updateNotificationPolicy(@NonNull String instance, String token,
                                                                  String for_not_following,
                                                                  String for_not_followers,
                                                                  String for_new_accounts,
                                                                  String for_private_mentions,
                                                                  String for_limited_accounts) {
        MutableLiveData<NotificationPolicy> notificationPolicyMutableLiveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = initV2(instance);
        new Thread(() -> {
            NotificationPolicy notificationPolicy = null;
            Call<NotificationPolicy> notificationPolicyCall = mastodonNotificationsService.updateNotificationPolicy(
                    token, for_not_following, for_not_followers, for_new_accounts, for_private_mentions, for_limited_accounts);
            if (notificationPolicyCall != null) {
                try {
                    retrofit2.Response<NotificationPolicy> response = notificationPolicyCall.execute();
                    if (response.isSuccessful()) {
                        notificationPolicy = response.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            NotificationPolicy finalNotificationPolicy = notificationPolicy;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> notificationPolicyMutableLiveData.setValue(finalNotificationPolicy));
        }).start();
        return notificationPolicyMutableLiveData;
    }

    /**
     * Get notification requests (filtered notifications)
     */
    public LiveData<NotificationRequests> getNotificationRequests(@NonNull String instance, String token, String max_id) {
        MutableLiveData<NotificationRequests> liveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            NotificationRequests notificationRequests = new NotificationRequests();
            Call<List<NotificationRequest>> call = mastodonNotificationsService.getNotificationRequests(token, max_id, null, null, 40);
            if (call != null) {
                try {
                    Response<List<NotificationRequest>> response = call.execute();
                    if (response.isSuccessful()) {
                        notificationRequests.notificationRequests = response.body();
                        notificationRequests.pagination = MastodonHelper.getPagination(response.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            NotificationRequests finalResult = notificationRequests;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> liveData.setValue(finalResult));
        }).start();
        return liveData;
    }

    /**
     * Accept a notification request
     */
    public LiveData<Boolean> acceptNotificationRequest(@NonNull String instance, String token, String id) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            boolean success = false;
            Call<Void> call = mastodonNotificationsService.acceptNotificationRequest(token, id);
            if (call != null) {
                try {
                    Response<Void> response = call.execute();
                    success = response.isSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            boolean finalSuccess = success;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> liveData.setValue(finalSuccess));
        }).start();
        return liveData;
    }

    /**
     * Dismiss a notification request
     */
    public LiveData<Boolean> dismissNotificationRequest(@NonNull String instance, String token, String id) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        MastodonNotificationsService mastodonNotificationsService = init(instance);
        new Thread(() -> {
            boolean success = false;
            Call<Void> call = mastodonNotificationsService.dismissNotificationRequest(token, id);
            if (call != null) {
                try {
                    Response<Void> response = call.execute();
                    success = response.isSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            boolean finalSuccess = success;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> liveData.setValue(finalSuccess));
        }).start();
        return liveData;
    }

}
