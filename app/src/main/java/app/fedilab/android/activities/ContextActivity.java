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


import static app.fedilab.android.ui.drawer.StatusAdapter.sendAction;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.QuickLoad;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.databinding.ActivityConversationBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonContext;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;

public class ContextActivity extends BaseActivity {

    public static boolean expand;
    public static boolean displayCW;
    public static Resources.Theme theme;
    Fragment currentFragment;
    private Status focusedStatus;
    private ActivityConversationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        binding.title.setText(R.string.context_conversation);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setPopupTheme(Helper.popupStyle());
        Bundle b = getIntent().getExtras();
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ContextActivity.this);
        displayCW = sharedpreferences.getBoolean(getString(R.string.SET_EXPAND_CW), false);
        focusedStatus = null; // or other values
        if (b != null)
            focusedStatus = (Status) b.getSerializable(Helper.ARG_STATUS);
        if (focusedStatus == null) {
            finish();
            return;
        }
        if (BaseMainActivity.accountWeakReference.get() != null) {
            MastodonHelper.loadPPMastodon(binding.profilePicture, BaseMainActivity.accountWeakReference.get().mastodon_account);
        }
        Bundle bundle = new Bundle();
        new Thread(() -> {
            focusedStatus = SpannableHelper.convertStatus(getApplication().getApplicationContext(), focusedStatus);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                bundle.putSerializable(Helper.ARG_STATUS, focusedStatus);
                currentFragment = Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_content_main, new FragmentMastodonContext(), bundle, null, null);
            };
            mainHandler.post(myRunnable);
        }).start();
        StatusesVM timelinesVM = new ViewModelProvider(ContextActivity.this).get(StatusesVM.class);
        timelinesVM.getStatus(MainActivity.currentInstance, MainActivity.currentToken, focusedStatus.id).observe(ContextActivity.this, status -> {
            StatusCache statusCache = new StatusCache();
            statusCache.instance = MainActivity.currentInstance;
            statusCache.user_id = MainActivity.currentUserID;
            statusCache.status = status;
            statusCache.status_id = status.id;
            //Update cache
            new Thread(() -> {
                try {
                    new StatusCache(getApplication()).updateIfExists(statusCache);
                    new QuickLoad(getApplication().getApplicationContext()).updateStatus(MainActivity.accountWeakReference.get(), status);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    //Update UI
                    Runnable myRunnable = () -> sendAction(ContextActivity.this, Helper.ARG_STATUS_ACTION, status, null);
                    mainHandler.post(myRunnable);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();

        });
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_context, menu);
        MenuItem itemExpand = menu.findItem(R.id.action_expand);
        if (expand) {
            itemExpand.setIcon(R.drawable.ic_baseline_expand_less_24);
        } else {
            itemExpand.setIcon(R.drawable.ic_baseline_expand_more_24);
        }
        MenuItem itemDisplayCW = menu.findItem(R.id.action_show_cw);
        if (displayCW) {
            itemDisplayCW.setIcon(R.drawable.ic_baseline_remove_red_eye_24);
        } else {
            itemDisplayCW.setIcon(R.drawable.ic_outline_remove_red_eye_24);
        }
        return true;
    }

    public void setCurrentFragment(FragmentMastodonContext fragmentMastodonContext) {
        currentFragment = fragmentMastodonContext;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_expand) {
            expand = !expand;
            if (currentFragment != null && currentFragment instanceof FragmentMastodonContext) {
                ((FragmentMastodonContext) currentFragment).redraw();
            }
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.action_show_cw) {
            displayCW = !displayCW;
            if (currentFragment != null && currentFragment instanceof FragmentMastodonContext) {
                ((FragmentMastodonContext) currentFragment).refresh();
            }
            invalidateOptionsMenu();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        currentFragment = null;
    }
}