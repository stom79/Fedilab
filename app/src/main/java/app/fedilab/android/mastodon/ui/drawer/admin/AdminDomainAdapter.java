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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAdminDomainBinding;
import app.fedilab.android.mastodon.activities.admin.AdminDomainBlockActivity;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminDomainBlock;
import app.fedilab.android.mastodon.helper.Helper;


public class AdminDomainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AdminDomainBlock> adminDomainBlockList;
    private Context context;


    public AdminDomainAdapter(List<AdminDomainBlock> adminDomainBlocks) {
        this.adminDomainBlockList = adminDomainBlocks;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerAdminDomainBinding itemBinding = DrawerAdminDomainBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DomainViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        DomainViewHolder holder = (DomainViewHolder) viewHolder;
        AdminDomainBlock adminDomainBlock = adminDomainBlockList.get(position);

        holder.binding.date.setText(Helper.shortDateToString(adminDomainBlock.created_at));
        holder.binding.title.setText(adminDomainBlock.domain);
        String text = adminDomainBlock.severity;
        if (adminDomainBlock.reject_media) {
            text += " - " + context.getString(R.string.reject_media);
        }
        if (adminDomainBlock.reject_reports) {
            text += " - " + context.getString(R.string.reject_reports);
        }
        holder.binding.severity.setText(text);
        holder.binding.mainContainer.setOnClickListener(view -> {
            Intent intent = new Intent(context, AdminDomainBlockActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ADMIN_DOMAINBLOCK, adminDomainBlock);
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
        return adminDomainBlockList.size();
    }


    public static class DomainViewHolder extends RecyclerView.ViewHolder {
        DrawerAdminDomainBinding binding;

        DomainViewHolder(DrawerAdminDomainBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}