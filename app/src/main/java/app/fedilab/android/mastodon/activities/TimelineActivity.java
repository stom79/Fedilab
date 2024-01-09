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

import static app.fedilab.android.BaseMainActivity.currentAccount;

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
        Bundle b = getIntent().getExtras();
        Timeline.TimeLineEnum timelineType = null;
        String lemmy_post_id = null;
        PinnedTimeline pinnedTimeline = null;
        Status status = null;
        if (b != null) {
            timelineType = (Timeline.TimeLineEnum) b.get(Helper.ARG_TIMELINE_TYPE);
            lemmy_post_id = b.getString(Helper.ARG_LEMMY_POST_ID, null);
            pinnedTimeline = (PinnedTimeline) b.getSerializable(Helper.ARG_REMOTE_INSTANCE);
            status = (Status) b.getSerializable(Helper.ARG_STATUS);
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
        new CachedBundle(TimelineActivity.this).insertBundle(args, currentAccount, bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            fragmentMastodonTimeline.setArguments(bundle);
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
