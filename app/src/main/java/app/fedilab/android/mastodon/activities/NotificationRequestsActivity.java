package app.fedilab.android.mastodon.activities;
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


import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityStatusInfoBinding;
import app.fedilab.android.mastodon.client.entities.api.NotificationRequest;
import app.fedilab.android.mastodon.client.entities.api.NotificationRequests;
import app.fedilab.android.mastodon.ui.drawer.NotificationRequestAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.NotificationsVM;


public class NotificationRequestsActivity extends BaseActivity {

    private ActivityStatusInfoBinding binding;
    private List<NotificationRequest> requestList;
    private NotificationRequestAdapter adapter;
    private String max_id;
    private boolean flagLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStatusInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        requestList = new ArrayList<>();
        flagLoading = false;
        max_id = null;
        binding.title.setText(R.string.filtered_notifications);
        NotificationsVM notificationsVM = new ViewModelProvider(NotificationRequestsActivity.this).get(NotificationsVM.class);
        adapter = new NotificationRequestAdapter(requestList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(NotificationRequestsActivity.this);
        binding.lvAccounts.setLayoutManager(mLayoutManager);
        binding.lvAccounts.setAdapter(adapter);

        binding.lvAccounts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextAccounts.setVisibility(View.VISIBLE);
                            notificationsVM.getNotificationRequests(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id)
                                    .observe(NotificationRequestsActivity.this, result -> manageView(result));
                        }
                    } else {
                        binding.loadingNextAccounts.setVisibility(View.GONE);
                    }
                }
            }
        });
        notificationsVM.getNotificationRequests(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                .observe(NotificationRequestsActivity.this, this::manageView);
    }

    private void manageView(NotificationRequests result) {
        binding.loadingNextAccounts.setVisibility(View.GONE);
        if (requestList != null && result != null && result.notificationRequests != null && result.notificationRequests.size() > 0) {
            int startId = requestList.size();
            flagLoading = result.pagination.max_id == null;
            requestList.addAll(result.notificationRequests);
            max_id = result.pagination.max_id;
            adapter.notifyItemRangeInserted(startId, result.notificationRequests.size());
            binding.noAction.setVisibility(View.GONE);
            binding.lvAccounts.setVisibility(View.VISIBLE);
        } else if (requestList == null || requestList.size() == 0) {
            binding.noActionText.setText(R.string.no_notification_requests);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.lvAccounts.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }
}
