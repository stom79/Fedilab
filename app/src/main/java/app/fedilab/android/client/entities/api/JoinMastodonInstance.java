package app.fedilab.android.client.entities.api;
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

import java.util.List;

public class JoinMastodonInstance {
    @SerializedName("domain")
    public String domain;
    @SerializedName("version")
    public String version;
    @SerializedName("description")
    public String description;
    @SerializedName("languages")
    public List<String> languages;
    @SerializedName("categories")
    public List<String> categories;
    @SerializedName("proxied_thumbnail")
    public String proxied_thumbnail;
    @SerializedName("total_users")
    public int total_users;
    @SerializedName("last_week_users")
    public int last_week_users;
    @SerializedName("approval_required")
    public boolean approval_required;
    @SerializedName("language")
    public String language;
    @SerializedName("general")
    public String general;
}
