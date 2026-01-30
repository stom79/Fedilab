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

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentScheduledBinding;
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
        StatusesVM statusesVM = new ViewModelProvider(requireActivity()).get(StatusesVM.class);
        statusesVM.getScheduledStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                .observe(requireActivity(), scheduledStatuses -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledStatuses != null && scheduledStatuses.scheduledStatuses != null && !scheduledStatuses.scheduledStatuses.isEmpty()) {
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(scheduledStatuses.scheduledStatuses, null, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);

                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void displayScheduledMisskey() {
        MisskeyStatusesVM misskeyStatusesVM = new ViewModelProvider(requireActivity()).get(MisskeyStatusesVM.class);
        misskeyStatusesVM.getScheduledNotes(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, MastodonHelper.statusesPerCall(requireActivity()), 0)
                .observe(requireActivity(), scheduledStatuses -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledStatuses != null && scheduledStatuses.scheduledStatuses != null && !scheduledStatuses.scheduledStatuses.isEmpty()) {
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(scheduledStatuses.scheduledStatuses, null, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);
                    } else {
                        binding.noAction.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void displayScheduledDevice() {
        new Thread(() -> {
            try {
                List<StatusDraft> scheduledDrafts = new StatusDraft(requireActivity()).geStatusDraftScheduledList(Helper.getCurrentAccount(requireActivity()));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledDrafts != null && !scheduledDrafts.isEmpty()) {
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(null, scheduledDrafts, null);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);

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

    private void displayScheduledBoost(){
        new Thread(() -> {
            try {
                List<ScheduledBoost> scheduledBoosts = new ScheduledBoost(requireActivity()).getScheduled(Helper.getCurrentAccount(requireActivity()));
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    binding.loader.setVisibility(View.GONE);
                    if (scheduledBoosts != null && !scheduledBoosts.isEmpty()) {
                        StatusScheduledAdapter statusScheduledAdapter = new StatusScheduledAdapter(null, null, scheduledBoosts);
                        statusScheduledAdapter.scheduledActions = FragmentScheduled.this;
                        binding.recyclerView.setAdapter(statusScheduledAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireActivity());
                        binding.recyclerView.setLayoutManager(linearLayoutManager);

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

    @Override
    public void onAllDeleted() {
        binding.noAction.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
    }
}
