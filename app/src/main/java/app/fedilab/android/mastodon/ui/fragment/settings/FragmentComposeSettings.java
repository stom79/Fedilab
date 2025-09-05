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

import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Languages;
import app.fedilab.android.mastodon.helper.Helper;

public class FragmentComposeSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_compose);
        createPref();
    }

    @SuppressLint("ApplySharedPref")
    private void createPref() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        //Theme for dialogs
        ListPreference SET_THREAD_MESSAGE = findPreference(getString(R.string.SET_THREAD_MESSAGE));
        if (SET_THREAD_MESSAGE != null) {
            SET_THREAD_MESSAGE.getContext().setTheme(Helper.dialogStyle());
        }
        //---------

        EditTextPreference SET_WATERMARK_TEXT = findPreference(getString(R.string.SET_WATERMARK_TEXT));
        if (SET_WATERMARK_TEXT != null) {
            String val = sharedPreferences.getString(getString(R.string.SET_WATERMARK_TEXT) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, sharedPreferences.getString(getString(R.string.SET_WATERMARK_TEXT), null));
            SET_WATERMARK_TEXT.setText(val);
        }

        SwitchPreferenceCompat SET_MENTION_BOOSTER = findPreference(getString(R.string.SET_MENTION_BOOSTER));
        if (SET_MENTION_BOOSTER != null) {
            boolean val = sharedPreferences.getBoolean(getString(R.string.SET_MENTION_BOOSTER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, sharedPreferences.getBoolean(getString(R.string.SET_MENTION_BOOSTER), false));
            SET_MENTION_BOOSTER.setChecked(val);
        }


        Preference SET_CUSTOMIZE_COLORS_VISIBILITY = findPreference(getString(R.string.SET_CUSTOMIZE_COLORS_VISIBILITY));
        if (SET_CUSTOMIZE_COLORS_VISIBILITY != null) {
            SET_CUSTOMIZE_COLORS_VISIBILITY.setOnPreferenceClickListener(preference -> {
                NavOptions.Builder navBuilder = new NavOptions.Builder();
                navBuilder.setEnterAnim(R.anim.enter).setExitAnim(R.anim.exit).setPopEnterAnim(R.anim.pop_enter).setPopExitAnim(R.anim.pop_exit);

                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(R.id.FragmentCustomLightSettings, null, navBuilder.build());
                return true;
            });
        }

        MultiSelectListPreference SET_SELECTED_LANGUAGE = findPreference(getString(R.string.SET_SELECTED_LANGUAGE));
        if (SET_SELECTED_LANGUAGE != null) {

            Set<String> storedLanguages = sharedPreferences.getStringSet(getString(R.string.SET_SELECTED_LANGUAGE), null);

            String[] selectedValue = new String[0];
            if (storedLanguages != null && !storedLanguages.isEmpty()) {
                if (storedLanguages.size() == 1 && storedLanguages.toArray()[0] == null) {
                    sharedPreferences.edit().remove(getString(R.string.SET_SELECTED_LANGUAGE)).commit();
                } else {
                    selectedValue = storedLanguages.toArray(new String[0]);
                }
            }
            List<Languages.Language> languages = Languages.get(requireActivity());
            if (languages != null) {
                String[] codesArr = new String[languages.size()];
                String[] languagesArr = new String[languages.size()];
                int i = 0;
                for (Languages.Language language : languages) {
                    codesArr[i] = language.code;
                    languagesArr[i] = language.language;
                    i++;
                }
                SET_SELECTED_LANGUAGE.setEntries(languagesArr);
                SET_SELECTED_LANGUAGE.setEntryValues(codesArr);
                if (selectedValue.length > 0) {
                    SET_SELECTED_LANGUAGE.setDefaultValue(selectedValue);
                }
            }

        }


    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Objects.requireNonNull(key).equalsIgnoreCase(getString(R.string.SET_WATERMARK_TEXT))) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.SET_WATERMARK_TEXT) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, sharedPreferences.getString(getString(R.string.SET_WATERMARK_TEXT), null));
            editor.apply();
        }
        if (Objects.requireNonNull(key).equalsIgnoreCase(getString(R.string.SET_MENTION_BOOSTER))) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.SET_MENTION_BOOSTER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, sharedPreferences.getBoolean(getString(R.string.SET_MENTION_BOOSTER), false));
            editor.apply();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                .unregisterOnSharedPreferenceChangeListener(this);
    }


}
