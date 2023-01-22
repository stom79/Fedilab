package app.fedilab.android.peertube.client;
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

import java.util.List;
import java.util.Map;

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
import app.fedilab.android.peertube.client.entities.CaptionsParams;
import app.fedilab.android.peertube.client.entities.ChannelParams;
import app.fedilab.android.peertube.client.entities.NotificationSettings;
import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.OverviewVideo;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.Rating;
import app.fedilab.android.peertube.client.entities.Report;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.client.entities.WellKnownNodeinfo;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

@SuppressWarnings({"unused", "RedundantSuppression"})
public interface PeertubeService {

    @GET("instances")
    Call<InstanceData> getInstances(
            @QueryMap Map<String, String> params,
            @Query("nsfwPolicy[]") String nsfwPolicy,
            @Query("categoriesOr[]") List<Integer> categories,
            @Query("languagesOr[]") List<String> languages);

    //Server settings
    @GET(".well-known/nodeinfo")
    Call<WellKnownNodeinfo> getWellKnownNodeinfo();

    @GET("plugins/peertube-plugin-player-watermark/public-settings")
    Call<PluginData.WaterMark> waterMark();

    //Instance info
    @GET("config/about")
    Call<InstanceData.InstanceInfo> configAbout();

    //Instance config
    @GET("config")
    Call<InstanceData.InstanceConfig> config();

    @GET("{nodeInfoPath}")
    Call<WellKnownNodeinfo.NodeInfo> getNodeinfo(@Path(value = "nodeInfoPath", encoded = true) String nodeInfoPath);

    @GET("{captionContent}")
    Call<String> getCaptionContent(@Path("captionContent") String captionContent);

    @GET("videos/categories")
    Call<Map<Integer, String>> getCategories();

    @GET("videos/languages")
    Call<Map<String, String>> getLanguages();

    @GET("videos/privacies")
    Call<Map<Integer, String>> getPrivacies();

    @GET("video-playlists/privacies")
    Call<Map<Integer, String>> getPlaylistsPrivacies();

    @GET("videos/licences")
    Call<Map<Integer, String>> getLicences();

    //This one doesn't use api/v1 path
    @GET("client/locales/{local}/server.json")
    Call<Map<String, String>> getTranslations(@Path("local") String local);


    //TOKEN
    //Refresh
    @FormUrlEncoded
    @POST("users/token")
    Call<Token> createOpenIdToken(
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("response_type") String response_type,
            @Field("grant_type") String grant_type,
            @Field("scope") String scope,
            @Field("username") String username,
            @Field("password") String password,
            @Field("externalAuthToken") String externalAuthToken);

    //TOKEN
    //Refresh
    @FormUrlEncoded
    @POST("users/token")
    Call<Token> createToken(
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("grant_type") String grant_type,
            @Field("username") String username,
            @Field("password") String password);

    //TOKEN
    //Refresh
    @FormUrlEncoded
    @POST("users/token")
    Call<Token> refreshToken(
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("refresh_token") String refresh_token,
            @Field("grant_type") String grant_type);

    @GET("users/me")
    Call<UserMe> verifyCredentials(@Header("Authorization") String credentials);


    @GET("users/me/video-quota-used")
    Call<UserMe.VideoQuota> getVideoQuota(@Header("Authorization") String credentials);

    @FormUrlEncoded
    @PUT("videos/{id}/watching")
    Call<String> addToHistory(
            @Header("Authorization") String credentials,
            @Path("id") String id,
            @Field("currentTime") long currentTime);


    @FormUrlEncoded
    @PUT("users/me")
    Call<String> updateUser(
            @Header("Authorization") String credentials,
            @Field("videosHistoryEnabled") Boolean videosHistoryEnabled,
            @Field("autoPlayVideo") Boolean autoPlayVideo,
            @Field("autoPlayNextVideo") Boolean autoPlayNextVideo,
            @Field("webTorrentEnabled") Boolean webTorrentEnabled,
            @Field("videoLanguages") List<String> videoLanguages,
            @Field("description") String description,
            @Field("displayName") String displayName,
            @Field("nsfwPolicy") String nsfwPolicy
    );

    @Multipart
    @POST("video-channels/{channelHandle}/avatar/pick")
    Call<UserMe.AvatarResponse> updateChannelProfilePicture(
            @Header("Authorization") String credentials,
            @Path("channelHandle") String channelHandle,
            @Part MultipartBody.Part avatarfile);

