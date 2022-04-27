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


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.databinding.DrawerAccountReplyBinding;
import app.fedilab.android.helper.MastodonHelper;


public class AccountsReplyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Account> accounts;
    private final boolean[] checked;
    public ActionDone actionDone;


    public AccountsReplyAdapter(List<Account> accounts, List<Boolean> checked) {
        this.accounts = accounts;
        this.checked = new boolean[checked.size()];
        int index = 0;
        for (Boolean val : checked) {
            this.checked[index++] = val;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerAccountReplyBinding itemBinding = DrawerAccountReplyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountReplyViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Account account = accounts.get(position);
        AccountReplyViewHolder holder = (AccountReplyViewHolder) viewHolder;
        holder.binding.checkbox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            try {
                actionDone.onContactClick(isChecked, account.acct);
                checked[position] = isChecked;
            } catch (Exception ignored) {
            }
        });
        holder.binding.checkbox.setChecked(checked[position]);
        holder.binding.accountDn.setText(String.format("@%s", account.acct));
        holder.binding.accountContainer.setOnClickListener(view -> holder.binding.checkbox.performClick());
        //Profile picture
        MastodonHelper.loadPPMastodon(holder.binding.accountPp, account);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }


    public interface ActionDone {
        void onContactClick(boolean isChecked, String acct);
    }

    public static class AccountReplyViewHolder extends RecyclerView.ViewHolder {
        DrawerAccountReplyBinding binding;

        AccountReplyViewHolder(DrawerAccountReplyBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}