package app.fedilab.android.ui.pageadapter;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.ui.fragment.timeline.FragmentProfileTimeline;

public class FedilabProfileTLPageAdapter extends FragmentStateAdapter {

    private final Account account;

    public FedilabProfileTLPageAdapter(FragmentActivity fa, Account account) {
        super(fa);
        this.account = account;
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {

            case 0:
                FragmentProfileTimeline fragmentProfileTimeline = new FragmentProfileTimeline();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_ACCOUNT, account);
                fragmentProfileTimeline.setArguments(bundle);
                return fragmentProfileTimeline;
            case 1:
            case 2:
                FragmentMastodonAccount fragmentMastodonAccount = new FragmentMastodonAccount();
                bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_ACCOUNT, account);
                bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + position);
                bundle.putSerializable(Helper.ARG_FOLLOW_TYPE, position == 1 ? follow_type.FOLLOWING : follow_type.FOLLOWERS);
                fragmentMastodonAccount.setArguments(bundle);
                return fragmentMastodonAccount;
            default:
                return new FragmentMastodonTimeline();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public enum follow_type {
        FOLLOWING,
        FOLLOWERS
    }
}
