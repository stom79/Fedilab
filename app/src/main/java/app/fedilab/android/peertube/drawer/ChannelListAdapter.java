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

import static app.fedilab.android.mastodon.helper.Helper.PREF_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
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
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        return new ViewHolder(layoutInflater.inflate(R.layout.drawer_channel_peertube, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final ViewHolder holder = (ViewHolder) viewHolder;
        final Channel channel = channels.get(position);
        holder.account_dn.setText(channel.getDisplayName());
        holder.account_ac.setText(String.format("@%s", channel.getAcct()));
        if (channel.getDescription() == null) {
            channel.setDescription("");
        }

        //Profile picture
        Helper.loadAvatar(context, channel, holder.account_pp);

        if (!isMyChannel(channel)) {
            holder.more_actions.setVisibility(View.GONE);
        }
        holder.more_actions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.more_actions);
            popup.getMenuInflater()
                    .inflate(R.menu.playlist_menu_peertube, popup.getMenu());
            if (channels.size() == 1) {
                popup.getMenu().findItem(R.id.action_delete).setEnabled(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getString(R.string.delete_channel) + ": " + channel.getName());
                    builder.setMessage(context.getString(R.string.action_channel_confirm_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                new Thread(() -> {
                                    new RetrofitPeertubeAPI(context).post(RetrofitPeertubeAPI.ActionType.DELETE_CHANNEL, channel.getName(), null);
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> {
                                        channels.remove(channel);
                                        notifyDataSetChanged();
                                        if (channels.size() == 0) {
                                            allChannelRemoved.onAllChannelRemoved();
                                        }
                                    };
                                    mainHandler.post(myRunnable);
                                }).start();
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

        holder.account_pp.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowChannelActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("channel", channel);
            intent.putExtras(b);
            context.startActivity(intent);
        });


    }


    private boolean isMyChannel(Channel channel) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        String channeIdOwner = channel.getOwnerAccount().getId();
        String channeInstanceOwner = channel.getOwnerAccount().getHost();
        String instanceShar = sharedpreferences.getString(PREF_INSTANCE, null);
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

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView account_pp;
        TextView account_ac;
        TextView account_dn;
        ImageButton more_actions;
        LinearLayout account_container;

        ViewHolder(View itemView) {
            super(itemView);
            account_pp = itemView.findViewById(R.id.account_pp);
            account_dn = itemView.findViewById(R.id.account_dn);
            account_ac = itemView.findViewById(R.id.account_ac);
            more_actions = itemView.findViewById(R.id.more_actions);
            account_container = itemView.findViewById(R.id.account_container);
        }
    }

}