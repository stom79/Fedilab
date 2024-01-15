package app.fedilab.android.mastodon.activities;
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
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySearchResultTabsBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.AccountsSearchTopBarAdapter;
import app.fedilab.android.mastodon.ui.drawer.TagSearchTopBarAdapter;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonAccount;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTag;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import es.dmoral.toasty.Toasty;


public class SearchResultTabActivity extends BaseBarActivity {


    public Boolean tagEmpty, accountEmpty;
    private String search;
    private ActivitySearchResultTabsBinding binding;
    private TabLayout.Tab initial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        }
        setTitle(search);
        initial = binding.searchTabLayout.newTab();
        binding.searchTabLayout.addTab(initial.setText(getString(R.string.tags)));
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.accounts)));
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.toots)));
        binding.searchTabLayout.addTab(binding.searchTabLayout.newTab().setText(getString(R.string.action_cache)));
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
                Fragment fragment;
                if (binding.searchViewpager.getAdapter() != null) {
                    fragment = (Fragment) binding.searchViewpager.getAdapter().instantiateItem(binding.searchViewpager, tab.getPosition());
                    if (fragment instanceof FragmentMastodonAccount fragmentMastodonAccount) {
                        fragmentMastodonAccount.scrollToTop();
                    } else if (fragment instanceof FragmentMastodonTimeline fragmentMastodonTimeline) {
                        fragmentMastodonTimeline.scrollToTop();
                    } else if (fragment instanceof FragmentMastodonTag fragmentMastodonTag) {
                        fragmentMastodonTag.scrollToTop();
                    }
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
        if(searchView == null) {
            return true;
        }
        if (search != null) {
            searchView.setQuery(search, false);
        }
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.searchTabLayout.getWindowToken(), 0);
                query = query.replaceAll("^#+", "");
                search = query.trim();
                ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
                binding.searchViewpager.setAdapter(mPagerAdapter);
                setTitle(search);
                searchView.setIconified(true);
                searchView.setQuery(search, false);
                searchView.clearFocus();
                binding.searchTabLayout.selectTab(initial);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String pattern = "^(@[\\w_-]+@[a-z0-9.\\-]+|@[\\w_-]+)";
                final Pattern mentionPattern = Pattern.compile(pattern);
                String patternTag = "^#([\\w-]{2,})$";
                final Pattern tagPattern = Pattern.compile(patternTag);
                Matcher matcherMention, matcherTag;
                matcherMention = mentionPattern.matcher(newText);
                matcherTag = tagPattern.matcher(newText);
                if (newText.trim().isEmpty()) {
                    searchView.setSuggestionsAdapter(null);
                }
                if (matcherMention.matches()) {
                    String[] from = new String[]{SearchManager.SUGGEST_COLUMN_ICON_1, SearchManager.SUGGEST_COLUMN_TEXT_1};
                    int[] to = new int[]{R.id.account_pp, R.id.account_un};
                    String searchGroup = matcherMention.group();
                    AccountsVM accountsVM = new ViewModelProvider(SearchResultTabActivity.this).get(AccountsVM.class);
                    MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                            SearchManager.SUGGEST_COLUMN_ICON_1,
                            SearchManager.SUGGEST_COLUMN_TEXT_1});
                    accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, 5, false, false)
                            .observe(SearchResultTabActivity.this, accounts -> {
                                if (accounts == null) {
                                    return;
                                }
                                AccountsSearchTopBarAdapter cursorAdapter = new AccountsSearchTopBarAdapter(SearchResultTabActivity.this, accounts, R.layout.drawer_account_search, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                                searchView.setSuggestionsAdapter(cursorAdapter);
                                new Thread(() -> {
                                    int i = 0;
                                    for (Account account : accounts) {
                                        FutureTarget<File> futureTarget = Glide
                                                .with(SearchResultTabActivity.this.getApplicationContext())
                                                .load(account.avatar_static)
                                                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                                        File cacheFile;
                                        try {
                                            cacheFile = futureTarget.get();
                                            cursor.addRow(new String[]{String.valueOf(i), cacheFile.getAbsolutePath(), "@" + account.acct});
                                            i++;
                                        } catch (ExecutionException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    runOnUiThread(() -> cursorAdapter.changeCursor(cursor));
                                }).start();

                            });
                } else if (matcherTag.matches()) {
                    SearchVM searchVM = new ViewModelProvider(SearchResultTabActivity.this).get(SearchVM.class);
                    String[] from = new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1};
                    int[] to = new int[]{R.id.tag_name};
                    String searchGroup = matcherTag.group();
                    MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                            SearchManager.SUGGEST_COLUMN_TEXT_1});
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, null,
                            "hashtags", false, true, false, 0,
                            null, null, 10).observe(SearchResultTabActivity.this,
                            results -> {
                                if (results == null || results.hashtags == null) {
                                    return;
                                }
                                TagSearchTopBarAdapter cursorAdapter = new TagSearchTopBarAdapter(SearchResultTabActivity.this, results.hashtags, R.layout.drawer_tag_search, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                                searchView.setSuggestionsAdapter(cursorAdapter);
                                int i = 0;
                                for (Tag tag : results.hashtags) {
                                    cursor.addRow(new String[]{String.valueOf(i), "#" + tag.name});
                                    i++;
                                }
                                runOnUiThread(() -> cursorAdapter.changeCursor(cursor));
                            });
                }
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

        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.searchViewpager.setAdapter(mPagerAdapter);
        binding.searchViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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


    public void moveToAccount() {
        tagEmpty = null;
        accountEmpty = null;
        binding.searchViewpager.post(() -> binding.searchViewpager.setCurrentItem(1));
    }

    public void moveToMessage() {
        tagEmpty = null;
        accountEmpty = null;
        binding.searchViewpager.post(() -> binding.searchViewpager.setCurrentItem(2));
    }

    /**
     * Pager adapter for the 4 fragments
     */
    @SuppressWarnings("deprecation")
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            FragmentMastodonTimeline fragmentMastodonTimeline;
            switch (position) {
                case 0 -> {
                    FragmentMastodonTag fragmentMastodonTag = new FragmentMastodonTag();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonTag.setArguments(bundle);
                    return fragmentMastodonTag;
                }
                case 1 -> {
                    FragmentMastodonAccount fragmentMastodonAccount = new FragmentMastodonAccount();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonAccount.setArguments(bundle);
                    return fragmentMastodonAccount;
                }
                case 2 -> {
                    fragmentMastodonTimeline = new FragmentMastodonTimeline();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD, search);
                    fragmentMastodonTimeline.setArguments(bundle);
                    return fragmentMastodonTimeline;
                }
                default -> {
                    fragmentMastodonTimeline = new FragmentMastodonTimeline();
                    bundle.putString(Helper.ARG_SEARCH_KEYWORD_CACHE, search);
                    fragmentMastodonTimeline.setArguments(bundle);
                    return fragmentMastodonTimeline;
                }
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
