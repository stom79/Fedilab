package app.fedilab.android.activities;
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


import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.databinding.ActivityCacheBinding;
import app.fedilab.android.helper.CacheHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.CacheAdapter;

public class CacheActivity extends BaseActivity {

    private ActivityCacheBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityCacheBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        CacheHelper.getCacheValues(CacheActivity.this, size -> {
            if (size > 0) {
                size = size / 1000000.0f;
            }
            binding.fileCacheSize.setText(String.format("%s %s", String.format(Locale.getDefault(), "%.2f", size), getString(R.string.cache_units)));
        });


        new Thread(() -> {
            List<BaseAccount> accounts = new Account(CacheActivity.this).getPushNotificationAccounts();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                CacheAdapter cacheAdapter = new CacheAdapter(accounts);
                LinearLayoutManager mLayoutManager = new LinearLayoutManager(CacheActivity.this);
                binding.cacheRecyclerview.setLayoutManager(mLayoutManager);
                binding.cacheRecyclerview.setAdapter(cacheAdapter);
            };
            mainHandler.post(myRunnable);
        }).start();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
