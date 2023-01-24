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

import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.Error;


public class NotificationsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public NotificationsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getNotifications(BaseAccount account, String max_id) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadNotifications(account, max_id);
        return apiResponseMutableLiveData;
    }

    private void loadNotifications(BaseAccount account, String max_id) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api;
                APIResponse apiResponse;
                if (account == null) {
                    api = new RetrofitPeertubeAPI(_mContext);
                    apiResponse = api.getNotifications(max_id, null);
                } else {
                    if (_mContext == null) {
                        apiResponse = new APIResponse();
                        apiResponse.setError(new Error());
                    }
                    api = new RetrofitPeertubeAPI(_mContext, account.instance, account.token);
                    apiResponse = api.getNotifications(null, max_id);
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                APIResponse finalApiResponse = apiResponse;
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(finalApiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
