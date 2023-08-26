package app.fedilab.android.mastodon.activities;
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


import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityInstanceBinding;
import app.fedilab.android.mastodon.client.entities.api.Instance;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.InstancesVM;


public class InstanceActivity extends DialogFragment {

    ActivityInstanceBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityInstanceBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setView(binding.getRoot());

        Dialog dialog = materialAlertDialogBuilder.create();

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        final SpannableString contentAbout = new SpannableString(getString(R.string.action_about_instance));
        contentAbout.setSpan(new UnderlineSpan(), 0, contentAbout.length(), 0);
        contentAbout.setSpan(new ForegroundColorSpan(ThemeHelper.getAttColor(requireContext(), R.attr.colorPrimary)), 0, contentAbout.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.about.setText(contentAbout);

        final SpannableString contentPrivacy = new SpannableString(getString(R.string.action_privacy_policy));
        contentPrivacy.setSpan(new UnderlineSpan(), 0, contentPrivacy.length(), 0);
        contentPrivacy.setSpan(new ForegroundColorSpan(ThemeHelper.getAttColor(requireContext(), R.attr.colorPrimary)), 0, contentPrivacy.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.privacy.setText(contentPrivacy);

        binding.about.setOnClickListener(v -> Helper.openBrowser(requireActivity(), "https://" + MainActivity.currentInstance + "/about"));
        binding.privacy.setOnClickListener(v -> Helper.openBrowser(requireActivity(), "https://" + MainActivity.currentInstance + "/privacy-policy"));
        int maxCharCustom = sharedpreferences.getInt(getString(R.string.SET_MAX_INSTANCE_CHAR) + MainActivity.currentInstance, -1);
        if (maxCharCustom != -1) {
            binding.maxChar.setText(String.valueOf(maxCharCustom));
        }
        binding.close.setOnClickListener(view -> {
                    String max_char = binding.maxChar.getText().toString();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    if (!max_char.isEmpty()) {
                        try {
                            editor.putInt(getString(R.string.SET_MAX_INSTANCE_CHAR) + MainActivity.currentInstance, Integer.parseInt(max_char));
                            editor.apply();
                        } catch (Exception ignored) {
                        }
                    } else {
                        editor.putInt(getString(R.string.SET_MAX_INSTANCE_CHAR) + MainActivity.currentInstance, -1);
                        editor.apply();
                    }
                    requireDialog().dismiss();
                }

        );

        InstancesVM instancesVM = new ViewModelProvider(InstanceActivity.this).get(InstancesVM.class);
        instancesVM.getInstance(BaseMainActivity.currentInstance).observe(InstanceActivity.this, instanceInfo -> {
            binding.loader.setVisibility(View.GONE);

            if (instanceInfo == null || instanceInfo.info == null || instanceInfo.info.description == null) {
                binding.instanceData.setVisibility(View.GONE);
                binding.contact.setVisibility(View.GONE);
                int val = sharedpreferences.getInt(getString(R.string.SET_MAX_INSTANCE_CHAR) + MainActivity.currentInstance, -1);
                if (val != -1) {
                    binding.maxChar.setText(String.valueOf(val));
                }
            } else {
                Instance instance = instanceInfo.info;

                Glide.with(InstanceActivity.this)
                        .asDrawable()
                        .placeholder(R.drawable.default_banner)
                        .load(instance.thumbnail)
                        .into(binding.backgroundImage);

                binding.name.setText(instance.title);

                if (instance.description == null || instance.description.trim().length() == 0)
                    binding.description.setText(getString(R.string.instance_no_description));
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    binding.description.setText(Html.fromHtml(instance.description, Html.FROM_HTML_MODE_LEGACY));
                else
                    binding.description.setText(Html.fromHtml(instance.description));

                binding.version.setText(instance.version);

                binding.uri.setText(instance.uri);

                if (instance.email == null) {
                    binding.contact.setVisibility(View.GONE);
                } else {
                    binding.contact.setVisibility(View.VISIBLE);
                }

                binding.contact.setOnClickListener(v -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", instance.email, null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Mastodon] - " + instance.uri);
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
                });

                binding.instanceData.setVisibility(View.VISIBLE);
            }
        });

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
