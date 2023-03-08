package app.fedilab.android.mastodon.ui.fragment.settings;
/* Copyright 2023 Thomas Schneider
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


import static android.content.Context.POWER_SERVICE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.work.Data;
import androidx.work.WorkManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.activities.CheckHomeCacheActivity;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.jobs.FetchHomeWorker;
import es.dmoral.toasty.Toasty;


public class FragmentHomeCacheSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_home_cache);
        createPref();
    }


    private void createPref() {

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.pref_home_cache);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        SwitchPreference SET_FETCH_HOME = findPreference(getString(R.string.SET_FETCH_HOME));
        if (SET_FETCH_HOME != null) {
            boolean checked = sharedpreferences.getBoolean(getString(R.string.SET_FETCH_HOME) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            SET_FETCH_HOME.setChecked(checked);
        }

        ListPreference SET_FETCH_HOME_DELAY_VALUE = findPreference(getString(R.string.SET_FETCH_HOME_DELAY_VALUE));
        if (SET_FETCH_HOME_DELAY_VALUE != null) {
            String timeRefresh = sharedpreferences.getString(getString(R.string.SET_FETCH_HOME_DELAY_VALUE) + MainActivity.currentUserID + MainActivity.currentInstance, "60");
            SET_FETCH_HOME_DELAY_VALUE.setValue(timeRefresh);
        }


        Preference pref_category_show_data = findPreference(getString(R.string.pref_category_show_data));
        if (pref_category_show_data != null) {
            pref_category_show_data.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireActivity(), CheckHomeCacheActivity.class));
                return false;
            });
        }


        Preference SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS = findPreference(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
        if (SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) requireActivity().getSystemService(POWER_SERVICE);
                String packageName = requireActivity().getPackageName();
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS.setOnPreferenceClickListener(preference -> {
                        Intent intent = new Intent();
                        String packageName1 = requireActivity().getPackageName();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName1));
                        startActivity(intent);
                        return false;
                    });
                } else {
                    preferenceScreen.removePreferenceRecursively(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
                }
            } else {
                preferenceScreen.removePreferenceRecursively(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
            }
        }

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            Data inputData = new Data.Builder()
                    .putString(Helper.ARG_INSTANCE, BaseMainActivity.currentInstance)
                    .putString(Helper.ARG_USER_ID, BaseMainActivity.currentUserID)
                    .build();
            if (key.compareToIgnoreCase(getString(R.string.SET_FETCH_HOME)) == 0) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                SwitchPreference SET_FETCH_HOME = findPreference(getString(R.string.SET_FETCH_HOME));
                if (SET_FETCH_HOME != null) {
                    editor.putBoolean(getString(R.string.SET_FETCH_HOME) + MainActivity.currentUserID + MainActivity.currentInstance, SET_FETCH_HOME.isChecked());
                    editor.commit();
                    if (SET_FETCH_HOME.isChecked()) {
                        FetchHomeWorker.setRepeatHome(requireActivity(), MainActivity.currentAccount, inputData);
                    } else {
                        WorkManager.getInstance(requireActivity()).cancelAllWorkByTag(Helper.WORKER_REFRESH_HOME + MainActivity.currentUserID + MainActivity.currentInstance);
                    }
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_FETCH_HOME_DELAY_VALUE)) == 0) {
                ListPreference SET_FETCH_HOME_DELAY_VALUE = findPreference(getString(R.string.SET_FETCH_HOME_DELAY_VALUE));
                if (SET_FETCH_HOME_DELAY_VALUE != null) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(getString(R.string.SET_FETCH_HOME_DELAY_VALUE) + MainActivity.currentUserID + MainActivity.currentInstance, SET_FETCH_HOME_DELAY_VALUE.getValue());
                    editor.commit();
                    FetchHomeWorker.setRepeatHome(requireActivity(), MainActivity.currentAccount, inputData);
                }
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
    }


}
