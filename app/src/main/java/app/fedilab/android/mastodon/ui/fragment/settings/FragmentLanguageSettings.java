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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentLanguageSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_language);
        createPref();
    }

    @SuppressLint("ApplySharedPref")
    private void createPref() {
        Preference SET_TRANSLATE_VALUES_RESET = findPreference(getString(R.string.SET_TRANSLATE_VALUES_RESET));
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        if (SET_TRANSLATE_VALUES_RESET != null) {
            SET_TRANSLATE_VALUES_RESET.setOnPreferenceClickListener(preference -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.SET_DEFAULT_LOCALE_NEW), null);
                editor.commit();
                return true;
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.compareToIgnoreCase(getString(R.string.SET_DEFAULT_LOCALE_NEW)) == 0 || key.compareToIgnoreCase(getString(R.string.SET_TRANSLATE_VALUES_RESET)) == 0) {
            requireActivity().finish();
            startActivity(requireActivity().getIntent());
            Helper.recreateMainActivity(requireActivity());
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
