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

import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.entities.Item;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class PlaylistData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Playlist> data;

    public static class Playlist implements Parcelable {
        public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
            @Override
            public Playlist createFromParcel(Parcel source) {
                return new Playlist(source);
            }

            @Override
            public Playlist[] newArray(int size) {
                return new Playlist[size];
            }
        };
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
        private AccountData.Account ownerAccount;
        @SerializedName("videoChannel")
        private ChannelData.Channel videoChannel;

        public Playlist() {
        }

        protected Playlist(Parcel in) {
            this.id = in.readString();
            long tmpCreatedAt = in.readLong();
            this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
            long tmpUpdatedAt = in.readLong();
            this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
            this.description = in.readString();
            this.uuid = in.readString();
            this.displayName = in.readString();
            this.isLocal = in.readByte() != 0;
            this.videoLength = in.readLong();
            this.thumbnailPath = in.readString();
            this.privacy = in.readParcelable(Item.class.getClassLoader());
            this.type = in.readParcelable(Item.class.getClassLoader());
            this.ownerAccount = in.readParcelable(AccountData.Account.class.getClassLoader());
            this.videoChannel = in.readParcelable(ChannelData.Channel.class.getClassLoader());
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

        public AccountData.Account getOwnerAccount() {
            return ownerAccount;
        }

        public void setOwnerAccount(AccountData.Account ownerAccount) {
            this.ownerAccount = ownerAccount;
        }

        public ChannelData.Channel getVideoChannel() {
            return videoChannel;
        }

        public void setVideoChannel(ChannelData.Channel videoChannel) {
            this.videoChannel = videoChannel;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.id);
            dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
            dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
            dest.writeString(this.description);
            dest.writeString(this.uuid);
            dest.writeString(this.displayName);
            dest.writeByte(this.isLocal ? (byte) 1 : (byte) 0);
            dest.writeLong(this.videoLength);
            dest.writeString(this.thumbnailPath);
            dest.writeParcelable(this.privacy, flags);
            dest.writeParcelable(this.type, flags);
            dest.writeParcelable(this.ownerAccount, flags);
            dest.writeParcelable(this.videoChannel, flags);
        }
    }

}
