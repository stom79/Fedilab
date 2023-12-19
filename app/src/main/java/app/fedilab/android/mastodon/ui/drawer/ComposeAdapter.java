package app.fedilab.android.mastodon.ui.drawer;
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
import static app.fedilab.android.mastodon.activities.ComposeActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;
import static de.timfreiheit.mathjax.android.MathJaxConfig.Input.TeX;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
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
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ComposeAttachmentItemBinding;
import app.fedilab.android.databinding.ComposePollBinding;
import app.fedilab.android.databinding.ComposePollItemBinding;
import app.fedilab.android.databinding.DrawerMediaListBinding;
import app.fedilab.android.databinding.DrawerStatusComposeBinding;
import app.fedilab.android.databinding.DrawerStatusSimpleBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.EmojiInstance;
import app.fedilab.android.mastodon.client.entities.api.Mention;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.CamelTag;
import app.fedilab.android.mastodon.client.entities.app.Languages;
import app.fedilab.android.mastodon.client.entities.app.Quotes;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.ComposeHelper;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.LongClickLinkMovementMethod;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.imageeditor.EditImageActivity;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import de.timfreiheit.mathjax.android.MathJaxConfig;
import de.timfreiheit.mathjax.android.MathJaxView;
import es.dmoral.toasty.Toasty;


