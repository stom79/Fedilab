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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerDomainBlockBinding;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;


public class DomainBlockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<String> domainList;
    private Context context;

    public DomainBlockAdapter(List<String> domainList) {
        this.domainList = domainList;
    }

    public int getCount() {
        return domainList.size();
    }

    public String getItem(int position) {
        return domainList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerDomainBlockBinding itemBinding = DrawerDomainBlockBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DomainBlockViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        String domain = domainList.get(position);
        DomainBlockViewHolder holder = (DomainBlockViewHolder) viewHolder;

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }

        holder.binding.domainName.setText(domain);
        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
        holder.binding.unblockDomain.setOnClickListener(v -> {
            AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context, Helper.dialogStyle());
            alt_bld.setMessage(context.getString(R.string.unblock_domain_confirm, domain));
            alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                accountsVM.removeDomainBlocks(MainActivity.currentInstance, MainActivity.currentToken, domain);
                domainList.remove(position);
                notifyItemRemoved(position);
                dialog.dismiss();
            });
            alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            AlertDialog alert = alt_bld.create();
            alert.show();
        });
    }

    @Override
    public int getItemCount() {
        return domainList.size();
    }


    public static class DomainBlockViewHolder extends RecyclerView.ViewHolder {
        DrawerDomainBlockBinding binding;

        DomainBlockViewHolder(DrawerDomainBlockBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