    @Multipart
    @POST("users/me/avatar/pick")
    Call<UserMe.AvatarResponse> updateProfilePicture(
            @Header("Authorization") String credentials,
            @Part MultipartBody.Part avatarfile);

    //Timelines Authenticated
    //Subscriber timeline
    @GET("users/me/subscriptions/videos?sort=-publishedAt")
    Call<VideoData> getSubscriptionVideos(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String coun,
            @Query("languageOneOf") List<String> languageOneOf);

    //Overview videos
    @GET("overviews/videos")
    Call<OverviewVideo> getOverviewVideos(
            @Header("Authorization") String credentials,
            @Query("page") String page,
            @Query("nsfw") String nsfw,
            @Query("languageOneOf") List<String> languageOneOf);

    //Most liked videos
    @GET("videos?sort=-likes")
    Call<VideoData> getMostLikedVideos(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw,
            @Query("languageOneOf") List<String> languageOneOf);

    //Trending videos
    @GET("videos?sort=-trending")
    Call<VideoData> getTrendingVideos(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw,
            @Query("languageOneOf") List<String> languageOneOf);

    //Recently added videos
    @GET("videos?sort=-publishedAt")
    Call<VideoData> getRecentlyAddedVideos(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw,
            @Query("languageOneOf") List<String> languageOneOf);

    //Local videos
    @GET("videos?sort=-publishedAt&filter=local")
    Call<VideoData> getLocalVideos(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw,
            @Query("languageOneOf") List<String> languageOneOf);

