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


import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentUserID;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;
import es.dmoral.toasty.Toasty;

public class FragmentThemingSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    boolean prefChanged = false;

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
            if (prefChanged) {
                Helper.recreateMainActivity(requireActivity());
                prefChanged = false;
            }
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        SharedPreferences.Editor editor = sharedpreferences.edit();
        prefChanged = true;
        if (key.compareTo(getString(R.string.SET_THEME_BASE)) == 0) {
            ListPreference SET_THEME_BASE = findPreference(getString(R.string.SET_THEME_BASE));
            if (SET_THEME_BASE != null) {
                requireActivity().finish();
                startActivity(requireActivity().getIntent());
            }
            Helper.recreateMainActivity(requireActivity());
        } else if (key.compareTo(getString(R.string.SET_CUSTOM_ACCENT)) == 0) {
            SwitchPreferenceCompat SET_CUSTOM_ACCENT = findPreference(getString(R.string.SET_CUSTOM_ACCENT));
            if (SET_CUSTOM_ACCENT != null) {
                editor.putBoolean(getString(R.string.SET_CUSTOM_ACCENT) + MainActivity.currentUserID + MainActivity.currentInstance, SET_CUSTOM_ACCENT.isChecked());
            }
        } else if (key.compareTo(getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE)) == 0) {
            ColorPreferenceCompat SET_CUSTOM_ACCENT_VALUE = findPreference(getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE));
            if (SET_CUSTOM_ACCENT_VALUE != null) {
                editor.putInt(getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE) + MainActivity.currentUserID + MainActivity.currentInstance, SET_CUSTOM_ACCENT_VALUE.getColor());
            }
        } else if (key.compareTo(getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE)) == 0) {
            ColorPreferenceCompat SET_CUSTOM_ACCENT_VALUE = findPreference(getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE));
            if (SET_CUSTOM_ACCENT_VALUE != null) {
                editor.putInt(getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE) + MainActivity.currentUserID + MainActivity.currentInstance, SET_CUSTOM_ACCENT_VALUE.getColor());
            }
        }
        editor.apply();
    }


    private void createPref() {
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.pref_theming);
        if (getPreferenceScreen() == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        SwitchPreferenceCompat SET_DYNAMIC_COLOR = findPreference(getString(R.string.SET_DYNAMICCOLOR));
        SwitchPreferenceCompat SET_CUSTOM_ACCENT = findPreference(getString(R.string.SET_CUSTOM_ACCENT));
        ColorPreferenceCompat SET_CUSTOM_ACCENT_DARK_VALUE = findPreference(getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE));
        ColorPreferenceCompat SET_CUSTOM_ACCENT_LIGHT_VALUE = findPreference(getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE));
        if (DynamicColors.isDynamicColorAvailable()) {
            if (SET_CUSTOM_ACCENT != null) {
                boolean customAccentEnabled = sharedpreferences.getBoolean(getString(R.string.SET_CUSTOM_ACCENT) + currentUserID + currentInstance, false);
                SET_CUSTOM_ACCENT.setChecked(customAccentEnabled);
            }
            if (SET_CUSTOM_ACCENT_DARK_VALUE != null) {
                int darkValue = sharedpreferences.getInt(getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE) + currentUserID + currentInstance, -1);
                SET_CUSTOM_ACCENT_DARK_VALUE.setColor(darkValue);
            }
            if (SET_CUSTOM_ACCENT_LIGHT_VALUE != null) {
                int lightValue = sharedpreferences.getInt(getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE) + currentUserID + currentInstance, -1);
                SET_CUSTOM_ACCENT_LIGHT_VALUE.setColor(lightValue);
            }
        } else {
            if (SET_DYNAMIC_COLOR != null) {
                getPreferenceScreen().removePreference(SET_DYNAMIC_COLOR);
            }
            if (SET_CUSTOM_ACCENT != null) {
                getPreferenceScreen().removePreference(SET_CUSTOM_ACCENT);
            }
            if (SET_CUSTOM_ACCENT_DARK_VALUE != null) {
                getPreferenceScreen().removePreference(SET_CUSTOM_ACCENT_DARK_VALUE);
            }
            if (SET_CUSTOM_ACCENT_LIGHT_VALUE != null) {
                getPreferenceScreen().removePreference(SET_CUSTOM_ACCENT_LIGHT_VALUE);
            }
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
            SET_RESET_CUSTOM_COLOR.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder resetConfirm = new MaterialAlertDialogBuilder(requireActivity());
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
