package app.fedilab.android.peertube.client.entities;
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
public class StreamingPlaylists implements Serializable {

    @SerializedName("id")
    private String id;
    @SerializedName("type")
    private int type;
    @SerializedName("playlistUrl")
    private String playlistUrl;
    @SerializedName("segmentsSha256Url")
    private String segmentsSha256Url;
    @SerializedName("files")
    private List<File> files;
    @SerializedName("redundancies")
    private List<Redundancies> redundancies;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPlaylistUrl() {
        return playlistUrl;
    }

    public void setPlaylistUrl(String playlistUrl) {
        this.playlistUrl = playlistUrl;
    }

    public String getSegmentsSha256Url() {
        return segmentsSha256Url;
    }

    public void setSegmentsSha256Url(String segmentsSha256Url) {
        this.segmentsSha256Url = segmentsSha256Url;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public List<Redundancies> getRedundancies() {
        return redundancies;
    }

    public void setRedundancies(List<Redundancies> redundancies) {
        this.redundancies = redundancies;
    }


    public static class Redundancies implements Serializable {
        @SerializedName("baseUrl")
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
