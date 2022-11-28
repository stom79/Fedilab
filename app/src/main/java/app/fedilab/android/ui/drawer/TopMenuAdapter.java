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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.DrawerTopMenuItemBinding;


public class TopMenuAdapter extends RecyclerView.Adapter<TopMenuAdapter.TopMenuHolder> {
    private final List<PinnedTimeline> pinnedTimelines;
    public TopMenuClicked itemListener;
    private Context _mContext;

    public TopMenuAdapter(List<PinnedTimeline> pinnedTimelines) {
        this.pinnedTimelines = pinnedTimelines;
    }

    public int getCount() {
        return pinnedTimelines.size();
    }

    public PinnedTimeline getItem(int position) {
        return pinnedTimelines.get(position);
    }

    @NonNull
    @Override
    public TopMenuHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        _mContext = parent.getContext();
        DrawerTopMenuItemBinding itemBinding = DrawerTopMenuItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TopMenuHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopMenuHolder holder, int position) {

        PinnedTimeline pinnedTimeline = pinnedTimelines.get(position);
        if (pinnedTimeline.displayed) {
            String name = "";
            if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST) {
                name = pinnedTimeline.mastodonList.title;
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.TAG) {
                name = pinnedTimeline.tagTimeline.name;
            } else if (pinnedTimeline.type == Timeline.TimeLineEnum.REMOTE) {
                name = pinnedTimeline.remoteInstance.host;
            }
            holder.binding.name.setText(name);
            holder.binding.getRoot().setVisibility(View.VISIBLE);
        } else {
            holder.binding.getRoot().setVisibility(View.GONE);
        }
        holder.binding.getRoot().setOnClickListener(v -> itemListener.onClick(v, pinnedTimeline, position));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            itemListener.onLongClick(holder.binding.getRoot(), pinnedTimeline, position);
            return true;
        });
        //Manage item decoration below the text
        if (pinnedTimeline.isSelected) {
            holder.binding.underline.setVisibility(View.VISIBLE);
            holder.binding.name.setTextColor(ResourcesCompat.getColor(_mContext.getResources(), R.color.colorAccent, _mContext.getTheme()));
        } else {
            holder.binding.underline.setVisibility(View.GONE);
            int textColor = _mContext.getResources().getColor(android.R.color.primary_text_dark);
            holder.binding.name.setTextColor(textColor);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return pinnedTimelines.size();
    }

    public interface TopMenuClicked {
        void onClick(View v, PinnedTimeline pinnedTimeline, int position);

        void onLongClick(View v, PinnedTimeline pinnedTimeline, int position);
    }

    static class TopMenuHolder extends RecyclerView.ViewHolder {
        DrawerTopMenuItemBinding binding;

        TopMenuHolder(DrawerTopMenuItemBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}