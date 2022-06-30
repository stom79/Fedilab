package app.fedilab.android.webview;
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


import static app.fedilab.android.client.entities.app.DomainsBlock.trackingDomains;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.WebviewActivity;


public class FedilabWebViewClient extends WebViewClient {

    private final Activity activity;
    public List<String> domains = new ArrayList<>();
    private int count = 0;

    public FedilabWebViewClient(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
    }


    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
        if (trackingDomains != null) {
            URI uri;
            try {
                uri = new URI(url);
                String domain = uri.getHost();
                if (domain != null) {
                    domain = domain.startsWith("www.") ? domain.substring(4) : domain;
                }
                if (domain != null && trackingDomains.contains(domain)) {
                    if (activity instanceof WebviewActivity) {
                        count++;
                        domains.add(url);
                        ((WebviewActivity) activity).setCount(activity, String.valueOf(count));
                    }
                    ByteArrayInputStream nothing = new ByteArrayInputStream("".getBytes());
                    return new WebResourceResponse("text/plain", "utf-8", nothing);
                }
            } catch (URISyntaxException e) {
                try {
                    if (url.length() > 50) {
                        url = url.substring(0, 50);
                    }
                    uri = new URI(url);
                    String domain = uri.getHost();
                    if (domain != null) {
                        domain = domain.startsWith("www.") ? domain.substring(4) : domain;
                    }
                    if (domain != null && trackingDomains.contains(domain)) {
                        if (activity instanceof WebviewActivity) {
                            count++;
                            domains.add(url);
                            ((WebviewActivity) activity).setCount(activity, String.valueOf(count));
                        }
                        ByteArrayInputStream nothing = new ByteArrayInputStream("".getBytes());
                        return new WebResourceResponse("text/plain", "utf-8", nothing);

                    }
                } catch (URISyntaxException ignored) {
                }
            }
        }
        return super.shouldInterceptRequest(view, url);
    }

    public List<String> getDomains() {
        return this.domains;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if (view.getUrl() != null && view.getUrl().endsWith(".onion")) {
            handler.proceed();
        } else {
            super.onReceivedSslError(view, handler, error);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (URLUtil.isNetworkUrl(url)) {
            return false;
        } else {
            view.stopLoading();
            view.goBack();
        }
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        count = 0;
        domains = new ArrayList<>();
        domains.clear();
        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        LayoutInflater mInflater = LayoutInflater.from(activity);
        if (actionBar != null) {
            View webview_actionbar = mInflater.inflate(R.layout.webview_actionbar, new LinearLayout(activity), false);
            TextView webview_title = webview_actionbar.findViewById(R.id.webview_title);
            webview_title.setText(url);
            actionBar.setCustomView(webview_actionbar);
            actionBar.setDisplayShowCustomEnabled(true);
        } else {
            activity.setTitle(url);
        }
        //Changes the url in webview activity so that it can be opened with an external app
        try {
            if (activity instanceof WebviewActivity)
                ((WebviewActivity) activity).setUrl(url);
        } catch (Exception ignore) {
        }

    }

}
