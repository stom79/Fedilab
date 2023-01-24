package app.fedilab.android.peertube.helper;
/* Copyright 2023 Thomas Schneider
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

import static app.fedilab.android.mastodon.helper.Helper.PREF_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.mastodon.helper.Helper.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.peertube.activities.LoginActivity;
import app.fedilab.android.peertube.drawer.OwnAccountsAdapter;

public class SwitchAccountHelper {


    public static void switchDialog(Activity activity, boolean withAddAccount) {
        List<BaseAccount> accounts = null;
        try {
            accounts = new Account(activity).getAll();
        } catch (DBException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
        builderSingle.setTitle(activity.getString(R.string.list_of_accounts));
        if (accounts != null) {
            final OwnAccountsAdapter accountsListAdapter = new OwnAccountsAdapter(activity, accounts);
            final BaseAccount[] accountArray = new BaseAccount[accounts.size()];
            int i = 0;
            for (BaseAccount account : accounts) {
                accountArray[i] = account;
                i++;
            }
            builderSingle.setAdapter(accountsListAdapter, (dialog, which) -> {
                final BaseAccount account = accountArray[which];
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(PREF_USER_TOKEN, account.token);
                editor.putString(PREF_USER_SOFTWARE, account.software);
                Log.v(TAG, "put 1: " + account.software);
                editor.putString(PREF_INSTANCE, account.instance);
                editor.putString(PREF_USER_ID, account.user_id);
                editor.apply();
                dialog.dismiss();
                Intent intent = new Intent(activity, BaseMainActivity.class);
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
