package app.fedilab.android.mastodon.ui.fragment.timeline;
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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Collections;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentScheduledBinding;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.app.ScheduledBoost;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusScheduledAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.misskey.viewmodel.MisskeyStatusesVM;

public class FragmentScheduled extends Fragment implements StatusScheduledAdapter.ScheduledActions {

    private FragmentScheduledBinding binding;
    private Timeline.TimeLineEnum type;
    private boolean initialLoadDone = false;
    private boolean sortAscending = true;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScheduledBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            type = (Timeline.TimeLineEnum) getArguments().getSerializable(Helper.ARG_TIMELINE_TYPE);
        }
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!initialLoadDone) {
            initialLoadDone = true;
            return;
        }
        if (type == Timeline.TimeLineEnum.SCHEDULED_TOOT_SERVER) {
            displayScheduledServer();
        } else if (type == Timeline.TimeLineEnum.SCHEDULED_TOOT_CLIENT) {
            displayScheduledDevice();
        } else if (type == Timeline.TimeLineEnum.SCHEDULED_BOOST) {
            displayScheduledBoost();
        }
    }

    private void displayScheduledServer() {
        Account.API currentApi = BaseMainActivity.api;
        if (currentApi == null && Helper.getCurrentAccount(requireActivity()) != null) {
            currentApi = Helper.getCurrentAccount(requireActivity()).api;
        }
        if (currentApi == Account.API.MISSKEY) {
            displayScheduledMisskey();
            return;
        }
        int savedPosition = saveScrollPosition();
        int limit = MastodonHelper.statusesPerCall(requireActivity());
        StatusesVM statusesVM = new ViewModelProvider(requireActivity()).get(StatusesVM.class);
        statusesVM.getScheduledStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, limit)
                .observe(requireActivity(), scheduledStatuses -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledStatuses != null && scheduledStatuses.scheduledStatuses != null && !scheduledStatuses.scheduledStatuses.isEmpty()) {
                        List<ScheduledStatus> statusList = scheduledStatuses.scheduledStatuses;
                        if (statusList.size() < limit) {
                            sortScheduledStatuses(statusList);
                            setupSortButton(statusList, null, null);
                        }
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(statusList, null, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);
                        restoreScrollPosition(linearLayoutManager, savedPosition);
                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void displayScheduledMisskey() {
        int savedPosition = saveScrollPosition();
        int limit = MastodonHelper.statusesPerCall(requireActivity());
        MisskeyStatusesVM misskeyStatusesVM = new ViewModelProvider(requireActivity()).get(MisskeyStatusesVM.class);
        misskeyStatusesVM.getScheduledNotes(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, limit, 0)
                .observe(requireActivity(), scheduledStatuses -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledStatuses != null && scheduledStatuses.scheduledStatuses != null && !scheduledStatuses.scheduledStatuses.isEmpty()) {
                        List<ScheduledStatus> statusList = scheduledStatuses.scheduledStatuses;
                        if (statusList.size() < limit) {
                            sortScheduledStatuses(statusList);
                            setupSortButton(statusList, null, null);
                        }
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(statusList, null, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);
                        restoreScrollPosition(linearLayoutManager, savedPosition);
                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void displayScheduledDevice() {
        int savedPosition = saveScrollPosition();
        new Thread(() -> {
            try {
                List<StatusDraft> scheduledDrafts = new StatusDraft(requireActivity()).geStatusDraftScheduledList(Helper.getCurrentAccount(requireActivity()));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledDrafts != null && !scheduledDrafts.isEmpty()) {
                        sortScheduledDrafts(scheduledDrafts);
                        setupSortButton(null, scheduledDrafts, null);
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(null, scheduledDrafts, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);
                        restoreScrollPosition(linearLayoutManager, savedPosition);
                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                };
                mainHandler.post(myRunnable);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void displayScheduledBoost() {
        int savedPosition = saveScrollPosition();
        new Thread(() -> {
            try {
                List<ScheduledBoost> scheduledBoosts = new ScheduledBoost(requireActivity()).getScheduled(Helper.getCurrentAccount(requireActivity()));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledBoosts != null && !scheduledBoosts.isEmpty()) {
                        sortScheduledBoosts(scheduledBoosts);
                        setupSortButton(null, null, scheduledBoosts);
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(null, null, scheduledBoosts);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);
                        restoreScrollPosition(linearLayoutManager, savedPosition);
                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.noActionText.setText(R.string.no_scheduled_boosts);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                };
                mainHandler.post(myRunnable);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.loader.setVisibility(View.VISIBLE);
        if (type == Timeline.TimeLineEnum.SCHEDULED_TOOT_SERVER) {
            displayScheduledServer();
        } else if (type == Timeline.TimeLineEnum.SCHEDULED_TOOT_CLIENT) {
            displayScheduledDevice();
        } else if (type == Timeline.TimeLineEnum.SCHEDULED_BOOST) {
            displayScheduledBoost();
        }
    }

    private int saveScrollPosition() {
        if (binding.recyclerView.getLayoutManager() instanceof LinearLayoutManager lm) {
            return lm.findFirstVisibleItemPosition();
        }
        return 0;
    }

    private void restoreScrollPosition(LinearLayoutManager layoutManager, int position) {
        if (position > 0) {
            layoutManager.scrollToPosition(position);
        }
    }

    private void sortScheduledStatuses(List<ScheduledStatus> list) {
        Collections.sort(list, (a, b) -> {
            if (a.scheduled_at == null || b.scheduled_at == null) return 0;
            return sortAscending ? a.scheduled_at.compareTo(b.scheduled_at) : b.scheduled_at.compareTo(a.scheduled_at);
        });
    }

    private void sortScheduledDrafts(List<StatusDraft> list) {
        Collections.sort(list, (a, b) -> {
            if (a.scheduled_at == null || b.scheduled_at == null) return 0;
            return sortAscending ? a.scheduled_at.compareTo(b.scheduled_at) : b.scheduled_at.compareTo(a.scheduled_at);
        });
    }

    private void sortScheduledBoosts(List<ScheduledBoost> list) {
        Collections.sort(list, (a, b) -> {
            if (a.scheduledAt == null || b.scheduledAt == null) return 0;
            return sortAscending ? a.scheduledAt.compareTo(b.scheduledAt) : b.scheduledAt.compareTo(a.scheduledAt);
        });
    }

    private void setupSortButton(List<ScheduledStatus> scheduledStatuses, List<StatusDraft> drafts, List<ScheduledBoost> boosts) {
        binding.sortButton.setVisibility(View.VISIBLE);
        updateSortIcon();
        binding.sortButton.setOnClickListener(v -> {
            sortAscending = !sortAscending;
            updateSortIcon();
            int itemCount = 0;
            if (scheduledStatuses != null) {
                sortScheduledStatuses(scheduledStatuses);
                itemCount = scheduledStatuses.size();
            } else if (drafts != null) {
                sortScheduledDrafts(drafts);
                itemCount = drafts.size();
            } else if (boosts != null) {
                sortScheduledBoosts(boosts);
                itemCount = boosts.size();
            }
            if (binding.recyclerView.getAdapter() != null && itemCount > 0) {
                binding.recyclerView.getAdapter().notifyItemRangeChanged(0, itemCount);
            }
        });
    }

    private void updateSortIcon() {
        binding.sortButton.setImageResource(sortAscending
                ? R.drawable.ic_baseline_filter_asc_24
                : R.drawable.ic_baseline_filter_desc_24);
    }

    @Override
    public void onAllDeleted() {
        binding.noAction.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
    }
}
