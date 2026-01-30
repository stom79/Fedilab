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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatuses;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyFile;
import app.fedilab.android.misskey.client.entities.MisskeyNote;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.MisskeyScheduledNote;
import app.fedilab.android.misskey.client.entities.MisskeyUser;
import app.fedilab.android.misskey.client.entities.NoteCreateRequest;
import app.fedilab.android.misskey.client.entities.NoteCreateResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisskeyStatusesVM extends AndroidViewModel {

    private final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<Status> statusMutableLiveData;
    private MutableLiveData<Context> contextMutableLiveData;
    private MutableLiveData<Attachment> attachmentMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;
    private MutableLiveData<Poll> pollMutableLiveData;
    private MutableLiveData<ScheduledStatuses> scheduledStatusesMutableLiveData;

    public MisskeyStatusesVM(@NonNull Application application) {
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

    public LiveData<Status> createNote(
            @NonNull String instance,
            String token,
            String text,
            String visibility,
            String cw,
            String replyId,
            List<String> fileIds,
            Boolean localOnly) {
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            NoteCreateRequest request = new NoteCreateRequest(token);
            request.text = text;
            request.visibility = NoteCreateRequest.mapVisibility(visibility);
            request.cw = cw;
            request.replyId = replyId;
            request.fileIds = fileIds;
            request.localOnly = localOnly;

            Status status = null;
            try {
                Response<NoteCreateResponse> response = misskeyService.createNote(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().createdNote != null) {
                    status = response.body().createdNote.toStatus(instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            mainHandler.post(() -> statusMutableLiveData.setValue(finalStatus));
        }).start();
        return statusMutableLiveData;
    }

    public LiveData<Status> renote(
            @NonNull String instance,
            String token,
            String noteId) {
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            NoteCreateRequest request = new NoteCreateRequest(token);
            request.renoteId = noteId;

            Status status = null;
            try {
                Response<NoteCreateResponse> response = misskeyService.createNote(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().createdNote != null) {
                    status = response.body().createdNote.toStatus(instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            mainHandler.post(() -> statusMutableLiveData.setValue(finalStatus));
        }).start();
        return statusMutableLiveData;
    }

    public LiveData<Boolean> unrenote(
            @NonNull String instance,
            String token,
            String noteId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.unrenote(request).execute();
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

    public LiveData<Status> getNote(@NonNull String instance, String token, String noteId) {
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            Status status = null;
            try {
                Response<MisskeyNote> response = misskeyService.getNote(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    status = response.body().toStatus(instance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            mainHandler.post(() -> statusMutableLiveData.setValue(finalStatus));
        }).start();
        return statusMutableLiveData;
    }

    public LiveData<Boolean> deleteNote(@NonNull String instance, String token, String noteId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.deleteNote(request).execute();
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

    public LiveData<Boolean> createReaction(@NonNull String instance, String token, String noteId, String reaction) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            String reactionEmoji = reaction != null ? reaction : "\u2764";
            MisskeyRequest.ReactionRequest request = new MisskeyRequest.ReactionRequest(token, noteId, reactionEmoji);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.createReaction(request).execute();
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

    public LiveData<Boolean> deleteReaction(@NonNull String instance, String token, String noteId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.deleteReaction(request).execute();
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

    public LiveData<Boolean> createFavorite(@NonNull String instance, String token, String noteId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.createFavorite(request).execute();
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

    public LiveData<Boolean> deleteFavorite(@NonNull String instance, String token, String noteId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.deleteFavorite(request).execute();
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

    public LiveData<Status> pin(@NonNull String instance, String token, String noteId) {
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);
            Status status = null;
            try {
                Response<MisskeyUser> response = misskeyService.pinNote(request).execute();
                if (response.isSuccessful()) {
                    status = new Status();
                    status.id = noteId;
                    status.pinned = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            mainHandler.post(() -> statusMutableLiveData.setValue(finalStatus));
        }).start();
        return statusMutableLiveData;
    }

    public LiveData<Status> unpin(@NonNull String instance, String token, String noteId) {
        statusMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);
            Status status = null;
            try {
                Response<MisskeyUser> response = misskeyService.unpinNote(request).execute();
                if (response.isSuccessful()) {
                    status = new Status();
                    status.id = noteId;
                    status.pinned = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Status finalStatus = status;
            mainHandler.post(() -> statusMutableLiveData.setValue(finalStatus));
        }).start();
        return statusMutableLiveData;
    }

    public LiveData<Poll> votePoll(@NonNull String instance, String token, String noteId, int[] choices) {
        pollMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            boolean success = true;
            try {
                for (int choice : choices) {
                    MisskeyRequest.PollVoteRequest request = new MisskeyRequest.PollVoteRequest(token, noteId, choice);
                    Response<Void> response = misskeyService.votePoll(request).execute();
                    if (!response.isSuccessful()) {
                        success = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
            Poll poll = null;
            if (success) {
                try {
                    MisskeyRequest.NoteIdRequest noteRequest = new MisskeyRequest.NoteIdRequest(token, noteId);
                    Response<MisskeyNote> noteResponse = misskeyService.getNote(noteRequest).execute();
                    if (noteResponse.isSuccessful() && noteResponse.body() != null && noteResponse.body().poll != null) {
                        poll = noteResponse.body().poll.toPoll(noteId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Poll finalPoll = poll;
            mainHandler.post(() -> pollMutableLiveData.setValue(finalPoll));
        }).start();
        return pollMutableLiveData;
    }

    public LiveData<Poll> getPoll(@NonNull String instance, String token, String noteId) {
        pollMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            Poll poll = null;
            try {
                MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);
                Response<MisskeyNote> response = misskeyService.getNote(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().poll != null) {
                    poll = response.body().poll.toPoll(noteId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Poll finalPoll = poll;
            mainHandler.post(() -> pollMutableLiveData.setValue(finalPoll));
        }).start();
        return pollMutableLiveData;
    }

    public LiveData<Context> getContext(@NonNull String instance, String token, String noteId) {
        contextMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.NoteIdRequest request = new MisskeyRequest.NoteIdRequest(token, noteId);

            Context context = new Context();
            context.ancestors = new ArrayList<>();
            context.descendants = new ArrayList<>();

            try {
                Response<List<MisskeyNote>> conversationResponse = misskeyService.getConversation(request).execute();
                if (conversationResponse.isSuccessful() && conversationResponse.body() != null) {
                    for (MisskeyNote note : conversationResponse.body()) {
                        context.ancestors.add(note.toStatus(instance));
                    }
                }

                Response<List<MisskeyNote>> repliesResponse = misskeyService.getReplies(request).execute();
                if (repliesResponse.isSuccessful() && repliesResponse.body() != null) {
                    for (MisskeyNote note : repliesResponse.body()) {
                        context.descendants.add(note.toStatus(instance));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> contextMutableLiveData.setValue(context));
        }).start();
        return contextMutableLiveData;
    }

    public LiveData<Attachment> uploadFile(
            @NonNull String instance,
            String token,
            @NonNull Uri fileUri,
            String description,
            boolean sensitive) {
        attachmentMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);

            Attachment attachment = null;
            try {
                MultipartBody.Part filePart = Helper.getMultipartBody(getApplication(), "file", fileUri);
                RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), token);
                RequestBody sensitiveBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(sensitive));
                RequestBody commentBody = description != null ?
                        RequestBody.create(MediaType.parse("text/plain"), description) : null;

                Response<MisskeyFile> response = misskeyService.uploadFile(tokenBody, filePart, sensitiveBody, commentBody).execute();
                if (response.isSuccessful() && response.body() != null) {
                    attachment = response.body().toAttachment();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Attachment finalAttachment = attachment;
            mainHandler.post(() -> attachmentMutableLiveData.setValue(finalAttachment));
        }).start();
        return attachmentMutableLiveData;
    }

    public LiveData<ScheduledStatuses> getScheduledNotes(@NonNull String instance, String token, int limit, int offset) {
        scheduledStatusesMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ScheduledListRequest request = new MisskeyRequest.ScheduledListRequest(token);
            request.limit = limit;
            request.offset = offset;

            ScheduledStatuses scheduledStatuses = new ScheduledStatuses();
            try {
                Response<List<MisskeyScheduledNote>> response = misskeyService.getScheduledNotes(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<ScheduledStatus> list = new ArrayList<>();
                    for (MisskeyScheduledNote note : response.body()) {
                        list.add(note.toScheduledStatus());
                    }
                    scheduledStatuses.scheduledStatuses = list;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> scheduledStatusesMutableLiveData.setValue(scheduledStatuses));
        }).start();
        return scheduledStatusesMutableLiveData;
    }

    public LiveData<Boolean> cancelScheduledNote(@NonNull String instance, String token, String draftId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ScheduledCancelRequest request = new MisskeyRequest.ScheduledCancelRequest(token, draftId);

            boolean success = false;
            try {
                Response<Void> response = misskeyService.cancelScheduledNote(request).execute();
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
}
