package app.fedilab.android.ui.fragment.settings
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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.fedilab.android.BaseMainActivity.currentAccount
import app.fedilab.android.R
import app.fedilab.android.helper.SettingsStorage


class FragmentSettingsCategories : PreferenceFragmentCompat() {

    private val REQUEST_CODE = 5412

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_categories, rootKey)

        findPreference<Preference>(getString(R.string.pref_category_key_account))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToAccount())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_timeline))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToTimelines())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_notifications))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToNotifications())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_interface))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToInterface())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_compose))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToCompose())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_privacy))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToPrivacy())
            false
        }

        findPreference<Preference>(getString(R.string.pref_category_key_theming))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToTheming())
            false
        }

        findPreference<Preference>(getString(R.string.pref_export_settings))?.setOnPreferenceClickListener {
            val permissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    SettingsStorage.saveSharedPreferencesToFile(context)
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
                }
            }
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            false
        }

        findPreference<Preference>(getString(R.string.pref_import_settings))?.setOnPreferenceClickListener {

            false
        }

        val adminPreference = findPreference<Preference>(getString(R.string.pref_category_key_administration))
        adminPreference?.isVisible = currentAccount.admin
        adminPreference?.setOnPreferenceClickListener { false }

        findPreference<Preference>(getString(R.string.pref_category_key_languages))?.setOnPreferenceClickListener {
            findNavController().navigate(FragmentSettingsCategoriesDirections.categoriesToLanguage())
            false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SettingsStorage.saveSharedPreferencesToFile(context)
            } else {
                Toast.makeText(context, getString(R.string.permission_missing), Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}
