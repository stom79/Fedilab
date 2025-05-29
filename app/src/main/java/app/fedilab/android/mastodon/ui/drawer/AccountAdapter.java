package app.fedilab.android.mastodon.ui.drawer;
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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerAccountBinding;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import es.dmoral.toasty.Toasty;


public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Account> accountList;
    private final boolean home_mute;

    private final String remoteInstance;
    private Context context;

    public AccountAdapter(List<Account> accountList, boolean home_mute, String remoteInstance) {
        this.accountList = accountList;
        this.home_mute = home_mute;
        this.remoteInstance = remoteInstance;
    }

    public AccountAdapter(List<Account> accountList) {
        this.accountList = accountList;
        this.home_mute = false;
        this.remoteInstance = null;
    }

    public static void accountManagement(Context context, AccountViewHolder accountViewHolder, Account account, int position, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, boolean home_mute, String remoteInstance) {
        MastodonHelper.loadPPMastodon(accountViewHolder.binding.avatar, account);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);

        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            accountViewHolder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            accountViewHolder.binding.dividerCard.setVisibility(View.GONE);
        }

        if (home_mute) {
            accountViewHolder.binding.muteHome.setVisibility(View.VISIBLE);
            boolean muted = MainActivity.filteredAccounts != null && MainActivity.filteredAccounts.contains(account);
            accountViewHolder.binding.muteHome.setChecked(muted);
            accountViewHolder.binding.muteHome.setOnClickListener(v -> {
                if (muted) {
                    accountsVM.unmuteHome(Helper.getCurrentAccount(context), account).observe((LifecycleOwner) context, account1 -> adapter.notifyItemChanged(accountViewHolder.getLayoutPosition()));
                } else {
                    accountsVM.muteHome(Helper.getCurrentAccount(context), account).observe((LifecycleOwner) context, account1 -> adapter.notifyItemChanged(accountViewHolder.getLayoutPosition()));
                }
            });
        } else {
            accountViewHolder.binding.muteHome.setVisibility(View.GONE);
        }
        if (remoteInstance != null) {
            accountViewHolder.binding.muteGroup.setVisibility(View.GONE);
            accountViewHolder.binding.followAction.setVisibility(View.GONE);
            accountViewHolder.binding.block.setVisibility(View.GONE);
        }

        accountViewHolder.binding.avatar.setOnClickListener(v -> {
            if (remoteInstance == null) {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_ACCOUNT, account);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                });
            } else {
                Toasty.info(context, context.getString(R.string.retrieve_remote_account), Toasty.LENGTH_SHORT).show();
                SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
                searchVM.search(remoteInstance, null, account.acct, null, "accounts", null, null, null, null, null, null, null)
                        .observe((LifecycleOwner) context, results -> {
                            if (results != null && results.accounts != null && results.accounts.size() > 0) {
                                Account accountSearch = results.accounts.get(0);
                                Intent intent = new Intent(context, ProfileActivity.class);
                                Bundle args = new Bundle();
                                args.putSerializable(Helper.ARG_ACCOUNT, accountSearch);
                                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                                    intent.putExtras(bundle);
                                    context.startActivity(intent);
                                });
                            } else {
                                Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                            }
                        });
            }
        });


        accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_add_24);
        if (account.relationShip != null) {

            ProfileActivity.action doAction = ProfileActivity.action.FOLLOW;
            accountViewHolder.binding.followAction.setContentDescription(context.getString(R.string.action_follow));
            accountViewHolder.binding.followAction.setVisibility(View.VISIBLE);
            accountViewHolder.binding.followAction.setEnabled(true);

            if (account.relationShip.id.compareToIgnoreCase(BaseMainActivity.currentUserID) == 0) {
                doAction = ProfileActivity.action.NOTHING;
                accountViewHolder.binding.followAction.setVisibility(View.GONE);
                accountViewHolder.binding.muteNotification.setVisibility(View.GONE);
                accountViewHolder.binding.muteTimed.setVisibility(View.GONE);
                accountViewHolder.binding.mute.setVisibility(View.GONE);
                accountViewHolder.binding.block.setVisibility(View.GONE);
            } else {
                accountViewHolder.binding.followAction.setVisibility(View.VISIBLE);
                accountViewHolder.binding.muteNotification.setVisibility(View.VISIBLE);
                accountViewHolder.binding.muteTimed.setVisibility(View.VISIBLE);
                accountViewHolder.binding.mute.setVisibility(View.VISIBLE);
                accountViewHolder.binding.block.setVisibility(View.VISIBLE);
            }

            if (account.relationShip.following) {
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAttColor(context, R.attr.colorError)));
                doAction = ProfileActivity.action.UNFOLLOW;
                accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_remove_24);
            } else if (account.relationShip.requested) {
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAttColor(context, R.attr.colorError)));
                doAction = ProfileActivity.action.NOTHING;
                accountViewHolder.binding.followAction.setEnabled(false);
                accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_hourglass_full_24);
            } else {
                accountViewHolder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_add_24);
                accountViewHolder.binding.followAction.setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAttColor(context, R.attr.colorPrimary)));
            }

            if (account.relationShip.followed_by) {
                accountViewHolder.binding.followIndicator.setVisibility(View.VISIBLE);
            } else {
                accountViewHolder.binding.followIndicator.setVisibility(View.GONE);
            }
            if (account.relationShip.requested_by) {
                accountViewHolder.binding.requestIndicator.setVisibility(View.VISIBLE);
            } else {
                accountViewHolder.binding.requestIndicator.setVisibility(View.GONE);
            }
            if (account.relationShip.blocking) {
                accountViewHolder.binding.block.setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAttColor(context, R.attr.colorError)));
                accountViewHolder.binding.block.setIconResource(R.drawable.ic_baseline_lock_open_24);
                accountViewHolder.binding.block.setContentDescription(context.getString(R.string.action_unblock));
                accountViewHolder.binding.block.setOnClickListener(v -> accountsVM.unblock(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe((LifecycleOwner) context, relationShip -> {
                            account.relationShip = relationShip;
                            adapter.notifyItemChanged(position);
                        }));
            } else {
                accountViewHolder.binding.block.setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAttColor(context, R.attr.colorPrimary)));
                accountViewHolder.binding.block.setIconResource(R.drawable.ic_baseline_block_24);
                accountViewHolder.binding.block.setContentDescription(context.getString(R.string.more_action_2));
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
            ProfileActivity.action finalDoAction = doAction;
            accountViewHolder.binding.followAction.setOnClickListener(v -> {
                if (finalDoAction == ProfileActivity.action.NOTHING) {
                    Toasty.info(context, context.getString(R.string.nothing_to_do), Toast.LENGTH_LONG).show();
                } else if (finalDoAction == ProfileActivity.action.FOLLOW) {
                    accountViewHolder.binding.followAction.setEnabled(false);
                    accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false, null)
                            .observe((LifecycleOwner) context, relationShip -> {
                                account.relationShip = relationShip;
                                adapter.notifyItemChanged(position);
                            });
                } else if (finalDoAction == ProfileActivity.action.UNFOLLOW) {
                    boolean confirm_unfollow = sharedpreferences.getBoolean(context.getString(R.string.SET_UNFOLLOW_VALIDATION), true);
                    if (confirm_unfollow) {
                        AlertDialog.Builder unfollowConfirm = new MaterialAlertDialogBuilder(context);
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
        accountViewHolder.binding.displayName.setText(
                account.getSpanDisplayName(context,
                        accountViewHolder.binding.displayName),
                TextView.BufferType.SPANNABLE);
        accountViewHolder.binding.username.setText(String.format("@%s", account.acct));
        accountViewHolder.binding.bio.setText(
                account.getSpanNote(context,
                        accountViewHolder.binding.bio),
                TextView.BufferType.SPANNABLE);
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
        accountManagement(context, holder, account, position, this, home_mute, remoteInstance);

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