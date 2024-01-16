package app.fedilab.android.mastodon.ui.fragment.timeline;
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentProfileTimelinesBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.pageadapter.FedilabProfilePageAdapter;

public class FragmentProfileTimeline extends Fragment {

    private Account account;
    private FragmentProfileTimelinesBinding binding;
    private boolean checkRemotely;
    private boolean show_boosts = true, show_replies = true;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileTimelinesBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            String cached_account_id = getArguments().getString(Helper.ARG_CACHED_ACCOUNT_ID);
            try {
                account = new CachedBundle(requireActivity()).getCachedAccount(currentAccount, cached_account_id);
            } catch (DBException e) {
                e.printStackTrace();
            }
            checkRemotely = getArguments().getBoolean(Helper.ARG_CHECK_REMOTELY, false);
            initializeAfterBundle();
        }
        return binding.getRoot();
    }


    private void initializeAfterBundle() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.toots)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.replies)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.media)));
        binding.viewpager.setAdapter(new FedilabProfilePageAdapter(getChildFragmentManager(), account, checkRemotely));
        binding.viewpager.setOffscreenPageLimit(3);
        binding.viewpager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (binding.viewpager.getAdapter() != null && binding.viewpager
                        .getAdapter()
                        .instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem()) instanceof FragmentMastodonTimeline fragmentMastodonTimeline) {
                    fragmentMastodonTimeline.goTop();
                }
            }
        });

        final LinearLayout tabStrip = (LinearLayout) binding.tabLayout.getChildAt(0);
        tabStrip.getChildAt(0).setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireActivity(), binding.tabLayout.getChildAt(0));
            popup.getMenuInflater()
                    .inflate(R.menu.option_filter_toots_account, popup.getMenu());
            Menu menu = popup.getMenu();

            final MenuItem itemShowBoosts = menu.findItem(R.id.action_show_boosts);
            final MenuItem itemShowReplies = menu.findItem(R.id.action_show_replies);

            itemShowBoosts.setChecked(show_boosts);
            itemShowReplies.setChecked(show_replies);

            popup.setOnDismissListener(menu1 -> {
                if (binding.viewpager.getAdapter() != null && binding.viewpager
                        .getAdapter()
                        .instantiateItem(binding.viewpager, binding.viewpager.getCurrentItem()) instanceof FragmentMastodonTimeline fragmentMastodonTimeline) {
                    FragmentTransaction fragTransaction = getChildFragmentManager().beginTransaction();
                    fragTransaction.detach(fragmentMastodonTimeline).commit();
                    Bundle args = new Bundle();
                    args.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.ACCOUNT_TIMELINE);
                    args.putSerializable(Helper.ARG_ACCOUNT, account);
                    args.putBoolean(Helper.ARG_SHOW_PINNED, true);
                    args.putBoolean(Helper.ARG_CHECK_REMOTELY, checkRemotely);
                    args.putBoolean(Helper.ARG_SHOW_REBLOGS, show_boosts);
                    args.putBoolean(Helper.ARG_SHOW_REPLIES, show_replies);
                    new CachedBundle(requireActivity()).insertBundle(args, currentAccount, bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        fragmentMastodonTimeline.setArguments(bundle);
                        FragmentTransaction fragTransaction2 = getChildFragmentManager().beginTransaction();
                        fragTransaction2.attach(fragmentMastodonTimeline);
                        fragTransaction2.commit();
                        fragmentMastodonTimeline.recreate();
                    });
                }

            });
            popup.setOnMenuItemClickListener(item -> {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                item.setActionView(new View(requireActivity()));
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
                int itemId = item.getItemId();
                if (itemId == R.id.action_show_boosts) {
                    show_boosts = !show_boosts;
                } else if (itemId == R.id.action_show_replies) {
                    show_replies = !show_replies;
                }
                if (binding.tabLayout.getTabAt(0) != null)
                    binding.tabLayout.getTabAt(0).select();
                itemShowReplies.setChecked(show_replies);
                itemShowBoosts.setChecked(show_boosts);
                return true;
            });
            popup.show();
            return true;
        });

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


}
