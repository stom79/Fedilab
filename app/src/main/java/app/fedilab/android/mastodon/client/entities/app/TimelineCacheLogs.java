package app.fedilab.android.mastodon.client.entities.app;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

public class TimelineCacheLogs {

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("slug")
    public String slug;
    @SerializedName("type")
    public Timeline.TimeLineEnum type;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("fetched")
    public int fetched;
    @SerializedName("failed")
    public int failed;
    @SerializedName("inserted")
    public int inserted;
    @SerializedName("updated")
    public int updated;
    @SerializedName("frequency")
    public int frequency;
    private Context context;

    public TimelineCacheLogs() {
        db = null;
    }


    public TimelineCacheLogs(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }


    /**
     * get all cache timelineCacheLogs for home
     *
     * @param baseAccount Status {@link BaseAccount}
     * @return List<Status>
     * @throws DBException Exception
     */
    public List<TimelineCacheLogs> getHome(BaseAccount baseAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + baseAccount.instance + "' AND " + Sqlite.COL_USER_ID + "= '" + baseAccount.user_id + "' AND " + Sqlite.COL_SLUG + "= '" + Timeline.TimeLineEnum.HOME.getValue() + "' ";
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINE_CACHE_LOGS, null, selection, null, null, null, Sqlite.COL_ID + " ASC", null);
            return cursorToListOfStatuses(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Insert a status in db
     *
     * @param timelineCacheLogs {@link TimelineCacheLogs}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insert(TimelineCacheLogs timelineCacheLogs) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, timelineCacheLogs.user_id);
        values.put(Sqlite.COL_INSTANCE, timelineCacheLogs.instance);
        values.put(Sqlite.COL_SLUG, timelineCacheLogs.slug);
        values.put(Sqlite.COL_TYPE, timelineCacheLogs.type.getValue());
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        values.put(Sqlite.COL_FAILED, timelineCacheLogs.failed);
        values.put(Sqlite.COL_FETCHED, timelineCacheLogs.fetched);
        values.put(Sqlite.COL_FREQUENCY, timelineCacheLogs.frequency);
        values.put(Sqlite.COL_INSERTED, timelineCacheLogs.inserted);
        values.put(Sqlite.COL_UPDATED, timelineCacheLogs.updated);
        //Inserts token
        try {
            return db.insertOrThrow(Sqlite.TABLE_TIMELINE_CACHE_LOGS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * delete all cache for all account
     *
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteForAllAccount() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_TIMELINE_CACHE_LOGS, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete all cache for all account after 7 days
     *
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteForAllAccountAfter7Days() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -7);
        Date date = cal.getTime();
        String dateStr = Helper.dateToString(date);
        try {
            return db.delete(Sqlite.TABLE_TIMELINE_CACHE_LOGS, Sqlite.COL_CREATED_AT + " <  ?", new String[]{dateStr});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete all cache for an slug
     *
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteForSlug(String slug) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_TIMELINE_CACHE_LOGS,
                    Sqlite.COL_SLUG + " = ? AND " + Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{slug, MainActivity.currentUserID, MainActivity.currentInstance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    public int count(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_TIMELINE_CACHE_LOGS
                + " where " + Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
    }

    /**
     * Convert a cursor to list of TimelineCacheLogs
     *
     * @param c Cursor
     * @return List<TimelineCacheLogs>
     */
    private List<TimelineCacheLogs> cursorToListOfStatuses(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<TimelineCacheLogs> timelineCacheLogsList = new ArrayList<>();
        while (c.moveToNext()) {
            TimelineCacheLogs timelineCacheLogs = convertCursorToTimelineCacheLogs(c);
            timelineCacheLogsList.add(timelineCacheLogs);
        }
        //Close the cursor
        c.close();
        return timelineCacheLogsList;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Timeline
     */
    private TimelineCacheLogs convertCursorToTimelineCacheLogs(Cursor c) {
        TimelineCacheLogs timelineCacheLogs = new TimelineCacheLogs();
        timelineCacheLogs.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        timelineCacheLogs.type = Timeline.TimeLineEnum.valueOf(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TYPE)));
        timelineCacheLogs.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        timelineCacheLogs.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        timelineCacheLogs.slug = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SLUG));
        timelineCacheLogs.created_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
        timelineCacheLogs.failed = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_FAILED));
        timelineCacheLogs.fetched = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_FETCHED));
        timelineCacheLogs.inserted = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_INSERTED));
        timelineCacheLogs.updated = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_UPDATED));
        timelineCacheLogs.frequency = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_FREQUENCY));
        return timelineCacheLogs;
    }


}
