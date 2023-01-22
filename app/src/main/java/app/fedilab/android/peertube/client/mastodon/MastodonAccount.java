package app.fedilab.android.peertube.client.mastodon;
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

import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.entities.Avatar;


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MastodonAccount {


    public static AccountData.Account convertToPeertubeAccount(Account initialAccount) {
        AccountData.Account account = new AccountData.Account();
        Avatar avatar = new Avatar();
        avatar.setPath(initialAccount.getAvatar());
        account.setAvatar(avatar);
        account.setDescription(initialAccount.getDescription());
        account.setDisplayName(initialAccount.getDisplayName());
        account.setUsername(initialAccount.getUsername());
        account.setHost(initialAccount.getHost());
        return account;
    }

    public static class Account implements Parcelable {
        public static final Creator<Account> CREATOR = new Creator<Account>() {
            @Override
            public Account createFromParcel(Parcel source) {
                return new Account(source);
            }

            @Override
            public Account[] newArray(int size) {
                return new Account[size];
            }
        };
        @SerializedName("avatar")
        private String avatar;
        @SerializedName("created_at")
        private Date createdAt;
        @SerializedName("note")
        private String description;
        @SerializedName("display_name")
        private String displayName;
        @SerializedName("followers_count")
        private int followersCount;
        @SerializedName("following_count")
        private int followingCount;
        @SerializedName("id")
        private String id;
        @SerializedName("username")
        private String username;
        @SerializedName("updated_at")
        private Date updatedAt;
        @SerializedName("url")
        private String url;
        private String token;
        private String client_id;
        private String client_secret;
        private String refresh_token;
        private String software;
        private String host;

        public Account() {
        }

        protected Account(Parcel in) {
            this.avatar = in.readString();
            long tmpCreatedAt = in.readLong();
            this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
            this.description = in.readString();
            this.displayName = in.readString();
            this.followersCount = in.readInt();
            this.followingCount = in.readInt();
            this.id = in.readString();
            this.username = in.readString();
            long tmpUpdatedAt = in.readLong();
            this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
            this.url = in.readString();
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
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


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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
            return username + "@" + host;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getClient_id() {
            return client_id;
        }

        public void setClient_id(String client_id) {
            this.client_id = client_id;
        }

        public String getClient_secret() {
            return client_secret;
        }

        public void setClient_secret(String client_secret) {
            this.client_secret = client_secret;
        }

        public String getRefresh_token() {
            return refresh_token;
        }

        public void setRefresh_token(String refresh_token) {
            this.refresh_token = refresh_token;
        }


        public String getSoftware() {
            return software;
        }

        public void setSoftware(String software) {
            this.software = software;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.avatar);
            dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
            dest.writeString(this.description);
            dest.writeString(this.displayName);
            dest.writeInt(this.followersCount);
            dest.writeInt(this.followingCount);
            dest.writeString(this.id);
            dest.writeString(this.username);
            dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
            dest.writeString(this.url);
        }
    }
}
