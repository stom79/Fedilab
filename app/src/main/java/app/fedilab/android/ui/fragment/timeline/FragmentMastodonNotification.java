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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.Notifications;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.QuickLoad;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.NotificationAdapter;
import app.fedilab.android.viewmodel.mastodon.NotificationsVM;


public class FragmentMastodonNotification extends Fragment implements NotificationAdapter.FetchMoreCallBack {


    private static final int NOTIFICATION_PRESENT = -1;
    private static final int NOTIFICATION__AT_THE_BOTTOM = -2;
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
                            notificationList.get(position).status.favourited = receivedStatus.favourited;
                            notificationList.get(position).status.bookmarked = receivedStatus.bookmarked;
                            notificationAdapter.notifyItemChanged(position);
                        }
                    }
                }
            }
        }
    };
    private String max_id, min_id, min_id_fetch_more;
    private LinearLayoutManager mLayoutManager;
    private String instance, user_id;
    private ArrayList<String> idOfAddedNotifications;
    private NotificationTypeEnum notificationType;
    private List<String> excludeType;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        flagLoading = false;
        instance = MainActivity.currentInstance;
        user_id = MainActivity.currentUserID;
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
        excludeType = new ArrayList<>();
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
        route(null, false);
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION));
        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        NotificationManager notificationManager = (NotificationManager) requireActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Helper.NOTIFICATION_USER_NOTIF);
    }


    /**
     * Intialize the view for notifications
     *
     * @param notifications {@link Notifications}
     */
    private void initializeNotificationView(final Notifications notifications) {
        flagLoading = false;
        if (binding == null) {
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
        if (notificationAdapter != null && this.notificationList != null) {
            int size = this.notificationList.size();
            this.notificationList.clear();
            this.notificationList = new ArrayList<>();
            notificationAdapter.notifyItemRangeRemoved(0, size);
        }
        if (this.notificationList == null) {
            this.notificationList = new ArrayList<>();
        }
        this.notificationList.addAll(notifications.notifications);

        if (max_id == null || (notifications.pagination.max_id != null && notifications.pagination.max_id.compareTo(max_id) < 0)) {
            max_id = notifications.pagination.max_id;
        }
        if (min_id == null || (notifications.pagination.min_id != null && notifications.pagination.min_id.compareTo(min_id) > 0)) {
            min_id = notifications.pagination.min_id;
        }

        notificationAdapter = new NotificationAdapter(this.notificationList);
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
        new Thread(() -> {
            if (binding == null) {
                return;
            }
            QuickLoad quickLoad = new QuickLoad(requireActivity()).getSavedValue(MainActivity.currentUserID, MainActivity.currentInstance, notificationType);
            if (direction != FragmentMastodonTimeline.DIRECTION.REFRESH && !fetchingMissing && !binding.swipeContainer.isRefreshing() && direction == null && quickLoad != null && quickLoad.notifications != null && quickLoad.notifications.size() > 0) {
                Notifications notifications = new Notifications();
                notifications.notifications = quickLoad.notifications;
                notifications.pagination = new Pagination();
                notifications.pagination.max_id = quickLoad.notifications.get(quickLoad.statuses.size() - 1).id;
                notifications.pagination.min_id = quickLoad.notifications.get(0).id;
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> initializeNotificationView(notifications);
                mainHandler.post(myRunnable);
            } else {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (direction == null) {
                        notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                                .observe(getViewLifecycleOwner(), this::initializeNotificationView);
                    } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
                        notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                                .observe(getViewLifecycleOwner(), notificationsBottom -> dealWithPagination(notificationsBottom, FragmentMastodonTimeline.DIRECTION.BOTTOM, false));
                    } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
                        notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                                .observe(getViewLifecycleOwner(), notificationsTop -> dealWithPagination(notificationsTop, FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing));
                    } else if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH) {
                        notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                                .observe(getViewLifecycleOwner(), notificationsRefresh -> {
                                    if (notificationAdapter != null) {
                                        dealWithPagination(notificationsRefresh, FragmentMastodonTimeline.DIRECTION.REFRESH, true);
                                    } else {
                                        initializeNotificationView(notificationsRefresh);
                                    }
                                });
                    }
                };
                mainHandler.post(myRunnable);
            }
        }).start();
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
    private synchronized void dealWithPagination(Notifications fetched_notifications, FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing) {
        if (binding == null) {
            return;
        }
        int currentPosition = mLayoutManager.findFirstVisibleItemPosition();
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (notificationList != null && fetched_notifications != null && fetched_notifications.notifications != null && fetched_notifications.notifications.size() > 0) {
            flagLoading = fetched_notifications.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);
            //Update the timeline with new statuses
            int inserted = updateNotificationListWith(direction, fetched_notifications.notifications, fetchingMissing);
            if (fetchingMissing) {
                //  binding.recyclerView.scrollToPosition(currentPosition + inserted);
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
    }

    /**
     * Update the timeline with received statuses
     *
     * @param notificationsReceived - List<Notification> Notifications received
     * @param fetchingMissing       - boolean if the call concerns fetching messages (ie: refresh of from fetch more button)
     * @return int - Number of messages that have been inserted in the middle of the timeline (ie between other statuses)
     */
    private int updateNotificationListWith(FragmentMastodonTimeline.DIRECTION direction, List<Notification> notificationsReceived, boolean fetchingMissing) {
        int numberInserted = 0;
        int lastInsertedPosition = 0;
        int initialInsertedPosition = NOTIFICATION_PRESENT;
        if (notificationsReceived != null && notificationsReceived.size() > 0) {
            int insertedPosition = NOTIFICATION_PRESENT;
            for (Notification notificationReceived : notificationsReceived) {
                insertedPosition = insertNotification(notificationReceived);
                if (insertedPosition != NOTIFICATION_PRESENT && insertedPosition != NOTIFICATION__AT_THE_BOTTOM) {
                    numberInserted++;
                    if (initialInsertedPosition == NOTIFICATION_PRESENT) {
                        initialInsertedPosition = insertedPosition;
                    }
                    if (insertedPosition < initialInsertedPosition) {
                        initialInsertedPosition = lastInsertedPosition;
                    }
                }
            }
            lastInsertedPosition = initialInsertedPosition + numberInserted;
            //lastInsertedPosition contains the position of the last inserted status
            //If there were no overlap for top status
            if (fetchingMissing && insertedPosition != NOTIFICATION_PRESENT && insertedPosition != NOTIFICATION__AT_THE_BOTTOM && this.notificationList.size() > insertedPosition) {
                Notification notificationFetchMore = new Notification();
                notificationFetchMore.isFetchMore = true;
                notificationFetchMore.id = Helper.generateString();
                int insertAt;
                if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH) {
                    insertAt = lastInsertedPosition;
                } else {
                    insertAt = initialInsertedPosition;
                }

                this.notificationList.add(insertAt, notificationFetchMore);
                notificationAdapter.notifyItemInserted(insertAt);
            }
        }
        return numberInserted;
    }

    /**
     * Insert a status if not yet in the timeline
     *
     * @param notificationReceived - Notification coming from the api/db
     * @return int >= 0 |  STATUS_PRESENT = -1 | STATUS_AT_THE_BOTTOM = -2
     */
    private int insertNotification(Notification notificationReceived) {
        if (idOfAddedNotifications.contains(notificationReceived.id)) {
            return NOTIFICATION_PRESENT;
        }
        int position = 0;
        //We loop through messages already in the timeline
        for (Notification notificationsAlreadyPresent : this.notificationList) {
            //We compare the date of each status and we only add status having a date greater than the another, it is inserted at this position
            //Pinned messages are ignored because their date can be older
            if (notificationReceived.id.compareTo(notificationsAlreadyPresent.id) > 0) {
                //We add the status to a list of id - thus we know it is already in the timeline
                idOfAddedNotifications.add(notificationReceived.id);
                this.notificationList.add(position, notificationReceived);
                notificationAdapter.notifyItemInserted(position);
                break;
            }
            position++;
        }
        //Statuses added at the bottom, we flag them by position = -2 for not dealing with them and fetch more
        if (position == this.notificationList.size()) {
            //We add the status to a list of id - thus we know it is already in the timeline
            idOfAddedNotifications.add(notificationReceived.id);
            this.notificationList.add(position, notificationReceived);
            notificationAdapter.notifyItemInserted(position);
            return NOTIFICATION__AT_THE_BOTTOM;
        }
        return position;
    }


    @Override
    public void onDestroyView() {
        if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            new Thread(() -> {
                try {
                    new QuickLoad(requireActivity()).storeNotifications(position, user_id, instance, notificationType, notificationList);
                } catch (Exception ignored) {
                }
            }).start();
        }
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receive_action);
    }

    @Override
    public void onPause() {
        if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            new Thread(() -> {
                try {
                    new QuickLoad(requireActivity()).storeNotifications(position, user_id, instance, notificationType, notificationList);
                } catch (Exception ignored) {
                }
            }).start();
        }
        super.onPause();
    }

    @Override
    public void onClick(String min_id, String id) {
        //Fetch more has been pressed
        min_id_fetch_more = min_id;
        Notification notification = null;
        int position = 0;
        for (Notification currentNotification : this.notificationList) {
            if (currentNotification.id.compareTo(id) == 0) {
                notification = currentNotification;
                break;
            }
            position++;
        }
        if (notification != null) {
            this.notificationList.remove(position);
            notificationAdapter.notifyItemRemoved(position);
        }
        route(FragmentMastodonTimeline.DIRECTION.TOP, true);
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