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
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class PlaylistsActivity extends BaseBarActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        setContentView(R.layout.activity_playlists_peertube);


        PlaylistData.Playlist playlist;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            playlist = b.getParcelable("playlist");
            if (playlist == null) {
                return;
            }
        } else {
            Toasty.error(PlaylistsActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        setTitle(playlist.getDisplayName());
        if (savedInstanceState == null) {
            DisplayVideosFragment displayVideosFragment = new DisplayVideosFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.VIDEOS_IN_PLAYLIST);
            bundle.putSerializable("playlistId", playlist.getUuid());
            displayVideosFragment.setArguments(bundle);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.nav_host_fragment, displayVideosFragment).commit();
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
