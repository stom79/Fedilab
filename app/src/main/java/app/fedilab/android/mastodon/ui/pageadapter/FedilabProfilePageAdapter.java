package app.fedilab.android.mastodon.ui.pageadapter;
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
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.media.FragmentMediaProfile;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;

public class FedilabProfilePageAdapter extends FragmentStatePagerAdapter {
    private final Account account;
    private final boolean checkRemotely;
    private Fragment mCurrentFragment;

    public FedilabProfilePageAdapter(FragmentManager fm, Account account, boolean remotely) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.account = account;
        this.checkRemotely = remotely;
    }

    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (getCurrentFragment() != object) {
            mCurrentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + position);
        FragmentMastodonTimeline fragmentProfileTimeline;
        switch (position) {
            case 0 -> {
                fragmentProfileTimeline = new FragmentMastodonTimeline();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.ACCOUNT_TIMELINE);
                bundle.putSerializable(Helper.ARG_CACHED_ACCOUNT_ID, account.id);
                bundle.putBoolean(Helper.ARG_SHOW_PINNED, true);
                bundle.putBoolean(Helper.ARG_SHOW_REPLIES, false);
                bundle.putBoolean(Helper.ARG_SHOW_REBLOGS, true);
                bundle.putBoolean(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                fragmentProfileTimeline.setArguments(bundle);
                return fragmentProfileTimeline;
            }
            case 1 -> {
                fragmentProfileTimeline = new FragmentMastodonTimeline();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.ACCOUNT_TIMELINE);
                bundle.putSerializable(Helper.ARG_CACHED_ACCOUNT_ID, account.id);
                bundle.putBoolean(Helper.ARG_SHOW_PINNED, false);
                bundle.putBoolean(Helper.ARG_SHOW_REPLIES, true);
                bundle.putBoolean(Helper.ARG_SHOW_REBLOGS, false);
                bundle.putBoolean(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                fragmentProfileTimeline.setArguments(bundle);
                return fragmentProfileTimeline;
            }
            case 2 -> {
                FragmentMediaProfile fragmentMediaProfile = new FragmentMediaProfile();
                bundle.putSerializable(Helper.ARG_CACHED_ACCOUNT_ID, account.id);
                bundle.putBoolean(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                fragmentMediaProfile.setArguments(bundle);
                return fragmentMediaProfile;
            }
            default -> {
                return new FragmentMastodonTimeline();
            }
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}