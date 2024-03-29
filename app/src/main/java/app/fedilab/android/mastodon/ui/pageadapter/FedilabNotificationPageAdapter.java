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

import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonNotification;

public class FedilabNotificationPageAdapter extends FragmentStatePagerAdapter {
    private final boolean extended;
    private Fragment mCurrentFragment;

    public FedilabNotificationPageAdapter(FragmentManager fm, boolean extended) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.extended = extended;
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
        FragmentMastodonNotification fragmentMastodonNotification = new FragmentMastodonNotification();
        if (!extended) {
            switch (position) {
                case 0 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ALL);
                case 1 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.MENTIONS);
            }
        } else {
            switch (position) {
                case 0 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ALL);
                case 1 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.MENTIONS);
                case 2 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.FAVOURITES);
                case 3 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.REBLOGS);
                case 4 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.POLLS);
                case 5 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.TOOTS);
                case 6 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.FOLLOWS);
                case 7 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.UPDATES);
                case 8 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ADMIN_SIGNUP);
                case 9 ->
                        bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ADMIN_REPORT);
            }
        }
        fragmentMastodonNotification.setArguments(bundle);
        return fragmentMastodonNotification;
    }

    @Override
    public int getCount() {
        return extended ? 10 : 2;
    }
}