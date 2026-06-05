package app.fedilab.android.mastodon.client.endpoints;
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

import app.fedilab.android.mastodon.client.entities.api.Collection;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonCollectionsService {

    @FormUrlEncoded
    @POST("collections")
    Call<Collection.WrappedCollection> createCollection(
            @Header("Authorization") String token,
            @Field("name") String name,
            @Field("description") String description,
            @Field("language") String language,
            @Field("tag_name") String tag_name,
            @Field("sensitive") Boolean sensitive,
            @Field("discoverable") Boolean discoverable
    );

    @GET("collections/{id}")
    Call<Collection.CollectionWithAccounts> getCollection(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @GET("accounts/{account_id}/collections")
    Call<Collection.CollectionList> getAccountCollections(
            @Header("Authorization") String token,
            @Path("account_id") String account_id,
            @Query("limit") Integer limit,
            @Query("offset") Integer offset
    );

    @GET("accounts/{account_id}/in_collections")
    Call<Collection.CollectionList> getAccountInCollections(
            @Header("Authorization") String token,
            @Path("account_id") String account_id,
            @Query("limit") Integer limit,
            @Query("offset") Integer offset
    );

    @FormUrlEncoded
    @PATCH("collections/{id}")
    Call<Collection.WrappedCollection> updateCollection(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("name") String name,
            @Field("description") String description,
            @Field("language") String language,
            @Field("tag_name") String tag_name,
            @Field("sensitive") Boolean sensitive,
            @Field("discoverable") Boolean discoverable
    );

    @DELETE("collections/{id}")
    Call<Void> deleteCollection(
            @Header("Authorization") String token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @POST("collections/{collection_id}/items")
    Call<Collection.WrappedCollectionItem> addAccountToCollection(
            @Header("Authorization") String token,
            @Path("collection_id") String collection_id,
            @Field("account_id") String account_id
    );

    @DELETE("collections/{collection_id}/items/{id}")
    Call<Void> removeAccountFromCollection(
            @Header("Authorization") String token,
            @Path("collection_id") String collection_id,
            @Path("id") String id
    );

    @POST("collections/{collection_id}/items/{id}/revoke")
    Call<Void> revokeCollectionItem(
            @Header("Authorization") String token,
            @Path("collection_id") String collection_id,
            @Path("id") String id
    );
}
