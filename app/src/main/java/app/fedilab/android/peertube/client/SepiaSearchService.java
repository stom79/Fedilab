package app.fedilab.android.peertube.client;
/* Copyright 2023 Thomas Schneider
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

import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.VideoData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

interface SepiaSearchService {

    @GET("search/videos")
    Call<VideoData> getVideos(
            @Query("start") String maxId,
            @Query("count") String count,
            @Query("search") String search,
            @Query("durationMin") int durationMin,
            @Query("durationMax") int durationMax,
            @Query("startDate") String startDate,
            @Query("boostLanguages") List<String> languageOneOf,
            @Query("categoryOneOf") List<Integer> categoryOneOf,
            @Query("licenceOneOf") List<Integer> licenceOneOf,
            @Query("tagsOneOf") List<String> tagsOneOf,
            @Query("tagsAllOf") List<String> tagsAllOf,
            @Query("nsfw") boolean nsfw,
            @Query("sort") String sort


    );

    @GET("search/channels")
    Call<ChannelData> getChannels(
            @Query("search") String search,
            @Query("start") String maxId,
            @Query("count") String count
    );

}
