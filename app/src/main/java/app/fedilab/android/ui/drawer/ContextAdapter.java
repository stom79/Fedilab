package app.fedilab.android.ui.drawer;
/* Copyright 2021 Thomas Schneider
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


import static app.fedilab.android.ui.drawer.StatusAdapter.statusManagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.DrawerStatusBinding;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;


public class ContextAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Status> statusList;
    private final int TYPE_NORMAL = 0;
    private final int TYPE_FOCUSED = 1;
    private Context context;

    public ContextAdapter(List<Status> statusList) {
        this.statusList = statusList;
    }

    public int getCount() {
        return statusList.size();
    }

    public Status getItem(int position) {
        return statusList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return statusList.get(position).isFocused ? TYPE_FOCUSED : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusBinding itemBinding = DrawerStatusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusAdapter.StatusViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Status status = statusList.get(position);
        StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
        SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
        StatusAdapter.StatusViewHolder holder = (StatusAdapter.StatusViewHolder) viewHolder;
        statusManagement(context, statusesVM, searchVM, holder, this, statusList, null, status, Timeline.TimeLineEnum.UNKNOWN, false, true);
        //Hide/Show specific view

    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }


}