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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerReleaseNoteBinding;
import app.fedilab.android.mastodon.client.entities.app.ReleaseNote;
import app.fedilab.android.mastodon.helper.TranslateHelper;
import es.dmoral.toasty.Toasty;


public class ReleaseNoteAdapter extends RecyclerView.Adapter<ReleaseNoteAdapter.ReleaseNoteViewHolder> {

    private final List<ReleaseNote.Note> notes;
    private Context context;

    public ReleaseNoteAdapter(List<ReleaseNote.Note> notes) {
        this.notes = notes;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @NonNull
    @Override
    public ReleaseNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerReleaseNoteBinding itemBinding = DrawerReleaseNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReleaseNoteViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseNoteViewHolder holder, int position) {
        ReleaseNote.Note note = notes.get(position);
        holder.binding.note.setText(note.note);
        holder.binding.version.setText(String.format(Locale.getDefault(), "%s (%s)", note.version, note.code));
        if (note.noteTranslated != null) {
            holder.binding.noteTranslated.setText(note.noteTranslated);
            holder.binding.containerTrans.setVisibility(View.VISIBLE);
            holder.binding.translate.setVisibility(View.GONE);
        } else {
            holder.binding.containerTrans.setVisibility(View.GONE);
            holder.binding.translate.setVisibility(View.VISIBLE);
        }
        holder.binding.translate.setOnClickListener(v -> TranslateHelper.translate(context, note.note, translated -> {
            if (translated != null) {
                note.noteTranslated = translated;
                notifyItemChanged(holder.getBindingAdapterPosition());
            } else {
                Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
            }
        }));
    }


    public static class ReleaseNoteViewHolder extends RecyclerView.ViewHolder {
        DrawerReleaseNoteBinding binding;

        ReleaseNoteViewHolder(DrawerReleaseNoteBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}