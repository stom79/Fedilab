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


import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Field;
import app.fedilab.android.databinding.DrawerFieldBinding;


public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.FieldViewHolder> {

    private final List<Field> fields;
    private Context context;
    private Account account;

    public FieldAdapter(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerFieldBinding itemBinding = DrawerFieldBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FieldViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        Field field = fields.get(position);
        if (field.verified_at != null) {
            holder.binding.value.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(context, R.drawable.ic_baseline_verified_24), null);
        }
        holder.binding.value.setText(
                field.getValueSpan(context, account,
                        new WeakReference<>(holder.binding.value)),
                TextView.BufferType.SPANNABLE);
        holder.binding.value.setMovementMethod(LinkMovementMethod.getInstance());
        holder.binding.label.setText(field.name);
    }


    public static class FieldViewHolder extends RecyclerView.ViewHolder {
        DrawerFieldBinding binding;

        FieldViewHolder(DrawerFieldBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}