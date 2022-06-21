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

import static app.fedilab.android.BaseMainActivity.currentAccount;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayout;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityScheduledBinding;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.pageadapter.FedilabScheduledPageAdapter;

public class ScheduledActivity extends BaseActivity {

    private ActivityScheduledBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityScheduledBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        MastodonHelper.loadPPMastodon(binding.profilePicture, currentAccount.mastodon_account);
        binding.title.setText(R.string.scheduled);
        binding.scheduleTablayout.addTab(binding.scheduleTablayout.newTab().setText(getString(R.string.toots_server)));
        binding.scheduleTablayout.addTab(binding.scheduleTablayout.newTab().setText(getString(R.string.toots_client)));
        binding.scheduleTablayout.addTab(binding.scheduleTablayout.newTab().setText(getString(R.string.reblog)));

        binding.scheduleViewpager.setAdapter(new FedilabScheduledPageAdapter(getSupportFragmentManager()));
        binding.scheduleViewpager.setOffscreenPageLimit(3);
        binding.scheduleViewpager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.scheduleTablayout));

        binding.scheduleTablayout.setTabTextColors(ThemeHelper.getAttColor(ScheduledActivity.this, R.attr.mTextColor), ContextCompat.getColor(ScheduledActivity.this, R.color.cyanea_accent_dark_reference));
        binding.scheduleTablayout.setTabIconTint(ThemeHelper.getColorStateList(ScheduledActivity.this));

        binding.scheduleTablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.scheduleViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
