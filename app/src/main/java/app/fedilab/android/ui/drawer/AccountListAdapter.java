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
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.databinding.DrawerAccountListBinding;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;


public class AccountListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Account> accountList;
    private final List<Account> searchList;
    private final MastodonList mastodonList;
    private Context context;
    private TimelinesVM timelinesVM;

    public AccountListAdapter(MastodonList mastodonList, List<Account> accountList, List<Account> searchList) {
        this.mastodonList = mastodonList;
        this.accountList = accountList;
        this.searchList = searchList;
    }


    public int getCount() {
        return searchList == null ? accountList.size() : searchList.size();
    }

    public Account getItem(int position) {
        return searchList == null ? accountList.get(position) : searchList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        timelinesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(TimelinesVM.class);
        DrawerAccountListBinding itemBinding = DrawerAccountListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountListViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Account account;

        account = getItem(position);
        AccountListViewHolder holder = (AccountListViewHolder) viewHolder;
        MastodonHelper.loadPPMastodon(holder.binding.avatar, account);
        holder.binding.displayName.setText(
                account.getSpanDisplayName(context,
                        new WeakReference<>(holder.binding.displayName)),
                TextView.BufferType.SPANNABLE);
        holder.binding.username.setText(String.format("@%s", account.acct));

        if (searchList != null) {
            if (accountList.contains(account)) {
                holder.binding.listAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_1)));
                holder.binding.listAction.setIconResource(R.drawable.ic_baseline_person_remove_alt_1_24);
                holder.binding.listAction.setOnClickListener(v -> {
                    List<String> ids = new ArrayList<>();
                    ids.add(account.id);
                    timelinesVM.deleteAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id, ids);
                    accountList.remove(account);
                    notifyItemChanged(position);
                });
            } else {
                holder.binding.listAction.setIconResource(R.drawable.ic_baseline_person_add_alt_1_24);
                holder.binding.listAction.setOnClickListener(v -> {
                    accountList.add(0, account);
                    List<String> ids = new ArrayList<>();
                    ids.add(account.id);
                    timelinesVM.addAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id, ids);
                    notifyItemChanged(position);
                });
            }
        } else {
            holder.binding.listAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_1)));
            holder.binding.listAction.setIconResource(R.drawable.ic_baseline_person_remove_alt_1_24);
            holder.binding.listAction.setOnClickListener(v -> {
                accountList.remove(account);
                List<String> ids = new ArrayList<>();
                ids.add(account.id);
                timelinesVM.deleteAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id, ids);
                notifyItemRemoved(position);
            });
        }

    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return searchList == null ? accountList.size() : searchList.size();
    }


    public static class AccountListViewHolder extends RecyclerView.ViewHolder {
        DrawerAccountListBinding binding;

        AccountListViewHolder(DrawerAccountListBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}