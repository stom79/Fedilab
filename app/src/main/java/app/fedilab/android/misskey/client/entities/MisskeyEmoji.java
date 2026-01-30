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

import app.fedilab.android.mastodon.client.entities.api.Emoji;

public class MisskeyEmoji implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("aliases")
    public List<String> aliases;
    @SerializedName("name")
    public String name;
    @SerializedName("category")
    public String category;
    @SerializedName("url")
    public String url;
    @SerializedName("isSensitive")
    public boolean isSensitive;

    public Emoji toEmoji() {
        Emoji emoji = new Emoji();
        emoji.shortcode = this.name;
        emoji.url = this.url;
        emoji.static_url = this.url;
        emoji.visible_in_picker = !this.isSensitive;
        emoji.category = this.category;
        return emoji;
    }
}
