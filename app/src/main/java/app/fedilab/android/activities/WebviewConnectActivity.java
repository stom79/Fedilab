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

package app.fedilab.android.activities;


import static app.fedilab.android.activities.LoginActivity.apiLogin;
import static app.fedilab.android.activities.LoginActivity.client_idLogin;
import static app.fedilab.android.activities.LoginActivity.client_secretLogin;
import static app.fedilab.android.activities.LoginActivity.currentInstanceLogin;
import static app.fedilab.android.activities.LoginActivity.softwareLogin;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.regex.Matcher;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.databinding.ActivityWebviewConnectBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.AdminVM;
import app.fedilab.android.viewmodel.mastodon.OauthVM;
import es.dmoral.toasty.Toasty;


public class WebviewConnectActivity extends BaseActivity {


    private ActivityWebviewConnectBinding binding;
    private AlertDialog alert;
    private String login_url;
    private boolean requestedAdmin;

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void proceedLogin(Activity activity, Account account) {
        new Thread(() -> {
            try {
                //update the database
                new Account(activity).insertOrUpdate(account);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                BaseMainActivity.currentToken = account.token;
                BaseMainActivity.currentUserID = account.user_id;
                BaseMainActivity.api = Account.API.MASTODON;
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(PREF_USER_TOKEN, account.token);
                editor.commit();
                //The user is now authenticated, it will be redirected to MainActivity
                Runnable myRunnable = () -> {
                    Intent mainActivity = new Intent(activity, MainActivity.class);
                    activity.startActivity(mainActivity);
                    activity.finish();
                };
                mainHandler.post(myRunnable);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(WebviewConnectActivity.this);

        binding = ActivityWebviewConnectBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            login_url = b.getString("login_url");
            requestedAdmin = b.getBoolean("requestedAdmin", false);
        }
        if (login_url == null)
            finish();

        clearCookies(WebviewConnectActivity.this);
        binding.webviewConnect.getSettings().setJavaScriptEnabled(true);
        String user_agent = sharedpreferences.getString(getString(R.string.SET_CUSTOM_USER_AGENT), Helper.USER_AGENT);
        binding.webviewConnect.getSettings().setUserAgentString(user_agent);
        binding.webviewConnect.getSettings().setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webviewConnect, true);


        final ProgressBar pbar = findViewById(R.id.progress_bar);
        binding.webviewConnect.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100 && pbar.getVisibility() == ProgressBar.GONE) {
                    pbar.setVisibility(ProgressBar.VISIBLE);
                }
                pbar.setProgress(progress);
                if (progress == 100) {
                    pbar.setVisibility(ProgressBar.GONE);
                }
            }
        });

        binding.webviewConnect.setWebViewClient(new WebViewClient() {

           /* @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String x_xsrf_token = null;
                String x_csrf_token = null;
                if (request.getUrl().toString().contains("accounts/verify_credentials")) {

                    String cookies = CookieManager.getInstance().getCookie(request.getUrl().toString());

                    Map<String, String> requestHeaders = request.getRequestHeaders();
                    Iterator<Map.Entry<String, String>> it = requestHeaders.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> pair = it.next();
                        if (pair.getKey().compareTo("X-XSRF-TOKEN") == 0) {
                            x_xsrf_token = pair.getValue();
                        }
                        if (pair.getKey().compareTo("X-CSRF-TOKEN") == 0) {
                            x_csrf_token = pair.getValue();
                        }
                        it.remove();
                    }
                    if (x_xsrf_token != null && x_csrf_token != null) {
                        String finalX_xsrf_token = x_xsrf_token;
                        String finalX_csrf_token = x_csrf_token;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            view.stopLoading();
                            SharedPreferences sharedpreferences1 = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedpreferences1.edit();
                            String token = "X-XSRF-TOKEN= " + finalX_xsrf_token + ";X-CSRF-TOKEN= " + finalX_csrf_token + "|" + cookies;
                            editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token);
                            editor.commit();
                            view.setVisibility(View.GONE);
                            //Update the account with the token;
                            new UpdateAccountInfoAsyncTask(WebviewConnectActivity.this, token, clientId, clientSecret, null, instance, social);
                        });
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }*/


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                super.shouldOverrideUrlLoading(view, url);
                if (url.contains(Helper.REDIRECT_CONTENT_WEB)) {
                    Matcher matcher = Helper.codePattern.matcher(url);
                    if (!matcher.find()) {
                        return false;
                    }
                    String code = matcher.group(1);
                    OauthVM oauthVM = new ViewModelProvider(WebviewConnectActivity.this).get(OauthVM.class);
                    //API call to get the user token
                    String scope = requestedAdmin ? Helper.OAUTH_SCOPES_ADMIN : Helper.OAUTH_SCOPES;
                    oauthVM.createToken(currentInstanceLogin, "authorization_code", client_idLogin, client_secretLogin, Helper.REDIRECT_CONTENT_WEB, scope, code)
                            .observe(WebviewConnectActivity.this, tokenObj -> {
                                Account account = new Account();
                                account.client_id = client_idLogin;
                                account.client_secret = client_secretLogin;
                                account.token = tokenObj.token_type + " " + tokenObj.access_token;
                                account.api = apiLogin;
                                account.software = softwareLogin;
                                account.instance = currentInstanceLogin;
                                //API call to retrieve account information for the new token
                                AccountsVM accountsVM = new ViewModelProvider(WebviewConnectActivity.this).get(AccountsVM.class);
                                accountsVM.getConnectedAccount(currentInstanceLogin, account.token).observe(WebviewConnectActivity.this, mastodonAccount -> {
                                    if (mastodonAccount != null) {
                                        account.mastodon_account = mastodonAccount;
                                        account.user_id = mastodonAccount.id;
                                        //We check if user have really moderator rights
                                        if (requestedAdmin) {
                                            AdminVM adminVM = new ViewModelProvider(WebviewConnectActivity.this).get(AdminVM.class);
                                            adminVM.getAccount(account.instance, account.token, account.user_id).observe(WebviewConnectActivity.this, adminAccount -> {
                                                account.admin = adminAccount != null;
                                                proceedLogin(WebviewConnectActivity.this, account);
                                            });
                                        } else {
                                            proceedLogin(WebviewConnectActivity.this, account);
                                        }
                                    } else {
                                        Toasty.error(WebviewConnectActivity.this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                    }

                                });
                            });
                    return true;
                } else {
                    return false;
                }
            }
        });

        binding.webviewConnect.loadUrl(login_url);
    }

    @Override
    public void onBackPressed() {
        if (binding.webviewConnect.canGoBack()) {
            binding.webviewConnect.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (alert != null) {
            alert.dismiss();
            alert = null;
        }
        binding.webviewConnect.destroy();
    }
}