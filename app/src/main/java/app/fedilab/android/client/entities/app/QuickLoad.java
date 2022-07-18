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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.sqlite.Sqlite;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonNotification;

public class QuickLoad {

    private final SQLiteDatabase db;
    @SerializedName("id")
    public long id;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("slug")
    public String slug;
    @SerializedName("position")
    public int position;
    @SerializedName("statuses")
    public List<Status> statuses;
    @SerializedName("notifications")
    public List<Notification> notifications;
    private Context _mContext;
    private type typeOfFetch;

    public QuickLoad() {
        db = null;
    }

    public QuickLoad(Context context) {
        //Creation of the DB with tables
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        _mContext = context;
        this.typeOfFetch = type.STATUSES;
    }

    public QuickLoad(Context context, type type) {
        //Creation of the DB with tables
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        _mContext = context;
        this.typeOfFetch = type;
    }

    /**
     * Check if the current timeline can be stored
     *
     * @param timeLineType - Timeline.TimeLineEnum
     * @return boolean
     */
    private static boolean cannotBeStored(Timeline.TimeLineEnum timeLineType) {
        return timeLineType != Timeline.TimeLineEnum.HOME && timeLineType != Timeline.TimeLineEnum.LOCAL && timeLineType != Timeline.TimeLineEnum.PUBLIC && timeLineType != Timeline.TimeLineEnum.REMOTE && timeLineType != Timeline.TimeLineEnum.LIST && timeLineType != Timeline.TimeLineEnum.TAG;
    }

