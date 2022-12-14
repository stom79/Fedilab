package app.fedilab.android.client.entities.app;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;


public class MutedAccounts implements Serializable {


    private transient final SQLiteDatabase db;
    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("type")
    public Timeline.TimeLineEnum type;
    @SerializedName("accounts")
    public List<Account> accounts;

    public MutedAccounts() {
        db = null;
    }

    public MutedAccounts(Context context) {
        //Creation of the DB with tables
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }


    /**
     * Serialized a list of BaseAccount class
     *
     * @param accounts List of {@link Account} to serialize
     * @return String serialized emoji list
     */
    public static String accountListToStringStorage(List<Account> accounts) {
        Gson gson = new Gson();
        try {
            return gson.toJson(accounts);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a BaseAccount List
     *
     * @param serializedAccounts String serialized BaseAccount list
     * @return List of {@link Account}
     */
    public static List<Account> restoreAccountsFromString(String serializedAccounts) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedAccounts, new TypeToken<List<Account>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Insert an Account in muted account in db
     *
     * @param forAccount {@link BaseAccount}
     * @param target     {@link Account}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long muteAccount(BaseAccount forAccount, Account target) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean insert = false;
        MutedAccounts mutedAccounts = getMutedAccount(forAccount);
        ContentValues values = new ContentValues();
        if (mutedAccounts == null) {
            mutedAccounts = new MutedAccounts();
            mutedAccounts.accounts = new ArrayList<>();
            mutedAccounts.type = Timeline.TimeLineEnum.HOME;
            values.put(Sqlite.COL_INSTANCE, forAccount.instance);
            values.put(Sqlite.COL_USER_ID, forAccount.user_id);
            insert = true;
            values.put(Sqlite.COL_TYPE, mutedAccounts.type.getValue());
        } else if (mutedAccounts.accounts == null) {
            mutedAccounts.accounts = new ArrayList<>();
        }
        if (!mutedAccounts.accounts.contains(target)) {
            mutedAccounts.accounts.add(target);
        }
        values.put(Sqlite.COL_MUTED_ACCOUNTS, accountListToStringStorage(mutedAccounts.accounts));

        //Inserts or updates
        try {
            if (insert) {
                return db.insertOrThrow(Sqlite.TABLE_MUTED, null, values);
            } else {
                return db.update(Sqlite.TABLE_MUTED,
                        values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " = ?",
                        new String[]{forAccount.user_id, forAccount.instance});
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Remove an Account in muted account in db
     *
     * @param forAccount {@link BaseAccount}
     * @param target     {@link Account}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long unMuteAccount(BaseAccount forAccount, Account target) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean insert = false;
        MutedAccounts mutedAccounts = getMutedAccount(forAccount);
        ContentValues values = new ContentValues();
        if (mutedAccounts == null) {
            mutedAccounts = new MutedAccounts();
            mutedAccounts.accounts = new ArrayList<>();
            mutedAccounts.type = Timeline.TimeLineEnum.HOME;
            values.put(Sqlite.COL_INSTANCE, forAccount.instance);
            values.put(Sqlite.COL_USER_ID, forAccount.user_id);
            insert = true;
            values.put(Sqlite.COL_TYPE, mutedAccounts.type.getValue());
        } else if (mutedAccounts.accounts == null) {
            mutedAccounts.accounts = new ArrayList<>();
        }
        mutedAccounts.accounts.remove(target);
        values.put(Sqlite.COL_MUTED_ACCOUNTS, accountListToStringStorage(mutedAccounts.accounts));

        //Inserts or updates
        try {
            if (insert) {
                return db.insertOrThrow(Sqlite.TABLE_MUTED, null, values);
            } else {
                return db.update(Sqlite.TABLE_MUTED,
                        values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " = ?",
                        new String[]{forAccount.user_id, forAccount.instance});
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the MutedAccounts for an account
     *
     * @param account Account
     * @return MutedAccounts - {@link MutedAccounts}
     */
    public MutedAccounts getMutedAccount(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_MUTED, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null, null, null, null, null);
            return convertCursorToMuted(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return MutedAccounts
     */
    private MutedAccounts convertCursorToMuted(Cursor c) {
        MutedAccounts mutedAccounts = new MutedAccounts();
        mutedAccounts.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        mutedAccounts.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        mutedAccounts.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        mutedAccounts.accounts = restoreAccountsFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_MUTED_ACCOUNTS)));
        mutedAccounts.type = Timeline.TimeLineEnum.valueOf(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TYPE)));
        return mutedAccounts;
    }
}
