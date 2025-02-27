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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.app.ActivityOptionsCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.smarteist.autoimageslider.SliderViewAdapter;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerSliderBinding;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import jp.wasabeef.glide.transformations.BlurTransformation;

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
        if (status.sensitive) {
            Glide.with(viewHolder.itemView)
                    .load(sliderItem.preview_url)
                    .fitCenter()
                    .apply(new RequestOptions().transform(new BlurTransformation(50, 3)))
                    .into(viewHolder.binding.ivAutoImageSlider);
        } else {
            Glide.with(viewHolder.itemView)
                    .load(sliderItem.preview_url)
                    .fitCenter()
                    .into(viewHolder.binding.ivAutoImageSlider);
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);

        viewHolder.itemView.setOnClickListener(v -> {
            if (status.sensitive && !expand_media) {
                status.sensitive = false;
                notifyDataSetChanged();
                if (timeout > 0) {
                    new CountDownTimer((timeout * 1000L), 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            status.sensitive = true;
                            notifyDataSetChanged();
                        }
                    }.start();
                }
            } else {
                Intent mediaIntent = new Intent(context, MediaActivity.class);
                Bundle args = new Bundle();
                args.putInt(Helper.ARG_MEDIA_POSITION, position + 1);
                args.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(status.media_attachments));
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    mediaIntent.putExtras(bundle);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) context, viewHolder.binding.ivAutoImageSlider, status.media_attachments.get(0).url);
                    context.startActivity(mediaIntent, options.toBundle());
                });
            }
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
