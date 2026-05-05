package app.fedilab.android.activities;
/* Copyright 2025 Thomas Schneider
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.AccountsSearchAdapter;
import app.fedilab.android.R;


public class WebActivityPub extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent appIntent = getIntent();
        if (appIntent == null) {
            finish();
            return;
        }
        Uri uri = appIntent.getData();
        if (uri == null) {
            finish();
            return;
        }
        String scheme = uri.getScheme();
        String uriString = uri.toString();
        if (!uriString.startsWith(scheme + "://")) {
            uriString = uriString.replace(scheme + ":", scheme + "://");
            uri = Uri.parse(uriString);
            if (uri == null) {
                finish();
                return;
            }
        }

        String host = uri.getHost();
        String path = uri.getPath();

        if (host == null || path == null || path.isEmpty()) {
            finish();
            return;
        }

        String httpsUrl = "https://" + host + path;
        String acct = null;
        boolean isStatus = false;

        if (path.startsWith("/@")) {
            if (path.matches("/@[^/]+/\\d+")) {
                isStatus = true;
            } else {
                String[] params = path.split("@");
                if (params.length == 2) {
                    acct = params[1] + "@" + host;
                } else if (params.length == 3) {
                    acct = params[1] + "@" + params[2];
                }
            }
        } else if (path.split("/").length > 2) {
            String[] params = path.split("/");
            String root = params[1].toLowerCase();
            if (root.equals("users") && params.length == 3) {
                acct = params[2] + "@" + host;
            } else {
                isStatus = true;
            }
        }

        if (acct != null) {
            String finalAcct = acct;
            chooseAccount(account -> openProfile(account, finalAcct));
        } else if (isStatus) {
            String finalHttpsUrl = httpsUrl;
            chooseAccount(account -> openStatus(account, finalHttpsUrl));
        } else {
            finish();
        }
    }

    private interface AccountSelectedCallback {
        void onAccountSelected(BaseAccount account);
    }

    private void chooseAccount(AccountSelectedCallback callback) {
        new Thread(() -> {
            try {
                List<BaseAccount> accounts = new app.fedilab.android.mastodon.client.entities.app.Account(WebActivityPub.this).getCrossAccounts();
                if (accounts == null || accounts.isEmpty()) {
                    runOnUiThread(this::finish);
                    return;
                }
                if (accounts.size() == 1) {
                    runOnUiThread(() -> callback.onAccountSelected(accounts.get(0)));
                } else {
                    List<Account> accountList = new ArrayList<>();
                    for (BaseAccount account : accounts) {
                        account.mastodon_account.acct += "@" + account.instance;
                        accountList.add(account.mastodon_account);
                    }
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        AlertDialog.Builder builderSingle = new MaterialAlertDialogBuilder(WebActivityPub.this);
                        builderSingle.setTitle(getString(R.string.choose_accounts));
                        final AccountsSearchAdapter accountsSearchAdapter = new AccountsSearchAdapter(WebActivityPub.this, accountList);
                        final BaseAccount[] accountArray = new BaseAccount[accounts.size()];
                        int i = 0;
                        for (BaseAccount account : accounts) {
                            accountArray[i] = account;
                            i++;
                        }
                        builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        });
                        builderSingle.setOnCancelListener(dialog -> finish());
                        builderSingle.setAdapter(accountsSearchAdapter, (dialog, which) -> {
                            callback.onAccountSelected(accountArray[which]);
                            dialog.dismiss();
                        });
                        builderSingle.show();
                    });
                }
            } catch (DBException e) {
                e.printStackTrace();
                runOnUiThread(this::finish);
            }
        }).start();
    }

    private void switchAccount(BaseAccount selectedAccount) {
        BaseMainActivity.currentToken = selectedAccount.token;
        BaseMainActivity.currentUserID = selectedAccount.user_id;
        BaseMainActivity.currentInstance = selectedAccount.instance;
        Helper.setCurrentAccount(selectedAccount);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(Helper.PREF_USER_TOKEN, selectedAccount.token);
        editor.putString(Helper.PREF_USER_INSTANCE, selectedAccount.instance);
        editor.putString(Helper.PREF_USER_ID, selectedAccount.user_id);
        editor.commit();
    }

    private void openProfile(BaseAccount selectedAccount, String acct) {
        switchAccount(selectedAccount);
        Intent intentProfile = new Intent(WebActivityPub.this, ProfileActivity.class);
        Bundle args = new Bundle();
        args.putString(Helper.ARG_MENTION, acct);
        new CachedBundle(WebActivityPub.this).insertBundle(args, selectedAccount, bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            intentProfile.putExtras(bundle);
            intentProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentProfile);
            finish();
        });
    }

    private void openStatus(BaseAccount selectedAccount, String httpsUrl) {
        switchAccount(selectedAccount);
        Intent mainActivity = new Intent(WebActivityPub.this, MainActivity.class);
        mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_WITH_ANOTHER_ACCOUNT);
        mainActivity.putExtra(Helper.PREF_MESSAGE_URL, httpsUrl);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivity);
        finish();
    }
}
