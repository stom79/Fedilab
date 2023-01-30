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


import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerStatusScheduledBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.app.ScheduledBoost;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;


public class StatusScheduledAdapter extends RecyclerView.Adapter<StatusScheduledAdapter.StatusScheduledHolder> {
    private final List<ScheduledStatus> scheduledStatuses;
    private final List<StatusDraft> statusDraftList;
    private final List<ScheduledBoost> scheduledBoosts;
    public ScheduledActions scheduledActions;
    private Context context;
    private ScheduledStatus scheduledStatus;
    private StatusDraft statusDraft;
    private ScheduledBoost scheduledBoost;

    public StatusScheduledAdapter(List<ScheduledStatus> scheduledStatuses, List<StatusDraft> statusDraftList, List<ScheduledBoost> scheduledBoosts) {
        this.scheduledStatuses = scheduledStatuses;
        this.statusDraftList = statusDraftList;
        this.scheduledBoosts = scheduledBoosts;
    }

    @NonNull
    @Override
    public StatusScheduledHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusScheduledBinding itemBinding = DrawerStatusScheduledBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusScheduledHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusScheduledHolder holder, int position) {

        scheduledStatus = null;
        statusDraft = null;
        String scheduledDate = null;
        String statusContent = null;
        if (scheduledStatuses != null) {
            scheduledStatus = scheduledStatuses.get(position);
            scheduledDate = Helper.dateToString(scheduledStatus.scheduled_at);
            statusContent = scheduledStatus.params.text;
            if (scheduledStatus.params.in_reply_to_id != null) {
                holder.binding.reply.setVisibility(View.VISIBLE);
            } else {
                holder.binding.reply.setVisibility(View.GONE);
            }
        } else if (statusDraftList != null) {
            statusDraft = statusDraftList.get(position);
            scheduledDate = Helper.dateToString(statusDraft.scheduled_at);
            statusContent = statusDraft.statusDraftList.get(0).text;
            if (statusDraft.statusDraftList.get(0).in_reply_to_id != null) {
                holder.binding.reply.setVisibility(View.VISIBLE);
            } else {
                holder.binding.reply.setVisibility(View.GONE);
            }
        } else if (scheduledBoosts != null) {
            scheduledBoost = scheduledBoosts.get(position);
            scheduledDate = Helper.dateToString(scheduledBoost.scheduledAt);
            if (scheduledBoost.status.in_reply_to_id != null) {
                holder.binding.reply.setVisibility(View.VISIBLE);
            } else {
                holder.binding.reply.setVisibility(View.GONE);
            }
        }

        holder.binding.date.setText(scheduledDate);

        if (scheduledBoost != null) {
            SpannableString statusContentSpan;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                statusContentSpan = new SpannableString(Html.fromHtml(scheduledBoost.status.content, FROM_HTML_MODE_LEGACY));
            else
                statusContentSpan = new SpannableString(Html.fromHtml(statusContent));
            holder.binding.statusContent.setText(statusContentSpan, TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.statusContent.setText(statusContent);
        }

        holder.binding.cardviewContainer.setOnClickListener(v -> {
            if (statusDraft != null) {
                Intent intent = new Intent(context, ComposeActivity.class);
                intent.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
                context.startActivity(intent);
            }

        });
        holder.binding.delete.setOnClickListener(v -> {
            AlertDialog.Builder unfollowConfirm = new MaterialAlertDialogBuilder(context);
            unfollowConfirm.setMessage(context.getString(R.string.remove_scheduled));
            unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            unfollowConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                if (scheduledStatus != null) {
                    StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
                    statusesVM.deleteScheduledStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, scheduledStatus.id)
                            .observe((LifecycleOwner) context, unused -> {
                                if (scheduledStatuses != null) {
                                    scheduledStatuses.remove(scheduledStatus);
                                    if (scheduledStatuses.size() == 0) {
                                        scheduledActions.onAllDeleted();
                                    }
                                    notifyItemRemoved(position);
                                }
                            });
                } else if (statusDraft != null) {
                    try {
                        new StatusDraft(context).removeScheduled(statusDraft);
                        WorkManager.getInstance(context).cancelWorkById(statusDraft.workerUuid);
                        if (statusDraftList != null) {
                            statusDraftList.remove(statusDraft);
                            if (statusDraftList.size() == 0) {
                                scheduledActions.onAllDeleted();
                            }
                            notifyItemRemoved(position);
                        }
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                } else if (scheduledBoost != null) {
                    try {
                        new ScheduledBoost(context).removeScheduled(scheduledBoost);
                        WorkManager.getInstance(context).cancelWorkById(scheduledBoost.workerUuid);
                        if (scheduledBoosts != null) {
                            scheduledBoosts.remove(scheduledBoost);
                            if (scheduledBoosts.size() == 0) {
                                scheduledActions.onAllDeleted();
                            }
                            notifyItemRemoved(position);
                        }
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
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
        if (scheduledStatuses != null) {
            return scheduledStatuses.size();
        } else if (scheduledBoosts != null) {
            return scheduledBoosts.size();
        } else if (statusDraftList != null) {
            return statusDraftList.size();
        }
        return 0;
    }

    public interface ScheduledActions {
        void onAllDeleted();
    }

    static class StatusScheduledHolder extends RecyclerView.ViewHolder {
        DrawerStatusScheduledBinding binding;

        StatusScheduledHolder(DrawerStatusScheduledBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}