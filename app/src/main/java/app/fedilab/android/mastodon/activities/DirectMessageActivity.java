package app.fedilab.android.mastodon.activities;
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


import static app.fedilab.android.BaseMainActivity.currentAccount;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityDirectMessageBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonDirectMessage;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;

public class DirectMessageActivity extends BaseActivity implements FragmentMastodonDirectMessage.FirstMessage {

    public static boolean expand;
    public static boolean displayCW;

    Fragment currentFragment;
    private Status firstMessage;
    private String remote_instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDirectMessageBinding binding = ActivityDirectMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        binding.title.setText(R.string.context_conversation);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float scale = sharedpreferences.getFloat(getString(R.string.SET_FONT_SCALE), 1.1f);
        binding.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        Bundle b = getIntent().getExtras();
        displayCW = sharedpreferences.getBoolean(getString(R.string.SET_EXPAND_CW), false);
        Status focusedStatus = null; // or other values
        if (b != null) {
            focusedStatus = (Status) b.getSerializable(Helper.ARG_STATUS);
            remote_instance = b.getString(Helper.ARG_REMOTE_INSTANCE, null);
        }
        if (focusedStatus == null || currentAccount == null || currentAccount.mastodon_account == null) {
            finish();
            return;
        }
        MastodonHelper.loadPPMastodon(binding.profilePicture, currentAccount.mastodon_account);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Helper.ARG_STATUS, focusedStatus);
        bundle.putString(Helper.ARG_REMOTE_INSTANCE, remote_instance);
        FragmentMastodonDirectMessage FragmentMastodonDirectMessage = new FragmentMastodonDirectMessage();
        FragmentMastodonDirectMessage.firstMessage = this;
        currentFragment = Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_content_main, FragmentMastodonDirectMessage, bundle, null, null);
        StatusesVM timelinesVM = new ViewModelProvider(DirectMessageActivity.this).get(StatusesVM.class);
        timelinesVM.getStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, focusedStatus.id).observe(DirectMessageActivity.this, status -> {
            if (status != null) {
                StatusCache statusCache = new StatusCache();
                statusCache.instance = BaseMainActivity.currentInstance;
                statusCache.user_id = BaseMainActivity.currentUserID;
                statusCache.status = status;
                statusCache.status_id = status.id;
                //Update cache
                new Thread(() -> {
                    try {
                        new StatusCache(getApplication()).updateIfExists(statusCache);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        //Update UI
                        Runnable myRunnable = () -> StatusAdapter.sendAction(DirectMessageActivity.this, Helper.ARG_STATUS_ACTION, status, null);
                        mainHandler.post(myRunnable);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }).start();
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

    @Override
    public void get(Status status) {
        firstMessage = status;
        invalidateOptionsMenu();
    }
}