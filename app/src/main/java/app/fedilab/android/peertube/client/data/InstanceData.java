package app.fedilab.android.peertube.client.data;
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

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class InstanceData {

    @SerializedName("total")
    public int total;
    @SerializedName("data")
    public List<Instance> data;


    public static class Instance {

        @SerializedName("autoBlacklistUserVideosEnabled")
        private boolean autoBlacklistUserVideosEnabled;
        @SerializedName("categories")
        private List<Integer> categories;
        @SerializedName("country")
        private String country;
        @SerializedName("createdAt")
        private Date createdAt;
        @SerializedName("defaultNSFWPolicy")
        private String defaultNSFWPolicy;
        @SerializedName("health")
        private int health;
        @SerializedName("host")
        private String host;
        @SerializedName("id")
        private String id;
        @SerializedName("languages")
        private List<String> languages;
        @SerializedName("name")
        private String name;
        @SerializedName("shortDescription")
        private String shortDescription;
        @SerializedName("signupAllowed")
        private boolean signupAllowed;
        @SerializedName("supportsIPv6")
        private boolean supportsIPv6;
        @SerializedName("totalInstanceFollowers")
        private int totalInstanceFollowers;
        @SerializedName("totalInstanceFollowing")
        private int totalInstanceFollowing;
        @SerializedName("totalLocalVideos")
        private int totalLocalVideos;
        @SerializedName("totalUsers")
        private int totalUsers;
        @SerializedName("totalVideos")
        private int totalVideos;
        @SerializedName("userVideoQuota")
        private String userVideoQuota;
        @SerializedName("version")
        private String version;
        @SerializedName("isNSFW")
        private boolean isNSFW;
        private SpannableStringBuilder spannableStringBuilder;
        private boolean truncatedDescription = true;

        public boolean isAutoBlacklistUserVideosEnabled() {
            return autoBlacklistUserVideosEnabled;
        }

        public void setAutoBlacklistUserVideosEnabled(boolean autoBlacklistUserVideosEnabled) {
            this.autoBlacklistUserVideosEnabled = autoBlacklistUserVideosEnabled;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public String getDefaultNSFWPolicy() {
            return defaultNSFWPolicy;
        }

        public void setDefaultNSFWPolicy(String defaultNSFWPolicy) {
            this.defaultNSFWPolicy = defaultNSFWPolicy;
        }

        public int getHealth() {
            return health;
        }

        public void setHealth(int health) {
            this.health = health;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortDescription() {
            return shortDescription;
        }

        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public boolean isSignupAllowed() {
            return signupAllowed;
        }

        public void setSignupAllowed(boolean signupAllowed) {
            this.signupAllowed = signupAllowed;
        }

        public boolean isSupportsIPv6() {
            return supportsIPv6;
        }

        public void setSupportsIPv6(boolean supportsIPv6) {
            this.supportsIPv6 = supportsIPv6;
        }

        public int getTotalInstanceFollowers() {
            return totalInstanceFollowers;
        }

        public void setTotalInstanceFollowers(int totalInstanceFollowers) {
            this.totalInstanceFollowers = totalInstanceFollowers;
        }

        public int getTotalInstanceFollowing() {
            return totalInstanceFollowing;
        }

        public void setTotalInstanceFollowing(int totalInstanceFollowing) {
            this.totalInstanceFollowing = totalInstanceFollowing;
        }

        public int getTotalLocalVideos() {
            return totalLocalVideos;
        }

        public void setTotalLocalVideos(int totalLocalVideos) {
            this.totalLocalVideos = totalLocalVideos;
        }

        public int getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(int totalUsers) {
            this.totalUsers = totalUsers;
        }

        public int getTotalVideos() {
            return totalVideos;
        }

        public void setTotalVideos(int totalVideos) {
            this.totalVideos = totalVideos;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isNSFW() {
            return isNSFW;
        }

        public void setNSFW(boolean NSFW) {
            isNSFW = NSFW;
        }

        public String getUserVideoQuota() {
            return userVideoQuota;
        }

        public void setUserVideoQuota(String userVideoQuota) {
            this.userVideoQuota = userVideoQuota;
        }

        public SpannableStringBuilder getSpannableStringBuilder() {
            return spannableStringBuilder;
        }

        public void setSpannableStringBuilder(SpannableStringBuilder spannableStringBuilder) {
            this.spannableStringBuilder = spannableStringBuilder;
        }

        public List<String> getLanguages() {
            return languages;
        }

        public void setLanguages(List<String> languages) {
            this.languages = languages;
        }

        public List<Integer> getCategories() {
            return categories;
        }

        public void setCategories(List<Integer> categories) {
            this.categories = categories;
        }

        public boolean isTruncatedDescription() {
            return truncatedDescription;
        }

        public void setTruncatedDescription(boolean truncatedDescription) {
            this.truncatedDescription = truncatedDescription;
        }
    }

    public static class InstanceInfo {
        @SerializedName("instance")
        private AboutInstance instance;

        public AboutInstance getInstance() {
            return instance;
        }

        public void setInstance(AboutInstance instance) {
            this.instance = instance;
        }
    }

    public static class AboutInstance implements Parcelable, Serializable {

        public static final Creator<AboutInstance> CREATOR = new Creator<AboutInstance>() {
            @Override
            public AboutInstance createFromParcel(Parcel in) {
                return new AboutInstance(in);
            }

            @Override
            public AboutInstance[] newArray(int size) {
                return new AboutInstance[size];
            }
        };
        @SerializedName("name")
        private String name;
        @SerializedName("shortDescription")
        private String shortDescription;
        @SerializedName("description")
        private String description;
        @SerializedName("terms")
        private String terms;
        private String host;
        private boolean truncatedDescription = true;

        public AboutInstance() {
        }

        protected AboutInstance(Parcel in) {
            name = in.readString();
            shortDescription = in.readString();
            description = in.readString();
            terms = in.readString();
            host = in.readString();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortDescription() {
            return shortDescription;
        }

        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTerms() {
            return terms;
        }

        public void setTerms(String terms) {
            this.terms = terms;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public boolean isTruncatedDescription() {
            return truncatedDescription;
        }

        public void setTruncatedDescription(boolean truncatedDescription) {
            this.truncatedDescription = truncatedDescription;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(name);
            parcel.writeString(shortDescription);
            parcel.writeString(description);
            parcel.writeString(terms);
            parcel.writeString(host);
        }
    }


    public static class InstanceConfig {
        @SerializedName("user")
        private User user;
        @SerializedName("plugin")
        private PluginData.Plugin plugin;

        public PluginData.Plugin getPlugin() {
            return plugin;
        }

        public void setPlugin(PluginData.Plugin plugin) {
            this.plugin = plugin;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }


    public static class User {
        @SerializedName("videoQuota")
        private long videoQuota;
        @SerializedName("videoQuotaDaily")
        private long videoQuotaDaily;

        public long getVideoQuota() {
            return videoQuota;
        }

        public void setVideoQuota(long videoQuota) {
            this.videoQuota = videoQuota;
        }

        public long getVideoQuotaDaily() {
            return videoQuotaDaily;
        }

        public void setVideoQuotaDaily(long videoQuotaDaily) {
            this.videoQuotaDaily = videoQuotaDaily;
        }
    }
}

