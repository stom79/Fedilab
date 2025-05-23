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
import static app.fedilab.android.mastodon.helper.CacheHelper.deleteDir;
import static app.fedilab.android.mastodon.helper.Helper.ARG_REFRESH_NOTFICATION;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.mastodon.helper.Helper.displayReleaseNotesIfNeeded;
import static app.fedilab.android.mastodon.helper.ThemeHelper.fetchAccentColor;
import static app.fedilab.android.mastodon.ui.drawer.StatusAdapter.sendAction;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.typeOfConnection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.MatrixCursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.multidex.BuildConfig;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.avatarfirst.avatargenlib.AvatarGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.IDN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.activities.AboutActivity;
import app.fedilab.android.activities.LoginActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityMainBinding;
import app.fedilab.android.databinding.NavHeaderMainBinding;
import app.fedilab.android.mastodon.activities.ActionActivity;
import app.fedilab.android.mastodon.activities.AnnouncementActivity;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.mastodon.activities.CacheActivity;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.activities.DirectoryActivity;
import app.fedilab.android.mastodon.activities.DraftActivity;
import app.fedilab.android.mastodon.activities.FilterActivity;
import app.fedilab.android.mastodon.activities.FollowRequestActivity;
import app.fedilab.android.mastodon.activities.FollowedTagActivity;
import app.fedilab.android.mastodon.activities.InstanceActivity;
import app.fedilab.android.mastodon.activities.InstanceHealthActivity;
import app.fedilab.android.mastodon.activities.MastodonListActivity;
import app.fedilab.android.mastodon.activities.PartnerShipActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.activities.ProxyActivity;
import app.fedilab.android.mastodon.activities.ReorderTimelinesActivity;
import app.fedilab.android.mastodon.activities.ScheduledActivity;
import app.fedilab.android.mastodon.activities.SearchResultTabActivity;
import app.fedilab.android.mastodon.activities.SettingsActivity;
import app.fedilab.android.mastodon.activities.SuggestionActivity;
import app.fedilab.android.mastodon.activities.TrendsActivity;
import app.fedilab.android.mastodon.activities.admin.AdminActionActivity;
import app.fedilab.android.mastodon.broadcastreceiver.NetworkStateReceiver;
import app.fedilab.android.mastodon.client.endpoints.MastodonAccountsService;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.EmojiInstance;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.client.entities.api.Instance;
import app.fedilab.android.mastodon.client.entities.api.MastodonList;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.BottomMenu;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.MutedAccounts;
import app.fedilab.android.mastodon.client.entities.app.Pinned;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.client.entities.app.TimelineCacheLogs;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.CrossActionHelper;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.PinnedTimelineHelper;
import app.fedilab.android.mastodon.helper.PushHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.ui.drawer.AccountsSearchTopBarAdapter;
import app.fedilab.android.mastodon.ui.drawer.TagSearchTopBarAdapter;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentNotificationContainer;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.FiltersVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.InstancesVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.TopBarVM;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class BaseMainActivity extends BaseActivity implements NetworkStateReceiver.NetworkStateReceiverListener, FragmentMastodonTimeline.UpdateCounters, FragmentNotificationContainer.UpdateCounters, FragmentMastodonConversation.UpdateCounters {

    private static final int REQUEST_CODE = 5415;
    public static String currentInstance, currentToken, currentUserID, client_id, client_secret, software;
    public static HashMap<String, List<Emoji>> emojis = new HashMap<>();
    public static Account.API api;
    public static boolean admin;
    public static status networkAvailable = UNKNOWN;
    public static Instance instanceInfo;
    public static List<Filter> mainFilters;
    public static List<app.fedilab.android.mastodon.client.entities.api.Account> filteredAccounts;
    public static boolean filterFetched;
    public static boolean show_boosts, show_replies, show_dms, show_art_nsfw, show_self_boosts, show_self_replies, show_my_messages;
    public static String regex_home, regex_local, regex_public;
    public static iconLauncher mLauncher = iconLauncher.BUBBLES;
    public static boolean headerMenuOpen;
    public static int currentNightMode;
    Fragment currentFragment;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private final BroadcastReceiver broadcast_error_message = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            Bundle args = intent.getExtras();
            if (args != null) {
                long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
                new CachedBundle(BaseMainActivity.this).getBundle(bundleId, Helper.getCurrentAccount(BaseMainActivity.this), bundle -> {
                    if (bundle.getBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, false)) {
                        String errorMessage = bundle.getString(Helper.RECEIVE_ERROR_MESSAGE);
                        StatusDraft statusDraft = (StatusDraft) bundle.getSerializable(Helper.ARG_STATUS_DRAFT);
                        Snackbar snackbar = Snackbar.make(binding.getRoot(), errorMessage, 5000);
                        View snackbarView = snackbar.getView();
                        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                        textView.setMaxLines(5);
                        snackbar
                                .setAction(getString(R.string.open_draft), view -> {
                                    Intent intentCompose = new Intent(context, ComposeActivity.class);
                                    Bundle args2 = new Bundle();
                                    args2.putSerializable(Helper.ARG_STATUS_DRAFT, statusDraft);
                                    new CachedBundle(BaseMainActivity.this).insertBundle(args2, Helper.getCurrentAccount(BaseMainActivity.this), bundleId2 -> {
                                        Bundle bundle2 = new Bundle();
                                        bundle2.putLong(Helper.ARG_INTENT_ID, bundleId2);
                                        intentCompose.putExtras(bundle2);
                                        intentCompose.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intentCompose);
                                    });
                                })
                                .show();
                    }
                });

            }
        }
    };
    private Pinned pinned;
    private BottomMenu bottomMenu;
    private final BroadcastReceiver broadcast_data = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle args = intent.getExtras();
            if (args != null) {
                long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
                new CachedBundle(BaseMainActivity.this).getBundle(bundleId, Helper.getCurrentAccount(BaseMainActivity.this), bundle -> {
                    if (bundle.getBoolean(Helper.RECEIVE_REDRAW_TOPBAR, false)) {
                        List<MastodonList> mastodonLists = (List<MastodonList>) bundle.getSerializable(Helper.RECEIVE_MASTODON_LIST);
                        redrawPinned(mastodonLists);
                    }
                    if (bundle.getBoolean(Helper.RECEIVE_REDRAW_BOTTOM, false)) {
                        bottomMenu = new BottomMenu(BaseMainActivity.this).hydrate(Helper.getCurrentAccount(BaseMainActivity.this), binding.bottomNavView);
                        if (bottomMenu != null) {
                            //ManageClick on bottom menu items
                            if (binding.bottomNavView.findViewById(R.id.nav_home) != null) {
                                binding.bottomNavView.findViewById(R.id.nav_home).setOnLongClickListener(view -> {
                                    int position = BottomMenu.getPosition(bottomMenu, R.id.nav_home);
                                    if (position >= 0) {
                                        manageFilters(position);
                                    }
                                    return false;
                                });
                            }
                            if (binding.bottomNavView.findViewById(R.id.nav_local) != null) {
                                binding.bottomNavView.findViewById(R.id.nav_local).setOnLongClickListener(view -> {
                                    int position = BottomMenu.getPosition(bottomMenu, R.id.nav_local);
                                    if (position >= 0) {
                                        manageFilters(position);
                                    }
                                    return false;
                                });
                            }
                            if (binding.bottomNavView.findViewById(R.id.nav_public) != null) {
                                binding.bottomNavView.findViewById(R.id.nav_public).setOnLongClickListener(view -> {
                                    int position = BottomMenu.getPosition(bottomMenu, R.id.nav_public);
                                    if (position >= 0) {
                                        manageFilters(position);
                                    }
                                    return false;
                                });
                            }
                            binding.bottomNavView.setOnItemSelectedListener(item -> {
                                int itemId = item.getItemId();
                                int position = BottomMenu.getPosition(bottomMenu, itemId);
                                if (position >= 0) {
                                    if (binding.viewPager.getCurrentItem() == position) {
                                        scrollToTop();
                                    } else {
                                        binding.viewPager.setCurrentItem(position, false);
                                    }
                                }
                                return true;
                            });
                        }
                    } else if (bundle.getBoolean(Helper.RECEIVE_RECREATE_ACTIVITY, false)) {
                        recreate();
                    } else if (bundle.getBoolean(Helper.RECEIVE_NEW_MESSAGE, false)) {
                        Status statusSent = (Status) bundle.getSerializable(Helper.RECEIVE_STATUS_ACTION);
                        String statusEditId = bundle.getString(Helper.ARG_EDIT_STATUS_ID, null);
                        Snackbar.make(binding.displaySnackBar, getString(R.string.message_has_been_sent), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.display), view -> {
                                    Intent intentContext = new Intent(BaseMainActivity.this, ContextActivity.class);
                                    Bundle args2 = new Bundle();
                                    args2.putSerializable(Helper.ARG_STATUS, statusSent);
                                    new CachedBundle(BaseMainActivity.this).insertBundle(args2, Helper.getCurrentAccount(BaseMainActivity.this), bundleId2 -> {
                                        Bundle bundle2 = new Bundle();
                                        bundle2.putLong(Helper.ARG_INTENT_ID, bundleId2);
                                        intentContext.putExtras(bundle2);
                                        intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intentContext);
                                    });
                                })
                                .show();
                        //The message was edited, we need to update the timeline
                        if (statusEditId != null) {
                            //Update message in cache
                            new Thread(() -> {
                                StatusCache statusCache = new StatusCache();
                                statusCache.instance = BaseMainActivity.currentInstance;
                                statusCache.user_id = BaseMainActivity.currentUserID;
                                statusCache.status = statusSent;
                                statusCache.status_id = statusEditId;
                                try {
                                    new StatusCache(BaseMainActivity.this).updateIfExists(statusCache);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                            //Update timelines
                            sendAction(context, Helper.ARG_STATUS_UPDATED, statusSent, null);
                        }
                    }
                });
            }
        }
    };
    private NetworkStateReceiver networkStateReceiver;

    SharedPreferences sharedpreferences;

    public static void fetchRecentAccounts(Activity activity, NavHeaderMainBinding headerMainBinding) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        //Fetch some db values to initialize data
        new Thread(() -> {
            try {
                if (Helper.getCurrentAccount(activity) == null) {
                    if (currentToken == null || currentToken.trim().isEmpty()) {
                        currentToken = sharedpreferences.getString(Helper.PREF_USER_TOKEN, null);
                    }
                    try {
                        Helper.setCurrentAccount(new Account(activity).getConnectedAccount());
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
                if (Helper.getCurrentAccount(activity) != null) {
                    MutedAccounts mutedAccounts = new MutedAccounts(activity).getMutedAccount(Helper.getCurrentAccount(activity));
                    if (mutedAccounts != null && mutedAccounts.accounts != null) {
                        filteredAccounts = mutedAccounts.accounts;
                    }
                }
                //Delete cache older than 7 days
                new StatusCache(activity).deleteForAllAccountAfter7Days();
                new TimelineCacheLogs(activity).deleteForAllAccountAfter7Days();
                new CachedBundle(activity).deleteOldIntent();
            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();

        //Fetch recent used accounts
        new Thread(() -> {
            try {
                List<BaseAccount> accounts = new Account(activity).getLastUsedAccounts();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (accounts != null && accounts.size() > 0) {
                        Helper.loadPP(activity, headerMainBinding.otherAccount1, accounts.get(0));
                        headerMainBinding.otherAccount1.setVisibility(View.VISIBLE);
                        headerMainBinding.otherAccount1.setOnClickListener(v -> {
                            headerMenuOpen = false;
                            String account = "";
                            if (accounts.get(0).mastodon_account != null) {
                                account = "@" + accounts.get(0).mastodon_account.acct + "@" + accounts.get(0).instance;
                            } else if (accounts.get(0).peertube_account != null) {
                                account = "@" + accounts.get(0).peertube_account.getAcct() + "@" + accounts.get(0).instance;
                            }
                            typeOfConnection = PeertubeMainActivity.TypeOfConnection.NORMAL;
                            Toasty.info(activity, activity.getString(R.string.toast_account_changed, account), Toasty.LENGTH_LONG).show();
                            BaseMainActivity.currentToken = accounts.get(0).token;
                            BaseMainActivity.currentUserID = accounts.get(0).user_id;
                            BaseMainActivity.currentInstance = accounts.get(0).instance;
                            api = accounts.get(0).api;
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(PREF_USER_ID, accounts.get(0).user_id);
                            editor.putString(PREF_USER_TOKEN, accounts.get(0).token);
                            editor.putString(PREF_USER_INSTANCE, accounts.get(0).instance);
                            editor.putString(PREF_USER_SOFTWARE, accounts.get(0).software);
                            editor.commit();
                            //The user is now aut
                            //The user is now authenticated, it will be redirected to MainActivity
                            Intent mainActivity = new Intent(activity, MainActivity.class);
                            activity.startActivity(mainActivity);
                            activity.finish();
                        });
                        if (accounts.size() > 1) {
                            Helper.loadPP(activity, headerMainBinding.otherAccount2, accounts.get(1));
                            headerMainBinding.otherAccount2.setVisibility(View.VISIBLE);
                            headerMainBinding.otherAccount2.setOnClickListener(v -> {
                                headerMenuOpen = false;
                                String account = "";
                                if (accounts.get(1).mastodon_account != null) {
                                    account = "@" + accounts.get(1).mastodon_account.acct + "@" + accounts.get(1).instance;
                                } else if (accounts.get(1).peertube_account != null) {
                                    account = "@" + accounts.get(1).peertube_account.getAcct() + "@" + accounts.get(1).instance;
                                }
                                Toasty.info(activity, activity.getString(R.string.toast_account_changed, account), Toasty.LENGTH_LONG).show();
                                BaseMainActivity.currentToken = accounts.get(1).token;
                                BaseMainActivity.currentUserID = accounts.get(1).user_id;
                                BaseMainActivity.currentInstance = accounts.get(1).instance;
                                api = accounts.get(1).api;
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(PREF_USER_ID, accounts.get(1).user_id);
                                editor.putString(PREF_USER_TOKEN, accounts.get(1).token);
                                editor.putString(PREF_USER_SOFTWARE, accounts.get(1).software);
                                editor.putString(PREF_USER_INSTANCE, accounts.get(1).instance);
                                editor.commit();
                                //The user is now aut
                                //The user is now authenticated, it will be redirected to MainActivity
                                Intent mainActivity = new Intent(activity, MainActivity.class);
                                activity.startActivity(mainActivity);
                                activity.finish();
                            });
                        }
                    }
                };
                mainHandler.post(myRunnable);

            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void manageDrawerMenu(Activity activity, NavigationView navigationView, NavHeaderMainBinding headerMainBinding) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        if (headerMenuOpen) {
            headerMainBinding.ownerAccounts.setIconResource(R.drawable.ic_baseline_arrow_drop_up_24);
            new Thread(() -> {
                try {
                    List<BaseAccount> accounts = new Account(activity).getOtherAccounts();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        navigationView.getMenu().clear();
                        navigationView.inflateMenu(R.menu.menu_accounts);
                        headerMenuOpen = true;

                        Menu mainMenu = navigationView.getMenu();
                        SubMenu currentSubmenu = null;
                        String lastInstance = "";
                        if (accounts != null) {
                            for (final BaseAccount account : accounts) {
                                if (!currentToken.equalsIgnoreCase(account.token)) {
                                    if (!lastInstance.trim().equalsIgnoreCase(account.instance.trim())) {
                                        lastInstance = account.instance.toUpperCase();
                                        currentSubmenu = mainMenu.addSubMenu(account.instance.toUpperCase());
                                    }
                                    if (currentSubmenu == null) {
                                        continue;
                                    }
                                    String acct = "";
                                    String url = "";
                                    boolean disableGif = sharedpreferences.getBoolean(activity.getString(R.string.SET_DISABLE_GIF), false);
                                    if (account.mastodon_account != null) {
                                        acct = account.mastodon_account.acct;
                                        url = !disableGif ? account.mastodon_account.avatar : account.mastodon_account.avatar_static;
                                        if (url != null && url.startsWith("/")) {
                                            url = "https://" + account.instance + account.mastodon_account.avatar;
                                        }
                                    } else if (account.peertube_account != null) {
                                        acct = account.peertube_account.getAcct();
                                        if (account.peertube_account.getAvatar() != null) {
                                            url = account.peertube_account.getAvatar().getPath();
                                            if (url != null && url.startsWith("/")) {
                                                url = "https://" + account.instance + account.peertube_account.getAvatar().getPath();
                                            }
                                        }
                                    }

                                    final MenuItem item = currentSubmenu.add("@" + acct);
                                    item.setIcon(R.drawable.ic_person);
                                    if (!activity.isDestroyed() && !activity.isFinishing() && url != null) {
                                        if (url.trim().isEmpty()) {
                                            BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(activity)
                                                    .setLabel(acct)
                                                    .setAvatarSize(120)
                                                    .setTextSize(30)
                                                    .toSquare()
                                                    .setBackgroundColor(fetchAccentColor(activity))
                                                    .build();
                                            Glide.with(activity)
                                                    .asDrawable()
                                                    .load(avatar)
                                                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
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
                                        } else if (url.contains(".gif")) {
                                            Glide.with(activity)
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
                                            Glide.with(activity)
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
                                        if (!activity.isFinishing()) {
                                            headerMenuOpen = false;
                                            String acctForAccount = "";
                                            if (account.mastodon_account != null) {
                                                acctForAccount = "@" + account.mastodon_account.username + "@" + account.instance;
                                            } else if (account.peertube_account != null) {
                                                acctForAccount = "@" + account.peertube_account.getUsername() + "@" + account.instance;
                                            }
                                            typeOfConnection = PeertubeMainActivity.TypeOfConnection.NORMAL;
                                            Toasty.info(activity, activity.getString(R.string.toast_account_changed, acctForAccount), Toasty.LENGTH_LONG).show();
                                            BaseMainActivity.currentToken = account.token;
                                            BaseMainActivity.currentUserID = account.user_id;
                                            BaseMainActivity.currentInstance = account.instance;
                                            api = account.api;
                                            SharedPreferences.Editor editor = sharedpreferences.edit();
                                            editor.putString(PREF_USER_TOKEN, account.token);
                                            editor.putString(PREF_USER_SOFTWARE, account.software);
                                            editor.putString(PREF_USER_INSTANCE, account.instance);
                                            editor.putString(PREF_USER_ID, account.user_id);
                                            editor.commit();
                                            //The user is now aut
                                            //The user is now authenticated, it will be redirected to MainActivity

                                            Intent mainActivity = new Intent(activity, MainActivity.class);
                                            activity.startActivity(mainActivity);
                                            activity.finish();
                                            headerMainBinding.ownerAccounts.setIconResource(R.drawable.ic_accounts);
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
                            Intent intent = new Intent(activity, LoginActivity.class);
                            activity.startActivity(intent);
                            return true;
                        });

                    };
                    mainHandler.post(myRunnable);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            navigationView.getMenu().clear();
            if (Helper.getCurrentAccount(activity).mastodon_account != null) {
                navigationView.inflateMenu(R.menu.activity_main_drawer);
            } else if (Helper.getCurrentAccount(activity).peertube_account != null) {
                navigationView.inflateMenu(R.menu.activity_main_drawer_peertube);
            }
            headerMainBinding.ownerAccounts.setIconResource(R.drawable.ic_accounts);
            headerMenuOpen = false;
        }
    }

    public static void headerLogoutClick(Activity activity, NavHeaderMainBinding headerMainBinding, FragmentManager fragmentManager) {
        AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(activity);
        alt_bld.setTitle(R.string.action_logout);
        if (Helper.getCurrentAccount(activity).mastodon_account != null && Helper.getCurrentAccount(activity).instance != null) {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, Helper.getCurrentAccount(activity).mastodon_account.username, Helper.getCurrentAccount(activity).instance));
        } else if (Helper.getCurrentAccount(activity).peertube_account != null && Helper.getCurrentAccount(activity).instance != null) {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, Helper.getCurrentAccount(activity).peertube_account.getUsername(), Helper.getCurrentAccount(activity).instance));
        } else {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, "", ""));
        }
        alt_bld.setPositiveButton(R.string.action_logout, (dialog, id) -> {
            dialog.dismiss();
            try {
                Helper.removeAccount(activity);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });
        alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    /**
     * Open notifications tab when coming from a notification device
     *
     * @param intent - Intent intent that will be cancelled
     */
    private static void openNotifications(Activity activity, Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle args = intent.getExtras();
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(activity).getBundle(bundleId, Helper.getCurrentAccount(activity), bundle -> {
                app.fedilab.android.mastodon.client.entities.api.Account account = (app.fedilab.android.mastodon.client.entities.api.Account) bundle.getSerializable(Helper.INTENT_TARGETED_ACCOUNT);
                Status status = (Status) bundle.getSerializable(Helper.INTENT_TARGETED_STATUS);
                if (account != null) {
                    Intent intentAccount = new Intent(activity, ProfileActivity.class);
                    Bundle args2 = new Bundle();
                    args2.putSerializable(Helper.ARG_ACCOUNT, account);
                    new CachedBundle(activity).insertBundle(args2, Helper.getCurrentAccount(activity), bundleId2 -> {
                        Bundle bundleCached = new Bundle();
                        bundleCached.putLong(Helper.ARG_INTENT_ID, bundleId2);
                        intentAccount.putExtras(bundleCached);
                        intentAccount.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intentAccount);
                    });
                } else if (status != null) {
                    Intent intentContext = new Intent(activity, ContextActivity.class);
                    Bundle args2 = new Bundle();
                    args2.putSerializable(Helper.ARG_STATUS, status);
                    new CachedBundle(activity).insertBundle(args2, Helper.getCurrentAccount(activity), bundleId2 -> {
                        Bundle bundleCached = new Bundle();
                        bundleCached.putLong(Helper.ARG_INTENT_ID, bundleId2);
                        intentContext.putExtras(bundleCached);
                        intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intentContext);
                    });
                }
            });
        }
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
            BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottom_nav_view);
            TabLayout tabLayout = activity.findViewById(R.id.tabLayout);
            ViewPager viewPager = activity.findViewById(R.id.view_pager);

            if (bottomNavigationView != null && tabLayout != null && viewPager != null) {
                if (!singleBar) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_notifications);
                } else {
                    int position = 0;
                    for (int i = 0; i < tabLayout.getTabCount(); i++) {
                        TabLayout.Tab tab = tabLayout.getTabAt(i);
                        if (tab != null && tab.getTag() != null && tab.getTag().equals(Timeline.TimeLineEnum.NOTIFICATION.getValue())) {
                            break;
                        }
                        position++;
                    }
                    viewPager.setCurrentItem(position);
                }
                Bundle args = new Bundle();
                args.putBoolean(ARG_REFRESH_NOTFICATION, true);
                Intent intentBC = new Intent(Helper.RECEIVE_STATUS_ACTION);
                intentBC.setPackage(BuildConfig.APPLICATION_ID);
                new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intentBC.putExtras(bundle);
                    activity.sendBroadcast(intentBC);
                });

            }
        }, 1000);
        intent.removeExtra(Helper.INTENT_ACTION);
    }


    @SuppressLint("ApplySharedPref")
    public static void mamageNewIntent(Activity activity, Intent intent) {
        if (intent == null)
            return;
        String action = intent.getAction();
        String type = intent.getType();
        Bundle extras = intent.getExtras();
        String userIdIntent, instanceIntent, urlOfMessage;
        if (action != null && action.equalsIgnoreCase("app.fedilab.android.shorcut.compose")) {
            if (!activity.isFinishing()) {
                CrossActionHelper.doCrossAction(activity, CrossActionHelper.TypeOfCrossAction.COMPOSE, null, null);
                intent.replaceExtras(new Bundle());
                intent.setAction("");
                intent.setData(null);
                intent.setFlags(0);
                return;
            }
        }
        if (extras != null && extras.containsKey(Helper.INTENT_ACTION)) {
            userIdIntent = extras.getString(Helper.PREF_USER_ID); //Id of the account in the intent
            instanceIntent = extras.getString(Helper.PREF_USER_INSTANCE);
            urlOfMessage = extras.getString(Helper.PREF_MESSAGE_URL);
            if (extras.getInt(Helper.INTENT_ACTION) == Helper.NOTIFICATION_INTENT) {
                if (userIdIntent != null && instanceIntent != null && userIdIntent.equals(currentUserID) && instanceIntent.equals(currentInstance)) {
                    openNotifications(activity, intent);
                } else {
                    try {
                        BaseAccount account = new Account(activity).getUniqAccount(userIdIntent, instanceIntent);
                        if (account == null) {
                            return;
                        }
                        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                        headerMenuOpen = false;
                        String acct = "";
                        if (account.mastodon_account != null) {
                            acct = "@" + account.mastodon_account.username + "@" + account.instance;
                        } else if (account.peertube_account != null) {
                            acct = "@" + account.peertube_account.getUsername() + "@" + account.instance;
                        }
                        typeOfConnection = PeertubeMainActivity.TypeOfConnection.NORMAL;
                        Toasty.info(activity, activity.getString(R.string.toast_account_changed, acct), Toasty.LENGTH_LONG).show();
                        BaseMainActivity.currentToken = account.token;
                        BaseMainActivity.currentUserID = account.user_id;
                        ThemeHelper.applyThemeColor(activity);
                        api = account.api;
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(PREF_USER_TOKEN, account.token);
                        editor.putString(PREF_USER_SOFTWARE, account.software);
                        editor.commit();
                        Intent mainActivity = new Intent(activity, MainActivity.class);
                        mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_NOTIFICATION);
                        Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            mainActivity.putExtras(bundle);
                        }
                        activity.startActivity(mainActivity);
                        activity.finish();
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            } else if (extras.getInt(Helper.INTENT_ACTION) == Helper.OPEN_NOTIFICATION) {
                openNotifications(activity, intent);
            } else if (extras.getInt(Helper.INTENT_ACTION) == Helper.OPEN_WITH_ANOTHER_ACCOUNT) {
                CrossActionHelper.fetchRemoteStatus(activity, Helper.getCurrentAccount(activity), urlOfMessage, new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {
                        if (status != null) {
                            Intent intent = new Intent(activity, ContextActivity.class);
                            Bundle args = new Bundle();
                            args.putSerializable(Helper.ARG_STATUS, status);
                            new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intent.putExtras(bundle);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            });
                        }
                    }

                    @Override
                    public void federatedAccount(app.fedilab.android.mastodon.client.entities.api.Account account) {

                    }
                });
            }
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                final String[] url = {null};
                String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                //SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
                //boolean shouldRetrieveMetaData = sharedpreferences.getBoolean(getString(R.string.SET_RETRIEVE_METADATA_IF_URL_FROM_EXTERAL), true);
                if (sharedText != null) {
                    /* Some apps don't send the URL as the first part of the EXTRA_TEXT,
                        the BBC News app being one such, in this case find where the URL
                        is and strip that out into sharedText.
                     */
                    Matcher matcher;
                    matcher = Patterns.WEB_URL.matcher(sharedText);
                    int count = 0;
                    while (matcher.find()) {
                        int matchStart = matcher.start(1);
                        int matchEnd = matcher.end();
                        if (matchStart < matchEnd && sharedText.length() >= matchEnd) {
                            url[0] = sharedText.substring(matchStart, matchEnd);
                            count++;
                        }
                    }
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    boolean fetchSharedMedia = sharedpreferences.getBoolean(activity.getString(R.string.SET_RETRIEVE_METADATA_IF_URL_FROM_EXTERAL), true);
                    boolean fetchShareContent = sharedpreferences.getBoolean(activity.getString(R.string.SET_SHARE_DETAILS), true);
                    if (url[0] != null && count == 1 && (fetchShareContent || fetchSharedMedia)) {
                        String originalUrl = url[0];
                        new Thread(() -> {
                            if (!url[0].matches("^https?://.*")) url[0] = "http://" + url[0];
                            Matcher matcherPattern = Patterns.WEB_URL.matcher(url[0]);
                            String potentialUrl = null;
                            while (matcherPattern.find()) {
                                int matchStart = matcherPattern.start(1);
                                int matchEnd = matcherPattern.end();
                                if (matchStart < matchEnd && url[0].length() >= matchEnd)
                                    potentialUrl = url[0].substring(matchStart, matchEnd);
                            }
                            // If we actually have a URL then make use of it.
                            if (potentialUrl != null && potentialUrl.length() > 0) {


                                try {
                                    OkHttpClient client = new OkHttpClient.Builder()
                                            .connectTimeout(10, TimeUnit.SECONDS)
                                            .writeTimeout(10, TimeUnit.SECONDS)
                                            .proxy(Helper.getProxy(activity.getApplication().getApplicationContext()))
                                            .readTimeout(10, TimeUnit.SECONDS).build();
                                    Request request = new Request.Builder()
                                            .url(potentialUrl)
                                            .build();
                                    String finalPotentialUrl = potentialUrl;
                                    client.newCall(request).enqueue(new Callback() {
                                        @Override
                                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                            e.printStackTrace();
                                            activity.runOnUiThread(() -> Toasty.warning(activity, activity.getString(R.string.toast_error), Toast.LENGTH_LONG).show());
                                        }

                                        @Override
                                        public void onResponse(@NonNull Call call, @NonNull final Response response) {
                                            if (response.isSuccessful()) {
                                                try {
                                                    String data = response.body().string();
                                                    Document html = Jsoup.parse(data);

                                                    Element titleEl = html.selectFirst("meta[property='og:title']");
                                                    Element descriptionEl = html.selectFirst("meta[property='og:description']");
                                                    Element imageUrlEl = html.selectFirst("meta[property='og:image']");

                                                    String title = "";
                                                    String description = "";

                                                    if (titleEl != null) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            title = Html.fromHtml(titleEl.attr("content"), Html.FROM_HTML_MODE_LEGACY).toString();
                                                        } else {
                                                            title = Html.fromHtml(titleEl.attr("content")).toString();
                                                        }
                                                    }

                                                    if (descriptionEl != null) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            description = Html.fromHtml(descriptionEl.attr("content"), Html.FROM_HTML_MODE_LEGACY).toString();
                                                        } else {
                                                            description = Html.fromHtml(descriptionEl.attr("content")).toString();
                                                        }
                                                    }

                                                    String imageUrl = "";
                                                    if (imageUrlEl != null) {
                                                        imageUrl = imageUrlEl.attr("content");
                                                    }

                                                    StringBuilder titleBuilder = new StringBuilder();

                                                    if (!originalUrl.trim().equalsIgnoreCase(sharedText.trim())) {
                                                        // If the shared text is not just the URL, add it to the top
                                                        String toAppend = sharedText.replaceAll("\\s*" + Pattern.quote(originalUrl) + "\\s*", "");
                                                        titleBuilder.append(toAppend);
                                                    }

                                                    if (title.length() > 0) {
                                                        // OG title fetched from source
                                                        if (titleBuilder.length() > 0)
                                                            titleBuilder.append("\n\n");
                                                        titleBuilder.append(title);
                                                    }

                                                    String finalImage = imageUrl;
                                                    String finalTitle = titleBuilder.toString();
                                                    String finalDescription = description;

                                                    activity.runOnUiThread(() -> {
                                                        Bundle b = new Bundle();
                                                        b.putString(Helper.ARG_SHARE_URL, url[0]);
                                                        if (fetchSharedMedia) {
                                                            b.putString(Helper.ARG_SHARE_URL_MEDIA, finalImage);
                                                        }
                                                        b.putString(Helper.ARG_SHARE_TITLE, finalTitle);
                                                        b.putString(Helper.ARG_SHARE_DESCRIPTION, finalDescription);
                                                        b.putString(Helper.ARG_SHARE_SUBJECT, sharedSubject);
                                                        b.putString(Helper.ARG_SHARE_CONTENT, sharedText);
                                                        CrossActionHelper.doCrossShare(activity, b);
                                                    });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else if (response.code() == 103) {
                                                activity.runOnUiThread(() -> {
                                                    Bundle b = new Bundle();
                                                    b.putString(Helper.ARG_SHARE_DESCRIPTION, finalPotentialUrl);
                                                    CrossActionHelper.doCrossShare(activity, b);
                                                });
                                            } else {
                                                activity.runOnUiThread(() -> Toasty.warning(activity, activity.getString(R.string.toast_error), Toast.LENGTH_LONG).show());
                                            }
                                        }
                                    });
                                } catch (IndexOutOfBoundsException e) {
                                    Toasty.warning(activity, activity.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                                }

                            }
                        }).start();
                    } else {
                        Bundle b = new Bundle();
                        b.putString(Helper.ARG_SHARE_TITLE, sharedSubject);
                        b.putString(Helper.ARG_SHARE_DESCRIPTION, sharedText);
                        CrossActionHelper.doCrossShare(activity, b);
                    }


                }
            } else if (type.startsWith("image/") || type.startsWith("video/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Bundle b = new Bundle();
                    List<Uri> uris = new ArrayList<>();
                    uris.add(imageUri);
                    Helper.createAttachmentFromUri(activity, uris, attachments -> {
                        b.putSerializable(Helper.ARG_MEDIA_ATTACHMENTS, new ArrayList<>(attachments));
                        CrossActionHelper.doCrossShare(activity, b);
                    });
                } else {
                    Toasty.warning(activity, activity.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                ArrayList<Uri> imageList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageList != null) {
                    Bundle b = new Bundle();
                    Helper.createAttachmentFromUri(activity, imageList, attachments -> {
                        b.putSerializable(Helper.ARG_MEDIA_ATTACHMENTS, new ArrayList<>(attachments));
                        CrossActionHelper.doCrossShare(activity, b);
                    });
                } else {
                    Toasty.warning(activity, activity.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String url = intent.getDataString();

            if (url == null) {
                intent.replaceExtras(new Bundle());
                intent.setAction("");
                intent.setData(null);
                intent.setFlags(0);
                return;
            }
            Matcher matcher;
            matcher = Patterns.WEB_URL.matcher(url);
            boolean isUrl = false;
            while (matcher.find()) {
                isUrl = true;
            }
            if (!isUrl) {
                intent.replaceExtras(new Bundle());
                intent.setAction("");
                intent.setData(null);
                intent.setFlags(0);
                return;
            }
            //Here we know that the intent contains a valid URL
            if (!url.contains("medium.com")) {
                Pattern link = Pattern.compile("https?://([\\da-z.-]+[à-ü]?\\.[a-z.]{2,10})/(@[@\\w._-]*[0-9]*)(/[0-9]+)?$");
                Matcher matcherLink;
                matcherLink = link.matcher(url);
                if (matcherLink.find()) {
                    if (Helper.getCurrentAccount(activity) == null) {
                        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                        if (currentToken == null || currentToken.trim().isEmpty()) {
                            currentToken = sharedpreferences.getString(Helper.PREF_USER_TOKEN, null);
                        }
                        try {
                            Helper.setCurrentAccount(new Account(activity).getConnectedAccount());
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }
                    if (matcherLink.group(3) != null && Objects.requireNonNull(matcherLink.group(3)).length() > 0) { //It's a toot
                        CrossActionHelper.fetchRemoteStatus(activity, Helper.getCurrentAccount(activity), url, new CrossActionHelper.Callback() {
                            @Override
                            public void federatedStatus(Status status) {
                                if (status != null) {
                                    Intent intent = new Intent(activity, ContextActivity.class);
                                    Bundle args = new Bundle();
                                    args.putSerializable(Helper.ARG_STATUS, status);
                                    new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                        intent.putExtras(bundle);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        activity.startActivity(intent);
                                    });
                                } else {
                                    Toasty.error(activity, activity.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void federatedAccount(app.fedilab.android.mastodon.client.entities.api.Account account) {
                            }
                        });
                    } else {//It's an account
                        CrossActionHelper.fetchRemoteAccount(activity, Helper.getCurrentAccount(activity), matcherLink.group(2) + "@" + matcherLink.group(1), new CrossActionHelper.Callback() {
                            @Override
                            public void federatedStatus(Status status) {
                            }

                            @Override
                            public void federatedAccount(app.fedilab.android.mastodon.client.entities.api.Account account) {
                                if (account != null) {
                                    Intent intent = new Intent(activity, ProfileActivity.class);
                                    Bundle args = new Bundle();
                                    args.putSerializable(Helper.ARG_ACCOUNT, account);
                                    new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                        intent.putExtras(bundle);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        activity.startActivity(intent);
                                    });
                                } else {
                                    Toasty.error(activity, activity.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Helper.forwardToBrowser(activity, intent);
                }
            } else {
                Helper.forwardToBrowser(activity, intent);
            }
        }
        intent.replaceExtras(new Bundle());
        intent.setAction("");
        intent.setData(null);
        intent.setFlags(0);
    }

    protected abstract void rateThisApp();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mamageNewIntent(BaseMainActivity.this, intent);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        if (!Helper.isLoggedIn(BaseMainActivity.this)) {
            //It is not, the user is redirected to the login page
            Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
            startActivity(myIntent);
            finish();
            return;
        } else {
            BaseMainActivity.currentToken = sharedpreferences.getString(PREF_USER_TOKEN, null);
        }
        String software = sharedpreferences.getString(PREF_USER_SOFTWARE, null);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    ActivityCompat.requestPermissions(BaseMainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
                }
            });
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        NavHeaderMainBinding headerMainBinding = NavHeaderMainBinding.inflate(getLayoutInflater());
        //Update account details
        new Thread(() -> {
            try {
                if (currentToken == null) {
                    currentToken = sharedpreferences.getString(PREF_USER_TOKEN, null);
                }
                Helper.setCurrentAccount(new Account(BaseMainActivity.this).getConnectedAccount());
            } catch (DBException e) {
                e.printStackTrace();
            }
            //Apply the custom theme
            if (Helper.getCurrentAccount(BaseMainActivity.this) != null && currentInstance == null) {
                currentInstance = Helper.getCurrentAccount(BaseMainActivity.this).instance;
                currentUserID = Helper.getCurrentAccount(BaseMainActivity.this).user_id;
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = this::recreate;
                mainHandler.post(myRunnable);
            }

            if (Helper.getCurrentAccount(BaseMainActivity.this) != null && Helper.getCurrentAccount(BaseMainActivity.this).peertube_account != null) {
                //It is a peertube user
                Intent intent = getIntent();
                Intent myIntent = new Intent(this, PeertubeMainActivity.class);
                if (intent.getExtras() != null) {
                    Bundle currentExtra = myIntent.getExtras();
                    if (currentExtra == null) {
                        currentExtra = new Bundle();
                    }
                    Bundle bundleToForward = intent.getExtras();
                    currentExtra.putAll(bundleToForward);
                    myIntent.putExtras(currentExtra);
                }
                if (intent.getAction() != null) {
                    myIntent.setAction(intent.getAction());
                }
                startActivity(myIntent);
                finish();
                return;
            }
            //If the attached account is null, the app will fetch remote instance to get up-to-date values
            if (Helper.getCurrentAccount(BaseMainActivity.this) != null && Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account == null) {
                OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://" + (MainActivity.currentInstance != null ? IDN.toASCII(MainActivity.currentInstance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                        .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                        .client(okHttpClient)
                        .build();
                MastodonAccountsService mastodonAccountsService = retrofit.create(MastodonAccountsService.class);
                retrofit2.Call<app.fedilab.android.mastodon.client.entities.api.Account> accountCall = mastodonAccountsService.verify_credentials(MainActivity.currentToken);
                if (accountCall != null) {
                    try {
                        retrofit2.Response<app.fedilab.android.mastodon.client.entities.api.Account> accountResponse = accountCall.execute();
                        if (accountResponse.isSuccessful()) {
                            Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account = accountResponse.body();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                if (Helper.getCurrentAccount(BaseMainActivity.this) == null || (Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account == null && Helper.getCurrentAccount(BaseMainActivity.this).peertube_account == null)) {
                    //It is not, the user is redirected to the login page
                    if (Helper.getCurrentAccount(BaseMainActivity.this) != null) {
                        try {
                            Helper.removeAccount(BaseMainActivity.this);
                        } catch (DBException e) {
                            Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
                            startActivity(myIntent);
                            finish();
                            e.printStackTrace();
                        }
                    } else {
                        Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
                        startActivity(myIntent);
                        finish();
                    }
                    return;
                }
                bottomMenu = new BottomMenu(BaseMainActivity.this).hydrate(Helper.getCurrentAccount(BaseMainActivity.this), binding.bottomNavView);
                if (Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.locked) {
                    binding.navView.getMenu().findItem(R.id.nav_follow_requests).setVisible(true);
                }
                if (Helper.getCurrentAccount(BaseMainActivity.this).admin) {
                    binding.navView.getMenu().findItem(R.id.nav_administration).setVisible(true);
                }
                if (bottomMenu != null) {
                    //ManageClick on bottom menu items
                    if (binding.bottomNavView.findViewById(R.id.nav_home) != null) {
                        binding.bottomNavView.findViewById(R.id.nav_home).setOnLongClickListener(view -> {
                            int position = BottomMenu.getPosition(bottomMenu, R.id.nav_home);
                            if (position >= 0) {
                                manageFilters(position);
                            }
                            return false;
                        });
                    }
                    if (binding.bottomNavView.findViewById(R.id.nav_local) != null) {
                        binding.bottomNavView.findViewById(R.id.nav_local).setOnLongClickListener(view -> {
                            int position = BottomMenu.getPosition(bottomMenu, R.id.nav_local);
                            if (position >= 0) {
                                manageFilters(position);
                            }
                            return false;
                        });
                    }
                    if (binding.bottomNavView.findViewById(R.id.nav_public) != null) {
                        binding.bottomNavView.findViewById(R.id.nav_public).setOnLongClickListener(view -> {
                            int position = BottomMenu.getPosition(bottomMenu, R.id.nav_public);
                            if (position >= 0) {
                                manageFilters(position);
                            }
                            return false;
                        });
                    }
                    binding.bottomNavView.setOnItemSelectedListener(item -> {
                        int itemId = item.getItemId();
                        int position = BottomMenu.getPosition(bottomMenu, itemId);
                        if (position >= 0) {
                            if (binding.viewPager.getCurrentItem() == position) {
                                scrollToTop();
                                binding.bottomNavView.removeBadge(itemId);
                            } else {
                                binding.viewPager.setCurrentItem(position, false);
                            }
                        }
                        return true;
                    });
                }

                currentInstance = Helper.getCurrentAccount(BaseMainActivity.this).instance;
                currentUserID = Helper.getCurrentAccount(BaseMainActivity.this).user_id;

                show_boosts = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_BOOSTS) + currentUserID + currentInstance, true);
                show_my_messages = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_MY_MESSAGES) + currentUserID + currentInstance, true);
                show_self_boosts = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_SELF_BOOSTS) + currentUserID + currentInstance, true);
                show_replies = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_REPLIES) + currentUserID + currentInstance, true);
                show_self_replies = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_SELF_REPLIES) + currentUserID + currentInstance, true);
                show_dms = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_DMS) + currentUserID + currentInstance, true);
                regex_home = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, null);
                regex_local = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, null);
                regex_public = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, null);
                show_art_nsfw = sharedpreferences.getBoolean(getString(R.string.SET_ART_WITH_NSFW) + currentUserID + currentInstance, false);

                binding.profilePicture.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
                Helper.loadPP(BaseMainActivity.this, binding.profilePicture, Helper.getCurrentAccount(BaseMainActivity.this));
                headerMainBinding.accountAcc.setText(String.format("%s@%s", Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.username, Helper.getCurrentAccount(BaseMainActivity.this).instance));
                if (Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.display_name == null || Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.display_name.isEmpty()) {
                    Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.display_name = Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.acct;
                }
                if (!isFinishing()) {
                    headerMainBinding.accountName.setText(
                            Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account.getSpanDisplayNameEmoji(BaseMainActivity.this,
                                    new WeakReference<>(headerMainBinding.accountName)),
                            TextView.BufferType.SPANNABLE);
                }
                float scale = sharedpreferences.getFloat(getString(R.string.SET_FONT_SCALE), 1.1f);
                headerMainBinding.accountName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
                headerMainBinding.accountAcc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
                Helper.loadPP(BaseMainActivity.this, headerMainBinding.accountProfilePicture, Helper.getCurrentAccount(BaseMainActivity.this), false);
                MastodonHelper.loadProfileMediaMastodon(BaseMainActivity.this, headerMainBinding.backgroundImage, Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account, MastodonHelper.MediaAccountType.HEADER);
                headerMainBinding.backgroundImage.setAlpha(0.5f);
                /*
                 * Some general data are loaded when the app starts such;
                 *  - Pinned timelines (in app feature)
                 *  - Instance info (for limits)
                 *  - Emoji for picker
                 *  - Filters for timelines

                 */

                //Update pinned timelines
                new ViewModelProvider(BaseMainActivity.this).get(TopBarVM.class).getDBPinned()
                        .observe(this, pinned -> {
                            this.pinned = pinned;
                            //Initialize the slug of the first fragment
                            //First it's taken from db (last stored values)
                            PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, bottomMenu, null);
                            //Fetch remote lists for the authenticated account and update them
                            new ViewModelProvider(BaseMainActivity.this).get(TimelinesVM.class).getLists(currentInstance, currentToken)
                                    .observe(this, mastodonLists ->
                                            PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, bottomMenu, mastodonLists)
                                    );
                        });

                //Update emoji in db for the current instance
                new ViewModelProvider(BaseMainActivity.this).get(InstancesVM.class).getEmoji(currentInstance);
                //Retrieve instance info
                new ViewModelProvider(BaseMainActivity.this).get(InstancesVM.class).getInstance(currentInstance)
                        .observe(BaseMainActivity.this, instance -> {
                            instanceInfo = instance.info;
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(getString(R.string.INSTANCE_INFO) + MainActivity.currentInstance, Instance.serialize(instanceInfo));
                            editor.apply();
                        });
                //Retrieve filters
                new ViewModelProvider(BaseMainActivity.this).get(FiltersVM.class).getFilters(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, filters -> mainFilters = filters);
                new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class).getConnectedAccount(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, mastodonAccount -> {
                            //Initialize static var
                            if (mastodonAccount != null && Helper.getCurrentAccount(BaseMainActivity.this) != null) {
                                Helper.setCurrentAccountMastodonAccount(BaseMainActivity.this, mastodonAccount);
                                displayReleaseNotesIfNeeded(BaseMainActivity.this, false);
                                new Thread(() -> {
                                    try {
                                        //Update account in db
                                        new Account(BaseMainActivity.this).insertOrUpdate(Helper.getCurrentAccount(BaseMainActivity.this));
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        });
                mamageNewIntent(this, getIntent());

            };
            mainHandler.post(myRunnable);
        }).start();
        filteredAccounts = new ArrayList<>();

        filterFetched = false;
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        manageTopBarScrolling(binding.toolbar);
        rateThisApp();

        binding.compose.setOnClickListener(v -> startActivity(new Intent(this, ComposeActivity.class)));
        binding.compose.setOnLongClickListener(view -> {
            CrossActionHelper.doCrossAction(BaseMainActivity.this, CrossActionHelper.TypeOfCrossAction.COMPOSE, null, null);
            return false;
        });
        headerMenuOpen = false;

        PushHelper.startStreaming(BaseMainActivity.this);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder()
                .setOpenableLayout(binding.drawerLayout)
                .build();


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
            } else if (id == R.id.nav_followed_tags) {
                Intent intent = new Intent(this, FollowedTagActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_scheduled) {
                Intent intent = new Intent(this, ScheduledActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_follow_requests) {
                Intent intent = new Intent(this, FollowRequestActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_administration) {
                Intent intent = new Intent(this, AdminActionActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about) {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_release_notes) {
                displayReleaseNotesIfNeeded(BaseMainActivity.this, true);
            } else if (id == R.id.nav_partnership) {
                Intent intent = new Intent(this, PartnerShipActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_announcements) {
                Intent intent = new Intent(this, AnnouncementActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_trends) {
                Intent intent = new Intent(this, TrendsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_suggestions) {
                Intent intent = new Intent(this, SuggestionActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_directory) {
                Intent intent = new Intent(this, DirectoryActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_cache) {
                Intent intent = new Intent(BaseMainActivity.this, CacheActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_peertube) {
                Intent intent = new Intent(BaseMainActivity.this, PeertubeMainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean(Helper.ARG_PEERTUBE_NAV_REMOTE, true);
                intent.putExtras(bundle);
                startActivity(intent);
            } else if (id == R.id.nav_about_instance) {
                (new InstanceActivity()).show(getSupportFragmentManager(), null);
            } else if (id == R.id.nav_instance_info) {
                (new InstanceHealthActivity()).show(getSupportFragmentManager(), null);
            }
            binding.drawerLayout.close();
            return false;
        });

        headerMainBinding.accountProfilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(BaseMainActivity.this, ProfileActivity.class);
            Bundle args = new Bundle();
            args.putSerializable(Helper.ARG_ACCOUNT, Helper.getCurrentAccount(BaseMainActivity.this).mastodon_account);
            new CachedBundle(BaseMainActivity.this).insertBundle(args, Helper.getCurrentAccount(BaseMainActivity.this), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intent.putExtras(bundle);
                startActivity(intent);
            });

        });

        TooltipCompat.setTooltipText(headerMainBinding.ownerAccounts, getString(R.string.manage_accounts));
        headerMainBinding.accountName.setOnClickListener(v -> headerMainBinding.ownerAccounts.performClick());
        headerMainBinding.accountAcc.setOnClickListener(v -> headerMainBinding.ownerAccounts.performClick());
        headerMainBinding.ownerAccounts.setOnClickListener(v -> {
            headerMenuOpen = !headerMenuOpen;
            manageDrawerMenu(BaseMainActivity.this, binding.navView, headerMainBinding);
        });

        TooltipCompat.setTooltipText(headerMainBinding.headerLogout,getString(R.string.action_logout));
        headerMainBinding.headerLogout.setOnClickListener(v -> headerLogoutClick(BaseMainActivity.this, headerMainBinding, getSupportFragmentManager()));

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
                String pattern = "^(@[\\w_-]+@[a-z0-9.\\-]+|@[\\w_-]+)";
                final Pattern mentionPattern = Pattern.compile(pattern);
                String patternTag = "^#([\\w-]{2,})$";
                final Pattern tagPattern = Pattern.compile(patternTag);
                Matcher matcherMention, matcherTag;
                matcherMention = mentionPattern.matcher(newText);
                matcherTag = tagPattern.matcher(newText);
                if (newText.trim().isEmpty()) {
                    binding.toolbarSearch.setSuggestionsAdapter(null);
                }
                if (matcherMention.matches()) {
                    String[] from = new String[]{SearchManager.SUGGEST_COLUMN_ICON_1, SearchManager.SUGGEST_COLUMN_TEXT_1};
                    int[] to = new int[]{R.id.account_pp, R.id.account_un};
                    String searchGroup = matcherMention.group();
                    AccountsVM accountsVM = new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class);
                    accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, 10, false, false)
                            .observe(BaseMainActivity.this, accounts -> {
                                if (accounts == null) {
                                    return;
                                }
                                MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_ICON_1,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1});
                                AccountsSearchTopBarAdapter cursorAdapter = new AccountsSearchTopBarAdapter(BaseMainActivity.this, accounts, R.layout.drawer_account_search, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                                binding.toolbarSearch.setSuggestionsAdapter(cursorAdapter);
                                new Thread(() -> {
                                    int i = 0;
                                    for (app.fedilab.android.mastodon.client.entities.api.Account account : accounts) {
                                        FutureTarget<File> futureTarget = Glide
                                                .with(BaseMainActivity.this.getApplicationContext())
                                                .load(account.avatar_static)
                                                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                                        File cacheFile;
                                        try {
                                            cacheFile = futureTarget.get();
                                            cursor.addRow(new String[]{String.valueOf(i), cacheFile.getAbsolutePath(), "@" + account.acct});
                                            i++;
                                        } catch (ExecutionException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    runOnUiThread(() -> cursorAdapter.changeCursor(cursor));
                                }).start();

                            });
                } else if (matcherTag.matches()) {
                    SearchVM searchVM = new ViewModelProvider(BaseMainActivity.this).get(SearchVM.class);
                    String[] from = new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1};
                    int[] to = new int[]{R.id.tag_name};
                    String searchGroup = matcherTag.group();

                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, null,
                            "hashtags", false, true, false, 0,
                            null, null, 10).observe(BaseMainActivity.this,
                            results -> {
                                if (results == null || results.hashtags == null) {
                                    return;
                                }
                                MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                                        SearchManager.SUGGEST_COLUMN_TEXT_1});
                                TagSearchTopBarAdapter cursorAdapter = new TagSearchTopBarAdapter(BaseMainActivity.this, results.hashtags, R.layout.drawer_tag_search, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                                binding.toolbarSearch.setSuggestionsAdapter(cursorAdapter);
                                int i = 0;
                                for (Tag tag : results.hashtags) {
                                    cursor.addRow(new String[]{String.valueOf(i), "#" + tag.name});
                                    i++;
                                }
                                runOnUiThread(() -> cursorAdapter.changeCursor(cursor));
                            });
                }
                return false;
            }
        });
        binding.toolbarSearch.setOnCloseListener(() -> {
            binding.tabLayout.setVisibility(View.VISIBLE);
            return false;
        });


        binding.toolbarSearch.setOnSearchClickListener(v -> binding.tabLayout.setVisibility(View.VISIBLE));
        //For receiving  data from other activities
        ContextCompat.registerReceiver(BaseMainActivity.this, broadcast_data, new IntentFilter(Helper.BROADCAST_DATA), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(BaseMainActivity.this, broadcast_error_message, new IntentFilter(Helper.INTENT_COMPOSE_ERROR_MESSAGE), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (emojis == null || !emojis.containsKey(BaseMainActivity.currentInstance) || emojis.get(BaseMainActivity.currentInstance) == null) {
            new Thread(() -> {
                try {
                    emojis.put(currentInstance, new EmojiInstance(BaseMainActivity.this).getEmojiList(BaseMainActivity.currentInstance));
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        fetchRecentAccounts(BaseMainActivity.this, headerMainBinding);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sharedpreferences.getBoolean(getString(R.string.SET_AUTO_HIDE_COMPOSE), true) && !getFloatingVisibility())
            manageFloatingButton(true);
    }

    private void manageTopBarScrolling(Toolbar toolbar) {
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

    private void manageFilters(int position) {
        View view = binding.bottomNavView.findViewById(R.id.nav_home);
        boolean showExtendedFilter = true;
        if (position == BottomMenu.getPosition(bottomMenu, R.id.nav_local)) {
            view = binding.bottomNavView.findViewById(R.id.nav_local);
            showExtendedFilter = false;
        } else if (position == BottomMenu.getPosition(bottomMenu, R.id.nav_public)) {
            view = binding.bottomNavView.findViewById(R.id.nav_public);
            showExtendedFilter = false;
        }
        PopupMenu popup = new PopupMenu(BaseMainActivity.this, view, Gravity.TOP);
        popup.getMenuInflater()
                .inflate(R.menu.option_filter_toots, popup.getMenu());
        Menu menu = popup.getMenu();
        final MenuItem itemShowBoosts = menu.findItem(R.id.action_show_boosts);
        final MenuItem itemShowMyMessages = menu.findItem(R.id.action_show_my_messages);
        final MenuItem itemShowSelfBoosts = menu.findItem(R.id.action_show_self_boosts);
        final MenuItem itemShowDMs = menu.findItem(R.id.action_show_dms);
        final MenuItem itemShowReplies = menu.findItem(R.id.action_show_replies);
        final MenuItem itemShowSelfReplies = menu.findItem(R.id.action_show_self_replies);
        final MenuItem itemFilter = menu.findItem(R.id.action_filter);
        if (!showExtendedFilter) {
            itemShowBoosts.setVisible(false);
            itemShowReplies.setVisible(false);
            itemShowSelfBoosts.setVisible(false);
            itemShowSelfReplies.setVisible(false);
            itemShowMyMessages.setVisible(false);
            itemShowDMs.setVisible(false);
        } else {
            itemShowBoosts.setVisible(true);
            itemShowReplies.setVisible(true);
            itemShowSelfBoosts.setVisible(true);
            itemShowSelfReplies.setVisible(true);
            itemShowMyMessages.setVisible(true);
            itemShowDMs.setVisible(true);
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        String show_filtered = null;
        if (position == BottomMenu.getPosition(bottomMenu, R.id.nav_home)) {
            show_filtered = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, null);
        } else if (position == BottomMenu.getPosition(bottomMenu, R.id.nav_local)) {
            show_filtered = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, null);
        } else if (position == BottomMenu.getPosition(bottomMenu, R.id.nav_public)) {
            show_filtered = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, null);
        }

        itemShowBoosts.setChecked(show_boosts);
        itemShowMyMessages.setChecked(show_my_messages);
        itemShowReplies.setChecked(show_replies);
        itemShowSelfBoosts.setChecked(show_self_boosts);
        itemShowSelfReplies.setChecked(show_self_replies);
        itemShowDMs.setChecked(show_dms);
        if (show_filtered != null && show_filtered.length() > 0) {
            itemFilter.setTitle(show_filtered);
        }
        popup.setOnDismissListener(menu1 -> {
            if (binding.viewPager.getAdapter() != null) {
                int tabPosition = binding.tabLayout.getSelectedTabPosition();
                Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, Math.max(tabPosition, 0));
                if (fragment instanceof FragmentMastodonTimeline fragmentMastodonTimeline && fragment.isVisible()) {
                    fragmentMastodonTimeline.refreshAllAdapters();
                }
            }
        });
        String finalShow_filtered = show_filtered;
        popup.setOnMenuItemClickListener(item -> {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(BaseMainActivity.this));
            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return false;
                }
            });
            final SharedPreferences.Editor editor = sharedpreferences.edit();
            int itemId = item.getItemId();
            if (itemId == R.id.action_show_boosts) {
                show_boosts = !show_boosts;
                editor.putBoolean(getString(R.string.SET_SHOW_BOOSTS) + currentUserID + currentInstance, show_boosts);
                itemShowBoosts.setChecked(show_boosts);
                editor.apply();
            } else if (itemId == R.id.action_show_my_messages) {
                show_my_messages = !show_my_messages;
                editor.putBoolean(getString(R.string.SET_SHOW_MY_MESSAGES) + currentUserID + currentInstance, show_my_messages);
                itemShowMyMessages.setChecked(show_my_messages);
                editor.apply();
            } else if (itemId == R.id.action_show_self_boosts) {
                show_self_boosts = !show_self_boosts;
                editor.putBoolean(getString(R.string.SET_SHOW_SELF_BOOSTS) + currentUserID + currentInstance, show_self_boosts);
                itemShowSelfBoosts.setChecked(show_self_boosts);
                editor.apply();
            } else if (itemId == R.id.action_show_replies) {
                show_replies = !show_replies;
                editor.putBoolean(getString(R.string.SET_SHOW_REPLIES) + currentUserID + currentInstance, show_replies);
                itemShowReplies.setChecked(show_replies);
                editor.apply();
            } else if (itemId == R.id.action_show_self_replies) {
                show_self_replies = !show_self_replies;
                editor.putBoolean(getString(R.string.SET_SHOW_SELF_REPLIES) + currentUserID + currentInstance, show_self_replies);
                itemShowSelfReplies.setChecked(show_self_replies);
                editor.apply();
            } else if (itemId == R.id.action_show_dms) {
                show_dms = !show_dms;
                editor.putBoolean(getString(R.string.SET_SHOW_DMS) + currentUserID + currentInstance, show_dms);
                itemShowDMs.setChecked(show_dms);
                editor.apply();
            } else if (itemId == R.id.action_filter) {
                AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(BaseMainActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.popup_filter_regex, new LinearLayout(BaseMainActivity.this), false);
                dialogBuilder.setView(dialogView);
                final EditText editText = dialogView.findViewById(R.id.filter_regex);
                Toast alertRegex = Toasty.warning(BaseMainActivity.this, getString(R.string.alert_regex), Toast.LENGTH_LONG);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            Pattern.compile("(" + s.toString() + ")", Pattern.CASE_INSENSITIVE);
                        } catch (Exception e) {
                            if (!alertRegex.getView().isShown()) {
                                alertRegex.show();
                            }
                        }

                    }
                });
                if (finalShow_filtered != null) {
                    editText.setText(finalShow_filtered);
                    editText.setSelection(editText.getText().toString().length());
                }
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                    itemFilter.setTitle(editText.getText().toString().trim());
                    if (position == 0) {
                        editor.putString(getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, editText.getText().toString().trim());
                        regex_home = editText.getText().toString().trim();
                    } else if (position == 1) {
                        editor.putString(getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, editText.getText().toString().trim());
                        regex_local = editText.getText().toString().trim();
                    } else if (position == 2) {
                        editor.putString(getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, editText.getText().toString().trim());
                        regex_public = editText.getText().toString().trim();
                    }
                    editor.apply();
                });
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
                return true;
            }
            return false;
        });
        popup.show();

    }

    public void refreshFragment() {
        if (binding.viewPager.getAdapter() != null) {
            int position = binding.tabLayout.getSelectedTabPosition();
            Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, Math.max(position, 0));
            if (fragment instanceof FragmentNotificationContainer) {
                FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
                fragTransaction.detach(fragment).commit();
                FragmentTransaction fragTransaction2 = getSupportFragmentManager().beginTransaction();
                fragTransaction2.attach(fragment);
                fragTransaction2.commit();
            }
        }
    }

    @Override
    public void onUpdateConversation(int count) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        boolean singleBar = sharedpreferences.getBoolean(getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        boolean displayCounters = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_COUNTERS), true);
        if (!displayCounters) {
            return;
        }
        if (!singleBar) {
            if (count > 0) {
                binding.bottomNavView.getOrCreateBadge(R.id.nav_privates).setNumber(count);
            } else {
                binding.bottomNavView.removeBadge(R.id.nav_privates);
            }
        }
        setCounterToTab(Timeline.TimeLineEnum.CONVERSATION.getValue(), count);
    }

    @Override
    public void onUpdateNotification(int count) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        boolean singleBar = sharedpreferences.getBoolean(getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        boolean displayCounters = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_COUNTERS), true);
        if (!displayCounters) {
            return;
        }
        if (!singleBar) {
            if (count > 0) {
                binding.bottomNavView.getOrCreateBadge(R.id.nav_notifications).setNumber(count);
            } else {
                binding.bottomNavView.removeBadge(R.id.nav_notifications);
            }
        }
        setCounterToTab(Timeline.TimeLineEnum.NOTIFICATION.getValue(), count);
    }

    /**
     * Get the tab depending of its position
     *
     * @param slug String slug for the timeline
     * @return int - position
     */
    private int getTabPosition(String slug) {
        int position = 0;
        for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
            if (tab != null && tab.getTag() != null && tab.getTag().equals(slug)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    /**
     * Set the counter to the tab depending of the slug
     *
     * @param slug  - String slug for the pinned timeline
     * @param count - int new messages
     */
    private void setCounterToTab(String slug, int count) {
        int position = getTabPosition(slug);
        if (position >= 0 && position < binding.tabLayout.getTabCount()) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(position);
            View view = null;
            if (tab != null) {
                view = tab.getCustomView();
            }
            if (view != null) {
                TextView counter = view.findViewById(R.id.tab_counter);
                if (counter != null) {
                    if (count > 0) {
                        counter.setVisibility(View.VISIBLE);
                        counter.setText(String.valueOf(count));
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate(int count, Timeline.TimeLineEnum type, String slug) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
        boolean displayCounters = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_COUNTERS), true);
        if (!displayCounters) {
            return;
        }
        boolean singleBar = sharedpreferences.getBoolean(getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (!singleBar) {
            switch (type) {
                case HOME:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_home).setNumber(count);
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_home);
                    }
                    break;
                case LOCAL:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_local).setNumber(count);
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_local);
                    }
                    break;
                case PUBLIC:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_public).setNumber(count);
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_public);
                    }
                    break;
                case NOTIFICATION:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_notifications).setNumber(count);
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_notifications);
                    }
                    break;
                case DIRECT:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_privates).setNumber(count);
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_privates);
                    }
                    break;
            }
        }
        setCounterToTab(slug, count);
    }

    @Override
    protected void onDestroy() {

        try {
            unregisterReceiver(broadcast_data);
            unregisterReceiver(broadcast_error_message);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (networkStateReceiver != null) {
            try {
                networkStateReceiver.removeListener(this);
                unregisterReceiver(networkStateReceiver);
            } catch (Exception ignored) {}
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

    /**
     * Allow to scroll to top for bottom navigation items
     */
    private void scrollToTop() {
        int position = binding.tabLayout.getSelectedTabPosition();
        if (binding.viewPager.getAdapter() != null) {
            Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, Math.max(position, 0));
            if (fragment instanceof FragmentMastodonTimeline fragmentMastodonTimeline) {
                fragmentMastodonTimeline.scrollToTop();
            } else if (fragment instanceof FragmentMastodonConversation fragmentMastodonConversation) {
                fragmentMastodonConversation.scrollToTop();
            } else if (fragment instanceof FragmentNotificationContainer fragmentNotificationContainer) {
                fragmentNotificationContainer.scrollToTop();
            }
        }
    }

    public void redrawPinned(List<MastodonList> mastodonLists) {
        int currentItem = binding.viewPager.getCurrentItem();
        new ViewModelProvider(BaseMainActivity.this).get(TopBarVM.class).getDBPinned()
                .observe(this, pinned -> {
                    this.pinned = pinned;
                    //First it's taken from db (last stored values)
                    PinnedTimelineHelper.redrawTopBarPinned(BaseMainActivity.this, binding, pinned, bottomMenu, mastodonLists);
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
        if (sharedpreferences.getBoolean(getString(R.string.SET_AUTO_HIDE_COMPOSE), true)) {
            if (display) {
                binding.compose.show();
            } else {
                binding.compose.hide();
            }
        }
    }

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

   /* @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(BaseMainActivity.this);
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
    public void networkAvailable() {
        networkAvailable = status.CONNECTED;
    }

    @Override
    public void networkUnavailable() {
        networkAvailable = DISCONNECTED;
    }

    public enum iconLauncher {
        BUBBLES,
        BUBBLESUA,
        BUBBLESPEAGREEN,
        BUBBLESPRIDE,
        BUBBLESPINK,
        BUBBLESPIRATE,
        FEDIVERSE,
        HERO,
        ATOM,
        BRAINCRASH,
        MASTALAB
    }


    public enum status {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED
    }
}