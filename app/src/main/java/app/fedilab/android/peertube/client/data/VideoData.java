package app.fedilab.android.peertube.client.data;
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


import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.peertube.client.entities.File;
import app.fedilab.android.peertube.client.entities.Item;
import app.fedilab.android.peertube.client.entities.ItemStr;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.StreamingPlaylists;
import app.fedilab.android.peertube.helper.Helper;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class VideoData implements Serializable {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Video> data;


    public static class Video implements Serializable {
        @SerializedName("account")
        private AccountData.PeertubeAccount account;
        @SerializedName("blacklisted")
        private boolean blacklisted;
        @SerializedName("blacklistedReason")
        private String blacklistedReason;
        @SerializedName("category")
        private Item category;
        @SerializedName("channel")
        private ChannelData.Channel channel;
        @SerializedName("commentsEnabled")
        private boolean commentsEnabled;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("description")
        private String description;
        @SerializedName("descriptionPath")
        private String descriptionPath;
        @SerializedName("dislikes")
        private int dislikes;
        @SerializedName("downloadEnabled")
        private boolean downloadEnabled;
        @SerializedName("duration")
        private int duration;
        @SerializedName("embedPath")
        private String embedPath;
        @SerializedName("embedUrl")
        private String embedUrl;
        @SerializedName("files")
        private List<File> files;
        @SerializedName("id")
        private String id;
        @SerializedName("isLive")
        private boolean isLive = false;
        @SerializedName("isLocal")
        private boolean isLocal;
        @SerializedName("language")
        private ItemStr language;
        @SerializedName("licence")
        private Item licence;
        @SerializedName("likes")
        private int likes;
        @SerializedName("name")
        private String name;
        @SerializedName("nsfw")
        private boolean nsfw;
        @SerializedName("originallyPublishedAt")
        private Date originallyPublishedAt;
        @SerializedName("previewPath")
        private String previewPath;
        @SerializedName("privacy")
        private Item privacy;
        @SerializedName("publishedAt")
        private Date publishedAt;
        @SerializedName("state")
        private Item state;
        @SerializedName("streamingPlaylists")
        private List<StreamingPlaylists> streamingPlaylists;
        @SerializedName("support")
        private String support;
        @SerializedName("tags")
        private List<String> tags;
        @SerializedName("thumbnailPath")
        private String thumbnailPath;
        @SerializedName("trackerUrls")
        private List<String> trackerUrls;
        @SerializedName("updatedAt")
        private Date updatedAt;
        @SerializedName("userHistory")
        private UserHistory userHistory;
        @SerializedName("uuid")
        private String uuid;
        @SerializedName("views")
        private int views;
        @SerializedName("waitTranscoding")
        private boolean waitTranscoding;
        private String myRating;
        private String originUrl;
        private int errorCode;
        private String errorMessage;
        //Dedicated to overview videos to reuse the logic of videos
        private boolean hasTitle = false;
        private String title;
        private titleType titleType;
        private List<PlaylistExist> playlistExists;

        public Video() {
        }

        public String getFileUrl(String resolution, Context context) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            int mode = sharedpreferences.getInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_NORMAL);
            List<File> files = getAllFile(context);
            if (files != null && files.size() > 0) {
                return getFile(context, files, resolution, mode);
            }
            return null;
        }

        public List<File> getAllFile(Context context) {
            if (files != null && files.size() > 0) { //Old support
                return files;
            } else if (streamingPlaylists != null) {
                List<File> files = new ArrayList<>();
                for (StreamingPlaylists streamingPlaylists : streamingPlaylists) {
                    File file = new File();
                    file.setResolutions(new Item(0, context.getString(R.string.auto)));
                    file.setFileUrl(streamingPlaylists.getPlaylistUrl());
                    file.setFileDownloadUrl(streamingPlaylists.getPlaylistUrl());
                    files.add(file);
                }
                return files;
            }
            return files;
        }


        private String getFile(Context context, List<File> files, String resolution, int mode) {
            if (resolution != null) {
                for (File file : files) {
                    if (file.getResolutions().getLabel().compareTo(resolution) == 0) {
                        return file.getFileUrl();
                    }
                }
            }
            File file = Helper.defaultFile(context, files);

            if (file != null) {
                return file.getFileUrl();
            } else {
                return null;
            }
        }

        public String getTorrentUrl(String resolution, Context context) {
            for (File file : files) {
                if (file.getResolutions().getLabel().compareTo(resolution) == 0) {
                    return file.getTorrentUrl();
                }
            }
            return Helper.defaultFile(context, files).getTorrentUrl();

        }

        public String getTorrentDownloadUrl(String resolution) {
            for (File file : files) {
                if (file.getResolutions().getLabel().compareTo(resolution) == 0) {
                    return file.getTorrentDownloadUrl();
                }
            }
            return files.get(0).getTorrentDownloadUrl();

        }

        public String getFileDownloadUrl(String resolution) {
            if (resolution != null) {
                for (File file : files) {
                    if (file.getResolutions().getLabel().compareTo(resolution) == 0) {
                        return file.getFileDownloadUrl();
                    }
                }
            }
            return files != null && files.size() > 0 ? files.get(0).getFileDownloadUrl() : null;
        }

        public AccountData.PeertubeAccount getAccount() {
            return account;
        }

        public void setAccount(AccountData.PeertubeAccount account) {
            this.account = account;
        }

        public boolean isBlacklisted() {
            return blacklisted;
        }

        public void setBlacklisted(boolean blacklisted) {
            this.blacklisted = blacklisted;
        }

        public String getBlacklistedReason() {
            return blacklistedReason;
        }

        public void setBlacklistedReason(String blacklistedReason) {
            this.blacklistedReason = blacklistedReason;
        }

        public Item getCategory() {
            return category;
        }

        public void setCategory(Item category) {
            this.category = category;
        }

        public ChannelData.Channel getChannel() {
            return channel;
        }

        public void setChannel(ChannelData.Channel channel) {
            this.channel = channel;
        }

        public boolean isCommentsEnabled() {
            return commentsEnabled;
        }

        public void setCommentsEnabled(boolean commentsEnabled) {
            this.commentsEnabled = commentsEnabled;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescriptionPath() {
            return descriptionPath;
        }

        public void setDescriptionPath(String descriptionPath) {
            this.descriptionPath = descriptionPath;
        }

        public int getDislikes() {
            return dislikes;
        }

        public void setDislikes(int dislikes) {
            this.dislikes = dislikes;
        }

        public boolean isDownloadEnabled() {
            return downloadEnabled;
        }

        public void setDownloadEnabled(boolean downloadEnabled) {
            this.downloadEnabled = downloadEnabled;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public String getEmbedPath() {
            return embedPath;
        }

        public void setEmbedPath(String embedPath) {
            this.embedPath = embedPath;
        }

        public String getEmbedUrl() {
            return embedUrl;
        }

        public void setEmbedUrl(String embedUrl) {
            this.embedUrl = embedUrl;
        }

        public List<File> getFiles() {
            return files;
        }

        public void setFiles(List<File> files) {
            this.files = files;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isLive() {
            return isLive;
        }

        public void setLive(boolean live) {
            isLive = live;
        }

        public boolean isLocal() {
            return isLocal;
        }

        public void setLocal(boolean local) {
            isLocal = local;
        }

        public ItemStr getLanguage() {
            return language;
        }

        public void setLanguage(ItemStr language) {
            this.language = language;
        }

        public Item getLicence() {
            return licence;
        }

        public void setLicence(Item licence) {
            this.licence = licence;
        }

        public int getLikes() {
            return likes;
        }

        public void setLikes(int likes) {
            this.likes = likes;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getPreviewPath() {
            return previewPath;
        }

        public void setPreviewPath(String previewPath) {
            this.previewPath = previewPath;
        }

        public Item getPrivacy() {
            return privacy;
        }

        public void setPrivacy(Item privacy) {
            this.privacy = privacy;
        }

        public Date getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(Date publishedAt) {
            this.publishedAt = publishedAt;
        }

        public Item getState() {
            return state;
        }

        public void setState(Item state) {
            this.state = state;
        }

        public List<StreamingPlaylists> getStreamingPlaylists() {
            return streamingPlaylists;
        }

        public void setStreamingPlaylists(List<StreamingPlaylists> streamingPlaylists) {
            this.streamingPlaylists = streamingPlaylists;
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

        public String getThumbnailPath() {
            return thumbnailPath;
        }

        public void setThumbnailPath(String thumbnailPath) {
            this.thumbnailPath = thumbnailPath;
        }

        public List<String> getTrackerUrls() {
            return trackerUrls;
        }

        public void setTrackerUrls(List<String> trackerUrls) {
            this.trackerUrls = trackerUrls;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getViews() {
            return views;
        }

        public void setViews(int views) {
            this.views = views;
        }

        public boolean isWaitTranscoding() {
            return waitTranscoding;
        }

        public void setWaitTranscoding(boolean waitTranscoding) {
            this.waitTranscoding = waitTranscoding;
        }

        public String getOriginUrl() {
            return originUrl;
        }

        public void setOriginUrl(String originUrl) {
            this.originUrl = originUrl;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public String getMyRating() {
            return myRating;
        }

        public void setMyRating(String myRating) {
            this.myRating = myRating;
        }

        public boolean isHasTitle() {
            return hasTitle;
        }

        public void setHasTitle(boolean hasTitle) {
            this.hasTitle = hasTitle;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Video.titleType getTitleType() {
            return titleType;
        }

        public void setTitleType(Video.titleType titleType) {
            this.titleType = titleType;
        }

        public List<PlaylistExist> getPlaylistExists() {
            return playlistExists;
        }

        public void setPlaylistExists(List<PlaylistExist> playlistExists) {
            this.playlistExists = playlistExists;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public UserHistory getUserHistory() {
            return userHistory;
        }

        public void setUserHistory(UserHistory userHistory) {
            this.userHistory = userHistory;
        }

        public enum titleType {
            TAG,
            CATEGORY,
            CHANNEL
        }
    }

    public static class VideoImport implements Serializable {
        @SerializedName("id")
        private String id;
        @SerializedName("video")
        private Video video;
        @SerializedName("torrentName")
        private String torrentName;
        @SerializedName("magnetUri")
        private String magnetUri;
        @SerializedName("targetUri")
        private String targetUri;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Video getVideo() {
            return video;
        }

        public void setVideo(Video video) {
            this.video = video;
        }

        public String getTorrentName() {
            return torrentName;
        }

        public void setTorrentName(String torrentName) {
            this.torrentName = torrentName;
        }

        public String getMagnetUri() {
            return magnetUri;
        }

        public void setMagnetUri(String magnetUri) {
            this.magnetUri = magnetUri;
        }

        public String getTargetUri() {
            return targetUri;
        }

        public void setTargetUri(String targetUri) {
            this.targetUri = targetUri;
        }


    }

    public static class UserHistory implements Serializable {


        @SerializedName("currentTime")
        long currentTime;

        public UserHistory() {
        }

        public long getCurrentTime() {
            return currentTime;
        }

        public void setCurrentTime(long currentTime) {
            this.currentTime = currentTime;
        }

    }


    public static class Description implements Serializable {
        @SerializedName("description")
        private String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }


    public static class VideoExport implements Serializable {

        private int id;
        private String uuid;
        private Video videoData;
        private int playlistDBid;

        public VideoExport() {
        }


        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public Video getVideoData() {
            return videoData;
        }

        public void setVideoData(Video videoData) {
            this.videoData = videoData;
        }

        public int getPlaylistDBid() {
            return playlistDBid;
        }

        public void setPlaylistDBid(int playlistDBid) {
            this.playlistDBid = playlistDBid;
        }

    }
}
