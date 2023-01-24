package app.fedilab.android.peertube.client.entities;
/* Copyright 2023 Thomas Schneider
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

@SuppressWarnings({"unused", "RedundantSuppression"})
public class WellKnownNodeinfo {


    @SerializedName("links")
    private List<NodeInfoLinks> links;

    public List<NodeInfoLinks> getLinks() {
        return links;
    }

    public void setLinks(List<NodeInfoLinks> links) {
        this.links = links;
    }

    public static class NodeInfoLinks {
        @SerializedName("reel")
        private String reel;
        @SerializedName("href")
        private String href;

        public String getReel() {
            return reel;
        }

        public void setReel(String reel) {
            this.reel = reel;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }

    public static class NodeInfo {
        @SerializedName("version")
        private String version;
        @SerializedName("software")
        private Software software;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Software getSoftware() {
            return software;
        }

        public void setSoftware(Software software) {
            this.software = software;
        }
    }

    public static class Software {
        @SerializedName("name")
        private String name;
        @SerializedName("version")
        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
