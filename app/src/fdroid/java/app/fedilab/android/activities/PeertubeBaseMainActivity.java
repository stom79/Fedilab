package app.fedilab.android.activities;
/* Copyright 2021 Thomas Schneider
 *
 * This file is a part of TubeLab
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityMainPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.helper.Helper;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;
import su.litvak.chromecast.api.v2.MediaStatus;

public abstract class PeertubeBaseMainActivity extends BaseActivity implements ChromeCastsListener {

    public static List<ChromeCast> chromeCasts;
    public static ChromeCast chromeCast;
    public static boolean chromecastActivated = false;
    protected ActivityMainPeertubeBinding binding;
    private BroadcastReceiver manage_chromecast;
    private VideoData.Video castedTube;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ChromeCastsListener chromeCastsListener = this;
        ChromeCasts.registerListener(chromeCastsListener);


        binding.castClose.setOnClickListener(v -> {
            Intent intentBC = new Intent(Helper.RECEIVE_CAST_SETTINGS);
            Bundle b = new Bundle();
            b.putInt("displayed", 0);
            intentBC.putExtras(b);
            LocalBroadcastManager.getInstance(PeertubeBaseMainActivity.this).sendBroadcast(intentBC);
        });

        binding.castTogglePlay.setOnClickListener(v -> {
            if (chromeCast != null) {
                new Thread(() -> {
                    try {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> binding.castTogglePlay.setVisibility(View.GONE);
                        mainHandler.post(myRunnable);
                        int icon = -1;
                        if (chromeCast.getMediaStatus().playerState == MediaStatus.PlayerState.PLAYING) {
                            chromeCast.pause();
                            icon = R.drawable.ic_baseline_play_arrow_32;
                        } else if (chromeCast.getMediaStatus().playerState == MediaStatus.PlayerState.PAUSED) {
                            chromeCast.play();
                            icon = R.drawable.ic_baseline_pause_32;
                        }
                        if (icon != -1) {
                            int finalIcon = icon;
                            myRunnable = () -> binding.castTogglePlay.setImageResource(finalIcon);
                            mainHandler.post(myRunnable);
                        }
                        myRunnable = () -> binding.castTogglePlay.setVisibility(View.VISIBLE);
                        mainHandler.post(myRunnable);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });


        manage_chromecast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle b = intent.getExtras();
                assert b != null;
                int state = b.getInt("state_asked", -1);
                int displayed = b.getInt("displayed", -1);
                castedTube = (VideoData.Video) b.getSerializable("castedTube");

                if (state == 1) {
                    discoverCast();
                } else if (state == 0) {
                    new Thread(() -> {
                        try {
                            if (chromeCast != null) {
                                chromeCast.stopApp();
                                chromeCast.disconnect();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                if (displayed == 1) {
                    chromecastActivated = true;
                    if (castedTube != null) {
                        binding.castInfo.setVisibility(View.VISIBLE);
                        Helper.loadGiF(PeertubeBaseMainActivity.this, castedTube.getThumbnailPath(), binding.castView);
                        binding.castTitle.setText(castedTube.getTitle());
                        binding.castDescription.setText(castedTube.getDescription());
                    }
                } else if (displayed == 0) {
                    chromecastActivated = false;
                    binding.castInfo.setVisibility(View.GONE);
                    new Thread(() -> {
                        try {
                            if (chromeCast != null) {
                                chromeCast.stopApp();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        };
        LocalBroadcastManager.getInstance(PeertubeBaseMainActivity.this).registerReceiver(manage_chromecast, new IntentFilter(Helper.RECEIVE_CAST_SETTINGS));
    }

    @Override
    public void newChromeCastDiscovered(ChromeCast chromeCast) {
        if (chromeCasts == null) {
            chromeCasts = new ArrayList<>();
            chromeCasts.add(chromeCast);
        } else {
            boolean canBeAdded = true;
            for (ChromeCast cast : chromeCasts) {
                if (cast.getName().compareTo(chromeCast.getName()) == 0) {
                    canBeAdded = false;
                    break;
                }
            }
            if (canBeAdded) {
                chromeCasts.add(chromeCast);
            }
        }
        try {
            if (chromeCast.isAppRunning(Helper.CAST_ID) && chromeCast.getMediaStatus() != null && chromeCast.getMediaStatus().playerState != null) {
                if (binding.castInfo.getVisibility() == View.GONE) {
                    binding.castInfo.setVisibility(View.VISIBLE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void chromeCastRemoved(ChromeCast chromeCast) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ChromeCasts.unregisterListener(this);
        if (manage_chromecast != null) {
            LocalBroadcastManager.getInstance(PeertubeBaseMainActivity.this).unregisterReceiver(manage_chromecast);

            new Thread(() -> {
                if (chromeCasts != null && chromeCasts.size() > 0) {
                    for (ChromeCast cast : chromeCasts) {
                        try {
                            cast.stopApp();
                            cast.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        if (chromeCasts != null) {
            chromeCasts = null;
        }
        if (chromeCast != null) {
            chromeCast = null;
        }
    }


    //Method for discovering cast devices
    public void discoverCast() {

        new Thread(() -> {
            if (chromeCasts != null) {
                for (ChromeCast cast : chromeCasts) {
                    try {
                        cast.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                chromeCasts = null;
            }
            chromeCasts = new ArrayList<>();
            try {
                List<NetworkInterface> interfaces;
                interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface ni : interfaces) {
                    if ((!ni.isLoopback()) && ni.isUp() && (ni.getName().equals("wlan0"))) {
                        Enumeration<InetAddress> inetAddressEnumeration = ni.getInetAddresses();
                        while (inetAddressEnumeration.hasMoreElements()) {
                            InetAddress inetAddress = inetAddressEnumeration.nextElement();
                            ChromeCasts.restartDiscovery(inetAddress);
                            int tryFind = 0;
                            while (ChromeCasts.get().isEmpty() && tryFind < 5) {
                                try {
                                    //noinspection BusyWait
                                    Thread.sleep(1000);
                                    tryFind++;
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    }
                }
                ChromeCasts.stopDiscovery();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = this::invalidateOptionsMenu;
                mainHandler.post(myRunnable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


}
