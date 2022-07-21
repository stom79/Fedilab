package app.fedilab.android.client.entities.app;
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
import java.util.List;


public class ReleaseNote implements Serializable {

    @SerializedName("languages")
    public List<Note> ReleaseNotes;


    public static class Note implements Serializable {
        @SerializedName("code")
        public String code;
        @SerializedName("version")
        public String version;
        @SerializedName("note")
        public String note;
        @SerializedName("noteTranslated")
        public String noteTranslated;
    }
}
