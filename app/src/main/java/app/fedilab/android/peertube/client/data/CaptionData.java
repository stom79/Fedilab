package app.fedilab.android.peertube.client.data;
/* Copyright 2023 Thomas Schneider
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

import java.util.List;

import app.fedilab.android.peertube.client.entities.ItemStr;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class CaptionData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Caption> data;

    public static class Caption {
        @SerializedName("captionPath")
        private String captionPath;
        @SerializedName("language")
        private ItemStr language;

        public String getCaptionPath() {
            return captionPath;
        }

        public void setCaptionPath(String captionPath) {
            this.captionPath = captionPath;
        }

        public ItemStr getLanguage() {
            return language;
        }

        public void setLanguage(ItemStr language) {
            this.language = language;
        }
    }
}
