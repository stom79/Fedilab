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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.databinding.DrawerFollowBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;


public class AccountFollowRequestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Account> accountList;
    private Context context;

    public AccountFollowRequestAdapter(List<Account> accountList) {
        this.accountList = accountList;
    }

    public int getCount() {
        return accountList.size();
    }

    public Account getItem(int position) {
        return accountList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerFollowBinding itemBinding = DrawerFollowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolderFollow(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Account account = accountList.get(position);
        ViewHolderFollow holderFollow = (ViewHolderFollow) viewHolder;
        MastodonHelper.loadPPMastodon(holderFollow.binding.avatar, account);
        holderFollow.binding.displayName.setText(account.display_name);
        holderFollow.binding.username.setText(String.format("@%s", account.acct));
        holderFollow.binding.rejectButton.setVisibility(View.VISIBLE);
        holderFollow.binding.acceptButton.setVisibility(View.VISIBLE);
        holderFollow.binding.title.setText(R.string.follow_request);
        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
        holderFollow.binding.acceptButton.setOnClickListener(v -> {
            accountsVM.acceptFollow(MainActivity.currentInstance, MainActivity.currentToken, account.id)
                    .observe((LifecycleOwner) context, relationShip -> {
                        accountList.remove(position);
                        notifyItemRemoved(position);
                    });
        });
        holderFollow.binding.rejectButton.setOnClickListener(v -> {
            accountsVM.rejectFollow(MainActivity.currentInstance, MainActivity.currentToken, account.id)
                    .observe((LifecycleOwner) context, relationShip -> {
                        accountList.remove(position);
                        notifyItemRemoved(position);
                    });
        });
        holderFollow.binding.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT, account);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, holderFollow.binding.avatar, context.getString(R.string.activity_porfile_pp));
            // start the new activity
            context.startActivity(intent, options.toBundle());
        });
    }


    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }


    static class ViewHolderFollow extends RecyclerView.ViewHolder {
        DrawerFollowBinding binding;

        ViewHolderFollow(DrawerFollowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}