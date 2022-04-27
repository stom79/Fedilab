package com.github.stom79.mytransl.client;
/* Copyright 2017 Thomas Schneider
 *
 * This file is a part of MyTransL
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * MyTransL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MyTransL; if not,
 * see <http://www.gnu.org/licenses>. */

import android.os.Build;
import android.text.Html;
import android.text.SpannableString;

/**
 * Created by @stom79 on 28/11/2017.
 * Manage custom Exception
 * Changed 10/01/2021
 */

@SuppressWarnings({"unused", "RedundantSuppression"})
public class HttpsConnectionException extends Exception {

    private final int statusCode;
    private final String message;

    public HttpsConnectionException(int statusCode, String message) {
        this.statusCode = statusCode;
        SpannableString spannableString;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannableString = new SpannableString(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        } else {
            spannableString = new SpannableString(Html.fromHtml(message));
        }
        this.message = spannableString.toString();
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
