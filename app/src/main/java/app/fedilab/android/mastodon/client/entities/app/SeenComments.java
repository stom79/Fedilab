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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.List;

import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;


public class SeenComments implements Serializable {


    private transient final SQLiteDatabase db;
    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("context_status_id")
    public String context_status_id;
    @SerializedName("descendant_ids")
    public List<String> descendant_ids;

    public SeenComments() {
        db = null;
    }

    public SeenComments(Context context) {
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    public static String idListToStringStorage(List<String> ids) {
        Gson gson = new Gson();
        try {
            return gson.toJson(ids);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> restoreIdListFromString(String serializedIds) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedIds, new TypeToken<List<String>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

    public SeenComments getSeenComments(BaseAccount account, String contextStatusId) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_SEEN_COMMENTS, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND " + Sqlite.COL_CONTEXT_STATUS_ID + " = '" + contextStatusId + "'", null, null, null, null, "1");
            return convertCursorToSeenComments(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public long insertOrUpdate(BaseAccount account, String contextStatusId, List<String> descendantIds) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean insert = false;
        SeenComments seenComments = getSeenComments(account, contextStatusId);
        ContentValues values = new ContentValues();
        if (seenComments == null) {
            values.put(Sqlite.COL_INSTANCE, account.instance);
            values.put(Sqlite.COL_USER_ID, account.user_id);
            values.put(Sqlite.COL_CONTEXT_STATUS_ID, contextStatusId);
            insert = true;
        }
        values.put(Sqlite.COL_DESCENDANT_IDS, idListToStringStorage(descendantIds));
        try {
            if (insert) {
                return db.insertOrThrow(Sqlite.TABLE_SEEN_COMMENTS, null, values);
            } else {
                return db.update(Sqlite.TABLE_SEEN_COMMENTS,
                        values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " = ? AND " + Sqlite.COL_CONTEXT_STATUS_ID + " = ?",
                        new String[]{account.user_id, account.instance, contextStatusId});
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private SeenComments convertCursorToSeenComments(Cursor c) {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        c.moveToFirst();
        SeenComments seenComments = new SeenComments();
        seenComments.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        seenComments.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        seenComments.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        seenComments.context_status_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CONTEXT_STATUS_ID));
        seenComments.descendant_ids = restoreIdListFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_DESCENDANT_IDS)));
        c.close();
        return seenComments;
    }
}
