package app.fedilab.android.peertube.client.mastodon;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.MainActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.sqlite.MastodonAccountDAO;
import app.fedilab.android.peertube.sqlite.Sqlite;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitMastodonAPI {

    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();
    private final String finalUrl;
    private final String finalUrl2;
    private final Context _context;
    private String instance;
    private String token;

    public RetrofitMastodonAPI(Context context) {
        _context = context;
        SharedPreferences sharedpreferences = _context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        this.instance = sharedpreferences.getString(Helper.PREF_REMOTE_INSTANCE, null);
        this.token = sharedpreferences.getString(Helper.PREF_KEY_OAUTH_TOKEN, null);
        finalUrl = "https://" + this.instance + "/api/v1/";
        finalUrl2 = "https://" + this.instance + "/api/v2/";
    }


    public RetrofitMastodonAPI(Context context, String instance, String token) {
        _context = context;
        this.instance = instance;
        this.token = token;
        finalUrl = "https://" + instance + "/api/v1/";
        finalUrl2 = "https://" + this.instance + "/api/v2/";
    }

    public Status search(String url) throws Error {
        MastodonService mastodonService2 = init2();
        Call<Results> statusCall = mastodonService2.searchMessage(getToken(), url);
        Response<Results> response;
        try {
            response = statusCall.execute();
            if (response.isSuccessful() && response.body() != null && response.body().getStatuses() != null && response.body().getStatuses().size() > 0) {
                return response.body().getStatuses().get(0);
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
                throw error;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateCredential(Activity activity, String client_id, String client_secret, String refresh_token, String software) {
        new Thread(() -> {
            MastodonAccount.Account account;
            try {
                account = new RetrofitMastodonAPI(activity, instance, token).verifyCredentials();
            } catch (Error error) {
                Error.displayError(activity, error);
                error.printStackTrace();
                return;
            }
            try {
                //At the state the instance can be encoded
                instance = URLDecoder.decode(instance, "utf-8");
            } catch (UnsupportedEncodingException ignored) {
            }
            SharedPreferences sharedpreferences = activity.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
            account.setToken(token);
            account.setClient_id(client_id);
            account.setClient_secret(client_secret);
            account.setRefresh_token(refresh_token);
            account.setHost(instance);
            account.setSoftware(software);
            SQLiteDatabase db = Sqlite.getInstance(activity.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
            boolean userExists = new MastodonAccountDAO(activity, db).userExist(account);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(Helper.PREF_KEY_ID, account.getId());
            editor.putString(Helper.PREF_KEY_NAME, account.getUsername());
            if (token != null) {
                editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token);
            }
            editor.putString(Helper.PREF_REMOTE_INSTANCE, account.getHost());
            editor.putString(Helper.PREF_SOFTWARE, software);
            editor.apply();
            if (userExists) {
                new MastodonAccountDAO(activity, db).updateAccountCredential(account);
            } else {
                if (account.getUsername() != null && account.getCreatedAt() != null) {
                    new MastodonAccountDAO(activity, db).insertAccount(account);
                }
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                Intent mainActivity = new Intent(activity, MainActivity.class);
                mainActivity.putExtra(Helper.INTENT_ACTION, Helper.ADD_USER_INTENT);
                activity.startActivity(mainActivity);
                activity.finish();
            };
            mainHandler.post(myRunnable);
        }).start();
    }


    private MastodonService init_no_api() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + instance)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        SharedPreferences sharedpreferences = _context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        if (token == null) {
            token = sharedpreferences.getString(Helper.PREF_KEY_OAUTH_TOKEN, null);
        }
        return retrofit.create(MastodonService.class);
    }

    private MastodonService init() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(finalUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        SharedPreferences sharedpreferences = _context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        if (token == null) {
            token = sharedpreferences.getString(Helper.PREF_KEY_OAUTH_TOKEN, null);
        }
        return retrofit.create(MastodonService.class);
    }

    private MastodonService init2() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(finalUrl2)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        SharedPreferences sharedpreferences = _context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        if (token == null) {
            token = sharedpreferences.getString(Helper.PREF_KEY_OAUTH_TOKEN, null);
        }
        return retrofit.create(MastodonService.class);
    }

    /**
     * Get Oauth
     *
     * @return APIResponse
     */
    public Oauth oauthClient(String client_name, String redirect_uris, String scopes, String website) {
        MastodonService mastodonService = init();
        try {
            Call<Oauth> oauth;
            oauth = mastodonService.getOauth(client_name, redirect_uris, scopes, website);
            Response<Oauth> response = oauth.execute();

            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * Verifiy credential of the authenticated user *synchronously*
     * @return Account
     */
    public MastodonAccount.Account verifyCredentials() throws Error {
        MastodonService mastodonService = init();
        Call<MastodonAccount.Account> accountCall = mastodonService.verifyCredentials("Bearer " + token);
        APIResponse apiResponse = new APIResponse();
        try {
            Response<MastodonAccount.Account> response = accountCall.execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                Error error = new Error();
                error.setStatusCode(response.code());
                if (response.errorBody() != null) {
                    error.setError(response.errorBody().string());
                } else {
                    error.setError(_context.getString(R.string.toast_error));
                }
                throw error;
            }
        } catch (IOException e) {
            Error error = new Error();
            error.setError(_context.getString(R.string.toast_error));
            apiResponse.setError(error);
            e.printStackTrace();
        }
        return null;
    }

    /***
     * Verifiy credential of the authenticated user *synchronously*
     * @return Account
     */
    public Token manageToken(OauthParams oauthParams) throws Error {
        MastodonService mastodonService = init_no_api();
        Call<Token> createToken = mastodonService.createToken(
                oauthParams.getGrant_type(),
                oauthParams.getClient_id(),
                oauthParams.getClient_secret(),
                oauthParams.getRedirect_uri(),
                oauthParams.getCode()
        );
        if (createToken != null) {
            try {
                Response<Token> response = createToken.execute();
                if (response.isSuccessful()) {
                    return response.body();
                } else {
                    Error error = new Error();
                    error.setStatusCode(response.code());
                    if (response.errorBody() != null) {
                        error.setError(response.errorBody().string());
                    } else {
                        error.setError(_context.getString(R.string.toast_error));
                    }
                    throw error;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Status commentAction(String url, String content) throws Error {
        MastodonService mastodonService = init();
        Status status = search(url);
        if (status != null) {
            Call<Status> postReplyCall = mastodonService.postReply(getToken(), status.getId(), content, null);
            try {
                Response<Status> responsePost = postReplyCall.execute();
                if (responsePost.isSuccessful()) {
                    Status statusReturned = responsePost.body();
                    if (statusReturned != null && statusReturned.getAccount() != null) {
                        statusReturned.getAccount().setHost(instance);
                    }
                    return statusReturned;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Status postAction(actionType type, Status status) {
        MastodonService mastodonService = init();
        Call<Status> postAction = null;
        if (status != null) {
            switch (type) {
                case BOOST:
                    postAction = mastodonService.boost(getToken(), status.getId());
                    break;
                case UNBOOST:
                    postAction = mastodonService.unBoost(getToken(), status.getId());
                    break;
                case FAVOURITE:
                    postAction = mastodonService.favourite(getToken(), status.getId());
                    break;
                case UNFAVOURITE:
                    postAction = mastodonService.unfavourite(getToken(), status.getId());
                    break;
                case BOOKMARK:
                    postAction = mastodonService.bookmark(getToken(), status.getId());
                    break;
                case UNBOOKMARK:
                    postAction = mastodonService.unbookmark(getToken(), status.getId());
                    break;
            }
            try {
                if (postAction != null) {
                    Response<Status> responsePost = postAction.execute();
                    if (responsePost.isSuccessful()) {
                        Status statusReturned = responsePost.body();
                        if (statusReturned != null && statusReturned.getAccount() != null) {
                            statusReturned.getAccount().setHost(instance);
                        }
                        return statusReturned;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String getToken() {
        if (token != null) {
            return "Bearer " + token;
        } else {
            return null;
        }
    }

    public enum actionType {
        BOOST,
        UNBOOST,
        FAVOURITE,
        UNFAVOURITE,
        BOOKMARK,
        UNBOOKMARK
    }
}
