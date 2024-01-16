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
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentExtraFeaturesSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_extra_features);
        createPref();
    }

    private void createPref() {

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.pref_extra_features);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        //Theme for dialogs
        ListPreference SET_POST_FORMAT = findPreference(getString(R.string.SET_POST_FORMAT));
        if (SET_POST_FORMAT != null) {
            SET_POST_FORMAT.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_COMPOSE_LOCAL_ONLY = findPreference(getString(R.string.SET_DEFAULT_LOCALE_NEW));
        if (SET_COMPOSE_LOCAL_ONLY != null) {
            SET_COMPOSE_LOCAL_ONLY.getContext().setTheme(Helper.dialogStyle());
        }
        //---------

        SwitchPreferenceCompat SET_EXTAND_EXTRA_FEATURES = findPreference(getString(R.string.SET_EXTAND_EXTRA_FEATURES));
        if (SET_EXTAND_EXTRA_FEATURES != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_EXTAND_EXTRA_FEATURES.setChecked(checked);
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

        SwitchPreferenceCompat SET_DISPLAY_QUOTES = findPreference(getString(R.string.SET_DISPLAY_QUOTES));
        if (SET_DISPLAY_QUOTES != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_QUOTES) + MainActivity.currentUserID + MainActivity.currentInstance, true);
            SET_DISPLAY_QUOTES.setChecked(checked);
        }

        SwitchPreferenceCompat SET_DISPLAY_REACTIONS = findPreference(getString(R.string.SET_DISPLAY_REACTIONS));
        if (SET_DISPLAY_REACTIONS != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_REACTIONS) + MainActivity.currentUserID + MainActivity.currentInstance, true);
            SET_DISPLAY_REACTIONS.setChecked(checked);
        }

        if (SET_POST_FORMAT != null) {
            String format = sharedpreferences.getString(getString(R.string.SET_POST_FORMAT) + MainActivity.currentUserID + MainActivity.currentInstance, "text/plain");
            SET_POST_FORMAT.setValue(format);
        }

        if (SET_COMPOSE_LOCAL_ONLY != null) {
            int localOnly = sharedpreferences.getInt(getString(R.string.SET_COMPOSE_LOCAL_ONLY) + MainActivity.currentUserID + MainActivity.currentInstance, 0);
            SET_COMPOSE_LOCAL_ONLY.setValue(String.valueOf(localOnly));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (key.compareToIgnoreCase(getString(R.string.SET_EXTAND_EXTRA_FEATURES)) == 0) {
                SwitchPreferenceCompat SET_EXTAND_EXTRA_FEATURES = findPreference(getString(R.string.SET_EXTAND_EXTRA_FEATURES));
                if (SET_EXTAND_EXTRA_FEATURES != null) {
                    editor.putBoolean(getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, SET_EXTAND_EXTRA_FEATURES.isChecked());
                }
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
            if (key.compareToIgnoreCase(getString(R.string.SET_DISPLAY_QUOTES)) == 0) {
                SwitchPreferenceCompat SET_DISPLAY_QUOTES = findPreference(getString(R.string.SET_DISPLAY_QUOTES));
                if (SET_DISPLAY_QUOTES != null) {
                    editor.putBoolean(getString(R.string.SET_DISPLAY_QUOTES) + MainActivity.currentUserID + MainActivity.currentInstance, SET_DISPLAY_QUOTES.isChecked());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_DISPLAY_REACTIONS)) == 0) {
                SwitchPreferenceCompat SET_DISPLAY_REACTIONS = findPreference(getString(R.string.SET_DISPLAY_REACTIONS));
                if (SET_DISPLAY_REACTIONS != null) {
                    editor.putBoolean(getString(R.string.SET_DISPLAY_REACTIONS) + MainActivity.currentUserID + MainActivity.currentInstance, SET_DISPLAY_REACTIONS.isChecked());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_POST_FORMAT)) == 0) {
                ListPreference SET_POST_FORMAT = findPreference(getString(R.string.SET_POST_FORMAT));
                if (SET_POST_FORMAT != null) {
                    editor.putString(getString(R.string.SET_POST_FORMAT) + MainActivity.currentUserID + MainActivity.currentInstance, SET_POST_FORMAT.getValue());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_COMPOSE_LOCAL_ONLY)) == 0) {
                ListPreference SET_COMPOSE_LOCAL_ONLY = findPreference(getString(R.string.SET_COMPOSE_LOCAL_ONLY));
                if (SET_COMPOSE_LOCAL_ONLY != null) {
                    editor.putInt(getString(R.string.SET_COMPOSE_LOCAL_ONLY) + MainActivity.currentUserID + MainActivity.currentInstance, Integer.parseInt(SET_COMPOSE_LOCAL_ONLY.getValue()));
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
