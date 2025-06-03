package app.fedilab.android.mastodon.ui.drawer;
/* Copyright 2025 Thomas Schneider
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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerLinkBinding;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.History;
import app.fedilab.android.mastodon.client.entities.api.Link;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;

public class LinkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Link> linkList;
    private Context context;

    public LinkAdapter(List<Link> linkList) {
        this.linkList = linkList;
    }

    public int getCount() {
        return linkList.size();
    }

    public Link getItem(int position) {
        return linkList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerLinkBinding itemBinding = DrawerLinkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LinkViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Link link = linkList.get(position);
        LinkViewHolder linkViewHolder = (LinkViewHolder) viewHolder;
        linkViewHolder.binding.linkTitle.setText(link.title);
        linkViewHolder.binding.linkDescription.setText(link.description);
        linkViewHolder.binding.linkAuthor.setText(link.provider_name);

        if(link.author_url != null && link.author_url.startsWith("http")) {
            linkViewHolder.binding.linkAuthor.setText(link.provider_name + " (" +link.author_name + ")");
            linkViewHolder.binding.linkAuthor.setOnClickListener(v->{
                Helper.openBrowser(context, link.author_url);
            });
        }

        if(link.image != null) {
            Glide.with(context).load(link.image) .apply(new RequestOptions().transform(new CenterInside(), new RoundedCorners(10))).into(linkViewHolder.binding.linkImage);
            linkViewHolder.binding.linkImage.setVisibility(View.VISIBLE);
            linkViewHolder.binding.linkImage.setOnClickListener(v->{
                Intent intent = new Intent(context, MediaActivity.class);
                Bundle args = new Bundle();
                Attachment attachment = new Attachment();
                attachment.preview_url = link.image;
                attachment.url = link.image;
                attachment.remote_url = link.image;
                attachment.type = "image";
                ArrayList<Attachment> attachments = new ArrayList<>();
                attachments.add(attachment);
                args.putSerializable(Helper.ARG_MEDIA_ARRAY, attachments);
                args.putInt(Helper.ARG_MEDIA_POSITION, 1);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation(((Activity)context), linkViewHolder.binding.linkImage, attachment.url);
                    // start the new activity
                    context.startActivity(intent, options.toBundle());
                });
            });
        } else {
            linkViewHolder.binding.linkImage.setVisibility(View.GONE);
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            linkViewHolder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            linkViewHolder.binding.dividerCard.setVisibility(View.GONE);
        }

        List<Entry> trendsEntry = new ArrayList<>();

        List<History> historyList = link.history;



        if (historyList != null) {
            for (History history : historyList) {
                trendsEntry.add(0, new Entry(Float.parseFloat(history.day), Float.parseFloat(history.uses)));
            }
        }

        LineDataSet dataTrending = new LineDataSet(trendsEntry, context.getString(R.string.trending));
        dataTrending.setDrawValues(false);
        dataTrending.setDrawFilled(true);
        dataTrending.setDrawCircles(false);
        dataTrending.setDrawCircleHole(false);
        linkViewHolder.binding.chart.getAxis(YAxis.AxisDependency.LEFT).setEnabled(false);
        linkViewHolder.binding.chart.getAxis(YAxis.AxisDependency.RIGHT).setEnabled(false);
        linkViewHolder.binding.chart.getXAxis().setEnabled(false);
        linkViewHolder.binding.chart.getLegend().setEnabled(false);
        linkViewHolder.binding.chart.setTouchEnabled(false);
        dataTrending.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        Description description = linkViewHolder.binding.chart.getDescription();
        description.setEnabled(false);
        List<ILineDataSet> dataSets = new ArrayList<>();


        dataSets.add(dataTrending);

        LineData data = new LineData(dataSets);
        linkViewHolder.binding.chart.setData(data);
        linkViewHolder.binding.chart.invalidate();

        linkViewHolder.binding.getRoot().setOnClickListener(v -> Helper.openBrowser(context, link.url));
    }

    @Override
    public int getItemCount() {
        return linkList.size();
    }


    public static class LinkViewHolder extends RecyclerView.ViewHolder {
        DrawerLinkBinding binding;

        LinkViewHolder(DrawerLinkBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
