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

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.helper.HelperInstance;


public class PlaylistsVM extends AndroidViewModel {
    private MutableLiveData<APIResponse> apiResponseMutableLiveData;
    private MutableLiveData<List<VideoPlaylistData.VideoPlaylistExport>> videoPlaylistExportMutableLiveData;

    public PlaylistsVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<APIResponse> manage(action apiAction, Playlist playlist, String videoId) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        managePlaylists(apiAction, playlist, videoId);
        return apiResponseMutableLiveData;
    }


    public LiveData<APIResponse> videoExists(List<String> videoIds) {
        apiResponseMutableLiveData = new MutableLiveData<>();
        checkVideosExist(videoIds);
        return apiResponseMutableLiveData;
    }

    private void checkVideosExist(List<String> videoIds) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            APIResponse apiResponse = new RetrofitPeertubeAPI(_mContext).getVideosExist(videoIds);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(apiResponse);
            mainHandler.post(myRunnable);
        }).start();
    }


    private void managePlaylists(action apiAction, Playlist playlist, String videoId) {
        Context _mContext = getApplication().getApplicationContext();
        new Thread(() -> {
            try {
                String token = HelperInstance.getToken();
                BaseAccount account = new Account(_mContext).getAccountByToken(token);
                int statusCode = -1;
                APIResponse apiResponse;
                if (account == null) {
                    statusCode = 403;
                    apiResponse = new APIResponse();
                    apiResponse.setPlaylists(new ArrayList<>());
                } else {
                    apiResponse = new RetrofitPeertubeAPI(_mContext).playlistAction(apiAction, playlist != null ? playlist.getId() : null, videoId, account.peertube_account.getAcct(), null);
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                if (apiResponse != null) {
                    apiResponse.setStatusCode(statusCode);
                }
                APIResponse finalApiResponse = apiResponse;
                Runnable myRunnable = () -> apiResponseMutableLiveData.setValue(finalApiResponse);
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public enum action {
        GET_PLAYLISTS,
        GET_PLAYLIST_INFO,
        GET_LIST_VIDEOS,
        CREATE_PLAYLIST,
        UPDATE_PLAYLIST,
        DELETE_PLAYLIST,
        ADD_VIDEOS,
        DELETE_VIDEOS
    }
}
