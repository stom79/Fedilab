package app.fedilab.android.ui.fragment.settings;
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

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;

public class FragmentTimelinesSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_timelines);
        createPref();
    }

    private void createPref() {

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.pref_timelines);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        ListPreference SET_LOAD_MEDIA_TYPE = findPreference(getString(R.string.SET_LOAD_MEDIA_TYPE));
        if (SET_LOAD_MEDIA_TYPE != null) {
            SET_LOAD_MEDIA_TYPE.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_TRANSLATOR = findPreference(getString(R.string.SET_TRANSLATOR));
        if (SET_TRANSLATOR != null) {
            SET_TRANSLATOR.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_TRANSLATOR_VERSION = findPreference(getString(R.string.SET_TRANSLATOR_VERSION));
        if (SET_TRANSLATOR_VERSION != null) {
            SET_TRANSLATOR_VERSION.getContext().setTheme(Helper.dialogStyle());
        }
        EditTextPreference SET_TRANSLATOR_API_KEY = findPreference(getString(R.string.SET_TRANSLATOR_API_KEY));
        if (SET_TRANSLATOR != null && SET_TRANSLATOR.getValue().equals("FEDILAB")) {
            if (SET_TRANSLATOR_API_KEY != null) {
                preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_API_KEY");
            }
            if (SET_TRANSLATOR_VERSION != null) {
                preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_VERSION");
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.apply();
            if (key.compareToIgnoreCase(getString(R.string.SET_TRANSLATOR)) == 0) {
                createPref();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


}
