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


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import app.fedilab.android.R;
import app.fedilab.android.activities.ReorderTimelinesActivity;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.DrawerReorderBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.itemtouchhelper.ItemTouchHelperAdapter;
import app.fedilab.android.helper.itemtouchhelper.ItemTouchHelperViewHolder;
import app.fedilab.android.helper.itemtouchhelper.OnStartDragListener;
import app.fedilab.android.helper.itemtouchhelper.OnUndoListener;
import es.dmoral.toasty.Toasty;


/**
 * Simple RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link androidx.recyclerview.widget.ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class ReorderTabAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

    private final OnStartDragListener mDragStartListener;
    private final OnUndoListener mUndoListener;
    private final Pinned pinned;
    private Context context;

    public ReorderTabAdapter(Pinned pinned, OnStartDragListener dragStartListener, OnUndoListener undoListener) {
        this.mDragStartListener = dragStartListener;
        this.mUndoListener = undoListener;
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
                    case GNU:
                        holder.binding.icon.setImageResource(R.drawable.ic_gnu_social);
                        break;
                    case NITTER:
                        holder.binding.icon.setImageResource(R.drawable.nitter);
                        break;
                }
                holder.binding.text.setText(pinned.pinnedTimelines.get(position).remoteInstance.host);
                break;
            case TAG:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_label_24);
                if (pinned.pinnedTimelines.get(position).tagTimeline.displayName != null)
                    holder.binding.text.setText(pinned.pinnedTimelines.get(position).tagTimeline.displayName);
                else
                    holder.binding.text.setText(pinned.pinnedTimelines.get(position).tagTimeline.name);
                break;
            case LIST:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_view_list_24);
                holder.binding.text.setText(pinned.pinnedTimelines.get(position).mastodonList.title);
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

    }

    @Override
    public void onItemDismiss(int position) {
        PinnedTimeline item = pinned.pinnedTimelines.get(position);
        if (item.type == Timeline.TimeLineEnum.TAG || item.type == Timeline.TimeLineEnum.REMOTE || item.type == Timeline.TimeLineEnum.LIST) {
            mUndoListener.onUndo(item, position);
            pinned.pinnedTimelines.remove(position);
            notifyItemRemoved(position);
        } else {
            notifyItemChanged(position);
            Toasty.info(context, context.getString(R.string.warning_main_timeline), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(pinned.pinnedTimelines, fromPosition, toPosition);
        //update position value
        for (int j = 0; j < pinned.pinnedTimelines.size(); j++) {
            pinned.pinnedTimelines.get(j).position = j;
        }
        notifyItemMoved(fromPosition, toPosition);
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
    public class ReorderViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        DrawerReorderBinding binding;

        ReorderViewHolder(DrawerReorderBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.mastodonC3));
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}
