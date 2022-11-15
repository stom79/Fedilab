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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.client.entities.api.Tag;
import app.fedilab.android.databinding.DrawerListBinding;


public class FollowedTagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Tag> tagList;
    public ActionOnTag actionOnTag;

    public FollowedTagAdapter(List<Tag> tagList) {
        this.tagList = tagList;
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
        DrawerListBinding itemBinding = DrawerListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ListViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Tag tag = tagList.get(position);
        ListViewHolder holder = (ListViewHolder) viewHolder;
        holder.binding.title.setText(tag.name);
        holder.binding.title.setOnClickListener(v -> actionOnTag.click(tag));
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }


    public interface ActionOnTag {
        void click(Tag tag);
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        DrawerListBinding binding;

        ListViewHolder(DrawerListBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
