package app.fedilab.android.client.entities;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

/**
 * Class that manages Accounts from database
 * Accounts details are serialized and can be for different softwares
 * The type of the software is stored in api field
 */
public class Account implements Serializable {


    private final SQLiteDatabase db;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("api")
    public API api;
    @SerializedName("software")
    public String software;
    @SerializedName("token")
    public String token;
    @SerializedName("refresh_token")
    public String refresh_token;
    @SerializedName("token_validity")
    public long token_validity;
    @SerializedName("client_id")
    public String client_id;
    @SerializedName("client_secret")
    public String client_secret;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("mastodon_account")
    public app.fedilab.android.client.mastodon.entities.Account mastodon_account;

    private transient Context context;

    public Account() {
        db = null;
    }

    public Account(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a Mastodon Account class
     *
     * @param mastodon_account {@link app.fedilab.android.client.mastodon.entities.Account} to serialize
     * @return String serialized account
     */
    public static String mastodonAccountToStringStorage(app.fedilab.android.client.mastodon.entities.Account mastodon_account) {
        Gson gson = new Gson();
        try {
            return gson.toJson(mastodon_account);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a Mastodon Account
     *
     * @param serializedAccount String serialized account
     * @return {@link app.fedilab.android.client.mastodon.entities.Account}
     */
    public static app.fedilab.android.client.mastodon.entities.Account restoreAccountFromString(String serializedAccount) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedAccount, app.fedilab.android.client.mastodon.entities.Account.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all Account in db
     *
     * @return Account List<Account>
     */
    public List<Account> getPushNotificationAccounts() {

        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, "(" + Sqlite.COL_API + " = 'MASTODON' OR " + Sqlite.COL_API + " = 'PLEROMA') AND " + Sqlite.COL_TOKEN + " IS NOT NULL", null, null, null, Sqlite.COL_INSTANCE + " ASC", null);
            return cursorToListUserWithOwner(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert or update a user
     *
     * @param account {@link Account}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean exists = accountExist(account);
        long idReturned;
        if (exists) {
            idReturned = updateAccount(account);
        } else {
            idReturned = insertAccount(account);
        }
        return idReturned;
    }

    /**
     * Insert an account in db
     *
     * @param account {@link Account}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertAccount(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_APP_CLIENT_ID, account.client_id);
        values.put(Sqlite.COL_APP_CLIENT_SECRET, account.client_secret);
        values.put(Sqlite.COL_USER_ID, account.user_id);
        values.put(Sqlite.COL_INSTANCE, account.instance);
        values.put(Sqlite.COL_API, account.api.name());
        values.put(Sqlite.COL_SOFTWARE, account.software);
        values.put(Sqlite.COL_TOKEN_VALIDITY, account.token_validity);
        values.put(Sqlite.COL_TOKEN, account.token);
        values.put(Sqlite.COL_REFRESH_TOKEN, account.refresh_token);
        if (account.mastodon_account != null) {
            values.put(Sqlite.COL_ACCOUNT, mastodonAccountToStringStorage(account.mastodon_account));
        }
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.insertOrThrow(Sqlite.TABLE_USER_ACCOUNT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Update an account in db
     *
     * @param account {@link Account}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateAccount(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        //Can be null if only retrieving account details - IE : not the whole authentication process
        if (account.client_id != null) {
            values.put(Sqlite.COL_APP_CLIENT_ID, account.client_id);
            values.put(Sqlite.COL_APP_CLIENT_SECRET, account.client_secret);
            values.put(Sqlite.COL_API, account.api.name());
            values.put(Sqlite.COL_SOFTWARE, account.software);
            values.put(Sqlite.COL_TOKEN_VALIDITY, account.token_validity);
            values.put(Sqlite.COL_TOKEN, account.token);
            values.put(Sqlite.COL_REFRESH_TOKEN, account.refresh_token);
        }
        if (account.mastodon_account != null) {
            values.put(Sqlite.COL_ACCOUNT, mastodonAccountToStringStorage(account.mastodon_account));
        }
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_USER_ACCOUNT,
                    values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{account.user_id, account.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Check if a user exists in db
     *
     * @param account Account {@link Account}
     * @return boolean - user exists
     * @throws DBException Exception
     */
    public boolean accountExist(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_USER_ACCOUNT
                + " where " + Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND " + Sqlite.COL_INSTANCE + " = '" + account.instance + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Returns an Account by token
     *
     * @param userId   String
     * @param instance String
     * @return Account {@link Account}
     */
    public Account getUniqAccount(String userId, String instance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, Sqlite.COL_USER_ID + " = \"" + userId + "\" AND " + Sqlite.COL_INSTANCE + " = \"" + instance + "\"", null, null, null, null, "1");
            return cursorToUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns authenticated Account
     *
     * @return Account {@link Account}
     */
    public Account getConnectedAccount() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, Sqlite.COL_TOKEN + " = '" + BaseMainActivity.currentToken + "'", null, null, null, null, "1");
            return cursorToUser(c);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns all accounts that allows cross-account actions
     *
     * @return Account List<{@link Account}>
     */
    public List<Account> getCrossAccounts() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, Sqlite.COL_API + " = 'MASTODON'", null, null, null, null, null);
            return cursorToListUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all accounts
     *
     * @return Account List<{@link Account}>
     */
    public List<Account> getAll() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, null, null, null, null, null, null);
            return cursorToListUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns last used account
     *
     * @return Account  {@link Account}
     */
    public Account getLastUsedAccount() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, null, null, null, null, Sqlite.COL_UPDATED_AT + " DESC", "1");
            return cursorToUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove an account from db
     *
     * @param account {@link Account}
     * @return int
     */
    public int removeUser(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_USER_ACCOUNT, Sqlite.COL_USER_ID + " = '" + account.user_id +
                "' AND " + Sqlite.COL_INSTANCE + " = '" + account.instance + "'", null);
    }


    private List<Account> cursorToListUser(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Account> accountList = new ArrayList<>();
        while (c.moveToNext()) {
            Account account = convertCursorToAccount(c);
            //We don't add in the list the current connected account
            if (!account.token.equalsIgnoreCase(BaseMainActivity.currentToken)) {
                accountList.add(account);
            }
        }
        //Close the cursor
        c.close();
        return accountList;
    }

    private List<Account> cursorToListUserWithOwner(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Account> accountList = new ArrayList<>();
        while (c.moveToNext()) {
            Account account = convertCursorToAccount(c);
            //We don't add in the list the current connected account
            accountList.add(account);
        }
        //Close the cursor
        c.close();
        return accountList;
    }

    /***
     * Method to hydrate an Account from database
     * @param c Cursor
     * @return Account {@link Account}
     */
    private Account cursorToUser(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        //New user
        Account account = convertCursorToAccount(c);
        //Close the cursor
        c.close();
        return account;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Account
     */
    private Account convertCursorToAccount(Cursor c) {
        Account account = new Account();
        account.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        account.client_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_APP_CLIENT_ID));
        account.client_secret = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_APP_CLIENT_SECRET));
        account.token = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TOKEN));
        account.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        account.refresh_token = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_REFRESH_TOKEN));
        account.token_validity = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_TOKEN_VALIDITY));
        account.created_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
        account.updated_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_UPDATED_AT)));
        account.software = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SOFTWARE));
        String apiStr = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_API));
        API api = null;
        switch (apiStr) {
            case "MASTODON":
                api = API.MASTODON;
                break;
            case "PEERTUBE":
                api = API.PEERTUBE;
                break;
            case "PIXELFED":
                api = API.PIXELFED;
                break;
        }
        account.api = api;
        if (api == API.MASTODON) {
            account.mastodon_account = restoreAccountFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ACCOUNT)));
        }

        return account;
    }

    public enum API {
        MASTODON,
        PEERTUBE,
        PIXELFED
    }
}
