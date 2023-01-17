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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Suggestion;
import app.fedilab.android.databinding.DrawerSuggestionBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;


public class SuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Suggestion> suggestionList;
    private Context context;

    public SuggestionAdapter(List<Suggestion> suggestionList) {
        this.suggestionList = suggestionList;
    }


    public int getCount() {
        return suggestionList.size();
    }

    public Suggestion getItem(int position) {
        return suggestionList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        DrawerSuggestionBinding itemBinding = DrawerSuggestionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SuggestionViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Account account = suggestionList.get(position).account;
        SuggestionViewHolder holder = (SuggestionViewHolder) viewHolder;
        MastodonHelper.loadPPMastodon(holder.binding.avatar, account);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }
        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);

        holder.binding.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT, account);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, holder.binding.avatar, context.getString(R.string.activity_porfile_pp));
            // start the new activity
            context.startActivity(intent, options.toBundle());
        });
        holder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_add_24);
        if (account == null) {
            return;
        }
        holder.binding.displayName.setText(
                account.getSpanDisplayName(context,
                        new WeakReference<>(holder.binding.displayName)),
                TextView.BufferType.SPANNABLE);
        holder.binding.username.setText(String.format("@%s", account.acct));
        holder.binding.bio.setText(
                account.getSpanNote(context,
                        new WeakReference<>(holder.binding.bio)),
                TextView.BufferType.SPANNABLE);

        holder.binding.followAction.setOnClickListener(v -> {
            suggestionList.remove(position);
            notifyItemRemoved(position);
            accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false, null);
        });
        holder.binding.notInterested.setOnClickListener(view -> {
            suggestionList.remove(position);
            notifyItemRemoved(position);
            accountsVM.removeSuggestion(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id);
        });
        //TODO, remove when supported
        holder.binding.notInterested.setVisibility(View.GONE);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }


    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        DrawerSuggestionBinding binding;

        SuggestionViewHolder(DrawerSuggestionBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}