package app.fedilab.android.mastodon.client.entities.app;
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.sqlite.Sqlite;

/**
 * Class that manages Accounts from database
 * Accounts details are serialized and can be for different softwares
 * The type of the software is stored in api field
 */
public class Account extends BaseAccount implements Serializable {


    private final SQLiteDatabase db;

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
     * Serialized a Mastodon BaseAccount class
     *
     * @param mastodon_account {@link BaseAccount} to serialize
     * @return String serialized account
     */
    public static String mastodonAccountToStringStorage(app.fedilab.android.mastodon.client.entities.api.Account mastodon_account) {
        Gson gson = new Gson();
        try {
            return gson.toJson(mastodon_account);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized a Peertube BaseAccount class
     *
     * @param peertube_account {@link AccountData.PeertubeAccount} to serialize
     * @return String serialized account
     */
    public static String peertubeAccountToStringStorage(AccountData.PeertubeAccount peertube_account) {
        Gson gson = new Gson();
        try {
            return gson.toJson(peertube_account);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a Mastodon BaseAccount
     *
     * @param serializedAccount String serialized account
     * @return {@link app.fedilab.android.mastodon.client.entities.api.Account}
     */
    public static app.fedilab.android.mastodon.client.entities.api.Account restoreAccountFromString(String serializedAccount) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedAccount, app.fedilab.android.mastodon.client.entities.api.Account.class);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Unserialized a Peertube account AccountData.PeertubeAccount
     *
     * @param serializedAccount String serialized account
     * @return {@link AccountData.PeertubeAccount}
     */
    public static AccountData.PeertubeAccount restorePeertubeAccountFromString(String serializedAccount) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedAccount, AccountData.PeertubeAccount.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all BaseAccount in db
     *
     * @return BaseAccount List<BaseAccount>
     */
    public List<BaseAccount> getPushNotificationAccounts() {

        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, "(" + Sqlite.COL_API + " = 'MASTODON' OR " + Sqlite.COL_API + " = 'PLEROMA' OR " + Sqlite.COL_API + " = 'FRIENDICA') AND " + Sqlite.COL_TOKEN + " IS NOT NULL", null, null, null, Sqlite.COL_INSTANCE + " ASC", null);
            return cursorToListUserWithOwner(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all BaseAccount in db
     *
     * @return BaseAccount List<BaseAccount>
     */
    public List<BaseAccount> getPeertubeAccounts() {

        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, Sqlite.COL_API + " = 'PEERTUBE' AND " + Sqlite.COL_TOKEN + " IS NOT NULL", null, null, null, Sqlite.COL_INSTANCE + " ASC", null);
            return cursorToListUserWithOwner(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert or update a user
     *
     * @param account {@link BaseAccount}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(BaseAccount account) throws DBException {
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
     * @param account {@link BaseAccount}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertAccount(BaseAccount account) throws DBException {
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
        values.put(Sqlite.COL_ADMIN, account.admin);
        if (account.mastodon_account != null) {
            values.put(Sqlite.COL_ACCOUNT, mastodonAccountToStringStorage(account.mastodon_account));
        }
        if (account.peertube_account != null) {
            values.put(Sqlite.COL_ACCOUNT, peertubeAccountToStringStorage(account.peertube_account));
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
     * @param account {@link BaseAccount}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateAccount(BaseAccount account) throws DBException {
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
            values.put(Sqlite.COL_ADMIN, account.admin);
        }
        if (account.mastodon_account != null) {
            values.put(Sqlite.COL_ACCOUNT, mastodonAccountToStringStorage(account.mastodon_account));
        }
        if (account.peertube_account != null) {
            values.put(Sqlite.COL_ACCOUNT, peertubeAccountToStringStorage(account.peertube_account));
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
     * Update an account in db
     *
     * @param token {@link Token}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long updatePeertubeToken(Token token) throws DBException {
        ContentValues values = new ContentValues();
        if (token.getRefresh_token() != null) {
            values.put(Sqlite.COL_REFRESH_TOKEN, token.getRefresh_token());
        }
        if (token.getAccess_token() != null) {
            values.put(Sqlite.COL_TOKEN, token.getAccess_token());
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = sharedpreferences.getString(Helper.PREF_USER_ID, null);
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
     * Check if a user exists in db
     *
     * @param account BaseAccount {@link BaseAccount}
     * @return boolean - user exists
     * @throws DBException Exception
     */
    public boolean accountExist(BaseAccount account) throws DBException {
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
     * Returns an BaseAccount by userId and instance
     *
     * @param userId   String
     * @param instance String
     * @return BaseAccount {@link BaseAccount}
     */
    public BaseAccount getUniqAccount(String userId, String instance) throws DBException {
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
     * Returns an BaseAccount by token
     *
     * @param token String
     * @return BaseAccount {@link BaseAccount}
     */
    public BaseAccount getAccountByToken(String token) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, Sqlite.COL_TOKEN + " = \"" + token + "\"", null, null, null, null, "1");
            return cursorToUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns authenticated BaseAccount
     *
     * @return BaseAccount {@link BaseAccount}
     */
    public BaseAccount getConnectedAccount() throws DBException {
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
     * @return BaseAccount List<{@link BaseAccount}>
     */
    public List<BaseAccount> getOtherAccounts() throws DBException {
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
     * Returns all accounts that allows cross-account actions
     *
     * @return BaseAccount List<{@link BaseAccount}>
     */
    public List<BaseAccount> getCrossAccounts() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, null, null, null, null, null, null);
            return cursorToListMastodonUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns all accounts
     *
     * @return BaseAccount List<{@link BaseAccount}>
     */
    public List<BaseAccount> getAll() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, null, null, null, null, null, null);
            return cursorToListUserWithOwner(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns last used account
     *
     * @return BaseAccount  {@link BaseAccount}
     */
    public BaseAccount getLastUsedAccount() throws DBException {
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
     * Returns last used account
     *
     * @return BaseAccount  {@link BaseAccount}
     */
    public List<BaseAccount> getLastUsedAccounts() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_USER_ACCOUNT, null, null, null, null, null, Sqlite.COL_UPDATED_AT + " DESC", null);
            return cursorToListUser(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove an account from db
     *
     * @param account {@link BaseAccount}
     * @return int
     */
    public int removeUser(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_USER_ACCOUNT, Sqlite.COL_USER_ID + " = '" + account.user_id +
                "' AND " + Sqlite.COL_INSTANCE + " = '" + account.instance + "'", null);
    }


    private List<BaseAccount> cursorToListUser(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<BaseAccount> accountList = new ArrayList<>();
        while (c.moveToNext()) {
            BaseAccount account = convertCursorToAccount(c);
            //We don't add in the list the current connected account
            if (!account.token.equalsIgnoreCase(BaseMainActivity.currentToken)) {
                accountList.add(account);
            }
        }
        //Close the cursor
        c.close();
        return accountList;
    }


    private List<BaseAccount> cursorToListMastodonUser(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<BaseAccount> accountList = new ArrayList<>();
        while (c.moveToNext()) {
            BaseAccount account = convertCursorToAccount(c);
            //We don't add in the list the current connected account
            if (account.mastodon_account != null) {
                accountList.add(account);
            }
        }
        //Close the cursor
        c.close();
        return accountList;
    }


    private List<BaseAccount> cursorToListUserWithOwner(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<BaseAccount> accountList = new ArrayList<>();
        while (c.moveToNext()) {
            BaseAccount account = convertCursorToAccount(c);
            accountList.add(account);
        }
        //Close the cursor
        c.close();
        return accountList;
    }

    /***
     * Method to hydrate an BaseAccount from database
     * @param c Cursor
     * @return BaseAccount {@link BaseAccount}
     */
    private BaseAccount cursorToUser(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        //New user
        BaseAccount account = convertCursorToAccount(c);
        //Close the cursor
        c.close();
        return account;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return BaseAccount
     */
    private BaseAccount convertCursorToAccount(Cursor c) {
        BaseAccount account = new BaseAccount();
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
        account.admin = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ADMIN)) == 1;
        String apiStr = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_API));
        API api;
        switch (apiStr) {
            case "MASTODON":
                api = API.MASTODON;
                break;
            case "FRIENDICA":
                api = API.FRIENDICA;
                break;
            case "PIXELFED":
                api = API.PIXELFED;
                break;
            case "AKKOMA":
            case "PLEROMA":
                api = API.PLEROMA;
                break;
            case "PEERTUBE":
                api = API.PEERTUBE;
                break;
            case "MISSKEY":
                api = API.MISSKEY;
                break;
            default:
                api = API.UNKNOWN;
                break;
        }
        account.api = api;
        if (api != API.PEERTUBE) {
            account.mastodon_account = restoreAccountFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ACCOUNT)));
        } else {
            account.peertube_account = restorePeertubeAccountFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ACCOUNT)));
        }
        return account;
    }

    public enum API {
        MASTODON,
        FRIENDICA,
        PLEROMA,
        PIXELFED,
        PEERTUBE,
        MISSKEY,
        UNKNOWN
    }
}
