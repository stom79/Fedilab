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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.helper.Helper;


public class BasePeertubeActivity extends BaseBarActivity {

    protected ActivityPeertubeBinding binding;
    protected VideoData.Video peertube;
    protected ExoPlayer player;
    protected String videoURL;
    protected String subtitlesStr;

    private CastContext mCastContext;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        final SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        boolean search_cast = sharedpreferences.getBoolean(getString(R.string.set_cast_choice), false);
        if (search_cast) {
            setupCastListener();
            mCastContext = CastContext.getSharedInstance(BasePeertubeActivity.this);
            mCastSession = mCastContext.getSessionManager().getCurrentCastSession();

        }

    }


    protected void loadCast() {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_TITLE, peertube.getTitle());
        movieMetadata.putString(MediaMetadata.KEY_ARTIST, peertube.getAccount().getDisplayName());
        if (subtitlesStr != null) {
            movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitlesStr);
        }
        movieMetadata.addImage(new WebImage(Uri.parse("https://" + peertube.getChannel().getHost() + peertube.getPreviewPath())));
        MediaInfo mediaInfo = new MediaInfo.Builder(videoURL)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(movieMetadata)
                .setStreamDuration(peertube.getDuration() * 1000L)
                .build();
        if (mCastSession != null) {
            RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            remoteMediaClient.load(mediaInfo);
        }
    }


    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionStarting(@NonNull CastSession castSession) {
            }

            @Override
            public void onSessionStarted(@NonNull CastSession castSession, String s) {
                onApplicationConnected(castSession, true);
            }

            @Override
            public void onSessionStartFailed(@NonNull CastSession castSession, int i) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionEnding(@NonNull CastSession castSession) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionEnded(@NonNull CastSession castSession, int i) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResuming(@NonNull CastSession castSession, String s) {
            }

            @Override
            public void onSessionResumed(@NonNull CastSession castSession, boolean b) {
                onApplicationConnected(castSession, false);
            }

            @Override
            public void onSessionResumeFailed(@NonNull CastSession castSession, int i) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionSuspended(@NonNull CastSession castSession, int i) {
                onApplicationDisconnected();
            }

            private void onApplicationConnected(CastSession castSession, boolean hide) {
                mCastSession = castSession;
                supportInvalidateOptionsMenu();
                player.setPlayWhenReady(false);
                if (hide) {
                    binding.doubleTapPlayerView.setVisibility(View.INVISIBLE);
                }
                binding.minController.castMiniController.setVisibility(View.VISIBLE);
                loadCast();
            }

            private void onApplicationDisconnected() {
                binding.doubleTapPlayerView.setVisibility(View.VISIBLE);
                binding.minController.castMiniController.setVisibility(View.GONE);
                supportInvalidateOptionsMenu();
            }
        };
    }

    @Override
    protected void onResume() {
        if (mCastContext != null) {
            mCastContext.getSessionManager().addSessionManagerListener(
                    mSessionManagerListener, CastSession.class);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mCastContext != null) {
            mCastContext.getSessionManager().removeSessionManagerListener(
                    mSessionManagerListener, CastSession.class);
        }
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.video_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_button);
        return true;
    }
}
