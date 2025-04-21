package app.fedilab.android.mastodon.activities;
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


import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.emojis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityPaginationBinding;
import app.fedilab.android.databinding.PopupContactBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.EmojiInstance;
import app.fedilab.android.mastodon.client.entities.api.Instance;
import app.fedilab.android.mastodon.client.entities.api.Mention;
import app.fedilab.android.mastodon.client.entities.api.ScheduledStatus;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.DividerDecorationSimple;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.interfaces.OnDownloadInterface;
import app.fedilab.android.mastodon.jobs.ComposeWorker;
import app.fedilab.android.mastodon.jobs.ScheduleThreadWorker;
import app.fedilab.android.mastodon.services.ThreadMessageService;
import app.fedilab.android.mastodon.ui.drawer.AccountsReplyAdapter;
import app.fedilab.android.mastodon.ui.drawer.ComposeAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;

public class ComposeActivity extends BaseActivity implements ComposeAdapter.ManageDrafts, AccountsReplyAdapter.ActionDone, ComposeAdapter.promptDraftListener, ComposeAdapter.MediaDescriptionCallBack {


    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 754;
    public static final int REQUEST_AUDIO_PERMISSION_RESULT = 1653;
    public static final int PICK_MEDIA = 5700;
    public static final int TAKE_PHOTO = 5600;
    private final Timer timer = new Timer();
    private List<Status> statusList;
    private Status statusReply, statusMention, statusQuoted;
    private StatusDraft statusDraft;
    private ActionBar actionBar;
    private ComposeAdapter composeAdapter;
    private final BroadcastReceiver imageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            Bundle args = intent.getExtras();
            if (args != null) {
                long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
                new CachedBundle(ComposeActivity.this).getBundle(bundleId, Helper.getCurrentAccount(ComposeActivity.this), bundle -> {
                    String imgpath = bundle.getString("imgpath");
                    float focusX = bundle.getFloat("focusX", -2);
                    float focusY = bundle.getFloat("focusY", -2);
                    if (imgpath != null) {
                        int position = 0;
                        for (Status status : statusList) {
                            if (status.media_attachments != null && !status.media_attachments.isEmpty()) {
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
                });
            }
        }
    };
    private boolean promptSaveDraft;
    private boolean restoredDraft;
    private List<Attachment> sharedAttachments;
    private ActivityPaginationBinding binding;
    private BaseAccount account;
    private String instance, token;
    private Uri photoFileUri;
    private ScheduledStatus scheduledStatus;
    private String visibility;
    private Account accountMention;
    private String statusReplyId;
    private Account mentionBooster;
    private String sharedSubject, sharedContent, sharedTitle, sharedDescription, shareURL, sharedUrlMedia;
    private String editMessageId;

    private static int visibilityToNumber(String visibility) {
        return switch (visibility) {
            case "unlisted" -> 2;
            case "private" -> 1;
            case "direct" -> 0;
            default -> 3;
        };
    }

    private static String visibilityToString(int visibility) {
        return switch (visibility) {
            case 2 -> "unlisted";
            case 1 -> "private";
            case 0 -> "direct";
            default -> "public";
        };
    }

    public static String getVisibility(BaseAccount account, String defaultVisibility) {
        int tootVisibility = visibilityToNumber(defaultVisibility);
        if (account != null && account.mastodon_account != null && account.mastodon_account.source != null) {
            int userVisibility = visibilityToNumber(account.mastodon_account.source.privacy);
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
        try {
            unregisterReceiver(imageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    private void storeDraftWarning() {
        if (statusDraft == null) {
            statusDraft = ComposeAdapter.prepareDraft(statusList, composeAdapter, account.instance, account.user_id);
        }
        if (canBeSent(statusDraft) != 0) {
            if (promptSaveDraft) {
                AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(ComposeActivity.this);
                alt_bld.setMessage(R.string.save_draft);
                alt_bld.setPositiveButton(R.string.save, (dialog, id) -> {
                    dialog.dismiss();
                    storeDraft(false);
                    finish();

                });
                alt_bld.setNegativeButton(R.string.no, (dialog, id) -> {
                    try {
                        new StatusDraft(ComposeActivity.this).removeDraft(statusDraft);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                    finish();
                });
                AlertDialog alert = alt_bld.create();
                alert.show();
            } else {
                if (!restoredDraft) {
                    try {
                        new StatusDraft(ComposeActivity.this).removeDraft(statusDraft);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
                finish();
            }
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
        composeAdapter.mediaDescriptionCallBack = this;
        composeAdapter.promptDraftListener = this;
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MenuItem micro = menu.findItem(R.id.action_microphone);
            if (micro != null) {
                micro.setVisible(false);
            }
        }
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
            AlertDialog.Builder builderSingle = new MaterialAlertDialogBuilder(ComposeActivity.this);

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
                statusDraft = ComposeAdapter.prepareDraft(statusList, composeAdapter, account.instance, account.user_id);
            }
            if (canBeSent(statusDraft) == 1) {
                MediaHelper.scheduleMessage(ComposeActivity.this, date -> storeDraft(true, date));
            } else if (canBeSent(statusDraft) == -1) {
                Toasty.warning(ComposeActivity.this, getString(R.string.toot_error_no_media_description), Toasty.LENGTH_SHORT).show();
            } else if (canBeSent(statusDraft) == -2) {
                Toasty.warning(ComposeActivity.this, getString(R.string.toot_error_no_media_description), Toasty.LENGTH_SHORT).show();
                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this);
                materialAlertDialogBuilder.setMessage(R.string.toot_error_no_media_description);
                materialAlertDialogBuilder.setPositiveButton(R.string.send_anyway, (dialog, id) -> {
                    MediaHelper.scheduleMessage(ComposeActivity.this, date -> storeDraft(true, date));
                    dialog.dismiss();

                });
                materialAlertDialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> {
                    dialog.cancel();
                });
                AlertDialog alert = materialAlertDialogBuilder.create();
                alert.show();
            } else {
                Toasty.info(ComposeActivity.this, getString(R.string.toot_error_no_content), Toasty.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.action_tags) {
            TagCacheActivity tagCacheActivity = new TagCacheActivity();
            tagCacheActivity.show(getSupportFragmentManager(), null);
        }
        return true;
    }

    private void onRetrieveContact(PopupContactBinding popupContactBinding, List<Account> accounts) {
        popupContactBinding.loader.setVisibility(View.GONE);
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        List<Boolean> checkedValues = new ArrayList<>();
        List<Account> contacts = new ArrayList<>(accounts);
        for (Account account : contacts) {
            checkedValues.add(composeAdapter.getLastComposeContent().contains("@" + account.acct));
        }
        AccountsReplyAdapter contactAdapter = new AccountsReplyAdapter(contacts, checkedValues);
        contactAdapter.actionDone = ComposeActivity.this;
        popupContactBinding.lvAccountsSearch.setAdapter(contactAdapter);
        popupContactBinding.lvAccountsSearch.setLayoutManager(new LinearLayoutManager(ComposeActivity.this));
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

        binding = ActivityPaginationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        promptSaveDraft = false;
        restoredDraft = false;
        actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
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
            long bundleId = b.getLong(Helper.ARG_INTENT_ID, -1);
            if (bundleId != -1) {
                new CachedBundle(ComposeActivity.this).getBundle(bundleId, Helper.getCurrentAccount(ComposeActivity.this), this::initializeAfterBundle);
            } else {
                initializeAfterBundle(b);
            }
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle b) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);

        new Thread(() -> {
            if (b != null) {
                statusReply = (Status) b.getSerializable(Helper.ARG_STATUS_REPLY);
                statusQuoted = (Status) b.getSerializable(Helper.ARG_QUOTED_MESSAGE);
                statusDraft = (StatusDraft) b.getSerializable(Helper.ARG_STATUS_DRAFT);
                scheduledStatus = (ScheduledStatus) b.getSerializable(Helper.ARG_STATUS_SCHEDULED);
                statusReplyId = b.getString(Helper.ARG_STATUS_REPLY_ID);
                statusMention = (Status) b.getSerializable(Helper.ARG_STATUS_MENTION);
                account = (BaseAccount) b.getSerializable(Helper.ARG_ACCOUNT);
                if (account == null) {
                    account = Helper.getCurrentAccount(ComposeActivity.this);
                }
                boolean setMentionBooster = sharedpreferences.getBoolean(getString(R.string.SET_MENTION_BOOSTER) + account.user_id + account.instance, false);

                editMessageId = b.getString(Helper.ARG_EDIT_STATUS_ID, null);
                instance = b.getString(Helper.ARG_INSTANCE, null);
                token = b.getString(Helper.ARG_TOKEN, null);
                visibility = b.getString(Helper.ARG_VISIBILITY, null);
                if (visibility == null && statusReply != null) {
                    visibility = getVisibility(account, statusReply.visibility);
                } else if (visibility == null && Helper.getCurrentAccount(ComposeActivity.this) != null && Helper.getCurrentAccount(ComposeActivity.this).mastodon_account != null && Helper.getCurrentAccount(ComposeActivity.this).mastodon_account.source != null) {
                    visibility = Helper.getCurrentAccount(ComposeActivity.this).mastodon_account.source.privacy;
                }
                if(setMentionBooster) {
                    mentionBooster = (Account) b.getSerializable(Helper.ARG_MENTION_BOOSTER);
                } else {
                    mentionBooster = null;
                }
                accountMention = (Account) b.getSerializable(Helper.ARG_ACCOUNT_MENTION);
                //Shared elements
                sharedAttachments = (ArrayList<Attachment>) b.getSerializable(Helper.ARG_MEDIA_ATTACHMENTS);
                sharedUrlMedia = b.getString(Helper.ARG_SHARE_URL_MEDIA);
                sharedSubject = b.getString(Helper.ARG_SHARE_SUBJECT, null);
                sharedContent = b.getString(Helper.ARG_SHARE_CONTENT, null);
                sharedTitle = b.getString(Helper.ARG_SHARE_TITLE, null);
                sharedDescription = b.getString(Helper.ARG_SHARE_DESCRIPTION, null);
                shareURL = b.getString(Helper.ARG_SHARE_URL, null);
            }
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                if (sharedContent != null && shareURL != null && sharedContent.compareTo(shareURL) == 0) {
                    sharedContent = "";
                }
                if (sharedTitle != null && sharedSubject != null && sharedSubject.length() > sharedTitle.length()) {
                    sharedTitle = sharedSubject;
                }
                //Edit a scheduled status from server
                if (scheduledStatus != null) {
                    statusDraft = new StatusDraft();
                    List<Status> statuses = new ArrayList<>();
                    Status status = new Status();
                    status.id = Helper.generateIdString();
                    status.text = scheduledStatus.params.text;
                    status.in_reply_to_id = scheduledStatus.params.in_reply_to_id;
                    status.poll = scheduledStatus.params.poll;
                    if (scheduledStatus.params.media_ids != null && !scheduledStatus.params.media_ids.isEmpty()) {
                        status.media_attachments = new ArrayList<>();
                        for (String attachmentId : scheduledStatus.params.media_ids) {
                            Attachment attachment = new Attachment();
                            attachment.id = attachmentId;
                            status.media_attachments.add(attachment);
                        }
                    }
                    status.sensitive = scheduledStatus.params.sensitive;
                    status.spoiler_text = scheduledStatus.params.spoiler_text;
                    status.visibility = scheduledStatus.params.visibility;
                    statuses.add(status);
                    statusDraft.statusDraftList = statuses;
                }
                if (account == null) {
                    account = Helper.getCurrentAccount(ComposeActivity.this);
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
                if (emojis == null || !emojis.containsKey(instance)) {
                    new Thread(() -> {
                        try {
                            emojis.put(instance, new EmojiInstance(ComposeActivity.this).getEmojiList(instance));
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
                if (statusQuoted != null) {
                    status.quote_id = statusQuoted.id;
                }
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
                    restoredDraft = true;
                    if (statusDraft.statusReplyList != null) {
                        statusList.addAll(statusDraft.statusReplyList);
                        binding.recyclerView.addItemDecoration(new DividerDecorationSimple(ComposeActivity.this, statusList));
                    }
                    int statusCount = statusList.size();
                    statusList.addAll(statusDraft.statusDraftList);
                    composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility, editMessageId);
                    composeAdapter.mediaDescriptionCallBack = this;
                    composeAdapter.manageDrafts = this;
                    composeAdapter.promptDraftListener = this;
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
                    if (statusReply.account.acct != null && account.mastodon_account != null && !statusReply.account.acct.equalsIgnoreCase(account.mastodon_account.acct)) {
                        Mention mention = new Mention();
                        mention.acct = "@" + statusReply.account.acct;
                        mention.url = statusReply.account.url;
                        mention.username = statusReply.account.username;
                        statusDraftList.get(0).mentions.add(mention);
                    }


                    //There are other mentions to
                    if (statusReply.mentions != null && statusReply.mentions.size() > 0) {
                        for (Mention mentionTmp : statusReply.mentions) {
                            if (statusReply.account.acct != null && !mentionTmp.acct.equalsIgnoreCase(statusReply.account.acct) && account.mastodon_account != null && !mentionTmp.acct.equalsIgnoreCase(account.mastodon_account.acct)) {
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
                            if (mentionTmp.acct.equalsIgnoreCase("@"+mentionBooster.acct)) {
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
                        if (!statusReply.spoiler_text.trim().isEmpty()) {
                            statusDraftList.get(0).spoilerChecked = true;
                        }
                    }
                    if (statusReply.language != null && !statusReply.language.isEmpty()) {
                        Set<String> storedLanguages = sharedpreferences.getStringSet(getString(R.string.SET_SELECTED_LANGUAGE), null);
                        if (storedLanguages == null || storedLanguages.isEmpty()) {
                            statusDraftList.get(0).language = statusReply.language;
                        } else {
                            if (storedLanguages.contains(statusReply.language)) {
                                statusDraftList.get(0).language = statusReply.language;
                            } else {
                                String currentCode = sharedpreferences.getString(getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, Locale.getDefault().getLanguage());
                                if (currentCode.isEmpty()) {
                                    currentCode = "EN";
                                }
                                statusDraftList.get(0).language = currentCode;
                            }
                        }
                    }
                    //StatusDraftList at this point should only have one element
                    statusList.addAll(statusDraftList);
                    composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility, editMessageId);
                    composeAdapter.mediaDescriptionCallBack = this;
                    composeAdapter.manageDrafts = this;
                    composeAdapter.promptDraftListener = this;
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
                    binding.recyclerView.setLayoutManager(mLayoutManager);
                    binding.recyclerView.setAdapter(composeAdapter);
                    statusesVM.getContext(currentInstance, BaseMainActivity.currentToken, statusReply.id)
                            .observe(ComposeActivity.this, this::initializeContextView);
                } else if (statusQuoted != null) {
                    statusList.add(statusQuoted);
                    int statusCount = statusList.size();
                    statusDraftList.get(0).quote_id = statusQuoted.id;
                    //StatusDraftList at this point should only have one element
                    statusList.addAll(statusDraftList);
                    composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility, editMessageId);
                    composeAdapter.mediaDescriptionCallBack = this;
                    composeAdapter.manageDrafts = this;
                    composeAdapter.promptDraftListener = this;
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
                    binding.recyclerView.setLayoutManager(mLayoutManager);
                    binding.recyclerView.setAdapter(composeAdapter);
                } else {
                    //Compose without replying
                    statusList.addAll(statusDraftList);
                    composeAdapter = new ComposeAdapter(statusList, 0, account, accountMention, visibility, editMessageId);
                    composeAdapter.mediaDescriptionCallBack = this;
                    composeAdapter.manageDrafts = this;
                    composeAdapter.promptDraftListener = this;
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
                    binding.recyclerView.setLayoutManager(mLayoutManager);
                    binding.recyclerView.setAdapter(composeAdapter);
                    if (statusMention != null) {
                        composeAdapter.loadMentions(statusMention);
                    }
                }
                MastodonHelper.loadPPMastodon(binding.profilePicture, account.mastodon_account);
                ContextCompat.registerReceiver(ComposeActivity.this, imageReceiver, new IntentFilter(Helper.INTENT_SEND_MODIFIED_IMAGE), ContextCompat.RECEIVER_NOT_EXPORTED);
                if (timer != null) {
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            if (promptSaveDraft) {
                                storeDraft(false);
                            }
                        }
                    }, 0, 10000);
                }

                if (sharedAttachments != null && !sharedAttachments.isEmpty()) {
                    for (Attachment attachment : sharedAttachments) {
                        composeAdapter.addAttachment(-1, attachment);
                    }
                } /*else if (sharedUri != null && !sharedUri.toString().startsWith("http")) {
            List<Uri> uris = new ArrayList<>();
            uris.add(sharedUri);
            Helper.createAttachmentFromUri(ComposeActivity.this, uris, attachments -> {
                for(Attachment attachment: attachments) {
                    composeAdapter.addAttachment(-1, attachment);
                }
            });
        } */ else if (shareURL != null) {

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

                getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (binding.recyclerView.getVisibility() == View.VISIBLE) {
                            storeDraftWarning();
                        }
                    }
                });
            };
            mainHandler.post(myRunnable);
        }).start();
    }


    @Override
    public void onItemDraftAdded(int position, String initialContent) {
        Status status = new Status();

        status.id = Helper.generateIdString();
        status.mentions = statusList.get(position - 1).mentions;
        status.visibility = statusList.get(position - 1).visibility;
        if (initialContent != null) {
            status.text = initialContent;
        }
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ComposeActivity.this);
        boolean unlistedReplies = sharedpreferences.getBoolean(getString(R.string.SET_UNLISTED_REPLIES), true);
        if (status.visibility.equalsIgnoreCase("public") && unlistedReplies) {
            status.visibility = "unlisted";
        }
        status.spoiler_text = statusList.get(position - 1).spoiler_text;
        status.sensitive = statusList.get(position - 1).sensitive;
        status.spoilerChecked = statusList.get(position - 1).spoilerChecked;
        statusList.add(status);
        composeAdapter.notifyItemInserted(position);
        composeAdapter.notifyItemRangeChanged(0, statusList.size());
        binding.recyclerView.smoothScrollToPosition(statusList.size());

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
        String scheduledDate = null;
        if(scheduledStatus != null && scheduledStatus.scheduled_at != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(Helper.SCHEDULE_DATE_FORMAT, Locale.getDefault());
            scheduledDate = sdf.format(scheduledStatus.scheduled_at.getTime());
        } else if(statusDraft != null && statusDraft.scheduled_at != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(Helper.SCHEDULE_DATE_FORMAT, Locale.getDefault());
            scheduledDate = sdf.format(statusDraft.scheduled_at.getTime());
        }
        storeDraft(sendMessage, scheduledDate);
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
                    try {
                        new StatusDraft(ComposeActivity.this).removeScheduled(statusDraft);
                        WorkManager.getInstance(ComposeActivity.this).cancelWorkById(statusDraft.workerUuid);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!statusReplies.isEmpty()) {
                statusDraft.statusReplyList = new ArrayList<>();
                statusDraft.statusReplyList.addAll(statusReplies);
            }
            if (!statusDrafts.isEmpty()) {
                statusDraft.statusDraftList = new ArrayList<>();
                statusDraft.statusDraftList.addAll(statusDrafts);
            }
            if (statusDraft.instance == null) {
                statusDraft.instance = account.instance;
            }
            if (statusDraft.user_id == null) {
                statusDraft.user_id = account.user_id;
            }

            if (canBeSent(statusDraft) != 1 && sendMessage) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (canBeSent(statusDraft) == -1) {
                        Toasty.warning(ComposeActivity.this, getString(R.string.toot_error_no_media_description), Toasty.LENGTH_SHORT).show();
                    } else if (canBeSent(statusDraft) == -2) {
                        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this);
                        materialAlertDialogBuilder.setMessage(R.string.toot_error_no_media_description);
                        materialAlertDialogBuilder.setPositiveButton(R.string.send_anyway, (dialog, id) -> {
                            sendMessage(true, scheduledDate);
                            dialog.dismiss();
                        });
                        materialAlertDialogBuilder.setNegativeButton(R.string.add_description, (dialog, id) -> {
                            composeAdapter.openMissingDescription();
                            dialog.cancel();
                        });
                        AlertDialog alert = materialAlertDialogBuilder.create();
                        alert.show();
                    } else {
                        Toasty.info(ComposeActivity.this, getString(R.string.toot_error_no_content), Toasty.LENGTH_SHORT).show();
                    }
                    if (!statusDrafts.isEmpty()) {
                        statusDrafts.get(statusDrafts.size() - 1).submitted = false;
                        composeAdapter.notifyItemChanged(statusList.size() - 1);
                    }
                };
                mainHandler.post(myRunnable);
                return;
            }
            sendMessage(sendMessage, scheduledDate);
        }).start();
    }

    private void sendMessage(boolean sendMessage, String scheduledDate) {
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
                try {
                    new StatusDraft(ComposeActivity.this).updateStatusDraft(statusDraft);
                } catch (DBException e) {
                    e.printStackTrace();
                }
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
                String scheduledStatusId = scheduledStatus!=null&&scheduledStatus.id!=null?scheduledStatus.id:null;
                Data inputData = new Data.Builder()
                        .putString(Helper.ARG_STATUS_DRAFT_ID, String.valueOf(statusDraft.id))
                        .putString(Helper.ARG_INSTANCE, instance)
                        .putString(Helper.ARG_TOKEN, token)
                        .putString(Helper.ARG_EDIT_STATUS_ID, editMessageId)
                        .putString(Helper.ARG_USER_ID, account.user_id)
                        .putString(Helper.ARG_SCHEDULED_ID, scheduledStatusId)
                        .putString(Helper.ARG_SCHEDULED_DATE, scheduledDate).build();
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ComposeWorker.class)
                        .setInputData(inputData)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();
                WorkManager.getInstance(ComposeActivity.this).enqueue(request);

            } else {
                String scheduledStatusId = scheduledStatus!=null&&scheduledStatus.id!=null?scheduledStatus.id:null;
                new ThreadMessageService(ComposeActivity.this, instance, account.user_id, token, statusDraft, scheduledDate, editMessageId, scheduledStatusId);
            }
            finish();
        }
    }


    private int canBeSent(StatusDraft statusDraft) {
        if (statusDraft == null) {
            return 0;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkAlt = sharedpreferences.getBoolean(getString(R.string.SET_MANDATORY_ALT_TEXT), true);
        boolean warnOnly = sharedpreferences.getBoolean(getString(R.string.SET_MANDATORY_ALT_TEXT_WARN), true);
        if (checkAlt) {
            for (Status status : statusDraft.statusDraftList) {
                if (status != null && status.media_attachments != null && status.media_attachments.size() > 0) {
                    for (Attachment attachment : status.media_attachments) {
                        if (attachment.description == null || attachment.description.trim().isEmpty()) {
                            return warnOnly ? -2 : -1;
                        }
                    }
                }
            }
        }
        List<Status> statuses = statusDraft.statusDraftList;
        if (statuses == null || statuses.size() == 0) {
            return 0;
        }
        Status statusCheck = statuses.get(0);
        if (statusCheck == null) {
            return 0;
        }
        return (statusCheck.text != null && statusCheck.text.trim().length() != 0)
                || (statusCheck.media_attachments != null && statusCheck.media_attachments.size() != 0)
                || statusCheck.poll != null
                || (statusCheck.spoiler_text != null && statusCheck.spoiler_text.trim().length() != 0) ? 1 : 0;
    }


    @Override
    public void onContactClick(boolean isChecked, String acct) {
        composeAdapter.updateContent(isChecked, acct);
    }

    @Override
    public void promptDraft() {
        promptSaveDraft = true;
    }

    @Override
    public void click(ComposeAdapter.ComposeViewHolder holder, Attachment attachment, int messagePosition, int mediaPosition) {
        binding.description.setVisibility(View.VISIBLE);
        actionBar.hide();
        binding.recyclerView.setVisibility(View.GONE);
        binding.mediaDescription.setText("");
        String attachmentPath = attachment.local_path != null && !attachment.local_path.trim().isEmpty() ? attachment.local_path : attachment.preview_url;
        Glide.with(binding.mediaPreview.getContext())
                .load(attachmentPath)
                .into(binding.mediaPreview);
        if (attachment.description != null) {
            binding.mediaDescription.setText(attachment.description);
            binding.mediaDescription.setSelection(Objects.requireNonNull(binding.mediaDescription.getText()).length());
        }
        binding.mediaDescription.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1500)});
        binding.mediaDescription.requestFocus();
        Objects.requireNonNull(getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        binding.mediaDescription.requestFocus();

        binding.mediaSave.setOnClickListener(v -> {
            binding.description.setVisibility(View.GONE);
            actionBar.show();
            binding.recyclerView.setVisibility(View.VISIBLE);
            promptSaveDraft = true;
            composeAdapter.openDescriptionActivity(true, Objects.requireNonNull(binding.mediaDescription.getText()).toString().trim(), holder, attachment, messagePosition, mediaPosition);
        });
        binding.mediaCancel.setOnClickListener(v -> {
            binding.description.setVisibility(View.GONE);
            actionBar.show();
            binding.recyclerView.setVisibility(View.VISIBLE);
            composeAdapter.openDescriptionActivity(false, Objects.requireNonNull(binding.mediaDescription.getText()).toString().trim(), holder, attachment, messagePosition, mediaPosition);
        });
    }


    public enum mediaType {
        PHOTO,
        VIDEO,
        AUDIO,
        ALL
    }


}