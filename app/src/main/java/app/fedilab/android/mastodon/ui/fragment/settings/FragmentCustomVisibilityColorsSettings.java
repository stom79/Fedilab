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


import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.fedilab.android.R;

public class FragmentCustomVisibilityColorsSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_custom_visibility_colors);
        createPref();
    }

    private void createPref() {
        Preference SET_RESET_CUSTOM_COLOR_VISIBILITY = findPreference(getString(R.string.SET_RESET_CUSTOM_COLOR_VISIBILITY));
        if (SET_RESET_CUSTOM_COLOR_VISIBILITY != null) {
            SET_RESET_CUSTOM_COLOR_VISIBILITY.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder resetConfirm = new MaterialAlertDialogBuilder(requireActivity());
                resetConfirm.setMessage(getString(R.string.reset_color));
                resetConfirm.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                resetConfirm.setPositiveButton(R.string.reset, (dialog, which) -> {
                    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
                    if (sharedPreferences != null) {
                        sharedPreferences.edit().remove(getString(R.string.SET_COLOR_VISIBILITY_PUBLIC)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_COLOR_VISIBILITY_UNLISTED)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_COLOR_VISIBILITY_PRIVATE)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_COLOR_VISIBILITY_DIRECT)).apply();
                    }

                    dialog.dismiss();
                });
                resetConfirm.show();
                return true;
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

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
