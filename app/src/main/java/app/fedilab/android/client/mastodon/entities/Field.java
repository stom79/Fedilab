package app.fedilab.android.client.mastodon.entities;
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

import android.text.Spannable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class Field implements Serializable {
    @SerializedName("name")
    public String name;
    @SerializedName("value")
    public String value;
    @SerializedName("verified_at")
    public Date verified_at;

    //Some extra spannable element - They will be filled automatically when fetching the account
    public transient Spannable value_span;

    public static class FieldParams implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("value")
        public String value;
    }
}
