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

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;
import es.dmoral.toasty.Toasty;

public class FragmentThemingSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {



    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        createPref();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.compareTo(getString(R.string.SET_THEME_BASE)) == 0) {
            ListPreference SET_THEME_BASE = findPreference(getString(R.string.SET_THEME_BASE));
            if (SET_THEME_BASE != null) {
                requireActivity().finish();
                startActivity(requireActivity().getIntent());
            }
        }
        //TODO: check if can be removed
        Helper.recreateMainActivity(requireActivity());
    }


    private void createPref() {
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.pref_theming);
        if (getPreferenceScreen() == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
        }
        ListPreference SET_THEME_BASE = findPreference(getString(R.string.SET_THEME_BASE));
        if (SET_THEME_BASE != null) {
            SET_THEME_BASE.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_THEME_DEFAULT_LIGHT = findPreference(getString(R.string.SET_THEME_DEFAULT_LIGHT));
        if (SET_THEME_DEFAULT_LIGHT != null) {
            SET_THEME_DEFAULT_LIGHT.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_THEME_DEFAULT_DARK = findPreference(getString(R.string.SET_THEME_DEFAULT_DARK));
        if (SET_THEME_DEFAULT_DARK != null) {
            SET_THEME_DEFAULT_DARK.getContext().setTheme(Helper.dialogStyle());
        }
    }

}
