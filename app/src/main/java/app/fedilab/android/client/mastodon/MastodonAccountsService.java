package app.fedilab.android.client.mastodon;
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

import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.client.mastodon.entities.FeaturedTag;
import app.fedilab.android.client.mastodon.entities.Filter;
import app.fedilab.android.client.mastodon.entities.IdentityProof;
import app.fedilab.android.client.mastodon.entities.MastodonList;
import app.fedilab.android.client.mastodon.entities.Preferences;
import app.fedilab.android.client.mastodon.entities.RelationShip;
import app.fedilab.android.client.mastodon.entities.Report;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.client.mastodon.entities.Tag;
import app.fedilab.android.client.mastodon.entities.Token;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonAccountsService {


    /*
     *  Accounts
     */
    //Register account
    @FormUrlEncoded
    @POST("accounts")
    Call<Token> registerAccount(
            @Header("Authorization") String app_token,
            @Field("username") String username,
            @Field("email") String email,
            @Field("password") String password,
            @Field("agreement") boolean agreement,
            @Field("locale") String locale,
            @Field("reason") String reason);

    //Info about the connected account
    @GET("accounts/verify_credentials")
    Call<Account> verify_credentials(
            @Header("Authorization") String token);

    @Multipart
    @PATCH("accounts/update_credentials")
    Call<Account> update_media(
            @Header("Authorization") String token,
            @Part MultipartBody.Part avatar,
            @Part MultipartBody.Part header

    );


    @Headers({"Accept: application/json"})
    @PATCH("accounts/update_credentials")
    Call<Account> update_credentials(
            @Header("Authorization") String token, @Body Account.AccountParams accountParams
    );

    @FormUrlEncoded
    @PATCH("accounts/update_credentials")
    Call<Account> update_credentials(
            @Header("Authorization") String token,
            @Field("discoverable") Boolean discoverable,
            @Field("bot") Boolean bot,
            @Field("display_name") String display_name,
            @Field("note") String note,
            @Field("locked") Boolean locked,
            @Field("source[privacy]") String privacy,
            @Field("source[sensitive]") Boolean sensitive,
            @Field("source[language]") String language,
            @Field("fields_attributes") List<app.fedilab.android.client.mastodon.entities.Field.FieldParams> fields
    );

    //Get Account
    @GET("accounts/{id}")
    Call<Account> getAccount(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get Account statuses
    @GET("accounts/{id}/statuses")
    Call<List<Status>> getAccountStatuses(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id,
            @Query("exclude_replies") Boolean exclude_replies,
            @Query("exclude_reblogs") Boolean exclude_reblogs,
            @Query("only_media") Boolean only_media,
            @Query("pinned") Boolean pinned,
            @Query("limit") int limit
    );

    //Get Account followers
    @GET("accounts/{id}/followers")
    Call<List<Account>> getAccountFollowers(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Get Account following
    @GET("accounts/{id}/following")
    Call<List<Account>> getAccountFollowing(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Get Account featured tags
    @GET("accounts/{id}/featured_tags")
    Call<List<FeaturedTag>> getAccountFeaturedTags(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Lists containing this account
    @GET("accounts/{id}/lists")
    Call<List<MastodonList>> getListContainingAccount(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Get Identity proofs
    @GET("accounts/{id}/identity_proofs")
    Call<List<IdentityProof>> getIdentityProofs(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Follow account
    @FormUrlEncoded
    @POST("accounts/{id}/follow")
    Call<RelationShip> follow(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Field("reblogs") boolean reblogs,
            @Field("notify") boolean notify
    );

    //Follow account
    @FormUrlEncoded
    @POST("accounts/{id}/note")
    Call<RelationShip> note(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Field("comment") boolean comment
    );

    //Unfollow account
    @POST("accounts/{id}/unfollow")
    Call<RelationShip> unfollow(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //Block account
    @POST("accounts/{id}/block")
    Call<RelationShip> block(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //Unblock account
    @POST("accounts/{id}/unblock")
    Call<RelationShip> unblock(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //Mute account
    @FormUrlEncoded
    @POST("accounts/{id}/mute")
    Call<RelationShip> mute(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Field("notifications") Boolean notifications,
            @Field("duration") Integer duration
    );

    //Unmute account
    @POST("accounts/{id}/unmute")
    Call<RelationShip> unmute(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //Feature on profile
    @POST("accounts/{id}/pin")
    Call<RelationShip> endorse(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //Unfeature account
    @POST("accounts/{id}/unpin")
    Call<RelationShip> unendorse(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    //User note
    @FormUrlEncoded
    @POST("accounts/{id}/note")
    Call<RelationShip> note(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Field("comment") String comment
    );

    //Get relationships
    @GET("accounts/relationships")
    Call<List<RelationShip>> getRelationships(
            @Header("Authorization") String token,
            @Query("id[]") List<String> ids
    );

    //Get search
    @GET("accounts/search")
    Call<List<Account>> searchAccounts(
            @Header("Authorization") String token,
            @Query("q") String q,
            @Query("limit") int limit,
            @Query("resolve") boolean resolve,
            @Query("following") boolean following
    );


    //Bookmarks
    @GET("bookmarks")
    Call<List<Status>> getBookmarks(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id,
            @Query("min_id") String min_id
    );

    //favourites
    @GET("favourites")
    Call<List<Status>> getFavourites(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("min_id") String min_id,
            @Query("max_id") String max_id
    );

    //muted users
    @GET("mutes")
    Call<List<Account>> getMutes(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Blocked users
    @GET("blocks")
    Call<List<Account>> getBlocks(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Get blocked domains
    @GET("domain_blocks")
    Call<List<String>> getDomainBlocks(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Add a blocked domains
    @FormUrlEncoded
    @POST("domain_blocks")
    Call<Void> addDomainBlock(
            @Header("Authorization") String token,
            @Field("domain") String domain
    );

    //Remove a blocked domains
    @DELETE("domain_blocks")
    Call<Void> removeDomainBlocks(
            @Header("Authorization") String token,
            @Field("domain") String domain
    );

    //Get filters
    @GET("filters")
    Call<List<Filter>> getFilters(
            @Header("Authorization") String token);

    //Get a filter with its id
    @GET("filters/{id}")
    Call<Filter> getFilter(
            @Header("Authorization") String token,
            @Path("id") String id);

    //Add a filter
    @FormUrlEncoded
    @POST("filters")
    Call<Filter> addFilter(
            @Header("Authorization") String token,
            @Field("phrase") String phrase,
            @Field("context[]") List<String> context,
            @Field("irreversible") boolean irreversible,
            @Field("whole_word") boolean whole_word,
            @Field("expires_in") long expires_in
    );

    //Edit a filter
    @FormUrlEncoded
    @PUT("filters/{id}")
    Call<Filter> editFilter(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("phrase") String phrase,
            @Field("context[]") List<String> context,
            @Field("irreversible") boolean irreversible,
            @Field("whole_word") boolean whole_word,
            @Field("expires_in") long expires_in
    );

    //Remove a filter
    @DELETE("filters/{id}")
    Call<Void> removeFilter(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Post a report
    @Headers({"Accept: application/json"})
    @POST("reports")
    Call<Report> report(
            @Header("Authorization") String token, @Body Report.ReportParams params
    );

    //Get follow request
    @GET("follow_requests")
    Call<List<Account>> getFollowRequests(
            @Header("Authorization") String token,
            @Path("limit") String limit);

    //Accept follow request
    @POST("follow_requests/{id}/authorize")
    Call<RelationShip> acceptFollow(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Reject follow request
    @POST("follow_requests/{id}/reject")
    Call<RelationShip> rejectFollow(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Accounts that the user is currently featuring on their profile.
    @GET("endorsements")
    Call<List<Account>> getEndorsements(
            @Header("Authorization") String token,
            @Query("limit") String limit,
            @Query("max_id") String max_id,
            @Query("since_id") String since_id
    );

    //Feature tags
    @GET("featured_tags")
    Call<List<FeaturedTag>> getFeaturedTags(
            @Header("Authorization") String token
    );

    //Add a feature tags
    @FormUrlEncoded
    @POST("featured_tags")
    Call<FeaturedTag> addFeaturedTag(
            @Header("Authorization") String token,
            @Field("name") String name
    );

    //Remove a feature tags
    @DELETE("featured_tags/{id}")
    Call<Void> removeFeaturedTag(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    //Feature tags suggestions
    @GET("featured_tags/suggestions")
    Call<List<Tag>> getFeaturedTagsSuggestions(
            @Header("Authorization") String token
    );

    //Get user preferences
    @GET("preferences")
    Call<Preferences> getPreferences(
            @Header("Authorization") String token
    );

    //Get user suggestions
    @GET("suggestions")
    Call<List<Account>> getSuggestions(
            @Header("Authorization") String token,
            @Query("limit") String limit
    );

    //Remove a user suggestion
    @DELETE("suggestions/{account_id}")
    Call<Void> removeSuggestion(
            @Header("Authorization") String token,
            @Path("account_id") String account_id
    );
}
