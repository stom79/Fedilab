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
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.mastodon.client.entities.app.InstanceSocial;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.interfaces.InstancesSocialService;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InstanceSocialVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private final InstancesSocialService instancesSocialService;
    private MutableLiveData<InstanceSocial> instanceSocialMutableLiveData;


    public InstanceSocialVM(@NonNull Application application) {
        super(application);
        instancesSocialService = init();
    }

    private InstancesSocialService init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://instances.social/api/1.0/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(InstancesSocialService.class);
    }


    /**
     * Get instance social instances
     *
     * @return MutableLiveData<List < InstanceSocial>>
     */
    public MutableLiveData<InstanceSocial> getInstances(String search) {
        instanceSocialMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            try {
                Call<InstanceSocial> instanceSocialCall = instancesSocialService.getInstances("Bearer " + Helper.INSTANCE_SOCIAL_KEY, search);
                Response<InstanceSocial> response = instanceSocialCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    InstanceSocial instanceSocial = response.body();
                    InstanceSocial filtered = new InstanceSocial();
                    filtered.instances = new ArrayList<>();
                    if (instanceSocial != null && instanceSocial.instances != null) {
                        filtered.instances.addAll(instanceSocial.instances);
                    }
                    Runnable myRunnable = () -> instanceSocialMutableLiveData.setValue(filtered);
                    mainHandler.post(myRunnable);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return instanceSocialMutableLiveData;
    }

}
