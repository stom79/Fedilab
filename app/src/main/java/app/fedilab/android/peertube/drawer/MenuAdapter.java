package app.fedilab.android.peertube.drawer;
/* Copyright 2023 Thomas Schneider
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

import app.fedilab.android.databinding.DrawerMenuPeertubeBinding;
import app.fedilab.android.peertube.client.MenuItemVideo;


public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private final List<MenuItemVideo> menuItemVideos;
    public ItemClicked itemClicked;


    public MenuAdapter(List<MenuItemVideo> menuItemVideos) {
        this.menuItemVideos = menuItemVideos;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return menuItemVideos.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerMenuPeertubeBinding itemBinding = DrawerMenuPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {

        final ViewHolder holder = (ViewHolder) viewHolder;

        final MenuItemVideo menuItemVideo = menuItemVideos.get(i);

        holder.binding.menuIcon.setImageResource(menuItemVideo.getIcon());
        holder.binding.title.setText(menuItemVideo.getTitle());
        holder.binding.itemMenuContainer.setOnClickListener(v -> itemClicked.onItemClicked(menuItemVideo.getAction()));

    }

    public interface ItemClicked {
        void onItemClicked(MenuItemVideo.actionType actionType);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        DrawerMenuPeertubeBinding binding;

        ViewHolder(DrawerMenuPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

    }

}