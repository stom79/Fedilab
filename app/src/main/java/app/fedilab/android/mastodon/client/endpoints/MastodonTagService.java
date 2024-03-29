package app.fedilab.android.mastodon.client.endpoints;
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


import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Tag;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MastodonTagService {

    //Get followed tags
    @GET("followed_tags")
    Call<List<Tag>> getFollowedTags(
            @Header("Authorization") String token
    );


    //Get followed tags
    @GET("tags/{name}")
    Call<Tag> getTag(
            @Header("Authorization") String token,
            @Path("name") String name
    );

    //Follow tag
    @POST("tags/{name}/follow")
    Call<Tag> follow(
            @Header("Authorization") String app_token,
            @Path("name") String name
    );

    //Unfollow tag
    @POST("tags/{name}/unfollow")
    Call<Tag> unfollow(
            @Header("Authorization") String app_token,
            @Path("name") String name
    );
}
