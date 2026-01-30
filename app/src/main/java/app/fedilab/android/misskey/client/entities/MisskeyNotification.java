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
import java.util.Date;

import app.fedilab.android.mastodon.client.entities.api.Notification;

public class MisskeyNotification implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("createdAt")
    public Date createdAt;
    @SerializedName("type")
    public String type;
    @SerializedName("user")
    public MisskeyUser user;
    @SerializedName("userId")
    public String userId;
    @SerializedName("note")
    public MisskeyNote note;
    @SerializedName("reaction")
    public String reaction;

    public Notification toNotification(String instance) {
        Notification notification = new Notification();
        notification.id = this.id;
        notification.created_at = this.createdAt;
        notification.type = mapNotificationType(this.type);

        if (this.user != null) {
            notification.account = this.user.toAccount();
        }

        if (this.note != null) {
            notification.status = this.note.toStatus(instance);
        }

        if (this.reaction != null) {
            notification.emoji = this.reaction;
        }

        return notification;
    }

    private String mapNotificationType(String misskeyType) {
        if (misskeyType == null) {
            return "mention";
        }
        switch (misskeyType) {
            case "follow":
                return "follow";
            case "mention":
            case "reply":
                return "mention";
            case "renote":
                return "reblog";
            case "reaction":
                return "favourite";
            case "quote":
                return "mention";
            case "pollEnded":
                return "poll";
            case "followRequestAccepted":
                return null;
            case "receiveFollowRequest":
                return "follow_request";
            default:
                return "mention";
        }
    }
}
