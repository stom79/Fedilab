package app.fedilab.android.ui.fragment;
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import app.fedilab.android.databinding.FragmentLoginJoinBinding;
import app.fedilab.android.mastodon.helper.Helper;


public class FragmentLoginJoin extends Fragment {


    private FragmentLoginJoinBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLoginJoinBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.joinMastodon.setOnClickListener(v -> {
            Helper.addFragment(
                    getParentFragmentManager(), android.R.id.content, new FragmentLoginPickInstanceMastodon(),
                    null, null, FragmentLoginPickInstanceMastodon.class.getName());
        });

        return root;
    }
}