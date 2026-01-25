package app.fedilab.android.mastodon.ui.fragment.timeline;
/* Copyright 2026 Thomas Schneider
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


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonLinkTimeline extends Fragment {

    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private boolean flagLoading;
    private String max_id;
    private StatusAdapter statusAdapter;
    private List<Status> statuses;
    private String url;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        Bundle args = getArguments();
        if (args != null) {
            url = args.getString(Helper.ARG_URL, null);
        }
        if (url == null) {
            return;
        }
        flagLoading = false;
        timelinesVM = new ViewModelProvider(FragmentMastodonLinkTimeline.this).get(TimelinesVM.class);
        loadTimeline(null);
    }

    private void loadTimeline(String max_id) {
        timelinesVM.getLinkTimeline(
                        BaseMainActivity.currentToken,
                        BaseMainActivity.currentInstance,
                        url,
                        max_id,
                        null,
                        null,
                        MastodonHelper.statusesPerCall(requireActivity()))
                .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
    }

    private void initializeStatusesCommonView(final Statuses statuses) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        if (statuses == null || statuses.statuses == null || statuses.statuses.isEmpty()) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_status);
            return;
        }
        binding.recyclerView.setVisibility(View.VISIBLE);
        flagLoading = false;
        if (this.statuses == null) {
            this.statuses = new ArrayList<>();
        }
        int startPosition = this.statuses.size();
        this.statuses.addAll(statuses.statuses);
        if (statuses.pagination != null && statuses.pagination.max_id != null) {
            max_id = statuses.pagination.max_id;
        } else if (!statuses.statuses.isEmpty()) {
            max_id = statuses.statuses.get(statuses.statuses.size() - 1).id;
        }
        if (statusAdapter == null) {
            statusAdapter = new StatusAdapter(this.statuses, null, false, true, false);
            binding.swipeContainer.setOnRefreshListener(() -> {
                binding.swipeContainer.setRefreshing(true);
                this.statuses.clear();
                statusAdapter.notifyDataSetChanged();
                loadTimeline(null);
            });
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
            binding.recyclerView.setLayoutManager(mLayoutManager);
            binding.recyclerView.setAdapter(statusAdapter);
            binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0) {
                        int visibleItemCount = mLayoutManager.getChildCount();
                        int totalItemCount = mLayoutManager.getItemCount();
                        int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
                        if (!flagLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            loadTimeline(max_id);
                        }
                    }
                }
            });
        } else {
            binding.loadingNextElements.setVisibility(View.GONE);
            statusAdapter.notifyItemRangeInserted(startPosition, statuses.statuses.size());
        }
    }
}
