package app.fedilab.android.mastodon.viewmodel.mastodon;
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

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.mastodon.client.endpoints.MastodonCollectionsService;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Collection;
import app.fedilab.android.mastodon.helper.Helper;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CollectionsVM extends AndroidViewModel {

    final OkHttpClient okHttpClient = Helper.myOkHttpClient(getApplication().getApplicationContext());

    private MutableLiveData<List<Collection>> collectionListMutableLiveData;
    private MutableLiveData<Collection> collectionMutableLiveData;
    private MutableLiveData<Collection.CollectionWithAccounts> collectionWithAccountsMutableLiveData;
    private MutableLiveData<Collection.CollectionItem> collectionItemMutableLiveData;

    public CollectionsVM(@NonNull Application application) {
        super(application);
    }

    private MastodonCollectionsService init(String instance) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonCollectionsService.class);
    }

    public LiveData<List<Collection>> getAccountCollections(@NonNull String instance, String token, @NonNull String accountId) {
        collectionListMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            List<Collection> collections = null;
            Call<Collection.CollectionList> call = service.getAccountCollections(token, accountId, null, null);
            if (call != null) {
                try {
                    Response<Collection.CollectionList> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        collections = response.body().collections;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (collections != null) {
                for (Collection collection : collections) {
                    try {
                        Call<Collection.CollectionWithAccounts> detailCall = service.getCollection(token, collection.id);
                        if (detailCall != null) {
                            Response<Collection.CollectionWithAccounts> detailResponse = detailCall.execute();
                            if (detailResponse.isSuccessful() && detailResponse.body() != null && detailResponse.body().accounts != null) {
                                List<Account> accounts = new ArrayList<>();
                                for (Account account : detailResponse.body().accounts) {
                                    if (account.id != null && !account.id.equals(collection.account_id)) {
                                        accounts.add(account);
                                    }
                                }
                                collection.previewAccounts = accounts.size() > 4 ? accounts.subList(0, 4) : accounts;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            List<Collection> finalCollections = collections;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionListMutableLiveData.setValue(finalCollections));
        }).start();
        return collectionListMutableLiveData;
    }

    public LiveData<Collection.CollectionWithAccounts> getCollection(@NonNull String instance, String token, @NonNull String collectionId) {
        collectionWithAccountsMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Collection.CollectionWithAccounts result = null;
            Call<Collection.CollectionWithAccounts> call = service.getCollection(token, collectionId);
            if (call != null) {
                try {
                    Response<Collection.CollectionWithAccounts> response = call.execute();
                    if (response.isSuccessful()) {
                        result = response.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Collection.CollectionWithAccounts finalResult = result;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionWithAccountsMutableLiveData.setValue(finalResult));
        }).start();
        return collectionWithAccountsMutableLiveData;
    }

    public LiveData<Collection> createCollection(@NonNull String instance, String token,
                                                  @NonNull String name, String description,
                                                  String language, String tagName,
                                                  Boolean sensitive, Boolean discoverable) {
        collectionMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Collection collection = null;
            Call<Collection.WrappedCollection> call = service.createCollection(token, name, description, language, tagName, sensitive, discoverable);
            if (call != null) {
                try {
                    Response<Collection.WrappedCollection> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        collection = response.body().collection;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Collection finalCollection = collection;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionMutableLiveData.setValue(finalCollection));
        }).start();
        return collectionMutableLiveData;
    }

    public LiveData<Collection> updateCollection(@NonNull String instance, String token,
                                                  @NonNull String collectionId,
                                                  String name, String description,
                                                  String language, String tagName,
                                                  Boolean sensitive, Boolean discoverable) {
        collectionMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Collection collection = null;
            Call<Collection.WrappedCollection> call = service.updateCollection(token, collectionId, name, description, language, tagName, sensitive, discoverable);
            if (call != null) {
                try {
                    Response<Collection.WrappedCollection> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        collection = response.body().collection;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Collection finalCollection = collection;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionMutableLiveData.setValue(finalCollection));
        }).start();
        return collectionMutableLiveData;
    }

    public void deleteCollection(@NonNull String instance, String token, @NonNull String collectionId) {
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Call<Void> call = service.deleteCollection(token, collectionId);
            if (call != null) {
                try {
                    call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public LiveData<Collection.CollectionItem> addAccountToCollection(@NonNull String instance, String token,
                                                                       @NonNull String collectionId, @NonNull String accountId) {
        collectionItemMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Collection.CollectionItem item = null;
            Call<Collection.WrappedCollectionItem> call = service.addAccountToCollection(token, collectionId, accountId);
            if (call != null) {
                try {
                    Response<Collection.WrappedCollectionItem> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        item = response.body().collection_item;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Collection.CollectionItem finalItem = item;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionItemMutableLiveData.setValue(finalItem));
        }).start();
        return collectionItemMutableLiveData;
    }

    public void removeAccountFromCollection(@NonNull String instance, String token,
                                             @NonNull String collectionId, @NonNull String itemId) {
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Call<Void> call = service.removeAccountFromCollection(token, collectionId, itemId);
            if (call != null) {
                try {
                    call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void revokeCollectionItem(@NonNull String instance, String token,
                                      @NonNull String collectionId, @NonNull String itemId) {
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            Call<Void> call = service.revokeCollectionItem(token, collectionId, itemId);
            if (call != null) {
                try {
                    call.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public LiveData<Boolean> revokeCurrentUserFromCollection(@NonNull String instance, String token,
                                                              @NonNull String collectionId, @NonNull String currentAccountId) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            boolean success = false;
            try {
                Call<Collection.CollectionWithAccounts> call = service.getCollection(token, collectionId);
                if (call != null) {
                    Response<Collection.CollectionWithAccounts> response = call.execute();
                    if (response.isSuccessful() && response.body() != null && response.body().collection != null
                            && response.body().collection.items != null) {
                        for (Collection.CollectionItem item : response.body().collection.items) {
                            if (currentAccountId.equals(item.account_id)) {
                                Call<Void> revokeCall = service.revokeCollectionItem(token, collectionId, item.id);
                                if (revokeCall != null) {
                                    Response<Void> revokeResponse = revokeCall.execute();
                                    success = revokeResponse.isSuccessful();
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            boolean finalSuccess = success;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> resultLiveData.setValue(finalSuccess));
        }).start();
        return resultLiveData;
    }

    public LiveData<List<Collection>> getAccountInCollections(@NonNull String instance, String token, @NonNull String accountId) {
        collectionListMutableLiveData = new MutableLiveData<>();
        MastodonCollectionsService service = init(instance);
        new Thread(() -> {
            List<Collection> collections = null;
            Call<Collection.CollectionList> call = service.getAccountInCollections(token, accountId, null, null);
            if (call != null) {
                try {
                    Response<Collection.CollectionList> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                        collections = response.body().collections;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (collections != null) {
                for (Collection collection : collections) {
                    try {
                        Call<Collection.CollectionWithAccounts> detailCall = service.getCollection(token, collection.id);
                        if (detailCall != null) {
                            Response<Collection.CollectionWithAccounts> detailResponse = detailCall.execute();
                            if (detailResponse.isSuccessful() && detailResponse.body() != null && detailResponse.body().accounts != null) {
                                List<Account> previewAccounts = new ArrayList<>();
                                for (Account account : detailResponse.body().accounts) {
                                    if (account.id != null && account.id.equals(collection.account_id)) {
                                        collection.ownerAccount = account;
                                    } else {
                                        previewAccounts.add(account);
                                    }
                                }
                                collection.previewAccounts = previewAccounts.size() > 4 ? previewAccounts.subList(0, 4) : previewAccounts;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            List<Collection> finalCollections = collections;
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> collectionListMutableLiveData.setValue(finalCollections));
        }).start();
        return collectionListMutableLiveData;
    }
}
