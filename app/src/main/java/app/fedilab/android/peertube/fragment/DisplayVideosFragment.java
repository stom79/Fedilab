package app.fedilab.android.peertube.fragment;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static app.fedilab.android.peertube.viewmodel.TimelineVM.TimelineType.VIDEOS_IN_LOCAL_PLAYLIST;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.BuildConfig;
import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.MainActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.databinding.FragmentVideoBinding;
import app.fedilab.android.peertube.drawer.AccountsHorizontalListAdapter;
import app.fedilab.android.peertube.drawer.PeertubeAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.AccountsVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import app.fedilab.android.peertube.viewmodel.RelationshipVM;
import app.fedilab.android.peertube.viewmodel.SearchVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class DisplayVideosFragment extends Fragment implements AccountsHorizontalListAdapter.EventListener, PeertubeAdapter.RelationShipListener, PeertubeAdapter.PlaylistListener {


    private LinearLayoutManager mLayoutManager;
    private GridLayoutManager gLayoutManager;
    private boolean flag_loading, flag_loading_account;
    private Context context;
    private PeertubeAdapter peertubeAdapater;
    private AccountsHorizontalListAdapter accountsHorizontalListAdapter;
    private String max_id, max_id_accounts;
    private List<VideoData.Video> peertubes;
    private List<ChannelData.Channel> channels;
    private TimelineVM.TimelineType type;
    private boolean firstLoad;
    private String search_peertube;
    private boolean check_ScrollingUp;
    private ChannelData.Channel forChannel;
    private TimelineVM viewModelFeeds;
    private SearchVM viewModelSearch;
    private AccountsVM viewModelAccounts;
    private ChannelData.Channel channel;
    private AccountData.Account account;
    private Map<String, Boolean> relationship;
    private Map<String, List<PlaylistExist>> playlists;
    private String playlistId;
    private String remoteInstance;
    private boolean sepiaSearch;
    private String startDate, endDate;
    private FragmentVideoBinding binding;
    private String channelId;

    public DisplayVideosFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        peertubes = new ArrayList<>();
        channels = new ArrayList<>();
        context = getContext();
        startDate = null;
        endDate = null;
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            search_peertube = bundle.getString("search_peertube", null);
            channel = bundle.getParcelable("channel");
            account = bundle.getParcelable("account");
            remoteInstance = bundle.getString("peertube_instance", null);
            sepiaSearch = bundle.getBoolean("sepia_search", false);
            type = (TimelineVM.TimelineType) bundle.get(Helper.TIMELINE_TYPE);
            playlistId = bundle.getString("playlistId", null);
            startDate = bundle.getString("startDate", null);
            endDate = bundle.getString("endDate", null);
        }

        if (channel != null) {
            channelId = channel.getAcct();
        } else if (account != null) {
            channelId = account.getAcct();
        }
        max_id = "0";
        //forChannel = type == TimelineVM.TimelineType.ACCOUNT_VIDEOS ? channelId : null;
        max_id_accounts = null;
        flag_loading = true;
        flag_loading_account = false;
        firstLoad = true;
        check_ScrollingUp = false;

        binding.loader.setVisibility(View.VISIBLE);
        binding.loadingNextVideos.setVisibility(View.GONE);

        peertubeAdapater = new PeertubeAdapter(this.peertubes, type, sepiaSearch, forChannel, account);
        peertubeAdapater.playlistListener = this;
        peertubeAdapater.relationShipListener = this;
        binding.lvVideos.setAdapter(peertubeAdapater);

        accountsHorizontalListAdapter = new AccountsHorizontalListAdapter(this.channels, this);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        binding.lvAccounts.setLayoutManager(layoutManager);
        binding.lvAccounts.setAdapter(accountsHorizontalListAdapter);

        if (!Helper.isTablet(context)) {
            mLayoutManager = new LinearLayoutManager(context);
            binding.lvVideos.setLayoutManager(mLayoutManager);
        } else {
            gLayoutManager = new GridLayoutManager(context, 2);
            int spanCount = (int) Helper.convertDpToPixel(2, context);
            int spacing = (int) Helper.convertDpToPixel(5, context);
            binding.lvVideos.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));
            binding.lvVideos.setLayoutManager(gLayoutManager);
        }
        viewModelAccounts = new ViewModelProvider(DisplayVideosFragment.this).get(AccountsVM.class);
        viewModelFeeds = new ViewModelProvider(DisplayVideosFragment.this).get(TimelineVM.class);
        viewModelSearch = new ViewModelProvider(DisplayVideosFragment.this).get(SearchVM.class);
        binding.swipeContainer.setOnRefreshListener(() -> pullToRefresh(true));

        binding.lvAccounts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if (dx > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount && context != null && !flag_loading_account) {
                        flag_loading_account = true;
                        viewModelAccounts.getAccounts(RetrofitPeertubeAPI.DataType.SUBSCRIBER, max_id_accounts).observe(DisplayVideosFragment.this.requireActivity(), apiResponse -> manageViewAccounts(apiResponse));
                    }
                }
            }
        });


        if (type != VIDEOS_IN_LOCAL_PLAYLIST) {
            binding.lvVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (type == TimelineVM.TimelineType.SUBSCRIBTIONS) {
                        if (dy > 0) {
                            if (check_ScrollingUp) {
                                binding.topAccountContainer.setVisibility(View.GONE);
                                final Handler handler = new Handler();
                                handler.postDelayed(() -> check_ScrollingUp = false, 300);

                            }
                        } else {
                            if (!check_ScrollingUp) {
                                binding.topAccountContainer.setVisibility(View.VISIBLE);
                                final Handler handler = new Handler();
                                handler.postDelayed(() -> check_ScrollingUp = true, 300);
                            }
                        }
                    }
                    if (mLayoutManager != null) {
                        int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                        if (dy > 0) {
                            int visibleItemCount = mLayoutManager.getChildCount();
                            int totalItemCount = mLayoutManager.getItemCount();
                            if (firstVisibleItem + visibleItemCount == totalItemCount && context != null) {
                                if (!flag_loading) {
                                    flag_loading = true;
                                    loadTimeline(max_id);
                                    binding.loadingNextVideos.setVisibility(View.VISIBLE);
                                }
                            } else {
                                binding.loadingNextVideos.setVisibility(View.GONE);
                            }
                        }
                    } else if (gLayoutManager != null) {
                        int firstVisibleItem = gLayoutManager.findFirstVisibleItemPosition();
                        if (dy > 0) {
                            int visibleItemCount = gLayoutManager.getChildCount();
                            int totalItemCount = gLayoutManager.getItemCount();
                            if (firstVisibleItem + visibleItemCount == totalItemCount && context != null) {
                                if (!flag_loading) {
                                    flag_loading = true;
                                    loadTimeline(max_id);
                                    binding.loadingNextVideos.setVisibility(View.VISIBLE);
                                }
                            } else {
                                binding.loadingNextVideos.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            });
        }
        if (type == TimelineVM.TimelineType.SUBSCRIBTIONS) {
            AccountsVM viewModel = new ViewModelProvider(this).get(AccountsVM.class);
            viewModel.getAccounts(RetrofitPeertubeAPI.DataType.SUBSCRIBER, max_id).observe(DisplayVideosFragment.this.requireActivity(), this::manageViewAccounts);
        }
        loadTimeline(max_id);
        binding.displayAll.setOnClickListener(v -> {
            forChannel = null;
            pullToRefresh(false);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.swipeContainer.setEnabled(true);
    }


    @Override
    public void onPause() {
        super.onPause();
        binding.swipeContainer.setEnabled(false);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.clearAnimation();
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }


    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void manageViewAccounts(APIResponse apiResponse) {
        flag_loading_account = false;
        if (apiResponse != null && apiResponse.getChannels() != null && apiResponse.getChannels().size() > 0) {
            if (binding.topAccountContainer.getVisibility() == View.GONE) {
                binding.topAccountContainer.setVisibility(View.VISIBLE);
            }
            int previousPosition = channels.size();
            channels.addAll(apiResponse.getChannels());
            accountsHorizontalListAdapter.notifyItemRangeInserted(previousPosition, apiResponse.getChannels().size());
            if (max_id_accounts == null) {
                max_id_accounts = "0";
            }
            final SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
            //max_id_accounts needs to work like an offset
            int tootPerPage = sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE);
            max_id_accounts = String.valueOf(Integer.parseInt(max_id_accounts) + tootPerPage);
        }
    }


    private void manageVIewVideos(APIResponse apiResponse) {
        //hide loaders
        binding.loader.setVisibility(View.GONE);
        binding.loadingNextVideos.setVisibility(View.GONE);
        //handle other API error
        if (this.peertubes == null || apiResponse == null || (apiResponse.getError() != null)) {
            if (apiResponse == null)
                Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            else if (apiResponse.getError() != null) {
                if (apiResponse.getError().getError().length() > 500) {
                    Toasty.info(context, getString(R.string.remote_account), Toast.LENGTH_LONG).show();
                } else {
                    Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
                }

            }
            binding.swipeContainer.setRefreshing(false);
            flag_loading = false;
            return;
        }
        int previousPosition = this.peertubes.size();
        if (max_id == null)
            max_id = "0";
        //max_id needs to work like an offset
        final SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        int videoPerPage = sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE);
        max_id = String.valueOf(Integer.parseInt(max_id) + videoPerPage);
        if (apiResponse.getPeertubes() == null && apiResponse.getVideoPlaylist() == null) {
            return;
        }
        if (apiResponse.getVideoPlaylist() != null) {
            apiResponse.setPeertubes(new ArrayList<>());
            for (VideoPlaylistData.VideoPlaylist v : apiResponse.getVideoPlaylist()) {
                apiResponse.getPeertubes().add(v.getVideo());
            }
        }
        if (!BuildConfig.google_restriction) {
            this.peertubes.addAll(apiResponse.getPeertubes());
        } else {
            for (VideoData.Video video : apiResponse.getPeertubes()) {
                if (video.getName() == null || !video.getName().toLowerCase().contains("youtube") || !video.getName().toLowerCase().contains("download")) {
                    this.peertubes.add(video);
                }
            }
        }


        //If no item were inserted previously the adapter is created
        if (previousPosition == 0) {
            peertubeAdapater = new PeertubeAdapter(this.peertubes, type, sepiaSearch, forChannel, account);
            peertubeAdapater.playlistListener = DisplayVideosFragment.this;
            peertubeAdapater.relationShipListener = DisplayVideosFragment.this;
            binding.lvVideos.setAdapter(peertubeAdapater);
        } else
            peertubeAdapater.notifyItemRangeInserted(previousPosition, apiResponse.getPeertubes().size());
        //remove handlers
        binding.swipeContainer.setRefreshing(false);
        binding.noAction.setVisibility(View.GONE);
        if (firstLoad && (apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0)) {
            binding.noActionText.setText(R.string.no_video_to_display);
            binding.noAction.setVisibility(View.VISIBLE);
        }
        flag_loading = false;
        firstLoad = false;

        if (Helper.isLoggedIn(context)) {
            List<String> uids = new ArrayList<>();
            for (VideoData.Video video : apiResponse.getPeertubes()) {
                if (video != null) {
                    uids.add(video.getChannel().getName() + "@" + video.getChannel().getHost());
                }
            }
            if (uids.size() > 0 && !DisplayVideosFragment.this.isDetached()) {
                try {
                    RelationshipVM viewModel = new ViewModelProvider(this).get(RelationshipVM.class);
                    viewModel.get(uids).observe(DisplayVideosFragment.this.requireActivity(), this::manageVIewRelationship);
                } catch (Exception ignored) {
                }
            }

            List<String> videoIds = new ArrayList<>();
            for (VideoData.Video video : apiResponse.getPeertubes()) {
                if (video != null) {
                    videoIds.add(video.getId());
                }
            }
            if (videoIds.size() > 0 && !DisplayVideosFragment.this.isDetached()) {
                try {
                    PlaylistsVM viewModel = new ViewModelProvider(this).get(PlaylistsVM.class);
                    viewModel.videoExists(videoIds).observe(DisplayVideosFragment.this.requireActivity(), this::manageVIewPlaylist);
                } catch (Exception ignored) {
                }
            }

        }
    }

    public void manageVIewPlaylist(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getVideoExistPlaylist() == null) {
            return;
        }
        if (playlists == null) {
            playlists = new HashMap<>();
        }
        playlists.putAll(apiResponse.getVideoExistPlaylist());
        for (VideoData.Video video : peertubes) {
            if (video != null) {
                video.setPlaylistExists(playlists.get(video.getId()));
            }
        }

    }

    public void manageVIewRelationship(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getRelationships() == null) {
            return;
        }
        if (relationship == null) {
            relationship = new HashMap<>();
        }
        relationship.putAll(apiResponse.getRelationships());
    }

    @Override
    public void onDestroyView() {
        try {
            binding.lvVideos.setAdapter(null);
        } catch (Exception ignored) {
        }
        super.onDestroyView();
    }


    public void scrollToTop() {
        if (mLayoutManager != null) {
            mLayoutManager.scrollToPositionWithOffset(0, 0);
        } else if (gLayoutManager != null) {
            gLayoutManager.scrollToPositionWithOffset(0, 0);
        }
    }


    public void pullToRefresh(boolean reload) {
        if (type == TimelineVM.TimelineType.SUBSCRIBTIONS && reload) {
            DisplayVideosFragment subscriptionFragment = ((MainActivity) context).getSubscriptionFragment();
            if (subscriptionFragment != null) {
                FragmentTransaction ft = ((MainActivity) context).getSupportFragmentManager().beginTransaction();
                ft.detach(subscriptionFragment).attach(subscriptionFragment).commit();
            }
        } else {
            int size = peertubes.size();
            peertubes.clear();
            peertubes = new ArrayList<>();
            max_id = "0";
            peertubeAdapater.notifyItemRangeRemoved(0, size);
            if (forChannel == null) {
                for (ChannelData.Channel channel : channels) {
                    channel.setSelected(false);
                }
                accountsHorizontalListAdapter.notifyItemRangeRemoved(0, channels.size());
            }
            loadTimeline("0");
        }

    }

    @Override
    public void click(ChannelData.Channel forChannel) {
        this.forChannel = forChannel;
        pullToRefresh(false);
    }

    /**
     * Manage timeline load
     *
     * @param max_id String pagination
     */
    private void loadTimeline(String max_id) {
        if (search_peertube == null) { //Not a Peertube search
            if (type == TimelineVM.TimelineType.CHANNEL_VIDEOS) {
                viewModelFeeds.getVideosInChannel(sepiaSearch ? remoteInstance : null, channelId, max_id).observe(this.requireActivity(), this::manageVIewVideos);
            } else if (type == TimelineVM.TimelineType.VIDEOS_IN_PLAYLIST) {
                viewModelFeeds.loadVideosInPlaylist(playlistId, max_id).observe(this.requireActivity(), this::manageVIewVideos);
            } else if (type == VIDEOS_IN_LOCAL_PLAYLIST) {
                viewModelFeeds.loadVideosInLocalPlaylist(playlistId).observe(this.requireActivity(), this::manageVIewVideos);
            } else if (type == TimelineVM.TimelineType.HISTORY) {
                viewModelFeeds.getVideoHistory(max_id, startDate, endDate).observe(this.requireActivity(), this::manageVIewVideos);
            } else {
                viewModelFeeds.getVideos(type, max_id, forChannel, account).observe(this.requireActivity(), this::manageVIewVideos);
            }
        } else {
            viewModelSearch.getVideos(max_id, search_peertube).observe(this.requireActivity(), this::manageVIewVideos);
        }
    }


    @Override
    public Map<String, Boolean> getRelationShip() {
        return relationship;
    }

    @Override
    public Map<String, List<PlaylistExist>> getPlaylist() {
        return playlists;
    }


    static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NotNull Rect outRect, @NotNull View view, RecyclerView parent, @NotNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
