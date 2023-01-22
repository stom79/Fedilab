package app.fedilab.android.peertube.helper;
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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.OpenableColumns;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

import app.fedilab.android.peertube.activities.AllLocalPlaylistsActivity;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.sqlite.ManagePlaylistsDAO;
import app.fedilab.android.peertube.sqlite.Sqlite;

public class PlaylistExportHelper {


    /**
     * Unserialized  VideoPlaylistExport
     *
     * @param serializedVideoPlaylistExport String serialized VideoPlaylistExport
     * @return VideoPlaylistExport
     */
    public static VideoPlaylistData.VideoPlaylistExport restorePlaylistFromString(String serializedVideoPlaylistExport) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedVideoPlaylistExport, VideoPlaylistData.VideoPlaylistExport.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized VideoPlaylistExport class
     *
     * @param videoPlaylistExport Playlist to serialize
     * @return String serialized VideoPlaylistData.VideoPlaylistExport
     */
    public static String playlistToStringStorage(VideoPlaylistData.VideoPlaylistExport videoPlaylistExport) {
        Gson gson = new Gson();
        try {
            return gson.toJson(videoPlaylistExport);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Manage intent for opening a tubelab file allowing to import a whole playlist and store it in db
     *
     * @param activity Activity
     * @param intent   Intent
     */
    public static void manageIntentUrl(Activity activity, Intent intent) {
        if (intent.getData() != null) {
            String url = intent.getData().toString();

            String filename = url;

            if (url.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = activity.getContentResolver().query(intent.getData(), null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    assert cursor != null;
                    cursor.close();
                }
            }
            String text = null;
            if (filename.endsWith(".tubelab")) {
                try {
                    InputStream inputStream = activity.getContentResolver().openInputStream(intent.getData());
                    Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                    text = s.hasNext() ? s.next() : "";
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (text != null && text.length() > 20) {
                    String finalText = text;
                    new Thread(() -> {
                        VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = PlaylistExportHelper.restorePlaylistFromString(finalText);
                        if (videoPlaylistExport != null) {
                            SQLiteDatabase db = Sqlite.getInstance(activity.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                            new ManagePlaylistsDAO(activity, db).insertPlaylist(videoPlaylistExport);
                        }
                        activity.runOnUiThread(() -> {
                            Intent intentPlaylist = new Intent(activity, AllLocalPlaylistsActivity.class);
                            activity.startActivity(intentPlaylist);
                        });
                    }).start();
                }
            }
        }
    }
}
