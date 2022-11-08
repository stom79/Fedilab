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


import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.emojis;
import static app.fedilab.android.ui.drawer.ComposeAdapter.prepareDraft;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Context;
import app.fedilab.android.client.entities.api.EmojiInstance;
import app.fedilab.android.client.entities.api.Instance;
import app.fedilab.android.client.entities.api.Mention;
import app.fedilab.android.client.entities.api.ScheduledStatus;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.Languages;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.databinding.ActivityPaginationBinding;
import app.fedilab.android.databinding.PopupContactBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.DividerDecorationSimple;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.MediaHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.interfaces.OnDownloadInterface;
import app.fedilab.android.jobs.ComposeWorker;
import app.fedilab.android.jobs.ScheduleThreadWorker;
import app.fedilab.android.services.ThreadMessageService;
import app.fedilab.android.ui.drawer.AccountsReplyAdapter;
import app.fedilab.android.ui.drawer.ComposeAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;

public class ComposeActivity extends BaseActivity implements ComposeAdapter.ManageDrafts, AccountsReplyAdapter.ActionDone {


    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 754;
    public static final int REQUEST_AUDIO_PERMISSION_RESULT = 1653;
    public static final int PICK_MEDIA = 5700;
    public static final int TAKE_PHOTO = 5600;
    private final Timer timer = new Timer();
    private List<Status> statusList;
    private Status statusReply, statusMention;
    private StatusDraft statusDraft;
    private ComposeAdapter composeAdapter;