    /**
     * Update a QuickLoad in db
     *
     * @param quickLoad {@link QuickLoad}
     * @throws DBException exception with database
     */
    private void updateStatus(QuickLoad quickLoad) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_POSITION, quickLoad.position);
        if (quickLoad.statuses != null) {
            values.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(quickLoad.statuses));
        } else if (quickLoad.notifications != null) {
            values.put(Sqlite.COL_STATUSES, Notification.notificationsToStringStorage(quickLoad.notifications));
        }
        //Inserts token
        try {
            db.update(Sqlite.TABLE_QUICK_LOAD,
                    values, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                    new String[]{quickLoad.user_id, quickLoad.instance, quickLoad.slug});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert or update a QuickLoad
     *
     * @param quickLoad {@link QuickLoad}
     * @throws DBException exception with database
     */
    private void insertOrUpdate(QuickLoad quickLoad) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean exists = quickLoadExist(quickLoad);
        if (exists) {
            updateStatus(quickLoad);
        } else {
            insertQuickLoad(quickLoad);
        }
    }

    /**
     * Check if a QuickLoad exists in db
     *
     * @param quickLoad QuickLoad {@link QuickLoad}
     * @return boolean - QuickLoad exists
     * @throws DBException Exception
     */
    public boolean quickLoadExist(QuickLoad quickLoad) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_QUICK_LOAD
                + " where " + Sqlite.COL_SLUG + " = '" + quickLoad.slug + "'"
                + " AND " + Sqlite.COL_INSTANCE + " = '" + quickLoad.instance + "'"
                + " AND " + Sqlite.COL_USER_ID + "= '" + quickLoad.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }


    /**
     * Count statuses in cache for an account
     *
     * @param account Account {@link Account}
     * @return int - Number of statuses
     * @throws DBException Exception
     */
    public int count(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        int count = 0;
        Cursor c = db.query(Sqlite.TABLE_QUICK_LOAD, null, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                new String[]{account.user_id, account.instance}, null, null, null, null);
        List<QuickLoad> quickLoadList = cursorToQuickLoadList(c);
        if (quickLoadList != null) {
            for (QuickLoad quickLoad : quickLoadList) {
                count += quickLoad.statuses.size();
            }
        }
        return count;
    }

    /**
     * @param position     - current position in timeline
     * @param timeLineType - Timeline.TimeLineEnum
     * @param statusList   - List<Status> to save
     * @param ident        - the name for pinned timeline
     */
    public void storeTimeline(int position, String user_id, String instance, Timeline.TimeLineEnum timeLineType, List<Status> statusList, String ident) {
        if (cannotBeStored(timeLineType)) {
            return;
        }
        String key = timeLineType.getValue();
        if (ident != null) {
            key += "|" + ident;
        }
        QuickLoad quickLoad = new QuickLoad();
        quickLoad.position = position;
        quickLoad.statuses = statusList;
        quickLoad.slug = key;
        quickLoad.instance = instance;
        quickLoad.user_id = user_id;
        purge(quickLoad);
        try {
            insertOrUpdate(quickLoad);
        } catch (DBException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param position             - current position in timeline
     * @param notificationTypeEnum - Timeline.NotificationTypeEnum
     * @param notificationList     - List<Notification> to save
     */
    public void storeNotifications(int position, String user_id, String instance, FragmentMastodonNotification.NotificationTypeEnum notificationTypeEnum, List<Notification> notificationList) {

        String key = notificationTypeEnum.getValue();
        QuickLoad quickLoad = new QuickLoad();
        quickLoad.position = position;
        quickLoad.notifications = notificationList;
        quickLoad.slug = key;
        quickLoad.instance = instance;
        quickLoad.user_id = user_id;
        purge(quickLoad);
        try {
            insertOrUpdate(quickLoad);
        } catch (DBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert a QuickLoad in db
     *
     * @param quickLoad {@link QuickLoad}
     * @throws DBException exception with database
     */
    private void insertQuickLoad(QuickLoad quickLoad) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_USER_ID, quickLoad.user_id);
        values.put(Sqlite.COL_INSTANCE, quickLoad.instance);
        values.put(Sqlite.COL_SLUG, quickLoad.slug);
        values.put(Sqlite.COL_POSITION, quickLoad.position);
        if (quickLoad.statuses != null) {
            values.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(quickLoad.statuses));
        } else if (quickLoad.notifications != null) {
            values.put(Sqlite.COL_STATUSES, Notification.notificationsToStringStorage(quickLoad.notifications));
        }
        //Inserts token
        try {
            db.insertOrThrow(Sqlite.TABLE_QUICK_LOAD, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * delete all cache for all accounts
     *
     * @return long - db id
     * @throws DBException exception with database
     */
    @SuppressWarnings("UnusedReturnValue")
    public long deleteForAllAccount() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_QUICK_LOAD, null, null);
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
    @SuppressWarnings("UnusedReturnValue")
    public long deleteForAccount(BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            return db.delete(Sqlite.TABLE_QUICK_LOAD,
                    Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =?",
                    new String[]{account.user_id, account.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Update a status in quickload
     *
     * @param account   {@link BaseAccount}
     * @param newStatus - Status
     * @throws DBException exception with database
     */
    public void updateStatus(BaseAccount account, Status newStatus) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }

        QuickLoad homeQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.HOME, null);
        QuickLoad localQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.LOCAL, null);
        QuickLoad publicQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.PUBLIC, null);

        if (homeQuickLoad != null && homeQuickLoad.statuses != null && newStatus != null) {
            for (int i = 0; i < homeQuickLoad.statuses.size(); i++) {
                if (homeQuickLoad.statuses.get(i).id.equals(newStatus.id)) {
                    homeQuickLoad.statuses.set(i, newStatus);
                    break;
                }
            }
            ContentValues valuesHome = new ContentValues();
            valuesHome.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(homeQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesHome, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{homeQuickLoad.user_id, homeQuickLoad.instance, homeQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (localQuickLoad != null && localQuickLoad.statuses != null && newStatus != null) {
            for (int i = 0; i < localQuickLoad.statuses.size(); i++) {
                if (localQuickLoad.statuses.get(i).id.equals(newStatus.id)) {
                    localQuickLoad.statuses.set(i, newStatus);
                    break;
                }
            }
            ContentValues valuesLocal = new ContentValues();
            valuesLocal.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(localQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesLocal, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{localQuickLoad.user_id, localQuickLoad.instance, localQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (publicQuickLoad != null && publicQuickLoad.statuses != null && newStatus != null) {
            for (int i = 0; i < publicQuickLoad.statuses.size(); i++) {
                if (publicQuickLoad.statuses.get(i).id.equals(newStatus.id)) {
                    publicQuickLoad.statuses.set(i, newStatus);
                    break;
                }
            }
            ContentValues valuesPublic = new ContentValues();
            valuesPublic.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(publicQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesPublic, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{publicQuickLoad.user_id, publicQuickLoad.instance, publicQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Delete a status in quickload
     *
     * @param account {@link Account}
     * @param id      - String id of the status
     * @throws DBException exception with database
     */
    public void deleteStatus(BaseAccount account, String id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }

        QuickLoad homeQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.HOME, null);
        QuickLoad localQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.LOCAL, null);
        QuickLoad publicQuickLoad = getSavedValue(account, Timeline.TimeLineEnum.PUBLIC, null);

        if (homeQuickLoad != null && homeQuickLoad.statuses != null) {
            for (Status status : homeQuickLoad.statuses) {
                if (status.id.equals(id)) {
                    homeQuickLoad.statuses.remove(status);
                    break;
                }
            }
            ContentValues valuesHome = new ContentValues();
            valuesHome.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(homeQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesHome, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{homeQuickLoad.user_id, homeQuickLoad.instance, homeQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (localQuickLoad != null && localQuickLoad.statuses != null) {
            for (Status status : localQuickLoad.statuses) {
                if (status.id.equals(id)) {
                    localQuickLoad.statuses.remove(status);
                    break;
                }
            }
            ContentValues valuesLocal = new ContentValues();
            valuesLocal.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(localQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesLocal, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{localQuickLoad.user_id, localQuickLoad.instance, localQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (publicQuickLoad != null && publicQuickLoad.statuses != null) {
            for (Status status : publicQuickLoad.statuses) {
                if (status.id.equals(id)) {
                    publicQuickLoad.statuses.remove(status);
                    break;
                }
            }
            ContentValues valuesPublic = new ContentValues();
            valuesPublic.put(Sqlite.COL_STATUSES, StatusDraft.mastodonStatusListToStringStorage(publicQuickLoad.statuses));
            //Inserts token
            try {
                db.update(Sqlite.TABLE_QUICK_LOAD,
                        valuesPublic, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                        new String[]{publicQuickLoad.user_id, publicQuickLoad.instance, publicQuickLoad.slug});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Retrieves saved values
     *
     * @param timeLineType - Timeline.TimeLineEnum
     * @param ident        - the name for pinned timeline
     * @return SavedValues
     */
    public QuickLoad getSavedValue(String user_id, String instance, Timeline.TimeLineEnum timeLineType, String ident) {
        if (cannotBeStored(timeLineType)) {
            return null;
        }
        String key = timeLineType.getValue();
        if (ident != null) {
            key += "|" + ident;
        }
        try {
            return get(user_id, instance, key);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Retrieves saved values
     *
     * @param notificationTypeEnum - FragmentMastodonNotification.NotificationTypeEnum
     * @return SavedValues
     */
    public QuickLoad getSavedValue(String user_id, String instance, FragmentMastodonNotification.NotificationTypeEnum notificationTypeEnum) {
        String key = notificationTypeEnum.getValue();
        try {
            return get(user_id, instance, key);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Retrieves saved values
     *
     * @param notificationTypeEnum - FragmentMastodonNotification.NotificationTypeEnum
     * @return SavedValues
     */
    public QuickLoad getSavedValue(BaseAccount account, FragmentMastodonNotification.NotificationTypeEnum notificationTypeEnum) {
        String key = notificationTypeEnum.getValue();
        try {
            return get(key, account);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves saved values
     *
     * @param timeLineType - Timeline.TimeLineEnum
     * @param ident        - the name for pinned timeline
     * @return SavedValues
     */
    public QuickLoad getSavedValue(BaseAccount account, Timeline.TimeLineEnum timeLineType, String ident) {
        if (cannotBeStored(timeLineType)) {
            return null;
        }
        String key = timeLineType.getValue();
        if (ident != null) {
            key += "|" + ident;
        }
        try {
            return get(key, account);
        } catch (DBException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Purge the list to avoid long list of Statuses in db
     *
     * @param quickLoad - QuickLoad to purge
     */
    private void purge(QuickLoad quickLoad) {
        List<Status> statuses = quickLoad.statuses;
        int position = quickLoad.position;
        int limit = MastodonHelper.statusesPerCall(_mContext) + 10;
        int startAt = Math.max(position - limit, 0);
        int endAt = Math.min(position + limit, statuses.size());
        quickLoad.position = position - startAt;
        quickLoad.statuses = statuses.subList(startAt, endAt);
    }


    /**
     * Restore statusDraft list from db
     *
     * @param c Cursor
     * @return List<Emoji>
     */
    private List<QuickLoad> cursorToQuickLoadList(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<QuickLoad> quickLoads = new ArrayList<>();
        while (c.moveToNext()) {
            QuickLoad quickLoad = convertCursorToQuickLoad(c);
            quickLoads.add(quickLoad);
        }
        //Close the cursor
        c.close();
        return quickLoads;
    }

    /**
     * Convert a cursor to QuickLoad
     *
     * @param c Cursor
     * @return QuickLoad
     */
    private QuickLoad convertCursorToQuickLoad(Cursor c) {
        QuickLoad quickLoad = new QuickLoad();
        quickLoad.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        quickLoad.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        quickLoad.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        quickLoad.slug = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_SLUG));
        if (typeOfFetch == type.STATUSES) {
            quickLoad.statuses = StatusDraft.restoreStatusListFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUSES)));
        } else if (typeOfFetch == type.NOTIFICATIONS) {
            quickLoad.notifications = Notification.restoreNotificationsFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_STATUSES)));
        }
        quickLoad.position = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_POSITION));
        //TimelineHelper.filterStatus(_mContext, quickLoad.statuses, TimelineHelper.FilterTimeLineType.PUBLIC);
        return quickLoad;
    }

    /**
     * Convert a cursor to QuickLoad
     *
     * @param c Cursor
     * @return QuickLoad
     */
    private QuickLoad cursorToQuickLoad(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        QuickLoad quickLoad = convertCursorToQuickLoad(c);
        //Close the cursor
        c.close();
        return quickLoad;
    }

    /**
     * Get paginated statuses from db
     *
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    private QuickLoad get(String user_id, String instance, String slug) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_QUICK_LOAD, null, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                    new String[]{user_id, instance, slug}, null, null, null, "1");
            return cursorToQuickLoad(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get paginated statuses from db
     *
     * @return Statuses
     * @throws DBException - throws a db exception
     */
    private QuickLoad get(String slug, BaseAccount account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_QUICK_LOAD, null, Sqlite.COL_USER_ID + " =  ? AND " + Sqlite.COL_INSTANCE + " =? AND " + Sqlite.COL_SLUG + "=?",
                    new String[]{account.user_id, account.instance, slug}, null, null, null, "1");
            return cursorToQuickLoad(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    enum type {
        STATUSES,
        NOTIFICATIONS
    }


}
