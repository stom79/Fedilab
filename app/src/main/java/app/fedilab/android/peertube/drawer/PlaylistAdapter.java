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


import static app.fedilab.android.mastodon.helper.Helper.dialogStyle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerPlaylistPeertubeBinding;
import app.fedilab.android.peertube.activities.AllPlaylistsActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;


public class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Playlist> playlists;
    private final boolean locale;
    public AllPlaylistRemoved allPlaylistRemoved;
    private Context context;

    public PlaylistAdapter(List<Playlist> lists, boolean locale) {
        this.playlists = lists;
        this.locale = locale;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerPlaylistPeertubeBinding itemBinding = DrawerPlaylistPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int position) {
        context = viewHolder.itemView.getContext();

        final ViewHolder holder = (ViewHolder) viewHolder;
        final Playlist playlist = playlists.get(position);
        String imgUrl;

        if (locale) {
            imgUrl = "https://" + playlist.getOwnerAccount().getHost() + playlist.getThumbnailPath();
        } else {
            imgUrl = playlist.getThumbnailPath();
        }
        Helper.loadGiF(context, imgUrl, holder.binding.previewPlaylist);

        holder.binding.previewTitle.setText(playlist.getDisplayName());
        if (playlist.getDescription() != null && playlist.getDescription().trim().compareTo("null") != 0 && playlist.getDescription().length() > 0) {
            holder.binding.previewDescription.setText(playlist.getDescription());
            holder.binding.previewDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.previewDescription.setVisibility(View.GONE);
        }
        holder.binding.previewVisibility.setText(playlist.getPrivacy().getLabel());


        if (playlist.getDisplayName().compareTo("Watch later") == 0) {
            holder.binding.playlistMore.setVisibility(View.GONE);
        } else {
            holder.binding.playlistMore.setVisibility(View.VISIBLE);
        }

        holder.binding.playlistMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.playlistMore);
            popup.getMenuInflater()
                    .inflate(R.menu.playlist_menu_peertube, popup.getMenu());
            if (locale) {
                popup.getMenu().findItem(R.id.action_edit).setVisible(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, dialogStyle());
                    builder.setTitle(context.getString(R.string.action_lists_delete) + ": " + playlist.getDisplayName());
                    builder.setMessage(context.getString(R.string.action_lists_confirm_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                playlists.remove(playlist);
                                notifyDataSetChanged();
                                PlaylistsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PlaylistsVM.class);
                                viewModel.manage(PlaylistsVM.action.DELETE_PLAYLIST, playlist, null).observe((LifecycleOwner) context, apiResponse -> manageVIewPlaylists(PlaylistsVM.action.DELETE_PLAYLIST, apiResponse));
                                if (playlists.size() == 0) {
                                    allPlaylistRemoved.onAllPlaylistRemoved();
                                }
                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (itemId == R.id.action_edit) {
                    if (context instanceof AllPlaylistsActivity) {
                        ((AllPlaylistsActivity) context).manageAlert(playlist);
                    }
                }
                return true;
            });
            popup.show();

        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }


    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void manageVIewPlaylists(PlaylistsVM.action actionType, APIResponse apiResponse) {

    }

    public interface AllPlaylistRemoved {
        void onAllPlaylistRemoved();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerPlaylistPeertubeBinding binding;

        ViewHolder(DrawerPlaylistPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}