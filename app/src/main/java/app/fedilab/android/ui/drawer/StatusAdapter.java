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
import static app.fedilab.android.BaseMainActivity.regex_home;
import static app.fedilab.android.BaseMainActivity.regex_local;
import static app.fedilab.android.BaseMainActivity.regex_public;
import static app.fedilab.android.BaseMainActivity.show_boosts;
import static app.fedilab.android.BaseMainActivity.show_replies;
import static app.fedilab.android.activities.ContextActivity.expand;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.github.stom79.mytransl.MyTransL;
import com.github.stom79.mytransl.client.HttpsConnectionException;
import com.github.stom79.mytransl.client.Results;
import com.github.stom79.mytransl.translate.Params;
import com.github.stom79.mytransl.translate.Translate;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.one.EmojiOneProvider;
import com.varunest.sparkbutton.SparkButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.CustomSharingActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.activities.ReportActivity;
import app.fedilab.android.activities.StatusHistoryActivity;
import app.fedilab.android.activities.StatusInfoActivity;
import app.fedilab.android.activities.admin.AdminAccountActivity;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Poll;
import app.fedilab.android.client.entities.api.Reaction;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.client.entities.app.BaseAccount;
import app.fedilab.android.client.entities.app.StatusCache;
import app.fedilab.android.client.entities.app.StatusDraft;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.DrawerStatusArtBinding;
import app.fedilab.android.databinding.DrawerStatusBinding;
import app.fedilab.android.databinding.DrawerStatusFilteredBinding;
import app.fedilab.android.databinding.DrawerStatusHiddenBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.DrawerStatusReportBinding;
import app.fedilab.android.databinding.LayoutMediaBinding;
import app.fedilab.android.databinding.LayoutPollItemBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.GlideFocus;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.LongClickLinkMovementMethod;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.MediaHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.helper.TimelineHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonContext;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.viewmodel.pleroma.ActionsVM;
import es.dmoral.toasty.Toasty;
import jp.wasabeef.glide.transformations.BlurTransformation;


