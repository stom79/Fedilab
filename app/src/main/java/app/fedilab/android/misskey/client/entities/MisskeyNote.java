package app.fedilab.android.misskey.client.entities;
/* Copyright 2026 Thomas Schneider
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
import java.util.Map;

import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Mention;
import app.fedilab.android.mastodon.client.entities.api.Reaction;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Tag;

public class MisskeyNote implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("createdAt")
    public Date createdAt;
    @SerializedName("text")
    public String text;
    @SerializedName("cw")
    public String cw;
    @SerializedName("userId")
    public String userId;
    @SerializedName("user")
    public MisskeyUser user;
    @SerializedName("replyId")
    public String replyId;
    @SerializedName("renoteId")
    public String renoteId;
    @SerializedName("reply")
    public MisskeyNote reply;
    @SerializedName("renote")
    public MisskeyNote renote;
    @SerializedName("visibility")
    public String visibility;
    @SerializedName("localOnly")
    public boolean localOnly;
    @SerializedName("uri")
    public String uri;
    @SerializedName("url")
    public String url;
    @SerializedName("repliesCount")
    public int repliesCount;
    @SerializedName("renoteCount")
    public int renoteCount;
    @SerializedName("reactions")
    public Map<String, Integer> reactions;
    @SerializedName("reactionEmojis")
    public Map<String, String> reactionEmojis;
    @SerializedName("emojis")
    public Object emojis;
    @SerializedName("tags")
    public List<String> tags;
    @SerializedName("fileIds")
    public List<String> fileIds;
    @SerializedName("files")
    public List<MisskeyFile> files;
    @SerializedName("poll")
    public MisskeyPoll poll;
    @SerializedName("mentions")
    public List<String> mentions;
    @SerializedName("myReaction")
    public String myReaction;
    @SerializedName("isHidden")
    public boolean isHidden;

    public Status toStatus(String instance) {
        Status status = new Status();
        status.id = this.id;
        status.created_at = this.createdAt;
        status.in_reply_to_id = this.replyId;
        status.sensitive = this.cw != null;
        status.spoiler_text = this.cw;
        status.visibility = mapVisibility(this.visibility);
        status.uri = this.uri;
        status.url = this.url != null ? this.url : "https://" + instance + "/notes/" + this.id;
        status.replies_count = this.repliesCount;
        status.reblogs_count = this.renoteCount;
        status.local_only = this.localOnly;

        status.content = this.text != null ? formatContent(this.text) : "";
        status.text = this.text;

        if (this.reactions != null) {
            int totalReactions = 0;
            status.reactions = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : this.reactions.entrySet()) {
                totalReactions += entry.getValue();
                Reaction reaction = new Reaction();
                reaction.name = entry.getKey();
                reaction.count = entry.getValue();
                reaction.me = this.myReaction != null && this.myReaction.equals(entry.getKey());
                if (this.reactionEmojis != null) {
                    String raw = entry.getKey().replace(":", "");
                    String emojiUrl = this.reactionEmojis.get(raw);
                    if (emojiUrl == null && raw.endsWith("@.")) {
                        emojiUrl = this.reactionEmojis.get(raw.substring(0, raw.length() - 2));
                    }
                    if (emojiUrl == null && !raw.endsWith("@.")) {
                        emojiUrl = this.reactionEmojis.get(raw + "@.");
                    }
                    if (emojiUrl != null) {
                        reaction.url = emojiUrl;
                        reaction.static_url = emojiUrl;
                    }
                }
                status.reactions.add(reaction);
            }
            status.favourites_count = totalReactions;
        }

        status.favourited = this.myReaction != null;
        status.reblogged = false;
        status.bookmarked = false;

        if (this.user != null) {
            status.account = this.user.toAccount();
        }

        if (this.files != null && !this.files.isEmpty()) {
            status.media_attachments = new ArrayList<>();
            for (MisskeyFile file : this.files) {
                status.media_attachments.add(file.toAttachment());
            }
        }

        if (this.tags != null && !this.tags.isEmpty()) {
            status.tags = new ArrayList<>();
            for (String tagName : this.tags) {
                Tag tag = new Tag();
                tag.name = tagName;
                tag.url = "https://" + instance + "/tags/" + tagName;
                status.tags.add(tag);
            }
        }

        status.emojis = MisskeyUser.convertEmojis(this.emojis);

        if (this.poll != null) {
            status.poll = this.poll.toPoll(this.id);
        }

        if (this.renote != null && this.text == null) {
            status.reblog = this.renote.toStatus(instance);
        }

        if (this.mentions != null && !this.mentions.isEmpty()) {
            status.mentions = new ArrayList<>();
            for (String mentionId : this.mentions) {
                Mention mention = new Mention();
                mention.id = mentionId;
                status.mentions.add(mention);
            }
        }

        return status;
    }

    private String mapVisibility(String misskeyVisibility) {
        if (misskeyVisibility == null) {
            return "public";
        }
        switch (misskeyVisibility) {
            case "public":
                return "public";
            case "home":
                return "unlisted";
            case "followers":
                return "private";
            case "specified":
                return "direct";
            default:
                return "public";
        }
    }

    private String formatContent(String text) {
        if (text == null) {
            return "";
        }
        String content = text;
        content = content.replace("\n", "<br>");
        content = content.replaceAll("(https?://[\\w/:%#$&?()~.=+\\-]+)", "<a href=\"$1\">$1</a>");
        content = content.replaceAll("@([\\w_]+)(@[\\w.]+)?", "<span class=\"h-card\"><a href=\"#\" class=\"u-url mention\">@<span>$1</span></a></span>");
        content = content.replaceAll("#([\\w_]+)", "<a href=\"#\" class=\"mention hashtag\">#<span>$1</span></a>");
        return "<p>" + content + "</p>";
    }
}
