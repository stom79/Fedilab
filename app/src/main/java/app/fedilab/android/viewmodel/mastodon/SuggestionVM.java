package app.fedilab.android.viewmodel.mastodon;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.endpoints.MastodonFiltersService;
import app.fedilab.android.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.client.entities.api.Filter;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.api.Statuses;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.TimelineHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SuggestionVM extends AndroidViewModel {


    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();


    private MutableLiveData<Filter> filterMutableLiveData;
    private MutableLiveData<List<Filter>> filterListMutableLiveData;

    public SuggestionVM(@NonNull Application application) {
        super(application);
    }

    private MastodonFiltersService initV2(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v2/")
                //    .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonFiltersService.class);
    }


    public LiveData<Suggestions> getSuggestions(String token, @NonNull String instance, String max_id, Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = initV2(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<Status>> publicTlCall = mastodonTimelinesService.getStatusTrends(token, max_id, limit);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<List<Status>> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        List<Status> statusList = publicTlResponse.body();
                        statuses.statuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), statusList, Timeline.TimeLineEnum.TREND_MESSAGE);
                        statuses.pagination = MastodonHelper.getOffSetPagination(publicTlResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }
}
