package app.fedilab.android.ui.fragment.admin;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.AdminActionActivity;
import app.fedilab.android.client.entities.api.AdminReport;
import app.fedilab.android.client.entities.api.AdminReports;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.ReportAdapter;
import app.fedilab.android.viewmodel.mastodon.AdminVM;


public class FragmentAdminReport extends Fragment {


    private FragmentPaginationBinding binding;
    private AdminVM adminVM;
    private boolean flagLoading;
    private List<AdminReport> adminReports;
    private String max_id, min_id;
    private ReportAdapter reportAdapter;
    private LinearLayoutManager mLayoutManager;
    private String viewModelKey;

    public void scrollToTop() {
        if (binding != null) {
            binding.recyclerView.scrollToPosition(0);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        if (getArguments() != null) {
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
        }

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));

        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );

        adminVM = new ViewModelProvider(FragmentAdminReport.this).get(viewModelKey, AdminVM.class);

        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        flagLoading = false;
        adminVM.getReports(
                        BaseMainActivity.currentInstance, BaseMainActivity.currentToken, AdminActionActivity.resolved, null, null, null)
                .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Intialize the common view for statuses on different timelines
     *
     * @param adminReports {@link AdminReports}
     */
    private void initializeStatusesCommonView(final AdminReports adminReports) {
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
            adminVM.getReports(
                            BaseMainActivity.currentInstance, BaseMainActivity.currentToken, AdminActionActivity.resolved, null, null, null)
                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
        });

        if (adminReports == null || adminReports.adminReports == null || adminReports.adminReports.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            return;
        }
        flagLoading = adminReports.pagination.max_id == null;
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (reportAdapter != null && this.adminReports != null) {
            int size = this.adminReports.size();
            this.adminReports.clear();
            this.adminReports = new ArrayList<>();
            reportAdapter.notifyItemRangeRemoved(0, size);
        }
        if (this.adminReports == null) {
            this.adminReports = new ArrayList<>();
        }
        this.adminReports.addAll(adminReports.adminReports);

        if (max_id == null || (adminReports.pagination.max_id != null && Helper.compareTo(adminReports.pagination.max_id, max_id) < 0)) {
            max_id = adminReports.pagination.max_id;
        }
        if (min_id == null || (adminReports.pagination.max_id != null && Helper.compareTo(adminReports.pagination.min_id, min_id) > 0)) {
            min_id = adminReports.pagination.min_id;
        }

        reportAdapter = new ReportAdapter(adminReports.adminReports);

        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(reportAdapter);
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
                            adminVM.getReports(
                                            BaseMainActivity.currentInstance, BaseMainActivity.currentToken, AdminActionActivity.resolved, null, null, max_id)
                                    .observe(getViewLifecycleOwner(), adminReports1 -> dealWithPagination(adminReports1));
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
     * @param admReports AdminReports
     */
    private void dealWithPagination(AdminReports admReports) {

        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (adminReports != null && admReports != null && admReports.adminReports != null && admReports.adminReports.size() > 0) {
            flagLoading = admReports.pagination.max_id == null;
            //There are some adminReports present in the timeline
            int startId = adminReports.size();
            adminReports.addAll(admReports.adminReports);
            reportAdapter.notifyItemRangeInserted(startId, admReports.adminReports.size());
            if (max_id == null || (admReports.pagination.max_id != null && Helper.compareTo(admReports.pagination.max_id, max_id) < 0)) {
                max_id = admReports.pagination.max_id;
            }
            if (min_id == null || (admReports.pagination.min_id != null && Helper.compareTo(admReports.pagination.min_id, min_id) > 0)) {
                min_id = admReports.pagination.min_id;
            }
        }
    }



}