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


import app.fedilab.android.client.entities.api.App;
import app.fedilab.android.client.entities.api.Token;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MastodonAppsService {


    /*
     *  OAUTH - TOKEN
     */
    //Create app
    @FormUrlEncoded
    @POST("apps")
    Call<App> createApp(
            @Field("client_name") String client_name,
            @Field("redirect_uris") String redirect_uris,
            @Field("scopes") String scopes,
            @Field("website") String website);

    @GET("apps/verify_credentials")
    Call<App> verifyCredentials(
            @Header("Authorization") String app_token);


    //Create token
    @FormUrlEncoded
    @POST("oauth/token")
    Call<Token> createToken(
            @Field("grant_type") String grant_type,
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("redirect_uri") String redirect_uri,
            @Field("scope") String scope,
            @Field("code") String code);

    //Revoke token
    @FormUrlEncoded
    @POST("oauth/revoke")
    Call<Void> revokeToken(
            @Field("client_id") String client_id,
            @Field("client_secret") String client_secret,
            @Field("token") String token);

}
