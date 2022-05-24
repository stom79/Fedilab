package app.fedilab.android.client.entities.api;
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

import java.io.Serializable;

import app.fedilab.android.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;


public class InstanceInfo implements Serializable {

    private final SQLiteDatabase db;
    @SerializedName("instance")
    public String instance;
    @SerializedName("info")
    public Instance info;

    public InstanceInfo() {
        db = null;
    }

    public InstanceInfo(Context context) {
        //Creation of the DB with tables
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a list of Emoji class
     *
     * @param instance {@link Instance} to serialize
     * @return String serialized instance
     */
    public static String instanceInfoToStringStorage(Instance instance) {
        Gson gson = new Gson();
        try {
            return gson.toJson(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized an instance
     *
     * @param serializedInstanceInfo String serialized instance
     * @return {@link Instance}
     */
    public static Instance restoreInstanceInfoFromString(String serializedInstanceInfo) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedInstanceInfo, Instance.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert or update instance
     *
     * @param instanceInfo {@link InstanceInfo}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(InstanceInfo instanceInfo) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (instanceInfo == null) {
            return -1;
        }
        boolean exists = instanceInfoExist(instanceInfo);
        long idReturned;
        if (exists) {
            idReturned = updateInstanceInfo(instanceInfo);
        } else {
            idReturned = insertInstanceInfo(instanceInfo);
        }
        return idReturned;
    }

    /**
     * Check if instanceInfo exists in db
     *
     * @param instanceInfo InstanceInfo {@link InstanceInfo}
     * @return boolean - instanceInfo exists
     * @throws DBException Exception
     */
    public boolean instanceInfoExist(InstanceInfo instanceInfo) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_INSTANCE_INFO
                + " where " + Sqlite.COL_INSTANCE + " = '" + instanceInfo.instance + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Insert instanceInfo in db
     *
     * @param instanceInfo {@link InstanceInfo}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertInstanceInfo(InstanceInfo instanceInfo) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, instanceInfo.instance);
        values.put(Sqlite.COL_INFO, instanceInfoToStringStorage(instanceInfo.info));
        //Inserts instance
        try {
            return db.insertOrThrow(Sqlite.TABLE_INSTANCE_INFO, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * update instanceInfo in db
     *
     * @param instanceInfo {@link InstanceInfo}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateInstanceInfo(InstanceInfo instanceInfo) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INFO, instanceInfoToStringStorage(instanceInfo.info));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_INSTANCE_INFO,
                    values, Sqlite.COL_INSTANCE + " =  ?",
                    new String[]{instanceInfo.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the info for an instance
     *
     * @param instance String
     * @return InstanceInfo - {@link InstanceInfo}
     */
    public Instance getInstanceInfo(String instance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_INSTANCE_INFO, null, Sqlite.COL_INSTANCE + " = '" + instance + "'", null, null, null, null, "1");
            return cursorToInstanceInfo(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restore instanceInfo from db
     *
     * @param c Cursor
     * @return Instance
     */
    private Instance cursorToInstanceInfo(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        Instance instance = restoreInstanceInfoFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INFO)));
        c.close();
        return instance;
    }
}