    //History
    @GET("users/me/history/videos")
    Call<VideoData> getHistory(
            @Header("Authorization") String credentials,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @POST("users/me/history/videos/remove")
    Call<String> deleteHistory(
            @Header("Authorization") String credentials);

    //Search videos
    @GET("search/videos")
    Call<VideoData> searchVideos(
            @Header("Authorization") String credentials,
            @Query("search") String search,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw);

    //Search channels
    @GET("search/video-channels")
    Call<ChannelData> searchChannels(
            @Header("Authorization") String credentials,
            @Query("search") String search,
            @Query("searcharget") String searchTarget,
            @Query("start") String maxId,
            @Query("count") String count);

    //Search
    @GET("search/videos")
    Call<VideoData> searchNextVideo(
            @Header("Authorization") String credentials,
            @Query("tagsOneOf") List<String> tagsOneOf,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw);

    //Get notifications
    @GET("users/me/notifications")
    Call<NotificationData> getNotifications(@Header("Authorization") String credentials, @Query("start") String maxId, @Query("count") String count, @Query("since_id") String sinceId);

    @GET("users/me/notifications?start=0&count=0&unread=true")
    Call<NotificationData> countNotifications(@Header("Authorization") String credentials);

    @POST("users/me/notifications/read-all")
    Call<String> markAllAsRead(@Header("Authorization") String credentials);

    @FormUrlEncoded
    @POST("users/me/notifications/read")
    Call<String> markAsRead(@Header("Authorization") String credentials, @Field("ids[]") List<String> ids);

    //Update Notification settings
    @PUT("users/me/notification-settings")
    Call<String> updateNotifications(@Header("Authorization") String credentials, @Body NotificationSettings notificationSettings);

    //Get/Post/Update/Delete video
    //Get a video
    @GET("videos/{id}")
    Call<VideoData.Video> getVideo(@Path("id") String id);

    //Get a video description
    @GET("videos/{uuid}/description")
    Call<VideoData.Description> getVideoDescription(@Path("uuid") String uuid);

    @GET("videos/{id}")
    Call<VideoData.Video> getMyVideo(@Header("Authorization") String credentials, @Path("id") String id);

    //Get my video
    @GET("users/me/videos?sort=-publishedAt")
    Call<VideoData> getMyVideos(@Header("Authorization") String credentials, @Query("start") String maxId, @Query("count") String count);

    //Get user videos
    @GET("accounts/{name}/videos?sort=-publishedAt")
    Call<VideoData> getVideosForAccount(
            @Path("name") String name,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw
    );

    @POST("videos/{id}/views")
    Call<String> postView(@Path("id") String id);

    @Multipart
    @PUT("videos/{id}")
    Call<String> updateVideo(
            @Header("Authorization") String credentials,
            @Path("id") String videoId,
            @Part("channelId") RequestBody channelId,
            @Part("name") RequestBody name,
            @Part("category") int category,
            @Part("commentsEnabled") boolean commentsEnabled,
            @Part("description") RequestBody description,
            @Part("downloadEnabled") boolean downloadEnabled,
            @Part("language") RequestBody language,
            @Part("licence") RequestBody licence,
            @Part("nsfw") boolean nsfw,
            @Part("privacy") int privacy,
            @Part("support") RequestBody support,
            @Part("tags[]") List<RequestBody> tags,
            @Part("waitTranscoding") boolean waitTranscoding,
            @Part MultipartBody.Part thumbnailfile,
            @Part MultipartBody.Part previewfile);

    @DELETE("videos/{id}")
    Call<String> deleteVideo(@Header("Authorization") String credentials, @Path("id") String videoId);


    @GET("oauth-clients/local")
    Call<Oauth> getOauthAcad();

    @GET("oauth-clients/local")
    Call<Oauth> getOauth(@Query("client_name") String client_name, @Query("redirect_uris") String redirect_uris, @Query("scopes") String scopes, @Query("website") String website);


    //Post/Update/Delete channel
    //Channels for account
    @GET("accounts/{accountId}/video-channels")
    Call<ChannelData> getChannelsForAccount(@Path("accountId") String accountId);

    //Get a channel
    @GET("video-channels/{name}")
    Call<ChannelData.Channel> getChannel(@Path("name") String name);

    @GET("video-channels")
    Call<ChannelData> getAllChannels();

    @GET("video-channels/{channelHandle}/videos")
    Call<VideoData> getChannelVideos(
            @Path("channelHandle") String channelHandle,
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("nsfw") String nsfw);

    @POST("video-channels")
    Call<ChannelData.ChannelCreation> addChannel(@Header("Authorization") String credentials, @Body ChannelParams channelParams);

    @PUT("video-channels/{channelHandle}")
    Call<String> updateChannel(@Header("Authorization") String credentials, @Path("channelHandle") String channelHandle, @Body ChannelParams channelParams);

    @DELETE("video-channels/{channelHandle}")
    Call<String> deleteChannel(@Header("Authorization") String credentials, @Path("channelHandle") String channelHandle);


    //Get/Post/Update/Delete playlist
    @GET("video-playlists")
    Call<PlaylistData> getPlaylists();


    //Get a single account
    @GET("accounts/{accountHandle}")
    Call<AccountData.Account> getAccount(@Path("accountHandle") String accountHandle);

    //Get/Post/Update/Delete playlist
    @GET("accounts/{accountHandle}/video-playlists")
    Call<PlaylistData> getPlaylistsForAccount(@Header("Authorization") String credentials, @Path("accountHandle") String accountHandle);

    @GET("video-playlists/{id}")
    Call<PlaylistData.Playlist> getPlaylist(@Path("id") String id);

    @GET("video-playlists/{id}/videos")
    Call<VideoPlaylistData> getVideosPlayList(@Header("Authorization") String credentials, @Path("id") String id, @Query("start") String maxId, @Query("count") String count);

    @GET("users/me/video-playlists/videos-exist")
    Call<Map<String, List<PlaylistExist>>> getVideoExistsInPlaylist(@Header("Authorization") String credentials, @Query("videoIds") List<String> videoIds);

    @Multipart
    @POST("video-playlists")
    Call<VideoPlaylistData.VideoPlaylistCreation> addPlaylist(
            @Header("Authorization") String credentials,
            @Part("displayName") RequestBody displayName,
            @Part("description") RequestBody description,
            @Part("privacy") int privacy,
            @Part("videoChannelId") RequestBody videoChannelId,
            @Part MultipartBody.Part thumbnailfile);

    @Multipart
    @PUT("video-playlists/{id}")
    Call<String> updatePlaylist(
            @Header("Authorization") String credentials,
            @Path("id") String videoId,
            @Part("displayName") RequestBody displayName,
            @Part("description") RequestBody description,
            @Part("privacy") int privacy,
            @Part("videoChannelId") RequestBody videoChannelId,
            @Part MultipartBody.Part thumbnailfil);


    @FormUrlEncoded
    @POST("video-playlists/{id}/videos")
    Call<VideoPlaylistData.PlaylistElement> addVideoInPlaylist(@Header("Authorization") String credentials, @Path("id") String id, @Field("videoId") String videoId);


    @DELETE("video-playlists/{id}")
    Call<String> deletePlaylist(@Header("Authorization") String credentials, @Path("id") String playlistId);

    @DELETE("video-playlists/{id}/videos/{playlistElementId}")
    Call<String> deleteVideoInPlaylist(@Header("Authorization") String credentials, @Path("id") String videoId, @Path("playlistElementId") String playlistElementId);

    //Get/Update/Delete captions
    @GET("videos/{id}/captions")
    Call<CaptionData> getCaptions(@Path("id") String videoId);

    @PUT("videos/{id}/captions/{captionLanguage}")
    Call<String> updateCaptions(@Header("Authorization") String credentials, @Path("id") String videoId, @Path("captionLanguage") String captionLanguage, @Body CaptionsParams captionsParams, @Part MultipartBody.Part captionfile);

    @DELETE("videos/{id}/captions/{captionLanguage}")
    Call<String> deleteCaptions(@Header("Authorization") String credentials, @Path("id") String videoId, @Path("captionLanguage") String captionLanguage);


    //Subscribe/Unsubscribe
    //subscribers
    @GET("users/me/subscriptions")
    Call<ChannelData> getSubscription(@Header("Authorization") String credentials, @Query("start") String maxId, @Query("count") String count);

    @GET("users/me/subscriptions/exist")
    Call<Map<String, Boolean>> getSubscriptionsExist(@Header("Authorization") String credentials, @Query("uris") List<String> uris);

    @FormUrlEncoded
    @POST("users/me/subscriptions")
    Call<String> follow(@Header("Authorization") String credentials, @Field("uri") String uri);

    @DELETE("users/me/subscriptions/{subscriptionHandle}")
    Call<String> unfollow(@Header("Authorization") String credentials, @Path("subscriptionHandle") String subscriptionHandle);

    //Mute/Unmute
    //Muted accounts
    @GET("users/me/blocklist/accounts")
    Call<BlockData> getMuted(@Header("Authorization") String credentials, @Query("start") String maxId, @Query("count") String count);

    @FormUrlEncoded
    @POST("users/me/blocklist/accounts")
    Call<String> mute(@Header("Authorization") String credentials, @Field("accountName") String accountName);

    @DELETE("users/me/blocklist/accounts/{accountName}")
    Call<String> unmute(@Header("Authorization") String credentials, @Path("accountName") String accountName);


    //Get video rating
    @GET("users/me/videos/{id}/rating")
    Call<Rating> getRating(@Header("Authorization") String credentials, @Path("id") String id);

    //Like/unlike
    @FormUrlEncoded
    @PUT("videos/{id}/rate")
    Call<String> rate(@Header("Authorization") String credentials, @Path("id") String id, @Field("rating") String rating);


    //Comment
    @GET("videos/{id}/comment-threads")
    Call<CommentData> getComments(@Path("id") String id, @Query("start") String maxId, @Query("count") String count);

    @GET("videos/{id}/comment-threads/{threadId}")
    Call<CommentData.CommentThreadData> getReplies(@Path("id") String id, @Path("threadId") String threadId);

    @FormUrlEncoded
    @POST("videos/{id}/comment-threads")
    Call<CommentData.CommentPosted> postComment(@Header("Authorization") String credentials, @Path("id") String id, @Field("text") String text);

    @FormUrlEncoded
    @POST("videos/{id}/comments/{commentId}")
    Call<CommentData.CommentPosted> postReply(@Header("Authorization") String credentials, @Path("id") String id, @Path("commentId") String commentId, @Field("text") String text);

    @DELETE("videos/{id}/comments/{commentId}")
    Call<String> deleteComment(@Header("Authorization") String credentials, @Path("id") String id, @Path("commentId") String commentId);

    @POST("bulk/remove-comments-of")
    Call<String> deleteAllCommentForAccount(@Header("Authorization") String credentials, @Field("accountName") String accountName, @Field("scope") String scope);

    @Headers({"Content-Type: application/json", "Cache-Control: max-age=640000"})
    @POST("abuses")
    Call<Report.ReportReturn> report(
            @Header("Authorization") String credentials,
            @Body Report report);

    @FormUrlEncoded
    @POST("users/register")
    Call<String> register(
            @Field("email") String email,
            @Field("password") String password,
            @Field("username") String username,
            @Field("displayName") String displayName
    );
}
