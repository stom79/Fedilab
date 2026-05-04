package app.fedilab.android.mastodon.ui.fragment.settings;
/* Copyright 2026 Thomas Schneider
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

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentMediaSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_media);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        ListPreference SET_LOAD_MEDIA_TYPE = findPreference(getString(R.string.SET_LOAD_MEDIA_TYPE));
        if (SET_LOAD_MEDIA_TYPE != null) {
            SET_LOAD_MEDIA_TYPE.getContext().setTheme(Helper.dialogStyle());
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
