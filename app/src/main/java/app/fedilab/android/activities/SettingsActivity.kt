package app.fedilab.android.activities
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

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import app.fedilab.android.R
import app.fedilab.android.databinding.ActivitySettingsBinding
import app.fedilab.android.helper.Helper
import app.fedilab.android.helper.ThemeHelper
import app.fedilab.android.ui.fragment.settings.FragmentThemingSettings

class SettingsActivity : BaseActivity(), FragmentThemingSettings.ActionTheming {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.applyThemeBar(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navController = findNavController(R.id.fragment_container)
        appBarConfiguration = AppBarConfiguration.Builder().build()
        setupActionBarWithNavController(navController, appBarConfiguration)
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragment_container)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.fragment_container)
        if (item.itemId == android.R.id.home && navController.currentDestination?.id == R.id.FragmentSettingsCategories) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun restart() {
        val restartBuilder = AlertDialog.Builder(this, Helper.dialogStyle())
        restartBuilder.setTitle(getString(R.string.restart_the_app))
        restartBuilder.setMessage(getString(R.string.restart_the_app_theme))
        restartBuilder.setNegativeButton(R.string.no) { dialog, which -> dialog.dismiss() }
        restartBuilder.setPositiveButton(R.string.restart) { dialog, which ->
            dialog.dismiss()
            Helper.restart(this)
        }
        restartBuilder.create().show()
    }
}
