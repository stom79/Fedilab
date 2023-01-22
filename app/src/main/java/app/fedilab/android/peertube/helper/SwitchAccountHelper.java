package app.fedilab.android.peertube.helper;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.LoginActivity;
import app.fedilab.android.peertube.activities.MainActivity;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.drawer.OwnAccountsAdapter;
import app.fedilab.android.peertube.sqlite.AccountDAO;
import app.fedilab.android.peertube.sqlite.Sqlite;

public class SwitchAccountHelper {


    public static void switchDialog(Activity activity, boolean withAddAccount) {
        SQLiteDatabase db = Sqlite.getInstance(activity.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        List<AccountData.Account> accounts = new AccountDAO(activity, db).getAllAccount();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
        builderSingle.setTitle(activity.getString(R.string.list_of_accounts));
        if (accounts != null) {
            final OwnAccountsAdapter accountsListAdapter = new OwnAccountsAdapter(activity, accounts);
            final AccountData.Account[] accountArray = new AccountData.Account[accounts.size()];
            int i = 0;
            for (AccountData.Account account : accounts) {
                accountArray[i] = account;
                i++;
            }
            builderSingle.setAdapter(accountsListAdapter, (dialog, which) -> {
                final AccountData.Account account = accountArray[which];
                SharedPreferences sharedpreferences = activity.getSharedPreferences(Helper.APP_PREFS, MODE_PRIVATE);
                boolean remote_account = account.getSoftware() != null && account.getSoftware().toUpperCase().trim().compareTo("PEERTUBE") != 0;
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, account.getToken());
                editor.putString(Helper.PREF_SOFTWARE, remote_account ? account.getSoftware() : null);
                editor.putString(Helper.PREF_REMOTE_INSTANCE, remote_account ? account.getHost() : null);
                if (!remote_account) {
                    editor.putString(Helper.PREF_INSTANCE, account.getHost());
                }
                editor.putString(Helper.PREF_KEY_ID, account.getId());
                editor.putString(Helper.PREF_KEY_NAME, account.getUsername());
                editor.apply();
                dialog.dismiss();
                Intent intent = new Intent(activity, MainActivity.class);
                activity.startActivity(intent);
                activity.finish();
            });
        }
        builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        if (withAddAccount) {
            builderSingle.setPositiveButton(R.string.add_account, (dialog, which) -> {
                Intent intent = new Intent(activity, LoginActivity.class);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        builderSingle.show();
    }
}
