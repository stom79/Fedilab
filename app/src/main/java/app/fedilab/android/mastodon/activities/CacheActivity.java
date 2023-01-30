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
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityCacheBinding;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CacheAccount;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.CacheHelper;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.CacheAdapter;

public class CacheActivity extends BaseBarActivity {

    private ActivityCacheBinding binding;
    private List<CacheAccount> cacheAccounts;
    private CacheAdapter cacheAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCacheBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        CacheHelper.getCacheValues(CacheActivity.this, size -> {
            if (size > 0) {
                size = size / 1000000.0f;
            }
            binding.fileCacheSize.setText(String.format("%s %s", String.format(Locale.getDefault(), "%.2f", size), getString(R.string.cache_units)));
        });


        new Thread(() -> {
            List<BaseAccount> accounts;
            cacheAccounts = new ArrayList<>();
            try {
                accounts = new Account(CacheActivity.this).getAll();
                for (BaseAccount baseAccount : accounts) {
                    CacheAccount cacheAccount = new CacheAccount();
                    cacheAccount.account = baseAccount;
                    try {
                        cacheAccount.home_cache_count = new StatusCache(CacheActivity.this).countHome(baseAccount);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    try {
                        cacheAccount.other_cache_count = new StatusCache(CacheActivity.this).countOther(baseAccount);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    try {
                        cacheAccount.draft_count = new StatusDraft(CacheActivity.this).count(baseAccount);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    cacheAccounts.add(cacheAccount);
                }
            } catch (DBException e) {
                e.printStackTrace();
            }


            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                cacheAdapter = new CacheAdapter(cacheAccounts);
                binding.cacheRecyclerview.setAdapter(cacheAdapter);
                LinearLayoutManager mLayoutManager = new LinearLayoutManager(CacheActivity.this);
                binding.cacheRecyclerview.setLayoutManager(mLayoutManager);
            };
            mainHandler.post(myRunnable);
        }).start();

    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cache, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear) {
            AlertDialog.Builder deleteConfirm = new MaterialAlertDialogBuilder(CacheActivity.this);
            deleteConfirm.setTitle(getString(R.string.delete_cache));
            deleteConfirm.setMessage(getString(R.string.delete_cache_message));
            deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                CacheHelper.clearCache(CacheActivity.this, binding.labelFileCache.isChecked(), cacheAccounts, () -> CacheHelper.getCacheValues(CacheActivity.this, size -> {
                    if (size > 0) {
                        size = size / 1000000.0f;
                    }
                    binding.fileCacheSize.setText(String.format("%s %s", String.format(Locale.getDefault(), "%.2f", size), getString(R.string.cache_units)));
                    AlertDialog.Builder restartBuilder = new MaterialAlertDialogBuilder(CacheActivity.this);
                    restartBuilder.setMessage(getString(R.string.restart_the_app));
                    restartBuilder.setNegativeButton(R.string.no, (dialogRestart, whichRestart) -> {
                        recreate();
                        dialogRestart.dismiss();
                    });
                    restartBuilder.setPositiveButton(R.string.restart, (dialogRestart, whichRestart) -> {
                        dialogRestart.dismiss();
                        Helper.restart(CacheActivity.this);
                    });
                    AlertDialog alertDialog = restartBuilder.create();
                    if (!isFinishing()) {
                        alertDialog.show();
                    }

                }));
                dialog.dismiss();
            });
            deleteConfirm.create().show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
