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


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerReorderBinding;
import app.fedilab.android.mastodon.activities.ReorderTimelinesActivity;
import app.fedilab.android.mastodon.client.entities.app.Pinned;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.RemoteInstance;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.itemtouchhelper.ItemTouchHelperAdapter;
import app.fedilab.android.mastodon.helper.itemtouchhelper.ItemTouchHelperViewHolder;
import app.fedilab.android.mastodon.helper.itemtouchhelper.OnStartDragListener;
import app.fedilab.android.mastodon.viewmodel.mastodon.TimelinesVM;


/**
 * Simple RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link androidx.recyclerview.widget.ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class ReorderTabAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

    private final OnStartDragListener mDragStartListener;
    private final Pinned pinned;
    private Context context;

    public ReorderTabAdapter(Pinned pinned, OnStartDragListener dragStartListener) {
        this.mDragStartListener = dragStartListener;
        this.pinned = pinned;
    }

    @NotNull
    @Override
    public ReorderViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerReorderBinding itemBinding = DrawerReorderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReorderViewHolder(itemBinding);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder viewHolder, int position) {

        ReorderViewHolder holder = (ReorderViewHolder) viewHolder;

        switch (pinned.pinnedTimelines.get(position).type) {
            case REMOTE:
                switch (pinned.pinnedTimelines.get(position).remoteInstance.type) {
                    case PEERTUBE:
                        holder.binding.icon.setImageResource(R.drawable.peertube_icon);
                        break;
                    case MASTODON:
                        holder.binding.icon.setImageResource(R.drawable.mastodon_icon_item);
                        break;
                    case PIXELFED:
                        holder.binding.icon.setImageResource(R.drawable.pixelfed);
                        break;
                    case MISSKEY:
                        holder.binding.icon.setImageResource(R.drawable.misskey);
                        break;
                    case LEMMY:
                        holder.binding.icon.setImageResource(R.drawable.lemmy);
                        break;
                    case GNU:
                        holder.binding.icon.setImageResource(R.drawable.ic_gnu_social);
                        break;
                    case NITTER_TAG:
                    case NITTER:
                        holder.binding.icon.setImageResource(R.drawable.nitter);
                        break;
                }
                if (pinned.pinnedTimelines.get(position).remoteInstance.type != RemoteInstance.InstanceType.NITTER && pinned.pinnedTimelines.get(position).remoteInstance.type != RemoteInstance.InstanceType.NITTER_TAG) {
                    holder.binding.text.setText(pinned.pinnedTimelines.get(position).remoteInstance.host);
                } else {
                    if (pinned.pinnedTimelines.get(position).remoteInstance.displayName != null && !pinned.pinnedTimelines.get(position).remoteInstance.displayName.trim().isEmpty()) {
                        holder.binding.text.setText(pinned.pinnedTimelines.get(position).remoteInstance.displayName);
                    } else {
                        holder.binding.text.setText(pinned.pinnedTimelines.get(position).remoteInstance.host);
                    }
                }
                break;
            case TAG:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_label_24);
                if (pinned.pinnedTimelines.get(position).tagTimeline.displayName != null) {
                    String tagTimelineDisplayName = pinned.pinnedTimelines.get(position).tagTimeline.displayName;
                    holder.binding.text.setText(tagTimelineDisplayName);
                    holder.binding.getRoot().setContentDescription(context.getString(R.string.cd_hash_tag_timeline, tagTimelineDisplayName));
                } else {
                    String tagTimelineName = pinned.pinnedTimelines.get(position).tagTimeline.name;
                    holder.binding.text.setText(tagTimelineName);
                    holder.binding.getRoot().setContentDescription("#" + tagTimelineName);
                }
                break;
            case LIST:
                String listTitle = pinned.pinnedTimelines.get(position).mastodonList.title;
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_view_list_24);
                holder.binding.text.setText(listTitle);
                holder.binding.getRoot().setContentDescription(context.getString(R.string.cd_list_timeline, listTitle));
                break;
            case HOME:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_home_24);
                holder.binding.text.setText(R.string.tab_home_timeline);
                break;
            case LOCAL:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_supervisor_account_24);
                holder.binding.text.setText(R.string.tab_local_timeline);
                break;
            case PUBLIC:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_public_24);
                holder.binding.text.setText(R.string.tab_public_timeline);
                break;
            case NOTIFICATION:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_notifications_24);
                holder.binding.text.setText(R.string.notifications);
                break;
            case DIRECT:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_mail_24);
                holder.binding.text.setText(R.string.tab_private_mentions);
                break;
            case BUBBLE:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_bubble_chart_24);
                holder.binding.text.setText(R.string.tab_bubble_timeline);
                break;
            case TREND_MESSAGE:
                holder.binding.icon.setImageResource(R.drawable.baseline_moving_24);
                holder.binding.text.setText(R.string.trending);
                break;
        }


        if (pinned.pinnedTimelines.get(position).displayed) {
            holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_24);
        } else {
            holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
        }

        holder.binding.hide.setOnClickListener(v -> {
            pinned.pinnedTimelines.get(position).displayed = !pinned.pinnedTimelines.get(position).displayed;
            notifyItemChanged(position);
            new Thread(() -> {
                try {
                    new Pinned(context).updatePinned(pinned);
                    ((ReorderTimelinesActivity) context).setChanges(true);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        // Start a drag whenever the handle view it touched
        holder.binding.handle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder);
                return true;
            }
            return false;
        });
        PinnedTimeline item = pinned.pinnedTimelines.get(position);
        if (item.type == Timeline.TimeLineEnum.TAG || item.type == Timeline.TimeLineEnum.REMOTE || item.type == Timeline.TimeLineEnum.LIST) {
            holder.binding.delete.setVisibility(View.VISIBLE);
        } else {
            holder.binding.delete.setVisibility(View.GONE);
        }
        holder.binding.delete.setOnClickListener(v -> {
            if (item.type == Timeline.TimeLineEnum.TAG || item.type == Timeline.TimeLineEnum.REMOTE || item.type == Timeline.TimeLineEnum.LIST) {
                AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);
                String title = "";
                String message = "";
                alt_bld.setTitle(R.string.action_lists_delete);
                alt_bld.setMessage(R.string.action_lists_confirm_delete);
                switch (item.type) {
                    case TAG:
                    case REMOTE:
                        title = context.getString(R.string.action_pinned_delete);
                        message = context.getString(R.string.unpin_timeline_description);
                        break;
                    case LIST:
                        title = context.getString(R.string.action_lists_delete);
                        message = context.getString(R.string.action_lists_confirm_delete);
                        break;
                }
                alt_bld.setTitle(title);
                alt_bld.setMessage(message);

                alt_bld.setPositiveButton(R.string.delete, (dialog, id) -> {
                    //change position of pinned that are after the removed item
                    if (position < pinned.pinnedTimelines.size()) {
                        for (int i = item.position + 1; i < pinned.pinnedTimelines.size(); i++) {
                            pinned.pinnedTimelines.get(i).position -= 1;
                        }
                        pinned.pinnedTimelines.remove(position);
                        notifyItemRemoved(position);
                        notifyItemChanged(position, pinned.pinnedTimelines.size() - position);
                        try {
                            new Pinned(context).updatePinned(pinned);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }

                    if (item.type == Timeline.TimeLineEnum.LIST) {
                        TimelinesVM timelinesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(TimelinesVM.class);
                        timelinesVM.deleteList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, item.mastodonList.id);
                    }


                    ((ReorderTimelinesActivity) context).setChanges(true);
                    dialog.dismiss();

                });
                alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                AlertDialog alert = alt_bld.create();
                alert.show();

            }
        });
    }

    @Override
    public void onItemDismiss(int position) {
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(pinned.pinnedTimelines, fromPosition, toPosition);
        //update position value
        for (int j = 0; j < pinned.pinnedTimelines.size(); j++) {
            pinned.pinnedTimelines.get(j).position = j;
        }
        notifyItemMoved(fromPosition, toPosition);
        notifyItemChanged(fromPosition);
        notifyItemChanged(toPosition);
        try {
            new Pinned(context).updatePinned(pinned);
            ((ReorderTimelinesActivity) context).setChanges(true);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public int getItemCount() {
        return pinned.pinnedTimelines.size();
    }

    /**
     * Simple example of a view holder that implements {@link ItemTouchHelperViewHolder} and has a
     * "handle" view that initiates a drag event when touched.
     */
    public static class ReorderViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        DrawerReorderBinding binding;

        ReorderViewHolder(DrawerReorderBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

        @Override
        public void onItemSelected() {
        }

        @Override
        public void onItemClear() {
        }
    }
}
