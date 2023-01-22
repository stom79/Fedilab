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

import static app.fedilab.android.peertube.activities.PeertubeUploadActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;
import static app.fedilab.android.peertube.worker.WorkHelper.NOTIFICATION_WORKER;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityMyAccountSettingsPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.NotificationSettings;
import app.fedilab.android.peertube.client.entities.UserMe;
import app.fedilab.android.peertube.client.entities.UserSettings;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.worker.WorkHelper;
import es.dmoral.toasty.Toasty;

public class MyAccountActivity extends BaseBarActivity {

    private static final int PICK_IMAGE = 466;
    ActivityMyAccountSettingsPeertubeBinding binding;
    private Uri inputData;
    private String fileName;
    private NotificationSettings notificationSettings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyAccountSettingsPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (PeertubeMainActivity.userMe == null) {
            finish();
            return;
        }
        setTitle(String.format("@%s", PeertubeMainActivity.userMe.getUsername()));
        binding.displayname.setText(PeertubeMainActivity.userMe.getAccount().getDisplayName());
        binding.description.setText(PeertubeMainActivity.userMe.getAccount().getDescription());

        notificationSettings = PeertubeMainActivity.userMe.getNotificationSettings();
        initializeValues(notificationSettings.getAbuseStateChange(), binding.notifAbuseAcceptedApp, binding.notifAbuseAcceptedMail);
        initializeValues(notificationSettings.getAbuseNewMessage(), binding.notifAbuseReceivedApp, binding.notifAbuseReceivedMail);
        initializeValues(notificationSettings.getCommentMention(), binding.notifVideoMentionApp, binding.notifVideoMentionMail);
        initializeValues(notificationSettings.getNewFollow(), binding.notifNewFollowersApp, binding.notifNewFollowersMail);
        initializeValues(notificationSettings.getMyVideoImportFinished(), binding.notifVideoImportedApp, binding.notifVideoImportedMail);
        initializeValues(notificationSettings.getMyVideoPublished(), binding.notifVideoPublishedApp, binding.notifVideoPublishedMail);
        initializeValues(notificationSettings.getBlacklistOnMyVideo(), binding.notifBlockedApp, binding.notifBlockedMail);
        initializeValues(notificationSettings.getNewCommentOnMyVideo(), binding.notifNewCommentApp, binding.notifNewCommentMail);
        initializeValues(notificationSettings.getNewVideoFromSubscription(), binding.notifNewVideoApp, binding.notifNewVideoMail);

