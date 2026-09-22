package app.fedilab.android.mastodon.helper;
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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.client.entities.app.TimelineCacheLogs;
import app.fedilab.android.mastodon.client.entities.app.TimelineLoadLogs;
import app.fedilab.android.mastodon.exception.DBException;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Compare the home cache with what the timeline can really reach
 * The report has no content, no author, no media, no keyword
 */
public class HomeCacheDiffHelper {

    public static final String CONSTRAINT_NONE = "none";
    public static final String CONSTRAINT_FILTER_HIDE = "filter_hide";
    public static final String CONSTRAINT_FILTER_WARN = "filter_warn";
    public static final String CONSTRAINT_APP_MUTE = "app_mute";
    public static final String CONSTRAINT_APP_REBLOG_GROUP = "app_reblog_group";

    public static final String STOP_END_OF_CACHE = "end_of_cache";
    public static final String STOP_DUPLICATE_PAGE = "duplicate_page";
    public static final String STOP_CURSOR_STUCK = "cursor_stuck";
    public static final String STOP_PAGE_LIMIT = "page_limit";

    public static final String SERVER_STOP_COVERED = "cache_covered";
    public static final String SERVER_STOP_END = "end_of_timeline";
    public static final String SERVER_STOP_FAILED = "call_failed";
    public static final String SERVER_STOP_NO_PROGRESS = "no_progress";
    public static final String SERVER_STOP_RATE_LIMITED = "rate_limited";

    private static final String FEDILAB_MUTE_FILTER = "Fedilab";
    private static final String FEDILAB_REBLOG_FILTER = "Fedilab reblog";

    private static final int MAX_PAGES = 500;
    private static final int MAX_SERVER_PAGES = 1000;
    private static final int MAX_RUNS = 200;
    private static final int SERVER_PAGE_LIMIT = 80;
    private static final long SERVER_PAGE_DELAY = 500L;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final long HOUR_IN_MILLIS = 3600000L;
    private static final int MAX_BUCKETS = 24 * 90;
    private static final int BUCKET_WINDOW = 2;
    private static final int BUCKET_FLOOR = 4;
    private static final int BUCKET_FACTOR = 4;
    private static final int REPORT_VERSION = 1;

    /**
     * Cached message reduced to what the analysis needs
     */
    public static class Message {
        public String id;
        public long createdAt;
        public long insertedAt;
        public boolean reblog;
        public String author;
        public int page = -1;
        public boolean onServer;
        public String constraint = CONSTRAINT_NONE;
        public String filter;
    }

    /**
     * One page the timeline would read from the cache
     */
    public static class Page {
        public int index;
        public String requestedMaxId;
        public int size;
        public int fresh;
        public String highestId;
        public String lowestId;
        public String cursorId;
        public int overlap;
    }

    /**
     * Number of cached messages for a given hour
     */
    public static class Bucket {
        public long hour;
        public int count;
        public boolean drop;
    }

    public static class Report {
        public long generatedAt;
        public int loadsRecorded;
        public int emptyLoads;
        public int serverPages;
        public int serverSeen;
        public int serverMissing;
        public int serverCovered;
        public String serverStopReason;
        public String serverLowestId;
        public int aboveMarker;
        public List<String> openGaps = new ArrayList<>();
        public String workerCursor;
        public int cachedAboveCursor;
        public int runsFailed;
        public long lastInsertion;
        public int cached;
        public int reached;
        public int unreached;
        public int pagesRead;
        public String stopReason;
        public String stopId;
        public int medianPerHour;
        public int drops;
        public Map<String, Integer> constraints = new LinkedHashMap<>();
        public List<Message> messages = new ArrayList<>();
        public List<Page> pages = new ArrayList<>();
        public List<Bucket> buckets = new ArrayList<>();
        public List<TimelineLoadLogs> loads = new ArrayList<>();
        public List<TimelineCacheLogs> runs = new ArrayList<>();
        public List<String> missingIds = new ArrayList<>();
        public JSONObject environment;
    }

