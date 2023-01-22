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
import app.fedilab.android.peertube.client.entities.InstanceParams;


public class InstancesVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public InstancesVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getInstances(InstanceParams instanceParams) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        get(instanceParams);
        return apiResponseMutableLiveData;
    }

    private void get(InstanceParams instanceParams) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                APIResponse apiResponse = new RetrofitPeertubeAPI(_mContext, "instances.joinpeertube.org", null).getInstances(instanceParams);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
