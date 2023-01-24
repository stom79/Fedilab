package app.fedilab.android.peertube.webview;
/* Copyright 2023 Thomas Schneider
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

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import app.fedilab.android.R;


public class MastalabWebViewClient extends WebViewClient {

    private final Activity activity;


    public MastalabWebViewClient(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
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

    }

}
