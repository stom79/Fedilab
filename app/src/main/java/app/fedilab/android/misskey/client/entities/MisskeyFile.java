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
import java.util.Date;

import app.fedilab.android.mastodon.client.entities.api.Attachment;

public class MisskeyFile implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("createdAt")
    public Date createdAt;
    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public String type;
    @SerializedName("md5")
    public String md5;
    @SerializedName("size")
    public long size;
    @SerializedName("url")
    public String url;
    @SerializedName("thumbnailUrl")
    public String thumbnailUrl;
    @SerializedName("isSensitive")
    public boolean isSensitive;
    @SerializedName("blurhash")
    public String blurhash;
    @SerializedName("comment")
    public String comment;
    @SerializedName("properties")
    public FileProperties properties;

    public static class FileProperties implements Serializable {
        @SerializedName("width")
        public int width;
        @SerializedName("height")
        public int height;
        @SerializedName("orientation")
        public int orientation;
        @SerializedName("avgColor")
        public String avgColor;
    }

    public Attachment toAttachment() {
        Attachment attachment = new Attachment();
        attachment.id = this.id;
        attachment.url = this.url;
        attachment.preview_url = this.thumbnailUrl != null ? this.thumbnailUrl : this.url;
        attachment.remote_url = this.url;
        attachment.description = this.comment;
        attachment.blurhash = this.blurhash;

        if (this.type != null) {
            if (this.type.startsWith("image/")) {
                if (this.type.equals("image/gif")) {
                    attachment.type = "gifv";
                } else {
                    attachment.type = "image";
                }
            } else if (this.type.startsWith("video/")) {
                attachment.type = "video";
            } else if (this.type.startsWith("audio/")) {
                attachment.type = "audio";
            } else {
                attachment.type = "unknown";
            }
        }

        if (this.properties != null) {
            attachment.meta = new Attachment.Meta();
            attachment.meta.small = new Attachment.MediaData();
            attachment.meta.small.width = this.properties.width;
            attachment.meta.small.height = this.properties.height;
            attachment.meta.original = new Attachment.MediaData();
            attachment.meta.original.width = this.properties.width;
            attachment.meta.original.height = this.properties.height;
        }

        return attachment;
    }
}
