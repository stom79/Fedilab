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

import static app.fedilab.android.client.entities.app.StatusCache.mastodonStatusToStringStorage;
import static app.fedilab.android.client.entities.app.StatusCache.restoreStatusFromString;

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
import java.util.UUID;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

public class ScheduledBoost implements Serializable {

    private transient final SQLiteDatabase db;
    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("userId")
    public String userId;
    @SerializedName("statusId")
    public String statusId;
    @SerializedName("status")
    public Status status;
    @SerializedName("reblogged")
    public int reblogged;
    @SerializedName("workerUuid")
    public UUID workerUuid;
    @SerializedName("scheduledAt")
    public Date scheduledAt;

    private Context context;

    public ScheduledBoost() {
        db = null;
    }

    public ScheduledBoost(Context context) {
        //Creation of the DB with tables
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        this.context = context;
    }

    /**
     * Serialized a UUID
     *
     * @param uuid {@link UUID} to serialize
     * @return String serialized uuid
     */
    public static String uuidToStringStorage(UUID uuid) {
        Gson gson = new Gson();
        try {
            return gson.toJson(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a UUID
     *
     * @param serializedUUID String serialized UUID
     * @return {@link UUID}
     */
    public static UUID restoreUuidFromString(String serializedUUID) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedUUID, UUID.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * * Remove a scheduled boost
     *
     * @param instance - String
     * @param userId   - String
     * @param statusId - String
     * @return long
     * @throws DBException exception
     */
    public int removeScheduled(String instance, String userId, String statusId) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_SCHEDULE_BOOST, Sqlite.COL_INSTANCE + " = '" + instance + "' AND "
                        + Sqlite.COL_USER_ID + " = '" + userId + "' AND "
                        + Sqlite.COL_STATUS_ID + " = '" + statusId + "'"
                , null);
    }

    /**
     * Insert scheduled boost in db
     *
     * @param scheduledBoost - ScheduledBoost
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertScheduledBoost(ScheduledBoost scheduledBoost) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, BaseMainActivity.currentInstance);
        values.put(Sqlite.COL_USER_ID, BaseMainActivity.currentUserID);
        values.put(Sqlite.COL_STATUS_ID, scheduledBoost.statusId);
        values.put(Sqlite.COL_WORKER_UUID, uuidToStringStorage(scheduledBoost.workerUuid));
        values.put(Sqlite.COL_STATUS, mastodonStatusToStringStorage(scheduledBoost.status));
        values.put(Sqlite.COL_SCHEDULED_AT, Helper.dateToString(scheduledBoost.scheduledAt));
        values.put(Sqlite.COL_REBLOGGED, 0);
        //Inserts scheduled
        try {
            return db.insertOrThrow(Sqlite.TABLE_SCHEDULE_BOOST, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * * Remove a scheduled boost
     *
     * @param scheduledBoost - ScheduledBoost
     * @return long
     * @throws DBException exception
     */
    public int removeScheduled(ScheduledBoost scheduledBoost) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_SCHEDULE_BOOST, Sqlite.COL_INSTANCE + " = '" + scheduledBoost.instance + "' AND "
                        + Sqlite.COL_USER_ID + " = '" + scheduledBoost.userId + "' AND "
                        + Sqlite.COL_STATUS_ID + " = '" + scheduledBoost.statusId + "'"
                , null);
    }

    /**
     * Returns the ScheduledBoost for an account that has been scheduled by the client
     *
     * @param account Account
     * @return List<ScheduledBoost> - List of {@link ScheduledBoost}
     */
    public List<ScheduledBoost> getScheduled(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_SCHEDULE_BOOST, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND " + Sqlite.COL_WORKER_UUID + " != ''", null, null, null, Sqlite.COL_ID + " DESC", null);
            return cursorToScheduledBoost(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restore scheduledBoost list from db
     *
     * @param c Cursor
     * @return List<ScheduledBoost>
     */
    private List<ScheduledBoost> cursorToScheduledBoost(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<ScheduledBoost> scheduledBoosts = new ArrayList<>();
        while (c.moveToNext()) {
            ScheduledBoost scheduledBoost = convertCursorToScheduledBoost(c);
            scheduledBoosts.add(scheduledBoost);
        }
        //Close the cursor
        c.close();
        return scheduledBoosts;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Timeline
     */
    private ScheduledBoost convertCursorToScheduledBoost(Cursor c) {
        ScheduledBoost scheduledBoost = new ScheduledBoost();
        scheduledBoost.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        scheduledBoost.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        scheduledBoost.userId = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        scheduledBoost.statusId = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUS_ID));
        scheduledBoost.reblogged = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_REBLOGGED));
        scheduledBoost.status = restoreStatusFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUS)));
        scheduledBoost.scheduledAt = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SCHEDULED_AT)));
        scheduledBoost.workerUuid = ScheduledBoost.restoreUuidFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_WORKER_UUID)));
        return scheduledBoost;
    }

}
