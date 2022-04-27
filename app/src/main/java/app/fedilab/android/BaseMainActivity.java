package app.fedilab.android;
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

import static app.fedilab.android.BaseMainActivity.status.DISCONNECTED;
import static app.fedilab.android.BaseMainActivity.status.UNKNOWN;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.helper.Helper.deleteDir;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jaredrummler.cyanea.Cyanea;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import app.fedilab.android.activities.ActionActivity;
import app.fedilab.android.activities.BaseActivity;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.activities.DraftActivity;
import app.fedilab.android.activities.FilterActivity;
import app.fedilab.android.activities.InstanceActivity;
import app.fedilab.android.activities.InstanceHealthActivity;
import app.fedilab.android.activities.LoginActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.MastodonListActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.activities.ProxyActivity;
import app.fedilab.android.activities.ReorderTimelinesActivity;
import app.fedilab.android.activities.ScheduledActivity;
import app.fedilab.android.activities.SearchResultTabActivity;
import app.fedilab.android.activities.SettingsActivity;
import app.fedilab.android.broadcastreceiver.NetworkStateReceiver;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.entities.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.mastodon.entities.Filter;
import app.fedilab.android.client.mastodon.entities.Instance;
import app.fedilab.android.client.mastodon.entities.MastodonList;
import app.fedilab.android.databinding.ActivityMainBinding;
import app.fedilab.android.databinding.NavHeaderMainBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.PinnedTimelineHelper;
import app.fedilab.android.helper.PushHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.InstancesVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;
import app.fedilab.android.viewmodel.mastodon.TopBarVM;
import es.dmoral.toasty.Toasty;

public abstract class BaseMainActivity extends BaseActivity implements NetworkStateReceiver.NetworkStateReceiverListener {

    public static String currentInstance, currentToken, currentUserID, client_id, client_secret, software;
    public static Account.API api;
    public static boolean admin;
    public static WeakReference<Account> accountWeakReference;
    public static HashMap<Integer, Fragment> mPageReferenceMap;
    public static status networkAvailable = UNKNOWN;
    public static Instance instanceInfo;
    public static List<Filter> mainFilters;
    public static boolean filterFetched;
    Fragment currentFragment;
    private Account account;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Pinned pinned;