    /**
     * Run the analysis, from a background thread
     *
     * @param context Context
     * @param account {@link BaseAccount} - account owning the home cache
     * @return {@link Report} or null when the cache is empty
     */
    public static Report analyse(Context context, BaseAccount account) throws DBException {
        StatusCache statusCacheDAO = new StatusCache(context);
        List<Status> cachedStatuses = statusCacheDAO.getHome(account);
        if (cachedStatuses == null || cachedStatuses.isEmpty()) {
            return null;
        }
        Report report = new Report();
        report.generatedAt = System.currentTimeMillis();
        String salt = newSalt();

        List<Status> displayOrder = new ArrayList<>(cachedStatuses);
        Collections.sort(displayOrder, (status1, status2) -> compareDate(status2, status1));
        TimelineHelper.filterStatus(context, displayOrder, Timeline.TimeLineEnum.HOME);

        HashMap<String, Long> insertionDates = statusCacheDAO.getHomeInsertionDates(context, account);
        Map<String, Message> messages = new LinkedHashMap<>();
        for (Status status : displayOrder) {
            Message message = new Message();
            message.id = status.id;
            message.createdAt = status.created_at != null ? status.created_at.getTime() : 0;
            Long insertedAt = insertionDates.get(status.id);
            message.insertedAt = insertedAt != null ? insertedAt : 0;
            message.reblog = status.reblog != null;
            message.author = hash(salt, status.account != null ? status.account.acct : null);
            fillConstraint(message, status.filteredByApp, salt);
            messages.put(message.id, message);
            increment(report.constraints, message.constraint);
        }
        report.messages.addAll(messages.values());
        report.cached = messages.size();
        displayOrder.clear();
        cachedStatuses.clear();

        replayPagination(account, statusCacheDAO, messages, report);
        buildBuckets(report);
        collectLoads(context, account, report);
        collectRuns(context, account, report);
        collectHealth(context, account, report);
        countAboveMarker(context, account, report);
        report.environment = environment(context, account, report, salt);
        return report;
    }

    /**
     * Read the cache like the timeline does, page after page
     */
    private static void replayPagination(BaseAccount account, StatusCache statusCacheDAO,
                                         Map<String, Message> messages, Report report) throws DBException {
        Set<String> displayed = new HashSet<>();
        String maxId = null;
        report.stopReason = STOP_PAGE_LIMIT;
        for (int pageIndex = 0; pageIndex < MAX_PAGES; pageIndex++) {
            List<Status> pageStatuses = statusCacheDAO.geStatuses(Timeline.TimeLineEnum.HOME.getValue(),
                    account.instance, account.user_id, maxId, null, null);
            if (pageStatuses == null || pageStatuses.isEmpty()) {
                report.stopReason = STOP_END_OF_CACHE;
                report.stopId = maxId;
                break;
            }
            Page page = new Page();
            page.index = pageIndex;
            page.requestedMaxId = maxId;
            page.size = pageStatuses.size();
            for (Status status : pageStatuses) {
                if (page.highestId == null || Helper.compareTo(status.id, page.highestId) > 0) {
                    page.highestId = status.id;
                }
                if (page.lowestId == null || Helper.compareTo(status.id, page.lowestId) < 0) {
                    page.lowestId = status.id;
                }
            }
            //Drop displayed messages first
            List<Status> freshStatuses = new ArrayList<>();
            for (Status status : pageStatuses) {
                if (!displayed.contains(status.id)) {
                    freshStatuses.add(status);
                    displayed.add(status.id);
                    Message message = messages.get(status.id);
                    if (message != null && message.page == -1) {
                        message.page = pageIndex;
                        report.reached++;
                    }
                }
            }
            page.fresh = freshStatuses.size();
            if (freshStatuses.isEmpty()) {
                //The timeline gets an empty page and stops loading
                page.cursorId = maxId;
                report.pages.add(page);
                report.stopReason = STOP_DUPLICATE_PAGE;
                report.stopId = maxId;
                break;
            }
            Collections.sort(freshStatuses, (status1, status2) -> compareDate(status2, status1));
            String cursorId = freshStatuses.get(freshStatuses.size() - 1).id;
            page.cursorId = cursorId;
            for (Status status : pageStatuses) {
                if (Helper.compareTo(status.id, cursorId) < 0) {
                    page.overlap++;
                }
            }
            report.pages.add(page);
            //The cursor only moves down
            if (maxId != null && Helper.compareTo(cursorId, maxId) >= 0) {
                report.stopReason = STOP_CURSOR_STUCK;
                report.stopId = maxId;
                break;
            }
            maxId = cursorId;
        }
        report.pagesRead = report.pages.size();
        report.unreached = report.cached - report.reached;
    }

