package app.fedilab.android.mastodon.viewmodel.mastodon;
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.endpoints.MastodonSearchService;
import app.fedilab.android.mastodon.client.entities.api.Results;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<Results> resultsMutableLiveData;

    public SearchVM(@NonNull Application application) {
        super(application);
    }

    private MastodonSearchService init(String instance) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonSearchService.class);
    }


    /**
     * Search for content in accounts, statuses and hashtags with API v2
     *
     * @param instance           Instance domain of the active account
     * @param token              Access token of the active account
     * @param q                  String - search words
     * @param account_id         String - If provided, statuses returned will be authored only by this account
     * @param type               String - Enum(accounts, hashtags, statuses)
     * @param exclude_unreviewed boolean - Filter out unreviewed tags? Defaults to false. Use true when trying to find trending tags.
     * @param resolve            boolean - Attempt WebFinger lookup. Defaults to false.
     * @param following          boolean - Only include accounts that the user is following. Defaults to false.
     * @param offset             int - Offset in search results. Used for pagination. Defaults to 0.
     * @param max_id             String - Return results older than this id
     * @param min_id             String - Return results immediately newer than this id
     * @param limit              int - Maximum number of results to load, per type. Defaults to 20. Max 40.
     * @return {@link LiveData} containing an {@link Results}
     */
    public LiveData<Results> search(@NonNull String instance,
                                    String token,
                                    @NonNull String q,
                                    String account_id,
                                    String type,
                                    Boolean exclude_unreviewed,
                                    Boolean resolve,
                                    Boolean following,
                                    Integer offset,
                                    String max_id,
                                    String min_id,
                                    Integer limit) {
        MastodonSearchService mastodonSearchService = init(instance);
        resultsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            int finalLimit = 40;
            if (limit != null && limit < 40) {
                finalLimit = limit;
            }
            Call<Results> resultsCall = mastodonSearchService.search(
                    token, q, account_id, type, exclude_unreviewed,
                    resolve, following, offset, max_id, min_id, finalLimit);
            Results results = null;

            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {
                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.statuses == null) {
                                results.statuses = new ArrayList<>();
                            }
                            if (results.accounts == null) {
                                results.accounts = new ArrayList<>();
                            }
                            if (results.hashtags == null) {
                                results.hashtags = new ArrayList<>();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> resultsMutableLiveData.setValue(finalResults);
            mainHandler.post(myRunnable);
        }).start();
        return resultsMutableLiveData;
    }

    public LiveData<Results> searchCache(@NonNull String instance, String userId, @NonNull String q) {
        resultsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Results results = new Results();
            try {
                results.statuses = new ArrayList<>();
                List<Status> statuses = new StatusCache(getApplication()).searchStatus(instance, userId, q);
                if (statuses != null) {
                    results.statuses.addAll(statuses);
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> resultsMutableLiveData.setValue(results);
            mainHandler.post(myRunnable);
        }).start();
        return resultsMutableLiveData;
    }

}
