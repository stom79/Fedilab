package app.fedilab.android.mastodon.viewmodel.pleroma;
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

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.mastodon.client.endpoints.PleromaAPI;
import app.fedilab.android.mastodon.client.entities.api.Announcement;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActionsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private MutableLiveData<Announcement> announcementMutableLiveData;
    private MutableLiveData<List<Announcement>> announcementListMutableLiveData;

    public ActionsVM(@NonNull Application application) {
        super(application);
    }

    private PleromaAPI init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(PleromaAPI.class);
    }

    /**
     * React to an announcement with an emoji.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       Local ID of an announcement
     * @param name     Unicode emoji, or shortcode of custom emoji
     */
    public void addReaction(@NonNull String instance, String token, @NonNull String id, @NonNull String name) {
        PleromaAPI pleromaAPI = init(instance);
        new Thread(() -> {
            Call<Void> addReactionCall = pleromaAPI.addReaction(token, id, name);
            if (addReactionCall != null) {
                try {
                    addReactionCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Undo a react emoji to an announcement.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       Local ID of an announcement
     * @param name     Unicode emoji, or shortcode of custom emoji
     */
    public void removeReaction(@NonNull String instance, String token, @NonNull String id, @NonNull String name) {
        PleromaAPI pleromaAPI = init(instance);
        new Thread(() -> {
            Call<Void> removeReactionCall = pleromaAPI.removeReaction(token, id, name);
            if (removeReactionCall != null) {
                try {
                    removeReactionCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
