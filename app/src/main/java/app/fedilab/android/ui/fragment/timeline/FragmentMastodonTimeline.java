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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Marker;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.api.Statuses;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.QuickLoad;
import app.fedilab.android.client.entities.app.RemoteInstance;
import app.fedilab.android.client.entities.app.TagTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.StatusAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;
import es.dmoral.toasty.Toasty;


public class FragmentMastodonTimeline extends Fragment implements StatusAdapter.FetchMoreCallBack {


    private static final int STATUS_PRESENT = -1;
    private static final int STATUS_AT_THE_BOTTOM = -2;
    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private AccountsVM accountsVM;
    private boolean flagLoading;
    private List<Status> statuses;
    private String search, searchCache;
    private Status statusReport;
    private String max_id, min_id, min_id_fetch_more, max_id_fetch_more;
    private StatusAdapter statusAdapter;
    private Timeline.TimeLineEnum timelineType;
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
                        statuses.get(position).reblogged = receivedStatus.reblogged;
                        statuses.get(position).favourited = receivedStatus.favourited;
                        statuses.get(position).bookmarked = receivedStatus.bookmarked;
                        statuses.get(position).reblogs_count = receivedStatus.reblogs_count;
                        statuses.get(position).favourites_count = receivedStatus.favourites_count;
                        statusAdapter.notifyItemChanged(position);
                    }
                } else if (delete_statuses_for_user != null && statusAdapter != null) {
                    List<Status> statusesToRemove = new ArrayList<>();
                    for (Status status : statuses) {
                        if (status != null && status.account != null && status.account.id != null && status.account.id.equals(delete_statuses_for_user)) {
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
                    statuses.add(0, statusPosted);
                    statusAdapter.notifyItemInserted(0);
                }
            }
        }
    };
    private List<String> markers;
    private String list_id;
    private TagTimeline tagTimeline;
    private LinearLayoutManager mLayoutManager;
    private Account accountTimeline;
    private boolean exclude_replies, exclude_reblogs, show_pinned, media_only, minified;
    private String viewModelKey, remoteInstance;
    private PinnedTimeline pinnedTimeline;
    private String ident;
    private String instance, user_id;

    private boolean canBeFederated;

    /**
     * Return the position of the status in the ArrayList
     *
     * @param status - Status to fetch
     * @return position or -1 if not found
     */
    private int getPosition(Status status) {
        int position = 0;
        boolean found = false;
        if (status.id == null) {
            return -1;
        }
        for (Status _status : statuses) {
            if (_status.id != null && _status.id.compareTo(status.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
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
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            route(DIRECTION.SCROLL_TOP, true);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        timelineType = Timeline.TimeLineEnum.HOME;
        instance = BaseMainActivity.currentInstance;
        user_id = BaseMainActivity.currentUserID;
        canBeFederated = true;
        if (getArguments() != null) {
            timelineType = (Timeline.TimeLineEnum) getArguments().get(Helper.ARG_TIMELINE_TYPE);
            list_id = getArguments().getString(Helper.ARG_LIST_ID, null);
            search = getArguments().getString(Helper.ARG_SEARCH_KEYWORD, null);
            searchCache = getArguments().getString(Helper.ARG_SEARCH_KEYWORD_CACHE, null);
            pinnedTimeline = (PinnedTimeline) getArguments().getSerializable(Helper.ARG_REMOTE_INSTANCE);
            if (pinnedTimeline != null && pinnedTimeline.remoteInstance != null) {
                if (pinnedTimeline.remoteInstance.type != RemoteInstance.InstanceType.NITTER) {
                    remoteInstance = pinnedTimeline.remoteInstance.host;
                } else {
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                    remoteInstance = sharedpreferences.getString(getString(R.string.SET_NITTER_HOST), getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
                    canBeFederated = false;
                }
            }

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
            if (pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER) {
                ident = "@R@" + pinnedTimeline.remoteInstance.host;
            } else {
                ident = "@R@" + remoteInstance;
            }
        } else if (search != null) {
            ident = "@S@" + search;
        } else {
            ident = null;
        }
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION));
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));

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

        return binding.getRoot();
    }

    private void initializeStatusesCommonView(final Statuses statuses) {
        initializeStatusesCommonView(statuses, -1);
    }

    /**
     * Intialize the common view for statuses on different timelines
     *
     * @param statuses {@link Statuses}
     */
    private void initializeStatusesCommonView(final Statuses statuses, int position) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        if (searchCache == null && timelineType != Timeline.TimeLineEnum.TREND_MESSAGE) {
            binding.swipeContainer.setOnRefreshListener(() -> {
                binding.swipeContainer.setRefreshing(true);
                flagLoading = false;
                route(DIRECTION.REFRESH, true);
            });
        }

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
        flagLoading = statuses.pagination.max_id == null;
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
        if (min_id == null || (statuses.pagination.min_id != null && statuses.pagination.min_id.compareTo(min_id) > 0)) {
            min_id = statuses.pagination.min_id;
        }
        statusAdapter = new StatusAdapter(this.statuses, timelineType, minified, canBeFederated);
        statusAdapter.fetchMoreCallBack = this;
        if (statusReport != null) {
            scrollToTop();
        }
        mLayoutManager = new LinearLayoutManager(requireActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusAdapter);

        if (position != -1 && position < this.statuses.size()) {
            binding.recyclerView.scrollToPosition(position);
        }

        if (searchCache == null && timelineType != Timeline.TimeLineEnum.TREND_MESSAGE) {
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
    private synchronized void dealWithPagination(Statuses fetched_statuses, DIRECTION direction, boolean fetchingMissing) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (statuses != null && fetched_statuses != null && fetched_statuses.statuses != null && fetched_statuses.statuses.size() > 0) {
            flagLoading = fetched_statuses.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);
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
            //Update the timeline with new statuses
            updateStatusListWith(direction, fetched_statuses.statuses, fetchingMissing);
            if (!fetchingMissing) {
                if (fetched_statuses.pagination.max_id == null) {
                    flagLoading = true;
                } else if (max_id == null || fetched_statuses.pagination.max_id.compareTo(max_id) < 0) {
                    max_id = fetched_statuses.pagination.max_id;
                }
                if (min_id == null || (fetched_statuses.pagination.min_id != null && fetched_statuses.pagination.min_id.compareTo(min_id) > 0)) {
                    min_id = fetched_statuses.pagination.min_id;
                }
            }
        } else if (direction == DIRECTION.BOTTOM) {
            flagLoading = true;
        }
        if (direction == DIRECTION.SCROLL_TOP) {
            binding.recyclerView.scrollToPosition(0);
        }
    }

    /**
     * Update the timeline with received statuses
     *
     * @param statusListReceived - List<Status> Statuses received
     * @param fetchingMissing    - boolean if the call concerns fetching messages (ie: refresh of from fetch more button)
     */
    private void updateStatusListWith(DIRECTION direction, List<Status> statusListReceived, boolean fetchingMissing) {
        int numberInserted = 0;
        int lastInsertedPosition = 0;
        int initialInsertedPosition = STATUS_PRESENT;
        if (statusListReceived != null && statusListReceived.size() > 0) {
            int insertedPosition = STATUS_PRESENT;
            for (Status statusReceived : statusListReceived) {
                insertedPosition = insertStatus(statusReceived);
                if (insertedPosition != STATUS_PRESENT && insertedPosition != STATUS_AT_THE_BOTTOM) {
                    numberInserted++;
                    //Find the first position of insertion, the initial id is set to STATUS_PRESENT
                    if (initialInsertedPosition == STATUS_PRESENT) {
                        initialInsertedPosition = insertedPosition;
                    }
                    //If next statuses have a lower id, there are inserted before (normally, that should not happen)
                    if (insertedPosition < initialInsertedPosition) {
                        initialInsertedPosition = lastInsertedPosition;
                    }
                }
            }
            lastInsertedPosition = initialInsertedPosition + numberInserted;
            //lastInsertedPosition contains the position of the last inserted status
            //If there were no overlap for top status
            if (fetchingMissing && insertedPosition != STATUS_PRESENT && insertedPosition != STATUS_AT_THE_BOTTOM && this.statuses.size() > insertedPosition && numberInserted == MastodonHelper.statusesPerCall(requireActivity())) {
                Status statusFetchMore = new Status();
                statusFetchMore.isFetchMore = true;
                statusFetchMore.id = Helper.generateString();
                int insertAt;
                if (direction == DIRECTION.REFRESH || direction == DIRECTION.BOTTOM || direction == DIRECTION.SCROLL_TOP) {
                    insertAt = lastInsertedPosition;
                } else {
                    insertAt = initialInsertedPosition;
                }

                this.statuses.add(insertAt, statusFetchMore);
                statusAdapter.notifyItemInserted(insertAt);
                if (direction == DIRECTION.TOP && lastInsertedPosition + 1 < statuses.size()) {
                    binding.recyclerView.scrollToPosition(lastInsertedPosition + 1);
                }
            }
        }
    }

    /**
     * Insert a status if not yet in the timeline and returns its position of insertion
     *
     * @param statusReceived - Status coming from the api/db
     * @return int >= 0 |  STATUS_PRESENT = -1 | STATUS_AT_THE_BOTTOM = -2
     */
    private int insertStatus(Status statusReceived) {
        int position = 0;
        if (this.statuses != null) {
            if (this.statuses.contains(statusReceived)) {
                return STATUS_PRESENT;
            }
            statusAdapter.notifyItemRangeChanged(0, this.statuses.size());
            //We loop through messages already in the timeline
            for (Status statusAlreadyPresent : this.statuses) {
                //We compare the date of each status and we only add status having a date greater than the another, it is inserted at this position
                //Pinned messages are ignored because their date can be older
                if (statusReceived.id.compareTo(statusAlreadyPresent.id) > 0 && !statusAlreadyPresent.pinned) {
                    //We add the status to a list of id - thus we know it is already in the timeline
                    this.statuses.add(position, statusReceived);
                    statusAdapter.notifyItemInserted(position);
                    break;
                }
                position++;
            }
            //Statuses added at the bottom, we flag them by position = -2 for not dealing with them and fetch more
            if (position == this.statuses.size()) {
                //We add the status to a list of id - thus we know it is already in the timeline
                this.statuses.add(position, statusReceived);
                statusAdapter.notifyItemInserted(position);
                return STATUS_AT_THE_BOTTOM;
            }
        }

        return position;
    }

    @Override
    public void onPause() {
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
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        //Update last read id for home timeline
        if (isAdded()) {
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
        }
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receive_action);
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
                Runnable myRunnable = () -> route(direction, false);
                mainHandler.post(myRunnable);
            }).start();
        } else {
            route(direction, false);
        }
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(DIRECTION direction, boolean fetchingMissing) {
        new Thread(() -> {
            if (binding == null || getActivity() == null || !isAdded()) {
                return;
            }
            boolean nitterInstance = timelineType == Timeline.TimeLineEnum.REMOTE && pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER;
            QuickLoad quickLoad = new QuickLoad(requireActivity()).getSavedValue(BaseMainActivity.currentUserID, BaseMainActivity.currentInstance, timelineType, ident);
            if (!nitterInstance && !fetchingMissing && !binding.swipeContainer.isRefreshing() && direction == null && quickLoad != null && quickLoad.statuses != null && quickLoad.statuses.size() > 0) {
                Statuses statuses = new Statuses();
                statuses.statuses = quickLoad.statuses;
                statuses.pagination = new Pagination();
                statuses.pagination.max_id = quickLoad.statuses.get(quickLoad.statuses.size() - 1).id;
                statuses.pagination.min_id = quickLoad.statuses.get(0).id;

                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> initializeStatusesCommonView(statuses, quickLoad.position);
                mainHandler.post(myRunnable);
            } else {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (!isAdded()) {
                        return;
                    }
                    // --- HOME TIMELINE ---
                    if (timelineType == Timeline.TimeLineEnum.HOME) {
                        //for more visibility it's done through loadHomeStrategy method
                        loadHomeStrategy(direction, fetchingMissing);
                    } else if (timelineType == Timeline.TimeLineEnum.LOCAL) { //LOCAL TIMELINE
                        if (direction == null) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, fetchingMissing ? max_id_fetch_more : max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP, fetchingMissing));
                        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                        if (statusAdapter != null) {
                                            dealWithPagination(statusesRefresh, direction, true);
                                        } else {
                                            initializeStatusesCommonView(statusesRefresh);
                                        }
                                    });
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.PUBLIC) { //PUBLIC TIMELINE
                        if (direction == null) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, fetchingMissing ? max_id_fetch_more : max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP, fetchingMissing));
                        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                            timelinesVM.getPublic(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, false, true, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                        if (statusAdapter != null) {
                                            dealWithPagination(statusesRefresh, direction, true);
                                        } else {
                                            initializeStatusesCommonView(statusesRefresh);
                                        }
                                    });
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.REMOTE) { //REMOTE TIMELINE

                        //NITTER TIMELINES
                        if (pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER) {
                            if (direction == null) {
                                timelinesVM.getNitter(pinnedTimeline.remoteInstance.host, null)
                                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                            } else if (direction == DIRECTION.BOTTOM) {
                                timelinesVM.getNitter(pinnedTimeline.remoteInstance.host, max_id)
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                            } else if (direction == DIRECTION.TOP) {
                                flagLoading = false;
                            } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                                timelinesVM.getNitter(pinnedTimeline.remoteInstance.host, null)
                                        .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                            if (statusAdapter != null) {
                                                dealWithPagination(statusesRefresh, direction, true);
                                            } else {
                                                initializeStatusesCommonView(statusesRefresh);
                                            }
                                        });
                            }
                        } //GNU TIMELINES
                        else if (pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.GNU) {

                        }//MISSKEY TIMELINES
                        else if (pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.MISSKEY) {
                            if (direction == null) {
                                timelinesVM.getMisskey(remoteInstance, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                            } else if (direction == DIRECTION.BOTTOM) {
                                timelinesVM.getMisskey(remoteInstance, max_id, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                            } else if (direction == DIRECTION.TOP) {
                                flagLoading = false;
                            } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                                timelinesVM.getMisskey(remoteInstance, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                            if (statusAdapter != null) {
                                                dealWithPagination(statusesRefresh, direction, true);
                                            } else {
                                                initializeStatusesCommonView(statusesRefresh);
                                            }
                                        });
                            }
                        } //PEERTUBE TIMELINES
                        else if (pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.PEERTUBE) {
                            if (direction == null) {

                                timelinesVM.getPeertube(remoteInstance, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                            } else if (direction == DIRECTION.BOTTOM) {
                                timelinesVM.getPeertube(remoteInstance, String.valueOf(statuses.size()), MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                            } else if (direction == DIRECTION.TOP) {
                                flagLoading = false;
                            } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                                timelinesVM.getPeertube(remoteInstance, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                            if (statusAdapter != null) {
                                                dealWithPagination(statusesRefresh, direction, true);
                                            } else {
                                                initializeStatusesCommonView(statusesRefresh);
                                            }
                                        });
                            }
                        } else { //Other remote timelines
                            if (direction == null) {
                                timelinesVM.getPublic(null, remoteInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                            } else if (direction == DIRECTION.BOTTOM) {
                                timelinesVM.getPublic(null, remoteInstance, true, false, false, fetchingMissing ? max_id_fetch_more : max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing));
                            } else if (direction == DIRECTION.TOP) {
                                timelinesVM.getPublic(null, remoteInstance, true, false, false, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP, fetchingMissing));
                            } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                                timelinesVM.getPublic(null, remoteInstance, true, false, false, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                            if (statusAdapter != null) {
                                                dealWithPagination(statusesRefresh, direction, true);
                                            } else {
                                                initializeStatusesCommonView(statusesRefresh);
                                            }
                                        });
                            }
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.LIST) { //LIST TIMELINE
                        if (direction == null) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, fetchingMissing ? max_id_fetch_more : max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP, fetchingMissing));
                        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                            timelinesVM.getList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, list_id, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                        if (statusAdapter != null) {
                                            dealWithPagination(statusesRefresh, direction, true);
                                        } else {
                                            initializeStatusesCommonView(statusesRefresh);
                                        }
                                    });
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
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, fetchingMissing ? max_id_fetch_more : max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing));
                        } else if (direction == DIRECTION.TOP) {
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, null, null, fetchingMissing ? min_id_fetch_more : min_id, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.TOP, fetchingMissing));
                        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
                            timelinesVM.getHashTag(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, tagTimeline.name, false, tagTimeline.isART, tagTimeline.all, tagTimeline.any, tagTimeline.none, null, null, null, MastodonHelper.statusesPerCall(requireActivity()))
                                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                                        if (statusAdapter != null) {
                                            dealWithPagination(statusesRefresh, direction, true);
                                        } else {
                                            initializeStatusesCommonView(statusesRefresh);
                                        }
                                    });
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.ACCOUNT_TIMELINE) { //PROFILE TIMELINES
                        if (direction == null) {
                            if (show_pinned) {
                                //Fetch pinned statuses to display them at the top
                                accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, null, null, false, true, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), pinnedStatuses -> accountsVM.getAccountStatuses(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountTimeline.id, null, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                                                .observe(getViewLifecycleOwner(), otherStatuses -> {
                                                    if (otherStatuses != null && otherStatuses.statuses != null && pinnedStatuses != null && pinnedStatuses.statuses != null) {
                                                        for (Status status : pinnedStatuses.statuses) {
                                                            status.pinned = true;
                                                        }
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
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                        } else {
                            flagLoading = false;
                        }
                    } else if (search != null) {
                        SearchVM searchVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, SearchVM.class);
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, null, false, true, false, 0, null, null, MastodonHelper.STATUSES_PER_CALL)
                                .observe(getViewLifecycleOwner(), results -> {
                                    if (results != null) {
                                        Statuses statuses = new Statuses();
                                        statuses.statuses = results.statuses;
                                        statuses.pagination = new Pagination();
                                        initializeStatusesCommonView(statuses);
                                    } else {
                                        Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                                    }
                                });
                    } else if (searchCache != null) {
                        SearchVM searchVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, SearchVM.class);
                        searchVM.searchCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, searchCache.trim())
                                .observe(getViewLifecycleOwner(), results -> {
                                    if (results != null) {
                                        Statuses statuses = new Statuses();
                                        statuses.statuses = results.statuses;
                                        statuses.pagination = new Pagination();
                                        initializeStatusesCommonView(statuses);
                                    } else {
                                        Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                                    }
                                });
                    } else if (timelineType == Timeline.TimeLineEnum.FAVOURITE_TIMELINE) {
                        if (direction == null) {
                            accountsVM.getFavourites(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, null)
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            accountsVM.getFavourites(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, max_id)
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                        } else {
                            flagLoading = false;
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.BOOKMARK_TIMELINE) {
                        if (direction == null) {
                            accountsVM.getBookmarks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), null, null, null)
                                    .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                        } else if (direction == DIRECTION.BOTTOM) {
                            accountsVM.getBookmarks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, String.valueOf(MastodonHelper.statusesPerCall(requireActivity())), max_id, null, null)
                                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                        } else {
                            flagLoading = false;
                        }
                    } else if (timelineType == Timeline.TimeLineEnum.TREND_MESSAGE) {
                        if (direction == null) {
                            timelinesVM.getStatusTrends(BaseMainActivity.currentToken, BaseMainActivity.currentInstance)
                                    .observe(getViewLifecycleOwner(), statusesTrends -> {
                                        Statuses statuses = new Statuses();
                                        statuses.statuses = new ArrayList<>();
                                        if (statusesTrends != null) {
                                            statuses.statuses.addAll(statusesTrends);
                                        }
                                        statuses.pagination = new Pagination();
                                        initializeStatusesCommonView(statuses);
                                    });
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
    private void loadHomeStrategy(DIRECTION direction, boolean fetchingMissing) {
        //When no direction is provided, it means it's the first call
        if (direction == null && !fetchingMissing) {
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
                        timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                                .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                    });
                } else {
                    timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                            .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                }

            } else {
                timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, null, null)
                        .observe(getViewLifecycleOwner(), cachedStatus -> {
                            if (cachedStatus != null && cachedStatus.statuses != null) {
                                initializeStatusesCommonView(cachedStatus);
                            }
                        });

            }
        } else if (direction == DIRECTION.BOTTOM) {
            if (!fetchingMissing) {
                if (networkAvailable == BaseMainActivity.status.CONNECTED) {
                    //We first if we get results from cache
                    timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, max_id, null, null)
                            .observe(getViewLifecycleOwner(), statusesBottomCache -> {
                                if (statusesBottomCache != null && statusesBottomCache.statuses != null && statusesBottomCache.statuses.size() > 0) {
                                    dealWithPagination(statusesBottomCache, DIRECTION.BOTTOM, false);
                                } else { // If not, we fetch remotely
                                    timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, false, max_id, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                                            .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                                }
                            });

                } else {
                    timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, max_id, null, null)
                            .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
                }
            } else {
                timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, true, max_id_fetch_more, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, true));
            }
        } else if (direction == DIRECTION.TOP) {
            if (!fetchingMissing) {
                if (networkAvailable == BaseMainActivity.status.CONNECTED) {
                    timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, min_id, null)
                            .observe(getViewLifecycleOwner(), statusesTopCache -> {
                                if (statusesTopCache != null && statusesTopCache.statuses != null && statusesTopCache.statuses.size() > 0) {
                                    dealWithPagination(statusesTopCache, DIRECTION.TOP, false);
                                } else {
                                    timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, false, null, null, min_id, MastodonHelper.statusesPerCall(requireActivity()), false)
                                            .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP, false));
                                }
                            });
                } else {
                    timelinesVM.getHomeCache(BaseMainActivity.currentInstance, BaseMainActivity.currentUserID, null, min_id, null)
                            .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP, false));
                }
            } else {
                timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, true, null, null, min_id_fetch_more, MastodonHelper.statusesPerCall(requireActivity()), false)
                        .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP, true));
            }

        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP) {
            timelinesVM.getHome(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, true, null, null, null, MastodonHelper.statusesPerCall(requireActivity()), false)
                    .observe(getViewLifecycleOwner(), statusRefresh -> {
                                if (statusAdapter != null) {
                                    dealWithPagination(statusRefresh, direction, true);
                                } else {
                                    initializeStatusesCommonView(statusRefresh);
                                }
                            }
                    );
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

    @Override
    public void onClickMinId(String min_id, String id) {
        //Fetch more has been pressed
        min_id_fetch_more = min_id;
        Status status = null;
        int position = 0;
        for (Status currentStatus : this.statuses) {
            if (currentStatus.id.compareTo(id) == 0) {
                status = currentStatus;
                break;
            }
            position++;
        }
        if (status != null) {
            this.statuses.remove(position);
            statusAdapter.notifyItemRemoved(position);
        }
        route(DIRECTION.TOP, true);
    }

    @Override
    public void onClickMaxId(String max_id, String id) {
        max_id_fetch_more = max_id;
        Status status = null;
        int position = 0;
        for (Status currentStatus : this.statuses) {
            if (currentStatus.id.compareTo(id) == 0) {
                status = currentStatus;
                break;
            }
            position++;
        }
        if (status != null) {
            this.statuses.remove(position);
            statusAdapter.notifyItemRemoved(position);
        }
        route(DIRECTION.BOTTOM, true);
    }

    public enum DIRECTION {
        TOP,
        BOTTOM,
        REFRESH,
        SCROLL_TOP
    }
}