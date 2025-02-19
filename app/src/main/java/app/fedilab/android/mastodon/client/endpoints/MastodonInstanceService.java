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


import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Activity;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.Instance;
import app.fedilab.android.mastodon.client.entities.api.InstanceV2;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface MastodonInstanceService {

    @GET("instance")
    Call<Instance> instance();

    @GET("instance")
    Call<InstanceV2> instanceV2();

    @GET("instance/peers")
    Call<List<String>> connectedInstance();

    @GET("instance/activity")
    Call<List<Activity>> weeklyActivity();

    @GET("trends")
    Call<List<Tag>> trends();

    @GET("directory")
    Call<List<Account>> directory(
            @Query("offset") int offset,
            @Query("limit") int limit,
            @Query("order") String order,
            @Query("local") boolean local
    );

    @GET("custom_emojis")
    Call<List<Emoji>> customEmoji();
}
