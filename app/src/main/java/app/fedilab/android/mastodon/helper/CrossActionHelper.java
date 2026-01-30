package app.fedilab.android.mastodon.helper;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonSearchService;
import app.fedilab.android.mastodon.client.entities.api.Results;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.ui.drawer.AccountsSearchAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyNote;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.MisskeyUser;
import app.fedilab.android.misskey.viewmodel.MisskeyAccountsVM;
import app.fedilab.android.misskey.viewmodel.MisskeyStatusesVM;
import es.dmoral.toasty.Toasty;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CrossActionHelper {


    /**
     * Allow to do the action with another account from db
     *
     * @param context         Context
     * @param actionType      enum TypeOfCrossAction
     * @param targetedAccount mastodon account that is targeted
     * @param targetedStatus  status that is targeted
     */
    public static void doCrossAction(@NonNull Context context, @NonNull TypeOfCrossAction actionType, app.fedilab.android.mastodon.client.entities.api.Account targetedAccount, Status targetedStatus) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        new Thread(() -> {
            try {
                boolean confirmFav = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION_FAV), false);
                boolean confirmBoost = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION), true);
                List<BaseAccount> accounts = new Account(context).getCrossAccounts();
                if (accounts.size() == 1) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        if ((actionType == TypeOfCrossAction.REBLOG_ACTION && confirmBoost) || (actionType == TypeOfCrossAction.FAVOURITE_ACTION && confirmFav)) {
                            AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);
                            if (actionType == TypeOfCrossAction.REBLOG_ACTION) {
                                alt_bld.setMessage(context.getString(R.string.reblog_add));
                            } else {
                                alt_bld.setMessage(context.getString(R.string.favourite_add));
                            }
                            alt_bld.setPositiveButton(R.string.yes, (dia, id) -> {
                                fetchRemote(context, actionType, accounts.get(0), targetedAccount, targetedStatus);
                                dia.dismiss();
                            });
                            alt_bld.setNegativeButton(R.string.cancel, (dia, id) -> dia.dismiss());
                            AlertDialog alert = alt_bld.create();
                            alert.show();
                        } else {
                            fetchRemote(context, actionType, accounts.get(0), targetedAccount, targetedStatus);
                        }
                    };
                    mainHandler.post(myRunnable);
                } else {
                    List<app.fedilab.android.mastodon.client.entities.api.Account> accountList = new ArrayList<>();
                    for (BaseAccount account : accounts) {
                        account.mastodon_account.acct += "@" + account.instance;
                        accountList.add(account.mastodon_account);
                    }
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        AlertDialog.Builder builderSingle = new MaterialAlertDialogBuilder(context);
                        builderSingle.setTitle(context.getString(R.string.choose_accounts));
                        final AccountsSearchAdapter accountsSearchAdapter = new AccountsSearchAdapter(context, accountList);
                        final BaseAccount[] accountArray = new BaseAccount[accounts.size()];
                        int i = 0;
                        for (BaseAccount account : accounts) {
                            accountArray[i] = account;
                            i++;
                        }
                        builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        builderSingle.setAdapter(accountsSearchAdapter, (dialog, which) -> {

                            BaseAccount selectedAccount = accountArray[which];
                            if ((actionType == TypeOfCrossAction.REBLOG_ACTION && confirmBoost) || (actionType == TypeOfCrossAction.FAVOURITE_ACTION && confirmFav)) {
                                AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);
                                if (actionType == TypeOfCrossAction.REBLOG_ACTION) {
                                    alt_bld.setMessage(context.getString(R.string.reblog_add));
                                } else {
                                    alt_bld.setMessage(context.getString(R.string.favourite_add));
                                }
                                alt_bld.setPositiveButton(R.string.yes, (dia, id) -> {
                                    fetchRemote(context, actionType, selectedAccount, targetedAccount, targetedStatus);
                                    dia.dismiss();
                                });
                                alt_bld.setNegativeButton(R.string.cancel, (dia, id) -> dia.dismiss());
                                AlertDialog alert = alt_bld.create();
                                alert.show();
                            } else {
                                fetchRemote(context, actionType, selectedAccount, targetedAccount, targetedStatus);
                            }
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
    private static void fetchRemote(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull BaseAccount ownerAccount, app.fedilab.android.mastodon.client.entities.api.Account targetedAccount, Status targetedStatus) {

        if (actionType == TypeOfCrossAction.COMPOSE) {
            applyAction(context, actionType, ownerAccount, null, null);
            return;
        }

        if (ownerAccount.api == Account.API.MISSKEY) {
            fetchRemoteMisskey(context, actionType, ownerAccount, targetedAccount, targetedStatus);
            return;
        }

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
                        if (results != null && results.accounts != null && !results.accounts.isEmpty()) {
                            app.fedilab.android.mastodon.client.entities.api.Account account = results.accounts.get(0);
                            applyAction(context, actionType, ownerAccount, account, null);
                        } else {
                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                        }
                    });
        } else if (targetedStatus != null) {
            searchVM.search(ownerAccount.instance, ownerAccount.token, targetedStatus.uri, null, "statuses", false, true, false, 0, null, null, 1)
                    .observe((LifecycleOwner) context, results -> {
                        if (results != null && results.statuses != null && !results.statuses.isEmpty()) {
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
     * Fetch remote content via Misskey ap/show endpoint
     */
    private static void fetchRemoteMisskey(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull BaseAccount ownerAccount, app.fedilab.android.mastodon.client.entities.api.Account targetedAccount, Status targetedStatus) {
        new Thread(() -> {
            String uri = null;
            if (targetedStatus != null) {
                uri = targetedStatus.uri;
            } else if (targetedAccount != null) {
                uri = targetedAccount.url;
            }
            if (uri == null) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
                return;
            }
            try {
                MisskeyService misskeyService = initMisskey(context, ownerAccount.instance);
                MisskeyRequest.ApShowRequest request = new MisskeyRequest.ApShowRequest(ownerAccount.token, uri);
                retrofit2.Response<MisskeyService.ApShowResponse> response = misskeyService.apShow(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    MisskeyService.ApShowResponse apResponse = response.body();
                    Gson gson = Helper.getDateBuilder();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    if ("Note".equals(apResponse.type) && targetedStatus != null) {
                        MisskeyNote note = gson.fromJson(apResponse.object, MisskeyNote.class);
                        if (note != null) {
                            Status resolvedStatus = note.toStatus(ownerAccount.instance);
                            mainHandler.post(() -> applyAction(context, actionType, ownerAccount, null, resolvedStatus));
                        } else {
                            mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
                        }
                    } else if ("User".equals(apResponse.type) && targetedAccount != null) {
                        MisskeyUser user = gson.fromJson(apResponse.object, MisskeyUser.class);
                        if (user != null) {
                            app.fedilab.android.mastodon.client.entities.api.Account resolvedAccount = user.toAccount();
                            mainHandler.post(() -> applyAction(context, actionType, ownerAccount, resolvedAccount, null));
                        } else {
                            mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
                        }
                    } else {
                        mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
                    }
                } else {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static MisskeyService initMisskey(Context context, String instance) {
        final OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MisskeyService.class);
    }

    /**
     * Do action when status or account has been fetched
     */
    private static void applyAction(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull BaseAccount ownerAccount, app.fedilab.android.mastodon.client.entities.api.Account targetedAccount, Status targetedStatus) {

        if (ownerAccount.api == Account.API.MISSKEY) {
            applyMisskeyAction(context, actionType, ownerAccount, targetedAccount, targetedStatus);
            return;
        }

        AccountsVM accountsVM = null;
        StatusesVM statusesVM = null;
        if (targetedAccount != null) {
            accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", AccountsVM.class);
        }
        if (targetedStatus != null) {
            statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", StatusesVM.class);
        }
        switch (actionType) {
            case MUTE_ACTION -> {
                assert accountsVM != null;
                accountsVM.mute(ownerAccount.instance, ownerAccount.token, targetedAccount.id, true, 0)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_SHORT).show());
            }
            case UNMUTE_ACTION -> {
                assert accountsVM != null;
                accountsVM.unmute(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unmute), Toasty.LENGTH_SHORT).show());
            }
            case BLOCK_ACTION -> {
                assert accountsVM != null;
                accountsVM.block(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_block), Toasty.LENGTH_SHORT).show());
            }
            case UNBLOCK_ACTION -> {
                assert accountsVM != null;
                accountsVM.unblock(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unblock), Toasty.LENGTH_SHORT).show());
            }
            case FOLLOW_ACTION -> {
                assert accountsVM != null;
                accountsVM.follow(ownerAccount.instance, ownerAccount.token, targetedAccount.id, true, false, null)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_follow), Toasty.LENGTH_SHORT).show());
            }
            case UNFOLLOW_ACTION -> {
                assert accountsVM != null;
                accountsVM.unfollow(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unfollow), Toasty.LENGTH_SHORT).show());
                if (BaseMainActivity.filteredAccounts != null && BaseMainActivity.filteredAccounts.contains(targetedAccount))
                    accountsVM.unmuteHome(ownerAccount, targetedAccount);
            }
            case FAVOURITE_ACTION -> {
                assert statusesVM != null;
                statusesVM.favourite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_favourite), Toasty.LENGTH_SHORT).show());
            }
            case UNFAVOURITE_ACTION -> {
                assert statusesVM != null;
                statusesVM.unFavourite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_unfavourite), Toasty.LENGTH_SHORT).show());
            }
            case BOOKMARK_ACTION -> {
                assert statusesVM != null;
                statusesVM.bookmark(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_bookmark), Toasty.LENGTH_SHORT).show());
            }
            case UNBOOKMARK_ACTION -> {
                assert statusesVM != null;
                statusesVM.unBookmark(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_unbookmark), Toasty.LENGTH_SHORT).show());
            }
            case REBLOG_ACTION -> {
                assert statusesVM != null;
                statusesVM.reblog(ownerAccount.instance, ownerAccount.token, targetedStatus.id, null)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_reblog), Toasty.LENGTH_SHORT).show());
            }
            case UNREBLOG_ACTION -> {
                assert statusesVM != null;
                statusesVM.unReblog(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_unreblog), Toasty.LENGTH_SHORT).show());
            }
            case REPLY_ACTION -> {
                Intent intent = new Intent(context, ComposeActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_STATUS_REPLY, targetedStatus);
                args.putSerializable(Helper.ARG_ACCOUNT, ownerAccount);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                });
            }
            case COMPOSE -> {
                Intent intentCompose = new Intent(context, ComposeActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_ACCOUNT, ownerAccount);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intentCompose.putExtras(bundle);
                    context.startActivity(intentCompose);
                });
            }
        }
    }

    /**
     * Apply action using Misskey API
     */
    private static void applyMisskeyAction(@NonNull Context context, @NonNull TypeOfCrossAction actionType, @NonNull BaseAccount ownerAccount, app.fedilab.android.mastodon.client.entities.api.Account targetedAccount, Status targetedStatus) {

        MisskeyAccountsVM misskeyAccountsVM = null;
        MisskeyStatusesVM misskeyStatusesVM = null;
        if (targetedAccount != null) {
            misskeyAccountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", MisskeyAccountsVM.class);
        }
        if (targetedStatus != null) {
            misskeyStatusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get("crossactions", MisskeyStatusesVM.class);
        }
        switch (actionType) {
            case MUTE_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.mute(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_SHORT).show());
            }
            case UNMUTE_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.unmute(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unmute), Toasty.LENGTH_SHORT).show());
            }
            case BLOCK_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.block(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_block), Toasty.LENGTH_SHORT).show());
            }
            case UNBLOCK_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.unblock(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unblock), Toasty.LENGTH_SHORT).show());
            }
            case FOLLOW_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.follow(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_follow), Toasty.LENGTH_SHORT).show());
            }
            case UNFOLLOW_ACTION -> {
                assert misskeyAccountsVM != null;
                misskeyAccountsVM.unfollow(ownerAccount.instance, ownerAccount.token, targetedAccount.id)
                        .observe((LifecycleOwner) context, relationShip -> Toasty.info(context, context.getString(R.string.toast_unfollow), Toasty.LENGTH_SHORT).show());
            }
            case FAVOURITE_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.createReaction(ownerAccount.instance, ownerAccount.token, targetedStatus.id, null)
                        .observe((LifecycleOwner) context, success -> Toasty.info(context, context.getString(R.string.toast_favourite), Toasty.LENGTH_SHORT).show());
            }
            case UNFAVOURITE_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.deleteReaction(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, success -> Toasty.info(context, context.getString(R.string.toast_unfavourite), Toasty.LENGTH_SHORT).show());
            }
            case BOOKMARK_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.createFavorite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, success -> Toasty.info(context, context.getString(R.string.toast_bookmark), Toasty.LENGTH_SHORT).show());
            }
            case UNBOOKMARK_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.deleteFavorite(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, success -> Toasty.info(context, context.getString(R.string.toast_unbookmark), Toasty.LENGTH_SHORT).show());
            }
            case REBLOG_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.renote(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, status -> Toasty.info(context, context.getString(R.string.toast_reblog), Toasty.LENGTH_SHORT).show());
            }
            case UNREBLOG_ACTION -> {
                assert misskeyStatusesVM != null;
                misskeyStatusesVM.unrenote(ownerAccount.instance, ownerAccount.token, targetedStatus.id)
                        .observe((LifecycleOwner) context, success -> Toasty.info(context, context.getString(R.string.toast_unreblog), Toasty.LENGTH_SHORT).show());
            }
            case REPLY_ACTION -> {
                Intent intent = new Intent(context, ComposeActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_STATUS_REPLY, targetedStatus);
                args.putSerializable(Helper.ARG_ACCOUNT, ownerAccount);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                });
            }
            case COMPOSE -> {
                Intent intentCompose = new Intent(context, ComposeActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_ACCOUNT, ownerAccount);
                new CachedBundle(context).insertBundle(args, Helper.getCurrentAccount(context), bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intentCompose.putExtras(bundle);
                    context.startActivity(intentCompose);
                });
            }
        }
    }


    private static MastodonSearchService init(Context context, String instance) {
        final OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonSearchService.class);
    }

    /**
     * Fetch and federate the remote status
     */
    public static void fetchRemoteStatus(@NonNull Context context, @NonNull BaseAccount ownerAccount, String url, Callback callback) {
        MastodonSearchService mastodonSearchService = init(context, BaseMainActivity.currentInstance);
        new Thread(() -> {
            Call<Results> resultsCall = mastodonSearchService.search(ownerAccount.token, url, null, "statuses", false, true, false, 0, null, null, 1);
            Results results = null;
            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {
                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.statuses == null) {
                                results.statuses = new ArrayList<>();
                            }
                            if (results.accounts == null) {
                                results.accounts = new ArrayList<>();
                            }
                            if (results.hashtags == null) {
                                results.hashtags = new ArrayList<>();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> {
                if (finalResults != null && finalResults.statuses != null && finalResults.statuses.size() > 0) {
                    callback.federatedStatus(finalResults.statuses.get(0));
                }
            };
            mainHandler.post(myRunnable);

        }).start();
    }

    /**
     * Fetch and federate the remote status
     */
    public static void fetchRemoteAccount(@NonNull Context context, @NonNull BaseAccount ownerAccount, String targetedAcct, Callback callback) {


        MastodonSearchService mastodonSearchService = init(context, BaseMainActivity.currentInstance);
        String search;
        if (targetedAcct.contains("@")) { //Not from same instance
            search = targetedAcct;
        } else {
            search = targetedAcct + "@" + BaseMainActivity.currentInstance;
        }
        new Thread(() -> {
            Call<Results> resultsCall = mastodonSearchService.search(ownerAccount.token, search, null, "accounts", false, true, false, 0, null, null, 1);
            Results results = null;
            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {
                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.statuses == null) {
                                results.statuses = new ArrayList<>();
                            }
                            if (results.accounts == null) {
                                results.accounts = new ArrayList<>();
                            }
                            if (results.hashtags == null) {
                                results.hashtags = new ArrayList<>();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> {
                if (finalResults != null && finalResults.accounts != null && finalResults.accounts.size() > 0) {
                    callback.federatedAccount(finalResults.accounts.get(0));
                }
            };
            mainHandler.post(myRunnable);

        }).start();
    }

    /**
     * Fetch and federate the remote status
     */
    public static void fetchRemoteAccount(@NonNull Context context, String acct, Callback callback) {


        MastodonSearchService mastodonSearchService = init(context, BaseMainActivity.currentInstance);
        new Thread(() -> {
            Call<Results> resultsCall = mastodonSearchService.search(BaseMainActivity.currentToken, acct, null, "accounts", false, true, false, 0, null, null, 1);
            Results results = null;
            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {
                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.statuses == null) {
                                results.statuses = new ArrayList<>();
                            }
                            if (results.accounts == null) {
                                results.accounts = new ArrayList<>();
                            }
                            if (results.hashtags == null) {
                                results.hashtags = new ArrayList<>();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> {
                if (finalResults != null && finalResults.accounts != null && finalResults.accounts.size() > 0) {
                    callback.federatedAccount(finalResults.accounts.get(0));
                }
            };
            mainHandler.post(myRunnable);

        }).start();
    }

    public static void doCrossShare(final Context context, final Bundle bundle) {
        List<BaseAccount> accounts;
        try {
            accounts = new Account(context).getCrossAccounts();
            List<app.fedilab.android.mastodon.client.entities.api.Account> accountList = new ArrayList<>();
            for (BaseAccount account : accounts) {
                account.mastodon_account.acct += "@" + account.instance;
                accountList.add(account.mastodon_account);
            }
            if (accounts.size() == 1) {
                Intent intentToot = new Intent(context, ComposeActivity.class);
                intentToot.putExtras(bundle);
                context.startActivity(intentToot);
                ((BaseActivity) context).finish();
            } else {
                AlertDialog.Builder builderSingle = new MaterialAlertDialogBuilder(context);
                builderSingle.setTitle(context.getString(R.string.choose_accounts));
                final AccountsSearchAdapter accountsSearchAdapter = new AccountsSearchAdapter(context, accountList);
                final BaseAccount[] accountArray = new BaseAccount[accounts.size()];
                int i = 0;
                for (BaseAccount account : accounts) {
                    accountArray[i] = account;
                    i++;
                }
                builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builderSingle.setAdapter(accountsSearchAdapter, (dialog, which) -> {
                    final BaseAccount account = accountArray[which];
                    Intent intentToot = new Intent(context, ComposeActivity.class);
                    bundle.putSerializable(Helper.ARG_ACCOUNT, account);
                    new CachedBundle(context).insertBundle(bundle, Helper.getCurrentAccount(context), bundleId -> {
                        Bundle bundleCached = new Bundle();
                        bundleCached.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intentToot.putExtras(bundleCached);
                        context.startActivity(intentToot);
                        ((BaseActivity) context).finish();
                        dialog.dismiss();
                    });

                });
                builderSingle.show();
            }

        } catch (DBException e) {
            e.printStackTrace();
        }


    }


    /**
     * Fetch and federate the remote status
     */
    public static void fetchStatusInRemoteInstance(@NonNull Context context, String url, String instance, Callback callback) {

        MastodonSearchService mastodonSearchService = init(context, instance);
        new Thread(() -> {
            Call<Results> resultsCall = mastodonSearchService.search(null, url, null, "statuses", null, null, null, null, null, null, null);
            Results results = null;
            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {

                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.statuses == null) {
                                results.statuses = new ArrayList<>();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> {
                if (finalResults != null && finalResults.statuses != null && finalResults.statuses.size() > 0) {
                    callback.federatedStatus(finalResults.statuses.get(0));
                }
            };
            mainHandler.post(myRunnable);

        }).start();
    }


    /**
     * Fetch and federate the remote status
     */
    public static void fetchAccountInRemoteInstance(@NonNull Context context, String acct, String instance, Callback callback) {

        MastodonSearchService mastodonSearchService = init(context, instance);
        new Thread(() -> {
            Call<Results> resultsCall = mastodonSearchService.search(null, acct, null, "accounts", null, null, null, null, null, null, 1);
            Results results = null;
            if (resultsCall != null) {
                try {
                    Response<Results> resultsResponse = resultsCall.execute();
                    if (resultsResponse.isSuccessful()) {
                        results = resultsResponse.body();
                        if (results != null) {
                            if (results.accounts == null) {
                                results.accounts = new ArrayList<>();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Results finalResults = results;
            Runnable myRunnable = () -> {
                if (finalResults != null && finalResults.accounts != null && finalResults.accounts.size() > 0) {
                    callback.federatedAccount(finalResults.accounts.get(0));
                }
            };
            mainHandler.post(myRunnable);

        }).start();
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
        BOOKMARK_ACTION,
        UNBOOKMARK_ACTION,
        REBLOG_ACTION,
        UNREBLOG_ACTION,
        REPLY_ACTION,
        COMPOSE
    }


    public interface Callback {
        void federatedStatus(Status status);

        void federatedAccount(app.fedilab.android.mastodon.client.entities.api.Account account);
    }
}
