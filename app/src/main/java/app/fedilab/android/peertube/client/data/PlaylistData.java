package app.fedilab.android.peertube.client.data;
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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.entities.Item;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class PlaylistData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Playlist> data;

    public static class Playlist implements Serializable {

        @SerializedName("id")
        private String id;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("updatedAt")
        private Date updatedAt;
        @SerializedName("description")
        private String description;
        @SerializedName("uuid")
        private String uuid;
        @SerializedName("displayName")
        private String displayName;
        @SerializedName("isLocal")
        private boolean isLocal;
        @SerializedName("videoLength")
        private long videoLength;
        @SerializedName("thumbnailPath")
        private String thumbnailPath;
        @SerializedName("privacy")
        private Item privacy;
        @SerializedName("type")
        private Item type;
        @SerializedName("ownerAccount")
        private AccountData.PeertubeAccount ownerAccount;
        @SerializedName("videoChannel")
        private ChannelData.Channel videoChannel;

        public Playlist() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isLocal() {
            return isLocal;
        }

        public void setLocal(boolean local) {
            isLocal = local;
        }

        public long getVideoLength() {
            return videoLength;
        }

        public void setVideoLength(long videoLength) {
            this.videoLength = videoLength;
        }

        public String getThumbnailPath() {
            return thumbnailPath;
        }

        public void setThumbnailPath(String thumbnailPath) {
            this.thumbnailPath = thumbnailPath;
        }

        public Item getPrivacy() {
            return privacy;
        }

        public void setPrivacy(Item privacy) {
            this.privacy = privacy;
        }

        public Item getType() {
            return type;
        }

        public void setType(Item type) {
            this.type = type;
        }

        public AccountData.PeertubeAccount getOwnerAccount() {
            return ownerAccount;
        }

        public void setOwnerAccount(AccountData.PeertubeAccount ownerAccount) {
            this.ownerAccount = ownerAccount;
        }

        public ChannelData.Channel getVideoChannel() {
            return videoChannel;
        }

        public void setVideoChannel(ChannelData.Channel videoChannel) {
            this.videoChannel = videoChannel;
        }

    }

}
