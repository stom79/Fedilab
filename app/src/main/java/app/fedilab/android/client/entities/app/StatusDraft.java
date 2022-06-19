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
import com.google.gson.reflect.TypeToken;

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


public class StatusDraft implements Serializable {


    private transient final SQLiteDatabase db;
    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("statusDraftList")
    public List<Status> statusDraftList;
    @SerializedName("statusReplyList")
    public List<Status> statusReplyList;
    @SerializedName("state")
    public PostState state;
    @SerializedName("created_at")
    public Date created_ad;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("worker_uuid")
    public UUID workerUuid;
    @SerializedName("scheduled_at")
    public Date scheduled_at;

    private transient Context context;

    public StatusDraft() {
        db = null;
    }

    public StatusDraft(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a list of Status class
     *
     * @param statuses List of {@link Status} to serialize
     * @return String serialized emoji list
     */
    public static String mastodonStatusListToStringStorage(List<Status> statuses) {
        Gson gson = new Gson();
        try {
            return gson.toJson(statuses);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a Status List
     *
     * @param serializedStatusList String serialized Status list
     * @return List of {@link Status}
     */
    public static List<Status> restoreStatusListFromString(String serializedStatusList) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedStatusList, new TypeToken<List<Status>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized a list of Status class
     *
     * @param postState {@link PostState} to serialize
     * @return String serialized PostState list
     */
    public static String postStateToStringStorage(PostState postState) {
        Gson gson = new Gson();
        try {
            return gson.toJson(postState);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a PostState
     *
     * @param serializedPostState String serialized PostState
     * @return {@link PostState}
     */
    public static PostState restorePostStateFromString(String serializedPostState) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedPostState, PostState.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert statusDraft in db
     *
     * @param statusDraft {@link StatusDraft}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertStatusDraft(StatusDraft statusDraft) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, statusDraft.instance);
        values.put(Sqlite.COL_USER_ID, statusDraft.user_id);
        values.put(Sqlite.COL_DRAFTS, mastodonStatusListToStringStorage(statusDraft.statusDraftList));
        values.put(Sqlite.COL_REPLIES, mastodonStatusListToStringStorage(statusDraft.statusReplyList));
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        if (statusDraft.workerUuid != null) {
            values.put(Sqlite.COL_WORKER_UUID, ScheduledBoost.uuidToStringStorage(statusDraft.workerUuid));
            values.put(Sqlite.COL_SCHEDULED_AT, Helper.dateToString(statusDraft.scheduled_at));
        }
        //Inserts drafts
        try {
            return db.insertOrThrow(Sqlite.TABLE_STATUS_DRAFT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Remove a draft from db
     *
     * @param statusDraft {@link StatusDraft}
     * @return int
     */
    public int removeDraft(StatusDraft statusDraft) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_STATUS_DRAFT, Sqlite.COL_ID + " = '" + statusDraft.id + "'", null);
    }

    /**
     * Remove all drafts for an account from db
     *
     * @return int
     */
    public int removeAllDraft() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        return db.delete(Sqlite.TABLE_STATUS_DRAFT, Sqlite.COL_USER_ID + " = '" + BaseMainActivity.currentUserID + "' AND " + Sqlite.COL_INSTANCE + " = '" + BaseMainActivity.currentInstance + "'", null);
    }

    /**
     * update statusDraft in db
     *
     * @param statusDraft {@link StatusDraft}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long updateStatusDraft(StatusDraft statusDraft) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_DRAFTS, mastodonStatusListToStringStorage(statusDraft.statusDraftList));
        values.put(Sqlite.COL_REPLIES, mastodonStatusListToStringStorage(statusDraft.statusReplyList));
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        if (statusDraft.workerUuid != null) {
            values.put(Sqlite.COL_WORKER_UUID, ScheduledBoost.uuidToStringStorage(statusDraft.workerUuid));
            values.put(Sqlite.COL_SCHEDULED_AT, Helper.dateToString(statusDraft.scheduled_at));
        }
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_STATUS_DRAFT,
                    values, Sqlite.COL_ID + " =  ?",
                    new String[]{String.valueOf(statusDraft.id)});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * remove schedule statusDraft in db
     *
     * @param statusDraft {@link StatusDraft}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long removeScheduled(StatusDraft statusDraft) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.putNull(Sqlite.COL_WORKER_UUID);
        values.putNull(Sqlite.COL_SCHEDULED_AT);
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_STATUS_DRAFT,
                    values, Sqlite.COL_ID + " =  ?",
                    new String[]{String.valueOf(statusDraft.id)});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * update statusDraft in db
     *
     * @param statusDraft {@link StatusDraft}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long updatePostState(StatusDraft statusDraft) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_STATE, postStateToStringStorage(statusDraft.state));
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_STATUS_DRAFT,
                    values, Sqlite.COL_ID + " =  ?",
                    new String[]{String.valueOf(statusDraft.id)});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Returns the StatusDraft for an account
     *
     * @param account Account
     * @return List<StatusDraft> - List of {@link StatusDraft}
     */
    public List<StatusDraft> geStatusDraftList(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_DRAFT, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null, null, null, Sqlite.COL_UPDATED_AT + " DESC", null);
            return cursorToStatusDraftList(c);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns the StatusDraft for an account that has been scheduled by the client
     *
     * @param account Account
     * @return List<StatusDraft> - List of {@link StatusDraft}
     */
    public List<StatusDraft> geStatusDraftScheduledList(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_DRAFT, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND " + Sqlite.COL_WORKER_UUID + " != ''", null, null, null, Sqlite.COL_UPDATED_AT + " ASC", null);
            return cursorToStatusDraftList(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the StatusDraft for an account
     *
     * @param draftId String
     * @return StatusDraft - {@link StatusDraft}
     */
    public StatusDraft geStatusDraft(String draftId) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_DRAFT, null, Sqlite.COL_ID + " = '" + draftId + "'", null, null, null, null, "1");
            return convertCursorToStatusDraft(c);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Restore statusDraft list from db
     *
     * @param c Cursor
     * @return List<Emoji>
     */
    private List<StatusDraft> cursorToStatusDraftList(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<StatusDraft> statusDrafts = new ArrayList<>();
        while (c.moveToNext()) {
            StatusDraft statusDraft = convertCursorToStatusDraft(c);
            statusDrafts.add(statusDraft);
        }
        //Close the cursor
        c.close();
        return statusDrafts;
    }


    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Timeline
     */
    private StatusDraft convertCursorToStatusDraft(Cursor c) {
        StatusDraft statusDraft = new StatusDraft();
        statusDraft.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        statusDraft.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        statusDraft.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        statusDraft.statusReplyList = restoreStatusListFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_REPLIES)));
        statusDraft.statusDraftList = restoreStatusListFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_DRAFTS)));
        statusDraft.state = restorePostStateFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATE)));
        statusDraft.created_ad = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
        statusDraft.updated_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_UPDATED_AT)));
        statusDraft.workerUuid = ScheduledBoost.restoreUuidFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_WORKER_UUID)));
        statusDraft.scheduled_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SCHEDULED_AT)));
        return statusDraft;
    }
}
