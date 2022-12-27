package com.github.stom79.mytransl.translate;
/* Copyright 2018 Thomas Schneider
 *
 * This file is a part of MyTransL
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * MyTransL is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MyTransL; if not,
 * see <http://www.gnu.org/licenses>. */

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class Params {

    private String source_lang;
    private String tag_handling;
    private String non_splitting_tags;
    private String ignore_tags;
    private boolean split_sentences = true;
    private boolean preserve_formatting = false;
    private boolean isPro = true;
    private Params.fType format;

    public Params.fType getFormat() {
        if (format == null) {
            format = Params.fType.TEXT;
        }
        return format;
    }

    public void setFormat(Params.fType format) {
        this.format = format;
    }

    public String getSource_lang() {
        return source_lang;
    }

    public void setSource_lang(String source_lang) {
        this.source_lang = source_lang;
    }

    public String getTag_handling() {
        return tag_handling;
    }

    public void setTag_handling(String tag_handling) {
        this.tag_handling = tag_handling;
    }

    public String getNon_splitting_tags() {
        return non_splitting_tags;
    }

    public void setNon_splitting_tags(String non_splitting_tags) {
        this.non_splitting_tags = non_splitting_tags;
    }

    public String getIgnore_tags() {
        return ignore_tags;
    }

    public void setIgnore_tags(String ignore_tags) {
        this.ignore_tags = ignore_tags;
    }

    public boolean isSplit_sentences() {
        return split_sentences;
    }

    public void setSplit_sentences(boolean split_sentences) {
        this.split_sentences = split_sentences;
    }

    public boolean isPreserve_formatting() {
        return preserve_formatting;
    }

    public void setPreserve_formatting(boolean preserve_formatting) {
        this.preserve_formatting = preserve_formatting;
    }


    public boolean isPro() {
        return isPro;
    }

    public void setPro(boolean pro) {
        this.isPro = pro;
    }

    public enum fType {
        @SerializedName("TEXT")
        TEXT("text"),
        @SerializedName("HTML")
        HTML("html");
        private final String value;

        fType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
