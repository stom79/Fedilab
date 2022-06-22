package app.fedilab.android.ui.fragment.timeline;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Conversation;
import app.fedilab.android.client.entities.api.Conversations;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.ConversationAdapter;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonConversation extends Fragment {


    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private FragmentMastodonConversation currentFragment;
    private boolean flagLoading;
    private List<Conversation> conversations;
    private String max_id;
    private ConversationAdapter conversationAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        currentFragment = this;
        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));
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
        timelinesVM = new ViewModelProvider(FragmentMastodonConversation.this).get(TimelinesVM.class);
        max_id = null;
        timelinesVM.getConversations(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                .observe(getViewLifecycleOwner(), this::initializeConversationCommonView);
    }

    /**
     * Intialize the view for conversations
     *
     * @param conversations {@link Conversations}
     */
    private void initializeConversationCommonView(final Conversations conversations) {
        flagLoading = false;
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        if (conversationAdapter != null && this.conversations != null) {
            int size = this.conversations.size();
            this.conversations.clear();
            this.conversations = new ArrayList<>();
            conversationAdapter.notifyItemRangeRemoved(0, size);
        }
        binding.recyclerView.setVisibility(View.VISIBLE);
        this.conversations = conversations.conversations;
        conversationAdapter = new ConversationAdapter(this.conversations);
        //conversationAdapter.itemListener = currentFragment;
        binding.swipeContainer.setRefreshing(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(conversationAdapter);
        max_id = conversations.pagination.max_id;
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
                            timelinesVM.getConversations(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(FragmentMastodonConversation.this, fetched_conversations -> {
                                        binding.loadingNextElements.setVisibility(View.GONE);
                                        if (currentFragment.conversations != null && fetched_conversations != null) {
                                            int startId = 0;
                                            flagLoading = fetched_conversations.pagination.max_id == null;
                                            //There are some statuses present in the timeline
                                            if (currentFragment.conversations.size() > 0) {
                                                startId = currentFragment.conversations.size();
                                            }
                                            currentFragment.conversations.addAll(fetched_conversations.conversations);
                                            max_id = fetched_conversations.pagination.max_id;
                                            conversationAdapter.notifyItemRangeInserted(startId, fetched_conversations.conversations.size());
                                        }
                                    });
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }

            }
        });
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (this.conversations.size() > 0) {
                binding.swipeContainer.setRefreshing(true);
                max_id = null;
                flagLoading = false;
                timelinesVM.getConversations(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(FragmentMastodonConversation.this, this::initializeConversationCommonView);
            }
        });

    }

    public void scrollToTop() {
        binding.recyclerView.scrollToPosition(0);
    }

}