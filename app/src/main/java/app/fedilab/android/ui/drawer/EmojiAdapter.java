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


import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.databinding.DrawerEmojiPickerBinding;


public class EmojiAdapter extends BaseAdapter {
    private final List<Emoji> emojiList;


    public EmojiAdapter(List<Emoji> emojiList) {
        this.emojiList = emojiList;
    }

    public int getCount() {
        return emojiList == null ? 0 : emojiList.size();
    }

    public Emoji getItem(int position) {
        return emojiList.get(position);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Emoji emoji = emojiList.get(position);
        EmojiViewHolder holder;
        if (convertView == null) {
            DrawerEmojiPickerBinding drawerEmojiPickerBinding = DrawerEmojiPickerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            holder = new EmojiViewHolder(drawerEmojiPickerBinding);
            holder.view = drawerEmojiPickerBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (EmojiViewHolder) convertView.getTag();
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(holder.view.getContext());
        boolean disableAnimatedEmoji = sharedpreferences.getBoolean(parent.getContext().getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
        Glide.with(holder.binding.imgCustomEmoji.getContext())
                .load(!disableAnimatedEmoji ? emoji.url : emoji.static_url)
                .into(holder.binding.imgCustomEmoji);

        return holder.view;
    }


    public long getItemId(int position) {
        return position;
    }


    public int getItemCount() {
        return emojiList.size();
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        DrawerEmojiPickerBinding binding;
        private View view;

        EmojiViewHolder(DrawerEmojiPickerBinding itemView) {
            super(itemView.getRoot());
            this.view = itemView.getRoot();
            binding = itemView;
        }
    }
}