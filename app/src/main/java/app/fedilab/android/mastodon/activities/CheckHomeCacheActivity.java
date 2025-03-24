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


import static app.fedilab.android.mastodon.helper.Helper.dateDiffFull;
import static app.fedilab.android.mastodon.helper.Helper.dateDiffFullShort;
import static app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM.sortAsc;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
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
import app.fedilab.android.databinding.ActivityCheckHomeCachetBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.TimelineCacheLogs;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import es.dmoral.toasty.Toasty;


public class CheckHomeCacheActivity extends BaseBarActivity {


    private ActivityCheckHomeCachetBinding binding;
    private List<Status> statuses;
    private List<TimelineCacheLogs> timelineCacheLogsList;
    private List<Status> statusesDay;
    private List<TimelineCacheLogs> timelineCacheLogsDayList;
    private ArrayList<String> xVals;

    private ArrayList<String> xVals2;

    private List<TimelineCacheLogs> timelineCacheLogsListToAnalyse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCheckHomeCachetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (Helper.getCurrentAccount(CheckHomeCacheActivity.this) == null || Helper.getCurrentAccount(CheckHomeCacheActivity.this).mastodon_account == null) {
            finish();
            return;
        }
        drawCacheGraph(range.ALL);
        drawCacheLogsGraph(range.ALL);
        binding.chartToggle.setOnCheckedChangeListener((compoundButton, checked) -> {
            drawCacheGraph(checked ? range.DAY : range.ALL);
            drawCacheLogsGraph(checked ? range.DAY : range.ALL);
        });
    }

    private void drawCacheGraph(range myRange) {
        binding.progress.setVisibility(View.VISIBLE);
        binding.chartToggle.setEnabled(false);
        new Thread(() -> {
            try {
                if (myRange == range.ALL) {
                    if (statuses == null) {
                        statuses = new StatusCache(this).getHome(Helper.getCurrentAccount(CheckHomeCacheActivity.this));
                        sortAsc(statuses);
                    }
                } else if (myRange == range.DAY) {
                    if (statusesDay == null && statuses != null) {
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
                    runOnUiThread(() -> binding.chartToggle.setEnabled(true));
                    return;
                }
                if ((statusesDay == null || statusesDay.size() < 2) && myRange == range.DAY) {
                    runOnUiThread(() -> binding.chartToggle.setEnabled(true));
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

                    xDateH = new SimpleDateFormat("HH", Locale.getDefault()).format(calendar.getTime());
                    df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
                    xDateD = df.format(calendar.getTime());
                    xDate = xDateD + " " + String.format(Locale.getDefault(), "%sh", xDateH);
                    xVals.add(xDate);
                    GraphElement graphElement = new GraphElement();
                    graphElement.dateLabel = xDate;
                    int count = 0;
                    for (Status status : statusToAnalyse) {
                        xDateH = new SimpleDateFormat("HH", Locale.getDefault()).format(status.created_at);
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
                    binding.chartToggle.setEnabled(true);
                    binding.progress.setVisibility(View.GONE);

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
                            if (value >= 0 && value < xVals.size()) {
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
                    binding.chart.setMarker(mv);

                    binding.chart.invalidate();
                });


            } catch (DBException | NegativeArraySizeException e) {
                Toasty.error(this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        }).start();
    }


    private void drawCacheLogsGraph(range myRange) {
        binding.progress2.setVisibility(View.VISIBLE);
        new Thread(() -> {
            xVals2 = new ArrayList<>();
            try {
                if (myRange == range.ALL) {
                    if (timelineCacheLogsList == null) {
                        timelineCacheLogsList = new TimelineCacheLogs(this).getHome(Helper.getCurrentAccount(CheckHomeCacheActivity.this));
                    }
                } else if (myRange == range.DAY) {
                    if (timelineCacheLogsDayList == null && timelineCacheLogsList != null) {
                        timelineCacheLogsDayList = new ArrayList<>();
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        for (TimelineCacheLogs timelineCacheLogs : timelineCacheLogsList) {
                            if (timelineCacheLogs.created_at.after(calendar.getTime())) {
                                timelineCacheLogsDayList.add(timelineCacheLogs);
                            }
                        }
                    }
                }
                if ((timelineCacheLogsList == null || timelineCacheLogsList.size() < 2) && myRange == range.ALL) {
                    return;
                }
                if ((timelineCacheLogsDayList == null || timelineCacheLogsDayList.size() < 2) && myRange == range.DAY) {
                    return;
                }

                timelineCacheLogsListToAnalyse = new ArrayList<>();
                if (myRange == range.ALL) {
                    timelineCacheLogsListToAnalyse.addAll(timelineCacheLogsList);
                } else {
                    timelineCacheLogsListToAnalyse.addAll(timelineCacheLogsDayList);
                }
                List<BarEntry> failEntry = new ArrayList<>();
                List<Entry> updateEntry = new ArrayList<>();
                List<Entry> insertEntry = new ArrayList<>();
                List<Entry> frequencyEntry = new ArrayList<>();
                List<Entry> fetchedEntry = new ArrayList<>();
                int inc = 0;
                for (TimelineCacheLogs timelineCacheLogs : timelineCacheLogsListToAnalyse) {
                    //X-Axis
                    //X-Axis
                    String xDate = dateDiffFullShort(timelineCacheLogs.created_at);
                    xVals2.add(xDate);
                    //Entries
                    failEntry.add(new BarEntry(inc, timelineCacheLogs.failed));
                    updateEntry.add(new Entry(inc, timelineCacheLogs.updated));
                    insertEntry.add(new Entry(inc, timelineCacheLogs.inserted));
                    frequencyEntry.add(new Entry(inc, timelineCacheLogs.frequency));
                    fetchedEntry.add(new Entry(inc, timelineCacheLogs.fetched));
                    inc++;
                }


                runOnUiThread(() -> {
                    binding.progress2.setVisibility(View.GONE);

                    LineData lineData = new LineData();
                    BarData barDataFailed = new BarData();

                    BarDataSet dataFailed = new BarDataSet(failEntry, getString(R.string.fails));
                    LineDataSet dataNewMessage = new LineDataSet(insertEntry, getString(R.string.new_messages));
                    LineDataSet dataUpdatedMessage = new LineDataSet(updateEntry, getString(R.string.updated_messages));
                    LineDataSet dataFrequency = new LineDataSet(frequencyEntry, getString(R.string.frequency_minutes));
                    LineDataSet dataFetched = new LineDataSet(fetchedEntry, getString(R.string.total_fetched));

                    dataFailed.setColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.errorColor));
                    dataFailed.setDrawValues(false);
                    dataFailed.setAxisDependency(YAxis.AxisDependency.RIGHT);
                    barDataFailed.addDataSet(dataFailed);

                    dataNewMessage.setColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.decoration_1));
                    dataNewMessage.setFillColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.transparent));
                    dataNewMessage.setDrawValues(false);
                    dataNewMessage.setDrawFilled(true);
                    dataNewMessage.setDrawCircles(false);
                    dataNewMessage.setDrawCircleHole(false);
                    dataNewMessage.setAxisDependency(YAxis.AxisDependency.LEFT);
                    dataNewMessage.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    lineData.addDataSet(dataNewMessage);

                    dataUpdatedMessage.setColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.decoration_2));
                    dataUpdatedMessage.setFillColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.transparent));
                    dataUpdatedMessage.setDrawValues(false);
                    dataUpdatedMessage.setDrawFilled(true);
                    dataUpdatedMessage.setDrawCircles(false);
                    dataUpdatedMessage.setDrawCircleHole(false);
                    dataUpdatedMessage.setAxisDependency(YAxis.AxisDependency.LEFT);
                    dataUpdatedMessage.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    lineData.addDataSet(dataUpdatedMessage);

                    dataFrequency.setColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.decoration_3));
                    dataFrequency.setFillColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.transparent));
                    dataFrequency.setDrawValues(false);
                    dataFrequency.setDrawFilled(true);
                    dataFrequency.setDrawCircles(false);
                    dataFrequency.setDrawCircleHole(false);
                    dataFrequency.setAxisDependency(YAxis.AxisDependency.LEFT);
                    dataFrequency.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    lineData.addDataSet(dataFrequency);


                    dataFetched.setColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.decoration_4));
                    dataFetched.setFillColor(ContextCompat.getColor(CheckHomeCacheActivity.this, R.color.transparent));
                    dataFetched.setDrawValues(false);
                    dataFetched.setDrawFilled(true);
                    dataFetched.setDrawCircles(false);
                    dataFetched.setDrawCircleHole(false);
                    dataFetched.setAxisDependency(YAxis.AxisDependency.LEFT);
                    dataFetched.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    lineData.addDataSet(dataFetched);


                    CombinedData data = new CombinedData();
                    data.setData(barDataFailed);
                    data.setData(lineData);


                    binding.chart2.setData(data);
                    IndexAxisValueFormatter formatter = new IndexAxisValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            if (value >= 0 && value < xVals2.size()) {
                                return xVals2.get((int) value);
                            } else
                                return "";
                        }
                    };
                    binding.chart2.setExtraBottomOffset(80);
                    //  binding.chart.getXAxis().setGranularity(1f);
                    binding.chart2.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    binding.chart2.getXAxis().setLabelRotationAngle(-45f);
                    binding.chart2.getXAxis().setValueFormatter(formatter);
                    binding.chart2.getXAxis().setEnabled(true);
                    binding.chart2.getXAxis().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart2.getAxisLeft().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart2.getAxisRight().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart2.getLegend().setTextColor(ThemeHelper.getAttColor(CheckHomeCacheActivity.this, R.attr.colorOnBackground));
                    binding.chart2.getAxisLeft().setAxisMinimum(0f);
                    binding.chart2.getAxisRight().setAxisMinimum(0f);
                    binding.chart2.getXAxis().setLabelCount(10, true);
                    binding.chart2.getLegend().setEnabled(true);
                    binding.chart2.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
                    binding.chart2.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                    binding.chart2.getLegend().setDrawInside(false);
                    binding.chart2.getLegend().setWordWrapEnabled(true);
                    binding.chart2.setTouchEnabled(true);
                    Description description = binding.chart2.getDescription();
                    description.setEnabled(true);
                    CustomMarkerView2 mv = new CustomMarkerView2(CheckHomeCacheActivity.this, R.layout.custom_marker_view_layout);
                    binding.chart2.setMarker(mv);

                    binding.chart2.invalidate();
                });


            } catch (DBException | NegativeArraySizeException e) {
                Toasty.error(this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            if (xVals.size() > (int) e.getX()) {
                tvContent.setText(getString(R.string.messages, (int) e.getY()));
            }

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


    public class CustomMarkerView2 extends MarkerView {

        private final TextView tvContent;
        private MPPointF mOffset;

        public CustomMarkerView2(Context context, int layoutResource) {
            super(context, layoutResource);

            // find your layout components
            tvContent = findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            if (xVals2.size() > (int) e.getX()) {
                if (timelineCacheLogsListToAnalyse != null && (int) e.getX() < timelineCacheLogsListToAnalyse.size()) {
                    String text = getString(R.string.fail_count, timelineCacheLogsListToAnalyse.get((int) e.getX()).failed) + "\r\n";
                    text += getString(R.string.fetched_count, timelineCacheLogsListToAnalyse.get((int) e.getX()).fetched) + "\r\n";
                    text += getString(R.string.inserted_count, timelineCacheLogsListToAnalyse.get((int) e.getX()).inserted) + "\r\n";
                    text += getString(R.string.updated_count, timelineCacheLogsListToAnalyse.get((int) e.getX()).updated) + "\r\n";
                    text += getString(R.string.frequency_count_minutes, timelineCacheLogsListToAnalyse.get((int) e.getX()).frequency) + "\r\n\r\n";
                    text += dateDiffFull(timelineCacheLogsListToAnalyse.get((int) e.getX()).created_at);
                    tvContent.setText(text);
                } else {
                    tvContent.setText(getString(R.string.messages, (int) e.getY()));
                }
            }

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
