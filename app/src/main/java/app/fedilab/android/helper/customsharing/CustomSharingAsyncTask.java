package app.fedilab.android.helper.customsharing;
/* Copyright 2017 Thomas Schneider
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
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;


/**
 * Created by Curtis on 13/02/2019.
 * Custom share status metadata to remote content aggregator
 */

public class CustomSharingAsyncTask {

    private final String encodedCustomSharingURL;
    private final OnCustomSharingInterface listener;
    private final WeakReference<Context> contextReference;
    private CustomSharingResponse customSharingResponse;

    public CustomSharingAsyncTask(Context context, String encodedCustomSharingURL, OnCustomSharingInterface onCustomSharingInterface) {
        this.contextReference = new WeakReference<>(context);
        this.encodedCustomSharingURL = encodedCustomSharingURL;
        this.listener = onCustomSharingInterface;
        doInBackground();
    }

    protected void doInBackground() {
        new Thread(() -> {
            customSharingResponse = new CustomSharing(this.contextReference.get()).customShare(encodedCustomSharingURL);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> listener.onCustomSharing(customSharingResponse);
            mainHandler.post(myRunnable);
        }).start();
    }

}