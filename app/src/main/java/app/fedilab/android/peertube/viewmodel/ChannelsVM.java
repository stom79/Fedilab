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

import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.helper.HelperInstance;


public class ChannelsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public ChannelsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> get(String instance, RetrofitPeertubeAPI.DataType type, String element) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getChannels(instance, type, element);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> get(RetrofitPeertubeAPI.DataType type, String element) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getChannels(null, type, element);
        return apiResponseMutableLiveData;
    }

    private void getChannels(String instance, RetrofitPeertubeAPI.DataType type, String element) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            String finalElement = element;
            try {
                if (type == RetrofitPeertubeAPI.DataType.MY_CHANNELS) {
                    String token = HelperInstance.getToken();
                    BaseAccount baseAccount = new Account(_mContext).getAccountByToken(token);
                    AccountData.PeertubeAccount account = baseAccount.peertube_account;
                    finalElement = account.getUsername() + "@" + account.getHost();
                }
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getChannelData(type, finalElement);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public enum action {
        CREATE_CHANNEL,
        UPDATE_CHANNEL
    }

}
