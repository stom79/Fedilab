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

import android.content.Context;
import android.text.Spannable;
import android.view.View;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.helper.SpannableHelper;

public class Poll implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("expires_at")
    public Date expires_at;
    @SerializedName("expire_in")
    public int expire_in;
    @SerializedName("expired")
    public boolean expired;
    @SerializedName("multiple")
    public boolean multiple;
    @SerializedName("votes_count")
    public int votes_count;
    @SerializedName("voters_count")
    public int voters_count;
    @SerializedName("voted")
    public boolean voted;
    @SerializedName("own_votes")
    public List<Integer> own_votes;
    @SerializedName("options")
    public List<PollItem> options;
    @SerializedName("emojis")
    public List<Emoji> emojis;

    public static class PollItem implements Serializable {
        @SerializedName("title")
        public String title;
        @SerializedName("votes_count")
        public int votes_count;

        public transient Spannable span_title;

        public Spannable getSpanTitle(Context context, Status status, WeakReference<View> viewWeakReference) {
            span_title = SpannableHelper.convert(context, title, status, null, null, viewWeakReference, null, false, false);
            return span_title;
        }
    }
}
