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


import static app.fedilab.android.BaseMainActivity.networkAvailable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.QuickLoad;
import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.client.entities.app.TagTimeline;
import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Marker;
import app.fedilab.android.client.mastodon.entities.Pagination;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.client.mastodon.entities.Statuses;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.StatusAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class FragmentMastodonTimeline extends Fragment {


    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private AccountsVM accountsVM;
    private boolean flagLoading;
    private List<Status> statuses;
    private String search, searchCache;
    private Status statusReport;
    private String max_id, min_id;
    private StatusAdapter statusAdapter;
    private Timeline.TimeLineEnum timelineType;
    private List<String> markers;
    private String list_id;
    private TagTimeline tagTimeline;
    private LinearLayoutManager mLayoutManager;
    private Account accountTimeline;
    private boolean exclude_replies, exclude_reblogs, show_pinned, media_only, minified;
    private String viewModelKey, remoteInstance;
    private String ident;
    private String instance, user_id;

    //Handle actions that can be done in other fragments
    private final BroadcastReceiver receive_action = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                Status receivedStatus = (Status) b.getSerializable(Helper.ARG_STATUS_ACTION);
                String delete_statuses_for_user = b.getString(Helper.ARG_STATUS_ACCOUNT_ID_DELETED);
                Status status_to_delete = (Status) b.getSerializable(Helper.ARG_STATUS_DELETED);
                Status statusPosted = (Status) b.getSerializable(Helper.ARG_STATUS_DELETED);
                if (receivedStatus != null && statusAdapter != null) {
                    int position = getPosition(receivedStatus);
                    if (position >= 0) {
                        statuses.get(position).reblog = receivedStatus.reblog;
                        statuses.get(position).favourited = receivedStatus.favourited;
                        statuses.get(position).bookmarked = receivedStatus.bookmarked;
                        statusAdapter.notifyItemChanged(position);
                    }
                } else if (delete_statuses_for_user != null && statusAdapter != null) {
                    List<Status> statusesToRemove = new ArrayList<>();
                    for (Status status : statuses) {
                        if (status.account.id.equals(delete_statuses_for_user)) {
                            statusesToRemove.add(status);
                        }
                    }
                    for (Status statusToRemove : statusesToRemove) {
                        int position = getPosition(statusToRemove);
                        if (position >= 0) {
                            statuses.remove(position);
                            statusAdapter.notifyItemRemoved(position);
                        }
                    }
                } else if (status_to_delete != null && statusAdapter != null) {
                    int position = getPosition(status_to_delete);
                    if (position >= 0) {
                        statuses.remove(position);
                        statusAdapter.notifyItemRemoved(position);
                    }
                } else if (statusPosted != null && statusAdapter != null && timelineType == Timeline.TimeLineEnum.HOME) {
                    new Thread(() -> {
                        Status convertStatus = SpannableHelper.convertStatus(context, statusPosted);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> {
                            statuses.add(0, convertStatus);
                            statusAdapter.notifyItemInserted(0);
                        };
                        mainHandler.post(myRunnable);
                    }).start();
                }
            }
        }
    };

    /**
     * Return the position of the status in the ArrayList
     *
     * @param status - Status to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Status status) {
        int position = 0;
        boolean found = false;
        for (Status _status : statuses) {
            if (_status.id.compareTo(status.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        timelineType = Timeline.TimeLineEnum.HOME;
        instance = MainActivity.currentInstance;
        user_id = MainActivity.currentUserID;
        if (getArguments() != null) {
            timelineType = (Timeline.TimeLineEnum) getArguments().get(Helper.ARG_TIMELINE_TYPE);
            list_id = getArguments().getString(Helper.ARG_LIST_ID, null);
            search = getArguments().getString(Helper.ARG_SEARCH_KEYWORD, null);
            searchCache = getArguments().getString(Helper.ARG_SEARCH_KEYWORD_CACHE, null);
            remoteInstance = getArguments().getString(Helper.ARG_REMOTE_INSTANCE, null);
            tagTimeline = (TagTimeline) getArguments().getSerializable(Helper.ARG_TAG_TIMELINE);
            accountTimeline = (Account) getArguments().getSerializable(Helper.ARG_ACCOUNT);
            exclude_replies = !getArguments().getBoolean(Helper.ARG_SHOW_REPLIES, true);
            show_pinned = getArguments().getBoolean(Helper.ARG_SHOW_PINNED, false);
            exclude_reblogs = !getArguments().getBoolean(Helper.ARG_SHOW_REBLOGS, true);
            media_only = getArguments().getBoolean(Helper.ARG_SHOW_MEDIA_ONY, false);
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
            minified = getArguments().getBoolean(Helper.ARG_MINIFIED, false);
            statusReport = (Status) getArguments().getSerializable(Helper.ARG_STATUS_REPORT);
        }
        if (tagTimeline != null) {
            ident = "@T@" + tagTimeline.name;
            if (tagTimeline.isART) {
                timelineType = Timeline.TimeLineEnum.ART;
            }
        } else if (list_id != null) {
            ident = "@l@" + list_id;
        } else if (remoteInstance != null) {
            ident = "@R@" + remoteInstance;
        } else if (search != null) {
            ident = "@S@" + search;
        } else {
            ident = null;
        }
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION));
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));
        return binding.getRoot();
    }

    /**
     * Returned list of checked status id for reports
     *
     * @return List<String>
     */
    public List<String> getCheckedStatusesId() {
        List<String> stringList = new ArrayList<>();
        for (Status status : statuses) {
            if (status.isChecked) {
                stringList.add(status.id);
            }
        }
        return stringList;
    }

    public void scrollToTop() {
        if (binding != null) {
            binding.recyclerView.scrollToPosition(0);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );

        timelinesVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, TimelinesVM.class);
        accountsVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, AccountsVM.class);

        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        //Markers for home and notifications to get last read ones
        markers = new ArrayList<>();
        max_id = statusReport != null ? statusReport.id : null;
        flagLoading = false;
        router(null);
    }

    /**
     * Intialize the common view for statuses on different timelines
     *
     * @param statuses {@link Statuses}
     */
    private void initializeStatusesCommonView(final Statuses statuses) {
        if (binding == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            max_id = null;
            flagLoading = false;
            router(null);
        });

        if (statuses == null || statuses.statuses == null || statuses.statuses.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            return;
        } else if (timelineType == Timeline.TimeLineEnum.ART) {
            //We have to split media in different statuses
            List<Status> mediaStatuses = new ArrayList<>();
            for (Status status : statuses.statuses) {
                if (!tagTimeline.isNSFW && status.sensitive) {
                    continue;
                }
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
            if (mediaStatuses.size() > 0) {
                statuses.statuses = mediaStatuses;
            }
        }
        flagLoading = (statuses.statuses.size() < MastodonHelper.statusesPerCall(requireActivity()));
        binding.recyclerView.setVisibility(View.VISIBLE);
        if (statusAdapter != null && this.statuses != null) {
            int size = this.statuses.size();
            this.statuses.clear();
            this.statuses = new ArrayList<>();
            statusAdapter.notifyItemRangeRemoved(0, size);
        }
        if (this.statuses == null) {
            this.statuses = new ArrayList<>();
        }
        if (statusReport != null) {
            this.statuses.add(statusReport);
        }
        this.statuses.addAll(statuses.statuses);

        if (max_id == null || (statuses.pagination.max_id != null && statuses.pagination.max_id.compareTo(max_id) < 0)) {
            max_id = statuses.pagination.max_id;
        }
        if (min_id == null || (statuses.pagination.max_id != null && statuses.pagination.min_id.compareTo(min_id) > 0)) {
            min_id = statuses.pagination.min_id;
        }

        statusAdapter = new StatusAdapter(this.statuses, timelineType, minified);

        if (statusReport != null) {
            scrollToTop();
        }
        mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusAdapter);


        if (searchCache == null) {
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
                                router(DIRECTION.BOTTOM);
                            }
                        } else {
                            binding.loadingNextElements.setVisibility(View.GONE);
                        }
                    } else if (firstVisibleItem == 0) { //Scroll top and item is zero
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            router(DIRECTION.TOP);
                        }
                    }
                }
            });
        }
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_statuses Statuses
     */
    private void dealWithPagination(Statuses fetched_statuses, DIRECTION direction) {
        flagLoading = false;
        if (binding == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (statuses != null && fetched_statuses != null && fetched_statuses.statuses != null && fetched_statuses.statuses.size() > 0) {

            flagLoading = (direction == DIRECTION.BOTTOM && fetched_statuses.statuses.size() < MastodonHelper.statusesPerCall(requireActivity()));
            if (timelineType == Timeline.TimeLineEnum.ART) {
                //We have to split media in different statuses
                List<Status> mediaStatuses = new ArrayList<>();
                for (Status status : fetched_statuses.statuses) {
                    if (status.media_attachments.size() > 1) {
                        for (Attachment attachment : status.media_attachments) {
                            status.media_attachments = new ArrayList<>();
                            status.media_attachments.add(0, attachment);
                            mediaStatuses.add(status);
                        }
                    }
                }
                fetched_statuses.statuses = mediaStatuses;
            }
            //There are some statuses present in the timeline
            int startId = statuses.size();
            if (direction == DIRECTION.TOP) {
                statuses.addAll(0, fetched_statuses.statuses);
                statusAdapter.notifyItemRangeInserted(0, fetched_statuses.statuses.size());
                //Maybe a better solution but max_id excludes fetched id, so when fetching with min_id we have to scroll top of one status to get it.
                if (fetched_statuses.statuses.size() > 0) {
                    binding.recyclerView.scrollToPosition(fetched_statuses.statuses.size() - 1);
                }
            } else {
                statuses.addAll(fetched_statuses.statuses);
                statusAdapter.notifyItemRangeInserted(startId, fetched_statuses.statuses.size());
            }
            if (max_id == null || (fetched_statuses.pagination.max_id != null && fetched_statuses.pagination.max_id.compareTo(max_id) < 0)) {
                max_id = fetched_statuses.pagination.max_id;
            }
            if (min_id == null || (fetched_statuses.pagination.min_id != null && fetched_statuses.pagination.min_id.compareTo(min_id) > 0)) {
                min_id = fetched_statuses.pagination.min_id;
            }
        } else if (direction == DIRECTION.BOTTOM) {
            flagLoading = true;
        }
    }

    @Override
    public void onPause() {
     /*   if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            new Thread(() -> {
                try {
                    new QuickLoad(requireActivity()).storeTimeline(position, timelineType, statuses, ident);
                } catch (Exception ignored) {
                }
            }).start();
        }*/
        storeMarker();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        //Update last read id for home timeline
        if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            new Thread(() -> {
                try {
                    new QuickLoad(requireActivity()).storeTimeline(position, user_id, instance, timelineType, statuses, ident);
                } catch (Exception ignored) {
                }
            }).start();
        }
        storeMarker();
        if (binding != null) {
            binding.recyclerView.setAdapter(null);
        }
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receive_action);
        statusAdapter = null;
        binding = null;
        super.onDestroyView();
    }

    private void storeMarker() {
        if (timelineType == Timeline.TimeLineEnum.HOME && mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            if (statuses != null && statuses.size() > position) {
                try {
                    Status status = statuses.get(position);
                    timelinesVM.addMarker(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, null);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void router(DIRECTION direction) {
        if (networkAvailable == BaseMainActivity.status.UNKNOWN) {
            new Thread(() -> {
                if (networkAvailable == BaseMainActivity.status.UNKNOWN) {
                    networkAvailable = Helper.isConnectedToInternet(requireActivity(), BaseMainActivity.currentInstance);
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> route(direction);
                mainHandler.post(myRunnable);
            }).start();
        } else {
            route(direction);
        }
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(DIRECTION direction) {

        new Thread(() -> {
            QuickLoad quickLoad = new QuickLoad(requireActivity()).getSavedValue(MainActivity.currentUserID, MainActivity.currentInstance, timelineType, ident);
            if (binding == null) {
                return;
            }
            if (!binding.swipeContainer.isRefreshing() && direction == null && quickLoad != null && quickLoad.statuses != null && quickLoad.statuses.size() > 0) {
                Statuses statuses = new Statuses();
                statuses.statuses = quickLoad.statuses;
                statuses.pagination = new Pagination();
                statuses.pagination.max_id = quickLoad.statuses.get(quickLoad.statuses.size() - 1).id;
                statuses.pagination.min_id = quickLoad.statuses.get(0).id;
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> initializeStatusesCommonView(statuses);
                mainHandler.post(myRunnable);
            } else {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    // --- HOME TIMELINE ---
                    if (timelineType == Timeline.TimeLineEnum.HOME) {
                        //for more visibility it's done through loadHomeStrategy method
                        loadHomeStrategy(direction);
                    } else if (timelineType == Timeline.TimeLineEnum.LOCAL) { //LOCAL TIMELINE
                        if (direction == null) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP));
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.PUBLIC) { //PUBLIC TIMELINE
                        if (direction == null) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP));
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.REMOTE) { //REMOTE TIMELINE
                        if (direction == null) {
                            timelinesVM.getPublic(null, remoteInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getPublic(null, remoteInstance, true, false, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getPublic(null, remoteInstance, true, false, false, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP));
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.LIST) { //LIST TIMELINE
                        if (direction == null) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP));
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.TAG || timelineType == Timeline.TimeLineEnum.ART) { //TAG TIMELINE
                        if (tagTimeline == null) {
                            tagTimeline = new TagTimeline();
                            tagTimeline.name = search;
                        }
                        if (direction == null) {
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP));
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.ACCOUNT_TIMELINE) { //PROFILE TIMELINES
                        if (direction == null) {
                            if (show_pinned) {
                                //Fetch pinned statuses to display them at the top
                                accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, null, null, false, true, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), pinnedStatuses -> accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                                                .observe(getViewLifecycleOwner(), otherStatuses -> {
                                                    if (otherStatuses != null && otherStatuses.statuses != null) {
                                                        otherStatuses.statuses.addAll(0, pinnedStatuses.statuses);
                                                        initializeStatusesCommonView(otherStatuses);
                                                    }
                                                }));
                            } else {
                                accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                            }
                        } else if (direction == DIRECTION.BOTTOM) {
                            accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, max_id, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else {
                            flagLoading = false;
                        }
                    } else if (search != null) {
                        SearchVM searchVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, SearchVM.class);
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, null, false, true, false, 0, null, null, MastodonHelper.STATUSES_PER_CALL)
                                .observe(getViewLifecycleOwner(), results -> {
                                    Statuses statuses = new Statuses();
                                    statuses.statuses = results.statuses;
                                    statuses.pagination = new Pagination();
                                    initializeStatusesCommonView(statuses);
                                });
                    } else if (searchCache != null) {
                        SearchVM searchVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, SearchVM.class);
                        searchVM.searchCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, searchCache.trim())
                                .observe(getViewLifecycleOwner(), results -> {
                                    Statuses statuses = new Statuses();
                                    statuses.statuses = results.statuses;
                                    statuses.pagination = new Pagination();
                                    initializeStatusesCommonView(statuses);
                                });
                    } else if (timelineType == Timeline.TimeLineEnum.FAVOURITE_TIMELINE) {
                        if (direction == null) {
                            accountsVM.getFavourites(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, null)
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            accountsVM.getFavourites(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, max_id)
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else {
                            flagLoading = false;
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.BOOKMARK_TIMELINE) {
                        if (direction == null) {
                            accountsVM.getBookmarks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, null, null)
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            accountsVM.getBookmarks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), max_id, null, null)
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                        } else {
                            flagLoading = false;
                        }
                    }
                };
                mainHandler.post(myRunnable);
            }

        }).start();

    }

    /**
     * Load home timeline strategy
     *
     * @param direction - DIRECTION enum
     */
    private void loadHomeStrategy(DIRECTION direction) {
        //When no direction is provided, it means it's the first call
        if (direction == null) {
            //Two ways, depending of the Internet connection
            //Connection is available toots are loaded remotely
            if (networkAvailable == BaseMainActivity.status.CONNECTED) {
                boolean fetchMarker = false;
                if (markers.isEmpty()) {
                    markers.add("home");
                    fetchMarker = true;
                }
                //We search for marker only once - It should not be fetched again when pull to refresh
                if (fetchMarker && !binding.swipeContainer.isRefreshing()) {
                    //Search for last position
                    timelinesVM.getMarker(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, markers).observe(getViewLifecycleOwner(), marker -> {
                        if (marker != null) {
                            Marker.MarkerContent markerContent = marker.home;
                            if (markerContent != null) {
                                max_id = markerContent.last_read_id;
                                min_id = markerContent.last_read_id;
                            } else {
                                max_id = null;
                            }
                        } else {
                            max_id = null;
                        }
                        timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                                .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                    });
                } else {
                    timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                            .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                }

            } else {
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, null)
                        .observe(getViewLifecycleOwner(), cachedStatus -> {
                            if (cachedStatus != null && cachedStatus.statuses != null) {
                                initializeStatusesCommonView(cachedStatus);
                            }
                        });

            }
        } else if (direction == DIRECTION.BOTTOM) {
            if (networkAvailable == BaseMainActivity.status.CONNECTED) {
                //We first if we get results from cache
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, max_id, null)
                        .observe(getViewLifecycleOwner(), statusesBottomCache -> {
                            if (statusesBottomCache != null && statusesBottomCache.statuses != null && statusesBottomCache.statuses.size() > 0) {
                                dealWithPagination(statusesBottomCache, DIRECTION.BOTTOM);
                            } else { // If not, we fetch remotely
                                timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
                            }
                        });

            } else {
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, max_id, null)
                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM));
            }
        } else if (direction == DIRECTION.TOP) {
            if (networkAvailable == BaseMainActivity.status.CONNECTED) {
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, min_id)
                        .observe(getViewLifecycleOwner(), statusesTopCache -> {
                            if (statusesTopCache != null && statusesTopCache.statuses != null && statusesTopCache.statuses.size() > 0) {
                                dealWithPagination(statusesTopCache, DIRECTION.TOP);
                            } else {
                                timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()), false)
                                        .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP));
                            }
                        });
            } else {
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, min_id)
                        .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP));
            }
        }
    }


    /**
     * Refresh status in list
     */
    public void refreshAllAdapters() {
        if (statusAdapter != null && statuses != null) {
            statusAdapter.notifyItemRangeChanged(0, statuses.size());
        }
    }

    public enum DIRECTION {
        TOP,
        BOTTOM
    }
}