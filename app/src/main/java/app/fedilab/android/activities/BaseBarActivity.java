package app.fedilab.android.activities;
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
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;


@SuppressLint("Registered")
public class BaseBarActivity extends AppCompatActivity {

    static {
        Helper.installProvider();
        EmojiManager.install(new EmojiOneProvider());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String currentTheme = sharedpreferences.getString(getString(R.string.SET_THEME_BASE), getString(R.string.SET_DEFAULT_THEME));
        //Default automatic switch
        if (currentTheme.equals(getString(R.string.SET_DEFAULT_THEME))) {

            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    String defaultLight = sharedpreferences.getString(getString(R.string.SET_THEME_DEFAULT_LIGHT), "LIGHT");
                    switch (defaultLight) {
                        case "LIGHT":
                            setTheme(R.style.AppThemeBar);
                            break;
                        case "SOLARIZED_LIGHT":
                            setTheme(R.style.SolarizedAppThemeBar);
                            break;
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    String defaultDark = sharedpreferences.getString(getString(R.string.SET_THEME_DEFAULT_DARK), "DARK");
                    switch (defaultDark) {
                        case "DARK":
                            setTheme(R.style.AppThemeBar);
                            break;
                        case "SOLARIZED_DARK":
                            setTheme(R.style.SolarizedAppThemeBar);
                            break;
                    }
                    break;
            }

        } else {
            switch (currentTheme) {
                case "LIGHT":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    setTheme(R.style.AppThemeBar);
                    break;
                case "DARK":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.AppThemeBar);
                    break;
                case "SOLARIZED_LIGHT":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    setTheme(R.style.SolarizedAppThemeBar);
                    break;
                case "SOLARIZED_DARK":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.SolarizedAppThemeBar);
                    break;
            }
        }
        super.onCreate(savedInstanceState);
        ThemeHelper.adjustFontScale(this, getResources().getConfiguration());
        Helper.setLocale(this);
    }

}
