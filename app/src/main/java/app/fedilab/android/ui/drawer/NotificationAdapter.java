package app.fedilab.android.ui.drawer;
/* Copyright 2021 Thomas Schneider
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


import static app.fedilab.android.ui.drawer.StatusAdapter.statusManagement;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.databinding.DrawerFollowBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.NotificationsRelatedAccountsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;


public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Notification> notificationList;
    private final int TYPE_FOLLOW = 0;
    private final int TYPE_FOLLOW_REQUEST = 1;
    private final int TYPE_MENTION = 2;
    private final int TYPE_REBLOG = 3;
    private final int TYPE_FAVOURITE = 4;
    private final int TYPE_POLL = 5;
    private final int TYPE_STATUS = 6;
    private Context context;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    public int getCount() {
        return notificationList.size();
    }

    public Notification getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        String type = notificationList.get(position).type;
        switch (type) {
            case "follow":
                return TYPE_FOLLOW;
            case "follow_request":
                return TYPE_FOLLOW_REQUEST;
            case "mention":
                return TYPE_MENTION;
            case "reblog":
                return TYPE_REBLOG;
            case "favourite":
                return TYPE_FAVOURITE;
            case "poll":
                return TYPE_POLL;
            case "status":
                return TYPE_STATUS;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TYPE_FOLLOW || viewType == TYPE_FOLLOW_REQUEST) {
            DrawerFollowBinding itemBinding = DrawerFollowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolderFollow(itemBinding);
        } else {
            DrawerStatusNotificationBinding itemBinding = DrawerStatusNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusAdapter.StatusViewHolder(itemBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Notification notification = notificationList.get(position);
        if (getItemViewType(position) == TYPE_FOLLOW || getItemViewType(position) == TYPE_FOLLOW_REQUEST) {
            ViewHolderFollow holderFollow = (ViewHolderFollow) viewHolder;
            MastodonHelper.loadPPMastodon(holderFollow.binding.avatar, notification.account);
            holderFollow.binding.displayName.setText(notification.account.display_name);
            holderFollow.binding.username.setText(String.format("@%s", notification.account.acct));
            if (getItemViewType(position) == TYPE_FOLLOW_REQUEST) {
                holderFollow.binding.rejectButton.setVisibility(View.VISIBLE);
                holderFollow.binding.acceptButton.setVisibility(View.VISIBLE);
                holderFollow.binding.title.setText(R.string.follow_request);
            } else {
                holderFollow.binding.rejectButton.setVisibility(View.GONE);
                holderFollow.binding.acceptButton.setVisibility(View.GONE);
                holderFollow.binding.title.setText(R.string.follow);
            }
            holderFollow.binding.avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle b = new Bundle();
                b.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                intent.putExtras(b);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context, holderFollow.binding.avatar, context.getString(R.string.activity_porfile_pp));
                // start the new activity
                context.startActivity(intent, options.toBundle());
            });
        } else {
            StatusAdapter.StatusViewHolder holderStatus = (StatusAdapter.StatusViewHolder) viewHolder;
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
            statusManagement(context, statusesVM, searchVM, holderStatus, this, null, notificationList, notification.status, Timeline.TimeLineEnum.NOTIFICATION, false);
            holderStatus.bindingNotification.containerTransparent.setAlpha(.3f);
            if (getItemViewType(position) == TYPE_MENTION || getItemViewType(position) == TYPE_STATUS) {
                holderStatus.bindingNotification.status.actionButtons.setVisibility(View.VISIBLE);
                String title = "";
                if (getItemViewType(position) == TYPE_MENTION) {
                    title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_mention));
                } else if (getItemViewType(position) == TYPE_STATUS) {
                    title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_status));
                }
                holderStatus.bindingNotification.status.displayName.setText(title);
                holderStatus.bindingNotification.status.username.setText(String.format("@%s", notification.account.acct));
                holderStatus.bindingNotification.containerTransparent.setAlpha(.1f);
                if (notification.status != null && notification.status.visibility.equalsIgnoreCase("direct")) {
                    holderStatus.bindingNotification.containerTransparent.setVisibility(View.GONE);
                } else {
                    holderStatus.bindingNotification.containerTransparent.setVisibility(View.VISIBLE);
                    holderStatus.bindingNotification.containerTransparent.setAlpha(.1f);
                }
            } else {
                holderStatus.bindingNotification.containerTransparent.setVisibility(View.VISIBLE);
                String title = "";
                MastodonHelper.loadPPMastodon(holderStatus.binding.avatar, notification.account);
                if (getItemViewType(position) == TYPE_FAVOURITE) {
                    title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_favourite));
                } else if (getItemViewType(position) == TYPE_REBLOG) {
                    title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_reblog));
                } else if (getItemViewType(position) == TYPE_POLL) {
                    title = context.getString(R.string.notif_poll);
                }
                if (notification.relatedNotifications != null && notification.relatedNotifications.size() > 0) {
                    holderStatus.bindingNotification.otherAccounts.removeAllViews();
                    for (Notification relativeNotif : notification.relatedNotifications) {
                        NotificationsRelatedAccountsBinding notificationsRelatedAccountsBinding = NotificationsRelatedAccountsBinding.inflate(LayoutInflater.from(context));
                        MastodonHelper.loadPPMastodon(notificationsRelatedAccountsBinding.profilePicture, relativeNotif.account);
                        notificationsRelatedAccountsBinding.acc.setText(relativeNotif.account.acct);
                        notificationsRelatedAccountsBinding.relatedAccountContainer.setOnClickListener(v -> {
                            Intent intent = new Intent(context, ProfileActivity.class);
                            Bundle b = new Bundle();
                            b.putSerializable(Helper.ARG_ACCOUNT, relativeNotif.account);
                            intent.putExtras(b);
                            ActivityOptionsCompat options = ActivityOptionsCompat
                                    .makeSceneTransitionAnimation((Activity) context, notificationsRelatedAccountsBinding.profilePicture, context.getString(R.string.activity_porfile_pp));
                            // start the new activity
                            context.startActivity(intent, options.toBundle());
                        });
                        holderStatus.bindingNotification.otherAccounts.addView(notificationsRelatedAccountsBinding.getRoot());
                    }
                    holderStatus.bindingNotification.otherAccounts.setVisibility(View.VISIBLE);
                } else {
                    holderStatus.bindingNotification.otherAccounts.setVisibility(View.GONE);
                }
                holderStatus.bindingNotification.status.avatar.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                    intent.putExtras(b);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) context, holderStatus.bindingNotification.status.avatar, context.getString(R.string.activity_porfile_pp));
                    // start the new activity
                    context.startActivity(intent, options.toBundle());
                });
                holderStatus.bindingNotification.status.statusUserInfo.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                    intent.putExtras(b);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) context, holderStatus.bindingNotification.status.avatar, context.getString(R.string.activity_porfile_pp));
                    // start the new activity
                    context.startActivity(intent, options.toBundle());
                });
                holderStatus.bindingNotification.status.displayName.setText(title);
                holderStatus.bindingNotification.status.username.setText(String.format("@%s", notification.account.acct));
                holderStatus.bindingNotification.status.actionButtons.setVisibility(View.GONE);
            }
        }
    }


    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }


    static class ViewHolderFollow extends RecyclerView.ViewHolder {
        DrawerFollowBinding binding;

        ViewHolderFollow(DrawerFollowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}