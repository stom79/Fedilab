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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;


public class WebActivityPub extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent appIntent = getIntent();
        String acct = null;
        String intent = null;
        if(appIntent == null) {
            finish();
            return;
        }
        Uri uri = appIntent.getData();
        if(uri == null) {
            finish();
            return;
        }
        String scheme = uri.getScheme();
        String uriString = uri.toString();
        if(!uriString.startsWith(scheme+"://")) {
            uriString = uri.toString().replace(scheme+":",scheme+"://");
            uri = Uri.parse(uriString);
            if(uri == null) {
                finish();
                return;
            }
        }

        String host = uri.getHost();
        String path = uri.getPath();
        String query = uri.getQuery();

        if(path == null || path.isEmpty()) {
            finish();
            return;
        }
        if(query != null) {
            String intentPatternString = "intent=(\\w+)";
            final Pattern intentPattern = Pattern.compile(intentPatternString, Pattern.CASE_INSENSITIVE);
            Matcher matcherIntent = intentPattern.matcher(query);
            while (matcherIntent.find()) {
                intent = matcherIntent.group(1);
            }
        }
        if(path.startsWith("/@")) {
            String[] params = path.split("@");
            if(params.length == 2) {
                acct = params[1] + "@" + host;
            }
        } else if(path.split("/").length > 2) {
            String[] params = path.split("/");
            String root = params[1].toLowerCase();
            if (root.equals("users")) {
                acct = params[2] + "@" + host;
            }
        }
        if(acct != null) {
            Intent intentProfile = new Intent(WebActivityPub.this, ProfileActivity.class);
            Bundle args = new Bundle();
            args.putString(Helper.ARG_MENTION, acct);
            new CachedBundle(WebActivityPub.this).insertBundle(args, Helper.getCurrentAccount(WebActivityPub.this), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intentProfile.putExtras(bundle);
                intentProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentProfile);
            });
        }
        finish();
    }
}
