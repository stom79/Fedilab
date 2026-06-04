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

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Collection implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("account_id")
    public String account_id;
    @SerializedName("uri")
    public String uri;
    @SerializedName("url")
    public String url;
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
    @SerializedName("language")
    public String language;
    @SerializedName("local")
    public boolean local;
    @SerializedName("sensitive")
    public boolean sensitive;
    @SerializedName("discoverable")
    public boolean discoverable;
    @SerializedName("tag")
    public Tag tag;
    @SerializedName("item_count")
    public int item_count;
    @SerializedName("items")
    public List<CollectionItem> items;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;

    public transient List<Account> previewAccounts;

    public static class CollectionItem implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("account_id")
        public String account_id;
        @SerializedName("state")
        public String state;
        @SerializedName("created_at")
        public Date created_at;
    }

    public static class WrappedCollection implements Serializable {
        @SerializedName("collection")
        public Collection collection;
    }

    public static class WrappedCollectionItem implements Serializable {
        @SerializedName("collection_item")
        public CollectionItem collection_item;
    }

    public static class CollectionWithAccounts implements Serializable {
        @SerializedName("collection")
        public Collection collection;
        @SerializedName("accounts")
        public List<Account> accounts;
    }

    public static class CollectionList implements Serializable {
        @SerializedName("collections")
        public List<Collection> collections;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean same = false;
        if (obj instanceof Collection) {
            same = this.id.equals(((Collection) obj).id);
        }
        return same;
    }
}
