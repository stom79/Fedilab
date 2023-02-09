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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;

public class CamelTag {

    private final SQLiteDatabase db;
    @SerializedName("name")
    public String name;

    public CamelTag(Context context) {
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Insert or update a status
     *
     * @param name {@link String}
     * @return long - -1 if exists or id
     * @throws DBException exception with database
     */
    public synchronized long insert(String name) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        boolean exists = tagExists(name);
        if (!exists) {
            ContentValues values = new ContentValues();
            values.put(Sqlite.COL_TAG, name);
            try {
                return db.insertOrThrow(Sqlite.TABLE_CACHE_TAGS, null, values);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns tags List of {@String} containing "search"
     *
     * @param search - String the word to search
     * @return List<String>
     */
    public List<String> getBy(String search) {
        Cursor c = db.query(Sqlite.TABLE_CACHE_TAGS, null, Sqlite.COL_TAG + " LIKE \"%" + search + "%\"", null, null, null, null, null);
        return cursorToTag(c);
    }

    public boolean tagExists(String name) throws DBException {
        Cursor c = db.query(Sqlite.TABLE_CACHE_TAGS, null, Sqlite.COL_TAG + " = \"" + name + "\"", null, null, null, null, null);
        boolean isPresent = (c != null && c.getCount() > 0);
        assert c != null;
        c.close();
        return isPresent;
    }

    public void removeAll() {
        db.delete(Sqlite.TABLE_CACHE_TAGS, null, null);
    }

    public void removeTag(String tag) {
        db.delete(Sqlite.TABLE_CACHE_TAGS, Sqlite.COL_TAG + " = ?", new String[]{tag});
    }

    public void update(String oldTag, String newTag) {
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_TAG, newTag);
        try {
            db.update(Sqlite.TABLE_CACHE_TAGS, values, Sqlite.COL_TAG + " = ?", new String[]{oldTag});
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns all tags in db
     *
     * @return string tags List<String>
     */
    public List<String> getAll() {
        try {
            Cursor c = db.query(Sqlite.TABLE_CACHE_TAGS, null, null, null, null, null, Sqlite.COL_TAG + " ASC", null);
            return cursorToTag(c);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> cursorToTag(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<String> tags = new ArrayList<>();
        while (c.moveToNext()) {
            tags.add(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TAG)));
        }
        //Close the cursor
        c.close();
        //Tag list is returned
        return tags;
    }
}
