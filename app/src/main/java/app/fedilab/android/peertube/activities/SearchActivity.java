package app.fedilab.android.peertube.activities;
/* Copyright 2023 Thomas Schneider
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
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySearchResultPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.fragment.DisplayChannelsFragment;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import es.dmoral.toasty.Toasty;


public class SearchActivity extends BaseBarActivity {


    private String search;
    private ActivitySearchResultPeertubeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchResultPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            search = b.getString("search");
        } else {
            Toasty.error(SearchActivity.this, getString(R.string.toast_error_search), Toast.LENGTH_LONG).show();
        }

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(search);

        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.videos)));
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.channels)));
        binding.searchPager.setOffscreenPageLimit(2);

        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.searchPager.setAdapter(mPagerAdapter);
        binding.searchPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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

        binding.searchTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.searchPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = null;
                if (binding.searchPager.getAdapter() != null)
                    fragment = (Fragment) binding.searchPager.getAdapter().instantiateItem(binding.searchPager, tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        if (fragment != null) {
                            DisplayVideosFragment displayVideosFragment = ((DisplayVideosFragment) fragment);
                            displayVideosFragment.scrollToTop();
                        }
                        break;
                    case 1:
                        if (fragment != null) {
                            DisplayChannelsFragment displayChannelsFragment = ((DisplayChannelsFragment) fragment);
                            displayChannelsFragment.scrollToTop();
                        }
                        break;
                }
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Pager adapter for the 2 fragments
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {
                DisplayVideosFragment displayVideosFragment = new DisplayVideosFragment();
                bundle.putString("search_peertube", search);
                displayVideosFragment.setArguments(bundle);
                return displayVideosFragment;
            }
            DisplayChannelsFragment displayChannelsFragment = new DisplayChannelsFragment();
            bundle.putString("search_peertube", search);
            displayChannelsFragment.setArguments(bundle);
            return displayChannelsFragment;
        }


        @Override
        public int getCount() {
            return 2;
        }
    }

}
