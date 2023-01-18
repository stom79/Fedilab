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


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityInstanceProfileBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import es.dmoral.toasty.Toasty;

public class InstanceProfileActivity extends DialogFragment {

    private String instance;
    private ActivityInstanceProfileBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityInstanceProfileBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setView(binding.getRoot());

        Dialog dialog = materialAlertDialogBuilder.create();

        Bundle b = getArguments();
        if (b != null)
            instance = b.getString(Helper.ARG_INSTANCE, null);
        if (instance == null) {
            requireDialog().dismiss();
        }

        binding.close.setOnClickListener(v -> requireDialog().dismiss());

        NodeInfoVM nodeInfoVM = new ViewModelProvider(InstanceProfileActivity.this).get(NodeInfoVM.class);
        nodeInfoVM.getNodeInfo(instance).observe(InstanceProfileActivity.this, nodeInfo -> {
            if (nodeInfo == null) {
                Toasty.error(requireContext(), getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                requireDialog().dismiss();
                return;
            }
            binding.name.setText(instance);
            binding.userCount.setText(Helper.withSuffix((nodeInfo.usage.users.total)));
            binding.statusCount.setText(Helper.withSuffix(((nodeInfo.usage.localPosts))));
            String softwareStr = nodeInfo.software.name + " - ";
            binding.software.setText(softwareStr);
            binding.version.setText(nodeInfo.software.version);
            binding.loader.setVisibility(View.GONE);
        });

        return dialog;
    }
}
