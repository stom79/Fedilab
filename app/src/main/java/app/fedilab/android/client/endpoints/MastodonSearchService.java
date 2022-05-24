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


import app.fedilab.android.client.entities.api.Results;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface MastodonSearchService {
    //API V2
    @GET("search")
    Call<Results> search(
            @Header("Authorization") String token,
            @Query("q") String q,
            @Query("account_id") String account_id,
            @Query("type") String type,
            @Query("exclude_unreviewed") Boolean exclude_unreviewed,
            @Query("resolve") Boolean resolve,
            @Query("following") Boolean following,
            @Query("offset") Integer offset,
            @Query("max_id") String max_id,
            @Query("min_id") String min_id,
            @Query("limit") Integer limit
    );
}
