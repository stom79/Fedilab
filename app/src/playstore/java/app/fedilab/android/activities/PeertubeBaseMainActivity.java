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


import android.os.Bundle;
import android.view.View;

import app.fedilab.android.databinding.ActivityMainPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseActivity;


public class PeertubeBaseMainActivity extends BaseActivity {

    protected ActivityMainPeertubeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
    }

    //Method for discovering cast devices
    public void discoverCast() {
    }

}
