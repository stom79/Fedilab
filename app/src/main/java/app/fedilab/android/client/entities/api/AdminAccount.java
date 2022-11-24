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

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class AdminAccount implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("username")
    public String username;
    @SerializedName("domain")
    public String domain;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("email")
    public String email;
    public static LinkedHashMap<Integer, String> permissions;

    static {
        permissions = new LinkedHashMap<>();
        permissions.put(1, "Administrator");
        permissions.put(2, "Devops");
        permissions.put(4, "View Audit Log");
        permissions.put(8, "View Dashboard");
        permissions.put(10, "Manage Reports");
        permissions.put(20, "Manage Federation");
        permissions.put(40, "Manage Settings");
        permissions.put(80, "Manage Blocks");
        permissions.put(100, "Manage Taxonomies");
        permissions.put(200, "Manage Appeals");
        permissions.put(400, "Manage Users");
        permissions.put(800, "Manage Invites");
        permissions.put(1000, "Manage Rules");
        permissions.put(2000, "Manage Announcements");
        permissions.put(4000, "Manage Custom Emojis");
        permissions.put(8000, "Manage Webhooks");
        permissions.put(10000, "Invite Users");
        permissions.put(20000, "Manage Roles");
        permissions.put(40000, "Manage User Access");
        permissions.put(80000, "Delete User Data");
    }

    @SerializedName("ip")
    public String ip;
    @SerializedName("role")
    public Role role;
    @SerializedName("confirmed")
    public boolean confirmed;
    @SerializedName("suspended")
    public boolean suspended;
    @SerializedName("silenced")
    public boolean silenced;
    @SerializedName("disabled")
    public boolean disabled;
    @SerializedName("approved")
    public boolean approved;
    @SerializedName("ips")
    public List<IP> ips;
    @SerializedName("account")
    public Account account;
    @SerializedName("created_by_application_id")
    public String created_by_application_id;
    @SerializedName("invited_by_account_id")
    public String invited_by_account_id;


    public static class IP implements Serializable {
        @SerializedName("ip")
        public String ip;
        @SerializedName("used_at")
        public Date used_at;
    }

    @SerializedName("locale")
    public String locale;
    @SerializedName("invite_request")
    public String invite_request;

    public static class Role implements Serializable {
        @SerializedName("ip")
        public String ip;
        @SerializedName("name")
        public String name;
        @SerializedName("color")
        public String color;
        @SerializedName("position")
        public long position;
        @SerializedName("permissions")
        public int permissions;
        @SerializedName("highlighted")
        public boolean highlighted;
        @SerializedName("created_at")
        public Date created_at;
        @SerializedName("updated_at")
        public Date updated_at;
    }

}
