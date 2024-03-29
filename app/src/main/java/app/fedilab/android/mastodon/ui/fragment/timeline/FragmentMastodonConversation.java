package app.fedilab.android.mastodon.ui.fragment.timeline;
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


import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.client.entities.api.Conversation;
import app.fedilab.android.mastodon.client.entities.api.Conversations;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.ConversationAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonConversation extends Fragment implements ConversationAdapter.FetchMoreCallBack {


    public UpdateCounters update;
    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private boolean flagLoading;
    private List<Conversation> conversationList;
    private String max_id, min_id, min_id_fetch_more, max_id_fetch_more;
    private ConversationAdapter conversationAdapter;
    private LinearLayoutManager mLayoutManager;
    private boolean isViewInitialized;
    private Conversations initialConversations;

    //Allow to recreate data when detaching/attaching fragment
    public void recreate() {
        initialConversations = null;
        if (conversationList != null && conversationList.size() > 0) {
            int count = conversationList.size();
            conversationList.clear();
            conversationList = new ArrayList<>();
            if (conversationAdapter != null) {
                conversationAdapter.notifyItemRangeRemoved(0, count);
                max_id = null;
                flagLoading = false;
                route(null, false);
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);
        isViewInitialized = false;
        return binding.getRoot();
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing) {
        route(direction, fetchingMissing, null);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!isViewInitialized) {
            isViewInitialized = true;
            if (initialConversations != null) {
                initializeConversationCommonView(initialConversations);
            } else {
                route(null, false);
            }
        }
        if (conversationList != null && conversationList.size() > 0) {
            route(FragmentMastodonTimeline.DIRECTION.FETCH_NEW, true);
        }
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, Conversation conversationToUpdate) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (!isAdded()) {
            return;
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean useCache = sharedpreferences.getBoolean(getString(R.string.SET_USE_CACHE), true);

        TimelinesVM.TimelineParams timelineParams = new TimelinesVM.TimelineParams(requireActivity(), Timeline.TimeLineEnum.NOTIFICATION, direction, null);
        timelineParams.limit = MastodonHelper.notificationsPerCall(requireActivity());
        if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH || direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP) {
            timelineParams.maxId = null;
            timelineParams.minId = null;
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            timelineParams.maxId = fetchingMissing ? max_id_fetch_more : max_id;
            timelineParams.minId = null;
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            timelineParams.minId = fetchingMissing ? min_id_fetch_more : min_id;
            timelineParams.maxId = null;
        } else {
            timelineParams.maxId = max_id;
        }

        timelineParams.fetchingMissing = fetchingMissing;

        if (useCache && direction != FragmentMastodonTimeline.DIRECTION.SCROLL_TOP && direction != FragmentMastodonTimeline.DIRECTION.FETCH_NEW) {
            getCachedConversations(direction, fetchingMissing, timelineParams);
        } else {
            getLiveConversations(direction, fetchingMissing, timelineParams, conversationToUpdate);
        }
    }

    private void getCachedConversations(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams) {

        if (direction == null) {
            timelinesVM.getConversationsCache(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsCached -> {
                        if (conversationsCached == null || conversationsCached.conversations == null || conversationsCached.conversations.size() == 0) {
                            getLiveConversations(null, fetchingMissing, timelineParams, null);
                        } else {
                            initialConversations = conversationsCached;
                            initializeConversationCommonView(conversationsCached);
                        }
                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            timelinesVM.getConversationsCache(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsBottom -> {
                        if (conversationsBottom == null || conversationsBottom.conversations == null || conversationsBottom.conversations.size() == 0) {
                            getLiveConversations(FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(conversationsBottom, FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, null);
                        }

                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            timelinesVM.getConversationsCache(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsTop -> {
                        if (conversationsTop == null || conversationsTop.conversations == null || conversationsTop.conversations.size() == 0) {
                            getLiveConversations(FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(conversationsTop, FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, null);
                        }
                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH) {
            timelinesVM.getConversationsCache(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), notificationsRefresh -> {
                        if (notificationsRefresh == null || notificationsRefresh.conversations == null || notificationsRefresh.conversations.size() == 0) {
                            getLiveConversations(direction, fetchingMissing, timelineParams, null);
                        } else {
                            if (conversationAdapter != null) {
                                dealWithPagination(notificationsRefresh, FragmentMastodonTimeline.DIRECTION.REFRESH, true, null);
                            } else {
                                initializeConversationCommonView(notificationsRefresh);
                            }
                        }

                    });
        }
    }

    private void getLiveConversations(FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams, Conversation conversationToUpdate) {
        if (direction == null) {
            timelinesVM.getConversations(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversations -> {
                        initialConversations = conversations;
                        initializeConversationCommonView(conversations);
                    });
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            timelinesVM.getConversations(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsBottom -> dealWithPagination(conversationsBottom, FragmentMastodonTimeline.DIRECTION.BOTTOM, fetchingMissing, conversationToUpdate));
        } else if (direction == FragmentMastodonTimeline.DIRECTION.TOP) {
            timelinesVM.getConversations(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsTop -> dealWithPagination(conversationsTop, FragmentMastodonTimeline.DIRECTION.TOP, fetchingMissing, conversationToUpdate));
        } else if (direction == FragmentMastodonTimeline.DIRECTION.REFRESH || direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP || direction == FragmentMastodonTimeline.DIRECTION.FETCH_NEW) {
            timelinesVM.getConversations(conversationList, timelineParams)
                    .observe(getViewLifecycleOwner(), conversationsRefresh -> {
                        if (conversationAdapter != null) {
                            dealWithPagination(conversationsRefresh, direction, true, conversationToUpdate);
                        } else {
                            initializeConversationCommonView(conversationsRefresh);
                        }
                    });
        }
    }


    private void storeMarker(BaseAccount connectedAccount) {
        if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            if (conversationList != null && conversationList.size() > position) {
                try {
                    Conversation conversation = conversationList.get(position);
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(getString(R.string.SET_INNER_MARKER) + connectedAccount.user_id + connectedAccount.instance + Timeline.TimeLineEnum.CONVERSATION, conversation.id);
                    editor.apply();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initialConversations = null;
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        timelinesVM = new ViewModelProvider(FragmentMastodonConversation.this).get(TimelinesVM.class);
        max_id = null;

    }

    /**
     * Intialize the view for conversations
     *
     * @param conversations {@link Conversations}
     */
    private void initializeConversationCommonView(final Conversations conversations) {
        flagLoading = false;
        if (!isViewInitialized) {
            return;
        }
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }

        RecyclerView.ItemAnimator animator = binding.recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            route(FragmentMastodonTimeline.DIRECTION.REFRESH, true);
        });
        if (conversations == null || conversations.conversations == null || conversations.conversations.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            return;
        } else {
            binding.noAction.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
        flagLoading = conversations.pagination.max_id == null;

        if (conversationAdapter != null && conversationList != null) {
            int size = conversationList.size();
            conversationList.clear();
            conversationList = new ArrayList<>();
            conversationAdapter.notifyItemRangeRemoved(0, size);
        }
        if (conversationList == null) {
            conversationList = new ArrayList<>();
        }
        conversationList.addAll(conversations.conversations);

        if (max_id == null || (conversations.pagination.max_id != null && Helper.compareTo(conversations.pagination.max_id, max_id) < 0)) {
            max_id = conversations.pagination.max_id;
        }
        if (min_id == null || (conversations.pagination.min_id != null && Helper.compareTo(conversations.pagination.min_id, min_id) > 0)) {
            min_id = conversations.pagination.min_id;
        }

        conversationAdapter = new ConversationAdapter(conversationList);
        conversationAdapter.fetchMoreCallBack = this;
        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(conversationAdapter);

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (requireActivity() instanceof BaseMainActivity) {
                    if (dy < 0 && !((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(true);
                    if (dy > 0 && ((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(false);
                }
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();

                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            route(FragmentMastodonTimeline.DIRECTION.BOTTOM, false);
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                } else if (firstVisibleItem == 0) { //Scroll top and item is zero
                    if (!flagLoading) {
                        flagLoading = true;
                        binding.loadingNextElements.setVisibility(View.VISIBLE);
                        route(FragmentMastodonTimeline.DIRECTION.TOP, false);
                    }
                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        if (isAdded()) {
            storeMarker(Helper.getCurrentAccount(requireActivity()));
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        storeMarker(Helper.getCurrentAccount(requireActivity()));
        super.onPause();
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_conversations Conversations
     */
    private synchronized void dealWithPagination(Conversations fetched_conversations, FragmentMastodonTimeline.DIRECTION direction, boolean fetchingMissing, Conversation conversationToUpdate) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (conversationList != null && fetched_conversations != null && fetched_conversations.conversations != null && fetched_conversations.conversations.size() > 0) {
            try {
                if (conversationToUpdate != null) {
                    new Thread(() -> {
                        StatusCache statusCache = new StatusCache();
                        statusCache.instance = BaseMainActivity.currentInstance;
                        statusCache.user_id = BaseMainActivity.currentUserID;
                        conversationToUpdate.isFetchMore = false;
                        statusCache.conversation = conversationToUpdate;
                        statusCache.status_id = conversationToUpdate.id;
                        try {
                            new StatusCache(requireActivity()).updateIfExists(statusCache);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception ignored) {
            }

            flagLoading = fetched_conversations.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);
            //Update the timeline with new statuses
            int insertedConversations = updateConversationListWith(fetched_conversations.conversations);
            //For these directions, the app will display counters for new messages
            if (insertedConversations >= 0 && update != null && (direction == FragmentMastodonTimeline.DIRECTION.FETCH_NEW || direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP || direction == FragmentMastodonTimeline.DIRECTION.REFRESH)) {
                update.onUpdateConversation(insertedConversations);
            }
            if (direction == FragmentMastodonTimeline.DIRECTION.TOP && fetchingMissing) {
                binding.recyclerView.scrollToPosition(getPosition(fetched_conversations.conversations.get(fetched_conversations.conversations.size() - 1)) + 1);
            }
            if (!fetchingMissing) {
                if (fetched_conversations.pagination.max_id == null) {
                    flagLoading = true;
                } else if (max_id == null || Helper.compareTo(fetched_conversations.pagination.max_id, max_id) < 0) {
                    max_id = fetched_conversations.pagination.max_id;
                }
                if (min_id == null || (fetched_conversations.pagination.min_id != null && Helper.compareTo(fetched_conversations.pagination.min_id, min_id) > 0)) {
                    min_id = fetched_conversations.pagination.min_id;
                }
            }
        } else if (direction == FragmentMastodonTimeline.DIRECTION.BOTTOM) {
            flagLoading = true;
        }
        if (direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP) {
            new Handler().postDelayed(() -> binding.recyclerView.scrollToPosition(0), 200);
        }
    }


    /**
     * Return the position of the convnersation in the ArrayList
     *
     * @param conversation - Conversation to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Conversation conversation) {
        int position = 0;
        boolean found = false;
        for (Conversation _conversation : conversationList) {
            if (conversation != null && _conversation.id.compareTo(conversation.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }

    /**
     * Update the timeline with received Conversations
     *
     * @param conversationsReceived - List<Conversation> Conversation received
     */
    private int updateConversationListWith(List<Conversation> conversationsReceived) {
        int insertedConversations = 0;
        if (conversationsReceived != null && conversationsReceived.size() > 0) {
            for (Conversation conversationReceived : conversationsReceived) {
                int position = 0;
                //We loop through messages already in the timeline
                if (conversationList != null) {
                    conversationAdapter.notifyItemRangeChanged(0, conversationList.size());
                    for (Conversation conversationsAlreadyPresent : conversationList) {
                        //We compare the date of each status and we only add status having a date greater than the another, it is inserted at this position
                        //Pinned messages are ignored because their date can be older
                        if (Helper.compareTo(conversationReceived.id, conversationsAlreadyPresent.id) > 0) {
                            if (!conversationList.contains(conversationReceived)) {
                                conversationList.add(position, conversationReceived);
                                conversationAdapter.notifyItemInserted(position);
                                if (!conversationReceived.cached) {
                                    insertedConversations++;
                                }
                            }
                            break;
                        }
                        position++;
                    }
                    //Statuses added at the bottom, we flag them by position = -2 for not dealing with them and fetch more
                    if (position == conversationList.size() && !conversationList.contains(conversationReceived)) {
                        conversationList.add(position, conversationReceived);
                        conversationAdapter.notifyItemInserted(position);
                    }
                }
            }
        }
        return insertedConversations;
    }


    public void scrollToTop() {
        if (binding != null) {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            route(FragmentMastodonTimeline.DIRECTION.SCROLL_TOP, true);
        }
    }

    @Override
    public void onClickMinId(String min_id, Conversation conversationToUpdate) {
        min_id_fetch_more = min_id;
        route(FragmentMastodonTimeline.DIRECTION.TOP, true, conversationToUpdate);
    }

    @Override
    public void onClickMaxId(String max_id, Conversation conversationToUpdate) {
        max_id_fetch_more = max_id;
        route(FragmentMastodonTimeline.DIRECTION.BOTTOM, true, conversationToUpdate);
    }

    public interface UpdateCounters {
        void onUpdateConversation(int count);
    }
}