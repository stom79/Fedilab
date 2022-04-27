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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Tag;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.ui.drawer.TagAdapter;
import app.fedilab.android.viewmodel.mastodon.SearchVM;


public class FragmentMastodonTag extends Fragment {


    private FragmentPaginationBinding binding;
    private TagAdapter tagAdapter;
    private String search;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            search = getArguments().getString(Helper.ARG_SEARCH_KEYWORD, null);
        }

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        router();
    }

    /**
     * Router for timelines
     */
    private void router() {
        if (search != null) {
            SearchVM searchVM = new ViewModelProvider(FragmentMastodonTag.this).get(SearchVM.class);
            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, "hashtags", false, true, false, 0, null, null, MastodonHelper.STATUSES_PER_CALL)
                    .observe(getViewLifecycleOwner(), results -> {
                        if (results != null && results.hashtags != null) {
                            initializeTagCommonView(results.hashtags);
                        }
                    });
        }
    }

    public void scrollToTop() {
        binding.recyclerView.setAdapter(tagAdapter);
    }

    /**
     * Intialize the view for tags
     *
     * @param tags List of {@link Tag}
     */
    private void initializeTagCommonView(final List<Tag> tags) {
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            router();
        });
        if (tags == null || tags.size() == 0) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_tags);
            return;
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.noAction.setVisibility(View.GONE);
        }
        tagAdapter = new TagAdapter(tags);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(tagAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        tagAdapter = null;
        binding = null;
    }
}