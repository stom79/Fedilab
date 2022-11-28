package app.fedilab.android.ui.fragment.timeline;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.api.Domains;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.DomainBlockAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;


public class FragmentMastodonDomainBlock extends Fragment {


    private FragmentPaginationBinding binding;
    private DomainBlockAdapter domainBlockAdapter;
    private AccountsVM accountsVM;
    private List<String> domainList;
    private boolean flagLoading;
    private String max_id;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        return binding.getRoot();
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
        accountsVM = new ViewModelProvider(FragmentMastodonDomainBlock.this).get(AccountsVM.class);
        flagLoading = false;
        max_id = null;
        domainList = new ArrayList<>();

        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            max_id = null;
            int size = domainList.size();
            domainList.clear();
            domainList = new ArrayList<>();
            domainBlockAdapter.notifyItemRangeRemoved(0, size);
            router();
        });
        domainBlockAdapter = new DomainBlockAdapter(domainList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(domainBlockAdapter);
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
                            router();
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }

            }
        });
        router();
    }

    /**
     * Router for timelines
     */
    private void router() {

        if (max_id == null) {
            accountsVM.getDomainBlocks(MainActivity.currentInstance, MainActivity.currentToken, null, null, null)
                    .observe(getViewLifecycleOwner(), this::initializeTagCommonView);
        } else {
            accountsVM.getDomainBlocks(MainActivity.currentInstance, MainActivity.currentToken, null, max_id, null)
                    .observe(getViewLifecycleOwner(), this::dealWithPagination);
        }
    }

    public void scrollToTop() {
        binding.recyclerView.setAdapter(domainBlockAdapter);
    }

    /**
     * Intialize the view for domains
     *
     * @param domains List of {@link String}
     */
    private void initializeTagCommonView(final Domains domains) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            max_id = null;
            router();
        });
        if (domains == null || domains.domains == null || domains.domains.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_accounts);
            return;
        }
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (domainBlockAdapter != null && this.domainList != null) {
            int size = this.domainList.size();
            this.domainList.clear();
            this.domainList = new ArrayList<>();
            domainBlockAdapter.notifyItemRangeRemoved(0, size);
        }

        this.domainList = domains.domains;
        domainBlockAdapter = new DomainBlockAdapter(this.domainList);
        flagLoading = domains.pagination.max_id == null;
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(domainBlockAdapter);

        max_id = domains.pagination.max_id;
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
                            router();
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }

            }
        });
    }


    private void dealWithPagination(Domains fetched_domains) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (domainList != null && fetched_domains != null && fetched_domains.domains != null) {
            flagLoading = fetched_domains.pagination.max_id == null;
            int startId = 0;
            //There are some domains present in the timeline
            if (domainList.size() > 0) {
                startId = domainList.size();
            }
            int position = domainList.size();
            domainList.addAll(fetched_domains.domains);
            max_id = fetched_domains.pagination.max_id;
            domainBlockAdapter.notifyItemRangeInserted(startId, fetched_domains.domains.size());
        } else {
            flagLoading = true;
        }
    }
}