package app.fedilab.android.peertube.client.data;
/* Copyright 2023 Thomas Schneider
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


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class VideoPlaylistData implements Serializable {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<VideoPlaylist> data;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<VideoPlaylist> getData() {
        return data;
    }

    public void setData(List<VideoPlaylist> data) {
        this.data = data;
    }

    public static class VideoPlaylist implements Serializable {
        @SerializedName("id")
        private String id;
        @SerializedName("position")
        private String position;
        @SerializedName("startTimestamp")
        private long startTimestamp;
        @SerializedName("stopTimestamp")
        private long stopTimestamp;
        @SerializedName("type")
        private int type;
        @SerializedName("video")
        private VideoData.Video video;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public long getStartTimestamp() {
            return startTimestamp;
        }

        public void setStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
        }

        public long getStopTimestamp() {
            return stopTimestamp;
        }

        public void setStopTimestamp(long stopTimestamp) {
            this.stopTimestamp = stopTimestamp;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public VideoData.Video getVideo() {
            return video;
        }

        public void setVideo(VideoData.Video video) {
            this.video = video;
        }
    }

    public static class VideoPlaylistCreation implements Serializable {
        @SerializedName("videoPlaylist")
        private VideoPlaylistCreationItem videoPlaylist;

        public VideoPlaylistCreationItem getVideoPlaylist() {
            return videoPlaylist;
        }

        public void setVideoPlaylist(VideoPlaylistCreationItem videoPlaylist) {
            this.videoPlaylist = videoPlaylist;
        }
    }

    public static class PlaylistElement implements Serializable {
        @SerializedName("videoPlaylistElement")
        private VideoPlaylistCreationItem videoPlaylistElement;

        public VideoPlaylistCreationItem getVideoPlaylistElement() {
            return videoPlaylistElement;
        }

        public void setVideoPlaylistElement(VideoPlaylistCreationItem videoPlaylistElement) {
            this.videoPlaylistElement = videoPlaylistElement;
        }
    }

    public static class VideoPlaylistCreationItem implements Serializable {
        @SerializedName("id")
        String id;
        @SerializedName("uuid")
        String uuid;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class VideoPlaylistExport implements Serializable {

        private long playlistDBkey;
        private String acct;
        private String uuid;
        private PlaylistData.Playlist playlist;
        private List<VideoPlaylist> videos;


        public VideoPlaylistExport() {
        }


        public String getAcct() {
            return acct;
        }

        public void setAcct(String acct) {
            this.acct = acct;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public PlaylistData.Playlist getPlaylist() {
            return playlist;
        }

        public void setPlaylist(PlaylistData.Playlist playlist) {
            this.playlist = playlist;
        }

        public long getPlaylistDBkey() {
            return playlistDBkey;
        }

        public void setPlaylistDBkey(long playlistDBkey) {
            this.playlistDBkey = playlistDBkey;
        }

        public List<VideoPlaylist> getVideos() {
            return videos;
        }

        public void setVideos(List<VideoPlaylist> videos) {
            this.videos = videos;
        }

    }
}
