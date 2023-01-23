package app.fedilab.android.peertube.sqlite;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.sqlite.Sqlite;


@SuppressWarnings("UnusedReturnValue")
public class StoredInstanceDAO {

    private final SQLiteDatabase db;
    public Context context;


    public StoredInstanceDAO(Context context, SQLiteDatabase db) {
        //Creation of the DB with tables
        this.context = context;
        this.db = db;
    }

    /**
     * Unserialized  AboutInstance
     *
     * @param serializedAboutInstance String serialized AboutInstance
     * @return AboutInstance
     */
    public static InstanceData.AboutInstance restoreAboutInstanceFromString(String serializedAboutInstance) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedAboutInstance, InstanceData.AboutInstance.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized AboutInstance class
     *
     * @param aboutInstance AboutInstance to serialize
     * @return String serialized AboutInstance
     */
    public static String aboutInstanceToStringStorage(InstanceData.AboutInstance aboutInstance) {
        Gson gson = new Gson();
        try {
            return gson.toJson(aboutInstance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert instance info in database
     *
     * @param aboutInstance    AboutInstance
     * @param targetedInstance String
     * @return boolean
     */
    public boolean insertInstance(InstanceData.AboutInstance aboutInstance, String targetedInstance) {

        if (checkExists(targetedInstance)) {
            return true;
        }
        ContentValues values = new ContentValues();
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        String userId = sharedpreferences.getString(Helper.PREF_KEY_ID, null);
        String instance = HelperInstance.getLiveInstance(context);

        values.put(Sqlite.COL_USER_ID, userId != null ? userId : "_ALL_");
        values.put(Sqlite.COL_USER_INSTANCE, instance != null ? instance : "_ALL_");
        values.put(Sqlite.COL_ABOUT, aboutInstanceToStringStorage(aboutInstance));
        values.put(Sqlite.COL_INSTANCE, targetedInstance);
        //Inserts instance
        try {
            db.insertOrThrow(Sqlite.TABLE_BOOKMARKED_INSTANCES, null, values);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Check if instance exists
     *
     * @param targetedInstance String
     * @return int
     */
    private boolean checkExists(String targetedInstance) {
        try {
            Cursor c = db.query(Sqlite.TABLE_BOOKMARKED_INSTANCES, null, Sqlite.COL_INSTANCE + " = \"" + targetedInstance + "\"", null, null, null, null, "1");
            return cursorToInstance(c) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Insert instance info in database
     *
     * @param aboutInstance    AboutInstance
     * @param targetedInstance String
     * @return int
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public int updateInstance(InstanceData.AboutInstance aboutInstance, String targetedInstance) {
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_ABOUT, aboutInstanceToStringStorage(aboutInstance));
        try {
            return db.update(Sqlite.TABLE_BOOKMARKED_INSTANCES,
                    values, Sqlite.COL_INSTANCE + " =  ?",
                    new String[]{targetedInstance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }


    public int removeInstance(String instance) {
        return db.delete(Sqlite.TABLE_BOOKMARKED_INSTANCES, Sqlite.COL_INSTANCE + " = '" + instance + "'", null);
    }


    /**
     * Returns all Instance in db
     *
     * @return List<AboutInstance>
     */
    public List<InstanceData.AboutInstance> getAllInstances() {

        try {
            Cursor c = db.query(Sqlite.TABLE_BOOKMARKED_INSTANCES, null, null, null, null, null, Sqlite.COL_INSTANCE + " ASC", null);
            return cursorToListInstances(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /***
     * Method to hydrate an AboutInstance from database
     * @param c Cursor
     * @return AboutInstance
     */
    private InstanceData.AboutInstance cursorToInstance(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        //New user
        String aboutInstanceStr = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ABOUT));
        InstanceData.AboutInstance aboutInstance = restoreAboutInstanceFromString(aboutInstanceStr);
        //Close the cursor
        c.close();
        return aboutInstance;
    }


    /***
     * Method to hydrate an AboutInstance from database
     * @param c Cursor
     * @return List<AboutInstance>
     */
    private List<InstanceData.AboutInstance> cursorToListInstances(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<InstanceData.AboutInstance> aboutInstances = new ArrayList<>();
        while (c.moveToNext()) {
            String aboutInstanceStr = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ABOUT));
            String instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
            InstanceData.AboutInstance aboutInstance = restoreAboutInstanceFromString(aboutInstanceStr);
            aboutInstance.setHost(instance);
            aboutInstances.add(aboutInstance);
        }
        //Close the cursor
        c.close();
        return aboutInstances;
    }


}
