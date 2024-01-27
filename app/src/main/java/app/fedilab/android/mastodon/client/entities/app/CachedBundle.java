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

import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import app.fedilab.android.MainApplication;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.sqlite.Sqlite;

/**
 * Class that manages Bundle of Intent from database
 */
public class CachedBundle {

    public String id;
    public Bundle bundle;
    public CacheType cacheType;
    public String instance;
    public String user_id;
    public String target_id;
    public Date created_at;

    private SQLiteDatabase db;

    private transient Context context;

    public CachedBundle() {
    }

    public CachedBundle(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }


    public void insertAccountBundle(Account account, BaseAccount currentUser) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues valuesAccount = new ContentValues();
        Bundle bundleAccount = new Bundle();
        if (account != null) {
            bundleAccount.putSerializable(Helper.ARG_ACCOUNT, account);
            valuesAccount.put(Sqlite.COL_BUNDLE, serializeBundle(bundleAccount));
            valuesAccount.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
            valuesAccount.put(Sqlite.COL_TARGET_ID, account.id);
            valuesAccount.put(Sqlite.COL_USER_ID, currentUser.user_id);
            valuesAccount.put(Sqlite.COL_INSTANCE, currentUser.instance);
            valuesAccount.put(Sqlite.COL_TYPE, CacheType.ACCOUNT.getValue());
            removeIntent(currentUser, account.id);
            db.insertOrThrow(Sqlite.TABLE_INTENT, null, valuesAccount);
        }
    }

    /**
     * Insert a bundle in db
     *
     * @param bundle {@link Bundle}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertIntent(Bundle bundle, BaseAccount currentUser) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_BUNDLE, serializeBundle(bundle));
        values.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
        values.put(Sqlite.COL_TYPE, CacheType.ARGS.getValue());
        if (bundle.containsKey(Helper.ARG_ACCOUNT) && currentUser != null) {
            ContentValues valuesAccount = new ContentValues();
            Bundle bundleAccount = new Bundle();
            Account account = null;
            try {
                account = (Account) bundle.getSerializable(Helper.ARG_ACCOUNT);
            } catch (ClassCastException ignored) {
            }
            if (account != null) {
                bundleAccount.putSerializable(Helper.ARG_ACCOUNT, account);
                valuesAccount.put(Sqlite.COL_BUNDLE, serializeBundle(bundleAccount));
                valuesAccount.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
                valuesAccount.put(Sqlite.COL_TARGET_ID, account.id);
                valuesAccount.put(Sqlite.COL_USER_ID, currentUser.user_id);
                valuesAccount.put(Sqlite.COL_INSTANCE, currentUser.instance);
                valuesAccount.put(Sqlite.COL_TYPE, CacheType.ACCOUNT.getValue());
                removeIntent(currentUser, account.id);
                db.insertOrThrow(Sqlite.TABLE_INTENT, null, valuesAccount);
            }
        }
        if (bundle.containsKey(Helper.ARG_STATUS) && currentUser != null) {
            ContentValues valuesAccount = new ContentValues();
            Bundle bundleStatus = new Bundle();
            Status status = null;
            try {
                status = (Status) bundle.getSerializable(Helper.ARG_STATUS);
            } catch (ClassCastException ignored) {
            }
            if (status != null) {
                bundleStatus.putSerializable(Helper.ARG_STATUS, status);
                valuesAccount.put(Sqlite.COL_BUNDLE, serializeBundle(bundleStatus));
                valuesAccount.put(Sqlite.COL_CREATED_AT, Helper.dateToString(new Date()));
                valuesAccount.put(Sqlite.COL_TARGET_ID, status.id);
                valuesAccount.put(Sqlite.COL_USER_ID, currentUser.user_id);
                valuesAccount.put(Sqlite.COL_INSTANCE, currentUser.instance);
                valuesAccount.put(Sqlite.COL_TYPE, CacheType.STATUS.getValue());
                removeIntent(currentUser, status.id);
                db.insertOrThrow(Sqlite.TABLE_INTENT, null, valuesAccount);
            }
        }
        try {
            return db.insertOrThrow(Sqlite.TABLE_INTENT, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void getBundle(long id, BaseAccount Account, BundleCallback callback) {
        new Thread(() -> {
            Bundle bundle = null;
            try {
                CachedBundle cachedBundle = getCachedBundle(String.valueOf(id));
                if (cachedBundle != null) {
                    bundle = cachedBundle.bundle;
                    if (bundle != null && bundle.containsKey(Helper.ARG_CACHED_ACCOUNT_ID)) {
                        Account cachedAccount = getCachedAccount(Account, bundle.getString(Helper.ARG_CACHED_ACCOUNT_ID));
                        if (cachedAccount != null) {
                            bundle.putSerializable(Helper.ARG_ACCOUNT, cachedAccount);
                        }
                    }
                    if (bundle != null && bundle.containsKey(Helper.ARG_CACHED_STATUS_ID)) {
                        Status cachedStatus = getCachedStatus(Account, bundle.getString(Helper.ARG_CACHED_STATUS_ID));
                        if (cachedStatus != null) {
                            bundle.putSerializable(Helper.ARG_STATUS, cachedStatus);
                        }
                    }
                }
            } catch (DBException ignored) {
            }
            if (bundle == null) {
                bundle = new Bundle();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Bundle finalBundle = bundle;
            Runnable myRunnable = () -> callback.get(finalBundle);
            mainHandler.post(myRunnable);
        }).start();
    }

    public void insertBundle(Bundle bundle, BaseAccount Account, BundleInsertCallback callback) {
        new Thread(() -> {
            long dbBundleId = -1;
            try {
                dbBundleId = insertIntent(bundle, Account);
            } catch (DBException ignored) {
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            long finalDbBundleId = dbBundleId;
            Runnable myRunnable = () -> callback.inserted(finalDbBundleId);
            mainHandler.post(myRunnable);
        }).start();
    }

    /**
     * Returns a bundle by targeted account id
     *
     * @param target_id String
     * @return Account {@link Account}
     */
    public Account getCachedAccount(BaseAccount account, String target_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (target_id == null) {
            return null;
        }
        if (account == null) {
            account = new BaseAccount();
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            account.user_id = sharedpreferences.getString(PREF_USER_ID, null);
            account.instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_INTENT, null, Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND "
                    + Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND "
                    + Sqlite.COL_TYPE + " = '" + CacheType.ACCOUNT.getValue() + "' AND "
                    + Sqlite.COL_TARGET_ID + " = '" + target_id + "'", null, null, null, null, "1");
            CachedBundle cachedBundle = cursorToCachedBundle(c);
            if (cachedBundle != null && cachedBundle.bundle.containsKey(Helper.ARG_ACCOUNT)) {
                return (Account) cachedBundle.bundle.getSerializable(Helper.ARG_ACCOUNT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Returns a bundle by targeted status id
     *
     * @param target_id String
     * @return Status {@link Status}
     */
    private Status getCachedStatus(BaseAccount account, String target_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (target_id == null) {
            return null;
        }
        if (account == null) {
            account = new BaseAccount();
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            account.user_id = sharedpreferences.getString(PREF_USER_ID, null);
            account.instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_INTENT, null, Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND "
                    + Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND "
                    + Sqlite.COL_TYPE + " = '" + CacheType.STATUS.getValue() + "' AND "
                    + Sqlite.COL_TARGET_ID + " = '" + target_id + "'", null, null, null, null, "1");
            CachedBundle cachedBundle = cursorToCachedBundle(c);
            if (cachedBundle != null && cachedBundle.bundle.containsKey(Helper.ARG_STATUS)) {
                return (Status) cachedBundle.bundle.getSerializable(Helper.ARG_STATUS);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
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
     */
    public void deleteOldIntent() throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);
        Date date = cal.getTime();
        String dateStr = Helper.dateToString(date);
        try {
            db.delete(Sqlite.TABLE_INTENT, Sqlite.COL_CREATED_AT + " <  ?", new String[]{dateStr});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove a bundle from db
     */
    private void removeIntent(BaseAccount account, String target_id) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (account == null || target_id == null) {
            return;
        }
        db.delete(Sqlite.TABLE_INTENT, Sqlite.COL_USER_ID + " = '" + account.user_id + "' AND "
                + Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND "
                + Sqlite.COL_TARGET_ID + " = '" + target_id + "'", null);
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
     * @return Account
     */
    private CachedBundle convertCursorToCachedBundle(Cursor c) {
        CachedBundle cachedBundle = new CachedBundle();
        cachedBundle.id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        cachedBundle.bundle = deserializeBundle(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_BUNDLE)));
        cachedBundle.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        cachedBundle.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        cachedBundle.target_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TARGET_ID));
        cachedBundle.cacheType = CacheType.valueOf(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_TYPE)));
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
        } catch (IOException e) {
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
        } finally {
            parcel.recycle();
        }
        return bundle;
    }

    public enum CacheType {
        @SerializedName("ARGS")
        ARGS("ARGS"),
        @SerializedName("ACCOUNT")
        ACCOUNT("ACCOUNT"),
        @SerializedName("STATUS")
        STATUS("STATUS");

        private final String value;

        CacheType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public interface BundleCallback {
        void get(Bundle bundle);
    }


    public interface BundleInsertCallback {
        void inserted(long bundleId);
    }

}
