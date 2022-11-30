package app.fedilab.android.activities;
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
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityTrendsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTag;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;


public class TrendsActivity extends BaseBarActivity {


    private ActivityTrendsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.tags)));
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.toots)));
        binding.searchTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.trendsViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment;
                if (binding.trendsViewpager.getAdapter() != null) {
                    fragment = (Fragment) binding.trendsViewpager.getAdapter().instantiateItem(binding.trendsViewpager, tab.getPosition());
                    if (fragment instanceof FragmentMastodonTimeline) {
                        FragmentMastodonTimeline fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                        fragmentMastodonTimeline.scrollToTop();
                    } else if (fragment instanceof FragmentMastodonTag) {
                        FragmentMastodonTag fragmentMastodonTag = ((FragmentMastodonTag) fragment);
                        fragmentMastodonTag.scrollToTop();
                    }
                }
            }
        });

        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.trendsViewpager.setAdapter(mPagerAdapter);
        binding.trendsViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                TabLayout.Tab tab = binding.searchTabLayout.getTabAt(position);
                if (tab != null)
                    tab.select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Pager adapter for the 4 fragments
     */
    @SuppressWarnings("deprecation")
    private static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {
                FragmentMastodonTag fragmentMastodonTag = new FragmentMastodonTag();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TREND_TAG);
                fragmentMastodonTag.setArguments(bundle);
                return fragmentMastodonTag;
            }
            FragmentMastodonTimeline fragmentMastodonTimeline = new FragmentMastodonTimeline();
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TREND_MESSAGE);
            fragmentMastodonTimeline.setArguments(bundle);
            return fragmentMastodonTimeline;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
