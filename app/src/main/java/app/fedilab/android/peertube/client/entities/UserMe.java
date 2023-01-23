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

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.data.AccountData.PeertubeAccount;
import app.fedilab.android.peertube.client.data.ChannelData;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class UserMe {

    @SerializedName("account")
    private PeertubeAccount account;
    @SerializedName("autoPlayNextVideo")
    private boolean autoPlayNextVideo;
    @SerializedName("autoPlayNextVideoPlaylist")
    private boolean autoPlayNextVideoPlaylist;
    @SerializedName("autoPlayVideo")
    private boolean autoPlayVideo;
    @SerializedName("blocked")
    private boolean blocked;
    @SerializedName("blockedReason")
    private String blockedReason;
    @SerializedName("createdAt")
    private Date createdAt;
    @SerializedName("email")
    private String email;
    @SerializedName("emailVerified")
    private String emailVerified;
    @SerializedName("id")
    private String id;
    @SerializedName("lastLoginDate")
    private Date lastLoginDate;
    @SerializedName("noInstanceConfigWarningModal")
    private boolean noInstanceConfigWarningModal;
    @SerializedName("noWelcomeModal")
    private boolean noWelcomeModal;
    @SerializedName("notificationSettings")
    private NotificationSettings notificationSettings;
    @SerializedName("nsfwPolicy")
    private String nsfwPolicy;
    @SerializedName("role")
    private int role;
    @SerializedName("roleLabel")
    private String roleLabel;
    @SerializedName("username")
    private String username;
    @SerializedName("videoChannels")
    private List<ChannelData.Channel> videoChannels;
    @SerializedName("videoLanguages")
    private List<String> videoLanguages;
    @SerializedName("videoQuota")
    private long videoQuota;
    @SerializedName("videoQuotaDaily")
    private long videoQuotaDaily;
    @SerializedName("videosHistoryEnabled")
    private boolean videosHistoryEnabled;
    @SerializedName("webTorrentEnabled")
    private boolean webTorrentEnabled;

    public PeertubeAccount getAccount() {
        return account;
    }

    public void setAccount(PeertubeAccount account) {
        this.account = account;
    }

    public boolean isAutoPlayNextVideo() {
        return autoPlayNextVideo;
    }

    public void setAutoPlayNextVideo(boolean autoPlayNextVideo) {
        this.autoPlayNextVideo = autoPlayNextVideo;
    }

    public boolean isAutoPlayNextVideoPlaylist() {
        return autoPlayNextVideoPlaylist;
    }

    public void setAutoPlayNextVideoPlaylist(boolean autoPlayNextVideoPlaylist) {
        this.autoPlayNextVideoPlaylist = autoPlayNextVideoPlaylist;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(String emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public boolean isNoInstanceConfigWarningModal() {
        return noInstanceConfigWarningModal;
    }

    public void setNoInstanceConfigWarningModal(boolean noInstanceConfigWarningModal) {
        this.noInstanceConfigWarningModal = noInstanceConfigWarningModal;
    }

    public boolean isNoWelcomeModal() {
        return noWelcomeModal;
    }

    public void setNoWelcomeModal(boolean noWelcomeModal) {
        this.noWelcomeModal = noWelcomeModal;
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

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ChannelData.Channel> getVideoChannels() {
        return videoChannels;
    }

    public void setVideoChannels(List<ChannelData.Channel> videoChannels) {
        this.videoChannels = videoChannels;
    }

    public List<String> getVideoLanguages() {
        return videoLanguages;
    }

    public void setVideoLanguages(List<String> videoLanguages) {
        this.videoLanguages = videoLanguages;
    }

    public long getVideoQuota() {
        return videoQuota;
    }

    public void setVideoQuota(long videoQuota) {
        this.videoQuota = videoQuota;
    }

    public long getVideoQuotaDaily() {
        return videoQuotaDaily;
    }

    public void setVideoQuotaDaily(long videoQuotaDaily) {
        this.videoQuotaDaily = videoQuotaDaily;
    }

    public boolean isVideosHistoryEnabled() {
        return videosHistoryEnabled;
    }

    public void setVideosHistoryEnabled(boolean videosHistoryEnabled) {
        this.videosHistoryEnabled = videosHistoryEnabled;
    }

    public boolean isWebTorrentEnabled() {
        return webTorrentEnabled;
    }

    public void setWebTorrentEnabled(boolean webTorrentEnabled) {
        this.webTorrentEnabled = webTorrentEnabled;
    }

    public boolean isAutoPlayVideo() {
        return autoPlayVideo;
    }

    public void setAutoPlayVideo(boolean autoPlayVideo) {
        this.autoPlayVideo = autoPlayVideo;
    }


    public static class AvatarResponse {
        @SerializedName("avatar")
        private Avatar avatar;

        public Avatar getAvatar() {
            return avatar;
        }

        public void setAvatar(Avatar avatar) {
            this.avatar = avatar;
        }
    }

    public static class VideoQuota {
        @SerializedName("videoQuotaUsed")
        private long videoQuotaUsed;
        @SerializedName("videoQuotaUsedDaily")
        private long videoQuotaUsedDaily;

        public long getVideoQuotaUsed() {
            return videoQuotaUsed;
        }

        public void setVideoQuotaUsed(long videoQuotaUsed) {
            this.videoQuotaUsed = videoQuotaUsed;
        }

        public long getVideoQuotaUsedDaily() {
            return videoQuotaUsedDaily;
        }

        public void setVideoQuotaUsedDaily(long videoQuotaUsedDaily) {
            this.videoQuotaUsedDaily = videoQuotaUsedDaily;
        }
    }
}
