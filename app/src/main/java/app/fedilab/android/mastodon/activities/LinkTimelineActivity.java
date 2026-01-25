package app.fedilab.android.mastodon.activities;
/* Copyright 2026 Thomas Schneider
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

import androidx.appcompat.app.ActionBar;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityLinkTimelineBinding;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonLinkTimeline;


public class LinkTimelineActivity extends BaseActivity {

    private String url;
    private ActivityLinkTimelineBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLinkTimelineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(LinkTimelineActivity.this).getBundle(bundleId, Helper.getCurrentAccount(LinkTimelineActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {
        if (bundle != null) {
            url = bundle.getString(Helper.ARG_URL, null);
        }
        String title = null;
        if (bundle != null) {
            title = bundle.getString(Helper.ARG_TITLE, null);
        }
        if (url == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        binding.title.setText(title != null ? title : url);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Bundle bundleFragment = new Bundle();
        bundleFragment.putString(Helper.ARG_URL, url);
        FragmentMastodonLinkTimeline fragment = new FragmentMastodonLinkTimeline();
        fragment.setArguments(bundleFragment);
        Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_link, fragment, null, null, null);

        binding.openLink.setOnClickListener(v -> Helper.openBrowser(LinkTimelineActivity.this, url));
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
