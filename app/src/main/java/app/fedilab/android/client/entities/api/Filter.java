package app.fedilab.android.client.entities.api;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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

public class Filter implements Serializable {
    @SerializedName("id")
    public String id;
    @SerializedName("title")
    public String title;
    @SerializedName("context")
    public List<String> context;
    @SerializedName("expires_at")
    public Date expires_at;
    @SerializedName("filter_action")
    public String filter_action;
    @SerializedName("keywords")
    public List<KeywordsAttributes> keywords;

    public static String getValueOf(FilterParams filterParams) {
        Gson gson = new Gson();
        try {
            return gson.toJson(filterParams);
        } catch (Exception e) {
            return null;
        }
    }

    public static class KeywordsAttributes implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("keyword")
        public String keyword;
        @SerializedName("whole_word")
        public Boolean whole_word;
        @SerializedName("_destroy")
        public Boolean _destroy;
    }

    public static class FilterResult implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("phrase")
        public String phrase;
        @SerializedName("context")
        public List<String> context;
        @SerializedName("whole_word")
        public boolean whole_word;
        @SerializedName("expires_in")
        public Date expires_at;
        @SerializedName("filter_action")
        public String filter_action;
    }

    public static class FilterParams implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("title")
        public String title;
        @SerializedName("context")
        public List<String> context;
        @SerializedName("expires_in")
        public Long expires_in;
        @SerializedName("filter_action")
        public String filter_action;
        @SerializedName("keywords_attributes")
        public List<KeywordsParams> keywords;
    }

    public static class KeywordsParams implements Serializable {
        @SerializedName("id")
        public String id;
        @SerializedName("keyword")
        public String keyword;
        @SerializedName("whole_word")
        public Boolean whole_word;
        @SerializedName("_destroy")
        public Boolean _destroy;
    }
}
