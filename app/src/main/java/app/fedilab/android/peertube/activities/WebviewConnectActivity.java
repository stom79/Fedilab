package app.fedilab.android.peertube.activities;
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

import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.updateCredential;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;

import java.net.URL;
import java.util.regex.Matcher;

import app.fedilab.android.peertube.BuildConfig;
import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.Theme;


public class WebviewConnectActivity extends BaseActivity {


    private WebView webView;
    private AlertDialog alert;
    private String clientId, clientSecret;
    private String url;

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
        Theme.setTheme(this, HelperInstance.getLiveInstance(this), false);
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        WebView.setWebContentsDebuggingEnabled(true);
        setContentView(R.layout.activity_webview_connect);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            url = b.getString("url");
        }
        if (url == null)
            finish();

        clientId = sharedpreferences.getString(Helper.CLIENT_ID, null);
        clientSecret = sharedpreferences.getString(Helper.CLIENT_SECRET, null);

        webView = findViewById(R.id.webviewConnect);
        clearCookies(WebviewConnectActivity.this);
        webView.getSettings().setJavaScriptEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(false);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.login);

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

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                //Avoid to load first page for academic  instances & openid
                if (!BuildConfig.full_instances && url.contains("/client")) {
                    view.stopLoading();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    Matcher matcher = Helper.redirectPattern.matcher(url);
                    if (matcher.find()) {
                        String externalAuthToken = matcher.group(1);
                        String username = matcher.group(2);
                        new Thread(() -> {
                            try {
                                OauthParams oauthParams = new OauthParams();
                                oauthParams.setClient_id(sharedpreferences.getString(Helper.CLIENT_ID, null));
                                oauthParams.setClient_secret(sharedpreferences.getString(Helper.CLIENT_SECRET, null));
                                oauthParams.setGrant_type("password");
                                oauthParams.setScope("upload");
                                oauthParams.setResponse_type("code");
                                oauthParams.setUsername(username);
                                oauthParams.setExternalAuthToken(externalAuthToken);
                                oauthParams.setPassword(externalAuthToken);
                                String instance = new URL(url).getHost();
                                Token token = null;
                                try {
                                    token = new RetrofitPeertubeAPI(WebviewConnectActivity.this, instance, null).manageToken(oauthParams);
                                } catch (Error error) {
                                    error.printStackTrace();
                                    Error.displayError(WebviewConnectActivity.this, error);
                                }
                                if (token != null) {
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token.getAccess_token());
                                    editor.putString(Helper.PREF_SOFTWARE, null);
                                    editor.putString(Helper.PREF_REMOTE_INSTANCE, null);
                                    editor.putString(Helper.PREF_INSTANCE, instance);
                                    editor.apply();
                                    updateCredential(WebviewConnectActivity.this, token.getAccess_token(), clientId, clientSecret, token.getRefresh_token(), new URL(url).getHost(), null);
                                    finish();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                        return true;
                    }
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

           /* @Override
            public void onPageFinished(WebView view, String url) {
                Matcher matcher = Helper.redirectPattern.matcher(url);
                if (matcher.find()) {
                    String externalAuthToken = matcher.group(1);
                    String username = matcher.group(2);
                    new Thread(() -> {
                        try {
                            OauthParams oauthParams = new OauthParams();
                            oauthParams.setClient_id(sharedpreferences.getString(Helper.CLIENT_ID, null));
                            oauthParams.setClient_secret(sharedpreferences.getString(Helper.CLIENT_SECRET, null));
                            oauthParams.setGrant_type("password");
                            oauthParams.setScope("upload");
                            oauthParams.setResponse_type("code");
                            oauthParams.setUsername(username);
                            oauthParams.setExternalAuthToken(externalAuthToken);
                            oauthParams.setPassword(externalAuthToken);
                            String instance = new URL(url).getHost();
                            Token token = null;
                            try {
                                token = new RetrofitPeertubeAPI(WebviewConnectActivity.this, instance, null).manageToken(oauthParams);
                            } catch (Error error) {
                                error.printStackTrace();
                                Error.displayError(WebviewConnectActivity.this, error);
                            }
                            if (token != null) {
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token.getAccess_token());
                                editor.putString(Helper.PREF_SOFTWARE, null);
                                editor.putString(Helper.PREF_REMOTE_INSTANCE, null);
                                editor.putString(Helper.PREF_INSTANCE, instance);
                                editor.apply();
                                updateCredential(WebviewConnectActivity.this, token.getAccess_token(), clientId, clientSecret, token.getRefresh_token(), new URL(url).getHost(), null);
                                finish();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                super.onPageFinished(view, url);
            }*/
        });
        webView.loadUrl(url);

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