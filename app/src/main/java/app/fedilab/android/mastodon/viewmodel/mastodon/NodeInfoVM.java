package app.fedilab.android.mastodon.viewmodel.mastodon;
/* Copyright 2021 Thomas Schneider
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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;

import app.fedilab.android.mastodon.client.NodeInfoService;
import app.fedilab.android.mastodon.client.entities.app.WellKnownNodeinfo;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NodeInfoVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();

    private MutableLiveData<WellKnownNodeinfo.NodeInfo> nodeInfoMutableLiveData;


    public NodeInfoVM(@NonNull Application application) {
        super(application);
    }

    private NodeInfoService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(NodeInfoService.class);
    }


    /**
     * Get nodeinfo
     *
     * @return LiveData<WellKnownNodeinfo.NodeInfo>
     */
    public LiveData<WellKnownNodeinfo.NodeInfo> getNodeInfo(String instance) {
        nodeInfoMutableLiveData = new MutableLiveData<>();
        if (instance != null) {
            NodeInfoService nodeInfoService;
            try {
                nodeInfoService = init(instance);
            } catch (Exception e) {
                nodeInfoMutableLiveData.setValue(null);
                return nodeInfoMutableLiveData;
            }
            new Thread(() -> {
                WellKnownNodeinfo.NodeInfo nodeInfo = null;

                Call<WellKnownNodeinfo> nodeInfoLinksCall = nodeInfoService.getWellKnownNodeinfoLinks();
                if (nodeInfoLinksCall != null) {
                    try {
                        Response<WellKnownNodeinfo> nodeInfoLinksResponse = nodeInfoLinksCall.execute();
                        if (nodeInfoLinksResponse.isSuccessful() && nodeInfoLinksResponse.body() != null) {
                            WellKnownNodeinfo wellKnownNodeinfo = nodeInfoLinksResponse.body();
                            Call<WellKnownNodeinfo.NodeInfo> wellKnownNodeinfoCall = nodeInfoService.getNodeinfo(wellKnownNodeinfo.links.get(0).href);
                            if (wellKnownNodeinfoCall != null) {
                                try {
                                    Response<WellKnownNodeinfo.NodeInfo> response = wellKnownNodeinfoCall.execute();
                                    if (response.isSuccessful() && response.body() != null) {
                                        nodeInfo = response.body();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                WellKnownNodeinfo.NodeInfo finalNodeInfo = nodeInfo;
                Runnable myRunnable = () -> nodeInfoMutableLiveData.setValue(finalNodeInfo);
                mainHandler.post(myRunnable);
            }).start();
        } else {
            nodeInfoMutableLiveData.setValue(null);
        }
        return nodeInfoMutableLiveData;
    }

}
