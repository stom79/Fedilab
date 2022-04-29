package app.fedilab.android.activities;
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


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.Pinned;
import app.fedilab.android.client.entities.StatusDraft;
import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.TagTimeline;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.ActivityHashtagBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import es.dmoral.toasty.Toasty;


public class HashTagActivity extends BaseActivity {


    public static int position;
    private String tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        ActivityHashtagBinding binding = ActivityHashtagBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        Bundle b = getIntent().getExtras();
        if (b != null) {
            tag = b.getString(Helper.ARG_SEARCH_KEYWORD, null);
        }
        if (tag == null)
            finish();

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        binding.title.setText(tag);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TAG);
        bundle.putString(Helper.ARG_SEARCH_KEYWORD, tag);
        Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_tags, new FragmentMastodonTimeline(), bundle, null, null);
        binding.toolbar.setPopupTheme(Helper.popupStyle());
        binding.compose.setOnClickListener(v -> {
            Intent intentToot = new Intent(HashTagActivity.this, ComposeActivity.class);
            StatusDraft statusDraft = new StatusDraft();
            Status status = new Status();
            status.text = "#" + tag;
            List<Status> statuses = new ArrayList<>();
            statuses.add(status);
            statusDraft.statusDraftList = statuses;
            Bundle _b = new Bundle();
            _b.putSerializable(Helper.ARG_TAG_TIMELINE, statusDraft);
            intentToot.putExtras(_b);
            startActivity(intentToot);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_add_timeline) {
            new Thread(() -> {
                try {
                    Pinned pinned = new Pinned(HashTagActivity.this).getPinned(BaseMainActivity.accountWeakReference.get());
                    boolean canBeAdded = true;
                    boolean update = true;
                    if (pinned == null) {
                        pinned = new Pinned();
                        pinned.pinnedTimelines = new ArrayList<>();
                        update = false;
                    } else {
                        for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                            if (pinnedTimeline.type == Timeline.TimeLineEnum.TAG) {
                                if (pinnedTimeline.tagTimeline.name.compareTo(tag.trim()) == 0) {
                                    canBeAdded = false;
                                }
                            }
                        }
                    }
                    if (!canBeAdded) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> Toasty.warning(HashTagActivity.this, getString(R.string.tags_already_stored), Toasty.LENGTH_SHORT).show();
                        mainHandler.post(myRunnable);
                        return;
                    }
                    PinnedTimeline pinnedTimeline = new PinnedTimeline();
                    pinnedTimeline.type = Timeline.TimeLineEnum.TAG;
                    pinnedTimeline.position = pinned.pinnedTimelines.size();
                    pinnedTimeline.displayed = true;
                    TagTimeline tagTimeline = new TagTimeline();
                    tagTimeline.name = tag.trim();
                    tagTimeline.isNSFW = false;
                    tagTimeline.isART = false;
                    pinnedTimeline.tagTimeline = tagTimeline;
                    pinned.pinnedTimelines.add(pinnedTimeline);
                    if (update) {
                        new Pinned(HashTagActivity.this).updatePinned(pinned);
                    } else {
                        new Pinned(HashTagActivity.this).insertPinned(pinned);
                    }
                    Bundle b = new Bundle();
                    b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
                    Intent intentBD = new Intent(Helper.BROADCAST_DATA);
                    intentBD.putExtras(b);
                    LocalBroadcastManager.getInstance(HashTagActivity.this).sendBroadcast(intentBD);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reorder, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
