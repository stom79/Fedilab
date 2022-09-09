package app.fedilab.android.ui.drawer;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.DrawerMediaBinding;
import app.fedilab.android.helper.Helper;


public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Status> statuses;
    private Context context;

    public ImageAdapter(List<Status> statuses) {
        this.statuses = statuses;
    }

    public int getCount() {
        return statuses.size();
    }

    public Status getItem(int position) {
        return statuses.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerMediaBinding itemBinding = DrawerMediaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Status status = statuses.get(position);

        final ViewHolder holder = (ViewHolder) viewHolder;

        if (Helper.isValidContextForGlide(context) && status.art_attachment != null) {
            if (status.art_attachment.preview_url != null) {
                Glide.with(context).load(status.art_attachment.preview_url).into(holder.binding.media);
            } else if (status.art_attachment.url != null) {
                Glide.with(context).load(status.art_attachment.url).into(holder.binding.media);
            }
        }
        holder.binding.media.setOnClickListener(v -> {
            Intent mediaIntent = new Intent(context, MediaActivity.class);
            Bundle b = new Bundle();
            b.putInt(Helper.ARG_MEDIA_POSITION, position + 1);
            ArrayList<Attachment> attachmentsTmp = new ArrayList<>();
            for (Status status1 : statuses) {
                attachmentsTmp.add(status1.art_attachment);
            }
            b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(attachmentsTmp));
            mediaIntent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, holder.binding.media, status.media_attachments.get(0).url);
            // start the new activity
            context.startActivity(mediaIntent, options.toBundle());
        });

        holder.binding.media.setOnLongClickListener(v -> {
            Intent intentContext = new Intent(context, ContextActivity.class);
            intentContext.putExtra(Helper.ARG_STATUS, status);
            intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentContext);
            return false;
        });
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerMediaBinding binding;

        public ViewHolder(DrawerMediaBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}