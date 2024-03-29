package app.fedilab.android.mastodon.activities;
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


import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentToken;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityStatusInfoBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Accounts;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.AccountAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;


public class StatusInfoActivity extends BaseActivity {

    private ActivityStatusInfoBinding binding;
    private List<Account> accountList;
    private AccountAdapter accountAdapter;
    private String max_id;
    private typeOfInfo type;
    private boolean flagLoading;
    private Status status;

    private boolean checkRemotely;
    private String instance, token;

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
        checkRemotely = false;
        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(StatusInfoActivity.this).getBundle(bundleId, Helper.getCurrentAccount(StatusInfoActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {

        if (bundle != null) {
            type = (typeOfInfo) bundle.getSerializable(Helper.ARG_TYPE_OF_INFO);
            status = (Status) bundle.getSerializable(Helper.ARG_STATUS);
            checkRemotely = bundle.getBoolean(Helper.ARG_CHECK_REMOTELY, false);
        }

        if (type == null || status == null) {
            finish();
            return;
        }

        token = currentToken;
        instance = currentInstance;
        if (checkRemotely) {
            try {
                URL url = new URL(status.uri);
                instance = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            token = null;
            if (instance != null && instance.equalsIgnoreCase(currentInstance)) {
                checkRemotely = false;
                instance = currentInstance;
                token = currentToken;
            }
        }

        flagLoading = false;
        max_id = null;
        setTitle("");
        binding.title.setText(type == typeOfInfo.BOOSTED_BY ? R.string.boosted_by : R.string.favourited_by);
        StatusesVM statusesVM = new ViewModelProvider(StatusInfoActivity.this).get(StatusesVM.class);
        accountAdapter = new AccountAdapter(accountList, false, checkRemotely ? instance : null);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(StatusInfoActivity.this);
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
                            if (type == typeOfInfo.BOOSTED_BY) {
                                statusesVM.rebloggedBy(instance, token, status.id, max_id, null, null).observe(StatusInfoActivity.this, accounts -> manageView(accounts));
                            } else if (type == typeOfInfo.LIKED_BY) {
                                statusesVM.favouritedBy(instance, token, status.id, max_id, null, null).observe(StatusInfoActivity.this, accounts -> manageView(accounts));
                            }
                        }
                    } else {
                        binding.loadingNextAccounts.setVisibility(View.GONE);
                    }
                }
            }
        });
        if (type == typeOfInfo.BOOSTED_BY) {
            statusesVM.rebloggedBy(instance, token, status.id, null, null, null).observe(StatusInfoActivity.this, this::manageView);
        } else if (type == typeOfInfo.LIKED_BY) {
            statusesVM.favouritedBy(instance, token, status.id, null, null, null).observe(StatusInfoActivity.this, this::manageView);
        }
    }


    private void manageView(Accounts accounts) {
        binding.loadingNextAccounts.setVisibility(View.GONE);
        if (accountList != null && accounts != null && accounts.accounts != null) {
            int position = this.accountList.size();
            if (!checkRemotely) {
                fetchRelationShip(accounts.accounts, position);
            }
            //There are some statuses present in the timeline
            int startId = accountList.size();
            accountList.addAll(accounts.accounts);
            max_id = accounts.pagination.max_id;
            flagLoading = accounts.pagination.max_id == null;
            accountAdapter.notifyItemRangeInserted(startId, accounts.accounts.size());
        }
    }

    private void fetchRelationShip(List<Account> accounts, int position) {
        List<String> ids = new ArrayList<>();
        for (Account account : accounts) {
            ids.add(account.id);
        }
        AccountsVM accountsVM = new ViewModelProvider(StatusInfoActivity.this).get(AccountsVM.class);
        accountsVM.getRelationships(instance, token, ids)
                .observe(StatusInfoActivity.this, relationShips -> {
                    if (relationShips != null) {
                        for (RelationShip relationShip : relationShips) {
                            for (Account account : accounts) {
                                if (account.id.compareToIgnoreCase(relationShip.id) == 0) {
                                    account.relationShip = relationShip;
                                }
                            }
                        }
                        accountAdapter.notifyItemRangeChanged(position, accounts.size());
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

    public enum typeOfInfo {
        LIKED_BY,
        BOOSTED_BY
    }
}
