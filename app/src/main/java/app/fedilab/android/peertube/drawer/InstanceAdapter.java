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


import static android.app.Activity.RESULT_OK;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerInstancePeertubeBinding;
import app.fedilab.android.peertube.client.data.InstanceData.Instance;
import app.fedilab.android.peertube.helper.RoundedBackgroundSpan;


public class InstanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Instance> instances;

    private Context context;

    public InstanceAdapter(List<Instance> instances) {
        this.instances = instances;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerInstancePeertubeBinding itemBinding = DrawerInstancePeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final Instance instance = instances.get(position);
        final ViewHolder holder = (ViewHolder) viewHolder;

        if (instance.getShortDescription() != null && instance.getShortDescription().trim().length() > 0) {
            if (instance.isTruncatedDescription()) {
                holder.binding.description.setText(instance.getShortDescription());
                holder.binding.description.setMaxLines(3);
                holder.binding.description.setEllipsize(TextUtils.TruncateAt.END);

            } else {
                holder.binding.description.setText(instance.getShortDescription());
                holder.binding.description.setMaxLines(Integer.MAX_VALUE);
                holder.binding.description.setEllipsize(null);
            }
            SpannableString spannableString;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                spannableString = new SpannableString(Html.fromHtml(instance.getShortDescription(), FROM_HTML_MODE_LEGACY));
            else
                spannableString = new SpannableString(Html.fromHtml(instance.getShortDescription()));
            holder.binding.description.setText(spannableString, TextView.BufferType.SPANNABLE);
            holder.binding.description.setOnClickListener(v -> {
                instance.setTruncatedDescription(!instance.isTruncatedDescription());
                notifyItemChanged(position);
            });
            holder.binding.description.setVisibility(View.VISIBLE);
        } else {
            holder.binding.description.setVisibility(View.GONE);
        }

        holder.binding.name.setText(instance.getName());
        holder.binding.host.setText(instance.getHost());


        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        String between = "";
        if (peertubeInformation != null && peertubeInformation.getCategories() != null) {
            LinkedHashMap<Integer, String> info_cat = new LinkedHashMap<>(peertubeInformation.getCategories());
            if (instance.getCategories() != null && instance.getCategories().size() > 0 && instance.getSpannableStringBuilder() == null) {
                for (int category : instance.getCategories()) {
                    String cat = info_cat.get(category);
                    stringBuilder.append(between);
                    if (cat != null && cat.trim().toLowerCase().compareTo("null") != 0) {
                        if (between.length() == 0) between = "  ";
                        String tag = "  " + cat + "  ";
                        stringBuilder.append(tag);
                        stringBuilder.setSpan(new RoundedBackgroundSpan(context), stringBuilder.length() - tag.length(), stringBuilder.length() - tag.length() + tag.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                instance.setSpannableStringBuilder(stringBuilder);
            }
        }
        if (instance.getSpannableStringBuilder() != null) {
            holder.binding.tags.setText(instance.getSpannableStringBuilder());
        }

        if (peertubeInformation != null && peertubeInformation.getLanguages() != null) {
            LinkedHashMap<String, String> info_lang = new LinkedHashMap<>(peertubeInformation.getLanguages());
            StringBuilder languages = new StringBuilder();
            if (instance.getLanguages() != null && instance.getLanguages().size() > 0) {
                for (String language : instance.getLanguages()) {
                    languages.append(info_lang.get(language)).append(" ");
                }
            }
            if (languages.toString().trim().length() == 0) {
                holder.binding.languages.setVisibility(View.GONE);
            } else {
                holder.binding.languages.setText(languages);
                holder.binding.languages.setVisibility(View.VISIBLE);
            }
        }
        if (instance.getDefaultNSFWPolicy().compareTo("do_not_list") != 0) {
            holder.binding.sensitiveContent.setText(context.getString(R.string.sensitive_content, instance.getDefaultNSFWPolicy()));
            holder.binding.sensitiveContent.setVisibility(View.VISIBLE);
        } else {
            holder.binding.sensitiveContent.setVisibility(View.GONE);
        }
        holder.binding.followersInstance.setText(context.getString(R.string.followers_instance, String.valueOf(instance.getTotalInstanceFollowers())));

        holder.binding.pickup.setOnClickListener(v -> {
            Intent data = new Intent();
            String instanceHost = instance.getHost();
            data.setData(Uri.parse(instanceHost));
            ((Activity) context).setResult(RESULT_OK, data);
            ((Activity) context).finish();
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return instances.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerInstancePeertubeBinding binding;

        ViewHolder(DrawerInstancePeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}