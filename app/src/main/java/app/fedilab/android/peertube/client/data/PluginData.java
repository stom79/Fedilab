package app.fedilab.android.peertube.client.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

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
@SuppressWarnings("ALL")
public class PluginData {

    public static class Plugin {
        @SerializedName("registered")
        private List<PluginInfo> registered;
        @SerializedName("registeredExternalAuths")
        private List<PluginInfo> registeredExternalAuths;
        @SerializedName("registeredIdAndPassAuths")
        private List<PluginInfo> registeredIdAndPassAuths;

        public List<PluginInfo> getRegistered() {
            return registered;
        }

        public void setRegistered(List<PluginInfo> registered) {
            this.registered = registered;
        }

        public List<PluginInfo> getRegisteredExternalAuths() {
            return registeredExternalAuths;
        }

        public void setRegisteredExternalAuths(List<PluginInfo> registeredExternalAuths) {
            this.registeredExternalAuths = registeredExternalAuths;
        }

        public List<PluginInfo> getRegisteredIdAndPassAuths() {
            return registeredIdAndPassAuths;
        }

        public void setRegisteredIdAndPassAuths(List<PluginInfo> registeredIdAndPassAuths) {
            this.registeredIdAndPassAuths = registeredIdAndPassAuths;
        }
    }

    public static class WaterMark {
        @SerializedName("publicSettings")
        private PublicSettings description;

        public PublicSettings getDescription() {
            return description;
        }

        public void setDescription(PublicSettings description) {
            this.description = description;
        }
    }

    public static class PublicSettings {
        @SerializedName("watermark-image-url")
        private String watermarkImageUrl;
        @SerializedName("watermark-target-url")
        private String watermarkTargetUrl;

        public String getWatermarkImageUrl() {
            return watermarkImageUrl;
        }

        public void setWatermarkImageUrl(String watermarkImageUrl) {
            this.watermarkImageUrl = watermarkImageUrl;
        }

        public String getWatermarkTargetUrl() {
            return watermarkTargetUrl;
        }

        public void setWatermarkTargetUrl(String watermarkTargetUrl) {
            this.watermarkTargetUrl = watermarkTargetUrl;
        }
    }

    public static class PluginInfo {
        @SerializedName("description")
        private String description;
        @SerializedName("name")
        private String name;
        @SerializedName("version")
        private String version;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
