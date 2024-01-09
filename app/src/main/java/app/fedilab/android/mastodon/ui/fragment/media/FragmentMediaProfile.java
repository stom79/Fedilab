package app.fedilab.android.mastodon.ui.fragment.media;
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

import static app.fedilab.android.BaseMainActivity.currentAccount;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.CrossActionHelper;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.ImageAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;


public class FragmentMediaProfile extends Fragment {

    public static List<Attachment> mediaAttachmentProfile;
    String tempToken;
    String tempInstance;
    private FragmentPaginationBinding binding;
    private AccountsVM accountsVM;
    private Account accountTimeline;
    private boolean flagLoading;
    private List<Status> mediaStatuses;
    private String max_id;
    private ImageAdapter imageAdapter;
    private boolean checkRemotely;
    private String accountId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean displayScrollBar = sharedpreferences.getBoolean(getString(R.string.SET_TIMELINE_SCROLLBAR), false);
        binding.recyclerView.setVerticalScrollBarEnabled(displayScrollBar);

        if (getArguments() != null) {
            long bundleId = getArguments().getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(requireActivity()).getBundle(bundleId, currentAccount, bundle -> {
                if (bundle != null) {
                    accountTimeline = (Account) bundle.getSerializable(Helper.ARG_ACCOUNT);
                    checkRemotely = bundle.getBoolean(Helper.ARG_CHECK_REMOTELY, false);
                }
            });
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        flagLoading = false;
        accountsVM = new ViewModelProvider(requireActivity()).get(AccountsVM.class);
        mediaStatuses = new ArrayList<>();

        if (checkRemotely) {
            tempToken = null;
            String[] acctArray = accountTimeline.acct.split("@");
            if (acctArray.length > 1) {
                tempInstance = acctArray[1];
            }
            CrossActionHelper.fetchAccountInRemoteInstance(requireActivity(), accountTimeline.acct, tempInstance, new CrossActionHelper.Callback() {
                @Override
                public void federatedStatus(Status status) {
                }

                @Override
                public void federatedAccount(Account account) {
                    if (account != null && isAdded() && !requireActivity().isFinishing()) {
                        accountId = account.id;
                        accountsVM.getAccountStatuses(tempInstance, null, accountId, null, null, null, null, null, true, false, MastodonHelper.statusesPerCall(requireActivity()))
                                .observe(getViewLifecycleOwner(), statuses -> initializeStatusesCommonView(statuses));
                    } else {
                        if (isAdded() && !requireActivity().isFinishing()) {
                            Toasty.error(requireActivity(), getString(R.string.toast_fetch_error), Toasty.LENGTH_LONG).show();
                        }
                    }
                }
            });
        } else {
            tempToken = BaseMainActivity.currentToken;
            tempInstance = BaseMainActivity.currentInstance;
            accountId = accountTimeline.id;
            accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, null, null, true, false, MastodonHelper.statusesPerCall(requireActivity()))
                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
        }

    }

    /**
     * Intialize the common view for statuses on different timelines
     *
     * @param statuses {@link Statuses}
     */
    private void initializeStatusesCommonView(final Statuses statuses) {
        mediaAttachmentProfile = new ArrayList<>();
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        if (statuses == null || statuses.statuses == null || statuses.statuses.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            return;
        }

        for (Status status : statuses.statuses) {
            for (Attachment attachment : status.media_attachments) {
                try {
                    Status statusTmp = (Status) status.clone();
                    statusTmp.art_attachment = attachment;
                    mediaStatuses.add(statusTmp);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        imageAdapter = new ImageAdapter();

        flagLoading = statuses.pagination.max_id == null;
        binding.recyclerView.setVisibility(View.VISIBLE);

        if (max_id == null || (statuses.pagination.max_id != null && Helper.compareTo(statuses.pagination.max_id, max_id) < 0)) {
            max_id = statuses.pagination.max_id;
        }
        GridLayoutManager gvLayout = new GridLayoutManager(requireActivity(), 3);
        binding.recyclerView.setLayoutManager(gvLayout);
        binding.recyclerView.setAdapter(imageAdapter);


        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (requireActivity() instanceof BaseMainActivity) {
                    if (dy < 0 && !((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(true);
                    if (dy > 0 && ((BaseMainActivity) requireActivity()).getFloatingVisibility())
                        ((BaseMainActivity) requireActivity()).manageFloatingButton(false);
                }
                int firstVisibleItem = gvLayout.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = gvLayout.getChildCount();
                    int totalItemCount = gvLayout.getItemCount();

                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            accountsVM.getAccountStatuses(tempInstance, tempToken, accountId, max_id, null, null, null, null, true, false, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), newStatuses -> dealWithPagination(newStatuses));
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }
            }
        });
        fillWithMedia();
    }


    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_statuses Statuses
     */
    private synchronized void dealWithPagination(Statuses fetched_statuses) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (this.mediaStatuses != null && fetched_statuses != null && fetched_statuses.statuses != null && fetched_statuses.statuses.size() > 0) {
            flagLoading = fetched_statuses.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);
            //We have to split media in different statuses

            int added = 0;
            for (Status status : fetched_statuses.statuses) {
                for (Attachment attachment : status.media_attachments) {
                    try {
                        Status statusTmp = (Status) status.clone();
                        statusTmp.art_attachment = attachment;
                        mediaStatuses.add(statusTmp);
                        added++;
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }

            imageAdapter.notifyItemRangeInserted(this.mediaStatuses.size() - added, this.mediaStatuses.size());
            if (fetched_statuses.pagination.max_id == null) {
                flagLoading = true;
            } else if (max_id == null || Helper.compareTo(fetched_statuses.pagination.max_id, max_id) < 0) {
                max_id = fetched_statuses.pagination.max_id;
            }
        } else {
            flagLoading = true;
        }
        fillWithMedia();
    }


    public void fillWithMedia() {

        if (mediaStatuses != null && mediaStatuses.size() > 0) {
            for (Status status : mediaStatuses) {
                if (status.media_attachments != null && status.media_attachments.size() > 0) {
                    for (Attachment attachment : status.media_attachments) {
                        attachment.status = status;
                        if (!mediaAttachmentProfile.contains(attachment)) {
                            mediaAttachmentProfile.add(attachment);
                        }
                    }
                }
            }
        }
    }
}
