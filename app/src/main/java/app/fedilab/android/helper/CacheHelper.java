package app.fedilab.android.helper;
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.CacheAccount;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.exception.DBException;

public class CacheHelper {

    public static void getCacheValues(Context context, Callback callback) {
        new Thread(() -> {
            long sizeCache = cacheSize(context.getCacheDir().getParentFile());
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> callback.getCacheSize(sizeCache);
            mainHandler.post(myRunnable);
        }).start();
    }

    public static void getTimelineValues(Context context, BaseAccount account, CallbackAccount callbackAccount) {
        new Thread(() -> {
            List<Integer> count = new ArrayList<>();
            try {
                count.add(new StatusCache(context).count(account));
            } catch (DBException e) {
                e.printStackTrace();
            }
            try {
                count.add(new StatusDraft(context).count(account));
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> callbackAccount.getcount(count);
            mainHandler.post(myRunnable);
        }).start();
    }

    /**
     * Retrieves the cache size
     *
     * @param directory File
     * @return long value in Mo
     */
    public static long cacheSize(File directory) {
        long length = 0;
        if (directory == null || directory.length() == 0)
            return -1;
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile()) {
                try {
                    length += file.length();
                } catch (NullPointerException e) {
                    return -1;
                }
            } else {
                if (!file.getName().equals("databases") && !file.getName().equals("shared_prefs")) {
                    length += cacheSize(file);
                }
            }
        }
        return length;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String aChildren : children) {
                if (!aChildren.equals("databases") && !aChildren.equals("shared_prefs") && !aChildren.equals(Helper.TEMP_MEDIA_DIRECTORY)) {
                    boolean success = deleteDir(new File(dir, aChildren));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else {
            return dir != null && dir.isFile() && dir.delete();
        }
    }

    public static void clearCache(Context context, boolean clearFiles, List<CacheAccount> cacheAccounts, CallbackClear callbackClear) {
        new Thread(() -> {
            if (clearFiles) {
                String path = context.getCacheDir().getParentFile().getPath();
                File dir = new File(path);
                if (dir.isDirectory()) {
                    deleteDir(dir);
                }
            }
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            for (CacheAccount cacheAccount : cacheAccounts) {
                if (cacheAccount.clear_home) {
                    try {
                        new StatusCache(context).deleteHomeForAccount(cacheAccount.account);
                        editor.putString(context.getString(R.string.SET_INNER_MARKER) + cacheAccount.account.user_id + cacheAccount.account.instance + Timeline.TimeLineEnum.HOME.getValue(), null);
                        editor.apply();
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
                if (cacheAccount.clear_other) {
                    try {
                        new StatusCache(context).deleteOthersForAccount(cacheAccount.account);
                        Map<String, ?> keys = sharedpreferences.getAll();

                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                            if (entry.getKey().startsWith(context.getString(R.string.SET_INNER_MARKER) + cacheAccount.account.user_id + cacheAccount.account.instance) && !entry.getKey().endsWith(Timeline.TimeLineEnum.HOME.getValue())) {
                                editor.putString(entry.getKey(), null);
                                editor.apply();
                            }
                            //Delete last notification ref
                            if (entry.getKey().startsWith(context.getString(R.string.LAST_NOTIFICATION_ID) + cacheAccount.account.user_id + cacheAccount.account.instance) && !entry.getKey().endsWith(Timeline.TimeLineEnum.HOME.getValue())) {
                                editor.putString(entry.getKey(), null);
                                editor.apply();
                            }
                        }
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
                if (cacheAccount.clear_drafts) {
                    try {
                        new StatusDraft(context).removeAllDraftFor(cacheAccount.account);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = callbackClear::onCleared;
            mainHandler.post(myRunnable);
        }).start();
    }


    public interface Callback {
        void getCacheSize(float size);
    }

    public interface CallbackAccount {
        void getcount(List<Integer> countStatuses);
    }

    public interface CallbackClear {
        void onCleared();
    }

}
