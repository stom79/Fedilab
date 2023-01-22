package app.fedilab.android.mastodon.client.entities.app;
/* Copyright 2022 Thomas Schneider
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
import java.util.List;

public class BubbleTimeline implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("only_media")
    public boolean only_media = false;
    @SerializedName("remote")
    public boolean remote = false;
    @SerializedName("with_muted")
    public boolean with_muted;
    @SerializedName("exclude_visibilities")
    public List<String> exclude_visibilities = null;
    @SerializedName("reply_visibility")
    public String reply_visibility = null;
}
