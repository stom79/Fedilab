package app.fedilab.android.peertube.viewmodel;
/* Copyright 2023 Thomas Schneider
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
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;


public class SearchVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public SearchVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getVideos(String max_id, String query) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadVideos(max_id, query);
        return apiResponseMutableLiveData;
    }


    public LiveData<APIResponse> getChannels(String max_id, String query) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadChannels(max_id, query);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> searchNextVideos(List<String> tags) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadNextVideos(tags);
        return apiResponseMutableLiveData;
    }


    private void loadChannels(String max_id, String query) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = api.searchChannels(query, max_id);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadVideos(String max_id, String query) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = api.searchPeertube(query, max_id);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void loadNextVideos(List<String> tags) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = api.searchNextVideos(tags);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
