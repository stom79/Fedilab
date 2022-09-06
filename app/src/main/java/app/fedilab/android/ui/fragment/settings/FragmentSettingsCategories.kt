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
import android.app.Activity
import android.content.Intent
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
import es.dmoral.toasty.Toasty


class FragmentSettingsCategories : PreferenceFragmentCompat() {

    private val REQUEST_CODE = 5412
    private val PICKUP_FILE = 452

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
        @Suppress("DEPRECATION") val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                SettingsStorage.saveSharedPreferencesToFile(context)
            } else {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
            }
        }
        findPreference<Preference>(getString(R.string.pref_export_settings))?.setOnPreferenceClickListener {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            false
        }

        findPreference<Preference>(getString(R.string.pref_import_settings))?.setOnPreferenceClickListener {
            val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            openFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
            openFileIntent.type = "text/plain"
            val mimeTypes = arrayOf("text/plain")
            openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

            startActivityForResult(
                    Intent.createChooser(
                            openFileIntent,
                            getString(R.string.load_settings)), PICKUP_FILE)
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICKUP_FILE) {
            val result = SettingsStorage.loadSharedPreferencesFromFile(context, data?.data)
            if (result) {
                activity?.let { Toasty.success(it, getString(R.string.data_import_settings_success), Toasty.LENGTH_LONG).show() }
            } else {
                activity?.let { Toasty.error(it, getString(R.string.toast_error), Toasty.LENGTH_LONG).show() }
            }
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