    private static void collectLoads(Context context, BaseAccount account, Report report) throws DBException {
        List<TimelineLoadLogs> loads = new TimelineLoadLogs(context).getHome(account);
        if (loads == null) {
            return;
        }
        report.loads.addAll(loads);
        report.loadsRecorded = loads.size();
        for (TimelineLoadLogs load : loads) {
            if (load.returned == 0) {
                report.emptyLoads++;
            }
        }
    }

    private static void collectHealth(Context context, BaseAccount account, Report report) throws DBException {
        report.openGaps.addAll(new StatusCache(context).getRecordedGaps(account.user_id, account.instance));
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        report.workerCursor = sharedpreferences.getString(context.getString(R.string.SET_HOME_FETCH_CURSOR) + account.user_id + account.instance, null);
        for (Message message : report.messages) {
            if (report.workerCursor != null && Helper.compareTo(message.id, report.workerCursor) > 0) {
                report.cachedAboveCursor++;
            }
            if (message.insertedAt > report.lastInsertion) {
                report.lastInsertion = message.insertedAt;
            }
        }
        for (TimelineCacheLogs run : report.runs) {
            if (run.failed > 0) {
                report.runsFailed++;
            }
        }
    }

    private static void collectRuns(Context context, BaseAccount account, Report report) throws DBException {
        List<TimelineCacheLogs> runs = new TimelineCacheLogs(context).getHome(account);
        if (runs != null) {
            report.runs.addAll(runs.subList(Math.max(0, runs.size() - MAX_RUNS), runs.size()));
        }
    }

    private static void buildBuckets(Report report) {
        Map<Long, Integer> counts = new HashMap<>();
        long oldest = Long.MAX_VALUE;
        long newest = 0;
        for (Message message : report.messages) {
            if (message.createdAt == 0) {
                continue;
            }
            long hour = message.createdAt / HOUR_IN_MILLIS;
            Integer count = counts.get(hour);
            counts.put(hour, count == null ? 1 : count + 1);
            oldest = Math.min(oldest, hour);
            newest = Math.max(newest, hour);
        }
        if (counts.isEmpty()) {
            return;
        }
        List<Integer> filled = new ArrayList<>(counts.values());
        Collections.sort(filled);
        report.medianPerHour = filled.get(filled.size() / 2);
        oldest = Math.max(oldest, newest - MAX_BUCKETS);
        for (long hour = newest; hour >= oldest; hour--) {
            Bucket bucket = new Bucket();
            bucket.hour = hour * HOUR_IN_MILLIS;
            Integer count = counts.get(hour);
            bucket.count = count == null ? 0 : count;
            report.buckets.add(bucket);
        }
        flagDrops(report);
    }

    private static void flagDrops(Report report) {
        for (int index = 0; index < report.buckets.size(); index++) {
            List<Integer> neighbours = new ArrayList<>();
            for (int around = index - BUCKET_WINDOW; around <= index + BUCKET_WINDOW; around++) {
                if (around >= 0 && around < report.buckets.size() && around != index) {
                    neighbours.add(report.buckets.get(around).count);
                }
            }
            if (neighbours.isEmpty()) {
                continue;
            }
            Collections.sort(neighbours);
            int local = neighbours.get(neighbours.size() / 2);
            Bucket bucket = report.buckets.get(index);
            bucket.drop = local >= BUCKET_FLOOR && bucket.count * BUCKET_FACTOR < local;
            if (bucket.drop) {
                report.drops++;
            }
        }
    }

