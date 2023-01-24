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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.VideoParams;


public class MyVideoVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public MyVideoVM(@NonNull Application application) {
        super(application);
    }


    public LiveData<APIResponse> updateVideo(String videoId, VideoParams videoParams, Uri thumbnail, Uri previewfile) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        update(videoId, videoParams, thumbnail, previewfile);
        return apiResponseMutableLiveData;
    }

    private void update(String videoId, VideoParams videoParams, Uri thumbnail, Uri previewfile) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI peertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = peertubeAPI.updateVideo(videoId, videoParams, thumbnail, previewfile);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.postValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
