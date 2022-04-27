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
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.mastodon.MastodonAppsService;
import app.fedilab.android.client.mastodon.entities.Token;
import app.fedilab.android.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OauthVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private MutableLiveData<Token> tokenMutableLiveData;
    private MutableLiveData<Void> voidMutableLiveData;


    /**
     * Constructor - String token can be for the app or the account
     *
     * @param application Application
     */
    public OauthVM(@NonNull Application application) {
        super(application);
    }

    private MastodonAppsService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAppsService.class);
    }


    /**
     * Obtain a token
     *
     * @param instance Instance domain of the active account
     * @return access token {@link LiveData} containing an {@link Token}
     */
    public LiveData<Token> createToken(@NonNull String instance,
                                       String grant_type,
                                       String client_id,
                                       String client_secret,
                                       String redirect_uri,
                                       String scope,
                                       String code) {
        MastodonAppsService mastodonAppsService = init(instance);
        tokenMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Token token = null;
            Call<Token> tokenCall = mastodonAppsService.createToken(grant_type, client_id, client_secret, redirect_uri, scope, code);
            if (tokenCall != null) {
                try {
                    Response<Token> tokenResponse = tokenCall.execute();
                    if (tokenResponse.isSuccessful()) {
                        token = tokenResponse.body();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Token finalToken = token;
            Runnable myRunnable = () -> tokenMutableLiveData.setValue(finalToken);
            mainHandler.post(myRunnable);
        }).start();
        return tokenMutableLiveData;
    }

    /**
     * Delete a token
     *
     * @param instance Domain of the instance
     * @param token    Access token to revoke
     * @return access token {@link LiveData} containing an {@link Void}
     */
    public LiveData<Void> revokeToken(@NonNull String instance,
                                      String token,
                                      String client_id,
                                      String client_secret) {
        MastodonAppsService mastodonAppsService = init(instance);
        voidMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Void> voidCall = mastodonAppsService.revokeToken(client_id, client_secret, token);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> voidMutableLiveData.setValue(null);
            mainHandler.post(myRunnable);
        }).start();
        return voidMutableLiveData;
    }
}
