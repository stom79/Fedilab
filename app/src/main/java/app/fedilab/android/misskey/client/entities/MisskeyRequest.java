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

public class MisskeyRequest implements Serializable {

    @SerializedName("i")
    public String token;

    @SerializedName("limit")
    public Integer limit;

    @SerializedName("sinceId")
    public String sinceId;

    @SerializedName("untilId")
    public String untilId;

    public MisskeyRequest() {
    }

    public MisskeyRequest(String token) {
        this.token = token;
    }

    public static class UserIdRequest extends MisskeyRequest {
        @SerializedName("userId")
        public String userId;

        public UserIdRequest(String token, String userId) {
            super(token);
            this.userId = userId;
        }
    }

    public static class UsernameRequest extends MisskeyRequest {
        @SerializedName("username")
        public String username;
        @SerializedName("host")
        public String host;

        public UsernameRequest(String token, String username, String host) {
            super(token);
            this.username = username;
            this.host = host;
        }
    }

    public static class NoteIdRequest extends MisskeyRequest {
        @SerializedName("noteId")
        public String noteId;

        public NoteIdRequest(String token, String noteId) {
            super(token);
            this.noteId = noteId;
        }
    }

    public static class ReactionRequest extends MisskeyRequest {
        @SerializedName("noteId")
        public String noteId;
        @SerializedName("reaction")
        public String reaction;

        public ReactionRequest(String token, String noteId, String reaction) {
            super(token);
            this.noteId = noteId;
            this.reaction = reaction;
        }
    }

    public static class TimelineRequest extends MisskeyRequest {
        @SerializedName("withFiles")
        public Boolean withFiles;
        @SerializedName("withRenotes")
        public Boolean withRenotes;
        @SerializedName("withReplies")
        public Boolean withReplies;

        public TimelineRequest(String token) {
            super(token);
        }
    }

    public static class UserNotesRequest extends MisskeyRequest {
        @SerializedName("userId")
        public String userId;
        @SerializedName("withFiles")
        public Boolean withFiles;
        @SerializedName("withReplies")
        public Boolean withReplies;
        @SerializedName("withRenotes")
        public Boolean withRenotes;
        @SerializedName("includeMyRenotes")
        public Boolean includeMyRenotes;

        public UserNotesRequest(String token, String userId) {
            super(token);
            this.userId = userId;
        }
    }

    public static class SearchRequest extends MisskeyRequest {
        @SerializedName("query")
        public String query;
        @SerializedName("origin")
        public String origin;

        public SearchRequest(String token, String query) {
            super(token);
            this.query = query;
        }
    }

    public static class HashtagRequest extends MisskeyRequest {
        @SerializedName("tag")
        public String tag;

        public HashtagRequest(String token, String tag) {
            super(token);
            this.tag = tag;
        }
    }

    public static class UpdateProfileRequest extends MisskeyRequest {
        @SerializedName("name")
        public String name;
        @SerializedName("description")
        public String description;
        @SerializedName("isLocked")
        public Boolean isLocked;
        @SerializedName("isBot")
        public Boolean isBot;
        @SerializedName("isCat")
        public Boolean isCat;
        @SerializedName("avatarId")
        public String avatarId;
        @SerializedName("bannerId")
        public String bannerId;

        public UpdateProfileRequest(String token) {
            super(token);
        }
    }

    public static class PollVoteRequest extends MisskeyRequest {
        @SerializedName("noteId")
        public String noteId;
        @SerializedName("choice")
        public int choice;

        public PollVoteRequest(String token, String noteId, int choice) {
            super(token);
            this.noteId = noteId;
            this.choice = choice;
        }
    }

    public static class ReportRequest extends MisskeyRequest {
        @SerializedName("userId")
        public String userId;
        @SerializedName("comment")
        public String comment;

        public ReportRequest(String token, String userId) {
            super(token);
            this.userId = userId;
        }
    }

    public static class ListRequest extends MisskeyRequest {
        @SerializedName("listId")
        public String listId;

        public ListRequest(String token, String listId) {
            super(token);
            this.listId = listId;
        }
    }

    public static class CreateListRequest extends MisskeyRequest {
        @SerializedName("name")
        public String name;

        public CreateListRequest(String token, String name) {
            super(token);
            this.name = name;
        }
    }

    public static class UpdateListRequest extends MisskeyRequest {
        @SerializedName("listId")
        public String listId;
        @SerializedName("name")
        public String name;

        public UpdateListRequest(String token, String listId, String name) {
            super(token);
            this.listId = listId;
            this.name = name;
        }
    }

    public static class ListUserRequest extends MisskeyRequest {
        @SerializedName("listId")
        public String listId;
        @SerializedName("userId")
        public String userId;

        public ListUserRequest(String token, String listId, String userId) {
            super(token);
            this.listId = listId;
            this.userId = userId;
        }
    }

    public static class ListTimelineRequest extends MisskeyRequest {
        @SerializedName("listId")
        public String listId;
        @SerializedName("withFiles")
        public Boolean withFiles;
        @SerializedName("withRenotes")
        public Boolean withRenotes;

        public ListTimelineRequest(String token, String listId) {
            super(token);
            this.listId = listId;
        }
    }

    public static class MetaRequest implements Serializable {
        @SerializedName("detail")
        public boolean detail = true;
    }

    public static class ApShowRequest extends MisskeyRequest {
        @SerializedName("uri")
        public String uri;

        public ApShowRequest(String token, String uri) {
            super(token);
            this.uri = uri;
        }
    }

    public static class MuteRequest extends MisskeyRequest {
        @SerializedName("userId")
        public String userId;
        @SerializedName("expiresAt")
        public Long expiresAt;

        public MuteRequest(String token, String userId) {
            super(token);
            this.userId = userId;
        }

        public MuteRequest(String token, String userId, Long expiresAt) {
            super(token);
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }

    public static class ScheduledListRequest extends MisskeyRequest {
        @SerializedName("offset")
        public Integer offset;

        public ScheduledListRequest(String token) {
            super(token);
        }
    }

    public static class ScheduledCancelRequest extends MisskeyRequest {
        @SerializedName("draftId")
        public String draftId;

        public ScheduledCancelRequest(String token, String draftId) {
            super(token);
            this.draftId = draftId;
        }
    }

    public static class NotificationsRequest extends MisskeyRequest {
        @SerializedName("includeTypes")
        public String[] includeTypes;
        @SerializedName("excludeTypes")
        public String[] excludeTypes;
        @SerializedName("markAsRead")
        public Boolean markAsRead;

        public NotificationsRequest(String token) {
            super(token);
        }
    }
}
