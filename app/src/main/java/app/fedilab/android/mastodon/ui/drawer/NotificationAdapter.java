package app.fedilab.android.mastodon.ui.drawer;
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


import static app.fedilab.android.BaseMainActivity.currentNightMode;
import static app.fedilab.android.mastodon.ui.drawer.StatusAdapter.statusManagement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerFollowBinding;
import app.fedilab.android.databinding.DrawerStatusFilteredBinding;
import app.fedilab.android.databinding.DrawerStatusFilteredHideBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.NotificationsRelatedAccountsBinding;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;


public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Notification> notificationList;
    private final int TYPE_FOLLOW = 0;
    private final int TYPE_FOLLOW_REQUEST = 1;
    private final int TYPE_MENTION = 2;
    private final int TYPE_REBLOG = 3;
    private final int TYPE_FAVOURITE = 4;
    private final int TYPE_POLL = 5;
    private final int TYPE_STATUS = 6;
    private final int TYPE_REACTION = 8;
    private final int TYPE_UPDATE = 9;
    private final int TYPE_FILERED = 10;
    private final int TYPE_ADMIN_SIGNUP = 11;
    private final int TYPE_ADMIN_REPORT = 12;
    private final int TYPE_HIDDEN = 13;
    public FetchMoreCallBack fetchMoreCallBack;
    private Context context;
    private RecyclerView mRecyclerView;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    public static void applyColorAccount(Context context, ViewHolderFollow holder) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean customLight = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS), false);
        boolean customDark = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_DARK_COLORS), false);
        int theme_icons_color = -1;
        int theme_statuses_color = -1;
        int theme_text_color = -1;
        int theme_text_header_1_line = -1;
        int theme_text_header_2_line = -1;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) { //LIGHT THEME
            if (customLight) {
                theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_ICON), -1);
                theme_statuses_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_BACKGROUND), -1);
                theme_text_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_TEXT), -1);
                theme_text_header_1_line = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_DISPLAY_NAME), -1);
                theme_text_header_2_line = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_USERNAME), -1);
            }
        } else {
            if (customDark) {
                theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_ICON), -1);
                theme_statuses_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_BACKGROUND), -1);
                theme_text_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_TEXT), -1);
                theme_text_header_1_line = sharedpreferences.getInt(context.getString(R.string.SET_DARK_DISPLAY_NAME), -1);
                theme_text_header_2_line = sharedpreferences.getInt(context.getString(R.string.SET_DARK_USERNAME), -1);
            }
        }
        if (theme_text_color != -1) {
            holder.binding.title.setTextColor(theme_text_color);
        }
        if (theme_icons_color != -1) {
            Helper.changeDrawableColor(context, holder.binding.icon, theme_icons_color);
        }
        if (theme_statuses_color != -1) {
            holder.binding.cardviewContainer.setBackgroundColor(theme_statuses_color);
        }
        if (theme_text_header_1_line != -1) {
            holder.binding.displayName.setTextColor(theme_text_header_1_line);
        }
        if (theme_text_header_2_line != -1) {
            holder.binding.username.setTextColor(theme_text_header_2_line);
        }
    }

    public int getCount() {
        return notificationList.size();
    }

    public Notification getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public int getItemViewType(int position) {

        if (notificationList.get(position).filteredByApp != null) {
            return TYPE_FILERED;
        }
        String type = notificationList.get(position).type;
        if (type != null) {
            switch (type) {
                case "follow" -> {
                    return TYPE_FOLLOW;
                }
                case "follow_request" -> {
                    return TYPE_FOLLOW_REQUEST;
                }
                case "mention" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_MENTION;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "reblog" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_REBLOG;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "update" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_UPDATE;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "favourite" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_FAVOURITE;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "poll" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_POLL;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "status" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_STATUS;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                case "admin.sign_up" -> {
                    return TYPE_ADMIN_SIGNUP;
                }
                case "admin.report" -> {
                    return TYPE_ADMIN_REPORT;
                }
                case "pleroma:emoji_reaction" -> {
                    if (notificationList.get(position).status != null) {
                        return TYPE_REACTION;
                    } else {
                        return TYPE_HIDDEN;
                    }
                }
                default -> {
                    return TYPE_HIDDEN;
                }
            }
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TYPE_FOLLOW || viewType == TYPE_FOLLOW_REQUEST || viewType == TYPE_ADMIN_REPORT || viewType == TYPE_ADMIN_SIGNUP) {
            DrawerFollowBinding itemBinding = DrawerFollowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolderFollow(itemBinding);
        } else if (viewType == TYPE_FILERED) {
            DrawerStatusFilteredBinding itemBinding = DrawerStatusFilteredBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusAdapter.StatusViewHolder(itemBinding);
        } else if (viewType == TYPE_HIDDEN) {
            DrawerStatusFilteredHideBinding itemBinding = DrawerStatusFilteredHideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusAdapter.StatusViewHolder(itemBinding);
        } else {
            DrawerStatusNotificationBinding itemBinding = DrawerStatusNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusAdapter.StatusViewHolder(itemBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Notification notification = notificationList.get(position);
        if (notification == null || notification.account == null) {
            return;
        }
        if (getItemViewType(position) == TYPE_FOLLOW || getItemViewType(position) == TYPE_FOLLOW_REQUEST || getItemViewType(position) == TYPE_ADMIN_REPORT || getItemViewType(position) == TYPE_ADMIN_SIGNUP) {
            ViewHolderFollow holderFollow = (ViewHolderFollow) viewHolder;
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
                holderFollow.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
                holderFollow.binding.dividerCard.setVisibility(View.GONE);
            }
            MastodonHelper.loadPPMastodon(holderFollow.binding.avatar, notification.account);
            holderFollow.binding.displayName.setText(
                    notification.account.getSpanDisplayName(context,
                            new WeakReference<>(holderFollow.binding.displayName)),
                    TextView.BufferType.SPANNABLE);
            holderFollow.binding.username.setText(String.format("@%s", notification.account.acct));

            holderFollow.binding.rejectButton.setVisibility(View.GONE);
            holderFollow.binding.acceptButton.setVisibility(View.GONE);
            if (getItemViewType(position) == TYPE_FOLLOW_REQUEST) {
                holderFollow.binding.rejectButton.setVisibility(View.VISIBLE);
                holderFollow.binding.acceptButton.setVisibility(View.VISIBLE);
                holderFollow.binding.title.setText(R.string.follow_request);
            } else if (getItemViewType(position) == TYPE_ADMIN_REPORT) {
                holderFollow.binding.title.setText(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_submitted_report)));
            } else if (getItemViewType(position) == TYPE_ADMIN_SIGNUP) {
                holderFollow.binding.title.setText(String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_signed_up)));
            } else {
                holderFollow.binding.title.setText(R.string.follow);
            }
            AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
            holderFollow.binding.rejectButton.setOnClickListener(v -> accountsVM.rejectFollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, notification.account.id)
                    .observe((LifecycleOwner) context, relationShip -> {
                        if (notificationList.size() > holderFollow.getBindingAdapterPosition() && holderFollow.getBindingAdapterPosition() >= 0) {
                            notificationList.remove(holderFollow.getBindingAdapterPosition());
                            notifyItemRemoved(holderFollow.getBindingAdapterPosition());
                        }
                    }));
            holderFollow.binding.acceptButton.setOnClickListener(v -> accountsVM.acceptFollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, notification.account.id)
                    .observe((LifecycleOwner) context, relationShip -> {
                        if (notificationList.size() > holderFollow.getBindingAdapterPosition() && holderFollow.getBindingAdapterPosition() >= 0) {
                            notificationList.remove(holderFollow.getBindingAdapterPosition());
                            notifyItemRemoved(holderFollow.getBindingAdapterPosition());
                        }
                    }));
            holderFollow.binding.avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                });
            });
            if (notification.isFetchMore && fetchMoreCallBack != null) {
                holderFollow.binding.layoutFetchMore.fetchMoreContainer.setVisibility(View.VISIBLE);
                holderFollow.binding.layoutFetchMore.fetchMoreMin.setOnClickListener(v -> {
                    notification.isFetchMore = false;
                    if (holderFollow.getBindingAdapterPosition() < notificationList.size() - 1) {
                        String fromId;
                        if (notification.positionFetchMore == Notification.PositionFetchMore.TOP) {
                            fromId = notificationList.get(position + 1).id;
                        } else {
                            fromId = notification.id;
                        }
                        fetchMoreCallBack.onClickMinId(fromId, notification);
                        notifyItemChanged(position);
                    }

                });
                holderFollow.binding.layoutFetchMore.fetchMoreMax.setOnClickListener(v -> {
                    //We hide the button
                    notification.isFetchMore = false;
                    String fromId;
                    if (notification.positionFetchMore == Notification.PositionFetchMore.TOP) {
                        fromId = notificationList.get(position).id;
                    } else {
                        fromId = notificationList.get(position - 1).id;
                    }
                    notifyItemChanged(position);
                    fetchMoreCallBack.onClickMaxId(fromId, notification);
                });
            } else {
                holderFollow.binding.layoutFetchMore.fetchMoreContainer.setVisibility(View.GONE);
            }
            applyColorAccount(context, holderFollow);
        } else if (getItemViewType(position) == TYPE_FILERED) {
            StatusAdapter.StatusViewHolder holder = (StatusAdapter.StatusViewHolder) viewHolder;

            holder.bindingFiltered.filteredText.setText(context.getString(R.string.filtered_by, notification.filteredByApp.title));
            holder.bindingFiltered.displayButton.setOnClickListener(v -> {
                notification.filteredByApp = null;
                notifyItemChanged(position);
            });
        } else if (notification.status != null && getItemViewType(position) != TYPE_HIDDEN) {
            StatusAdapter.StatusViewHolder holderStatus = (StatusAdapter.StatusViewHolder) viewHolder;
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
                holderStatus.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
                holderStatus.binding.dividerCard.setVisibility(View.GONE);
            }
            holderStatus.bindingNotification.status.typeOfNotification.setVisibility(View.VISIBLE);

            if (getItemViewType(position) == TYPE_MENTION) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_message_24);
            } else if (getItemViewType(position) == TYPE_STATUS) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_message_24);
            } else if (getItemViewType(position) == TYPE_FAVOURITE) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_star_24);
            } else if (getItemViewType(position) == TYPE_REBLOG) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_repeat_24);
            } else if (getItemViewType(position) == TYPE_UPDATE) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_edit_24);
            } else if (getItemViewType(position) == TYPE_REACTION) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_insert_emoticon_24);
            } else if (getItemViewType(position) == TYPE_POLL) {
                holderStatus.bindingNotification.status.typeOfNotification.setImageResource(R.drawable.ic_baseline_poll_24);
            }
            int theme_icons_color = -1;
            boolean customLight = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS), false);
            boolean customDark = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_DARK_COLORS), false);
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) { //LIGHT THEME
                if (customLight) {
                    theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_ICON), -1);
                }
            } else {
                if (customDark) {
                    theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_ICON), -1);
                }
            }
            if (theme_icons_color != -1) {
                Helper.changeDrawableColor(context, holderStatus.bindingNotification.status.typeOfNotification, theme_icons_color);
            }

            holderStatus.bindingNotification.status.mainContainer.setAlpha(1.0f);
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
            if (notification.status != null) {
                notification.status.cached = notification.cached;
                notification.status.attachedNotification = notification.id;
            }
            statusManagement(context, statusesVM, searchVM, holderStatus, mRecyclerView, this, null, notification.status, Timeline.TimeLineEnum.NOTIFICATION, false, true, false, null);
            holderStatus.bindingNotification.status.dateShort.setText(Helper.dateDiff(context, notification.created_at));
            holderStatus.bindingNotification.status.pronouns.setVisibility(View.GONE);
            if (getItemViewType(position) == TYPE_MENTION || getItemViewType(position) == TYPE_STATUS || getItemViewType(position) == TYPE_REACTION) {
                holderStatus.bindingNotification.status.actionButtons.setVisibility(View.VISIBLE);
                String title = "";
                if (getItemViewType(position) == TYPE_MENTION) {
                    //title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_mention));
                    title = notification.account.display_name;
                } else if (getItemViewType(position) == TYPE_STATUS) {
                   // title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_status));
                    title = notification.account.display_name;
                } else if (getItemViewType(position) == TYPE_REACTION) {
                    if (notification.emoji == null) {
                        notification.emoji = "";
                    }
                    //title = String.format(Locale.getDefault(), "%s reacted with %s", notification.account.username, notification.emoji);
                    title = notification.account.username;
                    MastodonHelper.loadPPMastodon(holderStatus.bindingNotification.status.avatar, notification.account);
                    holderStatus.bindingNotification.status.statusUserInfo.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ProfileActivity.class);
                        Bundle args = new Bundle();
                        args.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                        new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                            Bundle bundle = new Bundle();
                            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        });
                    });
                    holderStatus.bindingNotification.status.mainContainer.setAlpha(.8f);
                }
                holderStatus.bindingNotification.status.displayName.setText(
                        notification.account.getSpanDisplayNameTitle(context,
                                new WeakReference<>(holderStatus.bindingNotification.status.displayName), title),
                        TextView.BufferType.SPANNABLE);
                holderStatus.bindingNotification.status.username.setText(String.format("@%s", notification.account.acct));

            } else {
                holderStatus.bindingNotification.status.mainContainer.setAlpha(.7f);
                boolean displayMedia = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_MEDIA_NOTIFICATION), true);
                if (!displayMedia) {
                    holderStatus.bindingNotification.status.media.mediaContainer.setVisibility(View.GONE);
                    holderStatus.bindingNotification.status.mediaContainer.setVisibility(View.GONE);
                }
                String title = "";
                MastodonHelper.loadPPMastodon(holderStatus.binding.avatar, notification.account);
                if (getItemViewType(position) == TYPE_FAVOURITE) {
                   // title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_favourite));
                    title = notification.account.display_name;
                } else if (getItemViewType(position) == TYPE_REBLOG) {
                   // title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_reblog));
                    title = notification.account.display_name;
                } else if (getItemViewType(position) == TYPE_UPDATE) {
                  //  title = String.format(Locale.getDefault(), "%s %s", notification.account.display_name, context.getString(R.string.notif_update));
                    title = notification.account.display_name;
                } else if (getItemViewType(position) == TYPE_POLL) {
                    title = context.getString(R.string.notif_poll);
                } else if (getItemViewType(position) == TYPE_POLL) {
                    title = context.getString(R.string.notif_poll);
                }
                if (notification.relatedNotifications != null && !notification.relatedNotifications.isEmpty()) {
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
                            Bundle args = new Bundle();
                            args.putSerializable(Helper.ARG_ACCOUNT, relativeNotif.account);
                            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                Bundle bundle = new Bundle();
                                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                intent.putExtras(bundle);
                                context.startActivity(intent);
                            });
                        });
                        holderStatus.bindingNotification.relatedAccounts.addView(notificationsRelatedAccountsBinding.getRoot());
                    }
                    holderStatus.bindingNotification.otherAccounts.setVisibility(View.VISIBLE);
                } else {
                    holderStatus.bindingNotification.otherAccounts.setVisibility(View.GONE);
                }
                holderStatus.bindingNotification.status.avatar.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle args = new Bundle();
                    args.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                    new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intent.putExtras(bundle);
                        context.startActivity(intent);
                    });
                });
                holderStatus.bindingNotification.status.statusUserInfo.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle args = new Bundle();
                    args.putSerializable(Helper.ARG_ACCOUNT, notification.account);
                    new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intent.putExtras(bundle);
                        context.startActivity(intent);
                    });
                });
                holderStatus.bindingNotification.status.displayName.setText(
                        notification.account.getSpanDisplayNameTitle(context,
                                new WeakReference<>(holderStatus.bindingNotification.status.displayName), title),
                        TextView.BufferType.SPANNABLE);
                holderStatus.bindingNotification.status.username.setText(String.format("@%s", notification.account.acct));
                holderStatus.bindingNotification.status.actionButtons.setVisibility(View.GONE);
            }
            StatusAdapter.applyColor(context, holderStatus);
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
        void onClickMinId(String min_id, Notification notificationToUpdate);

        void onClickMaxId(String max_id, Notification notificationToUpdate);
    }

    static class ViewHolderFollow extends RecyclerView.ViewHolder {
        DrawerFollowBinding binding;

        ViewHolderFollow(DrawerFollowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}