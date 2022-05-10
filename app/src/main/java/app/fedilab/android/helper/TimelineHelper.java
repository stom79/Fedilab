package app.fedilab.android.helper;
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

import android.content.Context;
import android.os.Build;
import android.text.Html;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.mastodon.MastodonAccountsService;
import app.fedilab.android.client.mastodon.entities.Filter;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TimelineHelper {

    private static MastodonAccountsService init(Context context) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + MainActivity.currentInstance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAccountsService.class);
    }

    /**
     * Allows to filter statuses, should be called in API calls (background)
     *
     * @param context            - Context
     * @param statuses           - List of {@link Status}
     * @param filterTimeLineType - {@link FilterTimeLineType}
     * @return filtered List<Status>
     */
    public static List<Status> filterStatus(Context context, List<Status> statuses, FilterTimeLineType filterTimeLineType) {
        //A security to make sure filters have been fetched before displaying messages
        List<Status> statusesToRemove = new ArrayList<>();
        if (!BaseMainActivity.filterFetched) {
            MastodonAccountsService mastodonAccountsService = init(context);
            List<Filter> filterList;
            Call<List<Filter>> getFiltersCall = mastodonAccountsService.getFilters(MainActivity.currentToken);
            if (getFiltersCall != null) {
                try {
                    Response<List<Filter>> getFiltersResponse = getFiltersCall.execute();
                    if (getFiltersResponse.isSuccessful()) {
                        BaseMainActivity.filterFetched = true;
                        filterList = getFiltersResponse.body();
                        BaseMainActivity.mainFilters = filterList;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //If there are filters:
        if (BaseMainActivity.mainFilters != null && BaseMainActivity.mainFilters.size() > 0) {
            for (Filter filter : BaseMainActivity.mainFilters) {
                if (filter.irreversible) { //Dealt by the server
                    continue;
                }
                for (String filterContext : filter.context) {
                    if (filterTimeLineType.value.equalsIgnoreCase(filterContext)) {
                        if (filter.whole_word) {
                            Pattern p = Pattern.compile("(^" + Pattern.quote(filter.phrase) + "\\b|\\b" + Pattern.quote(filter.phrase) + "$)");
                            for (Status status : statuses) {
                                String content;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    content = Html.fromHtml(status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                                else
                                    content = Html.fromHtml(status.content).toString();
                                Matcher m = p.matcher(content);
                                if (m.find()) {
                                    statusesToRemove.add(status);
                                }
                            }
                        } else {
                            for (Status status : statuses) {
                                String content;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    content = Html.fromHtml(status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                                else
                                    content = Html.fromHtml(status.content).toString();
                                if (content.contains(filter.phrase)) {
                                    statusesToRemove.add(status);
                                }
                            }
                        }
                    }
                }
            }
        }
        statuses.removeAll(statusesToRemove);
        return statuses;
    }

    /**
     * Allows to filter notifications, should be called in API calls (background)
     *
     * @param context       - Context
     * @param notifications - List of {@link Notification}
     * @return filtered List<Status>
     */
    public static List<Notification> filterNotification(Context context, List<Notification> notifications) {
        //A security to make sure filters have been fetched before displaying messages
        List<Notification> notificationToRemove = new ArrayList<>();
        if (!BaseMainActivity.filterFetched) {
            try {
                AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
                accountsVM.getFilters(BaseMainActivity.currentInstance, BaseMainActivity.currentToken).observe((LifecycleOwner) context, filters -> {
                    BaseMainActivity.filterFetched = true;
                    BaseMainActivity.mainFilters = filters;
                });
            } catch (Exception e) {
                return notifications;
            }
        }
        //If there are filters:
        if (BaseMainActivity.mainFilters != null && BaseMainActivity.mainFilters.size() > 0) {
            for (Filter filter : BaseMainActivity.mainFilters) {
                if (filter.irreversible) { //Dealt by the server
                    continue;
                }
                for (String filterContext : filter.context) {
                    if (FilterTimeLineType.NOTIFICATION.value.equalsIgnoreCase(filterContext)) {
                        if (filter.whole_word) {
                            Pattern p = Pattern.compile("(^" + Pattern.quote(filter.phrase) + "\\b|\\b" + Pattern.quote(filter.phrase) + "$)");
                            for (Notification notification : notifications) {
                                if (notification.status != null) {
                                    String content;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                        content = Html.fromHtml(notification.status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                                    else
                                        content = Html.fromHtml(notification.status.content).toString();
                                    Matcher m = p.matcher(content);
                                    if (m.find()) {
                                        notificationToRemove.add(notification);
                                    }
                                }
                            }
                        } else {
                            for (Notification notification : notifications) {
                                String content;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    content = Html.fromHtml(notification.status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                                else
                                    content = Html.fromHtml(notification.status.content).toString();
                                if (content.contains(filter.phrase)) {
                                    notificationToRemove.add(notification);
                                }
                            }
                        }
                    }
                }
            }
        }
        notifications.removeAll(notificationToRemove);
        return notifications;
    }

    public enum FilterTimeLineType {
        @SerializedName("HOME")
        HOME("HOME"),
        @SerializedName("PUBLIC")
        PUBLIC("PUBLIC"),
        @SerializedName("CONTEXT")
        CONTEXT("CONTEXT"),
        @SerializedName("NOTIFICATION")
        NOTIFICATION("NOTIFICATION");
        private final String value;

        FilterTimeLineType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
