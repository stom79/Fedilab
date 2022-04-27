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
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.IdentityProof;
import app.fedilab.android.databinding.DrawerIdentityProofsBinding;
import app.fedilab.android.helper.Helper;


public class IdentityProofsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<IdentityProof> identityProofList;
    private Context context;

    public IdentityProofsAdapter(List<IdentityProof> identityProofs) {
        this.identityProofList = identityProofs;
    }

    public IdentityProof getItem(int position) {
        return identityProofList.get(position);
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerIdentityProofsBinding itemBinding = DrawerIdentityProofsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new IdentityProofViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        final IdentityProofViewHolder holder = (IdentityProofViewHolder) viewHolder;
        final IdentityProof identityProof = getItem(i);
        holder.binding.proofName.setText(String.format("@%s", identityProof.provider_username));
        holder.binding.proofName.setOnClickListener(v -> Helper.openBrowser(context, identityProof.profile_url));
        holder.binding.proofNameNetwork.setText(context.getString(R.string.verified_by, identityProof.provider, Helper.shortDateToString(identityProof.updated_at)));
        holder.binding.proofContainer.setOnClickListener(v -> Helper.openBrowser(context, identityProof.profile_url));
        holder.binding.proofNameNetwork.setOnClickListener(v -> Helper.openBrowser(context, identityProof.proof_url));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return identityProofList.size();
    }

    public static class IdentityProofViewHolder extends RecyclerView.ViewHolder {
        DrawerIdentityProofsBinding binding;

        IdentityProofViewHolder(DrawerIdentityProofsBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}