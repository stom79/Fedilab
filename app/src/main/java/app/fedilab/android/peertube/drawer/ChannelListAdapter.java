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

import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerChannelPeertubeBinding;
import app.fedilab.android.peertube.activities.AccountActivity;
import app.fedilab.android.peertube.activities.ShowChannelActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData.Channel;
import app.fedilab.android.peertube.helper.Helper;


public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Channel> channels;
    public AllChannelRemoved allChannelRemoved;
    public EditAlertDialog editAlertDialog;
    private Context context;


    public ChannelListAdapter(List<Channel> channels) {
        this.channels = channels;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerChannelPeertubeBinding itemBinding = DrawerChannelPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ChannelViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ChannelViewHolder holder = (ChannelViewHolder) viewHolder;
        final Channel channel = channels.get(position);
        holder.binding.accountDn.setText(channel.getDisplayName());
        holder.binding.accountAc.setText(String.format("@%s", channel.getAcct()));
        if (channel.getDescription() == null) {
            channel.setDescription("");
        }

        //Profile picture
        Helper.loadAvatar(context, channel, holder.binding.accountPp);

        if (!isMyChannel(channel)) {
            holder.binding.moreActions.setVisibility(View.GONE);
        }
        holder.binding.moreActions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.moreActions);
            popup.getMenuInflater()
                    .inflate(R.menu.playlist_menu_peertube, popup.getMenu());
            if (channels.size() == 1) {
                popup.getMenu().findItem(R.id.action_delete).setEnabled(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(context.getString(R.string.delete_channel) + ": " + channel.getName());
                    builder.setMessage(context.getString(R.string.action_channel_confirm_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                new Thread(() -> new RetrofitPeertubeAPI(context).post(RetrofitPeertubeAPI.ActionType.DELETE_CHANNEL, channel.getName(), null)).start();
                                channels.remove(channel);
                                notifyItemRemoved(position);
                                if (channels.size() == 0) {
                                    allChannelRemoved.onAllChannelRemoved();
                                }
                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (itemId == R.id.action_edit) {
                    if (context instanceof AccountActivity) {
                        editAlertDialog.show(channel);
                    }
                }
                return true;
            });
            popup.show();
        });

        holder.binding.accountPp.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowChannelActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("channel", channel);
            intent.putExtras(b);
            context.startActivity(intent);
        });


    }


    private boolean isMyChannel(Channel channel) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (channel.getOwnerAccount() == null) {
            return true;
        }
        String channeIdOwner = channel.getOwnerAccount().getId();
        String channeInstanceOwner = channel.getOwnerAccount().getHost();
        String instanceShar = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        String userIdShar = sharedpreferences.getString(PREF_USER_ID, null);
        if (channeIdOwner != null && channeInstanceOwner != null && instanceShar != null && userIdShar != null) {
            return channeIdOwner.compareTo(userIdShar) == 0 && channeInstanceOwner.compareTo(instanceShar) == 0;
        } else {
            return false;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }


    public interface AllChannelRemoved {
        void onAllChannelRemoved();
    }

    public interface EditAlertDialog {
        void show(Channel channel);
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        DrawerChannelPeertubeBinding binding;

        ChannelViewHolder(DrawerChannelPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}