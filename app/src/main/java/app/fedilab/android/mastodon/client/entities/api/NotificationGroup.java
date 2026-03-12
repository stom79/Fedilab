package app.fedilab.android.mastodon.client.entities.api;
/* Copyright 2026 Thomas Schneider
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationGroup {

    @SerializedName("group_key")
    public String group_key;
    @SerializedName("notifications_count")
    public int notifications_count;
    @SerializedName("type")
    public String type;
    @SerializedName("most_recent_notification_id")
    public String most_recent_notification_id;
    @SerializedName("page_min_id")
    public String page_min_id;
    @SerializedName("page_max_id")
    public String page_max_id;
    @SerializedName("latest_page_notification_at")
    public Date latest_page_notification_at;
    @SerializedName("sample_account_ids")
    public List<String> sample_account_ids;
    @SerializedName("status_id")
    public String status_id;
    @SerializedName("report")
    public Report report;

    /**
     * Convert grouped notifications results from API v2 to a flat list of Notification
     * compatible with the existing adapter and cache
     */
    public static List<Notification> fromGroupedResults(GroupedNotificationsResults results) {
        List<Notification> notifications = new ArrayList<>();
        if (results == null || results.notification_groups == null) {
            return notifications;
        }
        for (NotificationGroup group : results.notification_groups) {
            Notification notification = new Notification();
            notification.id = group.most_recent_notification_id;
            notification.type = group.type;
            notification.created_at = group.latest_page_notification_at;

            // Resolve status from the deduplicated list
            if (group.status_id != null && results.statuses != null) {
                for (Status status : results.statuses) {
                    if (group.status_id.equals(status.id)) {
                        notification.status = status;
                        break;
                    }
                }
            }

            // Resolve primary account (first in sample_account_ids)
            if (group.sample_account_ids != null && !group.sample_account_ids.isEmpty()) {
                notification.account = findAccount(group.sample_account_ids.get(0), results);

                // Remaining accounts become relatedNotifications
                if (group.sample_account_ids.size() > 1) {
                    notification.relatedNotifications = new ArrayList<>();
                    for (int j = 1; j < group.sample_account_ids.size(); j++) {
                        Notification related = new Notification();
                        related.id = group.most_recent_notification_id;
                        related.type = group.type;
                        related.account = findAccount(group.sample_account_ids.get(j), results);
                        related.status = notification.status;
                        notification.relatedNotifications.add(related);
                    }
                }
            }

            notifications.add(notification);
        }
        return notifications;
    }

    private static Account findAccount(String accountId, GroupedNotificationsResults results) {
        if (accountId == null) {
            return null;
        }
        if (results.accounts != null) {
            for (Account account : results.accounts) {
                if (accountId.equals(account.id)) {
                    return account;
                }
            }
        }
        return null;
    }
}
