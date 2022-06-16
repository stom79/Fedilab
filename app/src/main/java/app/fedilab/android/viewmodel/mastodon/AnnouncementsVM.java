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

import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.client.endpoints.MastodonAnnouncementsService;
import app.fedilab.android.client.entities.api.Announcement;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.SpannableHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AnnouncementsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();
    private MutableLiveData<Announcement> announcementMutableLiveData;
    private MutableLiveData<List<Announcement>> announcementListMutableLiveData;

    public AnnouncementsVM(@NonNull Application application) {
        super(application);
    }

    private MastodonAnnouncementsService init(@NonNull String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAnnouncementsService.class);
    }

    /**
     * See all currently active announcements set by admins.
     *
     * @param instance      Instance domain of the active account
     * @param token         Access token of the active account
     * @param withDismissed If true, response will include announcements dismissed by the user. Defaults to false.
     * @return {@link LiveData} containing a {@link List} of {@link Announcement}s
     */
    public LiveData<List<Announcement>> getAnnouncements(@NonNull String instance, String token, Boolean withDismissed) {
        MastodonAnnouncementsService mastodonAnnouncementsService = init(instance);
        announcementListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            List<Announcement> announcementList = null;
            Call<List<Announcement>> getAnnouncementsCall = mastodonAnnouncementsService.getAnnouncements(token, withDismissed);
            if (getAnnouncementsCall != null) {
                try {
                    Response<List<Announcement>> getAnnouncementsResponse = getAnnouncementsCall.execute();
                    if (getAnnouncementsResponse.isSuccessful()) {
                        announcementList = getAnnouncementsResponse.body();
                        SpannableHelper.convertAnnouncement(getApplication(), announcementList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<Announcement> finalAnnouncementList = announcementList;
            Runnable myRunnable = () -> announcementListMutableLiveData.setValue(finalAnnouncementList);
            mainHandler.post(myRunnable);
        }).start();
        return announcementListMutableLiveData;
    }

    /**
     * Allows a user to mark the announcement as read.
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       Local ID of an announcement
     */
    public void dismiss(@NonNull String instance, String token, @NonNull String id) {
        MastodonAnnouncementsService mastodonAnnouncementsService = init(instance);
        new Thread(() -> {
            Call<Void> dismissCall = mastodonAnnouncementsService.dismiss(token, id);
            if (dismissCall != null) {
                try {
                    dismissCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        MastodonAnnouncementsService mastodonAnnouncementsService = init(instance);
        new Thread(() -> {
            Call<Void> addReactionCall = mastodonAnnouncementsService.addReaction(token, id, name);
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
        MastodonAnnouncementsService mastodonAnnouncementsService = init(instance);
        new Thread(() -> {
            Call<Void> removeReactionCall = mastodonAnnouncementsService.removeReaction(token, id, name);
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
