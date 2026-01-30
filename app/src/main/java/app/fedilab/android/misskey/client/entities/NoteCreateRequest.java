package app.fedilab.android.misskey.client.entities;
/* Copyright 2026 Thomas Schneider
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

public class NoteCreateRequest implements Serializable {

    @SerializedName("i")
    public String token;

    @SerializedName("text")
    public String text;

    @SerializedName("visibility")
    public String visibility;

    @SerializedName("cw")
    public String cw;

    @SerializedName("localOnly")
    public Boolean localOnly;

    @SerializedName("fileIds")
    public List<String> fileIds;

    @SerializedName("replyId")
    public String replyId;

    @SerializedName("renoteId")
    public String renoteId;

    @SerializedName("poll")
    public PollRequest poll;

    @SerializedName("scheduledAt")
    public Long scheduledAt;

    @SerializedName("visibleUserIds")
    public List<String> visibleUserIds;

    public NoteCreateRequest(String token) {
        this.token = token;
    }

    public static class PollRequest implements Serializable {
        @SerializedName("choices")
        public List<String> choices;
        @SerializedName("multiple")
        public Boolean multiple;
        @SerializedName("expiresAt")
        public Long expiresAt;
        @SerializedName("expiredAfter")
        public Long expiredAfter;
    }

    public static String mapVisibility(String mastodonVisibility) {
        if (mastodonVisibility == null) {
            return "public";
        }
        switch (mastodonVisibility) {
            case "public":
                return "public";
            case "unlisted":
                return "home";
            case "private":
                return "followers";
            case "direct":
                return "specified";
            default:
                return "public";
        }
    }
}
