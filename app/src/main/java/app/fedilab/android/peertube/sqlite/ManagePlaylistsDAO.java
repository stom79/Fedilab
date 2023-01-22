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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;


@SuppressWarnings("UnusedReturnValue")
public class ManagePlaylistsDAO {

    private final SQLiteDatabase db;
    public Context context;


    public ManagePlaylistsDAO(Context context, SQLiteDatabase db) {
        //Creation of the DB with tables
        this.context = context;
        this.db = db;
    }

    /**
     * Unserialized  Video
     *
     * @param serializedVideo String serialized Video
     * @return Video
     */
    public static VideoData.Video restoreVideoFromString(String serializedVideo) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedVideo, VideoData.Video.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized Video class
     *
     * @param video Video to serialize
     * @return String serialized video
     */
    public static String videoToStringStorage(VideoData.Video video) {
        Gson gson = new Gson();
        try {
            return gson.toJson(video);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Unserialized  Playlist
     *
     * @param serializedPlaylist String serialized Playlist
     * @return Playlist
     */
    public static PlaylistData.Playlist restorePlaylistFromString(String serializedPlaylist) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedPlaylist, PlaylistData.Playlist.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Serialized Playlist class
     *
     * @param playlist Playlist to serialize
     * @return String serialized playlist
     */
    public static String playlistToStringStorage(PlaylistData.Playlist playlist) {
        Gson gson = new Gson();
        try {
            return gson.toJson(playlist);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert playlist info in database
     *
     * @param videoPlaylistExport VideoPlaylistExport
     * @return boolean
     */
    public boolean insertPlaylist(VideoPlaylistData.VideoPlaylistExport videoPlaylistExport) {

        if (videoPlaylistExport.getPlaylist() == null) {
            return true;
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_ACCT, videoPlaylistExport.getAcct());
        values.put(Sqlite.COL_UUID, videoPlaylistExport.getUuid());
        values.put(Sqlite.COL_PLAYLIST, playlistToStringStorage(videoPlaylistExport.getPlaylist()));
        //Inserts playlist
        try {
            long id = checkExists(videoPlaylistExport.getPlaylist().getUuid());
            if (id != -1) {
                videoPlaylistExport.setPlaylistDBkey(id);
                removeAllVideosInPlaylist(id);

            } else {
                long playlist_id = db.insertOrThrow(Sqlite.TABLE_LOCAL_PLAYLISTS, null, values);
                videoPlaylistExport.setPlaylistDBkey(playlist_id);
            }
            for (VideoPlaylistData.VideoPlaylist videoPlaylist : videoPlaylistExport.getVideos()) {
                //Insert videos
                insertVideos(videoPlaylist.getVideo(), videoPlaylistExport);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Insert videos for playlists in database
     *
     * @param video    Video to insert
     * @param playlist VideoPlaylistExport targeted
     * @return boolean
     */
    private boolean insertVideos(VideoData.Video video, VideoPlaylistData.VideoPlaylistExport playlist) {

        if (video == null || playlist == null) {
            return true;
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_UUID, video.getUuid());
        values.put(Sqlite.COL_PLAYLIST_ID, playlist.getPlaylistDBkey());
        values.put(Sqlite.COL_VIDEO_DATA, videoToStringStorage(video));
        //Inserts playlist
        try {
            db.insertOrThrow(Sqlite.TABLE_VIDEOS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Check if playlist exists
     *
     * @param uuid String
     * @return int
     */
    private long checkExists(String uuid) {
        try {
            Cursor c = db.query(Sqlite.TABLE_LOCAL_PLAYLISTS, null, Sqlite.COL_UUID + " = \"" + uuid + "\"", null, null, null, null, "1");
            VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = cursorToSingleVideoPlaylistExport(c);
            c.close();
            return videoPlaylistExport != null ? videoPlaylistExport.getPlaylistDBkey() : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * Check if playlist exists
     *
     * @param videoUuid    String
     * @param playlistUuid String
     * @return int
     */
    private boolean checkVideoExists(String videoUuid, String playlistUuid) {
        try {
            String check_query = "SELECT * FROM " + Sqlite.TABLE_LOCAL_PLAYLISTS + " p INNER JOIN "
                    + Sqlite.TABLE_VIDEOS + " v ON p.id = v." + Sqlite.COL_PLAYLIST_ID
                    + " WHERE p." + Sqlite.COL_UUID + "=? AND v." + Sqlite.COL_UUID + "=? LIMIT 1";
            Cursor c = db.rawQuery(check_query, new String[]{playlistUuid, videoUuid});
            int count = c.getCount();
            c.close();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Remove all videos in playlist
     *
     * @param playlistDBid long db id
     * @return int
     */
    public int removeAllVideosInPlaylist(long playlistDBid) {
        return db.delete(Sqlite.TABLE_VIDEOS, Sqlite.COL_PLAYLIST_ID + " = '" + playlistDBid + "'", null);
    }


    /**
     * Remove a playlist with its uuid
     *
     * @param uuid String uuid of the Playlist
     * @return int
     */
    public int removePlaylist(String uuid) {
        VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = getSinglePlaylists(uuid);
        db.delete(Sqlite.TABLE_VIDEOS, Sqlite.COL_PLAYLIST_ID + " = '" + videoPlaylistExport.getPlaylistDBkey() + "'", null);
        return db.delete(Sqlite.TABLE_LOCAL_PLAYLISTS, Sqlite.COL_ID + " = '" + videoPlaylistExport.getPlaylistDBkey() + "'", null);
    }


    /**
     * Returns a playlist from it's uid in db
     *
     * @return VideoPlaylistExport
     */
    public VideoPlaylistData.VideoPlaylistExport getSinglePlaylists(String uuid) {

        try {
            Cursor c = db.query(Sqlite.TABLE_LOCAL_PLAYLISTS, null, Sqlite.COL_UUID + "='" + uuid + "'", null, null, null, Sqlite.COL_ID + " DESC", null);
            return cursorToSingleVideoPlaylistExport(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Returns all playlists in db
     *
     * @return List<VideoPlaylistData.VideoPlaylistExport>
     */
    public List<VideoPlaylistData.VideoPlaylistExport> getAllPlaylists() {

        try {
            Cursor c = db.query(Sqlite.TABLE_LOCAL_PLAYLISTS, null, null, null, null, null, Sqlite.COL_ID + " DESC", null);
            return cursorToVideoPlaylistExport(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Returns all videos in a playlist
     *
     * @return List<VideoData.VideoExport>
     */
    public List<VideoData.VideoExport> getAllVideosInPlaylist(VideoPlaylistData.VideoPlaylistExport videoPlaylistExport) {

        try {
            Cursor c = db.query(Sqlite.TABLE_VIDEOS, null, Sqlite.COL_PLAYLIST_ID + "='" + videoPlaylistExport.getPlaylistDBkey() + "'", null, null, null, Sqlite.COL_ID + " DESC", null);
            return cursorToVideoExport(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all videos in a playlist
     *
     * @return List<VideoData.VideoExport>
     */
    public List<VideoData.VideoExport> getAllVideosInPlaylist(String uuid) {
        try {
            VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = getSinglePlaylists(uuid);
            Cursor c = db.query(Sqlite.TABLE_VIDEOS, null, Sqlite.COL_PLAYLIST_ID + "='" + videoPlaylistExport.getPlaylistDBkey() + "'", null, null, null, Sqlite.COL_ID + " DESC", null);
            return cursorToVideoExport(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /***
     * Method to hydrate  VideoPlaylistExport from database
     * @param c Cursor
     * @return VideoPlaylistData.VideoPlaylistExport
     */
    private VideoPlaylistData.VideoPlaylistExport cursorToSingleVideoPlaylistExport(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        c.moveToFirst();
        VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = new VideoPlaylistData.VideoPlaylistExport();
        videoPlaylistExport.setAcct(c.getString(c.getColumnIndex(Sqlite.COL_ACCT)));
        videoPlaylistExport.setUuid(c.getString(c.getColumnIndex(Sqlite.COL_UUID)));
        videoPlaylistExport.setPlaylistDBkey(c.getInt(c.getColumnIndex(Sqlite.COL_ID)));
        videoPlaylistExport.setPlaylist(restorePlaylistFromString(c.getString(c.getColumnIndex(Sqlite.COL_PLAYLIST))));
        //Close the cursor
        c.close();
        return videoPlaylistExport;
    }


    /***
     * Method to hydrate  VideoPlaylistExport from database
     * @param c Cursor
     * @return List<VideoPlaylistData.VideoPlaylistExport>
     */
    private List<VideoPlaylistData.VideoPlaylistExport> cursorToVideoPlaylistExport(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<VideoPlaylistData.VideoPlaylistExport> videoPlaylistExports = new ArrayList<>();
        while (c.moveToNext()) {
            VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = new VideoPlaylistData.VideoPlaylistExport();
            videoPlaylistExport.setAcct(c.getString(c.getColumnIndex(Sqlite.COL_ACCT)));
            videoPlaylistExport.setUuid(c.getString(c.getColumnIndex(Sqlite.COL_UUID)));
            videoPlaylistExport.setPlaylistDBkey(c.getInt(c.getColumnIndex(Sqlite.COL_ID)));
            videoPlaylistExport.setPlaylist(restorePlaylistFromString(c.getString(c.getColumnIndex(Sqlite.COL_PLAYLIST))));
            videoPlaylistExports.add(videoPlaylistExport);
        }
        //Close the cursor
        c.close();
        return videoPlaylistExports;
    }


    /***
     * Method to hydrate  Video from database
     * @param c Cursor
     * @return List<VideoData.Video>
     */
    private List<VideoData.VideoExport> cursorToVideoExport(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        List<VideoData.VideoExport> videoExports = new ArrayList<>();
        while (c.moveToNext()) {
            VideoData.VideoExport videoExport = new VideoData.VideoExport();
            videoExport.setPlaylistDBid(c.getInt(c.getColumnIndex(Sqlite.COL_PLAYLIST_ID)));
            videoExport.setUuid(c.getString(c.getColumnIndex(Sqlite.COL_UUID)));
            videoExport.setId(c.getInt(c.getColumnIndex(Sqlite.COL_ID)));
            videoExport.setVideoData(restoreVideoFromString(c.getString(c.getColumnIndex(Sqlite.COL_VIDEO_DATA))));
            videoExports.add(videoExport);
        }
        //Close the cursor
        c.close();
        return videoExports;
    }

}
