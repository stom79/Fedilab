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

import static app.fedilab.android.BaseMainActivity.filteredAccounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.Html;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.endpoints.MastodonFiltersService;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Filter;
import app.fedilab.android.client.entities.api.Notification;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.viewmodel.mastodon.FiltersVM;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TimelineHelper {

    private static MastodonFiltersService initv2(Context context) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(context))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + BaseMainActivity.currentInstance + "/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonFiltersService.class);
    }


    /**
     * Allows to filter statuses, should be called in API calls (background)
     *
     * @param context            - Context
     * @param statuses           - List of {@link Status}
     * @param filterTimeLineType - {@link Timeline.TimeLineEnum}
     * @return filtered List<Status>
     */
    public static List<Status> filterStatus(Context context, List<Status> statuses, Timeline.TimeLineEnum filterTimeLineType) {
        //A security to make sure filters have been fetched before displaying messages
        if (!BaseMainActivity.filterFetched) {
            MastodonFiltersService mastodonFiltersService = initv2(context);
            List<Filter> filterList;
            Call<List<Filter>> getFiltersCall = mastodonFiltersService.getFilters(BaseMainActivity.currentToken);
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
        if (BaseMainActivity.mainFilters != null && BaseMainActivity.mainFilters.size() > 0 && statuses != null && statuses.size() > 0) {

            //Loop through filters
            for (Filter filter : BaseMainActivity.mainFilters) {
                if (filter.expires_at != null && filter.expires_at.before(new Date())) {
                    //Expired filter
                    continue;
                }
                if (filterTimeLineType == Timeline.TimeLineEnum.HOME || filterTimeLineType == Timeline.TimeLineEnum.LIST) {
                    if (!filter.context.contains("home")) continue;
                } else if (filterTimeLineType == Timeline.TimeLineEnum.NOTIFICATION) {
                    if (!filter.context.contains("notifications")) continue;
                } else if (filterTimeLineType == Timeline.TimeLineEnum.CONTEXT) {
                    if (!filter.context.contains("thread")) continue;
                } else if (filterTimeLineType == Timeline.TimeLineEnum.ACCOUNT_TIMELINE) {
                    if (!filter.context.contains("account")) continue;
                } else {
                    if (!filter.context.contains("public")) continue;
                }
                if (filter.keywords != null && filter.keywords.size() > 0) {
                    for (Filter.KeywordsAttributes filterKeyword : filter.keywords) {
                        String sb = Pattern.compile("\\A[A-Za-z0-9_]").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        String eb = Pattern.compile("[A-Za-z0-9_]\\z").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        Pattern p;
                        if (filterKeyword.whole_word) {
                            p = Pattern.compile(sb + "(" + Pattern.quote(filterKeyword.keyword) + ")" + eb, Pattern.CASE_INSENSITIVE);
                        } else {
                            p = Pattern.compile("(" + Pattern.quote(filterKeyword.keyword) + ")", Pattern.CASE_INSENSITIVE);
                        }
                        for (Status status : statuses) {
                            if (status.account.id.equals(MainActivity.currentUserID)) {
                                continue;
                            }
                            String content;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                content = Html.fromHtml(status.reblog != null ? status.reblog.content : status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                            else
                                content = Html.fromHtml(status.reblog != null ? status.reblog.content : status.content).toString();
                            Matcher m = p.matcher(content);
                            if (m.find()) {
                                status.filteredByApp = filter;
                                continue;
                            }
                            if (status.spoiler_text != null) {
                                String spoilerText;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    spoilerText = Html.fromHtml(status.reblog != null ? status.reblog.spoiler_text : status.spoiler_text, Html.FROM_HTML_MODE_LEGACY).toString();
                                else
                                    spoilerText = Html.fromHtml(status.reblog != null ? status.reblog.spoiler_text : status.spoiler_text).toString();
                                Matcher ms = p.matcher(spoilerText);
                                if (ms.find()) {
                                    status.filteredByApp = filter;
                                    continue;
                                }
                            }

                            if (filterTimeLineType == Timeline.TimeLineEnum.HOME) {
                                if (filteredAccounts != null && filteredAccounts.size() > 0) {
                                    for (Account account : filteredAccounts) {
                                        if (account.acct.equals(status.account.acct)) {
                                            Filter filterCustom = new Filter();
                                            filterCustom.filter_action = "hide";
                                            ArrayList<String> contextCustom = new ArrayList<>();
                                            contextCustom.add("home");
                                            filterCustom.title = "Fedilab";
                                            filterCustom.context = contextCustom;
                                            status.filteredByApp = filterCustom;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
                FiltersVM filtersVM = new ViewModelProvider((ViewModelStoreOwner) context).get(FiltersVM.class);
                filtersVM.getFilters(BaseMainActivity.currentInstance, BaseMainActivity.currentToken).observe((LifecycleOwner) context, filters -> {
                    BaseMainActivity.filterFetched = true;
                    BaseMainActivity.mainFilters = filters;
                });
            } catch (Exception e) {
                return notifications;
            }
        }
        //If there are filters:
        if (BaseMainActivity.mainFilters != null && BaseMainActivity.mainFilters.size() > 0 && notifications != null && notifications.size() > 0) {

            //Loop through filters
            for (Filter filter : BaseMainActivity.mainFilters) {

                if (filter.expires_at != null && filter.expires_at.before(new Date())) {
                    //Expired filter
                    continue;
                }

                if (!filter.context.contains("notifications")) continue;
                if (filter.keywords != null && filter.keywords.size() > 0) {
                    for (Filter.KeywordsAttributes filterKeyword : filter.keywords) {
                        String sb = Pattern.compile("\\A[A-Za-z0-9_]").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        String eb = Pattern.compile("[A-Za-z0-9_]\\z").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        Pattern p;
                        if (filterKeyword.whole_word) {
                            p = Pattern.compile(sb + "(" + Pattern.quote(filterKeyword.keyword) + ")" + eb, Pattern.CASE_INSENSITIVE);
                        } else {
                            p = Pattern.compile("(" + Pattern.quote(filterKeyword.keyword) + ")", Pattern.CASE_INSENSITIVE);
                        }

                        for (Notification notification : notifications) {
                            if (notification.status == null) {
                                continue;
                            }
                            if (notification.status.account.id.equals(MainActivity.currentUserID)) {
                                continue;
                            }

                            String content;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                content = Html.fromHtml(notification.status.reblog != null ? notification.status.reblog.content : notification.status.content, Html.FROM_HTML_MODE_LEGACY).toString();
                            else
                                content = Html.fromHtml(notification.status.reblog != null ? notification.status.reblog.content : notification.status.content).toString();
                            Matcher m = p.matcher(content);
                            if (m.find()) {
                                if (filter.filter_action.equalsIgnoreCase("warn")) {
                                    notification.filteredByApp = filter;
                                } else {
                                    notificationToRemove.add(notification);
                                }
                                continue;
                            }
                            if (notification.status.spoiler_text != null) {
                                String spoilerText;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                    spoilerText = Html.fromHtml(notification.status.reblog != null ? notification.status.reblog.spoiler_text : notification.status.spoiler_text, Html.FROM_HTML_MODE_LEGACY).toString();
                                else
                                    spoilerText = Html.fromHtml(notification.status.reblog != null ? notification.status.reblog.spoiler_text : notification.status.spoiler_text).toString();
                                Matcher ms = p.matcher(spoilerText);
                                if (ms.find()) {
                                    if (filter.filter_action.equalsIgnoreCase("warn")) {
                                        notification.filteredByApp = filter;
                                    } else {
                                        notificationToRemove.add(notification);
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        if (notifications != null) {
            notifications.removeAll(notificationToRemove);
        }
        return notifications;
    }


    /**
     * Check if WIFI is opened
     *
     * @param context Context
     * @return boolean
     */
    public static boolean isOnWIFI(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
        }
        return false;
    }
}
