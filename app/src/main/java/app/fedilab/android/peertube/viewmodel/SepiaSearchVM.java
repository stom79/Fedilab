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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import app.fedilab.android.peertube.client.RetrofitSepiaSearchAPI;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.SepiaSearch;


public class SepiaSearchVM extends AndroidViewModel {
    private MutableLiveData<VideoData> apiResponseMutableLiveData;

    public SepiaSearchVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<VideoData> sepiaSearch(SepiaSearch sepiaSearch) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getVideos(sepiaSearch);
        return apiResponseMutableLiveData;
    }

    private void getVideos(SepiaSearch sepiaSearch) {
        new Thread(() -> {
            try {
                VideoData videoData = new RetrofitSepiaSearchAPI().getVideos(sepiaSearch);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(videoData);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}