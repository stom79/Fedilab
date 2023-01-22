package app.fedilab.android.peertube.client.mastodon;
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


import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.Token;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface MastodonService {

    @FormUrlEncoded
    @POST("apps")
    Call<Oauth> getOauth(
            @Field("client_name") String client_name,
            @Field("redirect_uris") String redirect_uris,
            @Field("scopes") String scopes,
            @Field("website") String website);

    @FormUrlEncoded
    @POST("oauth/token")
    Call<Token> createToken(
            @Field("grant_type") String grant_type,
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("redirect_uri") String redirect_uri,
            @Field("code") String code);

    @GET("accounts/verify_credentials")
    Call<MastodonAccount.Account> verifyCredentials(@Header("Authorization") String credentials);

    @GET("search?type=statuses&resolve=true")
    Call<Results> searchMessage(
            @Header("Authorization") String credentials,
            @Query("q") String messageURL
    );

    @FormUrlEncoded
    @POST("statuses")
    Call<Status> postReply(
            @Header("Authorization") String credentials,
            @Field("in_reply_to_id") String inReplyToId,
            @Field("status") String content,
            @Field("visibility") String visibility
    );


    @POST("statuses/{id}/reblog")
    Call<Status> boost(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );

    @POST("statuses/{id}/unreblog")
    Call<Status> unBoost(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );


    @POST("statuses/{id}/favourite")
    Call<Status> favourite(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );

    @POST("statuses/{id}/unfavourite")
    Call<Status> unfavourite(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );


    @POST("statuses/{id}/bookmark")
    Call<Status> bookmark(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );

    @POST("statuses/{id}/unbookmark")
    Call<Status> unbookmark(
            @Header("Authorization") String credentials,
            @Path("id") String id
    );
}
