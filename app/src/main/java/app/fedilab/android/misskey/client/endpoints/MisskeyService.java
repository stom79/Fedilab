package app.fedilab.android.misskey.client.endpoints;
/* Copyright 2026 Thomas Schneider
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

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import app.fedilab.android.misskey.client.entities.MisskeyEmoji;
import app.fedilab.android.misskey.client.entities.MisskeyFavorite;
import app.fedilab.android.misskey.client.entities.MisskeyFile;
import app.fedilab.android.misskey.client.entities.MisskeyMeta;
import app.fedilab.android.misskey.client.entities.MisskeyNote;
import app.fedilab.android.misskey.client.entities.MisskeyNotification;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.MisskeyScheduledNote;
import app.fedilab.android.misskey.client.entities.MisskeyUser;
import app.fedilab.android.misskey.client.entities.NoteCreateRequest;
import app.fedilab.android.misskey.client.entities.NoteCreateResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface MisskeyService {

    // ========== Account ==========

    @POST("i")
    Call<MisskeyUser> verifyCredentials(@Body MisskeyRequest request);

    @POST("users/show")
    Call<MisskeyUser> getUser(@Body MisskeyRequest.UserIdRequest request);

    @POST("users/show")
    Call<MisskeyUser> getUserByUsername(@Body MisskeyRequest.UsernameRequest request);

    @POST("users/followers")
    Call<List<MisskeyFollowing>> getFollowers(@Body MisskeyRequest.UserIdRequest request);

    @POST("users/following")
    Call<List<MisskeyFollowing>> getFollowing(@Body MisskeyRequest.UserIdRequest request);

    @POST("following/create")
    Call<MisskeyUser> follow(@Body MisskeyRequest.UserIdRequest request);

    @POST("following/delete")
    Call<MisskeyUser> unfollow(@Body MisskeyRequest.UserIdRequest request);

    @POST("blocking/create")
    Call<MisskeyUser> block(@Body MisskeyRequest.UserIdRequest request);

    @POST("blocking/delete")
    Call<MisskeyUser> unblock(@Body MisskeyRequest.UserIdRequest request);

    @POST("mute/create")
    Call<Void> mute(@Body MisskeyRequest.MuteRequest request);

    @POST("mute/delete")
    Call<Void> unmute(@Body MisskeyRequest.UserIdRequest request);

    @POST("i/update")
    Call<MisskeyUser> updateProfile(@Body MisskeyRequest.UpdateProfileRequest request);

    @POST("i/pin")
    Call<MisskeyUser> pinNote(@Body MisskeyRequest.NoteIdRequest request);

    @POST("i/unpin")
    Call<MisskeyUser> unpinNote(@Body MisskeyRequest.NoteIdRequest request);

    @POST("following/requests/accept")
    Call<Void> acceptFollowRequest(@Body MisskeyRequest.UserIdRequest request);

    @POST("following/requests/reject")
    Call<Void> rejectFollowRequest(@Body MisskeyRequest.UserIdRequest request);

    @POST("blocking/list")
    Call<List<MisskeyBlocking>> getBlocks(@Body MisskeyRequest request);

    @POST("mute/list")
    Call<List<MisskeyMuting>> getMutes(@Body MisskeyRequest request);

    @POST("users/relation")
    Call<List<MisskeyRelation>> getRelations(@Body MisskeyRequest.UserIdRequest request);

    // ========== Notes ==========

    @POST("notes/create")
    Call<NoteCreateResponse> createNote(@Body NoteCreateRequest request);

    @POST("notes/show")
    Call<MisskeyNote> getNote(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/delete")
    Call<Void> deleteNote(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/reactions/create")
    Call<Void> createReaction(@Body MisskeyRequest.ReactionRequest request);

    @POST("notes/reactions/delete")
    Call<Void> deleteReaction(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/favorites/create")
    Call<Void> createFavorite(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/favorites/delete")
    Call<Void> deleteFavorite(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/unrenote")
    Call<Void> unrenote(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/polls/vote")
    Call<Void> votePoll(@Body MisskeyRequest.PollVoteRequest request);

    @POST("notes/renotes")
    Call<List<MisskeyNote>> getRenotes(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/reactions")
    Call<List<MisskeyReaction>> getReactions(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/scheduled/list")
    Call<List<MisskeyScheduledNote>> getScheduledNotes(@Body MisskeyRequest.ScheduledListRequest request);

    @POST("notes/scheduled/cancel")
    Call<Void> cancelScheduledNote(@Body MisskeyRequest.ScheduledCancelRequest request);

    @POST("notes/children")
    Call<List<MisskeyNote>> getReplies(@Body MisskeyRequest.NoteIdRequest request);

    @POST("notes/conversation")
    Call<List<MisskeyNote>> getConversation(@Body MisskeyRequest.NoteIdRequest request);

    @POST("users/notes")
    Call<List<MisskeyNote>> getUserNotes(@Body MisskeyRequest.UserNotesRequest request);

    @POST("users/reactions")
    Call<List<MisskeyUserReaction>> getUserReactions(@Body MisskeyRequest.UserIdRequest request);

    // ========== Timelines ==========

    @POST("notes/timeline")
    Call<List<MisskeyNote>> getHomeTimeline(@Body MisskeyRequest.TimelineRequest request);

    @POST("notes/local-timeline")
    Call<List<MisskeyNote>> getLocalTimeline(@Body MisskeyRequest.TimelineRequest request);

    @POST("notes/global-timeline")
    Call<List<MisskeyNote>> getGlobalTimeline(@Body MisskeyRequest.TimelineRequest request);

    @POST("notes/hybrid-timeline")
    Call<List<MisskeyNote>> getHybridTimeline(@Body MisskeyRequest.TimelineRequest request);

    @POST("notes/search-by-tag")
    Call<List<MisskeyNote>> getHashtagTimeline(@Body MisskeyRequest.HashtagRequest request);

    @POST("notes/featured")
    Call<List<MisskeyNote>> getFeaturedNotes(@Body MisskeyRequest request);

    // ========== Search ==========

    @POST("notes/search")
    Call<List<MisskeyNote>> searchNotes(@Body MisskeyRequest.SearchRequest request);

    @POST("users/search")
    Call<List<MisskeyUser>> searchUsers(@Body MisskeyRequest.SearchRequest request);

    @POST("hashtags/search")
    Call<List<String>> searchHashtags(@Body MisskeyRequest.SearchRequest request);

    // ========== Notifications ==========

    @POST("i/notifications")
    Call<List<MisskeyNotification>> getNotifications(@Body MisskeyRequest.NotificationsRequest request);

    @POST("notifications/mark-all-as-read")
    Call<Void> markAllNotificationsAsRead(@Body MisskeyRequest request);

    // ========== Drive ==========

    @Multipart
    @POST("drive/files/create")
    Call<MisskeyFile> uploadFile(
            @Part("i") RequestBody token,
            @Part MultipartBody.Part file,
            @Part("isSensitive") RequestBody sensitive,
            @Part("comment") RequestBody comment
    );

    @POST("drive/files/update")
    Call<MisskeyFile> updateFile(@Body MisskeyFileUpdateRequest request);

    // ========== Favorites (Bookmarks) ==========

    @POST("i/favorites")
    Call<List<MisskeyFavorite>> getFavorites(@Body MisskeyRequest request);

    // ========== Report ==========

    @POST("users/report-abuse")
    Call<Void> reportAbuse(@Body MisskeyRequest.ReportRequest request);

    // ========== Lists ==========

    @POST("users/lists/list")
    Call<List<MisskeyList>> getLists(@Body MisskeyRequest request);

    @POST("users/lists/show")
    Call<MisskeyList> getList(@Body MisskeyRequest.ListRequest request);

    @POST("users/lists/create")
    Call<MisskeyList> createList(@Body MisskeyRequest.CreateListRequest request);

    @POST("users/lists/update")
    Call<MisskeyList> updateList(@Body MisskeyRequest.UpdateListRequest request);

    @POST("users/lists/delete")
    Call<Void> deleteList(@Body MisskeyRequest.ListRequest request);

    @POST("users/lists/push")
    Call<Void> addUserToList(@Body MisskeyRequest.ListUserRequest request);

    @POST("users/lists/pull")
    Call<Void> removeUserFromList(@Body MisskeyRequest.ListUserRequest request);

    @POST("notes/user-list-timeline")
    Call<List<MisskeyNote>> getListTimeline(@Body MisskeyRequest.ListTimelineRequest request);

    // ========== ActivityPub ==========

    @POST("ap/show")
    Call<ApShowResponse> apShow(@Body MisskeyRequest.ApShowRequest request);

    // ========== Instance ==========

    @POST("meta")
    Call<MisskeyMeta> getMeta(@Body MisskeyRequest.MetaRequest request);

    @retrofit2.http.GET("emojis")
    Call<MisskeyEmojisResponse> getEmojis();

    // ========== Inner classes for responses ==========

    class MisskeyFollowing {
        public String id;
        public MisskeyUser follower;
        public MisskeyUser followee;
    }

    class MisskeyRelation {
        public String id;
        public boolean isFollowing;
        public boolean isFollowed;
        public boolean hasPendingFollowRequestFromYou;
        public boolean hasPendingFollowRequestToYou;
        public boolean isBlocking;
        public boolean isBlocked;
        public boolean isMuted;
    }

    class MisskeyReaction {
        public String id;
        public String type;
        public MisskeyUser user;
    }

    class MisskeyBlocking {
        public String id;
        public MisskeyUser blockee;
    }

    class MisskeyMuting {
        public String id;
        public MisskeyUser mutee;
    }

    class MisskeyEmojisResponse {
        public List<MisskeyEmoji> emojis;
    }

    class MisskeyList {
        public String id;
        public String name;
        public List<String> userIds;
    }

    class MisskeyUserReaction {
        public String id;
        public String type;
        public MisskeyNote note;
    }

    class ApShowResponse implements Serializable {
        @SerializedName("type")
        public String type;
        @SerializedName("object")
        public JsonObject object;
    }

    class MisskeyFileUpdateRequest extends MisskeyRequest {
        public String fileId;
        public String name;
        public String folderId;
        public String comment;
        public Boolean isSensitive;

        public MisskeyFileUpdateRequest(String token, String fileId) {
            super(token);
            this.fileId = fileId;
        }
    }
}