        Helper.loadAvatar(MyAccountActivity.this, PeertubeMainActivity.userMe.getAccount(), binding.profilePicture);
        String[] refresh_array = getResources().getStringArray(R.array.refresh_time);
        ArrayAdapter<String> refreshArray = new ArrayAdapter<>(MyAccountActivity.this,
                android.R.layout.simple_spinner_dropdown_item, refresh_array);
        binding.refreshTime.setAdapter(refreshArray);

        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        int interval = sharedpreferences.getInt(Helper.NOTIFICATION_INTERVAL, 60);
        binding.refreshTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                int time;
                switch (position) {
                    case 1:
                        time = 15;
                        break;
                    case 2:
                        time = 30;
                        break;
                    case 3:
                        time = 60;
                        break;
                    case 4:
                        time = 120;
                        break;
                    case 5:
                        time = 360;
                        break;
                    case 6:
                        time = 720;
                        break;
                    default:
                        time = 0;
                }
                editor.putInt(Helper.NOTIFICATION_INTERVAL, time);
                editor.apply();
                WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(NOTIFICATION_WORKER);
                if (time > 0) {
                    WorkHelper.fetchNotifications(getApplication(), time);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int position = 0;
        switch (interval) {
            case 0:
                position = 0;
                break;
            case 15:
                position = 1;
                break;
            case 30:
                position = 2;
                break;
            case 60:
                position = 3;
                break;
            case 120:
                position = 4;
                break;
            case 360:
                position = 5;
                break;
            case 720:
                position = 6;
                break;
        }
        binding.refreshTime.setSelection(position, false);

        binding.selectFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MyAccountActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MyAccountActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {"image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, PICK_IMAGE);
        });

    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_my_account_peertube, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_validate) {
            item.setEnabled(false);
            new Thread(() -> {
                UserSettings userSettings = new UserSettings();
                userSettings.setNotificationSettings(notificationSettings);
                if (binding.displayname.getText() != null) {
                    userSettings.setDisplayName(binding.displayname.getText().toString().trim());
                }
                if (binding.description.getText() != null) {
                    userSettings.setDescription(binding.description.getText().toString().trim());
                }
                if (inputData != null) {
                    userSettings.setAvatarfile(inputData);
                    userSettings.setFileName(fileName);
                }
                try {
                    RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(MyAccountActivity.this);
                    UserMe.AvatarResponse avatarResponse = api.updateUser(userSettings);
                    PeertubeMainActivity.userMe.getAccount().setDisplayName(binding.displayname.getText().toString().trim());
                    PeertubeMainActivity.userMe.getAccount().setDescription(binding.description.getText().toString().trim());
                    if (avatarResponse != null && avatarResponse.getAvatar() != null) {
                        PeertubeMainActivity.userMe.getAccount().setAvatar(avatarResponse.getAvatar());
                    }

                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        Toasty.info(MyAccountActivity.this, getString(R.string.account_updated), Toasty.LENGTH_LONG).show();
                        item.setEnabled(true);
                    };
                    mainHandler.post(myRunnable);
                } catch (Exception | Error e) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        Toasty.error(MyAccountActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                        item.setEnabled(true);
                    };
                    mainHandler.post(myRunnable);
                }
            }).start();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(MyAccountActivity.this, getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
                return;
            }
            inputData = data.getData();
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, inputData);
            if (documentFile != null) {
                fileName = documentFile.getName();
            }
            Glide.with(MyAccountActivity.this)
                    .load(inputData)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(binding.profilePicture);

        }
    }

    private void initializeValues(int value, SwitchCompat app, SwitchCompat email) {
        switch (value) {
            case 1:
                app.setChecked(true);
                email.setChecked(false);
                break;
            case 2:
                app.setChecked(false);
                email.setChecked(true);
                break;
            case 3:
                app.setChecked(true);
                email.setChecked(true);
                break;
            default:
                app.setChecked(false);
                email.setChecked(false);
        }
        app.setOnCheckedChangeListener((compoundButton, checked) -> {
            int id = app.getId();
            if (id == R.id.notif_new_video_app) {
                notificationSettings.setNewVideoFromSubscription(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_new_comment_app) {
                notificationSettings.setNewCommentOnMyVideo(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_blocked_app) {
                notificationSettings.setBlacklistOnMyVideo(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_video_published_app) {
                notificationSettings.setMyVideoPublished(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_video_imported_app) {
                notificationSettings.setMyVideoImportFinished(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_new_followers_app) {
                notificationSettings.setNewFollow(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_video_mention_app) {
                notificationSettings.setCommentMention(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_abuse_received_app) {
                notificationSettings.setAbuseNewMessage(getNewAppCheckedValue(checked, email));
            } else if (id == R.id.notif_abuse_accepted_app) {
                notificationSettings.setAbuseStateChange(getNewAppCheckedValue(checked, email));
            }
        });
        email.setOnCheckedChangeListener((compoundButtonMail, checkedMail) -> {
            int id = email.getId();
            if (id == R.id.notif_new_video_mail) {
                notificationSettings.setNewVideoFromSubscription(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_new_comment_mail) {
                notificationSettings.setNewCommentOnMyVideo(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_blocked_mail) {
                notificationSettings.setBlacklistOnMyVideo(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_video_published_mail) {
                notificationSettings.setMyVideoPublished(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_video_imported_mail) {
                notificationSettings.setMyVideoImportFinished(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_new_followers_mail) {
                notificationSettings.setNewFollow(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_video_mention_mail) {
                notificationSettings.setCommentMention(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_abuse_received_mail) {
                notificationSettings.setAbuseNewMessage(getNewMailCheckedValue(checkedMail, app));
            } else if (id == R.id.notif_abuse_accepted_mail) {
                notificationSettings.setAbuseStateChange(getNewMailCheckedValue(checkedMail, app));
            }
        });
    }

    private int getNewAppCheckedValue(boolean checked, SwitchCompat email) {
        int newValue;
        if (checked && email.isChecked()) {
            newValue = 3;
        } else if (!checked && email.isChecked()) {
            newValue = 2;
        } else if (checked && !email.isChecked()) {
            newValue = 1;
        } else {
            newValue = 0;
        }
        return newValue;
    }

    private int getNewMailCheckedValue(boolean checked, SwitchCompat app) {
        int newValue;
        if (checked && app.isChecked()) {
            newValue = 3;
        } else if (!checked && app.isChecked()) {
            newValue = 1;
        } else if (checked && !app.isChecked()) {
            newValue = 2;
        } else {
            newValue = 0;
        }
        return newValue;
    }
}
