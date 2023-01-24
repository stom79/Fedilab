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


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.entities.Avatar;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class AccountData implements Serializable {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<PeertubeAccount> data;

    public static class PeertubeAccount implements Serializable {
        @SerializedName("avatar")
        private Avatar avatar;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("description")
        private String description;
        @SerializedName("displayName")
        private String displayName;
        @SerializedName("followersCount")
        private int followersCount;
        @SerializedName("followingCount")
        private int followingCount;
        @SerializedName("host")
        private String host;
        @SerializedName("hostRedundancyAllowed")
        private boolean hostRedundancyAllowed;
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private String name;
        @SerializedName("username")
        private String username;
        @SerializedName("updatedAt")
        private Date updatedAt;
        @SerializedName("url")
        private String url;
        @SerializedName("userId")
        private String userId;


        public PeertubeAccount() {
        }


        public Avatar getAvatar() {
            return avatar;
        }

        public void setAvatar(Avatar avatar) {
            this.avatar = avatar;
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

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getFollowersCount() {
            return followersCount;
        }

        public void setFollowersCount(int followersCount) {
            this.followersCount = followersCount;
        }

        public int getFollowingCount() {
            return followingCount;
        }

        public void setFollowingCount(int followingCount) {
            this.followingCount = followingCount;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public boolean isHostRedundancyAllowed() {
            return hostRedundancyAllowed;
        }

        public void setHostRedundancyAllowed(boolean hostRedundancyAllowed) {
            this.hostRedundancyAllowed = hostRedundancyAllowed;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return name != null ? name : username;
        }

        public void setUsername(String name) {
            this.name = name;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAcct() {
            return name + "@" + host;
        }


        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

    }
}
