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
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentProfileTimeline;

public class FedilabProfileTLPageAdapter extends FragmentStatePagerAdapter {
    private final Account account;
    private final boolean checkRemotely;
    private Fragment mCurrentFragment;

    public FedilabProfileTLPageAdapter(FragmentManager fm, Account account, boolean remotely) {
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
        Bundle bundle;
        switch (position) {
            case 0 -> {
                FragmentProfileTimeline fragmentProfileTimeline = new FragmentProfileTimeline();
                bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_ACCOUNT, account);
                bundle.putSerializable(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                fragmentProfileTimeline.setArguments(bundle);
                return fragmentProfileTimeline;
            }
            case 1, 2 -> {
                FragmentMastodonAccount fragmentMastodonAccount = new FragmentMastodonAccount();
                bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_ACCOUNT, account);
                bundle.putSerializable(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + position);
                bundle.putSerializable(Helper.ARG_FOLLOW_TYPE, position == 1 ? follow_type.FOLLOWING : follow_type.FOLLOWERS);
                fragmentMastodonAccount.setArguments(bundle);
                return fragmentMastodonAccount;
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

    public enum follow_type {
        FOLLOWING,
        FOLLOWERS
    }
}
