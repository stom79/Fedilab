package app.fedilab.android.mastodon.ui.fragment.settings;
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
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;

public class FragmentTimelinesSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_timelines);
        createPref();
    }

    private void createPref() {

        getPreferenceScreen().removeAll();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        addPreferencesFromResource(R.xml.pref_timelines);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        ListPreference SET_TRANSLATOR = findPreference(getString(R.string.SET_TRANSLATOR));

        ListPreference SET_TRANSLATOR_VERSION = findPreference(getString(R.string.SET_TRANSLATOR_VERSION));

        EditTextPreference SET_TRANSLATOR_API_KEY = findPreference(getString(R.string.SET_TRANSLATOR_API_KEY));
        EditTextPreference SET_TRANSLATOR_DOMAIN = findPreference(getString(R.string.SET_TRANSLATOR_DOMAIN));
        if (SET_TRANSLATOR != null && !SET_TRANSLATOR.getValue().equals("DEEPL")) {
            if (SET_TRANSLATOR_API_KEY != null) {
                preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_API_KEY");
            }
            if (SET_TRANSLATOR_VERSION != null) {
                preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_VERSION");
            }
        }
        if (SET_TRANSLATOR != null && !SET_TRANSLATOR.getValue().equals("LINGVA")) {
            if (SET_TRANSLATOR_DOMAIN != null) {
                preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_DOMAIN");
            }
        }
        SwitchPreferenceCompat SET_DISPLAY_BOOKMARK = findPreference(getString(R.string.SET_DISPLAY_BOOKMARK));
        if (SET_DISPLAY_BOOKMARK != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_BOOKMARK) + MainActivity.currentUserID + MainActivity.currentInstance, true);
            SET_DISPLAY_BOOKMARK.setChecked(checked);
        }
        SwitchPreferenceCompat SET_DISPLAY_TRANSLATE = findPreference(getString(R.string.SET_DISPLAY_TRANSLATE));
        if (SET_DISPLAY_TRANSLATE != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_TRANSLATE) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_DISPLAY_TRANSLATE.setChecked(checked);
        }

        SwitchPreferenceCompat SET_PIXELFED_PRESENTATION = findPreference(getString(R.string.SET_PIXELFED_PRESENTATION));
        if (SET_PIXELFED_PRESENTATION != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_PIXELFED_PRESENTATION) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_PIXELFED_PRESENTATION.setChecked(checked);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (key.compareToIgnoreCase(getString(R.string.SET_TRANSLATOR)) == 0) {
                createPref();
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_DISPLAY_BOOKMARK)) == 0) {
                SwitchPreferenceCompat SET_DISPLAY_BOOKMARK = findPreference(getString(R.string.SET_DISPLAY_BOOKMARK));
                if (SET_DISPLAY_BOOKMARK != null) {
                    editor.putBoolean(getString(R.string.SET_DISPLAY_BOOKMARK) + MainActivity.currentUserID + MainActivity.currentInstance, SET_DISPLAY_BOOKMARK.isChecked());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_DISPLAY_TRANSLATE)) == 0) {
                SwitchPreferenceCompat SET_DISPLAY_TRANSLATE = findPreference(getString(R.string.SET_DISPLAY_TRANSLATE));
                if (SET_DISPLAY_TRANSLATE != null) {
                    editor.putBoolean(getString(R.string.SET_DISPLAY_TRANSLATE) + MainActivity.currentUserID + MainActivity.currentInstance, SET_DISPLAY_TRANSLATE.isChecked());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_PIXELFED_PRESENTATION)) == 0) {
                SwitchPreferenceCompat SET_PIXELFED_PRESENTATION = findPreference(getString(R.string.SET_PIXELFED_PRESENTATION));
                if (SET_PIXELFED_PRESENTATION != null) {
                    editor.putBoolean(getString(R.string.SET_PIXELFED_PRESENTATION) + MainActivity.currentUserID + MainActivity.currentInstance, SET_PIXELFED_PRESENTATION.isChecked());
                }
            }
            editor.apply();
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
