package app.fedilab.android.mastodon.client.entities.app;
/* Copyright 2024 Thomas Schneider
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Base64;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

/**
 * Class that manages Bundle of Intent from database
 */
public class CachedBundle {

    public String id;
    public Bundle bundle;
    public Date created_at;

    private SQLiteDatabase db;

    private transient Context context;

    public CachedBundle() {}
    public CachedBundle(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }


    /**
     * Insert a bundle in db
     *
     * @param bundle {@link Bundle}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertIntent(Bundle bundle) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_BUNDLE, serializeBundle(bundle));
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        //Inserts token
        try {
            return db.insertOrThrow(Sqlite.TABLE_INTENT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public interface BundleCallback{
        public void get(Bundle bundle);
    }

    public interface BundleInsertCallback{
        public void inserted(long bundleId);
    }

    public void getBundle(long id, BundleCallback callback) {
        new Thread(()->{
            Bundle bundle = null;
            try {
                CachedBundle cachedBundle = getCachedBundle(String.valueOf(id));
                if (cachedBundle != null) {
                    bundle = cachedBundle.bundle;
                }
                removeIntent(String.valueOf(id));
            } catch (DBException ignored) {}
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Bundle finalBundle = bundle;
            Runnable myRunnable = () -> callback.get(finalBundle);
            mainHandler.post(myRunnable);
        }).start();
    }

    public void insertBundle(Bundle bundle, BundleInsertCallback callback) {
        new Thread(()->{
            long dbBundleId = -1;
            try {
                dbBundleId = insertIntent(bundle);
            } catch (DBException ignored) {}
            Handler mainHandler = new Handler(Looper.getMainLooper());
            long finalDbBundleId = dbBundleId;
            Runnable myRunnable = () -> callback.inserted(finalDbBundleId);
            mainHandler.post(myRunnable);
        }).start();
    }

    /**
     * Returns a bundle by its ID
     *
     * @param id String
     * @return CachedBundle {@link CachedBundle}
     */
    private CachedBundle getCachedBundle(String id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_INTENT, null, Sqlite.COL_ID + " = \"" + id + "\"", null, null, null, null, "1");
            return cursorToCachedBundle(c);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove a bundle from db
     *
     * @param id - intent id
     */
    private void removeIntent(String id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        db.delete(Sqlite.TABLE_INTENT, Sqlite.COL_ID + " = '" + id + "'", null);
    }



    /***
     * Method to hydrate an CachedBundle from database
     * @param c Cursor
     * @return CachedBundle {@link CachedBundle}
     */
    private CachedBundle cursorToCachedBundle(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        //New user
        CachedBundle account = convertCursorToCachedBundle(c);
        //Close the cursor
        c.close();
        return account;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return BaseAccount
     */
    private CachedBundle convertCursorToCachedBundle(Cursor c) {
        CachedBundle cachedBundle = new CachedBundle();
        cachedBundle.id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        cachedBundle.bundle = deserializeBundle(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_BUNDLE)));
        cachedBundle.created_at = Helper.stringToDate(context, c.getString(c.getColumnIndexOrThrow(Sqlite.COL_CREATED_AT)));
        return cachedBundle;
    }

    private String serializeBundle(final Bundle bundle) {
        String base64 = null;
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(bos));
            zos.write(parcel.marshall());
            zos.close();
            base64 = Base64.encodeToString(bos.toByteArray(), 0);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
        }
        return base64;
    }

    private Bundle deserializeBundle(final String base64) {
        Bundle bundle = null;
        final Parcel parcel = Parcel.obtain();
        try {
            final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            final GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(Base64.decode(base64, 0)));
            int len;
            while ((len = zis.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            zis.close();
            parcel.unmarshall(byteBuffer.toByteArray(), 0, byteBuffer.size());
            parcel.setDataPosition(0);
            bundle = parcel.readBundle(getClass().getClassLoader());
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            parcel.recycle();
        }
        return bundle;
    }

}
