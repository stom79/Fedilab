package app.fedilab.android.mastodon.client.entities.api.params;
/* Copyright 2025 Thomas Schneider
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

public class StatusParams implements Serializable {
    @SerializedName("id")
    public String id;
    @SerializedName("status")
    public String status;
    @SerializedName("media_ids")
    public List<String> media_ids;
    @SerializedName("poll")
    public PollParams pollParams;
    @SerializedName("in_reply_to_id")
    public String in_reply_to_id;
    @SerializedName("sensitive")
    public Boolean sensitive;

    @SerializedName("spoiler_text")
    public String spoiler_text;
    @SerializedName("visibility")
    public String visibility;
    @SerializedName("quote_approval_policy")
    public String quote_approval_policy;
    @SerializedName("quoted_status_id")
    public String quoted_status_id;
    @SerializedName("language")
    public String language;
    @SerializedName("media_attributes")
    public List<MediaParams> media_attributes;

    public static class PollParams implements Serializable{
        @SerializedName("options")
        public List<String> poll_options;
        @SerializedName("expires_in")
        public Integer poll_expire_in;
        @SerializedName("multiple")
        public Boolean poll_multiple;
        @SerializedName("hide_totals")
        public Boolean poll_hide_totals;

    }

    public static class MediaParams implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("description")
        public String description;
        @SerializedName("focus")
        public String focus;
    }
}
