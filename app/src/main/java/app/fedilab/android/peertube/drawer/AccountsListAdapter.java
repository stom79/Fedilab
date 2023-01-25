package app.fedilab.android.peertube.drawer;
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAccountPeertubeBinding;
import app.fedilab.android.peertube.activities.ShowAccountActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import es.dmoral.toasty.Toasty;


public class AccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AccountData.PeertubeAccount> accounts;
    private final AccountsListAdapter accountsListAdapter;
    private final RetrofitPeertubeAPI.DataType type;
    public AllAccountsRemoved allAccountsRemoved;
    private Context context;

    public AccountsListAdapter(RetrofitPeertubeAPI.DataType type, List<AccountData.PeertubeAccount> accounts) {
        this.accounts = accounts;
        this.accountsListAdapter = this;
        this.type = type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerAccountPeertubeBinding itemBinding = DrawerAccountPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final AccountViewHolder holder = (AccountViewHolder) viewHolder;
        final AccountData.PeertubeAccount account = accounts.get(position);
        if (type == RetrofitPeertubeAPI.DataType.MUTED) {
            holder.binding.accountAction.setOnClickListener(v -> {
                PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                viewModel.post(RetrofitPeertubeAPI.ActionType.UNMUTE, account.getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.UNMUTE, apiResponse, account.getAcct()));
            });
        } else {
            holder.binding.accountAction.hide();
        }

        holder.binding.accountDn.setText(account.getDisplayName());
        holder.binding.accountAc.setText(String.format("@%s", account.getAcct()));
        if (account.getDescription() == null) {
            account.setDescription("");
        }
        //Profile picture
        Helper.loadAvatar(context, account, holder.binding.accountPp);
        //Follow button
        if (type == RetrofitPeertubeAPI.DataType.MUTED) {
            holder.binding.accountAction.show();
            holder.binding.accountAction.setImageResource(R.drawable.ic_baseline_volume_mute_24);
        }

        holder.binding.accountPp.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowAccountActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("account", account);
            b.putString("accountAcct", account.getAcct());
            intent.putExtras(b);
            context.startActivity(intent);
        });


    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, APIResponse apiResponse, String elementTargeted) {
        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        if (statusAction == RetrofitPeertubeAPI.ActionType.UNMUTE) {
            int position = 0;
            for (AccountData.PeertubeAccount account : accounts) {
                if (account.getAcct().equals(elementTargeted)) {
                    accounts.remove(position);
                    accountsListAdapter.notifyItemRemoved(position);
                    break;
                }
                position++;
            }
            if (accounts.size() == 0 && allAccountsRemoved != null) {
                allAccountsRemoved.onAllAccountsRemoved();
            }
        }
    }


    public interface AllAccountsRemoved {
        void onAllAccountsRemoved();
    }


    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        DrawerAccountPeertubeBinding binding;

        AccountViewHolder(DrawerAccountPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}