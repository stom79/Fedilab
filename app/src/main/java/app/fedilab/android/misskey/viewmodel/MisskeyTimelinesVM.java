package app.fedilab.android.misskey.viewmodel;
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.MastodonList;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyFavorite;
import app.fedilab.android.misskey.client.entities.MisskeyNote;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.MisskeyUser;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisskeyTimelinesVM extends AndroidViewModel {

    private final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<Statuses> statusesMutableLiveData;
    private MutableLiveData<List<Tag>> tagsMutableLiveData;
    private MutableLiveData<List<MastodonList>> listsMutableLiveData;
    private MutableLiveData<MastodonList> listMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;
    private MutableLiveData<List<Account>> accountListMutableLiveData;

    public MisskeyTimelinesVM(@NonNull Application application) {
        super(application);
    }

    private MisskeyService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) + "/api/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MisskeyService.class);
    }

    public LiveData<Statuses> getHomeTimeline(
            @NonNull String instance,
            String token,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.TimelineRequest request = new MisskeyRequest.TimelineRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.withRenotes = true;
            request.withFiles = false;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getHomeTimeline(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getLocalTimeline(
            @NonNull String instance,
            String token,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.TimelineRequest request = new MisskeyRequest.TimelineRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.withRenotes = true;
            request.withFiles = false;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getLocalTimeline(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getGlobalTimeline(
            @NonNull String instance,
            String token,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.TimelineRequest request = new MisskeyRequest.TimelineRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.withRenotes = true;
            request.withFiles = false;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getGlobalTimeline(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getHashtagTimeline(
            @NonNull String instance,
            String token,
            String hashtag,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.HashtagRequest request = new MisskeyRequest.HashtagRequest(token, hashtag);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getHashtagTimeline(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getUserNotes(
            @NonNull String instance,
            String token,
            String userId,
            String maxId,
            String sinceId,
            Integer limit,
            Boolean withReplies,
            Boolean withMedia) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserNotesRequest request = new MisskeyRequest.UserNotesRequest(token, userId);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.withReplies = withReplies;
            request.withFiles = withMedia;
            request.withRenotes = true;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getUserNotes(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> searchNotes(
            @NonNull String instance,
            String token,
            String query,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.SearchRequest request = new MisskeyRequest.SearchRequest(token, query);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.searchNotes(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<List<Tag>> searchHashtags(
            @NonNull String instance,
            String token,
            String query,
            Integer limit) {
        tagsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.SearchRequest request = new MisskeyRequest.SearchRequest(token, query);
            request.limit = limit != null ? limit : 20;
            List<Tag> tags = new ArrayList<>();
            try {
                Response<List<String>> response = misskeyService.searchHashtags(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (String tagName : response.body()) {
                        Tag tag = new Tag();
                        tag.name = tagName;
                        tag.history = new ArrayList<>();
                        tags.add(tag);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> tagsMutableLiveData.setValue(tags));
        }).start();
        return tagsMutableLiveData;
    }

    public LiveData<Statuses> getBookmarks(
            @NonNull String instance,
            String token,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;

            Statuses statuses = new Statuses();
            Log.d(Helper.TAG, "getBookmarks: calling i/favorites on " + instance + " maxId=" + maxId + " sinceId=" + sinceId + " limit=" + request.limit);
            try {
                Response<List<MisskeyFavorite>> response = misskeyService.getFavorites(request).execute();
                Log.d(Helper.TAG, "getBookmarks: response code=" + response.code() + " successful=" + response.isSuccessful());
                if (response.errorBody() != null) {
                    Log.d(Helper.TAG, "getBookmarks: errorBody=" + response.errorBody().string());
                }
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(Helper.TAG, "getBookmarks: got " + response.body().size() + " favorites");
                    List<Status> statusList = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (MisskeyFavorite fav : response.body()) {
                        Log.d(Helper.TAG, "getBookmarks: fav id=" + fav.id + " noteId=" + fav.noteId + " note=" + (fav.note != null ? "not null" : "null"));
                        if (fav.note != null) {
                            Status status = fav.note.toStatus(instance);
                            status.bookmarked = true;
                            statusList.add(status);
                            ids.add(fav.id);
                        }
                    }
                    statuses.statuses = statusList;
                    Log.d(Helper.TAG, "getBookmarks: converted " + statusList.size() + " statuses");
                    statuses.pagination = new Pagination();
                    if (!ids.isEmpty()) {
                        statuses.pagination.max_id = ids.get(ids.size() - 1);
                        statuses.pagination.min_id = ids.get(0);
                    }
                } else {
                    Log.d(Helper.TAG, "getBookmarks: response body is null or not successful");
                }
            } catch (Exception e) {
                Log.e(Helper.TAG, "getBookmarks: exception", e);
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getUserReactions(
            @NonNull String instance,
            String token,
            String userId,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyService.MisskeyUserReaction>> response = misskeyService.getUserReactions(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<Status> statusList = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    for (MisskeyService.MisskeyUserReaction userReaction : response.body()) {
                        if (userReaction.note != null) {
                            Status status = userReaction.note.toStatus(instance);
                            status.favourited = true;
                            statusList.add(status);
                            ids.add(userReaction.id);
                        }
                    }
                    statuses.statuses = statusList;
                    statuses.pagination = new Pagination();
                    if (!ids.isEmpty()) {
                        statuses.pagination.max_id = ids.get(ids.size() - 1);
                        statuses.pagination.min_id = ids.get(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<Statuses> getListTimeline(
            @NonNull String instance,
            String token,
            String listId,
            String maxId,
            String sinceId,
            Integer limit) {
        statusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ListTimelineRequest request = new MisskeyRequest.ListTimelineRequest(token, listId);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            request.sinceId = sinceId;
            request.withRenotes = true;
            request.withFiles = false;

            Statuses statuses = new Statuses();
            try {
                Response<List<MisskeyNote>> response = misskeyService.getListTimeline(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    statuses.statuses = convertNotes(response.body(), instance);
                    statuses.pagination = extractPagination(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> statusesMutableLiveData.setValue(statuses));
        }).start();
        return statusesMutableLiveData;
    }

    public LiveData<List<MastodonList>> getLists(@NonNull String instance, String token) {
        listsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);
            List<MastodonList> mastodonLists = new ArrayList<>();
            try {
                Response<List<MisskeyService.MisskeyList>> response = misskeyService.getLists(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyService.MisskeyList list : response.body()) {
                        MastodonList mastodonList = new MastodonList();
                        mastodonList.id = list.id;
                        mastodonList.title = list.name;
                        mastodonLists.add(mastodonList);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> listsMutableLiveData.setValue(mastodonLists));
        }).start();
        return listsMutableLiveData;
    }

    public LiveData<MastodonList> createList(@NonNull String instance, String token, String name) {
        listMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.CreateListRequest request = new MisskeyRequest.CreateListRequest(token, name);
            MastodonList mastodonList = null;
            try {
                Response<MisskeyService.MisskeyList> response = misskeyService.createList(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    mastodonList = new MastodonList();
                    mastodonList.id = response.body().id;
                    mastodonList.title = response.body().name;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MastodonList finalList = mastodonList;
            mainHandler.post(() -> listMutableLiveData.setValue(finalList));
        }).start();
        return listMutableLiveData;
    }

    public LiveData<MastodonList> updateList(@NonNull String instance, String token, String listId, String name) {
        listMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UpdateListRequest request = new MisskeyRequest.UpdateListRequest(token, listId, name);
            MastodonList mastodonList = null;
            try {
                Response<MisskeyService.MisskeyList> response = misskeyService.updateList(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    mastodonList = new MastodonList();
                    mastodonList.id = response.body().id;
                    mastodonList.title = response.body().name;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            MastodonList finalList = mastodonList;
            mainHandler.post(() -> listMutableLiveData.setValue(finalList));
        }).start();
        return listMutableLiveData;
    }

    public LiveData<Boolean> deleteList(@NonNull String instance, String token, String listId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ListRequest request = new MisskeyRequest.ListRequest(token, listId);
            boolean success = false;
            try {
                Response<Void> response = misskeyService.deleteList(request).execute();
                success = response.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            boolean finalSuccess = success;
            mainHandler.post(() -> booleanMutableLiveData.setValue(finalSuccess));
        }).start();
        return booleanMutableLiveData;
    }

    public LiveData<Boolean> addUserToList(@NonNull String instance, String token, String listId, String userId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ListUserRequest request = new MisskeyRequest.ListUserRequest(token, listId, userId);
            boolean success = false;
            try {
                Response<Void> response = misskeyService.addUserToList(request).execute();
                success = response.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            boolean finalSuccess = success;
            mainHandler.post(() -> booleanMutableLiveData.setValue(finalSuccess));
        }).start();
        return booleanMutableLiveData;
    }

    public LiveData<Boolean> removeUserFromList(@NonNull String instance, String token, String listId, String userId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ListUserRequest request = new MisskeyRequest.ListUserRequest(token, listId, userId);
            boolean success = false;
            try {
                Response<Void> response = misskeyService.removeUserFromList(request).execute();
                success = response.isSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            boolean finalSuccess = success;
            mainHandler.post(() -> booleanMutableLiveData.setValue(finalSuccess));
        }).start();
        return booleanMutableLiveData;
    }

    public LiveData<List<Account>> getAccountsInList(@NonNull String instance, String token, String listId) {
        accountListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ListRequest request = new MisskeyRequest.ListRequest(token, listId);
            List<Account> accounts = new ArrayList<>();
            try {
                Response<MisskeyService.MisskeyList> response = misskeyService.getList(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().userIds != null) {
                    for (String userId : response.body().userIds) {
                        MisskeyRequest.UserIdRequest userRequest = new MisskeyRequest.UserIdRequest(token, userId);
                        Response<MisskeyUser> userResponse = misskeyService.getUser(userRequest).execute();
                        if (userResponse.isSuccessful() && userResponse.body() != null) {
                            accounts.add(userResponse.body().toAccount());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountListMutableLiveData.setValue(accounts));
        }).start();
        return accountListMutableLiveData;
    }

    private List<Status> convertNotes(List<MisskeyNote> notes, String instance) {
        List<Status> statuses = new ArrayList<>();
        if (notes != null) {
            for (MisskeyNote note : notes) {
                statuses.add(note.toStatus(instance));
            }
        }
        return statuses;
    }

    private Pagination extractPagination(List<MisskeyNote> notes) {
        Pagination pagination = new Pagination();
        if (notes != null && !notes.isEmpty()) {
            pagination.max_id = notes.get(notes.size() - 1).id;
            pagination.min_id = notes.get(0).id;
        }
        return pagination;
    }
}
