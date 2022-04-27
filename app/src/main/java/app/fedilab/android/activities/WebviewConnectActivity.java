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


import static app.fedilab.android.BaseMainActivity.api;
import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.software;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.databinding.ActivityWebviewConnectBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.OauthVM;
import es.dmoral.toasty.Toasty;


public class WebviewConnectActivity extends BaseActivity {


    private ActivityWebviewConnectBinding binding;
    private AlertDialog alert;
    private String login_url;

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
        }
        if (login_url == null)
            finish();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            View view = inflater.inflate(R.layout.simple_bar, new LinearLayout(WebviewConnectActivity.this), false);
            view.setBackground(new ColorDrawable(ContextCompat.getColor(WebviewConnectActivity.this, R.color.cyanea_primary)));
            actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ImageView toolbar_close = actionBar.getCustomView().findViewById(R.id.toolbar_close);
            TextView toolbar_title = actionBar.getCustomView().findViewById(R.id.toolbar_title);
            toolbar_close.setOnClickListener(v -> finish());
            toolbar_title.setText(R.string.add_account);
        }

        clearCookies(WebviewConnectActivity.this);
        binding.webviewConnect.getSettings().setJavaScriptEnabled(true);
        String user_agent = sharedpreferences.getString(getString(R.string.SET_CUSTOM_USER_AGENT), Helper.USER_AGENT);
        binding.webviewConnect.getSettings().setUserAgentString(user_agent);
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
                    String[] val = url.split("code=");
                    if (val.length < 2) {
                        Toasty.error(WebviewConnectActivity.this, getString(R.string.toast_code_error), Toast.LENGTH_LONG).show();
                        Intent myIntent = new Intent(WebviewConnectActivity.this, LoginActivity.class);
                        startActivity(myIntent);
                        finish();
                        return false;
                    }
                    String code = val[1];
                    OauthVM oauthVM = new ViewModelProvider(WebviewConnectActivity.this).get(OauthVM.class);
                    //API call to get the user token
                    oauthVM.createToken(currentInstance, "authorization_code", BaseMainActivity.client_id, BaseMainActivity.client_secret, Helper.REDIRECT_CONTENT_WEB, Helper.OAUTH_SCOPES, code)
                            .observe(WebviewConnectActivity.this, tokenObj -> {
                                Account account = new Account();
                                account.client_id = BaseMainActivity.client_id;
                                account.client_secret = BaseMainActivity.client_secret;
                                account.token = tokenObj.token_type + " " + tokenObj.access_token;
                                account.api = api;
                                account.software = software;
                                account.instance = currentInstance;
                                //API call to retrieve account information for the new token
                                AccountsVM accountsVM = new ViewModelProvider(WebviewConnectActivity.this).get(AccountsVM.class);
                                accountsVM.getConnectedAccount(currentInstance, account.token).observe(WebviewConnectActivity.this, mastodonAccount -> {
                                    account.mastodon_account = mastodonAccount;
                                    account.user_id = mastodonAccount.id;
                                    new Thread(() -> {
                                        try {
                                            //update the database
                                            new Account(WebviewConnectActivity.this).insertOrUpdate(account);
                                            Handler mainHandler = new Handler(Looper.getMainLooper());
                                            BaseMainActivity.currentToken = account.token;
                                            BaseMainActivity.currentUserID = account.user_id;
                                            api = Account.API.MASTODON;
                                            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(WebviewConnectActivity.this);
                                            SharedPreferences.Editor editor = sharedpreferences.edit();
                                            editor.putString(PREF_USER_TOKEN, account.token);
                                            editor.commit();
                                            //The user is now authenticated, it will be redirected to MainActivity
                                            Runnable myRunnable = () -> {
                                                Intent mainActivity = new Intent(WebviewConnectActivity.this, BaseMainActivity.class);
                                                startActivity(mainActivity);
                                                finish();
                                            };
                                            mainHandler.post(myRunnable);
                                        } catch (DBException e) {
                                            e.printStackTrace();
                                        }
                                    }).start();
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