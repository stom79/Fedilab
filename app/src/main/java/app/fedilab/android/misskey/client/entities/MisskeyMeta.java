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
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Instance;

public class MisskeyMeta implements Serializable {

    @SerializedName("uri")
    public String uri;
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
    @SerializedName("version")
    public String version;
    @SerializedName("maintainerName")
    public String maintainerName;
    @SerializedName("maintainerEmail")
    public String maintainerEmail;
    @SerializedName("langs")
    public List<String> langs;
    @SerializedName("tosUrl")
    public String tosUrl;
    @SerializedName("repositoryUrl")
    public String repositoryUrl;
    @SerializedName("feedbackUrl")
    public String feedbackUrl;
    @SerializedName("iconUrl")
    public String iconUrl;
    @SerializedName("bannerUrl")
    public String bannerUrl;
    @SerializedName("backgroundImageUrl")
    public String backgroundImageUrl;
    @SerializedName("maxNoteTextLength")
    public int maxNoteTextLength = 3000;
    @SerializedName("cacheRemoteFiles")
    public boolean cacheRemoteFiles;
    @SerializedName("enableRecaptcha")
    public boolean enableRecaptcha;
    @SerializedName("enableRegistration")
    public boolean enableRegistration;
    @SerializedName("requireSetup")
    public boolean requireSetup;
    @SerializedName("emojis")
    public List<MisskeyEmoji> emojis;
    @SerializedName("policies")
    public Policies policies;

    public static class Policies implements Serializable {
        @SerializedName("gtlAvailable")
        public boolean gtlAvailable = true;
        @SerializedName("ltlAvailable")
        public boolean ltlAvailable = true;
        @SerializedName("canPublicNote")
        public boolean canPublicNote = true;
        @SerializedName("driveCapacityMb")
        public int driveCapacityMb;
        @SerializedName("pinLimit")
        public int pinLimit;
        @SerializedName("antennaLimit")
        public int antennaLimit;
        @SerializedName("wordMuteLimit")
        public int wordMuteLimit;
        @SerializedName("clipLimit")
        public int clipLimit;
        @SerializedName("noteEachClipsLimit")
        public int noteEachClipsLimit;
        @SerializedName("userListLimit")
        public int userListLimit;
        @SerializedName("userEachUserListsLimit")
        public int userEachUserListsLimit;
        @SerializedName("rateLimitFactor")
        public float rateLimitFactor;
    }

    public Instance toInstance() {
        Instance instance = new Instance();
        instance.uri = this.uri;
        instance.title = this.name;
        instance.description = this.description;
        instance.short_description = this.description;
        instance.version = this.version;
        instance.email = this.maintainerEmail;
        instance.languages = this.langs;
        instance.registrations = this.enableRegistration;
        instance.thumbnail = this.bannerUrl != null ? this.bannerUrl : this.iconUrl;
        instance.max_toot_chars = String.valueOf(this.maxNoteTextLength);

        instance.configuration = new Instance.Configuration();
        instance.configuration.statusesConf = new Instance.StatusesConf();
        instance.configuration.statusesConf.max_characters = this.maxNoteTextLength;
        instance.configuration.statusesConf.max_media_attachments = 16;

        instance.configuration.pollsConf = new Instance.PollsConf();
        instance.configuration.pollsConf.max_options = 10;
        instance.configuration.pollsConf.max_option_chars = 150;

        instance.configuration.media_attachments = new Instance.MediaConf();
        instance.configuration.media_attachments.supported_mime_types = new ArrayList<>();
        instance.configuration.media_attachments.supported_mime_types.add("image/jpeg");
        instance.configuration.media_attachments.supported_mime_types.add("image/png");
        instance.configuration.media_attachments.supported_mime_types.add("image/gif");
        instance.configuration.media_attachments.supported_mime_types.add("image/webp");
        instance.configuration.media_attachments.supported_mime_types.add("image/avif");
        instance.configuration.media_attachments.supported_mime_types.add("video/mp4");
        instance.configuration.media_attachments.supported_mime_types.add("video/webm");
        instance.configuration.media_attachments.supported_mime_types.add("audio/mpeg");
        instance.configuration.media_attachments.supported_mime_types.add("audio/ogg");

        return instance;
    }
}
