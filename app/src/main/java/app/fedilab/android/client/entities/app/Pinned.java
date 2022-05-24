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
import java.util.Date;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;


public class Pinned implements Serializable {
    private final SQLiteDatabase db;

    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("pinnedTimelines")
    public List<PinnedTimeline> pinnedTimelines;
    @SerializedName("created_at")
    public Date created_ad;
    @SerializedName("updated_at")
    public Date updated_at;
    private Context context;

    public Pinned() {
        db = null;
    }

    public Pinned(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a list of PinnedTimeline class
     *
     * @param pinnedTimelines List of {@link PinnedTimeline} to serialize
     * @return String serialized pinnedTimelines list
     */
    public static String mastodonPinnedTimelinesToStringStorage(List<PinnedTimeline> pinnedTimelines) {
        Gson gson = new Gson();
        try {
            return gson.toJson(pinnedTimelines);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a PinnedTimeline List
     *
     * @param serializedPinnedTimelines String serialized PinnedTimeline list
     * @return List of {@link PinnedTimeline}
     */
    public static List<PinnedTimeline> restorePinnedTimelinesFromString(String serializedPinnedTimelines) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedPinnedTimelines, new TypeToken<List<PinnedTimeline>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert pinnedTimeline in db
     *
     * @param pinned {@link Pinned}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertPinned(Pinned pinned) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, BaseMainActivity.currentInstance);
        values.put(Sqlite.COL_USER_ID, BaseMainActivity.currentUserID);
        values.put(Sqlite.COL_PINNED_TIMELINES, mastodonPinnedTimelinesToStringStorage(pinned.pinnedTimelines));
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        //Inserts pinned
        try {
            return db.insertOrThrow(Sqlite.TABLE_PINNED_TIMELINES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * update pinned in db
     *
     * @param pinned {@link Pinned}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long updatePinned(Pinned pinned) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_PINNED_TIMELINES, mastodonPinnedTimelinesToStringStorage(pinned.pinnedTimelines));
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_PINNED_TIMELINES,
                    values, Sqlite.COL_INSTANCE + " =  ? AND " + Sqlite.COL_USER_ID + " = ?",
                    new String[]{pinned.instance, pinned.user_id});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the pinned timeline for an account
     *
     * @param account Account
     * @return Pinned - {@link Pinned}
     */
    public Pinned getPinned(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_PINNED_TIMELINES, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null, null, null, Sqlite.COL_UPDATED_AT + " DESC", "1");
            Pinned pinned = cursorToPined(c);
            List<PinnedTimeline> pinnedTimelines = new ArrayList<>();
            if (pinned != null) {
                for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                    if (pinnedTimeline.displayed) {
                        pinnedTimelines.add(pinnedTimeline);
                    }
                }
                pinned.pinnedTimelines = pinnedTimelines;
            }
            return pinned;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns the pinned timeline for an account
     *
     * @param account Account
     * @return Pinned - {@link Pinned}
     */
    public Pinned getAllPinned(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_PINNED_TIMELINES, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null, null, null, Sqlite.COL_UPDATED_AT + " DESC", "1");
            return cursorToPined(c);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean pinnedExist(Pinned pinned) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_PINNED_TIMELINES
                + " where " + Sqlite.COL_INSTANCE + " = '" + pinned.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + pinned.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Restore pinned from db
     *
     * @param c Cursor
     * @return Pinned
     */
    private Pinned cursorToPined(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        Pinned pinned = convertCursorToStatusDraft(c);
        //Close the cursor
        c.close();
        return pinned;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Timeline
     */
    private Pinned convertCursorToStatusDraft(Cursor c) {
        Pinned pinned = new Pinned();
        pinned.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        pinned.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        pinned.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        pinned.pinnedTimelines = restorePinnedTimelinesFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_PINNED_TIMELINES)));
        pinned.created_ad = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
        pinned.updated_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_UPDATED_AT)));
        return pinned;
    }
}
