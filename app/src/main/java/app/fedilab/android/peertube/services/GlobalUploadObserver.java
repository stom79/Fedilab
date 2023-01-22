package app.fedilab.android.peertube.services;
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import net.gotev.uploadservice.data.UploadInfo;
import net.gotev.uploadservice.network.ServerResponse;
import net.gotev.uploadservice.observer.request.RequestObserverDelegate;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import app.fedilab.android.peertube.helper.Helper;

public class GlobalUploadObserver implements RequestObserverDelegate {

    @Override
    public void onCompleted(@NotNull Context context, @NotNull UploadInfo uploadInfo) {
    }

    @Override
    public void onCompletedWhileNotObserving() {
    }

    @Override
    public void onError(@NotNull Context context, @NotNull UploadInfo uploadInfo, @NotNull Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onProgress(@NotNull Context context, @NotNull UploadInfo uploadInfo) {
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onSuccess(@NotNull Context context, @NotNull UploadInfo uploadInfo, @NotNull ServerResponse serverResponse) {
        try {
            JSONObject response = new JSONObject(serverResponse.getBodyString());

            if (response.has("video")) {
                String videoUuid = response.getJSONObject("video").getString("uuid");
                SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Helper.VIDEO_ID, videoUuid);
                editor.commit();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
