package app.fedilab.android.mastodon.client.entities.api;
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

import android.app.Activity;
import android.content.Context;
import android.text.Spannable;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.mastodon.helper.SpannableHelper;

public class Account implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("username")
    public String username;
    @SerializedName("acct")
    public String acct;
    @SerializedName("display_name")
    public String display_name;
    @SerializedName("locked")
    public boolean locked;
    @SerializedName("bot")
    public boolean bot;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("note")
    public String note;
    @SerializedName("url")
    public String url;
    @SerializedName("avatar")
    public String avatar;
    @SerializedName("avatar_static")
    public String avatar_static;
    @SerializedName("header")
    public String header;
    @SerializedName("header_static")
    public String header_static;
    @SerializedName("followers_count")
    public long followers_count;
    @SerializedName("following_count")
    public long following_count;
    @SerializedName("statuses_count")
    public long statuses_count;
    @SerializedName("last_status_at")
    public Date last_status_at;
    @SerializedName("source")
    public Source source;
    @SerializedName("emojis")
    public List<Emoji> emojis;
    @SerializedName("fields")
    public List<Field> fields;
    @SerializedName("suspended")
    public boolean suspended;
    @SerializedName("limited")
    public boolean limited;
    @SerializedName("discoverable")
    public boolean discoverable;
    @SerializedName("group")
    public boolean group;
    @SerializedName("mute_expires_at")
    public Date mute_expires_at;
    @SerializedName("moved")
    public Account moved;
    @SerializedName("role")
    public Role role;
    public transient RelationShip relationShip;
    public transient String pronouns = null;


    public synchronized Spannable getSpanDisplayName(Context context, View view) {
        if (display_name == null || display_name.isEmpty()) {
            display_name = username;
        }
        return SpannableHelper.convert(context, display_name, null, this, null, view,  true, false, null);
    }

    public synchronized Spannable getSpanDisplayNameEmoji(Activity activity, View view) {
        if (display_name == null || display_name.isEmpty()) {
            display_name = username;
        }
        return SpannableHelper.convertEmoji(activity, display_name, this, view);
    }

    public synchronized Spannable getSpanDisplayNameTitle(Context context, View view, String title) {
        return SpannableHelper.convert(context, title, null, this, null, view,  true, false, null);
    }

    public synchronized Spannable getSpanNote(Context context, View view, SpannableHelper.Callback callback) {
        return SpannableHelper.convert(context, note, null, this, null, view,  true, false, callback);
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        boolean same = false;
        if (obj instanceof Account) {
            same = this.id.equals(((Account) obj).id);
        }
        return same;
    }

    public static class Role implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("color")
        public String color;
        @SerializedName("position")
        public int position;
        @SerializedName("permissions")
        public int permissions;
        @SerializedName("highlighted")
        public boolean highlighted;
        @SerializedName("created_at")
        public Date created_at;
        @SerializedName("updated_at")
        public Date updated_at;
    }

    public static class AccountParams implements Serializable {
        @SerializedName("discoverable")
        public boolean discoverable;
        @SerializedName("bot")
        public boolean bot;
        @SerializedName("display_name")
        public String display_name;
        @SerializedName("note")
        public String note;
        @SerializedName("locked")
        public boolean locked;
        @SerializedName("source")
        public Source.SourceParams source;
        @SerializedName("fields_attributes")
        public LinkedHashMap<Integer, Field.FieldParams> fields;

    }
}
