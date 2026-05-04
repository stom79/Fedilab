package app.fedilab.android.activities;
/* Copyright 2025 Thomas Schneider
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.CrossActionHelper;
import app.fedilab.android.mastodon.helper.Helper;


public class WebActivityPub extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent appIntent = getIntent();
        if (appIntent == null) {
            finish();
            return;
        }
        Uri uri = appIntent.getData();
        if (uri == null) {
            finish();
            return;
        }
        String scheme = uri.getScheme();
        String uriString = uri.toString();
        if (!uriString.startsWith(scheme + "://")) {
            uriString = uriString.replace(scheme + ":", scheme + "://");
            uri = Uri.parse(uriString);
            if (uri == null) {
                finish();
                return;
            }
        }

        String host = uri.getHost();
        String path = uri.getPath();

        if (host == null || path == null || path.isEmpty()) {
            finish();
            return;
        }

        String httpsUrl = "https://" + host + path;
        String acct = null;
        boolean isStatus = false;

        if (path.startsWith("/@")) {
            String[] params = path.split("@");
            if (params.length == 2) {
                acct = params[1] + "@" + host;
            } else if (params.length >= 2 && path.matches("/@[^/]+/\\d+")) {
                isStatus = true;
            }
        } else if (path.split("/").length > 2) {
            String[] params = path.split("/");
            String root = params[1].toLowerCase();
            if (root.equals("users") && params.length == 3) {
                acct = params[2] + "@" + host;
            } else {
                isStatus = true;
            }
        }

        if (acct != null) {
            openProfile(acct);
        } else if (isStatus) {
            openStatus(httpsUrl);
        } else {
            finish();
        }
    }

    private void openProfile(String acct) {
        if (Helper.getCurrentAccount(WebActivityPub.this) == null) {
            finish();
            return;
        }
        Intent intentProfile = new Intent(WebActivityPub.this, ProfileActivity.class);
        Bundle args = new Bundle();
        args.putString(Helper.ARG_MENTION, acct);
        new CachedBundle(WebActivityPub.this).insertBundle(args, Helper.getCurrentAccount(WebActivityPub.this), bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            intentProfile.putExtras(bundle);
            intentProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentProfile);
            finish();
        });
    }

    private void openStatus(String httpsUrl) {
        if (Helper.getCurrentAccount(WebActivityPub.this) == null) {
            finish();
            return;
        }
        CrossActionHelper.fetchRemoteStatus(WebActivityPub.this, Helper.getCurrentAccount(WebActivityPub.this), httpsUrl, new CrossActionHelper.Callback() {
            @Override
            public void federatedStatus(Status status) {
                if (status != null) {
                    Intent intentContext = new Intent(WebActivityPub.this, ContextActivity.class);
                    Bundle args = new Bundle();
                    args.putSerializable(Helper.ARG_STATUS, status);
                    new CachedBundle(WebActivityPub.this).insertBundle(args, Helper.getCurrentAccount(WebActivityPub.this), bundleId -> {
                        Bundle bundle = new Bundle();
                        bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                        intentContext.putExtras(bundle);
                        intentContext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intentContext);
                        finish();
                    });
                } else {
                    finish();
                }
            }

            @Override
            public void federatedAccount(Account account) {
            }
        });
    }
}
