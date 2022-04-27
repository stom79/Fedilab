package app.fedilab.android.helper;
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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.ui.drawer.AccountsSearchAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;

public class CrossActionHelper {


    /**
     * Allow to do the action with another account from db
     *
     * @param context         Context
     * @param actionType      enum TypeOfCrossAction
     * @param targetedAccount mastodon account that is targeted
     * @param targetedStatus  status that is targeted
     */
    public static void doCrossAction(@NonNull Context context, @NonNull TypeOfCrossAction actionType, app.fedilab.android.client.mastodon.entities.Account targetedAccount, Status targetedStatus) {

        new Thread(() -> {
            try {
                List<Account> accounts = new Account(context).getCrossAccounts();
                if (accounts.size() == 1) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> fetchRemote(context, actionType, accounts.get(0), targetedAccount, targetedStatus);
                    mainHandler.post(myRunnable);
                } else {
                    List<app.fedilab.android.client.mastodon.entities.Account> accountList = new ArrayList<>();
                    for (Account account : accounts) {
                        accountList.add(account.mastodon_account);
                    }
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context, Helper.dialogStyle());
                        builderSingle.setTitle(context.getString(R.string.choose_accounts));
                        final AccountsSearchAdapter accountsSearchAdapter = new AccountsSearchAdapter(context, accountList);
                        final Account[] accountArray = new Account[accounts.size()];
                        int i = 0;
                        for (Account account : accounts) {
                            accountArray[i] = account;
                            i++;
                        }
                        builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        builderSingle.setAdapter(accountsSearchAdapter, (dialog, which) -> {
                            Account selectedAccount = accountArray[which];
                            fetchRemote(context, actionType, selectedAccount, targetedAccount, targetedStatus);
                            dialog.dismiss();
                        });
                        builderSingle.show();
                    };
                    mainHandler.post(myRunnable);
                }

            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();

    }

    /**
     * Fetch and federate the remote account or status
     */
    private static void fetchRemote(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull Account ownerAccount, app.fedilab.android.client.mastodon.entities.Account targetedAccount, Status targetedStatus) {

        SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", SearchVM.class);
        if (targetedAccount != null) {
            String search;
            if (targetedAccount.acct.contains("@")) { //Not from same instance
                search = targetedAccount.acct;
            } else {
                search = targetedAccount.acct + "@" + BaseMainActivity.currentInstance;
            }
            searchVM.search(ownerAccount.instance, ownerAccount.token, search, null, "accounts", false, true, false, 0, null, null, 1)
                    .observe((LifecycleOwner) context, results -> {
                        if (results.accounts != null && results.accounts.size() > 0) {
                            app.fedilab.android.client.mastodon.entities.Account account = results.accounts.get(0);
                            applyAction(context, actionType, ownerAccount, account, null);
                        } else {
                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                        }
                    });
        } else if (targetedStatus != null) {
            searchVM.search(ownerAccount.instance, ownerAccount.token, targetedStatus.url, null, "statuses", false, true, false, 0, null, null, 1)
                    .observe((LifecycleOwner) context, results -> {
                        if (results.statuses != null && results.statuses.size() > 0) {
                            Status status = results.statuses.get(0);
                            applyAction(context, actionType, ownerAccount, null, status);
                        } else {
                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
        }

    }

    /**
     * Do action when status or account has been fetched
     */
    private static void applyAction(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull Account ownerAccount, app.fedilab.android.client.mastodon.entities.Account targetedAccount, Status targetedStatus) {

        AccountsVM accountsVM = null;
        StatusesVM statusesVM = null;
        if (targetedAccount != null) {
            accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", AccountsVM.class);
        }
        if (targetedStatus != null) {
            statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", StatusesVM.class);
        }
        switch (actionType) {
            case MUTE_ACTION:
                assert accountsVM != null;
                accountsVM.mute(ownerAccount.instance, ownerAccount.token, targetedAccount.id, true, 0)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_SHORT).show());
                break;
            case UNMUTE_ACTION:
                assert accountsVM != null;
                accountsVM.unmute(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unmute), Toasty.LENGTH_SHORT).show());
                break;
            case BLOCK_ACTION:
                assert accountsVM != null;
                accountsVM.block(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_block), Toasty.LENGTH_SHORT).show());
                break;
            case UNBLOCK_ACTION:
                assert accountsVM != null;
                accountsVM.unblock(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unblock), Toasty.LENGTH_SHORT).show());
                break;
            case FOLLOW_ACTION:
                assert accountsVM != null;
                accountsVM.follow(ownerAccount.instance, ownerAccount.token, targetedAccount.id, true, false)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_follow), Toasty.LENGTH_SHORT).show());
                break;
            case UNFOLLOW_ACTION:
                assert accountsVM != null;
                accountsVM.unfollow(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unfollow), Toasty.LENGTH_SHORT).show());
                break;
            case FAVOURITE_ACTION:
                assert statusesVM != null;
                statusesVM.favourite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_favourite), Toasty.LENGTH_SHORT).show());
            case UNFAVOURITE_ACTION:
                assert statusesVM != null;
                statusesVM.unFavourite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_unfavourite), Toasty.LENGTH_SHORT).show());
            case REBLOG_ACTION:
                assert statusesVM != null;
                statusesVM.reblog(ownerAccount.instance, ownerAccount.token, targetedStatus.id, null)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_reblog), Toasty.LENGTH_SHORT).show());
            case UNREBLOG_ACTION:
                assert statusesVM != null;
                statusesVM.unReblog(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_unreblog), Toasty.LENGTH_SHORT).show());
        }
    }

    public enum TypeOfCrossAction {
        FOLLOW_ACTION,
        UNFOLLOW_ACTION,
        MUTE_ACTION,
        UNMUTE_ACTION,
        BLOCK_ACTION,
        UNBLOCK_ACTION,
        FAVOURITE_ACTION,
        UNFAVOURITE_ACTION,
        REBLOG_ACTION,
        UNREBLOG_ACTION
    }
}
