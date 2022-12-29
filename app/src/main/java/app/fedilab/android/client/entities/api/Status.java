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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import app.fedilab.android.helper.SpannableHelper;

public class Status implements Serializable, Cloneable {

    @SerializedName("id")
    public String id;
    @SerializedName("created_at")
    public Date created_at = new Date();
    @SerializedName("edited_at")
    public Date edited_at;
    @SerializedName("in_reply_to_id")
    public String in_reply_to_id;
    @SerializedName("in_reply_to_account_id")
    public String in_reply_to_account_id;
    @SerializedName("sensitive")
    public boolean sensitive;
    @SerializedName("spoiler_text")
    public String spoiler_text;
    @SerializedName("text")
    public String text;
    @SerializedName("quote_id")
    public String quote_id;
    @SerializedName("visibility")
    public String visibility;
    @SerializedName("language")
    public String language;
    @SerializedName("uri")
    public String uri;
    @SerializedName("url")
    public String url;
    @SerializedName("replies_count")
    public int replies_count;
    @SerializedName("reblogs_count")
    public int reblogs_count;
    @SerializedName("favourites_count")
    public int favourites_count;
    @SerializedName("favourited")
    public boolean favourited;
    @SerializedName("reblogged")
    public boolean reblogged;
    @SerializedName("muted")
    public boolean muted;
    @SerializedName("bookmarked")
    public boolean bookmarked;
    @SerializedName("pinned")
    public boolean pinned;
    @SerializedName("content")
    public String content;
    @SerializedName("reblog")
    public Status reblog;
    @SerializedName("quote")
    public Status quote;
    @SerializedName("application")
    public App application;
    @SerializedName("account")
    public Account account;
    @SerializedName("media_attachments")
    public List<Attachment> media_attachments;
    @SerializedName("mentions")
    public List<Mention> mentions;
    @SerializedName("tags")
    public List<Tag> tags;
    @SerializedName("emojis")
    public List<Emoji> emojis;
    @SerializedName("card")
    public Card card;
    @SerializedName("poll")
    public Poll poll;
    @SerializedName("filtered")
    public List<Filter.FilterResult> filtered;
    @SerializedName("pleroma")
    public Pleroma pleroma;
    @SerializedName("cached")
    public boolean cached = false;
    public Attachment art_attachment;
    public boolean isExpended = false;
    public boolean isTruncated = true;
    public boolean isFetchMore = false;
    public PositionFetchMore positionFetchMore = PositionFetchMore.BOTTOM;
    public boolean isChecked = false;
    public String translationContent;
    public boolean translationShown;
    public boolean canLoadMedia = false;
    public transient boolean isFocused = false;
    public transient boolean setCursorToEnd = false;
    public transient int cursorPosition = 0;
    public transient boolean submitted = false;
    public boolean spoilerChecked = false;
    public Filter filteredByApp;
    public transient Spannable contentSpan;
    public transient Spannable contentSpoilerSpan;
    public transient Spannable contentTranslateSpan;

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean same = false;
        if (obj instanceof Status) {
            same = this.id.equals(((Status) obj).id);
        }
        return same;
    }

    public synchronized Spannable getSpanContent(Context context, WeakReference<View> viewWeakReference, Callback callback) {
        if (contentSpan == null) {
            contentSpan = SpannableHelper.convert(context, content, this, null, null, true, false, viewWeakReference, callback);
        }
        return contentSpan;
    }

    public synchronized Spannable getSpanSpoiler(Context context, WeakReference<View> viewWeakReference, Callback callback) {
        if (contentSpoilerSpan == null) {
            contentSpoilerSpan = SpannableHelper.convert(context, spoiler_text, this, null, null, true, false, viewWeakReference, callback);
        }
        return contentSpoilerSpan;
    }

    public synchronized Spannable getSpanTranslate(Context context, WeakReference<View> viewWeakReference, Callback callback) {
        if (contentTranslateSpan == null) {
            contentTranslateSpan = SpannableHelper.convert(context, translationContent, this, null, null, true, false, viewWeakReference, callback);
        }
        return contentTranslateSpan;
    }

    public interface Callback {
        void emojiFetched();
    }

    @NonNull
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public enum PositionFetchMore {
        TOP,
        BOTTOM
    }

}
