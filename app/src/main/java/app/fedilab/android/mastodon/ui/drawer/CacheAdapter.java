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
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.databinding.DrawerCacheBinding;
import app.fedilab.android.mastodon.client.entities.app.CacheAccount;
import app.fedilab.android.mastodon.helper.CacheHelper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.peertube.helper.Helper;


public class CacheAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<CacheAccount> accountList;
    private Context context;

    public CacheAdapter(List<CacheAccount> accountList) {
        this.accountList = accountList;
    }


    public int getCount() {
        return accountList.size();
    }

    public CacheAccount getItem(int position) {
        return accountList.get(position);
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        DrawerCacheBinding itemBinding = DrawerCacheBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountCacheViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        CacheAccount cacheAccount = accountList.get(position);
        AccountCacheViewHolder holder = (AccountCacheViewHolder) viewHolder;

        if (cacheAccount.account.mastodon_account != null) {
            MastodonHelper.loadPPMastodon(holder.binding.pp, cacheAccount.account.mastodon_account);
            holder.binding.acct.setText(String.format("@%s@%s", cacheAccount.account.mastodon_account.username, cacheAccount.account.instance));
            holder.binding.displayName.setText(cacheAccount.account.mastodon_account.display_name);
        } else if (cacheAccount.account.peertube_account != null) {
            Helper.loadAvatar(context, cacheAccount.account.peertube_account, holder.binding.pp);
            holder.binding.acct.setText(String.format("@%s@%s", cacheAccount.account.peertube_account.getUsername(), cacheAccount.account.instance));
            holder.binding.displayName.setText(cacheAccount.account.peertube_account.getDisplayName());
        }

        CacheHelper.getTimelineValues(context, cacheAccount.account, countStatuses -> {
            if (countStatuses != null && countStatuses.size() == 3) {
                holder.binding.homeCount.setText(String.valueOf(countStatuses.get(0)));
                holder.binding.otherCount.setText(String.valueOf(countStatuses.get(1)));
                holder.binding.draftCount.setText(String.valueOf(countStatuses.get(2)));
            }
        });
        holder.binding.homeCount.setText(String.valueOf(cacheAccount.home_cache_count));
        holder.binding.otherCount.setText(String.valueOf(cacheAccount.other_cache_count));
        holder.binding.draftCount.setText(String.valueOf(cacheAccount.draft_count));

        holder.binding.labelHomeTimelineCacheCount.setChecked(cacheAccount.clear_home);
        holder.binding.labelTimelinesCacheCount.setChecked(cacheAccount.clear_other);
        holder.binding.labelDraftsCount.setChecked(cacheAccount.clear_drafts);
        holder.binding.labelHomeTimelineCacheCount.setOnCheckedChangeListener((compoundButton, checked) -> cacheAccount.clear_home = checked);
        holder.binding.labelTimelinesCacheCount.setOnCheckedChangeListener((compoundButton, checked) -> cacheAccount.clear_other = checked);
        holder.binding.labelDraftsCount.setOnCheckedChangeListener((compoundButton, checked) -> cacheAccount.clear_drafts = checked);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }


    public static class AccountCacheViewHolder extends RecyclerView.ViewHolder {
        DrawerCacheBinding binding;

        AccountCacheViewHolder(DrawerCacheBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}