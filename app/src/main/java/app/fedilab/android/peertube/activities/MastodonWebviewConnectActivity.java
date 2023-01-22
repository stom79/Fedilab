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

package app.fedilab.android.peertube.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.mastodon.RetrofitMastodonAPI;
import app.fedilab.android.peertube.helper.Helper;
import es.dmoral.toasty.Toasty;


public class MastodonWebviewConnectActivity extends BaseBarActivity {


    private WebView webView;
    private AlertDialog alert;
    private String clientId, clientSecret;
    private String instance, software;

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

    private static String redirectUserToAuthorizeAndLogin(String clientId, String instance) {
        String queryString = Helper.CLIENT_ID + "=" + clientId;
        queryString += "&" + Helper.REDIRECT_URI + "=" + Uri.encode(Helper.REDIRECT_CONTENT_WEB);
        queryString += "&response_type=code";
        queryString += "&scope=read write follow";
        return "https://" + instance + "/oauth/authorize?" + queryString;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_connect);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            instance = b.getString("instance");
            clientId = b.getString("client_id");
            clientSecret = b.getString("client_secret");
            software = b.getString("software");
        }
        if (instance == null)
            finish();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.login);
        webView = findViewById(R.id.webviewConnect);
        clearCookies(MastodonWebviewConnectActivity.this);
        webView.getSettings().setJavaScriptEnabled(true);

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        final ProgressBar pbar = findViewById(R.id.progress_bar);
        webView.setWebChromeClient(new WebChromeClient() {
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

        if (instance == null) {
            finish();
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                super.shouldOverrideUrlLoading(view, url);
                if (url.contains(Helper.REDIRECT_CONTENT_WEB)) {
                    String[] val = url.split("code=");
                    if (val.length < 2) {
                        Toasty.error(MastodonWebviewConnectActivity.this, getString(R.string.toast_code_error), Toast.LENGTH_LONG).show();
                        Intent myIntent = new Intent(MastodonWebviewConnectActivity.this, LoginActivity.class);
                        startActivity(myIntent);
                        finish();
                        return false;
                    }
                    String code = val[1];
                    if (code.contains("&")) {
                        code = code.split("&")[0];
                    }
                    OauthParams oauthParams = new OauthParams();
                    oauthParams.setClient_id(clientId);
                    oauthParams.setClient_secret(clientSecret);
                    oauthParams.setGrant_type("authorization_code");
                    oauthParams.setCode(code);
                    oauthParams.setRedirect_uri(Helper.REDIRECT_CONTENT_WEB);

                    new Thread(() -> {
                        try {
                            Token token = new RetrofitMastodonAPI(MastodonWebviewConnectActivity.this, instance, null).manageToken(oauthParams);
                            SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token.getAccess_token());
                            editor.apply();
                            new RetrofitMastodonAPI(MastodonWebviewConnectActivity.this, instance, token.getAccess_token()).updateCredential(MastodonWebviewConnectActivity.this, clientId, clientSecret, token.getRefresh_token(), software);
                        } catch (Exception | Error ignored) {
                        }
                    }).start();
                    return true;
                }
                return false;
            }

        });
        webView.loadUrl(redirectUserToAuthorizeAndLogin(clientId, instance));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
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
        if (webView != null) {
            webView.destroy();
        }
    }
}