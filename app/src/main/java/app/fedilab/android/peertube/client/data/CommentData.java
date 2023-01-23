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

@SuppressWarnings({"unused", "RedundantSuppression"})
public class CommentData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Comment> data;


    public static class Comment implements Serializable {

        @SerializedName("account")
        private AccountData.PeertubeAccount account;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("deletedAt")
        private Date deletedAt;
        @SerializedName("id")
        private String id;
        @SerializedName("inReplyToCommentId")
        private String inReplyToCommentId;
        @SerializedName("isDeleted")
        private boolean isDeleted;
        @SerializedName("text")
        private String text;
        @SerializedName("threadId")
        private String threadId;
        @SerializedName("totalReplies")
        private int totalReplies;
        @SerializedName("totalRepliesFromVideoAuthor")
        private int totalRepliesFromVideoAuthor;
        @SerializedName("updatedAt")
        private String updatedAt;
        @SerializedName("url")
        private String url;
        @SerializedName("videoId")
        private String videoId;
        private boolean isReply = false;
        private boolean isReplyViewOpen = false;


        public AccountData.PeertubeAccount getAccount() {
            return account;
        }

        public void setAccount(AccountData.PeertubeAccount account) {
            this.account = account;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Date getDeletedAt() {
            return deletedAt;
        }

        public void setDeletedAt(Date deletedAt) {
            this.deletedAt = deletedAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getInReplyToCommentId() {
            return inReplyToCommentId;
        }

        public void setInReplyToCommentId(String inReplyToCommentId) {
            this.inReplyToCommentId = inReplyToCommentId;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public void setDeleted(boolean deleted) {
            isDeleted = deleted;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public int getTotalReplies() {
            return totalReplies;
        }

        public void setTotalReplies(int totalReplies) {
            this.totalReplies = totalReplies;
        }

        public int getTotalRepliesFromVideoAuthor() {
            return totalRepliesFromVideoAuthor;
        }

        public void setTotalRepliesFromVideoAuthor(int totalRepliesFromVideoAuthor) {
            this.totalRepliesFromVideoAuthor = totalRepliesFromVideoAuthor;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        public boolean isReply() {
            return isReply;
        }

        public void setReply(boolean reply) {
            isReply = reply;
        }

        public boolean isReplyViewOpen() {
            return isReplyViewOpen;
        }

        public void setReplyViewOpen(boolean replyViewOpen) {
            isReplyViewOpen = replyViewOpen;
        }
    }


    public static class CommentThreadData {

        @SerializedName("comment")
        public Comment comment;
        @SerializedName("children")
        public List<CommentThreadData> children;

        public Comment getComment() {
            return comment;
        }

        public void setComment(Comment comment) {
            this.comment = comment;
        }

        public List<CommentThreadData> getChildren() {
            return children;
        }

        public void setChildren(List<CommentThreadData> children) {
            this.children = children;
        }
    }

    public static class CommentPosted {
        @SerializedName("comment")
        private Comment comment;

        public Comment getComment() {
            return comment;
        }

        public void setComment(Comment comment) {
            this.comment = comment;
        }
    }


    public static class NotificationComment {
        @SerializedName("id")
        private String id;
        @SerializedName("threadId")
        private String threadId;
        @SerializedName("video")
        private VideoData.Video video;
        @SerializedName("account")
        private AccountData.PeertubeAccount account;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public VideoData.Video getVideo() {
            return video;
        }

        public void setVideo(VideoData.Video video) {
            this.video = video;
        }

        public AccountData.PeertubeAccount getAccount() {
            return account;
        }

        public void setAccount(AccountData.PeertubeAccount account) {
            this.account = account;
        }
    }
}
