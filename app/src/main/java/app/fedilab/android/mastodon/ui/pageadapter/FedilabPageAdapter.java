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


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.BottomMenu;
import app.fedilab.android.mastodon.client.entities.app.Pinned;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.PinnedTimelineHelper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentNotificationContainer;

@SuppressWarnings("deprecation")
public class FedilabPageAdapter extends FragmentStatePagerAdapter {

    public static final int BOTTOM_TIMELINE_COUNT = 5; //home, local, public, notification, DM
    private final Pinned pinned;
    private final BottomMenu bottomMenu;
    private final int toRemove;
    private final boolean singleBar;
    private final BaseMainActivity activity;
    private Fragment mCurrentFragment;

    public FedilabPageAdapter(BaseMainActivity activity, FragmentManager fm, Pinned pinned, BottomMenu bottomMenu) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.pinned = pinned;
        this.activity = activity;
        this.bottomMenu = bottomMenu;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (!singleBar) {
            toRemove = PinnedTimelineHelper.itemToRemoveInBottomMenu(activity);
        } else {
            toRemove = BOTTOM_TIMELINE_COUNT;
        }

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

    @Override
    public int getCount() {
        if (pinned != null && pinned.pinnedTimelines != null) {
            return pinned.pinnedTimelines.size() + BOTTOM_TIMELINE_COUNT - toRemove;
        } else {
            return BOTTOM_TIMELINE_COUNT - toRemove;
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        FragmentMastodonTimeline fragment = new FragmentMastodonTimeline();
        fragment.update = activity;
        Bundle bundle = new Bundle();
        bundle.putBoolean(Helper.ARG_INITIALIZE_VIEW, false);
        if (position < (BOTTOM_TIMELINE_COUNT - toRemove) && !singleBar) {
            if (bottomMenu != null) {
                BottomMenu.ItemMenuType type = BottomMenu.getType(bottomMenu, position);
                if (type == null) {
                    return fragment;
                }
                if (type == BottomMenu.ItemMenuType.NOTIFICATION) {
                    FragmentNotificationContainer fragmentNotificationContainer = new FragmentNotificationContainer();
                    fragmentNotificationContainer.setArguments(bundle);
                    return fragmentNotificationContainer;
                } else if (type == BottomMenu.ItemMenuType.DIRECT) {
                    FragmentMastodonConversation fragmentMastodonConversation = new FragmentMastodonConversation();
                    fragmentMastodonConversation.setArguments(bundle);
                    return fragmentMastodonConversation;
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
            if (pinnedTimeline.type == Timeline.TimeLineEnum.NOTIFICATION) {
                FragmentNotificationContainer fragmentNotificationContainer = new FragmentNotificationContainer();
                FragmentNotificationContainer.update = activity;
                fragmentNotificationContainer.setArguments(bundle);
                return fragmentNotificationContainer;
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.DIRECT) {
                FragmentMastodonConversation fragmentMastodonConversation = new FragmentMastodonConversation();
                fragmentMastodonConversation.update = activity;
                fragmentMastodonConversation.setArguments(bundle);
                return fragmentMastodonConversation;
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST) {
                bundle.putString(Helper.ARG_LIST_ID, pinnedTimeline.mastodonList.id);
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.TAG) {
                bundle.putSerializable(Helper.ARG_TAG_TIMELINE, pinnedTimeline.tagTimeline);
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.REMOTE) {
                bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinnedTimeline);
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.BUBBLE) {
                bundle.putSerializable(Helper.ARG_BUBBLE_TIMELINE, pinnedTimeline.bubbleTimeline);
            }

        }
        bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override

    public int getItemPosition(@NonNull Object object) {
        // POSITION_NONE makes it possible to reload the PagerAdapter
        return POSITION_NONE;
    }
}
