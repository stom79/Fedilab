package app.fedilab.android.ui.fragment.timeline;
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.Notifications;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.NotificationAdapter;
import app.fedilab.android.viewmodel.mastodon.NotificationsVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonNotification extends Fragment implements NotificationAdapter.FetchMoreCallBack {


    private FragmentPaginationBinding binding;
    private NotificationsVM notificationsVM;
    private boolean flagLoading;
    private List<Notification> notificationList;
    private NotificationAdapter notificationAdapter;
    private final BroadcastReceiver receive_action = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                Status receivedStatus = (Status) b.getSerializable(Helper.ARG_STATUS_ACTION);
                if (receivedStatus != null && notificationAdapter != null) {
                    int position = getPosition(receivedStatus);
                    if (position >= 0) {
                        if (notificationList.get(position).status != null) {
                            notificationList.get(position).status.reblog = receivedStatus.reblog;
                            notificationList.get(position).status.reblogged = receivedStatus.reblogged;
                            notificationList.get(position).status.favourited = receivedStatus.favourited;
                            notificationList.get(position).status.bookmarked = receivedStatus.bookmarked;
                            notificationList.get(position).status.favourites_count = receivedStatus.favourites_count;
                            notificationList.get(position).status.reblogs_count = receivedStatus.reblogs_count;
                            notificationAdapter.notifyItemChanged(position);
                        }
                    }
                }
            }
        }
    };
    private String max_id, min_id, min_id_fetch_more, max_id_fetch_more;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<String> idOfAddedNotifications;
    private NotificationTypeEnum notificationType;
    private boolean aggregateNotification;

    /**
     * Return the position of the status in the ArrayList
     *
     * @param status - Status to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Status status) {
        int position = 0;
        boolean found = false;
        for (Notification _notification : notificationList) {
            if (_notification.status != null && _notification.status.id.compareTo(status.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }

    /**
     * Return the position of the notification in the ArrayList
     *
     * @param notification - Notification to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Notification notification) {
        int position = 0;
        boolean found = false;
        for (Notification _notification : notificationList) {
            if (_notification.status != null && _notification.id.compareTo(notification.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        flagLoading = false;
        idOfAddedNotifications = new ArrayList<>();
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        if (getArguments() != null) {
            notificationType = (NotificationTypeEnum) getArguments().get(Helper.ARG_NOTIFICATION_TYPE);
        }
        aggregateNotification = false;
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        String excludedCategories = sharedpreferences.getString(getString(R.string.SET_EXCLUDED_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );
        notificationsVM = new ViewModelProvider(FragmentMastodonNotification.this).get(NotificationsVM.class);
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        max_id = null;
        route(null, false);
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION));
        return root;
    }

    List<String> getExcludeType() {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        String excludedCategories = sharedpreferences.getString(getString(R.string.SET_EXCLUDED_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
        List<String> excludeType = new ArrayList<>();
        excludeType.add("follow");
        excludeType.add("favourite");
        excludeType.add("reblog");
        excludeType.add("poll");
        excludeType.add("follow_request");
        excludeType.add("mention");
        excludeType.add("update");
        excludeType.add("status");
        if (notificationType == NotificationTypeEnum.ALL) {
            aggregateNotification = sharedpreferences.getBoolean(getString(R.string.SET_AGGREGATE_NOTIFICATION), true);
            if (excludedCategories != null) {
                excludeType = new ArrayList<>();
                String[] categoriesArray = excludedCategories.split("\\|");
                Collections.addAll(excludeType, categoriesArray);
            } else {
                excludeType = null;
            }
        } else if (notificationType == NotificationTypeEnum.MENTIONS) {
            excludeType.remove("mention");
        } else if (notificationType == NotificationTypeEnum.FAVOURITES) {
            excludeType.remove("favourite");
        } else if (notificationType == NotificationTypeEnum.REBLOGS) {
            excludeType.remove("reblog");
        } else if (notificationType == NotificationTypeEnum.POLLS) {
            excludeType.remove("poll");
        } else if (notificationType == NotificationTypeEnum.TOOTS) {
            excludeType.remove("status");
        } else if (notificationType == NotificationTypeEnum.FOLLOWS) {
            excludeType.remove("follow");
            excludeType.remove("follow_request");
        }
        return excludeType;
    }
    /**
     * Intialize the view for notifications
     *
     * @param notifications {@link Notifications}
     */
    private void initializeNotificationView(final Notifications notifications) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            route(FragmentMastodonTimeline.DIRECTION.REFRESH, true);
        });
        if (notifications == null || notifications.notifications == null || notifications.notifications.size() == 0) {
            binding.noActionText.setText(R.string.no_notifications);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            return;
        } else {
            binding.noAction.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
        for (Notification notification : notifications.notifications) {
            idOfAddedNotifications.add(notification.id);
        }
        flagLoading = notifications.pagination.max_id == null;
        if (aggregateNotification) {
            notifications.notifications = aggregateNotifications(notifications.notifications);
        }
        if (notificationAdapter != null && notificationList != null) {
            int size = notificationList.size();
            notificationList.clear();
            notificationList = new ArrayList<>();
            notificationAdapter.notifyItemRangeRemoved(0, size);
        }
        if (notificationList == null) {
            notificationList = new ArrayList<>();
        }
        notificationList.addAll(notifications.notifications);

        if (max_id == null || (notifications.pagination.max_id != null && notifications.pagination.max_id.compareTo(max_id) < 0)) {
            max_id = notifications.pagination.max_id;
        }
        if (min_id == null || (notifications.pagination.min_id != null && notifications.pagination.min_id.compareTo(min_id) > 0)) {
            min_id = notifications.pagination.min_id;
        }

        notificationAdapter = new NotificationAdapter(notificationList);
        notificationAdapter.fetchMoreCallBack = this;
        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(notificationAdapter);

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (requireActivity() instanceof BaseMainActivity) {
                    if (dy < 0 && !((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(true);
                    if (dy > 0 && ((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(false);
                }
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();

                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            route(FragmentMastodonTimeline.DIRECTION.BOTTOM, false);
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                } else if (firstVisibleItem == 0) { //Scroll top and item is zero
                    if (!flagLoading) {
                        flagLoading = true;
                        binding.loadingNextElements.setVisibility(View.VISIBLE);
                        route(FragmentMastodonTimeline.DIRECTION.TOP, false);
                    }
                }
            }
        });
    }


    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing) {
        route(direction, fetchingMissing, null);
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, Notification notificationToUpdate) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (!isAdded()) {
            return;
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean useCache = sharedpreferences.getBoolean(getString(R.string.SET_USE_CACHE), true);

        TimelinesVM.TimelineParams timelineParams = new TimelinesVM.TimelineParams(Timeline.TimeLineEnum.NOTIFICATION, direction, null);
        timelineParams.limit = MastodonHelper.notificationsPerCall(requireActivity());
        if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH || direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP) {
            timelineParams.maxId = null;
            timelineParams.minId = null;
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            timelineParams.maxId = fetchingMissing ? max_id_fetch_more : max_id;
            timelineParams.minId = null;
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            timelineParams.minId = fetchingMissing ? min_id_fetch_more : min_id;
            timelineParams.maxId = null;
        } else {
            timelineParams.maxId = max_id;
        }
        timelineParams.excludeType = getExcludeType();

        timelineParams.fetchingMissing = fetchingMissing;

        if (useCache) {
            getCachedNotifications(direction, fetchingMissing, timelineParams);
        } else {
            getLiveNotifications(direction, fetchingMissing, timelineParams, notificationToUpdate);
        }


    }

    private void getCachedNotifications(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams) {

        if (direction == null) {
            notificationsVM.getNotificationCache(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsCached -> {
                        if (notificationsCached == null || notificationsCached.notifications == null || notificationsCached.notifications.size() == 0) {
                            getLiveNotifications(null, fetchingMissing, timelineParams, null);
                        } else {
                            initializeNotificationView(notificationsCached);
                        }
                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            notificationsVM.getNotificationCache(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsBottom -> {
                        if (notificationsBottom == null || notificationsBottom.notifications == null || notificationsBottom.notifications.size() == 0) {
                            getLiveNotifications(FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(notificationsBottom, FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, null);
                        }

                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            notificationsVM.getNotificationCache(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsTop -> {
                        if (notificationsTop == null || notificationsTop.notifications == null || notificationsTop.notifications.size() == 0) {
                            getLiveNotifications(FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(notificationsTop, FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, null);
                        }
                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH) {
            notificationsVM.getNotifications(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsRefresh -> {
                        if (notificationAdapter != null) {
                            dealWithPagination(notificationsRefresh, FragmentMastodonTimeline.DIRECTION.REFRESH, true, null);
                        } else {
                            initializeNotificationView(notificationsRefresh);
                        }
                    });
        }
    }

    private void getLiveNotifications(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams, Notification notificationToUpdate) {
        if (direction == null) {
            notificationsVM.getNotifications(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), this::initializeNotificationView);
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            notificationsVM.getNotifications(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsBottom -> dealWithPagination(notificationsBottom, FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, notificationToUpdate));
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            notificationsVM.getNotifications(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsTop -> dealWithPagination(notificationsTop, FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, notificationToUpdate));
        } else if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH) {
            notificationsVM.getNotifications(notificationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsRefresh -> {
                        if (notificationAdapter != null) {
                            dealWithPagination(notificationsRefresh, FragmentMastodonTimeline.DIRECTION.REFRESH, true, notificationToUpdate);
                        } else {
                            initializeNotificationView(notificationsRefresh);
                        }
                    });
        }
    }

    private List<Notification> aggregateNotifications(List<Notification> notifications) {
        List<Notification> notificationList = new ArrayList<>();
        int refPosition = 0;
        for (int i = 0; i < notifications.size(); i++) {
            if (i != refPosition) {
                if (notifications.get(i).type.equals(notifications.get(refPosition).type)
                        && (notifications.get(i).type.equals("favourite") || notifications.get(i).type.equals("reblog"))
                        && notifications.get(i).status != null && notifications.get(refPosition).status != null && notifications.get(i).status.id.equals(notifications.get(refPosition).status.id)
                ) {
                    if (notificationList.size() > 0) {
                        if (notificationList.get(notificationList.size() - 1).relatedNotifications == null) {
                            notificationList.get(notificationList.size() - 1).relatedNotifications = new ArrayList<>();
                        }
                        notificationList.get(notificationList.size() - 1).relatedNotifications.add(notifications.get(i));
                    }
                } else {
                    notificationList.add(notifications.get(i));
                    refPosition = i;
                }
            } else {
                notificationList.add(notifications.get(i));
            }
        }
        return notificationList;
    }

    public void scrollToTop() {
        binding.recyclerView.scrollToPosition(0);
    }



    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_notifications Notifications
     */
    private synchronized void dealWithPagination(Notifications fetched_notifications, FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, Notification notificationToUpdate) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (notificationList != null && fetched_notifications != null && fetched_notifications.notifications != null && fetched_notifications.notifications.size() > 0) {
            try {
                if (notificationToUpdate != null) {
                    new Thread(() -> {
                        StatusCache statusCache = new StatusCache();
                        statusCache.instance = BaseMainActivity.currentInstance;
                        statusCache.user_id = BaseMainActivity.currentUserID;
                        statusCache.notification = notificationToUpdate;
                        statusCache.status_id = notificationToUpdate.id;
                        try {
                            new StatusCache(requireActivity()).updateIfExists(statusCache);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception ignored) {
            }

            flagLoading = fetched_notifications.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);
            //Update the timeline with new statuses
            updateNotificationListWith(fetched_notifications.notifications);
            if (direction == FragmentMastodonTimeline.DIRECTION.TOP && fetchingMissing) {
                binding.recyclerView.scrollToPosition(getPosition(fetched_notifications.notifications.get(fetched_notifications.notifications.size() - 1)) + 1);
            }
            if (!fetchingMissing) {
                if (fetched_notifications.pagination.max_id == null) {
                    flagLoading = true;
                } else if (max_id == null || fetched_notifications.pagination.max_id.compareTo(max_id) < 0) {
                    max_id = fetched_notifications.pagination.max_id;
                }
                if (min_id == null || (fetched_notifications.pagination.min_id != null && fetched_notifications.pagination.min_id.compareTo(min_id) > 0)) {
                    min_id = fetched_notifications.pagination.min_id;
                }
            }
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            flagLoading = true;
        }
        if (direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP) {
            binding.recyclerView.scrollToPosition(0);
        }
    }

    /**
     * Update the timeline with received statuses
     *
     * @param notificationsReceived - List<Notification> Notifications received
     */
    private void updateNotificationListWith(List<Notification> notificationsReceived) {
        if (notificationsReceived != null && notificationsReceived.size() > 0) {
            for (Notification notificationReceived : notificationsReceived) {
                int position = 0;
                //We loop through messages already in the timeline
                if (notificationList != null) {
                    notificationAdapter.notifyItemRangeChanged(0, notificationList.size());
                    for (Notification notificationsAlreadyPresent : notificationList) {
                        //We compare the date of each status and we only add status having a date greater than the another, it is inserted at this position
                        //Pinned messages are ignored because their date can be older
                        if (notificationReceived.id.compareTo(notificationsAlreadyPresent.id) > 0) {
                            notificationList.add(position, notificationReceived);
                            notificationAdapter.notifyItemInserted(position);
                            break;
                        }
                        position++;
                    }
                    //Statuses added at the bottom, we flag them by position = -2 for not dealing with them and fetch more
                    if (position == notificationList.size() && !notificationList.contains(notificationReceived)) {
                        notificationList.add(position, notificationReceived);
                        notificationAdapter.notifyItemInserted(position);
                    }
                }
            }
        }
    }



    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receive_action);
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onClickMinId(String min_id, Notification notificationToUpdate) {
        //Fetch more has been pressed
        min_id_fetch_more = min_id;
        route(FragmentMastodonTimeline.DIRECTION.TOP, true, notificationToUpdate);
    }

    @Override
    public void onClickMaxId(String max_id, Notification notificationToUpdate) {
        //Fetch more has been pressed
        max_id_fetch_more = max_id;
        route(FragmentMastodonTimeline.DIRECTION.BOTTOM, true, notificationToUpdate);
    }

    public enum NotificationTypeEnum {
        @SerializedName("ALL")
        ALL("ALL"),
        @SerializedName("MENTIONS")
        MENTIONS("MENTIONS"),
        @SerializedName("FAVOURITES")
        FAVOURITES("FAVOURITES"),
        @SerializedName("REBLOGS")
        REBLOGS("REBLOGS"),
        @SerializedName("POLLS")
        POLLS("POLLS"),
        @SerializedName("TOOTS")
        TOOTS("TOOTS"),
        @SerializedName("FOLLOWS")
        FOLLOWS("FOLLOWS");

        private final String value;

        NotificationTypeEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}