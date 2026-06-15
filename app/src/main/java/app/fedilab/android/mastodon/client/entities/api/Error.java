package app.fedilab.android.mastodon.client.entities.api;
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

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import app.fedilab.android.R;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class Error {
    @SerializedName("code")
    public int code;
    @SerializedName("error")
    public String error;
    @SerializedName("error_description")
    public String error_description;

    public static String getErrorMessage(Context context, Response<?> response) {
        if (context == null) {
            return null;
        }
        if (response == null) {
            return context.getString(R.string.error_generic, 0);
        }
        int httpCode = response.code();
        String bodyStr = null;
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                bodyStr = errorBody.string();
            }
        } catch (Exception ignored) {
        }
        if (bodyStr != null && !bodyStr.trim().isEmpty()) {
            try {
                Error apiError = new Gson().fromJson(bodyStr, Error.class);
                if (apiError != null) {
                    if (apiError.error_description != null && !apiError.error_description.trim().isEmpty()) {
                        return apiError.error_description;
                    }
                    if (apiError.error != null && !apiError.error.trim().isEmpty()) {
                        return apiError.error;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return getGenericMessage(context, httpCode);
    }

    private static String getGenericMessage(Context context, int httpCode) {
        return switch (httpCode) {
            case 404 -> context.getString(R.string.error_feature_not_supported);
            case 413 -> context.getString(R.string.error_file_too_large);
            case 422 -> context.getString(R.string.error_file_rejected);
            case 429 -> context.getString(R.string.error_too_many_requests);
            case 500, 502, 503 -> context.getString(R.string.error_instance_unavailable);
            default -> context.getString(R.string.error_generic, httpCode);
        };
    }
}
