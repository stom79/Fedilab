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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class SepiaSearch implements Serializable {

    @SerializedName("start")
    private String start;
    @SerializedName("count")
    private String count;
    @SerializedName("search")
    private String search;
    @SerializedName("durationMax")
    private int durationMax;
    @SerializedName("durationMin")
    private int durationMin;
    @SerializedName("startDate")
    private Date startDate;
    @SerializedName("boostLanguages")
    private List<String> boostLanguages;
    @SerializedName("categoryOneOf")
    private List<Integer> categoryOneOf;
    @SerializedName("licenceOneOf")
    private List<Integer> licenceOneOf;
    @SerializedName("tagsOneOf")
    private List<String> tagsOneOf;
    @SerializedName("tagsAllOf")
    private List<String> tagsAllOf;
    @SerializedName("nsfw")
    private boolean nsfw;
    @SerializedName("sort")
    private String sort;

    public SepiaSearch() {
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public int getDurationMax() {
        return durationMax;
    }

    public void setDurationMax(int durationMax) {
        this.durationMax = durationMax;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(int durationMin) {
        this.durationMin = durationMin;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public List<String> getBoostLanguages() {
        return boostLanguages;
    }

    public void setBoostLanguages(List<String> boostLanguages) {
        this.boostLanguages = boostLanguages;
    }

    public List<Integer> getCategoryOneOf() {
        return categoryOneOf;
    }

    public void setCategoryOneOf(List<Integer> categoryOneOf) {
        this.categoryOneOf = categoryOneOf;
    }

    public List<Integer> getLicenceOneOf() {
        return licenceOneOf;
    }

    public void setLicenceOneOf(List<Integer> licenceOneOf) {
        this.licenceOneOf = licenceOneOf;
    }

    public List<String> getTagsOneOf() {
        return tagsOneOf;
    }

    public void setTagsOneOf(List<String> tagsOneOf) {
        this.tagsOneOf = tagsOneOf;
    }

    public List<String> getTagsAllOf() {
        return tagsAllOf;
    }

    public void setTagsAllOf(List<String> tagsAllOf) {
        this.tagsAllOf = tagsAllOf;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

}
