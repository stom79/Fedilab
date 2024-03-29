package app.fedilab.android.mastodon.client.entities.api;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/* Copyright 2021 Thomas Schneider
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
public class Conversation {
    @SerializedName("id")
    public String id;
    @SerializedName("unread")
    public boolean unread;
    @SerializedName("accounts")
    public List<Account> accounts;
    @SerializedName("last_status")
    public Status last_status;
    public boolean isFetchMore = false;
    @SerializedName("cached")
    public boolean cached = false;


    public PositionFetchMore positionFetchMore = PositionFetchMore.BOTTOM;

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean same = false;
        if (obj instanceof Conversation) {
            same = this.id.equals(((Conversation) obj).id);
        }
        return same;
    }

    public enum PositionFetchMore {
        TOP,
        BOTTOM
    }

}
