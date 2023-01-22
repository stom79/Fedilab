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

import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.PEERTUBEDELETEVIDEO;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.DataType.MY_CHANNELS;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData.Channel;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.data.VideoData.Video;
import app.fedilab.android.peertube.client.entities.Item;
import app.fedilab.android.peertube.client.entities.ItemStr;
import app.fedilab.android.peertube.client.entities.VideoParams;
import app.fedilab.android.peertube.databinding.ActivityPeertubeEditBinding;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.Theme;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.MyVideoVM;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class PeertubeEditUploadActivity extends BaseActivity {


    private final int PICK_IMAGE = 50378;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 724;
    Item licenseToSend, privacyToSend, categoryToSend;
    ItemStr languageToSend;
    private LinkedHashMap<String, String> channels;
    private String videoId;
    private Channel channel;
    private VideoParams videoParams;
    private Video video;
    private String channelToSendId;
    private ActivityPeertubeEditBinding binding;
    private Uri inputData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.setTheme(this, HelperInstance.getLiveInstance(this), false);
        super.onCreate(savedInstanceState);
        binding = ActivityPeertubeEditBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, MODE_PRIVATE);

        Bundle b = getIntent().getExtras();

        if (b != null) {
            videoId = b.getString("video_id", null);
        }
        if (videoId == null) {
            videoId = sharedpreferences.getString(Helper.VIDEO_ID, null);
            sharedpreferences.edit().remove(Helper.VIDEO_ID).apply();
        }
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.setUploadDelete.setOnClickListener(v -> {
            AlertDialog.Builder builderInner;
            builderInner = new AlertDialog.Builder(PeertubeEditUploadActivity.this);
            builderInner.setMessage(getString(R.string.delete_video_confirmation));
            builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builderInner.setPositiveButton(R.string.yes, (dialog, which) -> {
                PostActionsVM viewModel = new ViewModelProvider(PeertubeEditUploadActivity.this).get(PostActionsVM.class);
                viewModel.post(PEERTUBEDELETEVIDEO, videoId, null).observe(PeertubeEditUploadActivity.this, apiResponse -> manageVIewPostActions(PEERTUBEDELETEVIDEO, apiResponse));
                dialog.dismiss();
            });
            builderInner.show();
        });
        //Get params from the API
        LinkedHashMap<Integer, String> categories = new LinkedHashMap<>(peertubeInformation.getCategories());
        LinkedHashMap<Integer, String> licences = new LinkedHashMap<>(peertubeInformation.getLicences());
        LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        LinkedHashMap<String, String> languages = new LinkedHashMap<>(peertubeInformation.getLanguages());
        LinkedHashMap<String, String> translations = null;

        if (peertubeInformation.getTranslations() != null)
            translations = new LinkedHashMap<>(peertubeInformation.getTranslations());
        //Populate catgories
        String[] categoriesA = new String[categories.size()];
        Iterator<Map.Entry<Integer, String>> it = categories.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                categoriesA[i] = pair.getValue();
            else
                categoriesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }
        ArrayAdapter<String> adapterCatgories = new ArrayAdapter<>(PeertubeEditUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, categoriesA);
        binding.setUploadCategories.setAdapter(adapterCatgories);


        //Populate licenses
        String[] licensesA = new String[licences.size()];
        it = licences.entrySet().iterator();
        i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                licensesA[i] = pair.getValue();
            else
                licensesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }
        ArrayAdapter<String> adapterLicenses = new ArrayAdapter<>(PeertubeEditUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, licensesA);
        binding.setUploadLicenses.setAdapter(adapterLicenses);


        //Populate languages
        String[] languagesA = new String[languages.size()];
        Iterator<Map.Entry<String, String>> itl = languages.entrySet().iterator();
        i = 0;
        while (itl.hasNext()) {
            Map.Entry<String, String> pair = itl.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                languagesA[i] = pair.getValue();
            else
                languagesA[i] = translations.get(pair.getValue());
            itl.remove();
            i++;
        }
        ArrayAdapter<String> adapterLanguages = new ArrayAdapter<>(PeertubeEditUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, languagesA);
        binding.setUploadLanguages.setAdapter(adapterLanguages);


        //Populate languages
        String[] privaciesA = new String[privacies.size()];
        it = privacies.entrySet().iterator();
        i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                privaciesA[i] = pair.getValue();
            else
                privaciesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }

        ArrayAdapter<String> adapterPrivacies = new ArrayAdapter<>(PeertubeEditUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, privaciesA);
        binding.setUploadPrivacy.setAdapter(adapterPrivacies);


        TimelineVM feedsViewModel = new ViewModelProvider(PeertubeEditUploadActivity.this).get(TimelineVM.class);
        feedsViewModel.getMyVideo(null, videoId).observe(PeertubeEditUploadActivity.this, this::manageVIewVideo);

        channels = new LinkedHashMap<>();

        setTitle(R.string.edit_video);
    }

    public void manageUpdate(APIResponse apiResponse) {
        binding.setUploadSubmit.setEnabled(true);
        if (apiResponse.getError() != null) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(PeertubeEditUploadActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeEditUploadActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
        }
        Toasty.info(PeertubeEditUploadActivity.this, getString(R.string.toast_peertube_video_updated), Toast.LENGTH_LONG).show();
    }

    public void manageVIewVideo(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(PeertubeEditUploadActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeEditUploadActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            binding.setUploadSubmit.setEnabled(true);
            return;
        }

        //Peertube video
        video = apiResponse.getPeertubes().get(0);

        ChannelsVM viewModelC = new ViewModelProvider(PeertubeEditUploadActivity.this).get(ChannelsVM.class);
        viewModelC.get(MY_CHANNELS, null).observe(PeertubeEditUploadActivity.this, this::manageVIewChannels);

        languageToSend = video.getLanguage();
        licenseToSend = video.getLicence();
        privacyToSend = video.getPrivacy();
        categoryToSend = video.getCategory();

        if (video.getThumbnailPath() != null) {
            Helper.loadGiF(PeertubeEditUploadActivity.this, video.getThumbnailPath(), binding.pVideoPreview);
        }


        binding.setPreview.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(PeertubeEditUploadActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PeertubeEditUploadActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/jpg");
            String[] mimetypes = {"image/jpg", "image/jpeg"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, PICK_IMAGE);

        });
        if (languageToSend == null) {
            languageToSend = new ItemStr();
            LinkedHashMap<String, String> languages = new LinkedHashMap<>(peertubeInformation.getLanguages());
            Map.Entry<String, String> entryString = languages.entrySet().iterator().next();
            languageToSend.setId(entryString.getKey());
            languageToSend.setLabel(entryString.getValue());
        }

        if (licenseToSend == null) {
            licenseToSend = new Item();
            LinkedHashMap<Integer, String> licences = new LinkedHashMap<>(peertubeInformation.getLicences());
            Map.Entry<Integer, String> entryInt = licences.entrySet().iterator().next();
            licenseToSend.setId(entryInt.getKey());
            licenseToSend.setLabel(entryInt.getValue());
        }

        if (categoryToSend == null) {
            categoryToSend = new Item();
            LinkedHashMap<Integer, String> categories = new LinkedHashMap<>(peertubeInformation.getCategories());
            Map.Entry<Integer, String> entryInt = categories.entrySet().iterator().next();
            categoryToSend.setId(entryInt.getKey());
            categoryToSend.setLabel(entryInt.getValue());
        }
        if (privacyToSend == null) {
            privacyToSend = new Item();
            LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPrivacies());
            Map.Entry<Integer, String> entryInt = privacies.entrySet().iterator().next();
            privacyToSend.setId(entryInt.getKey());
            privacyToSend.setLabel(entryInt.getValue());
        }

        String language = languageToSend.getId();
        int license = licenseToSend.getId();
        int privacy = privacyToSend.getId();
        int category = categoryToSend.getId();

        channel = video.getChannel();
        String title = video.getName();
        boolean commentEnabled = video.isCommentsEnabled();
        boolean isNSFW = video.isNsfw();

        binding.setUploadEnableComments.setChecked(commentEnabled);
        binding.setUploadNsfw.setChecked(isNSFW);

        binding.pVideoTitle.setText(title);
        binding.pVideoDescription.setText(video.getDescription());

        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api;
                api = new RetrofitPeertubeAPI(PeertubeEditUploadActivity.this);
                VideoData.Description description = api.getVideoDescription(video.getUuid());
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (description != null) {
                        binding.pVideoDescription.setText(description.getDescription());
                    }
                };
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        LinkedHashMap<Integer, String> categories = new LinkedHashMap<>(peertubeInformation.getCategories());
        LinkedHashMap<Integer, String> licences = new LinkedHashMap<>(peertubeInformation.getLicences());
        LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        LinkedHashMap<String, String> languages = new LinkedHashMap<>(peertubeInformation.getLanguages());


        int languagePosition = 0;

        if (languages.containsKey(language)) {
            Iterator<Map.Entry<String, String>> itstr = languages.entrySet().iterator();
            while (itstr.hasNext()) {
                Map.Entry<String, String> pair = itstr.next();
                if (pair.getKey().compareTo(language) == 0)
                    break;
                itstr.remove();
                languagePosition++;
            }
        }
        int privacyPosition = 0;
        if (privacies.containsKey(privacy)) {
            Iterator<Map.Entry<Integer, String>> it = privacies.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pair = it.next();
                if (pair.getKey() == privacy)
                    break;
                it.remove();
                privacyPosition++;
            }
        }
        int licensePosition = 0;
        if (licences.containsKey(license)) {
            Iterator<Map.Entry<Integer, String>> it = licences.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pair = it.next();
                if (pair.getKey() == license)
                    break;
                it.remove();
                licensePosition++;
            }
        }
        int categoryPosition = 0;
        if (categories.containsKey(category)) {
            Iterator<Map.Entry<Integer, String>> it = categories.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, String> pair = it.next();
                if (pair.getKey() == category)
                    break;
                it.remove();
                categoryPosition++;
            }
        }
        //Manage privacies
        binding.setUploadPrivacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePrivacyPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.setUploadLicenses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLicensePosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //Manage categories
        binding.setUploadCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCategoryPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Manage languages
        binding.setUploadLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLanguagesPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //Manage languages
        binding.setUploadChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        Item finalCategoryToSend = categoryToSend;
        Item finalLicenseToSend = licenseToSend;
        ItemStr finalLanguageToSend = languageToSend;
        Item finalPrivacyToSend = privacyToSend;
        binding.setUploadSubmit.setOnClickListener(v -> {
            String title1 = binding.pVideoTitle.getText() != null ? binding.pVideoTitle.getText().toString().trim() : "";
            String description = binding.pVideoDescription.getText() != null ? binding.pVideoDescription.getText().toString().trim() : "";
            boolean isNSFW1 = binding.setUploadNsfw.isChecked();
            boolean commentEnabled1 = binding.setUploadEnableComments.isChecked();
            videoParams = new VideoParams();
            videoParams.setName(title1);
            videoParams.setDescription(description);
            videoParams.setNsfw(isNSFW1);
            videoParams.setCommentsEnabled(commentEnabled1);
            videoParams.setCategory(finalCategoryToSend.getId());
            videoParams.setLicence(String.valueOf(finalLicenseToSend.getId()));
            videoParams.setLanguage(finalLanguageToSend.getId());
            videoParams.setChannelId(channelToSendId);
            videoParams.setPrivacy(finalPrivacyToSend.getId());
            List<String> tags = binding.pVideoTags.getTags();
            if (tags.size() > 5) {
                Toasty.error(PeertubeEditUploadActivity.this, getString(R.string.max_tag_size), Toast.LENGTH_LONG).show();
                return;
            }
            videoParams.setTags(tags);
            binding.setUploadSubmit.setEnabled(false);
            MyVideoVM myVideoVM = new ViewModelProvider(PeertubeEditUploadActivity.this).get(MyVideoVM.class);
            myVideoVM.updateVideo(videoId, videoParams, inputData, inputData).observe(PeertubeEditUploadActivity.this, this::manageUpdate);
        });

        binding.setUploadPrivacy.setSelection(privacyPosition, false);
        updatePrivacyPosition(privacyPosition);
        binding.setUploadLanguages.setSelection(languagePosition, false);
        updateLanguagesPosition(languagePosition);
        binding.setUploadLicenses.setSelection(licensePosition, false);
        updateLicensePosition(licensePosition);
        binding.setUploadCategories.setSelection(categoryPosition, false);
        updateCategoryPosition(categoryPosition);

        List<String> tags = video.getTags();
        if (tags != null && tags.size() > 0) {
            binding.pVideoTags.setTags(tags.toArray(new String[0]));
        }
    }

    private void updateUploadChannel(int position) {
        LinkedHashMap<String, String> channelsCheck = new LinkedHashMap<>(channels);
        Iterator<Map.Entry<String, String>> it = channelsCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (i == position) {
                channelToSendId = pair.getValue();
                break;
            }
            it.remove();
            i++;
        }
    }

    private void updateLanguagesPosition(int position) {
        LinkedHashMap<String, String> languagesCheck = new LinkedHashMap<>(peertubeInformation.getLanguages());
        Iterator<Map.Entry<String, String>> it = languagesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (i == position) {
                languageToSend.setId(pair.getKey());
                languageToSend.setLabel(pair.getValue());
                break;
            }
            it.remove();
            i++;
        }
    }

    private void updateCategoryPosition(int position) {
        LinkedHashMap<Integer, String> categoriesCheck = new LinkedHashMap<>(peertubeInformation.getCategories());
        Iterator<Map.Entry<Integer, String>> it = categoriesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (i == position) {
                categoryToSend.setId(pair.getKey());
                categoryToSend.setLabel(pair.getValue());
                break;
            }
            it.remove();
            i++;
        }
    }

    private void updateLicensePosition(int position) {
        LinkedHashMap<Integer, String> licensesCheck = new LinkedHashMap<>(peertubeInformation.getLicences());
        Iterator<Map.Entry<Integer, String>> it = licensesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (i == position) {
                licenseToSend.setId(pair.getKey());
                licenseToSend.setLabel(pair.getValue());
                break;
            }
            it.remove();
            i++;
        }
    }

    private void updatePrivacyPosition(int position) {
        LinkedHashMap<Integer, String> privaciesCheck = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        Iterator<Map.Entry<Integer, String>> it = privaciesCheck.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (i == position) {
                privacyToSend.setId(pair.getKey());
                privacyToSend.setLabel(pair.getValue());
                break;
            }
            it.remove();
            i++;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(PeertubeEditUploadActivity.this, getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
                return;
            }
            inputData = data.getData();
            Glide.with(PeertubeEditUploadActivity.this)
                    .load(data.getData())
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(binding.pVideoPreview);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void manageVIewChannels(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getChannels() == null || apiResponse.getChannels().size() == 0) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(PeertubeEditUploadActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeEditUploadActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        //Populate channels
        List<Channel> channelsReply = apiResponse.getChannels();
        String[] channelName = new String[channelsReply.size()];
        int i = 0;
        for (Channel channel : channelsReply) {
            channels.put(channel.getName(), channel.getId());
            channelName[i] = channel.getName();
            i++;
        }
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(PeertubeEditUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, channelName);
        binding.setUploadChannel.setAdapter(adapterChannel);
        int channelPosition = 0;
        if (channels.containsKey(channel.getName())) {
            LinkedHashMap<String, String> channelsIterator = new LinkedHashMap<>(channels);
            Iterator<Map.Entry<String, String>> it = channelsIterator.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                if (pair.getKey().equals(channel.getName())) {
                    channelToSendId = pair.getKey();
                    break;
                }
                it.remove();
                channelPosition++;
            }
        }
        binding.setUploadChannel.setSelection(channelPosition, false);
        updateUploadChannel(channelPosition);
        binding.setUploadSubmit.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, APIResponse apiResponse) {
        Intent intent = new Intent(PeertubeEditUploadActivity.this, MainActivity.class);
        intent.putExtra(Helper.INTENT_ACTION, Helper.RELOAD_MYVIDEOS);
        startActivity(intent);
        finish();
    }
}
