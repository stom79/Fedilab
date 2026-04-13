package app.fedilab.android.mastodon.client.entities.api;
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

public class NotificationPolicy implements Serializable {

    @SerializedName("for_not_following")
    public String for_not_following;
    @SerializedName("for_not_followers")
    public String for_not_followers;
    @SerializedName("for_new_accounts")
    public String for_new_accounts;
    @SerializedName("for_private_mentions")
    public String for_private_mentions;
    @SerializedName("for_limited_accounts")
    public String for_limited_accounts;
    @SerializedName("summary")
    public Summary summary;

    public static class Summary implements Serializable {
        @SerializedName("pending_requests_count")
        public int pending_requests_count;
        @SerializedName("pending_notifications_count")
        public int pending_notifications_count;
    }
}
