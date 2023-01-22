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

import app.fedilab.android.peertube.client.data.CommentData;
import app.fedilab.android.peertube.client.data.VideoData;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class VideoAbuse {

    @SerializedName("id")
    private String id;
    @SerializedName("video")
    private VideoData.Video video;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public VideoData.Video getVideo() {
        return video;
    }

    public void setVideo(VideoData.Video video) {
        this.video = video;
    }


    public static class Abuse {
        @SerializedName("comment")
        private CommentData.Comment comment;
        @SerializedName("threadId")
        private String threadId;
        @SerializedName("id")
        private String id;

        public CommentData.Comment getComment() {
            return comment;
        }

        public void setComment(CommentData.Comment comment) {
            this.comment = comment;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
