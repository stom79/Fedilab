package app.fedilab.android.peertube.client.data;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import app.fedilab.android.peertube.client.entities.ActorFollow;
import app.fedilab.android.peertube.client.entities.VideoAbuse;
import app.fedilab.android.peertube.client.entities.VideoBlacklist;

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
@SuppressWarnings({"unused", "RedundantSuppression"})
public class NotificationData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Notification> data;

    public static class Notification {
        @SerializedName("id")
        private String id;
        @SerializedName("type")
        private int type;
        @SerializedName("read")
        private boolean read;
        @SerializedName("video")
        private VideoData.Video video;
        @SerializedName("videoImport")
        private VideoData.VideoImport videoImport;
        @SerializedName("comment")
        private CommentData.NotificationComment comment;
        @SerializedName("videoAbuse")
        private VideoAbuse videoAbuse;
        @SerializedName("abuse")
        private VideoAbuse.Abuse abuse;
        @SerializedName("videoBlacklist")
        private VideoBlacklist videoBlacklist;
        @SerializedName("account")
        private AccountData.Account account;
        @SerializedName("actorFollow")
        private ActorFollow actorFollow;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("updatedAt")
        private Date updatedAt;


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        public VideoData.Video getVideo() {
            return video;
        }

        public void setVideo(VideoData.Video video) {
            this.video = video;
        }

        public VideoData.VideoImport getVideoImport() {
            return videoImport;
        }

        public void setVideoImport(VideoData.VideoImport videoImport) {
            this.videoImport = videoImport;
        }

        public CommentData.NotificationComment getComment() {
            return comment;
        }

        public void setComment(CommentData.NotificationComment comment) {
            this.comment = comment;
        }

        public VideoAbuse getVideoAbuse() {
            return videoAbuse;
        }

        public void setVideoAbuse(VideoAbuse videoAbuse) {
            this.videoAbuse = videoAbuse;
        }

        public VideoBlacklist getVideoBlacklist() {
            return videoBlacklist;
        }

        public void setVideoBlacklist(VideoBlacklist videoBlacklist) {
            this.videoBlacklist = videoBlacklist;
        }

        public AccountData.Account getAccount() {
            return account;
        }

        public void setAccount(AccountData.Account account) {
            this.account = account;
        }

        public ActorFollow getActorFollow() {
            return actorFollow;
        }

        public void setActorFollow(ActorFollow actorFollow) {
            this.actorFollow = actorFollow;
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

        public VideoAbuse.Abuse getAbuse() {
            return abuse;
        }

        public void setAbuse(VideoAbuse.Abuse abuse) {
            this.abuse = abuse;
        }
    }
}
