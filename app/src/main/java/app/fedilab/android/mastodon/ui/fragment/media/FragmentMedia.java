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



import android.content.SharedPreferences;
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
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentSlideMediaBinding;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.helper.CacheDataSourceFactory;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.helper.TranslateHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;
import es.dmoral.toasty.Toasty;


public class FragmentMedia extends Fragment {


    private ExoPlayer player;
    private String url;
    private boolean canSwipe;
    private Attachment attachment;
    private boolean swipeEnabled;
    private FragmentSlideMediaBinding binding;
    private SlidrInterface slidrInterface;
    private int mediaPictureTranslateAccessibilityActionId = 0;
    private int mediaVideoTranslateAccessibilityActionId = 0;

    private boolean visible = false;

    public FragmentMedia() {
    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideMediaBinding.inflate(inflater, container, false);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            attachment = (Attachment) bundle.getSerializable(Helper.ARG_MEDIA_ATTACHMENT);
        }
        binding.controls.hide();
        return binding.getRoot();
    }


    @OptIn(markerClass = UnstableApi.class)
    public void toggleController(boolean display) {
        if (display) {
            binding.controls.show();
        } else {
            binding.controls.hide();
        }
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
            if (!canSwipe && isAdded() && !requireActivity().isFinishing()) {
                if (!((MediaActivity) requireActivity()).getFullScreen()) {
                    ((MediaActivity) requireActivity()).setFullscreen(true);
                }
                enableSliding(false);
            } else {
                enableSliding(true);
            }
        });
        binding.mediaPicture.setOnClickListener(v -> {
            if (isAdded()) {
                ((MediaActivity) requireActivity()).toogleFullScreen();
            }
        });

        binding.mediaVideo.setOnClickListener(v -> {
            if (isAdded()) {
                ((MediaActivity) requireActivity()).toogleFullScreen();
            }
        });

        if (attachment.description != null) {
            binding.mediaPicture.setContentDescription(attachment.description);
            mediaPictureTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaPicture, getString(R.string.translate), (view2, arguments) -> {
                translate();
                return true;
            });

            binding.mediaVideo.setContentDescription(attachment.description);
            mediaVideoTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaVideo, getString(R.string.translate), (view2, arguments) -> {
                translate();
                return true;
            });
        }

        mediaPictureTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaPicture, getString(R.string.download), (view2, arguments) -> {
            ((MediaActivity) requireActivity()).saveMedia();
            return true;
        });
        mediaPictureTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaVideo, getString(R.string.download), (view2, arguments) -> {
            ((MediaActivity) requireActivity()).saveMedia();
            return true;
        });
        mediaPictureTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaPicture, getString(R.string.share), (view2, arguments) -> {
            ((MediaActivity) requireActivity()).shareMedia();
            return true;
        });
        mediaPictureTranslateAccessibilityActionId = ViewCompat.addAccessibilityAction(binding.mediaVideo, getString(R.string.share), (view2, arguments) -> {
            ((MediaActivity) requireActivity()).shareMedia();
            return true;
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
        scheduleStartPostponedTransition(binding.mediaPicture);
        if (Helper.isValidContextForGlide(requireActivity()) && isAdded()) {
            String finalType1 = type;
            Glide.with(requireActivity())
                    .asDrawable()
                    .dontTransform()
                    .load(preview_url).into(
                            new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull final Drawable resource, Transition<? super Drawable> transition) {
                                    if (binding == null || !isAdded() || getActivity() == null) {
                                        return;
                                    }
                                    binding.mediaPicture.setZoomable(true);

                                    Drawable scaledRessource = MediaHelper.rescaleImageIfNeeded(requireActivity(), resource);
                                    binding.mediaPicture.setImageDrawable(scaledRessource);

                                    if (attachment.type.equalsIgnoreCase("image") && !attachment.url.toLowerCase().endsWith(".gif")) {
                                        binding.mediaPicture.setVisibility(View.VISIBLE);
                                        final Handler handler = new Handler();
                                        handler.postDelayed(() -> {
                                            if (isAdded() && Helper.isValidContextForGlide(requireActivity())) {
                                                Glide.with(requireActivity())
                                                        .asDrawable()
                                                        .dontTransform()
                                                        .load(url).into(
                                                                new CustomTarget<Drawable>() {
                                                                    @Override
                                                                    public void onResourceReady(@NonNull final Drawable resource, Transition<? super Drawable> transition) {
                                                                        if (binding == null || !isAdded() || getActivity() == null) {
                                                                            return;
                                                                        }
                                                                        binding.loader.setVisibility(View.GONE);
                                                                        Drawable scaledRessource = MediaHelper.rescaleImageIfNeeded(requireActivity(), resource);
                                                                        binding.mediaPicture.setImageDrawable(scaledRessource);
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
                                    if (binding == null || !isAdded() || getActivity() == null) {
                                        return;
                                    }
                                    scheduleStartPostponedTransition(binding.mediaPicture);
                                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                                    boolean autofetch = sharedpreferences.getBoolean(getString(R.string.SET_FETCH_REMOTE_MEDIA), false);
                                    if (autofetch) {
                                        binding.loadRemote.setVisibility(View.GONE);
                                        binding.loader.setVisibility(View.GONE);
                                        Glide.with(requireActivity())
                                                .load(attachment.remote_url).into(binding.mediaPicture);
                                    } else if (finalType1.equalsIgnoreCase("image")) {
                                        Toasty.error(requireActivity(), getString(R.string.toast_error_media), Toasty.LENGTH_SHORT).show();
                                        binding.loadRemote.setVisibility(View.VISIBLE);
                                        binding.loadRemote.setOnClickListener(v -> {
                                            binding.loadRemote.setVisibility(View.GONE);
                                            binding.loader.setVisibility(View.GONE);
                                            Glide.with(requireActivity())
                                                    .load(attachment.remote_url).into(binding.mediaPicture);
                                        });
                                    }

                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            }
                    );
        }
        switch (type.toLowerCase()) {
            case "video", "audio", "gifv" -> {
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
            }
        }
    }

    public void translate() {
        if (attachment.translation == null) TranslateHelper.translate(
                requireContext(),
                attachment.description,
                ((MediaActivity) requireActivity()).getStatusLanguageForTranslation(),
                translated -> {
                    attachment.translation = translated;
                    String translatedMediaDescription = getString(R.string.cd_translated_media_description, attachment.translation);

                    binding.mediaPicture.setContentDescription(translatedMediaDescription);
                    ViewCompat.removeAccessibilityAction(binding.mediaPicture, mediaPictureTranslateAccessibilityActionId);

                    binding.mediaVideo.setContentDescription(translatedMediaDescription);
                    ViewCompat.removeAccessibilityAction(binding.mediaVideo, mediaVideoTranslateAccessibilityActionId);
                });
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    private void loadVideo(String url, String type) {
        if (binding == null || !isAdded() || getActivity() == null || url == null) {
            return;
        }
        binding.pbarInf.setIndeterminate(false);
        binding.pbarInf.setScaleY(3f);
        binding.videoViewContainer.setVisibility(View.VISIBLE);
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
        if (type.equalsIgnoreCase("gifv")) {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
            binding.mediaVideo.setUseController(false);
        }
        binding.mediaVideo.setPlayer(player);
        binding.controls.setPlayer(player);
        binding.loader.setVisibility(View.GONE);
        binding.mediaPicture.setVisibility(View.GONE);
        player.setMediaSource(videoSource);
        player.prepare();
        player.setPlayWhenReady(visible);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                boolean autofetch = sharedpreferences.getBoolean(getString(R.string.SET_FETCH_REMOTE_MEDIA), false);
                if (autofetch) {
                    binding.loadRemote.setVisibility(View.GONE);
                    binding.loader.setVisibility(View.GONE);
                    loadVideo(attachment.remote_url, type);
                } else {
                    Toasty.error(requireActivity(), getString(R.string.toast_error_media), Toasty.LENGTH_SHORT).show();
                    binding.loadRemote.setVisibility(View.VISIBLE);
                    binding.loadRemote.setOnClickListener(v -> {
                        binding.loadRemote.setVisibility(View.GONE);
                        binding.loader.setVisibility(View.GONE);
                        loadVideo(attachment.remote_url, type);
                    });
                }
            }

        });
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onPause() {
        super.onPause();
        visible = false;
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
        visible = true;
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
                                binding.videoViewContainer.setVisibility(View.GONE);
                                binding.videoLayout.setVisibility(View.GONE);
                                try {
                                    ActivityCompat.finishAfterTransition(requireActivity());
                                } catch (Exception ignored) {
                                }
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
                if (isAdded()) {
                    ActivityCompat.startPostponedEnterTransition(requireActivity());
                }
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
