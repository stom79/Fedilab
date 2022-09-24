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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.api.Statuses;
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
    @SerializedName("slug")
    public String slug;
    @SerializedName("type")
    public Timeline.TimeLineEnum type;
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
    public long insertOrUpdate(StatusCache statusCache, String slug) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        statusCache.slug = slug;
        boolean exists = statusExist(statusCache);
        long idReturned;
        if (exists) {
            idReturned = updateStatus(statusCache);
        } else {
            idReturned = insertStatus(statusCache, slug);
        }
        return idReturned;
    }

    /**
     * update a status if presents in db
     *
     * @param statusCache {@link StatusCache}
     * @throws DBException exception with database
     */
    public void updateIfExists(StatusCache statusCache) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean exists = statusExist(statusCache);
        if (exists) {
            updateStatus(statusCache);
        }
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
     * Check if a status exists in db
     *
     * @param status Status {@link Status}
     * @return boolean - StatusCache exists
     * @throws DBException Exception
     */
    public boolean statusExist(Status status) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                + " where " + Sqlite.COL_STATUS_ID + " = '" + status.id + "'"
                + " AND " + Sqlite.COL_INSTANCE + " = '" + MainActivity.currentInstance + "'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + MainActivity.currentUserID + "'", null);
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
    private long insertStatus(StatusCache statusCache, String slug) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, statusCache.user_id);
        values.put(Sqlite.COL_INSTANCE, statusCache.instance);
        values.put(Sqlite.COL_SLUG, statusCache.slug);
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
            return db.delete(Sqlite.TABLE_STATUS_CACHE, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete all cache for an account
     *
     * @param account - Account
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteForAccount(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{account.user_id, account.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete a status in db
     *
     * @param instance - String instance
     * @param id       - String status id
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteStatus(String instance, String id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_STATUS_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{id, instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Get newest status for a timeline
     *
     * @param slug     String - slug for the timeline (it's a unique string value for a timeline)
     * @param instance String - instance
     * @param user_id  String - us
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Status getNewestStatus(String slug, String instance, String user_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_SLUG + "= '" + slug + "'";
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " DESC", "1");
            Statuses statuses = createStatusReply(cursorToListOfStatuses(c));
            if (statuses.statuses != null && statuses.statuses.size() > 0) {
                return statuses.statuses.get(0);
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get oldest status for a timeline
     *
     * @param slug     String - slug for the timeline (it's a unique string value for a timeline)
     * @param instance String - instance
     * @param user_id  String - us
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Status getOldestStatus(String slug, String instance, String user_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_SLUG + "= '" + slug + "'";
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " ASC", "1");
            Statuses statuses = createStatusReply(cursorToListOfStatuses(c));
            if (statuses.statuses != null && statuses.statuses.size() > 0) {
                return statuses.statuses.get(0);
            } else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get paginated statuses from db
     *
     * @param slug     String - slug for the timeline (it's a unique string value for a timeline)
     * @param instance String - instance
     * @param user_id  String - us
     * @param max_id   String - status having max id
     * @param min_id   String - status having min id
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Statuses geStatuses(String slug, String instance, String user_id, String max_id, String min_id, String since_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String order = " DESC";
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_SLUG + "= '" + slug + "'";
        String limit = String.valueOf(MastodonHelper.statusesPerCall(context));
        if (min_id != null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "'";
            order = " ASC";
        } else if (max_id != null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "'";
        } else if (since_id != null) {
            selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + since_id + "'";
            limit = null;
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + order, limit);
            return createStatusReply(cursorToListOfStatuses(c));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get statuses from db
     *
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Status getTopFetchMore(String slug, String instance, String user_id, String status_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND "
                + Sqlite.COL_USER_ID + "= '" + user_id + "' AND "
                + Sqlite.COL_SLUG + "= '" + slug + "' AND "
                + Sqlite.COL_STATUS_ID + " > '" + status_id + "'";
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " ASC", "1");
            if (c != null && c.getCount() > 0) {
                return convertCursorToStatus(c);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get statuses from db
     *
     * @param statusCache StatusCache - status in cache to compare
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    public Status getBottomFetchMore(String slug, String instance, String user_id, String status_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND "
                + Sqlite.COL_USER_ID + "= '" + user_id + "' AND "
                + Sqlite.COL_SLUG + "= '" + slug + "' AND "
                + Sqlite.COL_STATUS_ID + " < '" + status_id + "'";
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, null, null, Sqlite.COL_STATUS_ID + " DESC", "1");
            if (c != null && c.getCount() > 0) {
                return convertCursorToStatus(c);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param instance String - instance
     * @param user_id  String - us
     * @param search   String search
     * @return - List<Status>
     * @throws DBException exception
     */
    public List<Status> searchStatus(String instance, String user_id, String search) throws DBException {
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

    public enum order {
        @SerializedName("ASC")
        ASC("ASC"),
        @SerializedName("DESC")
        DESC("DESC");
        private final String value;

        order(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public int count(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                + " where " + Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
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
            //Status list is inverted, it happens for min_id due to ASC ordering
            if (statusList.get(0).id.compareTo(statusList.get(statusList.size() - 1).id) < 0) {
                Collections.reverse(statusList);
                statuses.statuses = statusList;
            }
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

}
