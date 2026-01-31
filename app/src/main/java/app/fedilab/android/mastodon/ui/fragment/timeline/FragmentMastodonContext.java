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


import static app.fedilab.android.mastodon.activities.ContextActivity.displayCW;
import static app.fedilab.android.mastodon.activities.ContextActivity.expand;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.SeenComments;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.CommentDecorationHelper;
import app.fedilab.android.mastodon.helper.DividerDecoration;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.misskey.viewmodel.MisskeyStatusesVM;


public class FragmentMastodonContext extends Fragment {


    public FirstMessage firstMessage;
    private FragmentPaginationBinding binding;
    private StatusesVM statusesVM;
    private MisskeyStatusesVM misskeyStatusesVM;
    private boolean isMisskey;
    private List<Status> statuses;
    private StatusAdapter statusAdapter;
    //Handle actions that can be done in other fragments
    private final BroadcastReceiver receive_action = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            Bundle args = intent.getExtras();
            if (args != null && isAdded()) {
                long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
                new CachedBundle(requireActivity()).getBundle(bundleId, Helper.getCurrentAccount(requireActivity()), bundle -> {
                    Status receivedStatus = (Status) bundle.getSerializable(Helper.ARG_STATUS_ACTION);
                    String delete_statuses_for_user = bundle.getString(Helper.ARG_STATUS_ACCOUNT_ID_DELETED);
                    Status status_to_delete = (Status) bundle.getSerializable(Helper.ARG_STATUS_DELETED);
                    Status statusPosted = (Status) bundle.getSerializable(Helper.ARG_STATUS_POSTED);
                    Status status_to_update = (Status) bundle.getSerializable(Helper.ARG_STATUS_UPDATED);
                    if (receivedStatus != null && statusAdapter != null) {
                        int position = getPosition(receivedStatus);
                        if (position >= 0) {
                            statuses.get(position).reblog = receivedStatus.reblog;
                            statuses.get(position).reblogged = receivedStatus.reblogged;
                            statuses.get(position).favourited = receivedStatus.favourited;
                            statuses.get(position).bookmarked = receivedStatus.bookmarked;
                            statuses.get(position).reblogs_count = receivedStatus.reblogs_count;
                            statuses.get(position).favourites_count = receivedStatus.favourites_count;
                            statusAdapter.notifyItemChanged(position);
                        }
                    } else if (delete_statuses_for_user != null && statusAdapter != null) {
                        List<Status> statusesToRemove = new ArrayList<>();
                        for (Status status : statuses) {
                            if (status.account.id.equals(delete_statuses_for_user)) {
                                statusesToRemove.add(status);
                            }
                        }
                        for (Status statusToRemove : statusesToRemove) {
                            int position = getPosition(statusToRemove);
                            if (position >= 0) {
                                statuses.remove(position);
                                statusAdapter.notifyItemRemoved(position);
                            }
                        }
                    } else if (status_to_delete != null && statusAdapter != null) {
                        int position = getPosition(status_to_delete);
                        if (position >= 0) {
                            statuses.remove(position);
                            statusAdapter.notifyItemRemoved(position);
                        }
                    } else if (status_to_update != null && statusAdapter != null) {
                        int position = getPosition(status_to_update);
                        if (position >= 0) {
                            statuses.set(position, status_to_update);
                            statusAdapter.notifyItemChanged(position);
                        }
                    } else if (statusPosted != null && statusAdapter != null) {
                        if (isAdded() && requireActivity() instanceof ContextActivity) {
                            int i = 0;
                            for (Status status : statuses) {
                                if (status.id.equals(statusPosted.in_reply_to_id)) {
                                    statuses.add((i + 1), statusPosted);
                                    statusAdapter.notifyItemInserted((i + 1));
                                    if (requireActivity() instanceof ContextActivity) {
                                        //Redraw decorations
                                        statusAdapter.notifyItemRangeChanged(0, statuses.size());
                                    }
                                    break;
                                }
                                i++;
                            }
                        }
                    }
                });

            }
        }
    };
    private boolean refresh;
    private Status focusedStatus;
    private String remote_instance, focusedStatusURI;
    private Status firstStatus;
    private boolean pullToRefresh;
    private String user_token, user_instance;
    private Bundle arguments;

    /**
     * Return the position of the status in the ArrayList
     *
     * @param status - Status to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Status status) {
        int position = 0;
        boolean found = false;
        for (Status _status : statuses) {
            if (_status.id.compareTo(status.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        focusedStatus = null;
        pullToRefresh = false;
        focusedStatusURI = null;
        refresh = true;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        arguments = getArguments();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (arguments != null) {
            long bundleId = arguments.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(requireActivity()).getBundle(bundleId, Helper.getCurrentAccount(requireActivity()), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {
        if (bundle != null) {
            focusedStatus = (Status) bundle.getSerializable(Helper.ARG_STATUS);
            remote_instance = bundle.getString(Helper.ARG_REMOTE_INSTANCE, null);
            focusedStatusURI = bundle.getString(Helper.ARG_FOCUSED_STATUS_URI, null);
        }
        if (remote_instance != null) {
            user_instance = remote_instance;
            user_token = null;
        } else {
            user_instance = MainActivity.currentInstance;
            user_token = MainActivity.currentToken;
        }
        if (focusedStatus == null) {
            getChildFragmentManager().beginTransaction().remove(this).commit();
        }


        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);
        statusesVM = new ViewModelProvider(FragmentMastodonContext.this).get(StatusesVM.class);
        Account.API currentApi = BaseMainActivity.api;
        if (currentApi == null && Helper.getCurrentAccount(requireActivity()) != null) {
            currentApi = Helper.getCurrentAccount(requireActivity()).api;
        }
        isMisskey = currentApi == Account.API.MISSKEY && remote_instance == null;
        if (isMisskey) {
            misskeyStatusesVM = new ViewModelProvider(FragmentMastodonContext.this).get(MisskeyStatusesVM.class);
        }
        binding.recyclerView.setNestedScrollingEnabled(true);
        this.statuses = new ArrayList<>();
        focusedStatus.isFocused = true;
        this.statuses.add(focusedStatus);
        statusAdapter = new StatusAdapter(this.statuses, Timeline.TimeLineEnum.UNKNOWN, false, true, remote_instance != null);
        binding.swipeContainer.setRefreshing(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusAdapter);
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (!this.statuses.isEmpty() && !refresh) {
                binding.swipeContainer.setRefreshing(true);
                pullToRefresh = true;
                getContextLiveData(focusedStatus.id)
                        .observe(getViewLifecycleOwner(), this::initializeContextView);
            }
        });
        if (focusedStatus != null) {
            getContextLiveData(focusedStatus.id)
                    .observe(getViewLifecycleOwner(), this::initializeContextView);
        }

        ContextCompat.registerReceiver(requireActivity(), receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);
    }


    public void refresh() {
        if (statuses != null) {
            for (Status status : statuses) {
                status.isExpended = displayCW;
            }
            if (statusAdapter != null) {
                statusAdapter.notifyItemRangeChanged(0, statuses.size());
            }
        }
    }

    public void redraw() {
        if (statusAdapter != null && firstStatus != null) {
            pullToRefresh = true;
            String id;
            if (expand) {
                id = firstStatus.id;
            } else {
                id = focusedStatus.id;
            }
            getContextLiveData(id)
                    .observe(FragmentMastodonContext.this, this::initializeContextView);
        }
    }

    private androidx.lifecycle.LiveData<Context> getContextLiveData(String statusId) {
        if (isMisskey && misskeyStatusesVM != null) {
            return misskeyStatusesVM.getContext(user_instance, user_token, statusId);
        }
        return statusesVM.getContext(user_instance, user_token, statusId);
    }


    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextView(final Context context) {
        refresh = false;
        if (context == null) {
            Helper.sendToastMessage(requireActivity(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
            return;
        }
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (pullToRefresh) {
            pullToRefresh = false;
            int size = this.statuses.size();
            statuses.clear();
            statusAdapter.notifyItemRangeRemoved(0, size);
            statuses.add(focusedStatus);
        }
        if (!context.ancestors.isEmpty()) {
            firstStatus = context.ancestors.get(0);
        } else {
            firstStatus = statuses.get(0);
        }
        if (firstMessage != null) {
            firstMessage.get(firstStatus);
        }

        int statusPosition = context.ancestors.size();
        //Build the array of statuses
        statuses.addAll(0, context.ancestors);
        statusAdapter.notifyItemRangeInserted(0, statusPosition);
        List<String> allParentIds = new ArrayList<>();
        for (Status ancestor : context.ancestors) {
            allParentIds.add(ancestor.id);
        }
        allParentIds.add(focusedStatus.id);
        List<Status> sortedDescendants = CommentDecorationHelper.sortDescendantsAsTree(context.descendants, allParentIds);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean highlightNewComments = sharedpreferences.getBoolean(getString(R.string.SET_HIGHLIGHT_NEW_COMMENTS), true);
        if (highlightNewComments) try {
            SeenComments seenCommentsDAO = new SeenComments(requireActivity());
            BaseAccount currentAccount = Helper.getCurrentAccount(requireActivity());
            if (currentAccount != null) {
                List<String> currentDescendantIds = new ArrayList<>();
                for (Status descendant : sortedDescendants) {
                    currentDescendantIds.add(descendant.id);
                }
                SeenComments previouslySeen = seenCommentsDAO.getSeenComments(currentAccount, focusedStatus.id);
                if (previouslySeen != null && previouslySeen.descendant_ids != null) {
                    Set<String> seenSet = new HashSet<>(previouslySeen.descendant_ids);
                    for (Status descendant : sortedDescendants) {
                        if (!seenSet.contains(descendant.id)) {
                            descendant.isNewComment = true;
                        }
                    }
                }
                seenCommentsDAO.insertOrUpdate(currentAccount, focusedStatus.id, currentDescendantIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        statuses.addAll(statusPosition + 1, sortedDescendants);
        statusAdapter.notifyItemRangeInserted(statusPosition + 1, sortedDescendants.size());
        if (binding.recyclerView.getItemDecorationCount() > 0) {
            for (int i = 0; i < binding.recyclerView.getItemDecorationCount(); i++) {
                binding.recyclerView.removeItemDecorationAt(i);
            }
        }
        binding.recyclerView.addItemDecoration(new DividerDecoration(requireActivity(), statuses));
        binding.swipeContainer.setRefreshing(false);
        if (focusedStatusURI == null) {
            binding.recyclerView.scrollToPosition(statusPosition);
        } else {
            int position = 0;
            boolean found = false;
            for (Status status : statuses) {
                if (status.uri.compareToIgnoreCase(focusedStatusURI) == 0) {
                    found = true;
                    break;
                }
                position++;
            }
            if (found) {
                binding.recyclerView.scrollToPosition(position);
                statuses.get(0).isFocused = false;
                statuses.get(position).isFocused = true;
                statusAdapter.notifyItemChanged(0);
                statusAdapter.notifyItemChanged(position);
            }
        }
    }

    @Override
    public void onDestroyView() {
        try {
            requireActivity().unregisterReceiver(receive_action);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        super.onDestroyView();
    }


    public interface FirstMessage {
        void get(Status status);
    }
}