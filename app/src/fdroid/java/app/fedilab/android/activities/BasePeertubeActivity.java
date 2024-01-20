package app.fedilab.android.activities;
/* Copyright 2023 Thomas Schneider
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



import static app.fedilab.android.peertube.helper.Helper.CAST_ID;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.Player;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.helper.Helper;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.MediaStatus;
import su.litvak.chromecast.api.v2.Status;


public class BasePeertubeActivity extends BaseBarActivity {

    protected ActivityPeertubeBinding binding;
    protected VideoData.Video peertube;
    protected Player player;
    protected String videoURL;
    protected String subtitlesStr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        binding.minController.castPlay.setOnClickListener(v -> {
            binding.minController.castLoader.setVisibility(View.VISIBLE);
            if (PeertubeBaseMainActivity.chromeCast != null) {
                new Thread(() -> {
                    try {
                        int icon = -1;
                        if (PeertubeBaseMainActivity.chromeCast.getMediaStatus().playerState == MediaStatus.PlayerState.PLAYING) {
                            PeertubeBaseMainActivity.chromeCast.pause();
                            icon = R.drawable.ic_baseline_play_arrow_32;
                        } else if (PeertubeBaseMainActivity.chromeCast.getMediaStatus().playerState == MediaStatus.PlayerState.PAUSED) {
                            PeertubeBaseMainActivity.chromeCast.play();
                            icon = R.drawable.ic_baseline_pause_32;
                        }
                        if (icon != -1) {
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            int finalIcon = icon;
                            Runnable myRunnable = () -> binding.minController.castPlay.setImageResource(finalIcon);
                            mainHandler.post(myRunnable);
                        }
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> binding.minController.castLoader.setVisibility(View.GONE);
                        mainHandler.post(myRunnable);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cast) {
            if (PeertubeBaseMainActivity.chromeCasts != null && PeertubeBaseMainActivity.chromeCasts.size() > 0) {
                String[] chromecast_choice = new String[PeertubeBaseMainActivity.chromeCasts.size()];
                AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(this);
                alt_bld.setTitle(R.string.chromecast_choice);
                int i = 0;
                for (ChromeCast cc : PeertubeBaseMainActivity.chromeCasts) {
                    chromecast_choice[i] = cc.getTitle();
                    i++;
                }
                i = 0;
                for (ChromeCast cc : PeertubeBaseMainActivity.chromeCasts) {
                    if (PeertubeBaseMainActivity.chromecastActivated && cc.isConnected()) {
                        break;
                    }
                    i++;
                }

                alt_bld.setSingleChoiceItems(chromecast_choice, i, (dialog, position) -> {
                    PeertubeBaseMainActivity.chromeCast = PeertubeBaseMainActivity.chromeCasts.get(position);
                    new Thread(() -> {
                        if (PeertubeBaseMainActivity.chromeCast != null) {
                            Intent intentBC = new Intent(Helper.RECEIVE_CAST_SETTINGS);
                            Bundle b = new Bundle();
                            if (PeertubeBaseMainActivity.chromecastActivated) {
                                b.putInt("displayed", 0);
                                intentBC.putExtras(b);
                                intentBC.setPackage(BuildConfig.APPLICATION_ID);
                                sendBroadcast(intentBC);
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                Runnable myRunnable = () -> {
                                    binding.doubleTapPlayerView.setVisibility(View.VISIBLE);
                                    binding.minController.castMiniController.setVisibility(View.GONE);
                                };
                                mainHandler.post(myRunnable);

                            } else {
                                b.putInt("displayed", 1);
                                b.putSerializable("castedTube", peertube);
                                intentBC.putExtras(b);
                                intentBC.setPackage(BuildConfig.APPLICATION_ID);
                                sendBroadcast(intentBC);
                                try {
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> {
                                        invalidateOptionsMenu();
                                        binding.minController.castLoader.setVisibility(View.VISIBLE);
                                        player.setPlayWhenReady(false);
                                        binding.doubleTapPlayerView.setVisibility(View.GONE);
                                        binding.minController.castMiniController.setVisibility(View.VISIBLE);
                                        dialog.dismiss();
                                        if (videoURL != null) {
                                            if (player != null && player.getCurrentPosition() > 0) {
                                                videoURL += "?start=" + (player.getCurrentPosition() / 1000);
                                            }
                                        }
                                    };
                                    mainHandler.post(myRunnable);
                                    if (!PeertubeBaseMainActivity.chromeCast.isConnected()) {
                                        PeertubeBaseMainActivity.chromeCast.connect();
                                    }
                                    myRunnable = this::invalidateOptionsMenu;
                                    mainHandler.post(myRunnable);
                                    Status status = PeertubeBaseMainActivity.chromeCast.getStatus();
                                    if (PeertubeBaseMainActivity.chromeCast.isAppAvailable(CAST_ID) && !status.isAppRunning(CAST_ID)) {
                                        PeertubeBaseMainActivity.chromeCast.launchApp(CAST_ID);
                                    }
                                    if (videoURL != null) {
                                        String mime = MimeTypeMap.getFileExtensionFromUrl(videoURL);
                                        PeertubeBaseMainActivity.chromeCast.setRequestTimeout(60000);
                                        PeertubeBaseMainActivity.chromeCast.load(peertube.getTitle(), null, videoURL, mime);
                                        PeertubeBaseMainActivity.chromeCast.play();
                                        binding.minController.castPlay.setImageResource(R.drawable.ic_baseline_pause_32);
                                    }
                                    myRunnable = () -> binding.minController.castLoader.setVisibility(View.GONE);
                                    mainHandler.post(myRunnable);
                                } catch (IOException | GeneralSecurityException e) {
                                    e.printStackTrace();
                                }
                            }
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> {
                                invalidateOptionsMenu();
                                dialog.dismiss();
                            };
                            mainHandler.post(myRunnable);
                        }
                    }).start();
                });
                alt_bld.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
                AlertDialog alert = alt_bld.create();
                alert.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.video_menu, menu);
        MenuItem castItem = menu.findItem(R.id.action_cast);
        if (PeertubeBaseMainActivity.chromeCasts != null && PeertubeBaseMainActivity.chromeCasts.size() > 0) {
            castItem.setVisible(true);
            if (PeertubeBaseMainActivity.chromeCast != null && PeertubeBaseMainActivity.chromeCast.isConnected()) {
                castItem.setIcon(R.drawable.ic_baseline_cast_connected_24);
            } else {
                castItem.setIcon(R.drawable.ic_baseline_cast_24);
            }
        }
        return true;
    }

}
