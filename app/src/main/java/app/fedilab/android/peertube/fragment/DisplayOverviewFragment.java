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

import static app.fedilab.android.peertube.client.data.VideoData.Video.titleType.CATEGORY;
import static app.fedilab.android.peertube.client.data.VideoData.Video.titleType.CHANNEL;
import static app.fedilab.android.peertube.client.data.VideoData.Video.titleType.TAG;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.OverviewVideo;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.drawer.PeertubeAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import app.fedilab.android.peertube.viewmodel.RelationshipVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class DisplayOverviewFragment extends Fragment implements PeertubeAdapter.RelationShipListener, PeertubeAdapter.PlaylistListener {


    private LinearLayoutManager mLayoutManager;
    private GridLayoutManager gLayoutManager;
    private boolean flag_loading;
    private Context context;
    private PeertubeAdapter peertubeAdapater;
    private int page;
    private List<VideoData.Video> peertubes;
    private RelativeLayout mainLoader, nextElementLoader, textviewNoAction;
    private boolean firstLoad;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textviewNoActionText;
    private View rootView;
    private RecyclerView lv_status;
    private TimelineVM viewModelFeeds;
    private Map<String, Boolean> relationship;
    private Map<String, List<PlaylistExist>> playlists;


    public DisplayOverviewFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_overview_peertube, container, false);


        peertubes = new ArrayList<>();
        context = getContext();
        lv_status = rootView.findViewById(R.id.lv_status);
        page = 1;
        flag_loading = true;
        firstLoad = true;

        swipeRefreshLayout = rootView.findViewById(R.id.swipeContainer);
        mainLoader = rootView.findViewById(R.id.loader);
        nextElementLoader = rootView.findViewById(R.id.loading_next_status);
        textviewNoAction = rootView.findViewById(R.id.no_action);
        textviewNoActionText = rootView.findViewById(R.id.no_action_text);
        mainLoader.setVisibility(View.VISIBLE);
        nextElementLoader.setVisibility(View.GONE);

        peertubeAdapater = new PeertubeAdapter(this.peertubes);

        peertubeAdapater.playlistListener = this;
        peertubeAdapater.relationShipListener = this;

        lv_status.setAdapter(peertubeAdapater);


        if (!Helper.isTablet(context)) {
            mLayoutManager = new LinearLayoutManager(context);
            lv_status.setLayoutManager(mLayoutManager);
        } else {
            gLayoutManager = new GridLayoutManager(context, 2);
            int spanCount = (int) Helper.convertDpToPixel(2, context);
            int spacing = (int) Helper.convertDpToPixel(5, context);
            lv_status.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));
            lv_status.setLayoutManager(gLayoutManager);
        }

        viewModelFeeds = new ViewModelProvider(DisplayOverviewFragment.this).get(TimelineVM.class);
        swipeRefreshLayout.setOnRefreshListener(this::pullToRefresh);
        loadTimeline(page);
        lv_status.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mLayoutManager != null) {
                    int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                    if (dy > 0) {
                        int visibleItemCount = mLayoutManager.getChildCount();
                        int totalItemCount = mLayoutManager.getItemCount();
                        if (firstVisibleItem + visibleItemCount == totalItemCount && context != null) {
                            if (!flag_loading) {
                                flag_loading = true;
                                loadTimeline(page);
                                nextElementLoader.setVisibility(View.VISIBLE);
                            }
                        } else {
                            nextElementLoader.setVisibility(View.GONE);
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
                                loadTimeline(page);
                                nextElementLoader.setVisibility(View.VISIBLE);
                            }
                        } else {
                            nextElementLoader.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });


        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.clearAnimation();
        }
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


    private void manageVIewVideos(APIResponse apiResponse) {
        //hide loaders
        mainLoader.setVisibility(View.GONE);
        nextElementLoader.setVisibility(View.GONE);
        //handle other API error
        if (this.peertubes == null || apiResponse == null || apiResponse.getOverviewVideo() == null || (apiResponse.getError() != null)) {
            if (apiResponse == null || apiResponse.getError() == null)
                Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            else {
                Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            }
            swipeRefreshLayout.setRefreshing(false);
            flag_loading = false;
            return;
        }
        OverviewVideo overviewVideos = apiResponse.getOverviewVideo();
        int totalAdded = 0;
        int previousPosition = this.peertubes.size();

        apiResponse.setPeertubes(new ArrayList<>());
        if (overviewVideos.getCategories().size() > 0 && overviewVideos.getCategories() != null) {
            String categoryTitle = overviewVideos.getCategories().get(0).getCategory().getLabel();
            List<VideoData.Video> videoCategories = overviewVideos.getCategories().get(0).getVideos();
            int i = 0;
            for (VideoData.Video video : videoCategories) {
                if (i == 0) {
                    video.setTitle(categoryTitle);
                    video.setHasTitle(true);
                    video.setTitleType(CATEGORY);
                }
                i++;
                peertubes.add(video);
                apiResponse.getPeertubes().add(video);
                totalAdded++;
            }
        }

        if (overviewVideos.getTags().size() > 0 && overviewVideos.getTags().get(0) != null) {
            String tagTitle = overviewVideos.getTags().get(0).getTag();
            List<VideoData.Video> videoTags = overviewVideos.getTags().get(0).getVideos();
            int i = 0;
            for (VideoData.Video video : videoTags) {
                if (i == 0) {
                    video.setTitle(tagTitle);
                    video.setHasTitle(true);
                    video.setTitleType(TAG);
                }
                i++;
                peertubes.add(video);
                apiResponse.getPeertubes().add(video);
                totalAdded++;
            }
        }
        if (overviewVideos.getChannels().size() > 0 && overviewVideos.getChannels().get(0).getChannels() != null) {
            String channelTitle = overviewVideos.getChannels().get(0).getChannels().getAcct();
            List<VideoData.Video> videoChannels = overviewVideos.getChannels().get(0).getVideos();
            int i = 0;
            for (VideoData.Video video : videoChannels) {
                if (i == 0) {
                    video.setTitle(channelTitle);
                    video.setHasTitle(true);
                    video.setTitleType(CHANNEL);
                }
                i++;
                peertubes.add(video);
                apiResponse.getPeertubes().add(video);
                totalAdded++;
            }
        }

        if (Helper.isLoggedIn(context)) {
            List<String> uids = new ArrayList<>();
            for (VideoData.Video video : apiResponse.getPeertubes()) {
                uids.add(video.getChannel().getName() + "@" + video.getChannel().getHost());
            }
            if (uids.size() > 0 && !DisplayOverviewFragment.this.isDetached()) {
                try {
                    RelationshipVM viewModel = new ViewModelProvider(this).get(RelationshipVM.class);
                    viewModel.get(uids).observe(DisplayOverviewFragment.this.requireActivity(), this::manageVIewRelationship);
                } catch (Exception ignored) {
                }
            }

            List<String> videoIds = new ArrayList<>();
            for (VideoData.Video video : apiResponse.getPeertubes()) {
                videoIds.add(video.getId());
            }
            if (videoIds.size() > 0 && !DisplayOverviewFragment.this.isDetached()) {
                try {
                    PlaylistsVM viewModel = new ViewModelProvider(this).get(PlaylistsVM.class);
                    viewModel.videoExists(videoIds).observe(DisplayOverviewFragment.this.requireActivity(), this::manageVIewPlaylist);
                } catch (Exception ignored) {
                }
            }

        }

        //max_id needs to work like an offset
        page++;
        //If no item were inserted previously the adapter is created
        if (previousPosition == 0) {
            peertubeAdapater = new PeertubeAdapter(this.peertubes);
            peertubeAdapater.playlistListener = DisplayOverviewFragment.this;
            peertubeAdapater.relationShipListener = DisplayOverviewFragment.this;
            lv_status.setAdapter(peertubeAdapater);
        } else
            peertubeAdapater.notifyItemRangeInserted(previousPosition, totalAdded);
        //remove handlers
        swipeRefreshLayout.setRefreshing(false);
        textviewNoAction.setVisibility(View.GONE);
        if (firstLoad && (this.peertubes == null || this.peertubes.size() == 0)) {
            textviewNoActionText.setText(R.string.no_video_to_display);
            textviewNoAction.setVisibility(View.VISIBLE);
        }
        flag_loading = false;
        firstLoad = false;
    }


    @Override
    public void onDestroyView() {
        if (lv_status != null) {
            try {
                lv_status.setAdapter(null);
            } catch (Exception ignored) {
            }
        }
        super.onDestroyView();
        rootView = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        swipeRefreshLayout.setEnabled(true);
    }


    public void scrollToTop() {
        if (mLayoutManager != null) {
            mLayoutManager.scrollToPositionWithOffset(0, 0);
        } else if (gLayoutManager != null) {
            gLayoutManager.scrollToPositionWithOffset(0, 0);
        }
    }


    public void pullToRefresh() {
        int size = peertubes.size();
        peertubes.clear();
        peertubes = new ArrayList<>();
        page = 1;
        peertubeAdapater.notifyItemRangeRemoved(0, size);
        loadTimeline(page);
    }

    /**
     * Manage timeline load
     *
     * @param page String pagination
     */
    private void loadTimeline(int page) {
        viewModelFeeds.getOverviewVideos(String.valueOf(page)).observe(DisplayOverviewFragment.this.requireActivity(), this::manageVIewVideos);
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
            video.setPlaylistExists(playlists.get(video.getId()));
        }

    }

    public void manageVIewRelationship(APIResponse apiResponse) {
        if (apiResponse.getError() != null) {
            return;
        }
        if (relationship == null) {
            relationship = new HashMap<>();
        }
        relationship.putAll(apiResponse.getRelationships());
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
