package app.fedilab.android.mastodon.client.entities.api;
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
import java.util.List;

public class Report implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("action_taken")
    public boolean action_taken;
    @SerializedName("comment")
    public String comment;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("updated_at")
    public Date updated_at;
    @SerializedName("account")
    public Account account;
    @SerializedName("target_account")
    public Account target_account;
    @SerializedName("assigned_account")
    public Account assigned_account;
    @SerializedName("action_taken_by_account")
    public String action_taken_by_account;
    @SerializedName("statuses")
    public List<Status> statuses;

    public static class ReportParams implements Serializable {
        @SerializedName("account_id")
        public String account_id;
        @SerializedName("status_ids")
        public List<String> status_ids;
        @SerializedName("comment")
        public String comment;
        @SerializedName("forward")
        public Boolean forward;
        @SerializedName("category")
        public String category;
        @SerializedName("rule_ids")
        public List<String> rule_ids;

    }
}
