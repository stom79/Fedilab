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
import app.fedilab.android.mastodon.activities.admin.AdminActionActivity;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminAccount;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminAccounts;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.ui.drawer.admin.AdminAccountAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AdminVM;


public class FragmentAdminAccount extends Fragment {


    String byDomain, username, displayName, email, ip;
    private FragmentPaginationBinding binding;
    private AdminVM adminVM;
    private boolean flagLoading;
    private List<AdminAccount> adminAccounts;
    private String max_id;
    private AdminAccountAdapter adminAccountAdapter;
    private String viewModelKey;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
        }
        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);
        return binding.getRoot();
    }

    private void fetchAccount(Callback callback) {
        adminVM.getAccounts(
                        BaseMainActivity.currentInstance, BaseMainActivity.currentToken,
                        AdminActionActivity.local,
                        AdminActionActivity.remote,
                        byDomain,
                        AdminActionActivity.active,
                        AdminActionActivity.pending,
                        AdminActionActivity.disabled,
                        AdminActionActivity.silenced,
                        AdminActionActivity.suspended,
                        username, displayName, email, ip,
                        AdminActionActivity.staff, max_id, null,
                        MastodonHelper.statusesPerCall(requireActivity()))
                .observe(requireActivity(), callback::accountFetched);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int c1 = ThemeHelper.getAttColor(requireActivity(), R.attr.colorAccent);
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        adminVM = new ViewModelProvider(FragmentAdminAccount.this).get(viewModelKey, AdminVM.class);
        max_id = null;
        fetchAccount(this::initializeAccountCommonView);
    }

    public void scrollToTop() {
        binding.recyclerView.setAdapter(adminAccountAdapter);
    }

    /**
     * Intialize the view for accounts
     *
     * @param adminAccounts {@link AdminAccounts}
     */
    private void initializeAccountCommonView(final AdminAccounts adminAccounts) {
        if (binding == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            max_id = null;
            fetchAccount(this::initializeAccountCommonView);
        });
        if (adminAccounts == null || adminAccounts.adminAccounts == null || adminAccounts.adminAccounts.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_accounts);
            return;
        }
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (adminAccountAdapter != null && this.adminAccounts != null) {
            int size = this.adminAccounts.size();
            this.adminAccounts.clear();
            this.adminAccounts = new ArrayList<>();
            adminAccountAdapter.notifyItemRangeRemoved(0, size);
        }

        this.adminAccounts = adminAccounts.adminAccounts;
        adminAccountAdapter = new AdminAccountAdapter(this.adminAccounts);
        flagLoading = adminAccounts.pagination.max_id == null;
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.recyclerView.getContext(),
                mLayoutManager.getOrientation());
        binding.recyclerView.addItemDecoration(dividerItemDecoration);
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(adminAccountAdapter);
        //Fetch the relationship
        if (max_id == null || (adminAccounts.pagination.max_id != null && Helper.compareTo(adminAccounts.pagination.max_id, max_id) < 0)) {
            max_id = adminAccounts.pagination.max_id;
        }
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
                            fetchAccount(adminAccounts1 -> dealWithPagination(adminAccounts1));
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
     * @param adminAccounts AdminAccounts
     */
    private void dealWithPagination(AdminAccounts adminAccounts) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (this.adminAccounts != null && adminAccounts != null && adminAccounts.adminAccounts != null) {
            flagLoading = adminAccounts.pagination.max_id == null;
            int startId = 0;
            //There are some statuses present in the timeline
            if (this.adminAccounts.size() > 0) {
                startId = this.adminAccounts.size();
            }
            int position = this.adminAccounts.size();
            this.adminAccounts.addAll(adminAccounts.adminAccounts);
            if (max_id == null || (adminAccounts.pagination.max_id != null && Helper.compareTo(adminAccounts.pagination.max_id, max_id) < 0)) {
                max_id = adminAccounts.pagination.max_id;
            }
            adminAccountAdapter.notifyItemRangeInserted(startId, adminAccounts.adminAccounts.size());
        }
    }

    interface Callback {
        void accountFetched(AdminAccounts adminAccounts);
    }
}