    private final BroadcastReceiver broadcast_data = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                if (b.getBoolean(Helper.RECEIVE_REDRAW_TOPBAR, false)) {
                    List<MastodonList> mastodonLists = (List<MastodonList>) b.getSerializable(Helper.RECEIVE_MASTODON_LIST);
                    redrawPinned(mastodonLists);
                } else if (b.getBoolean(Helper.RECEIVE_RECREATE_ACTIVITY, false)) {
                    Cyanea.getInstance().edit().apply().recreate(BaseMainActivity.this);
                }
            }
        }
    };
    private NetworkStateReceiver networkStateReceiver;
    private boolean headerMenuOpen;

    protected abstract void rateThisApp();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mamageNewIntent(intent);
    }

    @SuppressLint("ApplySharedPref")
    private void mamageNewIntent(Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        String type = intent.getType();
        Bundle extras = intent.getExtras();
        String userIdIntent, instanceIntent;
        if (extras != null && extras.containsKey(Helper.INTENT_ACTION)) {
            userIdIntent = extras.getString(Helper.PREF_KEY_ID); //Id of the account in the intent
            instanceIntent = extras.getString(Helper.PREF_INSTANCE);
            if (extras.getInt(Helper.INTENT_ACTION) == Helper.NOTIFICATION_INTENT) {
                try {
                    Account account = new Account(BaseMainActivity.this).getUniqAccount(userIdIntent, instanceIntent);
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
                    headerMenuOpen = false;
                    Toasty.info(BaseMainActivity.this, getString(R.string.toast_account_changed, "@" + account.mastodon_account.acct + "@" + account.instance), Toasty.LENGTH_LONG).show();
                    BaseMainActivity.currentToken = account.token;
                    BaseMainActivity.currentUserID = account.user_id;
                    api = account.api;
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(PREF_USER_TOKEN, account.token);
                    editor.commit();
                    Intent mainActivity = new Intent(this, MainActivity.class);
                    startActivity(mainActivity);
                    intent.removeExtra(Helper.INTENT_ACTION);
                    finish();
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        ThemeHelper.applyTheme(this);
        if (!Helper.isLoggedIn(BaseMainActivity.this)) {
            //It is not, the user is redirected to the login page
            Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
            startActivity(myIntent);
            finish();
            return;
        } else {
            BaseMainActivity.currentToken = sharedpreferences.getString(Helper.PREF_USER_TOKEN, null);
        }
        mamageNewIntent(getIntent());
        ThemeHelper.initiliazeColors(BaseMainActivity.this);
        filterFetched = false;
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        rateThisApp();
        SharedPreferences cyneaPref = getSharedPreferences("com.jaredrummler.cyanea", Context.MODE_PRIVATE);
        binding.tabLayout.setTabTextColors(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor), cyneaPref.getInt("theme_accent", -1));
        binding.tabLayout.setTabIconTint(ThemeHelper.getColorStateList(BaseMainActivity.this));
        binding.compose.setOnClickListener(v -> startActivity(new Intent(this, ComposeActivity.class)));
        headerMenuOpen = false;
        binding.bottomNavView.inflateMenu(R.menu.bottom_nav_menu);
        binding.bottomNavView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                binding.viewPager.setCurrentItem(0);
            } else if (itemId == R.id.nav_local) {
                binding.viewPager.setCurrentItem(1);
            } else if (itemId == R.id.nav_public) {
                binding.viewPager.setCurrentItem(2);
            } else if (itemId == R.id.nav_notifications) {
                binding.viewPager.setCurrentItem(3);
            } else if (itemId == R.id.nav_privates) {
                binding.viewPager.setCurrentItem(4);
            }
            return true;
        });


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder()
                .setOpenableLayout(binding.drawerLayout)
                .build();

        NavHeaderMainBinding headerMainBinding = NavHeaderMainBinding.inflate(getLayoutInflater());
        binding.navView.addHeaderView(headerMainBinding.getRoot());
        binding.navView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_drafts) {
                Intent intent = new Intent(this, DraftActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_reorder) {
                Intent intent = new Intent(this, ReorderTimelinesActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_interactions) {
                Intent intent = new Intent(this, ActionActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_filter) {
                Intent intent = new Intent(this, FilterActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_list) {
                Intent intent = new Intent(this, MastodonListActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_scheduled) {
                Intent intent = new Intent(this, ScheduledActivity.class);
                startActivity(intent);
            }
            binding.drawerLayout.close();
            return false;
        });
        headerMainBinding.instanceInfoContainer.setOnClickListener(v -> startActivity(new Intent(BaseMainActivity.this, InstanceHealthActivity.class)));
        headerMainBinding.accountProfilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(BaseMainActivity.this, ProfileActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT, account.mastodon_account);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(BaseMainActivity.this, headerMainBinding.instanceInfoContainer, getString(R.string.activity_porfile_pp));
            startActivity(intent, options.toBundle());
        });
        headerMainBinding.changeAccount.setOnClickListener(v -> {
            headerMenuOpen = !headerMenuOpen;
            if (headerMenuOpen) {
                headerMainBinding.ownerAccounts.setImageResource(R.drawable.ic_baseline_arrow_drop_up_24);
                new Thread(() -> {
                    try {
                        List<Account> accounts = new Account(BaseMainActivity.this).getAll();
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> {
                            binding.navView.getMenu().clear();
                            binding.navView.inflateMenu(R.menu.menu_accounts);
                            headerMenuOpen = true;

                            Menu mainMenu = binding.navView.getMenu();
                            SubMenu currentSubmenu = null;
                            String lastInstance = "";
                            if (accounts != null) {
                                for (final Account account : accounts) {
                                    if (!currentToken.equalsIgnoreCase(account.token)) {
                                        if (!lastInstance.trim().equalsIgnoreCase(account.instance.trim())) {
                                            lastInstance = account.instance.toUpperCase();
                                            currentSubmenu = mainMenu.addSubMenu(account.instance.toUpperCase());
                                        }
                                        if (currentSubmenu == null) {
                                            continue;
                                        }
                                        final MenuItem item = currentSubmenu.add("@" + account.mastodon_account.acct);
                                        item.setIcon(R.drawable.ic_person);
                                        boolean disableGif = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_GIF), false);
                                        String url = !disableGif ? account.mastodon_account.avatar : account.mastodon_account.avatar_static;
                                        if (url.startsWith("/")) {
                                            url = "https://" + account.instance + account.mastodon_account.avatar;
                                        }
                                        if (!this.isDestroyed() && !this.isFinishing()) {
                                            if (url.contains(".gif")) {
                                                Glide.with(BaseMainActivity.this)
                                                        .asGif()
                                                        .load(url)
                                                        .into(new CustomTarget<GifDrawable>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull GifDrawable resource, Transition<? super GifDrawable> transition) {
                                                                item.setIcon(resource);
                                                                item.getIcon().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {

                                                            }
                                                        });
                                            } else {
                                                Glide.with(BaseMainActivity.this)
                                                        .asDrawable()
                                                        .load(url)
                                                        .into(new CustomTarget<Drawable>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                                                                item.setIcon(resource);
                                                                item.getIcon().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {

                                                            }
                                                        });
                                            }

                                        }
                                        item.setOnMenuItemClickListener(item1 -> {
                                            if (!this.isFinishing()) {
                                                headerMenuOpen = false;
                                                Toasty.info(BaseMainActivity.this, getString(R.string.toast_account_changed, "@" + account.mastodon_account.acct + "@" + account.instance), Toasty.LENGTH_LONG).show();
                                                BaseMainActivity.currentToken = account.token;
                                                BaseMainActivity.currentUserID = account.user_id;
                                                api = account.api;
                                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                                editor.putString(PREF_USER_TOKEN, account.token);
                                                editor.commit();
                                                //The user is now aut
                                                //The user is now authenticated, it will be redirected to MainActivity
                                                Intent mainActivity = new Intent(this, MainActivity.class);
                                                startActivity(mainActivity);
                                                finish();
                                                headerMainBinding.ownerAccounts.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
                                                return true;
                                            }
                                            return false;
                                        });

                                    }
                                }

                            }
                            currentSubmenu = mainMenu.addSubMenu("");
                            MenuItem addItem = currentSubmenu.add(R.string.add_account);
                            addItem.setIcon(R.drawable.ic_baseline_person_add_24);
                            addItem.setOnMenuItemClickListener(item -> {
                                Intent intent = new Intent(BaseMainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                return true;
                            });

                        };
                        mainHandler.post(myRunnable);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                binding.navView.getMenu().clear();
                binding.navView.inflateMenu(R.menu.activity_main_drawer);
                headerMainBinding.ownerAccounts.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24);
                headerMenuOpen = false;
            }
        });
        headerMainBinding.headerOptionInfo.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(BaseMainActivity.this, Helper.popupStyle()), headerMainBinding.headerOptionInfo);
            popup.getMenuInflater()
                    .inflate(R.menu.main, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_logout_account) {
                    AlertDialog.Builder alt_bld = new AlertDialog.Builder(BaseMainActivity.this, Helper.dialogStyle());
                    alt_bld.setTitle(R.string.action_logout);
                    alt_bld.setMessage(getString(R.string.logout_account_confirmation, account.mastodon_account.username, account.instance));
                    alt_bld.setPositiveButton(R.string.action_logout, (dialog, id) -> {
                        dialog.dismiss();
                        try {
                            Helper.removeAccount(BaseMainActivity.this, null);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                    return true;
                } else if (itemId == R.id.action_about_instance) {
                    Intent intent = new Intent(BaseMainActivity.this, InstanceActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.action_cache) {
                    new Helper.CacheTask(BaseMainActivity.this);
                    return true;
                } else if (itemId == R.id.action_proxy) {
                    Intent intent = new Intent(BaseMainActivity.this, ProxyActivity.class);
                    startActivity(intent);
                    return true;
                }
                return true;
            });
            popup.show();
        });
        account = null;
        //Update account details
        new Thread(() -> {
            try {
                account = new Account(BaseMainActivity.this).getConnectedAccount();
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                if (account == null) {
                    //It is not, the user is redirected to the login page
                    Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
                    startActivity(myIntent);
                    finish();
                    return;
                }
                currentInstance = account.instance;
                currentUserID = account.user_id;
                accountWeakReference = new WeakReference<>(account);
                binding.profilePicture.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
                Helper.loadPP(binding.profilePicture, account);
                headerMainBinding.accountAcc.setText(String.format("%s@%s", account.mastodon_account.username, account.instance));
                if (account.mastodon_account.display_name.isEmpty()) {
                    account.mastodon_account.display_name = account.mastodon_account.acct;
                }
                headerMainBinding.accountName.setText(account.mastodon_account.display_name);

                Helper.loadPP(headerMainBinding.accountProfilePicture, account);

                /*
                 * Some general data are loaded when the app starts such;
                 *  - Instance info (for limits)
                 *  - Emoji for picker
                 *  - Filters for timelines
                 *  - Pinned timelines (in app feature)
                 */

                //Update emoji in db for the current instance
                new ViewModelProvider(BaseMainActivity.this).get(InstancesVM.class).getEmoji(currentInstance);
                //Retrieve instance info
                new ViewModelProvider(BaseMainActivity.this).get(InstancesVM.class).getInstance(currentInstance)
                        .observe(BaseMainActivity.this, instance -> instanceInfo = instance.info);
                //Retrieve filters
                new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class).getFilters(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, filters -> mainFilters = filters);
                new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class).getConnectedAccount(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, account1 -> {
                            BaseMainActivity.accountWeakReference.get().mastodon_account = account1;
                        });
                //Update pinned timelines
                new ViewModelProvider(BaseMainActivity.this).get(TopBarVM.class).getDBPinned()
                        .observe(this, pinned -> {
                            this.pinned = pinned;
                            //First it's taken from db (last stored values)
                            PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, null);
                            //Fetch remote lists for the authenticated account and update them
                            new ViewModelProvider(BaseMainActivity.this).get(TimelinesVM.class).getLists(currentInstance, currentToken)
                                    .observe(this, mastodonLists -> PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, mastodonLists));
                        });
            };
            mainHandler.post(myRunnable);
        }).start();
        //Toolbar search
        binding.toolbarSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.toolbarSearch.getWindowToken(), 0);
                query = query.replaceAll("^#+", "");
                Intent intent;
                intent = new Intent(BaseMainActivity.this, SearchResultTabActivity.class);
                intent.putExtra(Helper.ARG_SEARCH_KEYWORD, query);
                startActivity(intent);
                binding.toolbarSearch.setQuery("", false);
                binding.toolbarSearch.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        binding.toolbarSearch.setOnCloseListener(() -> {
            binding.tabLayout.setVisibility(View.VISIBLE);
            return false;
        });

        PushHelper.startStreaming(BaseMainActivity.this);

        binding.toolbarSearch.setOnSearchClickListener(v -> binding.tabLayout.setVisibility(View.VISIBLE));
        //For receiving  data from other activities
        LocalBroadcastManager.getInstance(BaseMainActivity.this).registerReceiver(broadcast_data, new IntentFilter(Helper.BROADCAST_DATA));
    }

    public void refreshFragment() {
        if (binding.viewPager.getAdapter() != null) {
            binding.viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(BaseMainActivity.this).unregisterReceiver(broadcast_data);
        if (networkStateReceiver != null) {
            try {
                unregisterReceiver(networkStateReceiver);
            } catch (IllegalArgumentException illegalArgumentException) {
                illegalArgumentException.printStackTrace();
            }
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        boolean clearCacheExit = sharedpreferences.getBoolean(getString(R.string.SET_CLEAR_CACHE_EXIT), false);
        //Clear cache when leaving - Default = false
        if (clearCacheExit) {
            new Thread(() -> {
                try {
                    if (getCacheDir().getParentFile() != null) {
                        String path = getCacheDir().getParentFile().getPath();
                        File dir = new File(path);
                        if (dir.isDirectory()) {
                            deleteDir(dir);
                        }
                    }
                } catch (Exception ignored) {
                }
            }).start();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void redrawPinned(List<MastodonList> mastodonLists) {
        int currentItem = binding.viewPager.getCurrentItem();
        new ViewModelProvider(BaseMainActivity.this).get(TopBarVM.class).getDBPinned()
                .observe(this, pinned -> {
                    this.pinned = pinned;
                    //First it's taken from db (last stored values)
                    PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, mastodonLists);
                    binding.viewPager.setCurrentItem(currentItem);
                });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            int fragments = getSupportFragmentManager().getBackStackEntryCount();
            if (fragments == 1) {
                finish();
            } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                getSupportFragmentManager().popBackStack();
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                for (Fragment fragment : fragmentList) {
                    if (fragment != null && fragment.isVisible()) {

                        if (fragment instanceof FragmentMastodonTimeline) {
                            currentFragment = fragment;
                            getSupportFragmentManager().beginTransaction().show(currentFragment).commit();
                        }

                    }
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    public boolean getFloatingVisibility() {
        return binding.compose.getVisibility() == View.VISIBLE;
    }

    public void manageFloatingButton(boolean display) {
        if (display) {
            binding.compose.show();
        } else {
            binding.compose.hide();
        }
    }

   /* @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(BaseMainActivity.this, Helper.dialogStyle());
            alt_bld.setTitle(R.string.action_logout);
            alt_bld.setMessage(getString(R.string.logout_account_confirmation, account.mastodon_account.username, account.instance));
            alt_bld.setPositiveButton(R.string.action_logout, (dialog, id) -> {
                dialog.dismiss();
                try {
                    Helper.removeAccount(BaseMainActivity.this, null);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            AlertDialog alert = alt_bld.create();
            alert.show();

        }
        return true;
    }*/

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        //unselect all tag elements
        for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
            pinnedTimeline.isSelected = false;
        }
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void networkAvailable() {
        networkAvailable = status.CONNECTED;
    }

    @Override
    public void networkUnavailable() {
        networkAvailable = DISCONNECTED;
    }


    public enum status {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED
    }
}