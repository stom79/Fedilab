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

import static app.fedilab.android.helper.Helper.getCurrentAccount;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.QuickLoad;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.exception.DBException;
import es.dmoral.toasty.Toasty;

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
                count.add(new QuickLoad(context).count(account));
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
                if (!aChildren.equals("databases") && !aChildren.equals("shared_prefs")) {
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


    public interface Callback {
        void getCacheSize(float size);
    }


    public interface CallbackAccount {
        void getcount(List<Integer> countStatuses);
    }

    public static class CacheTask {
        private final WeakReference<Context> contextReference;
        private float cacheSize;

        public CacheTask(Context context) {
            contextReference = new WeakReference<>(context);
            doInBackground();
        }

        protected void doInBackground() {
            new Thread(() -> {
                long sizeCache = cacheSize(contextReference.get().getCacheDir().getParentFile());
                cacheSize = 0;
                if (sizeCache > 0) {
                    cacheSize = (float) sizeCache / 1000000.0f;
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(contextReference.get(), Helper.dialogStyle());
                    LayoutInflater inflater = ((BaseMainActivity) contextReference.get()).getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.popup_cache, new LinearLayout(contextReference.get()), false);
                    TextView message = dialogView.findViewById(R.id.message);
                    message.setText(contextReference.get().getString(R.string.cache_message, String.format("%s %s", String.format(Locale.getDefault(), "%.2f", cacheSize), contextReference.get().getString(R.string.cache_units))));
                    builder.setView(dialogView);
                    builder.setTitle(R.string.cache_title);

                    final SwitchCompat clean_all = dialogView.findViewById(R.id.clean_all);
                    final float finalCacheSize = cacheSize;
                    builder
                            .setPositiveButton(R.string.clear, (dialog, which) -> new Thread(() -> {
                                try {
                                    String path = Objects.requireNonNull(contextReference.get().getCacheDir().getParentFile()).getPath();
                                    File dir = new File(path);
                                    if (dir.isDirectory()) {
                                        deleteDir(dir);
                                    }
                                    if (clean_all.isChecked()) {
                                        new QuickLoad(contextReference.get()).deleteForAllAccount();
                                        new StatusCache(contextReference.get()).deleteForAllAccount();
                                    } else {
                                        new QuickLoad(contextReference.get()).deleteForAccount(getCurrentAccount(contextReference.get()));
                                        new StatusCache(contextReference.get()).deleteForAccount(getCurrentAccount(contextReference.get()));
                                    }
                                    Handler mainHandler2 = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable2 = () -> {
                                        Toasty.success(contextReference.get(), contextReference.get().getString(R.string.toast_cache_clear, String.format("%s %s", String.format(Locale.getDefault(), "%.2f", finalCacheSize), contextReference.get().getString(R.string.cache_units))), Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    };
                                    mainHandler2.post(myRunnable2);
                                } catch (Exception ignored) {
                                }
                            }).start())
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                };
                mainHandler.post(myRunnable);
            }).start();
        }
    }

}
