package app.fedilab.android.ui.fragment.media;
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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;

import app.fedilab.android.R;
import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.databinding.FragmentSlideMediaBinding;
import app.fedilab.android.helper.CacheDataSourceFactory;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class FragmentMedia extends Fragment {


    private ExoPlayer player;
    private String url;
    private boolean canSwipe;
    private Attachment attachment;
    private boolean swipeEnabled;
    private FragmentSlideMediaBinding binding;
    private SlidrInterface slidrInterface;

    public FragmentMedia() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideMediaBinding.inflate(inflater, container, false);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            attachment = (Attachment) bundle.getSerializable(Helper.ARG_MEDIA_ATTACHMENT);
        }
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeEnabled = true;

        url = attachment.url;
        binding.mediaPicture.setOnMatrixChangeListener(rect -> {
            if (binding == null) {
                return;
            }
            canSwipe = (binding.mediaPicture.getScale() == 1);
            if (!canSwipe && !requireActivity().isFinishing() && isAdded()) {
                if (!((MediaActivity) requireActivity()).getFullScreen()) {
                    ((MediaActivity) requireActivity()).setFullscreen(true);
                }
                enableSliding(false);
            } else {
                enableSliding(true);
            }
        });
        binding.mediaPicture.setOnClickListener(v -> {
            ((MediaActivity) requireActivity()).toogleFullScreen();
        });

        binding.mediaVideo.setOnClickListener(v -> {
            ((MediaActivity) requireActivity()).toogleFullScreen();
        });

        String type = attachment.type;
        String preview_url = attachment.preview_url;
        if (type.equalsIgnoreCase("unknown")) {
            preview_url = attachment.remote_url;
            if (preview_url.toLowerCase().endsWith(".png") || preview_url.toLowerCase().endsWith(".jpg") || preview_url.toLowerCase().endsWith(".jpeg") || preview_url.toLowerCase().endsWith(".gif")) {
                type = "image";
            } else if (preview_url.toLowerCase().endsWith(".mp4") || preview_url.toLowerCase().endsWith(".mp3")) {
                type = "video";
            }
            url = attachment.remote_url;
            attachment.type = type;
        }
        binding.mediaPicture.setZoomable(false);
        binding.mediaPicture.setTransitionName(attachment.url);
        binding.mediaPicture.setVisibility(View.VISIBLE);

        binding.pbarInf.setScaleY(1f);
        binding.pbarInf.setIndeterminate(true);
        binding.loader.setVisibility(View.VISIBLE);
        if (Helper.isValidContextForGlide(requireActivity()) && isAdded()) {
            Glide.with(requireActivity())
                    .asBitmap()
                    .dontTransform()
                    .load(preview_url).into(
                            new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull final Bitmap resource, Transition<? super Bitmap> transition) {
                                    if (binding == null || !isAdded() || getActivity() == null) {
                                        return;
                                    }
                                    binding.mediaPicture.setImageBitmap(resource);
                                    scheduleStartPostponedTransition(binding.mediaPicture);
                                    if (attachment.type.equalsIgnoreCase("image") && !attachment.url.toLowerCase().endsWith(".gif")) {
                                        binding.mediaPicture.setVisibility(View.VISIBLE);
                                        final Handler handler = new Handler();
                                        handler.postDelayed(() -> {
                                            if (Helper.isValidContextForGlide(requireActivity()) && isAdded()) {
                                                Glide.with(requireActivity())
                                                        .asBitmap()
                                                        .dontTransform()
                                                        .load(url).into(
                                                                new CustomTarget<Bitmap>() {
                                                                    @Override
                                                                    public void onResourceReady(@NonNull final Bitmap resource, Transition<? super Bitmap> transition) {
                                                                        if (binding == null || !isAdded() || getActivity() == null) {
                                                                            return;
                                                                        }
                                                                        binding.loader.setVisibility(View.GONE);
                                                                        binding.mediaPicture.setImageBitmap(resource);
                                                                        binding.mediaPicture.setZoomable(true);
                                                                    }

                                                                    @Override
                                                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                                                    }
                                                                }
                                                        );
                                            }
                                        }, 500);
                                    } else if (attachment.type.equalsIgnoreCase("image") && attachment.url.toLowerCase().endsWith(".gif")) {
                                        binding.loader.setVisibility(View.GONE);
                                        binding.mediaPicture.setVisibility(View.VISIBLE);
                                        if (Helper.isValidContextForGlide(requireActivity())) {
                                            binding.mediaPicture.setZoomable(true);
                                            Glide.with(requireActivity())
                                                    .load(url).into(binding.mediaPicture);
                                        }
                                    }
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    scheduleStartPostponedTransition(binding.mediaPicture);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            }
                    );
        }
        switch (type.toLowerCase()) {
            case "video":
            case "audio":
            case "gifv":
                if (attachment.peertubeId != null) {
                    //It's a peertube video, we are fetching data
                    TimelinesVM timelinesVM = new ViewModelProvider(requireActivity()).get(TimelinesVM.class);
                    String finalType = type;
                    timelinesVM.getPeertubeVideo(attachment.peertubeHost, attachment.peertubeId).observe(requireActivity(), video -> {
                        if (video != null && video.files != null && video.files.size() > 0) {
                            loadVideo(video.files.get(0).fileUrl, finalType);
                        } else if (video != null && video.streamingPlaylists != null && video.streamingPlaylists.size() > 0 && video.streamingPlaylists.get(0).files.size() > 0) {
                            loadVideo(video.streamingPlaylists.get(0).files.get(0).fileUrl, finalType);
                        }
                    });
                } else {
                    loadVideo(url, type);
                }
                break;
        }
    }

    private void loadVideo(String url, String type) {
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.pbarInf.setIndeterminate(false);
        binding.pbarInf.setScaleY(3f);
        binding.mediaVideo.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse(url);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        int video_cache = sharedpreferences.getInt(getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);
        ProgressiveMediaSource videoSource;
        MediaItem mediaItem = new MediaItem.Builder().setUri(uri).build();
        if (video_cache == 0) {
            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(requireActivity());
            videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(requireActivity());
            videoSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem);
        }
        player = new ExoPlayer.Builder(requireActivity()).build();
        if (type.equalsIgnoreCase("gifv"))
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        binding.mediaVideo.setPlayer(player);
        binding.loader.setVisibility(View.GONE);
        binding.mediaPicture.setVisibility(View.GONE);
        player.setMediaSource(videoSource);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (player != null) {
                player.release();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
        if (slidrInterface == null) {
            slidrInterface = Slidr.replace(binding.mediaFragmentContainer, new SlidrConfig.Builder().sensitivity(1f)
                    .scrimColor(Color.BLACK)
                    .scrimStartAlpha(0.8f)
                    .scrimEndAlpha(0f)
                    .position(SlidrPosition.VERTICAL)
                    .velocityThreshold(2400)
                    .distanceThreshold(0.25f)
                    .edgeSize(0.18f)
                    .listener(new SlidrListener() {
                        @Override
                        public void onSlideStateChanged(int state) {

                        }

                        @Override
                        public void onSlideChange(float percent) {
                            if (percent < 0.70) {
                                binding.mediaVideo.setVisibility(View.GONE);
                                binding.videoLayout.setVisibility(View.GONE);
                                ActivityCompat.finishAfterTransition(requireActivity());
                            }

                        }

                        @Override
                        public void onSlideOpened() {

                        }

                        @Override
                        public boolean onSlideClosed() {
                            return false;
                        }
                    })
                    .build());
        }
    }

    private void scheduleStartPostponedTransition(final ImageView imageView) {
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                ActivityCompat.startPostponedEnterTransition(requireActivity());
                return true;
            }
        });
    }


    private void enableSliding(boolean enable) {
        if (enable && !swipeEnabled) {
            swipeEnabled = true;
            if (slidrInterface != null) {
                slidrInterface.unlock();
            }
        } else if (!enable && swipeEnabled) {
            if (slidrInterface != null) {
                slidrInterface.lock();
            }
            swipeEnabled = false;
        }
    }

}
