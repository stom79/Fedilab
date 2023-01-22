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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.entities.Avatar;
import app.fedilab.android.peertube.client.entities.ItemStr;
import app.fedilab.android.peertube.client.entities.ViewsPerDay;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ChannelData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Channel> data;

    public static class Channel implements Parcelable {
        public static final Creator<Channel> CREATOR = new Creator<Channel>() {
            @Override
            public Channel createFromParcel(Parcel source) {
                return new Channel(source);
            }

            @Override
            public Channel[] newArray(int size) {
                return new Channel[size];
            }
        };
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
        @SerializedName("isLocal")
        private boolean isLocal;
        @SerializedName("name")
        private String name;
        @SerializedName("ownerAccount")
        private AccountData.Account ownerAccount;
        @SerializedName("support")
        private String support;
        @SerializedName("updatedAt")
        private Date updatedAt;
        @SerializedName("url")
        private String url;
        @SerializedName("viewsPerDay")
        private List<ViewsPerDay> viewsPerDays;
        private String acct;
        private boolean selected;

        public Channel() {
        }

        protected Channel(Parcel in) {
            this.avatar = in.readParcelable(Avatar.class.getClassLoader());
            long tmpCreatedAt = in.readLong();
            this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
            this.description = in.readString();
            this.displayName = in.readString();
            this.followersCount = in.readInt();
            this.followingCount = in.readInt();
            this.host = in.readString();
            this.hostRedundancyAllowed = in.readByte() != 0;
            this.id = in.readString();
            this.isLocal = in.readByte() != 0;
            this.name = in.readString();
            this.ownerAccount = in.readParcelable(AccountData.Account.class.getClassLoader());
            this.support = in.readString();
            long tmpUpdatedAt = in.readLong();
            this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
            this.url = in.readString();
            this.viewsPerDays = new ArrayList<>();
            in.readList(this.viewsPerDays, ViewsPerDay.class.getClassLoader());
            this.acct = in.readString();
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

        public String getAcct() {
            return name + "@" + host;
        }

        public void setAcct(String acct) {
            this.acct = acct;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isLocal() {
            return isLocal;
        }

        public void setLocal(boolean local) {
            isLocal = local;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AccountData.Account getOwnerAccount() {
            return ownerAccount;
        }

        public void setOwnerAccount(AccountData.Account ownerAccount) {
            this.ownerAccount = ownerAccount;
        }

        public String getSupport() {
            return support;
        }

        public void setSupport(String support) {
            this.support = support;
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

        public List<ViewsPerDay> getViewsPerDays() {
            return viewsPerDays;
        }

        public void setViewsPerDays(List<ViewsPerDay> viewsPerDays) {
            this.viewsPerDays = viewsPerDays;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.avatar, flags);
            dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
            dest.writeString(this.description);
            dest.writeString(this.displayName);
            dest.writeInt(this.followersCount);
            dest.writeInt(this.followingCount);
            dest.writeString(this.host);
            dest.writeByte(this.hostRedundancyAllowed ? (byte) 1 : (byte) 0);
            dest.writeString(this.id);
            dest.writeByte(this.isLocal ? (byte) 1 : (byte) 0);
            dest.writeString(this.name);
            dest.writeParcelable(this.ownerAccount, flags);
            dest.writeString(this.support);
            dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
            dest.writeString(this.url);
            dest.writeList(this.viewsPerDays);
            dest.writeString(this.acct);
        }
    }

    public static class ChannelCreation {
        @SerializedName("videoChannel")
        private ItemStr videoChannel;

        public ItemStr getVideoChannel() {
            return videoChannel;
        }

        public void setVideoChannel(ItemStr videoChannel) {
            this.videoChannel = videoChannel;
        }
    }
}
