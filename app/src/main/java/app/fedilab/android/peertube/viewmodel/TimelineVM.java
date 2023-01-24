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

import static app.fedilab.android.peertube.viewmodel.PlaylistsVM.action.GET_LIST_VIDEOS;

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
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;


public class TimelineVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;

    public TimelineVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> getVideos(TimelineType action, String max_id, ChannelData.Channel forChannel, AccountData.PeertubeAccount forAccount) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadVideos(action, max_id, forChannel, forAccount);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getOverviewVideos(String page) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadOverviewVideos(page);
        return apiResponseMutableLiveData;
    }


    public LiveData<APIResponse> getVideoHistory(String max_id, String startDate, String endDate) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadHistory(max_id, startDate, endDate);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> deleterHistory() {
        apiResponseMutableLiveData = new MutableLiveData<>();
        deleteHistory();
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getVideo(String instance, String videoId, boolean isMyVideo) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getSingle(instance, videoId, isMyVideo);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> loadVideosInPlaylist(String playlistId, String maxId) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadVideosInPlayList(playlistId, maxId);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getMyVideo(String instance, String videoId) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        getSingle(instance, videoId, true);
        return apiResponseMutableLiveData;
    }

    public LiveData<APIResponse> getVideosInChannel(String instance, String channelId, String max_id) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        loadVideosForChannel(instance, channelId, max_id);
        return apiResponseMutableLiveData;
    }

    private void loadVideosForChannel(String instance, String channelId, String max_id) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getVideosForChannel(channelId, max_id);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getSingle(String instance, String videoId, boolean myVideo) {
        Context _mContext = getApplication().getApplicationContext();
        boolean canUseToken = instance == null || instance.compareTo(HelperInstance.getLiveInstance(_mContext)) == 0;
        boolean finalCanUseToken = canUseToken;
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                if (instance == null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, instance, null);
                }
                APIResponse apiResponse = retrofitPeertubeAPI.getVideos(videoId, myVideo, finalCanUseToken);
                if (Helper.isLoggedIn() && instance == null) {
                    if (apiResponse.getPeertubes() != null && apiResponse.getPeertubes().size() > 0 && apiResponse.getPeertubes().get(0) != null) {
                        APIResponse response = new RetrofitPeertubeAPI(_mContext).getRating(videoId);
                        if (response != null)
                            apiResponse.getPeertubes().get(0).setMyRating(response.getRating().getRating());
                    }
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadVideosInPlayList(String playlistId, String maxId) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = retrofitPeertubeAPI.playlistAction(GET_LIST_VIDEOS, playlistId, null, null, maxId);

                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void loadVideos(TimelineType timeline, String max_id, ChannelData.Channel forChannel, AccountData.PeertubeAccount forAccount) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI;
                String acct = null;
                if (forChannel != null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, forChannel.getHost(), null);
                    acct = forChannel.getAcct();
                } else if (forAccount != null) {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext, forAccount.getHost(), null);
                    acct = forAccount.getAcct();
                } else {
                    retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                }
                if (timeline == null)
                    return;
                APIResponse apiResponse;
                apiResponse = retrofitPeertubeAPI.getTL(timeline, max_id, acct);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadHistory(String max_id, String startDate, String endDate) {

        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = retrofitPeertubeAPI.getHistory(max_id, startDate, endDate);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteHistory() {

        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = retrofitPeertubeAPI.deleteHistory();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void loadOverviewVideos(String page) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(_mContext);
                APIResponse apiResponse = retrofitPeertubeAPI.getOverviewVideo(page);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public enum TimelineType {
        CHANNEL_VIDEOS,
        ACCOUNT_VIDEOS,
        SUBSCRIBTIONS,
        MY_VIDEOS,
        LOCAL,
        TRENDING,
        MOST_LIKED,
        HISTORY,
        RECENT,
        VIDEOS_IN_PLAYLIST,
        VIDEOS_IN_LOCAL_PLAYLIST,
        SEPIA_SEARCH
    }
}
