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


import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.helper.LogoHelper.getMainLogo;
import static app.fedilab.android.sqlite.Sqlite.DB_NAME;
import static app.fedilab.android.sqlite.Sqlite.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import app.fedilab.android.R;
import es.dmoral.toasty.Toasty;


public class ZipHelper {

    final static int BUFFER_SIZE = 2048;

    public static void exportData(Context context) throws IOException {
        String suffix = Helper.dateFileToString(context, new Date());
        String fileName = "Fedilab_data_export_" + suffix + ".zip";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String zipFile = filePath + "/" + fileName;
        BufferedInputStream origin;
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            byte[] data = new byte[BUFFER_SIZE];
            String settingsPath = storeSettings(context, suffix);
            if (settingsPath != null) {
                FileInputStream fi = new FileInputStream(settingsPath);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(settingsPath.substring(settingsPath.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
                //noinspection ResultOfMethodCallIgnored
                new File(settingsPath).delete();
            } else {
                Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                return;
            }
            String dbPath = exportDB(context, suffix);
            if (dbPath != null) {
                FileInputStream fi = new FileInputStream(dbPath);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(dbPath.substring(dbPath.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
                //noinspection ResultOfMethodCallIgnored
                new File(dbPath).delete();
            } else {
                Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                return;
            }
            String message = context.getString(R.string.data_export_settings_success);
            Intent intentOpen = new Intent();
            intentOpen.setAction(android.content.Intent.ACTION_VIEW);
            Uri uri = Uri.parse("file://" + zipFile);
            intentOpen.setDataAndType(uri, "application/zip");
            String title = context.getString(R.string.data_export_settings);
            Helper.notify_user(context, currentAccount, intentOpen, BitmapFactory.decodeResource(context.getResources(),
                    getMainLogo(context)), Helper.NotifType.BACKUP, title, message);
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    public static void importData(Context context, File file) {
        new Thread(() -> {
            try {
                int size;
                byte[] buffer = new byte[BUFFER_SIZE];

                String uriFullPath = file.getAbsolutePath();
                String[] uriFullPathStr = uriFullPath.split(":");
                String fullPath = uriFullPath;
                if (uriFullPathStr.length > 1) {
                    fullPath = uriFullPathStr[1];
                }
                fullPath = fullPath.replace(".zip", "");
                File f = new File(fullPath);
                if (!f.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.mkdirs();
                }
                boolean successful = true;
                try (ZipInputStream zin = new ZipInputStream(new FileInputStream(fullPath + ".zip"))) {
                    ZipEntry ze;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (!successful) {
                            break;
                        }
                        String path = fullPath + ze.getName();
                        File unzipFile = new File(path);
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ((size = zin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                fout.write(buffer, 0, size);
                            }

                            zin.closeEntry();
                        } finally {
                            fout.flush();
                            fout.close();
                        }
                        if (ze.getName().contains("settings")) {
                            successful = restoreSettings(context, Uri.fromFile(new File(path)));
                        } else if (ze.getName().contains("database")) {
                            successful = importDB(context, path);
                        } else {
                            break;
                        }
                    }
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                boolean finalSuccessful = successful;
                Runnable myRunnable = () -> {
                    if (finalSuccessful) {
                        Helper.restart(context);
                    } else {
                        Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                    }
                };
                mainHandler.post(myRunnable);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static String storeSettings(Context context, String suffix) {
        boolean res = false;
        ObjectOutputStream output = null;
        String fileName = "Fedilab_settings_export_" + suffix + ".fedilab";
        String filePath = context.getCacheDir().getAbsolutePath();
        String fullPath = filePath + "/" + fileName;
        File dst = new File(fullPath);
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            output.writeObject(sharedpreferences.getAll());
            res = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res ? fullPath : null;
    }

    @SuppressLint("ApplySharedPref")
    @SuppressWarnings("UnnecessaryUnboxing")
    private static boolean restoreSettings(Context context, Uri srcUri) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(context.getContentResolver().openInputStream(srcUri));
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor prefEdit = sharedpreferences.edit();
            prefEdit.clear();
            //noinspection unchecked
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();
                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
            }

            prefEdit.commit();
            res = true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }


    private static String exportDB(Context context, String suffix) {
        try {
            String fileName = "Fedilab_database_export_" + suffix + ".fedilab";
            String filePath = context.getCacheDir().getAbsolutePath();
            String fullPath = filePath + "/" + fileName;
            File dbSource = context.getDatabasePath(DB_NAME);
            File dbDest = new File(fullPath);
            FileChannel src = new FileInputStream(dbSource).getChannel();
            FileChannel dst = new FileOutputStream(dbDest).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            return fullPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean importDB(Context context, String backupDBPath) {
        try {
            if (db != null) {
                db.close();
            }
            File dbDest = context.getDatabasePath(DB_NAME);
            File dbSource = new File(backupDBPath);
            FileChannel src = new FileInputStream(dbSource).getChannel();
            FileChannel dst = new FileOutputStream(dbDest).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
