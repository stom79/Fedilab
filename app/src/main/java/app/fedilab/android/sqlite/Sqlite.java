package app.fedilab.android.sqlite;
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


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class Sqlite extends SQLiteOpenHelper {


    public static final int DB_VERSION = 2;
    public static final String DB_NAME = "fedilab_db";

    //Table of owned accounts
    public static final String TABLE_USER_ACCOUNT = "USER_ACCOUNT";


    public static final String COL_USER_ID = "USER_ID";
    public static final String COL_INSTANCE = "INSTANCE";
    public static final String COL_API = "API";
    public static final String COL_SOFTWARE = "SOFTWARE";
    public static final String COL_TOKEN = "TOKEN";
    public static final String COL_REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String COL_TOKEN_VALIDITY = "TOKEN_VALIDITY";
    public static final String COL_ACCOUNT = "ACCOUNT";
    public static final String COL_APP_CLIENT_SECRET = "APP_CLIENT_SECRET";
    public static final String COL_APP_CLIENT_ID = "APP_CLIENT_ID";
    public static final String COL_CREATED_AT = "CREATED_AT";
    public static final String COL_UPDATED_AT = "UPDATED_AT";
    //Table for timelines
    public static final String TABLE_TIMELINES = "TIMELINES";
    public static final String COL_ID = "ID";
    public static final String COL_TYPE = "TYPE";
    public static final String COL_DISPLAYED = "DISPLAYED";
    public static final String COL_POSITION = "POSITION";
    public static final String COL_REMOTE_INSTANCE = "REMOTE_INSTANCE";
    public static final String COL_TIMELINE_OPTION = "TIMELINE_OPTION";
    //Table for cache
    public static final String TABLE_STATUS_CACHE = "STATUS_CACHE";
    public static final String COL_STATUS_ID = "STATUS_ID";
    public static final String COL_STATUS = "STATUS";
    //Table for emojis
    public static final String TABLE_EMOJI_INSTANCE = "EMOJI_INSTANCE";
    public static final String COL_EMOJI_LIST = "EMOJI_LIST";
    //Table for instance info
    public static final String TABLE_INSTANCE_INFO = "INSTANCE_INFO";
    public static final String COL_INFO = "INFO";
    //Table for instance drafts
    public static final String TABLE_STATUS_DRAFT = "STATUS_DRAFT";
    public static final String COL_DRAFTS = "DRAFTS";
    public static final String COL_REPLIES = "REPLIES";
    public static final String COL_STATE = "STATE";
    //Table pinned timelines
    public static final String TABLE_PINNED_TIMELINES = "PINNED_TIMELINES";
    public static final String COL_PINNED_TIMELINES = "PINNED_TIMELINES";
    //Schedule boost
    public static final String TABLE_SCHEDULE_BOOST = "SCHEDULE_BOOST";
    public static final String COL_SCHEDULED_AT = "SCHEDULED_AT";
    public static final String COL_REBLOGGED = "REBLOGGED";
    public static final String COL_WORKER_UUID = "WORKER_UUID";
    //Quick load
    public static final String TABLE_QUICK_LOAD = "QUICK_LOAD";
    public static final String COL_SLUG = "SLUG";
    public static final String COL_STATUSES = "STATUSES";

    private static final String CREATE_TABLE_USER_ACCOUNT = "CREATE TABLE " + TABLE_USER_ACCOUNT + " ("
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_API + " TEXT NOT NULL, "
            + COL_SOFTWARE + " TEXT, "
            + COL_TOKEN + " TEXT NOT NULL, "
            + COL_REFRESH_TOKEN + " TEXT, "
            + COL_TOKEN_VALIDITY + " INTEGER, "
            + COL_ACCOUNT + " TEXT NOT NULL, "
            + COL_APP_CLIENT_ID + " TEXT NOT NULL, "
            + COL_APP_CLIENT_SECRET + " TEXT NOT NULL, "
            + COL_CREATED_AT + " TEXT NOT NULL,"
            + COL_UPDATED_AT + " TEXT)";
    private static final String CREATE_TABLE_TIMELINES = "CREATE TABLE IF NOT EXISTS " + TABLE_TIMELINES + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_POSITION + " INTEGER NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, " + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_TYPE + " TEXT NOT NULL, "
            + COL_REMOTE_INSTANCE + " TEXT, "
            + COL_DISPLAYED + " INTEGER NOT NULL, "
            + COL_TIMELINE_OPTION + " TEXT)";
    private static final String CREATE_TABLE_STATUS_CACHE = "CREATE TABLE IF NOT EXISTS " + TABLE_STATUS_CACHE + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_ID + " TEXT NOT NULL, " + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_TYPE + " TEXT NOT NULL, "
            + COL_STATUS_ID + " TEXT NOT NULL, "
            + COL_STATUS + " TEXT NOT NULL, "
            + COL_CREATED_AT + " TEXT NOT NULL,"
            + COL_UPDATED_AT + " TEXT)";
    private static final String CREATE_TABLE_EMOJI_INSTANCE = "CREATE TABLE IF NOT EXISTS " + TABLE_EMOJI_INSTANCE + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_EMOJI_LIST + " TEXT)";

    private static final String CREATE_TABLE_INSTANCE_INFO = "CREATE TABLE IF NOT EXISTS " + TABLE_INSTANCE_INFO + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_INFO + " TEXT)";


    private static final String CREATE_TABLE_STATUS_DRAFT = "CREATE TABLE IF NOT EXISTS " + TABLE_STATUS_DRAFT + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_DRAFTS + " TEXT NOT NULL, "
            + COL_REPLIES + " TEXT, "
            + COL_STATE + " TEXT, "
            + COL_WORKER_UUID + " TEXT, "
            + COL_CREATED_AT + " TEXT NOT NULL, "
            + COL_UPDATED_AT + " TEXT, "
            + COL_SCHEDULED_AT + " TEXT)";

    private static final String CREATE_TABLE_PINNED_TIMELINES = "CREATE TABLE IF NOT EXISTS " + TABLE_PINNED_TIMELINES + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_PINNED_TIMELINES + " TEXT NOT NULL, "
            + COL_CREATED_AT + " TEXT NOT NULL, "
            + COL_UPDATED_AT + " TEXT)";


    private static final String CREATE_TABLE_SCHEDULE_BOOST = "CREATE TABLE IF NOT EXISTS " + TABLE_SCHEDULE_BOOST + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_STATUS_ID + " TEXT NOT NULL, "
            + COL_STATUS + " TEXT NOT NULL, "
            + COL_REBLOGGED + " INTEGER, "
            + COL_WORKER_UUID + " TEXT, "
            + COL_SCHEDULED_AT + " TEXT NOT NULL)";

    private static final String CREATE_TABLE_QUICK_LOAD = "CREATE TABLE IF NOT EXISTS " + TABLE_QUICK_LOAD + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_INSTANCE + " TEXT NOT NULL, "
            + COL_USER_ID + " TEXT NOT NULL, "
            + COL_SLUG + " TEXT NOT NULL, "
            + COL_POSITION + " INTEGER, "
            + COL_STATUSES + " TEXT NOT NULL)";

    public static SQLiteDatabase db;
    private static Sqlite sInstance;

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
        db.execSQL(CREATE_TABLE_TIMELINES);
        db.execSQL(CREATE_TABLE_STATUS_CACHE);
        db.execSQL(CREATE_TABLE_EMOJI_INSTANCE);
        db.execSQL(CREATE_TABLE_INSTANCE_INFO);
        db.execSQL(CREATE_TABLE_STATUS_DRAFT);
        db.execSQL(CREATE_TABLE_PINNED_TIMELINES);
        db.execSQL(CREATE_TABLE_SCHEDULE_BOOST);
        db.execSQL(CREATE_TABLE_QUICK_LOAD);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (oldVersion) {
            case 1:
                db.execSQL(CREATE_TABLE_QUICK_LOAD);
            default:
                break;
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
