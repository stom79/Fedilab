package app.fedilab.android.mastodon.services;
/* Copyright 2022 Thomas Schneider
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
import android.content.Intent;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unifiedpush.android.connector.MessagingReceiver;

import app.fedilab.android.mastodon.helper.NotificationsHelper;
import app.fedilab.android.mastodon.helper.PushNotifications;


public class CustomReceiver extends MessagingReceiver {


    public CustomReceiver() {
        super();
    }


    @Override
    public void onMessage(@NotNull Context context, @NotNull byte[] message, @NotNull String slug) {
        // Called when a new message is received. The message contains the full POST body of the push message
        new Thread(() -> {
            try {
                /*Notification notification = ECDHFedilab.decryptNotification(context, slug, message);
                Log.v(Helper.TAG,"notification: " + notification);
                if(notification != null) {
                    Log.v(Helper.TAG,"id: " + notification.id);
                }
                */
                NotificationsHelper.task(context, slug);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }).start();
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onNewEndpoint(@Nullable Context context, @NotNull String endpoint, @NotNull String slug) {
        if (context != null) {
            PushNotifications
                    .registerPushNotifications(context, endpoint, slug);
        }
    }


    @Override
    public void onRegistrationFailed(@Nullable Context context, @NotNull String s) {
    }

    @Override
    public void onUnregistered(@Nullable Context context, @NotNull String s) {
    }
}

