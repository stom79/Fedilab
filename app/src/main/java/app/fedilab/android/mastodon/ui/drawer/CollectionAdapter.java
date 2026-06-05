package app.fedilab.android.mastodon.ui.drawer;
/* Copyright 2026 Thomas Schneider
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerCollectionBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Collection;

public class CollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MODE_OWN = 0;
    public static final int MODE_FEATURING = 1;
    public static final int MODE_OTHER = 2;

    private final List<Collection> collectionList;
    public ActionOnCollection actionOnCollection;
    private final int mode;

    public CollectionAdapter(List<Collection> collectionList, int mode) {
        this.collectionList = collectionList;
        this.mode = mode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerCollectionBinding itemBinding = DrawerCollectionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CollectionViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Collection collection = collectionList.get(position);
        CollectionViewHolder holder = (CollectionViewHolder) viewHolder;
        holder.binding.collectionName.setText(collection.name);
        holder.binding.itemCount.setText(holder.itemView.getContext().getString(R.string.collection_account_count, collection.item_count));
        if (mode == MODE_FEATURING && collection.ownerAccount != null) {
            holder.binding.collectionOwner.setVisibility(View.VISIBLE);
            holder.binding.collectionOwner.setText(String.format("by @%s", collection.ownerAccount.acct));
        } else {
            holder.binding.collectionOwner.setVisibility(View.GONE);
        }
        if (collection.description != null && !collection.description.isEmpty()) {
            holder.binding.collectionDescription.setText(collection.description);
            holder.binding.collectionDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.collectionDescription.setVisibility(View.GONE);
        }

        ImageView[] avatars = {holder.binding.avatar1, holder.binding.avatar2, holder.binding.avatar3, holder.binding.avatar4};
        for (ImageView avatar : avatars) {
            avatar.setVisibility(View.GONE);
        }
        if (collection.previewAccounts != null) {
            for (int idx = 0; idx < Math.min(collection.previewAccounts.size(), 4); idx++) {
                Account account = collection.previewAccounts.get(idx);
                avatars[idx].setVisibility(View.VISIBLE);
                if (account.avatar != null) {
                    Glide.with(avatars[idx].getContext())
                            .load(account.avatar)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(avatars[idx]);
                }
            }
        }
        holder.binding.avatarsContainer.setVisibility(
                collection.previewAccounts != null && !collection.previewAccounts.isEmpty() ? View.VISIBLE : View.GONE);

        holder.binding.cardviewContainer.setOnClickListener(v -> {
            if (actionOnCollection != null) {
                actionOnCollection.click(collection);
            }
        });

        if (mode == MODE_OWN) {
            holder.binding.moreActions.setVisibility(View.VISIBLE);
            holder.binding.moreActions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_collection_popup);
                popup.setOnMenuItemClickListener(item -> {
                    if (actionOnCollection == null) return false;
                    int id = item.getItemId();
                    if (id == R.id.action_view_collection) {
                        actionOnCollection.click(collection);
                    } else if (id == R.id.action_share_collection) {
                        actionOnCollection.share(collection);
                    } else if (id == R.id.action_copy_link_collection) {
                        actionOnCollection.copyLink(collection);
                    } else if (id == R.id.action_manage_accounts_collection) {
                        actionOnCollection.manageAccounts(collection);
                    } else if (id == R.id.action_edit_collection) {
                        actionOnCollection.edit(collection);
                    } else if (id == R.id.action_delete_collection) {
                        actionOnCollection.delete(collection);
                    }
                    return true;
                });
                popup.show();
            });
        } else if (mode == MODE_FEATURING) {
            holder.binding.moreActions.setVisibility(View.VISIBLE);
            holder.binding.moreActions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_collection_featuring_popup);
                popup.setOnMenuItemClickListener(item -> {
                    if (actionOnCollection == null) return false;
                    int id = item.getItemId();
                    if (id == R.id.action_view_collection) {
                        actionOnCollection.click(collection);
                    } else if (id == R.id.action_share_collection) {
                        actionOnCollection.share(collection);
                    } else if (id == R.id.action_copy_link_collection) {
                        actionOnCollection.copyLink(collection);
                    } else if (id == R.id.action_remove_myself_collection) {
                        actionOnCollection.removeMyself(collection);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.binding.moreActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return collectionList.size();
    }

    public interface ActionOnCollection {
        void click(Collection collection);
        void share(Collection collection);
        void copyLink(Collection collection);
        void manageAccounts(Collection collection);
        void edit(Collection collection);
        void delete(Collection collection);
        void removeMyself(Collection collection);
    }

    public static class CollectionViewHolder extends RecyclerView.ViewHolder {
        DrawerCollectionBinding binding;

        CollectionViewHolder(DrawerCollectionBinding itemBinding) {
            super(itemBinding.getRoot());
            binding = itemBinding;
        }
    }
}
