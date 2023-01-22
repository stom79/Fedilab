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


public class AccountsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public AccountsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getAccounts(RetrofitPeertubeAPI.DataType dataType, String element) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadAccounts(dataType, element);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getAccount(String acct) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadAccount(acct);
        return apiResponseMutableLiveData;
    }

    private void loadAccounts(RetrofitPeertubeAPI.DataType dataType, String element) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = new APIResponse();
                if (dataType == RetrofitPeertubeAPI.DataType.SUBSCRIBER) {
                    apiResponse = retrofitPeertubeAPI.getSubscribtions(element);
                } else if (dataType == RetrofitPeertubeAPI.DataType.MUTED) {
                    apiResponse = retrofitPeertubeAPI.getMuted(element);
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

    private void loadAccount(String acct) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (acct.split("@").length > 1) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, acct.split("@")[1], null);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getAccount(acct);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
