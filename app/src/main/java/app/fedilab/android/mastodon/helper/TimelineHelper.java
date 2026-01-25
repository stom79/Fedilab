package app.fedilab.android.mastodon.helper;
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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.Html;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonFiltersService;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.client.entities.api.FilterStatus;
import app.fedilab.android.mastodon.client.entities.api.Notification;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.viewmodel.mastodon.FiltersVM;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TimelineHelper {

    private static MastodonFiltersService initv2(Context context) {
        OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (MainActivity.currentInstance != null ? IDN.toASCII(MainActivity.currentInstance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
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
        // Ensure filters have been fetched before displaying messages
        if (!BaseMainActivity.filterFetched && BaseMainActivity.filterFetchedRetry < 3) {
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
            BaseMainActivity.filterFetchedRetry++;
        }

        if (statuses == null || statuses.isEmpty()) {
            return statuses;
        }

        // Precompile patterns for all active filters
        List<CompiledFilter> compiledFilters = new ArrayList<>();
        if (BaseMainActivity.mainFilters != null && !BaseMainActivity.mainFilters.isEmpty()) {
            Date now = new Date();
            for (Filter filter : BaseMainActivity.mainFilters) {
                // Skip expired filters
                if (filter.expires_at != null && filter.expires_at.before(now)) {
                    continue;
                }

                // Check context
                boolean contextMatches = false;
                if (filterTimeLineType == Timeline.TimeLineEnum.HOME || filterTimeLineType == Timeline.TimeLineEnum.LIST) {
                    contextMatches = filter.context.contains("home");
                } else if (filterTimeLineType == Timeline.TimeLineEnum.NOTIFICATION) {
                    contextMatches = filter.context.contains("notifications");
                } else if (filterTimeLineType == Timeline.TimeLineEnum.CONTEXT) {
                    contextMatches = filter.context.contains("thread");
                } else if (filterTimeLineType == Timeline.TimeLineEnum.ACCOUNT_TIMELINE) {
                    contextMatches = filter.context.contains("account");
                } else {
                    contextMatches = filter.context.contains("public");
                }

                boolean hasKeywords = filter.keywords != null && !filter.keywords.isEmpty();
                boolean hasStatuses = filter.statuses != null && !filter.statuses.isEmpty();
                if (!contextMatches || (!hasKeywords && !hasStatuses)) {
                    continue;
                }

                // Precompile patterns for this filter
                List<Pattern> patterns = new ArrayList<>();
                if (hasKeywords) {
                    for (Filter.KeywordsAttributes filterKeyword : filter.keywords) {
                        String sb = Pattern.compile("\\A[A-Za-z0-9_]").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        String eb = Pattern.compile("[A-Za-z0-9_]\\z").matcher(filterKeyword.keyword).find() ? "\\b" : "";
                        Pattern p;
                        if (filterKeyword.whole_word) {
                            p = Pattern.compile(sb + "(" + Pattern.quote(filterKeyword.keyword) + ")" + eb, Pattern.CASE_INSENSITIVE);
                        } else {
                            p = Pattern.compile("(" + Pattern.quote(filterKeyword.keyword) + ")", Pattern.CASE_INSENSITIVE);
                        }
                        patterns.add(p);
                    }
                }
                compiledFilters.add(new CompiledFilter(filter, patterns));
            }
        }

        // Apply filters to statuses (inverted loop order for early exit)
        for (Status status : statuses) {
            // Skip user's own statuses
            if (status.account.id.equals(MainActivity.currentUserID)) {
                continue;
            }

            // Cache parsed HTML content
            String content = parseStatusContent(status);
            String spoilerText = parseStatusSpoiler(status);
            List<Attachment> mediaAttachments = status.reblog != null ? status.reblog.media_attachments : status.media_attachments;

            // Check against all compiled filters
            for (CompiledFilter compiledFilter : compiledFilters) {
                boolean matched = false;

                // Check if status ID is in filter's statuses list
                if (compiledFilter.filter.statuses != null) {
                    String statusIdToCheck = status.reblog != null ? status.reblog.id : status.id;
                    for (FilterStatus filterStatus : compiledFilter.filter.statuses) {
                        if (filterStatus.status_id != null && filterStatus.status_id.equals(statusIdToCheck)) {
                            matched = true;
                            break;
                        }
                    }
                }

                // Check keywords (content, spoiler, media) if not already matched by status ID
                if (!matched) {
                    // Check content
                    for (Pattern pattern : compiledFilter.patterns) {
                        if (pattern.matcher(content).find()) {
                            matched = true;
                            break;
                        }
                    }

                    // Check spoiler text
                    if (!matched && spoilerText != null) {
                        for (Pattern pattern : compiledFilter.patterns) {
                            if (pattern.matcher(spoilerText).find()) {
                                matched = true;
                                break;
                            }
                        }
                    }

                    // Check media attachments
                    if (!matched && mediaAttachments != null && !mediaAttachments.isEmpty()) {
                        for (Attachment attachment : mediaAttachments) {
                            if (attachment.description != null) {
                                for (Pattern pattern : compiledFilter.patterns) {
                                    if (pattern.matcher(attachment.description).find()) {
                                        matched = true;
                                        break;
                                    }
                                }
                                if (matched) break;
                            }
                        }
                    }
                }

                if (matched) {
                    status.filteredByApp = compiledFilter.filter;
                    break; // Stop checking other filters for this status
                }
            }
        }

        // Apply additional filters for HOME timeline
        if (filterTimeLineType == Timeline.TimeLineEnum.HOME) {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean groupReblogs = sharedpreferences.getBoolean(context.getString(R.string.SET_GROUP_REBLOGS), true);

            // Index seen reblog IDs for O(1) lookup
            java.util.HashSet<String> seenReblogIds = new java.util.HashSet<>();

            for (int i = 0; i < statuses.size(); i++) {
                Status currentStatus = statuses.get(i);

                // Check filtered accounts
                if (filteredAccounts != null && !filteredAccounts.isEmpty()) {
                    for (Account account : filteredAccounts) {
                        if (account.acct.equals(currentStatus.account.acct) ||
                            (currentStatus.reblog != null && account.acct.equals(currentStatus.reblog.account.acct))) {
                            Filter filterCustom = new Filter();
                            filterCustom.filter_action = "hide";
                            ArrayList<String> contextCustom = new ArrayList<>();
                            contextCustom.add("home");
                            filterCustom.title = "Fedilab";
                            filterCustom.context = contextCustom;
                            currentStatus.filteredByApp = filterCustom;
                            break;
                        }
                    }
                }

                // Group duplicate reblogs using HashSet
                if (groupReblogs && currentStatus.filteredByApp == null && currentStatus.reblog != null) {
                    if (seenReblogIds.contains(currentStatus.reblog.id)) {
                        Filter filterCustom = new Filter();
                        filterCustom.filter_action = "hide";
                        ArrayList<String> contextCustom = new ArrayList<>();
                        contextCustom.add("home");
                        filterCustom.title = "Fedilab reblog";
                        filterCustom.context = contextCustom;
                        currentStatus.filteredByApp = filterCustom;
                    } else {
                        seenReblogIds.add(currentStatus.reblog.id);
                    }
                }
            }
        }

        return statuses;
    }

    /**
     * Parse HTML content from status (cached per status)
     */
    private static String parseStatusContent(Status status) {
        try {
            String content;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                content = Html.fromHtml(status.reblog != null ? status.reblog.content : status.content, Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                content = Html.fromHtml(status.reblog != null ? status.reblog.content : status.content).toString();
            }

            // Append quote content if present
            Status quote = status.reblog == null ? status.getQuote() : (status.reblog != null ? status.reblog.getQuote() : null);
            if (quote != null && quote.content != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    content += Html.fromHtml(quote.content, Html.FROM_HTML_MODE_LEGACY).toString();
                } else {
                    content += Html.fromHtml(quote.content).toString();
                }
            }
            return content;
        } catch (Exception e) {
            return status.reblog != null ? status.reblog.content : status.content;
        }
    }

    /**
     * Parse HTML spoiler text from status
     */
    private static String parseStatusSpoiler(Status status) {
        if (status.spoiler_text == null) {
            return null;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(status.reblog != null ? status.reblog.spoiler_text : status.spoiler_text, Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                return Html.fromHtml(status.reblog != null ? status.reblog.spoiler_text : status.spoiler_text).toString();
            }
        } catch (Exception e) {
            return status.reblog != null ? status.reblog.spoiler_text : status.spoiler_text;
        }
    }

    /**
     * Helper class to hold precompiled filter patterns
     */
    private static class CompiledFilter {
        final Filter filter;
        final List<Pattern> patterns;

        CompiledFilter(Filter filter, List<Pattern> patterns) {
            this.filter = filter;
            this.patterns = patterns;
        }
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
        if (!BaseMainActivity.filterFetched && BaseMainActivity.filterFetchedRetry < 3) {
            try {
                FiltersVM filtersVM = new ViewModelProvider((ViewModelStoreOwner) context).get(FiltersVM.class);
                filtersVM.getFilters(BaseMainActivity.currentInstance, BaseMainActivity.currentToken).observe((LifecycleOwner) context, filters -> {
                    BaseMainActivity.filterFetched = true;
                    BaseMainActivity.mainFilters = filters;
                });
            } catch (Exception e) {
                return notifications;
            }
            BaseMainActivity.filterFetchedRetry++;
        }
        //If there are filters:
        if (BaseMainActivity.mainFilters != null && !BaseMainActivity.mainFilters.isEmpty() && notifications != null && !notifications.isEmpty()) {

            //Loop through filters
            for (Filter filter : BaseMainActivity.mainFilters) {

                if (filter.expires_at != null && filter.expires_at.before(new Date())) {
                    //Expired filter
                    continue;
                }

                if (!filter.context.contains("notifications")) continue;
                if (filter.keywords != null && !filter.keywords.isEmpty()) {
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
