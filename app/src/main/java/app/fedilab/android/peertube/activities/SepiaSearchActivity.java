package app.fedilab.android.peertube.activities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static app.fedilab.android.peertube.activities.PeertubeActivity.hideKeyboard;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySepiaSearchBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.entities.SepiaSearch;
import app.fedilab.android.peertube.fragment.DisplaySepiaSearchFragment;
import app.fedilab.android.peertube.helper.Helper;


public class SepiaSearchActivity extends BaseBarActivity {


    private SepiaSearch sepiaSearchVideo, sepiaSearchChannel;

    private ActivitySepiaSearchBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySepiaSearchBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);


        sepiaSearchVideo = new SepiaSearch();
        sepiaSearchChannel = new SepiaSearch();
        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        sepiaSearchVideo.setCount(String.valueOf(sharedpreferences.getInt(Helper.SET_VIDEOS_PER_PAGE, Helper.VIDEOS_PER_PAGE)));
        sepiaSearchVideo.setDurationMin(0);
        sepiaSearchVideo.setDurationMax(9999999);
        sepiaSearchVideo.setStart("0");
        sepiaSearchVideo.setSort("-match");
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        binding.filter.setOnClickListener(view -> {
            if (binding.filterElements.getVisibility() == View.VISIBLE) {
                binding.filterElements.setVisibility(View.GONE);
            } else {
                binding.filterElements.setVisibility(View.VISIBLE);
            }
        });


        binding.sepiaElementNsfw.setOnCheckedChangeListener((group, checkedId) -> sepiaSearchVideo.setNsfw(checkedId != R.id.sepia_element_nsfw_no));

        binding.radioDate.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sepia_element_published_date_today) {
                Calendar cal = GregorianCalendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                sepiaSearchVideo.setStartDate(cal.getTime());
            } else if (checkedId == R.id.sepia_element_published_date_last_7_days) {
                Calendar cal;
                cal = GregorianCalendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -7);
                sepiaSearchVideo.setStartDate(cal.getTime());
            } else if (checkedId == R.id.sepia_element_published_date_last_30_days) {
                Calendar cal;
                cal = GregorianCalendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -30);
                sepiaSearchVideo.setStartDate(cal.getTime());
            } else if (checkedId == R.id.sepia_element_published_date_last_365_days) {
                Calendar cal;
                cal = GregorianCalendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -365);
                sepiaSearchVideo.setStartDate(cal.getTime());
            } else {
                sepiaSearchVideo.setStartDate(null);
            }
        });


        binding.duration.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sepia_element_duration_short) {
                sepiaSearchVideo.setDurationMin(0);
                sepiaSearchVideo.setDurationMax(240);
            } else if (checkedId == R.id.sepia_element_duration_medium) {
                sepiaSearchVideo.setDurationMin(240);
                sepiaSearchVideo.setDurationMax(600);
            } else if (checkedId == R.id.sepia_element_duration_long) {
                sepiaSearchVideo.setDurationMin(600);
                sepiaSearchVideo.setDurationMax(999999999);
            } else {
                sepiaSearchVideo.setDurationMin(0);
                sepiaSearchVideo.setDurationMax(999999999);
            }
        });


        ArrayAdapter<String> adapterSortBy = new ArrayAdapter<>(SepiaSearchActivity.this,
                android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.sort_by_array));
        binding.sortBy.setAdapter(adapterSortBy);
        binding.sortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String orderby, channelOrderBy;
                switch (position) {
                    case 1:
                        orderby = "-publishedAt";
                        channelOrderBy = "-createdAt";
                        break;
                    case 2:
                        orderby = "publishedAt";
                        channelOrderBy = "createdAt";
                        break;
                    default:
                        orderby = "-match";
                        channelOrderBy = null;
                }
                sepiaSearchVideo.setSort(orderby);
                sepiaSearchChannel.setSort(channelOrderBy);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        LinkedHashMap<Integer, String> categories = new LinkedHashMap<>(peertubeInformation.getCategories());
        LinkedHashMap<Integer, String> licences = new LinkedHashMap<>(peertubeInformation.getLicences());
        LinkedHashMap<String, String> languages = new LinkedHashMap<>(peertubeInformation.getLanguages());
        LinkedHashMap<String, String> translations = null;

        if (peertubeInformation.getTranslations() != null) {
            translations = new LinkedHashMap<>(peertubeInformation.getTranslations());
        }

        //Populate catgories
        String[] categoriesA = new String[categories.size() + 1];
        categoriesA[0] = getString(R.string.display_all_categories);
        Iterator<Map.Entry<Integer, String>> it = categories.entrySet().iterator();
        int i = 1;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                categoriesA[i] = pair.getValue();
            else
                categoriesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }
        ArrayAdapter<String> adapterCatgories = new ArrayAdapter<>(SepiaSearchActivity.this,
                android.R.layout.simple_spinner_dropdown_item, categoriesA);
        binding.sepiaElementCategory.setAdapter(adapterCatgories);


        //Populate licenses
        String[] licensesA = new String[licences.size() + 1];
        licensesA[0] = getString(R.string.display_all_licenses);
        it = licences.entrySet().iterator();
        i = 1;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                licensesA[i] = pair.getValue();
            else
                licensesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }
        ArrayAdapter<String> adapterLicenses = new ArrayAdapter<>(SepiaSearchActivity.this,
                android.R.layout.simple_spinner_dropdown_item, licensesA);
        binding.sepiaElementLicense.setAdapter(adapterLicenses);

        //Populate languages
        String[] languagesA = new String[languages.size() + 1];
        languagesA[0] = getString(R.string.display_all_languages);
        Iterator<Map.Entry<String, String>> itl = languages.entrySet().iterator();
        i = 1;
        while (itl.hasNext()) {
            Map.Entry<String, String> pair = itl.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                languagesA[i] = pair.getValue();
            else
                languagesA[i] = translations.get(pair.getValue());
            itl.remove();
            i++;
        }
        ArrayAdapter<String> adapterLanguages = new ArrayAdapter<>(SepiaSearchActivity.this,
                android.R.layout.simple_spinner_dropdown_item, languagesA);
        binding.sepiaElementLanguage.setAdapter(adapterLanguages);


        binding.sepiaElementLicense.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLicensePosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //Manage categories
        binding.sepiaElementCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCategoryPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Manage languages
        binding.sepiaElementLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLanguagesPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        binding.searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                makeSearch();
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                makeSearch();
            }
        });
        binding.applyFilter.setOnClickListener(v -> makeSearch());

        binding.searchBar.openSearch();
    }

    private void makeSearch() {
        hideKeyboard(SepiaSearchActivity.this);
        sepiaSearchVideo.setStart("0");
        if (binding.sepiaElementOneOfTags.getTags().size() > 0) {
            sepiaSearchVideo.setTagsOneOf(binding.sepiaElementOneOfTags.getTags());
        } else {
            sepiaSearchVideo.setTagsOneOf(null);
        }
        if (binding.sepiaElementAllOfTags.getTags().size() > 0) {
            sepiaSearchVideo.setTagsAllOf(binding.sepiaElementAllOfTags.getTags());
        } else {
            sepiaSearchVideo.setTagsAllOf(null);
        }

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("SEPIA_SEARCH");
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        binding.filterElements.setVisibility(View.GONE);
        sepiaSearchVideo.setSearch(binding.searchBar.getText());
        DisplaySepiaSearchFragment displaySepiaSearchFragment = new DisplaySepiaSearchFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("sepiaSearchVideo", sepiaSearchVideo);
        displaySepiaSearchFragment.setArguments(bundle);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.container, displaySepiaSearchFragment, "SEPIA_SEARCH").commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateLanguagesPosition(int position) {
        LinkedHashMap<String, String> languagesCheck = new LinkedHashMap<>(peertubeInformation.getLanguages());
        Iterator<Map.Entry<String, String>> it = languagesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (i == position && position > 0) {
                List<String> languages = new ArrayList<>();
                languages.add(pair.getKey());
                sepiaSearchVideo.setBoostLanguages(languages);
                break;
            } else {
                sepiaSearchVideo.setBoostLanguages(null);
            }
            it.remove();
            i++;
        }
    }

    private void updateCategoryPosition(int position) {
        LinkedHashMap<Integer, String> categoriesCheck = new LinkedHashMap<>(peertubeInformation.getCategories());
        Iterator<Map.Entry<Integer, String>> it = categoriesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (i == position && position > 0) {
                List<Integer> categories = new ArrayList<>();
                categories.add(pair.getKey());
                sepiaSearchVideo.setCategoryOneOf(categories);
                break;
            } else {
                sepiaSearchVideo.setCategoryOneOf(null);
            }
            it.remove();
            i++;
        }
    }

    private void updateLicensePosition(int position) {
        LinkedHashMap<Integer, String> licensesCheck = new LinkedHashMap<>(peertubeInformation.getLicences());
        Iterator<Map.Entry<Integer, String>> it = licensesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (i == position && position > 0) {
                List<Integer> licenses = new ArrayList<>();
                licenses.add(pair.getKey());
                sepiaSearchVideo.setLicenceOneOf(licenses);
                break;
            } else {
                sepiaSearchVideo.setLicenceOneOf(null);
            }
            it.remove();
            i++;
        }
    }

}
