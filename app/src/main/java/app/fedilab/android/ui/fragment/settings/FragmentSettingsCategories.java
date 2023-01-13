package app.fedilab.android.ui.fragment.settings;
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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.IOException;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ZipHelper;
import es.dmoral.toasty.Toasty;

public class FragmentSettingsCategories extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE = 5412;
    private static final int PICKUP_FILE = 452;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.pref_categories);


        Preference pref_category_key_account = findPreference(getString(R.string.pref_category_key_account));
        if (pref_category_key_account != null) {
            pref_category_key_account.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToAccount());
                return false;
            });
        }

        Preference pref_category_key_timeline = findPreference(getString(R.string.pref_category_key_timeline));
        if (pref_category_key_timeline != null) {
            pref_category_key_timeline.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToTimelines());
                return false;
            });
        }

        Preference pref_category_key_notifications = findPreference(getString(R.string.pref_category_key_notifications));
        if (pref_category_key_notifications != null) {
            pref_category_key_notifications.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToNotifications());
                return false;
            });
        }

        Preference pref_category_key_interface = findPreference(getString(R.string.pref_category_key_interface));
        if (pref_category_key_interface != null) {
            pref_category_key_interface.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToInterface());
                return false;
            });
        }

        Preference pref_category_key_compose = findPreference(getString(R.string.pref_category_key_compose));
        if (pref_category_key_compose != null) {
            pref_category_key_compose.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToCompose());
                return false;
            });
        }

        Preference pref_category_key_languages = findPreference(getString(R.string.pref_category_key_languages));
        if (pref_category_key_languages != null) {
            pref_category_key_languages.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToLanguage());
                return false;
            });
        }

        Preference pref_category_key_privacy = findPreference(getString(R.string.pref_category_key_privacy));
        if (pref_category_key_privacy != null) {
            pref_category_key_privacy.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToPrivacy());
                return false;
            });
        }

        Preference pref_category_key_theming = findPreference(getString(R.string.pref_category_key_theming));
        if (pref_category_key_theming != null) {
            pref_category_key_theming.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToTheming());
                return false;
            });
        }

        Preference pref_category_key_extra_features = findPreference(getString(R.string.pref_category_key_extra_features));
        if (pref_category_key_extra_features != null) {
            pref_category_key_extra_features.setOnPreferenceClickListener(preference -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(FragmentSettingsCategoriesDirections.Companion.categoriesToExtraFeatures());
                return false;
            });
        }

        ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                try {
                    ZipHelper.exportData(requireActivity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        });

        Preference pref_export_settings = findPreference(getString(R.string.pref_export_settings));
        if (pref_export_settings != null) {
            pref_export_settings.setOnPreferenceClickListener(preference -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    try {
                        ZipHelper.exportData(requireActivity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
        }

        Preference pref_import_settings = findPreference(getString(R.string.pref_import_settings));
        if (pref_import_settings != null) {
            pref_import_settings.setOnPreferenceClickListener(preference -> {
                Intent openFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openFileIntent.setType("application/zip");
                String[] mimeTypes = new String[]{"application/zip"};
                openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                //noinspection deprecation
                startActivityForResult(
                        Intent.createChooser(
                                openFileIntent,
                                getString(R.string.load_settings)), PICKUP_FILE);
                return false;
            });
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICKUP_FILE) {
            if (data == null || data.getData() == null) {
                Toasty.error(requireActivity(), getString(R.string.toot_select_file_error), Toast.LENGTH_LONG).show();
                return;
            }
            Helper.createFileFromUri(requireActivity(), data.getData(), file -> ZipHelper.importData(requireActivity(), file));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    ZipHelper.exportData(requireActivity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toasty.error(requireActivity(), getString(R.string.permission_missing), Toasty.LENGTH_SHORT).show();
            }
        }
    }
}
