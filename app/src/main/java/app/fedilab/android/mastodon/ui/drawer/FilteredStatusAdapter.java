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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.databinding.DrawerFilteredStatusBinding;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.client.entities.api.FilterStatus;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;

public class FilteredStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<FilterStatus> filterStatusList;
    private OnFilterStatusDeleteListener deleteListener;
    private Context context;

    public interface OnFilterStatusDeleteListener {
        void onDelete(FilterStatus filterStatus, int position);
    }

    public FilteredStatusAdapter(List<FilterStatus> filterStatusList, OnFilterStatusDeleteListener deleteListener) {
        this.filterStatusList = filterStatusList;
        this.deleteListener = deleteListener;
    }

    public void setDeleteListener(OnFilterStatusDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerFilteredStatusBinding itemBinding = DrawerFilteredStatusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FilteredStatusViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        FilterStatus filterStatus = filterStatusList.get(position);
        FilteredStatusViewHolder holder = (FilteredStatusViewHolder) viewHolder;
        holder.binding.statusId.setText(filterStatus.status_id);
        holder.binding.statusId.setOnClickListener(v -> {
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            statusesVM.getStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filterStatus.status_id)
                    .observe((LifecycleOwner) context, status -> {
                        if (status != null) {
                            Intent intent = new Intent(context, ContextActivity.class);
                            Bundle args = new Bundle();
                            args.putSerializable(Helper.ARG_STATUS, status);
                            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intent.putExtras(bundle);
                                context.startActivity(intent);
                            });
                        }
                    });
        });
        holder.binding.deleteStatus.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(filterStatus, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filterStatusList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < filterStatusList.size()) {
            filterStatusList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class FilteredStatusViewHolder extends RecyclerView.ViewHolder {
        DrawerFilteredStatusBinding binding;

        FilteredStatusViewHolder(DrawerFilteredStatusBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
