package app.fedilab.android.client.endpoints;
/* Copyright 2022 Thomas Schneider
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

import app.fedilab.android.client.entities.api.Filter;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;


public interface MastodonFiltersService {


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
            @Field("title") String title,
            @Field("expires_at") Date expires_at,
            @Field("filter_action") String filter_action,
            @Field("context[]") List<String> context,
            @Field("keywords_attributes") List<Filter.KeywordsAttributes> keywordsAttributes
    );

    //Edit a filter
    @FormUrlEncoded
    @PUT("filters/{id}")
    Call<Filter> editFilter(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Field("title") String title,
            @Field("expires_at") Date expires_at,
            @Field("filter_action") String filter_action,
            @Field("context[]") List<String> context,
            @Field("keywords_attributes") List<Filter.KeywordsAttributes> keywordsAttributes
    );

    //Remove a filter
    @DELETE("filters/{id}")
    Call<Void> removeFilter(
            @Header("Authorization") String token,
            @Path("id") String id
    );


    //Get a filter with its id
    @GET("filters/{id}/keywords")
    Call<List<Filter.KeywordsAttributes>> getKeywordFilter(
            @Header("Authorization") String token,
            @Path("id") String id);

    //Add a keyword to a filter
    @FormUrlEncoded
    @POST("filters/{filter_id}/keywords/{id}")
    Call<Filter.KeywordsAttributes> addKeywordFilter(
            @Header("Authorization") String token,
            @Path("filter_id") String filter_id,
            @Path("id") String id,
            @Field("keyword") Filter.Keyword keyword
    );

    //Edit a keyword for a filter
    @FormUrlEncoded
    @PUT("filters/{filter_id}/keywords/{id}")
    Call<Filter.KeywordsAttributes> editKeywordFilter(
            @Header("Authorization") String token,
            @Path("filter_id") String filter_id,
            @Path("id") String id,
            @Field("keyword") Filter.Keyword keyword
    );

    //Remove a keyword for a filter
    @DELETE("filters/keywords/{id}")
    Call<Void> removeKeywordFilter(
            @Header("Authorization") String token,
            @Path("id") String id
    );


}
