package app.fedilab.android.client.entities.api.admin;
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

public class AdminMeasure implements Serializable {

    @SerializedName("key")
    public String key;
    @SerializedName("unit")
    public String unit;
    @SerializedName("total")
    public int total;
    @SerializedName("human_value")
    public int human_value;
    @SerializedName("previous_total")
    public int previous_total;
    @SerializedName("data")
    public List<Data> data;

    public static class Data implements Serializable {
        @SerializedName("date")
        public Date date;
        @SerializedName("value")
        public int value;
    }
}
