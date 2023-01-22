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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerStatusHistoryBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;


public class StatusHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Status> statuses;
    private Context context;

    public StatusHistoryAdapter(List<Status> statusList) {
        statuses = statusList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusHistoryBinding itemBinding = DrawerStatusHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusHistoryViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        StatusHistoryViewHolder holder = (StatusHistoryViewHolder) viewHolder;
        Status status = statuses.get(position);
        holder.binding.statusContent.setText(
                status.getSpanContent(context,
                        new WeakReference<>(holder.binding.statusContent), null),
                TextView.BufferType.SPANNABLE);
        if (status.spoiler_text != null && !status.spoiler_text.trim().isEmpty()) {
            holder.binding.spoiler.setVisibility(View.VISIBLE);
            holder.binding.spoiler.setText(
                    status.getSpanSpoiler(context,
                            new WeakReference<>(holder.binding.spoiler), null),
                    TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.spoiler.setVisibility(View.GONE);
            holder.binding.spoiler.setText(null);
        }
        MastodonHelper.loadPPMastodon(holder.binding.avatar, status.account);
        if (status.account != null) {
            holder.binding.displayName.setText(
                    status.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.binding.displayName)),
                    TextView.BufferType.SPANNABLE);
            holder.binding.username.setText(String.format("@%s", status.account.acct));
        }

        if (position == 0) {
            holder.binding.dateModif.setText(context.getString(R.string.created_message_at, Helper.dateDiffFull(status.created_at)));
        } else {
            holder.binding.dateModif.setText(context.getString(R.string.edited_message_at, Helper.dateDiffFull(status.created_at)));
        }

    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }

    public static class StatusHistoryViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusHistoryBinding binding;

        StatusHistoryViewHolder(DrawerStatusHistoryBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
