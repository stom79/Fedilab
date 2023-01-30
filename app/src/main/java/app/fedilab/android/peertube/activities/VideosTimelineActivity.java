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

import static app.fedilab.android.peertube.viewmodel.TimelineVM.TimelineType.HISTORY;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityVideosTimelineBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.TimelineVM;


public class VideosTimelineActivity extends BaseBarActivity {

    private TimelineVM.TimelineType type;
    private DisplayVideosFragment displayVideosFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityVideosTimelineBinding binding = ActivityVideosTimelineBinding.inflate(getLayoutInflater());
        View mainView = binding.getRoot();
        setContentView(mainView);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle b = getIntent().getExtras();
        if (b != null)
            type = (TimelineVM.TimelineType) b.get("type");
        displayVideosFragment = null;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (savedInstanceState == null) {
            displayVideosFragment = new DisplayVideosFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.TIMELINE_TYPE, type);
            displayVideosFragment.setArguments(bundle);
            ft.add(R.id.container, displayVideosFragment).addToBackStack(null).commit();
        }

        if (type == TimelineVM.TimelineType.MY_VIDEOS) {
            setTitle(R.string.my_videos);
        } else if (type == HISTORY) {
            setTitle(R.string.my_history);
            //TODO: uncomment when available
            // binding.historyFilter.setVisibility(View.VISIBLE);
            binding.historyFilterAll.setOnClickListener(v -> historyFilter(null));
            binding.historyFilterToday.setOnClickListener(v -> {
                Calendar cal = GregorianCalendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.getTime();
                historyFilter(cal.getTime());
            });
            binding.historyFilterLast7Days.setOnClickListener(v -> {
                Calendar cal = GregorianCalendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -7);
                cal.getTime();
                historyFilter(cal.getTime());
            });

        } else if (type == TimelineVM.TimelineType.MOST_LIKED) {
            setTitle(R.string.title_most_liked);
        }


    }

    private void historyFilter(Date date) {
        String startDate = null;
        if (date != null) {
            SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            startDate = fmtOut.format(date);
        }
        if (displayVideosFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            displayVideosFragment = new DisplayVideosFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.TIMELINE_TYPE, HISTORY);
            bundle.putSerializable("startDate", startDate);
            displayVideosFragment.setArguments(bundle);
            ft.replace(R.id.container, displayVideosFragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        if (type == HISTORY) {
            getMenuInflater().inflate(R.menu.main_history_peertube, menu);
            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(VideosTimelineActivity.this);
            builder.setTitle(R.string.delete_history);
            builder.setMessage(R.string.delete_history_confirm);
            builder.setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        TimelineVM viewModelFeeds = new ViewModelProvider(VideosTimelineActivity.this).get(TimelineVM.class);
                        viewModelFeeds.deleterHistory().observe(VideosTimelineActivity.this, this::manageVIewVideos);

                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void manageVIewVideos(APIResponse apiResponse) {
        if (type == HISTORY) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            displayVideosFragment = new DisplayVideosFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.TIMELINE_TYPE, HISTORY);
            displayVideosFragment.setArguments(bundle);
            ft.replace(R.id.container, displayVideosFragment);
            ft.addToBackStack(null);
            ft.commit();
        }
    }
}
