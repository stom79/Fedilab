package app.fedilab.android.mastodon.helper;
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


import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentUserID;
import static app.fedilab.android.BaseMainActivity.show_boosts;
import static app.fedilab.android.BaseMainActivity.show_dms;
import static app.fedilab.android.BaseMainActivity.show_my_messages;
import static app.fedilab.android.BaseMainActivity.show_replies;
import static app.fedilab.android.BaseMainActivity.show_self_boosts;
import static app.fedilab.android.BaseMainActivity.show_self_replies;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityMainBinding;
import app.fedilab.android.databinding.DialogBubbleExcludeVisibilityBinding;
import app.fedilab.android.databinding.DialogBubbleReplyVisibilityBinding;
import app.fedilab.android.databinding.PopupFilterBubbleTimelineBinding;
import app.fedilab.android.databinding.PopupFilterInstanceTimelineBinding;
import app.fedilab.android.databinding.PopupFilterNitterTimelineBinding;
import app.fedilab.android.databinding.PopupFilterTagTimelineBinding;
import app.fedilab.android.databinding.PopupFilterTimelineBinding;
import app.fedilab.android.databinding.TabCustomDefaultViewBinding;
import app.fedilab.android.databinding.TabCustomViewBinding;
import app.fedilab.android.mastodon.client.entities.api.MastodonList;
import app.fedilab.android.mastodon.client.entities.app.BottomMenu;
import app.fedilab.android.mastodon.client.entities.app.BubbleTimeline;
import app.fedilab.android.mastodon.client.entities.app.Pinned;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.RemoteInstance;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.TagTimeline;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentNotificationContainer;
import app.fedilab.android.mastodon.ui.pageadapter.FedilabPageAdapter;
import es.dmoral.toasty.Toasty;

public class PinnedTimelineHelper {


    public static void sortPositionAsc(List<PinnedTimeline> pinnedTimelineList) {
        //noinspection ComparatorCombinators
        Collections.sort(pinnedTimelineList, (obj1, obj2) -> Integer.compare(obj1.position, obj2.position));
    }

    public static void sortListPositionAsc(List<MastodonList> mastodonLists) {
        //noinspection ComparatorCombinators
        Collections.sort(mastodonLists, (obj1, obj2) -> Integer.compare(obj1.position, obj2.position));
    }

    public static void sortMenuItem(List<BottomMenu.MenuItem> menuItemList) {
        //noinspection ComparatorCombinators
        Collections.sort(menuItemList, (obj1, obj2) -> Integer.compare(obj1.position, obj2.position));
    }


