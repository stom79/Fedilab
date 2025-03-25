package app.fedilab.android.mastodon.ui.fragment.settings;
/* Copyright 2025 Thomas Schneider
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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentPixelfedSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    boolean recreate;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_pixelfed);
        createPref();
    }

    @SuppressLint("ApplySharedPref")
    private void createPref() {
        getPreferenceScreen().removeAll();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        addPreferencesFromResource(R.xml.pref_pixelfed);
        SwitchPreferenceCompat SET_PIXELFED_FULL_MEDIA = findPreference(getString(R.string.SET_PIXELFED_FULL_MEDIA));
        if (SET_PIXELFED_FULL_MEDIA != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_PIXELFED_FULL_MEDIA) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_PIXELFED_FULL_MEDIA.setChecked(checked);
        }
        recreate = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (key.compareToIgnoreCase(getString(R.string.SET_PIXELFED_FULL_MEDIA)) == 0) {
                SwitchPreferenceCompat SET_PIXELFED_FULL_MEDIA = findPreference(getString(R.string.SET_PIXELFED_FULL_MEDIA));
                if (SET_PIXELFED_FULL_MEDIA != null) {
                    editor.putBoolean(getString(R.string.SET_PIXELFED_FULL_MEDIA) + MainActivity.currentUserID + MainActivity.currentInstance, SET_PIXELFED_FULL_MEDIA.isChecked());
                }
            }
            recreate = true;
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
        if (recreate) {
            recreate = false;
            requireActivity().recreate();
            Helper.recreateMainActivity(requireActivity());
        }
    }


}
