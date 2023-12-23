package app.fedilab.android.mastodon.ui.fragment.admin;
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


import android.content.Intent;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.activities.admin.AdminDomainBlockActivity;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminDomainBlock;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminDomainBlocks;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.ui.drawer.admin.AdminDomainAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AdminVM;


public class FragmentAdminDomain extends Fragment {


    private FragmentPaginationBinding binding;
    private AdminVM adminVM;
    private boolean flagLoading;
    private List<AdminDomainBlock> adminDomainBlocks;
    private String max_id, min_id;
    private AdminDomainAdapter adminDomainAdapter;
    private LinearLayoutManager mLayoutManager;
    private String viewModelKey;

    public void scrollToTop() {
        if (binding != null) {
            binding.recyclerView.scrollToPosition(0);
        }
    }

    public void delete(AdminDomainBlock adminDomainBlock) {
        int position = 0;
        for (AdminDomainBlock adminDomainBlockPresent : adminDomainBlocks) {
            if (adminDomainBlockPresent.id.equals(adminDomainBlock.id)) {
                adminDomainBlocks.remove(position);
                adminDomainAdapter.notifyItemRemoved(position);
                break;
            }
            position++;
        }
    }

    public void update(AdminDomainBlock adminDomainBlock) {
        if (adminDomainBlocks == null) {
            AdminDomainBlocks adminDomainBlocks = new AdminDomainBlocks();
            adminDomainBlocks.adminDomainBlocks = new ArrayList<>();
            adminDomainBlocks.adminDomainBlocks.add(adminDomainBlock);
            initializeStatusesCommonView(adminDomainBlocks);
        }
        int position = 0;
        boolean find = false;
        for (AdminDomainBlock adminDomainBlockPresent : adminDomainBlocks) {
            if (adminDomainBlockPresent.id.equals(adminDomainBlock.id)) {
                adminDomainBlocks.get(position).private_comment = adminDomainBlock.private_comment;
                adminDomainBlocks.get(position).public_comment = adminDomainBlock.public_comment;
                adminDomainBlocks.get(position).severity = adminDomainBlock.severity;
                adminDomainBlocks.get(position).reject_reports = adminDomainBlock.reject_reports;
                adminDomainBlocks.get(position).reject_media = adminDomainBlock.reject_media;
                adminDomainBlocks.get(position).obfuscate = adminDomainBlock.obfuscate;
                adminDomainAdapter.notifyItemChanged(position);
                find = true;
                break;
            }
            position++;
        }
        if (!find) {
            adminDomainBlocks.add(0, adminDomainBlock);
            adminDomainAdapter.notifyItemInserted(0);
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        if (getArguments() != null) {
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
        }

        binding = FragmentPaginationBinding.inflate(inflater, container, false);

        int c1 = ThemeHelper.getAttColor(requireActivity(), R.attr.colorAccent);
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );

        adminVM = new ViewModelProvider(FragmentAdminDomain.this).get(viewModelKey, AdminVM.class);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);
        binding.noActionText.setText(R.string.no_blocked_domains);
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        flagLoading = false;
        adminVM.getDomainBlocks(
                        BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
        binding.addAction.setVisibility(View.VISIBLE);
        binding.addAction.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AdminDomainBlockActivity.class);
            Bundle b = new Bundle();
            intent.putExtras(b);
            startActivity(intent);
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Intialize the common view for domain block on different timelines
     *
     * @param adminDomainBlocks {@link AdminDomainBlocks}
     */
    private void initializeStatusesCommonView(final AdminDomainBlocks adminDomainBlocks) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            max_id = null;
            flagLoading = false;
            adminVM.getDomainBlocks(
                            BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
        });

        if (adminDomainBlocks == null || adminDomainBlocks.adminDomainBlocks == null || adminDomainBlocks.adminDomainBlocks.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            return;
        }
        flagLoading = adminDomainBlocks.pagination.max_id == null;
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (adminDomainAdapter != null && this.adminDomainBlocks != null) {
            int size = this.adminDomainBlocks.size();
            this.adminDomainBlocks.clear();
            this.adminDomainBlocks = new ArrayList<>();
            adminDomainAdapter.notifyItemRangeRemoved(0, size);
        }
        if (this.adminDomainBlocks == null) {
            this.adminDomainBlocks = new ArrayList<>();
        }
        this.adminDomainBlocks.addAll(adminDomainBlocks.adminDomainBlocks);

        if (max_id == null || (adminDomainBlocks.pagination.max_id != null && Helper.compareTo(adminDomainBlocks.pagination.max_id, max_id) < 0)) {
            max_id = adminDomainBlocks.pagination.max_id;
        }
        if (min_id == null || (adminDomainBlocks.pagination.max_id != null && Helper.compareTo(adminDomainBlocks.pagination.min_id, min_id) > 0)) {
            min_id = adminDomainBlocks.pagination.min_id;
        }

        adminDomainAdapter = new AdminDomainAdapter(adminDomainBlocks.adminDomainBlocks);

        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(adminDomainAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.recyclerView.getContext(),
                mLayoutManager.getOrientation());
        binding.recyclerView.addItemDecoration(dividerItemDecoration);
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
                            adminVM.getDomainAllows(
                                            BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id)
                                    .observe(getViewLifecycleOwner(), adminDomainBlocks1 -> dealWithPagination(adminDomainBlocks1));
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param admDomainBlocks AdminDomainBlocks
     */
    private void dealWithPagination(AdminDomainBlocks admDomainBlocks) {

        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (this.adminDomainBlocks != null && admDomainBlocks != null && admDomainBlocks.adminDomainBlocks != null && admDomainBlocks.adminDomainBlocks.size() > 0) {
            flagLoading = admDomainBlocks.pagination.max_id == null;
            //There are some adminDomainBlocks present in the timeline
            int startId = this.adminDomainBlocks.size();
            this.adminDomainBlocks.addAll(admDomainBlocks.adminDomainBlocks);
            adminDomainAdapter.notifyItemRangeInserted(startId, admDomainBlocks.adminDomainBlocks.size());
            if (max_id == null || (admDomainBlocks.pagination.max_id != null && Helper.compareTo(admDomainBlocks.pagination.max_id, max_id) < 0)) {
                max_id = admDomainBlocks.pagination.max_id;
            }
            if (min_id == null || (admDomainBlocks.pagination.min_id != null && Helper.compareTo(admDomainBlocks.pagination.min_id, min_id) > 0)) {
                min_id = admDomainBlocks.pagination.min_id;
            }
        }
    }


}