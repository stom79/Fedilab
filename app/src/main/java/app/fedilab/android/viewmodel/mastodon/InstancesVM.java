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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.client.endpoints.MastodonInstanceService;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.client.entities.api.EmojiInstance;
import app.fedilab.android.client.entities.api.Instance;
import app.fedilab.android.client.entities.api.InstanceInfo;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InstancesVM extends AndroidViewModel {


    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private MutableLiveData<EmojiInstance> emojiInstanceMutableLiveData;
    private MutableLiveData<InstanceInfo> instanceInfoMutableLiveData;

    public InstancesVM(@NonNull Application application) {
        super(application);
    }

    private MastodonInstanceService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonInstanceService.class);
    }

    public LiveData<EmojiInstance> getEmoji(@NonNull String instance) {
        MastodonInstanceService mastodonInstanceService = init(instance);
        emojiInstanceMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            EmojiInstance emojiInstance = new EmojiInstance();
            emojiInstance.instance = BaseMainActivity.currentInstance;
            Call<List<Emoji>> emojiCall = mastodonInstanceService.customEmoji();
            if (emojiCall != null) {
                try {
                    Response<List<Emoji>> emojiResponse = emojiCall.execute();
                    if (emojiResponse.isSuccessful()) {
                        emojiInstance.emojiList = emojiResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (emojiInstance.emojiList != null) {
                try {
                    new EmojiInstance(getApplication().getApplicationContext()).insertOrUpdate(emojiInstance);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> emojiInstanceMutableLiveData.setValue(emojiInstance);
            mainHandler.post(myRunnable);
        }).start();
        return emojiInstanceMutableLiveData;
    }


    public LiveData<InstanceInfo> getInstance(@NonNull String instance) {
        MastodonInstanceService mastodonInstanceService = init(instance);
        instanceInfoMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.instance = BaseMainActivity.currentInstance;
            Call<Instance> instanceCall = mastodonInstanceService.instance();
            if (instanceCall != null) {
                try {
                    Response<Instance> instanceResponse = instanceCall.execute();
                    if (instanceResponse.isSuccessful()) {
                        instanceInfo.info = instanceResponse.body();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (instanceInfo.info != null) {
                try {
                    new InstanceInfo(getApplication().getApplicationContext()).insertOrUpdate(instanceInfo);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            } else {
                instanceInfo.info = new Instance();
            }
            //Some values that we must initialize
            if (instanceInfo.info.configuration == null) {
                instanceInfo.info.configuration = new Instance.Configuration();
            }
            if (instanceInfo.info.configuration.pollsConf == null) {
                instanceInfo.info.configuration.pollsConf = new Instance.PollsConf();
            }
            if (instanceInfo.info.configuration.statusesConf == null) {
                instanceInfo.info.configuration.statusesConf = new Instance.StatusesConf();
            }
            if (instanceInfo.info.configuration.media_attachments == null) {
                instanceInfo.info.configuration.media_attachments = new Instance.MediaConf();
            }
            if (instanceInfo.info.rules == null) {
                instanceInfo.info.rules = new ArrayList<>();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> instanceInfoMutableLiveData.setValue(instanceInfo);
            mainHandler.post(myRunnable);
        }).start();
        return instanceInfoMutableLiveData;
    }
}
