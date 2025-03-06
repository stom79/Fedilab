package app.fedilab.android.mastodon.ui.drawer.admin;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAdminAccountBinding;
import app.fedilab.android.mastodon.activities.admin.AdminAccountActivity;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;


public class AdminAccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AdminAccount> adminAccountList;
    private Context context;

    public AdminAccountAdapter(List<AdminAccount> adminAccountList) {
        this.adminAccountList = adminAccountList;
    }

    public int getCount() {
        return adminAccountList.size();
    }

    public AdminAccount getItem(int position) {
        return adminAccountList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerAdminAccountBinding itemBinding = DrawerAdminAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountAdminViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        AdminAccount adminAccount = adminAccountList.get(position);
        AccountAdminViewHolder holder = (AccountAdminViewHolder) viewHolder;

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }

        MastodonHelper.loadPPMastodon(holder.binding.pp, adminAccount.account);
        holder.binding.cardviewContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminAccountActivity.class);
            Bundle args = new Bundle();
            args.putSerializable(Helper.ARG_ACCOUNT, adminAccount);
            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        });
        holder.binding.username.setText(adminAccount.account.display_name);
        holder.binding.acct.setText(String.format(Locale.getDefault(), "@%s", adminAccount.account.acct));
        holder.binding.postCount.setText(String.valueOf(adminAccount.account.statuses_count));
        holder.binding.followersCount.setText(String.valueOf(adminAccount.account.followers_count));
        holder.binding.email.setText(adminAccount.email);
        if (adminAccount.ip != null) {
            holder.binding.ip.setText(adminAccount.ip);
        } else if (adminAccount.ips != null && !adminAccount.ips.isEmpty()) {
            holder.binding.lastActive.setText(Helper.shortDateToString(adminAccount.ips.get(0).used_at));
            holder.binding.ip.setText(adminAccount.ips.get(0).ip);
        } else {
            holder.binding.lastActive.setText(Helper.shortDateToString(adminAccount.created_at));
        }

    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return adminAccountList.size();
    }


    public static class AccountAdminViewHolder extends RecyclerView.ViewHolder {
        DrawerAdminAccountBinding binding;

        AccountAdminViewHolder(DrawerAdminAccountBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}