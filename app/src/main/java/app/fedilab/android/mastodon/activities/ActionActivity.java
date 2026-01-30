package app.fedilab.android.mastodon.activities;
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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.View;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityActionsBinding;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonDomainBlock;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;

public class ActionActivity extends BaseBarActivity {

    private ActivityActionsBinding binding;
    private boolean canGoBack;
    private boolean isMisskey;
    private FragmentMastodonTimeline fragmentMastodonTimeline;
    private FragmentMastodonAccount fragmentMastodonAccount;
    private FragmentMastodonDomainBlock fragmentMastodonDomainBlock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityActionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        canGoBack = false;

        app.fedilab.android.mastodon.client.entities.app.Account.API currentApi = BaseMainActivity.api;
        if (currentApi == null && Helper.getCurrentAccount(ActionActivity.this) != null) {
            currentApi = Helper.getCurrentAccount(ActionActivity.this).api;
        }
        isMisskey = currentApi == app.fedilab.android.mastodon.client.entities.app.Account.API.MISSKEY;

        binding.favourites.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.FAVOURITE_TIMELINE));
        binding.bookmarks.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.BOOKMARK_TIMELINE));
        binding.muted.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.MUTED_TIMELINE));
        binding.blocked.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.BLOCKED_TIMELINE));
        binding.domainBlock.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.BLOCKED_DOMAIN_TIMELINE));
        binding.mutedHome.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.MUTED_TIMELINE_HOME));

        if (isMisskey) {
            binding.domainBlock.setVisibility(View.GONE);
            binding.favourites.setText(R.string.reactions);
        }
    }

    private void displayTimeline(Timeline.TimeLineEnum type) {
        canGoBack = true;
        if (type == Timeline.TimeLineEnum.MUTED_TIMELINE || type == Timeline.TimeLineEnum.BLOCKED_TIMELINE || type == Timeline.TimeLineEnum.MUTED_TIMELINE_HOME) {
            fragmentMastodonAccount = new FragmentMastodonAccount();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
            bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + type.getValue());
            fragmentMastodonAccount.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonAccount);
            fragmentTransaction.commit();
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {

            });

        } else if (type == Timeline.TimeLineEnum.BLOCKED_DOMAIN_TIMELINE) {
            fragmentMastodonDomainBlock = new FragmentMastodonDomainBlock();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonDomainBlock);
            fragmentTransaction.commit();
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {

            });
        } else {
            fragmentMastodonTimeline = new FragmentMastodonTimeline();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
            bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + type.getValue());
            fragmentMastodonTimeline.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonTimeline);
            fragmentTransaction.commit();
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {

            });

        }
        switch (type) {
            case MUTED_TIMELINE -> setTitle(R.string.muted_menu);
            case FAVOURITE_TIMELINE -> setTitle(isMisskey ? R.string.reactions : R.string.favourite);
            case BLOCKED_TIMELINE -> setTitle(R.string.blocked_menu);
            case BOOKMARK_TIMELINE -> setTitle(R.string.bookmarks);
            case BLOCKED_DOMAIN_TIMELINE -> setTitle(R.string.blocked_domains);
            case MUTED_TIMELINE_HOME -> setTitle(R.string.muted_menu_home);
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.buttonContainer, () -> {
                if (fragmentMastodonTimeline != null) {
                    fragmentMastodonTimeline.onDestroyView();
                }
                if (fragmentMastodonAccount != null) {
                    fragmentMastodonAccount.onDestroyView();
                }
                if (fragmentMastodonDomainBlock != null) {
                    fragmentMastodonDomainBlock.onDestroyView();
                }
            });
            setTitle(R.string.interactions);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
