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
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.DrawerFetchMoreBinding;
import app.fedilab.android.databinding.DrawerFollowBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.NotificationsRelatedAccountsBinding;
import app.fedilab.android.helper.CustomEmoji;
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
    private final int NOTIFICATION_FETCH_MORE = 7;
    public FetchMoreCallBack fetchMoreCallBack;
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
        if (notificationList.get(position).isFetchMore) {
            return NOTIFICATION_FETCH_MORE;
        }
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
        } else if (viewType == NOTIFICATION_FETCH_MORE) { //Fetch more button
            DrawerFetchMoreBinding itemBinding = DrawerFetchMoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusAdapter.StatusViewHolder(itemBinding);
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
            if (notification.account.span_display_name == null && notification.account.display_name != null) {
                notification.account.span_display_name = new SpannableString(notification.account.display_name);
            } else {
                notification.account.span_display_name = new SpannableString(notification.account.username);
            }
            CustomEmoji.displayEmoji(context, notification.account.emojis, notification.account.span_display_name, holderFollow.binding.displayName, notification.id, id -> {
                if (!notification.account.emojiFetched) {
                    notification.account.emojiFetched = true;
                    holderFollow.binding.displayName.post(() -> notifyItemChanged(position));
                }
            });
            holderFollow.binding.displayName.setText(notification.account.span_display_name, TextView.BufferType.SPANNABLE);
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
        } else if (viewHolder.getItemViewType() == NOTIFICATION_FETCH_MORE) {
            StatusAdapter.StatusViewHolder holder = (StatusAdapter.StatusViewHolder) viewHolder;
            holder.bindingFetchMore.fetchMoreContainer.setEnabled(!notification.isFetchMoreHidden);
            holder.bindingFetchMore.fetchMoreMin.setOnClickListener(v -> {
                if (position + 1 < notificationList.size()) {
                    //We hide the button
                    notification.isFetchMoreHidden = true;
                    notifyItemChanged(position);
                    fetchMoreCallBack.onClickMin(notificationList.get(position + 1).id, notification.id);
                }
            });
            holder.bindingFetchMore.fetchMoreMax.setOnClickListener(v -> {
                if (position - 1 >= 0) {
                    //We hide the button
                    notification.isFetchMoreHidden = true;
                    notifyItemChanged(position);
                    fetchMoreCallBack.onClickMax(notificationList.get(position - 1).id, notification.id);
                }
            });
        } else {
            StatusAdapter.StatusViewHolder holderStatus = (StatusAdapter.StatusViewHolder) viewHolder;
            holderStatus.bindingNotification.status.typeOfNotification.setVisibility(View.VISIBLE);

            if (getItemViewType(position) == TYPE_MENTION) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_message_24);
            } else if (getItemViewType(position) == TYPE_STATUS) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_message_24);
            } else if (getItemViewType(position) == TYPE_FAVOURITE) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_star_24);
            } else if (getItemViewType(position) == TYPE_REBLOG) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_repeat_24);
            } else if (getItemViewType(position) == TYPE_POLL) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_poll_24);
            }
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
            statusManagement(context, statusesVM, searchVM, holderStatus, this, null, notificationList, notification.status, Timeline.TimeLineEnum.NOTIFICATION, false, true);
            holderStatus.bindingNotification.status.dateShort.setText(Helper.dateDiff(context, notification.created_at));
            holderStatus.bindingNotification.containerTransparent.setAlpha(.3f);
            if (getItemViewType(position) == TYPE_MENTION || getItemViewType(position) == TYPE_STATUS) {
                holderStatus.bindingNotification.status.actionButtons.setVisibility(View.VISIBLE);
                Spannable title = new SpannableString("");
                if (getItemViewType(position) == TYPE_MENTION) {
                    title = new SpannableString(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_mention)));
                } else if (getItemViewType(position) == TYPE_STATUS) {
                    title = new SpannableString(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_status)));
                }
                CustomEmoji.displayEmoji(context, notification.account.emojis, title, holderStatus.binding.displayName, notification.id, id -> {
                    if (!notification.account.emojiFetched) {
                        notification.account.emojiFetched = true;
                        holderStatus.binding.displayName.post(() -> notifyItemChanged(position));
                    }
                });
                holderStatus.bindingNotification.status.displayName.setText(title, TextView.BufferType.SPANNABLE);
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
                Spannable title = new SpannableString("");
                MastodonHelper.loadPPMastodon(holderStatus.binding.avatar, notification.account);
                if (getItemViewType(position) == TYPE_FAVOURITE) {
                    title = new SpannableString(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_favourite)));
                } else if (getItemViewType(position) == TYPE_REBLOG) {
                    title = new SpannableString(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_reblog)));
                } else if (getItemViewType(position) == TYPE_POLL) {
                    title = new SpannableString(context.getString(R.string.notif_poll));
                }
                if (notification.relatedNotifications != null && notification.relatedNotifications.size() > 0) {
                    if (notification.type.equals("favourite")) {
                        holderStatus.bindingNotification.typeOfConcat.setText(R.string.also_favourite_by);
                    } else if (notification.type.equals("reblog")) {
                        holderStatus.bindingNotification.typeOfConcat.setText(R.string.also_boosted_by);
                    }
                    holderStatus.bindingNotification.relatedAccounts.removeAllViews();
                    for (Notification relativeNotif : notification.relatedNotifications) {
                        NotificationsRelatedAccountsBinding notificationsRelatedAccountsBinding = NotificationsRelatedAccountsBinding.inflate(LayoutInflater.from(context));
                        MastodonHelper.loadPPMastodon(notificationsRelatedAccountsBinding.profilePicture, relativeNotif.account);
                        notificationsRelatedAccountsBinding.acc.setText(relativeNotif.account.username);
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
                        holderStatus.bindingNotification.relatedAccounts.addView(notificationsRelatedAccountsBinding.getRoot());
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
                CustomEmoji.displayEmoji(context, notification.account.emojis, title, holderStatus.binding.displayName, notification.id, id -> {
                    if (!notification.account.emojiFetched) {
                        notification.account.emojiFetched = true;
                        holderStatus.binding.displayName.post(() -> notifyItemChanged(position));
                    }
                });
                holderStatus.bindingNotification.status.displayName.setText(title, TextView.BufferType.SPANNABLE);
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

    public interface FetchMoreCallBack {
        void onClickMin(String min_id, String fetchmoreId);

        void onClickMax(String max_id, String fetchmoreId);
    }

    static class ViewHolderFollow extends RecyclerView.ViewHolder {
        DrawerFollowBinding binding;

        ViewHolderFollow(DrawerFollowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}