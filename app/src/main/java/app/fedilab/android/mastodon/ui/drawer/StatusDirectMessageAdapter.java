package app.fedilab.android.mastodon.ui.drawer;
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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerStatusChatBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;

public class StatusDirectMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Status> statusList;
    private Context context;
    private RecyclerView mRecyclerView;

    public StatusDirectMessageAdapter(List<Status> data) {
        this.statusList = data;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusChatBinding itemBinding = DrawerStatusChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusChatViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        StatusChatViewHolder holder = (StatusChatViewHolder) viewHolder;
        Status status = statusList.get(position);

        holder.binding.messageContent.setText(
                status.getSpanContent(context,
                        new WeakReference<>(holder.binding.messageContent),
                        () -> mRecyclerView.post(() -> notifyItemChanged(holder.getBindingAdapterPosition()))),
                TextView.BufferType.SPANNABLE);

        MastodonHelper.loadPPMastodon(holder.binding.userPp, status.account);
        holder.binding.date.setText(Helper.longDateToString(status.created_at));
        //Owner account
        int textColor;
        if (status.account.id.equals(MainActivity.currentUserID)) {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_right_tail);
            textColor = R.attr.colorOnPrimary;
        } else {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_left_tail);
            textColor = R.attr.colorOnSecondary;
        }
        holder.binding.date.setTextColor(ThemeHelper.getAttColor(context, textColor));
        holder.binding.messageContent.setTextColor(ThemeHelper.getAttColor(context, textColor));
        holder.binding.userName.setTextColor(ThemeHelper.getAttColor(context, textColor));
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }


    @Override
    public int getItemCount() {
        return statusList.size();
    }

    public static class StatusChatViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusChatBinding binding;

        StatusChatViewHolder(DrawerStatusChatBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}