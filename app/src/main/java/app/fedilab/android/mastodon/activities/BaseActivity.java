package app.fedilab.android.mastodon.activities;
/* Copyright 2021 Thomas Schneider
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
import static app.fedilab.android.BaseMainActivity.currentNightMode;
import static app.fedilab.android.BaseMainActivity.currentUserID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import org.conscrypt.Conscrypt;

import java.security.Security;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;


@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    public static int currentThemeId;

    static {
        EmojiManager.install(new EmojiOneProvider());
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        boolean patch_provider = true;
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            patch_provider = sharedpreferences.getBoolean(Helper.SET_SECURITY_PROVIDER, true);
        } catch (Exception ignored) {
        }
        if (patch_provider) {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1);
            } catch (Exception ignored) {
            }
        }

        String currentTheme = sharedpreferences.getString(getString(R.string.SET_THEME_BASE), getString(R.string.SET_DEFAULT_THEME));
        boolean customAccentEnabled = sharedpreferences.getBoolean(getString(R.string.SET_CUSTOM_ACCENT) + currentUserID + currentInstance, false);

        //Default automatic switch
        currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;


        if(customAccentEnabled && currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        if (currentTheme.equals(getString(R.string.SET_DEFAULT_THEME))) {
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO -> {
                    String defaultLight = sharedpreferences.getString(getString(R.string.SET_THEME_DEFAULT_LIGHT), "LIGHT");
                    switch (defaultLight) {
                        case "LIGHT" -> {
                            setTheme(R.style.AppTheme);
                            currentThemeId = R.style.AppTheme;
                        }
                        case "SOLARIZED_LIGHT" -> {
                            setTheme(R.style.SolarizedAppTheme);
                            currentThemeId = R.style.SolarizedAppTheme;
                        }
                    }
                }
                case Configuration.UI_MODE_NIGHT_YES -> {
                    String defaultDark = sharedpreferences.getString(getString(R.string.SET_THEME_DEFAULT_DARK), "DARK");
                    switch (defaultDark) {
                        case "DARK" -> {
                            setTheme(R.style.AppTheme);
                            currentThemeId = R.style.AppTheme;
                        }
                        case "SOLARIZED_DARK" -> {
                            setTheme(R.style.SolarizedAppTheme);
                            currentThemeId = R.style.SolarizedAppTheme;
                        }
                        case "BLACK" -> {
                            setTheme(R.style.BlackAppTheme);
                            currentThemeId = R.style.BlackAppTheme;
                        }
                        case "DRACULA" -> {
                            setTheme(R.style.DraculaAppTheme);
                            currentThemeId = R.style.DraculaAppTheme;
                        }
                    }
                }
            }
        } else {
            switch (currentTheme) {
                case "LIGHT" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    setTheme(R.style.AppTheme);
                    currentThemeId = R.style.AppTheme;
                }
                case "DARK" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.AppTheme);
                    currentThemeId = R.style.AppTheme;
                }
                case "SOLARIZED_LIGHT" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    setTheme(R.style.SolarizedAppTheme);
                    currentThemeId = R.style.SolarizedAppTheme;
                }
                case "SOLARIZED_DARK" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.SolarizedAppTheme);
                    currentThemeId = R.style.SolarizedAppTheme;
                }
                case "BLACK" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.BlackAppTheme);
                    currentThemeId = R.style.BlackAppTheme;
                }
                case "DRACULA" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    setTheme(R.style.DraculaAppTheme);
                    currentThemeId = R.style.DraculaAppTheme;
                }
            }
        }
        super.onCreate(savedInstanceState);

        ThemeHelper.applyThemeColor(this);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            ThemeHelper.adjustFontScale(this, getResources().getConfiguration());
        }
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }
        Helper.setLocale(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            final Configuration override = new Configuration();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
            override.fontScale = prefs.getFloat(newBase.getString(R.string.SET_FONT_SCALE), 1.1f);
            applyOverrideConfiguration(override);
        }
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

}
