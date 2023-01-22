package app.fedilab.android.mastodon.client.endpoints;
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

import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Card;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.StatusSource;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface MastodonStatusesService {

    //Post a status
    @FormUrlEncoded
    @POST("statuses")
    Call<Status> createStatus(
            @Header("Idempotency-Key") String idempotency_Key,
            @Header("Authorization") String token,
            @Field("status") String status,
            @Field("media_ids[]") List<String> media_ids,
            @Field("poll[options][]") List<String> poll_options,
            @Field("poll[expires_in]") Integer poll_expire_in,
            @Field("poll[multiple]") Boolean poll_multiple,
            @Field("poll[hide_totals]") Boolean poll_hide_totals,
            @Field("in_reply_to_id") String in_reply_to_id,
            @Field("sensitive") Boolean sensitive,
            @Field("spoiler_text") String spoiler_text,
            @Field("visibility") String visibility,
            @Field("language") String language,
            @Field("quote_id") String quote_id,
            @Field("content_type") String content_type
    );

    @GET("statuses/{id}/source")
    Call<StatusSource> getStatusSource(
            @Header("Authorization") String token,
            @Path("id") String id);

    @GET("statuses/{id}/history")
    Call<List<Status>> getStatusHistory(
            @Header("Authorization") String token,
            @Path("id") String id);

    //Post a status
    @FormUrlEncoded
    @PUT("statuses/{id}")
    Call<Status> updateStatus(
            @Header("Idempotency-Key") String idempotency_Key,
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("status") String status,
            @Field("media_ids[]") List<String> media_ids,
            @Field("poll[options][]") List<String> poll_options,
            @Field("poll[expires_in]") Integer poll_expire_in,
            @Field("poll[multiple]") Boolean poll_multiple,
            @Field("poll[hide_totals]") Boolean poll_hide_totals,
            @Field("in_reply_to_id") String in_reply_to_id,
            @Field("sensitive") Boolean sensitive,
            @Field("spoiler_text") String spoiler_text,
            @Field("visibility") String visibility,
            @Field("language") String language
    );


    //Post a scheduled status
    @FormUrlEncoded
    @POST("statuses")
    Call<ScheduledStatus> createScheduledStatus(
            @Header("Idempotency-Key") String idempotency_Key,
            @Header("Authorization") String token,
            @Field("status") String status,
            @Field("media_ids[]") List<String> media_ids,
            @Field("poll[options][]") List<String> poll_options,
            @Field("poll[expires_in]") Integer poll_expire_in,
            @Field("poll[multiple]") Boolean poll_multiple,
            @Field("poll[hide_totals]") Boolean poll_hide_totals,
            @Field("in_reply_to_id") String in_reply_to_id,
            @Field("sensitive") Boolean sensitive,
            @Field("spoiler_text") String spoiler_text,
            @Field("visibility") String visibility,
            @Field("scheduled_at") String scheduled_at,
            @Field("language") String language
    );

    //Get a specific status
    @GET("statuses/{id}")
    Call<Status> getStatus(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Delete a specific status
    @DELETE("statuses/{id}")
    Call<Status> deleteStatus(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get parent and child statuses
    @GET("statuses/{id}/context")
    Call<Context> getContext(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get reblogged by
    @GET("statuses/{id}/reblogged_by")
    Call<List<Account>> getRebloggedBy(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //Get favourited  by
    @GET("statuses/{id}/favourited_by")
    Call<List<Account>> getFavourited(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //Add status to favourites
    @POST("statuses/{id}/favourite")
    Call<Status> favourites(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Remove status from favourites
    @POST("statuses/{id}/unfavourite")
    Call<Status> unFavourite(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Reblog a status
    @FormUrlEncoded
    @POST("statuses/{id}/reblog")
    Call<Status> reblog(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("visibility") String visibility
    );

    //Unreblog a status
    @POST("statuses/{id}/unreblog")
    Call<Status> unReblog(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Bookmark a status
    @POST("statuses/{id}/bookmark")
    Call<Status> bookmark(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Unbookmark a status
    @POST("statuses/{id}/unbookmark")
    Call<Status> unBookmark(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Mute a conversation
    @POST("statuses/{id}/mute")
    Call<Status> muteConversation(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //UnMute a conversation
    @POST("statuses/{id}/unmute")
    Call<Status> unMuteConversation(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Pin a status
    @POST("statuses/{id}/pin")
    Call<Status> pin(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //UNPin a status
    @POST("statuses/{id}/unpin")
    Call<Status> unPin(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get reblogged by
    @GET("statuses/{id}/card")
    Call<Card> getCard(
            @Header("Authorization") String token,
            @Path("id") String id
    );


    //Get a Media
    @GET("media/{id}")
    Call<Attachment> getMedia(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Upload a Media
    @Multipart
    @POST("media")
    Call<Attachment> postMedia(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file,
            @Part MultipartBody.Part thumbnail,
            @Part("description") RequestBody description,
            @Part("focus") RequestBody focus
    );

    //Edit a Media
    @Multipart
    @PUT("media/{id}")
    Call<Attachment> updateMedia(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Part MultipartBody.Part file,
            @Part MultipartBody.Part thumbnail,
            @Part("description") String description,
            @Part("focus") String focus
    );

    //Get a Poll
    @GET("polls/{id}")
    Call<Poll> getPoll(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Vote on a Poll
    @FormUrlEncoded
    @POST("polls/{id}/votes")
    Call<Poll> votePoll(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("choices[]") int[] choices
    );

    //Get scheduled statuses
    @GET("scheduled_statuses")
    Call<List<ScheduledStatus>> getScheduledStatuses(
            @Header("Authorization") String token,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("limit") int limit
    );

    //Get scheduled status
    @GET("scheduled_statuses/{id}")
    Call<ScheduledStatus> getScheduledStatus(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Schedule a status
    @FormUrlEncoded
    @PUT("scheduled_statuses/{id}")
    Call<ScheduledStatus> updateScheduleStatus(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("scheduled_at") Date scheduled_at
    );

    //Delete a scheduled status
    @DELETE("scheduled_statuses/{id}")
    Call<Void> deleteScheduledStatus(
            @Header("Authorization") String token,
            @Path("id") String id
    );
}
