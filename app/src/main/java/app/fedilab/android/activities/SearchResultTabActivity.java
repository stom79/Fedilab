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

import android.app.SearchManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySearchResultTabsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTag;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import es.dmoral.toasty.Toasty;


public class SearchResultTabActivity extends BaseActivity {


    private String search;
    private ActivitySearchResultTabsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivitySearchResultTabsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle b = getIntent().getExtras();
        if (b != null) {
            search = b.getString(Helper.ARG_SEARCH_KEYWORD, null);

        }
        if (search == null) {
            Toasty.error(SearchResultTabActivity.this, getString(R.string.toast_error_search), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        setTitle(search);
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab());
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab());
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab());
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab());
        binding.searchTabLayout.setTabTextColors(ThemeHelper.getAttColor(SearchResultTabActivity.this, R.attr.mTextColor), ContextCompat.getColor(SearchResultTabActivity.this, R.color.cyanea_accent_dark_reference));
        binding.searchTabLayout.setTabIconTint(ThemeHelper.getColorStateList(SearchResultTabActivity.this));
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(SearchResultTabActivity.this);
        binding.searchViewpager.setAdapter(mPagerAdapter);
        binding.searchViewpager.setOffscreenPageLimit(3);
        new TabLayoutMediator(binding.searchTabLayout, binding.searchViewpager,
                (tab, position) -> {
                    binding.searchViewpager.setCurrentItem(tab.getPosition(), true);
                    switch (position) {
                        case 0:
                            tab.setText(getString(R.string.tags));
                            break;
                        case 1:
                            tab.setText(getString(R.string.accounts));
                            break;
                        case 2:
                            tab.setText(getString(R.string.toots));
                            break;
                        case 3:
                            tab.setText(getString(R.string.action_cache));
                            break;
                    }
                }
        ).attach();
        binding.searchTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.searchViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + binding.searchViewpager.getCurrentItem());
                if (fragment instanceof FragmentMastodonAccount) {
                    FragmentMastodonAccount fragmentMastodonAccount = ((FragmentMastodonAccount) fragment);
                    fragmentMastodonAccount.scrollToTop();
                } else if (fragment instanceof FragmentMastodonTimeline) {
                    FragmentMastodonTimeline fragmentMastodonTimeline = ((FragmentMastodonTimeline) fragment);
                    fragmentMastodonTimeline.scrollToTop();
                } else if (fragment instanceof FragmentMastodonTag) {
                    FragmentMastodonTag fragmentMastodonTag = ((FragmentMastodonTag) fragment);
                    fragmentMastodonTag.scrollToTop();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(binding.searchTabLayout.getWindowToken(), 0);
                query = query.replaceAll("^#+", "");
                search = query.trim();
                ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(SearchResultTabActivity.this);
                binding.searchViewpager.setAdapter(mPagerAdapter);
                searchView.clearFocus();
                setTitle(search);
                searchView.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            setTitle(search);
            return false;
        });
        searchView.setOnSearchClickListener(v -> {
            searchView.setQuery(search, false);
            searchView.setIconified(false);
        });



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_search) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Pager adapter for the 4 fragments
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }


        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle bundle = new Bundle();
            switch (position) {
                case 0:
                    FragmentMastodonTag fragmentMastodonTag = new FragmentMastodonTag();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonTag.setArguments(bundle);
                    return fragmentMastodonTag;
                case 1:
                    FragmentMastodonAccount fragmentMastodonAccount = new FragmentMastodonAccount();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonAccount.setArguments(bundle);
                    return fragmentMastodonAccount;
                case 2:
                    FragmentMastodonTimeline fragmentMastodonTimeline = new FragmentMastodonTimeline();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonTimeline.setArguments(bundle);
                    return fragmentMastodonTimeline;
                default:
                    fragmentMastodonTimeline = new FragmentMastodonTimeline();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD_CACHE, search);
                    fragmentMastodonTimeline.setArguments(bundle);
                    return fragmentMastodonTimeline;
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
