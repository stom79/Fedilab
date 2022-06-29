package app.fedilab.android.viewmodel.mastodon;
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


import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.client.endpoints.MastodonTimelinesService;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Conversation;
import app.fedilab.android.client.entities.api.Conversations;
import app.fedilab.android.client.entities.api.Marker;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.api.Statuses;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.client.entities.misskey.MisskeyNote;
import app.fedilab.android.client.entities.nitter.Nitter;
import app.fedilab.android.client.entities.peertube.PeertubeVideo;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.TimelineHelper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class TimelinesVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(Helper.getProxy(getApplication().getApplicationContext()))
            .build();


    private MutableLiveData<List<Account>> accountListMutableLiveData;
    private MutableLiveData<List<StatusDraft>> statusDraftListMutableLiveData;
    private MutableLiveData<Status> statusMutableLiveData;
    private MutableLiveData<Statuses> statusesMutableLiveData;
    private MutableLiveData<PeertubeVideo.Video> peertubeVideoMutableLiveData;
    private MutableLiveData<Conversations> conversationListMutableLiveData;
    private MutableLiveData<MastodonList> mastodonListMutableLiveData;
    private MutableLiveData<List<MastodonList>> mastodonListListMutableLiveData;
    private MutableLiveData<Marker> markerMutableLiveData;

    public TimelinesVM(@NonNull Application application) {
        super(application);
    }

    private MastodonTimelinesService initInstanceOnly(String instance) {
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    private MastodonTimelinesService initInstanceXMLOnly(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    private MastodonTimelinesService init(String instance) {
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonTimelinesService.class);
    }

    /**
     * Public timeline
     *
     * @param local     Show only local statuses? Defaults to false.
     * @param remote    Show only remote statuses? Defaults to false.
     * @param onlyMedia Show only statuses with media attached? Defaults to false.
     * @param maxId     Return results older than this id
     * @param sinceId   Return results newer than this id
     * @param minId     Return results immediately newer than this id
     * @param limit     Maximum number of results to return. Defaults to 20.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getPublic(String token, @NonNull String instance,
                                        Boolean local,
                                        Boolean remote,
                                        Boolean onlyMedia,
                                        String maxId,
                                        String sinceId,
                                        String minId,
                                        Integer limit) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<Status>> publicTlCall = mastodonTimelinesService.getPublic(token, local, remote, onlyMedia, maxId, sinceId, minId, limit);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<List<Status>> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        List<Status> notFilteredStatuses = publicTlResponse.body();
                        List<Status> filteredStatuses = TimelineHelper.filterStatus(getApplication(), notFilteredStatuses, TimelineHelper.FilterTimeLineType.PUBLIC);
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), filteredStatuses);
                        statuses.pagination = MastodonHelper.getPagination(publicTlResponse.headers());
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
    public LiveData<Statuses> getNitter(@NonNull String instance,
                                        String accountsStr,
                                        String max_position) {
        MastodonTimelinesService mastodonTimelinesService = initInstanceXMLOnly(instance);
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Nitter> publicTlCall = mastodonTimelinesService.getNitter(accountsStr, max_position);
            Statuses statuses = new Statuses();
            if (publicTlCall != null) {
                try {
                    Response<Nitter> publicTlResponse = publicTlCall.execute();
                    if (publicTlResponse.isSuccessful()) {
                        Nitter rssResponse = publicTlResponse.body();
                        List<Status> statusList = new ArrayList<>();
                        if (rssResponse != null && rssResponse.mFeedItems != null) {
                            for (Nitter.FeedItem feedItem : rssResponse.mFeedItems) {
                                Status status = Nitter.convert(getApplication(), instance, feedItem);
                                statusList.add(status);
                            }
                        }
                        statuses.statuses = SpannableHelper.convertNitterStatus(getApplication().getApplicationContext(), statusList);
                        statuses.pagination = MastodonHelper.getPagination(publicTlResponse.headers());
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
                                Status status = MisskeyNote.convert(misskeyNote);
                                statusList.add(status);
                            }
                        }
                        List<Status> filteredStatuses = TimelineHelper.filterStatus(getApplication(), statusList, TimelineHelper.FilterTimeLineType.PUBLIC);
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), filteredStatuses);
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
                        List<Status> filteredStatuses = TimelineHelper.filterStatus(getApplication(), statusList, TimelineHelper.FilterTimeLineType.PUBLIC);
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), filteredStatuses);
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


    /**
     * View public statuses containing the given hashtag.
     *
     * @param hashtag   Content of a #hashtag, not including # symbol.
     * @param local     If true, return only local statuses. Defaults to false.
     * @param onlyMedia If true, return only statuses with media attachments. Defaults to false.
     * @param maxId     Return results older than this ID.
     * @param sinceId   Return results newer than this ID.
     * @param minId     Return results immediately newer than this ID.
     * @param limit     Maximum number of results to return. Defaults to 20.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getHashTag(String token, @NonNull String instance,
                                         @NonNull String hashtag,
                                         boolean local,
                                         boolean onlyMedia,
                                         List<String> all,
                                         List<String> any,
                                         List<String> none,
                                         String maxId,
                                         String sinceId,
                                         String minId,
                                         int limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> hashTagTlCall = mastodonTimelinesService.getHashTag(token, hashtag, local, onlyMedia, all, any, none, maxId, sinceId, minId, limit);
            if (hashTagTlCall != null) {
                try {
                    Response<List<Status>> hashTagTlResponse = hashTagTlCall.execute();
                    if (hashTagTlResponse.isSuccessful()) {
                        List<Status> notFilteredStatuses = hashTagTlResponse.body();
                        List<Status> filteredStatuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), notFilteredStatuses, TimelineHelper.FilterTimeLineType.PUBLIC);
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), filteredStatuses);
                        statuses.pagination = MastodonHelper.getPagination(hashTagTlResponse.headers());
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
     * View statuses from followed users.
     *
     * @param maxId   Return results older than id
     * @param sinceId Return results newer than id
     * @param minId   Return results immediately newer than id
     * @param limit   Maximum number of results to return. Defaults to 20.
     * @param local   Return only local statuses?
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getHome(@NonNull String instance, String token,
                                      boolean fetchingMissing,
                                      String maxId,
                                      String sinceId,
                                      String minId,
                                      int limit,
                                      boolean local) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> homeTlCall = mastodonTimelinesService.getHome(token, maxId, sinceId, minId, limit, local);
            if (homeTlCall != null) {
                try {
                    Response<List<Status>> homeTlResponse = homeTlCall.execute();
                    if (homeTlResponse.isSuccessful()) {
                        List<Status> notFilteredStatuses = homeTlResponse.body();
                        List<Status> filteredStatuses = TimelineHelper.filterStatus(getApplication().getApplicationContext(), notFilteredStatuses, TimelineHelper.FilterTimeLineType.HOME);
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), filteredStatuses);
                        statuses.pagination = MastodonHelper.getPagination(homeTlResponse.headers());
                        if (!fetchingMissing) {
                            for (Status status : statuses.statuses) {
                                StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
                                StatusCache statusCache = new StatusCache();
                                statusCache.instance = instance;
                                statusCache.user_id = BaseMainActivity.currentUserID;
                                statusCache.status = status;
                                statusCache.type = StatusCache.CacheEnum.HOME;
                                statusCache.status_id = status.id;
                                try {
                                    statusCacheDAO.insertOrUpdate(statusCache);
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
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();

        return statusesMutableLiveData;
    }

    /**
     * Get home status from cache
     *
     * @param instance String - instance
     * @param user_id  String - user id
     * @param maxId    String - max id
     * @param minId    String - min id
     * @return LiveData<Statuses>
     */
    public LiveData<Statuses> getHomeCache(@NonNull String instance, String user_id,
                                           String maxId,
                                           String minId,
                                           String sinceId) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            StatusCache statusCacheDAO = new StatusCache(getApplication().getApplicationContext());
            Statuses statuses = null;
            try {
                statuses = statusCacheDAO.geStatuses(StatusCache.CacheEnum.HOME, instance, user_id, maxId, minId, sinceId);
                if (statuses != null) {
                    statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statuses.statuses);
                    if (statuses.statuses != null && statuses.statuses.size() > 0) {
                        statuses.pagination = new Pagination();
                        statuses.pagination.min_id = statuses.statuses.get(0).id;
                        statuses.pagination.max_id = statuses.statuses.get(statuses.statuses.size() - 1).id;
                    }
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Statuses finalStatuses = statuses;
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(finalStatuses);
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
     * View statuses in the given list timeline.
     *
     * @param listId  Local ID of the list in the database.
     * @param maxId   Return results older than this ID.
     * @param sinceId Return results newer than this ID.
     * @param minId   Return results immediately newer than this ID.
     * @param limit   Maximum number of results to return. Defaults to 20.Return results older than this ID.
     * @return {@link LiveData} containing a {@link Statuses}
     */
    public LiveData<Statuses> getList(@NonNull String instance, String token,
                                      @NonNull String listId,
                                      String maxId,
                                      String sinceId,
                                      String minId,
                                      int limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> listTlCall = mastodonTimelinesService.getList(token, listId, maxId, sinceId, minId, limit);
            if (listTlCall != null) {
                try {
                    Response<List<Status>> listTlResponse = listTlCall.execute();
                    if (listTlResponse.isSuccessful()) {
                        statuses.statuses = SpannableHelper.convertStatus(getApplication().getApplicationContext(), listTlResponse.body());
                        statuses.pagination = MastodonHelper.getPagination(listTlResponse.headers());
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
     * Show conversations
     *
     * @return {@link LiveData} containing a {@link Conversations}
     */
    public LiveData<Conversations> getConversations(@NonNull String instance, String token,
                                                    String maxId,
                                                    String sinceId,
                                                    String minId,
                                                    int limit) {
        conversationListMutableLiveData = new MutableLiveData<>();
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Conversations conversations = null;
            Call<List<Conversation>> conversationsCall = mastodonTimelinesService.getConversations(token, maxId, sinceId, minId, limit);
            if (conversationsCall != null) {
                conversations = new Conversations();
                try {
                    Response<List<Conversation>> conversationsResponse = conversationsCall.execute();
                    if (conversationsResponse.isSuccessful()) {
                        conversations.conversations = conversationsResponse.body();
                        if (conversations.conversations != null) {
                            for (Conversation conversation : conversations.conversations) {
                                conversation.last_status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), conversation.last_status);
                            }
                        }
                        conversations.pagination = MastodonHelper.getPagination(conversationsResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Conversations finalConversations = conversations;
            Runnable myRunnable = () -> conversationListMutableLiveData.setValue(finalConversations);
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
                        mastodonListList = getListsResponse.body();
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
    public void addAccountsList(@NonNull String instance, String token, @NonNull String listId, @NonNull List<String> accountIds) {
        MastodonTimelinesService mastodonTimelinesService = init(instance);
        new Thread(() -> {
            Call<Void> addAccountsListCall = mastodonTimelinesService.addAccountsList(token, listId, accountIds);
            if (addAccountsListCall != null) {
                try {
                    addAccountsListCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
}
