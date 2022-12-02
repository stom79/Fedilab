package app.fedilab.android.activities;
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


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityWebviewBinding;
import app.fedilab.android.helper.CountDrawable;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.webview.CustomWebview;
import app.fedilab.android.webview.FedilabWebChromeClient;
import app.fedilab.android.webview.FedilabWebViewClient;
import es.dmoral.toasty.Toasty;


public class WebviewActivity extends BaseActivity {


    private String url;
    private boolean peertubeLink;
    private CustomWebview webView;
    private FedilabWebViewClient FedilabWebViewClient;
    private ActivityWebviewBinding binding;
    private Menu defaultMenu;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityWebviewBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            url = b.getString("url", null);
            peertubeLink = b.getBoolean("peertubeLink", false);
        }
        if (url == null)
            finish();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView = Helper.initializeWebview(WebviewActivity.this, R.id.webview, null);
        setTitle("");

        webView.getSettings().setJavaScriptEnabled(true);


        FedilabWebChromeClient FedilabWebChromeClient = new FedilabWebChromeClient(WebviewActivity.this, webView, binding.webviewContainer, binding.videoLayout);
        FedilabWebChromeClient.setOnToggledFullscreen(fullscreen -> {

            if (fullscreen) {
                binding.videoLayout.setVisibility(View.VISIBLE);
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().setAttributes(attrs);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                WindowManager.LayoutParams attrs = getWindow().getAttributes();
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().setAttributes(attrs);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                binding.videoLayout.setVisibility(View.GONE);
            }
        });
        webView.setWebChromeClient(FedilabWebChromeClient);
        FedilabWebViewClient = new FedilabWebViewClient(WebviewActivity.this);
        webView.setWebViewClient(FedilabWebViewClient);
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {

            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(WebviewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(WebviewActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(WebviewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, Helper.EXTERNAL_STORAGE_REQUEST_CODE);
                } else {
                    Helper.manageDownloads(WebviewActivity.this, url);
                }
            } else {
                Helper.manageDownloads(WebviewActivity.this, url);
            }
        });
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
            url = "http://" + url;
        webView.loadUrl(url);

    }


    public void setCount(Context context, String count) {
        if (defaultMenu != null && !peertubeLink) {
            MenuItem menuItem = defaultMenu.findItem(R.id.action_block);
            LayerDrawable icon = (LayerDrawable) menuItem.getIcon();

            CountDrawable badge;

            // Reuse drawable if possible
            Drawable reuse = icon.findDrawableByLayerId(R.id.ic_block_count);
            if (reuse instanceof CountDrawable) {
                badge = (CountDrawable) reuse;
            } else {
                badge = new CountDrawable(context);
            }

            badge.setCount(count);
            icon.mutate();
            icon.setDrawableByLayerId(R.id.ic_block_count, badge);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!peertubeLink)
            setCount(WebviewActivity.this, "0");
        defaultMenu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_webview, menu);
        defaultMenu = menu;
        if (peertubeLink) {
            menu.findItem(R.id.action_go).setVisible(false);
            menu.findItem(R.id.action_block).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_block) {


            List<String> domains = FedilabWebViewClient.getDomains();

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(WebviewActivity.this, R.layout.domains_blocked);
            arrayAdapter.addAll(domains);

            AlertDialog.Builder builder = new AlertDialog.Builder(WebviewActivity.this, Helper.dialogStyle());
            builder.setTitle(R.string.list_of_blocked_domains);

            builder.setNegativeButton(R.string.close, (dialog, which) -> dialog.dismiss());

            builder.setAdapter(arrayAdapter, (dialog, which) -> {
                String strName = arrayAdapter.getItem(which);
                assert strName != null;
                Toasty.info(WebviewActivity.this, strName, Toast.LENGTH_LONG).show();
            });
            builder.show();

            return true;
        } else if (itemId == R.id.action_go) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(browserIntent);
            } catch (Exception e) {
                Toasty.error(WebviewActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setUrl(String newUrl) {
        this.url = newUrl;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null)
            webView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null)
            webView.onResume();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null)
            webView.destroy();
    }

}
