package app.fedilab.android.peertube.activities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static app.fedilab.android.mastodon.helper.Helper.PREF_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kobakei.ratethisapp.RateThisApp;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.AboutActivity;
import app.fedilab.android.activities.PeertubeBaseMainActivity;
import app.fedilab.android.databinding.ActivityMainPeertubeBinding;
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
import app.fedilab.android.peertube.client.entities.WellKnownNodeinfo;
import app.fedilab.android.peertube.fragment.DisplayOverviewFragment;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperAcadInstance;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.SwitchAccountHelper;
import app.fedilab.android.peertube.services.RetrieveInfoService;
import app.fedilab.android.peertube.sqlite.StoredInstanceDAO;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import app.fedilab.android.sqlite.Sqlite;
import es.dmoral.toasty.Toasty;


public class PeertubeMainActivity extends PeertubeBaseMainActivity {


    public static int PICK_INSTANCE = 5641;
    public static int PICK_INSTANCE_SURF = 5642;
    public static UserMe userMe;
    public static InstanceData.InstanceConfig instanceConfig;
    public static TypeOfConnection typeOfConnection;
    public static int badgeCount;
    private DisplayVideosFragment recentFragment, locaFragment, trendingFragment, subscriptionFragment, mostLikedFragment;
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

    @SuppressLint("ApplySharedPref")
    public static void showRadioButtonDialogFullInstances(Activity activity, boolean storeInDb) {
        final SharedPreferences sharedpreferences = activity.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(activity);
        alt_bld.setTitle(R.string.instance_choice);
        String instance = HelperInstance.getLiveInstance(activity);
        final EditText input = new EditText(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alt_bld.setView(input);
        input.setText(instance);
        alt_bld.setPositiveButton(R.string.validate,
                (dialog, which) -> new Thread(() -> {
                    try {
                        String newInstance = input.getText().toString().trim();
                        if (!newInstance.startsWith("http")) {
                            newInstance = "http://" + newInstance;
                        }
                        URL url = new URL(newInstance);
                        newInstance = url.getHost();

                        WellKnownNodeinfo.NodeInfo instanceNodeInfo = new RetrofitPeertubeAPI(activity, newInstance, null).getNodeInfo();
                        if (instanceNodeInfo.getSoftware() != null && instanceNodeInfo.getSoftware().getName().trim().toLowerCase().compareTo("peertube") == 0) {
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(PREF_INSTANCE, newInstance);
                            editor.commit();
                            if (storeInDb) {
                                newInstance = newInstance.trim().toLowerCase();
                                InstanceData.AboutInstance aboutInstance = new RetrofitPeertubeAPI(activity, newInstance, null).getAboutInstance();
                                SQLiteDatabase db = Sqlite.getInstance(activity.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                                new StoredInstanceDAO(activity, db).insertInstance(aboutInstance, newInstance);
                                activity.runOnUiThread(() -> {
                                    dialog.dismiss();
                                    Helper.logoutNoRemoval(activity);
                                });
                            } else {
                                activity.runOnUiThread(() -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(activity, PeertubeMainActivity.class);
                                    activity.startActivity(intent);
                                });
                            }
                        } else {
                            activity.runOnUiThread(() -> Toasty.error(activity, activity.getString(R.string.not_valide_instance), Toast.LENGTH_LONG).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }).start());
        alt_bld.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        alt_bld.setNeutralButton(R.string.help, (dialog, which) -> {
            Intent intent = new Intent(activity, InstancePickerActivity.class);
            if (storeInDb) {
                activity.startActivityForResult(intent, PICK_INSTANCE_SURF);
            } else {
                activity.startActivityForResult(intent, PICK_INSTANCE);
            }
        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = super.binding;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        typeOfConnection = TypeOfConnection.UNKNOWN;
        badgeCount = 0;

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

        mostLikedFragment = new DisplayVideosFragment();
        bundle = new Bundle();
        bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.MOST_LIKED);
        mostLikedFragment.setArguments(bundle);

        overviewFragment = new DisplayOverviewFragment();
        if (!Helper.isLoggedIn(PeertubeMainActivity.this)) {
            PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
            binding.viewpager.setAdapter(mPagerAdapter);
        } else {
            new Thread(() -> {
                badgeCount = new RetrofitPeertubeAPI(PeertubeMainActivity.this).unreadNotifications();
                invalidateOptionsMenu();
            }).start();
        }
        if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
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

        setTitleCustom(R.string.title_discover);

        if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
            binding.navView.inflateMenu(R.menu.bottom_nav_menu_connected_peertube);
            refreshToken();

        } else {
            binding.navView.inflateMenu(R.menu.bottom_nav_menu);
        }
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


        final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        int search_cast = sharedpreferences.getInt(getString(R.string.set_cast_choice), 0);
        if (search_cast == 1) {
            super.discoverCast();
        }

        //Instance
        if (HelperInstance.getLiveInstance(PeertubeMainActivity.this) == null) {
            Intent intent = new Intent(PeertubeMainActivity.this, InstancePickerActivity.class);
            startActivityForResult(intent, PICK_INSTANCE);
        }
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
            final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
            String tokenStr = Helper.getToken(PeertubeMainActivity.this);
            String instance = HelperInstance.getLiveInstance(PeertubeMainActivity.this);
            SQLiteDatabase db = Sqlite.getInstance(getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
            String instanceShar = sharedpreferences.getString(PREF_INSTANCE, null);
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
                            if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
                                PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                                binding.viewpager.setAdapter(mPagerAdapter);
                            }
                        });
                    });

                    userMe = new RetrofitPeertubeAPI(PeertubeMainActivity.this, instance, token.getAccess_token()).verifyCredentials();
                    if (userMe != null && userMe.getAccount() != null) {
                        account.peertube_account = userMe.getAccount();
                        try {
                            new Account(PeertubeMainActivity.this).insertOrUpdate(account);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                        SharedPreferences.Editor editor = sharedpreferences.edit();
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
                        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
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

        MenuItem uploadItem = menu.findItem(R.id.action_upload);
        MenuItem myVideosItem = menu.findItem(R.id.action_myvideos);
        MenuItem playslistItem = menu.findItem(R.id.action_playlist);
        MenuItem historyItem = menu.findItem(R.id.action_history);
        MenuItem mostLikedItem = menu.findItem(R.id.action_most_liked);
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        MenuItem sepiaSearchItem = menu.findItem(R.id.action_sepia_search);
        MenuItem incognitoItem = menu.findItem(R.id.action_incognito);
        MenuItem accountItem = menu.findItem(R.id.action_account);
        MenuItem changeInstanceItem = menu.findItem(R.id.action_change_instance);

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
            case UNKNOWN:
                accountItem.setVisible(false);
                uploadItem.setVisible(false);
                myVideosItem.setVisible(false);
                playslistItem.setVisible(false);
                historyItem.setVisible(false);
                settingsItem.setVisible(false);
                mostLikedItem.setVisible(false);
                incognitoItem.setVisible(false);
                break;
            case REMOTE_ACCOUNT:
            case NORMAL:
                accountItem.setVisible(true);
                if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
                    uploadItem.setVisible(true);
                    myVideosItem.setVisible(true);
                    playslistItem.setVisible(true);
                    historyItem.setVisible(true);
                    settingsItem.setVisible(false);
                    mostLikedItem.setVisible(true);
                    incognitoItem.setVisible(true);
                    final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
                    boolean checked = sharedpreferences.getBoolean(getString(R.string.set_store_in_history), true);
                    incognitoItem.setChecked(checked);
                } else {
                    uploadItem.setVisible(false);
                    myVideosItem.setVisible(false);
                    playslistItem.setVisible(false);
                    historyItem.setVisible(false);
                    settingsItem.setVisible(true);
                    mostLikedItem.setVisible(true);
                    incognitoItem.setVisible(false);
                }
                break;
            case SURFING:
                accountItem.setVisible(true);
                uploadItem.setVisible(false);
                myVideosItem.setVisible(false);
                playslistItem.setVisible(false);
                historyItem.setVisible(false);
                settingsItem.setVisible(false);
                mostLikedItem.setVisible(false);
                incognitoItem.setVisible(false);
                break;
        }

        return true;
    }




    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem accountItem = menu.findItem(R.id.action_account);
        FrameLayout rootView = (FrameLayout) accountItem.getActionView();
        rootView.setOnClickListener(v -> onOptionsItemSelected(accountItem));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String type = null;
        String action = "TIMELINE";
        if (item.getItemId() == R.id.action_change_instance) {
            Intent intent = new Intent(PeertubeMainActivity.this, ManageInstancesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            action = "CHANGE_INSTANCE";
            type = "";
        } else if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(PeertubeMainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_account) {
            Intent intent;
            if (typeOfConnection == TypeOfConnection.SURFING) {
                SwitchAccountHelper.switchDialog(PeertubeMainActivity.this, false);
            } else {
                if (Helper.canMakeAction(PeertubeMainActivity.this)) {
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
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", TimelineVM.TimelineType.MY_VIDEOS);
            intent.putExtras(bundle);
            startActivity(intent);
            type = HelperAcadInstance.MYVIDEOS;
        } else if (item.getItemId() == R.id.action_history) {
            Intent intent = new Intent(PeertubeMainActivity.this, VideosTimelineActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", TimelineVM.TimelineType.HISTORY);
            intent.putExtras(bundle);
            startActivity(intent);
            type = HelperAcadInstance.HISTORY;
        } else if (item.getItemId() == R.id.action_most_liked) {
            Intent intent = new Intent(PeertubeMainActivity.this, VideosTimelineActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", TimelineVM.TimelineType.MOST_LIKED);
            intent.putExtras(bundle);
            startActivity(intent);
            type = HelperAcadInstance.MOSTLIKED;
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
        } else if (item.getItemId() == R.id.action_incognito) {
            item.setChecked(!item.isChecked());
            final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
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
    }


    @SuppressLint("ApplySharedPref")
    private void showRadioButtonDialog() {

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle(R.string.instance_choice);
        final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
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
            editor.putString(PREF_INSTANCE, newInstance);
            editor.commit();
            dialog.dismiss();
            recreate();
        });
        alt_bld.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_INSTANCE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(PREF_INSTANCE, String.valueOf(data.getData()));
                editor.commit();
                recreate();
            }
        }
    }

    public enum TypeOfConnection {
        UNKNOWN,
        NORMAL,
        SURFING,
        REMOTE_ACCOUNT,
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(final int position) {
            if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
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
            if (Helper.isLoggedIn(PeertubeMainActivity.this)) {
                return 5;
            } else {
                return 4;
            }
        }
    }
}