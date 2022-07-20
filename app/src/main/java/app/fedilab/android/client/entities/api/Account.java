package app.fedilab.android.client.entities.api;
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

import android.content.Context;
import android.text.Spannable;
import android.view.View;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.helper.SpannableHelper;

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
    public int followers_count;
    @SerializedName("following_count")
    public int following_count;
    @SerializedName("statuses_count")
    public int statuses_count;
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
    @SerializedName("discoverable")
    public boolean discoverable;
    @SerializedName("mute_expires_at")
    public Date mute_expires_at;
    @SerializedName("moved")
    public Account moved;

    public synchronized Spannable getSpanDisplayName(Context context, WeakReference<View> viewWeakReference) {
        if (display_name == null || display_name.isEmpty()) {
            display_name = username;
        }
        return SpannableHelper.convert(context, display_name, null, this, null, false, viewWeakReference);
    }

    public synchronized Spannable getSpanDisplayNameTitle(Context context, WeakReference<View> viewWeakReference, String title) {
        return SpannableHelper.convert(context, title, null, this, null, false, viewWeakReference);
    }


    public synchronized Spannable getSpanNote(Context context, WeakReference<View> viewWeakReference) {
        return SpannableHelper.convert(context, note, null, this, null, true, viewWeakReference);
    }

    public transient RelationShip relationShip;


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
