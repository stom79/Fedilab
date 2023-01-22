package app.fedilab.android.mastodon.client.entities.api.admin;
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
import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Instance;
import app.fedilab.android.mastodon.client.entities.api.Status;

public class AdminReport implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("action_taken")
    public Boolean action_taken;
    @SerializedName("action_taken_at")
    public Date action_taken_at;
    @SerializedName("category")
    public String category;
    @SerializedName("comment")
    public String comment;
    @SerializedName("forwarded")
    public boolean forwarded;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("account")
    public AdminAccount account;
    @SerializedName("target_account")
    public AdminAccount target_account;
    @SerializedName("assigned_account")
    public AdminAccount assigned_account;
    @SerializedName("action_taken_by_account")
    public AdminAccount action_taken_by_account;
    @SerializedName("statuses")
    public List<Status> statuses;
    @SerializedName("rules")
    public List<Instance.Rule> rules;
}
