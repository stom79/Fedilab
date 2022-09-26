package app.fedilab.android.client.entities.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

public class Instance implements Serializable {

    @SerializedName("uri")
    public String uri;
    @SerializedName("title")
    public String title;
    @SerializedName("short_description")
    public String short_description;
    @SerializedName("description")
    public String description;
    @SerializedName("email")
    public String email;
    @SerializedName("version")
    public String version;
    @SerializedName("languages")
    public List<String> languages;
    @SerializedName("registrations")
    public boolean registrations;
    @SerializedName("rules")
    public List<Rule> rules;
    @SerializedName("approval_required")
    public boolean approval_required;
    @SerializedName("invites_enabled")
    public boolean invites_enabled;
    @SerializedName("stats")
    public Stats stats;
    @SerializedName("urls")
    public Urls urls;
    @SerializedName("thumbnail")
    public String thumbnail;
    @SerializedName("contact_account")
    public Account contact_account;
    @SerializedName("configuration")
    public Configuration configuration;
    @SerializedName("poll_limits")
    public PollsConf poll_limits;
    @SerializedName("max_toot_chars")
    public String max_toot_chars;

    public static String serialize(Instance instance) {
        Gson gson = new Gson();
        try {
            return gson.toJson(instance);
        } catch (Exception e) {
            return null;
        }
    }

    public static Instance restore(String serialized) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serialized, Instance.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getMimeTypeAudio() {
        List<String> mimeTypes = new ArrayList<>();
        if (configuration == null || configuration.media_attachments == null || configuration.media_attachments.supported_mime_types == null) {
            return mimeTypes;
        }
        for (String mimeType : configuration.media_attachments.supported_mime_types) {
            if (mimeType.startsWith("audio")) {
                mimeTypes.add(mimeType);
            }
        }
        return mimeTypes;
    }

    public List<String> getMimeTypeVideo() {
        List<String> mimeTypes = new ArrayList<>();
        if (configuration == null || configuration.media_attachments == null || configuration.media_attachments.supported_mime_types == null) {
            return mimeTypes;
        }
        for (String mimeType : configuration.media_attachments.supported_mime_types) {
            if (mimeType.startsWith("video")) {
                mimeTypes.add(mimeType);
            }
        }
        return mimeTypes;
    }

    public List<String> getMimeTypeImage() {
        List<String> mimeTypes = new ArrayList<>();
        if (configuration == null || configuration.media_attachments == null || configuration.media_attachments.supported_mime_types == null) {
            return mimeTypes;
        }
        for (String mimeType : configuration.media_attachments.supported_mime_types) {
            if (mimeType.startsWith("image")) {
                mimeTypes.add(mimeType);
            }
        }
        return mimeTypes;
    }

    public List<String> getMimeTypeOther() {
        List<String> mimeTypes = new ArrayList<>();
        if (configuration == null || configuration.media_attachments == null || configuration.media_attachments.supported_mime_types == null) {
            return mimeTypes;
        }
        for (String mimeType : configuration.media_attachments.supported_mime_types) {
            if (!mimeType.startsWith("image") && !mimeType.startsWith("video") && !mimeType.startsWith("audio")) {
                mimeTypes.add(mimeType);
            }
        }
        return mimeTypes;
    }

    public static class Configuration implements Serializable {
        @SerializedName("statuses")
        public StatusesConf statusesConf;
        @SerializedName("polls")
        public PollsConf pollsConf;
        @SerializedName("media_attachments")
        public MediaConf media_attachments;
    }

    public static class StatusesConf implements Serializable {
        @SerializedName("max_characters")
        public int max_characters = 500;
        @SerializedName("max_media_attachments")
        public int max_media_attachments = 4;
        @SerializedName("characters_reserved_per_url")
        public int characters_reserved_per_url;
    }

    public static class MediaConf implements Serializable {
        @SerializedName("supported_mime_types")
        public List<String> supported_mime_types;
        @SerializedName("image_size_limit")
        public int image_size_limit;
        @SerializedName("image_matrix_limit")
        public int image_matrix_limit;
        @SerializedName("video_size_limit")
        public int video_size_limit;
        @SerializedName("video_frame_rate_limit")
        public int video_frame_rate_limit;
        @SerializedName("video_matrix_limit")
        public int video_matrix_limit;
    }

    public static class PollsConf implements Serializable {
        @SerializedName("min_expiration")
        public int min_expiration;
        @SerializedName("max_options")
        public int max_options = 4;
        @SerializedName("max_option_chars")
        public int max_option_chars = 25;
        @SerializedName("max_expiration")
        public int max_expiration;
    }

    public static class Stats implements Serializable {
        @SerializedName("user_count")
        public int user_count;
        @SerializedName("status_count")
        public int status_count;
        @SerializedName("domain_count")
        public int domain_count;
    }

    public static class Urls implements Serializable {
        @SerializedName("streaming_api")
        public String streaming_api;
    }

    public static class Rule implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("text")
        public String text;
        public transient boolean isChecked = false;
    }


}
