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

import java.util.List;

public class WellKnownNodeinfo {

    @SerializedName("links")
    public List<NodeInfoLinks> links;

    public static class NodeInfoLinks {
        @SerializedName("reel")
        public String reel;
        @SerializedName("href")
        public String href;
    }

    public static class NodeInfo {
        @SerializedName("version")
        public String version;
        @SerializedName("software")
        public Software software;
        @SerializedName("usage")
        public Usage usage;
        @SerializedName("metadata")
        public Metadata metadata;
        @SerializedName("openRegistrations")
        public boolean openRegistrations;

    }

    public static class Software {
        @SerializedName("name")
        public String name;
        @SerializedName("version")
        public String version;
    }

    public static class Usage {
        @SerializedName("users")
        public Users users;
        @SerializedName("localPosts")
        public int localPosts;
    }

    public static class Users {
        @SerializedName("total")
        public int total;
        @SerializedName("activeMonth")
        public int activeMonth;
        @SerializedName("activeHalfyear")
        public int activeHalfyear;
    }

    public static class Metadata {
        @SerializedName("nodeName")
        public String nodeName;
        @SerializedName("nodeDescription")
        public String nodeDescription;
        @SerializedName("staffAccounts")
        public List<String> staffAccounts;
    }
}
