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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class App implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("website")
    public String website;
    @SerializedName("redirect_uri")
    public String redirect_uri;
    @SerializedName("client_id")
    public String client_id;
    @SerializedName("client_secret")
    public String client_secret;
    @SerializedName("vapid_key")
    public String vapid_key;
}
