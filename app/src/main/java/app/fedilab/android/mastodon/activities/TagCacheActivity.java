package app.fedilab.android.mastodon.activities;
/* Copyright 2023 Thomas Schneider
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityCamelTagBinding;
import app.fedilab.android.mastodon.client.entities.app.CamelTag;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.ui.drawer.TagsEditAdapter;
import es.dmoral.toasty.Toasty;

public class TagCacheActivity extends DialogFragment {

    private List<String> tags;
    private TagsEditAdapter tagsEditAdapter;

    private ActivityCamelTagBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityCamelTagBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setView(binding.getRoot());

        Dialog dialog = materialAlertDialogBuilder.create();
        tags = new ArrayList<>();

        binding.saveTag.setOnClickListener(v -> {
            if (binding.tagAdd.getText() != null && (binding.tagAdd.getText().toString().trim().replaceAll("#", "").length() > 0)) {
                String tagToInsert = binding.tagAdd.getText().toString().trim().replaceAll("#", "");
                try {
                    boolean isPresent = new CamelTag(requireActivity()).tagExists(tagToInsert);
                    if (isPresent)
                        Toasty.warning(requireActivity(), getString(R.string.tags_already_stored), Toast.LENGTH_LONG).show();
                    else {
                        new CamelTag(requireActivity()).insert(tagToInsert);
                        int position = tags.size();
                        tags.add(tagToInsert);
                        Toasty.success(requireActivity(), getString(R.string.tags_stored), Toast.LENGTH_LONG).show();
                        binding.tagAdd.setText("");
                        tagsEditAdapter.notifyItemInserted(position);
                    }
                } catch (DBException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        dialog.setTitle(R.string.manage_tags);

        new Thread(() -> {
            List<String> tagsTemp = new CamelTag(requireActivity()).getAll();
            requireActivity().runOnUiThread(() -> {
                if (tagsTemp != null)
                    tags = tagsTemp;
                if (tags != null) {
                    tagsEditAdapter = new TagsEditAdapter(tags);
                    binding.tagList.setAdapter(tagsEditAdapter);
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
                    binding.tagList.setLayoutManager(mLayoutManager);
                }
            });
        }).start();

        binding.close.setOnClickListener(v -> requireDialog().dismiss());
        return dialog;
    }


}
