package app.fedilab.android.client.entities;
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
import java.util.Date;
import java.util.List;

import app.fedilab.android.client.mastodon.entities.Pagination;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.client.mastodon.entities.Statuses;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.sqlite.Sqlite;

public class StatusCache {

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("type")
    public CacheEnum type;
    @SerializedName("status_id")
    public String status_id;
    @SerializedName("status")
    public Status status;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;
    private Context context;

    public StatusCache() {
        db = null;
    }

    public StatusCache(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a Status class
     *
     * @param mastodon_status {@link Status} to serialize
     * @return String serialized status
     */
    public static String mastodonStatusToStringStorage(Status mastodon_status) {
        Gson gson = new Gson();
        try {
            return gson.toJson(mastodon_status);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a Mastodon Status
     *
     * @param serializedStatus String serialized status
     * @return {@link Status}
     */
    public static Status restoreStatusFromString(String serializedStatus) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedStatus, Status.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a status
     *
     * @param statusCache {@link StatusCache}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(StatusCache statusCache) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean exists = statusExist(statusCache);
        long idReturned;
        if (exists) {
            idReturned = updateStatus(statusCache);
        } else {
            idReturned = insertStatus(statusCache);
        }
        return idReturned;
    }

    /**
     * Check if a status exists in db
     *
     * @param statusCache Status {@link StatusCache}
     * @return boolean - StatusCache exists
     * @throws DBException Exception
     */
    public boolean statusExist(StatusCache statusCache) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                + " where " + Sqlite.COL_STATUS_ID + " = '" + statusCache.status_id + "'"
                + " AND " + Sqlite.COL_INSTANCE + " = '" + statusCache.instance + "'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + statusCache.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Insert a status in db
     *
     * @param statusCache {@link StatusCache}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertStatus(StatusCache statusCache) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, statusCache.user_id);
        values.put(Sqlite.COL_INSTANCE, statusCache.instance);
        values.put(Sqlite.COL_TYPE, statusCache.type.getValue());
        values.put(Sqlite.COL_STATUS_ID, statusCache.status_id);
        values.put(Sqlite.COL_STATUS, mastodonStatusToStringStorage(statusCache.status));
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.insertOrThrow(Sqlite.TABLE_STATUS_CACHE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Update a status in db
     *
     * @param statusCache {@link StatusCache}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateStatus(StatusCache statusCache) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, statusCache.user_id);
        values.put(Sqlite.COL_TYPE, statusCache.type.getValue());
        values.put(Sqlite.COL_STATUS_ID, statusCache.status_id);
        values.put(Sqlite.COL_STATUS, mastodonStatusToStringStorage(statusCache.status));
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_STATUS_CACHE,
                    values, Sqlite.COL_STATUS_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{statusCache.status_id, statusCache.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Get paginated statuses from db
     *
     * @param type     CacheEnum - not used yet but will allow to extend cache to other timelines
     * @param instance String - instance
     * @param user_id  String - us
     * @param max_id   String - status having max id
     * @param min_id   String - status having min id
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Statuses geStatuses(CacheEnum type, String instance, String user_id, String max_id, String min_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "'";
        String limit = String.valueOf(MastodonHelper.statusesPerCall(context));
        if (max_id == null && min_id != null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "'";
        } else if (max_id != null && min_id == null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "'";
        } else if (max_id != null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "' AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "'";
            limit = null;
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " DESC", limit);
            return createStatusReply(cursorToListOfStatuses(c));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @param type     CacheEnum - not used yet but will allow to extend cache to other timelines
     * @param instance String - instance
     * @param user_id  String - us
     * @param search   String search
     * @return - List<Status>
     * @throws DBException exception
     */
    public List<Status> searchStatus(CacheEnum type, String instance, String user_id, String search) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance
                + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "'";
        List<Status> reply = new ArrayList<>();
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " DESC", "");
            List<Status> statuses = cursorToListOfStatuses(c);
            if (statuses != null && statuses.size() > 0) {
                for (Status status : statuses) {
                    if (status.content.toLowerCase().contains(search.trim().toLowerCase())) {
                        reply.add(status);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return reply;
    }

    /**
     * Convert a cursor to list of statuses
     *
     * @param c Cursor
     * @return List<Status>
     */
    private List<Status> cursorToListOfStatuses(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Status> statusList = new ArrayList<>();
        while (c.moveToNext()) {
            Status status = convertCursorToStatus(c);
            statusList.add(status);
        }
        //Close the cursor
        c.close();
        return statusList;
    }

    /**
     * Create a reply from db in the same way than API call
     *
     * @param statusList List<Status>
     * @return Statuses (with pagination)
     */
    private Statuses createStatusReply(List<Status> statusList) {
        Statuses statuses = new Statuses();
        statuses.statuses = statusList;
        Pagination pagination = new Pagination();
        if (statusList != null && statusList.size() > 0) {
            pagination.max_id = statusList.get(0).id;
            pagination.min_id = statusList.get(statusList.size() - 1).id;
        }
        statuses.pagination = pagination;
        return statuses;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Status
     */
    private Status convertCursorToStatus(Cursor c) {
        String serializedStatus = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUS));
        return restoreStatusFromString(serializedStatus);
    }

    public enum CacheEnum {
        @SerializedName("HOME")
        HOME("HOME");
        private final String value;

        CacheEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
