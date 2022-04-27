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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.databinding.ActivityActionsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;

public class ActionActivity extends BaseActivity {

    private ActivityActionsBinding binding;
    private boolean canGoBack;
    private FragmentMastodonTimeline fragmentMastodonTimeline;
    private FragmentMastodonAccount fragmentMastodonAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityActionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        canGoBack = false;
        binding.favourites.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.FAVOURITE_TIMELINE));
        binding.bookmarks.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.BOOKMARK_TIMELINE));
        binding.muted.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.MUTED_TIMELINE));
        binding.blocked.setOnClickListener(v -> displayTimeline(Timeline.TimeLineEnum.BLOCKED_TIMELINE));
    }

    private void displayTimeline(Timeline.TimeLineEnum type) {
        canGoBack = true;
        if (type == Timeline.TimeLineEnum.MUTED_TIMELINE || type == Timeline.TimeLineEnum.BLOCKED_TIMELINE) {

            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
                fragmentMastodonAccount = new FragmentMastodonAccount();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
                fragmentMastodonAccount.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonAccount);
                fragmentTransaction.commit();
            });

        } else {

            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
                fragmentMastodonTimeline = new FragmentMastodonTimeline();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
                fragmentMastodonTimeline.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonTimeline);
                fragmentTransaction.commit();
            });

        }
        switch (type) {
            case MUTED_TIMELINE:
                setTitle(R.string.muted_menu);
                break;
            case FAVOURITE_TIMELINE:
                setTitle(R.string.favourite);
                break;
            case BLOCKED_TIMELINE:
                setTitle(R.string.blocked_menu);
                break;
            case BOOKMARK_TIMELINE:
                setTitle(R.string.bookmarks);
                break;
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
