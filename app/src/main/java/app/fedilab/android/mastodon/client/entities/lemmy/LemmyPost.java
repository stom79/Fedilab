package app.fedilab.android.mastodon.client.entities.lemmy;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;

public class LemmyPost implements Serializable {

    @SerializedName("post")
    public Post post;
    @SerializedName("comment")
    public Comment comment;
    @SerializedName("creator")
    public Creator creator;
    @SerializedName("community")
    public Community community;
    @SerializedName("counts")
    public Counts counts;
    @SerializedName("creator_banned_from_community")
    public boolean creator_banned_from_community;
    @SerializedName("saved")
    public boolean saved;
    @SerializedName("read")
    public boolean read;
    @SerializedName("creator_blocked")
    public boolean creator_blocked;
    @SerializedName("unread_comments")
    public int unread_comments;


    public static Status convert(LemmyPost lemmyPost, String instance) {
        Status status = new Status();
        status.id = lemmyPost.comment == null ? lemmyPost.post.id : lemmyPost.comment.id;
        if (lemmyPost.comment != null) {
            status.in_reply_to_id = lemmyPost.comment.post_id;
        }
        status.content = lemmyPost.comment == null ? lemmyPost.post.name : lemmyPost.comment.content;
        status.visibility = "public";
        status.created_at = lemmyPost.comment == null ? lemmyPost.post.published : lemmyPost.comment.published;
        status.url = lemmyPost.comment == null ? lemmyPost.post.ap_id : lemmyPost.comment.ap_id;
        status.uri = lemmyPost.comment == null ? lemmyPost.post.ap_id : lemmyPost.comment.ap_id;


        Account account = new Account();
        account.id = lemmyPost.creator.id;
        account.acct = lemmyPost.creator.name + "@" + instance;
        account.username = "@" + lemmyPost.creator.name;
        account.display_name = lemmyPost.creator.name;
        account.avatar = lemmyPost.creator.avatar;
        account.avatar_static = lemmyPost.creator.avatar;
        status.account = account;

        if (lemmyPost.post.thumbnail_url != null) {
            List<Attachment> attachmentList = new ArrayList<>();
            Attachment attachment = new Attachment();
            attachment.type = "image";
            attachment.url = lemmyPost.post.thumbnail_url;
            attachment.preview_url = lemmyPost.post.thumbnail_url;
            if (lemmyPost.post.nsfw) {
                status.sensitive = true;
            }
            attachmentList.add(attachment);
            status.media_attachments = attachmentList;
        }
        return status;
    }

    public static class Post implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("body")
        public String body;
        @SerializedName("creator_id")
        public String creator_id;
        @SerializedName("community_id")
        public String community_id;
        @SerializedName("removed")
        public boolean removed;
        @SerializedName("locked")
        public boolean locked;
        @SerializedName("published")
        public Date published;
        @SerializedName("updated")
        public Date updated;
        @SerializedName("deleted")
        public boolean deleted;
        @SerializedName("nsfw")
        public boolean nsfw;
        @SerializedName("thumbnail_url")
        public String thumbnail_url;

        @SerializedName("ap_id")
        public String ap_id;
        @SerializedName("local")
        public boolean local;
        @SerializedName("language_id")
        public String language_id;
        @SerializedName("featured_community")
        public boolean featured_community;
        @SerializedName("featured_local")
        public boolean featured_local;
    }

    public static class Comment implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("creator_id")
        public String creator_id;
        @SerializedName("post_id")
        public String post_id;
        @SerializedName("content")
        public String content;
        @SerializedName("removed")
        public boolean removed;
        @SerializedName("published")
        public Date published;
        @SerializedName("thumbnail_url")
        public String thumbnail_url;
        @SerializedName("deleted")
        public boolean deleted;
        @SerializedName("ap_id")
        public String ap_id;
        @SerializedName("local")
        public boolean local;
        @SerializedName("path")
        public String path;
        @SerializedName("distinguished")
        public boolean distinguished;
        @SerializedName("language_id")
        public String language_id;
    }

    public static class Community implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("title")
        public String title;
        @SerializedName("description")
        public String description;
        @SerializedName("removed")
        public boolean removed;
        @SerializedName("published")
        public Date published;
        @SerializedName("updated")
        public Date updated;
        @SerializedName("deleted")
        public boolean deleted;
        @SerializedName("nsfw")
        public boolean nsfw;
        @SerializedName("actor_id")
        public String actor_id;
        @SerializedName("local")
        public boolean local;
        @SerializedName("icon")
        public String icon;
        @SerializedName("hidden")
        public boolean hidden;
        @SerializedName("posting_restricted_to_mods")
        public boolean posting_restricted_to_mods;
        @SerializedName("instance_id")
        public String instance_id;
    }

    public static class Creator implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("avatar")
        public String avatar;
        @SerializedName("banned")
        public boolean banned;
        @SerializedName("published")
        public Date published;
        @SerializedName("actor_id")
        public String actor_id;
        @SerializedName("bio")
        public String bio;
        @SerializedName("local")
        public boolean local;
        @SerializedName("deleted")
        public boolean deleted;
        @SerializedName("matrix_user_id")
        public String matrix_user_id;
        @SerializedName("admin")
        public boolean admin;
        @SerializedName("bot_account")
        public boolean bot_account;
        @SerializedName("instance_id")
        public String instance_id;
    }


    public static class Counts implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("post_id")
        public String post_id;
        @SerializedName("comments")
        public int comments;
        @SerializedName("score")
        public int score;
        @SerializedName("upvotes")
        public int upvotes;
        @SerializedName("downvotes")
        public int downvotes;
        @SerializedName("published")
        public Date published;
        @SerializedName("newest_comment_time_necro")
        public Date newest_comment_time_necro;
        @SerializedName("newest_comment_time")
        public Date newest_comment_time;
        @SerializedName("featured_local")
        public boolean featured_local;
        @SerializedName("hot_rank")
        public int hot_rank;
        @SerializedName("hot_rank_active")
        public int hot_rank_active;
    }
}
