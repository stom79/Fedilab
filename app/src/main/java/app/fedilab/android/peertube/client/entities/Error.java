package app.fedilab.android.peertube.client.entities;
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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import app.fedilab.android.peertube.R;
import es.dmoral.toasty.Toasty;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class Error extends Throwable {

    private String error = null;
    private int statusCode = -1;

    public static void displayError(Context context, Error error) {

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> {
            String message;
            if (error.getError() != null && error.getError().trim().length() > 0)
                message = error.getError();
            else
                message = context.getString(R.string.toast_error);
            Toasty.error(context, message, Toast.LENGTH_LONG).show();
        };
        mainHandler.post(myRunnable);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
