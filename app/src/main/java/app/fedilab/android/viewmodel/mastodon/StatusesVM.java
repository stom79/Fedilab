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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.client.endpoints.MastodonStatusesService;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Accounts;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Card;
import app.fedilab.android.client.entities.api.Context;
import app.fedilab.android.client.entities.api.Pagination;
import app.fedilab.android.client.entities.api.Poll;
import app.fedilab.android.client.entities.api.ScheduledStatus;
import app.fedilab.android.client.entities.api.ScheduledStatuses;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.QuickLoad;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.TimelineHelper;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class StatusesVM extends AndroidViewModel {


    private MutableLiveData<Status> statusMutableLiveData;
    private MutableLiveData<ScheduledStatus> scheduledStatusMutableLiveData;
    private MutableLiveData<ScheduledStatuses> scheduledStatusesMutableLiveData;
    private MutableLiveData<Void> voidMutableLiveData;
    private MutableLiveData<Card> cardMutableLiveData;
    private MutableLiveData<Attachment> attachmentMutableLiveData;
    private MutableLiveData<Poll> pollMutableLiveData;
    private MutableLiveData<Context> contextMutableLiveData;
    private MutableLiveData<Accounts> accountsMutableLiveData;


    public StatusesVM(@NonNull Application application) {
        super(application);
    }


    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .proxy(Helper.getProxy(getApplication().getApplicationContext()))
                .build();
    }

    private MastodonStatusesService init(@NonNull String instance) {
        Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(getOkHttpClient())
                .build();
        return retrofit.create(MastodonStatusesService.class);
    }

    /**
     * Post a media
     *
     * @param instance    Instance domain of the active account
     * @param token       Access token of the active account
     * @param file        URI
     * @param thumbnail   URI
     * @param description String
     * @param focus       String
     * @return LiveData<Attachment>
     */
    public LiveData<Attachment> postAttachment(@NonNull String instance,
                                               String token,
                                               @NonNull Uri file,
                                               Uri thumbnail,
                                               String description,
                                               String focus) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        attachmentMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MultipartBody.Part fileMultipartBody;
            MultipartBody.Part thumbnailMultipartBody;
            fileMultipartBody = Helper.getMultipartBody(getApplication(), "file", file);
            thumbnailMultipartBody = Helper.getMultipartBody(getApplication(), "file", thumbnail);
            Call<Attachment> attachmentCall = mastodonStatusesService.postMedia(token, fileMultipartBody, thumbnailMultipartBody, description, focus);
            Attachment attachment = null;
            if (attachmentCall != null) {
                try {
                    Response<Attachment> attachmentResponse = attachmentCall.execute();
                    if (attachmentResponse.isSuccessful()) {
                        attachment = attachmentResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Attachment finalAttachment = attachment;
            Runnable myRunnable = () -> attachmentMutableLiveData.setValue(finalAttachment);
            mainHandler.post(myRunnable);
        }).start();
        return attachmentMutableLiveData;
    }

    /**
     * Post a message with the authenticated user
     * text can be null if a media or a poll is attached
     * if media are attached, poll need to be null
     * if a poll is attached, media should be null
     *
     * @param instance         Instance domain of the active account
     * @param token            Access token of the active account
     * @param idempotency_Key  String
     * @param text             String
     * @param media_ids        List<String>
     * @param poll_options     String
     * @param poll_expire_in   int
     * @param poll_multiple    boolean
     * @param poll_hide_totals boolean
     * @param in_reply_to_id   String
     * @param sensitive        boolean
     * @param spoiler_text     String
     * @param visibility       String
     * @param language         String
     * @return LiveData<Status>
     */
    public LiveData<Status> postStatus(@NonNull String instance,
                                       String token,
                                       String idempotency_Key,
                                       String text,
                                       List<String> media_ids,
                                       List<String> poll_options,
                                       Integer poll_expire_in,
                                       Boolean poll_multiple,
                                       Boolean poll_hide_totals,
                                       String in_reply_to_id,
                                       Boolean sensitive,
                                       String spoiler_text,
                                       String visibility,
                                       String language) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Status> statusCall = mastodonStatusesService.createStatus(idempotency_Key, token, text, media_ids, poll_options, poll_expire_in,
                    poll_multiple, poll_hide_totals, in_reply_to_id, sensitive, spoiler_text, visibility, language);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = statusResponse.body();
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
     * @param instance         Instance domain of the active account
     * @param token            Access token of the active account
     *                         Schedule a message for the authenticated user
     *                         scheduledAt can't be null
     *                         text can be null if a media or a poll is attached
     *                         if media are attached, poll need to be null
     *                         if a poll is attached, media should be null
     * @param idempotency_Key  String
     * @param text             String
     * @param media_ids        List<String>
     * @param poll_options     String
     * @param poll_expire_in   int
     * @param poll_multiple    boolean
     * @param poll_hide_totals boolean
     * @param in_reply_to_id   String
     * @param sensitive        boolean
     * @param spoiler_text     String
     * @param visibility       String
     * @param scheduledAt      Date
     * @param language         String
     * @return LiveData<Status>
     */
    public LiveData<ScheduledStatus> postScheduledStatus(@NonNull String instance,
                                                         String token,
                                                         String idempotency_Key,
                                                         String text,
                                                         List<String> media_ids,
                                                         List<String> poll_options,
                                                         Integer poll_expire_in,
                                                         Boolean poll_multiple,
                                                         Boolean poll_hide_totals,
                                                         String in_reply_to_id,
                                                         Boolean sensitive,
                                                         String spoiler_text,
                                                         String visibility,
                                                         @NonNull String scheduledAt,
                                                         String language) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        scheduledStatusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.createScheduledStatus(idempotency_Key, token, text, media_ids, poll_options, poll_expire_in,
                    poll_multiple, poll_hide_totals, in_reply_to_id, sensitive, spoiler_text, visibility, scheduledAt, language);
            ScheduledStatus scheduledStatus = null;
            if (scheduledStatusCall != null) {
                try {
                    Response<ScheduledStatus> scheduledStatusResponse = scheduledStatusCall.execute();
                    if (scheduledStatusResponse.isSuccessful()) {
                        scheduledStatus = scheduledStatusResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            ScheduledStatus finalScheduledStatus = scheduledStatus;
            Runnable myRunnable = () -> scheduledStatusMutableLiveData.setValue(finalScheduledStatus);
            mainHandler.post(myRunnable);
        }).start();
        return scheduledStatusMutableLiveData;
    }

    /**
     * Get a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> getStatus(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Status> statusCall = mastodonStatusesService.getStatus(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
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
     * Delete a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> deleteStatus(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Status> statusCall = mastodonStatusesService.deleteStatus(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = statusResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //The status must also be deleted in cache
            try {
                app.fedilab.android.client.entities.app.Account account = new app.fedilab.android.client.entities.app.Account(getApplication().getApplicationContext()).getAccountByToken(token);
                new StatusCache(getApplication().getApplicationContext()).deleteStatus(id, account.instance);
                new QuickLoad(getApplication().getApplicationContext()).deleteStatus(account, id);
            } catch (DBException e) {
                e.printStackTrace();
            }


            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            Runnable myRunnable = () -> statusMutableLiveData.setValue(finalStatus);
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Get context of a status
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<app.fedilab.android.client.mastodon.entities.Context>
     */
    public LiveData<Context> getContext(@NonNull String instance, String token, @NonNull String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        contextMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Context> contextCall = mastodonStatusesService.getContext(token, id);
            Context context = null;
            if (contextCall != null) {
                try {
                    Response<Context> contextResponse = contextCall.execute();
                    if (contextResponse.isSuccessful()) {
                        context = contextResponse.body();
                        if (context != null) {
                            TimelineHelper.filterStatus(getApplication().getApplicationContext(), context.descendants, TimelineHelper.FilterTimeLineType.CONTEXT);
                            for (Status status : context.descendants) {
                                SpannableHelper.convertStatus(getApplication().getApplicationContext(), status);
                            }
                            TimelineHelper.filterStatus(getApplication().getApplicationContext(), context.ancestors, TimelineHelper.FilterTimeLineType.CONTEXT);
                            for (Status status : context.ancestors) {
                                SpannableHelper.convertStatus(getApplication().getApplicationContext(), status);
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Context finalContext = context;
            Runnable myRunnable = () -> contextMutableLiveData.setValue(finalContext);
            mainHandler.post(myRunnable);
        }).start();
        return contextMutableLiveData;
    }

    /**
     * People that reblogged the status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @param max_id   String
     * @param since_id String
     * @param min_id   String
     * @return LiveData<Accounts>
     */
    public LiveData<Accounts> rebloggedBy(@NonNull String instance,
                                          String token,
                                          @NonNull String id,
                                          String max_id,
                                          String since_id,
                                          String min_id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            int limit = MastodonHelper.accountsPerCall(getApplication().getApplicationContext());
            Call<List<Account>> accountsCall = mastodonStatusesService.getRebloggedBy(token, id, max_id, since_id, min_id, limit);
            List<Account> accounts = null;
            Headers headers = null;
            if (accountsCall != null) {
                try {
                    Response<List<Account>> accountsResponse = accountsCall.execute();
                    if (accountsResponse.isSuccessful()) {
                        accounts = SpannableHelper.convertAccounts(getApplication().getApplicationContext(), accountsResponse.body());
                    }
                    headers = accountsResponse.headers();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Accounts accountsPagination = new Accounts();
            accountsPagination.accounts = accounts;
            accountsPagination.pagination = MastodonHelper.getPagination(headers);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accountsPagination);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * People that favourited the status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @param max_id   String
     * @param since_id String
     * @param min_id   String
     * @return LiveData<Accounts>
     */
    public LiveData<Accounts> favouritedBy(@NonNull String instance,
                                           String token,
                                           @NonNull String id,
                                           String max_id,
                                           String since_id,
                                           String min_id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            int limit = MastodonHelper.accountsPerCall(getApplication().getApplicationContext());
            Call<List<Account>> accountsCall = mastodonStatusesService.getFavourited(token, id, max_id, since_id, min_id, limit);
            List<Account> accounts = null;
            Headers headers = null;
            if (accountsCall != null) {
                try {
                    Response<List<Account>> accountsResponse = accountsCall.execute();
                    if (accountsResponse.isSuccessful()) {
                        accounts = SpannableHelper.convertAccounts(getApplication().getApplicationContext(), accountsResponse.body());
                    }
                    headers = accountsResponse.headers();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Accounts accountsPagination = new Accounts();
            accountsPagination.accounts = accounts;
            accountsPagination.pagination = MastodonHelper.getPagination(headers);
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accountsPagination);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Add a status to favourites by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> favourite(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.favourites(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * remove a status from favourites by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> unFavourite(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.unFavourite(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Reblog a status by ID
     *
     * @param instance   Instance domain of the active account
     * @param token      Access token of the active account
     * @param id         String - id of the status
     * @param visibility MastodonHelper.visibility - visibility of the reblog (public, unlisted, private)
     * @return LiveData<Status>
     */
    public LiveData<Status> reblog(@NonNull String instance, String token, String id, MastodonHelper.visibility visibility) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();

        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.reblog(token, id, visibility != null ? visibility.name().toLowerCase() : null);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Unreblog a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> unReblog(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.unReblog(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Bookmark a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> bookmark(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.bookmark(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Unbookmark a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> unBookmark(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.unBookmark(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Mute a conversation by status ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> mute(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.muteConversation(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Unmute a conversation by a status ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> unMute(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.unMuteConversation(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Pin a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> pin(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Status> statusCall = mastodonStatusesService.pin(token, id);
            String errorMessage = null;
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Unpin a status by ID
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Status>
     */
    public LiveData<Status> unPin(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            String errorMessage = null;
            Call<Status> statusCall = mastodonStatusesService.unPin(token, id);
            Status status = null;
            if (statusCall != null) {
                try {
                    Response<Status> statusResponse = statusCall.execute();
                    if (statusResponse.isSuccessful()) {
                        status = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusResponse.body());
                    } else {
                        if (statusResponse.errorBody() != null) {
                            errorMessage = statusResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                statusMutableLiveData.setValue(finalStatus);
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
            };
            mainHandler.post(myRunnable);
        }).start();
        return statusMutableLiveData;
    }

    /**
     * Get card
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Card>
     */
    public LiveData<Card> getCard(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        cardMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Card> cardCall = mastodonStatusesService.getCard(token, id);
            Card card = null;
            if (cardCall != null) {
                try {
                    Response<Card> cardResponse = cardCall.execute();
                    if (cardResponse.isSuccessful()) {
                        card = cardResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Card finalCard = card;
            Runnable myRunnable = () -> cardMutableLiveData.setValue(finalCard);
            mainHandler.post(myRunnable);
        }).start();
        return cardMutableLiveData;
    }

    /**
     * Get attachment
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Card>
     */
    public LiveData<Attachment> getAttachment(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        attachmentMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Attachment> attachmentCall = mastodonStatusesService.getMedia(token, id);
            Attachment attachment = null;
            if (attachmentCall != null) {
                try {
                    Response<Attachment> attachmentResponse = attachmentCall.execute();
                    if (attachmentResponse.isSuccessful()) {
                        attachment = attachmentResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Attachment finalAttachment = attachment;
            Runnable myRunnable = () -> attachmentMutableLiveData.setValue(finalAttachment);
            mainHandler.post(myRunnable);
        }).start();
        return attachmentMutableLiveData;
    }

    /**
     * Update a media
     *
     * @param instance    Instance domain of the active account
     * @param token       Access token of the active account
     * @param id          String - Id of the media to update
     * @param file        URI
     * @param thumbnail   URI
     * @param description String
     * @param focus       String
     * @return LiveData<Attachment>
     */
    public LiveData<Attachment> updateAttachment(@NonNull String instance,
                                                 String token,
                                                 @NonNull String id,
                                                 @NonNull Uri file,
                                                 Uri thumbnail,
                                                 String description,
                                                 String focus) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        attachmentMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MultipartBody.Part fileMultipartBody = null;
            MultipartBody.Part thumbnailMultipartBody = null;
            fileMultipartBody = Helper.getMultipartBody(getApplication(), "file", file);
            thumbnailMultipartBody = Helper.getMultipartBody(getApplication(), "file", thumbnail);
            Call<Attachment> attachmentCall = mastodonStatusesService.updateMedia(token, id, fileMultipartBody, thumbnailMultipartBody, description, focus);
            Attachment attachment = null;
            if (attachmentCall != null) {
                try {
                    Response<Attachment> attachmentResponse = attachmentCall.execute();
                    if (attachmentResponse.isSuccessful()) {
                        attachment = attachmentResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Attachment finalAttachment = attachment;
            Runnable myRunnable = () -> attachmentMutableLiveData.setValue(finalAttachment);
            mainHandler.post(myRunnable);
        }).start();
        return attachmentMutableLiveData;
    }

    /**
     * Get Poll
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the status
     * @return LiveData<Poll>
     */
    public LiveData<Poll> getPoll(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        pollMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Poll> pollCall = mastodonStatusesService.getPoll(token, id);
            Poll poll = null;
            if (pollCall != null) {
                try {
                    Response<Poll> pollResponse = pollCall.execute();
                    if (pollResponse.isSuccessful()) {
                        poll = pollResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Poll finalPoll = poll;
            Runnable myRunnable = () -> pollMutableLiveData.setValue(finalPoll);
            mainHandler.post(myRunnable);
        }).start();
        return pollMutableLiveData;
    }

    /**
     * Vote on a Poll
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the poll
     * @param choices  int[] - array of choices
     * @return LiveData<Poll>
     */
    public LiveData<Poll> votePoll(@NonNull String instance, String token, String id, int[] choices) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        pollMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Poll> pollCall = mastodonStatusesService.votePoll(token, id, choices);
            Poll poll = null;
            if (pollCall != null) {
                try {
                    Response<Poll> pollResponse = pollCall.execute();
                    if (pollResponse.isSuccessful()) {
                        poll = pollResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Poll finalPoll = poll;
            Runnable myRunnable = () -> pollMutableLiveData.setValue(finalPoll);
            mainHandler.post(myRunnable);
        }).start();
        return pollMutableLiveData;
    }

    /**
     * Get list of scheduled status
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param max_id   String
     * @param since_id String
     * @param min_id   String
     * @param limit    int
     * @return LiveData<ScheduledStatuses>
     */
    public LiveData<ScheduledStatuses> getScheduledStatuses(@NonNull String instance,
                                                            String token,
                                                            String max_id,
                                                            String since_id,
                                                            String min_id,
                                                            int limit) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        scheduledStatusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<List<ScheduledStatus>> scheduledStatuseCall = mastodonStatusesService.getScheduledStatuses(token, max_id, since_id, min_id, limit);
            List<ScheduledStatus> scheduledStatusList = null;
            Pagination pagination = null;
            if (scheduledStatuseCall != null) {
                try {
                    Response<List<ScheduledStatus>> scheduledStatusResponse = scheduledStatuseCall.execute();
                    if (scheduledStatusResponse.isSuccessful()) {
                        scheduledStatusList = scheduledStatusResponse.body();
                        pagination = MastodonHelper.getPagination(scheduledStatusResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ScheduledStatuses scheduledStatuses = new ScheduledStatuses();
            scheduledStatuses.scheduledStatuses = scheduledStatusList;
            scheduledStatuses.pagination = pagination;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> scheduledStatusesMutableLiveData.setValue(scheduledStatuses);
            mainHandler.post(myRunnable);
        }).start();
        return scheduledStatusesMutableLiveData;
    }

    /**
     * Get a scheduled status
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the scheduled status
     * @return LiveData<ScheduledStatus>
     */
    public LiveData<ScheduledStatus> getScheduledStatus(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        scheduledStatusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.getScheduledStatus(token, id);
            ScheduledStatus scheduledStatus = null;
            if (scheduledStatusCall != null) {
                try {
                    Response<ScheduledStatus> scheduledStatusResponse = scheduledStatusCall.execute();
                    if (scheduledStatusResponse.isSuccessful()) {
                        scheduledStatus = scheduledStatusResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            ScheduledStatus finalScheduledStatus = scheduledStatus;
            Runnable myRunnable = () -> scheduledStatusMutableLiveData.setValue(finalScheduledStatus);
            mainHandler.post(myRunnable);
        }).start();
        return scheduledStatusMutableLiveData;
    }

    /**
     * Update a scheduled status
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the scheduled status
     * @return LiveData<ScheduledStatus>
     */
    public LiveData<ScheduledStatus> updateScheduledStatus(@NonNull String instance, String token, String id, Date scheduled_at) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        scheduledStatusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<ScheduledStatus> scheduledStatusCall = mastodonStatusesService.updateScheduleStatus(token, id, scheduled_at);
            ScheduledStatus scheduledStatus = null;
            if (scheduledStatusCall != null) {
                try {
                    Response<ScheduledStatus> scheduledStatusResponse = scheduledStatusCall.execute();
                    if (scheduledStatusResponse.isSuccessful()) {
                        scheduledStatus = scheduledStatusResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            ScheduledStatus finalScheduledStatus = scheduledStatus;
            Runnable myRunnable = () -> scheduledStatusMutableLiveData.setValue(finalScheduledStatus);
            mainHandler.post(myRunnable);
        }).start();
        return scheduledStatusMutableLiveData;
    }

    /**
     * Delete a scheduled status
     *
     * @param instance Instance domain of the active account
     * @param token    Access token of the active account
     * @param id       String - id of the scheduled status
     * @return LiveData<ScheduledStatus>
     */
    public LiveData<Void> deleteScheduledStatus(@NonNull String instance, String token, String id) {
        MastodonStatusesService mastodonStatusesService = init(instance);
        voidMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Call<Void> voidCall = mastodonStatusesService.deleteScheduledStatus(token, id);
            if (voidCall != null) {
                try {
                    voidCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> voidMutableLiveData.setValue(null);
            mainHandler.post(myRunnable);
        }).start();
        return voidMutableLiveData;
    }
}
