package app.fedilab.android.peertube.helper;
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

import static app.fedilab.android.BaseMainActivity.currentToken;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE_PEERTUBE_BROWSING;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.typeOfConnection;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import app.fedilab.android.peertube.activities.PeertubeMainActivity;


public class HelperInstance {


    /**
     * Returns the instance of the authenticated user
     *
     * @param context Context
     * @return String domain instance
     */
    public static String getLiveInstance(Context context) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (typeOfConnection == PeertubeMainActivity.TypeOfConnection.REMOTE_ACCOUNT) {
            return sharedpreferences.getString(PREF_USER_INSTANCE_PEERTUBE_BROWSING, null);
        } else {
            return sharedpreferences.getString(PREF_USER_INSTANCE, null);
        }
    }

    public static String getToken() {
        if (typeOfConnection == PeertubeMainActivity.TypeOfConnection.REMOTE_ACCOUNT) {
            return null;
        } else {
            return currentToken;
        }
    }

}
