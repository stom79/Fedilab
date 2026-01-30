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

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.Field;

public class MisskeyUser implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("username")
    public String username;
    @SerializedName("host")
    public String host;
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
    @SerializedName("avatarUrl")
    public String avatarUrl;
    @SerializedName("avatarBlurhash")
    public String avatarBlurhash;
    @SerializedName("bannerUrl")
    public String bannerUrl;
    @SerializedName("bannerBlurhash")
    public String bannerBlurhash;
    @SerializedName("isLocked")
    public boolean isLocked;
    @SerializedName("isBot")
    public boolean isBot;
    @SerializedName("isCat")
    public boolean isCat;
    @SerializedName("isSuspended")
    public boolean isSuspended;
    @SerializedName("createdAt")
    public Date createdAt;
    @SerializedName("followersCount")
    public int followersCount;
    @SerializedName("followingCount")
    public int followingCount;
    @SerializedName("notesCount")
    public int notesCount;
    @SerializedName("emojis")
    public Object emojis;
    @SerializedName("fields")
    public List<MisskeyField> fields;
    @SerializedName("pinnedNoteIds")
    public List<String> pinnedNoteIds;
    @SerializedName("pinnedNotes")
    public List<MisskeyNote> pinnedNotes;
    @SerializedName("isFollowing")
    public boolean isFollowing;
    @SerializedName("isFollowed")
    public boolean isFollowed;
    @SerializedName("hasPendingFollowRequestFromYou")
    public boolean hasPendingFollowRequestFromYou;
    @SerializedName("hasPendingFollowRequestToYou")
    public boolean hasPendingFollowRequestToYou;
    @SerializedName("isBlocking")
    public boolean isBlocking;
    @SerializedName("isBlocked")
    public boolean isBlocked;
    @SerializedName("isMuted")
    public boolean isMuted;

    public static class MisskeyField implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("value")
        public String value;
    }

    public Account toAccount() {
        Account account = new Account();
        account.id = this.id;
        account.username = this.username;
        account.acct = this.host != null ? this.username + "@" + this.host : this.username;
        account.display_name = this.name != null ? this.name : this.username;
        account.locked = this.isLocked;
        account.bot = this.isBot;
        account.created_at = this.createdAt;
        account.note = this.description;
        account.url = null;
        account.avatar = this.avatarUrl;
        account.avatar_static = this.avatarUrl;
        account.header = this.bannerUrl;
        account.header_static = this.bannerUrl;
        account.followers_count = this.followersCount;
        account.following_count = this.followingCount;
        account.statuses_count = this.notesCount;
        account.suspended = this.isSuspended;
        account.emojis = convertEmojis(this.emojis);
        if (this.fields != null) {
            account.fields = new java.util.ArrayList<>();
            for (MisskeyField misskeyField : this.fields) {
                Field field = new Field();
                field.name = misskeyField.name;
                field.value = misskeyField.value;
                account.fields.add(field);
            }
        }
        return account;
    }

    @SuppressWarnings("unchecked")
    public static List<Emoji> convertEmojis(Object emojis) {
        if (emojis == null) {
            return null;
        }
        List<Emoji> emojiList = new ArrayList<>();
        if (emojis instanceof Map) {
            Map<String, Object> emojiMap = (Map<String, Object>) emojis;
            for (Map.Entry<String, Object> entry : emojiMap.entrySet()) {
                Emoji emoji = new Emoji();
                emoji.shortcode = entry.getKey();
                emoji.url = String.valueOf(entry.getValue());
                emoji.static_url = emoji.url;
                emoji.visible_in_picker = true;
                emojiList.add(emoji);
            }
        } else if (emojis instanceof List) {
            for (Object obj : (List<?>) emojis) {
                if (obj instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    Emoji emoji = new Emoji();
                    emoji.shortcode = map.get("name") != null ? String.valueOf(map.get("name")) : null;
                    emoji.url = map.get("url") != null ? String.valueOf(map.get("url")) : null;
                    emoji.static_url = emoji.url;
                    emoji.visible_in_picker = true;
                    emojiList.add(emoji);
                }
            }
        }
        return emojiList.isEmpty() ? null : emojiList;
    }
}
