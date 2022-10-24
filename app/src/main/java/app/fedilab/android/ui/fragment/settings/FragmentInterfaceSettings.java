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

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;
import es.dmoral.toasty.Toasty;

public class FragmentInterfaceSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    boolean recreate;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_interface);
        createPref();
    }

    private void createPref() {
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.pref_interface);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            return;
        }
        SeekBarPreference SET_FONT_SCALE = findPreference(getString(R.string.SET_FONT_SCALE_INT));
        if (SET_FONT_SCALE != null) {
            SET_FONT_SCALE.setMax(180);
            SET_FONT_SCALE.setMin(80);
        }
        SeekBarPreference SET_FONT_SCALE_ICON = findPreference(getString(R.string.SET_FONT_SCALE_ICON_INT));
        if (SET_FONT_SCALE_ICON != null) {
            SET_FONT_SCALE_ICON.setMax(180);
            SET_FONT_SCALE_ICON.setMin(80);
        }
        recreate = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (key.compareToIgnoreCase(getString(R.string.SET_FONT_SCALE_INT)) == 0) {
                int progress = sharedPreferences.getInt(getString(R.string.SET_FONT_SCALE_INT), 110);
                float scale = (float) (progress) / 100.0f;
                editor.putFloat(getString(R.string.SET_FONT_SCALE), scale);
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_FONT_SCALE_ICON_INT)) == 0) {
                int progress = sharedPreferences.getInt(getString(R.string.SET_FONT_SCALE_ICON_INT), 110);
                float scale = (float) (progress) / 100.0f;
                editor.putFloat(getString(R.string.SET_FONT_SCALE_ICON), scale);
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_USE_SINGLE_TOPBAR)) == 0) {
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_TIMELINES_IN_A_LIST)) == 0) {
                recreate = true;
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
