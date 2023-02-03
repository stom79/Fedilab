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
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

public class HomeFetchLog implements Serializable {

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("fetched_count")
    public int fetched_count;
    @SerializedName("inserted")
    public int inserted;
    @SerializedName("updated")
    public int updated;
    @SerializedName("failed")
    public int failed;
    @SerializedName("frequency")
    public int frequency;
    @SerializedName("created_at")
    public Date created_ad;
    private Context context;

    public HomeFetchLog() {
        db = null;
    }

    public HomeFetchLog(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    public long insert(HomeFetchLog homeFetchLog) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, homeFetchLog.instance);
        values.put(Sqlite.COL_USER_ID, homeFetchLog.user_id);
        values.put(Sqlite.COL_FETCHED_COUNT, homeFetchLog.fetched_count);
        values.put(Sqlite.COL_FAILED, homeFetchLog.failed);
        values.put(Sqlite.COL_INSERTED, homeFetchLog.inserted);
        values.put(Sqlite.COL_UPDATED, homeFetchLog.updated);
        values.put(Sqlite.COL_FREQUENCY, homeFetchLog.frequency);
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));

        //Inserts logs
        try {
            return db.insertOrThrow(Sqlite.TABLE_HOME_FETCH_LOGS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}
