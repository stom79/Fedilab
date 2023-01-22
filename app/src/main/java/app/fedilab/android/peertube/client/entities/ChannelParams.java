package app.fedilab.android.peertube.client.entities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ChannelParams {

    @SerializedName("displayName")
    private String displayName;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("support")
    private String support;
    @SerializedName("bulkVideosSupportUpdate")
    private boolean bulkVideosSupportUpdate;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public boolean isBulkVideosSupportUpdate() {
        return bulkVideosSupportUpdate;
    }

    public void setBulkVideosSupportUpdate(boolean bulkVideosSupportUpdate) {
        this.bulkVideosSupportUpdate = bulkVideosSupportUpdate;
    }
}
