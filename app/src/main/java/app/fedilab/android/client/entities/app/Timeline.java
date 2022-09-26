package app.fedilab.android.client.entities.app;
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

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;

public class Timeline {

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("position")
    public int position;
    @SerializedName("type")
    public TimeLineEnum type;
    @SerializedName("remote_instance")
    public String remote_instance;
    @SerializedName("displayed")
    public boolean displayed;
    @SerializedName("timelineOptions")
    public TimelineOptions timelineOptions;
    private Context context;

    public Timeline() {
        db = null;
    }

    public Timeline(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a TimelineOptions class
     *
     * @param timelineOptions {@link TimelineOptions} to serialize
     * @return String serialized timeline options
     */
    public static String timelineOptionsToStringStorage(TimelineOptions timelineOptions) {
        Gson gson = new Gson();
        try {
            return gson.toJson(timelineOptions);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a TimelineOptions
     *
     * @param serializedTimelineOptionsString serialized timeline options
     * @return {@link TimelineOptions}
     */
    public static TimelineOptions restoreTimelineOptionsFromString(String serializedTimelineOptionsString) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedTimelineOptionsString, TimelineOptions.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert a timeline
     *
     * @param timeline {@link Timeline}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insert(Timeline timeline) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (!canBeModified(timeline)) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_POSITION, countEntries());
        values.put(Sqlite.COL_USER_ID, timeline.user_id);
        values.put(Sqlite.COL_INSTANCE, timeline.instance);
        values.put(Sqlite.COL_TYPE, timeline.type.getValue());
        values.put(Sqlite.COL_REMOTE_INSTANCE, timeline.remote_instance);
        values.put(Sqlite.COL_DISPLAYED, timeline.displayed);
        if (timeline.timelineOptions != null) {
            values.put(Sqlite.COL_TIMELINE_OPTION, timelineOptionsToStringStorage(timeline.timelineOptions));
        }
        try {
            return db.insertOrThrow(Sqlite.TABLE_TIMELINES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private boolean canBeModified(Timeline timeline) {
        return timeline.type != TimeLineEnum.HOME && timeline.type != TimeLineEnum.DIRECT && timeline.type != TimeLineEnum.LOCAL && timeline.type != TimeLineEnum.PUBLIC && timeline.type != TimeLineEnum.NOTIFICATION;
    }

    /**
     * update a timeline
     *
     * @param timeline {@link Timeline}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long update(Timeline timeline) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_POSITION, timeline.position);
        values.put(Sqlite.COL_DISPLAYED, timeline.displayed);
        if (timeline.timelineOptions != null && canBeModified(timeline)) {
            values.put(Sqlite.COL_TIMELINE_OPTION, timelineOptionsToStringStorage(timeline.timelineOptions));
        }
        reorderUpdatePosition(timeline);
        try {
            return db.update(Sqlite.TABLE_TIMELINES,
                    values, Sqlite.COL_ID + " =  ?",
                    new String[]{String.valueOf(timeline.id)});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Remove a timeline from db
     *
     * @param timeline {@link Timeline}
     * @return int
     */
    public int remove(Timeline timeline) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (!canBeModified(timeline)) {
            return -1;
        }
        reorderDeletePosition(timeline);
        return db.delete(Sqlite.TABLE_TIMELINES, Sqlite.COL_ID + " = '" + timeline.id + "'", null);
    }

    /**
     * Returns all timelines between two position (positions included)
     *
     * @return List<Timelines> timelines
     */
    public List<Timeline> getTimelineBetweenPosition(int min, int max) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (min > max) {
            int _t = min;
            min = max;
            max = min;
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINES, null, Sqlite.COL_POSITION + " >= '" + min + "' AND " + Sqlite.COL_POSITION + " <= '" + max + "'", null, null, null, Sqlite.COL_POSITION + " ASC", null);
            return cursorToListTimelines(c);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns all timelines after a position (position included)
     *
     * @return List<Timelines> timelines
     */
    public List<Timeline> getTimelineAfterPosition(int position) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINES, null, Sqlite.COL_POSITION + " > '" + position + "'", null, null, null, Sqlite.COL_POSITION + " ASC", null);
            return cursorToListTimelines(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reorder each position after moving an element
     *
     * @param _mTimeline Timeline
     * @throws DBException - db exception
     */
    public void reorderUpdatePosition(Timeline _mTimeline) throws DBException {
        Timeline previousPosition = getTimeline(_mTimeline.id);
        List<Timeline> timelines = getTimelineBetweenPosition(_mTimeline.position, previousPosition.position);
        if (previousPosition.position > _mTimeline.position) {
            for (int i = _mTimeline.position; i < timelines.size(); i++) {
                Timeline timeline = timelines.get(i);
                timeline.position++;
                update(timeline);
            }
        } else if (previousPosition.position < _mTimeline.position) {
            for (int i = previousPosition.position + 1; i <= timelines.size(); i++) {
                Timeline timeline = timelines.get(i);
                timeline.position--;
                update(timeline);
            }
        }
    }

    /**
     * Reorder each position after deleting an element
     *
     * @param _mTimeline Timeline
     * @throws DBException - db exception
     */
    public void reorderDeletePosition(Timeline _mTimeline) throws DBException {
        List<Timeline> timelines = getTimelineAfterPosition(_mTimeline.position);
        for (Timeline timeline : timelines) {
            timeline.position--;
            update(timeline);
        }
    }

    /**
     * Returns all timelines
     *
     * @return List<Timelines> timelines
     */
    public List<Timeline> getTimelines() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINES, null, null, null, null, null, Sqlite.COL_POSITION + " ASC", null);
            return cursorToListTimelines(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a timeline
     *
     * @return Timelines timeline
     */
    public Timeline getTimeline(long id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_TIMELINES, null, Sqlite.COL_ID + "='" + id + "'", null, null, null, Sqlite.COL_POSITION + " ASC", null);
            return cursorToTimeline(c);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Timeline> cursorToListTimelines(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Timeline> timelineList = new ArrayList<>();
        while (c.moveToNext()) {
            Timeline timeline = convertCursorToTimeLine(c);
            timelineList.add(timeline);
        }
        //Close the cursor
        c.close();
        return timelineList;
    }

    private Timeline cursorToTimeline(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        Timeline timeline = convertCursorToTimeLine(c);
        //Close the cursor
        c.close();
        return timeline;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Timeline
     */
    private Timeline convertCursorToTimeLine(Cursor c) {
        Timeline timeline = new Timeline();
        timeline.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        timeline.timelineOptions = restoreTimelineOptionsFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TIMELINE_OPTION)));
        timeline.type = TimeLineEnum.valueOf(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TYPE)));
        timeline.displayed = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_TIMELINE_OPTION)) == 1;
        timeline.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        timeline.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        timeline.remote_instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_REMOTE_INSTANCE));
        timeline.position = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_POSITION));
        return timeline;
    }


    /**
     * Count entry in db
     *
     * @return int - number of timelines recorded in db
     * @throws DBException Exception
     */
    public int countEntries() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_TIMELINES, null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
    }


    public enum TimeLineEnum {
        @SerializedName("HOME")
        HOME("HOME"),
        @SerializedName("DIRECT")
        DIRECT("DIRECT"),
        @SerializedName("NOTIFICATION")
        NOTIFICATION("NOTIFICATION"),
        @SerializedName("LOCAL")
        LOCAL("LOCAL"),
        @SerializedName("PUBLIC")
        PUBLIC("PUBLIC"),
        @SerializedName("CONTEXT")
        CONTEXT("CONTEXT"),
        @SerializedName("TAG")
        TAG("TAG"),
        @SerializedName("ART")
        ART("ART"),
        @SerializedName("LIST")
        LIST("LIST"),
        @SerializedName("REMOTE")
        REMOTE("REMOTE"),
        @SerializedName("TREND_TAG")
        TREND_TAG("TREND_TAG"),
        @SerializedName("TREND_MESSAGE")
        TREND_MESSAGE("TREND_MESSAGE"),
        @SerializedName("ACCOUNT_TIMELINE")
        ACCOUNT_TIMELINE("ACCOUNT_TIMELINE"),
        @SerializedName("MUTED_TIMELINE")
        MUTED_TIMELINE("MUTED_TIMELINE"),
        @SerializedName("BOOKMARK_TIMELINE")
        BOOKMARK_TIMELINE("BOOKMARK_TIMELINE"),
        @SerializedName("BLOCKED_TIMELINE")
        BLOCKED_TIMELINE("BLOCKED_TIMELINE"),
        @SerializedName("FAVOURITE_TIMELINE")
        FAVOURITE_TIMELINE("FAVOURITE_TIMELINE"),
        @SerializedName("REBLOG_TIMELINE")
        REBLOG_TIMELINE("REBLOG_TIMELINE"),
        @SerializedName("SCHEDULED_TOOT_SERVER")
        SCHEDULED_TOOT_SERVER("SCHEDULED_TOOT_SERVER"),
        @SerializedName("SCHEDULED_TOOT_CLIENT")
        SCHEDULED_TOOT_CLIENT("SCHEDULED_TOOT_CLIENT"),
        @SerializedName("SCHEDULED_BOOST")
        SCHEDULED_BOOST("SCHEDULED_BOOST"),
        @SerializedName("UNKNOWN")
        UNKNOWN("UNKNOWN");
        private final String value;

        TimeLineEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class TimelineOptions {
        @SerializedName("all")
        public List<String> all;
        @SerializedName("any")
        public List<String> any;
        @SerializedName("none")
        public List<String> none;
        @SerializedName("data")
        public List<String> data;
        @SerializedName("media_only")
        public boolean media_only;
        @SerializedName("sensitive")
        public boolean sensitive;
        @SerializedName("list_id")
        public String list_id;
    }

}
