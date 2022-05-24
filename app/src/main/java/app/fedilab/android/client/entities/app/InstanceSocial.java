package app.fedilab.android.client.entities.app;
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

import java.util.Date;
import java.util.List;

public class InstanceSocial {

    @SerializedName("instances")
    public List<Instance> instances;

    public static class Instance {
        @SerializedName("name")
        public String name;
        @SerializedName("added_at")
        public Date added_at;
        @SerializedName("updated_at")
        public Date updated_at;
        @SerializedName("checked_at")
        public Date checked_at;
        @SerializedName("uptime")
        public float uptime;
        @SerializedName("up")
        public boolean up;
        @SerializedName("version")
        public String version;
        @SerializedName("thumbnail")
        public String thumbnail;
        @SerializedName("dead")
        public boolean dead;
        @SerializedName("active_users")
        public int active_users;
        @SerializedName("statuses")
        public int statuses;
        @SerializedName("email")
        public String email;
        @SerializedName("admin")
        public String admin;
    }
}
