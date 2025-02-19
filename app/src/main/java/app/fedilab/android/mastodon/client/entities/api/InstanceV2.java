package app.fedilab.android.mastodon.client.entities.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

public class InstanceV2 implements Serializable {

    @SerializedName("domain")
    public String domain;
    @SerializedName("title")
    public String title;
    @SerializedName("version")
    public String version;
    @SerializedName("source_url")
    public String sourceUrl;
    @SerializedName("description")
    public String description;
    @SerializedName("configuration")
    public Configuration configuration;



    public static String serialize(InstanceV2 instance) {
        Gson gson = new Gson();
        try {
            return gson.toJson(instance);
        } catch (Exception e) {
            return null;
        }
    }

    public static InstanceV2 restore(String serialized) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serialized, InstanceV2.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static class Configuration implements Serializable {
        @SerializedName("vapid")
        public VapId vapId;
    }
    public static class VapId implements Serializable {
        @SerializedName("public_key")
        public String publicKey;
    }


}
