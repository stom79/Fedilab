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
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Tag;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.ui.drawer.TagAdapter;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonTag extends Fragment {


    private FragmentPaginationBinding binding;
    private TagAdapter tagAdapter;
    private String search;
    private Timeline.TimeLineEnum timelineType;
    private Integer offset;
    private boolean flagLoading;
    private List<Tag> tagList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            search = getArguments().getString(Helper.ARG_SEARCH_KEYWORD, null);
            timelineType = (Timeline.TimeLineEnum) getArguments().get(Helper.ARG_TIMELINE_TYPE);
        }

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
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
        router();
    }

    /**
     * Router for timelines
     */
    private void router() {
        if (search != null && timelineType == null) {
            SearchVM searchVM = new ViewModelProvider(FragmentMastodonTag.this).get(SearchVM.class);
            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, "hashtags", false, true, false, offset, null, null, MastodonHelper.SEARCH_PER_CALL)
                    .observe(getViewLifecycleOwner(), results -> {
                        if (results != null && results.hashtags != null && offset == 0) {
                            initializeTagCommonView(results.hashtags);
                        } else if (results != null && results.hashtags != null) {
                            dealWithPaginationTag(results.hashtags);
                        }
                    });
        } else if (timelineType == Timeline.TimeLineEnum.TREND_TAG) {
            TimelinesVM timelinesVM = new ViewModelProvider(FragmentMastodonTag.this).get(TimelinesVM.class);
            timelinesVM.getTagsTrends(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, offset, MastodonHelper.SEARCH_PER_CALL)
                    .observe(getViewLifecycleOwner(), tags -> {
                        if (tags != null && offset == 0) {
                            initializeTagCommonView(tags);
                        } else if (tags != null) {
                            dealWithPaginationTag(tags);
                        }
                    });
        }
    }

    public void scrollToTop() {
        binding.recyclerView.setAdapter(tagAdapter);
    }

    private void dealWithPaginationTag(final List<Tag> tags) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (tags == null || tags.size() == 0) {
            flagLoading = true;
            binding.loadingNextElements.setVisibility(View.GONE);
            return;
        }
        offset += MastodonHelper.SEARCH_PER_CALL;
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        int start = tagList.size();
        tagList.addAll(tags);
        tagAdapter.notifyItemRangeInserted(start, tags.size());
    }

    /**
     * Intialize the view for tags
     *
     * @param tags List of {@link Tag}
     */
    private void initializeTagCommonView(final List<Tag> tags) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        tagList = new ArrayList<>();
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
        }
        if (search != null) {
            Collections.sort(tags, (obj1, obj2) -> Integer.compare(obj2.getWeight(), obj1.getWeight()));
            boolean isInCollection = false;
            for (Tag tag : tags) {
                if (tag.name.compareToIgnoreCase(search) == 0) {
                    isInCollection = true;
                    break;
                }
            }
            if (!isInCollection) {
                Tag tag = new Tag();
                tag.name = search;
                tag.history = new ArrayList<>();
                tags.add(0, tag);
            }
        }
        offset += MastodonHelper.SEARCH_PER_CALL;
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.noAction.setVisibility(View.GONE);
        tagList.addAll(tags);
        tagAdapter = new TagAdapter(tagList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(tagAdapter);
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