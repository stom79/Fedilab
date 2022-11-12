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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.activities.AccountReportActivity;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.AdminReport;
import app.fedilab.android.databinding.DrawerReportBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;


public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<AdminReport> reports;
    private Context context;


    public ReportAdapter(List<AdminReport> reports) {
        this.reports = reports;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerReportBinding itemBinding = DrawerReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReportViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ReportViewHolder holder = (ReportViewHolder) viewHolder;
        AdminReport report = reports.get(position);
        Account account = report.account.account;
        Account target_account = report.target_account.account;
        if (account.display_name == null || account.display_name.trim().equals("")) {
            if (account.display_name != null && !account.display_name.trim().equals(""))
                holder.binding.accountDnReporter.setText(account.display_name);
            else
                holder.binding.accountDnReporter.setText(account.username.replace("@", ""));
        } else
            holder.binding.accountDnReporter.setText(account.display_name, TextView.BufferType.SPANNABLE);


        holder.binding.accountDn.setText(
                report.account.account.getSpanDisplayName(context,
                        new WeakReference<>(holder.binding.accountDn)),
                TextView.BufferType.SPANNABLE);

        MastodonHelper.loadPPMastodon(holder.binding.accountPp, target_account);
        MastodonHelper.loadPPMastodon(holder.binding.accountPpReporter, account);
        if (target_account.acct != null) {
            holder.binding.accountAc.setText(target_account.acct);
        }

        holder.binding.reportComment.setText(report.comment);

        if (report.statuses != null) {
            holder.binding.reportNumberStatus.setText(String.valueOf(report.statuses.size()));
        } else {
            holder.binding.reportNumberStatus.setText("0");
        }

        holder.binding.mainContainer.setOnClickListener(view -> {
            Intent intent = new Intent(context, AccountReportActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_REPORT, report);
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
        return reports.size();
    }


    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        DrawerReportBinding binding;

        ReportViewHolder(DrawerReportBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}