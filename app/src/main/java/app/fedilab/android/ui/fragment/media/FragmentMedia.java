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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.arges.sepan.argmusicplayer.Models.ArgAudio;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.Timer;

import app.fedilab.android.R;
import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.databinding.FragmentSlideMediaBinding;
import app.fedilab.android.helper.CacheDataSourceFactory;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.webview.CustomWebview;
import app.fedilab.android.webview.FedilabWebChromeClient;
import app.fedilab.android.webview.FedilabWebViewClient;


public class FragmentMedia extends Fragment {


    private SimpleExoPlayer player;
    private Timer timer;
    private String url;
    private boolean canSwipe;
    private Attachment attachment;
    private boolean swipeEnabled;
    private CustomWebview webview_video;
    private FragmentSlideMediaBinding binding;
    private ArgAudio audio;

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
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        url = attachment.url;
        binding.mediaPicture.setOnMatrixChangeListener(rect -> {
            canSwipe = (binding.mediaPicture.getScale() == 1);

            if (!canSwipe) {
                if (!((MediaActivity) requireActivity()).getFullScreen()) {
                    ((MediaActivity) requireActivity()).setFullscreen(true);
                }
                enableSliding(false);
            } else {
                enableSliding(true);
            }
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

        binding.mediaPicture.setVisibility(View.VISIBLE);
        binding.mediaPicture.setTransitionName(attachment.url);
        if (Helper.isValidContextForGlide(requireActivity())) {
            Glide.with(requireActivity())
                    .asBitmap()
                    .dontTransform()
                    .load(preview_url).into(
                    new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull final Bitmap resource, Transition<? super Bitmap> transition) {
                            binding.mediaPicture.setImageBitmap(resource);
                            scheduleStartPostponedTransition(binding.mediaPicture);
                            if (attachment.type.equalsIgnoreCase("image") && !attachment.url.toLowerCase().endsWith(".gif")) {
                                final Handler handler = new Handler();
                                handler.postDelayed(() -> {
                                    if (binding == null) {
                                        return;
                                    }
                                    binding.pbarInf.setScaleY(1f);
                                    binding.mediaPicture.setVisibility(View.VISIBLE);
                                    binding.pbarInf.setIndeterminate(true);
                                    binding.loader.setVisibility(View.VISIBLE);
                                    if (Helper.isValidContextForGlide(requireActivity())) {
                                        Glide.with(requireActivity())
                                                .asBitmap()
                                                .dontTransform()
                                                .load(url).into(
                                                new CustomTarget<Bitmap>() {
                                                    @Override
                                                    public void onResourceReady(@NonNull final Bitmap resource, Transition<? super Bitmap> transition) {
                                                        if (binding != null) {
                                                            binding.loader.setVisibility(View.GONE);
                                                            if (binding.mediaPicture.getScale() < 1.1) {
                                                                binding.mediaPicture.setImageBitmap(resource);
                                                            } else {
                                                                binding.messageReady.setVisibility(View.VISIBLE);
                                                            }
                                                            binding.messageReady.setOnClickListener(view -> {
                                                                binding.mediaPicture.setImageBitmap(resource);
                                                                binding.messageReady.setVisibility(View.GONE);
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                                    }
                                                }
                                        );
                                    }
                                }, 1000);


                            } else if (attachment.type.equalsIgnoreCase("image") && attachment.url.toLowerCase().endsWith(".gif")) {
                                binding.loader.setVisibility(View.GONE);
                                if (Helper.isValidContextForGlide(requireActivity())) {
                                    Glide.with(requireActivity())
                                            .load(url).into(binding.mediaPicture);
                                }
                                scheduleStartPostponedTransition(binding.mediaPicture);
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
                binding.pbarInf.setIndeterminate(false);
                binding.pbarInf.setScaleY(3f);
                binding.mediaVideo.setVisibility(View.VISIBLE);
                Uri uri = Uri.parse(url);

                String userAgent = sharedpreferences.getString(getString(R.string.SET_CUSTOM_USER_AGENT), Helper.USER_AGENT);
                int video_cache = sharedpreferences.getInt(getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);
                ProgressiveMediaSource videoSource;
                if (video_cache == 0) {
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireActivity(),
                            Util.getUserAgent(requireActivity(), userAgent), null);
                    videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(uri);
                } else {
                    CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(requireActivity());
                    videoSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(uri);
                }
                player = new SimpleExoPlayer.Builder(requireActivity()).build();
                if (type.equalsIgnoreCase("gifv"))
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                binding.mediaVideo.setPlayer(player);
                binding.loader.setVisibility(View.GONE);
                binding.mediaPicture.setVisibility(View.GONE);
                player.prepare(videoSource);
                player.setPlayWhenReady(true);
                break;
            case "web":
                binding.loader.setVisibility(View.GONE);
                binding.mediaPicture.setVisibility(View.GONE);
                webview_video = Helper.initializeWebview(requireActivity(), R.id.webview_video, binding.getRoot());
                webview_video.setVisibility(View.VISIBLE);
                FedilabWebChromeClient fedilabWebChromeClient = new FedilabWebChromeClient(requireActivity(), webview_video, binding.mainMediaFrame, binding.videoLayout);
                fedilabWebChromeClient.setOnToggledFullscreen(fullscreen -> {
                    if (fullscreen) {
                        binding.videoLayout.setVisibility(View.VISIBLE);
                        WindowManager.LayoutParams attrs = (requireActivity()).getWindow().getAttributes();
                        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        (requireActivity()).getWindow().setAttributes(attrs);
                        (requireActivity()).getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                    } else {
                        WindowManager.LayoutParams attrs = (requireActivity()).getWindow().getAttributes();
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                        (requireActivity()).getWindow().setAttributes(attrs);
                        (requireActivity()).getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                        binding.videoLayout.setVisibility(View.GONE);
                    }
                });
                webview_video.getSettings().setAllowFileAccess(true);
                webview_video.setWebChromeClient(fedilabWebChromeClient);
                webview_video.getSettings().setDomStorageEnabled(true);
                webview_video.getSettings().setAppCacheEnabled(true);
                String user_agent = sharedpreferences.getString(getString(R.string.SET_CUSTOM_USER_AGENT), Helper.USER_AGENT);
                webview_video.getSettings().setUserAgentString(user_agent);
                webview_video.getSettings().setMediaPlaybackRequiresUserGesture(false);
                webview_video.setWebViewClient(new FedilabWebViewClient(requireActivity()));
                webview_video.loadUrl(attachment.url);
                break;
        }
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
        if (webview_video != null) {
            webview_video.onPause();
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
        if (webview_video != null) {
            webview_video.destroy();
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }

        if (webview_video != null) {
            webview_video.onResume();
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
        } else if (!enable && swipeEnabled) {
            swipeEnabled = false;
        }
    }

}
