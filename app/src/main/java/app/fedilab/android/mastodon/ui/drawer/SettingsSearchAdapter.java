package app.fedilab.android.mastodon.ui.drawer;
/* Copyright 2026 Thomas Schneider
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
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerSearchSettingResultBinding;
import app.fedilab.android.mastodon.helper.settings.SettingsSearchEntry;

public class SettingsSearchAdapter extends RecyclerView.Adapter<SettingsSearchAdapter.SearchResultViewHolder> {

    private final List<SettingsSearchEntry> results;
    private final OnSettingClickListener listener;
    private Context context;

    public interface OnSettingClickListener {
        void onSettingClick(SettingsSearchEntry entry);
    }

    public SettingsSearchAdapter(List<SettingsSearchEntry> results, OnSettingClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerSearchSettingResultBinding binding = DrawerSearchSettingResultBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new SearchResultViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SettingsSearchEntry entry = results.get(position);
        holder.binding.title.setText(context.getString(entry.getTitleResId()));
        holder.binding.category.setText(context.getString(R.string.settings) + " > " + context.getString(entry.getCategoryTitleResId()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSettingClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        DrawerSearchSettingResultBinding binding;

        SearchResultViewHolder(DrawerSearchSettingResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
