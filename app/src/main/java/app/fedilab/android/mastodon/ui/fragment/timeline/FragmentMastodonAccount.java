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


import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentToken;
import static app.fedilab.android.mastodon.helper.MastodonHelper.ACCOUNTS_PER_CALL;

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

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.activities.SearchResultTabActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Accounts;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.AccountAdapter;
import app.fedilab.android.mastodon.ui.pageadapter.FedilabProfileTLPageAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import es.dmoral.toasty.Toasty;


public class FragmentMastodonAccount extends Fragment {


    private FragmentPaginationBinding binding;
    private AccountsVM accountsVM;
    private boolean flagLoading;
    private List<Account> accounts;
    private String max_id;
    private Integer offset;
    private AccountAdapter accountAdapter;
    private String search;
    private Account accountTimeline;
    private FedilabProfileTLPageAdapter.follow_type followType;
    private String viewModelKey;
    private Timeline.TimeLineEnum timelineType;
    private String order;
    private Boolean local;
    private boolean checkRemotely;
    private String instance, token, remoteAccountId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            search = getArguments().getString(Helper.ARG_SEARCH_KEYWORD, null);
            accountTimeline = (Account) getArguments().getSerializable(Helper.ARG_ACCOUNT);
            followType = (FedilabProfileTLPageAdapter.follow_type) getArguments().getSerializable(Helper.ARG_FOLLOW_TYPE);
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
            timelineType = (Timeline.TimeLineEnum) getArguments().get(Helper.ARG_TIMELINE_TYPE);
            order = getArguments().getString(Helper.ARG_DIRECTORY_ORDER, "active");
            local = getArguments().getBoolean(Helper.ARG_DIRECTORY_LOCAL, false);
            checkRemotely = getArguments().getBoolean(Helper.ARG_CHECK_REMOTELY, false);
        }
        instance = currentInstance;
        token = currentToken;

        if (checkRemotely) {
            String[] acctArray = accountTimeline.acct.split("@");
            if (acctArray.length > 1) {
                instance = acctArray[1];
                token = null;
            }
            if (instance != null && instance.equalsIgnoreCase(currentInstance)) {
                checkRemotely = false;
                instance = currentInstance;
                token = currentToken;
            }
        }

        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        accountsVM = new ViewModelProvider(FragmentMastodonAccount.this).get(viewModelKey, AccountsVM.class);
        max_id = null;
        offset = 0;
        if (search != null) {
            binding.swipeContainer.setRefreshing(false);
            binding.swipeContainer.setEnabled(false);
        }
        router(true);
    }

    /**
     * Router for timelines
     */
    private void router(boolean firstLoad) {
        if (checkRemotely) {
            if (remoteAccountId == null) {
                SearchVM searchVM = new ViewModelProvider(FragmentMastodonAccount.this).get(viewModelKey, SearchVM.class);
                searchVM.search(instance, token, accountTimeline.acct, null, "accounts", null, null, null, null, null, null, null)
                        .observe(getViewLifecycleOwner(), results -> {
                            if (results != null && results.accounts.size() > 0) {
                                remoteAccountId = results.accounts.get(0).id;
                                fetchAccount(firstLoad, remoteAccountId);
                            } else {
                                Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                            }
                        });
            } else {
                fetchAccount(firstLoad, remoteAccountId);
            }
        } else {
            fetchAccount(firstLoad, accountTimeline.id);
        }
    }


    private void fetchAccount(boolean firstLoad, String accountProfileId) {
        if (followType == FedilabProfileTLPageAdapter.follow_type.FOLLOWERS) {
            if (firstLoad) {
                accountsVM.getAccountFollowers(instance, token, accountProfileId, null, null)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            } else {
                accountsVM.getAccountFollowers(instance, token, accountProfileId, max_id, null)
                        .observe(getViewLifecycleOwner(), this::dealWithPagination);
            }
        } else if (followType == FedilabProfileTLPageAdapter.follow_type.FOLLOWING) {
            if (firstLoad) {
                accountsVM.getAccountFollowing(instance, token, accountProfileId, null, null)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            } else {
                accountsVM.getAccountFollowing(instance, token, accountProfileId, max_id, null)
                        .observe(getViewLifecycleOwner(), this::dealWithPagination);
            }
        } else if (search != null) {
            SearchVM searchVM = new ViewModelProvider(FragmentMastodonAccount.this).get(viewModelKey, SearchVM.class);
            if (firstLoad) {
                searchVM.search(instance, token, search.trim(), null, "accounts", false, true, false, 0, null, null, MastodonHelper.SEARCH_PER_CALL)
                        .observe(getViewLifecycleOwner(), results -> {
                            if (results != null) {
                                Accounts accounts = new Accounts();
                                Pagination pagination = new Pagination();
                                accounts.accounts = results.accounts;
                                accounts.pagination = pagination;
                                initializeAccountCommonView(accounts);
                            } else {
                                Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                            }
                        });
            } else {
                searchVM.search(instance, token, search.trim(), null, "accounts", false, true, false, offset, null, null, MastodonHelper.SEARCH_PER_CALL)
                        .observe(getViewLifecycleOwner(), results -> {
                            if (results != null) {
                                Accounts accounts = new Accounts();
                                Pagination pagination = new Pagination();
                                accounts.accounts = results.accounts;
                                accounts.pagination = pagination;
                                dealWithPagination(accounts);
                            }
                        });
            }
        } else if (timelineType == Timeline.TimeLineEnum.MUTED_TIMELINE) {
            if (firstLoad) {
                accountsVM.getMutes(instance, token, String.valueOf(MastodonHelper.accountsPerCall(requireActivity())), null, null)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            } else {
                accountsVM.getMutes(instance, token, String.valueOf(MastodonHelper.accountsPerCall(requireActivity())), max_id, null)
                        .observe(getViewLifecycleOwner(), this::dealWithPagination);
            }
        } else if (timelineType == Timeline.TimeLineEnum.MUTED_TIMELINE_HOME) {
            if (firstLoad) {
                accountsVM.getMutedHome(MainActivity.currentAccount)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            }
        } else if (timelineType == Timeline.TimeLineEnum.BLOCKED_TIMELINE) {
            if (firstLoad) {
                accountsVM.getBlocks(instance, token, String.valueOf(MastodonHelper.accountsPerCall(requireActivity())), null, null)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            } else {
                accountsVM.getBlocks(instance, token, String.valueOf(MastodonHelper.accountsPerCall(requireActivity())), max_id, null)
                        .observe(getViewLifecycleOwner(), this::dealWithPagination);
            }
        } else if (timelineType == Timeline.TimeLineEnum.ACCOUNT_DIRECTORY) {
            if (firstLoad) {
                accountsVM.getDirectory(instance, token, 0, ACCOUNTS_PER_CALL, order, local)
                        .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
            } else {
                accountsVM.getDirectory(instance, token, offset, ACCOUNTS_PER_CALL, order, local)
                        .observe(getViewLifecycleOwner(), this::dealWithPagination);
            }
        }
    }

    private void fetchRelationShip(List<Account> accounts, int position) {
        List<String> ids = new ArrayList<>();
        for (Account account : accounts) {
            ids.add(account.id);
        }
        accountsVM.getRelationships(instance, token, ids)
                .observe(getViewLifecycleOwner(), relationShips -> {
                    if (relationShips != null) {
                        for (RelationShip relationShip : relationShips) {
                            for (Account account : accounts) {
                                if (account.id.compareToIgnoreCase(relationShip.id) == 0) {
                                    account.relationShip = relationShip;
                                }
                            }
                        }
                        accountAdapter.notifyItemRangeChanged(position, accounts.size());
                    }
                });
    }


    public void scrollToTop() {
        binding.recyclerView.setAdapter(accountAdapter);
    }

    /**
     * Intialize the view for accounts
     *
     * @param accounts {@link Accounts}
     */
    private void initializeAccountCommonView(final Accounts accounts) {
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
            router(true);
        });
        if (accounts == null || accounts.accounts == null || accounts.accounts.size() == 0) {
            if (requireActivity() instanceof SearchResultTabActivity) {
                ((SearchResultTabActivity) requireActivity()).accountEmpty = true;
                if (((SearchResultTabActivity) requireActivity()).tagEmpty != null) {
                    if (((SearchResultTabActivity) requireActivity()).tagEmpty) {
                        ((SearchResultTabActivity) requireActivity()).moveToMessage();
                    }
                }
            }
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_accounts);
            return;
        }
        if (requireActivity() instanceof SearchResultTabActivity) {
            if (((SearchResultTabActivity) requireActivity()).tagEmpty != null) {
                if (((SearchResultTabActivity) requireActivity()).tagEmpty) {
                    ((SearchResultTabActivity) requireActivity()).moveToAccount();
                }
            }
        }
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (accountAdapter != null && this.accounts != null) {
            int size = this.accounts.size();
            this.accounts.clear();
            this.accounts = new ArrayList<>();
            accountAdapter.notifyItemRangeRemoved(0, size);
        }

        this.accounts = accounts.accounts;
        accountAdapter = new AccountAdapter(this.accounts, timelineType == Timeline.TimeLineEnum.MUTED_TIMELINE_HOME, checkRemotely ? instance : null);
        if (search == null && timelineType != Timeline.TimeLineEnum.ACCOUNT_DIRECTORY) {
            flagLoading = accounts.pagination.max_id == null;
        } else if (timelineType != Timeline.TimeLineEnum.ACCOUNT_DIRECTORY) {
            offset += ACCOUNTS_PER_CALL;
        } else {
            offset += MastodonHelper.SEARCH_PER_CALL;
        }
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(accountAdapter);
        //Fetch the relationship
        if (!checkRemotely) {
            fetchRelationShip(accounts.accounts, 0);
        }
        max_id = accounts.pagination.max_id;
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
                            router(false);
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
     * @param fetched_accounts Accounts
     */
    private void dealWithPagination(Accounts fetched_accounts) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (accounts != null && fetched_accounts != null && fetched_accounts.accounts != null) {
            flagLoading = fetched_accounts.pagination.max_id == null;
            int startId = 0;
            //There are some statuses present in the timeline
            if (accounts.size() > 0) {
                startId = accounts.size();
            }
            int position = accounts.size();
            accounts.addAll(fetched_accounts.accounts);
            //Fetch the relationship
            if (!checkRemotely) {
                fetchRelationShip(fetched_accounts.accounts, position);
            }
            max_id = fetched_accounts.pagination.max_id;
            if (search != null) {
                offset += MastodonHelper.SEARCH_PER_CALL;
            } else if (timelineType == Timeline.TimeLineEnum.ACCOUNT_DIRECTORY) {
                offset += ACCOUNTS_PER_CALL;
            }
            accountAdapter.notifyItemRangeInserted(startId, fetched_accounts.accounts.size());
        } else {
            flagLoading = true;
        }
    }
}