package app.fedilab.android.mastodon.client.entities.app;
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

import static app.fedilab.android.mastodon.helper.Helper.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.client.entities.api.Conversation;
import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
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
    @SerializedName("notification")
    public Notification notification;
    @SerializedName("conversation")
    public Conversation conversation;
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
     * Serialized a Notification class
     *
     * @param mastodon_notification {@link Notification} to serialize
     * @return String serialized status
     */
    public static String mastodonNotificationToStringStorage(Notification mastodon_notification) {
        Gson gson = new Gson();
        try {
            return gson.toJson(mastodon_notification);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized a Conversation class
     *
     * @param mastodon_conversation {@link Conversation} to serialize
     * @return String serialized Conversation
     */
    public static String mastodonConversationToStringStorage(Conversation mastodon_conversation) {
        Gson gson = new Gson();
        try {
            return gson.toJson(mastodon_conversation);
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
     * Unserialized a Mastodon Notification
     *
     * @param serializedNotification String serialized status
     * @return {@link Notification}
     */
    public static Notification restoreNotificationFromString(String serializedNotification) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedNotification, Notification.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Unserialized a Mastodon Conversation
     *
     * @param serializedConversation String serialized Conversation
     * @return {@link Conversation}
     */
    public static Conversation restoreConversationFromString(String serializedConversation) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedConversation, Conversation.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Insert or update a status
     *
     * @param statusCache {@link StatusCache}
     * @return int - 0 if updated 1 if inserted
     * @throws DBException exception with database
     */
    public synchronized int insertOrUpdate(StatusCache statusCache, String slug) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        statusCache.slug = slug;
        boolean exists = statusExist(statusCache);
        int idReturned = 0;
        if (exists) {
            updateStatus(statusCache);
        } else {
            insertStatus(statusCache, slug);
            idReturned = 1;
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
     * count messages for home
     *
     * @param baseAccount Status {@link BaseAccount}
     * @return int - number of occurrences
     * @throws DBException Exception
     */
    public int countHome(BaseAccount baseAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                        + " where " + Sqlite.COL_TYPE + " = ?"
                        + " AND " + Sqlite.COL_INSTANCE + " = ?"
                        + " AND " + Sqlite.COL_USER_ID + "= ?",
                new String[]{Timeline.TimeLineEnum.HOME.getValue(), baseAccount.instance, baseAccount.user_id});
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
    }

    /**
     * get all cache messages for home
     *
     * @param baseAccount Status {@link BaseAccount}
     * @return List<Status>
     * @throws DBException Exception
     */
    public List<Status> getHome(BaseAccount baseAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String selection = Sqlite.COL_INSTANCE + "='" + baseAccount.instance + "' AND " + Sqlite.COL_USER_ID + "= '" + baseAccount.user_id + "' AND " + Sqlite.COL_SLUG + "= '" + Timeline.TimeLineEnum.HOME.getValue() + "' ";
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, Sqlite.COL_STATUS_ID, null, Sqlite.COL_STATUS_ID + " ASC", null);
            return cursorToListOfStatuses(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * count messages for other timelines
     *
     * @param baseAccount Status {@link BaseAccount}
     * @return int - number of occurrences
     * @throws DBException Exception
     */
    public int countOther(BaseAccount baseAccount) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                + " where " + Sqlite.COL_TYPE + " != '" + Timeline.TimeLineEnum.HOME.getValue() + "'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + baseAccount.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
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
        String query = "select count(*) from " + Sqlite.TABLE_STATUS_CACHE
                + " where " + Sqlite.COL_STATUS_ID + " = '" + statusCache.status_id + "'"
                + " AND " + Sqlite.COL_INSTANCE + " = '" + statusCache.instance + "'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + statusCache.user_id + "'";
        if (statusCache.type != null) {
            query += " AND " + Sqlite.COL_TYPE + " = '" + statusCache.type.getValue() + "'";
        }
        Cursor mCount = db.rawQuery(query, null);
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
        values.put(Sqlite.COL_SLUG, slug);
        values.put(Sqlite.COL_STATUS_ID, statusCache.status_id);
        if (statusCache.type != null) {
            values.put(Sqlite.COL_TYPE, statusCache.type.getValue());
        }
        if (statusCache.status != null) {
            values.put(Sqlite.COL_STATUS, mastodonStatusToStringStorage(statusCache.status));
        }
        if (statusCache.notification != null) {
            values.put(Sqlite.COL_STATUS, mastodonNotificationToStringStorage(statusCache.notification));
        }
        if (statusCache.conversation != null) {
            values.put(Sqlite.COL_STATUS, mastodonConversationToStringStorage(statusCache.conversation));
        }
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
        if (statusCache.status != null) {
            values.put(Sqlite.COL_STATUS, mastodonStatusToStringStorage(statusCache.status));
        }
        if (statusCache.notification != null) {
            Notification currentNotification = getCachedNotification(statusCache);
            if(currentNotification != null && currentNotification.status != null) {
                currentNotification.status = statusCache.notification.status;
                values.put(Sqlite.COL_STATUS, mastodonNotificationToStringStorage(currentNotification));
            }
        }
        if (statusCache.conversation != null) {
            values.put(Sqlite.COL_STATUS, mastodonConversationToStringStorage(statusCache.conversation));
        }
        values.put(Sqlite.COL_UPDATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_STATUS_CACHE,
                    values, Sqlite.COL_STATUS_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_TYPE + " =? AND " + Sqlite.COL_USER_ID + " =?",
                    new String[]{statusCache.status_id, statusCache.instance, statusCache.type != null ? statusCache.type.getValue() : "", statusCache.user_id});
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
            return db.delete(Sqlite.TABLE_STATUS_CACHE, Sqlite.COL_CREATED_AT + " <  ?", new String[]{dateStr});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * delete all cached notification for an account
     *
     * @return long - db id
     * @throws DBException exception with database
     */
    public long deleteNotifications(String user_id, String instance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_TYPE + "=?",
                    new String[]{user_id, instance, Timeline.TimeLineEnum.NOTIFICATION.getValue()});
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
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_SLUG + " = ? AND " + Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{slug, MainActivity.currentUserID, MainActivity.currentInstance});
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
    public long deleteHomeForAccount(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_TYPE + " = ? AND " + Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{Timeline.TimeLineEnum.HOME.getValue(), account.user_id, account.instance});
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
    public long deleteOthersForAccount(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_TYPE + " != ? AND " + Sqlite.COL_USER_ID + " =  ?",
                    new String[]{Timeline.TimeLineEnum.HOME.getValue(), account.user_id});
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
     * delete statuses in db for a user that wrote them
     *
     * @param instance     - String instance
     * @param userid       - String status id
     * @param targetedUser - String id of the user that wrote them
     * @throws DBException exception with database
     */
    public void deleteStatusForTargetedAccount(String instance, String userid, String targetedUser) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            db.delete(Sqlite.TABLE_STATUS_CACHE,
                    Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_STATUS + " LIKE ?",
                    new String[]{userid, instance, "%\"id\":\"" + targetedUser + "\"%"});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get paginated notifications from db
     *
     * @param instance String - instance
     * @param user_id  String - us
     * @param max_id   String - status having max id
     * @param min_id   String - status having min id
     * @return List<Notification>
     * @throws DBException - throws a db exception
     */
    public List<Notification> getNotifications(List<String> exclude_type, String instance, String user_id, String max_id, String min_id, String since_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String order = " DESC";
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_TYPE + "= '" + Timeline.TimeLineEnum.NOTIFICATION.getValue() + "' ";
        String limit = String.valueOf(MastodonHelper.statusesPerCall(context));
        if (min_id != null) {
            if (Helper.isNumeric(min_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + min_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "' ";
            }
            order = " ASC";
        } else if (max_id != null) {
            if (Helper.isNumeric(max_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < cast(" + max_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "' ";
            }

        } else if (since_id != null) {
            if (Helper.isNumeric(since_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + since_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + since_id + "' ";
            }

            limit = null;
        }
        if (exclude_type != null && exclude_type.size() > 0) {
            StringBuilder exclude = new StringBuilder();
            for (String excluded : exclude_type) {
                exclude.append("'").append(excluded).append("'").append(",");
            }
            exclude = new StringBuilder(exclude.substring(0, exclude.length() - 1));
            selection += "AND " + Sqlite.COL_SLUG + " NOT IN (" + exclude + ") ";
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, Sqlite.COL_STATUS_ID, null, Sqlite.COL_STATUS_ID + " + 0 " + order, limit);
            return cursorToListOfNotifications(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Notification getCachedNotification(StatusCache statusCache) throws DBException  {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, Sqlite.COL_STATUS_ID + " = ? AND " + Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                new String[]{statusCache.status_id, statusCache.user_id, statusCache.instance}, null, null, null, "1");
        c.moveToFirst();
        return convertCursorToNotification(c);
    }

    /**
     * Get paginated conversations from db
     *
     * @param instance String - instance
     * @param user_id  String - us
     * @param max_id   String - status having max id
     * @param min_id   String - status having min id
     * @return List<Conversation>
     * @throws DBException - throws a db exception
     */
    public List<Conversation> getConversations(String instance, String user_id, String max_id, String min_id, String since_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String order = " DESC";
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_TYPE + "= '" + Timeline.TimeLineEnum.CONVERSATION.getValue() + "' ";
        String limit = String.valueOf(MastodonHelper.statusesPerCall(context));
        if (min_id != null) {
            if (Helper.isNumeric(min_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + min_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "' ";
            }
            order = " ASC";
        } else if (max_id != null) {

            if (Helper.isNumeric(max_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < cast(" + max_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "' ";
            }
        } else if (since_id != null) {
            if (Helper.isNumeric(since_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + since_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + since_id + "' ";
            }
            limit = null;
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, Sqlite.COL_STATUS_ID, null, Sqlite.COL_STATUS_ID + " + 0 " + order, limit);
            return cursorToListOfConversations(c);
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
     * @return List<Status>
     * @throws DBException - throws a db exception
     */
    public List<Status> geStatuses(String slug, String instance, String user_id, String max_id, String min_id, String since_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        String order = " DESC";
        String selection = Sqlite.COL_INSTANCE + "='" + instance + "' AND " + Sqlite.COL_USER_ID + "= '" + user_id + "' AND " + Sqlite.COL_SLUG + "= '" + slug + "' ";
        String limit = String.valueOf(MastodonHelper.statusesPerCall(context));
        if (min_id != null) {

            if (Helper.isNumeric(min_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + min_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + min_id + "' ";
            }
            order = " ASC";
        } else if (max_id != null) {
            if (Helper.isNumeric(max_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < cast(" + max_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " < '" + max_id + "' ";
            }
        } else if (since_id != null) {
            if (Helper.isNumeric(since_id)) {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > cast(" + since_id + " as int) ";
            } else {
                selection += "AND " + Sqlite.COL_STATUS_ID + " > '" + since_id + "' ";
            }
            limit = null;
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, Sqlite.COL_STATUS_ID, null, Sqlite.COL_STATUS_ID + " + 0 " + order, limit);
            return cursorToListOfStatuses(c);
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
                + "' AND " + Sqlite.COL_TYPE + "= '" + Timeline.TimeLineEnum.HOME.getValue() + "'"
                + " AND " + Sqlite.COL_STATUS + " LIKE '%" + search + "%'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + user_id + "'";
        List<Status> reply = new ArrayList<>();
        try {
            Cursor c = db.query(Sqlite.TABLE_STATUS_CACHE, null, selection, null, Sqlite.COL_STATUS_ID, null, Sqlite.COL_STATUS_ID + " DESC", "");
            List<Status> statuses = cursorToListOfStatuses(c);
            if (statuses != null && statuses.size() > 0) {
                for (Status status : statuses) {
                    if (status.content != null && status.content.toLowerCase().contains(search.trim().toLowerCase())) {
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
            status.cached = true;
            statusList.add(status);
        }
        //Close the cursor
        c.close();
        return statusList;
    }

    /**
     * Convert a cursor to list of notifications
     *
     * @param c Cursor
     * @return List<Notification>
     */
    private List<Notification> cursorToListOfNotifications(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Notification> notificationList = new ArrayList<>();
        while (c.moveToNext()) {
            Notification notification = convertCursorToNotification(c);
            notification.cached = true;
            notificationList.add(notification);
        }
        //Close the cursor
        c.close();
        return notificationList;
    }


    /**
     * Convert a cursor to list of Conversation
     *
     * @param c Cursor
     * @return List<Conversation>
     */
    private List<Conversation> cursorToListOfConversations(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<Conversation> conversationList = new ArrayList<>();
        while (c.moveToNext()) {
            Conversation conversation = convertCursorToConversation(c);
            conversation.cached = true;
            conversationList.add(conversation);
        }
        //Close the cursor
        c.close();
        return conversationList;
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

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Notification
     */
    private Notification convertCursorToNotification(Cursor c) {
        String serializedNotification = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUS));
        return restoreNotificationFromString(serializedNotification);
    }


    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return Conversation
     */
    private Conversation convertCursorToConversation(Cursor c) {
        String serializedNotification = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUS));
        return restoreConversationFromString(serializedNotification);
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

}
