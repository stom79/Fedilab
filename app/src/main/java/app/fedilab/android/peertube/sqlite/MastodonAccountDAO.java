package app.fedilab.android.peertube.sqlite;
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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.mastodon.MastodonAccount.Account;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;


@SuppressWarnings("UnusedReturnValue")
public class MastodonAccountDAO {

    private final SQLiteDatabase db;
    public Context context;


    public MastodonAccountDAO(Context context, SQLiteDatabase db) {
        //Creation of the DB with tables
        this.context = context;
        this.db = db;
    }


    /**
     * Insert an Account in database
     *
     * @param account Account
     * @return boolean
     */
    public boolean insertAccount(Account account) {
        ContentValues values = new ContentValues();
        if (account.getCreatedAt() == null)
            account.setCreatedAt(new Date());
        if (account.getDescription() == null)
            account.setDescription("");
        values.put(Sqlite.COL_USER_ID, account.getId());
        values.put(Sqlite.COL_USERNAME, account.getUsername());
        values.put(Sqlite.COL_ACCT, account.getUsername() + "@" + account.getHost());
        values.put(Sqlite.COL_DISPLAYED_NAME, account.getDisplayName() != null ? account.getDisplayName() : account.getUsername());
        values.put(Sqlite.COL_FOLLOWERS_COUNT, account.getFollowersCount());
        values.put(Sqlite.COL_FOLLOWING_COUNT, account.getFollowingCount());
        values.put(Sqlite.COL_NOTE, account.getDescription());
        values.put(Sqlite.COL_URL, account.getUrl());
        values.put(Sqlite.COL_AVATAR, account.getAvatar());
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(account.getCreatedAt()));
        values.put(Sqlite.COL_INSTANCE, account.getHost());
        values.put(Sqlite.COL_LOCKED, false);
        values.put(Sqlite.COL_STATUSES_COUNT, 0);
        values.put(Sqlite.COL_URL, "");
        values.put(Sqlite.COL_AVATAR_STATIC, "");
        values.put(Sqlite.COL_HEADER, "");
        values.put(Sqlite.COL_HEADER_STATIC, "");
        if (account.getSoftware() != null && account.getSoftware().toUpperCase().trim().compareTo("PEERTUBE") != 0) {
            values.put(Sqlite.COL_SOFTWARE, account.getSoftware().toUpperCase().trim());
        }
        if (account.getClient_id() != null && account.getClient_secret() != null) {
            values.put(Sqlite.COL_CLIENT_ID, account.getClient_id());
            values.put(Sqlite.COL_CLIENT_SECRET, account.getClient_secret());
        }
        if (account.getRefresh_token() != null) {
            values.put(Sqlite.COL_REFRESH_TOKEN, account.getRefresh_token());
        }
        if (account.getToken() != null)
            values.put(Sqlite.COL_OAUTHTOKEN, account.getToken());

        //Inserts account
        try {
            db.insertOrThrow(Sqlite.TABLE_USER_ACCOUNT, null, values);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Update an Account in database
     *
     * @param account Account
     * @return boolean
     */
    public int updateAccount(Account account) {
        ContentValues values = new ContentValues();
        if (account.getCreatedAt() == null)
            account.setCreatedAt(new Date());
        if (account.getDescription() == null)
            account.setDescription("");
        values.put(Sqlite.COL_USER_ID, account.getId());
        values.put(Sqlite.COL_USERNAME, account.getUsername());
        values.put(Sqlite.COL_ACCT, account.getUsername() + "@" + account.getHost());
        values.put(Sqlite.COL_DISPLAYED_NAME, account.getDisplayName());
        values.put(Sqlite.COL_FOLLOWERS_COUNT, account.getFollowersCount());
        values.put(Sqlite.COL_FOLLOWING_COUNT, account.getFollowingCount());
        values.put(Sqlite.COL_NOTE, account.getDescription());
        values.put(Sqlite.COL_URL, account.getUrl());
        values.put(Sqlite.COL_AVATAR, account.getAvatar());
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(account.getCreatedAt()));

        try {
            return db.update(Sqlite.TABLE_USER_ACCOUNT,
                    values, Sqlite.COL_USERNAME + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{account.getUsername(), account.getHost()});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }


    /**
     * Update an Account token in database
     *
     * @param token Token
     * @return boolean
     */
    public int updateAccountToken(Token token) {
        ContentValues values = new ContentValues();
        if (token.getRefresh_token() != null) {
            values.put(Sqlite.COL_REFRESH_TOKEN, token.getRefresh_token());
        }
        if (token.getAccess_token() != null)
            values.put(Sqlite.COL_OAUTHTOKEN, token.getAccess_token());
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        String userId = sharedpreferences.getString(Helper.PREF_KEY_ID, null);
        String instance = HelperInstance.getLiveInstance(context);
        try {
            return db.update(Sqlite.TABLE_USER_ACCOUNT,
                    values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{userId, instance});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    /**
     * Update an Account in database
     *
     * @param account Account
     * @return boolean
     */
    public int updateAccountCredential(Account account) {
        ContentValues values = new ContentValues();
        if (account.getCreatedAt() == null)
            account.setCreatedAt(new Date());
        if (account.getDescription() == null)
            account.setDescription("");
        values.put(Sqlite.COL_USERNAME, account.getUsername());
        values.put(Sqlite.COL_ACCT, account.getUsername() + "@" + account.getHost());
        values.put(Sqlite.COL_DISPLAYED_NAME, account.getDisplayName());
        values.put(Sqlite.COL_FOLLOWERS_COUNT, account.getFollowersCount());
        values.put(Sqlite.COL_FOLLOWING_COUNT, account.getFollowingCount());
        values.put(Sqlite.COL_NOTE, account.getDescription());
        values.put(Sqlite.COL_URL, account.getUrl());
        values.put(Sqlite.COL_AVATAR, account.getAvatar());
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(account.getCreatedAt()));

        if (account.getClient_id() != null && account.getClient_secret() != null) {
            values.put(Sqlite.COL_CLIENT_ID, account.getClient_id());
            values.put(Sqlite.COL_CLIENT_SECRET, account.getClient_secret());
        }
        if (account.getRefresh_token() != null) {
            values.put(Sqlite.COL_REFRESH_TOKEN, account.getRefresh_token());
        }
        if (account.getToken() != null)
            values.put(Sqlite.COL_OAUTHTOKEN, account.getToken());
        return db.update(Sqlite.TABLE_USER_ACCOUNT,
                values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                new String[]{account.getId(), account.getHost()});
    }


    public int removeUser(Account account) {
        return db.delete(Sqlite.TABLE_USER_ACCOUNT, Sqlite.COL_USER_ID + " = '" + account.getId() +
                "' AND " + Sqlite.COL_INSTANCE + " = '" + account.getHost() + "'", null);
    }


    /**
     * Test if the current user is already stored in data base
     *
     * @param account Account
     * @return boolean
     */
    public boolean userExist(Account account) {
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_USER_ACCOUNT
                + " where " + Sqlite.COL_USERNAME + " = '" + account.getUsername() + "' AND " + Sqlite.COL_INSTANCE + " = '" + account.getHost() + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }


}
