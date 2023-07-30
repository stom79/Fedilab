package app.fedilab.android.mastodon.viewmodel.mastodon;
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


import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonFiltersService;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FiltersVM extends AndroidViewModel {


    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();


    private MutableLiveData<Filter> filterMutableLiveData;
    private MutableLiveData<List<Filter>> filterListMutableLiveData;

    public FiltersVM(@NonNull Application application) {
        super(application);
    }

    private MastodonFiltersService initV2(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
                //    .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonFiltersService.class);
    }


    /**
     * View all filters created by the user
     *
     * @return {@link LiveData} containing a {@link List} of {@link Filter}s
     */
    public LiveData<List<Filter>> getFilters(@NonNull String instance, String token) {
        filterListMutableLiveData = new MutableLiveData<>();
        MastodonFiltersService mastodonFiltersService = initV2(instance);
        new Thread(() -> {
            List<Filter> filterList = null;
            Call<List<Filter>> getFiltersCall = mastodonFiltersService.getFilters(token);
            if (getFiltersCall != null) {
                try {
                    Response<List<Filter>> getFiltersResponse = getFiltersCall.execute();
                    if (getFiltersResponse.isSuccessful()) {
                        BaseMainActivity.filterFetched = true;
                        filterList = getFiltersResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<Filter> finalFilterList = filterList;
            Runnable myRunnable = () -> filterListMutableLiveData.setValue(finalFilterList);
            mainHandler.post(myRunnable);
        }).start();
        return filterListMutableLiveData;
    }

    /**
     * View a single filter
     *
     * @param id the id of the filter
     * @return {@link LiveData} containing a {@link Filter}
     */
    public LiveData<Filter> getFilter(@NonNull String instance, String token, @NonNull String id) {
        filterMutableLiveData = new MutableLiveData<>();
        MastodonFiltersService mastodonFiltersService = initV2(instance);
        new Thread(() -> {
            Filter filter = null;
            Call<Filter> getFilterCall = mastodonFiltersService.getFilter(token, id);
            if (getFilterCall != null) {
                try {
                    Response<Filter> getFiltersResponse = getFilterCall.execute();
                    if (getFiltersResponse.isSuccessful()) {
                        filter = getFiltersResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Filter finalFilter = filter;
            Runnable myRunnable = () -> filterMutableLiveData.setValue(finalFilter);
            mainHandler.post(myRunnable);
        }).start();
        return filterMutableLiveData;
    }

    /**
     * Create a filter
     *
     * @return {@link LiveData} containing a {@link Filter}
     */
    public LiveData<Filter> addFilter(@NonNull String instance, String token, @NonNull Filter.FilterParams filterParams) {
        filterMutableLiveData = new MutableLiveData<>();
        MastodonFiltersService mastodonFiltersService = initV2(instance);
        new Thread(() -> {
            Filter filter = null;
            Call<Filter> addFilterCall = mastodonFiltersService.addFilter(token, filterParams);
            if (addFilterCall != null) {
                try {
                    Response<Filter> addFiltersResponse = addFilterCall.execute();
                    if (addFiltersResponse.isSuccessful()) {
                        filter = addFiltersResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Filter finalFilter = filter;
            Runnable myRunnable = () -> filterMutableLiveData.setValue(finalFilter);
            mainHandler.post(myRunnable);
        }).start();
        return filterMutableLiveData;
    }

    /**
     * Update a filter
     *
     * @return {@link LiveData} containing a {@link Filter}
     */
    public LiveData<Filter> editFilter(@NonNull String instance, String token, @NonNull Filter.FilterParams filterParams) {
        filterMutableLiveData = new MutableLiveData<>();
        MastodonFiltersService mastodonFiltersService = initV2(instance);
        new Thread(() -> {
            Filter filter = null;
           /* List<String> keywordsId = new ArrayList<>();
            List<String> keywords = new ArrayList<>();
            List<Boolean> whole_words = new ArrayList<>();
            for(Filter.KeywordsAttributes attributes: keywordsAttributes) {
                keywordsId.add(attributes.id);
                keywords.add(attributes.keyword);
                whole_words.add(attributes.whole_word);
            }*/

            Call<Filter> editFilterCall = mastodonFiltersService.editFilter(token, filterParams.id, filterParams);
            if (editFilterCall != null) {
                try {
                    Response<Filter> editFiltersResponse = editFilterCall.execute();
                    if (editFiltersResponse.isSuccessful()) {
                        filter = editFiltersResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Filter finalFilter = filter;
            Runnable myRunnable = () -> filterMutableLiveData.setValue(finalFilter);
            mainHandler.post(myRunnable);
        }).start();
        return filterMutableLiveData;
    }

    /**
     * Remove a filter
     *
     * @param id ID of the filter
     */
    public void removeFilter(@NonNull String instance, String token, @NonNull String id) {
        MastodonFiltersService mastodonAccountsService = initV2(instance);
        new Thread(() -> {
            Call<Void> removeFilterCall = mastodonAccountsService.removeFilter(token, id);
            if (removeFilterCall != null) {
                try {
                    removeFilterCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * Remove a filter
     *
     * @param id ID of the filter
     */
    public void removeKeyword(@NonNull String instance, String token, @NonNull String id) {
        MastodonFiltersService mastodonAccountsService = initV2(instance);
        new Thread(() -> {
            Call<Void> removeFilterCall = mastodonAccountsService.removeKeywordFilter(token, id);
            if (removeFilterCall != null) {
                try {
                    removeFilterCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
