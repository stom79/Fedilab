package app.fedilab.android.ui.drawer;
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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.databinding.DrawerAccountBinding;
import app.fedilab.android.helper.CustomEmoji;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;


public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static ProfileActivity.action doAction;
    private final List<Account> accountList;
    private Context context;

    public AccountAdapter(List<Account> accountList) {
        this.accountList = accountList;
    }

    public static void accountManagement(Context context, AccountViewHolder accountViewHolder, Account account, int position, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        MastodonHelper.loadPPMastodon(accountViewHolder.binding.avatar, account);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);

        accountViewHolder.binding.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT, account);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, accountViewHolder.binding.avatar, context.getString(R.string.activity_porfile_pp));
            // start the new activity
            context.startActivity(intent, options.toBundle());
        });

        if (account.relationShip != null) {

            doAction = ProfileActivity.action.FOLLOW;
            accountViewHolder.binding.followAction.setText(R.string.action_follow);
            accountViewHolder.binding.followAction.setVisibility(View.VISIBLE);
            accountViewHolder.binding.followAction.setEnabled(true);

            if (account.relationShip.id.compareToIgnoreCase(BaseMainActivity.currentUserID) == 0) {
                doAction = ProfileActivity.action.NOTHING;
                accountViewHolder.binding.followAction.setVisibility(View.GONE);
                accountViewHolder.binding.muteGroup.setVisibility(View.GONE);
                accountViewHolder.binding.block.setVisibility(View.GONE);
            } else {
                accountViewHolder.binding.followAction.setVisibility(View.VISIBLE);
                accountViewHolder.binding.muteGroup.setVisibility(View.VISIBLE);
                accountViewHolder.binding.block.setVisibility(View.VISIBLE);
            }

            if (account.relationShip.following) {
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_1)));
                doAction = ProfileActivity.action.UNFOLLOW;
                accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_remove_24);
            } else if (account.relationShip.requested) {
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_1)));
                doAction = ProfileActivity.action.NOTHING;
                accountViewHolder.binding.followAction.setEnabled(false);
                accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_hourglass_full_24);
            } else {
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cyanea_accent_dark_reference)));
            }


            if (account.relationShip.blocking) {
                accountViewHolder.binding.block.setChecked(true);
                accountViewHolder.binding.block.setOnClickListener(v -> accountsVM.unblock(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            } else {
                accountViewHolder.binding.block.setChecked(false);
                accountViewHolder.binding.block.setOnClickListener(v -> accountsVM.block(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            }
            if (account.mute_expires_at != null && account.mute_expires_at.after(new Date())) {
                accountViewHolder.binding.muteTimed.setChecked(true);
                accountViewHolder.binding.muteTimed.setOnClickListener(v -> accountsVM.unmute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            accountsVM.getMutes(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, "1", account.id, null)
                                    .observe((LifecycleOwner) context, accounts -> {
                                        if (accounts != null && accounts.accounts != null && accounts.accounts.size() > 0) {
                                            account.mute_expires_at = accounts.accounts.get(0).mute_expires_at;
                                            adapter.notifyItemChanged(position);
                                        }
                                    });
                        }));
            } else {
                accountViewHolder.binding.muteTimed.setChecked(false);
            }
            if (account.relationShip.muting) {
                accountViewHolder.binding.mute.setChecked(true);
                accountViewHolder.binding.mute.setOnClickListener(v -> accountsVM.unmute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            } else {
                accountViewHolder.binding.mute.setChecked(false);
                accountViewHolder.binding.mute.setOnClickListener(v -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, null, 0)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
                accountViewHolder.binding.muteTimed.setEnabled(true);
                accountViewHolder.binding.muteTimed.setOnClickListener(v -> MastodonHelper.scheduleBoost(context, MastodonHelper.ScheduleType.TIMED_MUTED, null, account, relationShip -> {
                    account.relationShip = relationShip;
                    adapter.notifyItemChanged(position);
                }));
            }

            if (account.relationShip.muting_notifications) {
                accountViewHolder.binding.muteNotification.setChecked(true);
                accountViewHolder.binding.muteNotification.setOnClickListener(v -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, false, 0)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            } else {
                accountViewHolder.binding.muteNotification.setChecked(false);
                accountViewHolder.binding.muteNotification.setOnClickListener(v -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, 0)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            }

            if (account.relationShip.blocked_by) {
                doAction = ProfileActivity.action.NOTHING;
                accountViewHolder.binding.followAction.setEnabled(false);
            }


            accountViewHolder.binding.followAction.setOnClickListener(null);
            accountViewHolder.binding.followAction.setOnClickListener(v -> {
                if (doAction == ProfileActivity.action.NOTHING) {
                    Toasty.info(context, context.getString(R.string.nothing_to_do), Toast.LENGTH_LONG).show();
                } else if (doAction == ProfileActivity.action.FOLLOW) {
                    accountViewHolder.binding.followAction.setEnabled(false);
                    accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false)
                            .observe((LifecycleOwner) context, relationShip -> {
                                account.relationShip = relationShip;
                                adapter.notifyItemChanged(position);
                            });
                } else if (doAction == ProfileActivity.action.UNFOLLOW) {
                    boolean confirm_unfollow = sharedpreferences.getBoolean(context.getString(R.string.SET_UNFOLLOW_VALIDATION), true);
                    if (confirm_unfollow) {
                        AlertDialog.Builder unfollowConfirm = new AlertDialog.Builder(context, Helper.dialogStyle());
                        unfollowConfirm.setTitle(context.getString(R.string.unfollow_confirm));
                        unfollowConfirm.setMessage(account.acct);
                        unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        unfollowConfirm.setPositiveButton(R.string.yes, (dialog, which) -> {
                            accountViewHolder.binding.followAction.setEnabled(false);
                            accountsVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                                    .observe((LifecycleOwner) context, relationShip -> {
                                        account.relationShip = relationShip;
                                        adapter.notifyItemChanged(position);
                                    });
                            dialog.dismiss();
                        });
                        unfollowConfirm.show();
                    } else {
                        accountsVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                                .observe((LifecycleOwner) context, relationShip -> {
                                    account.relationShip = relationShip;
                                    adapter.notifyItemChanged(position);
                                });
                    }

                }
            });
        }
        CustomEmoji.displayEmoji(context, account.emojis, account.span_display_name, accountViewHolder.binding.displayName, account.id, id -> {
            if (!account.emojiFetched) {
                account.emojiFetched = true;
                accountViewHolder.binding.displayName.post(() -> adapter.notifyItemChanged(position));
            }
        });
        accountViewHolder.binding.displayName.setText(account.span_display_name, TextView.BufferType.SPANNABLE);
        accountViewHolder.binding.username.setText(String.format("@%s", account.acct));
        CustomEmoji.displayEmoji(context, account.emojis, account.span_note, accountViewHolder.binding.bio, account.id, id -> {
            if (!account.emojiFetched) {
                account.emojiFetched = true;
                accountViewHolder.binding.bio.post(() -> adapter.notifyItemChanged(position));
            }
        });
        accountViewHolder.binding.bio.setText(account.span_note, TextView.BufferType.SPANNABLE);
    }

    public int getCount() {
        return accountList.size();
    }

    public Account getItem(int position) {
        return accountList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        DrawerAccountBinding itemBinding = DrawerAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AccountViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Account account = accountList.get(position);
        AccountViewHolder holder = (AccountViewHolder) viewHolder;
        accountManagement(context, holder, account, position, this);

    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }


    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        DrawerAccountBinding binding;

        AccountViewHolder(DrawerAccountBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}