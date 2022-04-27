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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Notifications;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.ui.drawer.NotificationAdapter;
import app.fedilab.android.viewmodel.mastodon.NotificationsVM;


public class FragmentMastodonNotification extends Fragment {


    private FragmentPaginationBinding binding;
    private NotificationsVM notificationsVM;
    private FragmentMastodonNotification currentFragment;
    private boolean flagLoading;
    private List<Notification> notifications;
    private String max_id;
    private NotificationAdapter notificationAdapter;
    private NotificationTypeEnum notificationType;
    private List<String> excludeType;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        currentFragment = this;
        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        if (getArguments() != null) {
            notificationType = (NotificationTypeEnum) getArguments().get(Helper.ARG_NOTIFICATION_TYPE);
        }
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
        notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                .observe(getViewLifecycleOwner(), this::initializeNotificationView);
        return root;
    }

    /**
     * Intialize the view for notifications
     *
     * @param notifications {@link app.fedilab.android.client.mastodon.entities.Notifications}
     */
    private void initializeNotificationView(final Notifications notifications) {

        binding.loader.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        if (notifications == null || notifications.notifications == null) {
            binding.noActionText.setText(R.string.no_notifications);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            return;
        } else {
            binding.noAction.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
        if (notificationAdapter != null && this.notifications != null) {
            int size = this.notifications.size();
            this.notifications.clear();
            this.notifications = new ArrayList<>();
            notificationAdapter.notifyItemRangeRemoved(0, size);
        }
        this.notifications = notifications.notifications;
        notificationAdapter = new NotificationAdapter(this.notifications);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(notificationAdapter);
        max_id = notifications.pagination.min_id;
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                                    .observe(FragmentMastodonNotification.this, fetched_notifications -> dealWithPagination(fetched_notifications));
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }

            }
        });
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (this.notifications.size() > 0) {
                binding.swipeContainer.setRefreshing(true);
                max_id = null;
                notificationsVM.getNotifications(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()), excludeType, null)
                        .observe(FragmentMastodonNotification.this, this::initializeNotificationView);
            }
        });

    }

    public void scrollToTop() {
        binding.recyclerView.scrollToPosition(0);
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_notifications Notifications
     */
    private void dealWithPagination(Notifications fetched_notifications) {
        flagLoading = false;
        binding.loadingNextElements.setVisibility(View.GONE);
        if (currentFragment.notifications != null && fetched_notifications != null && fetched_notifications.notifications != null) {
            int startId = 0;
            //There are some statuses present in the timeline
            if (currentFragment.notifications.size() > 0) {
                startId = currentFragment.notifications.size();
            }
            currentFragment.notifications.addAll(fetched_notifications.notifications);
            max_id = fetched_notifications.pagination.min_id;
            notificationAdapter.notifyItemRangeInserted(startId, fetched_notifications.notifications.size());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        notificationAdapter = null;
        binding = null;
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