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

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentTranslationSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_translation);
        createPref();
    }

    private void createPref() {
        getPreferenceScreen().removeAll();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        addPreferencesFromResource(R.xml.pref_translation);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        ListPreference SET_TRANSLATOR = findPreference(getString(R.string.SET_TRANSLATOR));
        ListPreference SET_TRANSLATOR_VERSION = findPreference(getString(R.string.SET_TRANSLATOR_VERSION));
        if (SET_TRANSLATOR_VERSION != null) {
            SET_TRANSLATOR_VERSION.getContext().setTheme(Helper.dialogStyle());
        }
        if (SET_TRANSLATOR != null) {
            SET_TRANSLATOR.getContext().setTheme(Helper.dialogStyle());
        }

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
        if (SET_TRANSLATOR != null && !SET_TRANSLATOR.getValue().equals("MINT")) {
            preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_DOMAIN_MINT");
        }
        if (SET_TRANSLATOR != null && !SET_TRANSLATOR.getValue().equals("APERTIUM")) {
            preferenceScreen.removePreferenceRecursively("SET_TRANSLATOR_DOMAIN_APERTIUM");
        }

        ListPreference SET_TRANSLATE_BUTTON = findPreference(getString(R.string.SET_TRANSLATE_BUTTON));
        if (SET_TRANSLATE_BUTTON != null) {
            String value = sharedpreferences.getString(getString(R.string.SET_TRANSLATE_BUTTON) + MainActivity.currentUserID + MainActivity.currentInstance, null);
            SET_TRANSLATE_BUTTON.setValue(value);
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
            if (key.compareToIgnoreCase(getString(R.string.SET_TRANSLATE_BUTTON)) == 0) {
                ListPreference SET_TRANSLATE_BUTTON = findPreference(getString(R.string.SET_TRANSLATE_BUTTON));
                if (SET_TRANSLATE_BUTTON != null) {
                    editor.putString(getString(R.string.SET_TRANSLATE_BUTTON) + MainActivity.currentUserID + MainActivity.currentInstance, SET_TRANSLATE_BUTTON.getValue());
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
