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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerFilterBinding;
import app.fedilab.android.mastodon.activities.FilterActivity;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.viewmodel.mastodon.FiltersVM;


public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {

    private final List<Filter> filters;
    private final FilterAdapter filterAdapter;
    public Delete delete;
    private Context context;

    public FilterAdapter(List<Filter> filters) {
        this.filters = filters;
        this.filterAdapter = this;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    @NonNull
    @Override
    public FilterAdapter.FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerFilterBinding itemBinding = DrawerFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FilterAdapter.FilterViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        Filter filter = filters.get(position);
        if (filter.title != null) {
            holder.binding.filterWord.setText(filter.title);
        }
        StringBuilder contextString = new StringBuilder();
        if (filter.context != null)
            for (String ct : filter.context)
                contextString.append(ct).append(" ");
        holder.binding.filterContext.setText(contextString.toString());
        holder.binding.editFilter.setOnClickListener(v -> FilterActivity.addEditFilter(context, filter, filter1 -> {
            if (filter1 != null && BaseMainActivity.mainFilters.size() > position) {
                BaseMainActivity.mainFilters.get(position).context = filter1.context;
                BaseMainActivity.mainFilters.get(position).expires_at = filter1.expires_at;
                BaseMainActivity.mainFilters.get(position).filter_action = filter1.filter_action;
                BaseMainActivity.mainFilters.get(position).keywords = filter1.keywords;
                BaseMainActivity.mainFilters.get(position).title = filter1.title;
                filterAdapter.notifyItemChanged(position);
            }
        }));
        holder.binding.deleteFilter.setOnClickListener(v -> {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.action_filter_delete);
            builder.setMessage(R.string.action_lists_confirm_delete);
            builder.setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        FiltersVM filtersVM = new ViewModelProvider((ViewModelStoreOwner) context).get(FiltersVM.class);
                        filtersVM.removeFilter(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filter.id);
                        filters.remove(filter);
                        if (filters.size() == 0) {
                            delete.allFiltersDeleted();
                        }
                        filterAdapter.notifyItemRemoved(holder.getAbsoluteAdapterPosition());
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }


    public interface FilterAction {
        void callback(Filter filter);
    }

    public interface Delete {
        void allFiltersDeleted();
    }

    public static class FilterViewHolder extends RecyclerView.ViewHolder {
        DrawerFilterBinding binding;

        FilterViewHolder(DrawerFilterBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}