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
import java.util.List;

public class RemoteInstance implements Serializable {

    @SerializedName("dbID")
    public long dbID;
    @SerializedName("id")
    public String id;
    @SerializedName("host")
    public String host;
    @SerializedName("displayName")
    public String displayName;
    @SerializedName("type")
    public InstanceType type;
    @SerializedName("tags")
    public List<String> tags;
    @SerializedName("filteredWith")
    public String filteredWith;


    public enum InstanceType {
        @SerializedName("MASTODON")
        MASTODON("MASTODON"),
        @SerializedName("MASTODON_TRENDING")
        MASTODON_TRENDING("MASTODON_TRENDING"),
        @SerializedName("PIXELFED")
        PIXELFED("PIXELFED"),
        @SerializedName("PEERTUBE")
        PEERTUBE("PEERTUBE"),
        @SerializedName("NITTER")
        NITTER("NITTER"),
        @SerializedName("NITTER_TAG")
        NITTER_TAG("NITTER_TAG"),
        @SerializedName("MISSKEY")
        MISSKEY("MISSKEY"),
        @SerializedName("LEMMY")
        LEMMY("LEMMY"),
        @SerializedName("GNU")
        GNU("GNU");

        private final String value;

        InstanceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
