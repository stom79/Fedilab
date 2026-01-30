package app.fedilab.android.misskey.viewmodel;
/* Copyright 2026 Thomas Schneider
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

import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.misskey.client.endpoints.MisskeyOauthService;
import app.fedilab.android.misskey.client.entities.MisskeyToken;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisskeyOauthVM extends AndroidViewModel {

    private final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<MisskeyToken> tokenMutableLiveData;

    public MisskeyOauthVM(@NonNull Application application) {
        super(application);
    }

    private MisskeyOauthService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) + "/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MisskeyOauthService.class);
    }

    public LiveData<MisskeyToken> checkMiAuth(@NonNull String instance, @NonNull String session) {
        tokenMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyOauthService service = init(instance);
            MisskeyToken token = null;
            try {
                Response<MisskeyToken> response = service.checkMiAuth(session).execute();
                if (response.isSuccessful()) {
                    token = response.body();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MisskeyToken finalToken = token;
            mainHandler.post(() -> tokenMutableLiveData.setValue(finalToken));
        }).start();
        return tokenMutableLiveData;
    }
}
