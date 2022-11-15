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

import app.fedilab.android.client.endpoints.MastodonTagService;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Tag;
import app.fedilab.android.client.entities.api.Tags;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class TagVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();

    private MutableLiveData<Tags> tagsMutableLiveData;
    private MutableLiveData<Tag> tagMutableLiveData;


    /**
     * Constructor - String token can be for the app or the account
     *
     * @param application Application
     */
    public TagVM(@NonNull Application application) {
        super(application);
    }

    private MastodonTagService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTagService.class);
    }


    /**
     * Return followed tags with pagination
     *
     * @return {@link LiveData} containing a {@link Tags}. Note: Not to be confused with {@link Tag}
     */
    public LiveData<Tags> followedTags(@NonNull String instance, String token) {
        tagsMutableLiveData = new MutableLiveData<>();
        MastodonTagService mastodonTagService = init(instance);
        new Thread(() -> {
            List<Tag> tagList = null;
            Pagination pagination = null;
            Call<List<Tag>> followedTagsListCall = mastodonTagService.getFollowedTags(token);
            if (followedTagsListCall != null) {
                try {
                    Response<List<Tag>> tagsResponse = followedTagsListCall.execute();
                    if (tagsResponse.isSuccessful()) {
                        tagList = tagsResponse.body();
                        pagination = MastodonHelper.getPagination(tagsResponse.headers());

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Tags tags = new Tags();
            tags.pagination = pagination;
            tags.tags = tagList;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> tagsMutableLiveData.setValue(tags);
            mainHandler.post(myRunnable);
        }).start();
        return tagsMutableLiveData;
    }

    /**
     * Return tag
     *
     * @return {@link LiveData} containing a {@link Tag}
     */
    public LiveData<Tag> getTag(@NonNull String instance, String token,
                                String tagName) {
        tagMutableLiveData = new MutableLiveData<>();
        MastodonTagService mastodonTagService = init(instance);
        new Thread(() -> {
            Tag tag = null;
            Call<Tag> tagCall = mastodonTagService.getTag(token, tagName);
            if (tagCall != null) {
                try {
                    Response<Tag> tagResponse = tagCall.execute();
                    if (tagResponse.isSuccessful()) {
                        tag = tagResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Tag finalTag = tag;
            Runnable myRunnable = () -> tagMutableLiveData.setValue(finalTag);
            mainHandler.post(myRunnable);
        }).start();
        return tagMutableLiveData;
    }


    /**
     * Follow a tag
     *
     * @return {@link LiveData} containing an {@link Tag}
     */
    public LiveData<Tag> follow(@NonNull String instance, String token, String name) {
        MastodonTagService mastodonTagService = init(instance);
        tagMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Tag tag = null;
            Call<Tag> tagCall = mastodonTagService.follow(token, name);
            if (tagCall != null) {
                try {
                    Response<Tag> appResponse = tagCall.execute();
                    if (appResponse.isSuccessful()) {
                        tag = appResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Tag finalTag = tag;
            Runnable myRunnable = () -> tagMutableLiveData.setValue(finalTag);
            mainHandler.post(myRunnable);
        }).start();
        return tagMutableLiveData;
    }

    /**
     * Unfollow a tag
     *
     * @return {@link LiveData} containing an {@link Tag}
     */
    public LiveData<Tag> unfollow(@NonNull String instance, String token, String name) {
        MastodonTagService mastodonTagService = init(instance);
        tagMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Tag tag = null;
            Call<Tag> tagCall = mastodonTagService.unfollow(token, name);
            if (tagCall != null) {
                try {
                    Response<Tag> appResponse = tagCall.execute();
                    if (appResponse.isSuccessful()) {
                        tag = appResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Tag finalTag = tag;
            Runnable myRunnable = () -> tagMutableLiveData.setValue(finalTag);
            mainHandler.post(myRunnable);
        }).start();
        return tagMutableLiveData;
    }

}
