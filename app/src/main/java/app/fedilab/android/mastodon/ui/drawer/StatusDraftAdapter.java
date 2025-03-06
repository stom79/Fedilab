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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerStatusDraftBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;


public class StatusDraftAdapter extends RecyclerView.Adapter<StatusDraftAdapter.StatusDraftHolder> {
    private final List<StatusDraft> statusDrafts;
    public DraftActions draftActions;
    private Context context;

    public StatusDraftAdapter(List<StatusDraft> statusDrafts) {
        this.statusDrafts = statusDrafts;
    }

    public int getCount() {
        return statusDrafts.size();
    }

    public StatusDraft getItem(int position) {
        return statusDrafts.get(position);
    }

    @NonNull
    @Override
    public StatusDraftHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusDraftBinding itemBinding = DrawerStatusDraftBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusDraftHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusDraftHolder holder, int position) {
        StatusDraft statusDraft = statusDrafts.get(position);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }

        //--- MAIN CONTENT ---
        if (statusDraft.statusDraftList != null && statusDraft.statusDraftList.size() > 0) {
            holder.binding.statusContent.setText(statusDraft.statusDraftList.get(0).text, TextView.BufferType.SPANNABLE);
            holder.binding.numberOfMessages.setText(String.valueOf(statusDraft.statusDraftList.size()));
            int numberOfMedia = 0;
            for (Status status : statusDraft.statusDraftList) {
                numberOfMedia += status.media_attachments != null ? status.media_attachments.size() : 0;
            }
            holder.binding.numberOfMessages.setText(String.valueOf(statusDraft.statusDraftList.size()));
            holder.binding.numberOfMedia.setText(String.valueOf(numberOfMedia));
        } else {
            holder.binding.statusContent.setText("");
            holder.binding.numberOfMessages.setText("0");
            holder.binding.numberOfMessages.setText("0");
            holder.binding.numberOfMedia.setText("0");
        }
        //--- DATE ---
        holder.binding.date.setText(Helper.dateDiff(context, statusDraft.created_ad));

        holder.binding.cardviewContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComposeActivity.class);
            Bundle args = new Bundle();
            args.putSerializable(Helper.ARG_STATUS_DRAFT, statusDraft);
            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intent.putExtras(bundle);
                context.startActivity(intent);
            });
        });

        holder.binding.delete.setOnClickListener(v -> {
            AlertDialog.Builder unfollowConfirm = new MaterialAlertDialogBuilder(context);
            unfollowConfirm.setMessage(context.getString(R.string.remove_draft));
            unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            unfollowConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                statusDrafts.remove(holder.getAbsoluteAdapterPosition());
                notifyItemRemoved(holder.getAbsoluteAdapterPosition());
                if (statusDrafts.size() == 0) {
                    draftActions.onAllDeleted();
                }
                final StatusDraft statusDraftToDelete = statusDraft;
                new Thread(() -> {
                    try {
                        //Check if there are media in the drafts
                        List<Attachment> attachments = new ArrayList<>();
                        if (statusDraftToDelete.statusDraftList != null) {
                            for (Status drafts : statusDraftToDelete.statusDraftList) {
                                if (drafts.media_attachments != null && drafts.media_attachments.size() > 0) {
                                    attachments.addAll(drafts.media_attachments);
                                }
                            }
                        }
                        //If there are media, we need to remove them first.
                        if (!attachments.isEmpty()) {
                            for (Attachment attachment : attachments) {
                                if (attachment != null && attachment.local_path != null) {
                                    File fileToDelete = new File(attachment.local_path);
                                    if (fileToDelete.exists()) {
                                        //noinspection ResultOfMethodCallIgnored
                                        fileToDelete.delete();
                                    }
                                }
                            }
                        }
                        //Delete the draft
                        new StatusDraft(context).removeDraft(statusDraftToDelete);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }).start();
                dialog.dismiss();
            });
            unfollowConfirm.show();
        });

    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return statusDrafts.size();
    }

    public interface DraftActions {
        void onAllDeleted();
    }

    static class StatusDraftHolder extends RecyclerView.ViewHolder {
        DrawerStatusDraftBinding binding;

        StatusDraftHolder(DrawerStatusDraftBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}