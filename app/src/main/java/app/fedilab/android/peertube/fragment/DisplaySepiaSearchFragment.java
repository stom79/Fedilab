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

import static app.fedilab.android.peertube.viewmodel.TimelineVM.TimelineType.SEPIA_SEARCH;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentVideoPeertubeBinding;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.SepiaSearch;
import app.fedilab.android.peertube.drawer.AccountsHorizontalListAdapter;
import app.fedilab.android.peertube.drawer.PeertubeAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.SepiaSearchVM;
import es.dmoral.toasty.Toasty;


public class DisplaySepiaSearchFragment extends Fragment implements AccountsHorizontalListAdapter.EventListener {


    private LinearLayoutManager mLayoutManager;
    private GridLayoutManager gLayoutManager;
    private boolean flag_loading;
    private Context context;
    private PeertubeAdapter peertubeAdapater;
    private List<VideoData.Video> peertubes;
    private boolean firstLoad;
    private SharedPreferences sharedpreferences;
    private SepiaSearchVM viewModelSearch;
    private SepiaSearch sepiaSearchVideo;
    private FragmentVideoPeertubeBinding binding;

    public DisplaySepiaSearchFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoPeertubeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        peertubes = new ArrayList<>();
        context = getContext();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            sepiaSearchVideo = bundle.getParcelable("sepiaSearchVideo");
        }
        flag_loading = true;
        firstLoad = true;

        assert context != null;
        sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);


        binding.loader.setVisibility(View.VISIBLE);
        binding.loadingNextVideos.setVisibility(View.GONE);

        peertubeAdapater = new PeertubeAdapter(this.peertubes, SEPIA_SEARCH, true, null, null);
        binding.lvVideos.setAdapter(peertubeAdapater);


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
        viewModelSearch = new ViewModelProvider(DisplaySepiaSearchFragment.this).get(SepiaSearchVM.class);
        binding.swipeContainer.setOnRefreshListener(this::pullToRefresh);


        binding.lvVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mLayoutManager != null) {
                    int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                    if (dy > 0) {
                        int visibleItemCount = mLayoutManager.getChildCount();
                        int totalItemCount = mLayoutManager.getItemCount();
                        if (firstVisibleItem + visibleItemCount == totalItemCount && context != null) {
                            if (!flag_loading) {
                                flag_loading = true;
                                loadTimeline();
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
                                loadTimeline();
                                binding.loadingNextVideos.setVisibility(View.VISIBLE);
                            }
                        } else {
                            binding.loadingNextVideos.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
        loadTimeline();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (binding.swipeContainer != null) {
            binding.swipeContainer.setEnabled(false);
            binding.swipeContainer.setRefreshing(false);
            binding.swipeContainer.clearAnimation();
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


    private void manageVIewVideos(VideoData videoData) {
        //hide loaders
        binding.loader.setVisibility(View.GONE);
        binding.loadingNextVideos.setVisibility(View.GONE);
        //handle other API error
        if (videoData == null || videoData.data == null) {
            Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            binding.swipeContainer.setRefreshing(false);
            flag_loading = false;
            return;
        }
        int previousPosition = this.peertubes.size();
        int videoPerPage = sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE);
        sepiaSearchVideo.setStart(String.valueOf(Integer.parseInt(sepiaSearchVideo.getStart()) + videoPerPage));

        if (BuildConfig.FLAVOR.equalsIgnoreCase("fdroid")) {
            this.peertubes.addAll(videoData.data);
        } else {
            for (VideoData.Video video : videoData.data) {
                if (video.getName() == null || !video.getName().toLowerCase().contains("youtube") || !video.getName().toLowerCase().contains("download")) {
                    this.peertubes.add(video);
                }
            }
        }

        //If no item were inserted previously the adapter is created
        if (previousPosition == 0) {
            peertubeAdapater = new PeertubeAdapter(this.peertubes, SEPIA_SEARCH, true, null, null);
            binding.lvVideos.setAdapter(peertubeAdapater);
        } else
            peertubeAdapater.notifyItemRangeInserted(previousPosition, videoData.data.size());

        //remove handlers
        binding.swipeContainer.setRefreshing(false);
        binding.noAction.setVisibility(View.GONE);
        if (firstLoad && (videoData.data == null || videoData.data.size() == 0)) {
            binding.noActionText.setText(R.string.no_video_to_display);
            binding.noAction.setVisibility(View.VISIBLE);
        }
        flag_loading = false;
        firstLoad = false;
    }

    @Override
    public void onDestroyView() {
        if (binding.lvVideos != null) {
            try {
                binding.lvVideos.setAdapter(null);
            } catch (Exception ignored) {
            }
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.swipeContainer.setEnabled(true);
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
        peertubeAdapater.notifyItemRangeRemoved(0, size);
        loadTimeline();
    }

    @Override
    public void click(ChannelData.Channel forChannel) {
        pullToRefresh();
    }

    private void loadTimeline() {
        viewModelSearch.sepiaSearch(sepiaSearchVideo).observe(this.requireActivity(), this::manageVIewVideos);
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
