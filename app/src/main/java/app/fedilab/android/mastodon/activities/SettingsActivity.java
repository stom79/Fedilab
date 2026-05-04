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

import static androidx.navigation.ui.NavigationUI.setupActionBarWithNavController;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySettingsBinding;
import app.fedilab.android.mastodon.helper.settings.SettingsSearchEntry;
import app.fedilab.android.mastodon.helper.settings.SettingsSearchIndex;
import app.fedilab.android.mastodon.ui.drawer.SettingsSearchAdapter;

public class SettingsActivity extends BaseBarActivity {

    private ActivitySettingsBinding binding;
    private List<SettingsSearchEntry> searchResults;
    private SettingsSearchAdapter searchAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = Navigation.findNavController(this, R.id.fragment_container);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder().build();
        setupActionBarWithNavController(this, navController, appBarConfiguration);

        searchResults = new ArrayList<>();
        searchAdapter = new SettingsSearchAdapter(searchResults, this::onSearchResultClick);
        binding.searchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResults.setAdapter(searchAdapter);

        binding.searchSettings.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    binding.searchResults.setVisibility(View.GONE);
                    binding.noResults.setVisibility(View.GONE);
                } else {
                    List<SettingsSearchEntry> results = SettingsSearchIndex.search(SettingsActivity.this, query);
                    searchResults.clear();
                    searchResults.addAll(results);
                    searchAdapter.notifyDataSetChanged();
                    if (results.isEmpty()) {
                        binding.searchResults.setVisibility(View.GONE);
                        binding.noResults.setVisibility(View.VISIBLE);
                    } else {
                        binding.searchResults.setVisibility(View.VISIBLE);
                        binding.noResults.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void onSearchResultClick(SettingsSearchEntry entry) {
        binding.searchSettings.setText("");
        binding.searchSettings.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        binding.searchResults.setVisibility(View.GONE);
        binding.noResults.setVisibility(View.GONE);

        NavController navController = Navigation.findNavController(this, R.id.fragment_container);
        navController.navigate(entry.getNavigationActionId());

        String preferenceKey = entry.getPreferenceKey();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (navHostFragment != null) {
                List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
                if (!fragments.isEmpty()) {
                    Fragment currentFragment = fragments.get(fragments.size() - 1);
                    if (currentFragment instanceof PreferenceFragmentCompat) {
                        PreferenceFragmentCompat prefFragment = (PreferenceFragmentCompat) currentFragment;
                        prefFragment.scrollToPreference(preferenceKey);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> highlightPreference(prefFragment, preferenceKey), 200);
                    }
                }
            }
        }, 300);
    }

    private void highlightPreference(PreferenceFragmentCompat prefFragment, String preferenceKey) {
        Preference pref = prefFragment.findPreference(preferenceKey);
        if (pref == null || pref.getTitle() == null) return;

        String prefTitle = pref.getTitle().toString();
        RecyclerView listView = prefFragment.getListView();

        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            TextView titleView = child.findViewById(android.R.id.title);
            if (titleView != null && prefTitle.equals(titleView.getText().toString())) {
                Drawable originalBackground = child.getBackground();
                int highlightColor = MaterialColors.getColor(child, com.google.android.material.R.attr.colorPrimaryContainer, Color.YELLOW);
                ValueAnimator animator = ValueAnimator.ofArgb(highlightColor, Color.TRANSPARENT);
                animator.setDuration(1500);
                animator.addUpdateListener(a -> child.setBackgroundColor((int) a.getAnimatedValue()));
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        child.setBackground(originalBackground);
                    }
                });
                animator.start();
                break;
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.fragment_container);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.fragment_container);
        if (item.getItemId() == android.R.id.home && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == R.id.FragmentSettingsCategories) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
