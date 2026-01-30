package app.fedilab.android.misskey.client.entities;
/* Copyright 2026 Thomas Schneider
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

import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;

public class MisskeyScheduledNote implements Serializable {

    @SerializedName("id")
    public String id;
    @SerializedName("text")
    public String text;
    @SerializedName("visibility")
    public String visibility;
    @SerializedName("cw")
    public String cw;
    @SerializedName("localOnly")
    public Boolean localOnly;
    @SerializedName("fileIds")
    public List<String> fileIds;
    @SerializedName("replyId")
    public String replyId;
    @SerializedName("renoteId")
    public String renoteId;
    @SerializedName("scheduledAt")
    public Date scheduledAt;
    @SerializedName("hasPoll")
    public Boolean hasPoll;
    @SerializedName("pollChoices")
    public List<String> pollChoices;
    @SerializedName("pollMultiple")
    public Boolean pollMultiple;
    @SerializedName("pollExpiredAfter")
    public Long pollExpiredAfter;
    @SerializedName("pollExpiresAt")
    public Long pollExpiresAt;
    @SerializedName("visibleUserIds")
    public List<String> visibleUserIds;

    public ScheduledStatus toScheduledStatus() {
        ScheduledStatus scheduledStatus = new ScheduledStatus();
        scheduledStatus.id = this.id;
        scheduledStatus.scheduled_at = this.scheduledAt;
        scheduledStatus.params = new ScheduledStatus.Params();
        scheduledStatus.params.text = this.text;
        scheduledStatus.params.spoiler_text = this.cw;
        scheduledStatus.params.sensitive = this.cw != null;
        scheduledStatus.params.in_reply_to_id = this.replyId;
        if (this.visibility != null) {
            switch (this.visibility) {
                case "public":
                    scheduledStatus.params.visibility = "public";
                    break;
                case "home":
                    scheduledStatus.params.visibility = "unlisted";
                    break;
                case "followers":
                    scheduledStatus.params.visibility = "private";
                    break;
                case "specified":
                    scheduledStatus.params.visibility = "direct";
                    break;
                default:
                    scheduledStatus.params.visibility = "public";
                    break;
            }
        }
        scheduledStatus.params.media_ids = this.fileIds;
        return scheduledStatus;
    }
}
