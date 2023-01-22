package app.fedilab.android.peertube.activities;
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


import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.drawer.PlaylistAdapter;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;


public class AllLocalPlaylistsActivity extends BaseBarActivity implements PlaylistAdapter.AllPlaylistRemoved {


    PlaylistAdapter playlistAdapter;
    private RelativeLayout mainLoader;
    private RelativeLayout textviewNoAction;
    private List<Playlist> playlists;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_playlist_peertube);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.playlists);


        textviewNoAction = findViewById(R.id.no_action);
        mainLoader = findViewById(R.id.loader);
        RelativeLayout nextElementLoader = findViewById(R.id.loading_next_items);
        mainLoader.setVisibility(View.VISIBLE);
        nextElementLoader.setVisibility(View.GONE);

        PlaylistsVM viewModel = new ViewModelProvider(AllLocalPlaylistsActivity.this).get(PlaylistsVM.class);
        viewModel.localePlaylist().observe(AllLocalPlaylistsActivity.this, this::manageVIewPlaylists);

        FloatingActionButton add_new = findViewById(R.id.add_new);
        add_new.setVisibility(View.GONE);

        TextView no_action_text = findViewById(R.id.no_action_text);
        no_action_text.setText(R.string.no_playlist);
        playlists = new ArrayList<>();
        RecyclerView lv_playlist = findViewById(R.id.lv_playlist);
        playlistAdapter = new PlaylistAdapter(playlists, true);
        playlistAdapter.allPlaylistRemoved = this;
        lv_playlist.setAdapter(playlistAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(AllLocalPlaylistsActivity.this);
        lv_playlist.setLayoutManager(mLayoutManager);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void manageVIewPlaylists(List<VideoPlaylistData.VideoPlaylistExport> videoPlaylistExports) {
        mainLoader.setVisibility(View.GONE);
        if (videoPlaylistExports == null) {
            textviewNoAction.setVisibility(View.VISIBLE);
            return;
        }
        if (videoPlaylistExports.size() > 0) {
            for (VideoPlaylistData.VideoPlaylistExport videoPlaylistExport : videoPlaylistExports) {
                playlists.add(videoPlaylistExport.getPlaylist());
            }
            playlistAdapter.notifyDataSetChanged();
            textviewNoAction.setVisibility(View.GONE);
        } else {
            textviewNoAction.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAllPlaylistRemoved() {
        textviewNoAction.setVisibility(View.VISIBLE);
    }

}
