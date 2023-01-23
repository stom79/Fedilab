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
import java.util.Date;

import app.fedilab.android.peertube.client.data.AccountData;

/**
 * Class that manages Accounts from database
 * Accounts details are serialized and can be for different softwares
 * The type of the software is stored in api field
 */
public class BaseAccount implements Serializable {


    @SerializedName("user_id")
    public String user_id;
    @SerializedName("instance")
    public String instance;
    @SerializedName("api")
    public Account.API api;
    @SerializedName("software")
    public String software;
    @SerializedName("token")
    public String token;
    @SerializedName("refresh_token")
    public String refresh_token;
    @SerializedName("token_validity")
    public long token_validity;
    @SerializedName("client_id")
    public String client_id;
    @SerializedName("client_secret")
    public String client_secret;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("mastodon_account")
    public app.fedilab.android.mastodon.client.entities.api.Account mastodon_account;
    @SerializedName("peertube_account")
    public AccountData.PeertubeAccount peertube_account;
    @SerializedName("admin")
    public boolean admin;

}
