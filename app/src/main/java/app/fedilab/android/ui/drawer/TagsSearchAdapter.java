package app.fedilab.android.ui.drawer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.client.mastodon.entities.Tag;
import app.fedilab.android.databinding.DrawerTagSearchBinding;

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

public class TagsSearchAdapter extends ArrayAdapter<Tag> implements Filterable {

    private final List<Tag> tags;
    private final List<Tag> tempTags;
    private final List<Tag> suggestions;

    private final Filter searchFilter = new Filter() {
        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Tag tag = (Tag) resultValue;
            return "#" + tag.name;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                suggestions.addAll(tempTags);
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
            ArrayList<Tag> c = (ArrayList<Tag>) results.values;
            if (results.count > 0) {
                clear();
                addAll(c);
                notifyDataSetChanged();
            } else {
                clear();
                notifyDataSetChanged();
            }
        }
    };

    public TagsSearchAdapter(Context context, List<Tag> tags) {
        super(context, android.R.layout.simple_list_item_1, tags);
        this.tags = tags;
        this.tempTags = new ArrayList<>(tags);
        this.suggestions = new ArrayList<>(tags);
    }

    @Override
    public int getCount() {
        return tags.size();
    }

    @Override
    public Tag getItem(int position) {
        return tags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        final Tag tag = tags.get(position);
        TagSearchViewHolder holder;
        if (convertView == null) {
            DrawerTagSearchBinding drawerTagSearchBinding = DrawerTagSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            holder = new TagSearchViewHolder(drawerTagSearchBinding);
            holder.view = drawerTagSearchBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (TagSearchViewHolder) convertView.getTag();
        }
        holder.binding.tagName.setText(String.format("#%s", tag.name));

        return holder.view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return searchFilter;
    }

    public static class TagSearchViewHolder extends RecyclerView.ViewHolder {
        DrawerTagSearchBinding binding;
        private View view;

        TagSearchViewHolder(DrawerTagSearchBinding itemView) {
            super(itemView.getRoot());
            this.view = itemView.getRoot();
            binding = itemView;
        }
    }

}
