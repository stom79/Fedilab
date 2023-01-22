package app.fedilab.android.peertube.client.entities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.net.Uri;

import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class UserSettings {

    private Boolean videosHistoryEnabled;
    private Boolean autoPlayVideo;
    private Boolean webTorrentEnabled;
    private Boolean autoPlayNextVideo;
    private List<String> videoLanguages;
    private String description;
    private String displayName;
    private Uri avatarfile;
    private String fileName;
    private NotificationSettings notificationSettings;
    private String nsfwPolicy;

    public Boolean isVideosHistoryEnabled() {
        return videosHistoryEnabled;
    }

    public Boolean isAutoPlayVideo() {
        return autoPlayVideo;
    }

    public Boolean isWebTorrentEnabled() {
        return webTorrentEnabled;
    }

    public List<String> getVideoLanguages() {
        return videoLanguages;
    }

    public void setVideoLanguages(List<String> videoLanguages) {
        this.videoLanguages = videoLanguages;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Uri getAvatarfile() {
        return avatarfile;
    }

    public void setAvatarfile(Uri avatarfile) {
        this.avatarfile = avatarfile;
    }

    public Boolean getVideosHistoryEnabled() {
        return videosHistoryEnabled;
    }

    public void setVideosHistoryEnabled(Boolean videosHistoryEnabled) {
        this.videosHistoryEnabled = videosHistoryEnabled;
    }

    public Boolean getAutoPlayVideo() {
        return autoPlayVideo;
    }

    public void setAutoPlayVideo(Boolean autoPlayVideo) {
        this.autoPlayVideo = autoPlayVideo;
    }

    public Boolean getWebTorrentEnabled() {
        return webTorrentEnabled;
    }

    public void setWebTorrentEnabled(Boolean webTorrentEnabled) {
        this.webTorrentEnabled = webTorrentEnabled;
    }

    public Boolean isAutoPlayNextVideo() {
        return autoPlayNextVideo;
    }

    public Boolean getAutoPlayNextVideo() {
        return autoPlayNextVideo;
    }

    public void setAutoPlayNextVideo(Boolean autoPlayNextVideo) {
        this.autoPlayNextVideo = autoPlayNextVideo;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        if (fileName == null) {
            this.fileName = "avatar.png";
        } else {
            this.fileName = fileName;
        }
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }

    public void setNotificationSettings(NotificationSettings notificationSettings) {
        this.notificationSettings = notificationSettings;
    }

    public String getNsfwPolicy() {
        return nsfwPolicy;
    }

    public void setNsfwPolicy(String nsfwPolicy) {
        this.nsfwPolicy = nsfwPolicy;
    }
}

