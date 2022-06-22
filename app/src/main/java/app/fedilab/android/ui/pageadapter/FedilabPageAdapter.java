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

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.client.entities.app.BottomMenu;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.PinnedTimelineHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.ui.fragment.timeline.FragmentNotificationContainer;

public class FedilabPageAdapter extends FragmentStateAdapter {

    public static final int BOTTOM_TIMELINE_COUNT = 5; //home, local, public, notification, DM
    private final Pinned pinned;
    private final BottomMenu bottomMenu;
    private final int toRemove;

    public FedilabPageAdapter(BaseMainActivity activity, FragmentActivity fa, Pinned pinned, BottomMenu bottomMenu) {
        super(fa);
        this.pinned = pinned;
        this.bottomMenu = bottomMenu;
        toRemove = PinnedTimelineHelper.itemToRemoveInBottomMenu(activity);
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        FragmentMastodonTimeline fragment = new FragmentMastodonTimeline();
        Bundle bundle = new Bundle();
        //Position 3 is for notifications
        if (position < (BOTTOM_TIMELINE_COUNT - toRemove)) {
            if (bottomMenu != null) {
                BottomMenu.ItemMenuType type = BottomMenu.getType(bottomMenu, position);
                if (type == null) {
                    return fragment;
                }
                if (type == BottomMenu.ItemMenuType.NOTIFICATION) {
                    return new FragmentNotificationContainer();
                } else if (type == BottomMenu.ItemMenuType.DIRECT) {
                    return new FragmentMastodonConversation();
                }
                if (type == BottomMenu.ItemMenuType.HOME) { //Home timeline
                    bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.HOME);
                } else if (type == BottomMenu.ItemMenuType.LOCAL) { //Local timeline
                    bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.LOCAL);
                } else if (type == BottomMenu.ItemMenuType.PUBLIC) { //Public timeline
                    bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.PUBLIC);
                }
            } else {
                return fragment;
            }

        } else {
            int pinnedPosition = position - (BOTTOM_TIMELINE_COUNT - toRemove); //Real position has an offset.
            PinnedTimeline pinnedTimeline = pinned.pinnedTimelines.get(pinnedPosition);
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, pinnedTimeline.type);
            if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST) {
                bundle.putString(Helper.ARG_LIST_ID, pinnedTimeline.mastodonList.id);
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.TAG) {
                bundle.putSerializable(Helper.ARG_TAG_TIMELINE, pinnedTimeline.tagTimeline);
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.REMOTE) {
                String instance = pinnedTimeline.remoteInstance.host;
                bundle.putString(Helper.ARG_REMOTE_INSTANCE, instance);
            }

        }
        bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getItemCount() {
        if (pinned != null && pinned.pinnedTimelines != null) {
            return pinned.pinnedTimelines.size() + BOTTOM_TIMELINE_COUNT - toRemove;
        } else {
            return BOTTOM_TIMELINE_COUNT - toRemove;
        }
    }

}
