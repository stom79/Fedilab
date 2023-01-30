package app.fedilab.android.peertube.activities;
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
import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentToken;
import static app.fedilab.android.BaseMainActivity.currentUserID;
import static app.fedilab.android.BaseMainActivity.fetchRecentAccounts;
import static app.fedilab.android.BaseMainActivity.headerMenuOpen;
import static app.fedilab.android.BaseMainActivity.headerOptionInfoClick;
import static app.fedilab.android.BaseMainActivity.mamageNewIntent;
import static app.fedilab.android.BaseMainActivity.manageDrawerMenu;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.mastodon.helper.Helper.addFragment;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;
import static app.fedilab.android.peertube.helper.SwitchAccountHelper.switchDialog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kobakei.ratethisapp.RateThisApp;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.AboutActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.PeertubeBaseMainActivity;
import app.fedilab.android.databinding.ActivityMainPeertubeBinding;
import app.fedilab.android.databinding.NavHeaderMainBinding;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.entities.AcadInstances;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.client.entities.UserSettings;
import app.fedilab.android.peertube.fragment.DisplayOverviewFragment;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.fragment.FragmentLoginPickInstancePeertube;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.services.RetrieveInfoService;
import app.fedilab.android.peertube.viewmodel.TimelineVM;


public class PeertubeMainActivity extends PeertubeBaseMainActivity {
    public static String PICK_INSTANCE = "pick_instance";
    public static String INSTANCE_ADDRESS = "instance_address";
    public static UserMe userMe;
    public static InstanceData.InstanceConfig instanceConfig;
    public static TypeOfConnection typeOfConnection = TypeOfConnection.NORMAL;
    public static int badgeCount;
    private DisplayVideosFragment recentFragment;
    private DisplayVideosFragment locaFragment;
    private DisplayVideosFragment trendingFragment;
    private DisplayVideosFragment subscriptionFragment;
    private DisplayOverviewFragment overviewFragment;
    private ActivityMainPeertubeBinding binding;

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.navigation_discover) {
            setTitleCustom(R.string.title_discover);
            binding.viewpager.setCurrentItem(3);
        } else if (itemId == R.id.navigation_subscription) {
            binding.viewpager.setCurrentItem(4);
            setTitleCustom(R.string.subscriptions);
        } else if (itemId == R.id.navigation_trending) {
            setTitleCustom(R.string.title_trending);
            binding.viewpager.setCurrentItem(2);
        } else if (itemId == R.id.navigation_recently_added) {
            setTitleCustom(R.string.title_recently_added);
            binding.viewpager.setCurrentItem(1);
        } else if (itemId == R.id.navigation_local) {
            setTitleCustom(R.string.title_local);
            binding.viewpager.setCurrentItem(0);
        }
        return true;
    };


    private void setTitleCustom(int titleRId) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        if (mTitle != null) {
            mTitle.setText(getString(titleRId));
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = super.binding;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeMainActivity.this);

        badgeCount = 0;
        headerMenuOpen = false;
        binding.navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        recentFragment = new DisplayVideosFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.RECENT);
        recentFragment.setArguments(bundle);

        locaFragment = new DisplayVideosFragment();
        bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.LOCAL);
        locaFragment.setArguments(bundle);

        trendingFragment = new DisplayVideosFragment();
        bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.TRENDING);
        trendingFragment.setArguments(bundle);

        subscriptionFragment = new DisplayVideosFragment();
        bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.SUBSCRIBTIONS);
        subscriptionFragment.setArguments(bundle);

        DisplayVideosFragment mostLikedFragment = new DisplayVideosFragment();
        bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.MOST_LIKED);
        mostLikedFragment.setArguments(bundle);

        NavHeaderMainBinding headerMainBinding = NavHeaderMainBinding.inflate(getLayoutInflater());
        currentAccount = null;
        new Thread(() -> {
            try {
                if (currentToken == null) {
                    currentToken = sharedpreferences.getString(app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN, null);
                }
                currentAccount = new Account(PeertubeMainActivity.this).getConnectedAccount();
                if (currentAccount == null) {
                    if (currentUserID == null) {
                        currentUserID = sharedpreferences.getString(PREF_USER_ID, null);
                    }
                    if (currentInstance == null) {
                        currentInstance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
                    }
                    currentAccount = new Account(PeertubeMainActivity.this).getUniqAccount(currentUserID, currentInstance);
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            if (currentAccount != null && currentAccount.mastodon_account != null) {
                //It is a Mastodon User
                Intent myIntent = new Intent(PeertubeMainActivity.this, MainActivity.class);
                startActivity(myIntent);
                finish();
                return;
            }
            //If the attached account is null, the app will fetch remote instance to get up-to-date values
            if (currentAccount != null && currentAccount.peertube_account == null) {
                try {
                    userMe = new RetrofitPeertubeAPI(PeertubeMainActivity.this, currentInstance, currentToken).verifyCredentials();
                    currentAccount.peertube_account = userMe.getAccount();
                } catch (Error e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                headerMainBinding.accountAcc.setText(String.format("%s@%s", currentAccount.peertube_account.getUsername(), currentAccount.instance));
                if (currentAccount.peertube_account.getDisplayName() == null || currentAccount.peertube_account.getDisplayName().isEmpty()) {
                    currentAccount.peertube_account.setDisplayName(currentAccount.peertube_account.getAcct());
                }
                headerMainBinding.accountName.setText(currentAccount.peertube_account.getDisplayName());
                float scale = sharedpreferences.getFloat(getString(R.string.SET_FONT_SCALE), 1.1f);
                headerMainBinding.accountName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
                headerMainBinding.accountAcc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
                app.fedilab.android.mastodon.helper.Helper.loadPP(PeertubeMainActivity.this, headerMainBinding.accountProfilePicture, currentAccount, false);
                headerMainBinding.backgroundImage.setAlpha(0.5f);
                headerMainBinding.accountAcc.setOnClickListener(v -> headerMainBinding.changeAccount.callOnClick());
                headerMainBinding.changeAccount.setOnClickListener(v -> {

                    headerMenuOpen = !headerMenuOpen;
                    manageDrawerMenu(PeertubeMainActivity.this, binding.drawerNavView, headerMainBinding);
                });
                if (Helper.isLoggedIn()) {
                    binding.navView.inflateMenu(R.menu.bottom_nav_menu_connected_peertube);
                    refreshToken();

                } else {
                    binding.navView.inflateMenu(R.menu.bottom_nav_menu);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        headerMainBinding.instanceInfo.setVisibility(View.GONE);

        headerMainBinding.headerOptionInfo.setOnClickListener(v -> headerOptionInfoClick(PeertubeMainActivity.this, headerMainBinding, getSupportFragmentManager()));

        binding.drawerNavView.addHeaderView(headerMainBinding.getRoot());

        binding.drawerNavView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                Intent intent = new Intent(PeertubeMainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_account) {
                Intent intent;
                if (typeOfConnection == TypeOfConnection.SURFING) {
                    switchDialog(PeertubeMainActivity.this, false);
                } else {
                    if (Helper.isLoggedIn()) {
                        intent = new Intent(PeertubeMainActivity.this, AccountActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
                    } else {
                        intent = new Intent(PeertubeMainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }

                }
            } else if (item.getItemId() == R.id.action_upload) {
                Intent intent = new Intent(PeertubeMainActivity.this, PeertubeUploadActivity.class);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_myvideos) {
                Intent intent = new Intent(PeertubeMainActivity.this, VideosTimelineActivity.class);
                Bundle bundledrawer = new Bundle();
                bundledrawer.putSerializable("type", TimelineVM.TimelineType.MY_VIDEOS);
                intent.putExtras(bundledrawer);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_history) {
                Intent intent = new Intent(PeertubeMainActivity.this, VideosTimelineActivity.class);
                Bundle bundledrawer = new Bundle();
                bundledrawer.putSerializable("type", TimelineVM.TimelineType.HISTORY);
                intent.putExtras(bundledrawer);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_most_liked) {
                Intent intent = new Intent(PeertubeMainActivity.this, VideosTimelineActivity.class);
                Bundle bundledrawer = new Bundle();
                bundledrawer.putSerializable("type", TimelineVM.TimelineType.MOST_LIKED);
                intent.putExtras(bundledrawer);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_playlist) {
                Intent intent;
                intent = new Intent(PeertubeMainActivity.this, AllPlaylistsActivity.class);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_sepia_search) {
                Intent intent = new Intent(PeertubeMainActivity.this, SepiaSearchActivity.class);
                startActivity(intent);
            } else if (item.getItemId() == R.id.action_about) {
                Intent intent = new Intent(PeertubeMainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
            binding.drawerLayout.close();
            return false;
        });

        overviewFragment = new DisplayOverviewFragment();
        if (!Helper.isLoggedIn()) {
            PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
            binding.viewpager.setAdapter(mPagerAdapter);
        } else {
            new Thread(() -> {
                badgeCount = new RetrofitPeertubeAPI(PeertubeMainActivity.this).unreadNotifications();
                invalidateOptionsMenu();
            }).start();
        }
        if (Helper.isLoggedIn()) {
            binding.viewpager.setOffscreenPageLimit(5);
        } else {
            binding.viewpager.setOffscreenPageLimit(4);
        }


        binding.viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                MenuItem item = binding.navView.getMenu().getItem(position);
                binding.navView.setSelectedItemId(item.getItemId());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        toolbar.setOnClickListener(v -> {
            if (binding.viewpager.getAdapter() == null) {
                return;
            }
            if (binding.viewpager.getAdapter().instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem()) instanceof DisplayVideosFragment) {
                ((DisplayVideosFragment) binding.viewpager.getAdapter().instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem())).scrollToTop();
            } else if (binding.viewpager.getAdapter().instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem()) instanceof DisplayOverviewFragment) {
                ((DisplayOverviewFragment) binding.viewpager.getAdapter().instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem())).scrollToTop();
            }
        });

        setTitleCustom(R.string.title_home);


        peertubeInformation = new PeertubeInformation();
        peertubeInformation.setCategories(new LinkedHashMap<>());
        peertubeInformation.setLanguages(new LinkedHashMap<>());
        peertubeInformation.setLicences(new LinkedHashMap<>());
        peertubeInformation.setPrivacies(new LinkedHashMap<>());
        peertubeInformation.setPlaylistPrivacies(new LinkedHashMap<>());
        peertubeInformation.setTranslations(new LinkedHashMap<>());
        startInForeground();

        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.compareTo("playstore") == 0) {
            RateThisApp.onCreate(this);
            RateThisApp.showRateDialogIfNeeded(this);
        }

        boolean search_cast = sharedpreferences.getBoolean(getString(R.string.set_cast_choice), false);
        if (search_cast) {
            super.discoverCast();
        }

        //Instance
        if (HelperInstance.getLiveInstance(PeertubeMainActivity.this) == null) {
            getSupportFragmentManager().setFragmentResultListener(PICK_INSTANCE, this, (requestKey, result) -> {
                String instance = result.getString(INSTANCE_ADDRESS, null);
                if (instance != null) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(PREF_USER_INSTANCE, instance);
                    editor.commit();
                    PeertubeMainActivity.this.recreate();
                }
                getSupportFragmentManager().clearFragmentResultListener(requestKey);
            });
            addFragment(
                    getSupportFragmentManager(), android.R.id.content, new FragmentLoginPickInstancePeertube(),
                    null, null, FragmentLoginPickInstancePeertube.class.getName());
        }
        mamageNewIntent(PeertubeMainActivity.this, getIntent());
        fetchRecentAccounts(PeertubeMainActivity.this, headerMainBinding);

    }

    public DisplayVideosFragment getSubscriptionFragment() {
        return subscriptionFragment;
    }

    private void startInForeground() {
        Intent notificationIntent = new Intent(this, RetrieveInfoService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationIntent);
        } else {
            startService(notificationIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    private void refreshToken() {
        new Thread(() -> {
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeMainActivity.this);
            String tokenStr = Helper.getToken(PeertubeMainActivity.this);
            String instance = HelperInstance.getLiveInstance(PeertubeMainActivity.this);
            String instanceShar = sharedpreferences.getString(PREF_USER_INSTANCE, null);
            String userIdShar = sharedpreferences.getString(PREF_USER_ID, null);
            BaseAccount account = null;
            try {
                account = new Account(PeertubeMainActivity.this).getAccountByToken(tokenStr);
            } catch (DBException e) {
                e.printStackTrace();
            }
            if (account == null) {

                try {
                    account = new Account(PeertubeMainActivity.this).getUniqAccount(userIdShar, instanceShar);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
            if (account != null) {
                BaseAccount finalAccount = account;
                OauthParams oauthParams = new OauthParams();
                oauthParams.setGrant_type("refresh_token");
                oauthParams.setClient_id(account.client_id);
                oauthParams.setClient_secret(account.client_secret);
                oauthParams.setRefresh_token(account.refresh_token);
                oauthParams.setAccess_token(account.token);
                try {
                    Token token = new RetrofitPeertubeAPI(PeertubeMainActivity.this).manageToken(oauthParams);
                    if (token == null) {
                        return;
                    }

                    runOnUiThread(() -> {
                        //To avoid a token issue with subscriptions, adding fragment is done when the token is refreshed.
                        new Handler().post(() -> {
                            if (Helper.isLoggedIn()) {
                                PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                                binding.viewpager.setAdapter(mPagerAdapter);
                            }
                        });
                    });

                    userMe = new RetrofitPeertubeAPI(PeertubeMainActivity.this, instance, token.getAccess_token()).verifyCredentials();
                    account.token = token.getAccess_token();
                    account.refresh_token = token.getRefresh_token();
                    account.peertube_account = userMe.getAccount();
                    account.software = Account.API.PEERTUBE.name();
                    account.user_id = userMe.getAccount().getId();
                    account.instance = userMe.getAccount().getHost();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(PREF_USER_TOKEN, token.getAccess_token());
                    editor.putString(PREF_USER_SOFTWARE, account.software);
                    editor.apply();
                    if (userMe != null && userMe.getAccount() != null) {
                        account.peertube_account = userMe.getAccount();
                        try {
                            new Account(PeertubeMainActivity.this).insertOrUpdate(account);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                        BaseAccount finalAccount1 = account;
                        runOnUiThread(() -> {
                            app.fedilab.android.mastodon.helper.Helper.loadPP(this, binding.profilePicture, finalAccount1);
                            MenuItem accountItem = binding.drawerNavView.getMenu().findItem(R.id.action_account);
                            FrameLayout rootView = (FrameLayout) accountItem.getActionView();
                            FrameLayout redCircle = rootView.findViewById(R.id.view_alert_red_circle);
                            TextView countTextView = rootView.findViewById(R.id.view_alert_count_textview);
                            //change counter for notifications
                            if (badgeCount > 0) {
                                countTextView.setText(String.valueOf(badgeCount));
                                redCircle.setVisibility(View.VISIBLE);
                            } else {
                                redCircle.setVisibility(View.GONE);
                            }
                            TooltipCompat.setTooltipText(accountItem.getActionView(), getText(R.string.account));

                            switch (typeOfConnection) {
                                case NORMAL:
                                    accountItem.setVisible(true);
                                    break;
                                case SURFING:
                                    accountItem.setVisible(false);
                                    break;
                            }
                            binding.profilePicture.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
                        });
                        editor.putString(PREF_USER_ID, account.user_id);
                        editor.putBoolean(getString(R.string.set_autoplay_choice), userMe.isAutoPlayVideo());
                        editor.putBoolean(getString(R.string.set_store_in_history), userMe.isVideosHistoryEnabled());
                        editor.putBoolean(getString(R.string.set_autoplay_next_video_choice), userMe.isAutoPlayNextVideo());
                        editor.putString(getString(R.string.set_video_sensitive_choice), userMe.getNsfwPolicy());
                        //Sync languages from server
                        List<String> videoLanguageServer = userMe.getVideoLanguages();
                        if (videoLanguageServer != null) {
                            Set<String> videoLanguageServerSet = new TreeSet<>(videoLanguageServer);
                            videoLanguageServerSet.addAll(videoLanguageServer);
                            Set<String> videoLanguageLocal = sharedpreferences.getStringSet(getString(R.string.set_video_language_choice), null);
                            if (videoLanguageServerSet.size() > 0 && videoLanguageLocal != null) {
                                videoLanguageServer.addAll(videoLanguageLocal);
                            }
                            editor.putStringSet(getString(R.string.set_video_language_choice), videoLanguageServerSet);
                            editor.apply();
                        }
                    }
                    instanceConfig = new RetrofitPeertubeAPI(PeertubeMainActivity.this).getConfigInstance();
                } catch (Error error) {
                    runOnUiThread(() -> {
                        AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(this);
                        alt_bld.setTitle(R.string.refresh_token_failed);
                        alt_bld.setMessage(R.string.refresh_token_failed_message);
                        alt_bld.setNegativeButton(R.string.action_logout, (dialog, id) -> {
                            dialog.dismiss();
                            Helper.logoutCurrentUser(PeertubeMainActivity.this, finalAccount);
                        });
                        alt_bld.setPositiveButton(R.string._retry, (dialog, id) -> {
                            dialog.dismiss();
                            refreshToken();
                        });
                        AlertDialog alert = alt_bld.create();
                        alert.show();

                    });
                    error.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu_peertube, menu);


        MenuItem incognitoItem = menu.findItem(R.id.action_incognito);
        switch (typeOfConnection) {
            case NORMAL:
                if (Helper.isLoggedIn()) {
                    incognitoItem.setVisible(true);
                    final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeMainActivity.this);
                    boolean checked = sharedpreferences.getBoolean(getString(R.string.set_store_in_history), true);
                    incognitoItem.setChecked(checked);
                } else {
                    incognitoItem.setVisible(false);
                }
                break;
            case SURFING:
                incognitoItem.setVisible(false);
                break;
        }

        MenuItem myActionMenuItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Pattern link = Pattern.compile("(https?://[\\da-z.-]+\\.[a-z.]{2,10})/videos/watch/(\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})(\\?start=(\\d+[hH])?(\\d+[mM])?(\\d+[sS])?)?$");
                Matcher matcherLink = link.matcher(query.trim());
                if (matcherLink.find()) {
                    Intent intent = new Intent(PeertubeMainActivity.this, PeertubeActivity.class);
                    intent.setData(Uri.parse(query.trim()));
                    startActivity(intent);
                    myActionMenuItem.collapseActionView();
                    return false;
                }
                Intent intent = new Intent(PeertubeMainActivity.this, SearchActivity.class);
                Bundle b = new Bundle();
                String search = query.trim();
                b.putString("search", search);
                intent.putExtras(b);
                startActivity(intent);
                if (!searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                myActionMenuItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_instance) {
            Intent intent = new Intent(PeertubeMainActivity.this, ManageInstancesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        } else if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(PeertubeMainActivity.this, AboutActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_incognito) {
            item.setChecked(!item.isChecked());
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeMainActivity.this);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(getString(R.string.set_store_in_history), item.isChecked());
            editor.apply();
            new Thread(() -> {
                UserSettings userSettings = new UserSettings();
                userSettings.setVideosHistoryEnabled(item.isChecked());
                try {
                    RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(PeertubeMainActivity.this);
                    api.updateUser(userSettings);
                } catch (Exception | Error e) {
                    e.printStackTrace();
                }
            }).start();
            return false;
        }
        return true;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null)
            return;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(Helper.INTENT_ACTION)) {
            if (extras.getInt(Helper.INTENT_ACTION) == Helper.ADD_USER_INTENT) {
                recreate();
            }
        }
        mamageNewIntent(PeertubeMainActivity.this, intent);
    }

    /*public static void mamageNewIntent(Activity activity,
                                       Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        if (action != null && action.equalsIgnoreCase("app.fedilab.android.shorcut.compose")) {
            CrossActionHelper.doCrossAction(activity, CrossActionHelper.TypeOfCrossAction.COMPOSE, null, null);
        }
    }*/

    @SuppressLint("ApplySharedPref")
    private void showRadioButtonDialog() {

        AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(this);
        alt_bld.setTitle(R.string.instance_choice);
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeMainActivity.this);
        String acad = HelperInstance.getLiveInstance(PeertubeMainActivity.this);
        int i = 0;
        List<AcadInstances> acadInstances = AcadInstances.getInstances();
        String[] academiesKey = new String[acadInstances.size()];
        String[] academiesValue = new String[acadInstances.size()];
        int position = 0;
        for (AcadInstances ac : acadInstances) {
            academiesKey[i] = ac.getName();
            academiesValue[i] = ac.getUrl();
            if (ac.getUrl().compareTo(acad) == 0) {
                position = i;
            }
            i++;
        }

        alt_bld.setSingleChoiceItems(academiesKey, position, (dialog, item) -> {
            String newInstance = academiesValue[item];
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(PREF_USER_INSTANCE, newInstance);
            editor.commit();
            dialog.dismiss();
            recreate();
        });
        alt_bld.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        AlertDialog alert = alt_bld.create();
        alert.show();
    }


    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public enum TypeOfConnection {
        NORMAL,
        SURFING
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(final int position) {
            if (Helper.isLoggedIn()) {
                switch (position) {
                    case 0:
                        return locaFragment;
                    case 1:
                        return recentFragment;
                    case 2:
                        return trendingFragment;
                    case 3:
                        return overviewFragment;
                    case 4:
                        return subscriptionFragment;
                }
            } else {
                switch (position) {
                    case 0:
                        return locaFragment;
                    case 1:
                        return recentFragment;
                    case 2:
                        return trendingFragment;
                    case 3:
                        return overviewFragment;
                }
            }
            return overviewFragment;
        }

        @Override
        public int getCount() {
            if (Helper.isLoggedIn()) {
                return 5;
            } else {
                return 4;
            }
        }
    }
}