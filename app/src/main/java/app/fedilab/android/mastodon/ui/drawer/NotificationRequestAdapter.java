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


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerFollowBinding;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.NotificationRequest;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.NotificationsVM;


public class NotificationRequestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<NotificationRequest> requestList;
    private Context context;

    public NotificationRequestAdapter(List<NotificationRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerFollowBinding itemBinding = DrawerFollowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolderRequest(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        NotificationRequest request = requestList.get(position);
        ViewHolderRequest holder = (ViewHolderRequest) viewHolder;

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }

        MastodonHelper.loadPPMastodon(holder.binding.avatar, request.account);
        holder.binding.displayName.setText(request.account.display_name);
        holder.binding.username.setText(String.format("@%s", request.account.acct));
        holder.binding.rejectButton.setVisibility(View.VISIBLE);
        holder.binding.acceptButton.setVisibility(View.VISIBLE);
        holder.binding.title.setText(context.getString(R.string.notification_requests_count, request.notifications_count));

        NotificationsVM notificationsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(NotificationsVM.class);
        holder.binding.acceptButton.setOnClickListener(v -> notificationsVM.acceptNotificationRequest(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, request.id)
                .observe((LifecycleOwner) context, success -> {
                    if (success != null && success && requestList.size() > position) {
                        requestList.remove(position);
                        notifyItemRemoved(position);
                    }
                }));
        holder.binding.rejectButton.setOnClickListener(v -> notificationsVM.dismissNotificationRequest(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, request.id)
                .observe((LifecycleOwner) context, success -> {
                    if (success != null && success && requestList.size() > position) {
                        requestList.remove(position);
                        notifyItemRemoved(position);
                    }
                }));
        holder.binding.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            Bundle args = new Bundle();
            args.putSerializable(Helper.ARG_ACCOUNT, request.account);
            new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intent.putExtras(bundle);
                context.startActivity(intent);
            });
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolderRequest extends RecyclerView.ViewHolder {
        DrawerFollowBinding binding;

        ViewHolderRequest(DrawerFollowBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
