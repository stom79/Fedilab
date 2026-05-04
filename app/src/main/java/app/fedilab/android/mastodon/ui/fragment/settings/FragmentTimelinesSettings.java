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

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentTimelinesSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    boolean recreate;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_timelines);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        SwitchPreferenceCompat SET_DISPLAY_BOOKMARK = findPreference(getString(R.string.SET_DISPLAY_BOOKMARK));
        if (SET_DISPLAY_BOOKMARK != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_BOOKMARK) + MainActivity.currentUserID + MainActivity.currentInstance, true);
            SET_DISPLAY_BOOKMARK.setChecked(checked);
        }

        ListPreference SET_QUOTE_BUTTON = findPreference(getString(R.string.SET_QUOTE_BUTTON));
        if (SET_QUOTE_BUTTON != null) {
            String value = sharedpreferences.getString(getString(R.string.SET_QUOTE_BUTTON) + MainActivity.currentUserID + MainActivity.currentInstance, null);
            SET_QUOTE_BUTTON.setValue(value);
        }
        recreate = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (key.compareToIgnoreCase(getString(R.string.SET_TIMELINE_SCROLLBAR)) == 0) {
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_REVERSE_TIMELINE)) == 0) {
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_DISPLAY_BOOKMARK)) == 0) {
                SwitchPreferenceCompat SET_DISPLAY_BOOKMARK = findPreference(getString(R.string.SET_DISPLAY_BOOKMARK));
                if (SET_DISPLAY_BOOKMARK != null) {
                    editor.putBoolean(getString(R.string.SET_DISPLAY_BOOKMARK) + MainActivity.currentUserID + MainActivity.currentInstance, SET_DISPLAY_BOOKMARK.isChecked());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_QUOTE_BUTTON)) == 0) {
                ListPreference SET_QUOTE_BUTTON = findPreference(getString(R.string.SET_QUOTE_BUTTON));
                if (SET_QUOTE_BUTTON != null) {
                    editor.putString(getString(R.string.SET_QUOTE_BUTTON) + MainActivity.currentUserID + MainActivity.currentInstance, SET_QUOTE_BUTTON.getValue());
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
        if (recreate) {
            recreate = false;
            requireActivity().recreate();
            Helper.recreateMainActivity(requireActivity());
        }
    }

}
