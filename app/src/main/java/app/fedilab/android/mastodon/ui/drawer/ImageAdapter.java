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

import app.fedilab.android.databinding.DrawerMediaBinding;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.fragment.media.FragmentMediaProfile;


public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;

    public ImageAdapter() {
    }

    public int getCount() {
        return FragmentMediaProfile.mediaAttachmentProfile.size();
    }

    public Attachment getItem(int position) {
        return FragmentMediaProfile.mediaAttachmentProfile.get(position);
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

        Attachment attachment = FragmentMediaProfile.mediaAttachmentProfile.get(position);
        final ViewHolder holder = (ViewHolder) viewHolder;
        if (Helper.isValidContextForGlide(context) && attachment != null) {
            if (attachment.preview_url != null) {
                Glide.with(context).load(attachment.preview_url).into(holder.binding.media);
            } else if (attachment.url != null) {
                Glide.with(context).load(attachment.url).into(holder.binding.media);
            }
        }
        holder.binding.media.setOnClickListener(v -> {
            Intent mediaIntent = new Intent(context, MediaActivity.class);
            Bundle b = new Bundle();
            b.putInt(Helper.ARG_MEDIA_POSITION, position + 1);
            b.putBoolean(Helper.ARG_MEDIA_ARRAY_PROFILE, true);
            mediaIntent.putExtras(b);
            ActivityOptionsCompat options = null;
            if (attachment != null) {
                options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context, holder.binding.media, attachment.url);
            } else {
                return;
            }
            // start the new activity
            context.startActivity(mediaIntent, options.toBundle());
        });

        holder.binding.media.setOnLongClickListener(v -> {
            Intent intentContext = new Intent(context, ContextActivity.class);
            if (attachment != null) {
                intentContext.putExtra(Helper.ARG_STATUS, attachment.status);
            } else {
                return false;
            }
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
        return FragmentMediaProfile.mediaAttachmentProfile.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerMediaBinding binding;

        public ViewHolder(DrawerMediaBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}