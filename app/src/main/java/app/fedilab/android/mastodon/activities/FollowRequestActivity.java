package app.fedilab.android.mastodon.activities;
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
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Accounts;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.AccountFollowRequestAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;


public class FollowRequestActivity extends BaseActivity {

    private ActivityStatusInfoBinding binding;
    private List<Account> accountList;
    private AccountFollowRequestAdapter accountAdapter;
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
        accountList = new ArrayList<>();
        flagLoading = false;
        max_id = null;
        binding.title.setText(R.string.follow_request);
        AccountsVM accountsVM = new ViewModelProvider(FollowRequestActivity.this).get(AccountsVM.class);
        accountAdapter = new AccountFollowRequestAdapter(accountList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(FollowRequestActivity.this);
        binding.lvAccounts.setLayoutManager(mLayoutManager);
        binding.lvAccounts.setAdapter(accountAdapter);

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
                            accountsVM.getFollowRequests(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, MastodonHelper.accountsPerCall(FollowRequestActivity.this))
                                    .observe(FollowRequestActivity.this, accounts -> manageView(accounts));
                        }
                    } else {
                        binding.loadingNextAccounts.setVisibility(View.GONE);
                    }
                }
            }
        });
        accountsVM.getFollowRequests(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, MastodonHelper.accountsPerCall(FollowRequestActivity.this))
                .observe(FollowRequestActivity.this, this::manageView);
    }

    private void manageView(Accounts accounts) {

        binding.loadingNextAccounts.setVisibility(View.GONE);
        if (accountList != null && accounts != null && accounts.accounts != null && accounts.accounts.size() > 0) {
            int startId = 0;
            //There are some statuses present in the timeline
            if (accountList.size() > 0) {
                startId = accountList.size();
            }
            flagLoading = accounts.pagination.max_id == null;
            accountList.addAll(accounts.accounts);
            max_id = accounts.pagination.max_id;
            accountAdapter.notifyItemRangeInserted(startId, accounts.accounts.size());
            binding.noAction.setVisibility(View.GONE);
            binding.lvAccounts.setVisibility(View.VISIBLE);
        } else if (accountList == null || accountList.size() == 0) {
            binding.noActionText.setText(R.string.no_follow_request);
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