    public synchronized static void redrawTopBarPinned(BaseMainActivity activity, ActivityMainBinding activityMainBinding, Pinned pinned, BottomMenu bottomMenu, List<MastodonList> mastodonLists) {
        //Values must be initialized if there is no records in db
        if (pinned == null) {
            pinned = new Pinned();
            pinned.user_id = currentUserID;
            pinned.instance = currentInstance;
        }
        if (pinned.pinnedTimelines == null) {
            pinned.pinnedTimelines = new ArrayList<>();
        }
        //Set the slug of first visible fragment
        /*String slugOfFirstFragment = PinnedTimelineHelper.firstTimelineSlug(activity, pinned, bottomMenu);
        Helper.setSlugOfFirstFragment(activity, slugOfFirstFragment, currentUserID, currentInstance);*/

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        boolean timeInList = sharedpreferences.getBoolean(activity.getString(R.string.SET_TIMELINES_IN_A_LIST), false);
        if (timeInList) {
            activityMainBinding.moreTimelines.setVisibility(View.VISIBLE);
        } else {
            activityMainBinding.moreTimelines.setVisibility(View.GONE);
        }

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) activityMainBinding.viewPager.getLayoutParams();
        //Hiding/Showing bottom menu depending of settings
        if (singleBar) {
            activityMainBinding.bottomNavView.setVisibility(View.GONE);
            params.setMargins(0, 0, 0, 0);
        } else {
            TypedValue tv = new TypedValue();
            activityMainBinding.bottomNavView.setVisibility(View.VISIBLE);
            if (activity.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, activity.getResources().getDisplayMetrics());
                params.setMargins(0, 0, 0, actionBarHeight);
            }
        }

        activityMainBinding.viewPager.setLayoutParams(params);
        List<PinnedTimeline> pinnedTimelines = pinned.pinnedTimelines;
        boolean extraFeatures = sharedpreferences.getBoolean(activity.getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, false);

        if (singleBar) {
            boolean createDefaultAtTop = true;
            for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                if (pinnedTimeline.type == Timeline.TimeLineEnum.HOME) {
                    createDefaultAtTop = false;
                    break;
                }
            }
            //Default item in top doesn't exist yet, we have to create them, it should be done once
            if (createDefaultAtTop) {
                //We shift all position
                for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                    pinnedTimeline.position += FedilabPageAdapter.BOTTOM_TIMELINE_COUNT;
                }
                PinnedTimeline pinnedTimelineHome = new PinnedTimeline();
                pinnedTimelineHome.type = Timeline.TimeLineEnum.HOME;
                pinnedTimelineHome.position = 0;
                pinned.pinnedTimelines.add(pinnedTimelineHome);
                PinnedTimeline pinnedTimelineLocal = new PinnedTimeline();
                pinnedTimelineLocal.type = Timeline.TimeLineEnum.LOCAL;
                pinnedTimelineLocal.position = 1;
                pinned.pinnedTimelines.add(pinnedTimelineLocal);
                PinnedTimeline pinnedTimelinePublic = new PinnedTimeline();
                pinnedTimelinePublic.type = Timeline.TimeLineEnum.PUBLIC;
                pinnedTimelinePublic.position = 2;
                pinned.pinnedTimelines.add(pinnedTimelinePublic);
                PinnedTimeline pinnedTimelineNotifications = new PinnedTimeline();
                pinnedTimelineNotifications.type = Timeline.TimeLineEnum.NOTIFICATION;
                pinnedTimelineNotifications.position = 3;
                pinned.pinnedTimelines.add(pinnedTimelineNotifications);
                PinnedTimeline pinnedTimelineConversations = new PinnedTimeline();
                pinnedTimelineConversations.type = Timeline.TimeLineEnum.DIRECT;
                pinnedTimelineConversations.position = 4;
                pinned.pinnedTimelines.add(pinnedTimelineConversations);
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
        }

        //Create other default timelines
        try {
            Pinned pinnedAll = new Pinned(activity).getAllPinned(Helper.getCurrentAccount(activity));
            if (pinnedAll == null) {
                pinnedAll = new Pinned();
                pinnedAll.user_id = currentUserID;
                pinnedAll.instance = currentInstance;
                pinnedAll.pinnedTimelines = new ArrayList<>();
            }
            if (extraFeatures) {
                //Bubble timeline
                boolean createDefaultBubbleAtTop = true;
                for (PinnedTimeline pinnedTimeline : pinnedAll.pinnedTimelines) {
                    if (pinnedTimeline.type == Timeline.TimeLineEnum.BUBBLE) {
                        createDefaultBubbleAtTop = false;
                        break;
                    }
                }
                if (createDefaultBubbleAtTop) {
                    PinnedTimeline pinnedTimelineBubble = new PinnedTimeline();
                    pinnedTimelineBubble.type = Timeline.TimeLineEnum.BUBBLE;
                    pinnedTimelineBubble.position = pinnedAll.pinnedTimelines != null ? pinnedAll.pinnedTimelines.size() : 0;
                    pinned.pinnedTimelines.add(pinnedTimelineBubble);
                    boolean exist = new Pinned(activity).pinnedExist(pinned);
                    if (exist) {
                        new Pinned(activity).updatePinned(pinned);
                    } else {
                        new Pinned(activity).insertPinned(pinned);
                    }
                }
            }
            //Trend timeline
            boolean createDefaultTrendAtTop = true;
            for (PinnedTimeline pinnedTimeline : pinnedAll.pinnedTimelines) {
                if (pinnedTimeline.type == Timeline.TimeLineEnum.TREND_MESSAGE) {
                    createDefaultTrendAtTop = false;
                    break;
                }
            }
            if (createDefaultTrendAtTop) {
                PinnedTimeline pinnedTimelineBubble = new PinnedTimeline();
                pinnedTimelineBubble.type = Timeline.TimeLineEnum.TREND_MESSAGE;
                pinnedTimelineBubble.position = pinnedAll.pinnedTimelines != null ? pinnedAll.pinnedTimelines.size() : 0;
                pinned.pinnedTimelines.add(pinnedTimelineBubble);
                boolean exist = new Pinned(activity).pinnedExist(pinned);
                if (exist) {
                    new Pinned(activity).updatePinned(pinned);
                } else {
                    new Pinned(activity).insertPinned(pinned);
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }




        sortPositionAsc(pinnedTimelines);
        //Check if changes occurred, if mastodonLists is null it does need, because it is the first call to draw pinned
        boolean needRedraw = mastodonLists == null;
        //Lists have been fetched from remote account
        if (mastodonLists != null) { //Currently, needRedraw is set to false
            List<PinnedTimeline> pinnedToRemove = new ArrayList<>();
            for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST) {
                    boolean present = false;
                    for (MastodonList mastodonList : mastodonLists) {
                        if (mastodonList.id.compareTo(pinnedTimeline.mastodonList.id) == 0) {
                            present = true;
                            if (!mastodonList.title.equalsIgnoreCase(pinnedTimeline.mastodonList.title)) {
                                pinnedTimeline.mastodonList.title = mastodonList.title;
                                Pinned finalPinned1 = pinned;
                                new Thread(() -> {
                                    try {
                                        new Pinned(activity).updatePinned(finalPinned1);
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                            break;
                        }
                    }
                    //Needs to be removed
                    if (!present) {
                        pinnedToRemove.add(pinnedTimeline);
                        needRedraw = true; //Something changed, redraw must be done
                    }
                }
            }
            if (pinnedToRemove.size() > 0) {
                pinned.pinnedTimelines.removeAll(pinnedToRemove);
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }

            for (MastodonList mastodonList : mastodonLists) {
                boolean present = false;
                try {
                    Pinned pinnedAll = new Pinned(activity).getAllPinned(Helper.getCurrentAccount(activity));
                    if (pinnedAll == null) {
                        pinnedAll = pinned;
                    }
                    for (PinnedTimeline pinnedTimeline : pinnedAll.pinnedTimelines) {
                        if (pinnedTimeline.mastodonList != null && mastodonList.id.compareTo(pinnedTimeline.mastodonList.id) == 0) {
                            present = true;
                            break;
                        }
                    }
                    //Needs to be added
                    if (!present) {
                        needRedraw = true; //Something changed, redraw must be done
                        PinnedTimeline pinnedTimeline = new PinnedTimeline();
                        pinnedTimeline.type = Timeline.TimeLineEnum.LIST;
                        pinnedTimeline.position = pinnedAll.pinnedTimelines.size();
                        pinnedTimeline.mastodonList = mastodonList;
                        pinnedAll.pinnedTimelines.add(pinnedTimeline);

                        try {
                            boolean exist = new Pinned(activity).pinnedExist(pinnedAll);
                            if (exist) {
                                new Pinned(activity).updatePinned(pinnedAll);
                            } else {
                                new Pinned(activity).insertPinned(pinnedAll);
                            }
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (DBException e) {
                    e.printStackTrace();
                }

            }
        }
        if (!needRedraw) { //if there were no changes with list, no need to update tabs
            return;
        }
        //Pinned tab position will start after BOTTOM_TIMELINE_COUNT (ie 5)
        activityMainBinding.tabLayout.removeAllTabs();
        if(activityMainBinding.viewPager.getAdapter() != null) {
           activityMainBinding.viewPager.getAdapter().notifyDataSetChanged();
        }
        int toRemove = FedilabPageAdapter.BOTTOM_TIMELINE_COUNT;
        if (!singleBar) {
            //Small hack to hide first tabs (they represent the item of the bottom menu)
            toRemove = itemToRemoveInBottomMenu(activity);
            for (int i = 0; i < (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - toRemove); i++) {
                activityMainBinding.tabLayout.addTab(activityMainBinding.tabLayout.newTab(), false);
                if(activityMainBinding.viewPager.getAdapter() != null) {
                    activityMainBinding.viewPager.getAdapter().notifyDataSetChanged();
                }
                ((ViewGroup) activityMainBinding.tabLayout.getChildAt(0)).getChildAt(i).setVisibility(View.GONE);
            }
        }
        List<PinnedTimeline> pinnedTimelineVisibleList = new ArrayList<>();
        List<PinnedTimeline> pinnedToRemove = new ArrayList<>();
        for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
            //Default timelines are not added if we are not in the single bar mode
            String ident = null;
            if (!singleBar) {
                switch (pinnedTimeline.type) {
                    case HOME:
                    case LOCAL:
                    case PUBLIC:
                    case NOTIFICATION:
                    case DIRECT:
                        pinnedToRemove.add(pinnedTimeline);
                        continue;
                }
            }
            if (pinnedTimeline.displayed) {
                TabLayout.Tab tab = activityMainBinding.tabLayout.newTab();
                String name = "";
                switch (pinnedTimeline.type) {
                    case LIST:
                        name = pinnedTimeline.mastodonList.title;
                        ident = "@l@" + pinnedTimeline.mastodonList.id;
                        break;
                    case TAG:
                        name = pinnedTimeline.tagTimeline.displayName != null && !pinnedTimeline.tagTimeline.displayName.isEmpty() ? pinnedTimeline.tagTimeline.displayName : pinnedTimeline.tagTimeline.name.replaceAll("#", "");
                        ident = "@T@" + name;
                        break;
                    case REMOTE:
                        name = pinnedTimeline.remoteInstance.host;
                        if (pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER || pinnedTimeline.remoteInstance.type == RemoteInstance.InstanceType.NITTER_TAG) {
                            String remoteInstance = sharedpreferences.getString(activity.getString(R.string.SET_NITTER_HOST), activity.getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
                            //Custom name for Nitter instances
                            if (pinnedTimeline.remoteInstance.displayName != null && !pinnedTimeline.remoteInstance.displayName.trim().isEmpty()) {
                                name = pinnedTimeline.remoteInstance.displayName;
                            }
                            ident = "@R@" + remoteInstance;
                        } else {
                            ident = "@R@" + pinnedTimeline.remoteInstance.host;
                        }
                        break;
                }

                if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST || pinnedTimeline.type == Timeline.TimeLineEnum.TAG || pinnedTimeline.type == Timeline.TimeLineEnum.REMOTE) {
                    TabCustomViewBinding tabCustomViewBinding = TabCustomViewBinding.inflate(activity.getLayoutInflater());
                    tabCustomViewBinding.title.setText(name);
                    switch (pinnedTimeline.type) {
                        case LIST:
                            tabCustomViewBinding.icon.setImageResource(R.drawable.ic_tl_list);
                            tab.setContentDescription(activity.getString(R.string.cd_list_timeline, name));
                            break;
                        case TAG:
                            tabCustomViewBinding.icon.setImageResource(R.drawable.ic_tl_tag);
                            tab.setContentDescription("#" + name);
                            break;
                        case REMOTE:
                            switch (pinnedTimeline.remoteInstance.type) {
                                case PIXELFED:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.pixelfed);
                                    break;
                                case MASTODON:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.mastodon_icon_item);
                                    break;
                                case MASTODON_TRENDING:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.ic_baseline_trending_up_24);
                                    break;
                                case LEMMY:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.lemmy);
                                    break;
                                case MISSKEY:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.misskey);
                                    break;
                                case NITTER_TAG:
                                case NITTER:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.nitter);
                                    break;
                                case GNU:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.ic_gnu_social);
                                    break;
                                case PEERTUBE:
                                    tabCustomViewBinding.icon.setImageResource(R.drawable.peertube_icon);
                                    break;
                            }
                            tab.setContentDescription(name);
                            break;
                    }
                    tab.setCustomView(tabCustomViewBinding.getRoot());
                } else {
                    TabCustomDefaultViewBinding tabCustomDefaultViewBinding = TabCustomDefaultViewBinding.inflate(activity.getLayoutInflater());
                    switch (pinnedTimeline.type) {
                        case HOME:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_home_24);
                            tab.setContentDescription(R.string.tab_home_timeline);
                            break;
                        case LOCAL:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_supervisor_account_24);
                            tab.setContentDescription(R.string.tab_local_timeline);
                            break;
                        case PUBLIC:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_public_24);
                            tab.setContentDescription(R.string.tab_public_timeline);
                            break;
                        case NOTIFICATION:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_notifications_24);
                            tab.setContentDescription(R.string.notifications);
                            break;
                        case DIRECT:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_mail_24);
                            tab.setContentDescription(R.string.tab_private_mentions);
                            break;
                        case BUBBLE:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.ic_baseline_bubble_chart_24);
                            tab.setContentDescription(R.string.tab_bubble_timeline);
                            break;
                        case TREND_MESSAGE:
                            tabCustomDefaultViewBinding.icon.setImageResource(R.drawable.baseline_moving_24);
                            tab.setContentDescription(R.string.trending);
                            break;
                    }
                    tab.setCustomView(tabCustomDefaultViewBinding.getRoot());
                }
                //We be used to fetch position of tabs
                String slug = pinnedTimeline.type.getValue() + (ident != null ? "|" + ident : "");
                tab.setTag(slug);
                activityMainBinding.tabLayout.addTab(tab, false);
                if(activityMainBinding.viewPager.getAdapter() != null){
                    activityMainBinding.viewPager.getAdapter().notifyDataSetChanged();
                }
                pinnedTimelineVisibleList.add(pinnedTimeline);
            }
        }
        pinned.pinnedTimelines.removeAll(pinnedToRemove);

        Pinned finalPinned = pinned;
        int finalToRemove1 = toRemove;
        activityMainBinding.moreTimelines.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, v);
            try {
                @SuppressLint("PrivateApi")
                Method method = popup.getMenu().getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
                method.setAccessible(true);
                method.invoke(popup.getMenu(), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int i = 0;
            int j = 0;
            for (PinnedTimeline pinnedTimeline : finalPinned.pinnedTimelines) {
                MenuItem item = null;
                switch (pinnedTimeline.type) {
                    case LIST:
                        item = popup.getMenu().add(0, 0, Menu.NONE, pinnedTimeline.mastodonList.title);
                        item.setIcon(R.drawable.ic_tl_list);
                        break;
                    case TAG:
                        String name = (pinnedTimeline.tagTimeline.displayName != null && pinnedTimeline.tagTimeline.displayName.length() > 0) ? pinnedTimeline.tagTimeline.displayName : pinnedTimeline.tagTimeline.name;
                        item = popup.getMenu().add(0, 0, Menu.NONE, name);
                        item.setIcon(R.drawable.ic_tl_tag);
                        break;
                    case REMOTE:
                        item = popup.getMenu().add(0, 0, Menu.NONE, pinnedTimeline.remoteInstance.host);
                        switch (pinnedTimeline.remoteInstance.type) {
                            case MASTODON:
                                item.setIcon(R.drawable.mastodon_icon_item);
                                break;
                            case MASTODON_TRENDING:
                                item.setIcon(R.drawable.ic_baseline_trending_up_24);
                                break;
                            case PEERTUBE:
                                item.setIcon(R.drawable.peertube_icon);
                                break;
                            case GNU:
                                item.setIcon(R.drawable.ic_gnu_social);
                                break;
                            case MISSKEY:
                                item.setIcon(R.drawable.misskey);
                                break;
                            case LEMMY:
                                item.setIcon(R.drawable.lemmy);
                                break;
                            case PIXELFED:
                                item.setIcon(R.drawable.pixelfed);
                                break;
                            case NITTER_TAG:
                            case NITTER:
                                item.setIcon(R.drawable.nitter);
                                if (pinnedTimeline.remoteInstance.displayName != null && !pinnedTimeline.remoteInstance.displayName.trim().isEmpty()) {
                                    item.setTitle(pinnedTimeline.remoteInstance.displayName);
                                } else {
                                    item.setTitle(pinnedTimeline.remoteInstance.host);
                                }
                                break;
                        }
                        break;
                }
                if (item != null) {
                    int finalI;
                    if (singleBar) {
                        finalI = i;
                    } else {
                        finalI = FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - finalToRemove1 + j;
                    }
                    item.setOnMenuItemClickListener(item1 -> {
                        if (finalI < activityMainBinding.tabLayout.getTabCount() && activityMainBinding.tabLayout.getTabAt(finalI) != null) {
                            TabLayout.Tab tab = activityMainBinding.tabLayout.getTabAt(finalI);
                            if (tab != null) {
                                tab.select();
                            }
                        }
                        return false;
                    });
                    j++;
                }
                i++;
            }
            popup.show();
        });


        LinearLayout tabStrip = (LinearLayout) activityMainBinding.tabLayout.getChildAt(0);
        int finalToRemove = toRemove;
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            // Set LongClick listener to each Tab
            int finalI = i;
            tabStrip.getChildAt(i).setOnLongClickListener(v -> {

                int position = finalI - (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - finalToRemove);
                switch (pinnedTimelineVisibleList.get(position).type) {
                    case LIST:

                        break;
                    case TAG:
                        tagClick(activity, finalPinned, v, activityMainBinding, finalI, activityMainBinding.tabLayout.getTabAt(finalI).getTag().toString());
                        break;
                    case BUBBLE:
                        bubbleClick(activity, finalPinned, v, activityMainBinding, finalI, activityMainBinding.tabLayout.getTabAt(finalI).getTag().toString());
                        break;
                    case REMOTE:
                        if (pinnedTimelineVisibleList.get(position).remoteInstance.type != RemoteInstance.InstanceType.NITTER && pinnedTimelineVisibleList.get(position).remoteInstance.type != RemoteInstance.InstanceType.NITTER_TAG) {
                            instanceClick(activity, finalPinned, v, activityMainBinding, finalI, activityMainBinding.tabLayout.getTabAt(finalI).getTag().toString());
                        } else {
                            nitterClick(activity, finalPinned, v, activityMainBinding, finalI, activityMainBinding.tabLayout.getTabAt(finalI).getTag().toString());
                        }
                        break;
                    case HOME:
                    case LOCAL:
                    case TREND_MESSAGE:
                    case PUBLIC:
                        defaultClick(activity, pinnedTimelineVisibleList.get(position).type, v, activityMainBinding, finalI);
                        break;
                }
                return true;
            });
        }

        activityMainBinding.viewPager.setAdapter(null);
        activityMainBinding.viewPager.clearOnPageChangeListeners();
        activityMainBinding.tabLayout.clearOnTabSelectedListeners();
        FedilabPageAdapter fedilabPageAdapter = new FedilabPageAdapter(activity, activity.getSupportFragmentManager(), pinned, bottomMenu);
        activityMainBinding.viewPager.setAdapter(fedilabPageAdapter);

        activityMainBinding.viewPager.setOffscreenPageLimit(tabStrip.getChildCount());
        activityMainBinding.viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(activityMainBinding.tabLayout));
        if (!singleBar) {
            activityMainBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    if (position < FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - finalToRemove) {
                        activityMainBinding.bottomNavView.getMenu().getItem(position).setChecked(true);
                    } else {
                        activityMainBinding.bottomNavView.getMenu().setGroupCheckable(0, true, false);
                        for (int i = 0; i < activityMainBinding.bottomNavView.getMenu().size(); i++) {
                            activityMainBinding.bottomNavView.getMenu().getItem(i).setChecked(false);
                        }
                        activityMainBinding.bottomNavView.getMenu().setGroupCheckable(0, true, true);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        }


        activityMainBinding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                activityMainBinding.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (!singleBar && tab.getTag() != null) {
                    if (tab.getTag().equals(Timeline.TimeLineEnum.HOME.getValue())) {
                        activityMainBinding.bottomNavView.removeBadge(R.id.nav_home);
                    } else if (tab.getTag().equals(Timeline.TimeLineEnum.LOCAL.getValue())) {
                        activityMainBinding.bottomNavView.removeBadge(R.id.nav_local);
                    } else if (tab.getTag().equals(Timeline.TimeLineEnum.PUBLIC.getValue())) {
                        activityMainBinding.bottomNavView.removeBadge(R.id.nav_public);
                    } else if (tab.getTag().equals(Timeline.TimeLineEnum.NOTIFICATION.getValue())) {
                        activityMainBinding.bottomNavView.removeBadge(R.id.nav_notifications);
                    } else if (tab.getTag().equals(Timeline.TimeLineEnum.CONVERSATION.getValue())) {
                        activityMainBinding.bottomNavView.removeBadge(R.id.nav_privates);
                    }

                }
                Fragment fragment = fedilabPageAdapter.getCurrentFragment();
                View view = tab.getCustomView();
                if (view != null) {
                    TextView counter = view.findViewById(R.id.tab_counter);
                    if (counter != null) {
                        counter.setVisibility(View.GONE);
                        counter.setText("0");
                    }
                }
                if (fragment instanceof FragmentMastodonTimeline) {
                    ((FragmentMastodonTimeline) fragment).scrollToTop();
                } else if (fragment instanceof FragmentMastodonConversation) {
                    ((FragmentMastodonConversation) fragment).scrollToTop();
                } else if (fragment instanceof FragmentNotificationContainer) {
                    ((FragmentNotificationContainer) fragment).scrollToTop();
                }
            }
        });

    }

    /**
     * Manage long clicks on default timelines
     *
     * @param activity - BaseMainActivity activity
     * @param view     - View
     * @param position - int position of the tab
     */
    public static void defaultClick(BaseMainActivity activity, Timeline.TimeLineEnum timeLineEnum, View view, ActivityMainBinding activityMainBinding, int position) {
        boolean showExtendedFilter = timeLineEnum == Timeline.TimeLineEnum.HOME;

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String show_filtered = null;
        if (timeLineEnum == Timeline.TimeLineEnum.HOME) {
            show_filtered = sharedpreferences.getString(activity.getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, null);
        } else if (timeLineEnum == Timeline.TimeLineEnum.LOCAL) {
            show_filtered = sharedpreferences.getString(activity.getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, null);
        } else if (timeLineEnum == Timeline.TimeLineEnum.PUBLIC) {
            show_filtered = sharedpreferences.getString(activity.getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, null);
        }

        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
        PopupFilterTimelineBinding dialogView = PopupFilterTimelineBinding.inflate(activity.getLayoutInflater());
        dialogBuilder.setView(dialogView.getRoot());

        // Set initial checkbox states
        dialogView.showBoosts.setChecked(show_boosts);
        dialogView.showReplies.setChecked(show_replies);
        dialogView.showSelfBoosts.setChecked(show_self_boosts);
        dialogView.showSelfReplies.setChecked(show_self_replies);
        dialogView.showMyMessages.setChecked(show_my_messages);
        dialogView.showDms.setChecked(show_dms);

        // Hide extended filters for local/public timelines
        if (!showExtendedFilter) {
            dialogView.showBoosts.setVisibility(View.GONE);
            dialogView.showReplies.setVisibility(View.GONE);
            dialogView.showSelfBoosts.setVisibility(View.GONE);
            dialogView.showSelfReplies.setVisibility(View.GONE);
            dialogView.showMyMessages.setVisibility(View.GONE);
            dialogView.showDms.setVisibility(View.GONE);
        }

        // Update filter button text if there's an active filter
        if (show_filtered != null && show_filtered.length() > 0) {
            dialogView.filterRegex.setText(show_filtered);
        }

        final SharedPreferences.Editor editor = sharedpreferences.edit();

        // Checkbox listeners
        dialogView.showBoosts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_boosts = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_BOOSTS) + currentUserID + currentInstance, show_boosts);
            editor.apply();
        });

        dialogView.showReplies.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_replies = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_REPLIES) + currentUserID + currentInstance, show_replies);
            editor.apply();
        });

        dialogView.showSelfBoosts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_self_boosts = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_SELF_BOOSTS) + currentUserID + currentInstance, show_self_boosts);
            editor.apply();
        });

        dialogView.showSelfReplies.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_self_replies = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_SELF_REPLIES) + currentUserID + currentInstance, show_self_replies);
            editor.apply();
        });

        dialogView.showMyMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_my_messages = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_MY_MESSAGES) + currentUserID + currentInstance, show_my_messages);
            editor.apply();
        });

        dialogView.showDms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_dms = isChecked;
            editor.putBoolean(activity.getString(R.string.SET_SHOW_DMS) + currentUserID + currentInstance, show_dms);
            editor.apply();
        });

        String finalShow_filtered = show_filtered;
        dialogView.filterRegex.setOnClickListener(v -> {
            AlertDialog.Builder regexDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View regexDialogView = activity.getLayoutInflater().inflate(R.layout.popup_filter_regex, new LinearLayout(activity), false);
            regexDialogBuilder.setView(regexDialogView);
            final EditText editText = regexDialogView.findViewById(R.id.filter_regex);
            Toast alertRegex = Toasty.warning(activity, activity.getString(R.string.alert_regex), Toast.LENGTH_LONG);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        Pattern.compile("(" + s.toString() + ")", Pattern.CASE_INSENSITIVE);
                    } catch (Exception e) {
                        if (!alertRegex.getView().isShown()) {
                            alertRegex.show();
                        }
                    }
                }
            });
            if (finalShow_filtered != null) {
                editText.setText(finalShow_filtered);
                editText.setSelection(editText.getText().toString().length());
            }
            regexDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                dialogView.filterRegex.setText(editText.getText().toString().trim());
                if (position == 0) {
                    editor.putString(activity.getString(R.string.SET_FILTER_REGEX_HOME) + currentUserID + currentInstance, editText.getText().toString().trim());
                    BaseMainActivity.regex_home = editText.getText().toString().trim();
                } else if (position == 1) {
                    editor.putString(activity.getString(R.string.SET_FILTER_REGEX_LOCAL) + currentUserID + currentInstance, editText.getText().toString().trim());
                    BaseMainActivity.regex_local = editText.getText().toString().trim();
                } else if (position == 2) {
                    editor.putString(activity.getString(R.string.SET_FILTER_REGEX_PUBLIC) + currentUserID + currentInstance, editText.getText().toString().trim());
                    BaseMainActivity.regex_public = editText.getText().toString().trim();
                }
                editor.apply();
            });
            AlertDialog regexAlertDialog = regexDialogBuilder.create();
            regexAlertDialog.show();
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        dialogBuilder.setOnDismissListener(dialog -> {
            if (activityMainBinding.viewPager.getAdapter() != null && activityMainBinding.tabLayout.getSelectedTabPosition() != -1) {
                Fragment fragment = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                if (fragment instanceof FragmentMastodonTimeline fragmentMastodonTimeline && fragment.isVisible()) {
                    fragmentMastodonTimeline.refreshAllAdapters();
                }
            }
        });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }


    public static int itemToRemoveInBottomMenu(BaseMainActivity activity) {
        //Small hack to hide first tabs (they represent the item of the bottom menu)
        BottomMenu bottomMenuDb;
        int toRemove = 0;
        try {
            //If some menu items have been hidden we should not create tab for them
            bottomMenuDb = new BottomMenu(activity).getAllBottomMenu(Helper.getCurrentAccount(activity));
            if (bottomMenuDb != null) {
                List<BottomMenu.MenuItem> menuItemList = bottomMenuDb.bottom_menu;
                if (menuItemList != null) {
                    for (BottomMenu.MenuItem menuItem : menuItemList) {
                        if (!menuItem.visible) {
                            toRemove++;
                        }
                    }
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        return toRemove;
    }


    /**
     * Manage long clicks on Tag timelines
     *
     * @param activity - BaseMainActivity activity
     * @param pinned   - {@link Pinned}
     * @param view     - View
     * @param position - int position of the tab
     */
    public static void tagClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position, String slug) {
        int toRemove = itemToRemoveInBottomMenu(activity);
        int offSetPosition = position - (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - toRemove);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (singleBar) {
            offSetPosition = position;
        }
        String tag;
        TagTimeline tagTimeline = pinned.pinnedTimelines.get(offSetPosition).tagTimeline;
        if (tagTimeline == null)
            return;
        if (tagTimeline.displayName != null)
            tag = tagTimeline.displayName;
        else
            tag = tagTimeline.name;

        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
        PopupFilterTagTimelineBinding dialogView = PopupFilterTagTimelineBinding.inflate(activity.getLayoutInflater());
        dialogBuilder.setView(dialogView.getRoot());

        final boolean[] changes = {false};
        dialogView.actionShowMediaOnly.setChecked(tagTimeline.isART);
        dialogView.actionShowNsfw.setChecked(tagTimeline.isNSFW);

        int finalOffSetPosition = offSetPosition;
        String finalTag = tag;

        dialogView.actionShowMediaOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changes[0] = true;
            tagTimeline.isART = isChecked;
            pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });

        dialogView.actionShowNsfw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changes[0] = true;
            tagTimeline.isNSFW = isChecked;
            pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });

        dialogView.actionDisplayname.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder nameDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View nameDialogView = activity.getLayoutInflater().inflate(R.layout.tags_name, new LinearLayout(activity), false);
            nameDialogBuilder.setView(nameDialogView);
            final EditText editTextName = nameDialogView.findViewById(R.id.column_name);
            if (tagTimeline.displayName != null) {
                editTextName.setText(tagTimeline.displayName);
                editTextName.setSelection(editTextName.getText().toString().length());
            }
            nameDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String values = editTextName.getText().toString();
                if (values.trim().length() == 0)
                    values = finalTag;
                tagTimeline.displayName = values;
                View titleView = view.findViewById(R.id.title);
                if (titleView instanceof AppCompatTextView) {
                    ((AppCompatTextView) titleView).setText(tagTimeline.displayName);
                }
                pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            nameDialogBuilder.create().show();
        });

        dialogView.actionAny.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder anyDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View anyDialogView = activity.getLayoutInflater().inflate(R.layout.tags_any, new LinearLayout(activity), false);
            anyDialogBuilder.setView(anyDialogView);
            final EditText editText = anyDialogView.findViewById(R.id.filter_any);
            if (tagTimeline.any != null) {
                StringBuilder valuesTag = new StringBuilder();
                for (String val : tagTimeline.any)
                    valuesTag.append(val).append(" ");
                editText.setText(valuesTag.toString());
                editText.setSelection(editText.getText().toString().length());
            }
            anyDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String[] values = editText.getText().toString().trim().split("\\s+");
                tagTimeline.any = new ArrayList<>(Arrays.asList(values));
                pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            anyDialogBuilder.create().show();
        });

        dialogView.actionAll.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder allDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View allDialogView = activity.getLayoutInflater().inflate(R.layout.tags_all, new LinearLayout(activity), false);
            allDialogBuilder.setView(allDialogView);
            final EditText editTextAll = allDialogView.findViewById(R.id.filter_all);
            if (tagTimeline.all != null) {
                StringBuilder valuesTag = new StringBuilder();
                for (String val : tagTimeline.all)
                    valuesTag.append(val).append(" ");
                editTextAll.setText(valuesTag.toString());
                editTextAll.setSelection(editTextAll.getText().toString().length());
            }
            allDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String[] values = editTextAll.getText().toString().trim().split("\\s+");
                tagTimeline.all = new ArrayList<>(Arrays.asList(values));
                pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            allDialogBuilder.create().show();
        });

        dialogView.actionNone.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder noneDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View noneDialogView = activity.getLayoutInflater().inflate(R.layout.tags_all, new LinearLayout(activity), false);
            noneDialogBuilder.setView(noneDialogView);
            final EditText editTextNone = noneDialogView.findViewById(R.id.filter_all);
            if (tagTimeline.none != null) {
                StringBuilder valuesTag = new StringBuilder();
                for (String val : tagTimeline.none)
                    valuesTag.append(val).append(" ");
                editTextNone.setText(valuesTag.toString());
                editTextNone.setSelection(editTextNone.getText().toString().length());
            }
            noneDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String[] values = editTextNone.getText().toString().trim().split("\\s+");
                tagTimeline.none = new ArrayList<>(Arrays.asList(values));
                pinned.pinnedTimelines.get(finalOffSetPosition).tagTimeline = tagTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            noneDialogBuilder.create().show();
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        dialogBuilder.setOnDismissListener(dialog -> {
            if (changes[0]) {
                if (activityMainBinding.viewPager.getAdapter() != null) {
                    try {
                        new StatusCache(activity).deleteForSlug(slug);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(activity.getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, null);
                    editor.commit();
                    if (activityMainBinding.tabLayout.getSelectedTabPosition() != -1) {
                        Fragment fragmentMastodonTimeline = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                        if (fragmentMastodonTimeline instanceof FragmentMastodonTimeline && fragmentMastodonTimeline.isVisible()) {
                            FragmentTransaction fragTransaction = activity.getSupportFragmentManager().beginTransaction();
                            fragTransaction.detach(fragmentMastodonTimeline).commit();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TAG);
                            bundle.putSerializable(Helper.ARG_TAG_TIMELINE, tagTimeline);
                            bundle.putSerializable(Helper.ARG_INITIALIZE_VIEW, false);
                            fragmentMastodonTimeline.setArguments(bundle);
                            FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                            fragTransaction2.attach(fragmentMastodonTimeline);
                            fragTransaction2.commit();
                            ((FragmentMastodonTimeline) fragmentMastodonTimeline).recreate();
                        }
                    }
                }
            }
        });
        dialogBuilder.create().show();
    }


    /**
     * Manage long clicks on Bubble timelines
     *
     * @param activity - BaseMainActivity activity
     * @param pinned   - {@link Pinned}
     * @param view     - View
     * @param position - int position of the tab
     */
    public static void bubbleClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position, String slug) {
        int toRemove = itemToRemoveInBottomMenu(activity);
        int offSetPosition = position - (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - toRemove);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (singleBar) {
            offSetPosition = position;
        }

        if (pinned.pinnedTimelines.get(offSetPosition).bubbleTimeline == null) {
            pinned.pinnedTimelines.get(offSetPosition).bubbleTimeline = new BubbleTimeline();
        }
        BubbleTimeline bubbleTimeline = pinned.pinnedTimelines.get(offSetPosition).bubbleTimeline;

        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
        PopupFilterBubbleTimelineBinding dialogView = PopupFilterBubbleTimelineBinding.inflate(activity.getLayoutInflater());
        dialogBuilder.setView(dialogView.getRoot());

        final boolean[] changes = {false};
        dialogView.actionShowMediaOnly.setChecked(bubbleTimeline.only_media);
        dialogView.actionRemote.setChecked(bubbleTimeline.remote);

        int finalOffSetPosition = offSetPosition;

        dialogView.actionShowMediaOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changes[0] = true;
            bubbleTimeline.only_media = isChecked;
            pinned.pinnedTimelines.get(finalOffSetPosition).bubbleTimeline = bubbleTimeline;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });

        dialogView.actionRemote.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changes[0] = true;
            bubbleTimeline.remote = isChecked;
            pinned.pinnedTimelines.get(finalOffSetPosition).bubbleTimeline = bubbleTimeline;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });

        dialogView.actionExcludeVisibility.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder excludeDialogBuilder = new MaterialAlertDialogBuilder(activity);
            DialogBubbleExcludeVisibilityBinding dialogBinding = DialogBubbleExcludeVisibilityBinding.inflate(activity.getLayoutInflater());
            excludeDialogBuilder.setView(dialogBinding.getRoot());
            excludeDialogBuilder.setTitle(R.string.exclude_visibility);
            if (bubbleTimeline.exclude_visibilities == null) {
                bubbleTimeline.exclude_visibilities = new ArrayList<>();
            }
            for (String value : bubbleTimeline.exclude_visibilities) {
                if (value.equalsIgnoreCase("public")) {
                    dialogBinding.valuePublic.setChecked(true);
                }
                if (value.equalsIgnoreCase("local")) {
                    dialogBinding.valueLocal.setChecked(true);
                }
                if (value.equalsIgnoreCase("direct")) {
                    dialogBinding.valueDirect.setChecked(true);
                }
                if (value.equalsIgnoreCase("list")) {
                    dialogBinding.valueList.setChecked(true);
                }
                if (value.equalsIgnoreCase("private")) {
                    dialogBinding.valuePrivate.setChecked(true);
                }
                if (value.equalsIgnoreCase("unlisted")) {
                    dialogBinding.valueUnlisted.setChecked(true);
                }
            }
            dialogBinding.valuePrivate.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("private")) {
                        bubbleTimeline.exclude_visibilities.add("private");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("private");
                }
            });
            dialogBinding.valueDirect.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("direct")) {
                        bubbleTimeline.exclude_visibilities.add("direct");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("direct");
                }
            });
            dialogBinding.valueList.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("list")) {
                        bubbleTimeline.exclude_visibilities.add("list");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("list");
                }
            });
            dialogBinding.valueLocal.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("local")) {
                        bubbleTimeline.exclude_visibilities.add("local");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("local");
                }
            });
            dialogBinding.valuePublic.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("public")) {
                        bubbleTimeline.exclude_visibilities.add("public");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("public");
                }
            });
            dialogBinding.valueUnlisted.setOnCheckedChangeListener((compoundButton, checked) -> {
                if (checked) {
                    if (!bubbleTimeline.exclude_visibilities.contains("unlisted")) {
                        bubbleTimeline.exclude_visibilities.add("unlisted");
                    }
                } else {
                    bubbleTimeline.exclude_visibilities.remove("unlisted");
                }
            });
            excludeDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                pinned.pinnedTimelines.get(finalOffSetPosition).bubbleTimeline = bubbleTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            excludeDialogBuilder.create().show();
        });

        dialogView.actionReplyVisibility.setOnClickListener(v -> {
            changes[0] = true;
            AlertDialog.Builder replyDialogBuilder = new MaterialAlertDialogBuilder(activity);
            DialogBubbleReplyVisibilityBinding dialogBinding = DialogBubbleReplyVisibilityBinding.inflate(activity.getLayoutInflater());
            replyDialogBuilder.setView(dialogBinding.getRoot());
            replyDialogBuilder.setTitle(R.string.reply_visibility);
            int checkedId = R.id.all;
            if (bubbleTimeline.reply_visibility != null && bubbleTimeline.reply_visibility.equalsIgnoreCase("following")) {
                checkedId = R.id.following;
            } else if (bubbleTimeline.reply_visibility != null && bubbleTimeline.reply_visibility.equalsIgnoreCase("self")) {
                checkedId = R.id.self;
            }
            dialogBinding.replyVisibility.check(checkedId);
            dialogBinding.replyVisibility.setOnCheckedChangeListener((radioGroup, checkedElement) -> {
                if (checkedElement == R.id.all) {
                    bubbleTimeline.reply_visibility = null;
                } else if (checkedElement == R.id.following) {
                    bubbleTimeline.reply_visibility = "following";
                } else if (checkedElement == R.id.self) {
                    bubbleTimeline.reply_visibility = "self";
                }
            });
            replyDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                pinned.pinnedTimelines.get(finalOffSetPosition).bubbleTimeline = bubbleTimeline;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            replyDialogBuilder.create().show();
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        dialogBuilder.setOnDismissListener(dialog -> {
            if (changes[0]) {
                if (activityMainBinding.viewPager.getAdapter() != null) {
                    try {
                        new StatusCache(activity).deleteForSlug(slug);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(activity.getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, null);
                    editor.commit();
                    if (activityMainBinding.tabLayout.getSelectedTabPosition() != -1) {
                        Fragment fragmentMastodonTimeline = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                        if (fragmentMastodonTimeline instanceof FragmentMastodonTimeline && fragmentMastodonTimeline.isVisible()) {
                            FragmentTransaction fragTransaction = activity.getSupportFragmentManager().beginTransaction();
                            fragTransaction.detach(fragmentMastodonTimeline).commit();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.BUBBLE);
                            bundle.putSerializable(Helper.ARG_BUBBLE_TIMELINE, bubbleTimeline);
                            bundle.putSerializable(Helper.ARG_INITIALIZE_VIEW, false);
                            fragmentMastodonTimeline.setArguments(bundle);
                            FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                            fragTransaction2.attach(fragmentMastodonTimeline);
                            fragTransaction2.commit();
                            ((FragmentMastodonTimeline) fragmentMastodonTimeline).recreate();
                        }
                    }
                }
            }
        });
        dialogBuilder.create().show();
    }


    /**
     * Manage long clicks on followed instances
     *
     * @param activity - BaseMainActivity activity
     * @param pinned   - {@link Pinned}
     * @param view     - View
     * @param position - int position of the tab
     */
    public static void instanceClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position, String slug) {

        int toRemove = itemToRemoveInBottomMenu(activity);
        int offSetPosition = position - (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - toRemove);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (singleBar) {
            offSetPosition = position;
        }
        RemoteInstance remoteInstance = pinned.pinnedTimelines.get(offSetPosition).remoteInstance;

        if (remoteInstance == null)
            return;

        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
        PopupFilterInstanceTimelineBinding dialogView = PopupFilterInstanceTimelineBinding.inflate(activity.getLayoutInflater());
        dialogBuilder.setView(dialogView.getRoot());

        final String[] currentFilter = {remoteInstance.filteredWith};
        final boolean[] changes = {false};
        int finalOffSetPosition = offSetPosition;

        // Add "All" radio button
        RadioButton allRadio = new RadioButton(activity);
        allRadio.setText(activity.getString(R.string.all));
        allRadio.setChecked(currentFilter[0] == null);
        allRadio.setId(View.generateViewId());
        dialogView.filterGroup.addView(allRadio);

        allRadio.setOnClickListener(v -> {
            changes[0] = true;
            pinned.pinnedTimelines.get(finalOffSetPosition).remoteInstance.filteredWith = null;
            remoteInstance.filteredWith = null;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
        });

        // Add tag radio buttons
        java.util.List<String> tags = remoteInstance.tags;
        if (tags != null && !tags.isEmpty()) {
            java.util.Collections.sort(tags);
            for (String tag : tags) {
                if (tag == null || tag.isEmpty())
                    continue;
                RadioButton tagRadio = new RadioButton(activity);
                tagRadio.setText(tag);
                tagRadio.setChecked(currentFilter[0] != null && currentFilter[0].equals(tag));
                tagRadio.setId(View.generateViewId());
                dialogView.filterGroup.addView(tagRadio);

                tagRadio.setOnClickListener(v -> {
                    changes[0] = true;
                    pinned.pinnedTimelines.get(finalOffSetPosition).remoteInstance.filteredWith = tag;
                    remoteInstance.filteredWith = tag;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        // Add tags button
        dialogView.actionAddTags.setOnClickListener(v -> {
            AlertDialog.Builder tagsDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View tagsDialogView = activity.getLayoutInflater().inflate(R.layout.tags_instance, new LinearLayout(activity), false);
            tagsDialogBuilder.setView(tagsDialogView);
            final EditText editText = tagsDialogView.findViewById(R.id.filter_words);
            if (remoteInstance.tags != null) {
                StringBuilder valuesTag = new StringBuilder();
                for (String val : remoteInstance.tags)
                    valuesTag.append(val).append(" ");
                editText.setText(valuesTag.toString());
                editText.setSelection(editText.getText().toString().length());
            }
            tagsDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String[] values = editText.getText().toString().trim().split("\\s+");
                remoteInstance.tags = new ArrayList<>(Arrays.asList(values));
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            tagsDialogBuilder.create().show();
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        dialogBuilder.setOnDismissListener(dialog -> {
            if (changes[0]) {
                try {
                    new StatusCache(activity).deleteForSlug(slug);
                } catch (DBException e) {
                    e.printStackTrace();
                }

                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(activity.getString(R.string.SET_INNER_MARKER) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance + slug, null);
                editor.commit();

                if (activityMainBinding.viewPager.getAdapter() != null && activityMainBinding.tabLayout.getSelectedTabPosition() != -1) {
                    Fragment fragment = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                    if (fragment instanceof FragmentMastodonTimeline fragmentMastodonTimeline && fragment.isVisible()) {
                        fragmentMastodonTimeline.updatePinnedTimeline(pinned.pinnedTimelines.get(finalOffSetPosition));
                        fragmentMastodonTimeline.recreate();
                    }
                }
            }
        });
        dialogBuilder.create().show();
    }

    /**
     * Manage long clicks on Nitter instances
     *
     * @param activity - BaseMainActivity activity
     * @param pinned   - {@link Pinned}
     * @param position - int position of the tab
     */
    public static void nitterClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position, String slug) {

        int toRemove = itemToRemoveInBottomMenu(activity);
        int offSetPosition = position - (FedilabPageAdapter.BOTTOM_TIMELINE_COUNT - toRemove);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean singleBar = sharedpreferences.getBoolean(activity.getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (singleBar) {
            offSetPosition = position;
        }
        RemoteInstance remoteInstance = pinned.pinnedTimelines.get(offSetPosition).remoteInstance;
        if (remoteInstance == null)
            return;

        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(activity);
        PopupFilterNitterTimelineBinding dialogView = PopupFilterNitterTimelineBinding.inflate(activity.getLayoutInflater());
        dialogBuilder.setView(dialogView.getRoot());

        if (remoteInstance.type == RemoteInstance.InstanceType.NITTER_TAG) {
            dialogView.actionNitterManageAccounts.setText(R.string.manage_tags);
        }

        int finalOffSetPosition = offSetPosition;

        dialogView.actionDisplayname.setOnClickListener(v -> {
            AlertDialog.Builder nameDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View nameDialogView = activity.getLayoutInflater().inflate(R.layout.tags_name, new LinearLayout(activity), false);
            nameDialogBuilder.setView(nameDialogView);
            final EditText editTextName = nameDialogView.findViewById(R.id.column_name);
            if (remoteInstance.displayName != null) {
                editTextName.setText(remoteInstance.displayName);
                editTextName.setSelection(editTextName.getText().toString().length());
            }
            nameDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String values = editTextName.getText().toString();
                if (values.trim().isEmpty()) {
                    values = remoteInstance.displayName;
                }
                remoteInstance.displayName = values;
                View titleView = view.findViewById(R.id.title);
                if (titleView instanceof AppCompatTextView) {
                    ((AppCompatTextView) titleView).setText(remoteInstance.displayName);
                }
                pinned.pinnedTimelines.get(finalOffSetPosition).remoteInstance = remoteInstance;
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            nameDialogBuilder.create().show();
        });

        dialogView.actionNitterManageAccounts.setOnClickListener(v -> {
            String accounts = remoteInstance.host;
            AlertDialog.Builder accountsDialogBuilder = new MaterialAlertDialogBuilder(activity);
            View accountsDialogView = activity.getLayoutInflater().inflate(R.layout.tags_any, new LinearLayout(activity), false);
            accountsDialogBuilder.setView(accountsDialogView);
            final EditText editText = accountsDialogView.findViewById(R.id.filter_any);
            editText.setHint(R.string.list_of_twitter_accounts);
            if (accounts != null) {
                editText.setText(accounts);
                editText.setSelection(editText.getText().toString().length());
            }
            accountsDialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                pinned.pinnedTimelines.get(finalOffSetPosition).remoteInstance.host = editText.getText().toString().trim();
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
                FragmentMastodonTimeline fragmentMastodonTimeline = null;
                try {
                    new StatusCache(activity).deleteForSlug(slug);
                } catch (DBException e) {
                    e.printStackTrace();
                }
                if (activityMainBinding.viewPager.getAdapter() != null && activityMainBinding.tabLayout.getSelectedTabPosition() != -1) {
                    Fragment fragment = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                    if (fragment instanceof FragmentMastodonTimeline && fragment.isVisible()) {
                        fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                        fragmentMastodonTimeline.refreshAllAdapters();
                    }
                }
                FragmentTransaction fragTransaction1 = activity.getSupportFragmentManager().beginTransaction();
                if (fragmentMastodonTimeline == null)
                    return;
                fragTransaction1.detach(fragmentMastodonTimeline).commit();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinned.pinnedTimelines.get(finalOffSetPosition));
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.REMOTE);
                bundle.putSerializable(Helper.ARG_INITIALIZE_VIEW, false);
                fragmentMastodonTimeline.setArguments(bundle);
                FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                fragTransaction2.attach(fragmentMastodonTimeline);
                fragTransaction2.commit();
                fragmentMastodonTimeline.recreate();
            });
            accountsDialogBuilder.create().show();
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        dialogBuilder.create().show();
    }


}