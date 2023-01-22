package app.fedilab.android.mastodon.client.entities.api;
/* Copyright 2021 Thomas Schneider
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

import java.util.List;

public class RelationShip {

    @SerializedName("id")
    public String id;
    @SerializedName("following")
    public boolean following;
    @SerializedName("showing_reblogs")
    public boolean showing_reblogs;
    @SerializedName("notifying")
    public boolean notifying;
    @SerializedName("followed_by")
    public boolean followed_by;
    @SerializedName("blocking")
    public boolean blocking;
    @SerializedName("blocked_by")
    public boolean blocked_by;
    @SerializedName("muting")
    public boolean muting;
    @SerializedName("muting_notifications")
    public boolean muting_notifications;
    @SerializedName("requested")
    public boolean requested;
    @SerializedName("domain_blocking")
    public boolean domain_blocking;
    @SerializedName("languages")
    public List<String> languages;
    @SerializedName("endorsed")
    public boolean endorsed;
    @SerializedName("note")
    public String note;
}
