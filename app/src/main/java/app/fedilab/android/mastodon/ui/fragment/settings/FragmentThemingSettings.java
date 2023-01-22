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

import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.helper.Helper;
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

        Preference SET_CUSTOMIZE_LIGHT_COLORS_ACTION = findPreference(getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS_ACTION));
        if (SET_CUSTOMIZE_LIGHT_COLORS_ACTION != null) {
            SET_CUSTOMIZE_LIGHT_COLORS_ACTION.setOnPreferenceClickListener(preference -> {
                NavOptions.Builder navBuilder = new NavOptions.Builder();
                navBuilder.setEnterAnim(R.anim.enter).setExitAnim(R.anim.exit).setPopEnterAnim(R.anim.pop_enter).setPopExitAnim(R.anim.pop_exit);

                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(R.id.FragmentCustomLightSettings, null, navBuilder.build());
                return true;
            });
        }

        Preference SET_CUSTOMIZE_DARK_COLORS_ACTION = findPreference(getString(R.string.SET_CUSTOMIZE_DARK_COLORS_ACTION));
        if (SET_CUSTOMIZE_DARK_COLORS_ACTION != null) {
            SET_CUSTOMIZE_DARK_COLORS_ACTION.setOnPreferenceClickListener(preference -> {
                NavOptions.Builder navBuilder = new NavOptions.Builder();
                navBuilder.setEnterAnim(R.anim.enter).setExitAnim(R.anim.exit).setPopEnterAnim(R.anim.pop_enter).setPopExitAnim(R.anim.pop_exit);
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(R.id.FragmentCustomDarkSettings, null, navBuilder.build());
                return true;
            });
        }

        Preference SET_RESET_CUSTOM_COLOR = findPreference(getString(R.string.SET_RESET_CUSTOM_COLOR));
        if (SET_RESET_CUSTOM_COLOR != null) {
            SET_RESET_CUSTOM_COLOR.getContext().setTheme(Helper.dialogStyle());
            SET_RESET_CUSTOM_COLOR.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder resetConfirm = new MaterialAlertDialogBuilder(requireActivity(), Helper.dialogStyle());
                resetConfirm.setMessage(getString(R.string.reset_color));
                resetConfirm.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                resetConfirm.setPositiveButton(R.string.reset, (dialog, which) -> {
                    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
                    if (sharedPreferences != null) {
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_BACKGROUND)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_BOOST_HEADER)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_DISPLAY_NAME)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_USERNAME)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_TEXT)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_LINK)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_LIGHT_ICON)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_BACKGROUND)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_BOOST_HEADER)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_DISPLAY_NAME)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_USERNAME)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_TEXT)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_LINK)).apply();
                        sharedPreferences.edit().remove(getString(R.string.SET_DARK_ICON)).apply();

                    }

                    dialog.dismiss();
                });
                resetConfirm.show();
                return true;
            });
        }
    }

}
