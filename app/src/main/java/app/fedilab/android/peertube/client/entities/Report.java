package app.fedilab.android.peertube.client.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

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
public class Report {

    @SerializedName("reason")
    private String reason;
    @SerializedName("predefinedReasons")
    private List<String> predefinedReasons;
    @SerializedName("video")
    private VideoReport video;
    @SerializedName("comment")
    private CommentReport comment;
    @SerializedName("account")
    private AccountReport account;


    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getPredefinedReasons() {
        return predefinedReasons;
    }

    public void setPredefinedReasons(List<String> predefinedReasons) {
        this.predefinedReasons = predefinedReasons;
    }

    public VideoReport getVideo() {
        return video;
    }

    public void setVideo(VideoReport video) {
        this.video = video;
    }

    public CommentReport getComment() {
        return comment;
    }

    public void setComment(CommentReport comment) {
        this.comment = comment;
    }

    public AccountReport getAccount() {
        return account;
    }

    public void setAccount(AccountReport account) {
        this.account = account;
    }

    public static class VideoReport {
        @SerializedName("id")
        private String id;
        @SerializedName("startAt")
        private long startAt;
        @SerializedName("endAt")
        private long endAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getStartAt() {
            return startAt;
        }

        public void setStartAt(long startAt) {
            this.startAt = startAt;
        }

        public long getEndAt() {
            return endAt;
        }

        public void setEndAt(long endAt) {
            this.endAt = endAt;
        }
    }

    public static class CommentReport {
        @SerializedName("id")
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class AccountReport {
        @SerializedName("id")
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class ReportReturn {
        @SerializedName("abuse")
        private ItemStr reply;

        public ItemStr getItemStr() {
            return reply;
        }

        public void setItemStr(ItemStr itemStr) {
            this.reply = itemStr;
        }
    }
}
