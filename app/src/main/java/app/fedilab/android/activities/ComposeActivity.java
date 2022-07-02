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
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
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
import app.fedilab.android.client.entities.api.Mention;
import app.fedilab.android.client.entities.api.ScheduledStatus;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.databinding.ActivityPaginationBinding;
import app.fedilab.android.databinding.PopupContactBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.DividerDecorationSimple;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.MediaHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.jobs.ScheduleThreadWorker;
import app.fedilab.android.services.PostMessageService;
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

    private List<Status> statusList;
    private Status statusReply, statusMention;
    private StatusDraft statusDraft;
    private ComposeAdapter composeAdapter;
    private final BroadcastReceiver imageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String imgpath = intent.getStringExtra("imgpath");
            if (imgpath != null) {
                int position = 0;
                for (Status status : statusList) {
                    if (status.media_attachments != null && status.media_attachments.size() > 0) {
                        for (Attachment attachment : status.media_attachments) {
                            if (attachment.local_path.equalsIgnoreCase(imgpath)) {
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
        statusList = new ArrayList<>();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            statusReply = (Status) b.getSerializable(Helper.ARG_STATUS_REPLY);
            statusDraft = (StatusDraft) b.getSerializable(Helper.ARG_STATUS_DRAFT);
            scheduledStatus = (ScheduledStatus) b.getSerializable(Helper.ARG_STATUS_SCHEDULED);
            statusReplyId = b.getString(Helper.ARG_STATUS_REPLY_ID);
            statusMention = (Status) b.getSerializable(Helper.ARG_STATUS_MENTION);
            account = (Account) b.getSerializable(Helper.ARG_ACCOUNT);
            instance = b.getString(Helper.ARG_INSTANCE, null);
            token = b.getString(Helper.ARG_TOKEN, null);
            visibility = b.getString(Helper.ARG_VISIBILITY, null);
            mentionBooster = (app.fedilab.android.client.entities.api.Account) b.getSerializable(Helper.ARG_MENTION_BOOSTER);
            accountMention = (app.fedilab.android.client.entities.api.Account) b.getSerializable(Helper.ARG_ACCOUNT_MENTION);
        }
        binding.toolbar.setPopupTheme(Helper.popupStyle());

        //Edit a scheduled status from server
        if (scheduledStatus != null) {
            statusDraft = new StatusDraft();
            List<Status> statuses = new ArrayList<>();
            Status status = new Status();
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
        StatusesVM statusesVM = new ViewModelProvider(ComposeActivity.this).get(StatusesVM.class);
        //Empty compose
        List<Status> statusDraftList = new ArrayList<>();
        Status status = new Status();
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
            new Thread(() -> {
                if (statusDraft.statusReplyList != null) {
                    statusDraft.statusReplyList = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusDraft.statusReplyList);
                }
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (statusDraft.statusReplyList != null) {
                        statusList.addAll(statusDraft.statusReplyList);
                        binding.recyclerView.addItemDecoration(new DividerDecorationSimple(ComposeActivity.this, statusList));
                    }
                    int statusCount = statusList.size();
                    statusList.addAll(statusDraft.statusDraftList);
                    composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility);
                    composeAdapter.manageDrafts = this;
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
                    binding.recyclerView.setLayoutManager(mLayoutManager);
                    binding.recyclerView.setAdapter(composeAdapter);
                    binding.recyclerView.scrollToPosition(composeAdapter.getItemCount() - 1);
                };
                mainHandler.post(myRunnable);
            }).start();

        } else if (statusReply != null) {
            new Thread(() -> {
                statusReply = SpannableHelper.convertStatus(getApplication().getApplicationContext(), statusReply);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    statusList.add(statusReply);
                    int statusCount = statusList.size();
                    statusDraftList.get(0).in_reply_to_id = statusReply.id;
                    //We change order for mentions
                    //At first place the account that has been mentioned if it's not our
                    statusDraftList.get(0).mentions = new ArrayList<>();
                    if (!statusReply.account.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
                        Mention mention = new Mention();
                        mention.acct = "@" + statusReply.account.acct;
                        mention.url = statusReply.account.url;
                        mention.username = statusReply.account.username;
                        statusDraftList.get(0).mentions.add(mention);
                    }

                    //There are other mentions to
                    if (statusReply.mentions != null && statusReply.mentions.size() > 0) {
                        for (Mention mentionTmp : statusReply.mentions) {
                            if (!mentionTmp.acct.equalsIgnoreCase(statusReply.account.acct) && !mentionTmp.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
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
                    composeAdapter = new ComposeAdapter(statusList, statusCount, account, accountMention, visibility);
                    composeAdapter.manageDrafts = this;
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ComposeActivity.this);
                    binding.recyclerView.setLayoutManager(mLayoutManager);
                    binding.recyclerView.setAdapter(composeAdapter);
                    statusesVM.getContext(currentInstance, BaseMainActivity.currentToken, statusReply.id)
                            .observe(ComposeActivity.this, this::initializeContextView);
                };
                mainHandler.post(myRunnable);
            }).start();
        } else {
            //Compose without replying
            statusList.addAll(statusDraftList);
            composeAdapter = new ComposeAdapter(statusList, 0, account, accountMention, visibility);
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

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                storeDraft(false);
            }
        }, 0, 10000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        composeAdapter.notifyItemChanged(context.ancestors.size() + 1);
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
        composeAdapter = new ComposeAdapter(statusList, context.ancestors.size(), account, accountMention, visibility);
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
    public void onItemDraftAdded(int position) {
        Status status = new Status();
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
                if (status.id == null) {
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
            statusDraft.statusReplyList = statusReplies;
            statusDraft.statusDraftList = statusDrafts;
            statusDraft.instance = account.instance;
            statusDraft.user_id = account.user_id;

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
                    Intent intent = new Intent(ComposeActivity.this, PostMessageService.class);
                    intent.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
                    intent.putExtra(Helper.ARG_INSTANCE, instance);
                    intent.putExtra(Helper.ARG_TOKEN, token);
                    intent.putExtra(Helper.ARG_SCHEDULED_DATE, scheduledDate);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                } else {
                    new ThreadMessageService(ComposeActivity.this, instance, token, statusDraft, scheduledDate);
                }
                finish();
            }

        }).start();
    }


    private boolean canBeSent(StatusDraft statusDraft) {
        if (statusDraft == null || statusDraft.statusDraftList == null || statusDraft.statusDraftList.size() == 0) {
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