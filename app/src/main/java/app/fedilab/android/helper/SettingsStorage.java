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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;

import app.fedilab.android.R;


//From https://stackoverflow.com/a/10864463

public class SettingsStorage {


    public static boolean saveSharedPreferencesToFile(Context context) {
        boolean res = false;
        ObjectOutputStream output = null;
        String fileName = "Fedilab_settings_export_" + Helper.dateFileToString(context, new Date()) + ".txt";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String fullPath = filePath + "/" + fileName;
        File dst = new File(fullPath);
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            output.writeObject(sharedpreferences.getAll());
            res = true;
            String message = context.getString(R.string.data_export_settings_success);
            Intent intentOpen = new Intent();
            intentOpen.setAction(android.content.Intent.ACTION_VIEW);
            Uri uri = Uri.parse("file://" + fullPath);
            intentOpen.setDataAndType(uri, "text/txt");
            String title = context.getString(R.string.data_export_settings);
            Helper.notify_user(context, currentAccount, intentOpen, BitmapFactory.decodeResource(context.getResources(),
                    getMainLogo(context)), Helper.NotifType.BACKUP, title, message);
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
        return res;
    }

    @SuppressLint("ApplySharedPref")
    @SuppressWarnings({"unchecked", "UnnecessaryUnboxing"})
    public static boolean loadSharedPreferencesFromFile(Context context, Uri srcUri) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(context.getContentResolver().openInputStream(srcUri));
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor prefEdit = sharedpreferences.edit();
            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();
                //We skip some values
                if (key.compareTo(Helper.PREF_USER_ID) == 0) {
                    continue;
                }
                if (key.compareTo(Helper.PREF_INSTANCE) == 0) {
                    continue;
                }
                if (key.compareTo(Helper.PREF_USER_INSTANCE) == 0) {
                    continue;
                }
                if (key.compareTo(Helper.PREF_USER_TOKEN) == 0) {
                    continue;
                }
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
}
