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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentLoginPickInstanceMastodonBinding;
import app.fedilab.android.mastodon.client.entities.api.JoinMastodonInstance;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.InstanceRegAdapter;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.viewmodel.mastodon.JoinInstancesVM;

public class FragmentLoginPickInstanceMastodon extends Fragment implements InstanceRegAdapter.ActionClick {


    private List<JoinMastodonInstance> joinMastodonInstanceList;

    private FragmentLoginPickInstanceMastodonBinding binding;
    private FragmentLoginPickInstanceMastodon currentFragment;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentLoginPickInstanceMastodonBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        String[] categoriesA = {
                getString(R.string.category_general),
                getString(R.string.category_regional),
                getString(R.string.category_art),
                getString(R.string.category_music),
                getString(R.string.category_activism),
                "LGBTQ+",
                getString(R.string.category_games),
                getString(R.string.category_tech),
                getString(R.string.category_furry),
                getString(R.string.category_food),
                getString(R.string.category_custom),

        };
        String[] itemA = {
                "general",
                "regional",
                "art",
                "music",
                "activism",
                "lgbt",
                "games",
                "tech",
                "furry",
                "food",
                "custom"
        };
        ArrayAdapter<String> adcategories = new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, categoriesA);
        currentFragment = this;
        binding.regCategory.setAdapter(adcategories);
        binding.regCategory.setSelection(0);
        //Manage privacies
        binding.regCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (itemA[position].compareTo("custom") != 0) {
                    JoinInstancesVM joinInstancesVM = new ViewModelProvider(requireActivity()).get(JoinInstancesVM.class);
                    joinInstancesVM.getInstances(itemA[position]).observe(requireActivity(), instances -> {
                        joinMastodonInstanceList = instances;
                        if (instances != null) {
                            InstanceRegAdapter instanceRegAdapter = new InstanceRegAdapter(instances);
                            instanceRegAdapter.actionClick = currentFragment;
                            LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
                            binding.regCategoryView.setLayoutManager(mLayoutManager);
                            binding.regCategoryView.setNestedScrollingEnabled(false);
                            binding.regCategoryView.setAdapter(instanceRegAdapter);
                        } else {
                            Helper.sendToastMessage(requireActivity(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                        }
                    });
                } else {
                    binding.regCategory.setSelection(0);
                    Helper.addFragment(
                            getParentFragmentManager(), android.R.id.content, new FragmentLoginRegisterMastodon(),
                            null, null, FragmentLoginRegisterMastodon.class.getName());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return root;
    }


    @Override
    public void instance(int position) {
        if (joinMastodonInstanceList != null) {
            JoinMastodonInstance clickedInstance = joinMastodonInstanceList.get(position);
            Bundle args = new Bundle();
            args.putString("instance", clickedInstance.domain);
            Helper.addFragment(
                    getParentFragmentManager(), android.R.id.content, new FragmentLoginRegisterMastodon(),
                    args, null, FragmentLoginRegisterMastodon.class.getName());
        }
    }

    @Override
    public void trends(int position) {
        if (joinMastodonInstanceList != null) {
            JoinMastodonInstance clickedInstance = joinMastodonInstanceList.get(position);
            Bundle args = new Bundle();
            args.putSerializable(Helper.ARG_REMOTE_INSTANCE_STRING, clickedInstance.domain);
            args.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TREND_MESSAGE_PUBLIC);

            Helper.addFragment(
                    getParentFragmentManager(), android.R.id.content, new FragmentMastodonTimeline(),
                    args, null, FragmentLoginRegisterMastodon.class.getName());
        }
    }
}