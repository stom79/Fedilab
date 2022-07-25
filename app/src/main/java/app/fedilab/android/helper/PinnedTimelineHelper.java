package app.fedilab.android.helper;
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

import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.ui.pageadapter.FedilabPageAdapter.BOTTOM_TIMELINE_COUNT;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.app.BottomMenu;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.RemoteInstance;
import app.fedilab.android.client.entities.app.TagTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityMainBinding;
import app.fedilab.android.databinding.TabCustomViewBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonConversation;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.ui.pageadapter.FedilabPageAdapter;

public class PinnedTimelineHelper {


    public static void sortPositionAsc(List<PinnedTimeline> pinnedTimelineList) {
        //noinspection ComparatorCombinators
        Collections.sort(pinnedTimelineList, (obj1, obj2) -> Integer.compare(obj1.position, obj2.position));
    }

    public static void sortMenuItem(List<BottomMenu.MenuItem> menuItemList) {
        //noinspection ComparatorCombinators
        Collections.sort(menuItemList, (obj1, obj2) -> Integer.compare(obj1.position, obj2.position));
    }

    public synchronized static void redrawTopBarPinned(BaseMainActivity activity, ActivityMainBinding activityMainBinding, Pinned pinned, BottomMenu bottomMenu, List<MastodonList> mastodonLists) {
        //Values must be initialized if there is no records in db
        if (pinned == null) {
            pinned = new Pinned();
            pinned.user_id = BaseMainActivity.currentUserID;
            pinned.instance = BaseMainActivity.currentInstance;
        }
        if (pinned.pinnedTimelines == null) {
            pinned.pinnedTimelines = new ArrayList<>();
        }
        List<PinnedTimeline> pinnedTimelines = pinned.pinnedTimelines;
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
                for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
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
                    pinnedTimeline.position = pinned.pinnedTimelines.size();
                    pinnedTimeline.mastodonList = mastodonList;
                    pinned.pinnedTimelines.add(pinnedTimeline);
                    try {
                        boolean exist = new Pinned(activity).pinnedExist(pinned);
                        if (exist) {
                            new Pinned(activity).updatePinned(pinned);
                        } else {
                            new Pinned(activity).insertPinned(pinned);
                        }
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!needRedraw) { //if there were no changes with list, no need to update tabs
            return;
        }
        //Pinned tab position will start after BOTTOM_TIMELINE_COUNT (ie 5)
        activityMainBinding.tabLayout.removeAllTabs();
        //Small hack to hide first tabs (they represent the item of the bottom menu)
        int toRemove = itemToRemoveInBottomMenu(activity);
        for (int i = 0; i < (BOTTOM_TIMELINE_COUNT - toRemove); i++) {
            activityMainBinding.tabLayout.addTab(activityMainBinding.tabLayout.newTab());
            ((ViewGroup) activityMainBinding.tabLayout.getChildAt(0)).getChildAt(i).setVisibility(View.GONE);
        }
        List<PinnedTimeline> pinnedTimelineVisibleList = new ArrayList<>();
        for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
            if (pinnedTimeline.displayed) {
                TabLayout.Tab tab = activityMainBinding.tabLayout.newTab();
                String name = "";
                switch (pinnedTimeline.type) {
                    case LIST:
                        name = pinnedTimeline.mastodonList.title;
                        break;
                    case TAG:
                        name = pinnedTimeline.tagTimeline.displayName != null && !pinnedTimeline.tagTimeline.displayName.isEmpty() ? pinnedTimeline.tagTimeline.displayName : pinnedTimeline.tagTimeline.name.replaceAll("#", "");
                        break;
                    case REMOTE:
                        name = pinnedTimeline.remoteInstance.host;
                        break;
                }
                TabCustomViewBinding tabCustomViewBinding = TabCustomViewBinding.inflate(activity.getLayoutInflater());
                tabCustomViewBinding.title.setText(name);
                switch (pinnedTimeline.type) {
                    case LIST:
                        tabCustomViewBinding.icon.setImageResource(R.drawable.ic_tl_list);
                        break;
                    case TAG:
                        tabCustomViewBinding.icon.setImageResource(R.drawable.ic_tl_tag);
                        break;
                    case REMOTE:
                        switch (pinnedTimeline.remoteInstance.type) {
                            case PIXELFED:
                                tabCustomViewBinding.icon.setImageResource(R.drawable.pixelfed);
                                break;
                            case MASTODON:
                                tabCustomViewBinding.icon.setImageResource(R.drawable.mastodon_icon_item);
                                break;

                            case MISSKEY:
                                tabCustomViewBinding.icon.setImageResource(R.drawable.misskey);
                                break;
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
                        break;
                }
                tab.setCustomView(tabCustomViewBinding.getRoot());
                activityMainBinding.tabLayout.addTab(tab);
                pinnedTimelineVisibleList.add(pinnedTimeline);
            }
        }

        LinearLayout tabStrip = (LinearLayout) activityMainBinding.tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            // Set LongClick listener to each Tab
            int finalI = i;
            Pinned finalPinned = pinned;
            tabStrip.getChildAt(i).setOnLongClickListener(v -> {
                switch (pinnedTimelineVisibleList.get(finalI - (BOTTOM_TIMELINE_COUNT - toRemove)).type) {
                    case LIST:

                        break;
                    case TAG:
                        tagClick(activity, finalPinned, v, activityMainBinding, finalI);
                        break;
                    case REMOTE:
                        instanceClick(activity, finalPinned, v, activityMainBinding, finalI);
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
        activityMainBinding.viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(activityMainBinding.tabLayout));
        activityMainBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position < BOTTOM_TIMELINE_COUNT - toRemove) {
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
                Fragment fragment = fedilabPageAdapter.getCurrentFragment();
                if (fragment instanceof FragmentMastodonTimeline) {
                    ((FragmentMastodonTimeline) fragment).scrollToTop();
                } else if (fragment instanceof FragmentMastodonConversation) {
                    ((FragmentMastodonConversation) fragment).scrollToTop();
                }
            }
        });

    }

    public static int itemToRemoveInBottomMenu(BaseMainActivity activity) {
        //Small hack to hide first tabs (they represent the item of the bottom menu)
        BottomMenu bottomMenuDb;
        int toRemove = 0;
        try {
            //If some menu items have been hidden we should not create tab for them
            bottomMenuDb = new BottomMenu(activity).getAllBottomMenu(currentAccount);
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
    public static void tagClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position) {
        int toRemove = itemToRemoveInBottomMenu(activity);
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(activity, Helper.popupStyle()), view);
        int offSetPosition = position - (BOTTOM_TIMELINE_COUNT - toRemove);
        String tag;
        TagTimeline tagTimeline = pinned.pinnedTimelines.get(offSetPosition).tagTimeline;
        if (tagTimeline == null)
            return;
        if (tagTimeline.displayName != null)
            tag = tagTimeline.displayName;
        else
            tag = tagTimeline.name;
        popup.getMenuInflater()
                .inflate(R.menu.option_tag_timeline, popup.getMenu());
        Menu menu = popup.getMenu();


        final MenuItem itemMediaOnly = menu.findItem(R.id.action_show_media_only);
        final MenuItem itemShowNSFW = menu.findItem(R.id.action_show_nsfw);


        final boolean[] changes = {false};
        final boolean[] mediaOnly = {false};
        final boolean[] showNSFW = {false};
        mediaOnly[0] = tagTimeline.isART;
        showNSFW[0] = tagTimeline.isNSFW;
        itemMediaOnly.setChecked(mediaOnly[0]);
        itemShowNSFW.setChecked(showNSFW[0]);
        popup.setOnDismissListener(menu1 -> {
            if (changes[0]) {
                if (activityMainBinding.viewPager.getAdapter() != null) {
                    Fragment fragmentMastodonTimeline = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                    if (fragmentMastodonTimeline instanceof FragmentMastodonTimeline && fragmentMastodonTimeline.isVisible()) {
                        FragmentTransaction fragTransaction = activity.getSupportFragmentManager().beginTransaction();
                        fragTransaction.detach(fragmentMastodonTimeline).commit();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TAG);
                        bundle.putSerializable(Helper.ARG_TAG_TIMELINE, tagTimeline);
                        fragmentMastodonTimeline.setArguments(bundle);
                        FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                        fragTransaction2.attach(fragmentMastodonTimeline);
                        fragTransaction2.commit();
                    }
                }
            }
        });


        popup.setOnMenuItemClickListener(item -> {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(activity));
            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return false;
                }
            });
            changes[0] = true;
            int itemId = item.getItemId();
            if (itemId == R.id.action_show_media_only) {
                mediaOnly[0] = !mediaOnly[0];
                tagTimeline.isART = mediaOnly[0];
                pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                itemMediaOnly.setChecked(mediaOnly[0]);
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            } else if (itemId == R.id.action_show_nsfw) {
                showNSFW[0] = !showNSFW[0];
                tagTimeline.isNSFW = showNSFW[0];
                pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                itemShowNSFW.setChecked(showNSFW[0]);
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            } else if (itemId == R.id.action_any) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
                LayoutInflater inflater = activity.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.tags_any, new LinearLayout(activity), false);
                dialogBuilder.setView(dialogView);
                final EditText editText = dialogView.findViewById(R.id.filter_any);
                if (tagTimeline.any != null) {
                    StringBuilder valuesTag = new StringBuilder();
                    for (String val : tagTimeline.any)
                        valuesTag.append(val).append(" ");
                    editText.setText(valuesTag.toString());
                    editText.setSelection(editText.getText().toString().length());
                }
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                    String[] values = editText.getText().toString().trim().split("\\s+");
                    tagTimeline.any = new ArrayList<>(Arrays.asList(values));
                    pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                });
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            } else if (itemId == R.id.action_all) {
                AlertDialog.Builder dialogBuilder;
                LayoutInflater inflater;
                View dialogView;
                AlertDialog alertDialog;
                dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
                inflater = activity.getLayoutInflater();
                dialogView = inflater.inflate(R.layout.tags_all, new LinearLayout(activity), false);
                dialogBuilder.setView(dialogView);
                final EditText editTextAll = dialogView.findViewById(R.id.filter_all);
                if (tagTimeline.all != null) {
                    StringBuilder valuesTag = new StringBuilder();
                    for (String val : tagTimeline.all)
                        valuesTag.append(val).append(" ");
                    editTextAll.setText(valuesTag.toString());
                    editTextAll.setSelection(editTextAll.getText().toString().length());
                }
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                    String[] values = editTextAll.getText().toString().trim().split("\\s+");
                    tagTimeline.all = new ArrayList<>(Arrays.asList(values));
                    pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                });
                alertDialog = dialogBuilder.create();
                alertDialog.show();
            } else if (itemId == R.id.action_none) {
                AlertDialog.Builder dialogBuilder;
                LayoutInflater inflater;
                View dialogView;
                AlertDialog alertDialog;
                dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
                inflater = activity.getLayoutInflater();
                dialogView = inflater.inflate(R.layout.tags_all, new LinearLayout(activity), false);
                dialogBuilder.setView(dialogView);
                final EditText editTextNone = dialogView.findViewById(R.id.filter_all);
                if (tagTimeline.none != null) {
                    StringBuilder valuesTag = new StringBuilder();
                    for (String val : tagTimeline.none)
                        valuesTag.append(val).append(" ");
                    editTextNone.setText(valuesTag.toString());
                    editTextNone.setSelection(editTextNone.getText().toString().length());
                }
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                    String[] values = editTextNone.getText().toString().trim().split("\\s+");
                    tagTimeline.none = new ArrayList<>(Arrays.asList(values));
                    pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                });
                alertDialog = dialogBuilder.create();
                alertDialog.show();
            } else if (itemId == R.id.action_displayname) {
                AlertDialog.Builder dialogBuilder;
                LayoutInflater inflater;
                View dialogView;
                AlertDialog alertDialog;
                dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
                inflater = activity.getLayoutInflater();
                dialogView = inflater.inflate(R.layout.tags_name, new LinearLayout(activity), false);
                dialogBuilder.setView(dialogView);
                final EditText editTextName = dialogView.findViewById(R.id.column_name);
                if (tagTimeline.displayName != null) {
                    editTextName.setText(tagTimeline.displayName);
                    editTextName.setSelection(editTextName.getText().toString().length());
                }
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                    String values = editTextName.getText().toString();
                    if (values.trim().length() == 0)
                        values = tag;
                    tagTimeline.displayName = values;
                    pinned.pinnedTimelines.get(offSetPosition).tagTimeline = tagTimeline;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                });
                alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
            return false;
        });
        popup.show();
    }


    /**
     * Manage long clicks on followed instances
     *
     * @param activity  - BaseMainActivity activity
     * @param pinned   - {@link Pinned}
     * @param view     - View
     * @param position - int position of the tab
     */
    public static void instanceClick(BaseMainActivity activity, Pinned pinned, View view, ActivityMainBinding activityMainBinding, int position) {

        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(activity, Helper.popupStyle()), view);
        int toRemove = itemToRemoveInBottomMenu(activity);
        int offSetPosition = position - (BOTTOM_TIMELINE_COUNT - toRemove);
        RemoteInstance remoteInstance = pinned.pinnedTimelines.get(offSetPosition).remoteInstance;
        if (remoteInstance == null)
            return;
        final String[] currentFilter = {remoteInstance.filteredWith};
        final boolean[] changes = {false};
        String title;
        if (currentFilter[0] == null) {
            title = "✔ " + activity.getString(R.string.all);
        } else {
            title = activity.getString(R.string.all);
        }

        MenuItem itemAll = popup.getMenu().add(0, 0, Menu.NONE, title);

        itemAll.setOnMenuItemClickListener(item -> {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(activity));
            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return false;
                }
            });
            changes[0] = true;
            FragmentMastodonTimeline fragmentMastodonTimeline = null;
            if (activityMainBinding.viewPager.getAdapter() != null) {
                Fragment fragment = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                if (fragment instanceof FragmentMastodonTimeline && fragment.isVisible()) {
                    fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                }
            }
            if (fragmentMastodonTimeline == null)
                return false;
            FragmentTransaction fragTransaction1 = activity.getSupportFragmentManager().beginTransaction();

            pinned.pinnedTimelines.get(offSetPosition).remoteInstance.filteredWith = null;
            remoteInstance.filteredWith = null;
            currentFilter[0] = null;
            pinned.pinnedTimelines.get(offSetPosition).remoteInstance = remoteInstance;
            try {
                new Pinned(activity).updatePinned(pinned);
            } catch (DBException e) {
                e.printStackTrace();
            }
            fragTransaction1.detach(fragmentMastodonTimeline).commit();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinned);
            bundle.putString("instanceType", remoteInstance.type.getValue());
            bundle.putString("timelineId", remoteInstance.id);
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.REMOTE);
            fragmentMastodonTimeline.setArguments(bundle);
            FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
            fragTransaction2.attach(fragmentMastodonTimeline);
            fragTransaction2.commit();
            popup.getMenu().close();
            return false;
        });

        java.util.List<String> tags = remoteInstance.tags;
        if (tags != null && tags.size() > 0) {
            java.util.Collections.sort(tags);
            for (String tag : tags) {
                if (tag == null || tag.length() == 0)
                    continue;
                if (currentFilter[0] != null && currentFilter[0].equals(tag)) {
                    title = "✔ " + tag;
                } else {
                    title = tag;
                }
                MenuItem item = popup.getMenu().add(0, 0, Menu.NONE, title);
                item.setOnMenuItemClickListener(item1 -> {
                    FragmentMastodonTimeline fragmentMastodonTimeline = null;
                    if (activityMainBinding.viewPager.getAdapter() != null) {
                        Fragment fragment = (Fragment) activityMainBinding.viewPager.getAdapter().instantiateItem(activityMainBinding.viewPager, activityMainBinding.tabLayout.getSelectedTabPosition());
                        if (fragment instanceof FragmentMastodonTimeline && fragment.isVisible()) {
                            fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                            fragmentMastodonTimeline.refreshAllAdapters();
                        }
                    }
                    FragmentTransaction fragTransaction1 = activity.getSupportFragmentManager().beginTransaction();
                    if (fragmentMastodonTimeline == null)
                        return false;
                    pinned.pinnedTimelines.get(offSetPosition).remoteInstance.filteredWith = tag;
                    remoteInstance.filteredWith = tag;
                    try {
                        new Pinned(activity).updatePinned(pinned);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    currentFilter[0] = remoteInstance.filteredWith;
                    fragTransaction1.detach(fragmentMastodonTimeline).commit();
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinned);
                    bundle.putString("instanceType", remoteInstance.type.getValue());
                    bundle.putString("timelineId", remoteInstance.id);
                    bundle.putString("currentfilter", remoteInstance.filteredWith);
                    bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.REMOTE);
                    fragmentMastodonTimeline.setArguments(bundle);
                    FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                    fragTransaction2.attach(fragmentMastodonTimeline);
                    fragTransaction2.commit();
                    return false;
                });
            }
        }


        MenuItem itemadd = popup.getMenu().add(0, 0, Menu.NONE, activity.getString(R.string.add_tags));
        itemadd.setOnMenuItemClickListener(item -> {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            item.setActionView(new View(activity));
            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return false;
                }
            });
            changes[0] = true;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, Helper.dialogStyle());
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.tags_instance, new LinearLayout(activity), false);
            dialogBuilder.setView(dialogView);
            final EditText editText = dialogView.findViewById(R.id.filter_words);
            if (remoteInstance.tags != null) {
                StringBuilder valuesTag = new StringBuilder();
                for (String val : remoteInstance.tags)
                    valuesTag.append(val).append(" ");
                editText.setText(valuesTag.toString());
                editText.setSelection(editText.getText().toString().length());
            }
            dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                String[] values = editText.getText().toString().trim().split("\\s+");
                remoteInstance.tags = new ArrayList<>(Arrays.asList(values));
                try {
                    new Pinned(activity).updatePinned(pinned);
                } catch (DBException e) {
                    e.printStackTrace();
                }
                popup.getMenu().clear();
                popup.getMenu().close();
                instanceClick(activity, pinned, view, activityMainBinding, position);
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
            return false;
        });

        popup.setOnDismissListener(menu -> {
            if (changes[0]) {
                FragmentMastodonTimeline fragmentMastodonTimeline = null;
                if (activityMainBinding.viewPager.getAdapter() != null) {
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
                bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, pinned);
                bundle.putString("instanceType", remoteInstance.type.getValue());
                bundle.putString("timelineId", remoteInstance.id);
                if (currentFilter[0] != null) {
                    bundle.putString("currentfilter", remoteInstance.filteredWith);
                }
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.REMOTE);
                fragmentMastodonTimeline.setArguments(bundle);
                FragmentTransaction fragTransaction2 = activity.getSupportFragmentManager().beginTransaction();
                fragTransaction2.attach(fragmentMastodonTimeline);
                fragTransaction2.commit();
            }
        });

        popup.show();
    }
}
