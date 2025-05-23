package app.fedilab.android.mastodon.helper;
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
import static app.fedilab.android.BaseMainActivity.networkAvailable;
import static app.fedilab.android.mastodon.activities.BaseActivity.currentThemeId;
import static app.fedilab.android.mastodon.helper.LogoHelper.getNotificationIcon;
import static app.fedilab.android.mastodon.helper.ThemeHelper.fetchAccentColor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.avatarfirst.avatargenlib.AvatarGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.LoginActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.PopupReleaseNotesBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.broadcastreceiver.ToastMessage;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.ReleaseNote;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.interfaces.OnDownloadInterface;
import app.fedilab.android.mastodon.ui.drawer.ReleaseNoteAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.OauthVM;
import app.fedilab.android.mastodon.watermark.androidwm.WatermarkBuilder;
import app.fedilab.android.mastodon.watermark.androidwm.bean.WatermarkText;
import app.fedilab.android.peertube.client.data.AccountData;
import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Helper {

    public static final String TAG = "fedilab_app";
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
    public static final String RECEIVE_REDRAW_BOTTOM = "RECEIVE_REDRAW_BOTTOM";

    public static final String RECEIVE_STATUS_ACTION = "RECEIVE_STATUS_ACTION";

    public static final String RECEIVE_REFRESH_NOTIFICATIONS_ACTION = "RECEIVE_REFRESH_NOTIFICATIONS_ACTION";
    public static final String RECEIVE_ERROR_MESSAGE = "RECEIVE_ERROR_MESSAGE";

    public static final String RECEIVE_RECREATE_ACTIVITY = "RECEIVE_RECREATE_ACTIVITY";
    public static final String RECEIVE_RECREATE_PEERTUBE_ACTIVITY = "RECEIVE_RECREATE_PEERTUBE_ACTIVITY";

    public static final String RECEIVE_NEW_MESSAGE = "RECEIVE_NEW_MESSAGE";
    public static final String RECEIVE_COMPOSE_ERROR_MESSAGE = "RECEIVE_COMPOSE_ERROR_MESSAGE";
    public static final String RECEIVE_MASTODON_LIST = "RECEIVE_MASTODON_LIST";
    public static final String RECEIVE_REDRAW_PROFILE = "RECEIVE_REDRAW_PROFILE";

    public static final String ARG_TIMELINE_TYPE = "ARG_TIMELINE_TYPE";

    public static final String ARG_INTENT_ID = "ARG_INTENT_ID";
    public static final String ARG_PEERTUBE_NAV_REMOTE = "ARG_PEERTUBE_NAV_REMOTE";

    public static final String ARG_REMOTE_INSTANCE_STRING = "ARG_REMOTE_INSTANCE_STRING";

    public static final String ARG_NOTIFICATION_TYPE = "ARG_NOTIFICATION_TYPE";
    public static final String ARG_EXCLUDED_NOTIFICATION_TYPE = "ARG_EXCLUDED_NOTIFICATION_TYPE";
    public static final String ARG_STATUS = "ARG_STATUS";
    public static final String ARG_QR_CODE_URL = "ARG_QR_CODE_URL";
    public static final String ARG_FOCUSED_STATUS_URI = "ARG_FOCUSED_STATUS_URI";
    public static final String ARG_TIMELINE_REFRESH_ALL = "ARG_TIMELINE_REFRESH_ALL";
    public static final String ARG_REFRESH_NOTFICATION = "ARG_REFRESH_NOTFICATION";
    public static final String ARG_STATUS_DELETED = "ARG_STATUS_DELETED";
    public static final String ARG_STATUS_UPDATED = "ARG_STATUS_UPDATED";

    public static final String ARG_STATUS_POSTED = "ARG_STATUS_POSTED";
    public static final String ARG_STATUS_ACTION = "ARG_STATUS_ACTION";
    public static final String ARG_DELETE_ALL_FOR_ACCOUNT_ID = "ARG_DELETE_ALL_FOR_ACCOUNT_ID";
    public static final String ARG_STATUS_ACCOUNT_ID_DELETED = "ARG_STATUS_ACCOUNT_ID_DELETED";

    public static final String ARG_STATUS_DRAFT = "ARG_STATUS_DRAFT";
    public static final String ARG_EDIT_STATUS_ID = "ARG_EDIT_STATUS_ID";

    public static final String ARG_STATUS_SCHEDULED = "ARG_STATUS_SCHEDULED";
    public static final String ARG_SLUG_OF_FIRST_FRAGMENT = "ARG_SLUG_OF_FIRST_FRAGMENT";

    public static final String ARG_STATUS_DRAFT_ID = "ARG_STATUS_DRAFT_ID";
    public static final String ARG_STATUS_REPLY = "ARG_STATUS_REPLY";
    public static final String ARG_MENTION_BOOSTER = "ARG_MENTION_BOOSTER";
    public static final String ARG_QUOTED_MESSAGE = "ARG_QUOTED_MESSAGE";
    public static final String ARG_STATUS_REPLY_ID = "ARG_STATUS_REPLY_ID";
    public static final String ARG_ACCOUNT = "ARG_ACCOUNT";
    public static final String ARG_ACCOUNT_ID = "ARG_ACCOUNT_ID";
    public static final String ARG_CACHED_ACCOUNT_ID = "ARG_CACHED_ACCOUNT_ID";
    public static final String ARG_CACHED_STATUS_ID = "ARG_CACHED_STATUS_ID";
    public static final String ARG_ADMIN_DOMAINBLOCK = "ARG_ADMIN_DOMAINBLOCK";
    public static final String ARG_ADMIN_DOMAINBLOCK_DELETE = "ARG_ADMIN_DOMAINBLOCK_DELETE";
    public static final String FEDILAB_MUTED_HASHTAGS = "Fedilab muted hashtags";
    public static final String ARG_REPORT = "ARG_REPORT";
    public static final String ARG_ACCOUNT_MENTION = "ARG_ACCOUNT_MENTION";
    public static final String ARG_MINIFIED = "ARG_MINIFIED";
    public static final String ARG_STATUS_REPORT = "ARG_STATUS_REPORT";
    public static final String ARG_STATUS_MENTION = "ARG_STATUS_MENTION";
    public static final String ARG_SHARE_URL_MEDIA = "ARG_SHARE_URL_MEDIA";
    public static final String ARG_SHARE_URL = "ARG_SHARE_URL";
    public static final String ARG_SHARE_TITLE = "ARG_SHARE_TITLE";
    public static final String ARG_SHARE_SUBJECT = "ARG_SHARE_SUBJECT";
    public static final String ARG_SHARE_DESCRIPTION = "ARG_SHARE_DESCRIPTION";
    public static final String ARG_SHARE_CONTENT = "ARG_SHARE_CONTENT";
    public static final String ARG_FOLLOW_TYPE = "ARG_FOLLOW_TYPE";
    public static final String ARG_TYPE_OF_INFO = "ARG_TYPE_OF_INFO";
    public static final String ARG_TOKEN = "ARG_TOKEN";
    public static final String ARG_INSTANCE = "ARG_INSTANCE";
    public static final String ARG_REMOTE_INSTANCE = "ARG_REMOTE_INSTANCE";
    public static final String ARG_STATUS_ID = "ARG_STATUS_ID";
    public static final String ARG_WORK_ID = "ARG_WORK_ID";
    public static final String ARG_LIST_ID = "ARG_LIST_ID";
    public static final String ARG_LEMMY_POST_ID = "ARG_LEMMY_POST_ID";

    public static final String ARG_SEARCH_KEYWORD = "ARG_SEARCH_KEYWORD";
    public static final String ARG_DIRECTORY_ORDER = "ARG_DIRECTORY_ORDER";
    public static final String ARG_DIRECTORY_LOCAL = "ARG_DIRECTORY_LOCAL";
    public static final String ARG_SEARCH_TYPE = "ARG_SEARCH_TYPE";
    public static final String ARG_SEARCH_KEYWORD_CACHE = "ARG_SEARCH_KEYWORD_CACHE";
    public static final String ARG_VIEW_MODEL_KEY = "ARG_VIEW_MODEL_KEY";
    public static final String ARG_TAG_TIMELINE = "ARG_TAG_TIMELINE";
    public static final String ARG_BUBBLE_TIMELINE = "ARG_BUBBLE_TIMELINE";
    public static final String ARG_MEDIA_POSITION = "ARG_MEDIA_POSITION";
    public static final String ARG_MEDIA_ATTACHMENT = "ARG_MEDIA_ATTACHMENT";
    public static final String ARG_MEDIA_ATTACHMENTS = "ARG_MEDIA_ATTACHMENTS";
    public static final String ARG_SHOW_REPLIES = "ARG_SHOW_REPLIES";
    public static final String ARG_SHOW_REBLOGS = "ARG_SHOW_REBLOGS";
    public static final String ARG_INITIALIZE_VIEW = "ARG_INITIALIZE_VIEW";
    public static final String ARG_SHOW_PINNED = "ARG_SHOW_PINNED";
    public static final String ARG_SHOW_MEDIA_ONY = "ARG_SHOW_MEDIA_ONY";
    public static final String ARG_MENTION = "ARG_MENTION";
    public static final String ARG_CHECK_REMOTELY = "ARG_CHECK_REMOTELY";
    public static final String ARG_USER_ID = "ARG_USER_ID";
    public static final String ARG_MEDIA_ARRAY = "ARG_MEDIA_ARRAY";
    public static final String ARG_MEDIA_ARRAY_PROFILE = "ARG_MEDIA_ARRAY_PROFILE";
    public static final String ARG_VISIBILITY = "ARG_VISIBILITY";
    public static final String ARG_SCHEDULED_DATE = "ARG_SCHEDULED_DATE";
    public static final String ARG_SCHEDULED_ID = "ARG_SCHEDULED_ID";

    public static final String WORKER_REFRESH_NOTIFICATION = "WORKER_REFRESH_NOTIFICATION";
    public static final String WORKER_REFRESH_HOME = "WORKER_REFRESH_HOME";
    public static final String WORKER_SCHEDULED_STATUSES = "WORKER_SCHEDULED_STATUSES";
    public static final String WORKER_SCHEDULED_REBLOGS = "WORKER_SCHEDULED_REBLOGS";

    public static final String VALUE_TRENDS = "VALUE_TRENDS";

    //public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0";
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
    public static final String PREF_USER_INSTANCE_PEERTUBE_BROWSING = "PREF_USER_INSTANCE_PEERTUBE_BROWSING";


    public static final String PREF_USER_SOFTWARE = "PREF_USER_SOFTWARE";
    public static final String PREF_IS_MODERATOR = "PREF_IS_MODERATOR";
    public static final String PREF_IS_ADMINISTRATOR = "PREF_IS_ADMINISTRATOR";
    public static final String PREF_MESSAGE_URL = "PREF_MESSAGE_URL";


    public static final String SET_SECURITY_PROVIDER = "SET_SECURITY_PROVIDER";

    public static final int NOTIFICATION_INTENT = 1;
    public static final int OPEN_NOTIFICATION = 2;
    public static final int OPEN_WITH_ANOTHER_ACCOUNT = 3;
    public static final String INTENT_TARGETED_ACCOUNT = "INTENT_TARGETED_ACCOUNT";
    public static final String INTENT_TARGETED_STATUS = "INTENT_TARGETED_STATUS";
    public static final String INTENT_SEND_MODIFIED_IMAGE = "INTENT_SEND_MODIFIED_IMAGE";
    public static final String INTENT_COMPOSE_ERROR_MESSAGE = "INTENT_COMPOSE_ERROR_MESSAGE";
    public static final String TEMP_MEDIA_DIRECTORY = "TEMP_MEDIA_DIRECTORY";
    public static final String TEMP_EXPORT_DATA = "TEMP_EXPORT_DATA";

    public static final int EXTERNAL_STORAGE_REQUEST_CODE = 84;
    public static final int EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SAVE = 85;
    public static final int EXTERNAL_STORAGE_REQUEST_CODE_MEDIA_SHARE = 86;
    //Some regex
    /*public static final Pattern urlPattern = Pattern.compile(
            "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",

            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);*/

    public static final Pattern hashtagPattern = Pattern.compile("(#[\\w_A-zÀ-ÿ]+)");
    public static final Pattern groupPattern = Pattern.compile("(![\\w_]+)");
    public static final Pattern mentionPattern = Pattern.compile("(@[\\w_.-]?[\\w]+)");
    public static final Pattern mentionLongPattern = Pattern.compile("(@[\\w_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+)");


    public static final Pattern mentionPatternALL = Pattern.compile("(@[\\w_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+)|(@[\\w_.-]?[\\w]+)");
    public static final Pattern mathsPattern = Pattern.compile("\\\\\\(|\\\\\\[");
    public static final Pattern mathsComposePattern = Pattern.compile("\\\\\\(.*\\\\\\)|\\\\\\[.*\\\\\\]");
    public static final Pattern twitterPattern = Pattern.compile("((@[\\w]+)@twitter\\.com)");
    public static final Pattern youtubePattern = Pattern.compile("(www\\.|m\\.)?(youtube\\.com|youtu\\.be|youtube-nocookie\\.com)/(((?!([\"'<])).)*)");
    public static final Pattern nitterPattern = Pattern.compile("(mobile\\.|www\\.)?twitter\\.com([\\w/-]+)");
    public static final Pattern bibliogramPattern = Pattern.compile("(m\\.|www\\.)?instagram.com(/p/[\\w-/]+)");
    public static final Pattern libredditPattern = Pattern.compile("(www\\.|m\\.)?(reddit\\.com|preview\\.redd\\.it|i\\.redd\\.it|redd\\.it)/(((?!([\"'<])).)*)");
    public static final Pattern ouichesPattern = Pattern.compile("https?://ouich\\.es/tag/(\\w+)");

    public static final Pattern geminiPattern = Pattern.compile("(gemini://.*)\\b");
    public static final Pattern xmppPattern = Pattern.compile("xmpp:[-a-zA-Z0-9+$&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    public static final Pattern peertubePattern = Pattern.compile("(https?://([\\da-z.-]+\\.[a-z.]{2,10}))/videos/watch/(\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})$");
    public static final Pattern mediumPattern = Pattern.compile("([\\w@-]*)?\\.?medium.com/@?([/\\w-]+)");
    public static final Pattern wikipediaPattern = Pattern.compile("([\\w_-]+)\\.wikipedia.org/(((?!([\"'<])).)*)");
    public static final Pattern codePattern = Pattern.compile("code=([\\w-]+)");
    public static final Pattern nitterIDPattern = Pattern.compile("/status/(\\d+)");
    public static final Pattern emailPattern = Pattern.compile("(\\s+[\\w_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+)");
    public static final Pattern statusIdInUrl = Pattern.compile("statuses/(\\w+)");

    /*public static final Pattern urlPattern = Pattern.compile(
            "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,10}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))",

            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static final Pattern urlPatternSimple = Pattern.compile(
            "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)",

            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);*/

    public static final Pattern aLink = Pattern.compile("<a((?!href).)*href=\"([^\"]*)\"[^>]*(((?!</a).)*)</a>");
    public static final Pattern imgPattern = Pattern.compile("<img [^>]*src=\"([^\"]+)\"[^>]*>");

    // --- Static Map of patterns used in spannable status content
    public static final Map<PatternType, Pattern> patternHashMap;
    public static final int NOTIFICATION_MEDIA = 451;
    public static final int NOTIFICATION_USER_NOTIF = 411;
    public static final int NOTIFICATION_THEMING = 412;
    /*
     * List from ClearUrls
     * https://gitlab.com/KevinRoebert/ClearUrls/blob/master/data/data.min.json#L106
     */
    private static final String[] UTM_PARAMS = {
            "utm_\\w+",
            "ga_source",
            "ga_medium",
            "ga_term",
            "ga_content",
            "ga_campaign",
            "ga_place",
            "yclid",
            "_openstat",
            "fb_action_ids",
            "fb_action_types",
            "fb_source",
            "fb_ref",
            "fbclid",
            "action_object_map",
            "action_type_map",
            "action_ref_map",
            "gs_l",
            "mkt_tok",
            "hmb_campaign",
            "hmb_medium",
            "hmb_source",
            "[\\?|&]ref[\\_]?"

    };
    public static int counter = 1;
    private static int notificationId = 1;
    //Allow to store in shared preference first visible fragment when the app starts
    private static String slugOfFirstFragment;
    private static BaseAccount baseAccount;

    static {
        LinkedHashMap<PatternType, Pattern> aMap = new LinkedHashMap<>();
        aMap.put(PatternType.MENTION_LONG, mentionLongPattern);
        aMap.put(PatternType.MENTION, mentionPattern);
        aMap.put(PatternType.TAG, hashtagPattern);
        aMap.put(PatternType.GROUP, groupPattern);
        patternHashMap = Collections.unmodifiableMap(aMap);
    }

    /**
     * Manage downloads with URLs
     *
     * @param context Context
     * @param url     String download url
     */
    public static void manageDownloads(final Context context, final String url) {
        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
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
        if (date == null) {
            date = new Date();
        }
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;
        long years = days / 365;

        if (years > 0) {
            return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date);
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
        if (date == null) {
            date = new Date();
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        return df.format(date);
    }

    /**
     * Convert a date in String
     *
     * @param date Date
     * @return String
     */
    public static String mediumDateToString(Date date) {
        if (date == null) {
            date = new Date();
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
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
     * Convert String date from db to Date Object
     *
     * @param stringDate date to convert
     * @return Date
     */
    public static Date stringToDateWithFormat(Context context, String stringDate, String format) {
        if (stringDate == null)
            return new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
        Date date = new Date();
        try {
            date = dateFormat.parse(stringDate);
        } catch (java.text.ParseException ignored) {
            ignored.printStackTrace();
        }
        return date;
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
        if (url == null) {
            return;
        }
        /*if(networkAvailable == BaseMainActivity.status.DISCONNECTED){
            Toasty.warning(context, context.getString(R.string.toast_error_internet), Toast.LENGTH_LONG).show();
            return;
        }*/
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean customTab = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOM_TABS), true);
        if (customTab) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            int colorInt = ThemeHelper.getAttColor(context, R.attr.statusBar);
            CustomTabColorSchemeParams defaultColors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(colorInt)
                    .build();
            builder.setDefaultColorSchemeParams(defaultColors);
            CustomTabsIntent customTabsIntent = builder.build();
            try {
                customTabsIntent.launchUrl(context, Uri.parse(url).normalizeScheme());
            } catch (Exception e) {
                if(url.toLowerCase().startsWith("xmpp:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("https://xmpp.link/#" + url.replace("xmpp:","")).normalizeScheme());
                    context.startActivity(intent);
                } else {
                    Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            }

        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://") && !url.toLowerCase().startsWith("gemini://") && !url.toLowerCase().startsWith("xmpp:")) {
                url = "http://" + url;
            }
            intent.setData(Uri.parse(url).normalizeScheme());
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                if(url.toLowerCase().startsWith("xmpp:")) {
                    intent.setData(Uri.parse("https://xmpp.link/#" + url.replace("xmpp:","")).normalizeScheme());
                    context.startActivity(intent);
                } else {
                    Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Transform URLs to privacy frontend
     *
     * @param context Context
     * @param url     String
     */
    public static String transformURL(Context context, String url) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Matcher matcher;
        boolean nitter = Helper.getSharedValue(context, context.getString(R.string.SET_NITTER));
        if (nitter) {
            matcher = Helper.nitterPattern.matcher(url);
            if (matcher.find()) {
                final String nitter_directory = matcher.group(2);
                String nitterHost = sharedpreferences.getString(context.getString(R.string.SET_NITTER_HOST), context.getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
                if (nitterHost.trim().isEmpty()) {
                    nitterHost = context.getString(R.string.DEFAULT_NITTER_HOST);
                }
                return "https://" + nitterHost + nitter_directory;
            }
        }

        boolean bibliogram = Helper.getSharedValue(context, context.getString(R.string.SET_BIBLIOGRAM));

        if (bibliogram) {
            matcher = Helper.bibliogramPattern.matcher(url);
            if (matcher.find()) {
                final String bibliogram_directory = matcher.group(2);
                String bibliogramHost = sharedpreferences.getString(context.getString(R.string.SET_BIBLIOGRAM_HOST), context.getString(R.string.DEFAULT_BIBLIOGRAM_HOST)).toLowerCase();
                if (bibliogramHost.trim().isEmpty()) {
                    bibliogramHost = context.getString(R.string.DEFAULT_BIBLIOGRAM_HOST);
                }
                return "https://" + bibliogramHost + bibliogram_directory;
            }
        }

        boolean libreddit = Helper.getSharedValue(context, context.getString(R.string.SET_LIBREDDIT));
        if (libreddit) {
            matcher = Helper.libredditPattern.matcher(url);
            if (matcher.find()) {
                final String libreddit_directory = matcher.group(3);
                String libreddit_host = sharedpreferences.getString(context.getString(R.string.SET_LIBREDDIT_HOST), context.getString(R.string.DEFAULT_LIBREDDIT_HOST)).toLowerCase();
                if (libreddit_host.trim().isEmpty()) {
                    libreddit_host = context.getString(R.string.DEFAULT_LIBREDDIT_HOST);
                }
                return "https://" + libreddit_host + "/" + libreddit_directory;
            }
        }

        boolean invidious = Helper.getSharedValue(context, context.getString(R.string.SET_INVIDIOUS));
        if (invidious) {
            matcher = Helper.youtubePattern.matcher(url);
            if (matcher.find()) {
                final String youtubeId = matcher.group(3);
                String invidiousHost = sharedpreferences.getString(context.getString(R.string.SET_INVIDIOUS_HOST), context.getString(R.string.DEFAULT_INVIDIOUS_HOST)).toLowerCase();
                if (invidiousHost.trim().isEmpty()) {
                    invidiousHost = context.getString(R.string.DEFAULT_INVIDIOUS_HOST);
                }
                if (matcher.group(2) != null && Objects.equals(matcher.group(2), "youtu.be")) {
                    return "https://" + invidiousHost + "/watch?v=" + youtubeId + "&local=true";
                } else {
                    return "https://" + invidiousHost + "/" + youtubeId + "&local=true";
                }
            }
        }

        boolean medium = Helper.getSharedValue(context, context.getString(R.string.REPLACE_MEDIUM));
        if (medium) {
            matcher = Helper.mediumPattern.matcher(url);
            if (matcher.find()) {
                String path = matcher.group(2);
                String user = matcher.group(1);
                if (user != null && user.length() > 0 & !user.equals("www")) {
                    path = user + "/" + path;
                }
                String mediumReplaceHost = sharedpreferences.getString(context.getString(R.string.REPLACE_MEDIUM_HOST), context.getString(R.string.DEFAULT_REPLACE_MEDIUM_HOST)).toLowerCase();
                if (mediumReplaceHost.trim().isEmpty()) {
                    mediumReplaceHost = context.getString(R.string.DEFAULT_REPLACE_MEDIUM_HOST);
                }
                return "https://" + mediumReplaceHost + "/" + path;
            }
        }

        boolean wikipedia = Helper.getSharedValue(context, context.getString(R.string.REPLACE_WIKIPEDIA));
        if (wikipedia) {
            matcher = Helper.wikipediaPattern.matcher(url);
            if (matcher.find()) {
                String subdomain = matcher.group(1);
                String path = matcher.group(2);
                String wikipediaReplaceHost = sharedpreferences.getString(context.getString(R.string.REPLACE_WIKIPEDIA_HOST), context.getString(R.string.DEFAULT_REPLACE_WIKIPEDIA_HOST)).toLowerCase();
                String lang = "";
                if (path != null && subdomain != null && !subdomain.equals("www")) {
                    lang = (path.contains("?")) ? TextUtils.htmlEncode("&") : "?";
                    lang = lang + "lang=" + subdomain;
                }
                if (wikipediaReplaceHost.trim().isEmpty()) {
                    wikipediaReplaceHost = context.getString(R.string.DEFAULT_REPLACE_WIKIPEDIA_HOST);
                }
                return "https://" + wikipediaReplaceHost + "/" + path + lang;
            }
        }
        boolean filterUTM = Helper.getSharedValue(context, context.getString(R.string.SET_FILTER_UTM));
        if (filterUTM) {
            return remove_tracking_param(context, url);
        }
        return url;
    }

    /**
     * Remove tracking parameters
     *
     * @param context          - Context
     * @param original_content - String original URL
     * @return cleaned URL
     */
    private static String remove_tracking_param(Context context, String original_content) {
        if (original_content == null)
            return original_content;
        String cleaned_content = original_content;
        for (String utm : UTM_PARAMS) {
            cleaned_content = cleaned_content.replaceAll("&amp;" + utm + "=[0-9a-zA-Z._-]*", "");
            cleaned_content = cleaned_content.replaceAll("&" + utm + "=[0-9a-zA-Z._-]*", "");
            cleaned_content = cleaned_content.replaceAll("\\?" + utm + "=[0-9a-zA-Z._-]*", "?");
        }
        if (cleaned_content.endsWith("?")) {
            cleaned_content = cleaned_content.substring(0, cleaned_content.length() - 1);
        }
        return cleaned_content;
    }

    @SuppressLint("DefaultLocale")
    public static String withSuffix(long count) {
        if (count < 1000) return String.valueOf(count);
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
        Bundle args = new Bundle();
        args.putString(RECEIVE_TOAST_TYPE, type);
        args.putString(RECEIVE_TOAST_CONTENT, content);
        intentBC.setAction(Helper.RECEIVE_TOAST_MESSAGE);
        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            intentBC.putExtras(bundle);
            intentBC.setPackage(BuildConfig.APPLICATION_ID);
            context.sendBroadcast(intentBC);
        });
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
            ft.show(_fragment).commitAllowingStateLoss();
            fragment = _fragment;
        } else {
            if (args != null) fragment.setArguments(args);
            ft = fragmentManager.beginTransaction();
            ft.add(containerViewId, fragment, tag);
            if (backStackName != null) {
                try {
                    ft.addToBackStack(backStackName);
                }catch (Exception ignored){}
            }
            if (!fragmentManager.isDestroyed()) {
                ft.commitAllowingStateLoss();
            }
        }
        fragmentManager.executePendingTransactions();
        return fragment;
    }

    /**
     * Load a media into a view
     *
     * @param view ImageView - the view where the image will be loaded
     * @param url  - String
     */
    public static void loadImage(ImageView view, String url) {
        Context context = view.getContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean disableGif = sharedpreferences.getBoolean(context.getString(R.string.SET_DISABLE_GIF), false);
        if (disableGif || (!url.endsWith(".gif"))) {
            Glide.with(view.getContext())
                    .asDrawable()
                    .load(url)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(view);
        } else {
            Glide.with(view.getContext())
                    .asGif()
                    .load(url)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(view);
        }
    }

    /**
     * Log out the authenticated user by removing its token
     *
     * @param activity Activity
     * @throws DBException Exception
     */
    public static void removeAccount(Activity activity) throws DBException {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        //Current user
        String userId = sharedpreferences.getString(PREF_USER_ID, null);
        String instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        Account accountDB = new Account(activity);

        OauthVM oauthVM = new ViewModelProvider((ViewModelStoreOwner) activity).get(OauthVM.class);

        if (Helper.getCurrentAccount(activity) != null) {
            //Revoke the token
            oauthVM.revokeToken(Helper.getCurrentAccount(activity).instance, Helper.getCurrentAccount(activity).token, Helper.getCurrentAccount(activity).client_id, Helper.getCurrentAccount(activity).client_secret);
            //Log out the current user
            accountDB.removeUser(Helper.getCurrentAccount(activity));
        }
        BaseAccount newAccount = accountDB.getLastUsedAccount();
        SharedPreferences.Editor editor = sharedpreferences.edit();
        if (newAccount == null) {
            editor.putString(PREF_USER_TOKEN, null);
            editor.putString(PREF_USER_INSTANCE, null);
            editor.putString(PREF_USER_SOFTWARE, null);
            editor.putString(PREF_USER_ID, null);
            editor.commit();
            Intent loginActivity = new Intent(activity, LoginActivity.class);
            activity.startActivity(loginActivity);
            activity.finish();
        } else {
            Helper.setCurrentAccount(newAccount);
            editor.putString(PREF_USER_TOKEN, newAccount.token);
            editor.putString(PREF_USER_SOFTWARE, newAccount.software);
            editor.putString(PREF_USER_INSTANCE, newAccount.instance);
            editor.putString(PREF_USER_ID, newAccount.user_id);
            BaseMainActivity.currentUserID = newAccount.user_id;
            BaseMainActivity.currentToken = newAccount.token;
            BaseMainActivity.currentInstance = newAccount.instance;
            editor.commit();
            Intent changeAccount = new Intent(activity, MainActivity.class);
            changeAccount.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(changeAccount);
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
        if (context instanceof Activity activity) {
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
        Cursor returnCursor = null;
        try {
            returnCursor =
                    resolver.query(uri, null, null, null, null);
        } catch (Exception ignored) {
        }
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
     * Load a profile picture for the account
     *
     * @param view    ImageView - the view where the image will be loaded
     * @param account - {@link Account}
     */
    public static void loadPP(Activity activity, ImageView view, BaseAccount account) {
        loadPP(activity, view, account, false);
    }

    /**
     * Load a profile picture for the account
     *
     * @param view    ImageView - the view where the image will be loaded
     * @param account - {@link Account}
     */
    public static void loadPP(Activity activity, ImageView view, BaseAccount account, boolean crop) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean disableGif = sharedpreferences.getBoolean(activity.getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = "";
        if (account.mastodon_account != null) {
            targetedUrl = disableGif ? account.mastodon_account.avatar_static : account.mastodon_account.avatar;
        } else if (account.peertube_account != null) {
            if (account.peertube_account.getAvatar() != null) {
                targetedUrl = account.peertube_account.getAvatar().getPath();
                if (targetedUrl != null && targetedUrl.startsWith("/")) {
                    targetedUrl = "https://" + account.instance + account.peertube_account.getAvatar().getPath();
                }
            } else {
                BitmapDrawable avatar = new AvatarGenerator.AvatarBuilder(activity)
                        .setLabel(account.peertube_account.getAcct())
                        .setAvatarSize(120)
                        .setTextSize(30)
                        .toSquare()
                        .setBackgroundColor(fetchAccentColor(activity))
                        .build();
                if (Helper.isValidContextForGlide(activity)) {
                    Glide.with(activity)
                            .asDrawable()
                            .load(avatar)
                            .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                            .into(view);
                }
                return;
            }
        }

        if (targetedUrl != null && Helper.isValidContextForGlide(activity)) {
            if (disableGif || (!targetedUrl.endsWith(".gif"))) {
                try {
                    RequestBuilder<Drawable> requestBuilder = Glide.with(activity)
                            .asDrawable()
                            .load(targetedUrl)
                            .thumbnail(0.1f);
                    if (crop) {
                        requestBuilder = requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)));
                    }
                    requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10))).into(view);
                } catch (Exception ignored) {
                }
            } else {
                RequestBuilder<GifDrawable> requestBuilder = Glide.with(activity)
                        .asGif()
                        .load(targetedUrl)
                        .thumbnail(0.1f);
                if (crop) {
                    requestBuilder = requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)));
                }
                requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10))).into(view);
            }
        } else if (Helper.isValidContextForGlide(activity)) {
            Glide.with(activity)
                    .asDrawable()
                    .load(R.drawable.ic_person)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(view);
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
                InetSocketAddress.createUnresolved(hostVal, portVal));
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

    /**
     * Creates MultipartBody.Part from Uri
     *
     * @return MultipartBody.Part for the given Uri
     */
    public static MultipartBody.Part getMultipartBodyWithWM(Context context, String waterMark, @NonNull String paramName, @NonNull Attachment attachment) {
        File files = new File(attachment.local_path);
        float textSize = 15;
        Paint paint = new Paint();
        float width = paint.measureText(waterMark, 0, waterMark.length());
        try {

            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap backgroundBitmap = BitmapFactory.decodeFile(files.getAbsolutePath(), options);
            int w = backgroundBitmap.getWidth();
            int h = backgroundBitmap.getHeight();
            float valx = (float) 1.0 - ((Helper.convertDpToPixel(width, context) + 10)) / (float) w;
            if (valx < 0)
                valx = 0;
            float valy = (h - Helper.convertDpToPixel(textSize, context) - 0) / (float) h;
            WatermarkText watermarkText = new WatermarkText(waterMark)
                    .setPositionX(valx)
                    .setPositionY(valy)
                    .setTextColor(Color.WHITE)
                    .setTextShadow(0.1f, 1, 1, Color.LTGRAY)
                    .setTextAlpha(200)
                    .setRotation(0)
                    .setTextSize(textSize);

            Bitmap bitmap = WatermarkBuilder
                    .create(context, backgroundBitmap)
                    .loadWatermarkText(watermarkText)
                    .getWatermark()
                    .getOutputImage();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();
            RequestBody requestFile = RequestBody.create(MediaType.parse(attachment.mimeType), bitmapdata);
            return MultipartBody.Part.createFormData(paramName, attachment.filename, requestFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            List<Attachment> attachments = new ArrayList<>();
            for (Uri uri : uris) {
                Attachment attachment = new Attachment();
                attachment.filename = Helper.getFileName(context, uri);
                attachment.size = Helper.getRealSizeFromUri(context, uri);
                ContentResolver cR = context.getApplicationContext().getContentResolver();
                attachment.mimeType = cR.getType(uri);

                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String extension = mime.getExtensionFromMimeType(cR.getType(uri));
                if (uri.toString().endsWith("fedilab_recorded_audio.ogg")) {
                    extension = "ogg";
                    attachment.mimeType = "audio/ogg";
                }
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_" + counter, Locale.getDefault());
                counter++;
                Date now = new Date();
                attachment.filename = formatter.format(now) + "." + extension;
                Set<String> imageType = new HashSet<>(Arrays.asList("image/png", "image/jpeg", "image/jpg"));
                if (imageType.contains(attachment.mimeType)) {
                    final File certCacheDir = new File(context.getCacheDir(), TEMP_MEDIA_DIRECTORY);
                    boolean isCertCacheDirExists = certCacheDir.exists();
                    if (!isCertCacheDirExists) {
                        certCacheDir.mkdirs();
                    }
                    String filePath = certCacheDir.getAbsolutePath() + "/" + attachment.filename;
                    MediaHelper.ResizedImageRequestBody(context, uri, new File(filePath));
                    attachment.local_path = filePath;
                } else {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                attachments.add(attachment);
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> callBack.onAttachmentCopied(attachments);
            mainHandler.post(myRunnable);
        }).start();
    }

    public static void createFileFromUri(Context context, Uri uri, OnFileCopied callBack) {
        new Thread(() -> {
            InputStream selectedFileInputStream;
            File file = null;
            try {
                String uriFullPath = uri.getPath();
                String[] uriFullPathStr = uriFullPath.split(":");
                String fullPath = uriFullPath;
                if (uriFullPathStr.length > 1) {
                    fullPath = uriFullPathStr[1];
                }
                final String fileName = Helper.dateFileToString(context, new Date()) + ".zip";
                selectedFileInputStream = context.getContentResolver().openInputStream(uri);
                if (selectedFileInputStream != null) {
                    final File certCacheDir = new File(context.getCacheDir(), TEMP_EXPORT_DATA);
                    boolean isCertCacheDirExists = certCacheDir.exists();
                    if (!isCertCacheDirExists) {
                        isCertCacheDirExists = certCacheDir.mkdirs();
                    }
                    if (isCertCacheDirExists) {
                        String filePath = certCacheDir.getAbsolutePath() + "/" + fileName;
                        file = new File(filePath);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            File finalFile = file;
            Runnable myRunnable = () -> callBack.onFileCopied(finalFile);
            mainHandler.post(myRunnable);
        }).start();
    }

    public static void createAttachmentFromPAth(String path, OnAttachmentCopied callBack) {
        new Thread(() -> {
            List<Attachment> attachmentList = new ArrayList<>();
            Attachment attachment = new Attachment();
            attachment.mimeType = "image/*";
            String extension = "jpg";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_" + counter, Locale.getDefault());
            attachment.local_path = path;
            Date now = new Date();
            attachment.filename = formatter.format(now) + "." + extension;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            attachmentList.add(attachment);
            Runnable myRunnable = () -> callBack.onAttachmentCopied(attachmentList);
            mainHandler.post(myRunnable);
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
     * @param materialButton {@link MaterialButton}
     * @param hexaColor example 0xffff00
     */
    public static void changeDrawableColor(Context context, MaterialButton materialButton, int hexaColor) {
        if (materialButton == null)
            return;
        int color;
        try {
            color = context.getResources().getColor(hexaColor);
        } catch (Resources.NotFoundException e) {
            color = hexaColor;
        }
        materialButton.setIconTint(ColorStateList.valueOf(color));
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

    /**
     * Send broadcast to recreate Mainactivity
     *
     * @param activity - Activity
     */
    public static void recreateMainActivity(Activity activity) {
        Bundle args = new Bundle();
        args.putBoolean(Helper.RECEIVE_RECREATE_ACTIVITY, true);
        Intent intentBD = new Intent(Helper.BROADCAST_DATA);
        new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            intentBD.putExtras(bundle);
            intentBD.setPackage(BuildConfig.APPLICATION_ID);
            activity.sendBroadcast(intentBD);
        });
    }

    public static void showKeyboard(Context context, View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
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
    public static void notify_user(Context context, BaseAccount account, Intent intent, Bitmap icon, NotifType notifType, String title, String message) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // prepare intent which is triggered if the user click on the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        } else {
            pIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        // build notification
        String channelId;
        String channelTitle;

        switch (notifType) {
            case FAV -> {
                channelId = "channel_favourite";
                channelTitle = context.getString(R.string.channel_notif_fav);
            }
            case FOLLLOW -> {
                channelId = "channel_follow";
                channelTitle = context.getString(R.string.channel_notif_follow);
            }
            case MENTION -> {
                channelId = "channel_mention";
                channelTitle = context.getString(R.string.channel_notif_mention);
            }
            case POLL -> {
                channelId = "channel_poll";
                channelTitle = context.getString(R.string.channel_notif_poll);
            }
            case BACKUP -> {
                channelId = "channel_backup";
                channelTitle = context.getString(R.string.channel_notif_backup);
            }
            case STORE -> {
                channelId = "channel_media";
                channelTitle = context.getString(R.string.channel_notif_media);
            }
            case TOOT -> {
                channelId = "channel_status";
                channelTitle = context.getString(R.string.channel_notif_status);
            }
            case UPDATE -> {
                channelId = "channel_update";
                channelTitle = context.getString(R.string.channel_notif_update);
            }
            case SIGN_UP -> {
                channelId = "channel_signup";
                channelTitle = context.getString(R.string.channel_notif_signup);
            }
            case REPORT -> {
                channelId = "channel_report";
                channelTitle = context.getString(R.string.channel_notif_report);
            }
            default -> {
                channelId = "channel_boost";
                channelTitle = context.getString(R.string.channel_notif_boost);
            }
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getNotificationIcon(context)).setTicker(message);
      /*  if (notifType == NotifType.MENTION) {
            if (message.length() > 500) {
                message = message.substring(0, 499) + "…";
            }
        }*/
        notificationBuilder.setGroup(account.mastodon_account != null ? account.mastodon_account.username + "@" + account.instance : "@" + account.instance)
                .setContentIntent(pIntent)
                .setContentText(message);
        int ledColour = Color.BLUE;
        int prefColor;
        prefColor = Integer.parseInt(sharedpreferences.getString(context.getString(R.string.SET_LED_COLOUR_VAL_N), String.valueOf(LED_COLOUR)));
        switch (prefColor) {
            case 1 -> // CYAN
                    ledColour = Color.CYAN;
            case 2 -> // MAGENTA
                    ledColour = Color.MAGENTA;
            case 3 -> // GREEN
                    ledColour = Color.GREEN;
            case 4 -> // RED
                    ledColour = Color.RED;
            case 5 -> // YELLOW
                    ledColour = Color.YELLOW;
            case 6 -> // WHITE
                    ledColour = Color.WHITE;
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
                /*String soundUri = sharedpreferences.getString(context.getString(R.string.SET_NOTIF_SOUND), ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.boop);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(Uri.parse(soundUri), audioAttributes);*/
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
        notificationBuilder.setSubText(String.format("@%s@%s", account.mastodon_account.username, account.instance));

        Notification summaryNotification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(channelTitle)
                .setContentIntent(pIntent)
                .setLargeIcon(icon)
                .setSmallIcon(getNotificationIcon(context))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setGroup(account.mastodon_account != null ? account.mastodon_account.username + "@" + account.instance : "@" + account.instance)
                .setGroupSummary(true)
                .build();

        notificationManager.notify(notificationId++, notificationBuilder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            notificationManager.notify(0, summaryNotification);
        }
    }

    public static String dateDiffFull(Date dateToot) {
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault());
        try {
            return df.format(dateToot);
        } catch (Exception e) {
            return "";
        }
    }

    public static String dateDiffFullShort(Date dateToot) {
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        try {
            return df.format(dateToot);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Makes the tvDate TextView field clickable, and displays the absolute date & time of a toot
     * for 5 seconds.
     *
     * @param context Context
     * @param tvDate  TextView
     * @param date    Date
     */

    public static void absoluteDateTimeReveal(final Context context, final TextView tvDate, final Date date, final Date dateEdit) {
        tvDate.setOnClickListener(v -> {

            if (dateEdit == null) {
                tvDate.setText(dateDiffFull(date));
            } else {
                String dateEditText = context.getString(R.string.full_date_edited, dateDiffFull(date), dateDiffFull(dateEdit));
                tvDate.setText(dateEditText);
            }

            new CountDownTimer((5 * 1000), 1000) {

                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    tvDate.setText(dateDiff(context, date));
                }
            }.start();
        });
    }

    public static String generateString() {
        String uuid = UUID.randomUUID().toString();
        return "@fedilab_fetch_more_" + uuid;
    }

    public static String generateIdString() {
        String uuid = UUID.randomUUID().toString();
        return "@fedilab_compose_" + uuid;
    }

    public static Gson getDateBuilder() {
        SimpleDateFormat[] formats = new SimpleDateFormat[]{
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        };
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new TypeAdapter<Date>() {

                    @Override
                    public void write(JsonWriter out, Date value) {
                    }

                    @Override
                    public Date read(JsonReader reader) throws IOException {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull();
                            return null;
                        }
                        String dateAsString = reader.nextString();
                        for (SimpleDateFormat format : formats) {
                            try {
                                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                                return format.parse(dateAsString);
                            } catch (ParseException ignored) {
                            }
                        }
                        return null;
                    }
                })
                .create();
    }

    /***
     * Download method which works for http and https connections
     * @param downloadUrl String download url
     * @param listener OnDownloadInterface, listener which manages progress
     */
    public static void download(Context context, final String downloadUrl, final OnDownloadInterface listener) {
        new Thread(() -> {
            URL url;
            int CHUNK_SIZE = 4096;
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enable_proxy = sharedpreferences.getBoolean(context.getString(R.string.SET_PROXY_ENABLED), false);
            Proxy proxy = null;
            if (enable_proxy) {
                proxy = getProxy(context);
            }
            try {
                url = new URL(downloadUrl);
                HttpURLConnection httpURLConnection;
                if (proxy != null)
                    httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
                else
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                // httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                int responseCode = httpURLConnection.getResponseCode();

                // always check HTTP response code first
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String fileName = "";
                    String disposition = httpURLConnection.getHeaderField("Content-Disposition");
                    if (disposition != null) {
                        // extracts file name from header field
                        int index = disposition.indexOf("filename=");
                        if (index > 0) {
                            fileName = disposition.substring(index + 10,
                                    disposition.length() - 1);
                        }
                    } else {
                        // extracts file name from URL
                        try {
                            URL downLoadUrlTmp = new URL(downloadUrl);
                            fileName = downLoadUrlTmp.getPath().replace("/","_");
                        }catch (Exception exception) {
                            fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
                        }
                    }
                    fileName = FileNameCleaner.cleanFileName(fileName);
                    // opens input stream from the HTTP connection
                    InputStream inputStream = httpURLConnection.getInputStream();
                    final File saveDir = new File(context.getCacheDir(), TEMP_MEDIA_DIRECTORY);
                    boolean isCertCacheDirExists = saveDir.exists();
                    if (!isCertCacheDirExists) {
                        saveDir.mkdirs();
                    }
                    final String saveFilePath = saveDir + File.separator + fileName;
                    // opens an output stream to save into file
                    FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                    int bytesRead;
                    byte[] buffer = new byte[CHUNK_SIZE];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                    ((ComposeActivity) context).runOnUiThread(() -> listener.onDownloaded(saveFilePath, downloadUrl, null));
                } else {
                    ((ComposeActivity) context).runOnUiThread(() -> listener.onDownloaded(null, downloadUrl, new Error()));

                }
            } catch (IOException e) {
                e.printStackTrace();
                ((ComposeActivity) context).runOnUiThread(() -> listener.onDownloaded(null, downloadUrl, new Error()));
            }

        }).start();
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void displayReleaseNotesIfNeeded(Activity activity, boolean forced) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int lastReleaseNoteRead = sharedpreferences.getInt(activity.getString(R.string.SET_POPUP_RELEASE_NOTES), 0);
        int versionCode = BuildConfig.VERSION_CODE;
        boolean disabled = sharedpreferences.getBoolean(activity.getString(R.string.SET_DISABLE_RELEASE_NOTES_ALERT), false);
        if (disabled && !forced) {
            return;
        }
        if (lastReleaseNoteRead != versionCode || forced) {
            try {
                InputStream is = activity.getAssets().open("release_notes/notes.json");
                int size;
                size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String json = new String(buffer, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                AlertDialog.Builder dialogBuilderOptin = new MaterialAlertDialogBuilder(activity);
                PopupReleaseNotesBinding binding = PopupReleaseNotesBinding.inflate(activity.getLayoutInflater());
                dialogBuilderOptin.setView(binding.getRoot());
                try {
                    List<ReleaseNote.Note> releaseNotes = gson.fromJson(json, new TypeToken<List<ReleaseNote.Note>>() {
                    }.getType());
                    if (releaseNotes != null && releaseNotes.size() > 0) {
                        ReleaseNoteAdapter adapter = new ReleaseNoteAdapter(releaseNotes);
                        binding.releasenotes.setAdapter(adapter);
                        binding.releasenotes.setLayoutManager(new LinearLayoutManager(activity));
                    }
                } catch (Exception ignored) {
                }
                if (BuildConfig.DONATIONS) {
                    binding.aboutSupport.setVisibility(View.VISIBLE);
                    binding.aboutSupportPaypal.setVisibility(View.VISIBLE);
                } else {
                    binding.aboutSupport.setVisibility(View.GONE);
                    binding.aboutSupportPaypal.setVisibility(View.GONE);
                }
                binding.accountFollow.setIconResource(R.drawable.ic_baseline_person_add_24);
                binding.aboutSupport.setOnClickListener(v -> {
                    Intent intentLiberapay = new Intent(Intent.ACTION_VIEW);
                    intentLiberapay.setData(Uri.parse("https://liberapay.com/tom79"));
                    try {
                        activity.startActivity(intentLiberapay);
                    } catch (Exception e) {
                        Helper.openBrowser(activity, "https://liberapay.com/tom79");
                    }
                });
                binding.aboutSupportPaypal.setOnClickListener(v -> Helper.openBrowser(activity, "https://www.paypal.me/Mastalab"));
                CrossActionHelper.fetchRemoteAccount(activity, "@apps@toot.fedilab.app", new CrossActionHelper.Callback() {
                    @Override
                    public void federatedStatus(Status status) {

                    }

                    @Override
                    public void federatedAccount(app.fedilab.android.mastodon.client.entities.api.Account account) {
                        if (account != null && account.username.equalsIgnoreCase("apps")) {

                            MastodonHelper.loadPPMastodon(binding.accountPp, account);
                            binding.accountDn.setText(account.display_name);
                            binding.accountUn.setText(account.acct);
                            binding.accountPp.setOnClickListener(v -> {
                                Intent intent = new Intent(activity, ProfileActivity.class);
                                Bundle args = new Bundle();
                                args.putSerializable(Helper.ARG_ACCOUNT, account);
                                new CachedBundle(activity).insertBundle(args, Helper.getCurrentAccount(activity), bundleId -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                    intent.putExtras(bundle);
                                    activity.startActivity(intent);
                                });
                            });

                            AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) activity).get(AccountsVM.class);
                            List<String> ids = new ArrayList<>();
                            ids.add(account.id);
                            accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, ids)
                                    .observe((LifecycleOwner) activity, relationShips -> {
                                        if (relationShips != null && relationShips.size() > 0) {
                                            if (!relationShips.get(0).following) {
                                                binding.acccountContainer.setVisibility(View.VISIBLE);
                                                binding.accountFollow.setVisibility(View.VISIBLE);
                                                binding.accountFollow.setOnClickListener(v -> accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false, null)
                                                        .observe((LifecycleOwner) activity, relationShip -> binding.accountFollow.setVisibility(View.GONE)));
                                            }
                                        }
                                    });
                        }
                    }
                });
                dialogBuilderOptin.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
                try {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        if (!activity.isFinishing()) {
                            dialogBuilderOptin.show();
                        }
                    }, 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(activity.getString(R.string.SET_POPUP_RELEASE_NOTES), versionCode);
            editor.apply();
        }
    }

    public static String getSlugOfFirstFragment(Context context, String userId, String instance) {
        if (slugOfFirstFragment != null) {
            return slugOfFirstFragment;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedpreferences.getString(Helper.ARG_SLUG_OF_FIRST_FRAGMENT + userId + instance, Timeline.TimeLineEnum.HOME.getValue());
    }

    public static void setSlugOfFirstFragment(Context context, String slug, String userId, String instance) {
        if (slug != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            slugOfFirstFragment = slug;
            editor.putString(Helper.ARG_SLUG_OF_FIRST_FRAGMENT + userId + instance, slug);
            editor.apply();
        }
    }

    public static int compareTo(String value1, String value2) {
        try {
            long val1 = Long.parseLong(value1);
            long val2 = Long.parseLong(value2);
            return Long.compare(val1, val2);
        } catch (Exception e) {
            return value1.compareTo(value2);
        }
    }

    /**
     * Restart the app
     *
     * @param context
     */
    public static void restart(Context context) {
        Context ctx = context.getApplicationContext();
        PackageManager pm = ctx.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(ctx.getPackageName());
        Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        ctx.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void forwardToBrowser(Activity activity, Intent i) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(i.getData(), i.getType());
        List<ResolveInfo> activities = activity.getPackageManager().queryIntentActivities(intent, 0);
        ArrayList<Intent> targetIntents = new ArrayList<>();
        String thisPackageName = activity.getPackageName();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            if (!thisPackageName.equals(packageName)) {
                Intent targetIntent = new Intent(android.content.Intent.ACTION_VIEW);
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

    public static int dialogStyle() {
        if (R.style.AppThemeBar == currentThemeId || R.style.AppTheme == currentThemeId) {
            return R.style.AppThemeAlertDialog;
        } else if (R.style.SolarizedAppThemeBar == currentThemeId || R.style.SolarizedAppTheme == currentThemeId) {
            return R.style.SolarizedAlertDialog;
        } else if (R.style.BlackAppThemeBar == currentThemeId || R.style.BlackAppTheme == currentThemeId) {
            return R.style.BlackAlertDialog;
        } else if (R.style.DraculaAppThemeBar == currentThemeId || R.style.DraculaAppTheme == currentThemeId) {
            return R.style.DraculaAlertDialog;
        }
        return R.style.AppTheme;
    }

    public static void addMutedAccount(app.fedilab.android.mastodon.client.entities.api.Account target) {
        if (MainActivity.filteredAccounts == null) {
            MainActivity.filteredAccounts = new ArrayList<>();
        }
        if (!MainActivity.filteredAccounts.contains(target)) {
            MainActivity.filteredAccounts.add(target);
        }
    }

    public static void removeMutedAccount(app.fedilab.android.mastodon.client.entities.api.Account target) {
        if (MainActivity.filteredAccounts != null) {
            MainActivity.filteredAccounts.remove(target);
        }
    }

    public static BaseAccount getCurrentAccount(Context context) {
        if (baseAccount == null && context != null) {
            baseAccount = new BaseAccount();
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            baseAccount.user_id = sharedpreferences.getString(PREF_USER_ID, null);
            baseAccount.instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
            baseAccount.token = sharedpreferences.getString(PREF_USER_TOKEN, null);
        }
        return baseAccount;
    }

    public static void setCurrentAccount(BaseAccount newBaseAccount) {
        baseAccount = newBaseAccount;
    }

    public static void setCurrentAccountMastodonAccount(Context context, app.fedilab.android.mastodon.client.entities.api.Account newAccount) {
        BaseAccount tempBaseAccount = getCurrentAccount(context);
        tempBaseAccount.mastodon_account = newAccount;
        setCurrentAccount(tempBaseAccount);
    }

    public static void setCurrentAccountPeertubeAccount(Context context, AccountData.PeertubeAccount newAccount) {
        BaseAccount tempBaseAccount = getCurrentAccount(context);
        tempBaseAccount.peertube_account = newAccount;
        setCurrentAccount(tempBaseAccount);
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static OkHttpClient myOkHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", context.getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE)
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();
    }

    public static OkHttpClient myPostOkHttpClient(Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", context.getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE)
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .readTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();
    }

    public static String parseHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            //noinspection deprecation
            return Html.fromHtml(html).toString();
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
        UPDATE,
        SIGN_UP,
        REPORT,
        STATUS,
        BACKUP,
        STORE,
        TOOT
    }

    public interface OnAttachmentCopied {
        void onAttachmentCopied(List<Attachment> attachments);
    }

    public interface OnFileCopied {
        void onFileCopied(File file);
    }
}
