package app.fedilab.android.mastodon.activities;
/* Copyright 2022 Thomas Schneider
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

import static app.fedilab.android.BaseMainActivity.currentAccount;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityCustomSharingBinding;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.customsharing.CustomSharingAsyncTask;
import app.fedilab.android.mastodon.helper.customsharing.CustomSharingResponse;
import app.fedilab.android.mastodon.helper.customsharing.OnCustomSharingInterface;
import es.dmoral.toasty.Toasty;


/**
 * Created by Curtis on 13/02/2019.
 * Share status metadata to remote content aggregators
 */

public class CustomSharingActivity extends BaseBarActivity implements OnCustomSharingInterface {

    private String title, keywords, custom_sharing_url, encodedCustomSharingURL;
    private String bundle_url;
    private String bundle_source;
    private String bundle_id;
    private String bundle_content;
    private String bundle_thumbnailurl;
    private String bundle_creator;
    private ActivityCustomSharingBinding binding;
    private Status status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(CustomSharingActivity.this);
        binding = ActivityCustomSharingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        Bundle b = getIntent().getExtras();
        status = null;
        if (b != null) {
            status = (Status) b.getSerializable(Helper.ARG_STATUS);
        }
        if (status == null) {
            finish();
            return;
        }

        bundle_creator = status.account.acct;
        bundle_url = status.url;
        bundle_id = status.uri;
        bundle_source = status.account.url;
        String bundle_tags = getTagsString();
        bundle_content = formatedContent(status.content, status.emojis);
        if (status.card != null && status.card.image != null) {
            bundle_thumbnailurl = status.card.image;
        } else if (status.media_attachments != null && status.media_attachments.size() > 0) {
            List<Attachment> mediaAttachments = status.media_attachments;
            Attachment firstAttachment = mediaAttachments.get(0);
            bundle_thumbnailurl = firstAttachment.preview_url;
        } else {
            bundle_thumbnailurl = status.account.avatar;
        }
        if (!bundle_creator.contains("@")) {
            bundle_creator = bundle_creator + "@" + currentAccount.instance;
        }

        binding.setCustomSharingTitle.setEllipsize(TextUtils.TruncateAt.END);
        //set text on title, description, and keywords
        String[] lines = bundle_content.split("\n");
        //Remove tags in title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            lines[0] = Html.fromHtml(lines[0], Html.FROM_HTML_MODE_LEGACY).toString();
        else
            lines[0] = Html.fromHtml(lines[0]).toString();
        String newTitle;
        if (lines[0].length() > 60) {
            newTitle = lines[0].substring(0, 60) + '…';
        } else {
            newTitle = lines[0];
        }
        binding.setCustomSharingTitle.setText(newTitle);
        String newDescription;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            newDescription = Html.fromHtml(bundle_content, Html.FROM_HTML_MODE_LEGACY).toString();
        else
            newDescription = Html.fromHtml(bundle_content).toString();

        binding.setCustomSharingDescription.setText(newDescription);
        binding.setCustomSharingKeywords.setText(bundle_tags);
        binding.setCustomSharingSave.setOnClickListener(v -> {
            // obtain title, description, keywords
            title = binding.setCustomSharingTitle.getText().toString();
            keywords = Objects.requireNonNull(binding.setCustomSharingKeywords.getText()).toString();
            CharSequence comma_only = ",";
            CharSequence space_only = " ";
            CharSequence double_space = "  ";
            keywords = keywords.replace(comma_only, space_only);
            keywords = keywords.replace(double_space, space_only);
            // Create encodedCustomSharingURL
            custom_sharing_url = sharedpreferences.getString(getString(R.string.SET_CUSTOM_SHARING_URL),
                    "http://example.net/add?token=YOUR_TOKEN&url=${url}&title=${title}" +
                            "&source=${source}&id=${id}&description=${description}&keywords=${keywords}&creator=${creator}&thumbnailurl=${thumbnailurl}");
            encodedCustomSharingURL = encodeCustomSharingURL();
            new CustomSharingAsyncTask(CustomSharingActivity.this, encodedCustomSharingURL, CustomSharingActivity.this);
        });
    }

    private String getTagsString() {
        //iterate through tags and create comma delimited string of tag names
        StringBuilder tag_names = new StringBuilder();
        for (Tag t : status.tags) {
            if (tag_names.toString().equals("")) {
                tag_names = new StringBuilder(t.name);
            } else {
                tag_names.append(", ").append(t.name);
            }
        }
        return tag_names.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCustomSharing(CustomSharingResponse customSharingResponse) {
        binding.setCustomSharingSave.setEnabled(true);
        if (customSharingResponse.getError() != null) {
            Toasty.error(CustomSharingActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        String response = customSharingResponse.getResponse();
        Toasty.success(CustomSharingActivity.this, response, Toast.LENGTH_LONG).show();
        finish();
    }

    public String encodeCustomSharingURL() {
        Uri uri = Uri.parse(custom_sharing_url);
        String protocol = uri.getScheme();
        String server = uri.getAuthority();
        String path = uri.getPath();
        if (path != null) {
            path = path.replaceAll("/", "");
        }
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(protocol)
                .authority(server)
                .appendPath(path);
        Set<String> args = uri.getQueryParameterNames();
        boolean paramFound;
        for (String param_name : args) {
            paramFound = false;
            String param_value = uri.getQueryParameter(param_name);
            if (param_value != null)
                switch (param_value) {
                    case "${url}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_url);
                    }
                    case "${title}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, title);
                    }
                    case "${source}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_source);
                    }
                    case "${id}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_id);
                    }
                    case "${description}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_content);
                    }
                    case "${keywords}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, keywords);
                    }
                    case "${creator}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_creator);
                    }
                    case "${thumbnailurl}" -> {
                        paramFound = true;
                        builder.appendQueryParameter(param_name, bundle_thumbnailurl);
                    }
                }
            if (!paramFound) {
                builder.appendQueryParameter(param_name, param_value);
            }
        }
        return builder.build().toString();
    }


    private String formatedContent(String content, List<Emoji> emojis) {
        //Avoid null content
        if (content == null)
            return "";
        if (emojis == null || emojis.size() == 0)
            return content;
        for (Emoji emoji : emojis) {
            content = content.replaceAll(":" + emoji.shortcode + ":", "<img src='" + emoji.url + "' width=20 alt='" + emoji.shortcode + "'/>");
        }
        return content;
    }

}