    private static void countAboveMarker(Context context, BaseAccount account, Report report) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String markerKey = context.getString(R.string.SET_INNER_MARKER) + account.user_id + account.instance + Timeline.TimeLineEnum.HOME.getValue();
        String marker = sharedpreferences.getString(markerKey, null);
        if (marker == null) {
            return;
        }
        for (Message message : report.messages) {
            if (Helper.compareTo(message.id, marker) > 0) {
                report.aboveMarker++;
            }
        }
    }

    /**
     * Settings and filter shapes, no keyword, no account
     */
    private static JSONObject environment(Context context, BaseAccount account, Report report, String salt) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        JSONObject environment = new JSONObject();
        try {
            environment.put("app_version", BuildConfig.VERSION_NAME);
            environment.put("version_code", BuildConfig.VERSION_CODE);
            environment.put("android_sdk", Build.VERSION.SDK_INT);
            environment.put("statuses_per_call", MastodonHelper.statusesPerCall(context));
            environment.put("use_cache", sharedpreferences.getBoolean(context.getString(R.string.SET_USE_CACHE), true));
            environment.put("group_reblogs", sharedpreferences.getBoolean(context.getString(R.string.SET_GROUP_REBLOGS), true));
            environment.put("reverse_timeline", sharedpreferences.getBoolean(context.getString(R.string.SET_REVERSE_TIMELINE), false));
            environment.put("auto_fetch_missing", sharedpreferences.getBoolean(context.getString(R.string.SET_AUTO_FETCH_MISSING_MESSAGES), false));
            environment.put("home_muted_accounts", BaseMainActivity.filteredAccounts != null ? BaseMainActivity.filteredAccounts.size() : 0);
            String markerKey = context.getString(R.string.SET_INNER_MARKER) + account.user_id + account.instance + Timeline.TimeLineEnum.HOME.getValue();
            String marker = sharedpreferences.getString(markerKey, null);
            environment.put("inner_marker", marker != null ? marker : JSONObject.NULL);
            environment.put("above_marker", report.aboveMarker);
            JSONArray filters = new JSONArray();
            if (BaseMainActivity.mainFilters != null) {
                for (Filter filter : BaseMainActivity.mainFilters) {
                    JSONObject filterObject = new JSONObject();
                    filterObject.put("id", hash(salt, filter.title));
                    filterObject.put("action", filter.filter_action);
                    filterObject.put("contexts", filter.context != null ? new JSONArray(filter.context) : new JSONArray());
                    filterObject.put("keywords", filter.keywords != null ? filter.keywords.size() : 0);
                    filterObject.put("statuses", filter.statuses != null ? filter.statuses.size() : 0);
                    filterObject.put("expired", filter.expires_at != null && filter.expires_at.before(new Date()));
                    filters.put(filterObject);
                }
            }
            environment.put("filters", filters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return environment;
    }

    /**
     * Ask the server for the same range and see what the cache never got
     *
     * @param context Context
     * @param account {@link BaseAccount} - account owning the home cache
     * @param report  {@link Report} - filled with what the server returns
     */
    public static void compareWithServer(Context context, BaseAccount account, Report report) {
        MastodonTimelinesService mastodonTimelinesService = init(context, account.instance);
        Map<String, Message> messages = new HashMap<>();
        String oldestCachedId = null;
        String newestCachedId = null;
        for (Message message : report.messages) {
            messages.put(message.id, message);
            if (oldestCachedId == null || Helper.compareTo(message.id, oldestCachedId) < 0) {
                oldestCachedId = message.id;
            }
            if (newestCachedId == null || Helper.compareTo(message.id, newestCachedId) > 0) {
                newestCachedId = message.id;
            }
        }
        report.serverPages = 0;
        report.serverSeen = 0;
        report.serverMissing = 0;
        report.serverCovered = 0;
        report.serverLowestId = null;
        report.serverStopReason = STOP_PAGE_LIMIT;
        report.missingIds.clear();
        String maxId = null;
        for (int pageIndex = 0; pageIndex < MAX_SERVER_PAGES; pageIndex++) {
            List<Status> serverStatuses;
            try {
                if (pageIndex > 0) {
                    Thread.sleep(SERVER_PAGE_DELAY);
                }
                Response<List<Status>> response = mastodonTimelinesService.getHome(account.token, maxId, null, null, SERVER_PAGE_LIMIT, null).execute();
                if (response.code() == HTTP_TOO_MANY_REQUESTS) {
                    report.serverStopReason = SERVER_STOP_RATE_LIMITED;
                    break;
                }
                serverStatuses = response.isSuccessful() ? response.body() : null;
            } catch (Exception e) {
                e.printStackTrace();
                report.serverStopReason = SERVER_STOP_FAILED;
                break;
            }
            if (serverStatuses == null) {
                report.serverStopReason = SERVER_STOP_FAILED;
                break;
            }
            if (serverStatuses.isEmpty()) {
                report.serverStopReason = SERVER_STOP_END;
                break;
            }
            report.serverPages++;
            String lowestId = null;
            for (Status status : serverStatuses) {
                report.serverSeen++;
                Message message = messages.get(status.id);
                if (message != null) {
                    message.onServer = true;
                } else if (oldestCachedId != null && newestCachedId != null
                        && Helper.compareTo(status.id, oldestCachedId) > 0
                        && Helper.compareTo(status.id, newestCachedId) < 0) {
                    //Inside the cached range
                    report.serverMissing++;
                    report.missingIds.add(status.id);
                }
                if (lowestId == null || Helper.compareTo(status.id, lowestId) < 0) {
                    lowestId = status.id;
                }
            }
            if (lowestId == null || (maxId != null && Helper.compareTo(lowestId, maxId) >= 0)) {
                report.serverStopReason = SERVER_STOP_NO_PROGRESS;
                break;
            }
            maxId = lowestId;
            report.serverLowestId = lowestId;
            if (oldestCachedId != null && Helper.compareTo(maxId, oldestCachedId) <= 0) {
                report.serverStopReason = SERVER_STOP_COVERED;
                break;
            }
        }
        if (report.serverLowestId != null) {
            for (Message message : report.messages) {
                if (Helper.compareTo(message.id, report.serverLowestId) >= 0) {
                    report.serverCovered++;
                }
            }
        }
    }

    private static MastodonTimelinesService init(Context context, String instance) {
        OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    /**
     * Write the report as json in the download folder
     *
     * @param context Context
     * @param report  {@link Report}
     * @return String - where the report was written
     */
    public static String export(Context context, Report report) throws IOException, JSONException {
        JSONObject root = new JSONObject();
        root.put("report_version", REPORT_VERSION);
        root.put("generated_at", report.generatedAt / 1000);
        root.put("environment", report.environment);

        JSONObject cache = new JSONObject();
        cache.put("count", report.cached);
        cache.put("reached", report.reached);
        cache.put("unreached", report.unreached);
        cache.put("median_per_hour", report.medianPerHour);
        cache.put("frequency_drops", report.drops);
        cache.put("loads_recorded", report.loadsRecorded);
        cache.put("empty_loads", report.emptyLoads);
        root.put("cache", cache);

        JSONObject constraints = new JSONObject();
        for (Map.Entry<String, Integer> constraint : report.constraints.entrySet()) {
            constraints.put(constraint.getKey(), constraint.getValue());
        }
        root.put("constraints", constraints);

        JSONObject pagination = new JSONObject();
        pagination.put("pages", report.pagesRead);
        pagination.put("stop_reason", report.stopReason);
        pagination.put("stop_id", report.stopId != null ? report.stopId : JSONObject.NULL);
        root.put("pagination", pagination);

        JSONArray pages = new JSONArray();
        for (Page page : report.pages) {
            JSONObject pageObject = new JSONObject();
            pageObject.put("index", page.index);
            pageObject.put("max_id", page.requestedMaxId != null ? page.requestedMaxId : JSONObject.NULL);
            pageObject.put("size", page.size);
            pageObject.put("fresh", page.fresh);
            pageObject.put("highest_id", page.highestId);
            pageObject.put("lowest_id", page.lowestId);
            pageObject.put("cursor_id", page.cursorId);
            pageObject.put("overlap", page.overlap);
            pages.put(pageObject);
        }
        root.put("pages", pages);

        JSONArray buckets = new JSONArray();
        for (Bucket bucket : report.buckets) {
            JSONObject bucketObject = new JSONObject();
            bucketObject.put("hour", bucket.hour / 1000);
            bucketObject.put("count", bucket.count);
            bucketObject.put("drop", bucket.drop);
            buckets.put(bucketObject);
        }
        root.put("frequency", buckets);

        JSONObject health = new JSONObject();
        health.put("open_gaps", report.openGaps.size());
        health.put("open_gap_ids", new JSONArray(report.openGaps));
        health.put("worker_cursor", report.workerCursor != null ? report.workerCursor : JSONObject.NULL);
        health.put("cached_above_cursor", report.cachedAboveCursor);
        health.put("background_runs", report.runs.size());
        health.put("background_runs_failed", report.runsFailed);
        health.put("last_insertion", report.lastInsertion / 1000);
        health.put("minutes_since_last_insertion", report.lastInsertion > 0 ? (report.generatedAt - report.lastInsertion) / 60000 : -1);
        root.put("health", health);

        JSONObject server = new JSONObject();
        server.put("pages", report.serverPages);
        server.put("seen", report.serverSeen);
        server.put("missing", report.serverMissing);
        server.put("covered", report.serverCovered);
        server.put("stop_reason", report.serverStopReason != null ? report.serverStopReason : JSONObject.NULL);
        server.put("lowest_id", report.serverLowestId != null ? report.serverLowestId : JSONObject.NULL);
        server.put("missing_ids", new JSONArray(report.missingIds));
        root.put("server", server);

        JSONArray loads = new JSONArray();
        for (TimelineLoadLogs load : report.loads) {
            JSONObject loadObject = new JSONObject();
            loadObject.put("created_at", load.created_at != null ? load.created_at.getTime() / 1000 : 0);
            loadObject.put("direction", load.direction);
            loadObject.put("source", load.source);
            loadObject.put("max_id", load.max_id != null ? load.max_id : JSONObject.NULL);
            loadObject.put("min_id", load.min_id != null ? load.min_id : JSONObject.NULL);
            loadObject.put("returned", load.returned);
            loadObject.put("displayed", load.displayed);
            loadObject.put("added", load.added);
            loadObject.put("fetching_missing", load.fetching_missing);
            loadObject.put("trigger", load.trigger != null ? load.trigger : JSONObject.NULL);
            loads.put(loadObject);
        }
        root.put("loads", loads);

        JSONArray runs = new JSONArray();
        for (TimelineCacheLogs run : report.runs) {
            JSONObject runObject = new JSONObject();
            runObject.put("created_at", run.created_at != null ? run.created_at.getTime() / 1000 : 0);
            runObject.put("fetched", run.fetched);
            runObject.put("inserted", run.inserted);
            runObject.put("updated", run.updated);
            runObject.put("failed", run.failed);
            runObject.put("frequency", run.frequency);
            runs.put(runObject);
        }
        root.put("background_fetches", runs);

        JSONArray messages = new JSONArray();
        for (Message message : report.messages) {
            JSONObject messageObject = new JSONObject();
            messageObject.put("id", message.id);
            messageObject.put("created_at", message.createdAt / 1000);
            messageObject.put("inserted_at", message.insertedAt / 1000);
            messageObject.put("reblog", message.reblog);
            messageObject.put("author", message.author);
            messageObject.put("page", message.page);
            messageObject.put("constraint", message.constraint);
            //Outside the walked range we know nothing
            if (report.serverLowestId != null && Helper.compareTo(message.id, report.serverLowestId) >= 0) {
                messageObject.put("on_server", message.onServer);
            }
            if (message.filter != null) {
                messageObject.put("filter", message.filter);
            }
            messages.put(messageObject);
        }
        root.put("messages", messages);

        String fileName = "Fedilab_home_cache_report_"
                + Helper.dateFileToString(context, new Date(report.generatedAt)) + ".json";
        return writeInDownloads(context, fileName, root.toString());
    }

    private static String writeInDownloads(Context context, String fileName, String content) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Download folder is not writable");
            }
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Download folder is not writable");
                }
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return Environment.DIRECTORY_DOWNLOADS + "/" + fileName;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file.getAbsolutePath();
    }

    private static void fillConstraint(Message message, Filter filter, String salt) {
        if (filter == null) {
            message.constraint = CONSTRAINT_NONE;
            return;
        }
        if (FEDILAB_MUTE_FILTER.equals(filter.title)) {
            message.constraint = CONSTRAINT_APP_MUTE;
            return;
        }
        if (FEDILAB_REBLOG_FILTER.equals(filter.title)) {
            message.constraint = CONSTRAINT_APP_REBLOG_GROUP;
            return;
        }
        message.constraint = "hide".equals(filter.filter_action) ? CONSTRAINT_FILTER_HIDE : CONSTRAINT_FILTER_WARN;
        message.filter = hash(salt, filter.title);
    }

    private static int compareDate(Status status1, Status status2) {
        if (status1.created_at == null || status2.created_at == null) {
            return 0;
        }
        return status1.created_at.compareTo(status2.created_at);
    }

    private static void increment(Map<String, Integer> counters, String key) {
        Integer count = counters.get(key);
        counters.put(key, count == null ? 1 : count + 1);
    }

    /**
     * Random salt, never exported
     *
     * @return String - salt of the current report
     */
    private static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value));
        }
        return builder.toString();
    }

    private static String hash(String salt, String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((salt + value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 4; index++) {
                builder.append(String.format(Locale.US, "%02x", bytes[index]));
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
