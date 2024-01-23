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


import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.BaseMainActivity.currentInstance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.AppBarLayout;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityConversationBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonContext;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;

public class ContextActivity extends BaseActivity implements FragmentMastodonContext.FirstMessage {

    public static boolean expand;
    public static boolean displayCW;
    public static Resources.Theme theme;
    Fragment currentFragment;
    private Status firstMessage;
    private String remote_instance;
    private Status focusedStatus;
    private String focusedStatusURI;
    private boolean checkRemotely;
    private ActivityConversationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConversationBinding.inflate(getLayoutInflater());
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
        manageTopBarScrolling(binding.toolbar, sharedpreferences);
        displayCW = sharedpreferences.getBoolean(getString(R.string.SET_EXPAND_CW), false);
        focusedStatus = null; // or other values


        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(ContextActivity.this).getBundle(bundleId, currentAccount, this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {
        if (bundle != null) {
            focusedStatus = (Status) bundle.getSerializable(Helper.ARG_STATUS);
            remote_instance = bundle.getString(Helper.ARG_REMOTE_INSTANCE, null);
            focusedStatusURI = bundle.getString(Helper.ARG_FOCUSED_STATUS_URI, null);
        }
        if (focusedStatus == null || currentAccount == null || currentAccount.mastodon_account == null) {
            finish();
            return;
        }
        if (focusedStatusURI == null && remote_instance == null) {
            focusedStatusURI = focusedStatus.uri;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkRemotely = sharedpreferences.getBoolean(getString(R.string.SET_CONVERSATION_REMOTELY), false);
        if (!checkRemotely) {
            loadLocalConversation();
        } else {
            loadRemotelyConversation(true);
            invalidateOptionsMenu();
        }
        if (currentAccount != null) {
            MastodonHelper.loadPPMastodon(binding.profilePicture, currentAccount.mastodon_account);
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    private void manageTopBarScrolling(Toolbar toolbar, SharedPreferences sharedpreferences) {
        final boolean topBarScrolling = !sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_TOPBAR_SCROLLING), false);

        final AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();

        int scrollFlags = toolbarLayoutParams.getScrollFlags();

        if (topBarScrolling) {
            scrollFlags |= AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL;

        } else {
            scrollFlags &= ~AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL;
        }
        toolbarLayoutParams.setScrollFlags(scrollFlags);
    }

    private void loadLocalConversation() {
        Bundle args = new Bundle();
        args.putSerializable(Helper.ARG_STATUS, focusedStatus);
        args.putString(Helper.ARG_REMOTE_INSTANCE, remote_instance);
        new CachedBundle(ContextActivity.this).insertBundle(args, currentAccount, bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            FragmentMastodonContext fragmentMastodonContext = new FragmentMastodonContext();
            fragmentMastodonContext.firstMessage = this;
            currentFragment = Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_content_main, fragmentMastodonContext, bundle, null, null);
            //Update the status
            if (remote_instance == null) {
                StatusesVM timelinesVM = new ViewModelProvider(ContextActivity.this).get(StatusesVM.class);
                timelinesVM.getStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, focusedStatus.id).observe(ContextActivity.this, status -> {
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
                                Runnable myRunnable = () -> StatusAdapter.sendAction(ContextActivity.this, Helper.ARG_STATUS_ACTION, status, null);
                                mainHandler.post(myRunnable);
                            } catch (DBException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                });
            }
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
        MenuItem action_remote = menu.findItem(R.id.action_remote);
        if (remote_instance != null || checkRemotely) {
            action_remote.setVisible(false);
        } else {
            if (firstMessage != null && !firstMessage.visibility.equalsIgnoreCase("direct") && !firstMessage.visibility.equalsIgnoreCase("private")) {
                Pattern pattern = Helper.statusIdInUrl;
                Matcher matcher = pattern.matcher(firstMessage.uri);
                action_remote.setVisible(matcher.find());
            } else {
                action_remote.setVisible(false);
            }
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
        } else if (item.getItemId() == R.id.action_remote) {
            loadRemotelyConversation(false);

        }
        return true;
    }

    private void loadRemotelyConversation(boolean fallback) {
        if (fallback) {
            StatusesVM statusesVM;
            statusesVM = new ViewModelProvider(this).get(StatusesVM.class);
            statusesVM.getContext(currentInstance, null, focusedStatus.id)
                    .observe(this, result -> {
                        if (result != null && result.ancestors != null && result.ancestors.size() > 0) {
                            firstMessage = result.ancestors.get(0);
                            String instance = null;
                            try {
                                URL url = new URL(firstMessage.uri);
                                instance = url.getHost();
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            if (instance == null) {
                                loadLocalConversation();
                                return;
                            }
                            Pattern pattern = Helper.statusIdInUrl;
                            Matcher matcher = pattern.matcher(firstMessage.uri);
                            String remoteId = null;
                            if (matcher.find()) {
                                remoteId = matcher.group(1);
                            }
                            if (remoteId == null) {
                                loadLocalConversation();
                                return;
                            }
                            String finalInstance = instance;
                            statusesVM.getStatus(instance, null, remoteId).observe(ContextActivity.this, status -> {
                                if (status != null) {
                                    Bundle args = new Bundle();
                                    args.putSerializable(Helper.ARG_STATUS, status);
                                    args.putString(Helper.ARG_REMOTE_INSTANCE, finalInstance);
                                    args.putString(Helper.ARG_FOCUSED_STATUS_URI, focusedStatusURI);
                                    new CachedBundle(ContextActivity.this).insertBundle(args, currentAccount, bundleId -> {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                        FragmentMastodonContext fragmentMastodonContext = new FragmentMastodonContext();
                                        fragmentMastodonContext.firstMessage = ContextActivity.this;
                                        currentFragment = Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_content_main, fragmentMastodonContext, bundle, null, null);
                                    });
                                } else {
                                    loadLocalConversation();
                                }
                            });
                        } else {
                            loadLocalConversation();
                        }
                    });
        } else {
            if (firstMessage == null) {
                Toasty.warning(ContextActivity.this, getString(R.string.toast_try_later), Toasty.LENGTH_SHORT).show();
                return;
            }
            if (firstMessage.account.acct != null) {
                String instance = null;
                try {
                    URL url = new URL(firstMessage.uri);
                    instance = url.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (instance == null) {
                    Toasty.info(ContextActivity.this, getString(R.string.toast_error_fetch_message), Toasty.LENGTH_SHORT).show();
                    return;
                }
                if (instance.equalsIgnoreCase(MainActivity.currentInstance)) {
                    Toasty.info(ContextActivity.this, getString(R.string.toast_on_your_instance), Toasty.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Helper.statusIdInUrl;
                Matcher matcher = pattern.matcher(firstMessage.uri);
                String remoteId = null;
                if (matcher.find()) {
                    remoteId = matcher.group(1);
                }
                if (remoteId != null) {
                    StatusesVM statusesVM = new ViewModelProvider(ContextActivity.this).get(StatusesVM.class);
                    String finalInstance = instance;
                    statusesVM.getStatus(instance, null, remoteId).observe(ContextActivity.this, status -> {
                        if (status != null) {
                            Intent intentContext = new Intent(ContextActivity.this, ContextActivity.class);
                            Bundle args = new Bundle();
                            args.putSerializable(Helper.ARG_STATUS, status);
                            args.putString(Helper.ARG_FOCUSED_STATUS_URI, focusedStatusURI);
                            args.putString(Helper.ARG_REMOTE_INSTANCE, finalInstance);
                            new CachedBundle(ContextActivity.this).insertBundle(args, currentAccount, bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intentContext.putExtras(bundle);
                                intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intentContext);
                            });
                        } else {
                            Toasty.warning(ContextActivity.this, getString(R.string.toast_error_fetch_message), Toasty.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toasty.warning(ContextActivity.this, getString(R.string.toast_error_fetch_message), Toasty.LENGTH_SHORT).show();
                }
            } else {
                Toasty.warning(ContextActivity.this, getString(R.string.toast_error_fetch_message), Toasty.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void get(Status status) {
        firstMessage = status;
        invalidateOptionsMenu();
    }
}