public class ComposeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_COMPOSE = 1;
    private static final int searchDeep = 15;
    public static boolean autocomplete = false;
    public static String[] ALPHA = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "!", ",", "?",
            ".", "'", "!", "/", "(", ")", "&", ":", ";", "=", "+", "-", "_",
            "\"", "$", "@", "¿", "¡"
    };
    public static String[] MORSE = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..",
            "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----",
            "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", "-.-.--", "--..--",
            "..--..", ".-.-.-", ".----.", "-.-.--", "-..-.", "-.--.", "-.--.-", ".-...", "---...", "-.-.-.", "-...-", ".-.-.", "-....-", "..--.-",
            ".-..-.", "...-..-", ".--.-.", "..-.-", "--...-"
    };

    public static String[] MORSE2 = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..",
            "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----",
            "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", "-.-.--", "--..--",
            "..--..", ".-.-.-", ".----.", "-.-.--", "-..-.", "-.--.", "-.--.-", ".-...", "---...", "-.-.-.", "-...-", ".-.-.", "-....-", "..--.-",
            ".-..-.", "...-..-", ".--.-.", "..-.-", "--...-"
    };
    public static int currentCursorPosition;
    private final List<Status> statusList;
    private final int TYPE_NORMAL = 0;
    private final BaseAccount account;
    private final String visibility;
    private final Account mentionedAccount;
    private final String editMessageId;
    public ManageDrafts manageDrafts;
    public promptDraftListener promptDraftListener;
    public MediaDescriptionCallBack mediaDescriptionCallBack;
    private int statusCount;
    private Context context;
    private AlertDialog alertDialogEmoji;
    private List<Emoji> emojisList = new ArrayList<>();
    private boolean unlisted_changed = false;
    private RecyclerView mRecyclerView;


    public ComposeAdapter(List<Status> statusList, int statusCount, BaseAccount account, Account mentionedAccount, String visibility, String editMessageId) {
        this.statusList = statusList;
        this.statusCount = statusCount;
        this.account = account;
        this.mentionedAccount = mentionedAccount;
        this.visibility = visibility;
        this.editMessageId = editMessageId;

    }

    public static int countMorseChar(String content) {
        int count_char = 0;
        for (String morseCode : MORSE2) {
            if (content.contains(morseCode) && !morseCode.equals(".") && !morseCode.equals("..") && !morseCode.equals("...") && !morseCode.equals("-") && !morseCode.equals("--")) {
                count_char++;
            }
        }
        return count_char;
    }

    public static String morseToText(String morseContent) {
        LinkedHashMap<String, String> ALPHA_TO_MORSE = new LinkedHashMap<>();
        for (int i = 0; i < ALPHA.length && i < MORSE.length; i++) {
            ALPHA_TO_MORSE.put(MORSE[i], ALPHA[i]);
        }
        List<String> MORSELIST = Arrays.asList(MORSE2);
        MORSELIST.sort((s1, s2) -> s2.length() - s1.length());
        LinkedHashMap<String, String> MORSE_TO_ALPHA = new LinkedHashMap<>();
        for (String s : MORSELIST) {
            MORSE_TO_ALPHA.put(s, ALPHA_TO_MORSE.get(s));
        }
        for (String morseCode : MORSELIST) {
            if (MORSE_TO_ALPHA.containsKey(morseCode)) {
                morseContent = morseContent.replaceAll(Pattern.quote(morseCode), Objects.requireNonNull(MORSE_TO_ALPHA.get(morseCode)));
            }
        }
        return morseContent;
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
        // position = statusCount-1+position;
        if (statusList.get(position).media_attachments == null) {
            statusList.get(position).media_attachments = new ArrayList<>();
        }
        if (promptDraftListener != null) {
            promptDraftListener.promptDraft();
        }
        int finalPosition = position;
        Helper.createAttachmentFromUri(context, uris, attachments -> {
            for (Attachment attachment : attachments) {
                statusList.get(finalPosition).media_attachments.add(attachment);
            }
            notifyItemChanged(finalPosition);
        });
    }

    /**
     * Add an attachment from ComposeActivity
     *
     * @param position   int - position of the drawer that added a media
     * @param attachment Attachment - media attachment
     */
    public void addAttachment(int position, Attachment attachment) {
        if (position == -1) {
            position = statusList.size() - 1;
        }
        // position = statusCount-1+position;
        if (statusList.get(position).media_attachments == null) {
            statusList.get(position).media_attachments = new ArrayList<>();
        }
        if (promptDraftListener != null) {
            promptDraftListener.promptDraft();
        }
        int finalPosition = position;
        statusList.get(finalPosition).media_attachments.add(attachment);
        notifyItemChanged(finalPosition);

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
            boolean mentionsAtTop = sharedpreferences.getBoolean(context.getString(R.string.SET_MENTIONS_AT_TOP), false);

            if (inReplyToUser != null) {
                if (capitalize && !mentionsAtTop) {
                    statusDraft.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + "\n" : "@" + inReplyToUser.acct + "\n";
                } else {
                    statusDraft.text = inReplyToUser.acct.startsWith("@") ? inReplyToUser.acct + " " : "@" + inReplyToUser.acct + " ";
                }
            }
            holder.binding.content.setText(statusDraft.text);
            statusDraft.cursorPosition = statusDraft.text.length();
            if (statusDraft.mentions.size() > 1) {
                if (!mentionsAtTop) {
                    statusDraft.text += "\n";
                }
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
        } else if (mentionedAccount != null && statusDraft.text == null) {
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

    /**
     * Manage the actions when picking up a media
     *
     * @param type     - type of media in the list of {@link ComposeActivity.mediaType}
     * @param position - int position of the media in the message
     */
    private void pickupMedia(ComposeActivity.mediaType type, int position) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context,
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
                mimetypes = new String[]{"audio/*"};
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
        if (editMessageId != null || holder.getLayoutPosition() == statusCount && canBeRemoved(statusList.get(holder.getLayoutPosition()))) {
            holder.binding.addRemoveStatus.setVisibility(View.GONE);
            return;
        }

        //Manage last compose drawer button visibility
        if (holder.getLayoutPosition() == (getItemCount() - 1)) {
            if (statusList.size() > statusCount + 1) {
                if (canBeRemoved(statusList.get(statusList.size() - 1))) {
                    holder.binding.addRemoveStatus.setIconResource(R.drawable.ic_compose_thread_remove_status);
                    holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.remove_status));
                    holder.binding.addRemoveStatus.setOnClickListener(v -> {
                        manageDrafts.onItemDraftDeleted(statusList.get(holder.getLayoutPosition()), holder.getLayoutPosition());
                        notifyItemChanged((getItemCount() - 1));
                    });
                } else {
                    holder.binding.addRemoveStatus.setIconResource(R.drawable.ic_compose_thread_add_status);
                    holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.add_status));
                    holder.binding.addRemoveStatus.setOnClickListener(v -> {
                        manageDrafts.onItemDraftAdded(statusList.size(), null);
                        buttonVisibility(holder);
                    });
                }
            } else {
                holder.binding.addRemoveStatus.setIconResource(R.drawable.ic_compose_thread_add_status);
                holder.binding.addRemoveStatus.setContentDescription(context.getString(R.string.add_status));
                holder.binding.addRemoveStatus.setOnClickListener(v -> {
                    manageDrafts.onItemDraftAdded(statusList.size(), null);
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
        final boolean[] proceedToSplit = {false};
        textw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonVisibility(holder);
                //Text is copied pasted and the content is greater than the max of the instance
                int max_car = MastodonHelper.getInstanceMaxChars(context);
                if (ComposeHelper.countLength(s.toString()) > max_car) {
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String defaultFormat = sharedpreferences.getString(context.getString(R.string.SET_THREAD_MESSAGE), context.getString(R.string.DEFAULT_THREAD_VALUE));
                    //User asked to be prompted for threading long messages
                    if(defaultFormat.compareToIgnoreCase("ASK") == 0) {
                        AlertDialog.Builder threadConfirm = new MaterialAlertDialogBuilder(context);
                        threadConfirm.setTitle(context.getString(R.string.thread_long_this_message));
                        threadConfirm.setMessage(context.getString(R.string.thread_long_message_message));
                        threadConfirm.setNegativeButton(R.string.thread_long_message_no, (dialog, which) -> dialog.dismiss());
                        threadConfirm.setPositiveButton(R.string.thread_long_message_yes, (dialog, which) -> {
                            String currentContent = holder.binding.content.getText().toString();
                            ArrayList<String> splitText = ComposeHelper.splitToots(currentContent, max_car);
                            holder.binding.content.setText(splitText.get(0));
                            int statusListSize = statusList.size();
                            int i = 0;
                            for(String message: splitText) {
                                if(i==0) {
                                    i++;
                                    continue;
                                }
                                manageDrafts.onItemDraftAdded(statusListSize+(i-1), message);
                                buttonVisibility(holder);
                                i++;
                            }
                            dialog.dismiss();
                        });
                        threadConfirm.show();
                    } else if(defaultFormat.compareToIgnoreCase("ENABLE") == 0) { //User wants to automatically thread long messages
                        proceedToSplit[0] = true;
                        ArrayList<String> splitText = ComposeHelper.splitToots(s.toString(), max_car);
                        int statusListSize = statusList.size();
                        int i = 0;
                        for(String message: splitText) {
                            if(i==0) {
                                i++;
                                continue;
                            }
                            manageDrafts.onItemDraftAdded(statusListSize+(i-1), message);
                            buttonVisibility(holder);
                            i++;
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String contentString = s.toString();
                if(proceedToSplit[0]) {
                    int max_car = MastodonHelper.getInstanceMaxChars(context);
                    ArrayList<String> splitText = ComposeHelper.splitToots(contentString, max_car);
                    contentString = splitText.get(0);
                }
                int currentLength = MastodonHelper.countLength(holder);
                if (promptDraftListener != null) {
                    promptDraftListener.promptDraft();
                }
                if (holder.binding.content.getSelectionStart() > 0 && holder.getLayoutPosition() >= 0) {
                    statusList.get(holder.getLayoutPosition()).cursorPosition = holder.binding.content.getSelectionStart();
                }

                //Copy/past
                int max_car = MastodonHelper.getInstanceMaxChars(context);
                if (currentLength > max_car) {
                    holder.binding.characterCount.setTextColor(Color.RED);
                } else {
                    holder.binding.characterCount.setTextColor(holder.binding.content.getTextColors());
                }
                if (holder.getBindingAdapterPosition() < 0) {
                    return;
                }
                statusList.get(holder.getBindingAdapterPosition()).text = contentString;
                if (contentString.trim().length() < 2) {
                    buttonVisibility(holder);
                }
                //Update cursor position
                //statusList.get(holder.getBindingAdapterPosition()).cursorPosition = holder.binding.content.getSelectionStart();
                if (autocomplete) {
                    holder.binding.content.removeTextChangedListener(this);
                    String finalContentString = contentString;
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            String fedilabHugsTrigger = ":fedilab_hugs:";
                            String fedilabMorseTrigger = ":fedilab_morse:";
                            String fedilabQuoteTrigger = ":fedilab_quote:";
                            if (finalContentString.contains(fedilabHugsTrigger)) {
                                newContent[0] = finalContentString.replaceAll(Pattern.quote(fedilabHugsTrigger), "").trim();
                                int toFill = 500 - newContent[0].length();
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
                                    statusList.get(holder.getBindingAdapterPosition()).text = newContent[0];
                                    autocomplete = false;
                                    updateCharacterCount(holder);
                                };
                                mainHandler.post(myRunnable);
                            } else if (finalContentString.contains(fedilabMorseTrigger)) {
                                newContent[0] = finalContentString.replaceAll(fedilabMorseTrigger, "").trim();
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

                                LinkedHashMap<String, String> ALPHA_TO_MORSE = new LinkedHashMap<>();
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
                                newContent[0] = newContent[0].replaceAll("null", "");
                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                Runnable myRunnable = () -> {
                                    holder.binding.content.setText(newContent[0]);
                                    statusList.get(holder.getBindingAdapterPosition()).text = newContent[0];
                                    holder.binding.content.setSelection(holder.binding.content.getText().length());
                                    autocomplete = false;
                                    updateCharacterCount(holder);
                                };
                                mainHandler.post(myRunnable);
                            } else if (finalContentString.contains(fedilabQuoteTrigger)) {
                                newContent[0] = finalContentString.replaceAll(fedilabQuoteTrigger, "").trim();
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

                                InputStream is;
                                newContent[0] = "";
                                if (mentions.size() > 0) {
                                    for (String mention : mentions) {
                                        newContent[0] += mention + " ";
                                    }
                                }
                                try {
                                    is = context.getAssets().open("quotes/famous.json");
                                    int size;
                                    size = is.available();
                                    byte[] buffer = new byte[size];
                                    //noinspection ResultOfMethodCallIgnored
                                    is.read(buffer);
                                    is.close();
                                    String json = new String(buffer, StandardCharsets.UTF_8);
                                    Gson gson = new Gson();
                                    List<Quotes.Quote> quotes = gson.fromJson(json, new TypeToken<List<Quotes.Quote>>() {
                                    }.getType());
                                    if (quotes != null && quotes.size() > 0) {
                                        final int random = new Random().nextInt(quotes.size());
                                        Quotes.Quote quote = quotes.get(random);
                                        newContent[0] += quote.content + "\n- " + quote.author;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                Runnable myRunnable = () -> {
                                    holder.binding.content.setText(newContent[0]);
                                    statusList.get(holder.getBindingAdapterPosition()).text = newContent[0];
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
                if (contentString.length() == 0)
                    currentCursorPosition[0] = 0;
                //Only check last 15 characters before cursor position to avoid lags
                //Less than 15 characters are written before the cursor position
                searchLength[0] = Math.min(currentCursorPosition[0], searchDeep);


                if (currentCursorPosition[0] - (searchLength[0] - 1) < 0 || currentCursorPosition[0] == 0 || currentCursorPosition[0] > contentString.length()) {
                    updateCharacterCount(holder);
                    return;
                }
                Matcher mathsPatterns = Helper.mathsComposePattern.matcher((contentString));
                if (mathsPatterns.find()) {
                    if (holder.binding.laTexViewContainer.getChildCount() == 0) {
                        MathJaxConfig mathJaxConfig = new MathJaxConfig();
                        mathJaxConfig.setAutomaticLinebreaks(true);
                        mathJaxConfig.setInput(TeX);
                        switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                            case Configuration.UI_MODE_NIGHT_YES ->
                                    mathJaxConfig.setTextColor("white");
                            case Configuration.UI_MODE_NIGHT_NO ->
                                    mathJaxConfig.setTextColor("black");
                        }
                        statusList.get(holder.getBindingAdapterPosition()).mathJaxView = new MathJaxView(context, mathJaxConfig);
                        holder.binding.laTexViewContainer.addView(statusList.get(holder.getBindingAdapterPosition()).mathJaxView);
                        holder.binding.laTexViewContainer.setVisibility(View.VISIBLE);
                    }
                    if (statusList.get(holder.getBindingAdapterPosition()).mathJaxView != null) {
                        statusList.get(holder.getBindingAdapterPosition()).mathJaxView.setInputText(contentString);
                    }

                } else {
                    holder.binding.laTexViewContainer.setVisibility(View.GONE);
                }

                String patternh = "^(.|\\s)*(:fedilab_hugs:)";
                final Pattern hPattern = Pattern.compile(patternh);
                Matcher mh = hPattern.matcher((contentString.substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])));

                if (mh.matches()) {
                    autocomplete = true;
                    return;
                }

                String patternM = "^(.|\\s)*(:fedilab_morse:)";
                final Pattern mPattern = Pattern.compile(patternM);
                Matcher mm = mPattern.matcher((contentString.substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])));
                if (mm.matches()) {
                    autocomplete = true;
                    return;
                }

                String patternQ = "^(.|\\s)*(:fedilab_quote:)";
                final Pattern qPattern = Pattern.compile(patternQ);
                Matcher mq = qPattern.matcher((contentString.substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])));
                if (mq.matches()) {
                    autocomplete = true;
                    return;
                }

                String[] searchInArray = (contentString.substring(currentCursorPosition[0] - searchLength[0], currentCursorPosition[0])).split("\\s");
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
                    accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, 5, false, false).observe((LifecycleOwner) context, accounts -> {
                        if (accounts == null) {
                            return;
                        }
                        int currentCursorPosition = holder.binding.content.getSelectionStart();
                        AccountsSearchAdapter accountsListAdapter = new AccountsSearchAdapter(context, accounts);
                        holder.binding.content.setThreshold(1);
                        holder.binding.content.setAdapter(accountsListAdapter);
                        final String oldContent = holder.binding.content.getText().toString();
                        if (oldContent.length() >= currentCursorPosition) {
                            String[] searchA = oldContent.substring(0, currentCursorPosition).split("\\s+@|^@|\\(+@");
                            if (searchA.length > 0) {
                                final String search = searchA[searchA.length - 1];
                                holder.binding.content.setOnItemClickListener((parent, view, position, id) -> {
                                    Account account = accounts.get(position);
                                    String deltaSearch = "";
                                    //Less than 15 characters are written before the cursor position
                                    int searchLength = Math.min(currentCursorPosition, searchDeep);
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
                                    statusList.get(holder.getBindingAdapterPosition()).text = newContent;
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
                        List<String> camelTags = new CamelTag(context).getBy(searchGroup);
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, searchGroup, null,
                                "hashtags", false, true, false, 0,
                                null, null, 10).observe((LifecycleOwner) context,
                                results -> {
                                    if (results == null || results.hashtags == null || results.hashtags.size() == 0) {
                                        return;
                                    }
                                    if (camelTags != null && camelTags.size() > 0) {
                                        for (String camelTag : camelTags) {
                                            Tag tag = new Tag();
                                            tag.name = camelTag;
                                            if (!results.hashtags.contains(tag)) {
                                                results.hashtags.add(0, tag);
                                            }
                                        }
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
                                        //Less than 15 characters are written before the cursor position
                                        int searchLength = Math.min(currentCursorPosition, searchDeep);
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
                                        statusList.get(holder.getBindingAdapterPosition()).text = newContent;
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
                            } else if (emojisList == null) {
                                if (emojis.containsKey(BaseMainActivity.currentInstance)) {
                                    emojisList = emojis.get(BaseMainActivity.currentInstance);
                                }
                            }
                            if (emojisList == null) {
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
                                        //Less than 15 characters are written before the cursor position
                                        int searchLength = Math.min(currentCursorPosition, searchDeep);
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
                                        statusList.get(holder.getBindingAdapterPosition()).text = newContent;
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

        if (description == null && content == null) {
            return;
        }

        StringBuilder contentBuilder = new StringBuilder();

        if (title != null && title.trim().length() > 0) {
            contentBuilder.append(title);
        } else if (subject != null && subject.trim().length() > 0) {
            contentBuilder.append(subject);
        }

        if (contentBuilder.length() > 0) {
            contentBuilder.append("\n\n");
        }

        if (description != null && description.trim().length() > 0) {
            if (url != null && !description.contains(url)) {
                contentBuilder.append(url).append("\n\n");
            }
            contentBuilder.append("> ").append(description);
        } else if (content != null && content.trim().length() > 0) {
            if (!content.contains(url)) {
                contentBuilder.append(url).append("\n\n");
            }
            contentBuilder.append("> ").append(content);
        } else {
            contentBuilder.append(url);
        }

        int position = statusList.size() - 1;
        statusList.get(position).text = contentBuilder.toString();

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
        if (currentCursorPosition < statusList.size()) {
            return statusList.get(currentCursorPosition).text != null ? statusList.get(currentCursorPosition).text : "";
        } else return "";
    }
    //------- end contact ----->

    //Used to write contact when composing
    public void updateContent(boolean checked, String acct) {
        if (currentCursorPosition < statusList.size()) {
            if (checked) {
                if (statusList.get(currentCursorPosition).text == null) {
                    statusList.get(currentCursorPosition).text = "";
                }
                if (!statusList.get(currentCursorPosition).text.contains(acct)) {
                    statusList.get(currentCursorPosition).text = String.format("@%s %s", acct, statusList.get(currentCursorPosition).text);
                }
            } else {
                statusList.get(currentCursorPosition).text = statusList.get(currentCursorPosition).text.replaceAll("@" + acct, "");
            }
            notifyItemChanged(currentCursorPosition);
        }
    }

    //Put cursor to the end after changing contacts
    public void putCursor() {
        statusList.get(statusList.size() - 1).setCursorToEnd = true;
        notifyItemChanged(statusList.size() - 1);
    }

    /**
     * Display attachment for a holder
     *
     * @param holder                - view related to a compose element {@link ComposeViewHolder}
     * @param position              - int position of the message in the thread
     * @param scrollToMediaPosition - int the position to scroll to media
     */
    private void displayAttachments(ComposeViewHolder holder, int position, int scrollToMediaPosition) {
        if (statusList.size() > position && statusList.get(position).media_attachments != null) {
            holder.binding.attachmentsList.removeAllViews();
            List<Attachment> attachmentList = statusList.get(position).media_attachments;
            if (attachmentList != null && attachmentList.size() > 0) {
                holder.binding.sensitiveMedia.setVisibility(View.VISIBLE);
                if (!statusList.get(position).sensitive) {
                    if (currentAccount != null && currentAccount.mastodon_account != null && currentAccount.mastodon_account.source != null) {
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
                    if (attachment.local_path != null && (attachment.local_path.endsWith("png") || attachment.local_path.endsWith("jpg") || attachment.local_path.endsWith("jpeg"))) {
                        composeAttachmentItemBinding.editPreview.setVisibility(View.VISIBLE);
                    } else {
                        composeAttachmentItemBinding.editPreview.setVisibility(View.GONE);
                    }
                    composeAttachmentItemBinding.editPreview.setOnClickListener(v -> {
                        Intent intent = new Intent(context, EditImageActivity.class);
                        Bundle b = new Bundle();
                        intent.putExtra("imageUri", attachment.local_path);
                        intent.putExtras(b);
                        context.startActivity(intent);
                    });
                    composeAttachmentItemBinding.buttonDescription.setOnClickListener(v -> mediaDescriptionCallBack.click(holder, attachment, position, finalMediaPosition));

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
                        AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                        composeAttachmentItemBinding.buttonDescription.setChipIconResource(R.drawable.ic_baseline_warning_24);
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(context, R.color.black));
                        composeAttachmentItemBinding.buttonDescription.setChipIconTintResource(R.color.black);
                        composeAttachmentItemBinding.buttonDescription.setChipBackgroundColor(ThemeHelper.getNoDescriptionColorStateList(context));
                    } else {
                        composeAttachmentItemBinding.buttonDescription.setChipIconResource(R.drawable.ic_baseline_check_circle_24);
                        composeAttachmentItemBinding.buttonDescription.setTextColor(ContextCompat.getColor(context, R.color.white));
                        composeAttachmentItemBinding.buttonDescription.setChipIconTintResource(R.color.white);
                        composeAttachmentItemBinding.buttonDescription.setChipBackgroundColor(ThemeHelper.getHavingDescriptionColorStateList(context));
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

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }


    public void openMissingDescription() {
        int position = 0;
        if (mRecyclerView == null) {
            return;
        }
        for (Status status : statusList) {
            if (getItemViewType(position) == TYPE_COMPOSE) {
                if (status != null && status.media_attachments != null && status.media_attachments.size() > 0) {
                    int mediaPosition = 0;
                    for (Attachment attachment : status.media_attachments) {
                        if (attachment.description == null || attachment.description.trim().isEmpty()) {
                            ComposeViewHolder composeViewHolder = (ComposeViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
                            mediaDescriptionCallBack.click(composeViewHolder, attachment, position, mediaPosition);
                            return;
                        }
                        mediaPosition++;
                    }
                }
            }
            position++;
        }
    }

    public void openDescriptionActivity(boolean saved, String content, ComposeViewHolder holder, Attachment attachment, int messagePosition, int mediaPosition) {
        if (saved) {
            attachment.description = content;
            displayAttachments(holder, messagePosition, mediaPosition);
        }
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
                holder.binding.buttonPoll.setEnabled(statusDraft.media_attachments == null || statusDraft.media_attachments.size() == 0);
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {


        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (getItemViewType(position) == TYPE_NORMAL) {
            Status status = statusList.get(position);
            StatusSimpleViewHolder holder = (StatusSimpleViewHolder) viewHolder;
            if(status.media_attachments != null && status.media_attachments.size() > 0 ) {
                holder.binding.simpleMedia.removeAllViews();
                List<Attachment> attachmentList = statusList.get(position).media_attachments;
                for(Attachment attachment: attachmentList) {
                    DrawerMediaListBinding drawerMediaListBinding = DrawerMediaListBinding.inflate(LayoutInflater.from(context), holder.binding.simpleMedia, false);
                    Glide.with(drawerMediaListBinding.media.getContext())
                            .load(attachment.preview_url)
                            .into(drawerMediaListBinding.media);

                    if(attachment.filename != null) {
                        drawerMediaListBinding.mediaName.setText(attachment.filename);
                    } else if (attachment.preview_url != null){
                        drawerMediaListBinding.mediaName.setText(URLUtil.guessFileName(attachment.preview_url, null, null));
                    }
                    drawerMediaListBinding.getRoot().setOnClickListener(v->{
                        Intent mediaIntent = new Intent(context, MediaActivity.class);
                        Bundle b = new Bundle();
                        ArrayList<Attachment> attachments = new ArrayList<>();
                        attachments.add(attachment);
                        b.putSerializable(Helper.ARG_MEDIA_ARRAY, attachments);
                        mediaIntent.putExtras(b);
                        ActivityOptionsCompat options = ActivityOptionsCompat
                                .makeSceneTransitionAnimation((Activity) context, drawerMediaListBinding.media, attachment.url);
                        context.startActivity(mediaIntent, options.toBundle());
                    });
                    holder.binding.simpleMedia.addView(drawerMediaListBinding.getRoot());

                }
                holder.binding.simpleMediaContainer.setVisibility(View.VISIBLE);
            } else {
                holder.binding.simpleMediaContainer.setVisibility(View.GONE);
            }
            holder.binding.statusContent.setText(
                    status.getSpanContent(context,
                            new WeakReference<>(holder.binding.statusContent), () -> mRecyclerView.post(() -> notifyItemChanged(position))),
                    TextView.BufferType.SPANNABLE);
            holder.binding.statusContent.setMovementMethod(LongClickLinkMovementMethod.getInstance());
            MastodonHelper.loadPPMastodon(holder.binding.avatar, status.account);
            if (status.account != null) {
                holder.binding.displayName.setText(
                        status.account.getSpanDisplayName(context,
                                new WeakReference<>(holder.binding.displayName)),
                        TextView.BufferType.SPANNABLE);
                holder.binding.username.setText(String.format("@%s", status.account.acct));
            }

            if (status.spoiler_text != null && !status.spoiler_text.trim().isEmpty()) {
                holder.binding.spoiler.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setText(
                        status.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.spoiler), null),
                        TextView.BufferType.SPANNABLE);
            } else {
                holder.binding.spoiler.setVisibility(View.GONE);
                holder.binding.spoiler.setText(null);
            }

        } else if (getItemViewType(position) == TYPE_COMPOSE) {
            Status statusDraft = statusList.get(position);

            ComposeViewHolder holder = (ComposeViewHolder) viewHolder;
            boolean extraFeatures = sharedpreferences.getBoolean(context.getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, false);
            boolean mathsComposer = sharedpreferences.getBoolean(context.getString(R.string.SET_MATHS_COMPOSER), true);
            boolean forwardTag = sharedpreferences.getBoolean(context.getString(R.string.SET_FORWARD_TAGS_IN_REPLY), false);


            if (mathsComposer) {
                holder.binding.buttonMathsComposer.setVisibility(View.VISIBLE);
                holder.binding.buttonMathsComposer.setOnClickListener(v -> {
                    int cursorPosition = holder.binding.content.getSelectionStart();
                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
                    Resources res = context.getResources();
                    String[] formatArr = res.getStringArray(R.array.SET_MATHS_FORMAT);
                    builder.setItems(formatArr, (dialogInterface, i) -> {
                        if (statusDraft.text == null) {
                            statusDraft.text = "";
                        }
                        if (i == 0) {
                            statusDraft.text = new StringBuilder(statusDraft.text).insert(cursorPosition, "\\(  \\)").toString();
                        } else {
                            statusDraft.text = new StringBuilder(statusDraft.text).insert(cursorPosition, "\\[  \\]").toString();
                        }
                        statusDraft.cursorPosition = cursorPosition + 3;
                        notifyItemChanged(position);
                        dialogInterface.dismiss();
                    });
                    builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builder.create().show();
                });
            } else {
                holder.binding.buttonMathsComposer.setVisibility(View.GONE);
            }

            holder.binding.buttonEmojiOne.setVisibility(View.VISIBLE);
            if (extraFeatures) {
                boolean displayLocalOnly = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_LOCAL_ONLY) + MainActivity.currentUserID + MainActivity.currentInstance, true);
                holder.binding.buttonTextFormat.setVisibility(View.VISIBLE);
                if (displayLocalOnly) {
                    holder.binding.buttonLocalOnly.setVisibility(View.VISIBLE);
                }
                holder.binding.buttonTextFormat.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(context.getString(R.string.post_format));
                    Resources res = context.getResources();
                    String[] formatArr = res.getStringArray(R.array.SET_POST_FORMAT);
                    int selection = 0;
                    String defaultFormat = sharedpreferences.getString(context.getString(R.string.SET_POST_FORMAT) + account.user_id + account.instance, "text/plain");
                    for (String format : formatArr) {
                        if (statusDraft.content_type != null && statusDraft.content_type.equalsIgnoreCase(format)) {
                            break;
                        } else if (statusDraft.content_type == null && defaultFormat.equalsIgnoreCase(format)) {
                            break;
                        }
                        selection++;
                    }
                    builder.setSingleChoiceItems(formatArr, selection, null);
                    builder.setPositiveButton(R.string.validate, (dialog, which) -> {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        statusDraft.content_type = formatArr[selectedPosition];
                        notifyItemChanged(holder.getLayoutPosition());
                        dialog.dismiss();
                    });
                    builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builder.create().show();
                });
                holder.binding.buttonLocalOnly.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(context.getString(R.string.local_only));
                    Resources res = context.getResources();
                    boolean[] valArr = new boolean[]{false, true};
                    String[] labelArr = res.getStringArray(R.array.set_local_only);

                    int selection = 0;
                    int localOnly = sharedpreferences.getInt(context.getString(R.string.SET_COMPOSE_LOCAL_ONLY) + account.user_id + account.instance, 0);
                    if (statusDraft.local_only || localOnly == 1) {
                        selection = 1;
                    }
                    builder.setSingleChoiceItems(labelArr, selection, null);
                    builder.setPositiveButton(R.string.validate, (dialog, which) -> {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        statusDraft.local_only = valArr[selectedPosition];
                        notifyItemChanged(holder.getLayoutPosition());
                        dialog.dismiss();
                    });
                    builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builder.create().show();
                });
            } else {
                holder.binding.buttonTextFormat.setVisibility(View.GONE);
            }
            holder.binding.buttonEmojiOne.setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(holder.binding.buttonEmojiOne.getWindowToken(), 0);
                EmojiManager.install(new EmojiOneProvider());
                final EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(holder.binding.buttonEmojiOne).setOnEmojiPopupDismissListener(() -> {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }).build(holder.binding.content);
                emojiPopup.toggle();
            });

            int newInputType = holder.binding.content.getInputType() & (holder.binding.content.getInputType() ^ InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            holder.binding.content.setInputType(newInputType);

            int newInputTypeSpoiler = holder.binding.contentSpoiler.getInputType() & (holder.binding.contentSpoiler.getInputType() ^ InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
            holder.binding.contentSpoiler.setInputType(newInputTypeSpoiler);
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
                holder.binding.buttonAttach.setChecked(false);
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
            if (visibility != null && statusDraft.visibility == null) {
                statusDraft.visibility = visibility;
            }
            boolean unlistedReplies = sharedpreferences.getBoolean(context.getString(R.string.SET_UNLISTED_REPLIES), true);
            if (statusDraft.visibility == null) {
                if (position > 0) {
                    statusDraft.visibility = statusList.get(position - 1).visibility;
                } else if (currentAccount.mastodon_account != null && currentAccount.mastodon_account.source != null) {
                    statusDraft.visibility = currentAccount.mastodon_account.source.privacy;
                } else {
                    statusDraft.visibility = "public";
                }
                if (!unlisted_changed && position == 0 && unlistedReplies && statusDraft.visibility.equalsIgnoreCase("public") && statusList.size() > 1) {
                    statusDraft.visibility = "unlisted";
                }
            } else if (!unlisted_changed && position > 0 && position == statusCount && unlistedReplies && statusDraft.visibility.equalsIgnoreCase("public") && statusList.size() > 1) {
                statusDraft.visibility = "unlisted";
            }

            switch (statusDraft.visibility.toLowerCase()) {
                case "public" -> {
                    holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_public);
                    statusDraft.visibility = MastodonHelper.visibility.PUBLIC.name();
                }
                case "unlisted" -> {
                    holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_unlisted);
                    statusDraft.visibility = MastodonHelper.visibility.UNLISTED.name();
                }
                case "private" -> {
                    holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_private);
                    statusDraft.visibility = MastodonHelper.visibility.PRIVATE.name();
                }
                case "direct" -> {
                    holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_direct);
                    statusDraft.visibility = MastodonHelper.visibility.DIRECT.name();
                }
            }

            holder.binding.visibilityPanel.setOnTouchListener((view, motionEvent) -> true);
            holder.binding.buttonCloseAttachmentPanel.setOnClickListener(v -> holder.binding.attachmentChoicesPanel.setVisibility(View.GONE));
            holder.binding.buttonVisibility.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.VISIBLE);
                holder.binding.visibilityGroup.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                holder.binding.buttonVisibility.setChecked(false);
            });
            holder.binding.buttonCloseVisibilityPanel.setOnClickListener(v -> holder.binding.visibilityPanel.setVisibility(View.GONE));
            holder.binding.buttonVisibilityDirect.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_direct);
                statusDraft.visibility = MastodonHelper.visibility.DIRECT.name();

            });
            holder.binding.buttonVisibilityPrivate.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_private);
                statusDraft.visibility = MastodonHelper.visibility.PRIVATE.name();
            });
            holder.binding.buttonVisibilityUnlisted.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_unlisted);
                statusDraft.visibility = MastodonHelper.visibility.UNLISTED.name();
            });
            holder.binding.buttonVisibilityPublic.setOnClickListener(v -> {
                holder.binding.visibilityPanel.setVisibility(View.GONE);
                holder.binding.buttonVisibility.setIconResource(R.drawable.ic_compose_visibility_public);
                statusDraft.visibility = MastodonHelper.visibility.PUBLIC.name();
                unlisted_changed = true;
            });

            if (statusDraft.spoilerChecked || statusDraft.spoiler_text != null && statusDraft.spoiler_text.trim().length() > 0) {
                holder.binding.contentSpoiler.setVisibility(View.VISIBLE);
            } else {
                holder.binding.contentSpoiler.setVisibility(View.GONE);
            }
            holder.binding.buttonSensitive.setChecked(statusDraft.spoilerChecked);
            holder.binding.buttonSensitive.setOnClickListener(v -> {
                if (holder.binding.contentSpoiler.getVisibility() == View.VISIBLE) {
                    statusDraft.spoilerChecked = false;
                    holder.binding.contentSpoiler.setVisibility(View.GONE);
                } else {
                    holder.binding.contentSpoiler.setVisibility(View.VISIBLE);
                    statusDraft.spoilerChecked = true;
                }
            });
            //Last compose drawer
            buttonVisibility(holder);


            holder.binding.buttonEmoji.setOnClickListener(v -> {
                try {
                    displayEmojiPicker(holder, account.instance);
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
            int max_car = MastodonHelper.getInstanceMaxChars(context);
            holder.binding.content.setText(statusDraft.text);
            holder.binding.characterProgress.setMax(max_car);
            updateCharacterCount(holder);
            holder.binding.content.setKeyBoardInputCallbackListener((inputContentInfo, flags, opts) -> {
                if (inputContentInfo != null) {
                    Uri uri = inputContentInfo.getContentUri();
                    List<Uri> uris = new ArrayList<>();
                    uris.add(uri);
                    addAttachment(position, uris);
                }
            });
            holder.binding.content.setOnFocusChangeListener((view, focused) -> {
                if (focused) {
                    currentCursorPosition = holder.getLayoutPosition();
                }
            });
            boolean capitalize = sharedpreferences.getBoolean(context.getString(R.string.SET_CAPITALIZE), true);
            boolean mentionsAtTop = sharedpreferences.getBoolean(context.getString(R.string.SET_MENTIONS_AT_TOP), false);
            if (statusDraft.cursorPosition <= holder.binding.content.length()) {
                if (!mentionsAtTop) {
                    holder.binding.content.setSelection(statusDraft.cursorPosition);
                } else {
                    if (capitalize && statusDraft.text != null && !statusDraft.text.endsWith("\n")) {
                        statusDraft.text += "\n";
                        holder.binding.content.setText(statusDraft.text);
                    }
                    holder.binding.content.setSelection(holder.binding.content.getText().length());
                }
            }
            if (statusDraft.setCursorToEnd) {
                statusDraft.setCursorToEnd = false;
                holder.binding.content.setSelection(holder.binding.content.getText().length());
            }
            if (forwardTag && position > 0 && statusDraft.text != null && !statusDraft.text.contains("#") && !statusList.get(position).tagAdded) {
                statusList.get(position).tagAdded = true;
                Status status = statusList.get(position - 1).reblog == null ? statusList.get(position - 1) : statusList.get(position - 1).reblog;
                if (status.text == null && status.content != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        status.text = new SpannableString(Html.fromHtml(status.content, Html.FROM_HTML_MODE_LEGACY)).toString();
                    else
                        status.text = new SpannableString(Html.fromHtml(status.content)).toString();
                }
                List<String> camelCaseTags = new ArrayList<>();
                Matcher matcher = Helper.hashtagPattern.matcher(status.text);
                while (matcher.find()) {
                    int matchStart = matcher.start(1);
                    int matchEnd = matcher.end();
                    //Get cached tags
                    if (matchStart >= 0 && matchEnd < status.text.length()) {
                        String tag = status.text.substring(matchStart, matchEnd);
                        tag = tag.replace("#", "");
                        camelCaseTags.add(tag);
                    }
                }
                if (camelCaseTags.size() > 0) {
                    statusDraft.text += "\n\n";
                    int lenght = 0;
                    for (String tag : camelCaseTags) {
                        statusDraft.text += "#" + tag + " ";
                        lenght += ("#" + tag + " ").length();
                    }
                    holder.binding.content.setText(statusDraft.text);
                    if (statusDraft.text.length() - lenght - 3 >= 0) {
                        statusDraft.cursorPosition = statusDraft.text.length() - lenght - 3;
                        statusDraft.setCursorToEnd = false;
                        holder.binding.content.setSelection(statusDraft.text.length() - lenght - 3);
                    }
                }
            } else if (forwardTag && position > 0 && statusDraft.text != null && statusDraft.text.contains("#") && !statusList.get(position).tagAdded) {
                Status status = statusList.get(position - 1).reblog == null ? statusList.get(position - 1) : statusList.get(position - 1).reblog;
                if (status.text == null && status.content != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        status.text = new SpannableString(Html.fromHtml(status.content, Html.FROM_HTML_MODE_LEGACY)).toString();
                    else
                        status.text = new SpannableString(Html.fromHtml(status.content)).toString();
                }
                List<String> camelCaseTags = new ArrayList<>();
                Matcher matcher = Helper.hashtagPattern.matcher(status.text);
                while (matcher.find()) {
                    int matchStart = matcher.start(1);
                    int matchEnd = matcher.end();
                    //Get cached tags
                    if (matchStart >= 0 && matchEnd < status.text.length()) {
                        String tag = status.text.substring(matchStart, matchEnd);
                        tag = tag.replace("#", "");
                        camelCaseTags.add(tag);
                    }
                }
                if (camelCaseTags.size() > 0) {
                    statusList.get(position).tagAdded = true;
                    int lenght = 0;
                    for (String tag : camelCaseTags) {
                        lenght += ("#" + tag + " ").length();
                    }
                    if (statusDraft.text.length() - lenght - 3 >= 0) {
                        statusDraft.cursorPosition = statusDraft.text.length() - lenght - 3;
                        statusDraft.setCursorToEnd = false;
                        holder.binding.content.setSelection(statusDraft.text.length() - lenght - 3);
                    }
                }
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
            if (instanceInfo == null) {
                return;
            }
            holder.binding.characterProgress.setMax(max_car);
            holder.binding.contentSpoiler.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    buttonVisibility(holder);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (promptDraftListener != null) {
                        promptDraftListener.promptDraft();
                    }
                    int currentLength = MastodonHelper.countLength(holder);
                    int max_car = MastodonHelper.getInstanceMaxChars(context);
                    if (currentLength > max_car) {
                        holder.binding.characterCount.setTextColor(Color.RED);
                    } else {
                        holder.binding.characterCount.setTextColor(holder.binding.content.getTextColors());
                    }
                    statusList.get(holder.getBindingAdapterPosition()).spoiler_text = s.toString();
                    if (s.toString().trim().length() < 2) {
                        buttonVisibility(holder);
                    }
                    updateCharacterCount(holder);
                }
            });

            holder.binding.buttonPost.setEnabled(!statusDraft.submitted);

            holder.binding.buttonPost.setOnClickListener(v -> {
                statusDraft.submitted = true;
                notifyItemChanged(position);
                manageDrafts.onSubmit(prepareDraft(statusList, this, account.instance, account.user_id));
            });


            if (statusDraft.language == null || statusDraft.language.isEmpty()) {
                String currentCode = sharedpreferences.getString(context.getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, Locale.getDefault().getLanguage());
                if (currentCode.isEmpty()) {
                    currentCode = "EN";
                }
                statusDraft.language = currentCode;
            }
            holder.binding.buttonLanguage.setText(statusDraft.language);


            holder.binding.buttonLanguage.setOnClickListener(v -> {
                holder.binding.buttonLanguage.setChecked(false);
                Set<String> storedLanguages = sharedpreferences.getStringSet(context.getString(R.string.SET_SELECTED_LANGUAGE), null);
                String[] codesArr = new String[0];
                String[] languagesArr = new String[0];

                int selection = 0;
                if (storedLanguages != null && storedLanguages.size() > 0) {
                    int i = 0;
                    codesArr = new String[storedLanguages.size()];
                    languagesArr = new String[storedLanguages.size()];
                    for (String language : storedLanguages) {
                        codesArr[i] = language;
                        languagesArr[i] = language;
                        if (statusDraft.language.equalsIgnoreCase(language)) {
                            selection = i;
                        }
                        i++;
                    }
                } else {

                    List<Languages.Language> languages = Languages.get(context);
                    if (languages != null) {
                        codesArr = new String[languages.size()];
                        languagesArr = new String[languages.size()];
                        int i = 0;
                        for (Languages.Language language : languages) {
                            codesArr[i] = language.code;
                            languagesArr[i] = language.language;
                            if (statusDraft.language.equalsIgnoreCase(language.code)) {
                                selection = i;
                            }
                            i++;
                        }
                    }
                }

                SharedPreferences.Editor editor = sharedpreferences.edit();
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(context.getString(R.string.message_language));

                builder.setSingleChoiceItems(languagesArr, selection, null);
                String[] finalCodesArr = codesArr;
                builder.setPositiveButton(R.string.validate, (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    editor.putString(context.getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, finalCodesArr[selectedPosition]);
                    editor.apply();
                    statusDraft.language = finalCodesArr[selectedPosition];
                    notifyItemChanged(holder.getLayoutPosition());
                    dialog.dismiss();
                });
                builder.setNegativeButton(R.string.reset, (dialog, which) -> {
                    editor.putString(context.getString(R.string.SET_COMPOSE_LANGUAGE) + account.user_id + account.instance, null);
                    editor.apply();
                    statusDraft.language = null;
                    notifyItemChanged(holder.getLayoutPosition());
                    dialog.dismiss();
                });
                builder.create().show();
            });
        }

    }

    /**
     * Display the popup to attach a poll to message
     *
     * @param holder      - view for the message {@link ComposeViewHolder}
     * @param statusDraft - Status message instance  {@link Status} linked to the view
     * @param position    - int position
     */
    private void displayPollPopup(ComposeViewHolder holder, Status statusDraft, int position) {
        AlertDialog.Builder alertPoll = new MaterialAlertDialogBuilder(context);
        alertPoll.setTitle(R.string.create_poll);
        ComposePollBinding composePollBinding = ComposePollBinding.inflate(LayoutInflater.from(context), new LinearLayout(context), false);
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
        composePollBinding.option1.textLayout.setHint(context.getString(R.string.poll_choice_s, 1));
        composePollBinding.option2.text.setFilters(fArray);
        composePollBinding.option2.textLayout.setHint(context.getString(R.string.poll_choice_s, 2));
        composePollBinding.option1.buttonRemove.setVisibility(View.GONE);
        composePollBinding.option2.buttonRemove.setVisibility(View.GONE);
        int finalMax_entry = max_entry;
        composePollBinding.buttonAddOption.setOnClickListener(v -> {
            if (pollCountItem[0] < finalMax_entry) {
                ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(composePollBinding.optionsList.getContext()), composePollBinding.optionsList, false);
                if (composePollBinding.pollType.getCheckedButtonId() == R.id.poll_type_multiple)
                    composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                composePollItemBinding.text.setFilters(fArray);
                composePollItemBinding.textLayout.setHint(context.getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
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
        composePollBinding.pollDuration.setAdapter(pollduration);
        composePollBinding.pollDuration.setSelection(4);
        if (statusDraft != null && statusDraft.poll != null && statusDraft.poll.options != null) {
            int i = 1;
            for (Poll.PollItem pollItem : statusDraft.poll.options) {
                if (i == 1) {
                    if (statusDraft.poll.multiple)
                        composePollBinding.option1.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    if (pollItem.title != null)
                        composePollBinding.option1.text.setText(pollItem.title);
                } else if (i == 2) {
                    if (statusDraft.poll.multiple)
                        composePollBinding.option2.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    if (pollItem.title != null)
                        composePollBinding.option2.text.setText(pollItem.title);
                } else {

                    ComposePollItemBinding composePollItemBinding = ComposePollItemBinding.inflate(LayoutInflater.from(context), new LinearLayout(context), false);
                    if (composePollBinding.pollType.getCheckedButtonId() == R.id.poll_type_multiple)
                        composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_multiple);
                    else
                        composePollItemBinding.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_single);

                    composePollItemBinding.text.setFilters(fArray);
                    composePollItemBinding.textLayout.setHint(context.getString(R.string.poll_choice_s, (pollCountItem[0] + 1)));
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
                case 300 -> composePollBinding.pollDuration.setSelection(0);
                case 1800 -> composePollBinding.pollDuration.setSelection(1);
                case 3600 -> composePollBinding.pollDuration.setSelection(2);
                case 21600 -> composePollBinding.pollDuration.setSelection(3);
                case 86400 -> composePollBinding.pollDuration.setSelection(4);
                case 259200 -> composePollBinding.pollDuration.setSelection(5);
                case 604800 -> composePollBinding.pollDuration.setSelection(6);
            }
            if (statusDraft.poll.multiple)
                composePollBinding.pollType.check(R.id.poll_type_multiple);
            else
                composePollBinding.pollType.check(R.id.poll_type_single);


        }
        alertPoll.setNegativeButton(R.string.delete, (dialog, whichButton) -> {
            if (statusDraft != null && statusDraft.poll != null) statusDraft.poll = null;
            buttonState(holder);
            dialog.dismiss();
            notifyItemChanged(position);
        });
        alertPoll.setPositiveButton(R.string.save, null);
        final AlertDialog alertPollDiaslog = alertPoll.create();
        alertPollDiaslog.setOnShowListener(dialog -> {

            composePollBinding.pollType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.poll_type_single) {
                        if (statusDraft != null && statusDraft.poll != null)
                            statusDraft.poll.multiple = false;
                        for (int i = 0; i < composePollBinding.optionsList.getChildCount(); i++) {
                            ComposePollItemBinding child = ComposePollItemBinding.bind(composePollBinding.optionsList.getChildAt(i));
                            child.typeIndicator.setImageResource(R.drawable.ic_compose_poll_option_mark_single);
                        }
                    } else if (checkedId == R.id.poll_type_multiple) {
                        if (statusDraft != null && statusDraft.poll != null)
                            statusDraft.poll.multiple = true;
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
                String choice1 = Objects.requireNonNull(composePollBinding.option1.text.getText()).toString().trim();
                String choice2 = Objects.requireNonNull(composePollBinding.option2.text.getText()).toString().trim();

                if (choice1.isEmpty() && choice2.isEmpty()) {
                    Toasty.error(context, context.getString(R.string.poll_invalid_choices), Toasty.LENGTH_SHORT).show();
                } else if (statusDraft != null) {
                    statusDraft.poll = new Poll();
                    statusDraft.poll.multiple = selected_poll_type_id == R.id.poll_type_multiple;
                    statusDraft.poll.expire_in = switch (poll_duration_pos) {
                        case 0 -> 300;
                        case 1 -> 1800;
                        case 2 -> 3600;
                        case 3 -> 21600;
                        case 4 -> 86400;
                        case 5 -> 259200;
                        case 6 -> 604800;
                        default -> 864000;
                    };
                    if (promptDraftListener != null) {
                        promptDraftListener.promptDraft();
                    }
                    List<Poll.PollItem> pollItems = new ArrayList<>();
                    int childCount = composePollBinding.optionsList.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        Poll.PollItem pollItem = new Poll.PollItem();
                        AppCompatEditText title = (composePollBinding.optionsList.getChildAt(i)).findViewById(R.id.text);
                        pollItem.title = Objects.requireNonNull(title.getText()).toString();
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

    /**
     * Display the emoji picker in the current message
     *
     * @param holder - view for the message {@link ComposeViewHolder}
     */
    private void displayEmojiPicker(ComposeViewHolder holder, String instance) throws DBException {

        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
        int paddingPixel = 15;
        float density = context.getResources().getDisplayMetrics().density;
        int paddingDp = (int) (paddingPixel * density);
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.setTitle(R.string.insert_emoji);
        if (emojis != null && emojis.size() > 0) {
            GridView gridView = new GridView(context);
            gridView.setAdapter(new EmojiAdapter(emojis.get(instance)));
            gridView.setNumColumns(5);
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                holder.binding.content.getText().insert(holder.binding.content.getSelectionStart(), " :" + Objects.requireNonNull(emojis.get(instance)).get(position).shortcode + ": ");
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


    public interface MediaDescriptionCallBack {
        void click(ComposeViewHolder holder, Attachment attachment, int messagePosition, int mediaPosition);
    }

    public interface promptDraftListener {
        void promptDraft();
    }

    public interface ManageDrafts {
        void onItemDraftAdded(int position, String content);

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