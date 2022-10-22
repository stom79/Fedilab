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
import static app.fedilab.android.helper.CacheHelper.deleteDir;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.helper.Helper.displayReleaseNotesIfNeeded;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.ContextThemeWrapper;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.jaredrummler.cyanea.Cyanea;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.activities.AboutActivity;
import app.fedilab.android.activities.ActionActivity;
import app.fedilab.android.activities.AdminActionActivity;
import app.fedilab.android.activities.AnnouncementActivity;
import app.fedilab.android.activities.BaseActivity;
import app.fedilab.android.activities.CacheActivity;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.DraftActivity;
import app.fedilab.android.activities.FilterActivity;
import app.fedilab.android.activities.FollowRequestActivity;
import app.fedilab.android.activities.InstanceActivity;
import app.fedilab.android.activities.InstanceHealthActivity;
import app.fedilab.android.activities.LoginActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.MastodonListActivity;
import app.fedilab.android.activities.PartnerShipActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.activities.ProxyActivity;
import app.fedilab.android.activities.ReorderTimelinesActivity;
import app.fedilab.android.activities.ScheduledActivity;
import app.fedilab.android.activities.SearchResultTabActivity;
import app.fedilab.android.activities.SettingsActivity;
import app.fedilab.android.activities.TrendsActivity;
import app.fedilab.android.broadcastreceiver.NetworkStateReceiver;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.client.entities.api.EmojiInstance;
import app.fedilab.android.client.entities.api.Filter;
import app.fedilab.android.client.entities.api.Instance;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.BottomMenu;
import app.fedilab.android.client.entities.app.DomainsBlock;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityMainBinding;
import app.fedilab.android.databinding.NavHeaderMainBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.PinnedTimelineHelper;
import app.fedilab.android.helper.PushHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.ui.fragment.timeline.FragmentNotificationContainer;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.InstancesVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;
import app.fedilab.android.viewmodel.mastodon.TopBarVM;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class BaseMainActivity extends BaseActivity implements NetworkStateReceiver.NetworkStateReceiverListener, FragmentMastodonTimeline.UpdateCounters, FragmentNotificationContainer.UpdateCounters, FragmentMastodonConversation.UpdateCounters {

    public static String currentInstance, currentToken, currentUserID, client_id, client_secret, software;
    public static HashMap<String, List<Emoji>> emojis = new HashMap<>();
    public static Account.API api;
    public static boolean admin;
    public static status networkAvailable = UNKNOWN;
    public static Instance instanceInfo;
    public static List<Filter> mainFilters;
    public static boolean filterFetched;
    public static boolean show_boosts, show_replies, show_art_nsfw;
    public static String regex_home, regex_local, regex_public;
    public static BaseAccount currentAccount;
    Fragment currentFragment;
    public static String slugOfFirstFragment;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private final BroadcastReceiver broadcast_error_message = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                if (b.getBoolean(Helper.RECEIVE_COMPOSE_ERROR_MESSAGE, false)) {
                    String errorMessage = b.getString(Helper.RECEIVE_ERROR_MESSAGE);
                    StatusDraft statusDraft = (StatusDraft) b.getSerializable(Helper.ARG_STATUS_DRAFT);
                    Snackbar snackbar = Snackbar.make(binding.getRoot(), errorMessage, 5000);
                    View snackbarView = snackbar.getView();
                    TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                    textView.setMaxLines(5);
                    snackbar
                            .setAction(getString(R.string.open_draft), view -> {
                                Intent intentCompose = new Intent(context, ComposeActivity.class);
                                intentCompose.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
                                intentCompose.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intentCompose);
                            })
                            .setTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor))
                            .setActionTextColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference))
                            .setBackgroundTint(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_primary_dark_reference))
                            .show();
                }
            }
        }
    };
    private Pinned pinned;
    private BottomMenu bottomMenu;
    private final BroadcastReceiver broadcast_data = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                if (b.getBoolean(Helper.RECEIVE_REDRAW_TOPBAR, false)) {
                    List<MastodonList> mastodonLists = (List<MastodonList>) b.getSerializable(Helper.RECEIVE_MASTODON_LIST);
                    redrawPinned(mastodonLists);
                }
                if (b.getBoolean(Helper.RECEIVE_REDRAW_BOTTOM, false)) {
                    bottomMenu = new BottomMenu(BaseMainActivity.this).hydrate(currentAccount, binding.bottomNavView);
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
                } else if (b.getBoolean(Helper.RECEIVE_RECREATE_ACTIVITY, false)) {
                    Cyanea.getInstance().edit().apply().recreate(BaseMainActivity.this);
                } else if (b.getBoolean(Helper.RECEIVE_NEW_MESSAGE, false)) {
                    Status statusSent = (Status) b.getSerializable(Helper.RECEIVE_STATUS_ACTION);
                    Snackbar.make(binding.displaySnackBar, getString(R.string.message_has_been_sent), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.display), view -> {
                                Intent intentContext = new Intent(BaseMainActivity.this, ContextActivity.class);
                                intentContext.putExtra(Helper.ARG_STATUS, statusSent);
                                intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intentContext);
                            })
                            .setTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor))
                            .setActionTextColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference))
                            .setBackgroundTint(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_primary_dark_reference))
                            .show();
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

    /**
     * Open notifications tab when coming from a notification device
     *
     * @param intent - Intent intent that will be cancelled
     */
    private void openNotifications(Intent intent) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
            boolean singleBar = sharedpreferences.getBoolean(getString(R.string.SET_USE_SINGLE_TOPBAR), false);
            if (!singleBar) {
                binding.bottomNavView.setSelectedItemId(R.id.nav_notifications);
            } else {
                int position = 0;
                for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
                    if (tab != null && tab.getTag() != null && tab.getTag().equals(Timeline.TimeLineEnum.NOTIFICATION.getValue())) {
                        break;
                    }
                    position++;
                }
                binding.viewPager.setCurrentItem(position);
            }
        }, 1000);
        intent.removeExtra(Helper.INTENT_ACTION);
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
                if (userIdIntent != null && instanceIntent != null && userIdIntent.equals(currentUserID) && instanceIntent.equals(currentInstance)) {
                    openNotifications(intent);
                } else {
                    try {
                        BaseAccount account = new Account(BaseMainActivity.this).getUniqAccount(userIdIntent, instanceIntent);
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
                        mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_NOTIFICATION);
                        startActivity(mainActivity);
                        finish();
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            } else if (extras.getInt(Helper.INTENT_ACTION) == Helper.OPEN_NOTIFICATION) {
                openNotifications(intent);
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
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(BaseMainActivity.this);
                    boolean fetchSharedMedia = sharedpreferences.getBoolean(getString(R.string.SET_RETRIEVE_METADATA_IF_URL_FROM_EXTERAL), true);
                    boolean fetchShareContent = sharedpreferences.getBoolean(getString(R.string.SET_SHARE_DETAILS), true);
                    if (url[0] != null && count == 1 && (fetchShareContent || fetchSharedMedia)) {
                        new Thread(() -> {
                            if (url[0].startsWith("www."))
                                url[0] = "http://" + url[0];
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
                                Pattern titlePattern = Pattern.compile("<meta [^>]*property=[\"']og:title[\"'] [^>]*content=[\"']([^'^\"]+?)[\"'][^>]*>");
                                Pattern descriptionPattern = Pattern.compile("<meta [^>]*property=[\"']og:description[\"'] [^>]*content=[\"']([^'^\"]+?)[\"'][^>]*>");
                                Pattern imagePattern = Pattern.compile("<meta [^>]*property=[\"']og:image[\"'] [^>]*content=[\"']([^'^\"]+?)[\"'][^>]*>");

                                try {
                                    OkHttpClient client = new OkHttpClient.Builder()
                                            .connectTimeout(10, TimeUnit.SECONDS)
                                            .writeTimeout(10, TimeUnit.SECONDS)
                                            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
                                            .readTimeout(10, TimeUnit.SECONDS).build();
                                    Request request = new Request.Builder()
                                            .url(potentialUrl)
                                            .build();
                                    client.newCall(request).enqueue(new Callback() {
                                        @Override
                                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                            e.printStackTrace();
                                            runOnUiThread(() -> Toasty.warning(BaseMainActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show());
                                        }

                                        @Override
                                        public void onResponse(@NonNull Call call, @NonNull final Response response) {
                                            if (response.isSuccessful()) {
                                                try {
                                                    String data = response.body().string();
                                                    Matcher matcherTitle;
                                                    matcherTitle = titlePattern.matcher(data);
                                                    Matcher matcherDescription = descriptionPattern.matcher(data);
                                                    Matcher matcherImage = imagePattern.matcher(data);
                                                    String titleEncoded = null;
                                                    String descriptionEncoded = null;
                                                    if (fetchShareContent) {
                                                        while (matcherTitle.find())
                                                            titleEncoded = matcherTitle.group(1);
                                                        while (matcherDescription.find())
                                                            descriptionEncoded = matcherDescription.group(1);
                                                    }
                                                    String image = null;
                                                    if (fetchSharedMedia) {
                                                        while (matcherImage.find())
                                                            image = matcherImage.group(1);
                                                    }
                                                    String title = null;
                                                    String description = null;
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                        if (titleEncoded != null)
                                                            title = Html.fromHtml(titleEncoded, Html.FROM_HTML_MODE_LEGACY).toString();
                                                        if (descriptionEncoded != null)
                                                            description = Html.fromHtml(descriptionEncoded, Html.FROM_HTML_MODE_LEGACY).toString();
                                                    } else {
                                                        if (titleEncoded != null)
                                                            title = Html.fromHtml(titleEncoded).toString();
                                                        if (descriptionEncoded != null)
                                                            description = Html.fromHtml(descriptionEncoded).toString();
                                                    }
                                                    String finalImage = image;
                                                    String finalTitle = title;
                                                    String finalDescription = description;


                                                    runOnUiThread(() -> {
                                                        Bundle b = new Bundle();
                                                        b.putString(Helper.ARG_SHARE_URL, url[0]);
                                                        b.putString(Helper.ARG_SHARE_URL_MEDIA, finalImage);
                                                        b.putString(Helper.ARG_SHARE_TITLE, finalTitle);
                                                        b.putString(Helper.ARG_SHARE_DESCRIPTION, finalDescription);
                                                        b.putString(Helper.ARG_SHARE_SUBJECT, sharedSubject);
                                                        b.putString(Helper.ARG_SHARE_CONTENT, sharedText);
                                                        CrossActionHelper.doCrossShare(BaseMainActivity.this, b);
                                                    });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                runOnUiThread(() -> Toasty.warning(BaseMainActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show());
                                            }
                                        }
                                    });
                                } catch (IndexOutOfBoundsException e) {
                                    Toasty.warning(BaseMainActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                                }

                            }
                        }).start();
                    } else {
                        Bundle b = new Bundle();
                        b.putString(Helper.ARG_SHARE_TITLE, sharedSubject);
                        b.putString(Helper.ARG_SHARE_DESCRIPTION, sharedText);
                        CrossActionHelper.doCrossShare(BaseMainActivity.this, b);
                    }


                }
            } else if (type.startsWith("image/") || type.startsWith("video/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Bundle b = new Bundle();
                    b.putParcelable(Helper.ARG_SHARE_URI, imageUri);
                    CrossActionHelper.doCrossShare(BaseMainActivity.this, b);
                } else {
                    Toasty.warning(BaseMainActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                ArrayList<Uri> imageList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageList != null) {
                    Bundle b = new Bundle();
                    b.putParcelableArrayList(Helper.ARG_SHARE_URI_LIST, imageList);
                    CrossActionHelper.doCrossShare(BaseMainActivity.this, b);
                } else {
                    Toasty.warning(BaseMainActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
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
        binding.bottomNavView.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        binding.navView.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));


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
            } else if (id == R.id.nav_cache) {
                Intent intent = new Intent(BaseMainActivity.this, CacheActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about_instance) {
                Intent intent = new Intent(BaseMainActivity.this, InstanceActivity.class);
                startActivity(intent);
            }
            binding.drawerLayout.close();
            return false;
        });


        headerMainBinding.instanceInfo.setOnClickListener(v -> startActivity(new Intent(BaseMainActivity.this, InstanceHealthActivity.class)));
        headerMainBinding.accountProfilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(BaseMainActivity.this, ProfileActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT, currentAccount.mastodon_account);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(BaseMainActivity.this, headerMainBinding.instanceInfoContainer, getString(R.string.activity_porfile_pp));
            startActivity(intent, options.toBundle());
        });

        headerMainBinding.accountAcc.setOnClickListener(v -> headerMainBinding.changeAccount.callOnClick());
        headerMainBinding.changeAccount.setOnClickListener(v -> {
            headerMenuOpen = !headerMenuOpen;
            if (headerMenuOpen) {
                headerMainBinding.ownerAccounts.setImageResource(R.drawable.ic_baseline_arrow_drop_up_24);
                new Thread(() -> {
                    try {
                        List<BaseAccount> accounts = new Account(BaseMainActivity.this).getCrossAccounts();
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> {
                            binding.navView.getMenu().clear();
                            binding.navView.inflateMenu(R.menu.menu_accounts);
                            headerMenuOpen = true;

                            Menu mainMenu = binding.navView.getMenu();
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
                                        final MenuItem item = currentSubmenu.add("@" + account.mastodon_account.acct);
                                        item.setIcon(R.drawable.ic_person);
                                        boolean disableGif = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_GIF), false);
                                        String url = !disableGif ? account.mastodon_account.avatar : account.mastodon_account.avatar_static;
                                        if (url != null && url.startsWith("/")) {
                                            url = "https://" + account.instance + account.mastodon_account.avatar;
                                        }
                                        if (!this.isDestroyed() && !this.isFinishing() && url != null) {
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
                    if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.username != null && currentAccount.instance != null) {
                        alt_bld.setMessage(getString(R.string.logout_account_confirmation, currentAccount.mastodon_account.username, currentAccount.instance));
                    } else if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.acct != null) {
                        alt_bld.setMessage(getString(R.string.logout_account_confirmation, currentAccount.mastodon_account.acct, ""));
                    } else {
                        alt_bld.setMessage(getString(R.string.logout_account_confirmation, "", ""));
                    }
                    alt_bld.setPositiveButton(R.string.action_logout, (dialog, id) -> {
                        dialog.dismiss();
                        try {
                            Helper.removeAccount(BaseMainActivity.this);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
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
        currentAccount = null;
        //Update account details
        new Thread(() -> {
            try {
                currentAccount = new Account(BaseMainActivity.this).getConnectedAccount();
                //Delete cache older than 7 days
                new StatusCache(BaseMainActivity.this).deleteForAllAccountAfter7Days();
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                if (currentAccount == null) {
                    //It is not, the user is redirected to the login page
                    Intent myIntent = new Intent(BaseMainActivity.this, LoginActivity.class);
                    startActivity(myIntent);
                    finish();
                    return;
                }
                bottomMenu = new BottomMenu(BaseMainActivity.this).hydrate(currentAccount, binding.bottomNavView);
                if (currentAccount.mastodon_account.locked) {
                    binding.navView.getMenu().findItem(R.id.nav_follow_requests).setVisible(true);
                }
                if (currentAccount.admin) {
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

                currentInstance = currentAccount.instance;
                currentUserID = currentAccount.user_id;

                show_boosts = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_BOOSTS) + currentUserID + currentInstance, true);
                show_replies = sharedpreferences.getBoolean(getString(R.string.SET_SHOW_REPLIES) + currentUserID + currentInstance, true);
                regex_home = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, null);
                regex_local = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, null);
                regex_public = sharedpreferences.getString(getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, null);
                show_art_nsfw = sharedpreferences.getBoolean(getString(R.string.SET_ART_WITH_NSFW) + currentUserID + currentInstance, false);
                binding.profilePicture.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
                Helper.loadPP(binding.profilePicture, currentAccount);
                headerMainBinding.accountAcc.setText(String.format("%s@%s", currentAccount.mastodon_account.username, currentAccount.instance));
                if (currentAccount.mastodon_account.display_name == null || currentAccount.mastodon_account.display_name.isEmpty()) {
                    currentAccount.mastodon_account.display_name = currentAccount.mastodon_account.acct;
                }
                headerMainBinding.accountName.setText(currentAccount.mastodon_account.display_name);
                Helper.loadPP(headerMainBinding.accountProfilePicture, currentAccount, false);
                MastodonHelper.loadProfileMediaMastodon(headerMainBinding.backgroundImage, currentAccount.mastodon_account, MastodonHelper.MediaAccountType.HEADER);
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
                            slugOfFirstFragment = PinnedTimelineHelper.firstTimelineSlug(BaseMainActivity.this, pinned, bottomMenu);
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
                new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class).getFilters(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, filters -> mainFilters = filters);
                new ViewModelProvider(BaseMainActivity.this).get(AccountsVM.class).getConnectedAccount(currentInstance, currentToken)
                        .observe(BaseMainActivity.this, mastodonAccount -> {
                            //Initialize static var
                            currentAccount.mastodon_account = mastodonAccount;
                            displayReleaseNotesIfNeeded(BaseMainActivity.this, false);
                            new Thread(() -> {
                                try {
                                    //Update account in db
                                    new Account(BaseMainActivity.this).insertOrUpdate(currentAccount);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }).start();
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
        LocalBroadcastManager.getInstance(BaseMainActivity.this)
                .registerReceiver(broadcast_error_message,
                        new IntentFilter(Helper.INTENT_COMPOSE_ERROR_MESSAGE));
        if (emojis == null || !emojis.containsKey(BaseMainActivity.currentInstance) || emojis.get(BaseMainActivity.currentInstance) == null) {
            new Thread(() -> {
                try {
                    emojis.put(currentInstance, new EmojiInstance(BaseMainActivity.this).getEmojiList(BaseMainActivity.currentInstance));
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        boolean embedded_browser = sharedpreferences.getBoolean(getString(R.string.SET_EMBEDDED_BROWSER), true);
        if (embedded_browser) {
            DomainsBlock.updateDomains(BaseMainActivity.this);
        }
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
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(BaseMainActivity.this, Helper.popupStyle()), view, Gravity.TOP);
        popup.getMenuInflater()
                .inflate(R.menu.option_filter_toots, popup.getMenu());
        Menu menu = popup.getMenu();
        final MenuItem itemShowBoosts = menu.findItem(R.id.action_show_boosts);
        final MenuItem itemShowReplies = menu.findItem(R.id.action_show_replies);
        final MenuItem itemFilter = menu.findItem(R.id.action_filter);
        if (!showExtendedFilter) {
            itemShowBoosts.setVisible(false);
            itemShowReplies.setVisible(false);
        } else {
            itemShowBoosts.setVisible(true);
            itemShowReplies.setVisible(true);
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
        itemShowReplies.setChecked(show_replies);
        if (show_filtered != null && show_filtered.length() > 0) {
            itemFilter.setTitle(show_filtered);
        }
        popup.setOnDismissListener(menu1 -> {
            if (binding.viewPager.getAdapter() != null) {
                Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, binding.tabLayout.getSelectedTabPosition());
                if (fragment instanceof FragmentMastodonTimeline && fragment.isVisible()) {
                    FragmentMastodonTimeline fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
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
            } else if (itemId == R.id.action_show_replies) {
                show_replies = !show_replies;
                editor.putBoolean(getString(R.string.SET_SHOW_REPLIES) + currentUserID + currentInstance, show_replies);
                itemShowReplies.setChecked(show_replies);
                editor.apply();
            } else if (itemId == R.id.action_filter) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(BaseMainActivity.this, Helper.dialogStyle());
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
            Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, binding.tabLayout.getSelectedTabPosition());
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
                binding.bottomNavView.getBadge(R.id.nav_privates).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                binding.bottomNavView.getBadge(R.id.nav_privates).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
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
                binding.bottomNavView.getBadge(R.id.nav_notifications).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                binding.bottomNavView.getBadge(R.id.nav_notifications).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
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
                        binding.bottomNavView.getBadge(R.id.nav_home).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                        binding.bottomNavView.getBadge(R.id.nav_home).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_home);
                    }
                    break;
                case LOCAL:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_local).setNumber(count);
                        binding.bottomNavView.getBadge(R.id.nav_local).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                        binding.bottomNavView.getBadge(R.id.nav_local).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_local);
                    }
                    break;
                case PUBLIC:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_public).setNumber(count);
                        binding.bottomNavView.getBadge(R.id.nav_public).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                        binding.bottomNavView.getBadge(R.id.nav_public).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_public);
                    }
                    break;
                case NOTIFICATION:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_notifications).setNumber(count);
                        binding.bottomNavView.getBadge(R.id.nav_notifications).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                        binding.bottomNavView.getBadge(R.id.nav_notifications).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
                    } else {
                        binding.bottomNavView.removeBadge(R.id.nav_notifications);
                    }
                    break;
                case DIRECT:
                    if (count > 0) {
                        binding.bottomNavView.getOrCreateBadge(R.id.nav_privates).setNumber(count);
                        binding.bottomNavView.getBadge(R.id.nav_privates).setBackgroundColor(ContextCompat.getColor(BaseMainActivity.this, R.color.cyanea_accent_reference));
                        binding.bottomNavView.getBadge(R.id.nav_privates).setBadgeTextColor(ThemeHelper.getAttColor(BaseMainActivity.this, R.attr.mTextColor));
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
        LocalBroadcastManager.getInstance(BaseMainActivity.this).unregisterReceiver(broadcast_data);
        LocalBroadcastManager.getInstance(BaseMainActivity.this)
                .unregisterReceiver(broadcast_error_message);
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

    /**
     * Allow to scroll to top for bottom navigation items
     */
    private void scrollToTop() {

        if (binding.viewPager.getAdapter() != null) {
            Fragment fragment = (Fragment) binding.viewPager.getAdapter().instantiateItem(binding.viewPager, binding.tabLayout.getSelectedTabPosition());
            if (fragment instanceof FragmentMastodonTimeline) {
                FragmentMastodonTimeline fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                fragmentMastodonTimeline.scrollToTop();
            } else if (fragment instanceof FragmentMastodonConversation) {
                FragmentMastodonConversation fragmentMastodonConversation = ((FragmentMastodonConversation) fragment);
                fragmentMastodonConversation.scrollToTop();
            } else if (fragment instanceof FragmentNotificationContainer) {
                FragmentNotificationContainer fragmentNotificationContainer = ((FragmentNotificationContainer) fragment);
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