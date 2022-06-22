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

import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonNotification;

public class FedilabNotificationPageAdapter extends FragmentStateAdapter {

    private final boolean extended;


    public FedilabNotificationPageAdapter(FragmentActivity fa, boolean extended) {
        super(fa);
        this.extended = extended;
    }

    @Override
    public int getItemCount() {
        return extended ? 7 : 2;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle bundle = new Bundle();
        FragmentMastodonNotification fragmentMastodonNotification = new FragmentMastodonNotification();
        if (!extended) {
            switch (position) {
                case 0:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ALL);
                    break;
                case 1:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.MENTIONS);
                    break;
            }
        } else {
            switch (position) {
                case 0:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.ALL);
                    break;
                case 1:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.MENTIONS);
                    break;
                case 2:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.FAVOURITES);
                    break;
                case 3:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.REBLOGS);
                    break;
                case 4:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.POLLS);
                    break;
                case 5:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.TOOTS);
                    break;
                case 6:
                    bundle.putSerializable(Helper.ARG_NOTIFICATION_TYPE, FragmentMastodonNotification.NotificationTypeEnum.FOLLOWS);
                    break;
            }
        }
        fragmentMastodonNotification.setArguments(bundle);
        return fragmentMastodonNotification;
    }


}
