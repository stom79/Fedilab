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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.peertube.activities.ShowAccountActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData.Account;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import es.dmoral.toasty.Toasty;


public class AccountsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Account> accounts;
    private final AccountsListAdapter accountsListAdapter;
    private final RetrofitPeertubeAPI.DataType type;
    public AllAccountsRemoved allAccountsRemoved;
    private Context context;

    public AccountsListAdapter(RetrofitPeertubeAPI.DataType type, List<Account> accounts) {
        this.accounts = accounts;
        this.accountsListAdapter = this;
        this.type = type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        return new ViewHolder(layoutInflater.inflate(R.layout.drawer_account, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final ViewHolder holder = (ViewHolder) viewHolder;
        final Account account = accounts.get(position);
        if (type == RetrofitPeertubeAPI.DataType.MUTED) {
            holder.account_action.setOnClickListener(v -> {
                PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                viewModel.post(RetrofitPeertubeAPI.ActionType.UNMUTE, account.getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.UNMUTE, apiResponse, account.getAcct()));
            });
        } else {
            holder.account_action.hide();
        }

        holder.account_dn.setText(account.getDisplayName());
        holder.account_ac.setText(String.format("@%s", account.getAcct()));
        if (account.getDescription() == null) {
            account.setDescription("");
        }
        //Profile picture
        Helper.loadAvatar(context, account, holder.account_pp);
        //Follow button
        if (type == RetrofitPeertubeAPI.DataType.MUTED) {
            holder.account_action.show();
            holder.account_action.setImageResource(R.drawable.ic_baseline_volume_mute_24);
        }

        holder.account_pp.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShowAccountActivity.class);
            Bundle b = new Bundle();
            b.putParcelable("account", account);
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
            for (Account account : accounts) {
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


    private static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView account_pp;
        TextView account_ac;
        TextView account_dn;
        FloatingActionButton account_action;
        LinearLayout account_container;

        ViewHolder(View itemView) {
            super(itemView);
            account_pp = itemView.findViewById(R.id.account_pp);
            account_dn = itemView.findViewById(R.id.account_dn);
            account_ac = itemView.findViewById(R.id.account_ac);
            account_action = itemView.findViewById(R.id.account_action);
            account_container = itemView.findViewById(R.id.account_container);
        }
    }

}