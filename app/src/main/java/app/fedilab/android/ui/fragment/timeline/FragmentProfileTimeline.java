package app.fedilab.android.ui.fragment.timeline;
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.databinding.FragmentProfileTimelinesBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.pageadapter.FedilabProfilePageAdapter;

public class FragmentProfileTimeline extends Fragment {

    private Account account;
    private FragmentProfileTimelinesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        if (getArguments() != null) {
            account = (Account) getArguments().getSerializable(Helper.ARG_ACCOUNT);
        }
        binding = FragmentProfileTimelinesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.toots)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.replies)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.media)));
        binding.tabLayout.setTabTextColors(ThemeHelper.getAttColor(requireActivity(), R.attr.mTextColor), ContextCompat.getColor(requireActivity(), R.color.cyanea_accent_dark_reference));
        binding.tabLayout.setTabIconTint(ThemeHelper.getColorStateList(requireActivity()));
        binding.viewpager.setAdapter(new FedilabProfilePageAdapter(requireActivity(), account));
        binding.viewpager.setOffscreenPageLimit(3);
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
