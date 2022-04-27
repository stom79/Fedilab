package app.fedilab.android.client.mastodon.entities;
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

import java.io.Serializable;
import java.util.List;

public class Source implements Serializable {
    @SerializedName("privacy")
    public String privacy;
    @SerializedName("sensitive")
    public boolean sensitive;
    @SerializedName("language")
    public String language;
    @SerializedName("note")
    public String note;
    @SerializedName("fields")
    public List<Field> fields;
    @SerializedName("follow_requests_count")
    public int follow_requests_count;

    public static class SourceParams implements Serializable {
        @SerializedName("privacy")
        public String privacy;
        @SerializedName("sensitive")
        public boolean sensitive;
        @SerializedName("language")
        public String language;
    }
}
