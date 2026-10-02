package app.fedilab.android.mastodon.client.entities.app;
/* Copyright 2026 Thomas Schneider
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

import java.util.LinkedHashMap;
import java.util.Map;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;


public class LabeledAccounts {

    private transient final SQLiteDatabase db;

    public LabeledAccounts(Context context) {
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Store the color of an account, replacing the one already set
     *
     * @param forAccount {@link BaseAccount}
     * @param target     {@link Account}
     * @param color      int - color of the label
     * @return long - db id
     * @throws DBException exception with database
     */
    public long label(BaseAccount forAccount, Account target, int color) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_COLOR, color);
        try {
            int updated = db.update(Sqlite.TABLE_LABELED_ACCOUNTS, values,
                    Sqlite.COL_USER_ID + " = ? AND " + Sqlite.COL_INSTANCE + " = ? AND " + Sqlite.COL_ACCOUNT_ID + " = ?",
                    new String[]{forAccount.user_id, forAccount.instance, target.id});
            if (updated > 0) {
                return updated;
            }
            values.put(Sqlite.COL_INSTANCE, forAccount.instance);
            values.put(Sqlite.COL_USER_ID, forAccount.user_id);
            values.put(Sqlite.COL_ACCOUNT_ID, target.id);
            values.put(Sqlite.COL_ACCT, target.acct);
            return db.insertOrThrow(Sqlite.TABLE_LABELED_ACCOUNTS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Remove the color of an account
     *
     * @param forAccount {@link BaseAccount}
     * @param target     {@link Account}
     * @throws DBException exception with database
     */
    public void removeLabel(BaseAccount forAccount, Account target) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            db.delete(Sqlite.TABLE_LABELED_ACCOUNTS,
                    Sqlite.COL_USER_ID + " = ? AND " + Sqlite.COL_INSTANCE + " = ? AND " + Sqlite.COL_ACCOUNT_ID + " = ?",
                    new String[]{forAccount.user_id, forAccount.instance, target.id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Colors of all the labeled accounts
     *
     * @param forAccount {@link BaseAccount}
     * @return Map - color for each account id
     * @throws DBException exception with database
     */
    public Map<String, Integer> getLabels(BaseAccount forAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Map<String, Integer> labels = new LinkedHashMap<>();
        try (Cursor c = db.query(Sqlite.TABLE_LABELED_ACCOUNTS, null,
                Sqlite.COL_USER_ID + " = ? AND " + Sqlite.COL_INSTANCE + " = ?",
                new String[]{forAccount.user_id, forAccount.instance}, null, null, Sqlite.COL_ACCT + " ASC")) {
            while (c.moveToNext()) {
                labels.put(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ACCOUNT_ID)),
                        c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_COLOR)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return labels;
    }
}
