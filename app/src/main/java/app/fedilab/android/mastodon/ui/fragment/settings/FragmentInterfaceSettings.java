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

import static app.fedilab.android.mastodon.helper.LogoHelper.setDrawable;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ImageListPreference;
import app.fedilab.android.mastodon.helper.LogoHelper;
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

        //Theme for dialogs
        ImageListPreference SET_LOGO_LAUNCHER = findPreference(getString(R.string.SET_LOGO_LAUNCHER));
        if (SET_LOGO_LAUNCHER != null) {
            SET_LOGO_LAUNCHER.getContext().setTheme(Helper.dialogStyle());
        }
        //---------
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
        if (SET_LOGO_LAUNCHER != null) {
            SET_LOGO_LAUNCHER.setIcon(LogoHelper.getDrawable(SET_LOGO_LAUNCHER.getValue()));
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        SwitchPreferenceCompat SET_EXTAND_EXTRA_FEATURES = findPreference(getString(R.string.SET_EXTAND_EXTRA_FEATURES));
        if (SET_EXTAND_EXTRA_FEATURES != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_EXTAND_EXTRA_FEATURES.setChecked(checked);
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

            if (key.compareToIgnoreCase(getString(R.string.SET_EXTAND_EXTRA_FEATURES)) == 0) {
                SwitchPreferenceCompat SET_EXTAND_EXTRA_FEATURES = findPreference(getString(R.string.SET_EXTAND_EXTRA_FEATURES));
                if (SET_EXTAND_EXTRA_FEATURES != null) {
                    editor.putBoolean(getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, SET_EXTAND_EXTRA_FEATURES.isChecked());
                }
                recreate = true;
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_LOGO_LAUNCHER)) == 0) {
                ListPreference SET_LOGO_LAUNCHER = findPreference(getString(R.string.SET_LOGO_LAUNCHER));
                String newLauncher = sharedpreferences.getString(getString(R.string.SET_LOGO_LAUNCHER), "Bubbles");
                if (Character.isUpperCase(newLauncher.codePointAt(0))) {
                    setIcon(requireActivity(), newLauncher);
                    SET_LOGO_LAUNCHER.setIcon(LogoHelper.getDrawable(newLauncher));
                    setDrawable(newLauncher);
                    editor.putString(getString(R.string.SET_LOGO_LAUNCHER), newLauncher);
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_DISABLE_TOPBAR_SCROLLING)) == 0) {
                recreate = true;
            }
            editor.apply();
        }
    }

    private void setIcon(Context context, String theChosenIcon) {
        String[] logoTypeValues  = getResources().getStringArray(R.array.SET_LOGO_TYPE_VALUE);
        for (String logoTypeValue : logoTypeValues) {
            if (logoTypeValue.equals(theChosenIcon)) {
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName(BuildConfig.APPLICATION_ID, "app.fedilab.android.activities.MainActivity." + logoTypeValue),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } else {
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName(BuildConfig.APPLICATION_ID, "app.fedilab.android.activities.MainActivity." + logoTypeValue),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
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