    private final BroadcastReceiver imageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String imgpath = intent.getStringExtra("imgpath");
            float focusX = intent.getFloatExtra("focusX", -2);
            float focusY = intent.getFloatExtra("focusY", -2);
            if (imgpath != null) {
                int position = 0;
                for (Status status : statusList) {
                    if (status.media_attachments != null && status.media_attachments.size() > 0) {
                        for (Attachment attachment : status.media_attachments) {
                            if (attachment.local_path != null && attachment.local_path.equalsIgnoreCase(imgpath)) {
                                if (focusX != -2) {
                                    attachment.focus = focusX + "," + focusY;
                                }
                                composeAdapter.notifyItemChanged(position);
                                break;
                            }
                        }
                    }
                    position++;
                }
            }
        }
    };

    private ActivityPaginationBinding binding;
    private BaseAccount account;
    private String instance, token;
    private Uri photoFileUri;
    private ScheduledStatus scheduledStatus;
    private String visibility;
    private app.fedilab.android.client.entities.api.Account accountMention;
    private String statusReplyId;
    private app.fedilab.android.client.entities.api.Account mentionBooster;
    private ArrayList<Uri> sharedUriList = new ArrayList<>();
    private Uri sharedUri;
    private String sharedSubject, sharedContent, sharedTitle, sharedDescription, shareURL, sharedUrlMedia;
    private String editMessageId;

    private static int visibilityToNumber(String visibility) {
        switch (visibility) {
            case "public":
                return 3;
            case "unlisted":
                return 2;
            case "private":
                return 1;
            case "direct":
                return 0;
        }
        return 3;
    }

    private static String visibilityToString(int visibility) {
        switch (visibility) {
            case 3:
                return "public";
            case 2:
                return "unlisted";
            case 1:
                return "private";
            case 0:
                return "direct";
        }
        return "public";
    }

    public static String getVisibility(String defaultVisibility) {
        int tootVisibility = visibilityToNumber(defaultVisibility);
        if (currentAccount != null && currentAccount.mastodon_account != null && currentAccount.mastodon_account.source != null) {
            int userVisibility = visibilityToNumber(currentAccount.mastodon_account.source.privacy);
            if (tootVisibility > userVisibility) {
                return visibilityToString(userVisibility);
            } else {
                return visibilityToString(tootVisibility);
            }
        }
        return defaultVisibility;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(imageReceiver);

    }

    @Override
    public void onBackPressed() {
        storeDraftWarning();
    }

    private void storeDraftWarning() {
        if (statusDraft == null) {
            statusDraft = prepareDraft(statusList, composeAdapter, account.instance, account.user_id);
        }
        if (canBeSent(statusDraft)) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(ComposeActivity.this, Helper.dialogStyle());
            alt_bld.setMessage(R.string.save_draft);
            alt_bld.setPositiveButton(R.string.save, (dialog, id) -> {
                dialog.dismiss();
                storeDraft(false);
                finish();

            });
            alt_bld.setNegativeButton(R.string.no, (dialog, id) -> {
                dialog.dismiss();
                finish();
            });
            AlertDialog alert = alt_bld.create();
            alert.show();
        } else {
            finish();
        }
    }

    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextView(final Context context) {

        if (context == null) {
            return;
        }
        //Build the array of statuses
        statusList.addAll(0, context.ancestors);
        composeAdapter.setStatusCount(context.ancestors.size() + 1);
        composeAdapter.notifyItemRangeInserted(0, context.ancestors.size());

        composeAdapter.notifyItemRangeChanged(0, statusList.size());
        if (binding.recyclerView.getItemDecorationCount() > 0) {
            for (int i = 0; i < binding.recyclerView.getItemDecorationCount(); i++) {
                binding.recyclerView.removeItemDecorationAt(i);
            }
        }
        binding.recyclerView.addItemDecoration(new DividerDecorationSimple(ComposeActivity.this, statusList));
        binding.recyclerView.scrollToPosition(composeAdapter.getItemCount() - 1);
    }

    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextRedraftView(final Context context, Status initialStatus) {

        if (context == null) {
            return;
        }

        //Build the array of statuses
        statusList.addAll(0, context.ancestors);
        statusList.add(initialStatus);
        statusList.add(statusDraft.statusDraftList.get(0));
        composeAdapter = new ComposeAdapter(statusList, context.ancestors.size(), account, accountMention, visibility, editMessageId);
        composeAdapter.manageDrafts = this;
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(composeAdapter);
        composeAdapter.setStatusCount(context.ancestors.size() + 1);
        binding.recyclerView.addItemDecoration(new DividerDecorationSimple(ComposeActivity.this, statusList));
        binding.recyclerView.scrollToPosition(composeAdapter.getItemCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            storeDraftWarning();
            return true;
        } else if (item.getItemId() == R.id.action_photo_camera) {
            photoFileUri = MediaHelper.dispatchTakePictureIntent(ComposeActivity.this);
        } else if (item.getItemId() == R.id.action_contacts) {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(ComposeActivity.this, Helper.dialogStyle());

            builderSingle.setTitle(getString(R.string.select_accounts));
            PopupContactBinding popupContactBinding = PopupContactBinding.inflate(getLayoutInflater(), new LinearLayout(ComposeActivity.this), false);
            popupContactBinding.loader.setVisibility(View.VISIBLE);
            AccountsVM accountsVM = new ViewModelProvider(ComposeActivity.this).get(AccountsVM.class);
            accountsVM.searchAccounts(instance, token, "", 10, false, true)
                    .observe(ComposeActivity.this, accounts -> onRetrieveContact(popupContactBinding, accounts));

            popupContactBinding.searchAccount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (count > 0) {
                        popupContactBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_close_24, 0);
                    } else {
                        popupContactBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_search_24, 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && s.length() > 0) {
                        accountsVM.searchAccounts(instance, token, s.toString().trim(), 10, false, true)
                                .observe(ComposeActivity.this, accounts -> onRetrieveContact(popupContactBinding, accounts));
                    }
                }
            });
            popupContactBinding.searchAccount.setOnTouchListener((v, event) -> {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (popupContactBinding.searchAccount.length() > 0 && event.getRawX() >= (popupContactBinding.searchAccount.getRight() - popupContactBinding.searchAccount.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        popupContactBinding.searchAccount.setText("");
                        accountsVM.searchAccounts(instance, token, "", 10, false, true)
                                .observe(ComposeActivity.this, accounts -> onRetrieveContact(popupContactBinding, accounts));
                    }
                }

                return false;
            });
            builderSingle.setView(popupContactBinding.getRoot());
            builderSingle.setNegativeButton(R.string.validate, (dialog, which) -> {
                dialog.dismiss();
                composeAdapter.putCursor();
            });
            builderSingle.show();
        } else if (item.getItemId() == R.id.action_microphone) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED) {
                    MediaHelper.recordAudio(ComposeActivity.this, file -> {
                        List<Uri> uris = new ArrayList<>();
                        uris.add(Uri.fromFile(new File(file)));
                        composeAdapter.addAttachment(-1, uris);
                    });
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                        Toast.makeText(this,
                                getString(R.string.audio), Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO
                    }, REQUEST_AUDIO_PERMISSION_RESULT);
                }

            } else {
                MediaHelper.recordAudio(ComposeActivity.this, file -> {
                    List<Uri> uris = new ArrayList<>();
                    uris.add(Uri.fromFile(new File(file)));
                    composeAdapter.addAttachment(-1, uris);
                });
            }
        } else if (item.getItemId() == R.id.action_schedule) {
            if (statusDraft == null) {
                statusDraft = prepareDraft(statusList, composeAdapter, account.instance, account.user_id);
            }
            if (canBeSent(statusDraft)) {
                MediaHelper.scheduleMessage(ComposeActivity.this, date -> storeDraft(true, date));
            } else {
                Toasty.info(ComposeActivity.this, getString(R.string.toot_error_no_content), Toasty.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.action_language) {
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ComposeActivity.this);
            List<Languages.Language> languages = Languages.get(ComposeActivity.this);
            String[] codesArr = new String[0];
            String[] languagesArr = new String[0];

            String currentCode = sharedpreferences.getString(getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, null);
            int selection = 0;

            if (languages != null) {
                codesArr = new String[languages.size()];
                languagesArr = new String[languages.size()];
                int i = 0;
                for (Languages.Language language : languages) {
                    codesArr[i] = language.code;
                    languagesArr[i] = language.language;
                    if (currentCode != null && currentCode.equalsIgnoreCase(language.code)) {
                        selection = i;
                    }
                    i++;
                }
            }
            SharedPreferences.Editor editor = sharedpreferences.edit();
            AlertDialog.Builder builder = new AlertDialog.Builder(ComposeActivity.this, Helper.dialogStyle());
            builder.setTitle(getString(R.string.message_language));

            builder.setSingleChoiceItems(languagesArr, selection, null);
            String[] finalCodesArr = codesArr;
            builder.setPositiveButton(R.string.validate, (dialog, which) -> {
                int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                editor.putString(getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, finalCodesArr[selectedPosition]);
                editor.apply();
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.reset, (dialog, which) -> {
                editor.putString(getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, null);
                editor.apply();
                dialog.dismiss();
            });
            builder.create().show();
        }
        return true;
    }

    private void onRetrieveContact(PopupContactBinding binding, List<app.fedilab.android.client.entities.api.Account> accounts) {
        binding.loader.setVisibility(View.GONE);
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        List<Boolean> checkedValues = new ArrayList<>();
        List<app.fedilab.android.client.entities.api.Account> contacts = new ArrayList<>(accounts);
        for (app.fedilab.android.client.entities.api.Account account : contacts) {
            checkedValues.add(composeAdapter.getLastComposeContent().contains("@" + account.acct));
        }
        AccountsReplyAdapter contactAdapter = new AccountsReplyAdapter(contacts, checkedValues);
        binding.lvAccountsSearch.setAdapter(contactAdapter);
        binding.lvAccountsSearch.setLayoutManager(new LinearLayoutManager(ComposeActivity.this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        List<Uri> uris = new ArrayList<>();
        if (requestCode >= PICK_MEDIA && resultCode == RESULT_OK) {
            ClipData clipData = data.getClipData();
            int position = requestCode - PICK_MEDIA;
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    uris.add(item.getUri());
                }
            } else {
                uris.add(data.getData());
            }
            composeAdapter.addAttachment(position, uris);
        } else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            uris.add(photoFileUri);
            composeAdapter.addAttachment(-1, uris);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityPaginationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float scale = sharedpreferences.getFloat(getString(R.string.SET_FONT_SCALE), 1.1f);
        binding.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
        statusList = new ArrayList<>();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            statusReply = (Status) b.getSerializable(Helper.ARG_STATUS_REPLY);
            statusDraft = (StatusDraft) b.getSerializable(Helper.ARG_STATUS_DRAFT);
            scheduledStatus = (ScheduledStatus) b.getSerializable(Helper.ARG_STATUS_SCHEDULED);
            statusReplyId = b.getString(Helper.ARG_STATUS_REPLY_ID);
            statusMention = (Status) b.getSerializable(Helper.ARG_STATUS_MENTION);
            account = (BaseAccount) b.getSerializable(Helper.ARG_ACCOUNT);
            editMessageId = b.getString(Helper.ARG_EDIT_STATUS_ID, null);
            instance = b.getString(Helper.ARG_INSTANCE, null);
            token = b.getString(Helper.ARG_TOKEN, null);
            visibility = b.getString(Helper.ARG_VISIBILITY, null);
            if (visibility == null && statusReply != null) {
                visibility = getVisibility(statusReply.visibility);
            } else if (visibility == null && currentAccount != null && currentAccount.mastodon_account != null && currentAccount.mastodon_account.source != null) {
                visibility = currentAccount.mastodon_account.source.privacy;
            }
            mentionBooster = (app.fedilab.android.client.entities.api.Account) b.getSerializable(Helper.ARG_MENTION_BOOSTER);
            accountMention = (app.fedilab.android.client.entities.api.Account) b.getSerializable(Helper.ARG_ACCOUNT_MENTION);
            //Shared elements
            sharedUriList = b.getParcelableArrayList(Helper.ARG_SHARE_URI_LIST);
            sharedUri = b.getParcelable(Helper.ARG_SHARE_URI);
            sharedUrlMedia = b.getString(Helper.ARG_SHARE_URL_MEDIA);
            sharedSubject = b.getString(Helper.ARG_SHARE_SUBJECT, null);
            sharedContent = b.getString(Helper.ARG_SHARE_CONTENT, null);
            sharedTitle = b.getString(Helper.ARG_SHARE_TITLE, null);
            sharedDescription = b.getString(Helper.ARG_SHARE_DESCRIPTION, null);
            shareURL = b.getString(Helper.ARG_SHARE_URL, null);
        }

        if (sharedContent != null && shareURL != null && sharedContent.compareTo(shareURL) == 0) {
            sharedContent = "";
        }
        if (sharedTitle != null && sharedSubject != null && sharedSubject.length() > sharedTitle.length()) {
            sharedTitle = sharedSubject;
        }
        binding.toolbar.setPopupTheme(Helper.popupStyle());
        //Edit a scheduled status from server
        if (scheduledStatus != null) {
            statusDraft = new StatusDraft();
            List<Status> statuses = new ArrayList<>();
            Status status = new Status();
            status.id = Helper.generateIdString();
            status.text = scheduledStatus.params.text;
            status.in_reply_to_id = scheduledStatus.params.in_reply_to_id;
            status.poll = scheduledStatus.params.poll;

            if (scheduledStatus.params.media_ids != null && scheduledStatus.params.media_ids.size() > 0) {
                status.media_attachments = new ArrayList<>();
                new Thread(() -> {
                    StatusesVM statusesVM = new ViewModelProvider(ComposeActivity.this).get(StatusesVM.class);
                    for (String attachmentId : scheduledStatus.params.media_ids) {
                        statusesVM.getAttachment(instance, token, attachmentId)
                                .observe(ComposeActivity.this, attachment -> status.media_attachments.add(attachment));
                    }
                }).start();
            }
            status.sensitive = scheduledStatus.params.sensitive;
            status.spoiler_text = scheduledStatus.params.spoiler_text;
            status.visibility = scheduledStatus.params.visibility;
            statusDraft.statusDraftList = statuses;
        }
        if (account == null) {
            account = currentAccount;
        }
        if (account == null) {
            Toasty.error(ComposeActivity.this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (instance == null) {
            instance = account.instance;
        }
        if (token == null) {
            token = account.token;
        }
        if (emojis == null || !emojis.containsKey(currentInstance)) {
            new Thread(() -> {
                try {
                    emojis.put(currentInstance, new EmojiInstance(ComposeActivity.this).getEmojiList(currentInstance));
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        if (MainActivity.instanceInfo == null) {
            String instanceInfo = sharedpreferences.getString(getString(R.string.INSTANCE_INFO) + instance, null);
            if (instanceInfo != null) {
                MainActivity.instanceInfo = Instance.restore(instanceInfo);
            }
        }

        StatusesVM statusesVM = new ViewModelProvider(ComposeActivity.this).get(StatusesVM.class);
        //Empty compose
        List<Status> statusDraftList = new ArrayList<>();
        Status status = new Status();
        status.id = Helper.generateIdString();
        statusDraftList.add(status);

        if (statusReplyId != null && statusDraft != null) {//Delete and redraft
            statusesVM.getStatus(currentInstance, BaseMainActivity.currentToken, statusReplyId)
                    .observe(ComposeActivity.this, status1 -> {
                        if (status1 != null) {
                            statusesVM.getContext(currentInstance, BaseMainActivity.currentToken, statusReplyId)
                                    .observe(ComposeActivity.this, statusContext -> {
                                        if (statusContext != null) {
                                            initializeContextRedraftView(statusContext, status1);
                                        } else {
                                            Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                                        }
                                    });
                        } else {
                            Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                        }
                    });
        } else if (statusDraft != null) {//Restore a draft with all messages
            if (statusDraft.statusReplyList != null) {
                statusList.addAll(statusDraft.statusReplyList);
                binding.recyclerView.addItemDecoration(new DividerDecorationSimple(ComposeActivity.this, statusList));
            }
            int statusCount = statusList.size();
            statusList.addAll(statusDraft.statusDraftList);
            composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility, editMessageId);
            composeAdapter.manageDrafts = this;
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
            binding.recyclerView.setLayoutManager(mLayoutManager);
            binding.recyclerView.setAdapter(composeAdapter);
            binding.recyclerView.scrollToPosition(composeAdapter.getItemCount() - 1);

        } else if (statusReply != null) {
            statusList.add(statusReply);
            int statusCount = statusList.size();
            statusDraftList.get(0).in_reply_to_id = statusReply.id;
            //We change order for mentions
            //At first place the account that has been mentioned if it's not our
            statusDraftList.get(0).mentions = new ArrayList<>();
            if (statusReply.account.acct != null && currentAccount.mastodon_account != null && !statusReply.account.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
                Mention mention = new Mention();
                mention.acct = "@" + statusReply.account.acct;
                mention.url = statusReply.account.url;
                mention.username = statusReply.account.username;
                statusDraftList.get(0).mentions.add(mention);
            }

            //There are other mentions to
            if (statusReply.mentions != null && statusReply.mentions.size() > 0) {
                for (Mention mentionTmp : statusReply.mentions) {
                    if (statusReply.account.acct != null && !mentionTmp.acct.equalsIgnoreCase(statusReply.account.acct) && currentAccount.mastodon_account != null && !mentionTmp.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
                        statusDraftList.get(0).mentions.add(mentionTmp);
                    }
                }
            }
            if (mentionBooster != null) {
                Mention mention = new Mention();
                mention.acct = mentionBooster.acct;
                mention.url = mentionBooster.url;
                mention.username = mentionBooster.username;
                boolean present = false;
                for (Mention mentionTmp : statusDraftList.get(0).mentions) {
                    if (mentionTmp.acct.equalsIgnoreCase(mentionBooster.acct)) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    statusDraftList.get(0).mentions.add(mention);
                }
            }
            if (statusReply.spoiler_text != null) {
                statusDraftList.get(0).spoiler_text = statusReply.spoiler_text;
            }
            //StatusDraftList at this point should only have one element
            statusList.addAll(statusDraftList);
            composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility, editMessageId);
            composeAdapter.manageDrafts = this;
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
            binding.recyclerView.setLayoutManager(mLayoutManager);
            binding.recyclerView.setAdapter(composeAdapter);
            statusesVM.getContext(currentInstance, BaseMainActivity.currentToken, statusReply.id)
                    .observe(ComposeActivity.this, this::initializeContextView);
        } else {
            //Compose without replying
            statusList.addAll(statusDraftList);
            composeAdapter = new ComposeAdapter(statusList, 0, account, accountMention, visibility, editMessageId);
            composeAdapter.manageDrafts = this;
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
            binding.recyclerView.setLayoutManager(mLayoutManager);
            binding.recyclerView.setAdapter(composeAdapter);
            if (statusMention != null) {
                composeAdapter.loadMentions(statusMention);
            }
        }
        MastodonHelper.loadPPMastodon(binding.profilePicture, account.mastodon_account);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(imageReceiver,
                        new IntentFilter(Helper.INTENT_SEND_MODIFIED_IMAGE));

        if (timer != null) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    storeDraft(false);
                }
            }, 0, 10000);
        }

        if (sharedUriList != null && sharedUriList.size() > 0) {

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                List<Uri> uris = new ArrayList<>(sharedUriList);
                composeAdapter.addAttachment(-1, uris);
            }, 1000);
        } else if (sharedUri != null && !sharedUri.toString().startsWith("http")) {
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                List<Uri> uris = new ArrayList<>();
                uris.add(sharedUri);
                composeAdapter.addAttachment(-1, uris);
            }, 1000);
        } else if (shareURL != null) {
            Helper.download(ComposeActivity.this, sharedUrlMedia, new OnDownloadInterface() {
                @Override
                public void onDownloaded(String saveFilePath, String downloadUrl, Error error) {

                    composeAdapter.addSharing(shareURL, sharedTitle, sharedDescription, sharedSubject, sharedContent, saveFilePath);
                }

                @Override
                public void onUpdateProgress(int progress) {

                }
            });

        } else {
            if (composeAdapter != null) {
                composeAdapter.addSharing(null, null, sharedDescription, null, sharedContent, null);
            }
        }
    }

    @Override
    public void onItemDraftAdded(int position) {
        Status status = new Status();

        status.id = Helper.generateIdString();
        status.mentions = statusList.get(position).mentions;
        status.visibility = statusList.get(position).visibility;
        status.spoiler_text = statusList.get(position).spoiler_text;
        status.sensitive = statusList.get(position).sensitive;
        statusList.add(status);
        composeAdapter.notifyItemInserted(position + 1);
        binding.recyclerView.smoothScrollToPosition(position + 1);
    }

    @Override
    public void onItemDraftDeleted(Status status, int position) {
        statusList.remove(status);
        composeAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onSubmit(StatusDraft draft) {
        //Store in drafts
        if (statusDraft == null) {
            statusDraft = draft;
        } else {
            statusDraft.statusDraftList = draft.statusDraftList;
        }
        storeDraft(true);
    }


    private void storeDraft(boolean sendMessage) {
        storeDraft(sendMessage, null);
    }

    private void storeDraft(boolean sendMessage, String scheduledDate) {
        new Thread(() -> {

            //Collect all statusCompose
            List<Status> statusDrafts = new ArrayList<>();
            List<Status> statusReplies = new ArrayList<>();
            for (Status status : statusList) {
                if (status.id == null || status.id.startsWith("@fedilab_compose_")) {
                    statusDrafts.add(status);
                } else {
                    statusReplies.add(status);
                }

            }
            if (statusDraft == null) {
                statusDraft = new StatusDraft(ComposeActivity.this);
            } else {
                //Draft previously and date is changed
                if (statusDraft.scheduled_at != null && scheduledDate != null && statusDraft.workerUuid != null) {
                    WorkManager.getInstance(ComposeActivity.this).cancelWorkById(statusDraft.workerUuid);
                }
            }
            if (statusReplies.size() > 0) {
                statusDraft.statusReplyList = new ArrayList<>();
                statusDraft.statusReplyList.addAll(statusReplies);
            }
            if (statusDrafts.size() > 0) {
                statusDraft.statusDraftList = new ArrayList<>();
                statusDraft.statusDraftList.addAll(statusDrafts);
            }
            if (statusDraft.instance == null) {
                statusDraft.instance = account.instance;
            }
            if (statusDraft.user_id == null) {
                statusDraft.user_id = account.user_id;
            }

            if (!canBeSent(statusDraft)) {
                return;
            }
            if (statusDraft.id > 0) {
                try {
                    new StatusDraft(ComposeActivity.this).updateStatusDraft(statusDraft);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    statusDraft.id = new StatusDraft(ComposeActivity.this).insertStatusDraft(statusDraft);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
            //Only one single message scheduled
            if (sendMessage && scheduledDate != null && statusDraft.statusDraftList.size() > 1) {
                //Schedule a thread
                SimpleDateFormat sdf = new SimpleDateFormat(Helper.SCHEDULE_DATE_FORMAT, Locale.getDefault());
                Date date;
                try {
                    date = sdf.parse(scheduledDate);
                    long delayToPass = 0;
                    if (date != null) {
                        delayToPass = (date.getTime() - new Date().getTime());
                    }
                    Data inputData = new Data.Builder()
                            .putString(Helper.ARG_INSTANCE, currentInstance)
                            .putString(Helper.ARG_TOKEN, BaseMainActivity.currentToken)
                            .putString(Helper.ARG_USER_ID, BaseMainActivity.currentUserID)
                            .putLong(Helper.ARG_STATUS_DRAFT_ID, statusDraft.id)
                            .build();

                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ScheduleThreadWorker.class)
                            .setInputData(inputData)
                            .addTag(Helper.WORKER_SCHEDULED_STATUSES)
                            .setInitialDelay(delayToPass, TimeUnit.MILLISECONDS)
                            .build();
                    WorkManager.getInstance(ComposeActivity.this).enqueue(oneTimeWorkRequest);
                    statusDraft.workerUuid = oneTimeWorkRequest.getId();
                    statusDraft.scheduled_at = date;
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        Toasty.info(ComposeActivity.this, getString(R.string.toot_scheduled), Toasty.LENGTH_LONG).show();
                        finish();
                    };
                    mainHandler.post(myRunnable);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else if (sendMessage) {
                int mediaCount = 0;
                for (Status status : statusDraft.statusDraftList) {
                    mediaCount += status.media_attachments != null ? status.media_attachments.size() : 0;
                }
                if (mediaCount > 0) {
                    Data inputData = new Data.Builder()
                            .putString(Helper.ARG_STATUS_DRAFT_ID, String.valueOf(statusDraft.id))
                            .putString(Helper.ARG_INSTANCE, instance)
                            .putString(Helper.ARG_TOKEN, token)
                            .putString(Helper.ARG_EDIT_STATUS_ID, editMessageId)
                            .putString(Helper.ARG_USER_ID, account.user_id)
                            .putString(Helper.ARG_SCHEDULED_DATE, scheduledDate).build();
                    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ComposeWorker.class)
                            .setInputData(inputData)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build();
                    WorkManager.getInstance(ComposeActivity.this).enqueue(request);

                } else {
                    new ThreadMessageService(ComposeActivity.this, instance, account.user_id, token, statusDraft, scheduledDate, editMessageId);
                }
                finish();
            }

        }).start();
    }


    private boolean canBeSent(StatusDraft statusDraft) {
        if (statusDraft == null || statusDraft.statusDraftList == null || statusDraft.statusDraftList.isEmpty()) {
            return false;
        }
        Status statusCheck = statusDraft.statusDraftList.get(0);
        if (statusCheck == null) {
            return false;
        }
        return (statusCheck.text != null && statusCheck.text.trim().length() != 0)
                || (statusCheck.media_attachments != null && statusCheck.media_attachments.size() != 0)
                || statusCheck.poll != null
                || (statusCheck.spoiler_text != null && statusCheck.spoiler_text.trim().length() != 0);
    }


    @Override
    public void onContactClick(boolean isChecked, String acct) {
        composeAdapter.updateContent(isChecked, acct);
    }


    public enum mediaType {
        PHOTO,
        VIDEO,
        AUDIO,
        ALL
    }


}