package app.fedilab.android.mastodon.client.entities.api;
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
import java.util.List;

public class Tag implements Serializable {

    @SerializedName("name")
    public String name;
    @SerializedName("url")
    public String url;
    @SerializedName("history")
    public List<History> history;
    @SerializedName("following")
    public boolean following = false;

    public int getWeight() {
        int weight = 0;
        if (history != null && history.size() > 0) {
            for (History h : history) {
                try {
                    weight += Integer.parseInt(h.accounts);
                } catch (Exception ignored) {
                }
            }
        }
        return weight;
    }
}
