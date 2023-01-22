package app.fedilab.android.peertube.helper;
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
import android.content.SharedPreferences;

import app.fedilab.android.peertube.BuildConfig;


public class HelperInstance {


    /**
     * Returns the instance of the authenticated user
     *
     * @param context Context
     * @return String domain instance
     */
    public static String getLiveInstance(Context context) {
        final SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        if (BuildConfig.FLAVOR.compareTo("fdroid_full") == 0 || BuildConfig.FLAVOR.compareTo("google_full") == 0) {
            return sharedpreferences.getString(Helper.PREF_INSTANCE, null);
        } else {
            return sharedpreferences.getString(Helper.PREF_INSTANCE, "tube-institutionnel.apps.education.fr");
        }
    }

}
