package app.fedilab.android.mastodon.client.entities.app;
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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import app.fedilab.android.mastodon.client.entities.api.MastodonList;

public class PinnedTimeline implements Serializable {

    @SerializedName("id")
    public int id;
    @SerializedName("userId")
    public String userId;
    @SerializedName("instance")
    public String instance;
    @SerializedName("position")
    public int position;
    @SerializedName("displayed")
    public boolean displayed = true;
    @SerializedName("type")
    public Timeline.TimeLineEnum type;
    @SerializedName("remoteInstance")
    public RemoteInstance remoteInstance;
    @SerializedName("tagTimeline")
    public TagTimeline tagTimeline;
    @SerializedName("bubbleTimeline")
    public BubbleTimeline bubbleTimeline;
    @SerializedName("mastodonList")
    public MastodonList mastodonList;
    @SerializedName("currentFilter")
    public String currentFilter;


    public transient boolean isSelected = false;
}
