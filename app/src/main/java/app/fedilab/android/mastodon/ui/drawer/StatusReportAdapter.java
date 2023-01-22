package app.fedilab.android.mastodon.ui.drawer;
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

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import app.fedilab.android.databinding.DrawerAdminStatusReportBinding;

public class StatusReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<String> mData;


    public StatusReportAdapter(List<String> data) {
        this.mData = data;
    }


    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        DrawerAdminStatusReportBinding itemBinding = DrawerAdminStatusReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusReportViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        String content;
        StatusReportViewHolder holder = (StatusReportViewHolder) viewHolder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            content = Html.fromHtml(mData.get(position), Html.FROM_HTML_MODE_LEGACY).toString();
        else
            content = Html.fromHtml(mData.get(position)).toString();
        holder.binding.reportContent.setText(content);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class StatusReportViewHolder extends RecyclerView.ViewHolder {
        DrawerAdminStatusReportBinding binding;

        StatusReportViewHolder(DrawerAdminStatusReportBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}