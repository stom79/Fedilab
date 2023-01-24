package app.fedilab.android.peertube.drawer;
/* Copyright 2023 Thomas Schneider
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
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.peertube.activities.AccountActivity;
import app.fedilab.android.peertube.activities.PeertubeActivity;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.activities.ShowAccountActivity;
import app.fedilab.android.peertube.activities.ShowChannelActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.NotificationData.Notification;
import app.fedilab.android.peertube.client.entities.Actor;
import app.fedilab.android.peertube.fragment.DisplayNotificationsFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;


public class PeertubeNotificationsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Notification> notifications;
    private Context context;

    public PeertubeNotificationsListAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(this.context);
        return new ViewHolder(layoutInflater.inflate(R.layout.drawer_peertube_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        ViewHolder holder = (ViewHolder) viewHolder;
        Notification notification = notifications.get(position);
        //Follow Notification
        boolean clickableNotification = true;
        holder.peertube_notif_pp.setVisibility(View.VISIBLE);
        AccountData.PeertubeAccount accountAction = null;
        ChannelData.Channel channelAction = null;
        if (notification.isRead()) {
            holder.unread.setVisibility(View.INVISIBLE);
        } else {
            holder.unread.setVisibility(View.VISIBLE);
        }
        if (notification.getActorFollow() != null) {
            String profileUrl = notification.getActorFollow().getFollower().getAvatar() != null ? notification.getActorFollow().getFollower().getAvatar().getPath() : null;
            Helper.loadGiF(context, profileUrl, holder.peertube_notif_pp);
            Actor accountActionFollow = notification.getActorFollow().getFollower();
            String type = notification.getActorFollow().getFollowing().getType();
            String message;
            if (type != null && type.compareTo("channel") == 0) {
                message = context.getString(R.string.peertube_follow_channel, notification.getActorFollow().getFollower().getDisplayName(), notification.getActorFollow().getFollowing().getDisplayName());
            } else {
                message = context.getString(R.string.peertube_follow_account, accountActionFollow.getDisplayName());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                holder.peertube_notif_message.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
            else
                holder.peertube_notif_message.setText(Html.fromHtml(message));
            Actor actor = notification.getActorFollow().getFollower();
            accountAction = new AccountData.PeertubeAccount();
            accountAction.setAvatar(actor.getAvatar());
            accountAction.setDisplayName(actor.getDisplayName());
            accountAction.setHost(actor.getHost());
            accountAction.setUsername(actor.getName());
            holder.peertube_notif_message.setOnClickListener(v -> markAsRead(notification, position));
        } else if (notification.getComment() != null) { //Comment Notification
            Helper.loadAvatar(context, notification.getComment().getAccount(), holder.peertube_notif_pp);
            accountAction = notification.getComment().getAccount();
            String message = context.getString(R.string.peertube_comment_on_video, accountAction.getDisplayName(), accountAction.getUsername());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                holder.peertube_notif_message.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
            else
                holder.peertube_notif_message.setText(Html.fromHtml(message));
            AccountData.PeertubeAccount finalAccountAction1 = accountAction;
            holder.peertube_notif_message.setOnClickListener(v -> {
                Intent intent = new Intent(context, PeertubeActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("video", notification.getVideo());
                b.putString("peertube_instance", finalAccountAction1.getHost());
                b.putString("video_id", notification.getComment().getVideo().getId());
                b.putString("video_uuid", notification.getComment().getVideo().getUuid());
                intent.putExtras(b);
                markAsRead(notification, position);
                context.startActivity(intent);
            });
        } else {
            String profileUrl = notification.getVideo() != null && notification.getVideo().getChannel().getAvatar() != null ? notification.getVideo().getChannel().getAvatar().getPath() : null;
            Helper.loadGiF(context, profileUrl, holder.peertube_notif_pp);
            String message = "";
            boolean myVideo = false;
            holder.peertube_notif_pp.setVisibility(View.INVISIBLE);
            if (notification.getVideo() != null) {
                if (notification.getType() == DisplayNotificationsFragment.MY_VIDEO_PUBLISHED) {
                    message = context.getString(R.string.peertube_video_published, notification.getVideo().getName());
                    myVideo = true;
                } else if (notification.getType() == DisplayNotificationsFragment.MY_VIDEO_IMPORT_ERROR) {
                    message = context.getString(R.string.peertube_video_import_error, notification.getVideo().getName());
                    myVideo = true;
                } else if (notification.getType() == DisplayNotificationsFragment.MY_VIDEO_IMPORT_SUCCESS) {
                    message = context.getString(R.string.peertube_video_import_success, notification.getVideo().getName());
                    myVideo = true;
                } else if (notification.getType() == DisplayNotificationsFragment.NEW_VIDEO_FROM_SUBSCRIPTION) {
                    channelAction = notification.getVideo().getChannel();
                    message = context.getString(R.string.peertube_video_from_subscription, channelAction.getDisplayName(), notification.getVideo().getName());
                    holder.peertube_notif_pp.setVisibility(View.VISIBLE);
                } else if (notification.getType() == DisplayNotificationsFragment.BLACKLIST_ON_MY_VIDEO) {
                    message = context.getString(R.string.peertube_video_blacklist, notification.getVideo().getName());

                } else if (notification.getType() == DisplayNotificationsFragment.UNBLACKLIST_ON_MY_VIDEO) {
                    message = context.getString(R.string.peertube_video_unblacklist, notification.getVideo().getName());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    holder.peertube_notif_message.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
                else
                    holder.peertube_notif_message.setText(Html.fromHtml(message));
                boolean finalMyVideo = myVideo;
                holder.peertube_notif_message.setOnClickListener(v -> {
                    Intent intent = new Intent(context, PeertubeActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable("video", notification.getVideo());
                    b.putString("peertube_instance", HelperInstance.getLiveInstance(context));
                    b.putBoolean("isMyVideo", finalMyVideo);
                    b.putString("video_id", notification.getVideo().getId());
                    b.putString("video_uuid", notification.getVideo().getUuid());
                    intent.putExtras(b);
                    context.startActivity(intent);
                    markAsRead(notification, position);
                });
            } else if (notification.getVideoAbuse() != null && notification.getVideoAbuse().getVideo() != null) {
                message = context.getString(R.string.peertube_video_abuse, notification.getVideoAbuse().getVideo().getName());
                clickableNotification = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    holder.peertube_notif_message.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
                else
                    holder.peertube_notif_message.setText(Html.fromHtml(message));
                holder.peertube_notif_message.setOnClickListener(v -> markAsRead(notification, position));
            } else if (notification.getAbuse() != null) {
                clickableNotification = false;
                if (notification.getType() == DisplayNotificationsFragment.MY_VIDEO_REPPORT_SUCCESS) {
                    message = context.getString(R.string.peertube_video_report_success, notification.getAbuse().getId());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    holder.peertube_notif_message.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
                else
                    holder.peertube_notif_message.setText(Html.fromHtml(message));
                holder.peertube_notif_message.setOnClickListener(v -> markAsRead(notification, position));
            }
        }
        holder.peertube_notif_date.setText(Helper.dateDiff(context, notification.getCreatedAt()));
        AccountData.PeertubeAccount finalAccountAction = accountAction;
        ChannelData.Channel finalChannelAction = channelAction;
        if (clickableNotification) {
            holder.peertube_notif_pp.setOnClickListener(v -> {
                Bundle b = new Bundle();
                Intent intent = null;
                if (finalAccountAction != null) {
                    intent = new Intent(context, ShowAccountActivity.class);
                    b.putSerializable("account", finalAccountAction);
                    b.putString("accountAcct", finalAccountAction.getUsername() + "@" + finalAccountAction.getHost());
                } else if (finalChannelAction != null) {
                    intent = new Intent(context, ShowChannelActivity.class);
                    b.putSerializable("channel", finalChannelAction);
                }
                if (intent != null) {
                    intent.putExtras(b);
                    context.startActivity(intent);
                }
            });
        }
    }

    private void markAsRead(Notification notification, int position) {
        if (!notification.isRead()) {
            notification.setRead(true);
            PeertubeMainActivity.badgeCount--;
            if (context instanceof AccountActivity) {
                ((AccountActivity) context).updateCounter();
            }
            notifyItemChanged(position);
            new Thread(() -> new RetrofitPeertubeAPI(context).markAsRead(notification.getId())).start();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView peertube_notif_pp;
        TextView peertube_notif_message, peertube_notif_date, unread;
        RelativeLayout main_container_trans;

        public ViewHolder(View itemView) {
            super(itemView);
            peertube_notif_pp = itemView.findViewById(R.id.peertube_notif_pp);
            peertube_notif_message = itemView.findViewById(R.id.peertube_notif_message);
            peertube_notif_date = itemView.findViewById(R.id.peertube_notif_date);
            main_container_trans = itemView.findViewById(R.id.container_trans);
            unread = itemView.findViewById(R.id.unread);

        }

        public View getView() {
            return itemView;
        }
    }

}