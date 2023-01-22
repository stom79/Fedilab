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
public class CaptionsParams {

    @SerializedName("captionLanguage")
    private String captionLanguage;
    @SerializedName("id")
    private String id;

    public String getCaptionLanguage() {
        return captionLanguage;
    }

    public void setCaptionLanguage(String captionLanguage) {
        this.captionLanguage = captionLanguage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
