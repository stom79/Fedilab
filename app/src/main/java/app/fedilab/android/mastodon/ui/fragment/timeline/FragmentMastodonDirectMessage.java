package app.fedilab.android.mastodon.ui.fragment.timeline;
/* Copyright 2023 Thomas Schneider
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.FragmentDirectMessageBinding;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.StatusDirectMessageAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;


public class FragmentMastodonDirectMessage extends Fragment {


    public FirstMessage firstMessage;
    private FragmentDirectMessageBinding binding;
    private StatusesVM statusesVM;
    private List<Status> statuses;
    private StatusDirectMessageAdapter statusDirectMessageAdapter;
    //Handle actions that can be done in other fragments
    private Status focusedStatus;
    private Status firstStatus;
    private boolean pullToRefresh;
    private String user_token, user_instance;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        focusedStatus = null;
        pullToRefresh = false;
        if (getArguments() != null) {
            focusedStatus = (Status) getArguments().getSerializable(Helper.ARG_STATUS);
        }
        user_instance = MainActivity.currentInstance;
        user_token = MainActivity.currentToken;

        if (focusedStatus == null) {
            getChildFragmentManager().beginTransaction().remove(this).commit();
        }
        binding = FragmentDirectMessageBinding.inflate(inflater, container, false);
        statusesVM = new ViewModelProvider(FragmentMastodonDirectMessage.this).get(StatusesVM.class);
        binding.recyclerView.setNestedScrollingEnabled(true);
        this.statuses = new ArrayList<>();
        this.statuses.add(focusedStatus);
        statusDirectMessageAdapter = new StatusDirectMessageAdapter(this.statuses);
        binding.swipeContainer.setRefreshing(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusDirectMessageAdapter);
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (this.statuses.size() > 0) {
                binding.swipeContainer.setRefreshing(true);
                pullToRefresh = true;
                statusesVM.getContext(user_instance, user_token, focusedStatus.id)
                        .observe(getViewLifecycleOwner(), this::initializeContextView);
            }
        });
        if (focusedStatus != null) {
            statusesVM.getContext(user_instance, user_token, focusedStatus.id)
                    .observe(getViewLifecycleOwner(), this::initializeContextView);
        }
        return binding.getRoot();
    }


    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextView(final Context context) {

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
            statusDirectMessageAdapter.notifyItemRangeRemoved(0, size);
            statuses.add(focusedStatus);
        }
        if (context.ancestors.size() > 0) {
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
        statusDirectMessageAdapter.notifyItemRangeInserted(0, statusPosition);
        statuses.addAll(statusPosition + 1, context.descendants);
        statusDirectMessageAdapter.notifyItemRangeInserted(statusPosition + 1, context.descendants.size());
        if (binding.recyclerView.getItemDecorationCount() > 0) {
            for (int i = 0; i < binding.recyclerView.getItemDecorationCount(); i++) {
                binding.recyclerView.removeItemDecorationAt(i);
            }
        }
        binding.swipeContainer.setRefreshing(false);
        binding.recyclerView.scrollToPosition(statusPosition);
    }

    public interface FirstMessage {
        void get(Status status);
    }
}