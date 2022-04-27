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
import java.util.Date;
import java.util.List;

public class ScheduledStatus implements Serializable {
    @SerializedName("id")
    public String id;
    @SerializedName("scheduled_at")
    public Date scheduled_at;
    @SerializedName("params")
    public Params params;
    @SerializedName("media_attachments")
    public List<Attachment> media_attachments;


    public static class Params implements Serializable {
        @SerializedName("text")
        public String text;
        @SerializedName("media_ids")
        public List<String> media_ids;
        @SerializedName("sensitive")
        public boolean sensitive;
        @SerializedName("spoiler_text")
        public String spoiler_text;
        @SerializedName("visibility")
        public String visibility;
        @SerializedName("scheduled_at")
        public Date scheduled_at;
        @SerializedName("poll")
        public Poll poll;
        @SerializedName("idempotency")
        public String idempotency;
        @SerializedName("in_reply_to_id")
        public String in_reply_to_id;
        @SerializedName("application_id")
        public String application_id;
    }
}
