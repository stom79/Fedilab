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

import static app.fedilab.android.MainApplication.UPLOAD_CHANNEL_ID;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.userMe;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.DataType.MY_CHANNELS;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import net.gotev.uploadservice.data.UploadNotificationAction;
import net.gotev.uploadservice.data.UploadNotificationConfig;
import net.gotev.uploadservice.data.UploadNotificationStatusConfig;
import net.gotev.uploadservice.extensions.ContextExtensionsKt;
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityPeertubeUploadBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import es.dmoral.toasty.Toasty;


public class PeertubeUploadActivity extends BaseBarActivity {


    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 724;
    private final int PICK_IVDEO = 52378;
    private HashMap<String, String> channels;
    private Uri uri;
    private String filename;
    private HashMap<Integer, String> privacyToSend;
    private HashMap<String, String> channelToSend;
    private ActivityPeertubeUploadBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding = ActivityPeertubeUploadBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        new Thread(() -> {
            UserMe.VideoQuota videoQuotaReply = new RetrofitPeertubeAPI(PeertubeUploadActivity.this).getVideoQuota();
            runOnUiThread(() -> {
                if (videoQuotaReply != null) {
                    long videoQuota = videoQuotaReply.getVideoQuotaUsed();
                    long dailyQuota = videoQuotaReply.getVideoQuotaUsedDaily();
                    long instanceVideoQuota = userMe.getVideoQuota();
                    long instanceDailyQuota = userMe.getVideoQuotaDaily();

                    if (instanceVideoQuota != -1 && instanceVideoQuota != 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            binding.totalQuota.setProgress((int) (videoQuota * 100 / instanceVideoQuota), true);
                        } else {
                            binding.totalQuota.setProgress((int) (videoQuota * 100 / instanceVideoQuota));
                        }
                    } else {
                        int progress = videoQuota > 0 ? 30 : 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            binding.totalQuota.setProgress(progress, true);
                        } else {
                            binding.totalQuota.setProgress(progress);
                        }
                    }
                    if (instanceDailyQuota != -1 && instanceDailyQuota != 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            binding.dailyQuota.setProgress((int) (dailyQuota * 100 / instanceDailyQuota), true);
                        } else {
                            binding.dailyQuota.setProgress((int) (dailyQuota * 100 / instanceDailyQuota));
                        }
                    } else {
                        int progress = dailyQuota > 0 ? 30 : 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            binding.dailyQuota.setProgress(progress, true);
                        } else {
                            binding.dailyQuota.setProgress(progress);
                        }
                    }
                    binding.totalQuotaValue.setText(
                            String.format(Locale.getDefault(), "%s/%s",
                                    Helper.returnRoundedSize(PeertubeUploadActivity.this, videoQuota),
                                    Helper.returnRoundedSize(PeertubeUploadActivity.this, instanceVideoQuota)));
                    binding.dailyQuotaValue.setText(
                            String.format(Locale.getDefault(), "%s/%s",
                                    Helper.returnRoundedSize(PeertubeUploadActivity.this, dailyQuota),
                                    Helper.returnRoundedSize(PeertubeUploadActivity.this, instanceDailyQuota)));
                }
            });
        }).start();


        ChannelsVM viewModelC = new ViewModelProvider(PeertubeUploadActivity.this).get(ChannelsVM.class);
        viewModelC.get(MY_CHANNELS, null).observe(PeertubeUploadActivity.this, this::manageVIewChannels);
        channels = new HashMap<>();
        setTitle(R.string.upload_video);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IVDEO && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(PeertubeUploadActivity.this, getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
                return;
            }
            binding.setUploadSubmit.setEnabled(true);
            uri = data.getData();
            filename = null;
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
            if (documentFile != null) {
                filename = documentFile.getName();
            }
            if (filename == null) {
                filename = new Date().toString();
            }
            binding.setUploadFileName.setVisibility(View.VISIBLE);
            binding.setUploadFileName.setText(filename);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
                Toasty.error(PeertubeUploadActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeUploadActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }

        //Populate channels
        List<ChannelData.Channel> channelsForUser = apiResponse.getChannels();
        String[] channelName = new String[channelsForUser.size()];
        String[] channelId = new String[channelsForUser.size()];
        int i = 0;
        for (ChannelData.Channel channel : channelsForUser) {
            channels.put(channel.getName(), channel.getId());
            channelName[i] = channel.getName();
            channelId[i] = channel.getId();
            i++;
        }

        channelToSend = new HashMap<>();
        channelToSend.put(channelName[0], channelId[0]);
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(PeertubeUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, channelName);
        binding.setUploadChannel.setAdapter(adapterChannel);

        if (peertubeInformation == null) {
            return;
        }
        LinkedHashMap<String, String> translations = null;
        if (peertubeInformation.getTranslations() != null)
            translations = new LinkedHashMap<>(peertubeInformation.getTranslations());

        LinkedHashMap<Integer, String> privaciesInit = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        Map.Entry<Integer, String> entryInt = privaciesInit.entrySet().iterator().next();
        privacyToSend = new HashMap<>();
        privacyToSend.put(entryInt.getKey(), entryInt.getValue());
        LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        //Populate privacies
        String[] privaciesA = new String[privacies.size()];
        Iterator<Map.Entry<Integer, String>> it = privacies.entrySet().iterator();
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

        ArrayAdapter<String> adapterPrivacies = new ArrayAdapter<>(PeertubeUploadActivity.this,
                android.R.layout.simple_spinner_dropdown_item, privaciesA);
        binding.setUploadPrivacy.setAdapter(adapterPrivacies);

        //Manage privacies
        binding.setUploadPrivacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinkedHashMap<Integer, String> privaciesCheck = new LinkedHashMap<>(peertubeInformation.getPrivacies());
                Iterator<Map.Entry<Integer, String>> it = privaciesCheck.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Map.Entry<Integer, String> pair = it.next();
                    if (i == position) {
                        privacyToSend = new HashMap<>();
                        privacyToSend.put(pair.getKey(), pair.getValue());
                        break;
                    }
                    it.remove();
                    i++;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.setUploadFile.setEnabled(true);

        binding.setUploadFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(PeertubeUploadActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PeertubeUploadActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {"video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, PICK_IVDEO);

        });

        //Manage languages
        binding.setUploadChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinkedHashMap<String, String> channelsCheck = new LinkedHashMap<>(channels);
                Iterator<Map.Entry<String, String>> it = channelsCheck.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    if (i == position) {
                        channelToSend = new HashMap<>();
                        channelToSend.put(pair.getKey(), pair.getValue());

                        break;
                    }
                    it.remove();
                    i++;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.setUploadSubmit.setOnClickListener(v -> {
            if (uri != null) {
                Map.Entry<String, String> channelM = channelToSend.entrySet().iterator().next();
                String idChannel = channelM.getValue();
                Map.Entry<Integer, String> privacyM = privacyToSend.entrySet().iterator().next();
                Integer idPrivacy = privacyM.getKey();
                if (binding.videoTitle.getText() != null && binding.videoTitle.getText().toString().trim().length() > 0) {
                    filename = binding.videoTitle.getText().toString().trim();
                }
                try {
                    String token = Helper.getToken(PeertubeUploadActivity.this);
                    new MultipartUploadRequest(PeertubeUploadActivity.this, "https://" + HelperInstance.getLiveInstance(PeertubeUploadActivity.this) + "/api/v1/videos/upload")
                            .setMethod("POST")
                            .setBearerAuth(token)
                            .addHeader("User-Agent", getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME)
                            .addParameter("privacy", String.valueOf(idPrivacy))
                            .addParameter("nsfw", "false")
                            .addParameter("name", filename)
                            .addParameter("commentsEnabled", "true")
                            .addParameter("downloadEnabled", "true")
                            .addParameter("waitTranscoding", "true")
                            .addParameter("channelId", idChannel)
                            .addFileToUpload(uri.toString(), "videofile")
                            .setNotificationConfig((context, uploadId) -> getNotificationConfig(uploadId))
                            .setMaxRetries(2)
                            .startUpload();
                    finish();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
    }

    UploadNotificationConfig getNotificationConfig(String uploadId) {
        PendingIntent clickIntent = PendingIntent.getActivity(
                PeertubeUploadActivity.this, 1, new Intent(this, PeertubeEditUploadActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        final boolean autoClear = false;
        final boolean clearOnAction = true;
        final boolean ringToneEnabled = true;
        final ArrayList<UploadNotificationAction> noActions = new ArrayList<>(1);

        final UploadNotificationAction cancelAction = new UploadNotificationAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.cancel),
                ContextExtensionsKt.getCancelUploadIntent(this, uploadId)
        );


        final ArrayList<UploadNotificationAction> progressActions = new ArrayList<>(1);
        progressActions.add(cancelAction);

        UploadNotificationStatusConfig progress = new UploadNotificationStatusConfig(
                getString(R.string.app_name),
                getString(R.string.uploading),
                R.drawable.ic_baseline_cloud_upload_24,
                Color.BLUE,
                null,
                clickIntent,
                progressActions,
                clearOnAction,
                autoClear
        );

        UploadNotificationStatusConfig success = new UploadNotificationStatusConfig(
                getString(R.string.app_name),
                getString(R.string.upload_video_success),
                R.drawable.ic_baseline_check_24,
                Color.GREEN,
                null,
                clickIntent,
                noActions,
                clearOnAction,
                autoClear
        );


        UploadNotificationStatusConfig error = new UploadNotificationStatusConfig(
                getString(R.string.app_name),
                getString(R.string.toast_error),
                R.drawable.ic_baseline_error_24,
                Color.RED,
                null,
                clickIntent,
                noActions,
                clearOnAction,
                autoClear
        );

        UploadNotificationStatusConfig cancelled = new UploadNotificationStatusConfig(
                getString(R.string.app_name),
                getString(R.string.toast_cancelled),
                R.drawable.ic_baseline_cancel_24,
                Color.YELLOW,
                null,
                clickIntent,
                noActions,
                clearOnAction
        );

        return new UploadNotificationConfig(UPLOAD_CHANNEL_ID, ringToneEnabled, progress, success, error, cancelled);
    }
}
