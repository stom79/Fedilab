package app.fedilab.android.mastodon.activities;
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
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityTimelineBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;


public class TimelineActivity extends BaseBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app.fedilab.android.databinding.ActivityTimelineBinding binding = ActivityTimelineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(TimelineActivity.this).getBundle(bundleId, Helper.getCurrentAccount(TimelineActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {
        Timeline.TimeLineEnum timelineType = null;
        String lemmy_post_id = null;
        PinnedTimeline pinnedTimeline = null;
        Status status = null;
        if (bundle != null) {
            timelineType = (Timeline.TimeLineEnum) bundle.get(Helper.ARG_TIMELINE_TYPE);
            lemmy_post_id = bundle.getString(Helper.ARG_LEMMY_POST_ID, null);
            pinnedTimeline = (PinnedTimeline) bundle.getSerializable(Helper.ARG_REMOTE_INSTANCE);
            status = (Status) bundle.getSerializable(Helper.ARG_STATUS);
        }
        if (pinnedTimeline != null && pinnedTimeline.remoteInstance != null) {
            setTitle(pinnedTimeline.remoteInstance.host);
        }
        FragmentMastodonTimeline fragmentMastodonTimeline = new FragmentMastodonTimeline();
        Bundle args = new Bundle();
        args.putSerializable(Helper.ARG_TIMELINE_TYPE, timelineType);
        args.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinnedTimeline);
        args.putSerializable(Helper.ARG_LEMMY_POST_ID, lemmy_post_id);
        if (status != null) {
            args.putSerializable(Helper.ARG_STATUS, status);
        }
        new CachedBundle(TimelineActivity.this).insertBundle(args, Helper.getCurrentAccount(TimelineActivity.this), bundleId -> {
            Bundle bundle1 = new Bundle();
            bundle1.putLong(Helper.ARG_INTENT_ID, bundleId);
            fragmentMastodonTimeline.setArguments(bundle1);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_view, fragmentMastodonTimeline).commit();
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
