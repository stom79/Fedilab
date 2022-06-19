package app.fedilab.android.client.entities.api;
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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;

public class Notification {

    @SerializedName("id")
    public String id;
    @SerializedName("type")
    public String type;
    @SerializedName("created_at")
    public Date created_at;
    @SerializedName("account")
    public Account account;
    @SerializedName("status")
    public Status status;

    public transient List<Notification> relatedNotifications;
    public boolean isFetchMore;
    public boolean isFetchMoreHidden = false;

    /**
     * Serialized a list of Notification class
     *
     * @param notifications List of {@link Notification} to serialize
     * @return String serialized emoji list
     */
    public static String notificationsToStringStorage(List<Notification> notifications) {
        Gson gson = new Gson();
        try {
            return gson.toJson(notifications);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a notification List
     *
     * @param serializedNotificationList String serialized Status list
     * @return List of {@link Notification}
     */
    public static List<Notification> restoreNotificationsFromString(String serializedNotificationList) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedNotificationList, new TypeToken<List<Notification>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

}
