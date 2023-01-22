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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class PeertubeInformation {

    private Map<Integer, String> categories = new HashMap<>();
    private Map<String, String> languages = new HashMap<>();
    private Map<Integer, String> licences = new HashMap<>();
    private Map<Integer, String> privacies = new HashMap<>();
    private Map<Integer, String> playlistPrivacies = new HashMap<>();
    private Map<String, String> translations = new HashMap<>();

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public Map<Integer, String> getCategories() {
        return categories;
    }

    public void setCategories(Map<Integer, String> categories) {
        this.categories = categories;
    }

    public Map<String, String> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<String, String> languages) {
        this.languages = languages;
    }

    public Map<Integer, String> getLicences() {
        return licences;
    }

    public void setLicences(Map<Integer, String> licences) {
        this.licences = licences;
    }

    public Map<Integer, String> getPrivacies() {
        return privacies;
    }

    public void setPrivacies(Map<Integer, String> privacies) {
        this.privacies = privacies;
    }

    public Map<Integer, String> getPlaylistPrivacies() {
        return playlistPrivacies;
    }

    public void setPlaylistPrivacies(Map<Integer, String> playlistPrivacies) {
        this.playlistPrivacies = playlistPrivacies;
    }
}
