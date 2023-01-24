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


@SuppressWarnings({"unused", "RedundantSuppression"})
public class PlaylistExist {

    @SerializedName("playlistElementId")
    private String playlistElementId;
    @SerializedName("playlistId")
    private String playlistId;
    @SerializedName("startTimestamp")
    private long startTimestamp;
    @SerializedName("stopTimestamp")
    private long stopTimestamp;

    public String getPlaylistElementId() {
        return playlistElementId;
    }

    public void setPlaylistElementId(String playlistElementId) {
        this.playlistElementId = playlistElementId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
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
}
