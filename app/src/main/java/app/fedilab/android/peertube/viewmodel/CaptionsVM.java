package app.fedilab.android.peertube.viewmodel;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;


public class CaptionsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public CaptionsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getCaptions(String instance, String videoId) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadCaptions(instance, videoId);
        return apiResponseMutableLiveData;
    }

    private void loadCaptions(String instance, String videoId) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getCaptions(videoId);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
