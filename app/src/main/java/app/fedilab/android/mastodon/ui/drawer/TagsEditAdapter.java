package app.fedilab.android.mastodon.ui.drawer;
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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerTagEditBinding;
import app.fedilab.android.mastodon.client.entities.app.CamelTag;
import app.fedilab.android.mastodon.exception.DBException;
import es.dmoral.toasty.Toasty;

public class TagsEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<String> tags;
    private final TagsEditAdapter tagsEditAdapter;
    private Context context;

    public TagsEditAdapter(List<String> tags) {
        this.tags = tags;
        tagsEditAdapter = this;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        context = parent.getContext();
        DrawerTagEditBinding itemBinding = DrawerTagEditBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TagCaheViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        final String[] tag = {tags.get(viewHolder.getLayoutPosition())};
        TagCaheViewHolder holder = (TagCaheViewHolder) viewHolder;
        holder.binding.tagName.setText(String.format("#%s", tag[0]));
        holder.binding.saveTag.setOnClickListener(v -> {
            if (holder.binding.tagName.getText() != null && holder.binding.tagName.getText().toString().trim().replaceAll("#", "").length() > 0) {
                String tagToInsert = holder.binding.tagName.getText().toString().trim().replaceAll("#", "");
                try {
                    boolean isPresent = new CamelTag(context).tagExists(tagToInsert);
                    if (isPresent)
                        Toasty.warning(context, context.getString(R.string.tags_already_stored), Toast.LENGTH_LONG).show();
                    else {
                        new CamelTag(context).update(tag[0], tagToInsert);
                        Toasty.success(context, context.getString(R.string.tags_renamed), Toast.LENGTH_LONG).show();
                    }
                } catch (DBException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        holder.binding.deleteTag.setOnClickListener(v -> {
            holder.binding.tagName.clearFocus();
            new CamelTag(context).removeTag(tag[0]);
            tags.remove(tag[0]);
            tagsEditAdapter.notifyItemRemoved(viewHolder.getLayoutPosition());
            Toasty.success(context, context.getString(R.string.tags_deleted), Toast.LENGTH_LONG).show();
        });

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }


    static class TagCaheViewHolder extends RecyclerView.ViewHolder {
        DrawerTagEditBinding binding;

        public TagCaheViewHolder(@NonNull DrawerTagEditBinding drawerTagEditBinding) {
            super(drawerTagEditBinding.getRoot());
            binding = drawerTagEditBinding;
        }
    }
}


