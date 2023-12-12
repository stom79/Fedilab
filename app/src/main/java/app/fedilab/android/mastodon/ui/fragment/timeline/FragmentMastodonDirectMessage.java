package app.fedilab.android.mastodon.ui.fragment.timeline;
/* Copyright 2023 Thomas Schneider
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
import static app.fedilab.android.BaseMainActivity.currentToken;
import static app.fedilab.android.BaseMainActivity.currentUserID;
import static app.fedilab.android.BaseMainActivity.instanceInfo;
import static app.fedilab.android.mastodon.activities.ComposeActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ComposeAttachmentItemBinding;
import app.fedilab.android.databinding.ComposePollBinding;
import app.fedilab.android.databinding.ComposePollItemBinding;
import app.fedilab.android.databinding.FragmentDirectMessageBinding;
import app.fedilab.android.databinding.PopupMediaDescriptionBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Context;
import app.fedilab.android.mastodon.client.entities.api.Mention;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.imageeditor.EditImageActivity;
import app.fedilab.android.mastodon.jobs.ComposeWorker;
import app.fedilab.android.mastodon.services.ThreadMessageService;
import app.fedilab.android.mastodon.ui.drawer.StatusDirectMessageAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;


public class FragmentMastodonDirectMessage extends Fragment {


    public FirstMessage firstMessage;
    private FragmentDirectMessageBinding binding;
    private StatusesVM statusesVM;
    private List<Status> statuses;
    private StatusDirectMessageAdapter statusDirectMessageAdapter;
    //Handle actions that can be done in other fragments
    private Status focusedStatus;
    private Status firstStatus;
    private boolean pullToRefresh;
    private String user_token, user_instance;
    private Status statusCompose;
    private final BroadcastReceiver broadcast_data = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {

                if (b.getBoolean(Helper.RECEIVE_NEW_MESSAGE, false)) {
                    Status statusReceived = (Status) b.getSerializable(Helper.RECEIVE_STATUS_ACTION);
                    if (statusReceived != null) {
                        statuses.add(statusReceived);
                        statusDirectMessageAdapter.notifyItemInserted(statuses.size() - 1);
                        initiliazeStatus();
                    }
                }
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        focusedStatus = null;
        pullToRefresh = false;
        if (getArguments() != null) {
            focusedStatus = (Status) getArguments().getSerializable(Helper.ARG_STATUS);
        }
        user_instance = MainActivity.currentInstance;
        user_token = MainActivity.currentToken;

        if (focusedStatus == null) {
            getChildFragmentManager().beginTransaction().remove(this).commit();
        }
        binding = FragmentDirectMessageBinding.inflate(inflater, container, false);
        statusesVM = new ViewModelProvider(FragmentMastodonDirectMessage.this).get(StatusesVM.class);
        binding.recyclerView.setNestedScrollingEnabled(true);
        this.statuses = new ArrayList<>();
        this.statuses.add(focusedStatus);
        statusDirectMessageAdapter = new StatusDirectMessageAdapter(this.statuses);
        binding.swipeContainer.setRefreshing(false);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(statusDirectMessageAdapter);
        binding.swipeContainer.setOnRefreshListener(() -> {
            if (this.statuses.size() > 0) {
                binding.swipeContainer.setRefreshing(true);
                pullToRefresh = true;
                statusesVM.getContext(user_instance, user_token, focusedStatus.id)
                        .observe(getViewLifecycleOwner(), this::initializeContextView);
            }
        });
        if (focusedStatus != null) {
            statusesVM.getContext(user_instance, user_token, focusedStatus.id)
                    .observe(getViewLifecycleOwner(), this::initializeContextView);
        }
        binding.buttonCloseAttachmentPanel.setOnClickListener(v -> binding.attachmentChoicesPanel.setVisibility(View.GONE));
        statusCompose = new Status();
        binding.buttonAttach.setOnClickListener(v -> {

            if (instanceInfo.configuration.media_attachments.supported_mime_types != null) {
                if (instanceInfo.getMimeTypeAudio().size() == 0) {
                    binding.buttonAttachAudio.setEnabled(false);
                }
                if (instanceInfo.getMimeTypeImage().size() == 0) {
                    binding.buttonAttachImage.setEnabled(false);
                }
                if (instanceInfo.getMimeTypeVideo().size() == 0) {
                    binding.buttonAttachVideo.setEnabled(false);
                }
                if (instanceInfo.getMimeTypeOther().size() == 0) {
                    binding.buttonAttachManual.setEnabled(false);
                }
            }
            binding.attachmentChoicesPanel.setVisibility(View.VISIBLE);
            binding.buttonAttach.setChecked(false);
        });
        binding.buttonPoll.setOnClickListener(v -> displayPollPopup());
        binding.buttonAttachAudio.setOnClickListener(v -> {
            binding.attachmentChoicesPanel.setVisibility(View.GONE);
            pickupMedia(ComposeActivity.mediaType.AUDIO);
        });
        binding.buttonAttachImage.setOnClickListener(v -> {
            binding.attachmentChoicesPanel.setVisibility(View.GONE);
            pickupMedia(ComposeActivity.mediaType.PHOTO);
        });
        binding.buttonAttachVideo.setOnClickListener(v -> {
            binding.attachmentChoicesPanel.setVisibility(View.GONE);
            pickupMedia(ComposeActivity.mediaType.VIDEO);
        });
        binding.buttonAttachManual.setOnClickListener(v -> {
            binding.attachmentChoicesPanel.setVisibility(View.GONE);
            pickupMedia(ComposeActivity.mediaType.ALL);
        });

        binding.sendButton.setOnClickListener(v -> {
            statusCompose.submitted = true;
            statusCompose.text = binding.text.getText().toString();
            onSubmit(prepareDraft(statusCompose, MainActivity.currentInstance, MainActivity.currentUserID));
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().registerReceiver(broadcast_data, new IntentFilter(Helper.BROADCAST_DATA), android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(broadcast_data, new IntentFilter(Helper.BROADCAST_DATA));
        }
        binding.text.setKeyBoardInputCallbackListener((inputContentInfo, flags, opts) -> {
            if (inputContentInfo != null) {
                Uri uri = inputContentInfo.getContentUri();
                List<Uri> uris = new ArrayList<>();
                uris.add(uri);
                addAttachment(uris);
            }
        });

        return binding.getRoot();
    }


    private void onSubmit(StatusDraft statusDraft) {
        new Thread(() -> {
            if (statusDraft.instance == null) {
                statusDraft.instance = currentInstance;
            }
            if (statusDraft.user_id == null) {
                statusDraft.user_id = currentUserID;
            }

            if (!canBeSent(statusDraft)) {
                return;
            }
            if (statusDraft.id > 0) {
                try {
                    new StatusDraft(requireActivity()).updateStatusDraft(statusDraft);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    statusDraft.id = new StatusDraft(requireActivity()).insertStatusDraft(statusDraft);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }
            int mediaCount = 0;
            for (Status status : statusDraft.statusDraftList) {
                mediaCount += status.media_attachments != null ? status.media_attachments.size() : 0;
            }
            if (mediaCount > 0) {
                Data inputData = new Data.Builder()
                        .putString(Helper.ARG_STATUS_DRAFT_ID, String.valueOf(statusDraft.id))
                        .putString(Helper.ARG_INSTANCE, currentInstance)
                        .putString(Helper.ARG_TOKEN, currentToken)
                        .putString(Helper.ARG_USER_ID, currentUserID)
                        .build();
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ComposeWorker.class)
                        .setInputData(inputData)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();
                WorkManager.getInstance(requireActivity()).enqueue(request);

            } else {
                new ThreadMessageService(requireActivity(), currentInstance, currentUserID, currentToken, statusDraft, null, null);
            }
        }).start();
    }


    @Override
    public void onDestroyView() {
        requireActivity().unregisterReceiver(broadcast_data);
        super.onDestroyView();
    }


    private boolean canBeSent(StatusDraft statusDraft) {
        if (statusDraft == null) {
            return false;
        }
        List<Status> statuses = statusDraft.statusDraftList;
        if (statuses == null || statuses.size() == 0) {
            return false;
        }
        Status statusCheck = statuses.get(0);
        if (statusCheck == null) {
            return false;
        }
        return (statusCheck.text != null && statusCheck.text.trim().length() != 0)
                || (statusCheck.media_attachments != null && statusCheck.media_attachments.size() != 0)
                || statusCheck.poll != null
                || (statusCheck.spoiler_text != null && statusCheck.spoiler_text.trim().length() != 0);
    }

    public StatusDraft prepareDraft(Status status, String instance, String user_id) {
        //Collect all statusCompose
        List<Status> statusDrafts = new ArrayList<>();
        statusDrafts.add(status);
        StatusDraft statusDraftDB = new StatusDraft();
        statusDraftDB.statusReplyList = new ArrayList<>();
        statusDraftDB.statusReplyList.addAll(statuses);
        statusDraftDB.statusDraftList = new ArrayList<>();
        statusDraftDB.statusDraftList.addAll(statusDrafts);
        statusDraftDB.instance = instance;
        statusDraftDB.user_id = user_id;
        return statusDraftDB;
    }

    /**
     * Display the popup to attach a poll to message
     */
    private void displayPollPopup() {
        AlertDialog.Builder alertPoll = new MaterialAlertDialogBuilder(requireActivity());
        alertPoll.setTitle(R.string.create_poll);
        ComposePollBinding composePollBinding = ComposePollBinding.inflate(LayoutInflater.from(requireActivity()), new LinearLayout(requireActivity()), false);
        alertPoll.setView(composePollBinding.getRoot());
        int max_entry = 4;
        int max_length = 50;
        final int[] pollCountItem = {2};

        if (instanceInfo != null && instanceInfo.configuration != null && instanceInfo.configuration.pollsConf != null) {
            max_entry = instanceInfo.configuration.pollsConf.max_options;
            max_length = instanceInfo.configuration.pollsConf.max_option_chars;
        } else if (instanceInfo != null && instanceInfo.poll_limits != null) {
            max_entry = instanceInfo.poll_limits.max_options;
            max_length = instanceInfo.poll_limits.max_option_chars;
        }
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(max_length);
        composePollBinding.option1.text.setFilters(fArray);
        composePollBinding.option1.textLayout.setHint(getString(R.string.poll_choice_s, 1));
        composePollBinding.option2.text.setFilters(fArray);
        composePollBinding.option2.textLayout.setHint(getString(R.string.poll_choice_s, 2));
        composePollBinding.option1.buttonRemove.setVisibility(View.GONE);
        composePollBinding.option2.buttonRemove.setVisibility(View.GONE);
        int finalMax_entry = max_entry;
        composePollBinding.buttonAddOption.setOnClickListener(v -> {
            if (pollCountItem[0] < finalMax_entry) {
                ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(composePollBinding.optionsList.getContext()), composePollBinding.optionsList, false);
                if (composePollBinding.pollType.getCheckedButtonId() == R.id.poll_type_multiple)
                    composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                composePollItemBinding.text.setFilters(fArray);
                composePollItemBinding.textLayout.setHint(getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
                LinearLayoutCompat viewItem = composePollItemBinding.getRoot();
                composePollBinding.optionsList.addView(composePollItemBinding.getRoot());
                composePollItemBinding.buttonRemove.setOnClickListener(view -> {
                    composePollBinding.optionsList.removeView(viewItem);
                    pollCountItem[0]--;
                    if (pollCountItem[0] >= finalMax_entry) {
                        composePollBinding.buttonAddOption.setVisibility(View.GONE);
                    } else {
                        composePollBinding.buttonAddOption.setVisibility(View.VISIBLE);
                    }
                    int childCount = composePollBinding.optionsList.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        AppCompatEditText title = (composePollBinding.optionsList.getChildAt(i)).findViewById(R.id.text);
                        title.setHint(getString(R.string.poll_choice_s, i + 1));
                    }

                });
            }
            pollCountItem[0]++;
            if (pollCountItem[0] >= finalMax_entry) {
                composePollBinding.buttonAddOption.setVisibility(View.GONE);
            } else {
                composePollBinding.buttonAddOption.setVisibility(View.VISIBLE);
            }

        });


        ArrayAdapter<CharSequence> pollduration = ArrayAdapter.createFromResource(requireActivity(),
                R.array.poll_duration, android.R.layout.simple_spinner_dropdown_item);
        composePollBinding.pollDuration.setAdapter(pollduration);
        composePollBinding.pollDuration.setSelection(4);
        if (statusCompose != null && statusCompose.poll != null && statusCompose.poll.options != null) {
            int i = 1;
            for (Poll.PollItem pollItem : statusCompose.poll.options) {
                if (i == 1) {
                    if (statusCompose.poll.multiple)
                        composePollBinding.option1.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    if (pollItem.title != null)
                        composePollBinding.option1.text.setText(pollItem.title);
                } else if (i == 2) {
                    if (statusCompose.poll.multiple)
                        composePollBinding.option2.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    if (pollItem.title != null)
                        composePollBinding.option2.text.setText(pollItem.title);
                } else {

                    ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(requireActivity()), new LinearLayout(requireActivity()), false);
                    if (composePollBinding.pollType.getCheckedButtonId() == R.id.poll_type_multiple)
                        composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    else
                        composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_single);

                    composePollItemBinding.text.setFilters(fArray);
                    composePollItemBinding.textLayout.setHint(getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
                    composePollItemBinding.text.setText(pollItem.title);
                    composePollBinding.optionsList.addView(composePollItemBinding.getRoot());
                    composePollItemBinding.buttonRemove.setOnClickListener(view -> {
                        composePollBinding.optionsList.removeView(view);
                        pollCountItem[0]--;
                    });
                    pollCountItem[0]++;
                }
                i++;
            }
            if (statusCompose.poll.options.size() >= max_entry) {
                composePollBinding.buttonAddOption.setVisibility(View.GONE);
            }
            switch (statusCompose.poll.expire_in) {
                case 300:
                    composePollBinding.pollDuration.setSelection(0);
                    break;
                case 1800:
                    composePollBinding.pollDuration.setSelection(1);
                    break;
                case 3600:
                    composePollBinding.pollDuration.setSelection(2);
                    break;
                case 21600:
                    composePollBinding.pollDuration.setSelection(3);
                    break;
                case 86400:
                    composePollBinding.pollDuration.setSelection(4);
                    break;
                case 259200:
                    composePollBinding.pollDuration.setSelection(5);
                    break;
                case 604800:
                    composePollBinding.pollDuration.setSelection(6);
                    break;
            }
            if (statusCompose.poll.multiple)
                composePollBinding.pollType.check(R.id.poll_type_multiple);
            else
                composePollBinding.pollType.check(R.id.poll_type_single);


        }
        alertPoll.setNegativeButton(R.string.delete, (dialog, whichButton) -> {
            if (statusCompose != null && statusCompose.poll != null) statusCompose.poll = null;
            buttonState();
            dialog.dismiss();
        });
        alertPoll.setPositiveButton(R.string.save, null);
        final AlertDialog alertPollDiaslog = alertPoll.create();
        alertPollDiaslog.setOnShowListener(dialog -> {
            composePollBinding.pollType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.poll_type_single) {
                        if (statusCompose != null && statusCompose.poll != null)
                            statusCompose.poll.multiple = false;
                        for (int i = 0; i < composePollBinding.optionsList.getChildCount(); i++) {
                            ComposePollItemBinding child = ComposePollItemBinding.bind(composePollBinding.optionsList.getChildAt(i));
                            child.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_single);
                        }
                    } else if (checkedId == R.id.poll_type_multiple) {
                        if (statusCompose != null && statusCompose.poll != null)
                            statusCompose.poll.multiple = true;
                        for (int i = 0; i < composePollBinding.optionsList.getChildCount(); i++) {
                            ComposePollItemBinding child = ComposePollItemBinding.bind(composePollBinding.optionsList.getChildAt(i));
                            child.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                        }
                    }
                }
            });
            Button b = alertPollDiaslog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view1 -> {
                int poll_duration_pos = composePollBinding.pollDuration.getSelectedItemPosition();

                int selected_poll_type_id = composePollBinding.pollType.getCheckedButtonId();
                String choice1 = composePollBinding.option1.text.getText().toString().trim();
                String choice2 = composePollBinding.option2.text.getText().toString().trim();

                if (choice1.isEmpty() && choice2.isEmpty()) {
                    Toasty.error(requireActivity(), getString(R.string.poll_invalid_choices), Toasty.LENGTH_SHORT).show();
                } else if (statusCompose != null) {
                    statusCompose.poll = new Poll();
                    statusCompose.poll.multiple = selected_poll_type_id == R.id.poll_type_multiple;
                    int expire;
                    switch (poll_duration_pos) {
                        case 0:
                            expire = 300;
                            break;
                        case 1:
                            expire = 1800;
                            break;
                        case 2:
                            expire = 3600;
                            break;
                        case 3:
                            expire = 21600;
                            break;
                        case 4:
                            expire = 86400;
                            break;
                        case 5:
                            expire = 259200;
                            break;
                        case 6:
                            expire = 604800;
                            break;
                        default:
                            expire = 864000;
                    }
                    statusCompose.poll.expire_in = expire;
                    List<Poll.PollItem> pollItems = new ArrayList<>();
                    int childCount = composePollBinding.optionsList.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        Poll.PollItem pollItem = new Poll.PollItem();
                        AppCompatEditText title = (composePollBinding.optionsList.getChildAt(i)).findViewById(R.id.text);
                        pollItem.title = title.getText().toString();
                        pollItems.add(pollItem);
                    }
                    List<String> options = new ArrayList<>();
                    boolean doubleTitle = false;
                    for (Poll.PollItem po : pollItems) {
                        if (!options.contains(po.title.trim())) {
                            options.add(po.title.trim());
                        } else {
                            doubleTitle = true;
                        }
                    }
                    if (!doubleTitle) {
                        statusCompose.poll.options = pollItems;
                        dialog.dismiss();
                    } else {
                        Toasty.error(requireActivity(), getString(R.string.poll_duplicated_entry), Toasty.LENGTH_SHORT).show();
                    }
                }
                binding.buttonPoll.setVisibility(View.VISIBLE);
                buttonState();
            });
        });

        alertPollDiaslog.show();
    }


    private void initiliazeStatus() {
        statusCompose = new Status();
        binding.text.setText("");
        binding.attachmentsList.removeAllViews();
        if (statuses != null && statuses.size() > 0) {
            binding.recyclerView.scrollToPosition(statuses.size() - 1);
            Status lastStatus = statuses.get(statuses.size() - 1);
            statusCompose.in_reply_to_id = lastStatus.id;
            statusCompose.visibility = "direct";
            statusCompose.mentions = new ArrayList<>();
            if (lastStatus.account.acct != null && currentAccount.mastodon_account != null && !lastStatus.account.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
                Mention mention = new Mention();
                mention.acct = "@" + lastStatus.account.acct;
                mention.url = lastStatus.account.url;
                mention.username = lastStatus.account.username;
                statusCompose.mentions.add(mention);
            }
            //There are other mentions to
            if (lastStatus.mentions != null && lastStatus.mentions.size() > 0) {
                for (Mention mentionTmp : lastStatus.mentions) {
                    if (currentAccount.mastodon_account != null && !mentionTmp.acct.equalsIgnoreCase(currentAccount.mastodon_account.acct)) {
                        statusCompose.mentions.add(mentionTmp);
                    }
                }
            }
        }
        manageMentions(statusCompose);
    }

    /**
     * Manage mentions displayed when replying to a message
     *
     * @param statusCompose {@link Status} - Status that user is replying
     */
    private void manageMentions(Status statusCompose) {

        if (statusCompose.mentions != null && (statusCompose.text == null || statusCompose.text.length() == 0) && statusCompose.mentions.size() > 0) {
            //Retrieves mentioned accounts + OP and adds them at the beginin of the toot
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            Mention inReplyToUser;
            inReplyToUser = statusCompose.mentions.get(0);
            if (statusCompose.text == null) {
                statusCompose.text = "";
            }
            //Put other accounts mentioned at the bottom
            boolean capitalize = sharedpreferences.getBoolean(getString(R.string.SET_CAPITALIZE), true);
            boolean mentionsAtTop = sharedpreferences.getBoolean(getString(R.string.SET_MENTIONS_AT_TOP), false);

            if (inReplyToUser != null) {
                if (capitalize && !mentionsAtTop) {
                    statusCompose.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + "\n" : "@" + inReplyToUser.acct + "\n";
                } else {
                    statusCompose.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + " " : "@" + inReplyToUser.acct + " ";
                }
            }
            binding.text.setText(statusCompose.text);
            if (statusCompose.mentions.size() > 1) {
                if (!mentionsAtTop) {
                    statusCompose.text += "\n";
                }
                for (int i = 1; i < statusCompose.mentions.size(); i++) {
                    String tootTemp = String.format("@%s ", statusCompose.mentions.get(i).acct);
                    statusCompose.text = String.format("%s ", (statusCompose.text + tootTemp.trim()));
                }
            }
            binding.text.setText(statusCompose.text);
            binding.text.requestFocus();
            binding.text.post(() -> {
                binding.text.setSelection(statusCompose.text.length()); //Put cursor at the end
            });
        } else {
            binding.text.requestFocus();
        }

    }

    /**
     * Manage the actions when picking up a media
     *
     * @param type - type of media in the list of {@link ComposeActivity.mediaType}
     */
    private void pickupMedia(ComposeActivity.mediaType type) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }
        }
        Intent intent;
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        String[] mimetypes = new String[0];
        if (type == ComposeActivity.mediaType.PHOTO) {
            if (instanceInfo != null && instanceInfo.getMimeTypeImage() != null && instanceInfo.getMimeTypeImage().size() > 0) {
                mimetypes = instanceInfo.getMimeTypeImage().toArray(new String[0]);
            } else {
                mimetypes = new String[]{"image/*"};
            }
        } else if (type == ComposeActivity.mediaType.VIDEO) {
            if (instanceInfo != null && instanceInfo.getMimeTypeVideo() != null && instanceInfo.getMimeTypeVideo().size() > 0) {
                mimetypes = instanceInfo.getMimeTypeVideo().toArray(new String[0]);
            } else {
                mimetypes = new String[]{"video/*"};
            }
        } else if (type == ComposeActivity.mediaType.AUDIO) {
            if (instanceInfo != null && instanceInfo.getMimeTypeAudio() != null && instanceInfo.getMimeTypeAudio().size() > 0) {
                mimetypes = instanceInfo.getMimeTypeAudio().toArray(new String[0]);
            } else {
                mimetypes = new String[]{"audio/mpeg", "audio/opus", "audio/flac", "audio/wav", "audio/ogg"};
            }
        } else if (type == ComposeActivity.mediaType.ALL) {
            if (instanceInfo != null && instanceInfo.getMimeTypeOther() != null && instanceInfo.getMimeTypeOther().size() > 0) {
                mimetypes = instanceInfo.getMimeTypeOther().toArray(new String[0]);
            } else {
                mimetypes = new String[]{"*/*"};
            }
        }
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        requireActivity().startActivityForResult(intent, (ComposeActivity.PICK_MEDIA));
    }


    /**
     * Add an attachment from ComposeActivity
     *
     * @param uris List<Uri> - uris of the media
     */
    public void addAttachment(List<Uri> uris) {
        Helper.createAttachmentFromUri(requireActivity(), uris, attachments -> {
            if (statusCompose.media_attachments == null) {
                statusCompose.media_attachments = new ArrayList<>();
            }
            statusCompose.media_attachments.addAll(attachments);
            if (statusCompose.media_attachments.size() > 0) {
                displayAttachments(statusCompose.media_attachments.size() - 1);
            }
        });
    }

    /**
     * Display attachment for a holder
     */
    private void displayAttachments(int scrollToMediaPosition) {
        if (statusCompose != null && statusCompose.media_attachments != null) {
            binding.attachmentsList.removeAllViews();
            List<Attachment> attachmentList = statusCompose.media_attachments;
            if (attachmentList != null && attachmentList.size() > 0) {
                int mediaPosition = 0;
                for (Attachment attachment : attachmentList) {
                    ComposeAttachmentItemBinding composeAttachmentItemBinding = ComposeAttachmentItemBinding.inflate(LayoutInflater.from(requireActivity()), binding.attachmentsList, false);
                    composeAttachmentItemBinding.buttonPlay.setVisibility(View.GONE);
                   /* if (editMessageId != null && attachment.url != null) {
                        composeAttachmentItemBinding.editPreview.setVisibility(View.GONE);
                        composeAttachmentItemBinding.buttonDescription.setVisibility(View.INVISIBLE);
                        composeAttachmentItemBinding.buttonOrderDown.setVisibility(View.INVISIBLE);
                        composeAttachmentItemBinding.buttonOrderUp.setVisibility(View.INVISIBLE);
                    }*/
                    String attachmentPath = attachment.local_path != null && !attachment.local_path.trim().isEmpty() ? attachment.local_path : attachment.preview_url;
                    if (attachment.type != null || attachment.mimeType != null) {
                        if ((attachment.type != null && attachment.type.toLowerCase().startsWith("image")) || (attachment.mimeType != null && attachment.mimeType.toLowerCase().startsWith("image"))) {
                            Glide.with(composeAttachmentItemBinding.preview.getContext())
                                    .load(attachmentPath)
                                    //.diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(composeAttachmentItemBinding.preview);
                        } else if ((attachment.type != null && attachment.type.toLowerCase().startsWith("video")) || (attachment.mimeType != null && attachment.mimeType.toLowerCase().startsWith("video"))) {
                            composeAttachmentItemBinding.buttonPlay.setVisibility(View.VISIBLE);
                            long interval = 2000;
                            RequestOptions options = new RequestOptions().frame(interval);
                            Glide.with(composeAttachmentItemBinding.preview.getContext()).asBitmap()
                                    .load(attachmentPath)
                                    .apply(options)
                                    .into(composeAttachmentItemBinding.preview);
                        } else if ((attachment.type != null && attachment.type.toLowerCase().startsWith("audio")) || (attachment.mimeType != null && attachment.mimeType.toLowerCase().startsWith("audio"))) {
                            Glide.with(composeAttachmentItemBinding.preview.getContext())
                                    .load(R.drawable.ic_baseline_audio_file_24)
                                    .into(composeAttachmentItemBinding.preview);
                        } else {
                            Glide.with(composeAttachmentItemBinding.preview.getContext())
                                    .load(R.drawable.ic_baseline_insert_drive_file_24)
                                    .into(composeAttachmentItemBinding.preview);
                        }
                    } else {
                        Glide.with(composeAttachmentItemBinding.preview.getContext())
                                .load(R.drawable.ic_baseline_insert_drive_file_24)
                                .into(composeAttachmentItemBinding.preview);
                    }
                    if (mediaPosition == 0) {
                        composeAttachmentItemBinding.buttonOrderUp.setVisibility(View.INVISIBLE);
                    } else {
                        composeAttachmentItemBinding.buttonOrderUp.setVisibility(View.VISIBLE);
                    }
                    if (mediaPosition == attachmentList.size() - 1) {
                        composeAttachmentItemBinding.buttonOrderDown.setVisibility(View.INVISIBLE);
                    } else {
                        composeAttachmentItemBinding.buttonOrderDown.setVisibility(View.VISIBLE);
                    }
                    //Remote attachments when deleting/redrafting can't be ordered
                    if (attachment.local_path == null) {
                        composeAttachmentItemBinding.buttonOrderUp.setVisibility(View.INVISIBLE);
                        composeAttachmentItemBinding.buttonOrderDown.setVisibility(View.INVISIBLE);
                    }
                    int finalMediaPosition = mediaPosition;
                    if (attachment.local_path != null && (attachment.local_path.endsWith("png") || attachment.local_path.endsWith("jpg") || attachment.local_path.endsWith("jpeg"))) {
                        composeAttachmentItemBinding.editPreview.setVisibility(View.VISIBLE);
                    } else {
                        composeAttachmentItemBinding.editPreview.setVisibility(View.GONE);
                    }
                    composeAttachmentItemBinding.editPreview.setOnClickListener(v -> {
                        Intent intent = new Intent(requireActivity(), EditImageActivity.class);
                        Bundle b = new Bundle();
                        intent.putExtra("imageUri", attachment.local_path);
                        intent.putExtras(b);
                        startActivity(intent);
                    });
                    composeAttachmentItemBinding.buttonDescription.setOnClickListener(v -> {
                        AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(requireActivity());
                        // builderInner.setTitle(R.string.upload_form_description);
                        PopupMediaDescriptionBinding popupMediaDescriptionBinding = PopupMediaDescriptionBinding.inflate(LayoutInflater.from(requireActivity()), null, false);
                        builderInner.setView(popupMediaDescriptionBinding.getRoot());

                        popupMediaDescriptionBinding.mediaDescription.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1500)});
                        popupMediaDescriptionBinding.mediaDescription.requestFocus();
                        Glide.with(popupMediaDescriptionBinding.mediaPicture.getContext())
                                .asBitmap()
                                .load(attachmentPath)
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                        popupMediaDescriptionBinding.mediaPicture.setImageBitmap(resource);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }

                                    @Override
                                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                        super.onLoadFailed(errorDrawable);
                                    }
                                });
                        builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        if (attachment.description != null) {
                            popupMediaDescriptionBinding.mediaDescription.setText(attachment.description);
                            popupMediaDescriptionBinding.mediaDescription.setSelection(popupMediaDescriptionBinding.mediaDescription.getText().length());
                        }
                        builderInner.setPositiveButton(R.string.validate, (dialog, which) -> {
                            attachment.description = popupMediaDescriptionBinding.mediaDescription.getText().toString();
                            displayAttachments(finalMediaPosition);
                            dialog.dismiss();
                        });
                        AlertDialog alertDialog = builderInner.create();
                        Objects.requireNonNull(alertDialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        alertDialog.show();
                        popupMediaDescriptionBinding.mediaDescription.requestFocus();

                    });

                    composeAttachmentItemBinding.buttonOrderUp.setOnClickListener(v -> {
                        if (finalMediaPosition > 0 && attachmentList.size() > 1) {
                            Attachment at1 = attachmentList.get(finalMediaPosition);
                            Attachment at2 = attachmentList.get(finalMediaPosition - 1);
                            attachmentList.set(finalMediaPosition - 1, at1);
                            attachmentList.set(finalMediaPosition, at2);
                            displayAttachments(finalMediaPosition - 1);
                        }
                    });
                    composeAttachmentItemBinding.buttonOrderDown.setOnClickListener(v -> {
                        if (finalMediaPosition < (attachmentList.size() - 1) && attachmentList.size() > 1) {
                            Attachment at1 = attachmentList.get(finalMediaPosition);
                            Attachment at2 = attachmentList.get(finalMediaPosition + 1);
                            attachmentList.set(finalMediaPosition, at2);
                            attachmentList.set(finalMediaPosition + 1, at1);
                            displayAttachments(finalMediaPosition + 1);
                        }
                    });
                    composeAttachmentItemBinding.buttonRemove.setOnClickListener(v -> {
                        AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(requireActivity());
                        builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        builderInner.setPositiveButton(R.string.delete, (dialog, which) -> {
                            attachmentList.remove(attachment);
                            displayAttachments(finalMediaPosition);
                            new Thread(() -> {
                                if (attachment.local_path != null) {
                                    File fileToDelete = new File(attachment.local_path);
                                    if (fileToDelete.exists()) {
                                        //noinspection ResultOfMethodCallIgnored
                                        fileToDelete.delete();
                                    }
                                }
                            }).start();

                        });
                        builderInner.setMessage(R.string.toot_delete_media);
                        builderInner.show();
                    });
                    composeAttachmentItemBinding.preview.setOnClickListener(v -> displayAttachments(finalMediaPosition));
                    if (attachment.description == null || attachment.description.trim().isEmpty()) {
                        composeAttachmentItemBinding.buttonDescription.setChipIconResource(R.drawable.ic_baseline_warning_24);
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black));
                        composeAttachmentItemBinding.buttonDescription.setChipIconTintResource(R.color.black);
                        composeAttachmentItemBinding.buttonDescription.setChipBackgroundColor(ThemeHelper.getNoDescriptionColorStateList(requireActivity()));
                    } else {
                        composeAttachmentItemBinding.buttonDescription.setChipIconResource(R.drawable.ic_baseline_check_circle_24);
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white));
                        composeAttachmentItemBinding.buttonDescription.setChipIconTintResource(R.color.white);
                        composeAttachmentItemBinding.buttonDescription.setChipBackgroundColor(ThemeHelper.getHavingDescriptionColorStateList(requireActivity()));
                    }
                    binding.attachmentsList.addView(composeAttachmentItemBinding.getRoot());
                    mediaPosition++;
                }
                binding.attachmentsList.setVisibility(View.VISIBLE);
                if (scrollToMediaPosition >= 0 && binding.attachmentsList.getChildCount() < scrollToMediaPosition) {
                    binding.attachmentsList.requestChildFocus(binding.attachmentsList.getChildAt(scrollToMediaPosition), binding.attachmentsList.getChildAt(scrollToMediaPosition));
                }
            } else {
                binding.attachmentsList.setVisibility(View.GONE);
            }
        } else {
            binding.attachmentsList.setVisibility(View.GONE);
        }
        buttonState();
    }


    /**
     * Manage state of media and poll button
     */
    private void buttonState() {
        if (BaseMainActivity.software == null || BaseMainActivity.software.toUpperCase().compareTo("MASTODON") == 0) {
            if (statusCompose.poll == null) {
                binding.buttonAttachImage.setEnabled(true);
                binding.buttonAttachVideo.setEnabled(true);
                binding.buttonAttachAudio.setEnabled(true);
                binding.buttonAttachManual.setEnabled(true);
            } else {
                binding.buttonAttachImage.setEnabled(false);
                binding.buttonAttachVideo.setEnabled(false);
                binding.buttonAttachAudio.setEnabled(false);
                binding.buttonAttachManual.setEnabled(false);
                binding.buttonPoll.setEnabled(true);
            }
            binding.buttonPoll.setEnabled(statusCompose.media_attachments == null || statusCompose.media_attachments.size() <= 0);
        }
    }

    /**
     * Intialize the common view for the context
     *
     * @param context {@link Context}
     */
    private void initializeContextView(final Context context) {

        if (context == null) {
            Helper.sendToastMessage(requireActivity(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
            return;
        }
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        if (pullToRefresh) {
            pullToRefresh = false;
            int size = this.statuses.size();
            statuses.clear();
            statusDirectMessageAdapter.notifyItemRangeRemoved(0, size);
            statuses.add(focusedStatus);
        }
        if (context.ancestors.size() > 0) {
            firstStatus = context.ancestors.get(0);
        } else {
            firstStatus = statuses.get(0);
        }
        if (firstMessage != null) {
            firstMessage.get(firstStatus);
        }

        int statusPosition = context.ancestors.size();
        //Build the array of statuses
        statuses.addAll(0, context.ancestors);
        statusDirectMessageAdapter.notifyItemRangeInserted(0, statusPosition);
        statuses.addAll(statusPosition + 1, context.descendants);
        statusDirectMessageAdapter.notifyItemRangeInserted(statusPosition + 1, context.descendants.size());
        binding.swipeContainer.setRefreshing(false);
        initiliazeStatus();
    }

    public interface FirstMessage {
        void get(Status status);
    }
}