package app.fedilab.android.client.entities;
/* Copyright 2022 Thomas Schneider
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
import java.util.List;

public class PostState implements Serializable {

    @SerializedName("number_of_posts")
    public int number_of_posts;
    @SerializedName("posts_successfully_sent")
    public int posts_successfully_sent;
    @SerializedName("posts")
    public List<Post> posts;

    public static class Post implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("in_reply_to_id")
        public String in_reply_to_id;
        @SerializedName("number_of_media")
        public int number_of_media;
    }

}
