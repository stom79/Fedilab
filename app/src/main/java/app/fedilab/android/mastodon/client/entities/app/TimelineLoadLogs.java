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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

/**
 * What a timeline load really returned, to track down gaps
 */
public class TimelineLoadLogs {

    public static final String SOURCE_CACHE = "cache";
    public static final String SOURCE_API = "api";
    public static final String DIRECTION_INITIAL = "INITIAL";

    private static final int KEEP_DAYS = 3;

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("slug")
    public String slug;
    @SerializedName("direction")
    public String direction;
    @SerializedName("source")
    public String source;
    @SerializedName("max_id")
    public String max_id;
    @SerializedName("min_id")
    public String min_id;
    @SerializedName("returned")
    public int returned;
    @SerializedName("displayed")
    public int displayed;
    @SerializedName("added")
    public int added;
    @SerializedName("fetching_missing")
    public boolean fetching_missing;
    @SerializedName("trigger")
    public String trigger;
    @SerializedName("created_at")
    public Date created_at;
    private Context context;

    public TimelineLoadLogs() {
        db = null;
    }

    public TimelineLoadLogs(Context context) {
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Insert a load in db and drop the old ones
     *
     * @param timelineLoadLogs {@link TimelineLoadLogs}
     * @throws DBException exception with database
     */
    public void insert(TimelineLoadLogs timelineLoadLogs) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, timelineLoadLogs.user_id);
        values.put(Sqlite.COL_INSTANCE, timelineLoadLogs.instance);
        values.put(Sqlite.COL_SLUG, timelineLoadLogs.slug);
        values.put(Sqlite.COL_DIRECTION, timelineLoadLogs.direction);
        values.put(Sqlite.COL_SOURCE, timelineLoadLogs.source);
        values.put(Sqlite.COL_MAX_ID, timelineLoadLogs.max_id);
        values.put(Sqlite.COL_MIN_ID, timelineLoadLogs.min_id);
        values.put(Sqlite.COL_RETURNED, timelineLoadLogs.returned);
        values.put(Sqlite.COL_DISPLAYED, timelineLoadLogs.displayed);
        values.put(Sqlite.COL_ADDED, timelineLoadLogs.added);
        values.put(Sqlite.COL_FETCHING_MISSING, timelineLoadLogs.fetching_missing ? 1 : 0);
        values.put(Sqlite.COL_TRIGGER, timelineLoadLogs.trigger);
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        try {
            db.insert(Sqlite.TABLE_TIMELINE_LOAD_LOGS, null, values);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -KEEP_DAYS);
            db.delete(Sqlite.TABLE_TIMELINE_LOAD_LOGS, Sqlite.COL_CREATED_AT + " < ?", new String[]{Helper.dateToString(calendar.getTime())});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the loads of the home timeline, oldest first
     *
     * @param baseAccount {@link BaseAccount}
     * @return List<TimelineLoadLogs>
     * @throws DBException Exception
     */
    public List<TimelineLoadLogs> getHome(BaseAccount baseAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + baseAccount.instance + "' AND " + Sqlite.COL_USER_ID + "= '" + baseAccount.user_id + "' AND " + Sqlite.COL_SLUG + "= '" + Timeline.TimeLineEnum.HOME.getValue() + "' ";
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINE_LOAD_LOGS, null, selection, null, null, null, Sqlite.COL_ID + " ASC", null);
            return cursorToListOfLoads(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<TimelineLoadLogs> cursorToListOfLoads(Cursor c) {
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<TimelineLoadLogs> loadLogsList = new ArrayList<>();
        while (c.moveToNext()) {
            TimelineLoadLogs loadLogs = new TimelineLoadLogs();
            loadLogs.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
            loadLogs.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
            loadLogs.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
            loadLogs.slug = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SLUG));
            loadLogs.direction = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_DIRECTION));
            loadLogs.source = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SOURCE));
            loadLogs.max_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_MAX_ID));
            loadLogs.min_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_MIN_ID));
            loadLogs.returned = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_RETURNED));
            loadLogs.displayed = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_DISPLAYED));
            loadLogs.added = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ADDED));
            loadLogs.fetching_missing = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_FETCHING_MISSING)) == 1;
            loadLogs.trigger = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TRIGGER));
            loadLogs.created_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
            loadLogsList.add(loadLogs);
        }
        c.close();
        return loadLogsList;
    }
}
