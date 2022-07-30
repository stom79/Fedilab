package app.fedilab.android.ui.drawer;
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


import static android.content.Context.INPUT_METHOD_SERVICE;
import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.BaseMainActivity.emojis;
import static app.fedilab.android.BaseMainActivity.instanceInfo;
import static app.fedilab.android.activities.ComposeActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.client.entities.api.EmojiInstance;
import app.fedilab.android.client.entities.api.Mention;
import app.fedilab.android.client.entities.api.Poll;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.api.Tag;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.databinding.ComposeAttachmentItemBinding;
import app.fedilab.android.databinding.ComposePollBinding;
import app.fedilab.android.databinding.ComposePollItemBinding;
import app.fedilab.android.databinding.DrawerStatusComposeBinding;
import app.fedilab.android.databinding.DrawerStatusSimpleBinding;
import app.fedilab.android.databinding.PopupMediaDescriptionBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.imageeditor.EditImageActivity;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import es.dmoral.toasty.Toasty;


public class ComposeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int searchDeep = 15;
    private static final int TYPE_COMPOSE = 1;
    public static boolean autocomplete = false;
    public static String[] ALPHA = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "!", ",", "?",
            ".", "'"};
    public static String[] MORSE = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..",
            "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----",
            "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", "-.-.--", "--..--",
            "..--..", ".-.-.-", ".----.",};
    private final List<Status> statusList;
    private final int TYPE_NORMAL = 0;
    private final BaseAccount account;
    private final String visibility;
    private final app.fedilab.android.client.entities.api.Account mentionedAccount;
    public ManageDrafts manageDrafts;
    private int statusCount;
    private Context context;
    private AlertDialog alertDialogEmoji;

    public ComposeAdapter(List<Status> statusList, int statusCount, BaseAccount account, app.fedilab.android.client.entities.api.Account mentionedAccount, String visibility) {
        this.statusList = statusList;
        this.statusCount = statusCount;
        this.account = account;
        this.mentionedAccount = mentionedAccount;
        this.visibility = visibility;

    }

    private static void updateCharacterCount(ComposeViewHolder composeViewHolder) {
        int charCount = MastodonHelper.countLength(composeViewHolder);
        composeViewHolder.binding.characterCount.setText(String.valueOf(charCount));
        composeViewHolder.binding.characterProgress.setProgress(charCount);

    }

    public static StatusDraft prepareDraft(List<Status> statusList, ComposeAdapter composeAdapter, String instance, String user_id) {
        //Collect all statusCompose
        List<Status> statusDrafts = new ArrayList<>();
        List<Status> statusReplies = new ArrayList<>();
        int i = 0;
        for (Status status : statusList) {

            //Statuses must be sent
            if (composeAdapter.getItemViewType(i) == TYPE_COMPOSE) {
                statusDrafts.add(status);
            } else {
                statusReplies.add(status);
            }
            i++;
        }
        StatusDraft statusDraftDB = new StatusDraft();
        statusDraftDB.statusReplyList = statusReplies;
        statusDraftDB.statusDraftList = statusDrafts;
        statusDraftDB.instance = instance;
        statusDraftDB.user_id = user_id;
        return statusDraftDB;
    }

    //Create text when mentioning a toot
    public void loadMentions(Status status) {
        //Get the first draft
        statusList.get(statusCount).text = String.format("\n\nvia @%s\n\n%s\n\n", status.account.acct, status.url);
        notifyItemChanged(statusCount);
    }

    /**
     * Manage mentions displayed when replying to a message
     *
     * @param context     Context
     * @param statusDraft {@link Status} - Status that user is replying
     * @param holder      {@link ComposeViewHolder} - current compose viewHolder
     */
    private void manageMentions(Context context, Status statusDraft, ComposeViewHolder holder) {

        if (statusDraft.mentions != null && (statusDraft.text == null || statusDraft.text.length() == 0) && statusDraft.mentions.size() > 0) {
            //Retrieves mentioned accounts + OP and adds them at the beginin of the toot
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Mention inReplyToUser;
            inReplyToUser = statusDraft.mentions.get(0);
            if (statusDraft.text == null) {
                statusDraft.text = "";
            }
            //Put other accounts mentioned at the bottom
            boolean capitalize = sharedpreferences.getBoolean(context.getString(R.string.SET_CAPITALIZE), true);
            if (inReplyToUser != null) {
                if (capitalize) {
                    statusDraft.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + "\n" : "@" + inReplyToUser.acct + "\n";
                } else {
                    statusDraft.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + " " : "@" + inReplyToUser.acct + " ";
                }
            }
            holder.binding.content.setText(statusDraft.text);
            statusDraft.cursorPosition = statusDraft.text.length();
            if (statusDraft.mentions.size() > 1) {
                statusDraft.text += "\n";
                for (int i = 1; i < statusDraft.mentions.size(); i++) {
                    String tootTemp = String.format("@%s ", statusDraft.mentions.get(i).acct);
                    statusDraft.text = String.format("%s ", (statusDraft.text + tootTemp.trim()));
                }
            }
            holder.binding.content.setText(statusDraft.text);
            updateCharacterCount(holder);
            holder.binding.content.requestFocus();
            holder.binding.content.post(() -> {
                holder.binding.content.setSelection(statusDraft.cursorPosition); //Put cursor at the end
                buttonVisibility(holder);
            });
        } else if (mentionedAccount != null) {
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean capitalize = sharedpreferences.getBoolean(context.getString(R.string.SET_CAPITALIZE), true);
            if (capitalize) {
                statusDraft.text = "@" + mentionedAccount.acct + "\n";
            } else {
                statusDraft.text = "@" + mentionedAccount.acct + " ";
            }
            holder.binding.content.setText(statusDraft.text);
            updateCharacterCount(holder);
            holder.binding.content.requestFocus();
            holder.binding.content.post(() -> {
                buttonVisibility(holder);
                holder.binding.content.setSelection(statusDraft.text.length()); //Put cursor at the end
            });
        } else {
            holder.binding.content.requestFocus();
        }
    }

    public void setStatusCount(int count) {
        statusCount = count;
    }

    public int getCount() {
        return (statusList.size());
    }

    public Status getItem(int position) {
        return statusList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return position >= statusCount ? TYPE_COMPOSE : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == TYPE_NORMAL) {
            DrawerStatusSimpleBinding itemBinding = DrawerStatusSimpleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusSimpleViewHolder(itemBinding);
        } else {
            DrawerStatusComposeBinding itemBinding = DrawerStatusComposeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ComposeViewHolder(itemBinding);
        }
    }

    private void pickupMedia(ComposeActivity.mediaType type, int position) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return;
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
        ((Activity) context).startActivityForResult(intent, (ComposeActivity.PICK_MEDIA + position));
    }

    /**
     * Manage the visibility of the button (+/-) for adding a message to the composed thread
     *
     * @param holder - ComposeViewHolder
     */
    private void buttonVisibility(ComposeViewHolder holder) {
        //First message - Needs at least one char to display the + button
        if (holder.getLayoutPosition() == statusCount && canBeRemoved(statusList.get(holder.getLayoutPosition()))) {
            holder.binding.addRemoveStatus.setVisibility(View.GONE);
            return;
        }

        //Manage last compose drawer button visibility
        if (holder.getLayoutPosition() == (getItemCount() - 1)) {
            if (statusList.size() > statusCount + 1) {
                if (canBeRemoved(statusList.get(statusList.size() - 1))) {
                    holder.binding.addRemoveStatus.setImageResource(R.drawable.ic_compose_thread_remove_status);
                    holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.remove_status));
                    holder.binding.addRemoveStatus.setOnClickListener(v -> {
                        manageDrafts.onItemDraftDeleted(statusList.get(holder.getLayoutPosition()), holder.getLayoutPosition());
                        notifyItemChanged((getItemCount() - 1));
                    });
                } else {
                    holder.binding.addRemoveStatus.setImageResource(R.drawable.ic_compose_thread_add_status);
                    holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.add_status));
                    holder.binding.addRemoveStatus.setOnClickListener(v -> {
                        manageDrafts.onItemDraftAdded(holder.getLayoutPosition());
                        buttonVisibility(holder);
                    });
                }
            } else {
                holder.binding.addRemoveStatus.setImageResource(R.drawable.ic_compose_thread_add_status);
                holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.add_status));
                holder.binding.addRemoveStatus.setOnClickListener(v -> {
                    manageDrafts.onItemDraftAdded(holder.getLayoutPosition());
                    buttonVisibility(holder);
                });
            }
            holder.binding.addRemoveStatus.setVisibility(View.VISIBLE);
            holder.binding.buttonPost.setVisibility(View.VISIBLE);
        } else {
            holder.binding.addRemoveStatus.setVisibility(View.GONE);
            holder.binding.buttonPost.setVisibility(View.GONE);
        }

    }

    /**
     * Check content of the draft to set if it can be removed (empty poll / media / text / spoiler)
     *
     * @param draft - Status
     * @return boolean
     */
    private boolean canBeRemoved(Status draft) {
        return draft.poll == null
                && (draft.media_attachments == null || draft.media_attachments.size() == 0)
                && (draft.text == null || draft.text.trim().length() == 0)
                && (draft.spoiler_text == null || draft.spoiler_text.trim().length() == 0);
    }

    /**
     * Add an attachment from ComposeActivity
     *
     * @param position int - position of the drawer that added a media
     * @param uris     List<Uri> - uris of the media
     */
    public void addAttachment(int position, List<Uri> uris) {
        if (position == -1) {
            position = statusList.size() - 1;
        }
        if (statusList.get(position).media_attachments == null) {
            statusList.get(position).media_attachments = new ArrayList<>();
        }
        int finalPosition = position;
        Helper.createAttachmentFromUri(context, uris, attachment -> {
            statusList.get(finalPosition).media_attachments.add(attachment);
            notifyItemChanged(finalPosition);
        });
    }

    /**
     * Add a shared element
     * If title and description are empty, it will use subject and content coming from the intent
     *
     * @param url         - String url that is shared
     * @param title       - String title gather from the URL
     * @param description - String description gathered from the URL
     * @param subject     - String subject (title) comming from the shared elements
     * @param content     - String content (description) coming from the shared elements
     */
    public void addSharing(String url, String title, String description, String subject, String content, String saveFilePath) {
        int position = statusList.size() - 1;
        if (description == null && content == null) {
            return;
        }
        if (title != null || subject != null) {
            statusList.get(position).text = title != null ? title : subject;
            statusList.get(position).text += "\n\n";
        } else {
            statusList.get(position).text = "";
        }
        statusList.get(position).text += description != null ? description : content;
        statusList.get(position).text += "\n\n";
        if (url != null) {
            statusList.get(position).text += url;
        }
        if (saveFilePath != null) {
            Attachment attachment = new Attachment();
            attachment.mimeType = "image/*";
            String extension = "jpg";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_", Locale.getDefault());
            attachment.local_path = saveFilePath;
            Date now = new Date();
            attachment.filename = formatter.format(now) + "." + extension;
            if (statusList.get(position).media_attachments == null) {
                statusList.get(position).media_attachments = new ArrayList<>();
            }
            statusList.get(position).media_attachments.add(attachment);
        }
        notifyItemChanged(position);
    }

    //<------ Manage contact from compose activity
    //It only targets last message in a thread
    //Return content of last compose message
    public String getLastComposeContent() {
        return statusList.get(statusList.size() - 1).text != null ? statusList.get(statusList.size() - 1).text : "";
    }

    //Used to write contact when composing
    public void updateContent(boolean checked, String acct) {
        if (checked) {
            if (!statusList.get(statusList.size() - 1).text.contains(acct))
                statusList.get(statusList.size() - 1).text = String.format("%s %s", acct, statusList.get(statusList.size() - 1).text);
        } else {
            statusList.get(statusList.size() - 1).text = statusList.get(statusList.size() - 1).text.replaceAll("\\s*" + acct, "");
        }
        notifyItemChanged(statusList.size() - 1);
    }
    //------- end contact ----->

    //Put cursor to the end after changing contacts
    public void putCursor() {
        statusList.get(statusList.size() - 1).setCursorToEnd = true;
        notifyItemChanged(statusList.size() - 1);
    }

    private void displayAttachments(ComposeViewHolder holder, int position, int scrollToMediaPosition) {
        if (statusList.size() > position && statusList.get(position).media_attachments != null) {
            holder.binding.attachmentsList.removeAllViews();
            List<Attachment> attachmentList = statusList.get(position).media_attachments;
            if (attachmentList != null && attachmentList.size() > 0) {
                holder.binding.sensitiveMedia.setVisibility(View.VISIBLE);
                if (!statusList.get(position).sensitive) {
                    if (currentAccount.mastodon_account.source != null) {
                        holder.binding.sensitiveMedia.setChecked(currentAccount.mastodon_account.source.sensitive);
                        statusList.get(position).sensitive = currentAccount.mastodon_account.source.sensitive;
                    } else {
                        statusList.get(position).sensitive = false;
                    }
                }

                holder.binding.sensitiveMedia.setOnCheckedChangeListener((buttonView, isChecked) -> statusList.get(position).sensitive = isChecked);
                int mediaPosition = 0;
                for (Attachment attachment : attachmentList) {
                    ComposeAttachmentItemBinding composeAttachmentItemBinding = ComposeAttachmentItemBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                    composeAttachmentItemBinding.buttonPlay.setVisibility(View.GONE);
                    String attachmentPath = attachment.local_path != null && !attachment.local_path.trim().isEmpty() ? attachment.local_path : attachment.preview_url;
                    if (attachment.type != null || attachment.mimeType != null) {
                        if ((attachment.type != null && attachment.type.toLowerCase().startsWith("image")) || (attachment.mimeType != null && attachment.mimeType.toLowerCase().startsWith("image"))) {
                            Glide.with(composeAttachmentItemBinding.preview.getContext())
                                    .load(attachmentPath)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
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
                    composeAttachmentItemBinding.editPreview.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EditImageActivity.class);
                        Bundle b = new Bundle();
                        intent.putExtra("imageUri", attachment.local_path);
                        intent.putExtras(b);
                        context.startActivity(intent);
                    });
                    composeAttachmentItemBinding.buttonDescription.setOnClickListener(v -> {
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                        builderInner.setTitle(R.string.upload_form_description);
                        PopupMediaDescriptionBinding popupMediaDescriptionBinding = PopupMediaDescriptionBinding.inflate(LayoutInflater.from(context), null, false);
                        builderInner.setView(popupMediaDescriptionBinding.getRoot());

                        popupMediaDescriptionBinding.mediaDescription.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1500)});
                        popupMediaDescriptionBinding.mediaDescription.requestFocus();
                        Glide.with(popupMediaDescriptionBinding.mediaPicture.getContext())
                                .asBitmap()
                                .load(attachmentPath)
                                .into(new CustomTarget<Bitmap>() {
                                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                        popupMediaDescriptionBinding.mediaPicture.setImageBitmap(resource);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });
                        builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        if (attachment.description != null) {
                            popupMediaDescriptionBinding.mediaDescription.setText(attachment.description);
                            popupMediaDescriptionBinding.mediaDescription.setSelection(popupMediaDescriptionBinding.mediaDescription.getText().length());
                        }
                        builderInner.setPositiveButton(R.string.validate, (dialog, which) -> {
                            attachment.description = popupMediaDescriptionBinding.mediaDescription.getText().toString();
                            displayAttachments(holder, position, finalMediaPosition);
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
                            displayAttachments(holder, position, finalMediaPosition - 1);
                        }
                    });
                    composeAttachmentItemBinding.buttonOrderDown.setOnClickListener(v -> {
                        if (finalMediaPosition < (attachmentList.size() - 1) && attachmentList.size() > 1) {
                            Attachment at1 = attachmentList.get(finalMediaPosition);
                            Attachment at2 = attachmentList.get(finalMediaPosition + 1);
                            attachmentList.set(finalMediaPosition, at2);
                            attachmentList.set(finalMediaPosition + 1, at1);
                            displayAttachments(holder, position, finalMediaPosition + 1);
                        }
                    });
                    composeAttachmentItemBinding.buttonRemove.setOnClickListener(v -> {
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                        builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        builderInner.setPositiveButton(R.string.delete, (dialog, which) -> {
                            attachmentList.remove(attachment);
                            displayAttachments(holder, position, finalMediaPosition);
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
                    composeAttachmentItemBinding.preview.setOnClickListener(v -> displayAttachments(holder, position, finalMediaPosition));
                    if (attachment.description == null || attachment.description.trim().isEmpty()) {
                        composeAttachmentItemBinding.buttonDescription.setIconResource(R.drawable.ic_baseline_warning_24);
                        composeAttachmentItemBinding.buttonDescription.setStrokeColor(ThemeHelper.getNoDescriptionColorStateList(context));
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(context, R.color.no_description));
                        Helper.changeDrawableColor(context, R.drawable.ic_baseline_warning_24, ContextCompat.getColor(context, R.color.no_description));
                        composeAttachmentItemBinding.buttonDescription.setIconTint(ThemeHelper.getNoDescriptionColorStateList(context));
                    } else {
                        composeAttachmentItemBinding.buttonDescription.setIconTint(ThemeHelper.getHavingDescriptionColorStateList(context));
                        composeAttachmentItemBinding.buttonDescription.setIconResource(R.drawable.ic_baseline_check_circle_24);
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(context, R.color.having_description));
                        composeAttachmentItemBinding.buttonDescription.setStrokeColor(ThemeHelper.getHavingDescriptionColorStateList(context));
                        Helper.changeDrawableColor(context, R.drawable.ic_baseline_check_circle_24, ContextCompat.getColor(context, R.color.having_description));
                    }
                    holder.binding.attachmentsList.addView(composeAttachmentItemBinding.getRoot());
                    mediaPosition++;
                }
                holder.binding.attachmentsList.setVisibility(View.VISIBLE);
                if (scrollToMediaPosition >= 0 && holder.binding.attachmentsList.getChildCount() < scrollToMediaPosition) {
                    holder.binding.attachmentsList.requestChildFocus(holder.binding.attachmentsList.getChildAt(scrollToMediaPosition), holder.binding.attachmentsList.getChildAt(scrollToMediaPosition));
                }
            } else {
                holder.binding.attachmentsList.setVisibility(View.GONE);
                holder.binding.sensitiveMedia.setVisibility(View.GONE);
            }
        } else {
            holder.binding.attachmentsList.setVisibility(View.GONE);
            holder.binding.sensitiveMedia.setVisibility(View.GONE);
        }
        buttonState(holder);
    }

    /**
     * Manage state of media and poll button
     *
     * @param holder ComposeViewHolder
     */
    private void buttonState(ComposeViewHolder holder) {
        if (BaseMainActivity.software == null || BaseMainActivity.software.toUpperCase().compareTo("MASTODON") == 0) {
            if (holder.getBindingAdapterPosition() > 0) {
                Status statusDraft = statusList.get(holder.getBindingAdapterPosition());
                if (statusDraft.poll == null) {
                    holder.binding.buttonAttachImage.setEnabled(true);
                    holder.binding.buttonAttachVideo.setEnabled(true);
                    holder.binding.buttonAttachAudio.setEnabled(true);
                    holder.binding.buttonAttachManual.setEnabled(true);
                } else {
                    holder.binding.buttonAttachImage.setEnabled(false);
                    holder.binding.buttonAttachVideo.setEnabled(false);
                    holder.binding.buttonAttachAudio.setEnabled(false);
                    holder.binding.buttonAttachManual.setEnabled(false);
                    holder.binding.buttonPoll.setEnabled(true);
                }
                holder.binding.buttonPoll.setEnabled(statusDraft.media_attachments == null || statusDraft.media_attachments.size() <= 0);
            }
        }
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    private List<Emoji> emojisList = new ArrayList<>();
    /**
     * Initialize text watcher for content writing
     * It will allow to complete autocomplete edit text while starting words with @, #, : etc.
     *
     * @param holder {@link ComposeViewHolder} - current compose viewHolder
     * @return {@link TextWatcher}
     */
    public TextWatcher initializeTextWatcher(ComposeAdapter.ComposeViewHolder holder) {
        String pattern = "(.|\\s)*(@[\\w_-]+@[a-z0-9.\\-]+|@[\\w_-]+)";
        final Pattern mentionPattern = Pattern.compile(pattern);

        String patternTag = "^(.|\\s)*(#([\\w-]{2,}))$";
        final Pattern tagPattern = Pattern.compile(patternTag);

        String patternEmoji = "^(.|\\s)*(:([\\w_]+))$";
        final Pattern emojiPattern = Pattern.compile(patternEmoji);
        final int[] currentCursorPosition = {holder.binding.content.getSelectionStart()};
        final String[] newContent = {null};
        final int[] searchLength = {searchDeep};
        TextWatcher textw;
        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
        SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
        textw = new TextWatcher() {
            private int cPosition;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 2) {
                    holder.binding.addRemoveStatus.setVisibility(View.VISIBLE);
                }
                cPosition = start;
            }

            @Override
            public void afterTextChanged(Editable s) {
                int currentLength = MastodonHelper.countLength(holder);
                //Copy/past
                int max_car = MastodonHelper.getInstanceMaxChars(context);
                if (currentLength > max_car) {
                    holder.binding.characterCount.setTextColor(Color.RED);
                } else {
                    holder.binding.characterCount.setTextColor(holder.binding.content.getTextColors());
                }
                /*if (currentLength > max_car + 1) {
                    int from = max_car - holder.binding.contentSpoiler.getText().length();
                    int to = (currentLength - holder.binding.contentSpoiler.getText().length());
                    if (to <= s.length()) {
                        holder.binding.content.setText(s.delete(from, to));
                    }
                } else if (currentLength > max_car) {
                    if (cPosition + 1 <= s.length()) {
                        holder.binding.content.setText(s.delete(cPosition, cPosition + 1));
                    }
                }*/
                statusList.get(holder.getBindingAdapterPosition()).text = s.toString();
                if (s.toString().trim().length() < 2) {
                    buttonVisibility(holder);
                }
                //Update cursor position
                statusList.get(holder.getBindingAdapterPosition()).cursorPosition = holder.binding.content.getSelectionStart();
                if (autocomplete) {
                    holder.binding.content.removeTextChangedListener(this);
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            String fedilabHugsTrigger = ":fedilab_hugs:";
                            String fedilabMorseTrigger = ":fedilab_morse:";

                            if (s.toString().contains(fedilabHugsTrigger)) {
                                newContent[0] = s.toString().replaceAll(fedilabHugsTrigger, "");

                                int toFill = 500 - currentLength;
                                if (toFill <= 0) {
                                    return;
                                }
                                StringBuilder hugs = new StringBuilder();
                                for (int i = 0; i < toFill; i++) {
                                    hugs.append(new String(Character.toChars(0x1F917)));
                                }

                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                Runnable myRunnable = () -> {
                                    newContent[0] = newContent[0] + hugs;
                                    holder.binding.content.setText(newContent[0]);
                                    holder.binding.content.setSelection(holder.binding.content.getText().length());
                                    autocomplete = false;
                                    updateCharacterCount(holder);
                                };
                                mainHandler.post(myRunnable);
                            } else if (s.toString().contains(fedilabMorseTrigger)) {
                                newContent[0] = s.toString().replaceAll(fedilabMorseTrigger, "").trim();
                                List<String> mentions = new ArrayList<>();
                                String mentionPattern = "@[a-z0-9_]+(@[a-z0-9.\\-]+[a-z0-9]+)?";
                                final Pattern mPattern = Pattern.compile(mentionPattern, Pattern.CASE_INSENSITIVE);
                                Matcher matcherMentions = mPattern.matcher(newContent[0]);
                                while (matcherMentions.find()) {
                                    mentions.add(matcherMentions.group());
                                }
                                for (String mention : mentions) {
                                    newContent[0] = newContent[0].replace(mention, "");
                                }
                                newContent[0] = Normalizer.normalize(newContent[0], Normalizer.Form.NFD);
                                newContent[0] = newContent[0].replaceAll("[^\\p{ASCII}]", "");

                                HashMap<String, String> ALPHA_TO_MORSE = new HashMap<>();
                                for (int i = 0; i < ALPHA.length && i < MORSE.length; i++) {
                                    ALPHA_TO_MORSE.put(ALPHA[i], MORSE[i]);
                                }
                                StringBuilder builder = new StringBuilder();
                                String[] words = newContent[0].trim().split(" ");

                                for (String word : words) {
                                    for (int i = 0; i < word.length(); i++) {
                                        String morse = ALPHA_TO_MORSE.get(word.substring(i, i + 1).toLowerCase());
                                        builder.append(morse).append(" ");
                                    }

                                    builder.append("  ");
                                }
                                newContent[0] = "";
                                for (String mention : mentions) {
                                    newContent[0] += mention + " ";
                                }
                                newContent[0] += builder.toString();

                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                Runnable myRunnable = () -> {
                                    holder.binding.content.setText(newContent[0]);
                                    holder.binding.content.setSelection(holder.binding.content.getText().length());
                                    autocomplete = false;
                                    updateCharacterCount(holder);
                                };
                                mainHandler.post(myRunnable);
                            }
                        }
                    };
                    thread.start();
                    return;
                }

                if (holder.binding.content.getSelectionStart() != 0)
                    currentCursorPosition[0] = holder.binding.content.getSelectionStart();
                if (s.toString().length() == 0)
                    currentCursorPosition[0] = 0;
                //Only check last 15 characters before cursor position to avoid lags
                //Less than 15 characters are written before the cursor position
                searchLength[0] = Math.min(currentCursorPosition[0], searchDeep);


                if (currentCursorPosition[0] - (searchLength[0] - 1) < 0 || currentCursorPosition[0] == 0 || currentCursorPosition[0] > s.toString().length()) {
                    updateCharacterCount(holder);
                    return;
                }

                String patternh = "^(.|\\s)*(:fedilab_hugs:)$";
                final Pattern hPattern = Pattern.compile(patternh);
                Matcher mh = hPattern.matcher((s.toString().substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])));

                if (mh.matches()) {
                    autocomplete = true;
                    return;
                }

                String patternM = "^(.|\\s)*(:fedilab_morse:)$";
                final Pattern mPattern = Pattern.compile(patternM);
                Matcher mm = mPattern.matcher((s.toString().substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])));
                if (mm.matches()) {
                    autocomplete = true;
                    return;
                }
                String[] searchInArray = (s.toString().substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])).split("\\s");
                if (searchInArray.length < 1) {
                    updateCharacterCount(holder);
                    return;
                }
                String searchIn = searchInArray[searchInArray.length - 1];
                Matcher matcherMention, matcherTag, matcherEmoji;
                matcherMention = mentionPattern.matcher(searchIn);
                matcherTag = tagPattern.matcher(searchIn);
                matcherEmoji = emojiPattern.matcher(searchIn);
                if (matcherMention.matches()) {
                    String searchGroup = matcherMention.group();
                    accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, 10, true, false).observe((LifecycleOwner) context, accounts -> {
                        if (accounts == null) {
                            return;
                        }
                        int currentCursorPosition = holder.binding.content.getSelectionStart();
                        AccountsSearchAdapter accountsListAdapter = new AccountsSearchAdapter(context, accounts);
                        holder.binding.content.setThreshold(1);
                        holder.binding.content.setAdapter(accountsListAdapter);
                        final String oldContent = holder.binding.content.getText().toString();
                        if (oldContent.length() >= currentCursorPosition) {
                            String[] searchA = oldContent.substring(0, currentCursorPosition).split("@");
                            if (searchA.length > 0) {
                                final String search = searchA[searchA.length - 1];
                                holder.binding.content.setOnItemClickListener((parent, view, position, id) -> {
                                    app.fedilab.android.client.entities.api.Account account = accounts.get(position);
                                    String deltaSearch = "";
                                    int searchLength = searchDeep;
                                    if (currentCursorPosition < searchDeep) { //Less than 15 characters are written before the cursor position
                                        searchLength = currentCursorPosition;
                                    }
                                    if (currentCursorPosition - searchLength > 0 && currentCursorPosition < oldContent.length())
                                        deltaSearch = oldContent.substring(currentCursorPosition - searchLength, currentCursorPosition);
                                    else {
                                        if (currentCursorPosition >= oldContent.length())
                                            deltaSearch = oldContent.substring(currentCursorPosition - searchLength);
                                    }

                                    if (!search.equals(""))
                                        deltaSearch = deltaSearch.replace("@" + search, "");
                                    String newContent = oldContent.substring(0, currentCursorPosition - searchLength);
                                    newContent += deltaSearch;
                                    newContent += "@" + account.acct + " ";
                                    int newPosition = newContent.length();
                                    if (currentCursorPosition < oldContent.length())
                                        newContent += oldContent.substring(currentCursorPosition);
                                    holder.binding.content.setText(newContent);
                                    updateCharacterCount(holder);
                                    holder.binding.content.setSelection(newPosition);
                                    AccountsSearchAdapter accountsListAdapter1 = new AccountsSearchAdapter(context, new ArrayList<>());
                                    holder.binding.content.setThreshold(1);
                                    holder.binding.content.setAdapter(accountsListAdapter1);
                                });
                            }
                        }

                    });
                } else if (matcherTag.matches()) {
                    String searchGroup = matcherTag.group(3);
                    if (searchGroup != null) {
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, null,
                                "hashtags", false, true, false, 0,
                                null, null, 10).observe((LifecycleOwner) context,
                                results -> {
                                    if (results == null) {
                                        return;
                                    }
                                    int currentCursorPosition = holder.binding.content.getSelectionStart();
                                    TagsSearchAdapter tagsSearchAdapter = new TagsSearchAdapter(context, results.hashtags);
                                    holder.binding.content.setThreshold(1);
                                    holder.binding.content.setAdapter(tagsSearchAdapter);
                                    final String oldContent = holder.binding.content.getText().toString();
                                    if (oldContent.length() < currentCursorPosition)
                                        return;
                                    String[] searchA = oldContent.substring(0, currentCursorPosition).split("#");
                                    if (searchA.length < 1)
                                        return;
                                    final String search = searchA[searchA.length - 1];
                                    holder.binding.content.setOnItemClickListener((parent, view, position, id) -> {
                                        if (position >= results.hashtags.size())
                                            return;
                                        Tag tag = results.hashtags.get(position);
                                        String deltaSearch = "";
                                        int searchLength = searchDeep;
                                        if (currentCursorPosition < searchDeep) { //Less than 15 characters are written before the cursor position
                                            searchLength = currentCursorPosition;
                                        }
                                        if (currentCursorPosition - searchLength > 0 && currentCursorPosition < oldContent.length())
                                            deltaSearch = oldContent.substring(currentCursorPosition - searchLength, currentCursorPosition);
                                        else {
                                            if (currentCursorPosition >= oldContent.length())
                                                deltaSearch = oldContent.substring(currentCursorPosition - searchLength);
                                        }

                                        if (!search.equals(""))
                                            deltaSearch = deltaSearch.replace("#" + search, "");
                                        String newContent = oldContent.substring(0, currentCursorPosition - searchLength);
                                        newContent += deltaSearch;
                                        newContent += "#" + tag.name + " ";
                                        int newPosition = newContent.length();
                                        if (currentCursorPosition < oldContent.length())
                                            newContent += oldContent.substring(currentCursorPosition);
                                        holder.binding.content.setText(newContent);
                                        updateCharacterCount(holder);
                                        holder.binding.content.setSelection(newPosition);
                                        TagsSearchAdapter tagsSearchAdapter1 = new TagsSearchAdapter(context, new ArrayList<>());
                                        holder.binding.content.setThreshold(1);
                                        holder.binding.content.setAdapter(tagsSearchAdapter1);
                                    });
                                });
                    }
                } else if (matcherEmoji.matches()) {
                    String shortcode = matcherEmoji.group(3);
                    new Thread(() -> {
                        List<Emoji> emojisToDisplay = new ArrayList<>();
                        try {
                            if (emojisList == null || emojisList.size() == 0) {
                                emojisList = new EmojiInstance(context).getEmojiList(BaseMainActivity.currentInstance);
                            }
                            if (emojis == null) {
                                return;
                            }
                            for (Emoji emoji : emojisList) {
                                if (shortcode != null && emoji.shortcode.contains(shortcode)) {
                                    emojisToDisplay.add(emoji);
                                    if (emojisToDisplay.size() >= 10) {
                                        break;
                                    }
                                }
                            }
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> {
                                int currentCursorPosition = holder.binding.content.getSelectionStart();
                                EmojiSearchAdapter emojisSearchAdapter = new EmojiSearchAdapter(context, emojisToDisplay);
                                holder.binding.content.setThreshold(1);
                                holder.binding.content.setAdapter(emojisSearchAdapter);
                                final String oldContent = holder.binding.content.getText().toString();
                                String[] searchA = oldContent.substring(0, currentCursorPosition).split(":");
                                if (searchA.length > 0) {
                                    final String search = searchA[searchA.length - 1];
                                    holder.binding.content.setOnItemClickListener((parent, view, position, id) -> {
                                        String shortcodeSelected = emojisToDisplay.get(position).shortcode;
                                        String deltaSearch = "";
                                        int searchLength = searchDeep;
                                        if (currentCursorPosition < searchDeep) { //Less than 15 characters are written before the cursor position
                                            searchLength = currentCursorPosition;
                                        }
                                        if (currentCursorPosition - searchLength > 0 && currentCursorPosition < oldContent.length())
                                            deltaSearch = oldContent.substring(currentCursorPosition - searchLength, currentCursorPosition);
                                        else {
                                            if (currentCursorPosition >= oldContent.length())
                                                deltaSearch = oldContent.substring(currentCursorPosition - searchLength);
                                        }

                                        if (!search.equals(""))
                                            deltaSearch = deltaSearch.replace(":" + search, "");
                                        String newContent = oldContent.substring(0, currentCursorPosition - searchLength);
                                        newContent += deltaSearch;
                                        newContent += ":" + shortcodeSelected + ": ";
                                        int newPosition = newContent.length();
                                        if (currentCursorPosition < oldContent.length())
                                            newContent += oldContent.substring(currentCursorPosition);
                                        holder.binding.content.setText(newContent);
                                        updateCharacterCount(holder);
                                        holder.binding.content.setSelection(newPosition);
                                        EmojiSearchAdapter emojisSearchAdapter1 = new EmojiSearchAdapter(context, new ArrayList<>());
                                        holder.binding.content.setThreshold(1);
                                        holder.binding.content.setAdapter(emojisSearchAdapter1);
                                    });
                                }
                            };
                            mainHandler.post(myRunnable);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }).start();

                } else {
                    holder.binding.content.dismissDropDown();
                }

                updateCharacterCount(holder);
            }
        };
        return textw;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        int theme_statuses_color = -1;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean("use_custom_theme", false)) {
            theme_statuses_color = sharedpreferences.getInt("theme_statuses_color", -1);
        }
        if (getItemViewType(position) == TYPE_NORMAL) {
            Status status = statusList.get(position);
            StatusSimpleViewHolder holder = (StatusSimpleViewHolder) viewHolder;
            holder.binding.statusContent.setText(
                    status.getSpanContent(context,
                            new WeakReference<>(holder.binding.statusContent)),
                    TextView.BufferType.SPANNABLE);
            MastodonHelper.loadPPMastodon(holder.binding.avatar, status.account);
            holder.binding.displayName.setText(
                    status.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.binding.displayName)),
                    TextView.BufferType.SPANNABLE);
            holder.binding.username.setText(String.format("@%s", status.account.acct));
            if (status.spoiler_text != null && !status.spoiler_text.trim().isEmpty()) {
                holder.binding.spoiler.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setText(
                        status.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.spoiler)),
                        TextView.BufferType.SPANNABLE);
            } else {
                holder.binding.spoiler.setVisibility(View.GONE);
                holder.binding.spoiler.setText(null);
            }
            if (theme_statuses_color != -1) {
                holder.binding.cardviewContainer.setBackgroundColor(theme_statuses_color);
            } else {
                holder.binding.cardviewContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.cyanea_primary_dark_reference));
            }
        } else if (getItemViewType(position) == TYPE_COMPOSE) {
            Status statusDraft = statusList.get(position);


            ComposeViewHolder holder = (ComposeViewHolder) viewHolder;

            boolean displayEmoji = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_EMOJI), false);
            if (displayEmoji) {
                holder.binding.buttonEmojiOne.setVisibility(View.VISIBLE);
                holder.binding.buttonEmojiOne.setOnClickListener(v -> {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(holder.binding.buttonEmojiOne.getWindowToken(), 0);
                    EmojiManager.install(new EmojiOneProvider());
                    final EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(holder.binding.buttonEmojiOne).setOnEmojiPopupDismissListener(() -> {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }).build(holder.binding.content);
                    emojiPopup.toggle();
                });
            } else {
                holder.binding.buttonEmojiOne.setVisibility(View.GONE);
            }

            int newInputType = holder.binding.content.getInputType() & (holder.binding.content.getInputType() ^ InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            holder.binding.content.setInputType(newInputType);

            int newInputTypeSpoiler = holder.binding.contentSpoiler.getInputType() & (holder.binding.contentSpoiler.getInputType() ^ InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            holder.binding.contentSpoiler.setInputType(newInputTypeSpoiler);
            if (theme_statuses_color != -1) {
                holder.binding.cardviewContainer.setBackgroundColor(theme_statuses_color);
            } else {
                holder.binding.cardviewContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.cyanea_primary_dark_reference));
            }
            holder.binding.buttonAttach.setOnClickListener(v -> {
                if (instanceInfo.configuration.media_attachments.supported_mime_types != null) {
                    if (instanceInfo.getMimeTypeAudio().size() == 0) {
                        holder.binding.buttonAttachAudio.setEnabled(false);
                    }
                    if (instanceInfo.getMimeTypeImage().size() == 0) {
                        holder.binding.buttonAttachImage.setEnabled(false);
                    }
                    if (instanceInfo.getMimeTypeVideo().size() == 0) {
                        holder.binding.buttonAttachVideo.setEnabled(false);
                    }
                    if (instanceInfo.getMimeTypeOther().size() == 0) {
                        holder.binding.buttonAttachManual.setEnabled(false);
                    }
                }
                holder.binding.attachmentChoicesPanel.setVisibility(View.VISIBLE);
            });

            //Disable buttons to attach media if max has been reached
            if (statusDraft.media_attachments != null &&
                    ((instanceInfo != null && statusDraft.media_attachments.size() >= instanceInfo.configuration.statusesConf.max_media_attachments) || (instanceInfo == null && statusDraft.media_attachments.size() >= 4))) {
                holder.binding.buttonAttachImage.setEnabled(false);
                holder.binding.buttonAttachVideo.setEnabled(false);
                holder.binding.buttonAttachAudio.setEnabled(false);
                holder.binding.buttonAttachManual.setEnabled(false);

            } else {
                holder.binding.buttonAttachImage.setEnabled(true);
                holder.binding.buttonAttachVideo.setEnabled(true);
                holder.binding.buttonAttachAudio.setEnabled(true);
                holder.binding.buttonAttachManual.setEnabled(true);
            }
            holder.binding.buttonAttachAudio.setOnClickListener(v -> {
                holder.binding.attachmentChoicesPanel.setVisibility(View.GONE);
                pickupMedia(ComposeActivity.mediaType.AUDIO, position);
            });
            holder.binding.buttonAttachImage.setOnClickListener(v -> {
                holder.binding.attachmentChoicesPanel.setVisibility(View.GONE);
                pickupMedia(ComposeActivity.mediaType.PHOTO, position);
            });
            holder.binding.buttonAttachVideo.setOnClickListener(v -> {
                holder.binding.attachmentChoicesPanel.setVisibility(View.GONE);
                pickupMedia(ComposeActivity.mediaType.VIDEO, position);
            });
            holder.binding.buttonAttachManual.setOnClickListener(v -> {
                holder.binding.attachmentChoicesPanel.setVisibility(View.GONE);
                pickupMedia(ComposeActivity.mediaType.ALL, position);
            });
            //Used for DM
            if (visibility != null) {
                statusDraft.visibility = visibility;
            }
            if (statusDraft.visibility == null) {
                if (position > 0) {
                    statusDraft.visibility = statusList.get(position - 1).visibility;
                } else if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.source != null) {
                    statusDraft.visibility = currentAccount.mastodon_account.source.privacy;
                } else {
                    statusDraft.visibility = "public";
                }
            }

            switch (statusDraft.visibility.toLowerCase()) {
                case "public":
                    holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_public);
                    statusDraft.visibility = MastodonHelper.visibility.PUBLIC.name();
                    break;
                case "unlisted":
                    holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_unlisted);
                    statusDraft.visibility = MastodonHelper.visibility.UNLISTED.name();
                    break;
                case "private":
                    holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_private);
                    statusDraft.visibility = MastodonHelper.visibility.PRIVATE.name();
                    break;
                case "direct":
                    holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_direct);
                    statusDraft.visibility = MastodonHelper.visibility.DIRECT.name();
                    break;
            }

            holder.binding.buttonCloseAttachmentPanel.setOnClickListener(v -> holder.binding.attachmentChoicesPanel.setVisibility(View.GONE));
            holder.binding.buttonVisibility.setOnClickListener(v -> holder.binding.visibilityPanel.setVisibility(View.VISIBLE));
            holder.binding.buttonCloseVisibilityPanel.setOnClickListener(v -> holder.binding.visibilityPanel.setVisibility(View.GONE));
            holder.binding.buttonVisibilityDirect.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_direct);
                statusDraft.visibility = MastodonHelper.visibility.DIRECT.name();
            });
            holder.binding.buttonVisibilityPrivate.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_private);
                statusDraft.visibility = MastodonHelper.visibility.PRIVATE.name();
            });
            holder.binding.buttonVisibilityUnlisted.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_unlisted);
                statusDraft.visibility = MastodonHelper.visibility.UNLISTED.name();
            });
            holder.binding.buttonVisibilityPublic.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setImageResource(R.drawable.ic_compose_visibility_public);
                statusDraft.visibility = MastodonHelper.visibility.PUBLIC.name();
            });
            holder.binding.buttonSensitive.setOnClickListener(v -> {
                if (holder.binding.contentSpoiler.getVisibility() == View.VISIBLE)
                    holder.binding.contentSpoiler.setVisibility(View.GONE);
                else
                    holder.binding.contentSpoiler.setVisibility(View.VISIBLE);
            });
            //Last compose drawer
            buttonVisibility(holder);

            holder.binding.buttonEmoji.setOnClickListener(v -> {
                try {
                    displayEmojiPicker(holder);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            });
            displayAttachments(holder, position, -1);
            manageMentions(context, statusDraft, holder);
            //For some instances this value can be null, we have to transform the html content
            if (statusDraft.text == null && statusDraft.content != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    statusDraft.text = new SpannableString(Html.fromHtml(statusDraft.content, Html.FROM_HTML_MODE_LEGACY)).toString();
                else
                    statusDraft.text = new SpannableString(Html.fromHtml(statusDraft.content)).toString();
            }
            holder.binding.content.setText(statusDraft.text);
            holder.binding.content.setKeyBoardInputCallbackListener((inputContentInfo, flags, opts) -> {
                if (inputContentInfo != null) {
                    Uri uri = inputContentInfo.getContentUri();
                    List<Uri> uris = new ArrayList<>();
                    uris.add(uri);
                    addAttachment(position, uris);
                }
            });
            if (statusDraft.cursorPosition <= holder.binding.content.length()) {
                holder.binding.content.setSelection(statusDraft.cursorPosition);
            }
            if (statusDraft.setCursorToEnd) {
                statusDraft.setCursorToEnd = false;
                holder.binding.content.setSelection(holder.binding.content.getText().length());
            }
            if (statusDraft.spoiler_text != null) {
                holder.binding.contentSpoiler.setText(statusDraft.spoiler_text);
                holder.binding.contentSpoiler.setSelection(holder.binding.contentSpoiler.getText().length());
            } else {
                holder.binding.contentSpoiler.setText("");
            }
            holder.binding.sensitiveMedia.setChecked(statusDraft.sensitive);
            holder.binding.content.addTextChangedListener(initializeTextWatcher(holder));
            holder.binding.buttonPoll.setOnClickListener(v -> displayPollPopup(holder, statusDraft, position));
            holder.binding.buttonPoll.setOnClickListener(v -> displayPollPopup(holder, statusDraft, position));
            if (instanceInfo == null) {
                return;
            }
            int max_car = MastodonHelper.getInstanceMaxChars(context);
            holder.binding.characterProgress.setMax(max_car);
            holder.binding.contentSpoiler.addTextChangedListener(new TextWatcher() {
                private int cPosition;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    cPosition = start;
                    if (count > 2) {
                        holder.binding.addRemoveStatus.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    int currentLength = MastodonHelper.countLength(holder);
                    int max_car = MastodonHelper.getInstanceMaxChars(context);
                    if (currentLength > max_car) {
                        holder.binding.characterCount.setTextColor(Color.RED);
                    } else {
                        holder.binding.characterCount.setTextColor(holder.binding.content.getTextColors());
                    }
                    /*if (currentLength > max_car + 1) {
                        holder.binding.contentSpoiler.setText(s.delete(max_car - holder.binding.content.getText().length(), (currentLength - holder.binding.content.getText().length())));
                        buttonVisibility(holder);
                    } else if (currentLength > max_car) {
                        buttonVisibility(holder);
                        holder.binding.contentSpoiler.setText(s.delete(cPosition, cPosition + 1));
                    }*/
                    statusList.get(holder.getBindingAdapterPosition()).spoiler_text = s.toString();
                    if (s.toString().trim().length() < 2) {
                        buttonVisibility(holder);
                    }
                    updateCharacterCount(holder);
                }
            });
            if (statusDraft.poll != null) {
                ImageViewCompat.setImageTintList(holder.binding.buttonPoll, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cyanea_accent)));
            } else {
                ImageViewCompat.setImageTintList(holder.binding.buttonPoll, null);
            }
            holder.binding.buttonPost.setEnabled(!statusDraft.submitted);

            holder.binding.buttonPost.setOnClickListener(v -> {
                statusDraft.submitted = true;
                notifyItemChanged(position);
                manageDrafts.onSubmit(prepareDraft(statusList, this, account.instance, account.user_id));
            });
        }

    }


    private void displayEmojiPicker(ComposeViewHolder holder) throws DBException {

        final AlertDialog.Builder builder = new AlertDialog.Builder(context, Helper.dialogStyle());
        int paddingPixel = 15;
        float density = context.getResources().getDisplayMetrics().density;
        int paddingDp = (int) (paddingPixel * density);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.setTitle(R.string.insert_emoji);
        if (emojis != null && emojis.size() > 0) {
            GridView gridView = new GridView(context);
            gridView.setAdapter(new EmojiAdapter(emojis.get(BaseMainActivity.currentInstance)));
            gridView.setNumColumns(5);
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                holder.binding.content.getText().insert(holder.binding.content.getSelectionStart(), " :" + emojis.get(BaseMainActivity.currentInstance).get(position).shortcode + ": ");
                alertDialogEmoji.dismiss();
            });
            gridView.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
            builder.setView(gridView);
        } else {
            TextView textView = new TextView(context);
            textView.setText(context.getString(R.string.no_emoji));
            textView.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
            builder.setView(textView);
        }
        alertDialogEmoji = builder.show();
    }

    private void displayPollPopup(ComposeAdapter.ComposeViewHolder holder, Status statusDraft, int position) {
        AlertDialog.Builder alertPoll = new AlertDialog.Builder(context, Helper.dialogStyle());
        alertPoll.setTitle(R.string.create_poll);
        ComposePollBinding composePollBinding = ComposePollBinding.inflate(LayoutInflater.from(context), new LinearLayout(context), false);
        alertPoll.setView(composePollBinding.getRoot());
        int max_entry = 4;
        int max_length = 25;
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
        composePollBinding.option1.text.setHint(context.getString(R.string.poll_choice_s, 1));
        composePollBinding.option2.text.setFilters(fArray);
        composePollBinding.option2.text.setHint(context.getString(R.string.poll_choice_s, 2));
        composePollBinding.option1.buttonRemove.setVisibility(View.GONE);
        composePollBinding.option2.buttonRemove.setVisibility(View.GONE);
        int finalMax_entry = max_entry;
        composePollBinding.buttonAddOption.setOnClickListener(v -> {
            if (pollCountItem[0] < finalMax_entry) {
                ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(context), new LinearLayout(context), false);
                composePollItemBinding.text.setFilters(fArray);
                composePollItemBinding.text.setHint(context.getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
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
                        title.setHint(context.getString(R.string.poll_choice_s, i + 1));
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

        ArrayAdapter<CharSequence> pollduration = ArrayAdapter.createFromResource(context,
                R.array.poll_duration, android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> pollchoice = ArrayAdapter.createFromResource(context,
                R.array.poll_choice_type, android.R.layout.simple_spinner_dropdown_item);
        composePollBinding.pollType.setAdapter(pollchoice);
        composePollBinding.pollDuration.setAdapter(pollduration);
        composePollBinding.pollDuration.setSelection(4);
        composePollBinding.pollType.setSelection(0);
        if (statusDraft != null && statusDraft.poll != null && statusDraft.poll.options != null) {
            int i = 1;
            for (Poll.PollItem pollItem : statusDraft.poll.options) {
                if (i == 1) {
                    if (pollItem.title != null)
                        composePollBinding.option1.text.setText(pollItem.title);
                } else if (i == 2) {
                    if (pollItem.title != null)
                        composePollBinding.option2.text.setText(pollItem.title);
                } else {

                    ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(context), new LinearLayout(context), false);
                    composePollItemBinding.text.setFilters(fArray);
                    composePollItemBinding.text.setHint(context.getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
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
            if (statusDraft.poll.options.size() >= max_entry) {
                composePollBinding.buttonAddOption.setVisibility(View.GONE);
            }
            switch (statusDraft.poll.expire_in) {
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
            if (statusDraft.poll.multiple)
                composePollBinding.pollType.setSelection(1);
            else
                composePollBinding.pollType.setSelection(0);


        }
        alertPoll.setNegativeButton(R.string.delete, (dialog, whichButton) -> {
            if (statusDraft != null && statusDraft.poll != null) statusDraft.poll = null;
            buttonState(holder);
            dialog.dismiss();
            notifyItemChanged(position);
        });
        alertPoll.setPositiveButton(R.string.validate, null);
        final AlertDialog alertPollDiaslog = alertPoll.create();
        alertPollDiaslog.setOnShowListener(dialog -> {

            Button b = alertPollDiaslog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view1 -> {
                int poll_duration_pos = composePollBinding.pollDuration.getSelectedItemPosition();

                int poll_choice_pos = composePollBinding.pollType.getSelectedItemPosition();
                String choice1 = composePollBinding.option1.text.getText().toString().trim();
                String choice2 = composePollBinding.option2.text.getText().toString().trim();

                if (choice1.isEmpty() && choice2.isEmpty()) {
                    Toasty.error(context, context.getString(R.string.poll_invalid_choices), Toasty.LENGTH_SHORT).show();
                } else if (statusDraft != null) {
                    statusDraft.poll = new Poll();
                    statusDraft.poll.multiple = (poll_choice_pos != 0);
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
                    statusDraft.poll.expire_in = expire;

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
                        statusDraft.poll.options = pollItems;
                        dialog.dismiss();
                    } else {
                        Toasty.error(context, context.getString(R.string.poll_duplicated_entry), Toasty.LENGTH_SHORT).show();
                    }
                }
                holder.binding.buttonPoll.setVisibility(View.VISIBLE);
                buttonState(holder);
                notifyItemChanged(position);
            });
        });

        alertPollDiaslog.show();
    }

    public interface ManageDrafts {
        void onItemDraftAdded(int position);

        void onItemDraftDeleted(Status status, int position);

        void onSubmit(StatusDraft statusDraft);
    }

    public static class StatusSimpleViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusSimpleBinding binding;

        StatusSimpleViewHolder(DrawerStatusSimpleBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

    public static class ComposeViewHolder extends RecyclerView.ViewHolder {
        public DrawerStatusComposeBinding binding;

        ComposeViewHolder(DrawerStatusComposeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}