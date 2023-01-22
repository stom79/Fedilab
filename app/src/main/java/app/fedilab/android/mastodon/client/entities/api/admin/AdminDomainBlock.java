package app.fedilab.android.mastodon.client.entities.api.admin;
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
import java.util.Date;

public class AdminDomainBlock implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("domain")
    public String domain;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("severity")
    public String severity;
    @SerializedName("reject_media")
    public boolean reject_media;
    @SerializedName("reject_reports")
    public boolean reject_reports;
    @SerializedName("private_comment")
    public String private_comment;
    @SerializedName("public_comment")
    public String public_comment;
    @SerializedName("obfuscate")
    public boolean obfuscate;
}
