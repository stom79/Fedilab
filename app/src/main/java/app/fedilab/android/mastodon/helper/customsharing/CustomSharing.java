package app.fedilab.android.mastodon.helper.customsharing;
/* Copyright 2019 Curtis Rock
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

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.mastodon.client.entities.api.Error;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by Curtis on 13/02/2019.
 * Manage custom sharing of status metadata to remote content aggregator
 */

public class CustomSharing {

    private final Context context;
    private CustomSharingResponse customSharingResponse;
    private Error customSharingError;

    public CustomSharing(Context context) {
        this.context = context;
        if (context == null) {
            customSharingError = new Error();
            return;
        }
        customSharingResponse = new CustomSharingResponse();
        customSharingError = null;
    }

    /***
     * pass status metadata to remote content aggregator *synchronously*
     * @return CustomSharingResponse
     */
    public CustomSharingResponse customShare(String encodedCustomSharingURL) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context.getApplicationContext()))
                .readTimeout(10, TimeUnit.SECONDS).build();
        Request request = new Request.Builder()
                .url(encodedCustomSharingURL)
                .build();
        String HTTPResponse = "";
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                if (response.body() != null) {
                    HTTPResponse = response.body().string();
                }
            } else {
                setError(response.code(), new Exception(response.message()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            setError(500, e);
        }
        customSharingResponse.setResponse(HTTPResponse);
        return customSharingResponse;
    }

    public Error getError() {
        return customSharingError;
    }


    /**
     * Set the error message
     *
     * @param statusCode int code
     * @param error      Throwable error
     */
    private void setError(int statusCode, Throwable error) {
        customSharingError = new Error();
        customSharingError.code = statusCode;
        String message = statusCode + " - " + error.getMessage();
        try {
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(error.getMessage()));
            String errorM = jsonObject.get("error").toString();
            message = "Error " + statusCode + " : " + errorM;
        } catch (JSONException e) {
            if (error.getMessage().split("\\.").length > 0) {
                String errorM = error.getMessage().split("\\.")[0];
                message = "Error " + statusCode + " : " + errorM;
            }
        }
        customSharingError.error = message;
        customSharingResponse.setError(customSharingError);
    }
}
