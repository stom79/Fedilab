package app.fedilab.android.client.entities.misskey;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Status;


@SuppressWarnings("ALL")
public class MisskeyNote implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("createdAt")
    public Date createdAt;
    @SerializedName("replyId")
    public String replyId;
    @SerializedName("cw")
    public String cw;
    @SerializedName("text")
    public String text;
    @SerializedName("url")
    public String url;
    @SerializedName("uri")
    public String uri;
    @SerializedName("visibility")
    public String visibility;
    @SerializedName("repliesCount")
    public int repliesCount;
    @SerializedName("user")
    public MisskeyUser user;
    @SerializedName("files")
    public List<MisskeyFile> files;
    @SerializedName("emojis")
    public List<MisskeyEmoji> emojis;

    public static Status convert(MisskeyNote misskeyNote) {
        Status status = new Status();
        status.id = misskeyNote.id;
        status.in_reply_to_id = misskeyNote.replyId;
        status.content = misskeyNote.text != null ? misskeyNote.text : "";
        status.text = misskeyNote.text != null ? misskeyNote.text : "";
        status.spoiler_text = misskeyNote.cw;
        status.visibility = misskeyNote.visibility;
        status.created_at = misskeyNote.createdAt;
        status.uri = misskeyNote.uri;
        status.url = misskeyNote.url;

        Account account = new Account();
        account.id = misskeyNote.user.id;
        account.acct = misskeyNote.user.username;
        account.username = misskeyNote.user.username;
        account.display_name = misskeyNote.user.name;
        account.avatar = misskeyNote.user.avatarUrl;
        account.avatar_static = misskeyNote.user.avatarUrl;
        status.account = account;

        if (misskeyNote.files != null && misskeyNote.files.size() > 0) {
            List<Attachment> attachmentList = new ArrayList<>();
            for (MisskeyFile misskeyFile : misskeyNote.files) {
                Attachment attachment = new Attachment();
                attachment.type = misskeyFile.type;
                attachment.description = misskeyFile.comment;
                attachment.url = misskeyFile.url;
                attachment.preview_url = misskeyFile.thumbnailUrl;
                if (misskeyFile.isSensitive) {
                    status.sensitive = true;
                }
                attachmentList.add(attachment);
            }
            status.media_attachments = attachmentList;
        }

        return status;
    }

    public static class MisskeyUser implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("username")
        public String username;
        @SerializedName("avatarUrl")
        public String avatarUrl;
        @SerializedName("emojis")
        public List<MisskeyEmoji> emojis;
    }

    public static class MisskeyFile implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("comment")
        public String comment;
        @SerializedName("isSensitive")
        public boolean isSensitive;
        @SerializedName("thumbnailUrl")
        public String thumbnailUrl;
        @SerializedName("url")
        public String url;
        @SerializedName("type")
        public String type;
    }

    public static class MisskeyEmoji implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("comment")
        public String url;
    }

    public static class MisskeyParams implements Serializable {
        @SerializedName("local")
        public boolean local = true;
        @SerializedName("file")
        public boolean file = false;
        @SerializedName("poll")
        public boolean poll = false;
        @SerializedName("remote")
        public boolean remote = false;
        @SerializedName("reply")
        public boolean reply = false;
        @SerializedName("untilId")
        public String untilId;
        @SerializedName("limit")
        public int limit;
    }

}
