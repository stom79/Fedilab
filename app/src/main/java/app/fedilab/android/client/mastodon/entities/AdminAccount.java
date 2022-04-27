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

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class AdminAccount {

    @SerializedName("id")
    public String id;
    @SerializedName("username")
    public String username;
    @SerializedName("domain")
    public String domain;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("email")
    public String email;
    @SerializedName("ip")
    public String ip;
    @SerializedName("locale")
    public String locale;
    @SerializedName("invite_request")
    public String invite_request;
    @SerializedName("role")
    public String role;
    @SerializedName("confirmed")
    public boolean confirmed;
    @SerializedName("approved")
    public boolean approved;
    @SerializedName("disabled")
    public boolean disabled;
    @SerializedName("silenced")
    public boolean silenced;
    @SerializedName("suspended")
    public boolean suspended;
    @SerializedName("account")
    public Account account;
    @SerializedName("created_by_application_id")
    public String created_by_application_id;
    @SerializedName("invited_by_account_id")
    public String invited_by_account_id;
}
