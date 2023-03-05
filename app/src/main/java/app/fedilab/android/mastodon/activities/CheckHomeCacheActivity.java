package app.fedilab.android.mastodon.activities;
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


import static app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM.sortAsc;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityCheckHomeCachetBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import es.dmoral.toasty.Toasty;


public class CheckHomeCacheActivity extends BaseBarActivity {


    private ActivityCheckHomeCachetBinding binding;
    private List<Status> statuses;

    private List<Status> statusesDay;
    private ArrayList<String> xVals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCheckHomeCachetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (MainActivity.currentAccount == null || MainActivity.currentAccount.mastodon_account == null) {
            finish();
            return;
        }
        drawCacheGraph(range.ALL);
        binding.chartToggle.setOnCheckedChangeListener((compoundButton, checked) -> drawCacheGraph(checked ? range.DAY : range.ALL));
    }

    private void drawCacheGraph(range myRange) {
        binding.chartContainer.setVisibility(View.GONE);
        binding.progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                if (myRange == range.ALL) {
                    if (statuses == null) {
                        statuses = new StatusCache(this).getHome(MainActivity.currentAccount);
                        sortAsc(statuses);
                    }
                } else if (myRange == range.DAY) {
                    if (statusesDay == null) {
                        statusesDay = new ArrayList<>();
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        for (Status status : statuses) {
                            if (status.created_at.after(calendar.getTime())) {
                                statusesDay.add(status);
                            }
                        }
                    }
                }
                if ((statuses == null || statuses.size() < 2) && myRange == range.ALL) {
                    runOnUiThread(() -> binding.noAction.setVisibility(View.VISIBLE));
                    return;
                }
                if ((statusesDay == null || statusesDay.size() < 2) && myRange == range.DAY) {
                    runOnUiThread(() -> binding.noAction.setVisibility(View.VISIBLE));
                    return;
                }

                List<Status> statusToAnalyse = new ArrayList<>();
                if (myRange == range.ALL) {
                    statusToAnalyse.addAll(statuses);
                } else {
                    statusToAnalyse.addAll(statusesDay);
                }
                Date firstMessageDate = statusToAnalyse.get(0).created_at;
                Date lastMessageDate = statusToAnalyse.get(statusToAnalyse.size() - 1).created_at;
                long diff = lastMessageDate.getTime() - firstMessageDate.getTime();
                int numberOfHour = (int) Math.ceil((double) diff / (1000 * 60 * 60));
                List<GraphElement> graphElements = new ArrayList<>();
                xVals = new ArrayList<>();
                String xDateH;
                SimpleDateFormat df;
                String xDateD;
                String xDate;
                for (int i = 0; i < numberOfHour; i++) {

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(firstMessageDate);
                    calendar.add(Calendar.HOUR, i);

                    xDateH = new SimpleDateFormat("hh", Locale.getDefault()).format(calendar.getTime());
                    df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
                    xDateD = df.format(calendar.getTime());
                    xDate = xDateD + " " + String.format(Locale.getDefault(), "%sh", xDateH);
                    xVals.add(xDate);
                    GraphElement graphElement = new GraphElement();
                    graphElement.dateLabel = xDate;
                    int count = 0;
                    for (Status status : statusToAnalyse) {
                        xDateH = new SimpleDateFormat("hh", Locale.getDefault()).format(status.created_at);
                        df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
                        xDateD = df.format(status.created_at.getTime());
                        xDate = xDateD + " " + String.format(Locale.getDefault(), "%sh", xDateH);
                        if (xDate.equalsIgnoreCase(graphElement.dateLabel)) {
                            count++;
                        }
                    }
                    graphElement.count = count;
                    graphElements.add(graphElement);
                }

                runOnUiThread(() -> {

                    binding.progress.setVisibility(View.GONE);
                    binding.chartContainer.setVisibility(View.VISIBLE);

                    //We loop through cache
                    List<Entry> statusEntry = new ArrayList<>();

                    int inc = 0;
                    for (GraphElement ge : graphElements) {
                        statusEntry.add(new Entry(inc, ge.count));
                        inc++;
                    }
                    List<ILineDataSet> dataSets = new ArrayList<>();
                    LineDataSet dataStatus = new LineDataSet(statusEntry, getString(R.string.cached_messages));
                    dataStatus.setColor(ThemeHelper.getAttColor(this, R.attr.colorPrimary));
                    dataStatus.setFillColor(ThemeHelper.getAttColor(this, R.attr.colorPrimary));
                    dataStatus.setDrawValues(false);
                    dataStatus.setDrawFilled(true);
                    dataStatus.setDrawCircles(false);
                    dataStatus.setDrawCircleHole(false);
                    dataStatus.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSets.add(dataStatus);

                    LineData data = new LineData(dataSets);
                    binding.chart.setData(data);
                    IndexAxisValueFormatter formatter = new IndexAxisValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            if (value < xVals.size()) {
                                return xVals.get((int) value);
                            } else
                                return "";
                        }
                    };
                    binding.chart.setExtraBottomOffset(80);
                    //  binding.chart.getXAxis().setGranularity(1f);
                    binding.chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    binding.chart.getXAxis().setLabelRotationAngle(-45f);
                    binding.chart.getXAxis().setValueFormatter(formatter);
                    binding.chart.getXAxis().setEnabled(true);
                    binding.chart.getXAxis().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart.getAxisLeft().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart.getAxisRight().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart.getLegend().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart.getAxisLeft().setAxisMinimum(0f);
                    binding.chart.getAxisRight().setAxisMinimum(0f);
                    binding.chart.getXAxis().setLabelCount(10, true);
                    binding.chart.getLegend().setEnabled(false);
                    binding.chart.setTouchEnabled(true);
                    Description description = binding.chart.getDescription();
                    description.setEnabled(false);
                    CustomMarkerView mv = new CustomMarkerView(CheckHomeCacheActivity.this, R.layout.custom_marker_view_layout);
                    binding.chart.setMarkerView(mv);

                    binding.chart.invalidate();
                });


            } catch (DBException | NegativeArraySizeException e) {
                binding.noAction.setVisibility(View.VISIBLE);
                Toasty.error(this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        }).start();
    }

    public enum range {
        ALL,
        DAY
    }

    public static class GraphElement {
        String dateLabel;
        int count;

        @Override
        public boolean equals(@Nullable Object obj) {
            boolean same = false;
            if (obj instanceof GraphElement) {
                same = this.dateLabel.equals(((GraphElement) obj).dateLabel);
            }
            return same;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomMarkerView extends MarkerView {

        private final TextView tvContent;
        private MPPointF mOffset;

        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            // find your layout components
            tvContent = findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            tvContent.setText(getString(R.string.messages, (int) e.getY()) + "\r\n" + xVals.get((int) e.getX()));

            // this will perform necessary layouting
            super.refreshContent(e, highlight);
        }

        @Override
        public MPPointF getOffset() {
            if (mOffset == null) {
                // center the marker horizontally and vertically
                mOffset = new MPPointF(-(int) (getWidth() / 2), -getHeight());
            }

            return mOffset;
        }

    }
}
