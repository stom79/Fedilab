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

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.net.Uri;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Accounts;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.misskey.client.endpoints.MisskeyService;
import app.fedilab.android.misskey.client.entities.MisskeyFile;
import app.fedilab.android.misskey.client.entities.MisskeyRequest;
import app.fedilab.android.misskey.client.entities.MisskeyUser;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisskeyAccountsVM extends AndroidViewModel {

    private final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());
    private MutableLiveData<Account> accountMutableLiveData;
    private MutableLiveData<Accounts> accountsMutableLiveData;
    private MutableLiveData<RelationShip> relationShipMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;

    public MisskeyAccountsVM(@NonNull Application application) {
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

    public LiveData<Account> verifyCredentials(@NonNull String instance, String token) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);
            Account account = null;
            try {
                Response<MisskeyUser> response = misskeyService.verifyCredentials(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    account = response.body().toAccount();
                    account.url = "https://" + instance + "/@" + account.username;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            mainHandler.post(() -> accountMutableLiveData.setValue(finalAccount));
        }).start();
        return accountMutableLiveData;
    }

    public LiveData<Account> getAccount(@NonNull String instance, String token, String userId) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            Account account = null;
            try {
                Response<MisskeyUser> response = misskeyService.getUser(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    account = response.body().toAccount();
                    account.url = "https://" + instance + "/@" + account.username;
                    account.relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            mainHandler.post(() -> accountMutableLiveData.setValue(finalAccount));
        }).start();
        return accountMutableLiveData;
    }

    public LiveData<Account> getAccountByUsername(@NonNull String instance, String token, String username, String host) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UsernameRequest request = new MisskeyRequest.UsernameRequest(token, username, host);
            Account account = null;
            try {
                Response<MisskeyUser> response = misskeyService.getUserByUsername(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    account = response.body().toAccount();
                    String targetHost = host != null ? host : instance;
                    account.url = "https://" + targetHost + "/@" + account.username;
                    account.relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            mainHandler.post(() -> accountMutableLiveData.setValue(finalAccount));
        }).start();
        return accountMutableLiveData;
    }

    public LiveData<Accounts> getFollowers(@NonNull String instance, String token, String userId, String maxId, Integer limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            Accounts accounts = new Accounts();
            accounts.accounts = new ArrayList<>();
            accounts.pagination = new Pagination();
            try {
                Response<List<MisskeyService.MisskeyFollowing>> response = misskeyService.getFollowers(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyService.MisskeyFollowing following : response.body()) {
                        if (following.follower != null) {
                            accounts.accounts.add(following.follower.toAccount());
                        }
                    }
                    if (!response.body().isEmpty()) {
                        accounts.pagination.max_id = response.body().get(response.body().size() - 1).id;
                        accounts.pagination.min_id = response.body().get(0).id;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountsMutableLiveData.setValue(accounts));
        }).start();
        return accountsMutableLiveData;
    }

    public LiveData<Accounts> getFollowing(@NonNull String instance, String token, String userId, String maxId, Integer limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            Accounts accounts = new Accounts();
            accounts.accounts = new ArrayList<>();
            accounts.pagination = new Pagination();
            try {
                Response<List<MisskeyService.MisskeyFollowing>> response = misskeyService.getFollowing(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyService.MisskeyFollowing following : response.body()) {
                        if (following.followee != null) {
                            accounts.accounts.add(following.followee.toAccount());
                        }
                    }
                    if (!response.body().isEmpty()) {
                        accounts.pagination.max_id = response.body().get(response.body().size() - 1).id;
                        accounts.pagination.min_id = response.body().get(0).id;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountsMutableLiveData.setValue(accounts));
        }).start();
        return accountsMutableLiveData;
    }

    public LiveData<RelationShip> follow(@NonNull String instance, String token, String userId) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            RelationShip relationShip = null;
            try {
                Response<MisskeyUser> response = misskeyService.follow(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<RelationShip> unfollow(@NonNull String instance, String token, String userId) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            RelationShip relationShip = null;
            try {
                Response<MisskeyUser> response = misskeyService.unfollow(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<RelationShip> block(@NonNull String instance, String token, String userId) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            RelationShip relationShip = null;
            try {
                Response<MisskeyUser> response = misskeyService.block(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<RelationShip> unblock(@NonNull String instance, String token, String userId) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            RelationShip relationShip = null;
            try {
                Response<MisskeyUser> response = misskeyService.unblock(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    relationShip = buildRelationShip(response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<RelationShip> mute(@NonNull String instance, String token, String userId) {
        return mute(instance, token, userId, null);
    }

    public LiveData<RelationShip> mute(@NonNull String instance, String token, String userId, Long expiresAt) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.MuteRequest request = new MisskeyRequest.MuteRequest(token, userId, expiresAt);
            RelationShip relationShip = null;
            try {
                Response<Void> response = misskeyService.mute(request).execute();
                if (response.isSuccessful()) {
                    relationShip = new RelationShip();
                    relationShip.id = userId;
                    relationShip.muting = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<RelationShip> unmute(@NonNull String instance, String token, String userId) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            RelationShip relationShip = null;
            try {
                Response<Void> response = misskeyService.unmute(request).execute();
                if (response.isSuccessful()) {
                    relationShip = new RelationShip();
                    relationShip.id = userId;
                    relationShip.muting = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            mainHandler.post(() -> relationShipMutableLiveData.setValue(finalRelationShip));
        }).start();
        return relationShipMutableLiveData;
    }

    public LiveData<Accounts> getMutes(@NonNull String instance, String token, String maxId, Integer limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            Accounts accounts = new Accounts();
            accounts.accounts = new ArrayList<>();
            accounts.pagination = new Pagination();
            try {
                Response<List<MisskeyService.MisskeyMuting>> response = misskeyService.getMutes(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyService.MisskeyMuting muting : response.body()) {
                        if (muting.mutee != null) {
                            accounts.accounts.add(muting.mutee.toAccount());
                        }
                    }
                    if (!response.body().isEmpty()) {
                        accounts.pagination.max_id = response.body().get(response.body().size() - 1).id;
                        accounts.pagination.min_id = response.body().get(0).id;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountsMutableLiveData.setValue(accounts));
        }).start();
        return accountsMutableLiveData;
    }

    public LiveData<Accounts> getBlocks(@NonNull String instance, String token, String maxId, Integer limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest request = new MisskeyRequest(token);
            request.limit = limit != null ? limit : 20;
            request.untilId = maxId;
            Accounts accounts = new Accounts();
            accounts.accounts = new ArrayList<>();
            accounts.pagination = new Pagination();
            try {
                Response<List<MisskeyService.MisskeyBlocking>> response = misskeyService.getBlocks(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyService.MisskeyBlocking blocking : response.body()) {
                        if (blocking.blockee != null) {
                            accounts.accounts.add(blocking.blockee.toAccount());
                        }
                    }
                    if (!response.body().isEmpty()) {
                        accounts.pagination.max_id = response.body().get(response.body().size() - 1).id;
                        accounts.pagination.min_id = response.body().get(0).id;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountsMutableLiveData.setValue(accounts));
        }).start();
        return accountsMutableLiveData;
    }

    public LiveData<Account> updateProfilePicture(
            @NonNull String instance,
            String token,
            @NonNull Uri fileUri,
            boolean isAvatar) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            Account account = null;
            try {
                MultipartBody.Part filePart = Helper.getMultipartBody(getApplication(), "file", fileUri);
                RequestBody tokenBody = RequestBody.create(MediaType.parse("text/plain"), token);
                RequestBody sensitiveBody = RequestBody.create(MediaType.parse("text/plain"), "false");
                Response<MisskeyFile> uploadResponse = misskeyService.uploadFile(tokenBody, filePart, sensitiveBody, null).execute();
                if (uploadResponse.isSuccessful() && uploadResponse.body() != null) {
                    MisskeyRequest.UpdateProfileRequest request = new MisskeyRequest.UpdateProfileRequest(token);
                    if (isAvatar) {
                        request.avatarId = uploadResponse.body().id;
                    } else {
                        request.bannerId = uploadResponse.body().id;
                    }
                    Response<MisskeyUser> profileResponse = misskeyService.updateProfile(request).execute();
                    if (profileResponse.isSuccessful() && profileResponse.body() != null) {
                        account = profileResponse.body().toAccount();
                        account.url = "https://" + instance + "/@" + account.username;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            mainHandler.post(() -> accountMutableLiveData.setValue(finalAccount));
        }).start();
        return accountMutableLiveData;
    }

    public LiveData<Account> updateCredentials(
            @NonNull String instance,
            String token,
            String displayName,
            String note,
            Boolean locked,
            Boolean bot) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UpdateProfileRequest request = new MisskeyRequest.UpdateProfileRequest(token);
            request.name = displayName;
            request.description = note;
            request.isLocked = locked;
            request.isBot = bot;
            Account account = null;
            try {
                Response<MisskeyUser> response = misskeyService.updateProfile(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    account = response.body().toAccount();
                    account.url = "https://" + instance + "/@" + account.username;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            mainHandler.post(() -> accountMutableLiveData.setValue(finalAccount));
        }).start();
        return accountMutableLiveData;
    }

    public LiveData<Accounts> searchUsers(@NonNull String instance, String token, String query, Integer limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.SearchRequest request = new MisskeyRequest.SearchRequest(token, query);
            request.limit = limit != null ? limit : 20;
            Accounts accounts = new Accounts();
            accounts.accounts = new ArrayList<>();
            accounts.pagination = new Pagination();
            try {
                Response<List<MisskeyUser>> response = misskeyService.searchUsers(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (MisskeyUser user : response.body()) {
                        accounts.accounts.add(user.toAccount());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> accountsMutableLiveData.setValue(accounts));
        }).start();
        return accountsMutableLiveData;
    }

    public LiveData<Boolean> acceptFollow(@NonNull String instance, String token, String userId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            boolean success = false;
            try {
                Response<Void> response = misskeyService.acceptFollowRequest(request).execute();
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

    public LiveData<Boolean> rejectFollow(@NonNull String instance, String token, String userId) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.UserIdRequest request = new MisskeyRequest.UserIdRequest(token, userId);
            boolean success = false;
            try {
                Response<Void> response = misskeyService.rejectFollowRequest(request).execute();
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

    public LiveData<Boolean> reportAbuse(@NonNull String instance, String token, String userId, String comment) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MisskeyService misskeyService = init(instance);
            MisskeyRequest.ReportRequest request = new MisskeyRequest.ReportRequest(token, userId);
            request.comment = comment;
            boolean success = false;
            try {
                Response<Void> response = misskeyService.reportAbuse(request).execute();
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

    private RelationShip buildRelationShip(MisskeyUser user) {
        RelationShip relationShip = new RelationShip();
        relationShip.id = user.id;
        relationShip.following = user.isFollowing;
        relationShip.followed_by = user.isFollowed;
        relationShip.blocking = user.isBlocking;
        relationShip.blocked_by = user.isBlocked;
        relationShip.muting = user.isMuted;
        relationShip.requested = user.hasPendingFollowRequestFromYou;
        return relationShip;
    }
}
