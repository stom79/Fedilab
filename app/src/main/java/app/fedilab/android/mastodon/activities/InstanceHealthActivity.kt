package app.fedilab.android.mastodon.activities

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Window
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import app.fedilab.android.BaseMainActivity
import app.fedilab.android.R
import app.fedilab.android.databinding.ActivityInstanceSocialBinding
import app.fedilab.android.mastodon.client.entities.app.InstanceSocial
import app.fedilab.android.mastodon.helper.Helper
import app.fedilab.android.mastodon.helper.ThemeHelper
import app.fedilab.android.mastodon.viewmodel.mastodon.InstanceSocialVM
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
class InstanceHealthActivity : DialogFragment() {
    private var _binding: ActivityInstanceSocialBinding? = null
    private val binding: ActivityInstanceSocialBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = ActivityInstanceSocialBinding.inflate(layoutInflater)
        binding.close.setOnClickListener { dialog?.dismiss() }
        val content = SpannableString(binding.refInstance.text.toString())
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        binding.refInstance.text = content
        binding.refInstance.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instances.social")))
        }

        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
        materialAlertDialogBuilder.setView(binding.root)

        val dialog = materialAlertDialogBuilder.create()
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setOnShowListener { checkInstance() }

        return dialog
    }

    private fun checkInstance() {
        val instanceSocialVM =
            ViewModelProvider(this@InstanceHealthActivity)[InstanceSocialVM::class.java]
        instanceSocialVM.getInstances(BaseMainActivity.currentInstance.trim { it <= ' ' })
            .observe(this@InstanceHealthActivity) { instanceSocialList: InstanceSocial? ->
                val instance = instanceSocialList?.instances?.firstOrNull { instance ->
                    instance.name.equals(
                        BaseMainActivity.currentInstance.trim { it <= ' ' },
                        ignoreCase = true
                    )
                }
                if (instance != null) {
                    instance.thumbnail?.takeIf { it != "null" }?.let { thumbnail ->
                        Glide.with(this@InstanceHealthActivity)
                            .asBitmap()
                            .placeholder(R.drawable.default_banner)
                            .load(thumbnail)
                            .into(binding.backgroundImage)
                    }
                    binding.name.text = instance.name
                    if (instance.up) {
                        binding.up.setText(app.fedilab.android.R.string.is_up)
                        binding.up.setTextColor(
                            ThemeHelper.getAttColor(
                                requireContext(),
                                app.fedilab.android.R.attr.colorPrimary
                            )
                        )
                    } else {
                        binding.up.setText(app.fedilab.android.R.string.is_down)
                        binding.up.setTextColor(
                            ThemeHelper.getAttColor(
                                requireContext(),
                                app.fedilab.android.R.attr.colorError
                            )
                        )
                    }
                    binding.uptime.text = getString(
                        app.fedilab.android.R.string.instance_health_uptime,
                        instance.uptime * 100
                    )
                    if (instance.checked_at != null)
                        binding.checkedAt.text =
                            getString(
                                app.fedilab.android.R.string.instance_health_checkedat,
                                Helper.dateToString(instance.checked_at)
                            )
                    binding.values.text = getString(
                        app.fedilab.android.R.string.instance_health_indication,
                        instance.version,
                        Helper.withSuffix(instance.active_users.toLong()),
                        Helper.withSuffix(instance.statuses.toLong())
                    )
                } else {
                    binding.instanceData.isVisible = false
                    binding.noInstance.isVisible = true
                }
                binding.loader.isVisible = false
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}