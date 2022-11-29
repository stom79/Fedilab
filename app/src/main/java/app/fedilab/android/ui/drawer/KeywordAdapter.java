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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Filter;
import app.fedilab.android.databinding.DrawerKeywordBinding;

public class KeywordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Filter.KeywordsParams> keywordsParamsList;
    private Context context;

    public KeywordAdapter(List<Filter.KeywordsParams> keywordsParamsList) {
        this.keywordsParamsList = keywordsParamsList;
    }


    public int getCount() {
        return keywordsParamsList.size();
    }

    public Filter.KeywordsParams getItem(int position) {
        return keywordsParamsList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerKeywordBinding itemBinding = DrawerKeywordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new KeywordViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Filter.KeywordsParams keywordsParams = keywordsParamsList.get(position);
        KeywordViewHolder holder = (KeywordViewHolder) viewHolder;
        holder.binding.keywordPhrase.setText(keywordsParams.keyword);
        holder.binding.keywordPhrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                keywordsParams.keyword = charSequence.toString().trim();
                if (charSequence.toString().length() == 0) {
                    holder.binding.wholeWord.setError(context.getString(R.string.cannot_be_empty));
                } else {
                    holder.binding.wholeWord.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        if (keywordsParams._destroy != null) {
            holder.binding.mainContainer.setVisibility(View.GONE);
        } else {
            holder.binding.mainContainer.setVisibility(View.VISIBLE);
        }
        holder.binding.wholeWord.setOnCheckedChangeListener((compoundButton, checked) -> keywordsParams.whole_word = checked);
        holder.binding.wholeWord.setChecked(keywordsParams.whole_word != null && keywordsParams.whole_word);
        holder.binding.deleteKeyword.setOnClickListener(v -> {
            //The keyword exists
            if (keywordsParams.id != null) {
                keywordsParams._destroy = true;
                notifyItemChanged(position);
            } else { //It is currently only the app
                if (keywordsParamsList.size() > position) {
                    keywordsParamsList.remove(position);
                    notifyItemRemoved(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return keywordsParamsList.size();
    }


    public static class KeywordViewHolder extends RecyclerView.ViewHolder {
        DrawerKeywordBinding binding;

        KeywordViewHolder(DrawerKeywordBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
