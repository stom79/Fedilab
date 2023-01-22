package app.fedilab.android.peertube.drawer;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.peertube.client.MenuItemVideo;
import app.fedilab.android.peertube.client.entities.MenuItemView;
import app.fedilab.android.peertube.databinding.DrawerMenuItemBinding;


public class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MenuItemView> items;
    public ItemAction itemAction;
    MenuItemVideo.actionType actionType;

    public MenuItemAdapter(MenuItemVideo.actionType actionType, List<MenuItemView> items) {
        this.items = items;
        this.actionType = actionType;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerMenuItemBinding itemBinding = DrawerMenuItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {


        final ViewHolder holder = (ViewHolder) viewHolder;

        final MenuItemView item = items.get(i);

        holder.binding.title.setText(item.getLabel());
        holder.binding.radio.setChecked(item.isSelected());
        holder.binding.itemMenuContainer.setOnClickListener(v -> itemAction.which(actionType, item));
        holder.binding.radio.setOnClickListener(v -> itemAction.which(actionType, item));
    }

    public interface ItemAction {
        void which(MenuItemVideo.actionType actionType, MenuItemView item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        DrawerMenuItemBinding binding;

        ViewHolder(DrawerMenuItemBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

    }

}