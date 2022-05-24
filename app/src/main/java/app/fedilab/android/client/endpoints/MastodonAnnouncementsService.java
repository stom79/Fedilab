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

import app.fedilab.android.client.entities.api.Announcement;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MastodonAnnouncementsService {

    @GET("/announcements")
    Call<List<Announcement>> getAnnouncements(
            @Header("Authorization") String token,
            @Query("with_dismissed") boolean with_dismissed
    );

    @FormUrlEncoded
    @POST("/announcements/{id}/dismiss")
    Call<Void> dismiss(
            @Header("Authorization") String app_token,
            @Path("id") String id
    );

    @FormUrlEncoded
    @PUT("/announcements/{id}/reactions/{name}")
    Call<Void> addReaction(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Path("name") String name
    );

    @DELETE("/announcements/{id}/reactions/{name}")
    Call<Void> removeReaction(
            @Header("Authorization") String app_token,
            @Path("id") String id,
            @Path("name") String name
    );

}
