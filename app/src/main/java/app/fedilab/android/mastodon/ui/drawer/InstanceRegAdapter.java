package app.fedilab.android.mastodon.ui.drawer;
/* Copyright 2021 Thomas Schneider
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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerInstanceRegBinding;
import app.fedilab.android.mastodon.client.entities.api.JoinMastodonInstance;
import app.fedilab.android.mastodon.helper.Helper;


public class InstanceRegAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<JoinMastodonInstance> joinMastodonInstanceList;
    public ActionClick actionClick;
    private Context context;

    public InstanceRegAdapter(List<JoinMastodonInstance> joinMastodonInstanceList) {
        this.joinMastodonInstanceList = joinMastodonInstanceList;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        JoinMastodonInstance joinMastodonInstance = joinMastodonInstanceList.get(position);

        ViewHolder holder = (ViewHolder) viewHolder;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }
        holder.binding.instanceCountUser.setText(context.getString(R.string.users, Helper.withSuffix(joinMastodonInstance.total_users)));
        holder.binding.instanceDescription.setText(joinMastodonInstance.description);
        holder.binding.instanceHost.setText(joinMastodonInstance.domain);
        holder.binding.instanceVersion.setText(String.format("%s - %s", joinMastodonInstance.categories, joinMastodonInstance.version));
        Glide.with(context)
                .load(joinMastodonInstance.proxied_thumbnail)
                .apply(new RequestOptions().transform(new FitCenter(), new RoundedCorners(10)))
                .into(holder.binding.instancePp);

        holder.binding.getRoot().setOnClickListener(v -> actionClick.instance(position));

        holder.binding.watchTrendig.setOnClickListener(v -> actionClick.trends(position));
    }

    public int getCount() {
        return joinMastodonInstanceList.size();
    }

    public JoinMastodonInstance getItem(int position) {
        return joinMastodonInstanceList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerInstanceRegBinding itemBinding = DrawerInstanceRegBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return joinMastodonInstanceList.size();
    }

    public interface ActionClick {
        void instance(int position);

        void trends(int position);
    }

    public interface RecyclerViewClickListener {
        void recyclerViewListClicked(View v, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerInstanceRegBinding binding;

        ViewHolder(DrawerInstanceRegBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}