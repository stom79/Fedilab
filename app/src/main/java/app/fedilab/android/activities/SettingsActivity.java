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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySettingsBinding;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.settings.FragmentAdministrationSettings;
import app.fedilab.android.ui.fragment.settings.FragmentComposeSettings;
import app.fedilab.android.ui.fragment.settings.FragmentInterfaceSettings;
import app.fedilab.android.ui.fragment.settings.FragmentLanguageSettings;
import app.fedilab.android.ui.fragment.settings.FragmentNotificationsSettings;
import app.fedilab.android.ui.fragment.settings.FragmentPrivacySettings;
import app.fedilab.android.ui.fragment.settings.FragmentThemingSettings;
import app.fedilab.android.ui.fragment.settings.FragmentTimelinesSettings;


public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;
    private boolean canGoBack;
    private Fragment currentFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        canGoBack = false;

        binding.setTimelines.setOnClickListener(v -> displaySettings(SettingsEnum.TIMELINES));
        binding.setNotifications.setOnClickListener(v -> displaySettings(SettingsEnum.NOTIFICATIONS));
        binding.setInterface.setOnClickListener(v -> displaySettings(SettingsEnum.INTERFACE));
        binding.setCompose.setOnClickListener(v -> displaySettings(SettingsEnum.COMPOSE));
        binding.setPrivacy.setOnClickListener(v -> displaySettings(SettingsEnum.PRIVACY));
        binding.setTheming.setOnClickListener(v -> displaySettings(SettingsEnum.THEMING));
        binding.setAdministration.setOnClickListener(v -> displaySettings(SettingsEnum.ADMINISTRATION));
        binding.setLanguage.setOnClickListener(v -> displaySettings(SettingsEnum.LANGUAGE));
        if (MainActivity.accountWeakReference.get().mastodon_account.admin) {
            binding.setAdministration.setVisibility(View.VISIBLE);
        } else {
            binding.setAdministration.setVisibility(View.GONE);
        }
    }

    public void displaySettings(SettingsEnum settingsEnum) {

        ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            String category = "";
            switch (settingsEnum) {
                case TIMELINES:
                    FragmentTimelinesSettings fragmentTimelinesSettings = new FragmentTimelinesSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentTimelinesSettings);
                    currentFragment = fragmentTimelinesSettings;
                    category = getString(R.string.settings_category_label_timelines);
                    break;
                case NOTIFICATIONS:
                    FragmentNotificationsSettings fragmentNotificationsSettings = new FragmentNotificationsSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentNotificationsSettings);
                    currentFragment = fragmentNotificationsSettings;
                    category = getString(R.string.notifications);
                    break;
                case INTERFACE:
                    FragmentInterfaceSettings fragmentInterfaceSettings = new FragmentInterfaceSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentInterfaceSettings);
                    currentFragment = fragmentInterfaceSettings;
                    category = getString(R.string.settings_category_label_interface);
                    break;
                case COMPOSE:
                    FragmentComposeSettings fragmentComposeSettings = new FragmentComposeSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentComposeSettings);
                    currentFragment = fragmentComposeSettings;
                    category = getString(R.string.compose);
                    break;
                case PRIVACY:
                    FragmentPrivacySettings fragmentPrivacySettings = new FragmentPrivacySettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentPrivacySettings);
                    currentFragment = fragmentPrivacySettings;
                    category = getString(R.string.action_privacy);
                    break;
                case THEMING:
                    FragmentThemingSettings fragmentThemingSettings = new FragmentThemingSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentThemingSettings);
                    currentFragment = fragmentThemingSettings;
                    category = getString(R.string.theming);
                    break;
                case ADMINISTRATION:
                    FragmentAdministrationSettings fragmentAdministrationSettings = new FragmentAdministrationSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentAdministrationSettings);
                    currentFragment = fragmentAdministrationSettings;
                    category = getString(R.string.administration);
                    break;
                case LANGUAGE:
                    FragmentLanguageSettings fragmentLanguageSettings = new FragmentLanguageSettings();
                    fragmentTransaction.replace(R.id.fragment_container, fragmentLanguageSettings);
                    currentFragment = fragmentLanguageSettings;
                    category = getString(R.string.languages);
                    break;

            }
            String title = String.format(Locale.getDefault(), "%s - %s", getString(R.string.settings), category);
            setTitle(title);
            canGoBack = true;
            fragmentTransaction.commit();
        });
    }


    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.buttonContainer, () -> {
                if (currentFragment != null) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction =
                            fragmentManager.beginTransaction();
                    fragmentTransaction.remove(currentFragment).commit();
                }
            });
            setTitle(R.string.settings);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentFragment != null) {
            currentFragment.onDestroy();
        }
        binding = null;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public enum SettingsEnum {
        @SerializedName("TIMELINES")
        TIMELINES("TIMELINES"),
        @SerializedName("NOTIFICATIONS")
        NOTIFICATIONS("NOTIFICATIONS"),
        @SerializedName("INTERFACE")
        INTERFACE("INTERFACE"),
        @SerializedName("COMPOSE")
        COMPOSE("COMPOSE"),
        @SerializedName("PRIVACY")
        PRIVACY("PRIVACY"),
        @SerializedName("THEMING")
        THEMING("THEMING"),
        @SerializedName("ADMINISTRATION")
        ADMINISTRATION("ADMINISTRATION"),
        @SerializedName("LANGUAGE")
        LANGUAGE("LANGUAGE");

        private final String value;

        SettingsEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
