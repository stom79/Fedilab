package app.fedilab.android.ui.drawer;
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.HashTagActivity;
import app.fedilab.android.client.mastodon.entities.History;
import app.fedilab.android.client.mastodon.entities.Tag;
import app.fedilab.android.databinding.DrawerTagBinding;
import app.fedilab.android.helper.Helper;

public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Tag> tagList;
    private Context context;

    public TagAdapter(List<Tag> tagList) {
        this.tagList = tagList;
    }


    public static void tagManagement(Context context, TagViewHolder tagViewHolder, Tag tag) {
        tagViewHolder.binding.tagName.setText(String.format("#%s", tag.name));

        List<Entry> trendsEntry = new ArrayList<>();

        List<History> historyList = tag.history;

        int stat = 0;


        for (History history : historyList) {
            trendsEntry.add(0, new Entry(Float.parseFloat(history.day), Float.parseFloat(history.uses)));
            stat += Integer.parseInt(history.accounts);
        }
        tagViewHolder.binding.tagStats.setText(context.getString(R.string.talking_about, stat));
        LineDataSet dataTrending = new LineDataSet(trendsEntry, context.getString(R.string.trending));
        dataTrending.setColor(ContextCompat.getColor(context, R.color.cyanea_accent_reference));
        dataTrending.setValueTextColor(ContextCompat.getColor(context, R.color.cyanea_accent_reference));
        dataTrending.setFillColor(ContextCompat.getColor(context, R.color.cyanea_accent_reference));
        dataTrending.setDrawValues(false);
        dataTrending.setDrawFilled(true);
        dataTrending.setDrawCircles(false);
        dataTrending.setDrawCircleHole(false);
        tagViewHolder.binding.chart.getAxis(YAxis.AxisDependency.LEFT).setEnabled(false);
        tagViewHolder.binding.chart.getAxis(YAxis.AxisDependency.RIGHT).setEnabled(false);
        tagViewHolder.binding.chart.getXAxis().setEnabled(false);
        tagViewHolder.binding.chart.getLegend().setEnabled(false);
        tagViewHolder.binding.chart.setTouchEnabled(false);
        dataTrending.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        Description description = tagViewHolder.binding.chart.getDescription();
        description.setEnabled(false);
        List<ILineDataSet> dataSets = new ArrayList<>();


        dataSets.add(dataTrending);

        LineData data = new LineData(dataSets);
        tagViewHolder.binding.chart.setData(data);
        tagViewHolder.binding.chart.invalidate();


        tagViewHolder.binding.getRoot().setOnClickListener(v1 -> {
            Intent intent = new Intent(context, HashTagActivity.class);
            Bundle b = new Bundle();
            b.putString(Helper.ARG_SEARCH_KEYWORD, tag.name.trim());
            intent.putExtras(b);
            context.startActivity(intent);
        });
    }

    public int getCount() {
        return tagList.size();
    }

    public Tag getItem(int position) {
        return tagList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerTagBinding itemBinding = DrawerTagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TagViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Tag tag = tagList.get(position);
        TagViewHolder holder = (TagViewHolder) viewHolder;
        tagManagement(context, holder, tag);
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }


    public static class TagViewHolder extends RecyclerView.ViewHolder {
        DrawerTagBinding binding;

        TagViewHolder(DrawerTagBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
