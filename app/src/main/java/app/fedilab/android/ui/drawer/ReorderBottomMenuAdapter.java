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

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import app.fedilab.android.R;
import app.fedilab.android.activities.ReorderTimelinesActivity;
import app.fedilab.android.client.entities.app.BottomMenu;
import app.fedilab.android.databinding.DrawerReorderBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.itemtouchhelper.ItemTouchHelperAdapter;
import app.fedilab.android.helper.itemtouchhelper.ItemTouchHelperViewHolder;
import app.fedilab.android.helper.itemtouchhelper.OnStartDragListener;


/**
 * Simple RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link androidx.recyclerview.widget.ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class ReorderBottomMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

    private final OnStartDragListener mDragStartListener;
    private final BottomMenu bottomMenu;
    private Context context;

    public ReorderBottomMenuAdapter(BottomMenu bottomMenu, OnStartDragListener dragStartListener) {
        this.mDragStartListener = dragStartListener;
        this.bottomMenu = bottomMenu;
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
        String title = "";
        switch (bottomMenu.bottom_menu.get(position).item_menu_type) {
            case HOME:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_home_24);
                title = context.getString(R.string.home_menu);
                break;
            case LOCAL:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_people_alt_24);
                title = context.getString(R.string.local);
                break;
            case PUBLIC:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_public_24);
                title = context.getString(R.string.v_public);
                break;
            case NOTIFICATION:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_notifications_24);
                title = context.getString(R.string.notifications);
                break;
            case DIRECT:
                holder.binding.icon.setImageResource(R.drawable.ic_baseline_mail_24);
                title = context.getString(R.string.v_private);
                break;
        }
        holder.binding.text.setText(title);

        if (bottomMenu.bottom_menu.get(position).visible) {
            holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_24);
        } else {
            holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
        }

        holder.binding.hide.setOnClickListener(v -> {
            bottomMenu.bottom_menu.get(position).visible = !bottomMenu.bottom_menu.get(position).visible;
            if (bottomMenu.bottom_menu.get(position).visible) {
                holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_24);
            } else {
                holder.binding.hide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
            }
            new Thread(() -> {
                try {
                    new BottomMenu(context).insertOrUpdate(bottomMenu);
                    ((ReorderTimelinesActivity) context).setBottomChanges(true);
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
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(bottomMenu.bottom_menu, fromPosition, toPosition);
        //update position value
        for (int j = 0; j < bottomMenu.bottom_menu.size(); j++) {
            bottomMenu.bottom_menu.get(j).position = j;
        }
        notifyItemMoved(fromPosition, toPosition);
        try {
            new BottomMenu(context).insertOrUpdate(bottomMenu);
            ((ReorderTimelinesActivity) context).setBottomChanges(true);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public int getItemCount() {
        return bottomMenu.bottom_menu.size();
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
