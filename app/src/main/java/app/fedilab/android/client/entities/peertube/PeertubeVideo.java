package app.fedilab.android.client.entities.peertube;
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
public class PeertubeVideo implements Serializable {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Video> data;

    public static Status convert(Video peertubeVideo) {
        Status status = new Status();
        status.id = peertubeVideo.id;
        status.content = peertubeVideo.description != null ? peertubeVideo.description : "";
        status.text = peertubeVideo.description;
        status.visibility = "public";
        status.created_at = peertubeVideo.publishedAt;
        status.uri = peertubeVideo.uuid;
        status.sensitive = peertubeVideo.nsfw;
        status.url = "https://" + peertubeVideo.account.host + "/videos/watch/" + peertubeVideo.uuid;
        Account account = new Account();
        account.id = peertubeVideo.channel.id;
        account.acct = peertubeVideo.channel.name;
        account.username = peertubeVideo.channel.name;
        account.display_name = peertubeVideo.channel.displayName;
        if (peertubeVideo.channel.avatar != null) {
            account.avatar = "https://" + peertubeVideo.account.host + peertubeVideo.channel.avatar.path;
            account.avatar_static = "https://" + peertubeVideo.account.host + peertubeVideo.channel.avatar.path;
        }
        status.account = account;
        List<Attachment> attachmentList = new ArrayList<>();
        Attachment attachment = new Attachment();
        attachment.type = "video/mp4";
        attachment.url = "https://" + peertubeVideo.account.host + peertubeVideo.embedPath;
        attachment.preview_url = "https://" + peertubeVideo.account.host + peertubeVideo.thumbnailPath;
        attachmentList.add(attachment);
        status.media_attachments = attachmentList;
        return status;
    }

    public static class Video implements Serializable {
        @SerializedName("account")
        public PeertubeAccount account;
        @SerializedName("category")
        public Item category;
        @SerializedName("channel")
        public Channel channel;
        @SerializedName("createdAt")
        public Date createdAt;
        @SerializedName("description")
        public String description;
        @SerializedName("duration")
        public int duration;
        @SerializedName("embedPath")
        public String embedPath;
        @SerializedName("id")
        public String id;
        @SerializedName("isLive")
        public boolean isLive = false;
        @SerializedName("url")
        public String url;
        @SerializedName("isLocal")
        public boolean isLocal;
        @SerializedName("language")
        public ItemStr language;
        @SerializedName("licence")
        public Item licence;
        @SerializedName("likes")
        public int likes;
        @SerializedName("name")
        public String name;
        @SerializedName("nsfw")
        public boolean nsfw;
        @SerializedName("originallyPublishedAt")
        public Date originallyPublishedAt;
        @SerializedName("previewPath")
        public String previewPath;
        @SerializedName("privacy")
        public Item privacy;
        @SerializedName("publishedAt")
        public Date publishedAt;
        @SerializedName("thumbnailPath")
        public String thumbnailPath;
        @SerializedName("updatedAt")
        public Date updatedAt;
        @SerializedName("uuid")
        public String uuid;

        @SerializedName("views")
        public int views;
    }

    public static class PeertubeAccount implements Serializable {
        @SerializedName("avatar")
        public Avatar avatar;
        @SerializedName("createdAt")
        public Date createdAt;
        @SerializedName("description")
        public String description;
        @SerializedName("displayName")
        public String displayName;
        @SerializedName("followersCount")
        public int followersCount;
        @SerializedName("followingCount")
        public int followingCount;
        @SerializedName("host")
        public String host;
        @SerializedName("hostRedundancyAllowed")
        public boolean hostRedundancyAllowed;
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("username")
        public String username;
        @SerializedName("updatedAt")
        public Date updatedAt;
        @SerializedName("url")
        public String url;
        @SerializedName("userId")
        public String userId;
    }

    public static class Item implements Serializable {
        @SerializedName("id")
        public int id;
        @SerializedName("label")
        public String label;
    }

    public static class ItemStr implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("label")
        public String label;
    }

    public static class Channel implements Serializable {
        @SerializedName("avatar")
        public Avatar avatar;
        @SerializedName("displayName")
        public String displayName;
        @SerializedName("host")
        public String host;
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("url")
        public String url;
    }

    public static class Avatar implements Serializable {
        @SerializedName("createdAt")
        public Date createdAt;
        @SerializedName("path")
        public String path;
        @SerializedName("updatedAt")
        public Date updatedAt;
    }
}
