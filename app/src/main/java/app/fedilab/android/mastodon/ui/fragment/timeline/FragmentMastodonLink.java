package app.fedilab.android.mastodon.ui.fragment.timeline;
/* Copyright 2025 Thomas Schneider
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
import app.fedilab.android.mastodon.activities.SearchResultTabActivity;
import app.fedilab.android.mastodon.client.entities.api.Link;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.LinkAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonLink extends Fragment {


    private FragmentPaginationBinding binding;
    private LinkAdapter linkAdapter;
    private Integer offset;
    private boolean flagLoading;
    private List<Link> linkList;
    private String remoteInstance;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

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
        offset = 0;
        flagLoading = false;
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setEnabled(false);
        Bundle args = getArguments();
        if (args != null) {
            PinnedTimeline pinnedTimeline = (PinnedTimeline) args.getSerializable(Helper.ARG_REMOTE_INSTANCE);
            if (pinnedTimeline != null && pinnedTimeline.remoteInstance != null) {
                remoteInstance = pinnedTimeline.remoteInstance.host;
            }
        }
        router();
    }

    /**
     * Router for timelines
     */
    private void router() {
        TimelinesVM timelinesVM = new ViewModelProvider(FragmentMastodonLink.this).get(TimelinesVM.class);
        String token = remoteInstance != null ? null : BaseMainActivity.currentToken;
        String instance = remoteInstance != null ? remoteInstance : BaseMainActivity.currentInstance;
        timelinesVM.getLinksTrends(token, instance, offset, MastodonHelper.SEARCH_PER_CALL)
                .observe(getViewLifecycleOwner(), links -> {
                    if (links != null && offset == 0) {
                        initializeLinkCommonView(links);
                    } else if (links != null) {
                        dealWithPaginationTag(links);
                    }
                });
    }

    public void scrollToTop() {
        binding.recyclerView.setAdapter(linkAdapter);
    }

    private void dealWithPaginationTag(final List<Link> links) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (links == null || links.isEmpty()) {
            flagLoading = true;
            binding.loadingNextElements.setVisibility(View.GONE);
            return;
        }
        offset += MastodonHelper.SEARCH_PER_CALL;
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        int start = linkList.size();
        linkList.addAll(links);
        linkAdapter.notifyItemRangeInserted(start, links.size());
    }

    /**
     * Initialize the view for links
     *
     * @param links List of {@link Link}
     */
    private void initializeLinkCommonView(final List<Link> links) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        linkList = new ArrayList<>();
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            router();
        });
        if (links == null || links.isEmpty()) {
            if (requireActivity() instanceof SearchResultTabActivity) {
                ((SearchResultTabActivity) requireActivity()).tagEmpty = true;
                if (((SearchResultTabActivity) requireActivity()).accountEmpty != null) {
                    if (((SearchResultTabActivity) requireActivity()).accountEmpty) {
                        ((SearchResultTabActivity) requireActivity()).moveToMessage();
                    } else {
                        ((SearchResultTabActivity) requireActivity()).moveToAccount();
                    }
                }

            }
            binding.recyclerView.setVisibility(View.GONE);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_tags);
            return;
        }
        offset += MastodonHelper.SEARCH_PER_CALL;
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.noAction.setVisibility(View.GONE);
        linkList.addAll(links);
        linkAdapter = new LinkAdapter(linkList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(linkAdapter);
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();

                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            router();
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

}