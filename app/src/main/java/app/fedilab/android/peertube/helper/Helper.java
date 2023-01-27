package app.fedilab.android.peertube.helper;
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

import static android.content.Context.DOWNLOAD_SERVICE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.mastodon.helper.Helper.dialogStyle;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.typeOfConnection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.avatarfirst.avatargenlib.AvatarGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.activities.WebviewActivity;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.File;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.webview.ProxyHelper;
import es.dmoral.toasty.Toasty;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class Helper {

    public static final int RELOAD_MYVIDEOS = 10;
    public static final String SET_VIDEO_MODE = "set_video_mode";
    public static final String SET_QUALITY_MODE = "set_quality_mode";
    public static final String SET_THEME = "set_theme";
    public static final int LIGHT_MODE = 0;
    public static final int DARK_MODE = 1;
    public static final int DEFAULT_MODE = 2;
    public static final String DO_NOT_LIST = "do_not_list";
    public static final String BLUR = "blur";
    public static final String DISPLAY = "display";
    public static final String TIMELINE_TYPE = "timeline_type";
    public static final int VIDEO_MODE_NORMAL = 0;
    public static final int VIDEO_MODE_MAGNET = 0;
    public static final int VIDEO_MODE_TORRENT = 0;
    public static final int VIDEO_MODE_WEBVIEW = 1;
    public static final int QUALITY_HIGH = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_LOW = 2;
    public static final int ADD_USER_INTENT = 5;
    public static final String SET_SHARE_DETAILS = "set_share_details";
    public static final String NOTIFICATION_INTERVAL = "notification_interval";
    public static final String LAST_NOTIFICATION_READ = "last_notification_read";
    public static final String REDIRECT_CONTENT_WEB = "tubelab://backtotubelab";
    public static final int DEFAULT_VIDEO_CACHE_MB = 100;
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static final String ID = "id";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String WEBSITE_VALUE = "https://fedilab.app";
    public static final String CLIENT_NAME_VALUE = "TubeLab";
    public static final String OAUTH_SCOPES_PEERTUBE = "openid profile";
    public static final String OAUTH_SCOPES_MASTODON = "read write follow";
    public static final String REDIRECT_CONTENT = "urn:ietf:wg:oauth:2.0:oob";
    public static final String PREF_SOFTWARE = "pref_software";
    public static final String PREF_REMOTE_INSTANCE = "pref_remote_instance";
    public static final Pattern redirectPattern = Pattern.compile("externalAuthToken=(\\w+)&username=([\\w.-]+)");
    public static final String SET_VIDEO_CACHE = "set_video_cache";
    public static final String RECEIVE_CAST_SETTINGS = "receive_cast_settings";
    //Proxy
    public static final String SET_PROXY_ENABLED = "set_proxy_enabled";
    public static final String SET_PROXY_HOST = "set_proxy_host";
    public static final String SET_PROXY_PORT = "set_proxy_port";
    public static final String INTENT_ACTION = "intent_action";
    public static final int EXTERNAL_STORAGE_REQUEST_CODE = 84;
    public static final String SET_VIDEOS_PER_PAGE = "set_videos_per_page";
    public static final String VIDEO_ID = "video_id_update";
    public static final String APP_PREFS = "app_prefs";
    public static final String CAST_ID = "D402501A";
    public static final int VIDEOS_PER_PAGE = 10;
    public static final String RECEIVE_ACTION = "receive_action";
    public static final String SET_UNFOLLOW_VALIDATION = "set_unfollow_validation";
    public static PeertubeInformation peertubeInformation;

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
     * Convert second to String formated date
     *
     * @param pTime timestamp
     * @return String formatted value
     */
    public static String secondsToString(int pTime) {

        int hour = pTime / 3600;
        int min = (pTime - (hour * 3600)) / 60;
        int sec = pTime - (hour * 3600) - (min * 60);
        String strHour = "0", strMin = "0", strSec;

        if (hour > 0)
            strHour = String.format(Locale.getDefault(), "%02d", hour);
        if (min > 0)
            strMin = String.format(Locale.getDefault(), "%02d", min);
        strSec = String.format(Locale.getDefault(), "%02d", sec);
        if (hour > 0)
            return String.format(Locale.getDefault(), "%s:%s:%s", strHour, strMin, strSec);
        else
            return String.format(Locale.getDefault(), "%s:%s", strMin, strSec);
    }

    /***
     * Returns a String depending of the date
     * @param context Context
     * @param dateToot Date
     * @return String
     */
    public static String dateDiff(Context context, Date dateToot) {
        Date now = new Date();
        long diff = now.getTime() - dateToot.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        String format = DateFormat.getDateInstance(DateFormat.LONG).format(dateToot);
        if (years > 0) {
            return format;
        } else if (months > 0 || days > 7) {
            //Removes the year depending of the locale from DateFormat.SHORT format
            try {
                SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
                df.applyPattern(df.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
                return df.format(dateToot);
            } catch (Exception e) {
                return format;
            }
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
     * Return rounded numbers depending of the value
     *
     * @param count long
     * @return String rounded value to be displayed
     */
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
            return String.format(Locale.getDefault(), "%.1f %c",
                    count / Math.pow(1000, exp),
                    "kMGTPE".charAt(exp - 1));
    }


    public static void loadGiF(final Context context, String url, final ImageView imageView) {
        loadGiF(context, url, imageView, 10);
    }

    public static void loadGiF(final Context context, String instance, String url, final ImageView imageView, boolean blur) {
        loadGif(context, instance, url, imageView, 10, blur);
    }

    public static void loadGiF(final Context context, String instance, String url, final ImageView imageView) {
        loadGif(context, instance, url, imageView, 10, false);
    }

    public static void loadGiF(final Context context, String url, final ImageView imageView, int round) {
        loadGif(context, null, url, imageView, round, false);
    }


    public static void loadAvatar(final Context context, AccountData.PeertubeAccount account, final ImageView imageView) {
        String url = null;
        if (account.getAvatar() != null) {
            url = account.getAvatar().getPath();
        }
        String instance = account.getHost();
        if (url == null || url.trim().toLowerCase().compareTo("null") == 0 || url.endsWith("null")) {
            BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(context)
                    .setLabel(account.getAcct())
                    .setAvatarSize(120)
                    .setTextSize(30)
                    .toSquare()
                    .setBackgroundColor(Helper.fetchAccentColor(context))
                    .build();
            Glide.with(imageView.getContext())
                    .asDrawable()
                    .load(avatar)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(imageView);
            return;
        }
        if (url.startsWith("/")) {
            url = instance != null ? instance + url : HelperInstance.getLiveInstance(context) + url;
        }
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        try {
            RequestBuilder<Drawable> requestBuilder = Glide.with(imageView.getContext())
                    .load(url)
                    .thumbnail(0.1f);
            requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(imageView);
        } catch (Exception e) {
            try {
                BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(context)
                        .setLabel(account.getAcct())
                        .setAvatarSize(120)
                        .setTextSize(30)
                        .toSquare()
                        .setBackgroundColor(fetchAccentColor(context))
                        .build();
                Glide.with(imageView.getContext())
                        .asDrawable()
                        .load(avatar)
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                        .into(imageView);

            } catch (Exception ignored) {
            }
        }
    }

    public static void loadAvatar(final Context context, ChannelData.Channel channel, final ImageView imageView) {
        String url = null;
        if (channel.getAvatar() != null) {
            url = channel.getAvatar().getPath();
        }
        String instance = channel.getHost();
        if (url == null || url.trim().toLowerCase().compareTo("null") == 0 || url.endsWith("null")) {
            BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(context)
                    .setLabel(channel.getAcct())
                    .setAvatarSize(120)
                    .setTextSize(30)
                    .toSquare()
                    .setBackgroundColor(fetchAccentColor(context))
                    .build();
            Glide.with(imageView.getContext())
                    .asDrawable()
                    .load(avatar)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(imageView);
            return;
        }
        if (url.startsWith("/")) {
            url = instance != null ? instance + url : HelperInstance.getLiveInstance(context) + url;
        }
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        try {
            RequestBuilder<Drawable> requestBuilder = Glide.with(imageView.getContext())
                    .load(url);
            requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                            imageView.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(context)
                                    .setLabel(channel.getAcct())
                                    .setAvatarSize(120)
                                    .setTextSize(30)
                                    .toSquare()
                                    .setBackgroundColor(fetchAccentColor(context))
                                    .build();
                            Glide.with(imageView.getContext())
                                    .asDrawable()
                                    .load(avatar)
                                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                                    .into(imageView);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } catch (Exception e) {
            try {
                BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(context)
                        .setLabel(channel.getAcct())
                        .setAvatarSize(120)
                        .setTextSize(30)
                        .toSquare()
                        .setBackgroundColor(fetchAccentColor(context))
                        .build();
                Glide.with(imageView.getContext())
                        .asDrawable()
                        .load(avatar)
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                        .into(imageView);

            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("SameParameterValue")
    private static void loadGif(final Context context, String instance, String url, final ImageView imageView, int round, boolean blur) {
        if (url == null || url.trim().toLowerCase().compareTo("null") == 0 || url.endsWith("null")) {
            Glide.with(imageView.getContext())
                    .asDrawable()
                    .load(R.drawable.missing_peertube)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(imageView);
            return;
        }
        if (url.startsWith("/")) {
            url = instance != null ? instance + url : HelperInstance.getLiveInstance(context) + url;
        }
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        try {
            RequestBuilder<Drawable> requestBuilder = Glide.with(imageView.getContext())
                    .load(url)
                    .thumbnail(0.1f);
            if (blur) {
                requestBuilder.apply(new RequestOptions().transform(new BlurTransformation(50, 3), new CenterCrop(), new RoundedCorners(10)));
            } else {
                requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(round)));
            }
            requestBuilder.into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                    imageView.setImageDrawable(resource);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    Glide.with(imageView.getContext())
                            .asDrawable()
                            .load(R.drawable.missing_peertube)
                            .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                            .into(imageView);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
        } catch (Exception e) {
            try {
                Glide.with(imageView.getContext())
                        .asDrawable()
                        .load(R.drawable.missing_peertube)
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(round)))
                        .into(imageView);

            } catch (Exception ignored) {
            }
        }
    }


    /**
     * Initialize the webview
     *
     * @param activity  Current Activity
     * @param webviewId int id of the webview layout
     * @param rootView  View the root view
     * @return CustomWebview
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView initializeWebview(Activity activity, int webviewId, View rootView) {

        WebView webView;
        if (rootView == null) {
            webView = activity.findViewById(webviewId);
        } else {
            webView = rootView.findViewById(webviewId);
        }
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(false);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView, false);
        }
        webView.setBackgroundColor(Color.TRANSPARENT);
        //webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
            }
        });
        boolean proxyEnabled = sharedpreferences.getBoolean(Helper.SET_PROXY_ENABLED, false);
        if (proxyEnabled) {
            String host = sharedpreferences.getString(Helper.SET_PROXY_HOST, "127.0.0.1");
            int port = sharedpreferences.getInt(Helper.SET_PROXY_PORT, 8118);
            ProxyHelper.setProxy(activity, webView, host, port, WebviewActivity.class.getName());
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
        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context, dialogStyle());
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


    /**
     * Log out the authenticated user by removing its token
     *
     * @param activity       Activity
     * @param currentAccount BaseAccount
     */
    public static void logoutCurrentUser(Activity activity, BaseAccount currentAccount) {
        AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(activity, dialogStyle());
        alt_bld.setTitle(R.string.action_logout);
        if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.username != null && currentAccount.instance != null) {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, currentAccount.mastodon_account.username, currentAccount.instance));
        } else if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.acct != null) {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, currentAccount.mastodon_account.acct, ""));
        } else {
            alt_bld.setMessage(activity.getString(R.string.logout_account_confirmation, "", ""));
        }
        alt_bld.setPositiveButton(R.string.action_logout, (dialog, id) -> {
            dialog.dismiss();
            try {
                app.fedilab.android.mastodon.helper.Helper.removeAccount(activity);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });
        alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        AlertDialog alert = alt_bld.create();
        alert.show();
    }


    /**
     * Log out without removing user in db
     *
     * @param activity Activity
     */
    public static void logoutNoRemoval(Activity activity) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(PREF_USER_TOKEN, null);
        editor.putString(CLIENT_ID, null);
        editor.putString(CLIENT_SECRET, null);
        editor.putString(PREF_USER_ID, null);
        editor.putString(PREF_USER_INSTANCE, null);
        editor.putString(PREF_USER_SOFTWARE, null);
        editor.apply();
        Intent loginActivity = new Intent(activity, PeertubeMainActivity.class);
        activity.startActivity(loginActivity);
        activity.finish();
    }


    public static int getAttColor(Context context, int attColor) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attColor, typedValue, true);
        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static int fetchAccentColor(Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    /**
     * Returns boolean depending if the user is authenticated
     *
     * @return boolean
     */
    public static boolean isLoggedIn() {
        return typeOfConnection == PeertubeMainActivity.TypeOfConnection.NORMAL;
    }


    public static String getToken(Context context) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedpreferences.getString(PREF_USER_TOKEN, null);
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


    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.is_tablet);
    }


    public static boolean isOwner(Context context, AccountData.PeertubeAccount account) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = sharedpreferences.getString(PREF_USER_ID, "");
        String instance = sharedpreferences.getString(PREF_USER_INSTANCE, "");
        return account.getId().compareTo(userId) == 0 && account.getHost().compareTo(instance) == 0;
    }

    public static boolean isVideoOwner(Context context, VideoData.Video video) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = sharedpreferences.getString(PREF_USER_ID, null);
        String instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        if (video == null) {
            return false;
        }
        if (userId == null || instance == null) {
            return false;
        }
        AccountData.PeertubeAccount account = video.getAccount();
        ChannelData.Channel channel = video.getChannel();
        if (account != null && account.getId() != null && account.getHost() != null) {
            return account.getId().compareTo(userId) == 0 && account.getHost().compareTo(instance) == 0;
        } else if (channel != null && channel.getOwnerAccount() != null && channel.getOwnerAccount().getId() != null && channel.getOwnerAccount().getHost() != null) {
            return channel.getOwnerAccount().getId().compareTo(userId) == 0 && channel.getHost().compareTo(instance) == 0;
        } else {
            return false;
        }
    }

    /***
     * Return a File depending of the requested quality
     * @param context Context
     * @param files List<File>
     * @return File
     */
    public static File defaultFile(Context context, List<File> files) {
        if (files == null || files.size() == 0) {
            return null;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int video_quality = sharedpreferences.getInt(Helper.SET_QUALITY_MODE, Helper.QUALITY_HIGH);
        if (video_quality == QUALITY_HIGH) {
            return files.get(0);
        } else if (video_quality == QUALITY_LOW) {
            if (files.get(files.size() - 1).getResolutions().getLabel().trim().toLowerCase().compareTo("0p") != 0) {
                return files.get(files.size() - 1);
            } else {
                if (files.size() > 1) {
                    return files.get(files.size() - 2);
                } else {
                    return files.get(0);
                }
            }
        } else {
            if (files.size() < 3) {
                return files.get(files.size() - 1);
            } else {
                int middle = files.size() / 2 - 1;
                return files.get(middle);
            }
        }
    }

    /**
     * Forward the intent (open an URL) to another app
     *
     * @param activity Activity
     * @param i        Intent
     */
    public static void forwardToAnotherApp(Activity activity, Intent i) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(i.getData(), i.getType());
        List<ResolveInfo> activities = activity.getPackageManager().queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        String thisPackageName = activity.getPackageName();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(Intent.ACTION_VIEW);
                targetIntent.setDataAndType(intent.getData(), intent.getType());
                targetIntent.setPackage(intent.getPackage());
                targetIntent.setComponent(new ComponentName(packageName, currentInfo.activityInfo.name));
                targetIntents.add(targetIntent);
            }
        }
        if (targetIntents.size() > 0) {
            Intent chooserIntent = Intent.createChooser(targetIntents.remove(0), activity.getString(R.string.open_with));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[]{}));
            activity.startActivity(chooserIntent);
        }
    }


    public static boolean instanceOnline(String host) {
        try {
            InetAddress ipAddr = InetAddress.getByName(host);
            return ipAddr.toString().trim().compareTo("") != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String returnRoundedSize(Context context, long size) {
        if (size == -1) {
            return context.getString(R.string.unlimited);
        } else if (size > 1000000000) {
            float rounded = (float) size / 1000000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.gb));
        } else {
            float rounded = (float) size / 1000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.mb));
        }
    }


    public static String rateSize(Context context, long size) {
        if (size > 1000000000) {
            float rounded = (float) size / 1000000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.gb));
        } else if (size > 1000000) {
            float rounded = (float) size / 1000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.mb));
        } else if (size > 1000) {
            float rounded = (float) size / 1000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.kb));
        } else {
            float rounded = (float) size / 1000000;
            DecimalFormat df = new DecimalFormat("#.#");
            return String.format(Locale.getDefault(), "%s%s", df.format(rounded), context.getString(R.string.b));
        }
    }

    public static void requestPermissionAndProceed(Activity activity, PermissionGranted permissionGranted) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, app.fedilab.android.mastodon.helper.Helper.EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SAVE);
                } else {
                    permissionGranted.proceed();
                }
            } else {
                permissionGranted.proceed();
            }
        } else {
            permissionGranted.proceed();
        }
    }

    public interface PermissionGranted {
        void proceed();
    }


    public static String readFileFromAssets(Context context, String filename) {
        String json;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

}
