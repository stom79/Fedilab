package app.fedilab.android.mastodon.ui.fragment.timeline;
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


import static app.fedilab.android.BaseMainActivity.currentInstance;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.client.entities.app.BubbleTimeline;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.RemoteInstance;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.TagTimeline;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.CrossActionHelper;
import app.fedilab.android.mastodon.helper.GlideApp;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;
import es.dmoral.toasty.Toasty;


public class FragmentMastodonTimeline extends Fragment implements StatusAdapter.FetchMoreCallBack {


    private static final int PRELOAD_AHEAD_ITEMS = 10;
    public UpdateCounters update;
    private FragmentPaginationBinding binding;
    private TimelinesVM timelinesVM;
    private AccountsVM accountsVM;
    private boolean flagLoading;
    private String search, searchCache;
    private Status statusReport;
    private String max_id, min_id, min_id_fetch_more, max_id_fetch_more;
    private Integer offset;
    private StatusAdapter statusAdapter;
    private Timeline.TimeLineEnum timelineType;
    private List<Status> timelineStatuses;
    //Handle actions that can be done in other fragments
    private final BroadcastReceiver receive_action = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                Status receivedStatus = (Status) b.getSerializable(Helper.ARG_STATUS_ACTION);
                String delete_statuses_for_user = b.getString(Helper.ARG_STATUS_ACCOUNT_ID_DELETED);
                String delete_all_for_account_id = b.getString(Helper.ARG_DELETE_ALL_FOR_ACCOUNT_ID);
                Status status_to_delete = (Status) b.getSerializable(Helper.ARG_STATUS_DELETED);
                Status status_to_update = (Status) b.getSerializable(Helper.ARG_STATUS_UPDATED);
                Status statusPosted = (Status) b.getSerializable(Helper.ARG_STATUS_DELETED);
                boolean refreshAll = b.getBoolean(Helper.ARG_TIMELINE_REFRESH_ALL, false);
                if (receivedStatus != null && statusAdapter != null) {
                    int position = getPosition(receivedStatus);
                    if (position >= 0) {
                        if (receivedStatus.reblog != null) {
                            timelineStatuses.get(position).reblog = receivedStatus.reblog;
                        }
                        if (timelineStatuses.get(position).reblog != null) {
                            timelineStatuses.get(position).reblog.reblogged = receivedStatus.reblogged;
                            timelineStatuses.get(position).reblog.favourited = receivedStatus.favourited;
                            timelineStatuses.get(position).reblog.bookmarked = receivedStatus.bookmarked;
                            timelineStatuses.get(position).reblog.reblogs_count = receivedStatus.reblogs_count;
                            timelineStatuses.get(position).reblog.favourites_count = receivedStatus.favourites_count;
                        } else {
                            timelineStatuses.get(position).reblogged = receivedStatus.reblogged;
                            timelineStatuses.get(position).favourited = receivedStatus.favourited;
                            timelineStatuses.get(position).bookmarked = receivedStatus.bookmarked;
                            timelineStatuses.get(position).reblogs_count = receivedStatus.reblogs_count;
                            timelineStatuses.get(position).favourites_count = receivedStatus.favourites_count;
                        }


                        statusAdapter.notifyItemChanged(position);
                    }
                } else if (delete_statuses_for_user != null && statusAdapter != null) {
                    List<Status> statusesToRemove = new ArrayList<>();
                    for (Status status : timelineStatuses) {
                        if (status != null && status.account != null && status.account.id != null && status.account.id.equals(delete_statuses_for_user)) {
                            statusesToRemove.add(status);
                        }
                    }
                    for (Status statusToRemove : statusesToRemove) {
                        int position = getPosition(statusToRemove);
                        if (position >= 0) {
                            timelineStatuses.remove(position);
                            statusAdapter.notifyItemRemoved(position);
                        }
                    }
                } else if (status_to_delete != null && statusAdapter != null) {
                    int position = getPosition(status_to_delete);
                    if (position >= 0) {
                        timelineStatuses.remove(position);
                        statusAdapter.notifyItemRemoved(position);
                    }
                } else if (status_to_update != null && statusAdapter != null) {
                    int position = getPosition(status_to_update);
                    if (position >= 0) {
                        timelineStatuses.set(position, status_to_update);
                        statusAdapter.notifyItemChanged(position);
                    }
                } else if (statusPosted != null && statusAdapter != null && timelineType == Timeline.TimeLineEnum.HOME) {
                    timelineStatuses.add(0, statusPosted);
                    statusAdapter.notifyItemInserted(0);
                } else if (delete_all_for_account_id != null) {
                    List<Status> toRemove = new ArrayList<>();
                    if (timelineStatuses != null) {
                        for (int position = 0; position < timelineStatuses.size(); position++) {
                            if (timelineStatuses.get(position).account.id.equals(delete_all_for_account_id)) {
                                toRemove.add(timelineStatuses.get(position));
                            }
                        }
                    }
                    if (toRemove.size() > 0) {
                        for (int i = 0; i < toRemove.size(); i++) {
                            int position = getPosition(toRemove.get(i));
                            if (position >= 0) {
                                timelineStatuses.remove(position);
                                statusAdapter.notifyItemRemoved(position);
                            }
                        }
                    }
                } else if (refreshAll) {
                    refreshAllAdapters();
                }
            }
        }
    };
    private ViewPreloadSizeProvider<Attachment> preloadSizeProvider;
    private boolean checkRemotely;
    private String accountIDInRemoteInstance;
    private boolean isViewInitialized;
    private Statuses initialStatuses;
    private String list_id;
    private TagTimeline tagTimeline;
    private BubbleTimeline bubbleTimeline;
    private LinearLayoutManager mLayoutManager;
    private Account accountTimeline;
    private boolean exclude_replies, exclude_reblogs, show_pinned, media_only, minified;
    private String viewModelKey, remoteInstance;
    private PinnedTimeline pinnedTimeline;
    private String ident;
    private String slug;
    private boolean canBeFederated;
    private boolean rememberPosition;
    private String publicTrendsDomain;
    private int lockForResumeCall;
    private boolean isNotPinnedTimeline;
    private int extraCalls;

    //Allow to recreate data when detaching/attaching fragment
    public void recreate() {
        initialStatuses = null;
        if (timelineStatuses != null && timelineStatuses.size() > 0) {
            int count = timelineStatuses.size();
            timelineStatuses.clear();
            timelineStatuses = new ArrayList<>();
            if (statusAdapter != null) {
                statusAdapter.notifyItemRangeRemoved(0, count);
                max_id = statusReport != null ? statusReport.id : null;
                offset = 0;
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                rememberPosition = sharedpreferences.getBoolean(getString(R.string.SET_REMEMBER_POSITION), true);
                //Inner marker are only for pinned timelines and main timelines, they have isViewInitialized set to false
                if (max_id == null && !isViewInitialized && rememberPosition) {
                    max_id = sharedpreferences.getString(getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, null);
                }
                //Only fragment in main view pager should not have the view initialized
                //AND Only the first fragment will initialize its view
                flagLoading = false;
                router(null);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isViewInitialized) {
            isViewInitialized = true;
            if (initialStatuses != null) {
                initializeStatusesCommonView(initialStatuses);
            } else {
                router(null);
            }
        } else {
            if (isNotPinnedTimeline && lockForResumeCall == 0) {
                router(null);
                lockForResumeCall++;
            } /*else if (!isNotPinnedTimeline) {
                router(null);
            }*/
        }
        if (timelineStatuses != null && timelineStatuses.size() > 0) {
            route(DIRECTION.FETCH_NEW, true);
        }
    }

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
        for (Status _status : timelineStatuses) {
            if (_status.reblog == null && _status.id != null && _status.id.compareTo(status.id) == 0) {
                found = true;
                break;
            } else if (_status.reblog != null && _status.reblog.id != null && _status.reblog.id.compareTo(status.id) == 0) {
                found = true;
                break;
            }
            position++;
        }
        return found ? position : -1;
    }


    /**
     * Return the position of the status in the ArrayList
     *
     * @param status - Status to fetch
     * @return position or -1 if not found
     */
    private int getAbsolutePosition(Status status) {
        int position = 0;
        boolean found = false;
        if (status.id == null) {
            return -1;
        }
        for (Status _status : timelineStatuses) {
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
        for (Status status : timelineStatuses) {
            if (status.isChecked) {
                stringList.add(status.id);
            }
        }
        return stringList;
    }

    public void scrollToTop() {
        if (binding != null && search == null) {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            route(DIRECTION.SCROLL_TOP, true);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        timelinesVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, TimelinesVM.class);
        accountsVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, AccountsVM.class);
        initialStatuses = null;
        lockForResumeCall = 0;
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        max_id = statusReport != null ? statusReport.id : null;
        offset = 0;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        rememberPosition = sharedpreferences.getBoolean(getString(R.string.SET_REMEMBER_POSITION), true);
        //Inner marker are only for pinned timelines and main timelines, they have isViewInitialized set to false
        if (max_id == null && !isViewInitialized && rememberPosition) {
            max_id = sharedpreferences.getString(getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, null);
        }
        if (search != null) {
            binding.swipeContainer.setRefreshing(false);
            binding.swipeContainer.setEnabled(false);
        }
        //Only fragment in main view pager should not have the view initialized
        //AND Only the first fragment will initialize its view
        flagLoading = false;
        extraCalls = -1;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        timelineType = Timeline.TimeLineEnum.HOME;
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
            if (timelineType == Timeline.TimeLineEnum.TREND_MESSAGE_PUBLIC) {
                canBeFederated = false;
            }
            publicTrendsDomain = getArguments().getString(Helper.ARG_REMOTE_INSTANCE_STRING, null);
            isViewInitialized = getArguments().getBoolean(Helper.ARG_INITIALIZE_VIEW, true);
            isNotPinnedTimeline = isViewInitialized;
            tagTimeline = (TagTimeline) getArguments().getSerializable(Helper.ARG_TAG_TIMELINE);
            bubbleTimeline = (BubbleTimeline) getArguments().getSerializable(Helper.ARG_BUBBLE_TIMELINE);
            accountTimeline = (Account) getArguments().getSerializable(Helper.ARG_ACCOUNT);
            exclude_replies = !getArguments().getBoolean(Helper.ARG_SHOW_REPLIES, true);
            checkRemotely = getArguments().getBoolean(Helper.ARG_CHECK_REMOTELY, false);
            show_pinned = getArguments().getBoolean(Helper.ARG_SHOW_PINNED, false);
            exclude_reblogs = !getArguments().getBoolean(Helper.ARG_SHOW_REBLOGS, true);
            media_only = getArguments().getBoolean(Helper.ARG_SHOW_MEDIA_ONY, false);
            viewModelKey = getArguments().getString(Helper.ARG_VIEW_MODEL_KEY, "");
            minified = getArguments().getBoolean(Helper.ARG_MINIFIED, false);
            statusReport = (Status) getArguments().getSerializable(Helper.ARG_STATUS_REPORT);
        }

        //When visiting a profile without being authenticated
        if (checkRemotely) {
            String[] acctArray = accountTimeline.acct.split("@");
            if (acctArray.length > 1) {
                remoteInstance = acctArray[1];
            }
            if (remoteInstance != null && remoteInstance.equalsIgnoreCase(currentInstance)) {
                checkRemotely = false;
            } else if (remoteInstance == null) {
                checkRemotely = false;
            }
        }
        if (tagTimeline != null) {
            ident = "@T@" + tagTimeline.name;
            if (tagTimeline.isART) {
                timelineType = Timeline.TimeLineEnum.ART;
            }
        } else if (bubbleTimeline != null) {
            ident = "@B@Bubble";
        } else if (list_id != null) {
            ident = "@l@" + list_id;
        } else if (remoteInstance != null && !checkRemotely) {
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
        if (timelineType != null) {
            slug = timelineType != Timeline.TimeLineEnum.ART ? timelineType.getValue() + (ident != null ? "|" + ident : "") : Timeline.TimeLineEnum.TAG.getValue() + (ident != null ? "|" + ident : "");
        }


        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(receive_action, new IntentFilter(Helper.RECEIVE_STATUS_ACTION));
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_statuses Statuses
     */
    private synchronized void dealWithPagination(Statuses fetched_statuses, DIRECTION direction, boolean fetchingMissing, Status statusToUpdate) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.swipeContainer.setRefreshing(false);
        binding.loadingNextElements.setVisibility(View.GONE);
        flagLoading = false;
        if (timelineStatuses != null && fetched_statuses != null && fetched_statuses.statuses != null && fetched_statuses.statuses.size() > 0) {
            try {
                if (statusToUpdate != null) {
                    new Thread(() -> {
                        StatusCache statusCache = new StatusCache();
                        statusCache.instance = BaseMainActivity.currentInstance;
                        statusCache.user_id = BaseMainActivity.currentUserID;
                        statusToUpdate.isFetchMore = false;
                        statusCache.status = statusToUpdate;
                        statusCache.status_id = statusToUpdate.id;
                        try {
                            new StatusCache(requireActivity()).updateIfExists(statusCache);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception ignored) {
            }
            flagLoading = fetched_statuses.pagination.max_id == null;
            binding.noAction.setVisibility(View.GONE);


            if (timelineType == Timeline.TimeLineEnum.ART) {
                //We have to split media in different statuses
                List<Status> mediaStatuses = new ArrayList<>();
                for (Status status : fetched_statuses.statuses) {
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
                    fetched_statuses.statuses = mediaStatuses;
                }
            }
            //Update the timeline with new statuses
            int insertedStatus;
            if (timelineType != Timeline.TimeLineEnum.TREND_MESSAGE_PUBLIC && timelineType != Timeline.TimeLineEnum.TREND_MESSAGE && search == null) {
                insertedStatus = updateStatusListWith(fetched_statuses.statuses);
            } else { //Trends cannot be ordered by id
                insertedStatus = fetched_statuses.statuses.size();
                int fromPosition = timelineStatuses.size();
                timelineStatuses.addAll(fetched_statuses.statuses);
                statusAdapter.notifyItemRangeInserted(fromPosition, insertedStatus);
            }
            //For these directions, the app will display counters for new messages
            if (insertedStatus >= 0 && update != null && direction != DIRECTION.FETCH_NEW && !fetchingMissing) {
                update.onUpdate(insertedStatus, timelineType, slug);
            } else if (update != null && insertedStatus == 0 && direction == DIRECTION.REFRESH) {
                update.onUpdate(0, timelineType, slug);
            }
            if (direction == DIRECTION.TOP && fetchingMissing) {
                int position = getAbsolutePosition(fetched_statuses.statuses.get(fetched_statuses.statuses.size() - 1));

                if (position != -1) {
                    binding.recyclerView.scrollToPosition(position + 1);
                }
            }
            if (!fetchingMissing) {
                if (fetched_statuses.pagination.max_id == null) {
                    flagLoading = true;
                } else if (max_id == null || Helper.compareTo(fetched_statuses.pagination.max_id, max_id) < 0 || timelineType.getValue().startsWith("TREND_")) {
                    max_id = fetched_statuses.pagination.max_id;
                }
                if (min_id == null || (fetched_statuses.pagination.min_id != null && Helper.compareTo(fetched_statuses.pagination.min_id, min_id) > 0)) {
                    min_id = fetched_statuses.pagination.min_id;
                }
            }
            if (search != null) {
                offset += MastodonHelper.SEARCH_PER_CALL;
            }
            int sizeBeforeFilter = 0;
            int filteredMessage = 0;
            int requestedMessages = MastodonHelper.statusesPerCall(requireActivity());
            sizeBeforeFilter = fetched_statuses.statuses.size();
            for (Status status : fetched_statuses.statuses) {
                if (status.filteredByApp != null) {
                    filteredMessage++;
                }
            }
            //TODO: keep for an improvement in beta
            /*
            int displayedMessages = sizeBeforeFilter - filteredMessage;
            if(displayedMessages < 5 && extraCalls < 8) {
                router(direction);
                if(extraCalls == -1) {
                    extraCalls = 1;
                } else {
                    extraCalls++;
                }
            }*/
        } else if (direction == DIRECTION.BOTTOM) {
            flagLoading = true;
        }
        if (direction == DIRECTION.SCROLL_TOP) {
            new Handler().postDelayed(() -> binding.recyclerView.scrollToPosition(0), 200);
        }

    }

    /**
     * Intialize the common view for statuses on different timelines
     *
     * @param statuses {@link Statuses}
     */
    private void initializeStatusesCommonView(final Statuses statuses) {
        flagLoading = false;
        if (!isViewInitialized) {
            return;
        }
        if (binding == null || !isAdded() || getActivity() == null) {
            if (binding != null) {
                binding.loader.setVisibility(View.GONE);
            }
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
        if (statusAdapter != null && timelineStatuses != null) {
            int size = timelineStatuses.size();
            timelineStatuses.clear();
            timelineStatuses = new ArrayList<>();
            statusAdapter.notifyItemRangeRemoved(0, size);
        }
        if (timelineStatuses == null) {
            timelineStatuses = new ArrayList<>();
        }
        if (statusReport != null) {
            timelineStatuses.add(statusReport);
        }
        timelineStatuses.addAll(statuses.statuses);
        if (max_id == null || (statuses.pagination.max_id != null && Helper.compareTo(statuses.pagination.max_id, max_id) < 0) || timelineType.getValue().startsWith("TREND_")) {
            max_id = statuses.pagination.max_id;
        }
        if (min_id == null || (statuses.pagination.min_id != null && Helper.compareTo(statuses.pagination.min_id, min_id) > 0)) {
            min_id = statuses.pagination.min_id;
        }
        if (search != null) {
            offset += MastodonHelper.SEARCH_PER_CALL;
        }
        statusAdapter = new StatusAdapter(timelineStatuses, timelineType, minified, canBeFederated, checkRemotely);
        statusAdapter.fetchMoreCallBack = this;
        if (statusReport != null) {
            scrollToTop();
        }
        RecyclerView.ItemAnimator animator = binding.recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        mLayoutManager = new LinearLayoutManager(requireActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusAdapter);

        preloadSizeProvider = new ViewPreloadSizeProvider<>();
        RecyclerViewPreloader<Attachment> preloader =
                new RecyclerViewPreloader<>(
                        GlideApp.with(this), statusAdapter, preloadSizeProvider, PRELOAD_AHEAD_ITEMS);
        binding.recyclerView.addOnScrollListener(preloader);
        binding.recyclerView.setItemViewCacheSize(0);

        if (timelineType != Timeline.TimeLineEnum.TREND_MESSAGE) {
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
                                extraCalls = -1;
                            }
                        } else {
                            binding.loadingNextElements.setVisibility(View.GONE);
                        }
                    } else if (firstVisibleItem == 0) { //Scroll top and item is zero
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            router(DIRECTION.TOP);
                            extraCalls = -1;
                        }
                    }
                }
            });
            //For first tab we fetch new messages, if we keep position
            if (slug != null /*&& slug.compareTo(Helper.getSlugOfFirstFragment(requireActivity(), currentUserID, currentInstance)) == 0*/ && rememberPosition) {
                route(DIRECTION.FETCH_NEW, true);
            }
        }

    }

    /**
     * Update the timeline with received statuses
     *
     * @param statusListReceived - List<Status> Statuses received
     */
    private int updateStatusListWith(List<Status> statusListReceived) {
        int insertedStatus = 0;
        if (statusListReceived != null && statusListReceived.size() > 0) {
            for (Status statusReceived : statusListReceived) {
                int position = 0;
                if (timelineStatuses != null) {
                    //First we refresh statuses
                    statusAdapter.notifyItemRangeChanged(0, timelineStatuses.size());
                    //We loop through messages already in the timeline
                    for (Status statusAlreadyPresent : timelineStatuses) {
                        //We compare the id of each status and we only add status having an id greater than the another, it is inserted at this position
                        //Pinned messages are ignored because their date can be older
                        if (Helper.compareTo(statusReceived.id, statusAlreadyPresent.id) > 0 && !statusAlreadyPresent.pinned) {
                            //We add the status to a list of id - thus we know it is already in the timeline
                            if (!timelineStatuses.contains(statusReceived)) {
                                timelineStatuses.add(position, statusReceived);
                                statusAdapter.notifyItemInserted(position);
                                if (!statusReceived.cached) {
                                    insertedStatus++;
                                }
                            }
                            break;
                        }
                        position++;
                    }
                    //Statuses added at the bottom
                    if (position == timelineStatuses.size() && !timelineStatuses.contains(statusReceived)) {
                        //We add the status to a list of id - thus we know it is already in the timeline
                        timelineStatuses.add(position, statusReceived);
                        statusAdapter.notifyItemInserted(position);
                    }
                }
            }
        }
        return insertedStatus;
    }

    /**
     * Update view and pagination when scrolling down
     *
     * @param fetched_statuses Statuses
     */
    private synchronized void dealWithPagination(Statuses fetched_statuses, DIRECTION direction, boolean fetchingMissing) {
        dealWithPagination(fetched_statuses, direction, fetchingMissing, null);
    }

    /**
     * Router for common timelines that can have the same treatments
     * - HOME / LOCAL / PUBLIC / LIST / TAG
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void routeCommon(DIRECTION direction, boolean fetchingMissing, Status status) {
        if (binding == null || getActivity() == null || !isAdded()) {
            return;
        }
        //Initialize with default params
        TimelinesVM.TimelineParams timelineParams = new TimelinesVM.TimelineParams(timelineType, direction, ident);
        timelineParams.limit = MastodonHelper.statusesPerCall(requireActivity());
        if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP || direction == DIRECTION.FETCH_NEW) {
            timelineParams.maxId = null;
            timelineParams.minId = null;
        } else if (direction == DIRECTION.BOTTOM) {
            timelineParams.maxId = fetchingMissing ? max_id_fetch_more : max_id;
            timelineParams.minId = null;
        } else if (direction == DIRECTION.TOP) {
            timelineParams.minId = fetchingMissing ? min_id_fetch_more : min_id;
            timelineParams.maxId = null;
        } else {
            timelineParams.maxId = max_id;
        }
        timelineParams.fetchingMissing = fetchingMissing;
        switch (timelineType) {
            case LOCAL:
                timelineParams.local = true;
                timelineParams.remote = false;
                break;
            case PUBLIC:
                timelineParams.local = false;
                timelineParams.remote = true;
                break;
            case BUBBLE:
                if (bubbleTimeline != null) {
                    timelineParams.onlyMedia = bubbleTimeline.only_media;
                    timelineParams.remote = bubbleTimeline.remote;
                    timelineParams.replyVisibility = bubbleTimeline.reply_visibility;
                    timelineParams.excludeVisibilities = bubbleTimeline.exclude_visibilities;
                }
                break;
            case LIST:
                timelineParams.listId = list_id;
                break;
            case ART:
            case TAG:
                if (tagTimeline == null) {
                    tagTimeline = new TagTimeline();
                    tagTimeline.name = search;
                }
                timelineParams.onlyMedia = timelineType == Timeline.TimeLineEnum.ART;
                timelineParams.none = tagTimeline.none;
                timelineParams.all = tagTimeline.all;
                timelineParams.any = tagTimeline.any;
                timelineParams.hashtagTrim = tagTimeline.name;
                if (timelineParams.hashtagTrim != null && timelineParams.hashtagTrim.startsWith("#")) {
                    timelineParams.hashtagTrim = tagTimeline.name.substring(1);
                }
                break;
            case REMOTE:
                timelineParams.instance = remoteInstance;
                timelineParams.token = null;
                break;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean useCache = sharedpreferences.getBoolean(getString(R.string.SET_USE_CACHE), true);
        if (useCache && direction != DIRECTION.SCROLL_TOP && direction != DIRECTION.FETCH_NEW) {
            getCachedStatus(direction, fetchingMissing, timelineParams);
        } else {
            getLiveStatus(direction, fetchingMissing, timelineParams, status);
        }

    }

    @Override
    public void onPause() {
        storeMarker();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        //Update last read id for home timeline
        if (isAdded()) {
            storeMarker();
        }
        try {
            LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(receive_action);
        } catch (Exception ignored) {
        }
        super.onDestroyView();
    }


    private void storeMarker() {
        if (mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            if (timelineStatuses != null && timelineStatuses.size() > position) {
                try {
                    Status status = timelineStatuses.get(position);
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, status.id);
                    editor.apply();
                    if (timelineType == Timeline.TimeLineEnum.HOME) {
                        timelinesVM.addMarker(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.id, null);
                    }
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

    private void getCachedStatus(DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams) {

        if (direction == null) {
            timelinesVM.getTimelineCache(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesCached -> {
                        if (statusesCached == null || statusesCached.statuses == null || statusesCached.statuses.size() == 0) {
                            getLiveStatus(null, fetchingMissing, timelineParams, null);
                        } else {
                            initialStatuses = statusesCached;
                            initializeStatusesCommonView(statusesCached);
                        }
                    });
        } else if (direction == DIRECTION.BOTTOM) {
            timelinesVM.getTimelineCache(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesCachedBottom -> {
                        if (statusesCachedBottom == null || statusesCachedBottom.statuses == null || statusesCachedBottom.statuses.size() == 0) {
                            getLiveStatus(DIRECTION.BOTTOM, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(statusesCachedBottom, DIRECTION.BOTTOM, fetchingMissing);
                        }
                    });
        } else if (direction == DIRECTION.TOP) {
            timelinesVM.getTimelineCache(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesCachedTop -> {
                        if (statusesCachedTop == null || statusesCachedTop.statuses == null || statusesCachedTop.statuses.size() == 0) {
                            getLiveStatus(DIRECTION.TOP, fetchingMissing, timelineParams, null);
                        } else {
                            dealWithPagination(statusesCachedTop, DIRECTION.TOP, fetchingMissing);
                        }

                    });
        } else if (direction == DIRECTION.REFRESH) {
            timelinesVM.getTimelineCache(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                        if (statusesRefresh == null || statusesRefresh.statuses == null || statusesRefresh.statuses.size() == 0) {
                            getLiveStatus(direction, fetchingMissing, timelineParams, null);
                        } else {
                            if (statusAdapter != null) {
                                dealWithPagination(statusesRefresh, direction, true);
                            } else {
                                initializeStatusesCommonView(statusesRefresh);
                            }
                        }
                    });
        }
    }

    private void getLiveStatus(DIRECTION direction, boolean fetchingMissing, TimelinesVM.TimelineParams timelineParams, Status status) {

        if (direction == null) {
            timelinesVM.getTimeline(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statuses -> {
                        initialStatuses = statuses;
                        initializeStatusesCommonView(statuses);
                    });
        } else if (direction == DIRECTION.BOTTOM) {
            timelinesVM.getTimeline(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, fetchingMissing, status));
        } else if (direction == DIRECTION.TOP) {
            timelinesVM.getTimeline(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesTop -> dealWithPagination(statusesTop, DIRECTION.TOP, fetchingMissing, status));
        } else if (direction == DIRECTION.REFRESH || direction == DIRECTION.SCROLL_TOP || direction == DIRECTION.FETCH_NEW) {
            timelinesVM.getTimeline(timelineStatuses, timelineParams)
                    .observe(getViewLifecycleOwner(), statusesRefresh -> {
                        if (statusAdapter != null) {
                            dealWithPagination(statusesRefresh, direction, true, status);
                        } else {
                            initializeStatusesCommonView(statusesRefresh);
                        }
                    });
        }
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(DIRECTION direction, boolean fetchingMissing) {
        route(direction, fetchingMissing, null);
    }

    /**
     * Router for timelines
     *
     * @param direction - DIRECTION null if first call, then is set to TOP or BOTTOM depending of scroll
     */
    private void route(DIRECTION direction, boolean fetchingMissing, Status statusToUpdate) {
        if (binding == null || getActivity() == null || !isAdded()) {
            return;
        }
        // --- HOME TIMELINE ---
        if (timelineType == Timeline.TimeLineEnum.HOME) {
            //for more visibility it's done through loadHomeStrategy method
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.LOCAL) { //LOCAL TIMELINE
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.PUBLIC) { //PUBLIC TIMELINE
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.BUBBLE) { //BUBBLE TIMELINE
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.REMOTE) { //REMOTE TIMELINE
            //NITTER TIMELINES
            if (pinnedTimeline != null && pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER) {
                if (direction == null) {
                    timelinesVM.getNitter(pinnedTimeline.remoteInstance.host, null)
                            .observe(getViewLifecycleOwner(), nitterStatuses -> {
                                initialStatuses = nitterStatuses;
                                initializeStatusesCommonView(nitterStatuses);
                            });
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
                    timelinesVM.getPeertube(remoteInstance, String.valueOf(timelineStatuses.size()), MastodonHelper.statusesPerCall(requireActivity()))
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
                routeCommon(direction, fetchingMissing, statusToUpdate);
            }
        } else if (timelineType == Timeline.TimeLineEnum.LIST) { //LIST TIMELINE
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.TAG || timelineType == Timeline.TimeLineEnum.ART) { //TAG TIMELINE
            routeCommon(direction, fetchingMissing, statusToUpdate);
        } else if (timelineType == Timeline.TimeLineEnum.ACCOUNT_TIMELINE) { //PROFILE TIMELINES
            String tempToken;
            String tempInstance;
            String accountId;
            if (checkRemotely) {
                tempToken = null;
                tempInstance = remoteInstance;
                accountId = accountIDInRemoteInstance;
                if (accountIDInRemoteInstance == null) {
                    CrossActionHelper.fetchAccountInRemoteInstance(requireActivity(), accountTimeline.acct, tempInstance, new CrossActionHelper.Callback() {
                        @Override
                        public void federatedStatus(Status status) {
                        }

                        @Override
                        public void federatedAccount(Account account) {
                            if (account != null && !requireActivity().isFinishing() && isAdded()) {
                                accountIDInRemoteInstance = account.id;
                                accountsVM.getAccountStatuses(tempInstance, null, accountIDInRemoteInstance, null, null, null, null, null, false, true, MastodonHelper.statusesPerCall(requireActivity()))
                                        .observe(getViewLifecycleOwner(), pinnedStatuses -> accountsVM.getAccountStatuses(tempInstance, null, accountIDInRemoteInstance, null, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                                                .observe(getViewLifecycleOwner(), otherStatuses -> {
                                                    if (otherStatuses != null && otherStatuses.statuses != null) {
                                                        if (pinnedStatuses != null && pinnedStatuses.statuses != null) {
                                                            for (Status status : pinnedStatuses.statuses) {
                                                                status.pinned = true;
                                                            }
                                                            otherStatuses.statuses.addAll(0, pinnedStatuses.statuses);
                                                        }
                                                    }
                                                    initializeStatusesCommonView(otherStatuses);
                                                }));
                            } else {
                                Toasty.error(requireActivity(), getString(R.string.toast_fetch_error), Toasty.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    accountId = accountIDInRemoteInstance;
                }
            } else {
                tempToken = MainActivity.currentToken;
                tempInstance = currentInstance;
                accountId = accountTimeline.id;
            }
            if (accountId == null) {
                accountId = accountTimeline.id;
            }
            if (direction == null && !checkRemotely) {
                if (show_pinned) {
                    //Fetch pinned statuses to display them at the top
                    accountsVM.getAccountStatuses(currentInstance, MainActivity.currentToken, accountId, null, null, null, null, null, false, true, MastodonHelper.statusesPerCall(requireActivity()))
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
                    accountsVM.getAccountStatuses(tempInstance, tempToken, accountId, null, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                            .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
                }
            } else if (direction == DIRECTION.BOTTOM) {
                accountsVM.getAccountStatuses(tempInstance, tempToken, accountId, max_id, null, null, exclude_replies, exclude_reblogs, media_only, false, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
            } else {
                flagLoading = false;
            }
        } else if (search != null) {
            SearchVM searchVM = new ViewModelProvider(FragmentMastodonTimeline.this).get(viewModelKey, SearchVM.class);
            if (direction == null) {
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, null, false, true, false, 0, null, null, MastodonHelper.SEARCH_PER_CALL)
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
            } else if (direction == DIRECTION.BOTTOM) {
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, search.trim(), null, null, false, true, false, offset, null, null, MastodonHelper.SEARCH_PER_CALL)
                        .observe(getViewLifecycleOwner(), results -> {
                            if (results != null) {
                                Statuses statuses = new Statuses();
                                statuses.statuses = results.statuses;
                                statuses.pagination = new Pagination();
                                dealWithPagination(statuses, direction, false);
                            }
                        });
            } else {
                flagLoading = false;
            }
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
                timelinesVM.getStatusTrends(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, null, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
            } else if (direction == DIRECTION.BOTTOM) {
                timelinesVM.getStatusTrends(BaseMainActivity.currentToken, BaseMainActivity.currentInstance, max_id, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
            } else {
                flagLoading = false;
            }
        } else if (timelineType == Timeline.TimeLineEnum.TREND_MESSAGE_PUBLIC) {
            if (direction == null) {
                timelinesVM.getStatusTrends(null, publicTrendsDomain, null, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(getViewLifecycleOwner(), this::initializeStatusesCommonView);
            } else if (direction == DIRECTION.BOTTOM) {
                timelinesVM.getStatusTrends(null, publicTrendsDomain, max_id, MastodonHelper.statusesPerCall(requireActivity()))
                        .observe(getViewLifecycleOwner(), statusesBottom -> dealWithPagination(statusesBottom, DIRECTION.BOTTOM, false));
            } else {
                flagLoading = false;
            }
        }

    }

    /**
     * Refresh status in list
     */
    public void refreshAllAdapters() {
        if (statusAdapter != null && timelineStatuses != null) {
            statusAdapter.notifyItemRangeChanged(0, timelineStatuses.size());
        }
    }

    @Override
    public void onClickMinId(String min_id, Status statusToUpdate) {
        //Fetch more has been pressed
        min_id_fetch_more = min_id;
        route(DIRECTION.TOP, true, statusToUpdate);
    }

    @Override
    public void onClickMaxId(String max_id, Status statusToUpdate) {
        max_id_fetch_more = max_id;
        route(DIRECTION.BOTTOM, true, statusToUpdate);
    }

    public enum DIRECTION {
        TOP,
        BOTTOM,
        REFRESH,
        SCROLL_TOP,
        FETCH_NEW
    }


    public interface UpdateCounters {
        void onUpdate(int count, Timeline.TimeLineEnum type, String slug);
    }
}