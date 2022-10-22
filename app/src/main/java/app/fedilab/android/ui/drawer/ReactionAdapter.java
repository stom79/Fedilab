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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Reaction;
import app.fedilab.android.databinding.DrawerReactionBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.AnnouncementsVM;
import app.fedilab.android.viewmodel.pleroma.ActionsVM;


/**
 * Created by Thomas on 10/03/2020.
 * Adapter for reactions on messages
 */
public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ReactionHolder> {

    private final List<Reaction> reactions;
    private final String announcementId;
    private Context context;
    private final boolean statusReaction;

    ReactionAdapter(String announcementId, List<Reaction> reactions, boolean statusReaction) {
        this.reactions = reactions;
        this.announcementId = announcementId;
        this.statusReaction = statusReaction;
    }

    ReactionAdapter(String announcementId, List<Reaction> reactions) {
        this.reactions = reactions;
        this.announcementId = announcementId;
        this.statusReaction = false;
    }

    @NonNull
    @Override
    public ReactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        context = parent.getContext();
        DrawerReactionBinding itemBinding = DrawerReactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReactionHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReactionHolder holder, int position) {
        final Reaction reaction = reactions.get(position);

        holder.binding.reactionCount.setText(String.valueOf(reaction.count));
        if (reaction.me) {
            holder.binding.reactionContainer.setBackgroundResource(R.drawable.reaction_voted);
        } else {
            holder.binding.reactionContainer.setBackgroundResource(R.drawable.reaction_border);
        }
        if (reaction.url != null) {
            holder.binding.reactionName.setVisibility(View.GONE);
            holder.binding.reactionEmoji.setVisibility(View.VISIBLE);
            holder.binding.reactionEmoji.setContentDescription(reaction.name);
            Helper.loadImage(holder.binding.reactionEmoji, reaction.url);
        } else {
            holder.binding.reactionName.setText(reaction.name);
            holder.binding.reactionName.setVisibility(View.VISIBLE);
            holder.binding.reactionEmoji.setVisibility(View.GONE);
        }
        if (!statusReaction) {
            AnnouncementsVM announcementsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AnnouncementsVM.class);
            holder.binding.reactionContainer.setOnClickListener(v -> {
                if (reaction.me) {
                    announcementsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcementId, reaction.name);
                    reaction.me = false;
                } else {
                    announcementsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcementId, reaction.name);
                    reaction.me = true;
                }
                notifyItemChanged(position);
            });
        } else {
            ActionsVM actionVM = new ViewModelProvider((ViewModelStoreOwner) context).get(ActionsVM.class);
            holder.binding.reactionContainer.setOnClickListener(v -> {
                if (reaction.me) {
                    actionVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcementId, reaction.name);
                    reaction.me = false;
                    reaction.count -= 1;
                } else {
                    actionVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcementId, reaction.name);
                    reaction.me = true;
                    reaction.count += 1;
                }
                notifyItemChanged(position);
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return reactions.size();
    }


    static class ReactionHolder extends RecyclerView.ViewHolder {
        DrawerReactionBinding binding;

        ReactionHolder(DrawerReactionBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}