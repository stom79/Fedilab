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

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import app.fedilab.android.helper.SpannableHelper;

public class Announcement {
    @SerializedName("id")
    public String id;
    @SerializedName("content")
    public String content;
    @SerializedName("starts_at")
    public Date starts_at;
    @SerializedName("ends_at")
    public Date ends_at;
    @SerializedName("all_day")
    public boolean all_day;
    @SerializedName("published_at")
    public Date published_at;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("read")
    public boolean read;
    @SerializedName("mentions")
    public List<Mention> mentions;
    @SerializedName("statuses")
    public List<Status> statuses;
    @SerializedName("tags")
    public List<Tag> tags;
    @SerializedName("emojis")
    public List<Emoji> emojis;
    @SerializedName("reactions")
    public List<Reaction> reactions;


    public synchronized Spannable getSpanContent(Context context, WeakReference<View> viewWeakReference) {
        return SpannableHelper.convert(context, content, null, null, this, true, viewWeakReference);
    }

}
