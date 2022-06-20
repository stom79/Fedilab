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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.databinding.DrawerCacheBinding;
import app.fedilab.android.helper.CacheHelper;
import app.fedilab.android.helper.MastodonHelper;


public class CacheAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static ProfileActivity.action doAction;
    private final List<BaseAccount> accountList;
    private Context context;

    public CacheAdapter(List<BaseAccount> accountList) {
        this.accountList = accountList;
    }


    public int getCount() {
        return accountList.size();
    }

    public BaseAccount getItem(int position) {
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
        BaseAccount account = accountList.get(position);
        AccountCacheViewHolder holder = (AccountCacheViewHolder) viewHolder;
        MastodonHelper.loadPPMastodon(holder.binding.pp, account.mastodon_account);
        holder.binding.acct.setText(String.format("@%s@%s", account.mastodon_account.username, account.instance));
        holder.binding.displayName.setText(account.mastodon_account.display_name);
        CacheHelper.getTimelineValues(context, account, countStatuses -> {
            if (countStatuses != null && countStatuses.size() == 3) {
                holder.binding.homeCount.setText(String.valueOf(countStatuses.get(0)));
                holder.binding.otherCount.setText(String.valueOf(countStatuses.get(1)));
                holder.binding.draftCount.setText(String.valueOf(countStatuses.get(2)));
            }
        });
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