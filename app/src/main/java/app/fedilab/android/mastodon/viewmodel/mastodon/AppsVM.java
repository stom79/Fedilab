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

import app.fedilab.android.mastodon.client.endpoints.MastodonAppsService;
import app.fedilab.android.mastodon.client.entities.api.App;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<App> appMutableLiveData;


    /**
     * Constructor - String token can be for the app or the account
     *
     * @param application Application
     */
    public AppsVM(@NonNull Application application) {
        super(application);
    }

    private MastodonAppsService init(String instance) throws IllegalArgumentException {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAppsService.class);
    }

    /**
     * Register client applications that can be used to obtain OAuth tokens.
     *
     * @param client_name   String
     * @param redirect_uris String
     * @param scopes        String
     * @param website       String
     * @return {@link LiveData} containing an {@link App}
     */
    public LiveData<App> createApp(@NonNull String instance, String client_name,
                                   String redirect_uris,
                                   String scopes,
                                   String website) {
        appMutableLiveData = new MutableLiveData<>();
        MastodonAppsService mastodonAppsService;
        try {
            mastodonAppsService = init(instance);
        } catch (IllegalArgumentException e) {
            appMutableLiveData.setValue(null);
            return appMutableLiveData;
        }
        new Thread(() -> {
            App app = null;
            Call<App> appCall = mastodonAppsService.createApp(client_name, redirect_uris, scopes, website);
            if (appCall != null) {
                try {
                    Response<App> appResponse = appCall.execute();
                    if (appResponse.isSuccessful()) {
                        app = appResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            App finalApp = app;
            Runnable myRunnable = () -> appMutableLiveData.setValue(finalApp);
            mainHandler.post(myRunnable);
        }).start();
        return appMutableLiveData;
    }

    /**
     * Confirm that the app's OAuth2 credentials work.
     *
     * @return {@link LiveData} containing an {@link App}
     */
    public LiveData<App> verifyCredentials(@NonNull String instance, String token) {
        MastodonAppsService mastodonAppsService = init(instance);
        appMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            App app = null;
            Call<App> appCall = mastodonAppsService.verifyCredentials(token);
            if (appCall != null) {
                try {
                    Response<App> appResponse = appCall.execute();
                    if (appResponse.isSuccessful()) {
                        app = appResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            App finalApp = app;
            Runnable myRunnable = () -> appMutableLiveData.setValue(finalApp);
            mainHandler.post(myRunnable);
        }).start();
        return appMutableLiveData;
    }


}
