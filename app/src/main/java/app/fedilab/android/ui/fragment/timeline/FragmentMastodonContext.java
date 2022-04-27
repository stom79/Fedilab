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

import static app.fedilab.android.activities.ContextActivity.expand;

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

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Context;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.DividerDecoration;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.drawer.StatusAdapter;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;


public class FragmentMastodonContext extends Fragment {


    private FragmentPaginationBinding binding;
    private StatusesVM statusesVM;
    private List<Status> statuses;
    private StatusAdapter statusAdapter;
    private Status focusedStatus;
    private Status firstStatus;
    private boolean pullToRefresh;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        focusedStatus = null;
        pullToRefresh = false;
        if (getArguments() != null) {
            focusedStatus = (Status) getArguments().getSerializable(Helper.ARG_STATUS);
        }
        if (focusedStatus == null) {
            requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );

        statusesVM = new ViewModelProvider(FragmentMastodonContext.this).get(StatusesVM.class);
        binding.recyclerView.setNestedScrollingEnabled(true);
        this.statuses = new ArrayList<>();
        focusedStatus.isFocused = true;
        this.statuses.add(focusedStatus);
        statusAdapter = new StatusAdapter(this.statuses, false);
        binding.swipeContainer.setRefreshing(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusAdapter);
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (this.statuses.size() > 0) {
                binding.swipeContainer.setRefreshing(true);
                pullToRefresh = true;
                statusesVM.getContext(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, focusedStatus.id)
                        .observe(getViewLifecycleOwner(), this::initializeContextView);
            }
        });
        if (focusedStatus != null) {
            statusesVM.getContext(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, focusedStatus.id)
                    .observe(getViewLifecycleOwner(), this::initializeContextView);
        }
        return binding.getRoot();
    }

    public void refresh() {
        if (statusAdapter != null && statuses != null) {
            statusAdapter.notifyItemRangeChanged(0, statuses.size());
        }
    }

    public void redraw() {
        if (statusAdapter != null && firstStatus != null) {
            pullToRefresh = true;
            String id;
            if (expand)
                id = firstStatus.id;
            else
                id = focusedStatus.id;
            statusesVM.getContext(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, id)
                    .observe(FragmentMastodonContext.this, this::initializeContextView);
        }
    }


    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextView(final Context context) {

        if (context == null) {
            return;
        }
        if (pullToRefresh) {
            pullToRefresh = false;
            int size = this.statuses.size();
            statuses.clear();
            statusAdapter.notifyItemRangeRemoved(0, size);
            statuses.add(focusedStatus);
        }
        if (context.ancestors.size() > 0) {
            firstStatus = context.ancestors.get(0);
        } else {
            firstStatus = statuses.get(0);
        }
        int statusPosition = context.ancestors.size();
        //Build the array of statuses
        statuses.addAll(0, context.ancestors);
        statusAdapter.notifyItemRangeInserted(0, statusPosition);
        statuses.addAll(statusPosition + 1, context.descendants);
        statusAdapter.notifyItemRangeInserted(statusPosition + 1, context.descendants.size());
        if (binding.recyclerView.getItemDecorationCount() > 0) {
            for (int i = 0; i < binding.recyclerView.getItemDecorationCount(); i++) {
                binding.recyclerView.removeItemDecorationAt(i);
            }
        }
        binding.recyclerView.addItemDecoration(new DividerDecoration(requireActivity(), statuses));
        binding.swipeContainer.setRefreshing(false);
        binding.recyclerView.scrollToPosition(statusPosition);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        statusAdapter = null;
        binding = null;
    }

}