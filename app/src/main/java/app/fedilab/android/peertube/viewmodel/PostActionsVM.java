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

import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.Report;


public class PostActionsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public PostActionsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> post(RetrofitPeertubeAPI.ActionType apiAction, String id, String element) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        makeAction(apiAction, id, element);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> report(Report report) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        sendReport(report);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> comment(RetrofitPeertubeAPI.ActionType type, String videoId, String commentId, String content) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        postComment(type, videoId, commentId, content);
        return apiResponseMutableLiveData;
    }

    private void makeAction(RetrofitPeertubeAPI.ActionType apiAction, String id, String element) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI peertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = peertubeAPI.post(apiAction, id, element);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendReport(Report report) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI peertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = peertubeAPI.report(report);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void postComment(RetrofitPeertubeAPI.ActionType type, String videoId, String commentId, String content) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI peertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = peertubeAPI.commentAction(type, videoId, commentId, content);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
