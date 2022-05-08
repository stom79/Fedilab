package app.fedilab.android.activities;
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


import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.client.mastodon.entities.Accounts;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.ActivityStatusInfoBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.AccountAdapter;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;


public class StatusInfoActivity extends BaseActivity {

    private ActivityStatusInfoBinding binding;
    private List<Account> accountList;
    private AccountAdapter accountAdapter;
    private String max_id;
    private typeOfInfo type;
    private boolean flagLoading;
    private Status status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityStatusInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        accountList = new ArrayList<>();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            type = (typeOfInfo) b.getSerializable(Helper.ARG_TYPE_OF_INFO);
            status = (Status) b.getSerializable(Helper.ARG_STATUS);
        }
        if (type == null || status == null) {
            finish();
            return;
        }
        flagLoading = false;
        max_id = null;
        binding.title.setText(type == typeOfInfo.BOOSTED_BY ? R.string.boosted_by : R.string.favourited_by);
        StatusesVM statusesVM = new ViewModelProvider(StatusInfoActivity.this).get(StatusesVM.class);
        accountAdapter = new AccountAdapter(accountList);
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
                                statusesVM.rebloggedBy(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, max_id, null, null).observe(StatusInfoActivity.this, accounts -> manageView(accounts));
                            } else if (type == typeOfInfo.LIKED_BY) {
                                statusesVM.favouritedBy(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, max_id, null, null).observe(StatusInfoActivity.this, accounts -> manageView(accounts));
                            }
                        }
                    } else {
                        binding.loadingNextAccounts.setVisibility(View.GONE);
                    }
                }
            }
        });
        if (type == typeOfInfo.BOOSTED_BY) {
            statusesVM.rebloggedBy(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, null, null, null).observe(StatusInfoActivity.this, this::manageView);
        } else if (type == typeOfInfo.LIKED_BY) {
            statusesVM.favouritedBy(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, null, null, null).observe(StatusInfoActivity.this, this::manageView);
        }
    }

    private void manageView(Accounts accounts) {
        flagLoading = false;
        binding.loadingNextAccounts.setVisibility(View.GONE);
        if (accountList != null && accounts != null && accounts.accounts != null) {
            int startId = 0;
            //There are some statuses present in the timeline
            if (accountList.size() > 0) {
                startId = accountList.size();
            }
            accountList.addAll(accounts.accounts);
            max_id = accounts.pagination.max_id;
            accountAdapter.notifyItemRangeInserted(startId, accounts.accounts.size());
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

    public enum typeOfInfo {
        LIKED_BY,
        BOOSTED_BY
    }
}
