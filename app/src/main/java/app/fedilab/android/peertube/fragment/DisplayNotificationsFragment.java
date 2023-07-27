package app.fedilab.android.peertube.fragment;
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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentRecyclerviewPeertubeBinding;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.data.NotificationData.Notification;
import app.fedilab.android.peertube.drawer.PeertubeNotificationsListAdapter;
import app.fedilab.android.peertube.viewmodel.NotificationsVM;
import es.dmoral.toasty.Toasty;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class DisplayNotificationsFragment extends Fragment {

    //Peertube notification type
    public final static int NEW_VIDEO_FROM_SUBSCRIPTION = 1;
    public final static int NEW_COMMENT_ON_MY_VIDEO = 2;
    public final static int NEW_ABUSE_FOR_MODERATORS = 3;
    public final static int BLACKLIST_ON_MY_VIDEO = 4;
    public final static int UNBLACKLIST_ON_MY_VIDEO = 5;
    public final static int MY_VIDEO_PUBLISHED = 6;
    public final static int MY_VIDEO_IMPORT_SUCCESS = 7;
    public final static int MY_VIDEO_IMPORT_ERROR = 8;
    public final static int NEW_USER_REGISTRATION = 9;
    public final static int NEW_FOLLOW = 10;
    public final static int COMMENT_MENTION = 11;
    public final static int VIDEO_AUTO_BLACKLIST_FOR_MODERATORS = 12;
    public final static int NEW_INSTANCE_FOLLOWER = 13;
    public final static int AUTO_INSTANCE_FOLLOWING = 14;
    public final static int MY_VIDEO_REPPORT_SUCCESS = 15;
    public final static int ABUSE_NEW_MESSAGE = 16;
    FragmentRecyclerviewPeertubeBinding binding;
    private boolean flag_loading;
    private Context context;
    private PeertubeNotificationsListAdapter peertubeNotificationsListAdapter;
    private String max_id;
    private List<Notification> notifications;
    private boolean firstLoad;
    private NotificationsVM viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentRecyclerviewPeertubeBinding.inflate(getLayoutInflater());
        context = getContext();
        notifications = new ArrayList<>();
        max_id = "0";
        firstLoad = true;
        flag_loading = true;


        viewModel = new ViewModelProvider(this).get(NotificationsVM.class);


        binding.lvElements.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        binding.noActionText.setText(context.getString(R.string.no_notifications));
        binding.loader.setVisibility(View.VISIBLE);
        binding.loadingNext.setVisibility(View.GONE);
        peertubeNotificationsListAdapter = new PeertubeNotificationsListAdapter(this.notifications);
        binding.lvElements.setAdapter(peertubeNotificationsListAdapter);

        final LinearLayoutManager mLayoutManager;
        mLayoutManager = new LinearLayoutManager(context);
        binding.lvElements.setLayoutManager(mLayoutManager);
        binding.lvElements.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flag_loading) {
                            flag_loading = true;
                            viewModel.getNotifications(null, max_id).observe(DisplayNotificationsFragment.this.requireActivity(), apiResponse -> manageVIewNotifications(apiResponse));
                            binding.loadingNext.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.loadingNext.setVisibility(View.GONE);
                    }
                }
            }
        });
        binding.swipeContainer.setOnRefreshListener(this::pullToRefresh);

        viewModel.getNotifications(null, "0").observe(DisplayNotificationsFragment.this.requireActivity(), this::manageVIewNotifications);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onPause() {
        super.onPause();
        binding.swipeContainer.setEnabled(false);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.clearAnimation();
    }


    @Override
    public void onResume() {
        super.onResume();
        binding.swipeContainer.setEnabled(true);
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void scrollToTop() {
        binding.lvElements.setAdapter(peertubeNotificationsListAdapter);
    }


    public void pullToRefresh() {
        int size = notifications.size();
        notifications.clear();
        notifications = new ArrayList<>();
        max_id = "0";
        peertubeNotificationsListAdapter.notifyItemRangeRemoved(0, size);
        firstLoad = true;
        flag_loading = true;
        binding.swipeContainer.setRefreshing(true);
        viewModel.getNotifications(null, "0").observe(DisplayNotificationsFragment.this.requireActivity(), this::manageVIewNotifications);
    }

    private void manageVIewNotifications(APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        binding.loadingNext.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            flag_loading = false;
            binding.swipeContainer.setRefreshing(false);
            return;
        }

        int previousPosition = notifications.size();
        max_id = String.valueOf(Integer.parseInt(max_id) + 20);
        List<Notification> notifications = apiResponse.getPeertubeNotifications();
        if (firstLoad && (notifications == null || notifications.size() == 0))
            binding.noAction.setVisibility(View.VISIBLE);
        else
            binding.noAction.setVisibility(View.GONE);


        if (notifications != null && notifications.size() > 0) {
            this.notifications.addAll(notifications);
            if (previousPosition == 0) {
                peertubeNotificationsListAdapter = new PeertubeNotificationsListAdapter(this.notifications);
                binding.lvElements.setAdapter(peertubeNotificationsListAdapter);
            } else
                peertubeNotificationsListAdapter.notifyItemRangeInserted(previousPosition, notifications.size());
        } else {
            if (firstLoad)
                binding.noAction.setVisibility(View.VISIBLE);
        }
        binding.swipeContainer.setRefreshing(false);
        firstLoad = false;
        //The initial call comes from a classic tab refresh
        flag_loading = (max_id == null);
    }
}
