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


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.SepiaSearch;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitSepiaSearchAPI {


    private final String finalUrl;


    public RetrofitSepiaSearchAPI() {
        finalUrl = "https://search.joinpeertube.org/api/v1/";
    }

    private SepiaSearchService init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(finalUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(SepiaSearchService.class);
    }

    /**
     * Return videos for a sepia search
     *
     * @param sepiaSearch SepiaSearch
     * @return VideoData
     */
    public VideoData getVideos(SepiaSearch sepiaSearch) {
        SepiaSearchService sepiaSearchService = init();
        SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        String startDate = null;
        if (sepiaSearch.getStartDate() != null) {
            startDate = fmtOut.format(sepiaSearch.getStartDate());
        }
        Call<VideoData> videoDataCall = sepiaSearchService.getVideos(
                sepiaSearch.getStart(),
                sepiaSearch.getCount(),
                sepiaSearch.getSearch(),
                sepiaSearch.getDurationMin(),
                sepiaSearch.getDurationMax(),
                startDate,
                sepiaSearch.getBoostLanguages(),
                sepiaSearch.getCategoryOneOf(),
                sepiaSearch.getLicenceOneOf(),
                sepiaSearch.getTagsOneOf(),
                sepiaSearch.getTagsAllOf(),
                sepiaSearch.isNsfw(),
                sepiaSearch.getSort());

        try {
            Response<VideoData> response = videoDataCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
