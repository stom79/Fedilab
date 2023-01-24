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

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class VideoParams {

    @SerializedName("channelId")
    private String channelId;
    @SerializedName("name")
    private String name;
    @SerializedName("category")
    private int category;
    @SerializedName("commentsEnabled")
    private boolean commentsEnabled;
    @SerializedName("description")
    private String description;
    @SerializedName("downloadEnabled")
    private boolean downloadEnabled;
    @SerializedName("language")
    private String language;
    @SerializedName("licence")
    private String licence;
    @SerializedName("nsfw")
    private boolean nsfw;
    @SerializedName("originallyPublishedAt")
    private Date originallyPublishedAt;
    @SerializedName("privacy")
    private int privacy;
    @SerializedName("support")
    private String support;
    @SerializedName("tags")
    private List<String> tags;
    @SerializedName("waitTranscoding")
    private boolean waitTranscoding;

    @NotNull
    @Override
    public String toString() {
        return "channelId: " + channelId + "\nname: " + name + "\ncategory: " + category + "\ncommentsEnabled: " + commentsEnabled
                + "\ndescription: " + description + "\ndownloadEnabled: " + downloadEnabled + "\nlanguage: " + language
                + "\nlicence: " + licence + "\nnsfw: " + nsfw + "\noriginallyPublishedAt: " + originallyPublishedAt
                + "\nprivacy: " + privacy + "\nsupport: " + support + "\ntags: " + tags + "\nwaitTranscoding: " + waitTranscoding;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public boolean isCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public void setDownloadEnabled(boolean downloadEnabled) {
        this.downloadEnabled = downloadEnabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public Date getOriginallyPublishedAt() {
        return originallyPublishedAt;
    }

    public void setOriginallyPublishedAt(Date originallyPublishedAt) {
        this.originallyPublishedAt = originallyPublishedAt;
    }

    public int getPrivacy() {
        return privacy;
    }

    public void setPrivacy(int privacy) {
        this.privacy = privacy;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isWaitTranscoding() {
        return waitTranscoding;
    }

    public void setWaitTranscoding(boolean waitTranscoding) {
        this.waitTranscoding = waitTranscoding;
    }
}
