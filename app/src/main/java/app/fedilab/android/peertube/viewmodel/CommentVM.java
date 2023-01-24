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


public class CommentVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public CommentVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getThread(String instance, String videoId, String max_Id) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getThreadComments(instance, videoId, max_Id);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getRepliesComment(String instance, String videoId, String commentId) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getThreadRepliesComments(instance, videoId, commentId);
        return apiResponseMutableLiveData;
    }


    private void getThreadComments(String instance, String videoId, String max_id) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getComments(action.GET_THREAD, videoId, null, max_id);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void getThreadRepliesComments(String instance, String videoId, String commentId) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getComments(action.GET_REPLIES, videoId, commentId, null);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public enum action {
        GET_THREAD,
        GET_REPLIES
    }
}
