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

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Emoji;
import app.fedilab.android.databinding.DrawerEmojiSearchBinding;


public class EmojiSearchAdapter extends ArrayAdapter<Emoji> implements Filterable {

    private final List<Emoji> emojis;
    private final List<Emoji> tempEmojis;
    private final List<Emoji> suggestions;
    private final Filter searchFilter = new Filter() {
        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Emoji emoji = (Emoji) resultValue;
            return emoji.shortcode;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                suggestions.addAll(tempEmojis);
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }

        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            try {
                ArrayList<Emoji> c = (ArrayList<Emoji>) results.values;
                if (results.count > 0) {
                    clear();
                    addAll(c);
                    notifyDataSetChanged();
                } else {
                    clear();
                    notifyDataSetChanged();
                }
            } catch (Exception ignored) {
            }


        }
    };
    private final Context context;

    public EmojiSearchAdapter(Context context, List<Emoji> emojis) {
        super(context, android.R.layout.simple_list_item_1, emojis);
        this.emojis = emojis;
        this.tempEmojis = new ArrayList<>(emojis);
        this.suggestions = new ArrayList<>(emojis);
        this.context = context;
    }


    @Override
    public int getCount() {
        return emojis.size();
    }

    @Override
    public Emoji getItem(int position) {
        return emojis.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        final Emoji emoji = emojis.get(position);
        EmojiSearchViewHolder holder;
        if (convertView == null) {
            DrawerEmojiSearchBinding drawerEmojiSearchBinding = DrawerEmojiSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            holder = new EmojiSearchViewHolder(drawerEmojiSearchBinding);
            holder.view = drawerEmojiSearchBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (EmojiSearchViewHolder) convertView.getTag();
        }
        if (emoji != null) {
            holder.binding.emojiShortcode.setText(String.format("%s", emoji.shortcode));
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean disableGif = sharedpreferences.getBoolean(context.getString(R.string.SET_DISABLE_GIF), false);
            String targetedUrl = disableGif ? emoji.static_url : emoji.url;
            Glide.with(holder.view.getContext())
                    .asDrawable()
                    .load(targetedUrl)
                    .into(holder.binding.emojiIcon);
        }
        return holder.view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return searchFilter;
    }

    public static class EmojiSearchViewHolder extends RecyclerView.ViewHolder {
        DrawerEmojiSearchBinding binding;
        private View view;

        EmojiSearchViewHolder(DrawerEmojiSearchBinding itemView) {
            super(itemView.getRoot());
            this.view = itemView.getRoot();
            binding = itemView;
        }
    }

}
