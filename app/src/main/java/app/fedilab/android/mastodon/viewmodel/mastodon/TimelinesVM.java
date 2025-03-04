package app.fedilab.android.mastodon.viewmodel.mastodon;
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


import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.IDN;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Conversation;
import app.fedilab.android.mastodon.client.entities.api.Conversations;
import app.fedilab.android.mastodon.client.entities.api.Marker;
import app.fedilab.android.mastodon.client.entities.api.MastodonList;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.client.entities.lemmy.LemmyPost;
import app.fedilab.android.mastodon.client.entities.misskey.MisskeyNote;
import app.fedilab.android.mastodon.client.entities.nitter.Nitter;
import app.fedilab.android.mastodon.client.entities.peertube.PeertubeVideo;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.TimelineHelper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonTimeline;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class TimelinesVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());


    private MutableLiveData<List<Account>> accountListMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;
    private MutableLiveData<List<StatusDraft>> statusDraftListMutableLiveData;
    private MutableLiveData<Status> statusMutableLiveData;
    private MutableLiveData<Statuses> statusesMutableLiveData;
    private MutableLiveData<PeertubeVideo.Video> peertubeVideoMutableLiveData;
    private MutableLiveData<Conversations> conversationListMutableLiveData;
    private MutableLiveData<MastodonList> mastodonListMutableLiveData;
    private MutableLiveData<List<MastodonList>> mastodonListListMutableLiveData;
    private MutableLiveData<Marker> markerMutableLiveData;
    private MutableLiveData<List<Status>> statusListMutableLiveData;
    private MutableLiveData<List<Tag>> tagListMutableLiveData;

    public TimelinesVM(@NonNull Application application) {
        super(application);
    }


    private static void sortDesc(List<Status> statusList) {
        Collections.sort(statusList, (obj1, obj2) -> obj2.id.compareToIgnoreCase(obj1.id));
    }

    public static void sortAsc(List<Status> statusList) {
        Collections.sort(statusList, (obj1, obj2) -> obj1.id.compareToIgnoreCase(obj2.id));
    }


    private static void sortDescConv(List<Conversation> conversationList) {
        Collections.sort(conversationList, (obj1, obj2) -> obj2.id.compareToIgnoreCase(obj1.id));
    }

    private static void addFetchMore(List<Status> statusList, List<Status> timelineStatuses, TimelineParams timelineParams) throws DBException {
        if (statusList != null && statusList.size() > 1 && timelineStatuses != null && timelineStatuses.size() > 0) {
            sortDesc(statusList);
            if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.REFRESH || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.FETCH_NEW) {
                //When refreshing/scrolling to TOP, if last statuses fetched has a greater id from newest in cache, there is potential hole
                if (!timelineStatuses.contains(statusList.get(statusList.size() - 1))) {
                    statusList.get(statusList.size() - 1).isFetchMore = true;
                    statusList.get(statusList.size() - 1).positionFetchMore = Status.PositionFetchMore.TOP;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.TOP && timelineParams.fetchingMissing) {
                if (!timelineStatuses.contains(statusList.get(0))) {
                    statusList.get(0).isFetchMore = true;
                    statusList.get(0).positionFetchMore = Status.PositionFetchMore.BOTTOM;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.BOTTOM && timelineParams.fetchingMissing) {
                if (!timelineStatuses.contains(statusList.get(statusList.size() - 1))) {
                    statusList.get(statusList.size() - 1).isFetchMore = true;
                    statusList.get(statusList.size() - 1).positionFetchMore = Status.PositionFetchMore.TOP;
                }
            }
        }
    }

    private static void addFetchMoreConversation(List<Conversation> conversationList, List<Conversation> timelineConversations, TimelineParams timelineParams) throws DBException {
        if (conversationList != null && conversationList.size() > 1 && timelineConversations != null && timelineConversations.size() > 0) {
            sortDescConv(conversationList);
            if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.REFRESH || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.SCROLL_TOP || timelineParams.direction == FragmentMastodonTimeline.DIRECTION.FETCH_NEW) {
                //When refreshing/scrolling to TOP, if last statuses fetched has a greater id from newest in cache, there is potential hole
                if (!timelineConversations.contains(conversationList.get(conversationList.size() - 1))) {
                    conversationList.get(conversationList.size() - 1).isFetchMore = true;
                    conversationList.get(conversationList.size() - 1).positionFetchMore = Conversation.PositionFetchMore.TOP;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.TOP && timelineParams.fetchingMissing) {
                if (!timelineConversations.contains(conversationList.get(0))) {
                    conversationList.get(0).isFetchMore = true;
                    conversationList.get(0).positionFetchMore = Conversation.PositionFetchMore.BOTTOM;
                }
            } else if (timelineParams.direction == FragmentMastodonTimeline.DIRECTION.BOTTOM && timelineParams.fetchingMissing) {
                if (!timelineConversations.contains(conversationList.get(conversationList.size() - 1))) {
                    conversationList.get(conversationList.size() - 1).isFetchMore = true;
                    conversationList.get(conversationList.size() - 1).positionFetchMore = Conversation.PositionFetchMore.TOP;
                }
            }
        }
    }

    private MastodonTimelinesService initInstanceOnly(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null))
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    private MastodonTimelinesService initInstanceXMLOnly(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null))
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    private MastodonTimelinesService initInstanceHtmlOnly(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    private MastodonTimelinesService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    public LiveData<Statuses> getStatusTrends(String token, @NonNull String instance, String max_id, Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<Status>> publicTlCall = mastodonTimelinesService.getStatusTrends(token, max_id, limit);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<List<Status>> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        List<Status> statusList = publicTlResponse.body();
                        statuses.statuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), statusList, Timeline.TimeLineEnum.TREND_MESSAGE);
                        statuses.pagination = MastodonHelper.getOffSetPagination(publicTlResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<List<Tag>> getTagsTrends(String token, @NonNull String instance, Integer offset, Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        tagListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<Tag>> publicTlCall = mastodonTimelinesService.getTagTrends(token, offset, limit);
            List<Tag> tagList = null;
            if (publicTlCall != null) {
                try {
                    Response<List<Tag>> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        tagList = publicTlResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<Tag> finalTagList = tagList;
            Runnable myRunnable = () -> tagListMutableLiveData.setValue(finalTagList);
            mainHandler.post(myRunnable);
        }).start();
        return tagListMutableLiveData;
    }

    /**
     * Public timeline for Nitter
     *
     * @param max_position Return results older than this id
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getNitterRSS(
            String accountsStr,
            String max_position) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceXMLOnly("nitter.fedilab.app");
        accountsStr = accountsStr.replaceAll("\\s", ",");

        statusesMutableLiveData = new MutableLiveData<>();
        String finalAccountsStr = accountsStr;

        new Thread(() -> {
            Call<Nitter> publicTlCall = mastodonTimelinesService.getNitter(finalAccountsStr, max_position);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<Nitter> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        Nitter rssResponse = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (rssResponse != null && rssResponse.mFeedItems != null) {
                            for (Nitter.FeedItem feedItem : rssResponse.mFeedItems) {
                                if (!feedItem.title.startsWith("RT by")) {
                                    Status status = Nitter.convert(getApplication(), "nitter.fedilab.app", feedItem);
                                    statusList.add(status);
                                }
                            }
                        }
                        statuses.statuses = statusList;
                        String max_id = publicTlResponse.headers().get("min-id");
                        statuses.pagination = new Pagination();
                        statuses.pagination.max_id = max_id;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }



    /**
     * Public timeline for Nitter
     *
     * @param max_position Return results older than this id
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getNitterHTML(
            String accountsStr,
            String max_position) {
        statusesMutableLiveData = new MutableLiveData<>();
        Context context = getApplication().getApplicationContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String nitterInstance = sharedpreferences.getString(context.getString(R.string.SET_NITTER_HOST), context.getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
        final String fedilabInstance =  "nitter.fedilab.app";
        accountsStr = accountsStr.replaceAll("\\s", ",").replaceAll(",,",",");
        String cursor = max_position == null ? "" : max_position;
        String url = "https://"+fedilabInstance+"/" + accountsStr + "/with_replies" +cursor;
        Request request = new Request.Builder()
                .header("User-Agent", context.getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME + "/" + BuildConfig.VERSION_CODE)
                .url(url)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                Statuses statuses = new Statuses();

                if (response.isSuccessful()) {
                    try {
                        String data = response.body().string();
                        Document doc = Jsoup.parse(data);
                        Elements timelineItems = doc.select(".timeline-item");

                        List<Status> statusList = new ArrayList<>();
                        for(Element timelineItem: timelineItems) {
                            if(!timelineItem.select(".unavailable").html().isEmpty() || timelineItem.select(".tweet-link").attr("href").isEmpty()) {
                                continue;
                            }
                            //RT
                            boolean isBoosted = !timelineItem.select(".retweet-header").select(".icon-container").isEmpty();
                            Status status = Nitter.nitterHTMLParser(context, timelineItem, nitterInstance);

                            //Quoted message

                            if(!timelineItem.select(".quote").html().isEmpty()) {
                                status.quote = Nitter.nitterHTMLParser(context, timelineItem.select(".quote").first(), nitterInstance);
                            }

                            Status finalStatus;
                            if(isBoosted) {
                                finalStatus = new Status();
                                finalStatus.reblog = status;
                                finalStatus.id = status.id;
                                finalStatus.visibility = "public";
                                finalStatus.url = "https://"+ nitterInstance +timelineItem.select(".tweet-link").attr("href");
                                finalStatus.uri = finalStatus.url;
                                Account acccountOriginal = new Account();
                                acccountOriginal.display_name = timelineItem.select(".retweet-header").select(".icon-container").text();
                                finalStatus.account = acccountOriginal;
                            } else {
                                finalStatus = status;
                            }
                            statusList.add(finalStatus);
                        }

                        statuses.statuses = statusList;
                        Elements elementsShow = doc.select(".show-more a");
                        Element showMore = null;
                        if(elementsShow.size() > 1) {
                            showMore = elementsShow.get(elementsShow.size()-1);
                        } else {
                            showMore = elementsShow.get(0);
                        }
                        String cursor = showMore.attr("href");
                        statuses.pagination = new Pagination();
                        statuses.pagination.max_id = cursor;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
                mainHandler.post(myRunnable);
            }
        });

        return statusesMutableLiveData;
    }

    /**
     * Public timeline for Misskey
     *
     * @param untilId Return results older than this id
     * @param limit   Maximum number of results to return. Defaults to 20.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getMisskey(@NonNull String instance,
                                         String untilId,
                                         Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceOnly(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyNote.MisskeyParams misskeyParams = new MisskeyNote.MisskeyParams();
            misskeyParams.untilId = untilId;
            misskeyParams.limit = limit;
            Call<List<MisskeyNote>> publicTlCall = mastodonTimelinesService.getMisskey(misskeyParams);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<List<MisskeyNote>> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        List<MisskeyNote> misskeyNoteList = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (misskeyNoteList != null) {
                            for (MisskeyNote misskeyNote : misskeyNoteList) {
                                Status status = MisskeyNote.convert(misskeyNote, instance);
                                statusList.add(status);
                            }
                        }
                        statuses.statuses = TimelineHelper.filterStatus(getApplication(), statusList, Timeline.TimeLineEnum.PUBLIC);
                        statuses.pagination = new Pagination();
                        if (statusList.size() > 0) {
                            statuses.pagination.min_id = statusList.get(0).id;
                            statuses.pagination.max_id = statusList.get(statusList.size() - 1).id;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }

    /**
     * Public discover timeline for Pixelfed
     *
     * @param instance String Pixelfed instance
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getPixelfedDiscoverTrending(String instance) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = initInstanceOnly(instance+"/api/pixelfed/v2/");
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> timelineCall = mastodonTimelinesService.getPixelDiscoverTrending("daily");
            if (timelineCall != null) {
                try {
                    Response<List<Status>> timelineResponse = timelineCall.execute();
                    if (timelineResponse.isSuccessful()) {
                        statuses.statuses = timelineResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }


    /**
     * Public timeline for Lemmy
     *
     * @param post_id Return comments for post_id, if null it's for main threads
     * @param page    Return results from this page
     * @param limit   Maximum number of results to return. Defaults to 20.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getLemmy(@NonNull String instance, String post_id,
                                       String page,
                                       Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceOnly(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        if (page == null) {
            page = "1";
        }
        String finalPage = page;
        new Thread(() -> {
            Call<LemmyPost.LemmyPosts> lemmyPostsCall = null;
            Call<LemmyPost.LemmyComments> lemmyCommentsCall = null;
            if (post_id == null) {
                lemmyPostsCall = mastodonTimelinesService.getLemmyMain(limit, finalPage);
            } else {
                lemmyCommentsCall = mastodonTimelinesService.getLemmyThread(post_id, limit, finalPage);
            }
            Statuses statuses = new Statuses();
            if (lemmyPostsCall != null) {
                try {
                    Response<LemmyPost.LemmyPosts> publicTlResponse = lemmyPostsCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        LemmyPost.LemmyPosts lemmyPosts = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (lemmyPosts != null) {
                            for (LemmyPost lemmyPost : lemmyPosts.posts) {
                                Status status = LemmyPost.convert(lemmyPost, instance);
                                statusList.add(status);
                            }
                        }
                        statuses.statuses = TimelineHelper.filterStatus(getApplication(), statusList, Timeline.TimeLineEnum.PUBLIC);
                        statuses.pagination = new Pagination();
                        int pageInt = Integer.parseInt(finalPage);
                        statuses.pagination.min_id = finalPage;
                        statuses.pagination.max_id = String.valueOf((pageInt + 1));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (lemmyCommentsCall != null) {
                try {
                    Response<LemmyPost.LemmyComments> publicTlResponse = lemmyCommentsCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        LemmyPost.LemmyComments lemmyComments = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (lemmyComments != null) {
                            for (LemmyPost lemmyPost : lemmyComments.comments) {
                                Status status = LemmyPost.convert(lemmyPost, instance);
                                statusList.add(status);
                            }
                        }
                        statuses.statuses = TimelineHelper.filterStatus(getApplication(), statusList, Timeline.TimeLineEnum.PUBLIC);
                        statuses.pagination = new Pagination();
                        if (finalPage == null) {
                            statuses.pagination.min_id = "0";
                            statuses.pagination.max_id = "1";
                        } else {
                            int pageInt = Integer.parseInt(finalPage);
                            statuses.pagination.min_id = finalPage;
                            statuses.pagination.max_id = String.valueOf((pageInt + 1));
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }


    /**
     * Public timeline for Peertube
     *
     * @param maxId Return results older than this id
     * @param limit Maximum number of results to return. Defaults to 20.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getPeertube(@NonNull String instance,
                                          String maxId,
                                          Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceOnly(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<PeertubeVideo> publicTlCall = mastodonTimelinesService.getPeertube(maxId, "local", "-publishedAt", limit);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<PeertubeVideo> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        PeertubeVideo peertubeVideo = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (peertubeVideo != null) {
                            for (PeertubeVideo.Video video : peertubeVideo.data) {
                                Status status = PeertubeVideo.convert(video);
                                statusList.add(status);
                            }
                        }
                        statuses.statuses = TimelineHelper.filterStatus(getApplication(), statusList, Timeline.TimeLineEnum.PUBLIC);
                        statuses.pagination = new Pagination();
                        if (statusList.size() > 0) {
                            //These values are not used.
                            statuses.pagination.min_id = null;
                            statuses.pagination.max_id = null;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }

    /**
     * Returns details for a peertube video
     *
     * @return {@link LiveData} containing a {@link PeertubeVideo.Video}
     */
    public LiveData<PeertubeVideo.Video> getPeertubeVideo(@NonNull String instance, String id) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceOnly(instance);
        peertubeVideoMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<PeertubeVideo.Video> publicTlCall = mastodonTimelinesService.getPeertubeVideo(id);
            PeertubeVideo.Video peertubeVideo = null;
            try {
                Response<PeertubeVideo.Video> videoResponse = publicTlCall.execute();
                if (videoResponse.isSuccessful()) {
                    peertubeVideo = videoResponse.body();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            PeertubeVideo.Video finalPeertubeVideo = peertubeVideo;
            Runnable myRunnable = () -> peertubeVideoMutableLiveData.setValue(finalPeertubeVideo);
            mainHandler.post(myRunnable);
        }).start();
        return peertubeVideoMutableLiveData;
    }

    public LiveData<Statuses> getTimeline(List<Status> timelineStatuses, TimelineParams timelineParams) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(timelineParams.instance);
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> timelineCall = null;
            switch (timelineParams.type) {
                case HOME:
                    timelineCall = mastodonTimelinesService.getHome(timelineParams.token, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit, timelineParams.local);
                    break;
                case REMOTE:
                case LOCAL:
                    timelineCall = mastodonTimelinesService.getPublic(timelineParams.token, true, false, timelineParams.onlyMedia, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                    break;
                case PUBLIC:
                    timelineCall = mastodonTimelinesService.getPublic(timelineParams.token, false, true, timelineParams.onlyMedia, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                    break;
                case BUBBLE:
                    timelineCall = mastodonTimelinesService.getBubble(timelineParams.token, timelineParams.onlyMedia, timelineParams.remote, timelineParams.withMuted, timelineParams.excludeVisibilities, timelineParams.replyVisibility, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                    break;
                case ART:
                case TAG:
                    timelineCall = mastodonTimelinesService.getHashTag(timelineParams.token, timelineParams.hashtagTrim, timelineParams.local, timelineParams.onlyMedia, timelineParams.all, timelineParams.any, timelineParams.none, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                    break;
                case LIST:
                    timelineCall = mastodonTimelinesService.getList(timelineParams.token, timelineParams.listId, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
                    break;
            }
            if (timelineCall != null) {
                try {
                    Response<List<Status>> timelineResponse = timelineCall.execute();
                    if (timelineResponse.isSuccessful()) {
                        List<Status> statusList = timelineResponse.body();
                        statuses.statuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), statusList, timelineParams.type);
                        statuses.pagination = MastodonHelper.getPagination(timelineResponse.headers());
                        if (statuses.statuses != null && !statuses.statuses.isEmpty()) {
                            //Fetch More is added on filtered statuses
                            addFetchMore(statuses.statuses, timelineStatuses, timelineParams);
                            //All statuses (even filtered will be added to cache)
                            if (statusList != null && !statusList.isEmpty()) {
                                for (Status status : statusList) {
                                    StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
                                    StatusCache statusCache = new StatusCache();
                                    statusCache.instance = timelineParams.instance;
                                    statusCache.user_id = timelineParams.userId;
                                    statusCache.status = status;
                                    statusCache.type = timelineParams.type;
                                    statusCache.status_id = status.id;
                                    try {
                                        int inserted = statusCacheDAO.insertOrUpdate(statusCache, timelineParams.slug);
                                        if (inserted == 0) {
                                            status.cached = true;
                                        }
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getTimelineCache(List<Status> timelineStatuses, TimelineParams timelineParams) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
            Statuses statuses = new Statuses();
            try {
                List<Status> statusesDb = statusCacheDAO.geStatuses(timelineParams.slug, timelineParams.instance, timelineParams.userId, timelineParams.maxId, timelineParams.minId, timelineParams.sinceId);
                if (statusesDb != null && statusesDb.size() > 0) {
                    if (timelineStatuses != null) {
                        List<Status> notPresentStatuses = new ArrayList<>();
                        for (Status status : statusesDb) {
                            if (!timelineStatuses.contains(status)) {
                                status.cached = true;
                                notPresentStatuses.add(status);
                            }
                        }
                        //Only not already present statuses are added
                        statusesDb = notPresentStatuses;
                    }
                    statuses.statuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), statusesDb, timelineParams.type);
                    if (statuses.statuses.size() > 0) {
                        addFetchMore(statuses.statuses, timelineStatuses, timelineParams);
                        statuses.pagination = new Pagination();
                        statuses.pagination.min_id = statuses.statuses.get(0).id;
                        statuses.pagination.max_id = statuses.statuses.get(statuses.statuses.size() - 1).id;
                    }
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }


    /**
     * Get user drafts
     *
     * @param account app.fedilab.android.client.entities.app.Account
     * @return LiveData<ist < StatusDraft>>
     */
    public LiveData<List<StatusDraft>> getDrafts(BaseAccount account) {
        statusDraftListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            List<StatusDraft> statusCacheDAO = null;
            try {
                statusCacheDAO = new StatusDraft(getApplication().getApplicationContext()).geStatusDraftList(account);
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<StatusDraft> finalStatusCacheDAO = statusCacheDAO;
            Runnable myRunnable = () -> statusDraftListMutableLiveData.setValue(finalStatusCacheDAO);
            mainHandler.post(myRunnable);
        }).start();
        return statusDraftListMutableLiveData;
    }


    /**
     * Show conversations
     *
     * @return {@link LiveData} containing a {@link Conversations}
     */
    public LiveData<Conversations> getConversations(List<Conversation> conversationsTimeline, TimelineParams timelineParams) {
        conversationListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(timelineParams.instance);
        new Thread(() -> {
            Conversations conversations = new Conversations();
            Call<List<Conversation>> conversationsCall = mastodonTimelinesService.getConversations(timelineParams.token, timelineParams.maxId, timelineParams.sinceId, timelineParams.minId, timelineParams.limit);
            if (conversationsCall != null) {
                try {
                    Response<List<Conversation>> conversationsResponse = conversationsCall.execute();
                    if (conversationsResponse.isSuccessful()) {
                        conversations.conversations = conversationsResponse.body();
                        conversations.pagination = MastodonHelper.getPagination(conversationsResponse.headers());
                        if (conversations.conversations != null && conversations.conversations.size() > 0) {
                            addFetchMoreConversation(conversations.conversations, conversationsTimeline, timelineParams);
                            for (Conversation conversation : conversations.conversations) {
                                StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
                                StatusCache statusCache = new StatusCache();
                                statusCache.instance = timelineParams.instance;
                                statusCache.user_id = timelineParams.userId;
                                statusCache.conversation = conversation;
                                statusCache.type = Timeline.TimeLineEnum.CONVERSATION;
                                statusCache.status_id = conversation.id;
                                try {
                                    statusCacheDAO.insertOrUpdate(statusCache, timelineParams.slug);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> conversationListMutableLiveData.setValue(conversations);
            mainHandler.post(myRunnable);
        }).start();

        return conversationListMutableLiveData;
    }


    public LiveData<Conversations> getConversationsCache(List<Conversation> timelineConversations, TimelineParams timelineParams) {
        conversationListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
            Conversations conversations = new Conversations();
            try {
                List<Conversation> conversationsDb = statusCacheDAO.getConversations(timelineParams.instance, timelineParams.userId, timelineParams.maxId, timelineParams.minId, timelineParams.sinceId);
                if (conversationsDb != null && conversationsDb.size() > 0) {
                    if (timelineConversations != null) {
                        List<Conversation> notPresentConversations = new ArrayList<>();
                        for (Conversation conversation : conversationsDb) {
                            if (!timelineConversations.contains(conversation)) {
                                conversation.cached = true;
                                timelineConversations.add(conversation);
                            }
                        }
                        //Only not already present statuses are added
                        conversationsDb = notPresentConversations;
                    }
                    conversations.conversations = conversationsDb;
                    if (conversations.conversations.size() > 0) {
                        addFetchMoreConversation(conversations.conversations, timelineConversations, timelineParams);
                        conversations.pagination = new Pagination();
                        conversations.pagination.min_id = conversations.conversations.get(0).id;
                        conversations.pagination.max_id = conversations.conversations.get(conversations.conversations.size() - 1).id;
                    }
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> conversationListMutableLiveData.setValue(conversations);
            mainHandler.post(myRunnable);
        }).start();
        return conversationListMutableLiveData;
    }

    /**
     * Remove conversation
     *
     * @param id ID of the conversation
     */
    public void deleteConversation(@NonNull String instance, String token, @NonNull String id) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Call<Void> deleteStatusCall = mastodonTimelinesService.deleteConversation(token, id);
            if (deleteStatusCall != null) {
                try {
                    deleteStatusCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Mark a conversation as read
     *
     * @param id ID of the conversation
     * @return {@link LiveData} containing a {@link Status}
     */
    public LiveData<Status> markReadConversation(@NonNull String instance, String token, @NonNull String id) {
        statusMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Status status = null;
            Call<Status> markReadConversationCall = mastodonTimelinesService.markReadConversation(token, id);
            if (markReadConversationCall != null) {
                try {
                    Response<Status> markReadConversationResponse = markReadConversationCall.execute();
                    if (markReadConversationResponse.isSuccessful()) {
                        status = markReadConversationResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            Runnable myRunnable = () -> statusMutableLiveData.setValue(finalStatus);
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Fetch all lists that the user owns.
     *
     * @return {@link LiveData} containing a {@link List} of {@link MastodonList}s
     */
    public LiveData<List<MastodonList>> getLists(@NonNull String instance, String token) {
        mastodonListListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            List<MastodonList> mastodonListList = null;
            Call<List<MastodonList>> getListsCall = mastodonTimelinesService.getLists(token);
            if (getListsCall != null) {
                try {
                    Response<List<MastodonList>> getListsResponse = getListsCall.execute();
                    if (getListsResponse.isSuccessful()) {
                        mastodonListList = new ArrayList<>();
                        List<MastodonList> mastodonLists = getListsResponse.body();
                        if (mastodonLists != null) {
                            mastodonListList.addAll(mastodonLists);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<MastodonList> finalMastodonListList = mastodonListList;
            Runnable myRunnable = () -> mastodonListListMutableLiveData.setValue(finalMastodonListList);
            mainHandler.post(myRunnable);
        }).start();
        return mastodonListListMutableLiveData;
    }

    /**
     * Fetch the list with the given ID. Used for verifying the title of a list,
     * and which replies to show within that list.
     *
     * @param id ID of the list
     * @return {@link LiveData} containing a {@link MastodonList}
     */
    public LiveData<MastodonList> getList(@NonNull String instance, String token, @NonNull String id) {
        mastodonListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            MastodonList mastodonList = null;
            Call<MastodonList> getListCall = mastodonTimelinesService.getList(token, id);
            if (getListCall != null) {
                try {
                    Response<MastodonList> getListResponse = getListCall.execute();
                    if (getListResponse.isSuccessful()) {
                        mastodonList = getListResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MastodonList finalMastodonList = mastodonList;
            Runnable myRunnable = () -> mastodonListMutableLiveData.setValue(finalMastodonList);
            mainHandler.post(myRunnable);
        }).start();
        return mastodonListMutableLiveData;
    }

    /**
     * Create a new list.
     *
     * @param title         The title of the list to be created.
     * @param repliesPolicy Enumerable oneOf "followed", "list", "none". Defaults to "list".
     * @return {@link LiveData} containing a {@link MastodonList}
     */
    public LiveData<MastodonList> createList(@NonNull String instance, String token, @NonNull String title, String repliesPolicy) {
        mastodonListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            MastodonList mastodonList = null;
            Call<MastodonList> createListCall = mastodonTimelinesService.createList(token, title, repliesPolicy);
            if (createListCall != null) {
                try {
                    Response<MastodonList> createListResponse = createListCall.execute();
                    if (createListResponse.isSuccessful()) {
                        mastodonList = createListResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MastodonList finalMastodonList = mastodonList;
            Runnable myRunnable = () -> mastodonListMutableLiveData.setValue(finalMastodonList);
            mainHandler.post(myRunnable);
        }).start();
        return mastodonListMutableLiveData;
    }

    /**
     * Change the title of a list, or which replies to show.
     *
     * @param id            ID of the list
     * @param title         The title of the list to be updated.
     * @param repliesPolicy Enumerable oneOf "followed", "list", "none".
     * @return {@link LiveData} containing a {@link MastodonList}
     */
    public LiveData<MastodonList> updateList(@NonNull String instance, String token, @NonNull String id, String title, String repliesPolicy) {
        mastodonListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            MastodonList mastodonList = null;
            Call<MastodonList> updateListCall = mastodonTimelinesService.updateList(token, id, title, repliesPolicy);
            if (updateListCall != null) {
                try {
                    Response<MastodonList> updateListResponse = updateListCall.execute();
                    if (updateListResponse.isSuccessful()) {
                        mastodonList = updateListResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MastodonList finalMastodonList = mastodonList;
            Runnable myRunnable = () -> mastodonListMutableLiveData.setValue(finalMastodonList);
            mainHandler.post(myRunnable);
        }).start();
        return mastodonListMutableLiveData;
    }

    /**
     * Delete a list
     *
     * @param id ID of the list
     */
    public void deleteList(@NonNull String instance, String token, @NonNull String id) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Call<Void> deleteListCall = mastodonTimelinesService.deleteList(token, id);
            if (deleteListCall != null) {
                try {
                    deleteListCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * View accounts in list
     *
     * @param id ID of the list
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<List<Account>> getAccountsInList(@NonNull String instance, String token, @NonNull String id, String maxId, String sinceId, int limit) {
        accountListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Call<List<Account>> getAccountsInListCall = mastodonTimelinesService.getAccountsInList(token, id, maxId, sinceId, limit);
            if (getAccountsInListCall != null) {
                try {
                    Response<List<Account>> getAccountsInListResponse = getAccountsInListCall.execute();
                    if (getAccountsInListResponse.isSuccessful()) {
                        accountList = getAccountsInListResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<Account> finalAccountList = accountList;
            Runnable myRunnable = () -> accountListMutableLiveData.setValue(finalAccountList);
            mainHandler.post(myRunnable);
        }).start();
        return accountListMutableLiveData;
    }

    /**
     * Add accounts to the given list. Note that the user must be following these accounts.
     *
     * @param listId     ID of the list
     * @param accountIds Array of account IDs to add to the list.
     */
    public LiveData<Boolean> addAccountsList(@NonNull String instance, String token, @NonNull String listId, @NonNull List<String> accountIds) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Void> addAccountsListCall = mastodonTimelinesService.addAccountsList(token, listId, accountIds);
            Boolean reply = null;
            if (addAccountsListCall != null) {
                try {
                    Response<Void> response = addAccountsListCall.execute();
                    reply = response.isSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                    reply = false;
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Boolean finalReply = reply;
            Runnable myRunnable = () -> booleanMutableLiveData.setValue(finalReply);
            mainHandler.post(myRunnable);
        }).start();
        return booleanMutableLiveData;
    }

    /**
     * Remove accounts from the given list.
     *
     * @param listId     ID of the list
     * @param accountIds Array of account IDs to remove from the list.
     */
    public void deleteAccountsList(@NonNull String instance, String token, @NonNull String listId, @NonNull List<String> accountIds) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Call<Void> deleteAccountsListCall = mastodonTimelinesService.deleteAccountsList(token, listId, accountIds);
            if (deleteAccountsListCall != null) {
                try {
                    deleteAccountsListCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Get saved timeline position
     *
     * @param timeline Array of markers to fetch. String enum anyOf "home", "notifications".
     *                 If not provided, an empty object will be returned.
     * @return {@link LiveData} containing a {@link Marker}
     */
    public LiveData<Marker> getMarker(@NonNull String instance, String token, @NonNull List<String> timeline) {
        markerMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Marker marker = null;
            Call<Marker> getMarkerCall = mastodonTimelinesService.getMarker(token, timeline);
            if (getMarkerCall != null) {
                try {
                    Response<Marker> getMarkerResponse = getMarkerCall.execute();
                    if (getMarkerResponse.isSuccessful()) {
                        marker = getMarkerResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Marker finalMarker = marker;
            Runnable myRunnable = () -> markerMutableLiveData.setValue(finalMarker);
            mainHandler.post(myRunnable);
        }).start();
        return markerMutableLiveData;
    }

    /**
     * Save position in timeline
     *
     * @param homeLastReadId         ID of the last status read in the home timeline.
     * @param notificationLastReadId ID of the last notification read.
     */
    public void addMarker(@NonNull String instance, String token, String homeLastReadId, String notificationLastReadId) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Call<Void> addMarkerCall = mastodonTimelinesService.addMarker(token, homeLastReadId, notificationLastReadId);
            if (addMarkerCall != null) {
                try {
                    addMarkerCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static class TimelineParams {

        public FragmentMastodonTimeline.DIRECTION direction;
        public String instance;
        public String token;
        public Timeline.TimeLineEnum type;
        public String slug;
        public String userId;
        public Boolean remote;
        public Boolean onlyMedia;
        public Boolean withMuted;
        public String hashtagTrim;
        public List<String> all;
        public List<String> any;
        public List<String> none;
        public String listId;
        public Boolean fetchingMissing;
        public String maxId;
        public String sinceId;
        public String minId;
        public int limit = 40;
        public Boolean local;
        public List<String> excludeType;
        public List<String> excludeVisibilities;
        public String replyVisibility;

        public TimelineParams(Context context, @NonNull Timeline.TimeLineEnum timeLineEnum, @Nullable FragmentMastodonTimeline.DIRECTION timelineDirection, @Nullable String ident) {
            if (type != Timeline.TimeLineEnum.REMOTE) {
                instance = MainActivity.currentInstance;
                token = MainActivity.currentToken;
                userId = MainActivity.currentUserID;
                if (instance == null || userId == null) {
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    instance = sharedpreferences.getString(PREF_USER_INSTANCE, null);
                    userId = sharedpreferences.getString(PREF_USER_ID, null);
                }
            }
            type = timeLineEnum;
            direction = timelineDirection;
            String key = type.getValue();
            if (ident != null) {
                key += "|" + ident;
            }
            slug = key;
        }

        @NonNull
        @Override
        public String toString() {
            return "direction: " + direction + "\n" +
                    "instance: " + instance + "\n" +
                    "token: " + token + "\n" +
                    "type: " + type + "\n" +
                    "slug: " + slug + "\n" +
                    "userId: " + userId + "\n" +
                    "remote: " + remote + "\n" +
                    "onlyMedia: " + onlyMedia + "\n" +
                    "local: " + local + "\n" +
                    "maxId: " + maxId + "\n" +
                    "sinceId: " + sinceId + "\n" +
                    "minId: " + minId + "\n" +
                    "fetchingMissing: " + fetchingMissing;
        }
    }
}
