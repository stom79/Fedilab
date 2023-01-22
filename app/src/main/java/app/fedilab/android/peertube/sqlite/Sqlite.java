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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Sqlite extends SQLiteOpenHelper {

    public static final int DB_VERSION = 4;
    public static final String DB_NAME = "mastodon_etalab_db";
    /***
     * List of tables to manage users and data
     */
    //Table of owned accounts
    static final String TABLE_USER_ACCOUNT = "USER_ACCOUNT";
    static final String COL_USER_ID = "USER_ID";
    static final String COL_USERNAME = "USERNAME";
    static final String COL_ACCT = "ACCT";
    static final String COL_DISPLAYED_NAME = "DISPLAYED_NAME";
    static final String COL_LOCKED = "LOCKED";
    static final String COL_CREATED_AT = "CREATED_AT";
    static final String COL_FOLLOWERS_COUNT = "FOLLOWERS_COUNT";
    static final String COL_FOLLOWING_COUNT = "FOLLOWING_COUNT";
    static final String COL_STATUSES_COUNT = "STATUSES_COUNT";
    static final String COL_NOTE = "NOTE";
    static final String COL_URL = "URL";
    static final String COL_AVATAR = "AVATAR";
    static final String COL_AVATAR_STATIC = "AVATAR_STATIC";
    static final String COL_HEADER = "HEADER";
    static final String COL_HEADER_STATIC = "HEADER_STATIC";
    static final String COL_INSTANCE = "INSTANCE";
    static final String COL_OAUTHTOKEN = "OAUTH_TOKEN";
    static final String COL_CLIENT_ID = "CLIENT_ID";
    static final String COL_CLIENT_SECRET = "CLIENT_SECRET";
    static final String COL_REFRESH_TOKEN = "REFRESH_TOKEN";
    static final String COL_IS_MODERATOR = "IS_MODERATOR";
    static final String COL_IS_ADMIN = "IS_ADMIN";
    static final String COL_UPDATED_AT = "UPDATED_AT";
    static final String COL_PRIVACY = "PRIVACY";
    static final String COL_SENSITIVE = "SENSITIVE";
    //Table for peertube favorites
    static final String TABLE_PEERTUBE_FAVOURITES = "PEERTUBE_FAVOURITES";
    static final String COL_ID = "ID";
    static final String COL_UUID = "UUID";
    static final String COL_CACHE = "CACHE";
    static final String COL_DATE = "DATE";
    static final String COL_USER_INSTANCE = "USER_INSTANCE";
    static final String TABLE_BOOKMARKED_INSTANCES = "BOOKMARKED_INSTANCES";
    static final String COL_ABOUT = "ABOUT";
    static final String TABLE_LOCAL_PLAYLISTS = "LOCAL_PLAYLISTS";
    static final String COL_PLAYLIST = "PLAYLIST";
    static final String TABLE_VIDEOS = "VIDEOS";
    static final String COL_VIDEO_DATA = "VIDEO_DATA";
    static final String COL_PLAYLIST_ID = "PLAYLIST_ID";
    static final String COL_SOFTWARE = "SOFTWARE";
    private static final String CREATE_TABLE_USER_ACCOUNT = "CREATE TABLE " + TABLE_USER_ACCOUNT + " ("
            + COL_USER_ID + " TEXT, " + COL_USERNAME + " TEXT NOT NULL, " + COL_ACCT + " TEXT NOT NULL, "
            + COL_DISPLAYED_NAME + " TEXT NOT NULL, " + COL_LOCKED + " INTEGER NOT NULL, "
            + COL_FOLLOWERS_COUNT + " INTEGER NOT NULL, " + COL_FOLLOWING_COUNT + " INTEGER NOT NULL, " + COL_STATUSES_COUNT + " INTEGER NOT NULL, "
            + COL_NOTE + " TEXT NOT NULL, " + COL_URL + " TEXT NOT NULL, "
            + COL_AVATAR + " TEXT NOT NULL, " + COL_AVATAR_STATIC + " TEXT NOT NULL, "
            + COL_HEADER + " TEXT NOT NULL, " + COL_HEADER_STATIC + " TEXT NOT NULL, "
            + COL_IS_MODERATOR + " INTEGER  DEFAULT 0, "
            + COL_IS_ADMIN + " INTEGER  DEFAULT 0, "
            + COL_CLIENT_ID + " TEXT, " + COL_CLIENT_SECRET + " TEXT, " + COL_REFRESH_TOKEN + " TEXT,"
            + COL_UPDATED_AT + " TEXT, "
            + COL_PRIVACY + " TEXT, "
            + COL_SOFTWARE + " TEXT NOT NULL DEFAULT \"PEERTUBE\", "
            + COL_SENSITIVE + " INTEGER DEFAULT 0, "
            + COL_INSTANCE + " TEXT NOT NULL, " + COL_OAUTHTOKEN + " TEXT NOT NULL, " + COL_CREATED_AT + " TEXT NOT NULL)";


    public static SQLiteDatabase db;
    private static Sqlite sInstance;
    private final String CREATE_TABLE_PEERTUBE_FAVOURITES = "CREATE TABLE "
            + TABLE_PEERTUBE_FAVOURITES + "("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_UUID + " TEXT NOT NULL, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_CACHE + " TEXT NOT NULL, "
            + COL_DATE + " TEXT NOT NULL)";
    private final String CREATE_TABLE_STORED_INSTANCES = "CREATE TABLE "
            + TABLE_BOOKMARKED_INSTANCES + "("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_ABOUT + " TEXT NOT NULL, "
            + COL_USER_INSTANCE + " TEXT NOT NULL)";
    private final String CREATE_TABLE_LOCAL_PLAYLISTS = "CREATE TABLE "
            + TABLE_LOCAL_PLAYLISTS + "("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_ACCT + " TEXT NOT NULL, "
            + COL_UUID + " TEXT NOT NULL, "
            + COL_PLAYLIST + " TEXT NOT NULL)";
    private final String CREATE_TABLE_VIDEOS = "CREATE TABLE "
            + TABLE_VIDEOS + "("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_UUID + " TEXT NOT NULL, "
            + COL_VIDEO_DATA + " TEXT NOT NULL, "
            + COL_PLAYLIST_ID + " INTEGER, "
            + " FOREIGN KEY (" + COL_PLAYLIST_ID + ") REFERENCES " + COL_PLAYLIST + "(" + COL_ID + "));";

    public Sqlite(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    public static synchronized Sqlite getInstance(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        if (sInstance == null) {
            sInstance = new Sqlite(context, name, factory, version);
        }
        return sInstance;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER_ACCOUNT);
        db.execSQL(CREATE_TABLE_PEERTUBE_FAVOURITES);
        db.execSQL(CREATE_TABLE_STORED_INSTANCES);
        db.execSQL(CREATE_TABLE_LOCAL_PLAYLISTS);
        db.execSQL(CREATE_TABLE_VIDEOS);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 4:
                dropColumn(TABLE_USER_ACCOUNT, new String[]{COL_SOFTWARE});
            case 3:
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEOS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCAL_PLAYLISTS);
            case 2:
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKED_INSTANCES);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void dropColumn(
            String tableName,
            String[] colsToRemove) {
        List<String> updatedTableColumns = getTableColumns(tableName);
        // Remove the columns we don't want anymore from the table's list of columns
        updatedTableColumns.removeAll(Arrays.asList(colsToRemove));
        String columnsSeperated = TextUtils.join(",", updatedTableColumns);
        db.execSQL("ALTER TABLE " + tableName + " RENAME TO " + tableName + "_old;");
        // Creating the table on its new format (no redundant columns)
        db.execSQL("CREATE TABLE " + tableName);
        // Populating the table with the data
        db.execSQL("INSERT INTO " + tableName + "(" + columnsSeperated + ") SELECT "
                + columnsSeperated + " FROM " + tableName + "_old;");
        db.execSQL("DROP TABLE " + tableName + "_old;");
    }

    public List<String> getTableColumns(String tableName) {
        ArrayList<String> columns = new ArrayList<>();
        String cmd = "pragma table_info(" + tableName + ");";
        Cursor cur = db.rawQuery(cmd, null);
        while (cur.moveToNext()) {
            columns.add(cur.getString(cur.getColumnIndex("name")));
        }
        cur.close();
        return columns;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                db.execSQL(CREATE_TABLE_STORED_INSTANCES);
            case 2:
                db.execSQL(CREATE_TABLE_LOCAL_PLAYLISTS);
                db.execSQL(CREATE_TABLE_VIDEOS);
            case 3:
                db.execSQL("ALTER TABLE " + TABLE_USER_ACCOUNT + " ADD COLUMN " + COL_SOFTWARE + " TEXT NOT NULL DEFAULT \"PEERTUBE\"");
        }
    }

    public SQLiteDatabase open() {
        //opened with write access
        db = getWritableDatabase();
        return db;
    }

    public void close() {
        //Close the db
        if (db != null && db.isOpen()) {
            db.close();
        }
    }


}
