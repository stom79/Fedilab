package app.fedilab.android.viewmodel.mastodon;
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.endpoints.JoinMastodonService;
import app.fedilab.android.client.entities.api.JoinMastodonInstance;
import app.fedilab.android.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JoinInstancesVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private final String base_url;
    private final JoinMastodonService joinMastodonService;
    private MutableLiveData<List<JoinMastodonInstance>> joiListMutableLiveData;


    /**
     * Constructor - String token can be for the join instances (registration helper)
     *
     * @param application Application
     */
    public JoinInstancesVM(@NonNull Application application) {
        super(application);
        base_url = "https://api.joinmastodon.org/";
        joinMastodonService = init();

    }

    private JoinMastodonService init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(base_url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(JoinMastodonService.class);
    }

    /**
     * Find instances through joinmastodon api
     *
     * @param category String
     * @return {@link LiveData} containing a List of {@link JoinMastodonInstance}
     */
    public LiveData<List<JoinMastodonInstance>> getInstances(String category) {
        joiListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            List<JoinMastodonInstance> joinMastodonInstanceList = null;
            Call<List<JoinMastodonInstance>> listCall = joinMastodonService.getInstances(category);
            if (listCall != null) {
                try {
                    Response<List<JoinMastodonInstance>> listResponse = listCall.execute();
                    if (listResponse.isSuccessful()) {
                        joinMastodonInstanceList = listResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<JoinMastodonInstance> finalJoinMastodonInstanceList = joinMastodonInstanceList;
            Runnable myRunnable = () -> joiListMutableLiveData.setValue(finalJoinMastodonInstanceList);
            mainHandler.post(myRunnable);
        }).start();
        return joiListMutableLiveData;
    }

}
