package app.fedilab.android.mastodon.client.entities.api;
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

public class Link implements Serializable {
    @SerializedName("url")
    public String url;
    @SerializedName("title")
    public String title;
    @SerializedName("description")
    public String description;
    @SerializedName("type")
    public String type;
    @SerializedName("author_name")
    public String author_name;
    @SerializedName("author_url")
    public String author_url;
    @SerializedName("provider_name")
    public String provider_name;
    @SerializedName("provider_url")
    public String provider_url;
    @SerializedName("html")
    public String html;
    @SerializedName("width")
    public int width;
    @SerializedName("height")
    public int height;
    @SerializedName("image")
    public String image;
    @SerializedName("embed_url")
    public String embed_url;
    @SerializedName("blurhash")
    public String blurhash;
    @SerializedName("history")
    public List<History> history;
}
