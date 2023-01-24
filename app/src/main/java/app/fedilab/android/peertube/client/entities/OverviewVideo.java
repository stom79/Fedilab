package app.fedilab.android.peertube.client.entities;
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

import java.util.List;

import app.fedilab.android.peertube.client.data.ChannelData.Channel;
import app.fedilab.android.peertube.client.data.VideoData.Video;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class OverviewVideo {

    @SerializedName("categories")
    private List<Categories> categories;
    @SerializedName("channels")
    private List<Channels> channels;
    @SerializedName("tags")
    private List<Tags> tags;


    public List<Categories> getCategories() {
        return categories;
    }

    public void setCategories(List<Categories> categories) {
        this.categories = categories;
    }

    public List<Channels> getChannels() {
        return channels;
    }

    public void setChannels(List<Channels> channels) {
        this.channels = channels;
    }

    public List<Tags> getTags() {
        return tags;
    }

    public void setTags(List<Tags> tags) {
        this.tags = tags;
    }

    public static class Categories {
        @SerializedName("category")
        private Item category;
        @SerializedName("videos")
        private List<Video> videos;

        public Item getCategory() {
            return category;
        }

        public void setCategory(Item category) {
            this.category = category;
        }

        public List<Video> getVideos() {
            return videos;
        }

        public void setVideos(List<Video> videos) {
            this.videos = videos;
        }
    }

    public static class Channels {
        @SerializedName("channels")
        private Channel channels;
        @SerializedName("videos")
        private List<Video> videos;

        public Channel getChannels() {
            return channels;
        }

        public void setChannels(Channel channels) {
            this.channels = channels;
        }

        public List<Video> getVideos() {
            return videos;
        }

        public void setVideos(List<Video> videos) {
            this.videos = videos;
        }
    }

    public static class Tags {
        @SerializedName("tag")
        private String tag;
        @SerializedName("videos")
        private List<Video> videos;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public List<Video> getVideos() {
            return videos;
        }

        public void setVideos(List<Video> videos) {
            this.videos = videos;
        }
    }
}
