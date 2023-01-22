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

import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class InstanceParams {

    private boolean healthy = true;
    private boolean signup = true;
    private List<Integer> categoriesOr;
    private String nsfwPolicy = "do_not_list";
    private List<String> languagesOr;
    private String minUserQuota = "5000000000";


    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean isSignup() {
        return signup;
    }

    public void setSignup(boolean signup) {
        this.signup = signup;
    }

    public List<Integer> getCategoriesOr() {
        return categoriesOr;
    }

    public void setCategoriesOr(List<Integer> categoriesOr) {
        this.categoriesOr = categoriesOr;
    }

    public String getNsfwPolicy() {
        return nsfwPolicy;
    }

    public void setNsfwPolicy(String nsfwPolicy) {
        this.nsfwPolicy = nsfwPolicy;
    }


    public String getMinUserQuota() {
        return minUserQuota;
    }

    public void setMinUserQuota(String minUserQuota) {
        this.minUserQuota = minUserQuota;
    }

    public List<String> getLanguagesOr() {
        return languagesOr;
    }

    public void setLanguagesOr(List<String> languagesOr) {
        this.languagesOr = languagesOr;
    }
}
