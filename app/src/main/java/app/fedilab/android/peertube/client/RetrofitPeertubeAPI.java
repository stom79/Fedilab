package app.fedilab.android.peertube.client;
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

import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.BlockData;
import app.fedilab.android.peertube.client.data.CaptionData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.CommentData;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.data.NotificationData;
import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.client.data.PluginData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.client.entities.AccountCreation;
import app.fedilab.android.peertube.client.entities.ChannelParams;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.InstanceParams;
import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.OverviewVideo;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.PlaylistParams;
import app.fedilab.android.peertube.client.entities.Rating;
import app.fedilab.android.peertube.client.entities.Report;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.client.entities.UserSettings;
import app.fedilab.android.peertube.client.entities.VideoParams;
import app.fedilab.android.peertube.client.entities.WellKnownNodeinfo;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.CommentVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import app.fedilab.android.sqlite.Sqlite;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@SuppressWarnings({"unused", "RedundantSuppression", "ConstantConditions"})
public class RetrofitPeertubeAPI {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();
    private final String finalUrl;
    private final Context _context;
    private final String instance;
    private final String count;
    private final String showNSFWVideos;
    private String token;
    private Set<String> selection;


    public RetrofitPeertubeAPI(Context context) {
        _context = context;
        instance = HelperInstance.getLiveInstance(context);
        finalUrl = "https://" + HelperInstance.getLiveInstance(context) + "/api/v1/";
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        count = String.valueOf(sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE));
        String currentSensitive = sharedpreferences.getString(_context.getString(R.string.set_video_sensitive_choice), Helper.BLUR);
        if (currentSensitive.compareTo(Helper.DO_NOT_LIST) == 0) {
            showNSFWVideos = "false";
        } else if (currentSensitive.compareTo(Helper.BLUR) == 0) {
            showNSFWVideos = "both";
        } else {
            showNSFWVideos = "true";
        }
    }

    public RetrofitPeertubeAPI(Context context, String instance, String token) {
        _context = context;
        this.instance = instance;
        this.token = token;
        finalUrl = "https://" + instance + "/api/v1/";
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        count = String.valueOf(sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE));
        String currentSensitive = sharedpreferences.getString(_context.getString(R.string.set_video_sensitive_choice), Helper.BLUR);
        if (currentSensitive.compareTo(Helper.DO_NOT_LIST) == 0) {
            showNSFWVideos = "false";
        } else if (currentSensitive.compareTo(Helper.BLUR) == 0) {
            showNSFWVideos = "both";
        } else {
            showNSFWVideos = "true";
        }
    }


    public static void updateCredential(Activity activity, String token, String client_id, String client_secret, String refresh_token, String host, String software) {
        new Thread(() -> {
            AccountData.PeertubeAccount peertubeAccount;
            Account account = new Account();
            String instance = host;
            try {
                UserMe userMe = new RetrofitPeertubeAPI(activity, instance, token).verifyCredentials();

                peertubeAccount = userMe.getAccount();
            } catch (Error error) {
                Error.displayError(activity, error);
                error.printStackTrace();
                return;
            }
            try {
                //At the state the instance can be encoded
                instance = URLDecoder.decode(instance, "utf-8");
            } catch (UnsupportedEncodingException ignored) {
            }
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            account.token = token;
            account.client_id = client_id;
            account.client_secret = client_secret;
            account.refresh_token = refresh_token;
            account.instance = instance;
            account.api = Account.API.PEERTUBE;
            account.software = Account.API.PEERTUBE.name();
            account.peertube_account = peertubeAccount;
            account.user_id = peertubeAccount.getId();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(PREF_USER_ID, account.user_id);
            editor.putString(PREF_USER_INSTANCE, host);
            editor.putString(PREF_USER_TOKEN, token);
            editor.putString(PREF_USER_SOFTWARE, account.software);
            editor.commit();

            try {
                new Account(activity).insertOrUpdate(account);
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                Intent mainActivity = new Intent(activity, PeertubeMainActivity.class);
                mainActivity.putExtra(Helper.INTENT_ACTION, Helper.ADD_USER_INTENT);
                activity.startActivity(mainActivity);
                activity.finish();
            };
            mainHandler.post(myRunnable);
        }).start();
    }

    private String getToken() {
        if (token != null) {
            return "Bearer " + token;
        } else {
            return null;
        }
    }

    private PeertubeService init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(finalUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        if (token == null) {
            token = Helper.getToken(_context);
        }
        selection = sharedpreferences.getStringSet(_context.getString(R.string.set_video_language_choice), null);
        return retrofit.create(PeertubeService.class);
    }

    private PeertubeService initTranslation() {
        if (!URLUtil.isValidUrl("https://" + instance)) {
            return null;
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(PeertubeService.class);
    }

    /***
     * Verifiy credential of the authenticated user *synchronously*
     * @return Account
     */
    public Token manageToken(OauthParams oauthParams) throws Error {
        PeertubeService peertubeService = init();
        Call<Token> refreshTokenCall = null;
        if (oauthParams.getGrant_type().compareTo("password") == 0) {
            refreshTokenCall = peertubeService.createToken(oauthParams.getClient_id(), oauthParams.getClient_secret(), oauthParams.getGrant_type(), oauthParams.getUsername(), oauthParams.getPassword());
        } else if (oauthParams.getGrant_type().compareTo("refresh_token") == 0) {
            refreshTokenCall = peertubeService.refreshToken(oauthParams.getClient_id(), oauthParams.getClient_secret(), oauthParams.getRefresh_token(), oauthParams.getGrant_type());
        }
        if (refreshTokenCall != null) {
            try {
                Response<Token> response = refreshTokenCall.execute();
                if (response.isSuccessful()) {
                    Token tokenReply = response.body();
                    if (oauthParams.getGrant_type().compareTo("refresh_token") == 0 && tokenReply != null) {
                        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(_context);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(PREF_USER_TOKEN, tokenReply.getAccess_token());
                        editor.apply();
                        SQLiteDatabase db = Sqlite.getInstance(_context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                        BaseMainActivity.currentToken = tokenReply.getAccess_token();
                        new Account(_context).updatePeertubeToken(tokenReply);
                    }
                    return tokenReply;
                } else {
                    Error error = new Error();
                    error.setStatusCode(response.code());
                    if (response.errorBody() != null) {
                        error.setError(response.errorBody().string());
                    } else {
                        error.setError(_context.getString(R.string.toast_error));
                    }
                    throw error;
                }
            } catch (IOException | DBException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * POST a view count for a video
     */
    public void postView(String videoUuid) {
        if (videoUuid == null) {
            return;
        }
        PeertubeService peertubeService = init();
        Call<String> postViewCall = peertubeService.postView(videoUuid);
        try {
            Response<String> dd = postViewCall.execute();
        } catch (IOException ignored) {
        }
    }


    /**
     * Retrieve notifications
     *
     * @return APIResponse
     */
    public int unreadNotifications() {
        PeertubeService peertubeService = init();
        Call<NotificationData> notificationsCall = peertubeService.countNotifications("Bearer " + token);
        try {
            Response<NotificationData> response = notificationsCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().total;
            }
        } catch (IOException ignored) {
        }
        return 0;
    }

    /**
     * Mark all notifications as read
     */
    public void markAllAsRead() {
        PeertubeService peertubeService = init();
        Call<String> notificationsCall = peertubeService.markAllAsRead("Bearer " + token);
        try {
            Response<String> response = notificationsCall.execute();
        } catch (IOException ignored) {
        }
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(String id) {
        PeertubeService peertubeService = init();
        ArrayList<String> ids = new ArrayList<>();
        ids.add(id);
        Call<String> notificationsCall = peertubeService.markAsRead("Bearer " + token, ids);
        try {
            Response<String> response = notificationsCall.execute();
        } catch (IOException ignored) {
        }
    }

    /**
     * Retrieve notifications
     *
     * @return APIResponse
     */
    public APIResponse getNotifications() {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();

        Call<NotificationData> notificationsCall = peertubeService.getNotifications("Bearer " + token, "0", "40", null);
        try {
            Response<NotificationData> response = notificationsCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setPeertubeNotifications(response.body().data);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Retrieve notifications
     *
     * @param max_id   String pagination
     * @param since_id String pagination
     * @return APIResponse
     */
    public APIResponse getNotifications(String max_id, String since_id) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();

        Call<NotificationData> notificationsCall = peertubeService.getNotifications("Bearer " + token, max_id, "20", since_id);
        try {
            Response<NotificationData> response = notificationsCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setPeertubeNotifications(response.body().data);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Get caption content
     *
     * @param path String path to caption
     * @return APIResponse
     */
    public APIResponse getCaptionContent(String path) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<String> captionContentCall = peertubeService.getCaptionContent(path);
        try {
            Response<String> response = captionContentCall.execute();
            if (response.isSuccessful()) {
                apiResponse.setCaptionText(response.body());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Get videos in a channel
     *
     * @param channelId String id of the channel
     * @param max_id    String pagination
     * @return APIResponse
     */
    public APIResponse getVideosForChannel(String channelId, String max_id) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<VideoData> videoCall = peertubeService.getChannelVideos(channelId, max_id, count, showNSFWVideos);
        if (videoCall != null) {
            try {
                Response<VideoData> response = videoCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setPeertubes(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }


    public APIResponse deleteHistory() {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<String> stringCall = peertubeService.deleteHistory(getToken());
        if (stringCall != null) {
            try {
                Response<String> response = stringCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }

    public APIResponse getHistory(String max_id, String startDate, String endDate) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<VideoData> videoCall = peertubeService.getHistory(getToken(), max_id, count, showNSFWVideos, startDate, endDate);
        if (videoCall != null) {
            try {
                Response<VideoData> response = videoCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setPeertubes(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }

    public APIResponse getTL(TimelineVM.TimelineType timelineType, String max_id, String forAccount) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<VideoData> videoCall = null;
        ArrayList<String> filter = selection != null ? new ArrayList<>(selection) : null;
        switch (timelineType) {
            case MY_VIDEOS:
                videoCall = peertubeService.getMyVideos(getToken(), max_id, count);
                break;
            case ACCOUNT_VIDEOS:
                videoCall = peertubeService.getVideosForAccount(forAccount, max_id, count, showNSFWVideos);
                break;
            case SUBSCRIBTIONS:
                if (forAccount == null) {
                    videoCall = peertubeService.getSubscriptionVideos(getToken(), max_id, count, filter);
                } else {
                    videoCall = peertubeService.getChannelVideos(forAccount, max_id, count, showNSFWVideos);
                }
                break;
            case MOST_LIKED:
                videoCall = peertubeService.getMostLikedVideos(getToken(), max_id, count, showNSFWVideos, filter);
                break;
            case LOCAL:
                videoCall = peertubeService.getLocalVideos(getToken(), max_id, count, showNSFWVideos, filter);
                break;
            case TRENDING:
                videoCall = peertubeService.getTrendingVideos(getToken(), max_id, count, showNSFWVideos, filter);
                break;
            case HISTORY:
                videoCall = peertubeService.getHistory(getToken(), max_id, count, showNSFWVideos, null, null);
                break;
            case RECENT:
                videoCall = peertubeService.getRecentlyAddedVideos(getToken(), max_id, count, showNSFWVideos, filter);
                break;
        }
        if (videoCall != null) {
            try {
                Response<VideoData> response = videoCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setPeertubes(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }

    /**
     * Retrieves overview videos *synchronously*
     *
     * @param page String id pagination
     * @return APIResponse
     */
    public APIResponse getOverviewVideo(String page) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        ArrayList<String> filter = selection != null ? new ArrayList<>(selection) : null;
        Call<OverviewVideo> overviewVideoCall = peertubeService.getOverviewVideos(getToken(), page, showNSFWVideos, filter);
        try {
            Response<OverviewVideo> response = overviewVideoCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setOverviewVideo(response.body());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }


    /**
     * Retrieves playlists for a video *synchronously*
     *
     * @param videoIds List<String> ids of videos
     * @return APIResponse
     */
    public APIResponse getVideosExist(List<String> videoIds) {
        PeertubeService peertubeService = init();
        APIResponse apiResponse = new APIResponse();
        try {
            Call<Map<String, List<PlaylistExist>>> videoExistsInPlaylist = peertubeService.getVideoExistsInPlaylist(getToken(), videoIds);
            Response<Map<String, List<PlaylistExist>>> response = videoExistsInPlaylist.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setVideoExistPlaylist(response.body());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Update history
     *
     * @param videoId     String
     * @param currentTime int
     */
    public void updateHistory(String videoId, long currentTime) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<String> updateUser = peertubeService.addToHistory(getToken(),
                videoId,
                currentTime
        );
        try {
            Response<String> response = updateUser.execute();
            if (response.isSuccessful()) {
                apiResponse.setActionReturn(response.body());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update account information
     *
     * @param userSettings UserSettings
     */
    public UserMe.AvatarResponse updateUser(UserSettings userSettings) throws IOException, Error {
        APIResponse apiResponse = new APIResponse();
        UserMe.AvatarResponse avatarResponse = null;
        PeertubeService peertubeService = init();

        Call<String> updateNotifications = peertubeService.updateNotifications(getToken(), userSettings.getNotificationSettings());
        Response<String> responseNotif = updateNotifications.execute();
        Call<String> updateUser = peertubeService.updateUser(getToken(),
                userSettings.isVideosHistoryEnabled(),
                userSettings.isAutoPlayVideo(),
                userSettings.isAutoPlayNextVideo(),
                userSettings.isWebTorrentEnabled(),
                userSettings.getVideoLanguages(),
                userSettings.getDescription(),
                userSettings.getDisplayName(),
                userSettings.getNsfwPolicy()
        );
        Response<String> response = updateUser.execute();
        if (response.isSuccessful()) {
            apiResponse.setActionReturn(response.body());
        } else {
            setError(apiResponse, response.code(), response.errorBody());
            Error error = new Error();
            error.setStatusCode(response.code());
            if (response.errorBody() != null) {
                error.setError(response.errorBody().string());
            } else {

                error.setError(_context.getString(R.string.toast_error));
            }
            throw error;
        }
        if (userSettings.getAvatarfile() != null) {
            MultipartBody.Part bodyThumbnail = createFile("avatarfile", userSettings.getAvatarfile(), userSettings.getFileName());
            Call<UserMe.AvatarResponse> updateProfilePicture = peertubeService.updateProfilePicture(getToken(), bodyThumbnail);
            Response<UserMe.AvatarResponse> responseAvatar = updateProfilePicture.execute();
            if (response.isSuccessful()) {
                avatarResponse = responseAvatar.body();
            } else {
                setError(apiResponse, response.code(), response.errorBody());
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
                throw error;
            }
        }
        return avatarResponse;
    }

    private MultipartBody.Part createFile(@NotNull String paramName, @NotNull Uri uri, String filename) throws IOException {

        InputStream inputStream = _context.getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        byte[] imageBytes = byteBuffer.toByteArray();
        String mime = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (mime == null || mime.trim().length() == 0) {
            mime = "png";
        }
        if (filename == null) {
            filename = "my_image." + mime;
        }
        RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("image/" + mime));
        return MultipartBody.Part.createFormData(paramName, filename, requestFile);
    }

    /**
     * Check if users via their uris are following the authenticated user
     *
     * @param uris List<String>
     * @return APIResponse
     */
    public APIResponse areFollowing(List<String> uris) {
        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<Map<String, Boolean>> followingCall = peertubeService.getSubscriptionsExist(getToken(), uris);
        try {
            Response<Map<String, Boolean>> response = followingCall.execute();
            if (response.isSuccessful()) {
                apiResponse.setRelationships(response.body());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Find captions for a video
     *
     * @param videoId String id of the video
     * @return APIResponse
     */
    public APIResponse getCaptions(String videoId) {

        APIResponse apiResponse = new APIResponse();
        PeertubeService peertubeService = init();
        Call<CaptionData> captions = peertubeService.getCaptions(videoId);
        try {
            Response<CaptionData> response = captions.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setCaptions(response.body().data);

            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * About the instance
     *
     * @return AboutInstance
     */
    public InstanceData.AboutInstance getAboutInstance() {

        PeertubeService peertubeService = init();
        Call<InstanceData.InstanceInfo> about = peertubeService.configAbout();
        try {
            Response<InstanceData.InstanceInfo> response = about.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getInstance();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Config  of the instance
     *
     * @return InstanceConfig
     */
    public InstanceData.InstanceConfig getConfigInstance() {

        PeertubeService peertubeService = init();
        Call<InstanceData.InstanceConfig> config = peertubeService.config();
        try {
            Response<InstanceData.InstanceConfig> response = config.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Get watermark info
     *
     * @return PluginData.WaterMark
     */
    public PluginData.WaterMark getWaterMark() {

        PeertubeService peertubeService = init();
        Call<PluginData.WaterMark> waterMarkCall = peertubeService.waterMark();
        try {
            Response<PluginData.WaterMark> response = waterMarkCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get video quota
     *
     * @return UserMe.VideoQuota
     */
    public UserMe.VideoQuota getVideoQuota() {

        PeertubeService peertubeService = init();
        Call<UserMe.VideoQuota> videoQuotaCall = peertubeService.getVideoQuota(getToken());
        try {
            Response<UserMe.VideoQuota> response = videoQuotaCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns informations about Peertube such privacies, licenses, etc.
     *
     * @return PeertubeInformation information about peertube
     */
    public PeertubeInformation getPeertubeInformation() {
        PeertubeInformation peertubeInformation = new PeertubeInformation();
        PeertubeService peertubeService = init();
        Call<Map<Integer, String>> categories = peertubeService.getCategories();
        try {
            Response<Map<Integer, String>> response = categories.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setCategories(response.body());
            } else {
                String categoriesStr = Helper.readFileFromAssets(_context, "categories.json");
                try {
                    JSONObject obj = new JSONObject(categoriesStr);
                    Iterator<String> iter = obj.keys();
                    LinkedHashMap<Integer, String> data = new LinkedHashMap<>();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            String value = (String) obj.get(key);
                            data.put(Integer.valueOf(key), value);
                        } catch (JSONException ignored) {
                        }
                    }
                    peertubeInformation.setCategories(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Error error = new Error();
                    error.setStatusCode(response.code());
                    if (response.errorBody() != null) {
                        error.setError(response.errorBody().string());
                    } else {
                        error.setError(_context.getString(R.string.toast_error));
                    }
                }

            }
        } catch (IOException e) {
            String categoriesStr = Helper.readFileFromAssets(_context, "categories.json");
            try {
                JSONObject obj = new JSONObject(categoriesStr);
                Iterator<String> iter = obj.keys();
                LinkedHashMap<Integer, String> data = new LinkedHashMap<>();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        String value = (String) obj.get(key);
                        data.put(Integer.valueOf(key), value);
                    } catch (JSONException ignored) {
                    }
                }
                peertubeInformation.setCategories(data);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        Call<Map<String, String>> languages = peertubeService.getLanguages();
        try {
            Response<Map<String, String>> response = languages.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setLanguages(response.body());
            } else {
                String languageSrt = Helper.readFileFromAssets(_context, "languages.json");
                try {
                    JSONObject obj = new JSONObject(languageSrt);
                    Iterator<String> iter = obj.keys();
                    LinkedHashMap<String, String> data = new LinkedHashMap<>();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        try {
                            String value = (String) obj.get(key);
                            data.put(key, value);
                        } catch (JSONException ignored) {
                        }
                    }
                    peertubeInformation.setLanguages(data);
                } catch (JSONException e) {
                    Error error = new Error();
                    error.setStatusCode(response.code());
                    if (response.errorBody() != null) {
                        error.setError(response.errorBody().string());
                    } else {
                        error.setError(_context.getString(R.string.toast_error));
                    }
                }

            }
        } catch (IOException e) {
            String languageSrt = Helper.readFileFromAssets(_context, "languages.json");
            try {
                JSONObject obj = new JSONObject(languageSrt);
                Iterator<String> iter = obj.keys();
                LinkedHashMap<String, String> data = new LinkedHashMap<>();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        String value = (String) obj.get(key);
                        data.put(key, value);
                    } catch (JSONException ignored) {
                    }
                }
                peertubeInformation.setLanguages(data);
            } catch (JSONException e2) {
            }
        }
        Call<Map<Integer, String>> privacies = peertubeService.getPrivacies();
        try {
            Response<Map<Integer, String>> response = privacies.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setPrivacies(response.body());
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Call<Map<Integer, String>> playlistsPrivacies = peertubeService.getPlaylistsPrivacies();
        try {
            Response<Map<Integer, String>> response = playlistsPrivacies.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setPlaylistPrivacies(response.body());
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Call<Map<Integer, String>> licenses = peertubeService.getLicences();
        try {
            Response<Map<Integer, String>> response = licenses.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setLicences(response.body());
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String lang = Locale.getDefault().getLanguage();
        if (lang.contains("-")) {
            if (!lang.split("-")[0].trim().toLowerCase().startsWith("zh")) {
                lang = lang.split("-")[0];
            } else {
                lang = lang.split("-")[0] + "-" + lang.split("-")[1].toUpperCase();
            }
        }
        if (lang == null || lang.trim().length() == 0) {
            lang = "en";
        }
        Call<Map<String, String>> translations = initTranslation().getTranslations(lang);
        try {
            Response<Map<String, String>> response = translations.execute();
            if (response.isSuccessful()) {
                peertubeInformation.setTranslations(response.body());
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    translations = initTranslation().getTranslations("en");
                    try {
                        response = translations.execute();
                        if (response.isSuccessful()) {
                            peertubeInformation.setTranslations(response.body());
                        } else {
                            error = new Error();
                            error.setStatusCode(response.code());
                            if (response.errorBody() != null) {
                                error.setError(response.errorBody().string());
                            } else {
                                error.setError(_context.getString(R.string.toast_error));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peertubeInformation;
    }

    /**
     * Get instances
     *
     * @param instanceParams InstanceParams
     * @return APIResponse
     */
    public APIResponse getInstances(InstanceParams instanceParams) {
        PeertubeService peertubeService = init();
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("start", "0");
        params.put("count", "250");
        params.put("healthy", "true");
        params.put("signup", "true");
        params.put("sort", "-totalUsers");
        Call<InstanceData> instancesCall = peertubeService.getInstances(params, instanceParams.getNsfwPolicy(), instanceParams.getCategoriesOr(), instanceParams.getLanguagesOr());
        APIResponse apiResponse = new APIResponse();
        try {
            Response<InstanceData> response = instancesCall.execute();
            if (!response.isSuccessful()) {
                setError(apiResponse, response.code(), response.errorBody());
            } else {
                InstanceData instanceData = response.body();
                if (instanceData != null) {
                    apiResponse.setInstances(instanceData.data);
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }


    /**
     * Retrieves next peertube videos *synchronously*
     *
     * @param tags List<String> search
     * @return APIResponse
     */
    public APIResponse searchNextVideos(List<String> tags) {
        PeertubeService peertubeService = init();
        Call<VideoData> searchVideosCall = peertubeService.searchNextVideo(getToken(), tags, "0", "20", showNSFWVideos);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<VideoData> response = searchVideosCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setPeertubes(response.body().data);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Retrieves peertube search *synchronously*
     *
     * @param query String search
     * @return APIResponse
     */
    public APIResponse searchPeertube(String query, String max_id) {
        PeertubeService peertubeService = init();
        Call<VideoData> searchVideosCall = peertubeService.searchVideos(getToken(), query, max_id, count, showNSFWVideos);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<VideoData> response = searchVideosCall.execute();

            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setPeertubes(response.body().data);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }


    /**
     * Retrieves channels search *synchronously*
     *
     * @param query String search
     * @return APIResponse
     */
    public APIResponse searchChannels(String query, String max_id) {
        PeertubeService peertubeService = init();
        Call<ChannelData> searchChannelsCall = peertubeService.searchChannels(getToken(), query, "local", max_id, count);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<ChannelData> response = searchChannelsCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setChannels(response.body().data);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }


    /***
     * Verifiy credential of the authenticated user *synchronously*
     * @return Account
     */
    public UserMe verifyCredentials() throws Error {
        PeertubeService peertubeService = init();
        Call<UserMe> accountCall = peertubeService.verifyCredentials("Bearer " + token);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<UserMe> response = accountCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
                throw error;
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return null;
    }

    public APIResponse report(Report report) {
        PeertubeService peertubeService = init();
        Call<Report.ReportReturn> report1 = peertubeService.report(getToken(), report);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<Report.ReportReturn> response = report1.execute();
            if (response.isSuccessful() && response.body() != null) {
                apiResponse.setActionReturn(response.body().getItemStr().getLabel());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /***
     * Update a video
     * @param videoId String id of the video
     * @param videoParams VideoParams params for the video
     * @param thumbnail File thumbnail
     * @param previewfile File preview
     * @return APIResponse
     */
    public APIResponse updateVideo(String videoId, VideoParams videoParams, Uri thumbnail, Uri previewfile) {
        PeertubeService peertubeService = init();

        MultipartBody.Part bodyThumbnail = null;
        MultipartBody.Part bodyPreviewfile = null;
        try {
            if (thumbnail != null) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(_context, thumbnail);
                String thumbnailName = null;
                if (documentFile != null) {
                    thumbnailName = documentFile.getName();
                }
                bodyThumbnail = createFile("avatarfile", thumbnail, thumbnailName);
            }
            if (previewfile != null && thumbnail != null) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(_context, thumbnail);
                String previewfileName = null;
                if (documentFile != null) {
                    previewfileName = documentFile.getName();
                }
                bodyPreviewfile = createFile("image", previewfile, previewfileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        RequestBody channelId = RequestBody.create(videoParams.getChannelId(), MediaType.parse("text/plain"));
        RequestBody description = RequestBody.create(videoParams.getDescription(), MediaType.parse("text/plain"));
        RequestBody language = RequestBody.create(videoParams.getLanguage(), MediaType.parse("text/plain"));
        RequestBody license = RequestBody.create(videoParams.getLicence(), MediaType.parse("text/plain"));
        RequestBody name = RequestBody.create(videoParams.getName(), MediaType.parse("text/plain"));

        List<RequestBody> tags = null;
        if (videoParams.getTags() != null && videoParams.getTags().size() > 0) {
            tags = new ArrayList<>();
            for (String tag : videoParams.getTags()) {
                tags.add(RequestBody.create(tag, MediaType.parse("text/plain")));
            }
        }
        RequestBody support = null;
        if (videoParams.getSupport() != null) {
            support = RequestBody.create(videoParams.getSupport(), MediaType.parse("text/plain"));
        }


        Call<String> upload = peertubeService.updateVideo(getToken(), videoId,
                channelId, name, videoParams.getCategory(), videoParams.isCommentsEnabled(), description, videoParams.isDownloadEnabled(), language, license, videoParams.isNsfw(),
                videoParams.getPrivacy(), support, tags, videoParams.isWaitTranscoding()
                , bodyThumbnail, bodyPreviewfile);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<String> response = upload.execute();
            if (response.isSuccessful()) {
                apiResponse.setActionReturn("ok");
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    public APIResponse createAccount(AccountCreation accountCreation) {
        PeertubeService peertubeService = init();
        Call<String> report1 = peertubeService.register(accountCreation.getEmail(), accountCreation.getPassword(), accountCreation.getUsername(), accountCreation.getDisplayName());
        APIResponse apiResponse = new APIResponse();
        try {
            Response<String> response = report1.execute();
            if (response.isSuccessful()) {
                apiResponse.setActionReturn(accountCreation.getEmail());
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    public APIResponse post(ActionType actionType, String id, String element) {
        PeertubeService peertubeService = init();
        Call<String> postCall = null;
        APIResponse apiResponse = new APIResponse();
        switch (actionType) {
            case FOLLOW:
                postCall = peertubeService.follow(getToken(), id);
                break;
            case UNFOLLOW:
                postCall = peertubeService.unfollow(getToken(), id);
                break;
            case MUTE:
                postCall = peertubeService.mute(getToken(), id);
                break;
            case UNMUTE:
                postCall = peertubeService.unmute(getToken(), id);
                break;
            case RATEVIDEO:
                postCall = peertubeService.rate(getToken(), id, element);
                break;
            case PEERTUBEDELETEVIDEO:
                postCall = peertubeService.deleteVideo(getToken(), id);
                break;
            case PEERTUBEDELETECOMMENT:
                postCall = peertubeService.deleteComment(getToken(), id, element);
                break;
            case PEERTUBE_DELETE_ALL_COMMENT_FOR_ACCOUNT:
                postCall = peertubeService.deleteAllCommentForAccount(getToken(), id, element);
                break;
            case DELETE_CHANNEL:
                postCall = peertubeService.deleteChannel(getToken(), id);
                break;
        }
        if (postCall != null) {
            try {
                Response<String> response = postCall.execute();
                if (response.isSuccessful()) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }


    /**
     * Get single account by its handle
     *
     * @param accountHandle String
     * @return APIResponse
     */
    public APIResponse getAccount(String accountHandle) {
        PeertubeService peertubeService = init();
        Call<AccountData.PeertubeAccount> accountDataCall = peertubeService.getAccount(accountHandle);
        APIResponse apiResponse = new APIResponse();
        if (accountDataCall != null) {
            try {
                Response<AccountData.PeertubeAccount> response = accountDataCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AccountData.PeertubeAccount> accountList = new ArrayList<>();
                    accountList.add(response.body());
                    apiResponse.setAccounts(accountList);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }


    /**
     * Get video description
     *
     * @param uuid String (pagination)
     * @return APIResponse
     */
    public VideoData.Description getVideoDescription(String uuid) {
        PeertubeService peertubeService = init();
        Call<VideoData.Description> videoDescription = peertubeService.getVideoDescription(uuid);
        try {
            Response<VideoData.Description> response = videoDescription.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Get muted accounts
     *
     * @param maxId String (pagination)
     * @return APIResponse
     */
    public APIResponse getMuted(String maxId) {
        PeertubeService peertubeService = init();
        Call<BlockData> accountDataCall = peertubeService.getMuted("Bearer " + token, maxId, count);
        APIResponse apiResponse = new APIResponse();
        if (accountDataCall != null) {
            try {
                Response<BlockData> response = accountDataCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setMuted(response.body().getData());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }

    /**
     * Get subscriptions data
     *
     * @param maxId String (pagination)
     * @return APIResponse
     */
    public APIResponse getSubscribtions(String maxId) {
        PeertubeService peertubeService = init();
        Call<ChannelData> channelDataCall = peertubeService.getSubscription("Bearer " + token, maxId, count);
        APIResponse apiResponse = new APIResponse();
        if (channelDataCall != null) {
            try {
                Response<ChannelData> response = channelDataCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setChannels(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } catch (IOException e) {
                Error error = new Error();
                error.setError(_context.getString(R.string.toast_error));
                apiResponse.setError(error);
                e.printStackTrace();
            }
        }
        return apiResponse;
    }

    /**
     * Create or update a channel
     *
     * @param apiAction     ChannelsVM.action
     * @param channelId     String
     * @param channelParams PlaylistParams
     * @return APIResponse
     */
    public APIResponse createOrUpdateChannel(ChannelsVM.action apiAction, String channelId, ChannelParams channelParams, Uri avatar) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        try {
            if (apiAction == ChannelsVM.action.CREATE_CHANNEL) {
                Call<ChannelData.ChannelCreation> stringCall = peertubeService.addChannel(getToken(), channelParams);
                Response<ChannelData.ChannelCreation> response = stringCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setActionReturn(response.body().getVideoChannel().getId());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }

            } else if (apiAction == ChannelsVM.action.UPDATE_CHANNEL) {
                Call<String> stringCall = peertubeService.updateChannel(getToken(), channelId, channelParams);
                Response<String> response = stringCall.execute();
                if (response.isSuccessful()) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            }
            if (avatar != null) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(_context, avatar);
                String avatarfileName = null;
                if (documentFile != null) {
                    avatarfileName = documentFile.getName();
                }
                MultipartBody.Part bodyThumbnail = createFile("avatarfile", avatar, avatarfileName);
                Call<UserMe.AvatarResponse> updateProfilePicture = peertubeService.updateChannelProfilePicture(getToken(), channelId, bodyThumbnail);
                Response<UserMe.AvatarResponse> responseAvatar = updateProfilePicture.execute();
                if (responseAvatar.isSuccessful()) {
                    UserMe.AvatarResponse avatarResponse = responseAvatar.body();
                } else {
                    setError(apiResponse, responseAvatar.code(), responseAvatar.errorBody());
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Get Oauth
     *
     * @return APIResponse
     */
    public Oauth oauthClient(String client_name, String redirect_uris, String scopes, String website) {
        PeertubeService peertubeService = init();
        try {
            Call<Oauth> oauth;
            oauth = peertubeService.getOauth(client_name, redirect_uris, scopes, website);
            Response<Oauth> response = oauth.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get NodeInfo
     *
     * @return APIResponse
     */
    public WellKnownNodeinfo.NodeInfo getNodeInfo() {
        PeertubeService peertubeService = initTranslation();
        try {
            Call<WellKnownNodeinfo> wellKnownNodeinfoCall = peertubeService.getWellKnownNodeinfo();
            Response<WellKnownNodeinfo> response = wellKnownNodeinfoCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                int size = response.body().getLinks().size();
                String url = response.body().getLinks().get(size - 1).getHref();
                if (size > 0 && url != null) {
                    peertubeService = initTranslation();
                    String path = new URL(url).getPath();
                    path = path.replaceFirst("/", "").trim();
                    Call<WellKnownNodeinfo.NodeInfo> nodeinfo = peertubeService.getNodeinfo(path);
                    Response<WellKnownNodeinfo.NodeInfo> responseNodeInfo = nodeinfo.execute();
                    return responseNodeInfo.body();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get channel data
     *
     * @param accountDataType AccountDataType (type of requested data)
     * @param element         String (pagination or name for the channel)
     * @return APIResponse
     */
    public APIResponse getChannelData(DataType accountDataType, String element) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        switch (accountDataType) {
            case MY_CHANNELS:
            case CHANNELS_FOR_ACCOUNT:
                Call<ChannelData> channelDataCall = peertubeService.getChannelsForAccount(element);
                try {
                    Response<ChannelData> response = channelDataCall.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        apiResponse.setChannels(response.body().data);
                    } else {
                        setError(apiResponse, response.code(), response.errorBody());
                    }
                } catch (IOException e) {
                    Error error = new Error();
                    error.setError(_context.getString(R.string.toast_error));
                    apiResponse.setError(error);
                    e.printStackTrace();
                }
                break;
            case CHANNEL:
                Call<ChannelData.Channel> channelCall = peertubeService.getChannel(element);
                try {
                    Response<ChannelData.Channel> response = channelCall.execute();
                    if (!response.isSuccessful()) {
                        setError(apiResponse, response.code(), response.errorBody());
                    } else {
                        ChannelData.Channel channelData = response.body();
                        if (channelData != null) {
                            List<ChannelData.Channel> channelList = new ArrayList<>();
                            channelList.add(channelData);
                            apiResponse.setChannels(channelList);
                        }
                    }
                } catch (IOException e) {
                    Error error = new Error();
                    error.setError(_context.getString(R.string.toast_error));
                    apiResponse.setError(error);
                    e.printStackTrace();
                }
                break;
        }
        return apiResponse;
    }

    /**
     * Create or update a playlist
     *
     * @param apiAction      PlaylistsVM.action
     * @param playlistId     String
     * @param playlistParams PlaylistParams
     * @return APIResponse
     */
    public APIResponse createOrUpdatePlaylist(PlaylistsVM.action apiAction, String playlistId, PlaylistParams playlistParams, Uri thumbnail) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        MultipartBody.Part body = null;

        MultipartBody.Part bodyThumbnail = null;
        if (thumbnail != null) {
            DocumentFile documentFile = DocumentFile.fromSingleUri(_context, thumbnail);
            String avatarfileName = null;
            if (documentFile != null) {
                avatarfileName = documentFile.getName();
            }
            try {
                bodyThumbnail = createFile("thumbnailfile", thumbnail, avatarfileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            RequestBody displayName = RequestBody.create(playlistParams.getDisplayName(), MediaType.parse("text/plain"));
            RequestBody description = null;
            if (playlistParams.getDescription() != null) {
                description = RequestBody.create(playlistParams.getDescription(), MediaType.parse("text/plain"));
            }
            RequestBody channelId = RequestBody.create(playlistParams.getVideoChannelId(), MediaType.parse("text/plain"));
            if (apiAction == PlaylistsVM.action.CREATE_PLAYLIST) {
                Call<VideoPlaylistData.VideoPlaylistCreation> stringCall = peertubeService.addPlaylist(getToken(), displayName, description, playlistParams.getPrivacy(), channelId, bodyThumbnail);
                Response<VideoPlaylistData.VideoPlaylistCreation> response = stringCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setActionReturn(response.body().getVideoPlaylist().getId());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }

            } else if (apiAction == PlaylistsVM.action.UPDATE_PLAYLIST) {
                Call<String> stringCall = peertubeService.updatePlaylist(getToken(), playlistId, displayName, description, playlistParams.getPrivacy(), channelId, bodyThumbnail);
                Response<String> response = stringCall.execute();
                if (response.isSuccessful()) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }


    /**
     * Retrieves playlist  *synchronously*
     *
     * @param type       PlaylistsVM.action
     * @param playlistId String id of the playlist
     * @param videoId    String id of the video
     * @return APIResponse
     */
    public APIResponse playlistAction(PlaylistsVM.action type, String playlistId, String videoId, String acct, String max_id) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        try {
            if (type == PlaylistsVM.action.GET_PLAYLIST_INFO) {
                Call<PlaylistData.Playlist> playlistCall = peertubeService.getPlaylist(playlistId);
                Response<PlaylistData.Playlist> response = playlistCall.execute();
                if (response.isSuccessful()) {
                    List<PlaylistData.Playlist> playlists = new ArrayList<>();
                    playlists.add(response.body());
                    apiResponse.setPlaylists(playlists);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }

            } else if (type == PlaylistsVM.action.GET_PLAYLISTS) {
                Call<PlaylistData> playlistsCall = peertubeService.getPlaylistsForAccount(getToken(), acct);
                Response<PlaylistData> response = playlistsCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setPlaylists(response.body().data);
                } else {

                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == PlaylistsVM.action.GET_LIST_VIDEOS) {
                Call<VideoPlaylistData> videosPlayList = peertubeService.getVideosPlayList(getToken(), playlistId, max_id, count);
                Response<VideoPlaylistData> response = videosPlayList.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setVideoPlaylist(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == PlaylistsVM.action.DELETE_PLAYLIST) {
                Call<String> stringCall = peertubeService.deletePlaylist(getToken(), playlistId);
                Response<String> response = stringCall.execute();
                if (response.isSuccessful()) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == PlaylistsVM.action.ADD_VIDEOS) {
                Call<VideoPlaylistData.PlaylistElement> stringCall = peertubeService.addVideoInPlaylist(getToken(), playlistId, videoId);
                Response<VideoPlaylistData.PlaylistElement> response = stringCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setActionReturn(response.body().getVideoPlaylistElement().getId());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == PlaylistsVM.action.DELETE_VIDEOS) {
                Call<String> stringCall = peertubeService.deleteVideoInPlaylist(getToken(), playlistId, videoId);
                Response<String> response = stringCall.execute();
                if (response.isSuccessful()) {
                    apiResponse.setActionReturn(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            }

        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    private void setError(APIResponse apiResponse, int responseCode, ResponseBody errorBody) {
        Error error;
        if (errorBody != null) {
            try {
                error = generateError(responseCode, errorBody.string());
            } catch (IOException e) {
                error = new Error();
                error.setStatusCode(responseCode);
                error.setError(_context.getString(R.string.toast_error));
            }
        } else {
            error = new Error();
            error.setStatusCode(responseCode);
            error.setError(_context.getString(R.string.toast_error));
        }
        if (responseCode == 404 || responseCode == 502) {
            error.setError(_context.getString(R.string.instance_not_availabe));
        }
        apiResponse.setError(error);
    }

    public APIResponse getComments(CommentVM.action type, String videoId, String forCommentId, String max_id) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        try {
            if (type == CommentVM.action.GET_THREAD) {
                Call<CommentData> commentsCall = peertubeService.getComments(videoId, max_id, count);
                Response<CommentData> response = commentsCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setComments(response.body().data);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == CommentVM.action.GET_REPLIES) {
                Call<CommentData.CommentThreadData> commentsCall = peertubeService.getReplies(videoId, forCommentId);
                Response<CommentData.CommentThreadData> response = commentsCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    apiResponse.setCommentThreadData(response.body());
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Manage comments  *synchronously*
     *
     * @param type        (CommentVM.action
     * @param videoId     String id of the video
     * @param toCommentId String id of the comment for replies
     * @param text        String text
     * @return APIResponse
     */
    public APIResponse commentAction(ActionType type, String videoId, String toCommentId, String text) {
        PeertubeService peertubeService = init();

        APIResponse apiResponse = new APIResponse();
        try {
            if (type == ActionType.ADD_COMMENT) {
                Call<CommentData.CommentPosted> commentPostedCall = peertubeService.postComment(getToken(), videoId, text);
                Response<CommentData.CommentPosted> response = commentPostedCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentData.Comment> comments = new ArrayList<>();
                    comments.add(response.body().getComment());
                    apiResponse.setComments(comments);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            } else if (type == ActionType.REPLY) {
                Call<CommentData.CommentPosted> commentPostedCall = peertubeService.postReply(getToken(), videoId, toCommentId, text);
                Response<CommentData.CommentPosted> response = commentPostedCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<CommentData.Comment> comments = new ArrayList<>();
                    comments.add(response.body().getComment());
                    apiResponse.setComments(comments);
                } else {
                    setError(apiResponse, response.code(), response.errorBody());
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Retrieves playlist  *synchronously*
     *
     * @param id String id
     * @return APIResponse
     */
    public APIResponse getPlayist(String id) {
        PeertubeService peertubeService = init();
        Call<PlaylistData.Playlist> playlistCall;
        playlistCall = peertubeService.getPlaylist(id);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<PlaylistData.Playlist> response = playlistCall.execute();
            if (response.isSuccessful()) {
                List<PlaylistData.Playlist> playlists = new ArrayList<>();
                playlists.add(response.body());
                apiResponse.setPlaylists(playlists);
            } else {
                setError(apiResponse, response.code(), response.errorBody());
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    private Error generateError(int responseCode, String message) {
        Error error = new Error();
        error.setStatusCode(responseCode);
        if (message != null) {
            error.setError(message);
        } else {
            error.setError(_context.getString(R.string.toast_error));
        }
        return error;
    }

    /**
     * Retrieves rating of user on a video  *synchronously*
     *
     * @param id String id
     * @return APIResponse
     */
    public APIResponse getRating(String id) {
        PeertubeService peertubeService = init();
        Call<Rating> rating = peertubeService.getRating(getToken(), id);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<Rating> response = rating.execute();
            if (response.isSuccessful()) {
                apiResponse.setRating(response.body());
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Retrieves videos  *synchronously*
     *
     * @param id String id
     * @return APIResponse
     */
    public APIResponse getVideos(String id, boolean myVideo, boolean canUseToken) {
        PeertubeService peertubeService = init();
        Call<VideoData.Video> video;
        if (myVideo || canUseToken) {
            video = peertubeService.getMyVideo(getToken(), id);
        } else {
            video = peertubeService.getVideo(id);
        }
        APIResponse apiResponse = new APIResponse();
        try {
            Response<VideoData.Video> response = video.execute();
            if (response.isSuccessful()) {
                List<VideoData.Video> videos = new ArrayList<>();
                videos.add(response.body());
                apiResponse.setPeertubes(videos);
            } else {

                if (response.errorBody() != null) {

                    String error = response.errorBody().string();
                    if (error.contains("originUrl")) {
                        try {
                            JSONObject jsonObject = new JSONObject(error);
                            List<VideoData.Video> videos = new ArrayList<>();
                            VideoData.Video videoRedirect = new VideoData.Video();
                            videoRedirect.setErrorCode(jsonObject.getInt("errorCode"));
                            videoRedirect.setOriginUrl(jsonObject.getString("originUrl"));
                            videos.add(videoRedirect);
                            apiResponse.setPeertubes(videos);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (error.contains("error")) {
                        try {
                            JSONObject jsonObject = new JSONObject(error);
                            List<VideoData.Video> videos = new ArrayList<>();
                            VideoData.Video videoErrorMessage = new VideoData.Video();
                            videoErrorMessage.setErrorMessage(jsonObject.getString("error"));
                            videos.add(videoErrorMessage);
                            apiResponse.setPeertubes(videos);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Error error = new Error();
                    error.setStatusCode(response.code());
                    if (response.errorBody() != null) {
                        error.setError(response.errorBody().string());
                    } else {
                        error.setError(_context.getString(R.string.toast_error));
                    }
                }

            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return apiResponse;

    }


    public enum DataType {
        SUBSCRIBER,
        MUTED,
        CHANNELS_FOR_ACCOUNT,
        CHANNEL,
        MY_CHANNELS
    }


    public enum ActionType {
        FOLLOW,
        UNFOLLOW,
        MUTE,
        UNMUTE,
        RATEVIDEO,
        PEERTUBEDELETECOMMENT,
        PEERTUBE_DELETE_ALL_COMMENT_FOR_ACCOUNT,
        PEERTUBEDELETEVIDEO,
        REPORT_VIDEO,
        REPORT_ACCOUNT,
        REPORT_COMMENT,
        DELETE_CHANNEL,
        ADD_COMMENT,
        REPLY,
    }


}
