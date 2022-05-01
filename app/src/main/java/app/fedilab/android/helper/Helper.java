package app.fedilab.android.helper;
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

import static android.content.Context.DOWNLOAD_SERVICE;
import static app.fedilab.android.webview.ProxyHelper.setProxy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.jaredrummler.cyanea.Cyanea;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.LoginActivity;
import app.fedilab.android.activities.WebviewActivity;
import app.fedilab.android.broadcastreceiver.ToastMessage;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;
import app.fedilab.android.viewmodel.mastodon.OauthVM;
import app.fedilab.android.webview.CustomWebview;
import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class Helper {

    public static final String TAG = "fedilab_app";
    public static final String APP_CLIENT_ID = "APP_CLIENT_ID";
    public static final String APP_CLIENT_SECRET = "APP_CLIENT_SECRET";
    public static final String APP_INSTANCE = "APP_INSTANCE";
    public static final String APP_API = "APP_API";
    public static final String CLIP_BOARD = "CLIP_BOARD";

    public static final String INSTANCE_SOCIAL_KEY = "jGj9gW3z9ptyIpB8CMGhAlTlslcemMV6AgoiImfw3vPP98birAJTHOWiu5ZWfCkLvcaLsFZw9e3Pb7TIwkbIyrj3z6S7r2oE6uy6EFHvls3YtapP8QKNZ980p9RfzTb4";
    public static final String WEBSITE_VALUE = "https://fedilab.app";


    public static final String RECEIVE_TOAST_MESSAGE = "RECEIVE_TOAST_MESSAGE";
    public static final String RECEIVE_TOAST_TYPE = "RECEIVE_TOAST_TYPE";
    public static final String RECEIVE_TOAST_CONTENT = "RECEIVE_TOAST_CONTENT";
    public static final String RECEIVE_TOAST_TYPE_ERROR = "RECEIVE_TOAST_TYPE_ERROR";
    public static final String RECEIVE_TOAST_TYPE_INFO = "RECEIVE_TOAST_TYPE_INFO";
    public static final String RECEIVE_TOAST_TYPE_SUCCESS = "RECEIVE_TOAST_TYPE_SUCCESS";
    public static final String RECEIVE_TOAST_TYPE_WARNING = "RECEIVE_TOAST_TYPE_WARNING";

    //Intent
    public static final String INTENT_ACTION = "intent_action";

    public static final String BROADCAST_DATA = "BROADCAST_DATA";
    public static final String RECEIVE_REDRAW_TOPBAR = "RECEIVE_REDRAW_TOPBAR";
    public static final String RECEIVE_STATUS_ACTION = "RECEIVE_STATUS_ACTION";

    public static final String RECEIVE_RECREATE_ACTIVITY = "RECEIVE_RECREATE_ACTIVITY";
    public static final String RECEIVE_MASTODON_LIST = "RECEIVE_MASTODON_LIST";
    public static final String RECEIVE_REDRAW_PROFILE = "RECEIVE_REDRAW_PROFILE";

    public static final String ARG_TIMELINE_TYPE = "ARG_TIMELINE_TYPE";
    public static final String ARG_NOTIFICATION_TYPE = "ARG_NOTIFICATION_TYPE";
    public static final String ARG_EXCLUDED_NOTIFICATION_TYPE = "ARG_EXCLUDED_NOTIFICATION_TYPE";
    public static final String ARG_STATUS = "ARG_STATUS";
    public static final String ARG_STATUS_DELETED = "ARG_STATUS_DELETED";
    public static final String ARG_STATUS_ACTION = "ARG_STATUS_ACTION";
    public static final String ARG_STATUS_ACCOUNT_ID_DELETED = "ARG_STATUS_ACCOUNT_ID_DELETED";

    public static final String ARG_STATUS_DRAFT = "ARG_STATUS_DRAFT";
    public static final String ARG_STATUS_SCHEDULED = "ARG_STATUS_SCHEDULED";

    public static final String ARG_STATUS_DRAFT_ID = "ARG_STATUS_DRAFT_ID";
    public static final String ARG_STATUS_REPLY = "ARG_STATUS_REPLY";
    public static final String ARG_ACCOUNT = "ARG_ACCOUNT";
    public static final String ARG_ACCOUNT_MENTION = "ARG_ACCOUNT_MENTION";
    public static final String ARG_MINIFIED = "ARG_MINIFIED";
    public static final String ARG_STATUS_REPORT = "ARG_STATUS_REPORT";
    public static final String ARG_STATUS_MENTION = "ARG_STATUS_MENTION";
    public static final String ARG_FOLLOW_TYPE = "ARG_FOLLOW_TYPE";
    public static final String ARG_TYPE_OF_INFO = "ARG_TYPE_OF_INFO";
    public static final String ARG_TOKEN = "ARG_TOKEN";
    public static final String ARG_INSTANCE = "ARG_INSTANCE";
    public static final String ARG_REMOTE_INSTANCE = "ARG_REMOTE_INSTANCE";
    public static final String ARG_STATUS_ID = "ARG_STATUS_ID";
    public static final String ARG_WORK_ID = "ARG_WORK_ID";
    public static final String ARG_LIST_ID = "ARG_LIST_ID";
    public static final String ARG_SEARCH_KEYWORD = "ARG_SEARCH_KEYWORD";
    public static final String ARG_SEARCH_TYPE = "ARG_SEARCH_TYPE";
    public static final String ARG_SEARCH_KEYWORD_CACHE = "ARG_SEARCH_KEYWORD_CACHE";
    public static final String ARG_VIEW_MODEL_KEY = "ARG_VIEW_MODEL_KEY";
    public static final String ARG_TAG_TIMELINE = "ARG_TAG_TIMELINE";
    public static final String ARG_MEDIA_POSITION = "ARG_MEDIA_POSITION";
    public static final String ARG_MEDIA_ATTACHMENT = "ARG_MEDIA_ATTACHMENT";
    public static final String ARG_SHOW_REPLIES = "ARG_SHOW_REPLIES";
    public static final String ARG_SHOW_REBLOGS = "ARG_SHOW_REBLOGS";

    public static final String ARG_SHOW_PINNED = "ARG_SHOW_PINNED";
    public static final String ARG_SHOW_MEDIA_ONY = "ARG_SHOW_MEDIA_ONY";
    public static final String ARG_MENTION = "ARG_MENTION";
    public static final String ARG_USER_ID = "ARG_USER_ID";
    public static final String ARG_MEDIA_ARRAY = "ARG_MEDIA_ARRAY";
    public static final String ARG_VISIBILITY = "ARG_VISIBILITY";
    public static final String ARG_SCHEDULED_DATE = "ARG_SCHEDULED_DATE";

    public static final String WORKER_REFRESH_NOTIFICATION = "WORKER_REFRESH_NOTIFICATION";
    public static final String WORKER_SCHEDULED_STATUSES = "WORKER_SCHEDULED_STATUSES";
    public static final String WORKER_SCHEDULED_REBLOGS = "WORKER_SCHEDULED_REBLOGS";

    public static final String VALUE_TRENDS = "VALUE_TRENDS";

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0";
    public static final String REDIRECT_CONTENT_WEB = "fedilab://backtofedilab";
    public static final String REDIRECT_CONTENT = "urn:ietf:wg:oauth:2.0:oob";
    public static final String APP_OAUTH_SCOPES = "read write";
    public static final String OAUTH_SCOPES = "read write follow push";
    public static final String OAUTH_SCOPES_ADMIN = "read write follow push admin:read admin:write";
    public static final int DEFAULT_VIDEO_CACHE_MB = 100;
    public static final int LED_COLOUR = 0;


    public static final String SCHEDULE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final String PREF_USER_TOKEN = "PREF_USER_TOKEN";
    public static final String PREF_USER_ID = "PREF_USER_ID";
    public static final String PREF_USER_INSTANCE = "PREF_USER_INSTANCE";
    public static final String PREF_IS_MODERATOR = "PREF_IS_MODERATOR";
    public static final String PREF_IS_ADMINISTRATOR = "PREF_IS_ADMINISTRATOR";
    public static final String PREF_KEY_ID = "PREF_KEY_ID";
    public static final String PREF_INSTANCE = "PREF_INSTANCE";


    public static final int NOTIFICATION_INTENT = 1;
    public static final String INTENT_TARGETED_ACCOUNT = "INTENT_TARGETED_ACCOUNT";

    public static final String TEMP_MEDIA_DIRECTORY = "TEMP_MEDIA_DIRECTORY";


    public static final int EXTERNAL_STORAGE_REQUEST_CODE = 84;
    public static final int EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SAVE = 85;
    public static final int EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SHARE = 86;
    //Some regex
    /*public static final Pattern urlPattern = Pattern.compile(
            "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",

            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);*/

    public static final Pattern hashtagPattern = Pattern.compile("(#[\\w_A-zÀ-ÿ]+)");
    public static final Pattern groupPattern = Pattern.compile("(![\\w_]+)");
    public static final Pattern mentionPattern = Pattern.compile("(@[\\w_]+)");
    public static final Pattern mentionLongPattern = Pattern.compile("(@[\\w_-]+@[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+)");

    public static final Pattern twitterPattern = Pattern.compile("((@[\\w]+)@twitter\\.com)");
    public static final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    public static final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter.com([\\w-/]+)");
    public static final Pattern bibliogramPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(/p/[\\w-/]+)");
    public static final Pattern libredditPattern = Pattern.compile("(www\\.|m\\.)?(reddit\\.com|preview\\.redd\\.it|i\\.redd\\.it|redd\\.it)/(((?!([\"'<])).)*)");
    public static final Pattern ouichesPattern = Pattern.compile("https?://ouich\\.es/tag/(\\w+)");
    public static final Pattern xmppPattern = Pattern.compile("xmpp:[-a-zA-Z0-9+$&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    public static final Pattern mediumPattern = Pattern.compile("([\\w@-]*)?\\.?medium.com/@?([/\\w-]+)");
    public static final Pattern wikipediaPattern = Pattern.compile("([\\w_-]+)\\.wikipedia.org/(((?!([\"'<])).)*)");
    public static final Pattern codePattern = Pattern.compile("code=([\\w-]+)");

    // --- Static Map of patterns used in spannable status content
    public static final Map<PatternType, Pattern> patternHashMap;
    public static int counter = 1;

    static {
        Map<PatternType, Pattern> aMap = new HashMap<>();
        aMap.put(PatternType.MENTION, mentionPattern);
        aMap.put(PatternType.MENTION_LONG, mentionLongPattern);
        aMap.put(PatternType.TAG, hashtagPattern);
        aMap.put(PatternType.GROUP, groupPattern);
        patternHashMap = Collections.unmodifiableMap(aMap);
    }

    /***
     * Initialize a CustomWebview
     * @param activity Activity - activity containing the webview
     * @param webviewId int - webview id
     * @param rootView View - the root view that will contain the webview
     * @return {@link CustomWebview}
     */
    public static CustomWebview initializeWebview(Activity activity, int webviewId, View rootView) {

        CustomWebview webView;
        if (rootView == null) {
            webView = activity.findViewById(webviewId);
        } else {
            webView = rootView.findViewById(webviewId);
        }
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean javascript = sharedpreferences.getBoolean(activity.getString(R.string.SET_JAVASCRIPT), true);

        webView.getSettings().setJavaScriptEnabled(javascript);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(false);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        String user_agent = sharedpreferences.getString(activity.getString(R.string.SET_CUSTOM_USER_AGENT), USER_AGENT);
        webView.getSettings().setUserAgentString(user_agent);
        boolean cookies = sharedpreferences.getBoolean(activity.getString(R.string.SET_COOKIES), false);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, cookies);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
            }
        });
        boolean proxyEnabled = sharedpreferences.getBoolean(activity.getString(R.string.SET_PROXY_ENABLED), false);
        if (proxyEnabled) {
            String host = sharedpreferences.getString(activity.getString(R.string.SET_PROXY_HOST), "127.0.0.1");
            int port = sharedpreferences.getInt(activity.getString(R.string.SET_PROXY_PORT), 8118);
            setProxy(activity, webView, host, port, WebviewActivity.class.getName());
        }

        return webView;
    }

    /**
     * Manage downloads with URLs
     *
     * @param context Context
     * @param url     String download url
     */
    public static void manageDownloads(final Context context, final String url) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, Helper.dialogStyle());
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(url.trim()));
        } catch (Exception e) {
            Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        final String fileName = URLUtil.guessFileName(url, null, null);
        builder.setMessage(context.getResources().getString(R.string.download_file, fileName));
        builder.setCancelable(false)
                .setPositiveButton(context.getString(R.string.yes), (dialog, id) -> {
                    request.allowScanningByMediaScanner();
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                    assert dm != null;
                    dm.enqueue(request);
                    dialog.dismiss();
                })
                .setNegativeButton(context.getString(R.string.cancel), (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        if (alert.getWindow() != null)
            alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alert.show();
    }

    public static void colorizeIconMenu(Menu menu, int toolbarIconsColor) {
        final PorterDuffColorFilter colorFilter
                = new PorterDuffColorFilter(toolbarIconsColor, PorterDuff.Mode.MULTIPLY);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem v = menu.getItem(i);
            v.getIcon().setColorFilter(colorFilter);
        }
    }

    public static void installProvider() {

       /* boolean patch_provider = true;
        try {
            Context ctx = MainApplication.getApp();
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            patch_provider = sharedpreferences.getBoolean(Helper.SET_SECURITY_PROVIDER, true);
        } catch (Exception ignored) {
        }
        if (patch_provider) {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1);
            } catch (Exception ignored) {
            }
        }*/
    }

    /***
     *  Check if the user is connected to Internet
     * @return boolean
     */
    public static BaseMainActivity.status isConnectedToInternet(Context context, String instance) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return BaseMainActivity.status.CONNECTED;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnected()) {
            try {
                InetAddress ipAddr = InetAddress.getByName(instance);
                return (!ipAddr.toString().equals("")) ? BaseMainActivity.status.CONNECTED : BaseMainActivity.status.DISCONNECTED;
            } catch (Exception e) {
                try {
                    InetAddress ipAddr = InetAddress.getByName("mastodon.social");
                    return (!ipAddr.toString().equals("")) ? BaseMainActivity.status.CONNECTED : BaseMainActivity.status.DISCONNECTED;
                } catch (Exception ex) {
                    return BaseMainActivity.status.DISCONNECTED;
                }
            }
        } else {
            return BaseMainActivity.status.DISCONNECTED;
        }
    }

    /**
     * Returns boolean depending if the user is authenticated
     *
     * @param context Context
     * @return boolean
     */
    public static boolean isLoggedIn(Context context) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKeyOauthTokenT = sharedpreferences.getString(PREF_USER_TOKEN, null);
        return (prefKeyOauthTokenT != null);
    }

    /***
     * Returns a String depending of the date
     * @param context Context
     * @param date Date
     * @return String
     */
    public static String dateDiff(Context context, Date date) {
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        String format = DateFormat.getDateInstance(DateFormat.SHORT).format(date);
        if (years > 0) {
            return format;
        } else if (months > 0 || days > 7) {
            //Removes the year depending of the locale from DateFormat.SHORT format
            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            df.applyPattern(df.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
            return df.format(date);
        } else if (days > 0)
            return context.getString(R.string.date_day, days);
        else if (hours > 0)
            return context.getResources().getString(R.string.date_hours, (int) hours);
        else if (minutes > 0)
            return context.getResources().getString(R.string.date_minutes, (int) minutes);
        else {
            if (seconds < 0)
                seconds = 0;
            return context.getResources().getString(R.string.date_seconds, (int) seconds);
        }
    }

    /**
     * Convert a date in String -> format yyyy-MM-dd HH:mm:ss
     *
     * @param date Date
     * @return String
     */
    public static String dateToString(Date date) {
        if (date == null)
            return null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * Convert a date in String
     *
     * @param date Date
     * @return String
     */
    public static String longDateToString(Date date) {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        return df.format(date);
    }

    /**
     * Convert a date in String -> format yyyy-MM-dd HH:mm:ss
     *
     * @param date Date
     * @return String
     */
    public static String shortDateToString(Date date) {
        if (date == null) {
            date = new Date();
        }
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        return df.format(date);
    }

    /**
     * Convert String date from db to Date Object
     *
     * @param stringDate date to convert
     * @return Date
     */
    public static Date stringToDate(Context context, String stringDate) {
        if (stringDate == null)
            return null;
        Locale userLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            userLocale = context.getResources().getConfiguration().locale;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", userLocale);
        Date date = null;
        try {
            date = dateFormat.parse(stringDate);
        } catch (java.text.ParseException ignored) {

        }
        return date;
    }

    /**
     * Log out the authenticated user by removing its token
     *
     * @param activity Activity
     * @param account  {@link Account}
     * @throws DBException Exception
     */
    public static void removeAccount(Activity activity, Account account) throws DBException {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        //Current user
        SQLiteDatabase db = Sqlite.getInstance(activity.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        String userId = sharedpreferences.getString(PREF_USER_ID, null);
        String instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        Account accountDB = new Account(activity);
        boolean accountRemovedIsLogged = false;
        //Remove the current account
        if (account == null) {
            account = accountDB.getUniqAccount(userId, instance);
            accountRemovedIsLogged = true;
        }
        if (account != null) {
            Account finalAccount = account;
            OauthVM oauthVM = new ViewModelProvider((ViewModelStoreOwner) activity).get(OauthVM.class);
            //Revoke the token
            oauthVM.revokeToken(account.instance, account.token, account.client_id, account.client_secret);
            //Revoke token and remove user
            new Thread(() -> {
                try {
                    accountDB.removeUser(finalAccount);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        //If the account removed is not the logged one, no need to log out the current user
        if (!accountRemovedIsLogged) {
            return;
        }
        //Log out the current user
        Account newAccount = accountDB.getLastUsedAccount();
        SharedPreferences.Editor editor = sharedpreferences.edit();
        if (newAccount == null) {
            editor.putString(PREF_USER_TOKEN, null);
            editor.putString(PREF_USER_INSTANCE, null);
            editor.putString(PREF_USER_ID, null);
            editor.apply();
            Intent loginActivity = new Intent(activity, LoginActivity.class);
            activity.startActivity(loginActivity);
            activity.finish();
        } else {
            editor.putString(PREF_USER_TOKEN, newAccount.token);
            editor.putString(PREF_USER_INSTANCE, newAccount.instance);
            editor.putString(PREF_USER_ID, newAccount.user_id);
            BaseMainActivity.currentUserID = newAccount.user_id;
            BaseMainActivity.currentToken = newAccount.token;
            BaseMainActivity.currentInstance = newAccount.instance;
            editor.apply();
            Intent changeAccount = new Intent(activity, BaseMainActivity.class);
            changeAccount.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(changeAccount);
        }

    }

    /**
     * Converts dp to pixel
     *
     * @param dp      float - the value in dp to convert
     * @param context Context
     * @return float - the converted value in pixel
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Manage URLs to open (built-in or external app)
     *
     * @param context Context
     * @param url     String url to open
     */
    public static void openBrowser(Context context, String url) {
        url = transformURL(context, url);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean embedded_browser = sharedpreferences.getBoolean(context.getString(R.string.SET_EMBEDDED_BROWSER), true);
        if (embedded_browser && !url.toLowerCase().startsWith("gemini://")) {
            Intent intent = new Intent(context, WebviewActivity.class);
            Bundle b = new Bundle();
            String finalUrl = url;
            if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
                finalUrl = "http://" + url;
            b.putString("url", finalUrl);
            intent.putExtras(b);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(url));
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Transform URLs to privacy frontend
     *
     * @param context Context
     * @param url     String
     */
    private static String transformURL(Context context, String url) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Matcher matcher = Helper.nitterPattern.matcher(url);
        boolean nitter = Helper.getSharedValue(context, context.getString(R.string.SET_NITTER));
        if (nitter) {
            if (matcher.find()) {
                final String nitter_directory = matcher.group(2);
                String nitterHost = sharedpreferences.getString(context.getString(R.string.SET_NITTER_HOST), context.getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
                return "https://" + nitterHost + nitter_directory;
            }
        }
        matcher = Helper.bibliogramPattern.matcher(url);
        boolean bibliogram = Helper.getSharedValue(context, context.getString(R.string.SET_BIBLIOGRAM));
        if (bibliogram) {
            if (matcher.find()) {
                final String bibliogram_directory = matcher.group(2);
                String bibliogramHost = sharedpreferences.getString(context.getString(R.string.SET_BIBLIOGRAM_HOST), context.getString(R.string.DEFAULT_BIBLIOGRAM_HOST)).toLowerCase();
                return "https://" + bibliogramHost + bibliogram_directory;
            }
        }
        matcher = Helper.libredditPattern.matcher(url);
        boolean libreddit = Helper.getSharedValue(context, context.getString(R.string.SET_LIBREDDIT));
        if (libreddit) {
            if (matcher.find()) {
                final String libreddit_directory = matcher.group(3);
                String libreddit_host = sharedpreferences.getString(context.getString(R.string.SET_LIBREDDIT_HOST), context.getString(R.string.DEFAULT_LIBREDDIT_HOST)).toLowerCase();
                return "https://" + libreddit_host + "/" + libreddit_directory;
            }
        }
        matcher = Helper.youtubePattern.matcher(url);
        boolean invidious = Helper.getSharedValue(context, context.getString(R.string.SET_INVIDIOUS));
        if (invidious) {
            if (matcher.find()) {
                final String youtubeId = matcher.group(3);
                String invidiousHost = sharedpreferences.getString(context.getString(R.string.SET_INVIDIOUS_HOST), context.getString(R.string.DEFAULT_INVIDIOUS_HOST)).toLowerCase();
                if (matcher.group(2) != null && Objects.equals(matcher.group(2), "youtu.be")) {
                    return "https://" + invidiousHost + "/watch?v=" + youtubeId + "&local=true";
                } else {
                    return "https://" + invidiousHost + "/" + youtubeId + "&local=true";
                }
            }
        }
        matcher = Helper.mediumPattern.matcher(url);
        boolean medium = Helper.getSharedValue(context, context.getString(R.string.REPLACE_MEDIUM));
        if (medium) {
            if (matcher.find()) {
                String path = matcher.group(2);
                String user = matcher.group(1);
                if (user != null && user.length() > 0 & !user.equals("www")) {
                    path = user + "/" + path;
                }
                String mediumReplaceHost = sharedpreferences.getString(context.getString(R.string.REPLACE_MEDIUM_HOST), context.getString(R.string.DEFAULT_REPLACE_MEDIUM_HOST)).toLowerCase();
                return "https://" + mediumReplaceHost + "/" + path;
            }
        }
        matcher = Helper.wikipediaPattern.matcher(url);
        boolean wikipedia = Helper.getSharedValue(context, context.getString(R.string.REPLACE_WIKIPEDIA));
        if (wikipedia) {
            if (matcher.find()) {
                String subdomain = matcher.group(1);
                String path = matcher.group(2);
                String wikipediaReplaceHost = sharedpreferences.getString(context.getString(R.string.REPLACE_WIKIPEDIA_HOST), context.getString(R.string.DEFAULT_REPLACE_WIKIPEDIA_HOST)).toLowerCase();
                String lang = "";
                if (path != null && subdomain != null && !subdomain.equals("www")) {
                    lang = (path.contains("?")) ? TextUtils.htmlEncode("&") : "?";
                    lang = lang + "lang=" + subdomain;
                }
                return "https://" + wikipediaReplaceHost + "/" + path + lang;
            }
        }
        return url;
    }

    @SuppressLint("DefaultLocale")
    public static String withSuffix(long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        Locale locale = null;
        try {
            locale = Locale.getDefault();
        } catch (Exception ignored) {
        }
        if (locale != null)
            return String.format(locale, "%.1f %c",
                    count / Math.pow(1000, exp),
                    "kMGTPE".charAt(exp - 1));
        else
            return String.format("%.1f %c",
                    count / Math.pow(1000, exp),
                    "kMGTPE".charAt(exp - 1));
    }

    /**
     * Send a toast message to main activity
     *
     * @param context Context
     * @param type    String - type of the toast (error, warning, info, success)
     * @param content String - message of the toast
     */
    public static void sendToastMessage(Context context, String type, String content) {
        Intent intentBC = new Intent(context, ToastMessage.class);
        Bundle b = new Bundle();
        b.putString(RECEIVE_TOAST_TYPE, type);
        b.putString(RECEIVE_TOAST_CONTENT, content);
        intentBC.setAction(Helper.RECEIVE_TOAST_MESSAGE);
        intentBC.putExtras(b);
        context.sendBroadcast(intentBC);
    }

    /**
     * @param fragmentManager Fragment Manager
     * @param containerViewId Id of the fragment container
     * @param fragment        Fragment to be added
     * @param args            Arguments to pass to the new fragment. null for none
     * @param tag             Tag to pass to the fragment
     * @param backStackName   An optional name to use when adding to back stack, or null.
     */
    public static Fragment addFragment(@NonNull FragmentManager fragmentManager,
                                       @IdRes int containerViewId,
                                       @NonNull Fragment fragment,
                                       @Nullable Bundle args,
                                       @Nullable String tag,
                                       @Nullable String backStackName) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        Fragment _fragment = fragmentManager.findFragmentByTag(tag);
        if (_fragment != null && _fragment.isAdded()) {
            ft.show(_fragment).commit();
            fragment = _fragment;
        } else {
            if (args != null) fragment.setArguments(args);
            ft.add(containerViewId, fragment, tag);
            if (backStackName != null) ft.addToBackStack(backStackName);
            ft.commit();
        }
        fragmentManager.executePendingTransactions();
        return fragment;
    }

    public static int dialogStyle() {
        return Cyanea.getInstance().isDark() ? R.style.DialogDark : R.style.Dialog;
    }

    public static int popupStyle() {
        return Cyanea.getInstance().isDark() ? R.style.PopupDark : R.style.Popup;
    }

    /**
     * Load a profile picture for the account
     *
     * @param view    ImageView - the view where the image will be loaded
     * @param account - {@link Account}
     */
    public static void loadPP(ImageView view, Account account) {
        Context context = view.getContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean disableGif = sharedpreferences.getBoolean(context.getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = disableGif ? account.mastodon_account.avatar_static : account.mastodon_account.avatar;
        if (disableGif || (!targetedUrl.endsWith(".gif"))) {
            Glide.with(view.getContext())
                    .asDrawable()
                    .load(targetedUrl)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(view);
        } else {
            Glide.with(view.getContext())
                    .asGif()
                    .load(targetedUrl)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(view);
        }
    }

    /**
     * Check if the app is not finishing
     *
     * @param context - Context
     * @return boolean - context is valid and image can be loaded
     */
    public static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }

    /**
     * Get filename from uri
     *
     * @param context Context
     * @param uri     Uri
     * @return String
     */
    public static String getFileName(Context context, Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        Cursor returnCursor =
                resolver.query(uri, null, null, null, null);
        if (returnCursor != null) {
            try {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                String name = returnCursor.getString(nameIndex);
                returnCursor.close();
                Random r = new Random();
                int suf = r.nextInt(9999 - 1000) + 1000;
                return suf + name;
            } catch (Exception e) {
                Random r = new Random();
                int suf = r.nextInt(9999 - 1000) + 1000;
                ContentResolver cr = context.getContentResolver();
                String mime = cr.getType(uri);
                if (mime != null && mime.split("/").length > 1)
                    return "__" + suf + "." + mime.split("/")[1];
                else
                    return "__" + suf + ".jpg";
            }
        } else {
            Random r = new Random();
            int suf = r.nextInt(9999 - 1000) + 1000;
            ContentResolver cr = context.getContentResolver();
            String mime = cr.getType(uri);
            if (mime != null && mime.split("/").length > 1)
                return "__" + suf + "." + mime.split("/")[1];
            else
                return "__" + suf + ".jpg";
        }
    }

    /**
     * Return shared value
     *
     * @param context Context
     * @param type    String
     * @return boolean
     */
    public static boolean getSharedValue(Context context, String type) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (type.compareTo(context.getString(R.string.SET_INVIDIOUS)) == 0) {
            return sharedpreferences.getBoolean(type, false);
        } else if (type.compareTo(context.getString(R.string.SET_BIBLIOGRAM)) == 0) {
            return sharedpreferences.getBoolean(type, false);
        } else if (type.compareTo(context.getString(R.string.SET_NITTER)) == 0) {
            return sharedpreferences.getBoolean(type, false);
        } else if (type.compareTo(context.getString(R.string.REPLACE_MEDIUM)) == 0) {
            return sharedpreferences.getBoolean(type, false);
        } else if (type.compareTo(context.getString(R.string.REPLACE_WIKIPEDIA)) == 0) {
            return sharedpreferences.getBoolean(type, false);
        }
        return sharedpreferences.getBoolean(type, false);
    }

    /**
     * Get size from uri
     *
     * @param context Context
     * @param uri     Uri - uri to check
     * @return long - file size
     */
    public static long getRealSizeFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Audio.Media.SIZE};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            cursor.moveToFirst();
            return Long.parseLong(cursor.getString(column_index));
        } catch (Exception e) {
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    /**
     * Sends notification with intent
     *
     * @param context Context
     * @param intent  Intent associated to the notifcation
     * @param icon    Bitmap profile picture
     * @param title   String title of the notification
     * @param message String message for the notification
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    public static void notify_user(Context context, Account account, Intent intent, Bitmap icon, NotifType notifType, String title, String message) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // prepare intent which is triggered if the user click on the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = (int) System.currentTimeMillis();
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        } else {
            pIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_ONE_SHOT);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        // build notification
        String channelId;
        String channelTitle;

        switch (notifType) {
            case FAV:
                channelId = "channel_favourite";
                channelTitle = context.getString(R.string.channel_notif_fav);
                break;
            case FOLLLOW:
                channelId = "channel_follow";
                channelTitle = context.getString(R.string.channel_notif_follow);
                break;
            case MENTION:
                channelId = "channel_mention";
                channelTitle = context.getString(R.string.channel_notif_mention);
                break;
            case POLL:
                channelId = "channel_poll";
                channelTitle = context.getString(R.string.channel_notif_poll);
                break;
            case BACKUP:
                channelId = "channel_backup";
                channelTitle = context.getString(R.string.channel_notif_backup);
                break;
            case STORE:
                channelId = "channel_store";
                channelTitle = context.getString(R.string.channel_notif_media);
                break;
            case TOOT:
                channelId = "channel_status";
                channelTitle = context.getString(R.string.channel_notif_status);
                break;
            default:
                channelId = "channel_boost";
                channelTitle = context.getString(R.string.channel_notif_boost);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification).setTicker(message)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
        if (notifType == NotifType.MENTION) {
            if (message.length() > 500) {
                message = message.substring(0, 499) + "…";
            }
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }
        notificationBuilder.setGroup(account.mastodon_account.acct + "@" + account.instance)
                .setContentIntent(pIntent)
                .setContentText(message);
        int ledColour = Color.BLUE;
        int prefColor;
        try {
            prefColor = sharedpreferences.getInt(context.getString(R.string.SET_LED_COLOUR_VAL), LED_COLOUR);
        } catch (ClassCastException e) {
            prefColor = Integer.parseInt(sharedpreferences.getString(context.getString(R.string.SET_LED_COLOUR_VAL), String.valueOf(LED_COLOUR)));
        }
        switch (prefColor) {
            case 0: // BLUE
                ledColour = Color.BLUE;
                break;
            case 1: // CYAN
                ledColour = Color.CYAN;
                break;
            case 2: // MAGENTA
                ledColour = Color.MAGENTA;
                break;
            case 3: // GREEN
                ledColour = Color.GREEN;
                break;
            case 4: // RED
                ledColour = Color.RED;
                break;
            case 5: // YELLOW
                ledColour = Color.YELLOW;
                break;
            case 6: // WHITE
                ledColour = Color.WHITE;
                break;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel;
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_SILENT), false)) {
                channel = new NotificationChannel(channelId, channelTitle, NotificationManager.IMPORTANCE_LOW);
                channel.setSound(null, null);
                channel.setVibrationPattern(new long[]{500, 500, 500});
                channel.enableVibration(true);
                channel.setLightColor(ledColour);
            } else {
                channel = new NotificationChannel(channelId, channelTitle, NotificationManager.IMPORTANCE_DEFAULT);
                String soundUri = sharedpreferences.getString(context.getString(R.string.SET_NOTIF_SOUND), ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.boop);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(Uri.parse(soundUri), audioAttributes);
            }
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(channel);
        } else {
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_SILENT), false)) {
                notificationBuilder.setVibrate(new long[]{500, 500, 500});
            } else {
                String soundUri = sharedpreferences.getString(context.getString(R.string.SET_NOTIF_SOUND), ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.boop);
                notificationBuilder.setSound(Uri.parse(soundUri));
            }
            notificationBuilder.setLights(ledColour, 500, 1000);
        }
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setLargeIcon(icon);
        notificationManager.notify(notificationId, notificationBuilder.build());

        Notification summaryNotification =
                new NotificationCompat.Builder(context, channelId)
                        .setContentTitle(title)
                        .setContentText(channelTitle)
                        .setContentIntent(pIntent)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setGroup(account.mastodon_account.acct + "@" + account.instance)
                        .setGroupSummary(true)
                        .build();
        notificationManager.notify(0, summaryNotification);
    }

    /**
     * Retrieves the cache size
     *
     * @param directory File
     * @return long value in Mo
     */
    public static long cacheSize(File directory) {
        long length = 0;
        if (directory == null || directory.length() == 0)
            return -1;
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile()) {
                try {
                    length += file.length();
                } catch (NullPointerException e) {
                    return -1;
                }
            } else {
                if (!file.getName().equals("databases") && !file.getName().equals("shared_prefs")) {
                    length += cacheSize(file);
                }
            }
        }
        return length;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String aChildren : children) {
                if (!aChildren.equals("databases") && !aChildren.equals("shared_prefs")) {
                    boolean success = deleteDir(new File(dir, aChildren));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else {
            return dir != null && dir.isFile() && dir.delete();
        }
    }

    public static Proxy getProxy(Context context) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String hostVal = sharedpreferences.getString(context.getString(R.string.SET_PROXY_HOST), "127.0.0.1");
        int portVal = sharedpreferences.getInt(context.getString(R.string.SET_PROXY_PORT), 8118);
        final String login = sharedpreferences.getString(context.getString(R.string.SET_PROXY_LOGIN), null);
        final String pwd = sharedpreferences.getString(context.getString(R.string.SET_PROXY_PASSWORD), null);
        final int type = sharedpreferences.getInt(context.getString(R.string.SET_PROXY_TYPE), 0);
        boolean enable_proxy = sharedpreferences.getBoolean(context.getString(R.string.SET_PROXY_ENABLED), false);
        if (!enable_proxy) {
            return null;
        }
        Proxy proxy = new Proxy(type == 0 ? Proxy.Type.HTTP : Proxy.Type.SOCKS,
                new InetSocketAddress(hostVal, portVal));
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost().equalsIgnoreCase(hostVal)) {
                    if (portVal == getRequestingPort()) {
                        return new PasswordAuthentication(login, pwd.toCharArray());
                    }
                }
                return null;
            }
        });
        return proxy;
    }

    /***
     * Convert Uri to byte[]
     * @param context Context
     * @param uri Uri
     * @return byte[]
     */
    public static byte[] uriToByteArray(Context context, Uri uri) {
        byte[] buffer = null;
        try {
            InputStream iStream = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            buffer = new byte[bufferSize];
            int len;
            while ((len = iStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * Creates MultipartBody.Part from Uri
     *
     * @return MultipartBody.Part for the given Uri
     */
    public static MultipartBody.Part getMultipartBody(@NonNull String paramName, @NonNull Attachment attachment) {
        RequestBody requestFile = RequestBody.create(MediaType.parse(attachment.mimeType), new File(attachment.local_path));
        return MultipartBody.Part.createFormData(paramName, attachment.filename, requestFile);
    }

    public static MultipartBody.Part getMultipartBody(Context context, @NonNull String paramName, @NonNull Uri uri) {
        byte[] imageBytes = uriToByteArray(context, uri);
        ContentResolver cR = context.getApplicationContext().getContentResolver();
        String mimeType = cR.getType(uri);
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageBytes);
        return MultipartBody.Part.createFormData(paramName, Helper.getFileName(context, uri), requestFile);
    }

    public static void createAttachmentFromUri(Context context, List<Uri> uris, OnAttachmentCopied callBack) {
        new Thread(() -> {
            for (Uri uri : uris) {
                Attachment attachment = new Attachment();
                attachment.filename = Helper.getFileName(context, uri);
                attachment.size = Helper.getRealSizeFromUri(context, uri);
                ContentResolver cR = context.getApplicationContext().getContentResolver();
                attachment.mimeType = cR.getType(uri);

                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String extension = mime.getExtensionFromMimeType(cR.getType(uri));
                if (uri.toString().endsWith("fedilab_recorded_audio.wav")) {
                    extension = "wav";
                    attachment.mimeType = "audio/x-wav";
                }
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_" + counter, Locale.getDefault());
                counter++;
                Date now = new Date();
                attachment.filename = formatter.format(now) + "." + extension;
                InputStream selectedFileInputStream;
                try {
                    selectedFileInputStream = context.getContentResolver().openInputStream(uri);
                    if (selectedFileInputStream != null) {
                        final File certCacheDir = new File(context.getCacheDir(), TEMP_MEDIA_DIRECTORY);
                        boolean isCertCacheDirExists = certCacheDir.exists();
                        if (!isCertCacheDirExists) {
                            isCertCacheDirExists = certCacheDir.mkdirs();
                        }
                        if (isCertCacheDirExists) {
                            String filePath = certCacheDir.getAbsolutePath() + "/" + attachment.filename;
                            attachment.local_path = filePath;
                            OutputStream selectedFileOutPutStream = new FileOutputStream(filePath);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = selectedFileInputStream.read(buffer)) > 0) {
                                selectedFileOutPutStream.write(buffer, 0, length);
                            }
                            selectedFileOutPutStream.flush();
                            selectedFileOutPutStream.close();
                        }
                        selectedFileInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> callBack.onAttachmentCopied(attachment);
                mainHandler.post(myRunnable);
            }
        }).start();
    }

    /**
     * change color of a drawable
     *
     * @param imageView int the ImageView
     * @param hexaColor example 0xffff00
     */
    public static void changeDrawableColor(Context context, ImageView imageView, int hexaColor) {
        if (imageView == null)
            return;
        int color;
        try {
            color = context.getResources().getColor(hexaColor);
        } catch (Resources.NotFoundException e) {
            color = hexaColor;
        }
        imageView.setColorFilter(color);
    }

    /**
     * change color of a drawable
     *
     * @param drawable  int the drawable
     * @param hexaColor example 0xffff00
     */
    public static Drawable changeDrawableColor(Context context, int drawable, int hexaColor) {
        Drawable mDrawable = ContextCompat.getDrawable(context, drawable);
        int color;
        try {
            color = Color.parseColor(context.getString(hexaColor));
        } catch (Resources.NotFoundException e) {
            try {
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = context.getTheme();
                theme.resolveAttribute(hexaColor, typedValue, true);
                color = typedValue.data;
            } catch (Resources.NotFoundException ed) {
                color = hexaColor;
            }
        }
        assert mDrawable != null;
        mDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        DrawableCompat.setTint(mDrawable, color);
        return mDrawable;
    }

    /**
     * Convert a date in String -> format yyyy-MM-dd HH:mm:ss
     *
     * @param context Context
     * @param date    Date
     * @return String
     */
    public static String dateFileToString(Context context, Date date) {
        Locale userLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            userLocale = context.getResources().getConfiguration().locale;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", userLocale);
        return dateFormat.format(date);
    }

    /**
     * Change locale
     *
     * @param activity - Activity
     */
    public static void setLocale(Activity activity) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String defaultLocaleString = sharedpreferences.getString(activity.getString(R.string.SET_DEFAULT_LOCALE_NEW), null);
        if (defaultLocaleString != null) {
            Locale locale;
            if (defaultLocaleString.equals("zh-CN")) {
                locale = Locale.SIMPLIFIED_CHINESE;
            } else if (defaultLocaleString.equals("zh-TW")) {
                locale = Locale.TRADITIONAL_CHINESE;
            } else {
                locale = new Locale(defaultLocaleString);
            }
            Locale.setDefault(locale);
            Resources resources = activity.getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());

        }
    }

    //Enum that described actions to replace inside a toot content
    public enum PatternType {
        MENTION,
        MENTION_LONG,
        TAG,
        GROUP
    }

    public enum NotifType {
        FOLLLOW,
        MENTION,
        BOOST,
        FAV,
        POLL,
        STATUS,
        BACKUP,
        STORE,
        TOOT
    }

    public interface OnAttachmentCopied {
        void onAttachmentCopied(Attachment attachment);
    }

    public static class CacheTask {
        private final WeakReference<Context> contextReference;
        private float cacheSize;

        public CacheTask(Context context) {
            contextReference = new WeakReference<>(context);
            doInBackground();
        }

        protected void doInBackground() {
            new Thread(() -> {
                long sizeCache = cacheSize(contextReference.get().getCacheDir().getParentFile());
                cacheSize = 0;
                if (sizeCache > 0) {
                    cacheSize = (float) sizeCache / 1000000.0f;
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(contextReference.get(), Helper.dialogStyle());
                    LayoutInflater inflater = ((BaseMainActivity) contextReference.get()).getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.popup_cache, new LinearLayout(contextReference.get()), false);
                    TextView message = dialogView.findViewById(R.id.message);
                    message.setText(contextReference.get().getString(R.string.cache_message, String.format("%s %s", String.format(Locale.getDefault(), "%.2f", cacheSize), contextReference.get().getString(R.string.cache_units))));
                    builder.setView(dialogView);
                    builder.setTitle(R.string.cache_title);

                    final SwitchCompat clean_all = dialogView.findViewById(R.id.clean_all);
                    final float finalCacheSize = cacheSize;
                    builder
                            .setPositiveButton(R.string.clear, (dialog, which) -> new Thread(() -> {
                                try {
                                    String path = Objects.requireNonNull(contextReference.get().getCacheDir().getParentFile()).getPath();
                                    File dir = new File(path);
                                    if (dir.isDirectory()) {
                                        deleteDir(dir);
                                    }
                                    if (clean_all.isChecked()) {

                                    } else {

                                    }
                                    Handler mainHandler2 = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable2 = () -> {
                                        Toasty.success(contextReference.get(), contextReference.get().getString(R.string.toast_cache_clear, String.format("%s %s", String.format(Locale.getDefault(), "%.2f", finalCacheSize), contextReference.get().getString(R.string.cache_units))), Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    };
                                    mainHandler2.post(myRunnable2);
                                } catch (Exception ignored) {
                                }
                            }).start())
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                };
                mainHandler.post(myRunnable);
            }).start();
        }
    }

    public static void showKeyboard(Context context, View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
}
