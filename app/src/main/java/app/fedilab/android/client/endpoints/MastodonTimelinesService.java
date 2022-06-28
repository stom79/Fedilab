package app.fedilab.android.client.endpoints;
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

import java.util.List;

import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Conversation;
import app.fedilab.android.client.entities.api.Marker;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.misskey.MisskeyNote;
import app.fedilab.android.client.entities.peertube.PeertubeVideo;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonTimelinesService {

    //Public timelines
    @GET("timelines/public")
    Call<List<Status>> getPublic(
            @Header("Authorization") String token,
            @Query("local") Boolean local,
            @Query("remote") Boolean remote,
            @Query("only_media") Boolean only_media,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") Integer limit
    );

    //Public Tags timelines
    @GET("timelines/tag/{hashtag}")
    Call<List<Status>> getHashTag(
            @Header("Authorization") String token,
            @Path("hashtag") String hashtag,
            @Query("local") boolean local,
            @Query("only_media") boolean only_media,
            @Query("all[]") List<String> all,
            @Query("any[]") List<String> any,
            @Query("none[]") List<String> none,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //Home timeline
    @GET("timelines/home")
    Call<List<Status>> getHome(
            @Header("Authorization") String token,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit,
            @Query("local") boolean local
    );

    //List timeline
    @GET("timelines/list/{list_id}")
    Call<List<Status>> getList(
            @Header("Authorization") String token,
            @Path("list_id") String list_id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //get conversations
    @GET("conversations")
    Call<List<Conversation>> getConversations(
            @Header("Authorization") String token,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //Delete a conversation
    @DELETE("conversations/{id}")
    Call<Void> deleteConversation(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Mark a conversation as read
    @FormUrlEncoded
    @POST("conversations/{id}/read")
    Call<Status> markReadConversation(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Show user list
    @GET("lists")
    Call<List<MastodonList>> getLists(
            @Header("Authorization") String token
    );

    //Get Single list
    @GET("lists/{id}")
    Call<MastodonList> getList(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Create a user list
    @FormUrlEncoded
    @POST("lists")
    Call<MastodonList> createList(
            @Header("Authorization") String token,
            @Field("title") String title,
            @Field("replies_policy") String replies_policy
    );

    //Update a list
    @FormUrlEncoded
    @PUT("lists/{id}")
    Call<MastodonList> updateList(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("title") String title,
            @Field("replies_policy") String replies_policy
    );

    //Delete a conversation
    @DELETE("lists/{id}")
    Call<Void> deleteList(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get accounts in a list
    @GET("lists/{id}/accounts")
    Call<List<Account>> getAccountsInList(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("limit") int limit
    );

    //Add account in a list
    @FormUrlEncoded
    @POST("lists/{id}/accounts")
    Call<Void> addAccountsList(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("account_ids[]") List<String> account_ids
    );

    //Delete accounts in a list
    @DELETE("lists/{id}/accounts")
    Call<Void> deleteAccountsList(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("account_ids[]") List<String> account_ids
    );

    //Get a marker
    @GET("markers")
    Call<Marker> getMarker(
            @Header("Authorization") String token,
            @Query("timeline") List<String> timeline
    );

    //Save marker
    @FormUrlEncoded
    @POST("markers")
    Call<Void> addMarker(
            @Header("Authorization") String token,
            @Field("home[last_read_id]") String home_last_read_id,
            @Field("notifications[last_read_id]") String notifications_last_read_id
    );


    @Headers({"Accept: application/json"})
    @POST("api/notes")
    Call<List<MisskeyNote>> getMisskey(@Body MisskeyNote.MisskeyParams params);


    //Public timelines for Misskey
    @FormUrlEncoded
    @POST("api/notes")
    Call<List<MisskeyNote>> getMisskey(
            @Field("local") boolean local,
            @Field("file") boolean file,
            @Field("poll") boolean poll,
            @Field("remote") boolean remote,
            @Field("reply") boolean reply,
            @Field("untilId") String max_id,
            @Field("since_id") String since_id,
            @Field("limit") Integer limit
    );

    @GET("api/v1/videos")
    Call<PeertubeVideo> getPeertube(
            @Query("start") String start,
            @Query("filter") String filter,
            @Query("sort") String sort,
            @Query("count") int count
    );

    @GET("api/v1/videos/{id}")
    Call<PeertubeVideo.Video> getPeertubeVideo(
            @Path("id") String id
    );
}
