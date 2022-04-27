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

public class Attachment implements Serializable {
    @SerializedName("id")
    public String id;
    @SerializedName("type")
    public String type;
    @SerializedName("url")
    public String url;
    @SerializedName("preview_url")
    public String preview_url;
    @SerializedName("remote_url")
    public String remote_url;
    @SerializedName("text_url")
    public String text_url;
    @SerializedName("description")
    public String description;
    @SerializedName("blurhash")
    public String blurhash;
    @SerializedName("mimeType")
    public String mimeType;
    @SerializedName("filename")
    public String filename;
    @SerializedName("size")
    public long size;
    @SerializedName("local_path")
    public String local_path;

}
