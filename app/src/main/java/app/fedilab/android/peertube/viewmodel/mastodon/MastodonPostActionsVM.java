package app.fedilab.android.peertube.viewmodel.mastodon;
/* Copyright 2021 Thomas Schneider
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

import app.fedilab.android.peertube.client.mastodon.RetrofitMastodonAPI;
import app.fedilab.android.peertube.client.mastodon.Status;


public class MastodonPostActionsVM extends AndroidViewModel {
    private MutableLiveData<Status> statusMutableLiveData;

    public MastodonPostActionsVM(@NonNull Application application) {
        super(application);
    }


    public LiveData<Status> post(RetrofitMastodonAPI.actionType type, Status status) {
        statusMutableLiveData = new MutableLiveData<>();
        postAction(type, status);
        return statusMutableLiveData;
    }

    public LiveData<Status> comment(String url, String content) {
        statusMutableLiveData = new MutableLiveData<>();
        postComment(url, content);
        return statusMutableLiveData;
    }

    public LiveData<Status> searchRemoteStatus(String url) {
        statusMutableLiveData = new MutableLiveData<>();
        search(url);
        return statusMutableLiveData;
    }

    private void search(String videoURL) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitMastodonAPI mastodonAPI = new RetrofitMastodonAPI(_mContext);
                Status status = null;
                try {
                    status = mastodonAPI.search(videoURL);
                } catch (Error error) {
                    error.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Status finalStatus = status;
                Runnable myRunnable = () -> statusMutableLiveData.setValue(finalStatus);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void postComment(String videoURL, String content) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitMastodonAPI mastodonAPI = new RetrofitMastodonAPI(_mContext);
                Status status = null;
                try {
                    status = mastodonAPI.commentAction(videoURL, content);
                } catch (Error error) {
                    error.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Status finalStatus = status;
                Runnable myRunnable = () -> statusMutableLiveData.setValue(finalStatus);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void postAction(RetrofitMastodonAPI.actionType type, Status status) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitMastodonAPI mastodonAPI = new RetrofitMastodonAPI(_mContext);
                Status statusReply = mastodonAPI.postAction(type, status);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> statusMutableLiveData.setValue(statusReply);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
