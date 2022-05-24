package app.fedilab.android.client.entities.api;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
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

public class Filter {
    @SerializedName("id")
    public String id;
    @SerializedName("phrase")
    public String phrase;
    @SerializedName("context")
    public List<String> context;
    @SerializedName("whole_word")
    public boolean whole_word;
    @SerializedName("expires_at")
    public Date expires_at;
    @SerializedName("expires_at_sent")
    public long expires_at_sent;
    @SerializedName("irreversible")
    public boolean irreversible;
}
