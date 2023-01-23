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

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.databinding.DrawerHorizontalAccountPeertubeBinding;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.helper.Helper;


public class AccountsHorizontalListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChannelData.Channel> channels;
    EventListener listener;
    private Context context;

    public AccountsHorizontalListAdapter(List<ChannelData.Channel> channels, EventListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerHorizontalAccountPeertubeBinding itemBinding = DrawerHorizontalAccountPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final ViewHolder holder = (ViewHolder) viewHolder;
        final ChannelData.Channel channel = channels.get(position);

        if (channel.getDisplayName() != null && !channel.getDisplayName().trim().equals(""))
            holder.binding.accountDn.setText(channel.getDisplayName());
        else
            holder.binding.accountDn.setText(channel.getName().replace("@", ""));

        //Profile picture
        Helper.loadAvatar(context, channel, holder.binding.accountPp);
        if (channel.isSelected()) {
            holder.binding.mainContainer.setBackgroundColor(ColorUtils.setAlphaComponent(Helper.fetchAccentColor(context), 50));
        } else {
            holder.binding.mainContainer.setBackgroundColor(Color.TRANSPARENT);
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


    public interface EventListener {
        void click(ChannelData.Channel channel);
    }


    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        DrawerHorizontalAccountPeertubeBinding binding;

        ViewHolder(DrawerHorizontalAccountPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
            itemView.getRoot().setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            ChannelData.Channel channel = channels.get(getAdapterPosition());
            listener.click(channel);
            for (ChannelData.Channel acc : channels) {
                acc.setSelected(acc.getId().compareTo(channel.getId()) == 0);
            }
            notifyItemRangeChanged(0, channels.size());
        }
    }

}