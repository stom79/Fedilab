package app.fedilab.android.ui.drawer;
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.app.ActivityOptionsCompat;

import com.bumptech.glide.Glide;
import com.smarteist.autoimageslider.SliderViewAdapter;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.DrawerSliderBinding;
import app.fedilab.android.helper.Helper;

public class SliderAdapter extends SliderViewAdapter<SliderAdapter.SliderAdapterVH> {

    private final Status status;
    private final List<Attachment> mSliderItems;
    private Context context;

    public SliderAdapter(Status status) {
        this.status = status;
        this.mSliderItems = status.media_attachments;
    }


    public void addItem(Attachment sliderItem) {
        this.mSliderItems.add(sliderItem);
        notifyDataSetChanged();
    }

    @Override
    public SliderAdapterVH onCreateViewHolder(ViewGroup parent) {
        context = parent.getContext();
        DrawerSliderBinding itemBinding = DrawerSliderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SliderAdapterVH(itemBinding);
    }

    @Override
    public void onBindViewHolder(SliderAdapterVH viewHolder, final int position) {

        Attachment sliderItem = mSliderItems.get(position);

        Glide.with(viewHolder.itemView)
                .load(sliderItem.preview_url)
                .centerCrop()
                .into(viewHolder.binding.ivAutoImageSlider);
        viewHolder.itemView.setOnClickListener(v -> {
            Intent mediaIntent = new Intent(context, MediaActivity.class);
            Bundle b = new Bundle();
            b.putInt(Helper.ARG_MEDIA_POSITION, position + 1);
            b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(status.media_attachments));
            mediaIntent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, viewHolder.binding.ivAutoImageSlider, status.media_attachments.get(0).url);
            // start the new activity
            context.startActivity(mediaIntent, options.toBundle());
        });
    }

    @Override
    public int getCount() {
        return mSliderItems.size();
    }

    static class SliderAdapterVH extends ViewHolder {
        DrawerSliderBinding binding;

        SliderAdapterVH(DrawerSliderBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