public class StatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_VISIBLE = 1;
    public static final int STATUS_ART = 2;
    public static final int STATUS_FILTERED = 3;
    private final List<Status> statusList;
    private final boolean minified;
    private final Timeline.TimeLineEnum timelineType;
    private final boolean canBeFederated;
    private final boolean checkRemotely;
    public FetchMoreCallBack fetchMoreCallBack;
    private Context context;

    public StatusAdapter(List<Status> statuses, Timeline.TimeLineEnum timelineType, boolean minified, boolean canBeFederated, boolean checkRemotely) {
        this.statusList = statuses;
        this.timelineType = timelineType;
        this.minified = minified;
        this.canBeFederated = canBeFederated;
        this.checkRemotely = checkRemotely;
    }


    private static boolean isVisible(Timeline.TimeLineEnum timelineType, Status status) {
        if (timelineType == Timeline.TimeLineEnum.HOME && !show_boosts && status.reblog != null) {
            return false;
        }
        if (timelineType == Timeline.TimeLineEnum.HOME && !show_replies && status.in_reply_to_id != null) {
            return false;
        }
        if (timelineType == Timeline.TimeLineEnum.HOME && regex_home != null && !regex_home.trim().equals("")) {
            try {
                Pattern filterPattern = Pattern.compile("(" + regex_home + ")", Pattern.CASE_INSENSITIVE);
                Matcher matcher = filterPattern.matcher(status.content);
                if (matcher.find())
                    return false;
                matcher = filterPattern.matcher(status.spoiler_text);
                if (matcher.find())
                    return false;
            } catch (Exception ignored) {
            }
        }
        if (timelineType == Timeline.TimeLineEnum.LOCAL && regex_local != null && !regex_local.trim().equals("")) {
            try {
                Pattern filterPattern = Pattern.compile("(" + regex_local + ")", Pattern.CASE_INSENSITIVE);
                Matcher matcher = filterPattern.matcher(status.content);
                if (matcher.find())
                    return false;
                matcher = filterPattern.matcher(status.spoiler_text);
                if (matcher.find())
                    return false;
            } catch (Exception ignored) {
            }
        }
        if (timelineType == Timeline.TimeLineEnum.PUBLIC && regex_public != null && !regex_public.trim().equals("")) {
            try {
                Pattern filterPattern = Pattern.compile("(" + regex_public + ")", Pattern.CASE_INSENSITIVE);
                Matcher matcher = filterPattern.matcher(status.content);
                if (matcher.find())
                    return false;
                matcher = filterPattern.matcher(status.spoiler_text);
                if (matcher.find())
                    return false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }


    /***
     * Methode that will deal with results of actions (bookmark, favourite, boost)
     * @param context Context
     * @param adapter RecyclerView.Adapter<RecyclerView.ViewHolder>
     * @param holder StatusViewHolder used by the reycler
     * @param typeOfAction CrossActionHelper.TypeOfCrossAction
     * @param statusToDeal Status that received the action
     * @param statusReturned Status returned by the API
     * @param remote boolean - it's a remote message
     */
    private static void manageAction(Context context,
                                     RecyclerView.Adapter<RecyclerView.ViewHolder> adapter,
                                     StatusViewHolder holder,
                                     CrossActionHelper.TypeOfCrossAction typeOfAction,
                                     Status statusToDeal,
                                     Status statusReturned,
                                     boolean remote) {
        if (statusReturned == null) {
            switch (typeOfAction) {
                case BOOKMARK_ACTION:
                    statusToDeal.bookmarked = true;
                    break;
                case REBLOG_ACTION:
                    statusToDeal.reblogged = true;
                    statusToDeal.reblogs_count++;
                    break;
                case FAVOURITE_ACTION:
                    statusToDeal.favourited = true;
                    statusToDeal.favourites_count++;
                    break;
                case UNBOOKMARK_ACTION:
                    statusToDeal.bookmarked = false;
                    break;
                case UNREBLOG_ACTION:
                    statusToDeal.reblogged = false;
                    statusToDeal.reblogs_count--;
                    break;
                case UNFAVOURITE_ACTION:
                    statusToDeal.favourited = false;
                    statusToDeal.favourites_count--;
                    break;
            }
        } else {
            boolean isOK = true;
            switch (typeOfAction) {
                case BOOKMARK_ACTION:
                    isOK = statusReturned.bookmarked;
                    break;
                case REBLOG_ACTION:
                    isOK = statusReturned.reblogged;
                    break;
                case FAVOURITE_ACTION:
                    isOK = statusReturned.favourited;
                    break;
                case UNBOOKMARK_ACTION:
                    isOK = !statusReturned.bookmarked;
                    break;
                case UNREBLOG_ACTION:
                    isOK = !statusReturned.reblogged;
                    break;
                case UNFAVOURITE_ACTION:
                    isOK = !statusReturned.favourited;
                    break;
            }
            if (!isOK) {
                Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                return;
            }
            //Update elements
            statusToDeal.favourited = statusReturned.favourited;
            statusToDeal.reblogged = statusReturned.reblogged;
            statusToDeal.bookmarked = statusReturned.bookmarked;

            if (!remote) {
                if (statusReturned.reblog != null) {
                    statusToDeal.reblogs_count = statusReturned.reblog.reblogs_count;
                    statusToDeal.favourites_count = statusReturned.reblog.favourites_count;
                } else {
                    statusToDeal.reblogs_count = statusReturned.reblogs_count;
                    statusToDeal.favourites_count = statusReturned.favourites_count;
                }
            } else {
                switch (typeOfAction) {
                    case REBLOG_ACTION:
                        statusToDeal.reblogs_count++;
                        break;
                    case FAVOURITE_ACTION:
                        statusToDeal.favourites_count++;
                        break;
                    case UNREBLOG_ACTION:
                        statusToDeal.reblogs_count--;
                        break;
                    case UNFAVOURITE_ACTION:
                        statusToDeal.favourites_count--;
                        break;
                }
            }
        }
        //Update status in cache if not a remote instance
        if (!remote) {
            new Thread(() -> {
                StatusCache statusCache = new StatusCache();
                statusCache.instance = BaseMainActivity.currentInstance;
                statusCache.user_id = BaseMainActivity.currentUserID;
                statusCache.status = statusToDeal;
                statusCache.status_id = statusToDeal.id;
                try {
                    new StatusCache(context).updateIfExists(statusCache);
                } catch (DBException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
    }

    /**
     * Manage status, this method is also reused in notifications timelines
     *
     * @param context      Context
     * @param statusesVM   StatusesVM - For handling actions in background to the correct activity
     * @param searchVM     SearchVM - For handling remote actions
     * @param holder       StatusViewHolder
     * @param adapter      RecyclerView.Adapter<RecyclerView.ViewHolder> - General adapter that can be for {@link StatusAdapter} or {@link NotificationAdapter}
     * @param statusList   List<Status>
     * @param timelineType Timeline.TimeLineEnum timelineTypeTimeline.TimeLineEnum
     * @param status       {@link Status}
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void statusManagement(Context context,
                                        StatusesVM statusesVM,
                                        SearchVM searchVM,
                                        StatusViewHolder holder,
                                        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter,
                                        List<Status> statusList,
                                        Status status,
                                        Timeline.TimeLineEnum timelineType,
                                        boolean minified, boolean canBeFederated, boolean checkRemotely,
                                        FetchMoreCallBack fetchMoreCallBack) {
        if (status == null) {
            return;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean remote = timelineType == Timeline.TimeLineEnum.REMOTE || checkRemotely;

        Status statusToDeal = status.reblog != null ? status.reblog : status;


        boolean expand_cw = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_CW), false);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);
        boolean display_card = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_CARD), false);
        boolean share_details = sharedpreferences.getBoolean(context.getString(R.string.SET_SHARE_DETAILS), true);
        boolean confirmFav = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION_FAV), false);
        boolean confirmBoost = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION), true);
        boolean fullAttachement = sharedpreferences.getBoolean(context.getString(R.string.SET_FULL_PREVIEW), false);
        boolean displayBookmark = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_BOOKMARK), false);
        boolean long_press_media = sharedpreferences.getBoolean(context.getString(R.string.SET_LONG_PRESS_STORE_MEDIA), false);
        boolean displayCounters = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_COUNTER_FAV_BOOST), false);
        String loadMediaType = sharedpreferences.getString(context.getString(R.string.SET_LOAD_MEDIA_TYPE), "ALWAYS");

        if (currentAccount != null && currentAccount.api == Account.API.PLEROMA) {
            if (status.pleroma != null && status.pleroma.emoji_reactions != null && status.pleroma.emoji_reactions.size() > 0) {
                holder.binding.layoutReactions.getRoot().setVisibility(View.VISIBLE);
                ReactionAdapter reactionAdapter = new ReactionAdapter(status.id, status.pleroma.emoji_reactions, true);
                holder.binding.layoutReactions.reactionsView.setAdapter(reactionAdapter);
                LinearLayoutManager layoutManager
                        = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                holder.binding.layoutReactions.reactionsView.setLayoutManager(layoutManager);
            } else {
                holder.binding.layoutReactions.getRoot().setVisibility(View.GONE);
                holder.binding.layoutReactions.reactionsView.setAdapter(null);
            }
            holder.binding.statusEmoji.setOnClickListener(v -> {
                EmojiManager.install(new EmojiOneProvider());
                final EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(holder.binding.statusEmoji).setOnEmojiPopupDismissListener(() -> {
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(holder.binding.statusEmoji.getWindowToken(), 0);
                        }).setOnEmojiClickListener((emoji, imageView) -> {
                            String emojiStr = imageView.getUnicode();
                            boolean alreadyAdded = false;
                            if (status.pleroma == null || status.pleroma.emoji_reactions == null) {
                                return;
                            }
                            for (Reaction reaction : status.pleroma.emoji_reactions) {
                                if (reaction.name.compareTo(emojiStr) == 0 && reaction.me) {
                                    alreadyAdded = true;
                                    reaction.count = (reaction.count - 1);
                                    if (reaction.count == 0) {
                                        status.pleroma.emoji_reactions.remove(reaction);
                                    }
                                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                Reaction reaction = new Reaction();
                                reaction.me = true;
                                reaction.count = 1;
                                reaction.name = emojiStr;
                                status.pleroma.emoji_reactions.add(0, reaction);
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                            }
                            ActionsVM actionVM = new ViewModelProvider((ViewModelStoreOwner) context).get(ActionsVM.class);
                            if (alreadyAdded) {
                                actionVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                            } else {
                                actionVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                            }
                        })
                        .build(holder.binding.layoutReactions.fakeEdittext);
                emojiPopup.toggle();
            });
            holder.binding.statusAddCustomEmoji.setOnClickListener(v -> {

                final AlertDialog.Builder builder = new AlertDialog.Builder(context, Helper.dialogStyle());
                int paddingPixel = 15;
                float density = context.getResources().getDisplayMetrics().density;
                int paddingDp = (int) (paddingPixel * density);
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builder.setTitle(R.string.insert_emoji);

                if (emojis != null && emojis.size() > 0 && emojis.get(BaseMainActivity.currentInstance) != null) {
                    GridView gridView = new GridView(context);
                    gridView.setAdapter(new EmojiAdapter(emojis.get(BaseMainActivity.currentInstance)));
                    gridView.setNumColumns(5);
                    gridView.setOnItemClickListener((parent, view, index, id) -> {
                        String emojiStr = emojis.get(BaseMainActivity.currentInstance).get(index).shortcode;
                        String url = emojis.get(BaseMainActivity.currentInstance).get(index).url;
                        String static_url = emojis.get(BaseMainActivity.currentInstance).get(index).static_url;
                        boolean alreadyAdded = false;
                        if (status.pleroma == null || status.pleroma.emoji_reactions == null) {
                            return;
                        }
                        for (Reaction reaction : status.pleroma.emoji_reactions) {
                            if (reaction.name.compareTo(emojiStr) == 0 && reaction.me) {
                                alreadyAdded = true;
                                reaction.count = (reaction.count - 1);
                                if (reaction.count == 0) {
                                    status.pleroma.emoji_reactions.remove(reaction);
                                }
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                                break;
                            }
                        }
                        if (!alreadyAdded) {
                            Reaction reaction = new Reaction();
                            reaction.me = true;
                            reaction.count = 1;
                            reaction.name = emojiStr;
                            reaction.url = url;
                            reaction.static_url = static_url;
                            status.pleroma.emoji_reactions.add(0, reaction);
                            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                        }
                        ActionsVM actionsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(ActionsVM.class);
                        if (alreadyAdded) {
                            actionsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                        } else {
                            actionsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                        }
                    });
                    gridView.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
                    builder.setView(gridView);
                } else {
                    TextView textView = new TextView(context);
                    textView.setText(context.getString(R.string.no_emoji));
                    textView.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
                    builder.setView(textView);
                }
                builder.show();
            });
        }

        int truncate_toots_size = sharedpreferences.getInt(context.getString(R.string.SET_TRUNCATE_TOOTS_SIZE), 0);
        // boolean display_video_preview = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_VIDEO_PREVIEWS), true);
        //    boolean isModerator = sharedpreferences.getBoolean(Helper.PREF_IS_MODERATOR, false);
        //   boolean isAdmin = sharedpreferences.getBoolean(Helper.PREF_IS_ADMINISTRATOR, false);
        int theme_icons_color = -1;
        int theme_statuses_color = -1;
        int theme_boost_header_color = -1;
        int theme_text_color = -1;
        int theme_text_header_1_line = -1;
        int theme_text_header_2_line = -1;
        int link_color = -1;
        if (sharedpreferences.getBoolean("use_custom_theme", false)) {
            //Getting custom colors
            theme_icons_color = sharedpreferences.getInt("theme_icons_color", -1);
            theme_statuses_color = sharedpreferences.getInt("theme_statuses_color", -1);
            theme_boost_header_color = sharedpreferences.getInt("theme_boost_header_color", -1);
            theme_text_color = sharedpreferences.getInt("theme_text_color", -1);
            theme_text_header_1_line = sharedpreferences.getInt("theme_text_header_1_line", -1);
            theme_text_header_2_line = sharedpreferences.getInt("theme_text_header_2_line", -1);
            theme_text_color = sharedpreferences.getInt("theme_text_color", -1);
            link_color = sharedpreferences.getInt("theme_link_color", -1);

        }
        if (currentAccount != null && currentAccount.api == Account.API.PLEROMA) {
            holder.binding.statusAddCustomEmoji.setVisibility(View.VISIBLE);
            holder.binding.statusEmoji.setVisibility(View.VISIBLE);
        }

        Helper.changeDrawableColor(context, R.drawable.ic_baseline_mode_edit_message_24, R.color.cyanea_accent_reference);
        if (theme_icons_color != -1) {
            Helper.changeDrawableColor(context, holder.binding.actionButtonReply, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.cacheIndicator, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.statusAddCustomEmoji, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.statusEmoji, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.actionButtonMore, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_baseline_star_24, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_repeat, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.visibility, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_star_outline, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_person, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_bot, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_baseline_reply_16, theme_icons_color);
            holder.binding.actionButtonFavorite.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonBookmark.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonBoost.setInActiveImageTintColor(theme_icons_color);
            holder.binding.replyCount.setTextColor(theme_icons_color);
        } else {
            holder.binding.actionButtonFavorite.setInActiveImageTintColor(ThemeHelper.getAttColor(context, R.attr.colorControlNormal));
            holder.binding.actionButtonBookmark.setInActiveImageTintColor(ThemeHelper.getAttColor(context, R.attr.colorControlNormal));
            holder.binding.actionButtonBoost.setInActiveImageTintColor(ThemeHelper.getAttColor(context, R.attr.colorControlNormal));
        }

        holder.binding.actionButtonFavorite.pressOnTouch(false);
        holder.binding.actionButtonBoost.pressOnTouch(false);
        holder.binding.actionButtonBookmark.pressOnTouch(false);
        holder.binding.actionButtonFavorite.setActiveImage(R.drawable.ic_baseline_star_24);
        holder.binding.actionButtonFavorite.setInactiveImage(R.drawable.ic_star_outline);
        holder.binding.actionButtonBookmark.setInactiveImage(R.drawable.ic_baseline_bookmark_border_24);
        holder.binding.actionButtonFavorite.setDisableCircle(true);
        holder.binding.actionButtonBoost.setDisableCircle(true);
        holder.binding.actionButtonBookmark.setDisableCircle(true);
        holder.binding.actionButtonFavorite.setActiveImageTint(R.color.marked_icon);
        holder.binding.actionButtonBoost.setActiveImageTint(R.color.boost_icon);
        holder.binding.actionButtonBookmark.setActiveImageTint(R.color.marked_icon);

        if (status.cached) {
            holder.binding.cacheIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.binding.cacheIndicator.setVisibility(View.GONE);
        }


        if (status.pinned) {
            holder.binding.statusPinned.setVisibility(View.VISIBLE);
        } else {
            holder.binding.statusPinned.setVisibility(View.GONE);
        }

        if (theme_text_header_2_line != -1) {
            Pattern hashAcct;
            SpannableString wordToSpan;
            if (status.reblog != null) {
                wordToSpan = new SpannableString("@" + status.reblog.account.acct);
                hashAcct = Pattern.compile("(@" + status.reblog.account.acct + ")");
            } else {
                wordToSpan = new SpannableString("@" + status.account.acct);
                hashAcct = Pattern.compile("(@" + status.account.acct + ")");
            }
            Matcher matcherAcct = hashAcct.matcher(wordToSpan);
            while (matcherAcct.find()) {
                int matchStart = matcherAcct.start(1);
                int matchEnd = matcherAcct.end();
                if (wordToSpan.length() >= matchEnd && matchStart < matchEnd && matchStart >= 0) {
                    wordToSpan.setSpan(new ForegroundColorSpan(theme_text_header_2_line), matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
            Helper.changeDrawableColor(context, holder.binding.statusBoostIcon, theme_text_header_2_line);
            Helper.changeDrawableColor(context, holder.binding.statusPinned, theme_text_header_2_line);
        }
        if (theme_statuses_color != -1) {
            holder.binding.cardviewContainer.setBackgroundColor(theme_statuses_color);
            holder.binding.translationLabel.setBackgroundColor(theme_statuses_color);
        } else {
            holder.binding.cardviewContainer.setBackgroundColor(ThemeHelper.getBackgroundColor(context));
            holder.binding.translationLabel.setBackgroundColor(ThemeHelper.getBackgroundColor(context));
        }
        if (theme_boost_header_color != -1 && status.reblog != null) {
            holder.binding.statusBoosterInfo.setBackgroundColor(theme_boost_header_color);

        } else {
            holder.binding.statusBoosterInfo.setBackgroundColor(0);
        }
        if (theme_text_color != -1) {
            holder.binding.statusContent.setTextColor(theme_text_color);
            holder.binding.statusContentTranslated.setTextColor(theme_text_color);
            holder.binding.spoiler.setTextColor(theme_text_color);
            holder.binding.dateShort.setTextColor(theme_text_color);
            holder.binding.poll.pollInfo.setTextColor(theme_text_color);
            holder.binding.cardDescription.setTextColor(theme_text_color);
            holder.binding.time.setTextColor(theme_text_color);
            holder.binding.reblogsCount.setTextColor(theme_text_color);
            holder.binding.favoritesCount.setTextColor(theme_text_color);
            holder.binding.favoritesCount.setTextColor(theme_text_color);
            Helper.changeDrawableColor(context, holder.binding.repeatInfo, theme_text_color);
            Helper.changeDrawableColor(context, holder.binding.favInfo, theme_text_color);
            Helper.changeDrawableColor(context, R.drawable.ic_baseline_lock_24, theme_text_color);
        }
        if (link_color != -1) {
            holder.binding.cardUrl.setTextColor(link_color);
        }

        holder.binding.toggleTruncate.setVisibility(View.GONE);

        if (status.isFocused) {
            holder.binding.statusContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.binding.spoiler.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            float currentTextSize = holder.binding.statusContent.getTextSize();
            holder.binding.statusContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextSize * 1.3f);
            holder.binding.spoiler.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextSize * 1.3f);
        } else {
            holder.binding.statusContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.binding.spoiler.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }

        //If the message contain a link to peertube and no media was added, we add it
        if (statusToDeal.card != null && statusToDeal.card.url != null && (statusToDeal.media_attachments == null || statusToDeal.media_attachments.size() == 0)) {
            Matcher matcherLink = Helper.peertubePattern.matcher(statusToDeal.card.url);
            if (matcherLink.find()) { //Peertubee video
                List<Attachment> attachmentList = new ArrayList<>();
                Attachment attachment = new Attachment();
                attachment.type = "video";
                attachment.url = matcherLink.group(0);
                attachment.preview_url = statusToDeal.card.image;
                attachment.peertubeHost = matcherLink.group(2);
                attachment.peertubeId = matcherLink.group(3);
                attachmentList.add(attachment);
                statusToDeal.media_attachments = attachmentList;
                //adapter.notifyItemChanged(holder.getBindingAdapterPosition());
            }
        }

        if (statusToDeal.card != null && (display_card || statusToDeal.isFocused)) {
            if (statusToDeal.card.width > statusToDeal.card.height) {
                holder.binding.cardImageHorizontal.setVisibility(View.VISIBLE);
                holder.binding.cardImageVertical.setVisibility(View.GONE);
                Glide.with(context).load(statusToDeal.card.image).into(holder.binding.cardImageHorizontal);
            } else {
                holder.binding.cardImageHorizontal.setVisibility(View.GONE);
                holder.binding.cardImageVertical.setVisibility(View.VISIBLE);
                Glide.with(context).load(statusToDeal.card.image).into(holder.binding.cardImageVertical);
            }
            holder.binding.cardTitle.setText(statusToDeal.card.title);
            holder.binding.cardDescription.setText(statusToDeal.card.description);
            holder.binding.cardUrl.setText(Helper.transformURL(context, statusToDeal.card.url));
            holder.binding.card.setOnClickListener(v -> Helper.openBrowser(context, Helper.transformURL(context, statusToDeal.card.url)));
            holder.binding.card.setVisibility(View.VISIBLE);
        } else {
            holder.binding.card.setVisibility(View.GONE);
        }
        if (!canBeFederated && timelineType != Timeline.TimeLineEnum.TREND_MESSAGE_PUBLIC) {
            holder.binding.actionShareContainer.setVisibility(View.VISIBLE);
            holder.binding.actionShare.setOnClickListener(v -> {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                String url;
                if (statusToDeal.uri.startsWith("http"))
                    url = status.uri;
                else
                    url = status.url;
                String extra_text;
                if (share_details) {
                    extra_text = statusToDeal.account.acct;
                    if (extra_text.split("@").length == 1)
                        extra_text = "@" + extra_text + "@" + BaseMainActivity.currentInstance;
                    else
                        extra_text = "@" + extra_text;
                    extra_text += " \uD83D\uDD17 " + url + "\r\n-\n";
                    extra_text += statusToDeal.text;
                } else {
                    extra_text = url;
                }
                sendIntent.putExtra(Intent.EXTRA_TEXT, extra_text);
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_with)));
            });
        } else {
            holder.binding.actionShareContainer.setVisibility(View.GONE);
        }
        if (minified || !canBeFederated) {
            holder.binding.actionButtons.setVisibility(View.GONE);
        } else {
            holder.binding.actionButtons.setVisibility(View.VISIBLE);
            //Hide or display bookmark button when status is focused
            if (status.isFocused || displayBookmark) {
                holder.binding.actionButtonBookmark.setVisibility(View.VISIBLE);
            } else {
                holder.binding.actionButtonBookmark.setVisibility(View.GONE);
            }
            //--- ACTIONS ---
            holder.binding.actionButtonBookmark.setChecked(statusToDeal.bookmarked);
            //---> BOOKMARK/UNBOOKMARK
            holder.binding.actionButtonBookmark.setOnLongClickListener(v -> {
                CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.BOOKMARK_ACTION, null, statusToDeal);
                return true;
            });
            holder.binding.actionButtonBookmark.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                    Status fetchedStatus = statusList.get(0);
                                    statusesVM.bookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                            .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.BOOKMARK_ACTION, statusToDeal, _status, true));
                                } else {
                                    Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    if (statusToDeal.bookmarked) {
                        statusesVM.unBookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.UNBOOKMARK_ACTION, statusToDeal, _status, false));
                    } else {
                        ((SparkButton) v).playAnimation();
                        statusesVM.bookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.BOOKMARK_ACTION, statusToDeal, _status, false));
                    }
                }
            });
            holder.binding.actionButtonFavorite.setChecked(statusToDeal.favourited);
            holder.binding.statusUserInfo.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                    Status fetchedStatus = results.statuses.get(0);
                                    Intent intent = new Intent(context, ProfileActivity.class);
                                    Bundle b = new Bundle();
                                    b.putSerializable(Helper.ARG_ACCOUNT, fetchedStatus.reblog != null ? fetchedStatus.reblog.account : fetchedStatus.account);
                                    intent.putExtras(b);
                                    ActivityOptionsCompat options = ActivityOptionsCompat
                                            .makeSceneTransitionAnimation((Activity) context, holder.binding.avatar, context.getString(R.string.activity_porfile_pp));
                                    // start the new activity
                                    context.startActivity(intent, options.toBundle());
                                } else {
                                    Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable(Helper.ARG_ACCOUNT, status.reblog != null ? status.reblog.account : status.account);
                    intent.putExtras(b);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) context, holder.binding.avatar, context.getString(R.string.activity_porfile_pp));
                    // start the new activity
                    context.startActivity(intent, options.toBundle());
                }
            });
            holder.binding.statusBoosterInfo.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                    Status fetchedStatus = results.statuses.get(0);
                                    Intent intent = new Intent(context, ProfileActivity.class);
                                    Bundle b = new Bundle();
                                    b.putSerializable(Helper.ARG_ACCOUNT, fetchedStatus.account);
                                    intent.putExtras(b);
                                    ActivityOptionsCompat options = ActivityOptionsCompat
                                            .makeSceneTransitionAnimation((Activity) context, holder.binding.statusBoosterAvatar, context.getString(R.string.activity_porfile_pp));
                                    // start the new activity
                                    context.startActivity(intent, options.toBundle());
                                } else {
                                    Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable(Helper.ARG_ACCOUNT, status.account);
                    intent.putExtras(b);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) context, holder.binding.statusBoosterAvatar, context.getString(R.string.activity_porfile_pp));
                    // start the new activity
                    context.startActivity(intent, options.toBundle());
                }
            });
            //---> REBLOG/UNREBLOG
            holder.binding.actionButtonBoost.setOnLongClickListener(v -> {
                if (statusToDeal.visibility.equals("direct") || (statusToDeal.visibility.equals("private"))) {
                    return true;
                }
                CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, null, statusToDeal);
                return true;
            });
            holder.binding.actionButtonBoost.setOnClickListener(v -> {
                if (confirmBoost) {
                    AlertDialog.Builder alt_bld = new AlertDialog.Builder(context, Helper.dialogStyle());
                    if (statusToDeal.reblogged) {
                        alt_bld.setMessage(context.getString(R.string.reblog_remove));
                    } else {
                        alt_bld.setMessage(context.getString(R.string.reblog_add));
                    }
                    alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (remote) {
                            Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                    .observe((LifecycleOwner) context, results -> {
                                        if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                            Status fetchedStatus = results.statuses.get(0);
                                            statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id, null)
                                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, statusToDeal, _status, true));
                                        } else {
                                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            if (statusToDeal.reblogged) {
                                statusesVM.unReblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.UNREBLOG_ACTION, statusToDeal, _status, false));
                            } else {
                                ((SparkButton) v).playAnimation();
                                statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, null)
                                        .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, statusToDeal, _status, false));
                            }
                        }
                        dialog.dismiss();
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                } else {
                    if (remote) {
                        Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id, null)
                                                .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, statusToDeal, _status, true));
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        if (statusToDeal.reblogged) {
                            statusesVM.unReblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.UNREBLOG_ACTION, statusToDeal, _status, false));
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, null)
                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, statusToDeal, _status, false));
                        }
                    }
                }
            });
            holder.binding.actionButtonBoost.setChecked(statusToDeal.reblogged);
            //---> FAVOURITE/UNFAVOURITE
            holder.binding.actionButtonFavorite.setOnLongClickListener(v -> {
                if (statusToDeal.visibility.equals("direct") || (statusToDeal.visibility.equals("private"))) {
                    return true;
                }
                CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.FAVOURITE_ACTION, null, statusToDeal);
                return true;
            });
            holder.binding.actionButtonFavorite.setOnClickListener(v -> {
                if (confirmFav) {
                    AlertDialog.Builder alt_bld = new AlertDialog.Builder(context, Helper.dialogStyle());
                    if (status.favourited) {
                        alt_bld.setMessage(context.getString(R.string.favourite_remove));
                    } else {
                        alt_bld.setMessage(context.getString(R.string.favourite_add));
                    }
                    alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (remote) {
                            Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                    .observe((LifecycleOwner) context, results -> {
                                        if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                            Status fetchedStatus = results.statuses.get(0);
                                            statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.FAVOURITE_ACTION, statusToDeal, _status, true));
                                        } else {
                                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            if (status.favourited) {
                                statusesVM.unFavourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.UNFAVOURITE_ACTION, statusToDeal, _status, false));
                            } else {
                                ((SparkButton) v).playAnimation();
                                statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.FAVOURITE_ACTION, statusToDeal, _status, false));
                            }
                        }
                        dialog.dismiss();
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                } else {
                    if (remote) {
                        Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                                .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.FAVOURITE_ACTION, statusToDeal, _status, true));
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        if (statusToDeal.favourited) {
                            statusesVM.unFavourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.UNFAVOURITE_ACTION, statusToDeal, _status, false));
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> manageAction(context, adapter, holder, CrossActionHelper.TypeOfCrossAction.FAVOURITE_ACTION, statusToDeal, _status, false));
                        }
                    }
                }
            });
        }


        //--- ACCOUNT INFO ---
        MastodonHelper.loadPPMastodon(holder.binding.avatar, statusToDeal.account);

        holder.binding.displayName.setText(
                statusToDeal.account.getSpanDisplayName(context,
                        new WeakReference<>(holder.binding.displayName)),
                TextView.BufferType.SPANNABLE);
        if (theme_text_header_1_line != -1) {
            holder.binding.displayName.setTextColor(theme_text_header_1_line);
        }
        holder.binding.username.setText(String.format("@%s", statusToDeal.account.acct));
        if (theme_text_header_2_line != -1) {
            holder.binding.username.setTextColor(theme_text_header_2_line);
        }
        //final float scale = context.getResources().getDisplayMetrics().density;
        final float scale = sharedpreferences.getFloat(context.getString(R.string.SET_FONT_SCALE), 1.1f);
        final float scaleIcon = sharedpreferences.getFloat(context.getString(R.string.SET_FONT_SCALE_ICON), 1.1f);
        if (statusToDeal.account.locked) {
            Drawable img = ContextCompat.getDrawable(context, R.drawable.ic_baseline_lock_24);
            assert img != null;
            img.setBounds(0, 0, (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f), (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f));
            holder.binding.username.setCompoundDrawables(null, null, img, null);
        } else {
            holder.binding.username.setCompoundDrawables(null, null, null, null);
        }
        //Button sizes depending of the defined scale
        float normalSize = Helper.convertDpToPixel(28, context);
        holder.binding.actionButtonReply.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonReply.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonReply.requestLayout();
        holder.binding.actionButtonBoost.setImageSize((int) (normalSize * scaleIcon));
        holder.binding.actionButtonFavorite.setImageSize((int) (normalSize * scaleIcon));
        holder.binding.actionButtonBookmark.setImageSize((int) (normalSize * scaleIcon));
        holder.binding.statusAddCustomEmoji.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.statusAddCustomEmoji.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.statusAddCustomEmoji.requestLayout();
        holder.binding.statusEmoji.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.statusEmoji.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonMore.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonMore.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonMore.requestLayout();
        holder.binding.actionShare.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.actionShare.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionShare.requestLayout();

        if (statusToDeal.account.bot) {
            holder.binding.botIcon.setVisibility(View.VISIBLE);
        } else {
            holder.binding.botIcon.setVisibility(View.GONE);
        }
        if (statusToDeal.in_reply_to_id != null && timelineType != Timeline.TimeLineEnum.UNKNOWN) {
            holder.binding.replyIcon.setVisibility(View.VISIBLE);
        } else {
            holder.binding.replyIcon.setVisibility(View.GONE);
        }

        int ressource = R.drawable.ic_baseline_public_24;
        switch (status.visibility) {
            case "unlisted":
                ressource = R.drawable.ic_baseline_lock_open_24;
                break;
            case "private":
                ressource = R.drawable.ic_baseline_lock_24;
                break;
            case "direct":
                ressource = R.drawable.ic_baseline_mail_24;
                break;
        }
        if (status.isFocused) {
            holder.binding.statusInfo.setVisibility(View.VISIBLE);
            holder.binding.reblogsCount.setText(String.valueOf(status.reblogs_count));
            holder.binding.favoritesCount.setText(String.valueOf(status.favourites_count));

            if (statusToDeal.edited_at != null) {
                holder.binding.editTime.setText(context.getString(R.string.edited_message_at, Helper.longDateToString(status.edited_at)));
                holder.binding.editTime.setOnClickListener(v -> {
                    Intent historyIntent = new Intent(context, StatusHistoryActivity.class);
                    historyIntent.putExtra(Helper.ARG_STATUS_ID, statusToDeal.id);
                    context.startActivity(historyIntent);
                });
                holder.binding.editTime.setVisibility(View.VISIBLE);
            } else {
                holder.binding.editTime.setVisibility(View.GONE);
            }
            holder.binding.time.setText(Helper.longDateToString(status.created_at));
            holder.binding.time.setVisibility(View.VISIBLE);
            holder.binding.dateShort.setVisibility(View.GONE);
            holder.binding.visibility.setImageResource(ressource);
            holder.binding.dateShort.setVisibility(View.GONE);
            holder.binding.visibilitySmall.setVisibility(View.GONE);
            if (statusToDeal.application != null) {
                holder.binding.app.setVisibility(View.VISIBLE);
                holder.binding.app.setText(statusToDeal.application.name);
                holder.binding.app.setOnClickListener(v -> {
                    Helper.openBrowser(context, statusToDeal.application.website);
                });
            } else {
                holder.binding.app.setVisibility(View.GONE);
            }

        } else {
            holder.binding.app.setVisibility(View.GONE);
            holder.binding.editTime.setVisibility(View.GONE);
            holder.binding.visibilitySmall.setImageResource(ressource);
            if (displayCounters && canBeFederated) {
                holder.binding.replyCount.setText(String.valueOf(statusToDeal.replies_count));
                holder.binding.statusInfo.setVisibility(View.VISIBLE);
                holder.binding.dateShort.setVisibility(View.GONE);
                holder.binding.visibilitySmall.setVisibility(View.GONE);
                holder.binding.reblogsCount.setText(String.valueOf(statusToDeal.reblogs_count));
                holder.binding.favoritesCount.setText(String.valueOf(statusToDeal.favourites_count));
                holder.binding.time.setText(Helper.dateDiff(context, statusToDeal.created_at));
                if (statusToDeal.edited_at != null) {
                    Drawable img = ContextCompat.getDrawable(context, R.drawable.ic_baseline_mode_edit_message_24);
                    img.setBounds(0, 0, (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f), (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f));
                    holder.binding.time.setCompoundDrawables(null, null, img, null);
                } else {
                    holder.binding.time.setCompoundDrawables(null, null, null, null);
                }
                Helper.absoluteDateTimeReveal(context, holder.binding.time, statusToDeal.created_at, statusToDeal.edited_at);
                holder.binding.visibility.setImageResource(ressource);
                holder.binding.time.setVisibility(View.VISIBLE);
            } else {
                holder.binding.statusInfo.setVisibility(View.GONE);
                holder.binding.dateShort.setVisibility(View.VISIBLE);
                holder.binding.visibilitySmall.setVisibility(View.VISIBLE);
                if (statusToDeal.edited_at != null) {
                    Drawable img = ContextCompat.getDrawable(context, R.drawable.ic_baseline_mode_edit_message_24);
                    img.setBounds(0, 0, (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f), (int) (Helper.convertDpToPixel(16, context) * scale + 0.5f));
                    holder.binding.dateShort.setCompoundDrawables(null, null, img, null);
                } else {
                    holder.binding.dateShort.setCompoundDrawables(null, null, null, null);
                }
                holder.binding.dateShort.setText(Helper.dateDiff(context, statusToDeal.created_at));
                holder.binding.time.setVisibility(View.GONE);
                Helper.absoluteDateTimeReveal(context, holder.binding.dateShort, statusToDeal.created_at, statusToDeal.edited_at);
            }
        }

        //---- SPOILER TEXT -----
        if (statusToDeal.spoiler_text != null && !statusToDeal.spoiler_text.trim().isEmpty()) {
            if (expand_cw || expand) {
                holder.binding.spoilerExpand.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setText(
                        statusToDeal.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.spoiler)),
                        TextView.BufferType.SPANNABLE);
                statusToDeal.isExpended = true;
                statusToDeal.isMediaDisplayed = true;
            } else {
                holder.binding.spoilerExpand.setOnClickListener(v -> {
                    statusToDeal.isExpended = !statusToDeal.isExpended;
                    statusToDeal.isMediaDisplayed = !statusToDeal.isMediaDisplayed;
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                });
                holder.binding.spoilerExpand.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setVisibility(View.VISIBLE);

                holder.binding.spoiler.setText(
                        statusToDeal.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.spoiler)),
                        TextView.BufferType.SPANNABLE);
            }
            if (statusToDeal.isExpended) {
                holder.binding.spoilerExpand.setText(context.getString(R.string.hide_content));
            } else {
                holder.binding.spoilerExpand.setText(context.getString(R.string.show_content));
            }
        } else {
            holder.binding.spoiler.setVisibility(View.GONE);
            holder.binding.spoilerExpand.setVisibility(View.GONE);
            holder.binding.spoiler.setText(null);
        }

        //--- BOOSTER INFO ---
        if (status.reblog != null) {
            MastodonHelper.loadPPMastodon(holder.binding.statusBoosterAvatar, status.account);

            holder.binding.statusBoosterDisplayName.setText(
                    status.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.binding.statusBoosterDisplayName)),
                    TextView.BufferType.SPANNABLE);

            holder.binding.statusBoosterInfo.setVisibility(View.VISIBLE);
            if (theme_text_header_1_line != -1) {
                holder.binding.statusBoosterDisplayName.setTextColor(theme_text_header_1_line);
            }
            holder.binding.statusBoosterUsername.setText(String.format("@%s", status.account.acct));
            if (theme_text_header_2_line != -1) {
                holder.binding.statusBoosterUsername.setTextColor(theme_text_header_2_line);
            }
        } else {
            holder.binding.statusBoosterInfo.setVisibility(View.GONE);
        }
        //--- BOOST VISIBILITY ---
        switch (statusToDeal.visibility) {
            case "public":
            case "unlisted":
                holder.binding.actionButtonBoost.setVisibility(View.VISIBLE);
                break;
            case "private":
                if (status.account.id.compareTo(BaseMainActivity.currentUserID) == 0) {
                    holder.binding.actionButtonBoost.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.actionButtonBoost.setVisibility(View.GONE);
                }
                break;
            case "direct":
                holder.binding.actionButtonBoost.setVisibility(View.GONE);
                break;
        }
        //--- MAIN CONTENT ---
        holder.binding.statusContent.setText(
                statusToDeal.getSpanContent(context,
                        new WeakReference<>(holder.binding.statusContent)),
                TextView.BufferType.SPANNABLE);
        if (truncate_toots_size > 0) {
            holder.binding.statusContent.setMaxLines(truncate_toots_size);
            holder.binding.statusContent.setEllipsize(TextUtils.TruncateAt.END);

            holder.binding.statusContent.post(() -> {
                if (holder.binding.statusContent.getLineCount() > truncate_toots_size) {
                    holder.binding.toggleTruncate.setVisibility(View.VISIBLE);
                    if (statusToDeal.isTruncated) {
                        holder.binding.toggleTruncate.setText(R.string.display_toot_truncate);
                        holder.binding.toggleTruncate.setCompoundDrawables(null, null, ContextCompat.getDrawable(context, R.drawable.ic_display_more), null);
                    } else {
                        holder.binding.toggleTruncate.setText(R.string.hide_toot_truncate);
                        holder.binding.toggleTruncate.setCompoundDrawables(null, null, ContextCompat.getDrawable(context, R.drawable.ic_display_less), null);
                    }
                    holder.binding.toggleTruncate.setOnClickListener(v -> {
                        statusToDeal.isTruncated = !statusToDeal.isTruncated;
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    });
                    if (statusToDeal.isTruncated) {
                        holder.binding.statusContent.setMaxLines(5);
                    } else {
                        holder.binding.statusContent.setMaxLines(9999);
                    }
                } else {
                    holder.binding.toggleTruncate.setVisibility(View.GONE);
                }
            });
        }
        if (statusToDeal.translationContent != null) {
            holder.binding.containerTrans.setVisibility(View.VISIBLE);
            holder.binding.statusContentTranslated.setText(
                    statusToDeal.getSpanTranslate(context,
                            new WeakReference<>(holder.binding.statusContentTranslated)),
                    TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.containerTrans.setVisibility(View.GONE);
        }
        if (statusToDeal.spoiler_text == null || statusToDeal.spoiler_text.trim().isEmpty() || statusToDeal.isExpended) {
            if (statusToDeal.content.trim().length() == 0) {
                holder.binding.mediaContainer.setVisibility(View.GONE);
            } else {
                holder.binding.statusContent.setVisibility(View.VISIBLE);
            }
        } else {
            holder.binding.statusContent.setVisibility(View.GONE);
            holder.binding.mediaContainer.setVisibility(View.GONE);
        }

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        //--- MEDIA ATTACHMENT ---
        if (statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0) {
            holder.binding.attachmentsList.removeAllViews();
            holder.binding.mediaContainer.removeAllViews();
            //If only one attachment
            if (statusToDeal.media_attachments.size() == 1) {
                if ((loadMediaType.equals("ASK") || (loadMediaType.equals("WIFI") && !TimelineHelper.isOnWIFI(context))) && !statusToDeal.canLoadMedia) {
                    holder.binding.mediaContainer.setVisibility(View.GONE);
                    holder.binding.displayMedia.setVisibility(View.VISIBLE);
                    holder.binding.displayMedia.setOnClickListener(v -> {
                        statusToDeal.canLoadMedia = true;
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    });
                } else {
                    holder.binding.mediaContainer.setVisibility(View.VISIBLE);
                    holder.binding.displayMedia.setVisibility(View.GONE);
                    LayoutMediaBinding layoutMediaBinding = LayoutMediaBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                    RelativeLayout.LayoutParams lp;
                    if (fullAttachement) {
                        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, MediaHelper.returnMaxHeightForPreviews(context, statusToDeal.media_attachments));
                        layoutMediaBinding.media.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    } else {
                        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, (int) Helper.convertDpToPixel(200, context));
                        layoutMediaBinding.media.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                    if (statusToDeal.sensitive) {
                        Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.red_1);
                    } else {
                        Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.white);
                    }
                    layoutMediaBinding.media.setLayoutParams(lp);
                    layoutMediaBinding.media.setOnClickListener(v -> {
                        if (statusToDeal.isMediaObfuscated && mediaObfuscated(statusToDeal) && !expand_media) {
                            statusToDeal.isMediaObfuscated = false;
                            int position = holder.getBindingAdapterPosition();
                            adapter.notifyItemChanged(position);
                            final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);

                            if (timeout > 0) {
                                new CountDownTimer((timeout * 1000L), 1000) {
                                    public void onTick(long millisUntilFinished) {
                                    }

                                    public void onFinish() {
                                        status.isMediaObfuscated = true;
                                        adapter.notifyItemChanged(position);
                                    }
                                }.start();
                            }
                            return;
                        }
                        Intent mediaIntent = new Intent(context, MediaActivity.class);
                        Bundle b = new Bundle();
                        b.putInt(Helper.ARG_MEDIA_POSITION, 1);
                        b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(statusToDeal.media_attachments));
                        mediaIntent.putExtras(b);
                        ActivityOptionsCompat options = ActivityOptionsCompat
                                .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, statusToDeal.media_attachments.get(0).url);
                        // start the new activity
                        context.startActivity(mediaIntent, options.toBundle());
                    });
                    if (statusToDeal.media_attachments.get(0).type != null && (statusToDeal.media_attachments.get(0).type.equalsIgnoreCase("video") || statusToDeal.media_attachments.get(0).type.equalsIgnoreCase("gifv"))) {
                        layoutMediaBinding.playVideo.setVisibility(View.VISIBLE);
                    } else {
                        layoutMediaBinding.playVideo.setVisibility(View.GONE);
                    }
                    if (statusToDeal.media_attachments.get(0).type != null && statusToDeal.media_attachments.get(0).type.equalsIgnoreCase("audio")) {
                        layoutMediaBinding.playMusic.setVisibility(View.VISIBLE);
                    } else {
                        layoutMediaBinding.playMusic.setVisibility(View.GONE);
                    }
                    String finalUrl;
                    if (statusToDeal.media_attachments.get(0).url == null) {
                        finalUrl = statusToDeal.media_attachments.get(0).remote_url;
                    } else {
                        finalUrl = statusToDeal.media_attachments.get(0).url;
                    }
                    layoutMediaBinding.media.setOnLongClickListener(v -> {
                        if (long_press_media) {
                            MediaHelper.manageMove(context, finalUrl, false);
                        }
                        return true;
                    });

                    float focusX = 0.f;
                    float focusY = 0.f;
                    if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.focus != null) {
                        focusX = statusToDeal.media_attachments.get(0).meta.focus.x;
                        focusY = statusToDeal.media_attachments.get(0).meta.focus.y;
                    }
                    if (statusToDeal.media_attachments.get(0).description != null && !statusToDeal.media_attachments.get(0).description.isEmpty()) {
                        layoutMediaBinding.viewDescription.setVisibility(View.VISIBLE);
                    } else {
                        layoutMediaBinding.viewDescription.setVisibility(View.GONE);
                    }
                    if (!mediaObfuscated(statusToDeal) || expand_media) {
                        layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
                        RequestBuilder<Drawable> requestBuilder = Glide.with(layoutMediaBinding.media.getContext())
                                .load(statusToDeal.media_attachments.get(0).preview_url);
                        if (!fullAttachement) {
                            requestBuilder = requestBuilder.apply(new RequestOptions().transform(new GlideFocus(focusX, focusY)));
                        }
                        requestBuilder.into(layoutMediaBinding.media);
                    } else {
                        layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
                        Glide.with(layoutMediaBinding.media.getContext())
                                .load(statusToDeal.media_attachments.get(0).preview_url)
                                .apply(new RequestOptions().transform(new BlurTransformation(50, 3)))
                                // .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                                .into(layoutMediaBinding.media);
                    }
                    layoutMediaBinding.viewHide.setOnClickListener(v -> {
                        statusToDeal.sensitive = !statusToDeal.sensitive;
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    });
                    holder.binding.mediaContainer.addView(layoutMediaBinding.getRoot());
                    holder.binding.mediaContainer.setVisibility(View.VISIBLE);
                    holder.binding.attachmentsListContainer.setVisibility(View.GONE);
                }
            } else { //Several media
                if ((loadMediaType.equals("ASK") || (loadMediaType.equals("WIFI") && !TimelineHelper.isOnWIFI(context))) && !statusToDeal.canLoadMedia) {
                    holder.binding.mediaContainer.setVisibility(View.GONE);
                    holder.binding.displayMedia.setVisibility(View.VISIBLE);
                    holder.binding.displayMedia.setOnClickListener(v -> {
                        statusToDeal.canLoadMedia = true;
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    });
                } else {
                    int mediaPosition = 1;
                    for (Attachment attachment : statusToDeal.media_attachments) {
                        LayoutMediaBinding layoutMediaBinding = LayoutMediaBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                        RelativeLayout.LayoutParams lp;
                        float focusX = 0.f;
                        float focusY = 0.f;
                        if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.focus != null) {
                            focusX = statusToDeal.media_attachments.get(0).meta.focus.x;
                            focusY = statusToDeal.media_attachments.get(0).meta.focus.y;
                        }

                        String finalUrl;
                        if (attachment.url == null) {
                            finalUrl = attachment.remote_url;
                        } else {
                            finalUrl = attachment.url;
                        }
                        layoutMediaBinding.media.setOnLongClickListener(v -> {
                            if (long_press_media) {
                                MediaHelper.manageMove(context, finalUrl, false);
                            }
                            return true;
                        });
                        if (fullAttachement) {
                            lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, MediaHelper.returnMaxHeightForPreviews(context, statusToDeal.media_attachments));
                            layoutMediaBinding.media.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        } else {
                            lp = new RelativeLayout.LayoutParams((int) Helper.convertDpToPixel(200, context), (int) Helper.convertDpToPixel(200, context));
                            layoutMediaBinding.media.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                        if (attachment.type != null && (attachment.type.equalsIgnoreCase("video") || attachment.type.equalsIgnoreCase("gifv"))) {
                            layoutMediaBinding.playVideo.setVisibility(View.VISIBLE);
                        } else {
                            layoutMediaBinding.playVideo.setVisibility(View.GONE);
                        }
                        if (attachment.type != null && attachment.type.equalsIgnoreCase("audio")) {
                            layoutMediaBinding.playMusic.setVisibility(View.VISIBLE);
                        } else {
                            layoutMediaBinding.playMusic.setVisibility(View.GONE);
                        }
                        if (attachment.description != null && !attachment.description.isEmpty()) {
                            layoutMediaBinding.viewDescription.setVisibility(View.VISIBLE);
                        } else {
                            layoutMediaBinding.viewDescription.setVisibility(View.GONE);
                        }
                        lp.setMargins(0, 0, (int) Helper.convertDpToPixel(5, context), 0);
                        if (!mediaObfuscated(statusToDeal) || expand_media) {
                            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
                            RequestBuilder<Drawable> requestBuilder = Glide.with(layoutMediaBinding.media.getContext())
                                    .load(attachment.preview_url);
                            if (!fullAttachement) {
                                requestBuilder = requestBuilder.apply(new RequestOptions().transform(new GlideFocus(focusX, focusY)));
                            }
                            requestBuilder.into(layoutMediaBinding.media);
                        } else {
                            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
                            Glide.with(layoutMediaBinding.media.getContext())
                                    .load(attachment.preview_url)
                                    .apply(new RequestOptions().transform(new BlurTransformation(50, 3)))
                                    //    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                                    .into(layoutMediaBinding.media);
                        }
                        if (statusToDeal.sensitive) {
                            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.red_1);
                        } else {
                            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.white);
                        }
                        layoutMediaBinding.media.setLayoutParams(lp);
                        int finalMediaPosition = mediaPosition;
                        layoutMediaBinding.media.setOnClickListener(v -> {
                            Intent mediaIntent = new Intent(context, MediaActivity.class);
                            Bundle b = new Bundle();
                            b.putInt(Helper.ARG_MEDIA_POSITION, finalMediaPosition);
                            b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(statusToDeal.media_attachments));
                            mediaIntent.putExtras(b);
                            ActivityOptionsCompat options = ActivityOptionsCompat
                                    .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, statusToDeal.media_attachments.get(finalMediaPosition - 1).url);
                            // start the new activity
                            context.startActivity(mediaIntent, options.toBundle());
                        });
                        layoutMediaBinding.viewHide.setOnClickListener(v -> {
                            statusToDeal.sensitive = !statusToDeal.sensitive;
                            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                        });
                        holder.binding.attachmentsList.addView(layoutMediaBinding.getRoot());
                        mediaPosition++;
                    }
                    holder.binding.mediaContainer.setVisibility(View.GONE);
                    holder.binding.attachmentsListContainer.setVisibility(View.VISIBLE);
                }
            }
        } else {
            holder.binding.displayMedia.setVisibility(View.GONE);
            holder.binding.mediaContainer.setVisibility(View.GONE);
            holder.binding.attachmentsListContainer.setVisibility(View.GONE);
        }
        holder.binding.statusContent.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        holder.binding.reblogInfo.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                Status fetchedStatus = results.statuses.get(0);
                                if (fetchedStatus.reblogs_count > 0) {
                                    Intent intent = new Intent(context, StatusInfoActivity.class);
                                    intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.BOOSTED_BY);
                                    intent.putExtra(Helper.ARG_STATUS, fetchedStatus);
                                    context.startActivity(intent);
                                }
                            } else {
                                Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                            }
                        });
            } else {
                if (statusToDeal.reblogs_count > 0) {
                    Intent intent = new Intent(context, StatusInfoActivity.class);
                    intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.BOOSTED_BY);
                    intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                    context.startActivity(intent);
                }
            }
        });

        holder.binding.favouriteInfo.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                Status fetchedStatus = results.statuses.get(0);
                                if (fetchedStatus.favourites_count > 0) {
                                    Intent intent = new Intent(context, StatusInfoActivity.class);
                                    intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.LIKED_BY);
                                    intent.putExtra(Helper.ARG_STATUS, fetchedStatus);
                                    context.startActivity(intent);
                                }
                            } else {
                                Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                            }
                        });
            } else {
                if (statusToDeal.favourites_count > 0) {
                    Intent intent = new Intent(context, StatusInfoActivity.class);
                    intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.LIKED_BY);
                    intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                    context.startActivity(intent);
                }
            }
        });

        // --- POLL ---

        if (statusToDeal.poll != null && statusToDeal.poll.options != null) {
            if (statusToDeal.poll.voted || statusToDeal.poll.expired) {
                holder.binding.poll.submitVote.setVisibility(View.GONE);
                holder.binding.poll.rated.setVisibility(View.VISIBLE);
                holder.binding.poll.multipleChoice.setVisibility(View.GONE);
                holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.GONE);
                int greaterValue = 0;
                for (Poll.PollItem pollItem : statusToDeal.poll.options) {
                    if (pollItem.votes_count > greaterValue)
                        greaterValue = pollItem.votes_count;
                }
                holder.binding.poll.rated.removeAllViews();
                List<Integer> ownvotes = statusToDeal.poll.own_votes;
                int j = 0;
                if (statusToDeal.poll.voters_count == 0 && statusToDeal.poll.votes_count > 0) {
                    statusToDeal.poll.voters_count = statusToDeal.poll.votes_count;
                }
                for (Poll.PollItem pollItem : statusToDeal.poll.options) {
                    @NonNull LayoutPollItemBinding pollItemBinding = LayoutPollItemBinding.inflate(inflater, holder.binding.poll.rated, true);
                    double value = ((double) (pollItem.votes_count * 100) / (double) statusToDeal.poll.voters_count);
                    pollItemBinding.pollItemPercent.setText(String.format("%s %%", (int) value));
                    if (theme_text_color != -1) {
                        pollItemBinding.pollItemPercent.setTextColor(theme_text_color);
                        pollItemBinding.pollItemText.setTextColor(theme_text_color);
                    }
                    pollItemBinding.pollItemText.setText(
                            pollItem.getSpanTitle(context, statusToDeal,
                                    new WeakReference<>(pollItemBinding.pollItemText)),
                            TextView.BufferType.SPANNABLE);
                    pollItemBinding.pollItemValue.setProgress((int) value);
                    if (pollItem.votes_count == greaterValue) {
                        pollItemBinding.pollItemPercent.setTypeface(null, Typeface.BOLD);
                        pollItemBinding.pollItemText.setTypeface(null, Typeface.BOLD);
                    }
                    if (ownvotes != null && ownvotes.contains(j)) {
                        Drawable img = ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_24);
                        assert img != null;
                        img.setColorFilter(ContextCompat.getColor(context, R.color.cyanea_accent_reference), PorterDuff.Mode.SRC_IN);
                        img.setBounds(0, 0, (int) (20 * scale + 0.5f), (int) (20 * scale + 0.5f));
                        pollItemBinding.pollItemText.setCompoundDrawables(null, null, img, null);
                    }
                    j++;
                }
            } else {
                if (statusToDeal.poll.voters_count == 0 && statusToDeal.poll.votes_count > 0) {
                    statusToDeal.poll.voters_count = statusToDeal.poll.votes_count;
                }
                holder.binding.poll.rated.setVisibility(View.GONE);
                holder.binding.poll.submitVote.setVisibility(View.VISIBLE);
                if (statusToDeal.poll.multiple) {
                    if ((holder.binding.poll.multipleChoice).getChildCount() > 0)
                        (holder.binding.poll.multipleChoice).removeAllViews();
                    for (Poll.PollItem pollOption : statusToDeal.poll.options) {
                        CheckBox cb = new CheckBox(context);
                        cb.setButtonTintList(ThemeHelper.getButtonColorStateList(context));
                        cb.setText(
                                pollOption.getSpanTitle(context, statusToDeal,
                                        new WeakReference<>(cb)),
                                TextView.BufferType.SPANNABLE);
                        holder.binding.poll.multipleChoice.addView(cb);
                    }
                    holder.binding.poll.multipleChoice.setVisibility(View.VISIBLE);
                    holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.GONE);
                } else {
                    if ((holder.binding.poll.singleChoiceRadioGroup).getChildCount() > 0)
                        (holder.binding.poll.singleChoiceRadioGroup).removeAllViews();
                    for (Poll.PollItem pollOption : statusToDeal.poll.options) {
                        RadioButton rb = new RadioButton(context);
                        rb.setButtonTintList(ThemeHelper.getButtonColorStateList(context));
                        rb.setText(
                                pollOption.getSpanTitle(context, statusToDeal,
                                        new WeakReference<>(rb)),
                                TextView.BufferType.SPANNABLE);

                        holder.binding.poll.singleChoiceRadioGroup.addView(rb);
                    }
                    holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.VISIBLE);
                    holder.binding.poll.multipleChoice.setVisibility(View.GONE);
                }
                holder.binding.poll.submitVote.setVisibility(View.VISIBLE);
                holder.binding.poll.submitVote.setOnClickListener(v -> {
                    int[] choice;
                    if (statusToDeal.poll.multiple) {
                        ArrayList<Integer> choices = new ArrayList<>();
                        int choicesCount = holder.binding.poll.multipleChoice.getChildCount();
                        for (int i1 = 0; i1 < choicesCount; i1++) {
                            if (holder.binding.poll.multipleChoice.getChildAt(i1) != null && holder.binding.poll.multipleChoice.getChildAt(i1) instanceof CheckBox) {
                                if (((CheckBox) holder.binding.poll.multipleChoice.getChildAt(i1)).isChecked()) {
                                    choices.add(i1);
                                }
                            }
                        }
                        choice = new int[choices.size()];
                        Iterator<Integer> iterator = choices.iterator();
                        for (int i1 = 0; i1 < choice.length; i1++) {
                            choice[i1] = iterator.next();
                        }
                        if (choice.length == 0)
                            return;
                    } else {
                        choice = new int[1];
                        choice[0] = -1;
                        int choicesCount = holder.binding.poll.singleChoiceRadioGroup.getChildCount();
                        for (int i1 = 0; i1 < choicesCount; i1++) {
                            if (holder.binding.poll.singleChoiceRadioGroup.getChildAt(i1) != null && holder.binding.poll.singleChoiceRadioGroup.getChildAt(i1) instanceof RadioButton) {
                                if (((RadioButton) holder.binding.poll.singleChoiceRadioGroup.getChildAt(i1)).isChecked()) {
                                    choice[0] = i1;
                                }
                            }
                        }
                        if (choice[0] == -1)
                            return;
                    }
                    //Vote on the poll
                    if (remote) {
                        Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.votePoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.poll.id, choice)
                                                .observe((LifecycleOwner) context, poll -> {
                                                    int i = 0;
                                                    for (Poll.PollItem item : statusToDeal.poll.options) {
                                                        if (item.span_title != null) {
                                                            poll.options.get(i).span_title = item.span_title;
                                                        } else {
                                                            poll.options.get(i).span_title = new SpannableString(item.title);
                                                        }
                                                        i++;
                                                    }
                                                    statusToDeal.poll = poll;
                                                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                                                });
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        statusesVM.votePoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.poll.id, choice)
                                .observe((LifecycleOwner) context, poll -> {
                                    if (poll != null) {
                                        int i = 0;
                                        for (Poll.PollItem item : statusToDeal.poll.options) {
                                            if (item.span_title != null) {
                                                poll.options.get(i).span_title = item.span_title;
                                            } else {
                                                poll.options.get(i).span_title = new SpannableString(item.title);
                                            }
                                            i++;
                                        }
                                        statusToDeal.poll = poll;
                                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                                    }
                                });
                    }
                });
            }
            holder.binding.poll.refreshPoll.setOnClickListener(v -> statusesVM.getPoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.poll.id)
                    .observe((LifecycleOwner) context, poll -> {
                        //Store span elements
                        int i = 0;
                        for (Poll.PollItem item : statusToDeal.poll.options) {
                            if (item.span_title != null) {
                                poll.options.get(i).span_title = item.span_title;
                            } else {
                                poll.options.get(i).span_title = new SpannableString(item.title);
                            }
                            i++;
                        }
                        statusToDeal.poll = poll;
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    }));
            holder.binding.poll.pollContainer.setVisibility(View.VISIBLE);
            String pollInfo = context.getResources().getQuantityString(R.plurals.number_of_voters, statusToDeal.poll.voters_count, statusToDeal.poll.voters_count);
            if (statusToDeal.poll.expired) {
                pollInfo += " - " + context.getString(R.string.poll_finish_at, MastodonHelper.dateToStringPoll(statusToDeal.poll.expires_at));
            } else {
                pollInfo += " - " + context.getString(R.string.poll_finish_in, MastodonHelper.dateDiffPoll(context, statusToDeal.poll.expires_at));
            }
            holder.binding.poll.pollInfo.setText(pollInfo);
        } else {
            holder.binding.poll.pollContainer.setVisibility(View.GONE);
        }
        holder.binding.statusContent.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && !view.hasFocus()) {
                try {
                    view.requestFocus();
                } catch (Exception ignored) {
                }
            }
            return false;
        });
        if (!minified && canBeFederated) {
            holder.binding.mainContainer.setOnClickListener(v -> holder.binding.statusContent.callOnClick());
            holder.binding.statusContent.setOnClickListener(v -> {
                if (status.isFocused || v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                    if (v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                        v.setTag(null);
                    }
                    return;
                }
                if (context instanceof ContextActivity) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Helper.ARG_STATUS, statusToDeal);
                    Fragment fragment = Helper.addFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.nav_host_fragment_content_main, new FragmentMastodonContext(), bundle, null, FragmentMastodonContext.class.getName());
                    ((ContextActivity) context).setCurrentFragment((FragmentMastodonContext) fragment);
                } else {
                    if (remote) {
                        Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        Intent intent = new Intent(context, ContextActivity.class);
                                        intent.putExtra(Helper.ARG_STATUS, fetchedStatus);
                                        context.startActivity(intent);
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Intent intent = new Intent(context, ContextActivity.class);
                        intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                        context.startActivity(intent);
                    }
                }
            });
        } else if (!canBeFederated) {
            holder.binding.mainContainer.setOnClickListener(v -> Helper.openBrowser(context, status.url));
            holder.binding.statusContent.setOnClickListener(v -> Helper.openBrowser(context, status.url));
        }


        // Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> holder.binding.statusContent.invalidate(), 0, 100, TimeUnit.MILLISECONDS);
        if (remote) {
            holder.binding.actionButtonMore.setVisibility(View.GONE);
        } else {
            holder.binding.actionButtonMore.setVisibility(View.VISIBLE);
        }
        holder.binding.actionButtonMore.setOnClickListener(v -> {
            boolean isOwner = statusToDeal.account.id.compareTo(BaseMainActivity.currentUserID) == 0;
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(context, Helper.popupStyle()), holder.binding.actionButtonMore);
            popup.getMenuInflater()
                    .inflate(R.menu.option_toot, popup.getMenu());
            if (statusToDeal.visibility.equals("private") || status.visibility.equals("direct")) {
                popup.getMenu().findItem(R.id.action_mention).setVisible(false);
            }
            if (statusToDeal.bookmarked)
                popup.getMenu().findItem(R.id.action_bookmark).setTitle(R.string.bookmark_remove);
            else
                popup.getMenu().findItem(R.id.action_bookmark).setTitle(R.string.bookmark_add);
            if (statusToDeal.muted)
                popup.getMenu().findItem(R.id.action_mute_conversation).setTitle(R.string.unmute_conversation);
            else
                popup.getMenu().findItem(R.id.action_mute_conversation).setTitle(R.string.mute_conversation);
            if (statusToDeal.pinned)
                popup.getMenu().findItem(R.id.action_pin).setTitle(R.string.action_unpin);
            else
                popup.getMenu().findItem(R.id.action_pin).setTitle(R.string.action_pin);
            final String[] stringArrayConf;
            if (statusToDeal.visibility.equals("direct") || (statusToDeal.visibility.equals("private") && !isOwner))
                popup.getMenu().findItem(R.id.action_schedule_boost).setVisible(false);
            if (isOwner) {
                popup.getMenu().findItem(R.id.action_block).setVisible(false);
                popup.getMenu().findItem(R.id.action_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_report).setVisible(false);
                popup.getMenu().findItem(R.id.action_timed_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_block_domain).setVisible(false);
                popup.getMenu().findItem(R.id.action_pin).setVisible(!statusToDeal.visibility.equalsIgnoreCase("direct"));
                stringArrayConf = context.getResources().getStringArray(R.array.more_action_owner_confirm);
            } else {
                popup.getMenu().findItem(R.id.action_pin).setVisible(false);
                popup.getMenu().findItem(R.id.action_redraft).setVisible(false);
                popup.getMenu().findItem(R.id.action_edit).setVisible(false);
                popup.getMenu().findItem(R.id.action_remove).setVisible(false);
                if (statusToDeal.account.acct.split("@").length < 2)
                    popup.getMenu().findItem(R.id.action_block_domain).setVisible(false);
                stringArrayConf = context.getResources().getStringArray(R.array.more_action_confirm);
            }
            popup.getMenu().findItem(R.id.action_admin).setVisible(currentAccount.admin);

            boolean custom_sharing = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOM_SHARING), false);
            if (custom_sharing && statusToDeal.visibility.equals("public"))
                popup.getMenu().findItem(R.id.action_custom_sharing).setVisible(true);
            AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_redraft) {
                    AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                    builderInner.setTitle(stringArrayConf[1]);
                    builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (statusList != null) {
                            int position = holder.getBindingAdapterPosition();
                            statusList.remove(statusToDeal);
                            adapter.notifyItemRemoved(position);
                            statusesVM.deleteStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, statusDeleted -> {
                                Intent intent = new Intent(context, ComposeActivity.class);
                                StatusDraft statusDraft = new StatusDraft();
                                statusDraft.statusDraftList = new ArrayList<>();
                                statusDraft.statusReplyList = new ArrayList<>();
                                if (statusDeleted == null) {
                                    Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                    return;
                                }
                                statusDeleted.id = null;
                                statusDraft.statusDraftList.add(statusDeleted);
                                intent.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
                                intent.putExtra(Helper.ARG_STATUS_REPLY_ID, statusDeleted.in_reply_to_id);
                                context.startActivity(intent);
                                sendAction(context, Helper.ARG_STATUS_DELETED, statusToDeal, null);
                            });
                        }
                    });
                    builderInner.setMessage(statusToDeal.text);
                    builderInner.show();
                } else if (itemId == R.id.action_edit) {
                    statusesVM.getStatusSource(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                            .observe((LifecycleOwner) context, statusSource -> {
                                if (statusSource != null) {
                                    Intent intent = new Intent(context, ComposeActivity.class);
                                    StatusDraft statusDraft = new StatusDraft();
                                    statusDraft.statusDraftList = new ArrayList<>();
                                    statusDraft.statusReplyList = new ArrayList<>();
                                    statusToDeal.text = statusSource.text;
                                    statusToDeal.spoiler_text = statusSource.spoiler_text;
                                    statusDraft.statusDraftList.add(statusToDeal);
                                    intent.putExtra(Helper.ARG_STATUS_DRAFT, statusDraft);
                                    intent.putExtra(Helper.ARG_EDIT_STATUS_ID, statusToDeal.id);
                                    intent.putExtra(Helper.ARG_STATUS_REPLY_ID, statusToDeal.in_reply_to_id);
                                    context.startActivity(intent);
                                } else {
                                    Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                }
                            });
                } else if (itemId == R.id.action_schedule_boost) {
                    MastodonHelper.scheduleBoost(context, MastodonHelper.ScheduleType.BOOST, statusToDeal, null, null);
                } else if (itemId == R.id.action_admin) {
                    Intent intent = new Intent(context, AdminAccountActivity.class);
                    intent.putExtra(Helper.ARG_ACCOUNT_ID, statusToDeal.account.id);
                    context.startActivity(intent);
                } else if (itemId == R.id.action_open_browser) {
                    Helper.openBrowser(context, statusToDeal.url);
                } else if (itemId == R.id.action_remove) {
                    AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                    builderInner.setTitle(stringArrayConf[0]);
                    builderInner.setMessage(statusToDeal.content);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        builderInner.setMessage(Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString());
                    else
                        builderInner.setMessage(Html.fromHtml(statusToDeal.content).toString());
                    builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.yes, (dialog, which) -> statusesVM.deleteStatus(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                            .observe((LifecycleOwner) context, statusDeleted -> {
                                int position = holder.getBindingAdapterPosition();
                                statusList.remove(statusToDeal);
                                adapter.notifyItemRemoved(position);
                                sendAction(context, Helper.ARG_STATUS_DELETED, statusToDeal, null);
                            }));
                    builderInner.show();
                } else if (itemId == R.id.action_block_domain) {
                    AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                    builderInner.setTitle(stringArrayConf[3]);
                    String domain = statusToDeal.account.acct.split("@")[1];
                    builderInner.setMessage(context.getString(R.string.block_domain_confirm_message, domain));
                    builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.yes, (dialog, which) -> {
                        accountsVM.addDomainBlocks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, domain);
                        Toasty.info(context, context.getString(R.string.toast_block_domain), Toasty.LENGTH_LONG).show();
                    });
                    builderInner.show();
                } else if (itemId == R.id.action_mute) {
                    AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                    builderInner.setTitle(stringArrayConf[0]);
                    builderInner.setMessage(statusToDeal.account.acct);
                    builderInner.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setNegativeButton(R.string.keep_notifications, (dialog, which) -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.account.id, false, null)
                            .observe((LifecycleOwner) context, relationShip -> {
                                sendAction(context, Helper.ARG_STATUS_ACCOUNT_ID_DELETED, null, statusToDeal.account.id);
                                Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_LONG).show();
                            }));
                    builderInner.setPositiveButton(R.string.action_mute, (dialog, which) -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.account.id, null, null)
                            .observe((LifecycleOwner) context, relationShip -> {
                                sendAction(context, Helper.ARG_STATUS_ACCOUNT_ID_DELETED, null, statusToDeal.account.id);
                                Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_LONG).show();
                            }));
                    builderInner.show();
                } else if (itemId == R.id.action_mute_conversation) {
                    if (statusToDeal.muted) {
                        statusesVM.unMute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.toast_unmute_conversation)).show());
                    } else {
                        statusesVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.toast_mute_conversation)).show());
                    }
                    return true;
                } else if (itemId == R.id.action_pin) {
                    if (statusToDeal.pinned) {
                        statusesVM.unPin(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.toast_unpin)).show());
                    } else {
                        statusesVM.pin(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.toast_pin)).show());
                    }
                    return true;
                } else if (itemId == R.id.action_bookmark) {
                    if (statusToDeal.bookmarked) {
                        statusesVM.unBookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.status_unbookmarked)).show());
                    } else {
                        statusesVM.bookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id).observe((LifecycleOwner) context, status1 -> Toasty.info(context, context.getString(R.string.status_bookmarked)).show());
                    }
                } else if (itemId == R.id.action_timed_mute) {
                    MastodonHelper.scheduleBoost(context, MastodonHelper.ScheduleType.TIMED_MUTED, statusToDeal, null, null);
                    return true;
                } else if (itemId == R.id.action_block) {
                    AlertDialog.Builder builderInner = new AlertDialog.Builder(context, Helper.dialogStyle());
                    builderInner.setTitle(stringArrayConf[1]);
                    builderInner.setMessage(statusToDeal.account.acct);
                    builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.yes, (dialog, which) -> accountsVM.block(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.account.id)
                            .observe((LifecycleOwner) context, relationShip -> {
                                sendAction(context, Helper.ARG_STATUS_ACCOUNT_ID_DELETED, null, statusToDeal.account.id);
                                Toasty.info(context, context.getString(R.string.toast_block)).show();
                            }));
                    builderInner.show();
                } else if (itemId == R.id.action_translate) {
                    MyTransL.translatorEngine et = MyTransL.translatorEngine.LIBRETRANSLATE;
                    final MyTransL myTransL = MyTransL.getInstance(et);
                    myTransL.setObfuscation(true);
                    Params params = new Params();
                    params.setSplit_sentences(false);
                    params.setFormat(Params.fType.TEXT);
                    params.setSource_lang("auto");
                    myTransL.setLibretranslateDomain("translate.fedilab.app");
                    String statusToTranslate;
                    String translate = sharedpreferences.getString(context.getString(R.string.SET_LIVE_TRANSLATE), MyTransL.getLocale());
                    if (translate != null && translate.equalsIgnoreCase("default")) {
                        translate = MyTransL.getLocale();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        statusToTranslate = Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString();
                    else
                        statusToTranslate = Html.fromHtml(statusToDeal.content).toString();
                    myTransL.translate(statusToTranslate, translate, params, new Results() {
                        @Override
                        public void onSuccess(Translate translate) {
                            if (translate.getTranslatedContent() != null) {
                                statusToDeal.translationShown = true;
                                statusToDeal.translationContent = translate.getTranslatedContent();
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                            } else {
                                Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFail(HttpsConnectionException httpsConnectionException) {

                        }
                    });
                    return true;
                } else if (itemId == R.id.action_report) {
                    Intent intent = new Intent(context, ReportActivity.class);
                    intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                    context.startActivity(intent);
                } else if (itemId == R.id.action_copy) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    String content;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        content = Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString();
                    else
                        content = Html.fromHtml(statusToDeal.content).toString();
                    ClipData clip = ClipData.newPlainText(Helper.CLIP_BOARD, content);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toasty.info(context, context.getString(R.string.clipboard), Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else if (itemId == R.id.action_copy_link) {
                    ClipboardManager clipboard;
                    ClipData clip;
                    clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

                    clip = ClipData.newPlainText(Helper.CLIP_BOARD, statusToDeal.url);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toasty.info(context, context.getString(R.string.clipboard_url), Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else if (itemId == R.id.action_share) {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.shared_via));
                    String url;

                    if (statusToDeal.uri.startsWith("http"))
                        url = statusToDeal.uri;
                    else
                        url = statusToDeal.url;
                    String extra_text;
                    if (share_details) {
                        extra_text = statusToDeal.account.acct;
                        if (extra_text.split("@").length == 1)
                            extra_text = "@" + extra_text + "@" + BaseMainActivity.currentInstance;
                        else
                            extra_text = "@" + extra_text;
                        extra_text += " \uD83D\uDD17 " + url + "\r\n-\n";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            extra_text += Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString();
                        else
                            extra_text += Html.fromHtml(statusToDeal.content).toString();
                    } else {
                        extra_text = url;
                    }
                    sendIntent.putExtra(Intent.EXTRA_TEXT, extra_text);
                    sendIntent.setType("text/plain");
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_with)));
                } else if (itemId == R.id.action_custom_sharing) {
                    Intent intent = new Intent(context, CustomSharingActivity.class);
                    intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                    context.startActivity(intent);
                } else if (itemId == R.id.action_mention) {
                    Intent intent = new Intent(context, ComposeActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable(Helper.ARG_STATUS_MENTION, statusToDeal);
                    intent.putExtras(b);
                    context.startActivity(intent);
                } else if (itemId == R.id.action_open_with) {
                    new Thread(() -> {
                        try {
                            List<BaseAccount> accounts = new Account(context).getCrossAccounts();
                            if (accounts.size() > 1) {
                                List<app.fedilab.android.client.entities.api.Account> accountList = new ArrayList<>();
                                for (BaseAccount account : accounts) {
                                    accountList.add(account.mastodon_account);
                                }
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                Runnable myRunnable = () -> {
                                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(context, Helper.dialogStyle());
                                    builderSingle.setTitle(context.getString(R.string.choose_accounts));
                                    final AccountsSearchAdapter accountsSearchAdapter = new AccountsSearchAdapter(context, accountList);
                                    final BaseAccount[] accountArray = new BaseAccount[accounts.size()];
                                    int i = 0;
                                    for (BaseAccount account : accounts) {
                                        accountArray[i] = account;
                                        i++;
                                    }
                                    builderSingle.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                                    builderSingle.setAdapter(accountsSearchAdapter, (dialog, which) -> {
                                        BaseAccount account = accountArray[which];

                                        Toasty.info(context, context.getString(R.string.toast_account_changed, "@" + account.mastodon_account.acct + "@" + account.instance), Toasty.LENGTH_LONG).show();
                                        BaseMainActivity.currentToken = account.token;
                                        BaseMainActivity.currentUserID = account.user_id;
                                        BaseMainActivity.currentInstance = account.instance;
                                        MainActivity.currentAccount = account;
                                        SharedPreferences.Editor editor = sharedpreferences.edit();
                                        editor.putString(PREF_USER_TOKEN, account.token);
                                        editor.commit();
                                        Intent mainActivity = new Intent(context, MainActivity.class);
                                        mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_WITH_ANOTHER_ACCOUNT);
                                        mainActivity.putExtra(Helper.PREF_MESSAGE_URL, statusToDeal.url);
                                        context.startActivity(mainActivity);
                                        ((Activity) context).finish();
                                        dialog.dismiss();
                                    });
                                    builderSingle.show();
                                };
                                mainHandler.post(myRunnable);
                            }

                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                return true;
            });
            popup.show();
        });
        if (statusToDeal.replies_count > 0 && !(context instanceof ContextActivity)) {
            holder.binding.replyCount.setVisibility(View.VISIBLE);
        } else {
            holder.binding.replyCount.setVisibility(View.GONE);
        }
        holder.binding.actionButtonReply.setOnLongClickListener(v -> {
            CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.REPLY_ACTION, null, statusToDeal);
            return true;
        });
        holder.binding.actionButtonReply.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                Status fetchedStatus = statusList.get(0);
                                Intent intent = new Intent(context, ComposeActivity.class);
                                intent.putExtra(Helper.ARG_STATUS_REPLY, fetchedStatus);
                                context.startActivity(intent);
                            } else {
                                Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Intent intent = new Intent(context, ComposeActivity.class);
                intent.putExtra(Helper.ARG_STATUS_REPLY, statusToDeal);
                if (status.reblog != null) {
                    intent.putExtra(Helper.ARG_MENTION_BOOSTER, status.account);
                }
                context.startActivity(intent);
            }
        });
        //For reports
        if (holder.bindingReport != null) {
            holder.bindingReport.checkbox.setChecked(status.isChecked);
            holder.bindingReport.checkbox.setOnClickListener(v -> status.isChecked = !status.isChecked);
        }

        if (status.isFetchMore && fetchMoreCallBack != null) {
            holder.binding.layoutFetchMore.fetchMoreContainer.setVisibility(View.VISIBLE);
            holder.binding.layoutFetchMore.fetchMoreMin.setOnClickListener(v -> {
                status.isFetchMore = false;
                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                if (holder.getBindingAdapterPosition() < statusList.size() - 1) {
                    String fromId;
                    if (status.positionFetchMore == Status.PositionFetchMore.TOP) {
                        fromId = statusList.get(holder.getBindingAdapterPosition() + 1).id;
                    } else {
                        fromId = status.id;
                    }
                    fetchMoreCallBack.onClickMinId(fromId, status);
                }
            });
            holder.binding.layoutFetchMore.fetchMoreMax.setOnClickListener(v -> {
                //We hide the button
                status.isFetchMore = false;
                String fromId;
                if (status.positionFetchMore == Status.PositionFetchMore.TOP) {
                    fromId = statusList.get(holder.getBindingAdapterPosition()).id;
                } else {
                    fromId = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                }
                fetchMoreCallBack.onClickMaxId(fromId, status);
                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
            });
        } else {
            holder.binding.layoutFetchMore.fetchMoreContainer.setVisibility(View.GONE);
        }

    }

    private static boolean mediaObfuscated(Status status) {
        //Media is not sensitive and  doesn't have a spoiler text
        if (!status.isMediaObfuscated) {
            return false;
        }
        if (!status.sensitive && (status.spoiler_text == null || status.spoiler_text.trim().isEmpty())) {
            return false;
        }
        if (status.isMediaObfuscated && status.spoiler_text != null && !status.spoiler_text.trim().isEmpty()) {
            return true;
        } else {
            return status.sensitive;
        }
    }

    /**
     * Send a broadcast to other open fragments that content a timeline
     *
     * @param context - Context
     * @param type    - String type for the broadCast (Helper.ARG_STATUS_ACTION / Helper.ARG_STATUS_ACCOUNT_ID_DELETED / Helper.ARG_STATUS_DELETED )
     * @param status  - Status that is sent (can be null)
     * @param id      - Id of an account (can be null)
     */
    public static void sendAction(@NonNull Context context, @NonNull String type, @Nullable Status status, @Nullable String id) {
        Bundle b = new Bundle();
        if (status != null) {
            b.putSerializable(type, status);
        }
        if (id != null) {
            b.putSerializable(type, id);
        }
        Intent intentBC = new Intent(Helper.RECEIVE_STATUS_ACTION);
        intentBC.putExtras(b);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBC);
    }


    @Override
    public int getItemViewType(int position) {
        if (timelineType == Timeline.TimeLineEnum.ART) {
            return STATUS_ART;
        } else {
            if (statusList.get(position).filteredByApp != null) {
                return STATUS_FILTERED;
            } else {
                return isVisible(timelineType, statusList.get(position)) ? STATUS_VISIBLE : STATUS_HIDDEN;
            }

        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == STATUS_HIDDEN) { //Hidden statuses - ie: filtered
            DrawerStatusHiddenBinding itemBinding = DrawerStatusHiddenBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_ART) { //Art statuses
            DrawerStatusArtBinding itemBinding = DrawerStatusArtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_FILTERED) { //Art statuses
            DrawerStatusFilteredBinding itemBinding = DrawerStatusFilteredBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else { //Classic statuses
            if (!minified) {
                DrawerStatusBinding itemBinding = DrawerStatusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new StatusViewHolder(itemBinding);
            } else {
                DrawerStatusReportBinding itemBinding = DrawerStatusReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new StatusViewHolder(itemBinding);
            }
        }
    }

    public int getCount() {
        return statusList.size();
    }

    public Status getItem(int position) {
        return statusList.get(position);
    }


    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        //Nothing to do with hidden statuses
        if (viewHolder.getItemViewType() == STATUS_HIDDEN) {
            return;
        }
        Status status = statusList.get(position);
        if (viewHolder.getItemViewType() == STATUS_VISIBLE) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
            statusManagement(context, statusesVM, searchVM, holder, this, statusList, status, timelineType, minified, canBeFederated, checkRemotely, fetchMoreCallBack);
        } else if (viewHolder.getItemViewType() == STATUS_FILTERED) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            holder.bindingFiltered.filteredText.setText(context.getString(R.string.filtered_by, status.filteredByApp.title));
            holder.bindingFiltered.displayButton.setOnClickListener(v -> {
                status.filteredByApp = null;
                notifyItemChanged(position);
            });
        } else if (viewHolder.getItemViewType() == STATUS_ART) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            MastodonHelper.loadPPMastodon(holder.bindingArt.artPp, status.account);
            if (status.art_attachment != null) {

                holder.bindingArt.artMedia.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        holder.bindingArt.artMedia.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (status.art_attachment.meta != null) {
                            float viewWidth = holder.bindingArt.artMedia.getWidth();
                            ConstraintLayout.LayoutParams lp;
                            float mediaH = status.art_attachment.meta.small.height;
                            float mediaW = status.art_attachment.meta.small.width;
                            float ratio = 1.0f;
                            if (mediaW != 0) {
                                ratio = viewWidth / mediaW;
                            }
                            lp = new ConstraintLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, (int) (mediaH * ratio));
                            holder.bindingArt.artMedia.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            holder.bindingArt.artMedia.setLayoutParams(lp);
                        }

                    }
                });
                if (status.art_attachment.meta != null) {
                    float viewWidth = holder.bindingArt.artMedia.getWidth();
                    ConstraintLayout.LayoutParams lp;
                    float mediaH = status.art_attachment.meta.small.height;
                    float mediaW = status.art_attachment.meta.small.width;
                    float ratio = 1.0f;
                    if (mediaW != 0) {
                        ratio = viewWidth / mediaW;
                    }
                    lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (int) (mediaH * ratio));
                    holder.bindingArt.artMedia.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    holder.bindingArt.artMedia.setLayoutParams(lp);
                }
                Glide.with(holder.bindingArt.artMedia.getContext())
                        .load(status.art_attachment.preview_url)
                        .apply(new RequestOptions().transform(new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                        .into(holder.bindingArt.artMedia);
            }
            holder.bindingArt.artAcct.setText(
                    status.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.bindingArt.artAcct)),
                    TextView.BufferType.SPANNABLE);
            holder.bindingArt.artUsername.setText(String.format(Locale.getDefault(), "@%s", status.account.acct));
            holder.bindingArt.artPp.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle b = new Bundle();
                b.putSerializable(Helper.ARG_ACCOUNT, status.account);
                intent.putExtras(b);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context, holder.bindingArt.artPp, context.getString(R.string.activity_porfile_pp));
                context.startActivity(intent, options.toBundle());
            });
            holder.bindingArt.artMedia.setOnClickListener(v -> {
                Intent mediaIntent = new Intent(context, MediaActivity.class);
                Bundle b = new Bundle();
                b.putInt(Helper.ARG_MEDIA_POSITION, 1);
                ArrayList<Attachment> attachments = new ArrayList<>();
                attachments.add(status.art_attachment);
                b.putSerializable(Helper.ARG_MEDIA_ARRAY, attachments);
                mediaIntent.putExtras(b);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context, holder.bindingArt.artMedia, status.art_attachment.url);
                context.startActivity(mediaIntent, options.toBundle());
            });
            holder.bindingArt.bottomBanner.setOnClickListener(v -> {
                Intent intent = new Intent(context, ContextActivity.class);
                intent.putExtra(Helper.ARG_STATUS, status);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    public interface FetchMoreCallBack {
        void onClickMinId(String min_id, Status statusToUpdate);

        void onClickMaxId(String max_id, Status statusToUpdate);
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusBinding binding;
        DrawerStatusHiddenBinding bindingHidden;
        DrawerStatusReportBinding bindingReport;
        DrawerStatusNotificationBinding bindingNotification;
        DrawerStatusArtBinding bindingArt;
        DrawerStatusFilteredBinding bindingFiltered;

        StatusViewHolder(DrawerStatusBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

        StatusViewHolder(DrawerStatusReportBinding itemView) {
            super(itemView.getRoot());
            bindingReport = itemView;
            binding = itemView.status;
        }

        StatusViewHolder(DrawerStatusNotificationBinding itemView) {
            super(itemView.getRoot());
            bindingNotification = itemView;
            binding = itemView.status;
        }

        StatusViewHolder(DrawerStatusHiddenBinding itemView) {
            super(itemView.getRoot());
            bindingHidden = itemView;
        }


        StatusViewHolder(DrawerStatusArtBinding itemView) {
            super(itemView.getRoot());
            bindingArt = itemView;
        }

        StatusViewHolder(DrawerStatusFilteredBinding itemView) {
            super(itemView.getRoot());
            bindingFiltered = itemView;
        }
    }


}
