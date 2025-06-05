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


import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.net.IDN;
import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonAccountsService;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Accounts;
import app.fedilab.android.mastodon.client.entities.api.Domains;
import app.fedilab.android.mastodon.client.entities.api.FamiliarFollowers;
import app.fedilab.android.mastodon.client.entities.api.FeaturedTag;
import app.fedilab.android.mastodon.client.entities.api.Field;
import app.fedilab.android.mastodon.client.entities.api.Filter;
import app.fedilab.android.mastodon.client.entities.api.IdentityProof;
import app.fedilab.android.mastodon.client.entities.api.MastodonList;
import app.fedilab.android.mastodon.client.entities.api.Pagination;
import app.fedilab.android.mastodon.client.entities.api.Preferences;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.client.entities.api.Report;
import app.fedilab.android.mastodon.client.entities.api.Source;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Statuses;
import app.fedilab.android.mastodon.client.entities.api.Suggestion;
import app.fedilab.android.mastodon.client.entities.api.Suggestions;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.client.entities.api.Token;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.MutedAccounts;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AccountsVM extends AndroidViewModel {


    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());


    private MutableLiveData<Account> accountMutableLiveData;
    private MutableLiveData<List<Account>> accountListMutableLiveData;
    private MutableLiveData<Suggestions> suggestionsMutableLiveData;
    private MutableLiveData<Statuses> statusesMutableLiveData;
    private MutableLiveData<Accounts> accountsMutableLiveData;
    private MutableLiveData<List<Status>> statusListMutableLiveData;
    private MutableLiveData<FeaturedTag> featuredTagMutableLiveData;
    private MutableLiveData<List<FeaturedTag>> featuredTagListMutableLiveData;
    private MutableLiveData<List<MastodonList>> mastodonListListMutableLiveData;
    private MutableLiveData<List<IdentityProof>> identityProofListMutableLiveData;
    private MutableLiveData<RelationShip> relationShipMutableLiveData;
    private MutableLiveData<List<RelationShip>> relationShipListMutableLiveData;
    private MutableLiveData<List<FamiliarFollowers>> familiarFollowersListMutableLiveData;
    private MutableLiveData<Filter> filterMutableLiveData;
    private MutableLiveData<List<Filter>> filterListMutableLiveData;
    private MutableLiveData<List<Tag>> tagListMutableLiveData;
    private MutableLiveData<Preferences> preferencesMutableLiveData;
    private MutableLiveData<Token> tokenMutableLiveData;
    private MutableLiveData<Domains> domainsMutableLiveData;
    private MutableLiveData<Report> reportMutableLiveData;
    private MutableLiveData<Boolean> booleanMutableLiveData;


    public AccountsVM(@NonNull Application application) {
        super(application);
    }

    private MastodonAccountsService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAccountsService.class);
    }

    private MastodonAccountsService initv2(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v2/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonAccountsService.class);
    }

    /**
     * Get connected account
     *
     * @return LiveData<Account>
     */
    public LiveData<Account> getConnectedAccount(@NonNull String instance, String token) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Account account = null;
            Call<Account> accountCall = mastodonAccountsService.verify_credentials(token);
            if (accountCall != null) {
                try {
                    Response<Account> accountResponse = accountCall.execute();
                    if (accountResponse.isSuccessful()) {
                        account = accountResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            Runnable myRunnable = () -> accountMutableLiveData.setValue(finalAccount);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }

    /**
     * Register an Account
     *
     * @param username  String
     * @param email     String
     * @param password  String
     * @param agreement boolean
     * @param locale    boolean
     * @param reason    String
     * @return Token - {@link Token}
     */
    public LiveData<Token> registerAccount(@NonNull String instance, String token,
                                           @NonNull String username,
                                           @NonNull String email,
                                           @NonNull String password,
                                           boolean agreement,
                                           @NonNull String locale,
                                           String reason) {
        tokenMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            MastodonAccountsService mastodonAccountsService = init(instance);
            Call<Token> stringCall = mastodonAccountsService.registerAccount(token, username, email, password, agreement, locale, reason);
            Token returnedToken = null;
            String errorMessage = null;
            if (stringCall != null) {
                try {
                    Response<Token> stringResponse = stringCall.execute();
                    if (stringResponse.isSuccessful()) {
                        returnedToken = stringResponse.body();
                    } else {
                        if (stringResponse.errorBody() != null) {
                            errorMessage = stringResponse.errorBody().string();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage() != null ? e.getMessage() : getApplication().getString(R.string.toast_error);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Token finalReturnedToken = returnedToken;
            String finalErrorMessage = errorMessage;
            Runnable myRunnable = () -> {
                if (finalErrorMessage != null) {
                    Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, finalErrorMessage);
                }
                tokenMutableLiveData.setValue(finalReturnedToken);
            };
            mainHandler.post(myRunnable);
        }).start();
        return tokenMutableLiveData;
    }

    /**
     * Allow to only upload avatar or header when editing profile
     *
     * @return {@link LiveData} containing an {@link Account}
     */
    public LiveData<Account> updateProfilePicture(@NonNull String instance, String token, Uri uri, UpdateMediaType type) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Account account = null;
            MultipartBody.Part avatarMultipartBody = null;
            MultipartBody.Part headerMultipartBody = null;

            if (type == UpdateMediaType.AVATAR) {
                avatarMultipartBody = Helper.getMultipartBody(getApplication(), "avatar", uri);
            } else if (type == UpdateMediaType.HEADER) {
                headerMultipartBody = Helper.getMultipartBody(getApplication(), "header", uri);
            }
            Call<Account> accountCall = mastodonAccountsService.update_media(
                    token, avatarMultipartBody,
                    headerMultipartBody);
            if (accountCall != null) {
                try {
                    Response<Account> accountResponse = accountCall.execute();
                    if (accountResponse.isSuccessful()) {
                        account = accountResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            Runnable myRunnable = () -> accountMutableLiveData.setValue(finalAccount);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }

    /**
     * Update account credentials
     *
     * @param discoverable Whether the account should be shown in the profile directory.
     * @param bot          Whether the account has a bot flag.
     * @param displayName  The display name to use for the profile.
     * @param note         The account bio.
     * @param locked       Whether manual approval of follow requests is required.
     * @param privacy      Default post privacy for authored statuses.
     * @param sensitive    Whether to mark authored statuses as sensitive by default.
     * @param language     Default language to use for authored statuses. (ISO 6391)
     * @param fields       Profile metadata name (By default, max 4 fields and 255 characters per property/value)
     * @return {@link LiveData} containing an {@link Account}
     */
    public LiveData<Account> updateCredentials(@NonNull String instance, String token,
                                               Boolean discoverable,
                                               Boolean bot,
                                               String displayName,
                                               String note,
                                               Boolean locked,
                                               String privacy,
                                               Boolean sensitive,
                                               String language,
                                               LinkedHashMap<Integer, Field.FieldParams> fields
    ) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Account account = null;
            Account.AccountParams accountParams = new Account.AccountParams();
            accountParams.bot = bot;
            accountParams.discoverable = discoverable;
            accountParams.display_name = displayName;
            accountParams.note = note;
            accountParams.locked = locked;
            accountParams.source = new Source.SourceParams();
            accountParams.source.privacy = privacy;
            accountParams.source.language = language;
            accountParams.source.sensitive = sensitive;
            accountParams.fields = fields;
            Call<Account> accountCall = mastodonAccountsService.update_credentials(token, accountParams);
            // Call<Account> accountCall = mastodonAccountsService.update_credentials(token, discoverable, bot, displayName, note, locked, privacy, sensitive, language, fields);
            if (accountCall != null) {
                try {
                    Response<Account> accountResponse = accountCall.execute();
                    if (accountResponse.isSuccessful()) {
                        account = accountResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Account finalAccount = account;
            Runnable myRunnable = () -> accountMutableLiveData.setValue(finalAccount);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }


    /**
     * @param acct The acct of the account
     * @return {@link LiveData} containing an {@link Account}
     */
    public LiveData<Account> lookUpAccount(@NonNull String instance, @NonNull String acct) {
        accountMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Account account = null;
            Call<Account> accountCall = mastodonAccountsService.lookUpAccount(acct);
            if (accountCall != null) {

                try {
                    Response<Account> accountResponse = accountCall.execute();
                    if (accountResponse.isSuccessful()) {
                        account = accountResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Account finalAccount = account;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountMutableLiveData.setValue(finalAccount);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }

    /**
     * @param id The id of the account
     * @return {@link LiveData} containing an {@link Account}
     */
    public LiveData<Account> getAccount(@NonNull String instance, String token, @NonNull String id) {
        accountMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Account account = null;
            Call<Account> accountCall = mastodonAccountsService.getAccount(token, id);
            if (accountCall != null) {
                try {
                    Response<Account> accountResponse = accountCall.execute();
                    if (accountResponse.isSuccessful()) {
                        account = accountResponse.body();
                        new CachedBundle(getApplication().getApplicationContext()).insertAccountBundle(account, Helper.getCurrentAccount(getApplication().getApplicationContext()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Account finalAccount = account;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountMutableLiveData.setValue(finalAccount);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }

    /**
     * Statuses posted to the given account.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing a {@link Statuses}. Note: Not to be confused with {@link Status}
     */
    public LiveData<Statuses> getAccountStatuses(@NonNull String instance, String token, @NonNull String id,
                                                 String maxId,
                                                 String sinceId,
                                                 String minId,
                                                 Boolean excludeReplies,
                                                 Boolean excludeReblogs,
                                                 Boolean only_media,
                                                 Boolean pinned,
                                                 String tagged,
                                                 int count) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Status> statusList = null;
            Pagination pagination = null;
            Call<List<Status>> accountStatusesCall = mastodonAccountsService.getAccountStatuses(
                    token, id, maxId, sinceId, minId, excludeReplies, excludeReblogs, only_media, pinned, tagged, count);
            if (accountStatusesCall != null) {
                try {
                    Response<List<Status>> accountStatusesResponse = accountStatusesCall.execute();
                    if (accountStatusesResponse.isSuccessful()) {
                        statusList = accountStatusesResponse.body();
                        pagination = MastodonHelper.getPagination(accountStatusesResponse.headers());

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Statuses statuses = new Statuses();
            statuses.statuses = statusList;
            statuses.pagination = pagination;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> statusesMutableLiveData.setValue(statuses);
            mainHandler.post(myRunnable);
        }).start();
        return statusesMutableLiveData;
    }

    /**
     * Accounts which follow the given account, if network is not hidden by the account owner.
     *
     * @return {@link LiveData} containing an {@link Accounts}. Note: Not to be confused with {@link Account}
     */
    public LiveData<Accounts> getAccountFollowers(@NonNull String instance, String token, @NonNull String id,
                                                  String maxId,
                                                  String sinceId) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Pagination pagination = null;
            Call<List<Account>> followersCall = mastodonAccountsService.getAccountFollowers(token, id, maxId, sinceId);
            if (followersCall != null) {
                try {
                    Response<List<Account>> followersResponse = followersCall.execute();
                    if (followersResponse.isSuccessful()) {
                        accountList = followersResponse.body();
                        pagination = MastodonHelper.getPagination(followersResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Accounts accounts = new Accounts();
            accounts.accounts = accountList;
            accounts.pagination = pagination;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Accounts which the given account is following, if network is not hidden by the account owner.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing an {@link Accounts}. Note: Not to be confused with {@link Account}
     */
    public LiveData<Accounts> getAccountFollowing(@NonNull String instance, String token, @NonNull String id,
                                                  String maxId,
                                                  String sinceId) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Pagination pagination = null;
            Call<List<Account>> followingCall = mastodonAccountsService.getAccountFollowing(token, id, maxId, sinceId);
            if (followingCall != null) {
                try {
                    Response<List<Account>> followingResponse = followingCall.execute();
                    if (followingResponse.isSuccessful()) {
                        accountList = followingResponse.body();
                        pagination = MastodonHelper.getPagination(followingResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Accounts accounts = new Accounts();
            accounts.accounts = accountList;
            accounts.pagination = pagination;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Tags featured by this account.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing a {@link List} of {@link FeaturedTag}s
     */
    public LiveData<List<FeaturedTag>> getAccountFeaturedTags(@NonNull String instance, String token, @NonNull String id) {
        featuredTagListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<FeaturedTag> featuredTagList = null;
            Call<List<FeaturedTag>> featuredTagsCall = mastodonAccountsService.getAccountFeaturedTags(token, id);
            if (featuredTagsCall != null) {
                try {
                    Response<List<FeaturedTag>> featuredTagsResponse = featuredTagsCall.execute();
                    if (featuredTagsResponse.isSuccessful()) {
                        featuredTagList = featuredTagsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<FeaturedTag> finalFeaturedTagList = featuredTagList;
            Runnable myRunnable = () -> featuredTagListMutableLiveData.setValue(finalFeaturedTagList);
            mainHandler.post(myRunnable);
        }).start();
        return featuredTagListMutableLiveData;
    }

    /**
     * User lists that you have added this account to.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing a {@link List} of {@link MastodonList}s
     */
    public LiveData<List<MastodonList>> getListContainingAccount(@NonNull String instance, String token, @NonNull String id) {
        mastodonListListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<MastodonList> mastodonListList = null;
            Call<List<MastodonList>> listsCall = mastodonAccountsService.getListContainingAccount(token, id);
            if (listsCall != null) {
                try {
                    Response<List<MastodonList>> listsResponse = listsCall.execute();
                    if (listsResponse.isSuccessful()) {
                        mastodonListList = listsResponse.body();
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
     * List of IdentityProofs
     *
     * @param id The id of the account
     * @return {@link LiveData} containing a {@link List} of {@link IdentityProof}s of the given account
     */
    public LiveData<List<IdentityProof>> getIdentityProofs(@NonNull String instance, String token, @NonNull String id) {
        identityProofListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<IdentityProof> identityProofList = null;
            Call<List<IdentityProof>> identityProofsCall = mastodonAccountsService.getIdentityProofs(token, id);
            if (identityProofsCall != null) {
                try {
                    Response<List<IdentityProof>> identityProofsResponse = identityProofsCall.execute();
                    if (identityProofsResponse.isSuccessful()) {
                        identityProofList = identityProofsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<IdentityProof> finalIdentityProofList = identityProofList;
            Runnable myRunnable = () -> identityProofListMutableLiveData.setValue(finalIdentityProofList);
            mainHandler.post(myRunnable);
        }).start();
        return identityProofListMutableLiveData;
    }

    /**
     * Update account notes
     *
     * @param id       The id of the account
     * @param commment note for the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> updateNote(@NonNull String instance, String token, @NonNull String id, String commment) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> noteCall = mastodonAccountsService.note(token, id, commment);
            if (noteCall != null) {
                try {
                    Response<RelationShip> followResponse = noteCall.execute();
                    if (followResponse.isSuccessful()) {
                        relationShip = followResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Follow the given account. Can also be used to update whether to show reblogs or enable notifications.
     *
     * @param id        The id of the account
     * @param reblogs   Receive this account's reblogs in home timeline? Defaults to true.
     * @param notify    Receive notifications when this account posts a status? Defaults to false.
     * @param languages Filter received statuses for these languages.
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> follow(@NonNull String instance, String token, @NonNull String id, boolean reblogs, boolean notify, List<String> languages) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> followCall = mastodonAccountsService.follow(token, id, reblogs, notify, languages);
            if (followCall != null) {
                try {
                    Response<RelationShip> followResponse = followCall.execute();
                    if (followResponse.isSuccessful()) {

                        relationShip = followResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Unfollow the given account.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> unfollow(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> unfollowCall = mastodonAccountsService.unfollow(token, id);
            if (unfollowCall != null) {
                try {
                    Response<RelationShip> unfollowResponse = unfollowCall.execute();
                    if (unfollowResponse.isSuccessful()) {
                        relationShip = unfollowResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Block the given account. Clients should filter statuses from this account if received (e.g. due to a boost in the Home timeline)
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> block(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> blockCall = mastodonAccountsService.block(token, id);
            if (blockCall != null) {
                try {
                    Response<RelationShip> blockResponse = blockCall.execute();
                    if (blockResponse.isSuccessful()) {
                        relationShip = blockResponse.body();
                        StatusAdapter.sendAction(getApplication().getApplicationContext(), Helper.ARG_DELETE_ALL_FOR_ACCOUNT_ID, null, id);
                        new StatusCache(getApplication().getApplicationContext()).deleteStatusForTargetedAccount(MainActivity.currentInstance, MainActivity.currentUserID, id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Unblock the given account.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> unblock(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> unblockCall = mastodonAccountsService.unblock(token, id);
            if (unblockCall != null) {
                try {
                    Response<RelationShip> unblockResponse = unblockCall.execute();
                    if (unblockResponse.isSuccessful()) {
                        relationShip = unblockResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Mute the given account. Clients should filter statuses and notifications from this account, if received (e.g. due to a boost in the Home timeline).
     *
     * @param id            The id of the account
     * @param notifications Mute notifications in addition to statuses? Defaults to true.
     * @param duration      How long the mute should last, in seconds. Defaults to 0 (indefinite).
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> mute(@NonNull String instance, String token, @NonNull String id, Boolean notifications, Integer duration) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> muteCall = mastodonAccountsService.mute(token, id, notifications, duration);
            if (muteCall != null) {
                try {
                    Response<RelationShip> muteResponse = muteCall.execute();
                    if (muteResponse.isSuccessful()) {
                        relationShip = muteResponse.body();
                        StatusAdapter.sendAction(getApplication().getApplicationContext(), Helper.ARG_DELETE_ALL_FOR_ACCOUNT_ID, null, id);
                        new StatusCache(getApplication().getApplicationContext()).deleteStatusForTargetedAccount(MainActivity.currentInstance, MainActivity.currentUserID, id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }


    /**
     * Mute the given account in db
     *
     * @return {@link LiveData} containing the {@link Account} to the given account
     */
    public LiveData<Accounts> getMutedHome(@NonNull BaseAccount forAccount) {
        accountsMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Accounts accounts = new Accounts();
            MutedAccounts mutedAccount;
            try {
                mutedAccount = new MutedAccounts(getApplication().getApplicationContext()).getMutedAccount(forAccount);
                if (mutedAccount != null) {
                    accounts.accounts = mutedAccount.accounts;
                }
                accounts.pagination = new Pagination();
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }


    /**
     * Mute the given account in db
     *
     * @return {@link LiveData} containing the {@link Account} to the given account
     */
    public LiveData<Boolean> isMuted(@NonNull BaseAccount forAccount, @NonNull Account target) {
        booleanMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            boolean isMuted = false;
            try {
                isMuted = new MutedAccounts(getApplication().getApplicationContext()).isMuted(forAccount, target);
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            boolean finalIsMuted = isMuted;
            Runnable myRunnable = () -> booleanMutableLiveData.setValue(finalIsMuted);
            mainHandler.post(myRunnable);
        }).start();
        return booleanMutableLiveData;
    }

    /**
     * Mute the given account in db
     *
     * @return {@link LiveData} containing the {@link Account} to the given account
     */
    public LiveData<Account> muteHome(@NonNull BaseAccount forAccount, @NonNull Account target) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            try {
                new MutedAccounts(getApplication().getApplicationContext()).muteAccount(forAccount, target);
                Helper.addMutedAccount(target);
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            StatusAdapter.sendAction(getApplication().getApplicationContext(), Helper.ARG_STATUS_ACCOUNT_ID_DELETED, null, target.id);
            Runnable myRunnable = () -> accountMutableLiveData.setValue(target);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }


    /**
     * Mute the given account in db
     *
     * @return {@link LiveData} containing the {@link Account} to the given account
     */
    public LiveData<List<Account>> muteAccountsHome(@NonNull BaseAccount forAccount, @NonNull List<Account> targets) {
        accountListMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            try {
                for (Account target : targets) {
                    new MutedAccounts(getApplication().getApplicationContext()).muteAccount(forAccount, target);
                    StatusAdapter.sendAction(getApplication().getApplicationContext(), Helper.ARG_STATUS_ACCOUNT_ID_DELETED, null, target.id);
                    Helper.addMutedAccount(target);
                }
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountListMutableLiveData.setValue(targets);
            mainHandler.post(myRunnable);
        }).start();
        return accountListMutableLiveData;
    }


    /**
     * Unmute the given account in db
     *
     * @return {@link LiveData} containing the {@link Account} to the given account
     */
    public LiveData<Account> unmuteHome(@NonNull BaseAccount forAccount, @NonNull Account target) {
        accountMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            try {
                new MutedAccounts(getApplication().getApplicationContext()).unMuteAccount(forAccount, target);
                Helper.removeMutedAccount(target);
            } catch (DBException e) {
                e.printStackTrace();
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountMutableLiveData.setValue(target);
            mainHandler.post(myRunnable);
        }).start();
        return accountMutableLiveData;
    }

    /**
     * Unmute the given account.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> unmute(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> unmuteCall = mastodonAccountsService.unmute(token, id);
            if (unmuteCall != null) {
                try {
                    Response<RelationShip> unmuteResponse = unmuteCall.execute();
                    if (unmuteResponse.isSuccessful()) {
                        relationShip = unmuteResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Add the given account to the user's featured profiles. (Featured profiles are currently shown on the user's own public profile.)
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> endorse(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> endorseCall = mastodonAccountsService.endorse(token, id);
            if (endorseCall != null) {
                try {
                    Response<RelationShip> endorseResponse = endorseCall.execute();
                    if (endorseResponse.isSuccessful()) {
                        relationShip = endorseResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Remove the given account from the user's featured profiles.
     *
     * @param id The id of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> unendorse(@NonNull String instance, String token, @NonNull String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            RelationShip relationShip = null;
            MastodonAccountsService mastodonAccountsService = init(instance);
            Call<RelationShip> unendorseCall = mastodonAccountsService.unendorse(token, id);
            if (unendorseCall != null) {
                try {
                    Response<RelationShip> unendorseResponse = unendorseCall.execute();
                    if (unendorseResponse.isSuccessful()) {
                        relationShip = unendorseResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Sets a private note on a user.
     *
     * @param id      The id of the account
     * @param comment The comment to be set on that user. Provide an empty string or leave out this parameter to clear the currently set note.
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> note(@NonNull String instance, String token, @NonNull String id, String comment) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> noteCall = mastodonAccountsService.note(token, id, comment);
            if (noteCall != null) {
                try {
                    Response<RelationShip> noteResponse = noteCall.execute();
                    if (noteResponse.isSuccessful()) {
                        relationShip = noteResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                RelationShip finalRelationShip = relationShip;
                Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
                mainHandler.post(myRunnable);
            }
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Find out whether a given account is followed, blocked, muted, etc.
     *
     * @param ids {@link List} of account IDs to check
     * @return {@link LiveData} containing a {@link List} of {@link RelationShip}s to given account(s)
     */
    public LiveData<List<RelationShip>> getRelationships(@NonNull String instance, String token, @NonNull List<String> ids) {
        relationShipListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<RelationShip> relationShipList = null;
            Call<List<RelationShip>> relationshipsCall = mastodonAccountsService.getRelationships(token, ids);
            if (relationshipsCall != null) {
                try {
                    Response<List<RelationShip>> relationshipsResponse = relationshipsCall.execute();
                    if (relationshipsResponse.isSuccessful()) {
                        relationShipList = relationshipsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<RelationShip> finalRelationShipList = relationShipList;
            Runnable myRunnable = () -> relationShipListMutableLiveData.setValue(finalRelationShipList);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipListMutableLiveData;
    }


    /**
     * Obtain a list of all accounts that follow a given account, filtered for accounts you follow.
     *
     * @param ids {@link List} of account IDs to check
     * @return {@link LiveData} containing a {@link List} of {@link FamiliarFollowers}s to given account(s)
     */
    public LiveData<List<FamiliarFollowers>> getFamiliarFollowers(@NonNull String instance, String token, @NonNull List<String> ids) {
        familiarFollowersListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<FamiliarFollowers> familiarFollowers = null;
            Call<List<FamiliarFollowers>> familiarFollowersCall = mastodonAccountsService.getFamiliarFollowers(token, ids);

            if (familiarFollowersCall != null) {
                try {
                    Response<List<FamiliarFollowers>> familiarFollowersResponse = familiarFollowersCall.execute();
                    if (familiarFollowersResponse.isSuccessful()) {
                        familiarFollowers = familiarFollowersResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            List<FamiliarFollowers> finalFamiliarFollowers = familiarFollowers;
            Runnable myRunnable = () -> familiarFollowersListMutableLiveData.setValue(finalFamiliarFollowers);
            mainHandler.post(myRunnable);
        }).start();
        return familiarFollowersListMutableLiveData;
    }

    /**
     * Search for matching accounts by username or display name.
     *
     * @param q         What to search for
     * @param limit     Maximum number of results. Defaults to 40.
     * @param resolve   Attempt WebFinger lookup. Defaults to false. Use this when q is an exact address.
     * @param following Only who the user is following. Defaults to false.
     * @return {@link LiveData} containing a {@link List} of matching {@link Account}s
     */
    public LiveData<List<Account>> searchAccounts(@NonNull String instance, String token, @NonNull String q, int limit, boolean resolve, boolean following) {
        accountListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Call<List<Account>> searchCall = mastodonAccountsService.searchAccounts(token, q, limit, resolve, following);
            if (searchCall != null) {
                try {
                    Response<List<Account>> searchResponse = searchCall.execute();
                    if (searchResponse.isSuccessful()) {
                        accountList = searchResponse.body();
                        if (accountList != null && accountList.size() > 0) {
                            new CachedBundle(getApplication().getApplicationContext()).insertAccountBundle(accountList.get(0), Helper.getCurrentAccount(getApplication().getApplicationContext()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            List<Account> finalAccountList = accountList;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountListMutableLiveData.setValue(finalAccountList);
            mainHandler.post(myRunnable);
        }).start();
        return accountListMutableLiveData;
    }

    /**
     * Statuses the user has bookmarked.
     *
     * @return {@link LiveData} containing a {@link List} of {@link Status}es
     */
    public LiveData<Statuses> getBookmarks(@NonNull String instance, String token, String limit, String maxId, String sinceId, String minId) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Status> statusList;
            Statuses statuses = new Statuses();
            Call<List<Status>> bookmarksCall = mastodonAccountsService.getBookmarks(token, limit, maxId, sinceId, minId);
            if (bookmarksCall != null) {
                try {
                    Response<List<Status>> bookmarksResponse = bookmarksCall.execute();
                    if (bookmarksResponse.isSuccessful()) {
                        statusList = bookmarksResponse.body();
                        statuses.statuses = statusList;
                        statuses.pagination = MastodonHelper.getPagination(bookmarksResponse.headers());
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
     * Statuses the user has favourited.
     *
     * @return {@link LiveData} containing a {@link List} of {@link Status}es
     */
    public LiveData<Statuses> getFavourites(@NonNull String instance, String token, String limit, String minId, String maxId) {
        statusesMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Statuses statuses = new Statuses();
            Call<List<Status>> favouritesCall = mastodonAccountsService.getFavourites(token, limit, minId, maxId);
            List<Status> statusList;
            if (favouritesCall != null) {
                try {
                    Response<List<Status>> favouritesResponse = favouritesCall.execute();
                    if (favouritesResponse.isSuccessful()) {
                        statusList = favouritesResponse.body();
                        statuses.statuses = statusList;
                        statuses.pagination = MastodonHelper.getPagination(favouritesResponse.headers());
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
     * Accounts the user has muted.
     *
     * @param limit Maximum number of results to return per page. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<Accounts> getMutes(@NonNull String instance, String token, String limit, String maxId, String sinceId) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Accounts accounts = new Accounts();
            Call<List<Account>> mutesCall = mastodonAccountsService.getMutes(token, limit, maxId, sinceId);
            List<Account> accountList;
            if (mutesCall != null) {
                try {
                    Response<List<Account>> mutesResponse = mutesCall.execute();
                    if (mutesResponse.isSuccessful()) {
                        accountList = mutesResponse.body();
                        accounts.accounts = accountList;
                        accounts.pagination = MastodonHelper.getPagination(mutesResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Accounts the user has blocked.
     *
     * @param limit Maximum number of results. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<Accounts> getBlocks(@NonNull String instance, String token, String limit, String maxId, String sinceId) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList;
            Accounts accounts = new Accounts();
            Call<List<Account>> blocksCall = mastodonAccountsService.getBlocks(token, limit, maxId, sinceId);
            if (blocksCall != null) {
                try {
                    Response<List<Account>> blocksResponse = blocksCall.execute();
                    if (blocksResponse.isSuccessful()) {
                        accountList = blocksResponse.body();
                        accounts.accounts = accountList;
                        accounts.pagination = MastodonHelper.getPagination(blocksResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * View domains the user has blocked.
     *
     * @param limit Maximum number of results. Defaults to 40.
     * @return {@link LiveData} containing {@link Domains}
     */
    public LiveData<Domains> getDomainBlocks(@NonNull String instance, String token, String limit, String maxId, String sinceId) {
        domainsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        Domains domains = new Domains();
        new Thread(() -> {
            List<String> stringList = null;
            Call<List<String>> getDomainBlocksCall = mastodonAccountsService.getDomainBlocks(token, limit, maxId, sinceId);
            if (getDomainBlocksCall != null) {
                try {
                    Response<List<String>> getDomainBlocksResponse = getDomainBlocksCall.execute();
                    if (getDomainBlocksResponse.isSuccessful()) {
                        domains.domains = getDomainBlocksResponse.body();
                        domains.pagination = MastodonHelper.getPagination(getDomainBlocksResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> domainsMutableLiveData.setValue(domains);
            mainHandler.post(myRunnable);
        }).start();
        return domainsMutableLiveData;
    }

    /**
     * block a domain to:
     * • hide all public posts from it
     * • hide all notifications from it
     * • remove all followers from it
     * • prevent following new users from it (but does not remove existing follows)
     *
     * @param domain Domain to block.
     */
    public void addDomainBlocks(@NonNull String instance, String token, String domain) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Call<Void> addDomainBlockCall = mastodonAccountsService.addDomainBlock(token, domain);
            if (addDomainBlockCall != null) {
                try {
                    addDomainBlockCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Remove a domain block, if it exists in the user's array of blocked domains.
     *
     * @param domain Domain to unblock.
     */
    public void removeDomainBlocks(@NonNull String instance, String token, String domain) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Call<Void> removeDomainBlockCall = mastodonAccountsService.removeDomainBlocks(token, domain);
            if (removeDomainBlockCall != null) {
                try {
                    removeDomainBlockCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * File a report
     *
     * @param accountId ID of the account to report
     * @param statusIds {@link List} of IDs of statuses to attach to the report, for context
     * @param comment   Reason for the report (default max 1000 characters)
     * @param forward   If the account is remote, should the report be forwarded to the remote admin?
     */
    public LiveData<Report> report(@NonNull String instance, String token, @NonNull String accountId, String category, List<String> statusIds, List<String> ruleIds, String comment, Boolean forward) {
        reportMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Report report = null;
            Report.ReportParams reportParams = new Report.ReportParams();
            reportParams.account_id = accountId;
            reportParams.category = category;
            reportParams.comment = comment;
            reportParams.forward = forward;
            reportParams.rule_ids = ruleIds;
            reportParams.status_ids = statusIds;
            Call<Report> reportCall = mastodonAccountsService.report(token, reportParams);
            if (reportCall != null) {
                try {
                    Response<Report> reportRequestsResponse = reportCall.execute();
                    if (reportRequestsResponse.isSuccessful()) {
                        report = reportRequestsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Report finalReport = report;
            Runnable myRunnable = () -> reportMutableLiveData.setValue(finalReport);
            mainHandler.post(myRunnable);
        }).start();
        return reportMutableLiveData;
    }

    /**
     * View pending follow requests
     *
     * @param limit Maximum number of results to return. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<Accounts> getFollowRequests(@NonNull String instance, String token, String max_id, int limit) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Accounts accounts = new Accounts();
            Call<List<Account>> followRequestsCall = mastodonAccountsService.getFollowRequests(token, max_id, limit);
            if (followRequestsCall != null) {
                try {
                    Response<List<Account>> followRequestsResponse = followRequestsCall.execute();
                    if (followRequestsResponse.isSuccessful()) {
                        accountList = followRequestsResponse.body();
                        accounts.accounts = accountList;
                        accounts.pagination = MastodonHelper.getPagination(followRequestsResponse.headers());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
                mainHandler.post(myRunnable);
            }
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Accept a pending follow requests
     *
     * @param id ID of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> acceptFollow(@NonNull String instance, String token, String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> acceptFollowCall = mastodonAccountsService.acceptFollow(token, id);
            if (acceptFollowCall != null) {
                try {
                    Response<RelationShip> acceptFollowResponse = acceptFollowCall.execute();
                    if (acceptFollowResponse.isSuccessful()) {
                        relationShip = acceptFollowResponse.body();
                    }
                    new StatusCache(getApplication().getApplicationContext()).deleteNotifications(MainActivity.currentUserID, instance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * Reject a pending follow requests
     *
     * @param id ID of the account
     * @return {@link LiveData} containing the {@link RelationShip} to the given account
     */
    public LiveData<RelationShip> rejectFollow(@NonNull String instance, String token, String id) {
        relationShipMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            RelationShip relationShip = null;
            Call<RelationShip> rejectFollowCall = mastodonAccountsService.rejectFollow(token, id);
            if (rejectFollowCall != null) {
                try {
                    Response<RelationShip> rejectFollowResponse = rejectFollowCall.execute();
                    if (rejectFollowResponse.isSuccessful()) {
                        relationShip = rejectFollowResponse.body();
                    }
                    new StatusCache(getApplication().getApplicationContext()).deleteNotifications(MainActivity.currentUserID, instance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            RelationShip finalRelationShip = relationShip;
            Runnable myRunnable = () -> relationShipMutableLiveData.setValue(finalRelationShip);
            mainHandler.post(myRunnable);
        }).start();
        return relationShipMutableLiveData;
    }

    /**
     * View accounts that the user is currently featuring on their profile.
     *
     * @param limit Maximum number of results to return. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<List<Account>> getEndorsements(@NonNull String instance, String token, String limit, String maxId, String sinceId) {
        accountListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Account> accountList = null;
            Call<List<Account>> endorsementsCall = mastodonAccountsService.getEndorsements(token, limit, maxId, sinceId);
            if (endorsementsCall != null) {
                try {
                    Response<List<Account>> endorsementsResponse = endorsementsCall.execute();
                    if (endorsementsResponse.isSuccessful()) {
                        accountList = endorsementsResponse.body();
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
     * View your featured tags
     *
     * @return {@link LiveData} containing a {@link List} of {@link FeaturedTag}s
     */
    public LiveData<List<FeaturedTag>> getFeaturedTags(@NonNull String instance, String token) {
        featuredTagListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<FeaturedTag> featuredTagList = null;
            Call<List<FeaturedTag>> getFeaturedTagsCall = mastodonAccountsService.getFeaturedTags(token);
            if (getFeaturedTagsCall != null) {
                try {
                    Response<List<FeaturedTag>> getFeaturedTagsResponse = getFeaturedTagsCall.execute();
                    if (getFeaturedTagsResponse.isSuccessful()) {
                        featuredTagList = getFeaturedTagsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<FeaturedTag> finalFeaturedTagList = featuredTagList;
                Runnable myRunnable = () -> featuredTagListMutableLiveData.setValue(finalFeaturedTagList);
                mainHandler.post(myRunnable);
            }
        }).start();
        return featuredTagListMutableLiveData;
    }

    /**
     * Feature a tag
     *
     * @param name The hashtag to be featured.
     * @return {@link LiveData} containing a {@link FeaturedTag}
     */
    public LiveData<FeaturedTag> addFeaturedTag(@NonNull String instance, String token, @NonNull String name) {
        featuredTagMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            FeaturedTag featuredTag = null;
            Call<FeaturedTag> addFeaturedTagCall = mastodonAccountsService.addFeaturedTag(token, name);
            if (addFeaturedTagCall != null) {
                try {
                    Response<FeaturedTag> addFeaturedTagResponse = addFeaturedTagCall.execute();
                    if (addFeaturedTagResponse.isSuccessful()) {
                        featuredTag = addFeaturedTagResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            FeaturedTag finalFeaturedTag = featuredTag;
            Runnable myRunnable = () -> featuredTagMutableLiveData.setValue(finalFeaturedTag);
            mainHandler.post(myRunnable);
        }).start();
        return featuredTagMutableLiveData;
    }

    /**
     * Unfeature a tag
     *
     * @param id The id of the FeaturedTag to be unfeatured.
     */
    public void removeFeaturedTag(@NonNull String instance, String token, String id) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Call<Void> removeFeaturedTagCall = mastodonAccountsService.removeFeaturedTag(token, id);
            if (removeFeaturedTagCall != null) {
                try {
                    removeFeaturedTagCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Shows your 10 most-used tags, with usage history for the past week.
     *
     * @return {@link LiveData} containing a {@link List} of {@link Tag}s
     */
    public LiveData<List<Tag>> getFeaturedTagsSuggestions(@NonNull String instance, String token) {
        tagListMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            List<Tag> tagList = null;
            Call<List<Tag>> featuredTagsSuggestionsCall = mastodonAccountsService.getFeaturedTagsSuggestions(token);
            if (featuredTagsSuggestionsCall != null) {
                try {
                    Response<List<Tag>> featuredTagsSuggestionsResponse = featuredTagsSuggestionsCall.execute();
                    if (featuredTagsSuggestionsResponse.isSuccessful()) {
                        tagList = featuredTagsSuggestionsResponse.body();
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
     * Preferences defined by the user in their account settings.
     *
     * @return {@link LiveData} containing {@link Preferences}
     */
    public LiveData<Preferences> getPreferences(@NonNull String instance, String token) {
        preferencesMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Preferences preferences = null;
            Call<Preferences> preferencesCall = mastodonAccountsService.getPreferences(token);
            if (preferencesCall != null) {
                try {
                    Response<Preferences> preferencesResponse = preferencesCall.execute();
                    if (preferencesResponse.isSuccessful()) {
                        preferences = preferencesResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Preferences finalPreferences = preferences;
            Runnable myRunnable = () -> preferencesMutableLiveData.setValue(finalPreferences);
            mainHandler.post(myRunnable);
        }).start();
        return preferencesMutableLiveData;
    }

    /**
     * Accounts the user has had past positive interactions with, but is not yet following.
     *
     * @param limit Maximum number of results to return. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Suggestion}s
     */
    public LiveData<Suggestions> getSuggestions(@NonNull String instance, String token, String limit) {
        suggestionsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = initv2(instance);
        new Thread(() -> {
            Call<List<Suggestion>> suggestionsCall = mastodonAccountsService.getSuggestions(token, limit);
            Suggestions suggestions = new Suggestions();
            if (suggestionsCall != null) {
                try {
                    Response<List<Suggestion>> suggestionsResponse = suggestionsCall.execute();
                    if (suggestionsResponse.isSuccessful()) {
                        suggestions.pagination = MastodonHelper.getOffSetPagination(suggestionsResponse.headers());
                        suggestions.suggestions = suggestionsResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> suggestionsMutableLiveData.setValue(suggestions);
            mainHandler.post(myRunnable);
        }).start();
        return suggestionsMutableLiveData;
    }

    /**
     * List accounts visible in the directory.
     *
     * @param limit Maximum number of results to return. Defaults to 40.
     * @return {@link LiveData} containing a {@link List} of {@link Account}s
     */
    public LiveData<Accounts> getDirectory(@NonNull String instance, String token, Integer offset, Integer limit, String order, Boolean local) {
        accountsMutableLiveData = new MutableLiveData<>();
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Call<List<Account>> accountsCall = mastodonAccountsService.getDirectory(token, offset, limit, order, local);
            Accounts accounts = new Accounts();

            if (accountsCall != null) {
                try {
                    Response<List<Account>> directoryResponse = accountsCall.execute();
                    if (directoryResponse.isSuccessful()) {
                        accounts.pagination = MastodonHelper.getOffSetPagination(directoryResponse.headers());
                        accounts.accounts = directoryResponse.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> accountsMutableLiveData.setValue(accounts);
            mainHandler.post(myRunnable);
        }).start();
        return accountsMutableLiveData;
    }

    /**
     * Remove an account from follow suggestions.
     *
     * @param accountId id of the account in the database to be removed from suggestions
     */
    public void removeSuggestion(@NonNull String instance, String token, String accountId) {
        MastodonAccountsService mastodonAccountsService = init(instance);
        new Thread(() -> {
            Call<Void> removeSuggestionCall = mastodonAccountsService.removeSuggestion(token, accountId);
            if (removeSuggestionCall != null) {
                try {
                    removeSuggestionCall.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public enum UpdateMediaType {
        AVATAR,
        HEADER
    }
}
