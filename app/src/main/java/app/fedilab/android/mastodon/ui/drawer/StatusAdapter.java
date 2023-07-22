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
import static app.fedilab.android.BaseMainActivity.regex_home;
import static app.fedilab.android.BaseMainActivity.regex_local;
import static app.fedilab.android.BaseMainActivity.regex_public;
import static app.fedilab.android.BaseMainActivity.show_boosts;
import static app.fedilab.android.BaseMainActivity.show_dms;
import static app.fedilab.android.BaseMainActivity.show_replies;
import static app.fedilab.android.mastodon.activities.ContextActivity.expand;
import static app.fedilab.android.mastodon.helper.Helper.ARG_TIMELINE_REFRESH_ALL;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_ID;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_SOFTWARE;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.github.stom79.mytransl.MyTransL;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
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
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerFetchMoreBinding;
import app.fedilab.android.databinding.DrawerMessageFetchingBinding;
import app.fedilab.android.databinding.DrawerStatusArtBinding;
import app.fedilab.android.databinding.DrawerStatusBinding;
import app.fedilab.android.databinding.DrawerStatusFilteredBinding;
import app.fedilab.android.databinding.DrawerStatusFilteredHideBinding;
import app.fedilab.android.databinding.DrawerStatusHiddenBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.DrawerStatusPixelfedBinding;
import app.fedilab.android.databinding.DrawerStatusReportBinding;
import app.fedilab.android.databinding.LayoutMediaBinding;
import app.fedilab.android.databinding.LayoutPollItemBinding;
import app.fedilab.android.mastodon.activities.ComposeActivity;
import app.fedilab.android.mastodon.activities.ContextActivity;
import app.fedilab.android.mastodon.activities.CustomSharingActivity;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.activities.ReportActivity;
import app.fedilab.android.mastodon.activities.StatusHistoryActivity;
import app.fedilab.android.mastodon.activities.StatusInfoActivity;
import app.fedilab.android.mastodon.activities.TimelineActivity;
import app.fedilab.android.mastodon.activities.admin.AdminAccountActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.Reaction;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.RemoteInstance;
import app.fedilab.android.mastodon.client.entities.app.StatusCache;
import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.BlurHashDecoder;
import app.fedilab.android.mastodon.helper.CacheDataSourceFactory;
import app.fedilab.android.mastodon.helper.CrossActionHelper;
import app.fedilab.android.mastodon.helper.GlideApp;
import app.fedilab.android.mastodon.helper.GlideFocus;
import app.fedilab.android.mastodon.helper.GlideRequests;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.LongClickLinkMovementMethod;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.helper.SpannableHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.helper.TimelineHelper;
import app.fedilab.android.mastodon.helper.TranslateHelper;
import app.fedilab.android.mastodon.ui.fragment.timeline.FragmentMastodonContext;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.mastodon.viewmodel.pleroma.ActionsVM;
import de.timfreiheit.mathjax.android.MathJaxConfig;
import de.timfreiheit.mathjax.android.MathJaxView;
import es.dmoral.toasty.Toasty;
import jp.wasabeef.glide.transformations.BlurTransformation;


public class StatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<Attachment> {
    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_VISIBLE = 1;
    public static final int STATUS_ART = 2;
    public static final int STATUS_FILTERED = 3;
    public static final int STATUS_FILTERED_HIDE = 4;
    public static final int STATUS_PIXELFED = 5;
    private static float measuredWidth = -1;
    private static float measuredWidthArt = -1;
    private final List<Status> statusList;
    private final boolean minified;
    private final Timeline.TimeLineEnum timelineType;
    public RemoteInstance.InstanceType type;
    public PinnedTimeline pinnedTimeline;
    private final boolean canBeFederated;
    private final boolean checkRemotely;
    public FetchMoreCallBack fetchMoreCallBack;
    private Context context;
    private boolean visiblePixelfed;
    private RecyclerView mRecyclerView;

    public StatusAdapter(List<Status> statuses, Timeline.TimeLineEnum timelineType, boolean minified, boolean canBeFederated, boolean checkRemotely) {
        this.statusList = statuses;
        this.timelineType = timelineType;
        this.minified = minified;
        this.canBeFederated = canBeFederated;
        this.checkRemotely = checkRemotely;
    }


    private static boolean isVisiblePixelfed(Status status) {
        if (status.reblog != null) {
            status = status.reblog;
        }
        return status.media_attachments != null && status.media_attachments.size() > 0;
    }

    private static boolean isVisible(Timeline.TimeLineEnum timelineType, Status status) {
        if (timelineType == Timeline.TimeLineEnum.HOME && !show_boosts && status.reblog != null) {
            return false;
        }
        if (timelineType == Timeline.TimeLineEnum.HOME && !show_dms && status.visibility.equalsIgnoreCase("direct")) {
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
                                        RecyclerView recyclerView,
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
        boolean display_card = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_CARD), false);
        boolean share_details = sharedpreferences.getBoolean(context.getString(R.string.SET_SHARE_DETAILS), true);
        boolean confirmFav = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION_FAV), false);
        boolean confirmBoost = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION), true);
        boolean fullAttachement = sharedpreferences.getBoolean(context.getString(R.string.SET_FULL_PREVIEW), false);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);
        boolean displayBookmark = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_BOOKMARK) + MainActivity.currentUserID + MainActivity.currentInstance, true);
        boolean displayTranslate = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_TRANSLATE) + MainActivity.currentUserID + MainActivity.currentInstance, false);
        boolean displayCounters = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_COUNTER_FAV_BOOST), false);
        boolean removeLeftMargin = sharedpreferences.getBoolean(context.getString(R.string.SET_REMOVE_LEFT_MARGIN), false);
        boolean extraFeatures = sharedpreferences.getBoolean(context.getString(R.string.SET_EXTAND_EXTRA_FEATURES) + MainActivity.currentUserID + MainActivity.currentInstance, false);
        boolean displayQuote = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_QUOTES) + MainActivity.currentUserID + MainActivity.currentInstance, true);
        boolean displayReactions = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_REACTIONS) + MainActivity.currentUserID + MainActivity.currentInstance, true);
        boolean compactButtons = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_COMPACT_ACTION_BUTTON), false);
        boolean originalDateForBoost = sharedpreferences.getBoolean(context.getString(R.string.SET_BOOST_ORIGINAL_DATE), true);
        boolean hideSingleMediaWithCard = sharedpreferences.getBoolean(context.getString(R.string.SET_HIDE_SINGLE_MEDIA_WITH_CARD), false);
        boolean autofetch = sharedpreferences.getBoolean(context.getString(R.string.SET_AUTO_FETCH_MISSING_MESSAGES), false);
        boolean warnNoMedia = sharedpreferences.getBoolean(context.getString(R.string.SET_MANDATORY_ALT_TEXT_FOR_BOOSTS), true);


        if (compactButtons) {
            ConstraintSet set = new ConstraintSet();
            set.clone(holder.binding.actionButtons);
            set.clear(R.id.status_emoji, ConstraintSet.END);
            set.applyTo(holder.binding.actionButtons);
        }

        if (removeLeftMargin) {
            LinearLayoutCompat.MarginLayoutParams p = (LinearLayoutCompat.MarginLayoutParams) holder.binding.spoiler.getLayoutParams();
            p.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.spoiler.setLayoutParams(p);
            LinearLayoutCompat.MarginLayoutParams pe = (LinearLayoutCompat.MarginLayoutParams) holder.binding.spoilerExpand.getLayoutParams();
            pe.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.spoilerExpand.setLayoutParams(pe);
            LinearLayoutCompat.MarginLayoutParams psc = (LinearLayoutCompat.MarginLayoutParams) holder.binding.statusContent.getLayoutParams();
            psc.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.statusContent.setLayoutParams(psc);
            LinearLayoutCompat.MarginLayoutParams pct = (LinearLayoutCompat.MarginLayoutParams) holder.binding.containerTrans.getLayoutParams();
            pct.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.containerTrans.setLayoutParams(psc);
            LinearLayoutCompat.MarginLayoutParams pcv = (LinearLayoutCompat.MarginLayoutParams) holder.binding.card.getLayoutParams();
            pcv.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.card.setLayoutParams(pcv);
            LinearLayoutCompat.MarginLayoutParams pmc = (LinearLayoutCompat.MarginLayoutParams) holder.binding.mediaContainer.getLayoutParams();
            pmc.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.mediaContainer.setLayoutParams(pmc);
            LinearLayoutCompat.MarginLayoutParams pal = (LinearLayoutCompat.MarginLayoutParams) holder.binding.mediaCroppedContainer.getLayoutParams();
            pal.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.mediaCroppedContainer.setLayoutParams(pal);
            LinearLayoutCompat.MarginLayoutParams pp = (LinearLayoutCompat.MarginLayoutParams) holder.binding.poll.pollContainer.getLayoutParams();
            pp.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.poll.pollContainer.setLayoutParams(pp);
            LinearLayoutCompat.MarginLayoutParams pet = (LinearLayoutCompat.MarginLayoutParams) holder.binding.editTime.getLayoutParams();
            pet.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.editTime.setLayoutParams(pet);
            LinearLayoutCompat.MarginLayoutParams psi = (LinearLayoutCompat.MarginLayoutParams) holder.binding.statusInfo.getLayoutParams();
            psi.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.statusInfo.setLayoutParams(psi);
            LinearLayoutCompat.MarginLayoutParams pas = (LinearLayoutCompat.MarginLayoutParams) holder.binding.actionShareContainer.getLayoutParams();
            pas.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.actionShareContainer.setLayoutParams(pas);
            LinearLayoutCompat.MarginLayoutParams pab = (LinearLayoutCompat.MarginLayoutParams) holder.binding.actionButtons.getLayoutParams();
            pab.setMarginStart((int) Helper.convertDpToPixel(6, context));
            holder.binding.actionButtons.setLayoutParams(pab);
        }

        String loadMediaType = sharedpreferences.getString(context.getString(R.string.SET_LOAD_MEDIA_TYPE), "ALWAYS");

        if (statusToDeal.quote != null) {
            holder.binding.quotedMessage.cardviewContainer.setCardElevation((int) Helper.convertDpToPixel(5, context));
            holder.binding.quotedMessage.dividerCard.setVisibility(View.GONE);
            holder.binding.quotedMessage.cardviewContainer.setStrokeWidth((int) Helper.convertDpToPixel(1, context));
            holder.binding.quotedMessage.cardviewContainer.setOnClickListener(v -> holder.binding.quotedMessage.statusContent.callOnClick());
            holder.binding.quotedMessage.statusContent.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && !view.hasFocus()) {
                    try {
                        view.requestFocus();
                    } catch (Exception ignored) {
                    }
                }
                return false;
            });
            holder.binding.quotedMessage.statusContent.setOnClickListener(v -> {
                if (status.isFocused || v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                    if (v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                        v.setTag(null);
                    }
                    return;
                }
                Intent intent = new Intent(context, ContextActivity.class);
                intent.putExtra(Helper.ARG_STATUS, statusToDeal.quote);
                context.startActivity(intent);
            });
            holder.binding.quotedMessage.cardviewContainer.setStrokeColor(ThemeHelper.getAttColor(context, R.attr.colorPrimary));
            holder.binding.quotedMessage.statusContent.setText(
                    statusToDeal.quote.getSpanContent(context,
                            new WeakReference<>(holder.binding.quotedMessage.statusContent), null),
                    TextView.BufferType.SPANNABLE);
            MastodonHelper.loadPPMastodon(holder.binding.quotedMessage.avatar, statusToDeal.quote.account);
            if (statusToDeal.quote.account != null) {
                holder.binding.quotedMessage.displayName.setText(
                        statusToDeal.quote.account.getSpanDisplayName(context,
                                new WeakReference<>(holder.binding.quotedMessage.displayName)),
                        TextView.BufferType.SPANNABLE);
                holder.binding.quotedMessage.username.setText(String.format("@%s", statusToDeal.quote.account.acct));
            }

            if (statusToDeal.quote.spoiler_text != null && !statusToDeal.quote.spoiler_text.trim().isEmpty()) {
                holder.binding.quotedMessage.spoiler.setVisibility(View.VISIBLE);
                holder.binding.quotedMessage.spoiler.setText(
                        statusToDeal.quote.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.quotedMessage.spoiler), null),
                        TextView.BufferType.SPANNABLE);
            } else {
                holder.binding.quotedMessage.spoiler.setVisibility(View.GONE);
                holder.binding.quotedMessage.spoiler.setText(null);
            }
            holder.binding.quotedMessage.cardviewContainer.setVisibility(View.VISIBLE);
        } else {
            holder.binding.quotedMessage.cardviewContainer.setVisibility(View.GONE);
        }

        if (currentAccount != null && currentAccount.api == Account.API.PLEROMA || status.reactions != null) {
            if (status.pleroma != null && status.pleroma.emoji_reactions != null && status.pleroma.emoji_reactions.size() > 0) {
                holder.binding.layoutReactions.getRoot().setVisibility(View.VISIBLE);
                ReactionAdapter reactionAdapter = new ReactionAdapter(status.id, status.pleroma.emoji_reactions, true);
                holder.binding.layoutReactions.reactionsView.setAdapter(reactionAdapter);
                LinearLayoutManager layoutManager
                        = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                holder.binding.layoutReactions.reactionsView.setLayoutManager(layoutManager);
            } else if (status.reactions != null && status.reactions.size() > 0) {
                holder.binding.layoutReactions.getRoot().setVisibility(View.VISIBLE);
                ReactionAdapter reactionAdapter = new ReactionAdapter(status.id, status.reactions, true, false);
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
                            if (status.pleroma != null && status.pleroma.emoji_reactions != null) {
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
                            } else if (status.reactions != null) {
                                for (Reaction reaction : status.reactions) {
                                    if (reaction.name.compareTo(emojiStr) == 0 && reaction.me) {
                                        alreadyAdded = true;
                                        reaction.count = (reaction.count - 1);
                                        if (reaction.count == 0) {
                                            status.reactions.remove(reaction);
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
                                    status.reactions.add(0, reaction);
                                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                                }
                                if (alreadyAdded) {
                                    statusesVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                                } else {
                                    statusesVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                                }
                            }
                        })
                        .build(holder.binding.layoutReactions.fakeEdittext);
                emojiPopup.toggle();
            });
            holder.binding.statusAddCustomEmoji.setOnClickListener(v -> {

                final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context);
                int paddingPixel = 15;
                float density = context.getResources().getDisplayMetrics().density;
                int paddingDp = (int) (paddingPixel * density);
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builder.setTitle(R.string.insert_emoji);
                if (emojis != null && emojis.size() > 0) {
                    GridView gridView = new GridView(context);
                    gridView.setAdapter(new EmojiAdapter(emojis.get(BaseMainActivity.currentInstance)));
                    gridView.setNumColumns(5);
                    gridView.setOnItemClickListener((parent, view, index, id) -> {
                        if (emojis.get(BaseMainActivity.currentInstance) == null) {
                            return;
                        }
                        String emojiStr = emojis.get(BaseMainActivity.currentInstance).get(index).shortcode;
                        String url = emojis.get(BaseMainActivity.currentInstance).get(index).url;
                        String static_url = emojis.get(BaseMainActivity.currentInstance).get(index).static_url;
                        boolean alreadyAdded = false;
                        if (status.pleroma != null && status.pleroma.emoji_reactions != null) {
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
                        } else if (status.reactions != null) {
                            for (Reaction reaction : status.reactions) {
                                if (reaction.name.compareTo(emojiStr) == 0 && reaction.me) {
                                    alreadyAdded = true;
                                    reaction.count = (reaction.count - 1);
                                    if (reaction.count == 0) {
                                        status.reactions.remove(reaction);
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
                                status.reactions.add(0, reaction);
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                            }
                            if (alreadyAdded) {
                                statusesVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                            } else {
                                statusesVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, emojiStr);
                            }
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

        if (extraFeatures) {
            if (displayQuote) {
                holder.binding.actionButtonQuote.setVisibility(View.VISIBLE);
            } else {
                holder.binding.actionButtonQuote.setVisibility(View.GONE);
            }
            if (displayReactions) {
                holder.binding.statusAddCustomEmoji.setVisibility(View.VISIBLE);
                holder.binding.statusEmoji.setVisibility(View.VISIBLE);
            } else {
                holder.binding.statusAddCustomEmoji.setVisibility(View.GONE);
                holder.binding.statusEmoji.setVisibility(View.GONE);
            }


        }

        if (status.isMaths == null) {
            if (status.content != null && Helper.mathsPattern.matcher(status.content).find()) {
                holder.binding.actionButtonMaths.setVisibility(View.VISIBLE);
                status.isMaths = true;
            } else {
                holder.binding.actionButtonMaths.setVisibility(View.GONE);
            }
        } else {
            if (status.isMaths) {
                holder.binding.actionButtonMaths.setVisibility(View.VISIBLE);
            } else {
                holder.binding.actionButtonMaths.setVisibility(View.GONE);
            }
        }
        if (status.mathsShown) {
            holder.binding.statusContentMaths.setVisibility(View.VISIBLE);
            holder.binding.statusContent.setVisibility(View.GONE);
            holder.binding.statusContentMaths.removeAllViews();
            MathJaxConfig mathJaxConfig = new MathJaxConfig();
            switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    mathJaxConfig.setTextColor("white");
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    mathJaxConfig.setTextColor("black");
                    break;
            }
            mathJaxConfig.setAutomaticLinebreaks(true);

            MathJaxView mathview = new MathJaxView(context, mathJaxConfig);
            holder.binding.statusContentMaths.addView(mathview);
            if (status.contentSpan != null) {
                String input = status.contentSpan.toString();
                input = input.replaceAll("'", "&#39;");
                input = input.replaceAll("\"", "&#34;");
                mathview.setInputText(input);
            } else {
                status.mathsShown = false;
                holder.binding.statusContentMaths.setVisibility(View.GONE);
                holder.binding.statusContent.setVisibility(View.VISIBLE);
            }
        } else {
            holder.binding.statusContentMaths.setVisibility(View.GONE);
            holder.binding.statusContent.setVisibility(View.VISIBLE);
        }
        holder.binding.actionButtonMaths.setOnClickListener(v -> {

            status.mathsShown = !status.mathsShown;
            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
        });
        holder.binding.actionButtonFavorite.setActiveImage(R.drawable.ic_round_star_24);
        holder.binding.actionButtonFavorite.setInactiveImage(R.drawable.ic_round_star_border_24);
        holder.binding.actionButtonBookmark.setActiveImage(R.drawable.ic_round_bookmark_24);
        holder.binding.actionButtonBookmark.setInactiveImage(R.drawable.ic_round_bookmark_border_24);
        holder.binding.actionButtonBoost.setActiveImage(R.drawable.ic_round_repeat_active_24);
        holder.binding.actionButtonBoost.setInactiveImage(R.drawable.ic_round_repeat_24);
        holder.binding.actionButtonFavorite.setActiveImageTint(R.color.marked_icon);
        holder.binding.actionButtonBoost.setActiveImageTint(R.color.boost_icon);
        holder.binding.actionButtonBookmark.setActiveImageTint(R.color.marked_icon);
        applyColor(context, holder);

        if (status.pinned) {
            holder.binding.statusPinned.setVisibility(View.VISIBLE);
        } else {
            holder.binding.statusPinned.setVisibility(View.GONE);
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

        if (statusToDeal.card != null && (display_card || statusToDeal.isFocused) && statusToDeal.quote_id == null) {
            if (statusToDeal.card.width > statusToDeal.card.height) {
                holder.binding.cardImageHorizontal.setVisibility(View.VISIBLE);
                holder.binding.cardImageVertical.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext()).load(statusToDeal.card.image).into(holder.binding.cardImageHorizontal);
            } else {
                holder.binding.cardImageHorizontal.setVisibility(View.GONE);
                holder.binding.cardImageVertical.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(statusToDeal.card.image).into(holder.binding.cardImageVertical);
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
            if (displayTranslate) {
                if (statusToDeal.language != null && statusToDeal.language.trim().length() > 0 && statusToDeal.language.equalsIgnoreCase(MyTransL.getLocale())) {
                    holder.binding.actionButtonTranslate.setVisibility(View.GONE);
                } else {
                    holder.binding.actionButtonTranslate.setVisibility(View.VISIBLE);
                }
            } else {
                holder.binding.actionButtonTranslate.setVisibility(View.GONE);
            }
            //--- ACTIONS ---
            holder.binding.actionButtonBookmark.setChecked(statusToDeal.bookmarked);
            //---> BOOKMARK/UNBOOKMARK
            holder.binding.actionButtonBookmark.setOnLongClickListener(v -> {
                CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.BOOKMARK_ACTION, null, statusToDeal);
                return true;
            });
            holder.binding.actionButtonTranslate.setOnClickListener(v -> translate(context, statusToDeal, holder, adapter));
            holder.binding.actionButtonBookmark.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                    Status fetchedStatus = results.statuses.get(0);
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
            holder.binding.avatar.setOnClickListener(v -> {
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
                boolean needToWarnForMissingDescription = false;
                if (warnNoMedia && statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0) {
                    for (Attachment attachment : statusToDeal.media_attachments) {
                        if (attachment.description == null || attachment.description.trim().length() == 0) {
                            needToWarnForMissingDescription = true;
                            break;
                        }
                    }
                }
                if (needToWarnForMissingDescription) {
                    AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);
                    alt_bld.setMessage(context.getString(R.string.reblog_missing_description));
                    alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                        CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, null, statusToDeal);
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                } else {
                    CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.REBLOG_ACTION, null, statusToDeal);
                }
                return true;
            });
            holder.binding.actionButtonBoost.setOnClickListener(v -> {
                boolean needToWarnForMissingDescription = false;
                if (warnNoMedia && statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0) {
                    for (Attachment attachment : statusToDeal.media_attachments) {
                        if (attachment.description == null || attachment.description.trim().length() == 0) {
                            needToWarnForMissingDescription = true;
                            break;
                        }
                    }
                }
                if (confirmBoost || needToWarnForMissingDescription) {
                    AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);

                    if (statusToDeal.reblogged) {
                        alt_bld.setMessage(context.getString(R.string.reblog_remove));
                    } else {
                        if (!needToWarnForMissingDescription) {
                            alt_bld.setMessage(context.getString(R.string.reblog_add));
                        } else {
                            alt_bld.setMessage(context.getString(R.string.reblog_missing_description));
                        }
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
                    AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(context);
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
        holder.binding.username.setText(String.format("@%s", statusToDeal.account.acct));
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

        holder.binding.actionButtonTranslate.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonTranslate.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonTranslate.requestLayout();

        holder.binding.actionButtonBoost.setImageSize((int) (normalSize * scaleIcon));
        holder.binding.actionButtonFavorite.setImageSize((int) (normalSize * scaleIcon));
        holder.binding.actionButtonBookmark.setImageSize((int) (normalSize * scaleIcon));


        holder.binding.statusAddCustomEmoji.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.statusAddCustomEmoji.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.statusAddCustomEmoji.requestLayout();

        holder.binding.actionButtonQuote.getLayoutParams().width = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonQuote.getLayoutParams().height = (int) (normalSize * scaleIcon);
        holder.binding.actionButtonQuote.requestLayout();

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
        holder.binding.visibility.setContentDescription(context.getString(R.string.v_public));
        holder.binding.visibilitySmall.setContentDescription(context.getString(R.string.v_public));
        switch (status.visibility) {
            case "unlisted":
                holder.binding.visibility.setContentDescription(context.getString(R.string.v_unlisted));
                holder.binding.visibilitySmall.setContentDescription(context.getString(R.string.v_unlisted));
                ressource = R.drawable.ic_baseline_lock_open_24;
                break;
            case "private":
                ressource = R.drawable.ic_baseline_lock_24;
                holder.binding.visibility.setContentDescription(context.getString(R.string.v_private));
                holder.binding.visibilitySmall.setContentDescription(context.getString(R.string.v_private));
                break;
            case "direct":
                ressource = R.drawable.ic_baseline_mail_24;
                holder.binding.visibility.setContentDescription(context.getString(R.string.v_direct));
                holder.binding.visibilitySmall.setContentDescription(context.getString(R.string.v_direct));
                break;
        }

        if (statusToDeal.local_only) {
            holder.binding.localOnly.setVisibility(View.VISIBLE);
        } else {
            holder.binding.localOnly.setVisibility(View.GONE);
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
            if (originalDateForBoost || status.reblog == null) {
                holder.binding.time.setText(Helper.longDateToString(statusToDeal.created_at));
            } else {
                holder.binding.time.setText(Helper.longDateToString(status.created_at));
            }
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
                if (originalDateForBoost || status.reblog == null) {
                    holder.binding.time.setText(Helper.dateDiff(context, statusToDeal.created_at));
                } else {
                    holder.binding.time.setText(Helper.dateDiff(context, status.created_at));
                }
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
                if (originalDateForBoost || status.reblog == null) {
                    holder.binding.dateShort.setText(Helper.dateDiff(context, statusToDeal.created_at));
                } else {
                    holder.binding.dateShort.setText(Helper.dateDiff(context, status.created_at));
                }
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
                                new WeakReference<>(holder.binding.spoiler), () -> recyclerView.post(() -> adapter.notifyItemChanged(holder.getBindingAdapterPosition()))),
                        TextView.BufferType.SPANNABLE);
                statusToDeal.isExpended = true;
            } else {
                holder.binding.spoilerExpand.setOnClickListener(v -> {
                    statusToDeal.isExpended = !statusToDeal.isExpended;
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                });
                holder.binding.spoilerExpand.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setVisibility(View.VISIBLE);

                holder.binding.spoiler.setText(
                        statusToDeal.getSpanSpoiler(context,
                                new WeakReference<>(holder.binding.spoiler), () -> recyclerView.post(() -> adapter.notifyItemChanged(holder.getBindingAdapterPosition()))),
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
            holder.binding.statusBoosterUsername.setText(String.format("@%s", status.account.acct));
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
                        new WeakReference<>(holder.binding.statusContent), () -> {
                            recyclerView.post(() -> adapter.notifyItemChanged(holder.getBindingAdapterPosition()));
                        }),
                TextView.BufferType.SPANNABLE);
        if (truncate_toots_size > 0) {
            holder.binding.statusContent.setMaxLines(truncate_toots_size);
            holder.binding.statusContent.setEllipsize(TextUtils.TruncateAt.END);
            if (holder.binding.statusContent.getLineCount() == 0) {
                holder.binding.statusContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (holder.binding.statusContent.getLineCount() > 1) {
                            holder.binding.statusContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            resizeContent(context, holder, statusToDeal, adapter);
                        }
                    }
                });
            } else {
                resizeContent(context, holder, statusToDeal, adapter);
            }
        } else {
            holder.binding.toggleTruncate.setVisibility(View.GONE);
        }
        if (statusToDeal.translationContent != null) {
            holder.binding.containerTrans.setVisibility(View.VISIBLE);
            holder.binding.statusContentTranslated.setText(
                    statusToDeal.getSpanTranslate(context,
                            new WeakReference<>(holder.binding.statusContentTranslated), () -> {
                                recyclerView.post(() -> adapter.notifyItemChanged(holder.getBindingAdapterPosition()));
                            }),
                    TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.containerTrans.setVisibility(View.GONE);
        }
        if (statusToDeal.spoiler_text == null || statusToDeal.spoiler_text.trim().isEmpty() || statusToDeal.isExpended) {
            if (statusToDeal.content.trim().length() == 0) {
                holder.binding.mediaContainer.setVisibility(View.GONE);
            } else {
                if (!status.mathsShown) {
                    holder.binding.statusContent.setVisibility(View.VISIBLE);
                }
                if (statusToDeal.card != null && statusToDeal.quote_id == null && (display_card || statusToDeal.isFocused)) {
                    holder.binding.card.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.card.setVisibility(View.GONE);
                }
            }
        } else {
            holder.binding.statusContent.setVisibility(View.GONE);
            holder.binding.mediaContainer.setVisibility(View.GONE);
            holder.binding.card.setVisibility(View.GONE);
        }
        if (measuredWidth <= 0 && statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0) {
            holder.binding.mediaContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    holder.binding.mediaContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (fullAttachement) {
                        measuredWidth = holder.binding.mediaContainer.getWidth();
                    } else {
                        measuredWidth = holder.binding.media.mediaContainer.getWidth();
                    }
                    if (adapter != null && statusList != null) {
                        adapter.notifyItemChanged(0, statusList.size());
                    }
                }
            });
        }
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        holder.binding.mediaContainer.removeAllViews();
        PlayerView video = holder.binding.media.media1Container.findViewById(R.id.media_video);
        if (video != null && video.getPlayer() != null) {
            video.getPlayer().release();
        }
        holder.binding.media.media1Container.removeAllViews();
        video = holder.binding.media.media2Container.findViewById(R.id.media_video);
        if (video != null && video.getPlayer() != null) {
            video.getPlayer().release();
        }
        holder.binding.media.media2Container.removeAllViews();
        video = holder.binding.media.media3Container.findViewById(R.id.media_video);
        if (video != null && video.getPlayer() != null) {
            video.getPlayer().release();
        }
        holder.binding.media.media3Container.removeAllViews();
        video = holder.binding.media.media4Container.findViewById(R.id.media_video);
        if (video != null && video.getPlayer() != null) {
            video.getPlayer().release();
        }
        holder.binding.media.media4Container.removeAllViews();

        //--- MEDIA ATTACHMENT ---
        boolean cardDisplayed = (statusToDeal.card != null && (display_card || statusToDeal.isFocused) && statusToDeal.quote_id == null);
        if (statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0 && (!hideSingleMediaWithCard || !cardDisplayed || statusToDeal.media_attachments.size() > 1)) {

            if ((loadMediaType.equals("ASK") || (loadMediaType.equals("WIFI") && !TimelineHelper.isOnWIFI(context))) && !statusToDeal.canLoadMedia) {
                holder.binding.mediaContainer.setVisibility(View.GONE);
                holder.binding.displayMedia.setVisibility(View.VISIBLE);
                holder.binding.displayMedia.setOnClickListener(v -> {
                    statusToDeal.canLoadMedia = true;
                    if (adapter != null) {
                        adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                    }
                });
            } else {
                int mediaPosition = 1;
                boolean autoplaygif = sharedpreferences.getBoolean(context.getString(R.string.SET_AUTO_PLAY_GIG_MEDIA), true);
                if (!fullAttachement || statusToDeal.sensitive) {
                    int defaultHeight = (int) Helper.convertDpToPixel(300, context);
                    int orientation = context.getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_PORTRAIT && measuredWidth > 0) {
                        defaultHeight = (int) (measuredWidth * 3) / 4;
                    }
                    LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, defaultHeight);
                    holder.binding.media.mediaContainer.setLayoutParams(lp);
                    if (statusToDeal.media_attachments.size() == 1) {
                        holder.binding.media.media1Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media2Container.setVisibility(View.GONE);
                        holder.binding.media.media3Container.setVisibility(View.GONE);
                        holder.binding.media.media4Container.setVisibility(View.GONE);
                        holder.binding.media.moreMedia.setVisibility(View.GONE);
                    } else if (statusToDeal.media_attachments.size() == 2) {
                        holder.binding.media.media1Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media2Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media3Container.setVisibility(View.GONE);
                        holder.binding.media.media4Container.setVisibility(View.GONE);
                        holder.binding.media.moreMedia.setVisibility(View.GONE);
                    } else if (statusToDeal.media_attachments.size() == 3) {
                        holder.binding.media.media1Container.setVisibility(View.VISIBLE);
                        if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.getSmall() != null && statusToDeal.media_attachments.get(0).meta.getSmall().width < statusToDeal.media_attachments.get(0).meta.getSmall().height) {
                            ConstraintSet constraintSet = new ConstraintSet();
                            constraintSet.clone(holder.binding.media.mediaContainer);
                            constraintSet.connect(holder.binding.media.media4Container.getId(), ConstraintSet.START, holder.binding.media.media1Container.getId(), ConstraintSet.END);
                            constraintSet.connect(holder.binding.media.media4Container.getId(), ConstraintSet.TOP, holder.binding.media.media2Container.getId(), ConstraintSet.BOTTOM);
                            constraintSet.applyTo(holder.binding.media.mediaContainer);
                            holder.binding.media.media2Container.setVisibility(View.VISIBLE);
                            holder.binding.media.media3Container.setVisibility(View.GONE);
                        } else {
                            ConstraintSet constraintSet = new ConstraintSet();
                            constraintSet.clone(holder.binding.media.mediaContainer);
                            constraintSet.connect(holder.binding.media.media4Container.getId(), ConstraintSet.START, holder.binding.media.media3Container.getId(), ConstraintSet.END);
                            constraintSet.connect(holder.binding.media.media4Container.getId(), ConstraintSet.TOP, holder.binding.media.media1Container.getId(), ConstraintSet.BOTTOM);
                            constraintSet.applyTo(holder.binding.media.mediaContainer);
                            holder.binding.media.media2Container.setVisibility(View.GONE);
                            holder.binding.media.media3Container.setVisibility(View.VISIBLE);
                        }
                        holder.binding.media.media4Container.setVisibility(View.VISIBLE);
                        holder.binding.media.moreMedia.setVisibility(View.GONE);
                    } else if (statusToDeal.media_attachments.size() == 4) {
                        holder.binding.media.media1Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media2Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media3Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media4Container.setVisibility(View.VISIBLE);
                        holder.binding.media.moreMedia.setVisibility(View.GONE);
                    } else if (statusToDeal.media_attachments.size() > 4) {
                        holder.binding.media.media1Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media2Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media3Container.setVisibility(View.VISIBLE);
                        holder.binding.media.media4Container.setVisibility(View.VISIBLE);
                        holder.binding.media.moreMedia.setVisibility(View.VISIBLE);
                        holder.binding.media.moreMedia.setText(context.getString(R.string.more_media, "+" + (statusToDeal.media_attachments.size() - 4)));
                    }
                }

                for (Attachment attachment : statusToDeal.media_attachments) {
                    LayoutMediaBinding layoutMediaBinding = LayoutMediaBinding.inflate(LayoutInflater.from(context));
                    if ((fullAttachement && (!statusToDeal.sensitive || expand_media))) {
                        holder.binding.mediaContainer.addView(layoutMediaBinding.getRoot());
                    } else {
                        if (mediaPosition == 1) {
                            holder.binding.media.media1Container.addView(layoutMediaBinding.getRoot());
                        } else if (mediaPosition == 2 && statusToDeal.media_attachments.size() == 3) {
                            if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.getSmall() != null && statusToDeal.media_attachments.get(0).meta.getSmall().width < statusToDeal.media_attachments.get(0).meta.getSmall().height) {
                                holder.binding.media.media2Container.addView(layoutMediaBinding.getRoot());
                            } else {
                                holder.binding.media.media3Container.addView(layoutMediaBinding.getRoot());
                            }
                        } else if (mediaPosition == 2) {
                            holder.binding.media.media2Container.addView(layoutMediaBinding.getRoot());
                        } else if (mediaPosition == 3 && statusToDeal.media_attachments.size() == 3) {
                            holder.binding.media.media4Container.addView(layoutMediaBinding.getRoot());
                        } else if (mediaPosition == 3) {
                            holder.binding.media.media3Container.addView(layoutMediaBinding.getRoot());
                        } else if (mediaPosition == 4) {
                            holder.binding.media.media4Container.addView(layoutMediaBinding.getRoot());
                        }
                    }
                    if (fullAttachement && (!statusToDeal.sensitive || expand_media)) {

                        float ratio = 1.0f;
                        float mediaH = -1.0f;
                        float mediaW = -1.0f;
                        if (attachment.meta != null && attachment.meta.getSmall() != null) {
                            mediaH = attachment.meta.getSmall().height;
                            mediaW = attachment.meta.getSmall().width;
                            if (mediaW != 0) {
                                ratio = measuredWidth > 0 ? measuredWidth / mediaW : 1.0f;
                            }
                        }
                        if (autoplaygif && attachment.type.equalsIgnoreCase("gifv")) {

                            layoutMediaBinding.media.setVisibility(View.GONE);
                            layoutMediaBinding.mediaVideo.setVisibility(View.VISIBLE);
                            LinearLayout.LayoutParams lp;
                            if (mediaH > 0 && (!statusToDeal.sensitive || expand_media)) {
                                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (mediaH * ratio));
                            } else {
                                lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            }
                            layoutMediaBinding.mediaVideo.setLayoutParams(lp);

                            Uri uri = Uri.parse(attachment.url);
                            int video_cache = sharedpreferences.getInt(context.getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);
                            ProgressiveMediaSource videoSource;
                            MediaItem mediaItem = new MediaItem.Builder().setUri(uri).build();
                            if (video_cache == 0) {
                                DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
                                videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                        .createMediaSource(mediaItem);
                            } else {
                                CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(context);
                                videoSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                                        .createMediaSource(mediaItem);
                            }
                            try {
                                ExoPlayer player = new ExoPlayer.Builder(context).build();
                                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                                layoutMediaBinding.mediaVideo.setPlayer(player);
                                player.setMediaSource(videoSource);
                                player.prepare();
                                player.setPlayWhenReady(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            int finalMediaPosition = mediaPosition;
                            layoutMediaBinding.mediaVideo.setOnClickListener(v -> {
                                final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
                                if (statusToDeal.sensitive && !expand_media) {
                                    statusToDeal.sensitive = false;
                                    int position = holder.getBindingAdapterPosition();
                                    adapter.notifyItemChanged(position);

                                    if (timeout > 0) {
                                        new CountDownTimer((timeout * 1000L), 1000) {
                                            public void onTick(long millisUntilFinished) {
                                            }

                                            public void onFinish() {
                                                statusToDeal.sensitive = true;
                                                adapter.notifyItemChanged(position);
                                            }
                                        }.start();
                                    }
                                    return;
                                }
                                Intent mediaIntent = new Intent(context, MediaActivity.class);
                                Bundle b = new Bundle();
                                b.putInt(Helper.ARG_MEDIA_POSITION, finalMediaPosition);
                                b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(statusToDeal.media_attachments));
                                mediaIntent.putExtras(b);
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, statusToDeal.media_attachments.get(0).url);
                                // start the new activity
                                context.startActivity(mediaIntent, options.toBundle());
                            });
                            layoutMediaBinding.viewHide.setOnClickListener(v -> {
                                statusToDeal.sensitive = !statusToDeal.sensitive;
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                            });
                        } else {
                            loadAndAddAttachment(context, layoutMediaBinding, holder, adapter, mediaPosition, mediaW, mediaH, ratio, statusToDeal, attachment);
                        }

                    } else {
                        layoutMediaBinding.mediaRoot.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                        if (autoplaygif && attachment.type.equalsIgnoreCase("gifv") && !statusToDeal.sensitive) {
                            layoutMediaBinding.media.setVisibility(View.GONE);
                            layoutMediaBinding.mediaVideo.setVisibility(View.VISIBLE);
                            layoutMediaBinding.mediaVideo.onResume();
                            Uri uri = Uri.parse(attachment.url);
                            int video_cache = sharedpreferences.getInt(context.getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);
                            ProgressiveMediaSource videoSource;
                            MediaItem mediaItem = new MediaItem.Builder().setUri(uri).build();
                            if (video_cache == 0) {
                                DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);
                                videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                        .createMediaSource(mediaItem);
                            } else {
                                CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(context);
                                videoSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                                        .createMediaSource(mediaItem);
                            }
                            try {
                                ExoPlayer player = new ExoPlayer.Builder(context).build();
                                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                                layoutMediaBinding.mediaVideo.setPlayer(player);
                                player.setMediaSource(videoSource);
                                player.prepare();
                                player.setPlayWhenReady(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int finalMediaPosition = mediaPosition;
                            layoutMediaBinding.mediaVideo.setOnClickListener(v -> {
                                final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
                                if (statusToDeal.sensitive && !expand_media) {
                                    statusToDeal.sensitive = false;
                                    int position = holder.getBindingAdapterPosition();
                                    adapter.notifyItemChanged(position);

                                    if (timeout > 0) {
                                        new CountDownTimer((timeout * 1000L), 1000) {
                                            public void onTick(long millisUntilFinished) {
                                            }

                                            public void onFinish() {
                                                statusToDeal.sensitive = true;
                                                adapter.notifyItemChanged(position);
                                            }
                                        }.start();
                                    }
                                    return;
                                }
                                Intent mediaIntent = new Intent(context, MediaActivity.class);
                                Bundle b = new Bundle();
                                b.putInt(Helper.ARG_MEDIA_POSITION, finalMediaPosition);
                                b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(statusToDeal.media_attachments));
                                mediaIntent.putExtras(b);
                                ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, statusToDeal.media_attachments.get(0).url);
                                // start the new activity
                                context.startActivity(mediaIntent, options.toBundle());
                            });
                            layoutMediaBinding.viewHide.setOnClickListener(v -> {
                                statusToDeal.sensitive = !statusToDeal.sensitive;
                                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                            });
                        } else {
                            loadAndAddAttachment(context, layoutMediaBinding, holder, adapter, mediaPosition, -1.f, -1.f, -1.f, statusToDeal, attachment);
                        }

                    }
                    mediaPosition++;
                }
                if (!fullAttachement || (statusToDeal.sensitive && !expand_media)) {
                    holder.binding.mediaContainer.setVisibility(View.GONE);
                    holder.binding.media.mediaContainer.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.mediaContainer.setVisibility(View.VISIBLE);
                    holder.binding.media.mediaContainer.setVisibility(View.GONE);
                }
            }
        } else {
            holder.binding.displayMedia.setVisibility(View.GONE);
            holder.binding.mediaContainer.setVisibility(View.GONE);
            holder.binding.media.mediaContainer.setVisibility(View.GONE);
        }
        holder.binding.statusContent.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        holder.binding.reblogInfo.setOnClickListener(v -> {
            if (statusToDeal.reblogs_count > 0) {
                Intent intent = new Intent(context, StatusInfoActivity.class);
                intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.BOOSTED_BY);
                intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                intent.putExtra(Helper.ARG_CHECK_REMOTELY, remote);
                context.startActivity(intent);
            }
        });

        holder.binding.favouriteInfo.setOnClickListener(v -> {
            if (statusToDeal.favourites_count > 0) {
                Intent intent = new Intent(context, StatusInfoActivity.class);
                intent.putExtra(Helper.ARG_TYPE_OF_INFO, StatusInfoActivity.typeOfInfo.LIKED_BY);
                intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                intent.putExtra(Helper.ARG_CHECK_REMOTELY, remote);
                context.startActivity(intent);
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
                    double value = Math.ceil((pollItem.votes_count * 100) / (double) statusToDeal.poll.voters_count);
                    pollItemBinding.pollItemPercent.setText(String.format("%s %%", (int) value));
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
                        img.setColorFilter(ThemeHelper.getAttColor(context, R.attr.colorPrimary), PorterDuff.Mode.SRC_IN);
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
                        if (poll != null) {
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
                        }
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
            holder.binding.statusUserInfo.setOnClickListener(v -> {
                holder.binding.statusContent.callOnClick();
            });
            holder.binding.statusContent.setOnClickListener(v -> {
                if (status.isFocused || v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                    if (v.getTag() == SpannableHelper.CLICKABLE_SPAN) {
                        v.setTag(null);
                    }
                    return;
                }
                if (context instanceof ContextActivity && !remote) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(Helper.ARG_STATUS, statusToDeal);
                    Fragment fragment = Helper.addFragment(((AppCompatActivity) context).getSupportFragmentManager(), R.id.nav_host_fragment_content_main, new FragmentMastodonContext(), bundle, null, FragmentMastodonContext.class.getName());
                    ((ContextActivity) context).setCurrentFragment((FragmentMastodonContext) fragment);
                } else {
                    if (remote) {
                        //Lemmy main post that should open Lemmy threads
                        if (adapter instanceof StatusAdapter && ((StatusAdapter) adapter).type == RemoteInstance.InstanceType.LEMMY && status.lemmy_post_id != null) {
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Helper.ARG_REMOTE_INSTANCE, ((StatusAdapter) adapter).pinnedTimeline);
                            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.REMOTE);
                            bundle.putString(Helper.ARG_LEMMY_POST_ID, status.lemmy_post_id);
                            bundle.putSerializable(Helper.ARG_STATUS, status);
                            Intent intent = new Intent(context, TimelineActivity.class);
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        } //Classic other cases for remote instances that will search the remote context
                        else if (!(context instanceof ContextActivity)) { //We are not already checking a remote conversation
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
                        }
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
            PopupMenu popup = new PopupMenu(context, holder.binding.actionButtonMore);
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
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                                    if (statusToDeal.spoiler_text != null && statusToDeal.spoiler_text.length() > 0) {
                                        statusToDeal.spoilerChecked = true;
                                    }
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
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                } else if (itemId == R.id.action_mute_home) {
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
                    builderInner.setTitle(R.string.mute_home);
                    builderInner.setMessage(statusToDeal.account.acct);
                    builderInner.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.action_mute, (dialog, which) -> accountsVM.muteHome(currentAccount, statusToDeal.account)
                            .observe((LifecycleOwner) context, account -> Toasty.info(context, context.getString(R.string.toast_mute), Toasty.LENGTH_LONG).show()));
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
                    AlertDialog.Builder builderInner = new MaterialAlertDialogBuilder(context);
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
                    translate(context, statusToDeal, holder, adapter);
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
                                List<app.fedilab.android.mastodon.client.entities.api.Account> accountList = new ArrayList<>();
                                for (BaseAccount account : accounts) {
                                    accountList.add(account.mastodon_account);
                                }
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                Runnable myRunnable = () -> {
                                    AlertDialog.Builder builderSingle = new MaterialAlertDialogBuilder(context);
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
                                        currentAccount = account;
                                        SharedPreferences.Editor editor = sharedpreferences.edit();
                                        editor.putString(PREF_USER_TOKEN, account.token);
                                        editor.putString(PREF_USER_SOFTWARE, account.software);
                                        editor.putString(PREF_USER_INSTANCE, account.instance);
                                        editor.putString(PREF_USER_ID, account.user_id);
                                        editor.commit();
                                        Intent mainActivity = new Intent(context, MainActivity.class);
                                        mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_WITH_ANOTHER_ACCOUNT);
                                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        mainActivity.putExtra(Helper.PREF_MESSAGE_URL, statusToDeal.url);
                                        context.startActivity(mainActivity);
                                        ((Activity) context).finish();
                                        dialog.dismiss();
                                    });
                                    builderSingle.show();
                                };
                                mainHandler.post(myRunnable);
                            } else if (accounts.size() == 1) {
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                Runnable myRunnable = () -> {
                                    BaseAccount account = accounts.get(0);
                                    Toasty.info(context, context.getString(R.string.toast_account_changed, "@" + account.mastodon_account.acct + "@" + account.instance), Toasty.LENGTH_LONG).show();
                                    BaseMainActivity.currentToken = account.token;
                                    BaseMainActivity.currentUserID = account.user_id;
                                    BaseMainActivity.currentInstance = account.instance;
                                    currentAccount = account;
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.putString(PREF_USER_TOKEN, account.token);
                                    editor.putString(PREF_USER_SOFTWARE, account.software);
                                    editor.putString(PREF_USER_INSTANCE, account.instance);
                                    editor.putString(PREF_USER_ID, account.user_id);
                                    editor.commit();
                                    Intent mainActivity = new Intent(context, MainActivity.class);
                                    mainActivity.putExtra(Helper.INTENT_ACTION, Helper.OPEN_WITH_ANOTHER_ACCOUNT);
                                    mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    mainActivity.putExtra(Helper.PREF_MESSAGE_URL, statusToDeal.url);
                                    context.startActivity(mainActivity);
                                    ((Activity) context).finish();
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
        holder.binding.actionButtonQuote.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComposeActivity.class);
            intent.putExtra(Helper.ARG_QUOTED_MESSAGE, statusToDeal);
            context.startActivity(intent);
        });
        holder.binding.actionButtonReply.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.uri, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results != null && results.statuses != null && results.statuses.size() > 0) {
                                Status fetchedStatus = results.statuses.get(0);
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
            if (!autofetch) {
                DrawerFetchMoreBinding drawerFetchMoreBinding = DrawerFetchMoreBinding.inflate(LayoutInflater.from(context));
                if (status.positionFetchMore == Status.PositionFetchMore.BOTTOM) {
                    holder.binding.fetchMoreContainerBottom.setVisibility(View.GONE);
                    holder.binding.fetchMoreContainerTop.setVisibility(View.VISIBLE);
                    holder.binding.fetchMoreContainerTop.removeAllViews();
                    holder.binding.fetchMoreContainerTop.addView(drawerFetchMoreBinding.getRoot());
                } else {
                    holder.binding.fetchMoreContainerBottom.setVisibility(View.VISIBLE);
                    holder.binding.fetchMoreContainerTop.setVisibility(View.GONE);
                    holder.binding.fetchMoreContainerBottom.removeAllViews();
                    holder.binding.fetchMoreContainerBottom.addView(drawerFetchMoreBinding.getRoot());
                }
                drawerFetchMoreBinding.fetchMoreMin.setOnClickListener(v -> {
                    status.isFetchMore = false;
                    status.isFetching = true;
                    int position = holder.getBindingAdapterPosition();
                    adapter.notifyItemChanged(position);
                    if (position < statusList.size() - 1) {
                        String fromId;
                        if (status.positionFetchMore == Status.PositionFetchMore.TOP) {
                            fromId = statusList.get(position + 1).id;
                        } else {
                            fromId = status.id;
                        }
                        fetchMoreCallBack.onClickMinId(fromId, status);
                    }
                });
                drawerFetchMoreBinding.fetchMoreMax.setOnClickListener(v -> {
                    //We hide the button
                    status.isFetchMore = false;
                    status.isFetching = true;
                    String fromId;
                    if (status.positionFetchMore == Status.PositionFetchMore.TOP || holder.getBindingAdapterPosition() == 0) {
                        fromId = statusList.get(holder.getBindingAdapterPosition()).id;
                    } else {
                        fromId = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                    }
                    fetchMoreCallBack.onClickMaxId(fromId, status);
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                });
            } else {
                holder.binding.fetchMoreContainerBottom.setVisibility(View.GONE);
                holder.binding.fetchMoreContainerTop.setVisibility(View.GONE);
                status.isFetchMore = false;
                status.isFetching = true;
                int position = holder.getBindingAdapterPosition();
                String statusIdMin = null, statusIdMax;
                if (position < statusList.size() - 1) {
                    if (status.positionFetchMore == Status.PositionFetchMore.TOP) {
                        statusIdMin = statusList.get(position + 1).id;
                    } else {
                        statusIdMin = status.id;
                    }
                }
                if (status.positionFetchMore == Status.PositionFetchMore.TOP || holder.getBindingAdapterPosition() == 0) {
                    statusIdMax = statusList.get(holder.getBindingAdapterPosition()).id;
                } else {
                    statusIdMax = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                }
                fetchMoreCallBack.autoFetch(statusIdMin, statusIdMax, status);
                recyclerView.post(() -> adapter.notifyItemChanged(holder.getBindingAdapterPosition()));
            }
        } else if (status.isFetching) {
            DrawerMessageFetchingBinding drawerMessageFetchingBinding = DrawerMessageFetchingBinding.inflate(LayoutInflater.from(context));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            drawerMessageFetchingBinding.fetchingContainer.setLayoutParams(lp);
            drawerMessageFetchingBinding.fetchingProgress.getIndeterminateDrawable().setColorFilter(ThemeHelper.getAttColor(context, R.attr.colorPrimary), PorterDuff.Mode.SRC_IN);
            if (status.positionFetchMore == Status.PositionFetchMore.BOTTOM) {
                holder.binding.fetchMoreContainerBottom.setVisibility(View.GONE);
                holder.binding.fetchMoreContainerTop.setVisibility(View.VISIBLE);
                holder.binding.fetchMoreContainerTop.removeAllViews();
                holder.binding.fetchMoreContainerTop.addView(drawerMessageFetchingBinding.getRoot());
            } else {
                holder.binding.fetchMoreContainerBottom.setVisibility(View.VISIBLE);
                holder.binding.fetchMoreContainerTop.setVisibility(View.GONE);
                holder.binding.fetchMoreContainerBottom.removeAllViews();
                holder.binding.fetchMoreContainerBottom.addView(drawerMessageFetchingBinding.getRoot());
            }
        } else {
            holder.binding.fetchMoreContainerBottom.setVisibility(View.GONE);
            holder.binding.fetchMoreContainerTop.setVisibility(View.GONE);
        }

    }


    private static void translate(Context context, Status statusToDeal,
                                  StatusViewHolder holder,
                                  RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        String statusToTranslate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            statusToTranslate = Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString();
        else
            statusToTranslate = Html.fromHtml(statusToDeal.content).toString();
        int countMorseChar = ComposeAdapter.countMorseChar(statusToTranslate);
        if (countMorseChar < 4) {
            TranslateHelper.translate(context, statusToDeal.content, translated -> {
                if (translated != null) {
                    statusToDeal.translationShown = true;
                    statusToDeal.translationContent = translated;
                    adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                } else {
                    Toasty.error(context, context.getString(R.string.toast_error_translate), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            statusToDeal.translationShown = true;
            statusToDeal.translationContent = ComposeAdapter.morseToText(statusToTranslate);
            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
        }
    }

    public static RequestBuilder<Drawable> prepareRequestBuilder(Context context, Attachment attachment,
                                                                 float mediaW, float mediaH,
                                                                 float focusX, float focusY, boolean isSensitive, boolean isArt) {

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean fullAttachement = sharedpreferences.getBoolean(context.getString(R.string.SET_FULL_PREVIEW), false);
        if (isArt) {
            fullAttachement = true;
        }
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);
        RequestBuilder<Drawable> requestBuilder;
        GlideRequests glideRequests = GlideApp.with(context);
        Bitmap placeholder = null;
        if (attachment.blurhash != null) {
            placeholder = new BlurHashDecoder().decode(attachment.blurhash, 32, 32, 1f);
        }
        if (!isSensitive || expand_media) {
            requestBuilder = glideRequests.asDrawable();
            if (!fullAttachement || isSensitive) {
                if (placeholder != null) {
                    requestBuilder = requestBuilder.placeholder(new BitmapDrawable(context.getResources(), placeholder));
                }
                requestBuilder = requestBuilder.apply(new RequestOptions().transform(new GlideFocus(focusX, focusY)));
                requestBuilder = requestBuilder.dontAnimate();
            } else {
                if (placeholder != null) {
                    requestBuilder = requestBuilder.placeholder(new BitmapDrawable(context.getResources(), placeholder));
                } else {
                    requestBuilder = requestBuilder.placeholder(R.color.transparent_grey);
                }
                requestBuilder = requestBuilder.dontAnimate();
                requestBuilder = requestBuilder.apply(new RequestOptions().override((int) mediaW, (int) mediaH));
                requestBuilder = requestBuilder.fitCenter();
            }
        } else {
            requestBuilder = glideRequests.asDrawable()
                    .dontAnimate()
                    .apply(new RequestOptions().transform(new BlurTransformation(50, 3)));
            //    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
        }
        return requestBuilder;
    }

    private static void loadAndAddAttachment(Context context, LayoutMediaBinding layoutMediaBinding,
                                             StatusViewHolder holder,
                                             RecyclerView.Adapter<RecyclerView.ViewHolder> adapter,
                                             int mediaPosition, float mediaW, float mediaH, float ratio,
                                             Status statusToDeal, Attachment attachment) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
        boolean fullAttachement = sharedpreferences.getBoolean(context.getString(R.string.SET_FULL_PREVIEW), false);
        boolean long_press_media = sharedpreferences.getBoolean(context.getString(R.string.SET_LONG_PRESS_STORE_MEDIA), false);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);

        LinearLayout.LayoutParams lp;

        if (fullAttachement && mediaH > 0 && (!statusToDeal.sensitive || expand_media)) {
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (mediaH * ratio));
            layoutMediaBinding.media.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutMediaBinding.media.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        layoutMediaBinding.media.setVisibility(View.VISIBLE);
        layoutMediaBinding.mediaVideo.setVisibility(View.GONE);
        layoutMediaBinding.mediaVideo.onPause();
        Player player = layoutMediaBinding.mediaVideo.getPlayer();
        if (player != null) {
            player.release();
        }
        layoutMediaBinding.media.setLayoutParams(lp);

        float focusX = 0.f;
        float focusY = 0.f;
        if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.getSmall() != null && statusToDeal.media_attachments.get(0).meta.focus != null) {
            focusX = statusToDeal.media_attachments.get(0).meta.focus.x;
            focusY = statusToDeal.media_attachments.get(0).meta.focus.y;
        }

        if (attachment.description != null && attachment.description.trim().length() > 0) {
            layoutMediaBinding.media.setContentDescription(attachment.description.trim());
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

        RequestBuilder<Drawable> requestBuilder = prepareRequestBuilder(context, attachment, mediaW * ratio, mediaH * ratio, focusX, focusY, statusToDeal.sensitive, false);
        if (!statusToDeal.sensitive || expand_media) {
            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
        } else {
            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
        }
        requestBuilder.load(attachment.preview_url).into(layoutMediaBinding.media);
        if (statusToDeal.sensitive) {
            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, ThemeHelper.getAttColor(context, R.attr.colorError));
        } else {
            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.white);
        }

        layoutMediaBinding.media.setOnClickListener(v -> {
            if (statusToDeal.sensitive && !expand_media) {
                statusToDeal.sensitive = false;
                int position = holder.getBindingAdapterPosition();
                adapter.notifyItemChanged(position);

                if (timeout > 0) {
                    new CountDownTimer((timeout * 1000L), 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            statusToDeal.sensitive = true;
                            adapter.notifyItemChanged(position);
                        }
                    }.start();
                }
                return;
            }
            Intent mediaIntent = new Intent(context, MediaActivity.class);
            Bundle b = new Bundle();
            b.putInt(Helper.ARG_MEDIA_POSITION, mediaPosition);
            b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(statusToDeal.media_attachments));
            mediaIntent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, statusToDeal.media_attachments.get(0).url);
            // start the new activity
            context.startActivity(mediaIntent, options.toBundle());
        });
        layoutMediaBinding.viewHide.setOnClickListener(v -> {
            statusToDeal.sensitive = !statusToDeal.sensitive;
            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
        });

        if ((!statusToDeal.sensitive || expand_media) && (fullAttachement)) {
            layoutMediaBinding.getRoot().setPadding(0, 0, 0, 10);
        } else {
            layoutMediaBinding.getRoot().setPadding(0, 0, 10, 0);
        }

    }

    private static void resizeContent(Context context, StatusViewHolder holder, Status statusToDeal, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int truncate_toots_size = sharedpreferences.getInt(context.getString(R.string.SET_TRUNCATE_TOOTS_SIZE), 0);
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
                holder.binding.statusContent.setMaxLines(truncate_toots_size);
            } else {
                holder.binding.statusContent.setMaxLines(9999);
            }
        } else {
            holder.binding.toggleTruncate.setVisibility(View.GONE);
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
            b.putString(type, id);
        }
        if (type == ARG_TIMELINE_REFRESH_ALL) {
            b.putBoolean(ARG_TIMELINE_REFRESH_ALL, true);
        }
        Intent intentBC = new Intent(Helper.RECEIVE_STATUS_ACTION);
        intentBC.putExtras(b);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBC);
    }


    public static void applyColor(Context context, StatusViewHolder holder) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean customLight = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_LIGHT_COLORS), false);
        boolean customDark = sharedpreferences.getBoolean(context.getString(R.string.SET_CUSTOMIZE_DARK_COLORS), false);
        int theme_icons_color = -1;
        int theme_statuses_color = -1;
        int theme_boost_header_color = -1;
        int theme_text_color = -1;
        int theme_text_header_1_line = -1;
        int theme_text_header_2_line = -1;
        int link_color = -1;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) { //LIGHT THEME
            if (customLight) {
                theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_ICON), -1);
                theme_statuses_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_BACKGROUND), -1);
                theme_boost_header_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_BOOST_HEADER), -1);
                theme_text_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_TEXT), -1);
                theme_text_header_1_line = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_DISPLAY_NAME), -1);
                theme_text_header_2_line = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_USERNAME), -1);
                link_color = sharedpreferences.getInt(context.getString(R.string.SET_LIGHT_LINK), -1);
            }
        } else {
            if (customDark) {
                theme_icons_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_ICON), -1);
                theme_statuses_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_BACKGROUND), -1);
                theme_boost_header_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_BOOST_HEADER), -1);
                theme_text_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_TEXT), -1);
                theme_text_header_1_line = sharedpreferences.getInt(context.getString(R.string.SET_DARK_DISPLAY_NAME), -1);
                theme_text_header_2_line = sharedpreferences.getInt(context.getString(R.string.SET_DARK_USERNAME), -1);
                link_color = sharedpreferences.getInt(context.getString(R.string.SET_DARK_LINK), -1);
            }
        }

        if (theme_icons_color != -1) {
            Helper.changeDrawableColor(context, holder.binding.actionButtonReply, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.statusAddCustomEmoji, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.actionButtonQuote, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.statusEmoji, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.actionButtonMore, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_round_star_24, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_round_repeat_24, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.visibility, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_round_star_border_24, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_person, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_bot, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_round_reply_24, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.actionButtonTranslate, theme_icons_color);
            holder.binding.actionButtonBoost.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonFavorite.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonBookmark.setInActiveImageTintColor(theme_icons_color);
            holder.binding.replyCount.setTextColor(theme_icons_color);
        }
        if (theme_statuses_color != -1) {
            holder.binding.cardviewContainer.setBackgroundColor(theme_statuses_color);
            holder.binding.translationLabel.setBackgroundColor(theme_statuses_color);
        }
        if (theme_boost_header_color != -1) {
            holder.binding.statusBoosterInfo.setBackgroundColor(theme_boost_header_color);
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
        if (theme_text_header_1_line != -1) {
            holder.binding.displayName.setTextColor(theme_text_header_1_line);
        }
        if (theme_text_header_2_line != -1) {
            holder.binding.username.setTextColor(theme_text_header_2_line);
        }
        if (link_color != -1) {
            holder.binding.cardUrl.setTextColor(link_color);
        }
    }

    @NonNull
    @Override
    public List<Attachment> getPreloadItems(int position) {
        List<Attachment> attachments = new ArrayList<>();
        int max_size = statusList.size();
        if (max_size == 0) {
            return attachments;
        }
        int siblings = 3;
        int from = Math.max((position - siblings), 0);
        if (from > max_size - 1) {
            from = max_size - 1;
        }
        int to = Math.min(position + siblings, max_size - 1);
        for (Status status : statusList.subList(from, to)) {
            if (status == null) {
                continue;
            }
            Status statusToDeal = status.reblog != null ? status.reblog : status;
            if (statusToDeal.media_attachments != null && statusToDeal.media_attachments.size() > 0) {
                attachments.addAll(statusToDeal.media_attachments);
            }
        }
        return attachments;
    }

   /* private static boolean mediaObfuscated(Status status) {
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
    }*/

    @Nullable
    @Override
    public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull Attachment attachment) {
        float focusX = 0.f;
        float focusY = 0.f;
        if (attachment.meta != null && attachment.meta.focus != null) {
            focusX = attachment.meta.focus.x;
            focusY = attachment.meta.focus.y;
        }
        int mediaH = 0;
        int mediaW = 0;
        if (attachment.meta != null && attachment.meta.getSmall() != null) {
            mediaH = attachment.meta.getSmall().height;
            mediaW = attachment.meta.getSmall().width;
        }
        return prepareRequestBuilder(context, attachment, mediaW, mediaH, focusX, focusY, attachment.sensitive, timelineType == Timeline.TimeLineEnum.ART).load(attachment);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        if (timelineType == Timeline.TimeLineEnum.ART) {
            return STATUS_ART;
        } else {
            if (statusList.get(position).filteredByApp != null) {
                if (statusList.get(position).filteredByApp.filter_action.equals("warn")) {
                    return STATUS_FILTERED;
                } else { //These messages should not be displayed unless they contain a fetch more button
                    if (!statusList.get(position).isFetchMore) {
                        return STATUS_HIDDEN;
                    } else {
                        return STATUS_FILTERED_HIDE;
                    }
                }
            } else {
                if (isVisible(timelineType, statusList.get(position))) {
                    if (visiblePixelfed && isVisiblePixelfed(statusList.get(position)) && timelineType != Timeline.TimeLineEnum.UNKNOWN) {
                        return STATUS_PIXELFED;
                    } else {
                        return STATUS_VISIBLE;
                    }
                } else {
                    return STATUS_HIDDEN;
                }
            }

        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        visiblePixelfed = sharedpreferences.getBoolean(context.getString(R.string.SET_PIXELFED_PRESENTATION) + MainActivity.currentUserID + MainActivity.currentInstance, false);
        if (viewType == STATUS_HIDDEN) { //Hidden statuses - ie: filtered
            DrawerStatusHiddenBinding itemBinding = DrawerStatusHiddenBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_ART) { //Art statuses
            DrawerStatusArtBinding itemBinding = DrawerStatusArtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_PIXELFED) { //Art statuses
            DrawerStatusPixelfedBinding itemBinding = DrawerStatusPixelfedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_FILTERED) { //Filtered warn
            DrawerStatusFilteredBinding itemBinding = DrawerStatusFilteredBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StatusViewHolder(itemBinding);
        } else if (viewType == STATUS_FILTERED_HIDE) { //Filtered hide
            DrawerStatusFilteredHideBinding itemBinding = DrawerStatusFilteredHideBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        context = viewHolder.itemView.getContext();
        Status status = statusList.get(position);
        if (viewHolder.getItemViewType() == STATUS_VISIBLE) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
                holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
                holder.binding.dividerCard.setVisibility(View.GONE);
            }
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            SearchVM searchVM = new ViewModelProvider((ViewModelStoreOwner) context).get(SearchVM.class);
            statusManagement(context, statusesVM, searchVM, holder, mRecyclerView, this, statusList, status, timelineType, minified, canBeFederated, checkRemotely, fetchMoreCallBack);
            applyColor(context, holder);
        } else if (viewHolder.getItemViewType() == STATUS_FILTERED_HIDE) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
                holder.bindingFilteredHide.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
                holder.bindingFilteredHide.dividerCard.setVisibility(View.GONE);
            }
            if (status.isFetchMore && fetchMoreCallBack != null) {
                boolean autofetch = sharedpreferences.getBoolean(context.getString(R.string.SET_AUTO_FETCH_MISSING_MESSAGES), false);
                if (!autofetch) {
                    holder.bindingFilteredHide.layoutFetchMore.fetchMoreContainer.setVisibility(View.VISIBLE);
                    holder.bindingFilteredHide.layoutFetchMore.fetchMoreMin.setOnClickListener(v -> {
                        status.isFetchMore = false;
                        status.isFetching = true;
                        notifyItemChanged(holder.getBindingAdapterPosition());
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
                    holder.bindingFilteredHide.layoutFetchMore.fetchMoreMax.setOnClickListener(v -> {
                        //We hide the button
                        status.isFetchMore = false;
                        status.isFetching = true;
                        notifyItemChanged(holder.getBindingAdapterPosition());
                        String fromId;
                        if (status.positionFetchMore == Status.PositionFetchMore.TOP || holder.getBindingAdapterPosition() == 0) {
                            fromId = statusList.get(holder.getBindingAdapterPosition()).id;
                        } else {
                            fromId = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                        }
                        fetchMoreCallBack.onClickMaxId(fromId, status);

                    });
                } else {
                    status.isFetchMore = false;
                    status.isFetching = true;
                    String minId = null, maxId;
                    if (holder.getBindingAdapterPosition() < statusList.size() - 1) {
                        if (status.positionFetchMore == Status.PositionFetchMore.TOP) {
                            minId = statusList.get(holder.getBindingAdapterPosition() + 1).id;
                        } else {
                            minId = status.id;
                        }
                    }
                    if (status.positionFetchMore == Status.PositionFetchMore.TOP || holder.getBindingAdapterPosition() == 0) {
                        maxId = statusList.get(holder.getBindingAdapterPosition()).id;
                    } else {
                        maxId = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                    }
                    fetchMoreCallBack.autoFetch(minId, maxId, status);
                }

            } else {
                holder.bindingFilteredHide.layoutFetchMore.fetchMoreContainer.setVisibility(View.GONE);
            }
        } else if (viewHolder.getItemViewType() == STATUS_FILTERED) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            holder.bindingFiltered.filteredText.setText(context.getString(R.string.filtered_by, status.filteredByApp.title));
            holder.bindingFiltered.displayButton.setOnClickListener(v -> {
                status.filteredByApp = null;
                notifyItemChanged(position);
            });

            if (status.isFetchMore && fetchMoreCallBack != null) {
                holder.bindingFiltered.layoutFetchMore.fetchMoreContainer.setVisibility(View.VISIBLE);
                holder.bindingFiltered.layoutFetchMore.fetchMoreMin.setOnClickListener(v -> {
                    status.isFetchMore = false;
                    status.isFetching = true;
                    notifyItemChanged(holder.getBindingAdapterPosition());
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
                holder.bindingFiltered.layoutFetchMore.fetchMoreMax.setOnClickListener(v -> {
                    //We hide the button
                    status.isFetchMore = false;
                    status.isFetching = true;
                    String fromId;
                    if (status.positionFetchMore == Status.PositionFetchMore.TOP || holder.getBindingAdapterPosition() == 0) {
                        fromId = statusList.get(holder.getBindingAdapterPosition()).id;
                    } else {
                        fromId = statusList.get(holder.getBindingAdapterPosition() - 1).id;
                    }
                    fetchMoreCallBack.onClickMaxId(fromId, status);
                    notifyItemChanged(holder.getBindingAdapterPosition());
                });
            } else {
                holder.bindingFiltered.layoutFetchMore.fetchMoreContainer.setVisibility(View.GONE);
            }
        } else if (viewHolder.getItemViewType() == STATUS_ART) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            MastodonHelper.loadPPMastodon(holder.bindingArt.artPp, status.account);
            if (measuredWidthArt <= 0) {
                holder.bindingArt.artMedia.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        holder.bindingArt.artMedia.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        measuredWidthArt = holder.bindingArt.artMedia.getWidth();
                        notifyItemChanged(0, statusList.size());
                    }
                });
            }
            if (status.art_attachment != null) {
                if (status.art_attachment.meta != null && status.art_attachment.meta.getSmall() != null) {
                    ConstraintLayout.LayoutParams lp;
                    float mediaH = status.art_attachment.meta.getSmall().height;
                    float mediaW = status.art_attachment.meta.getSmall().width;
                    float ratio = measuredWidthArt > 0 ? measuredWidthArt / mediaW : 1.0f;
                    lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (int) (mediaH * ratio));
                    holder.bindingArt.artMedia.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    holder.bindingArt.artMedia.setLayoutParams(lp);
                    RequestBuilder<Drawable> requestBuilder = prepareRequestBuilder(context, status.art_attachment, mediaW * ratio, mediaH * ratio, 1.0f, 1.0f, status.sensitive, true);
                    requestBuilder.load(status.art_attachment.preview_url).into(holder.bindingArt.artMedia);
                }

            }
            holder.bindingArt.artUsername.setText(
                    status.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.bindingArt.artUsername)),
                    TextView.BufferType.SPANNABLE);
            holder.bindingArt.artAcct.setText(String.format(Locale.getDefault(), "@%s", status.account.acct));
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
                if (status.art_attachment != null) {
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
                } else {
                    Toasty.error(context, context.getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                }
            });
            holder.bindingArt.bottomBanner.setOnClickListener(v -> {
                Intent intent = new Intent(context, ContextActivity.class);
                intent.putExtra(Helper.ARG_STATUS, status);
                context.startActivity(intent);
            });
        } else if (viewHolder.getItemViewType() == STATUS_PIXELFED) {
            Status statusToDeal = status.reblog != null ? status.reblog : status;
            StatusViewHolder holder = (StatusViewHolder) viewHolder;

            if (status.reblog != null) {
                MastodonHelper.loadPPMastodon(holder.bindingPixelfed.artReblogPp, status.account);
                holder.bindingPixelfed.artReblogPp.setVisibility(View.VISIBLE);
            } else {
                holder.bindingPixelfed.artReblogPp.setVisibility(View.GONE);
            }

            MastodonHelper.loadPPMastodon(holder.bindingPixelfed.artPp, statusToDeal.account);
            SliderAdapter adapter = new SliderAdapter(statusToDeal);
            holder.bindingPixelfed.artMedia.setSliderAdapter(adapter);
            holder.bindingPixelfed.artMedia.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
            holder.bindingPixelfed.artMedia.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_BACK_AND_FORTH);
            holder.bindingPixelfed.artMedia.setScrollTimeInSec(4);
            holder.bindingPixelfed.artMedia.startAutoCycle();
            holder.bindingPixelfed.commentNumber.setText(String.valueOf(statusToDeal.replies_count));
            holder.bindingPixelfed.artUsername.setText(
                    statusToDeal.account.getSpanDisplayName(context,
                            new WeakReference<>(holder.bindingPixelfed.artUsername)),
                    TextView.BufferType.SPANNABLE);
            holder.bindingPixelfed.artAcct.setText(String.format(Locale.getDefault(), "@%s", statusToDeal.account.acct));
            holder.bindingPixelfed.artPp.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle b = new Bundle();
                b.putSerializable(Helper.ARG_ACCOUNT, statusToDeal.account);
                intent.putExtras(b);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context, holder.bindingPixelfed.artPp, context.getString(R.string.activity_porfile_pp));
                context.startActivity(intent, options.toBundle());
            });
            holder.bindingPixelfed.bottomBanner.setOnClickListener(v -> {
                Intent intent = new Intent(context, ContextActivity.class);
                intent.putExtra(Helper.ARG_STATUS, statusToDeal);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder viewHolder) {
        super.onViewRecycled(viewHolder);
        if (viewHolder instanceof StatusViewHolder) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            if (holder.binding != null) {
                PlayerView doubleTapPlayerView = holder.binding.media.getRoot().findViewById(R.id.media_video);
                if (doubleTapPlayerView != null && doubleTapPlayerView.getPlayer() != null) {
                    doubleTapPlayerView.getPlayer().release();
                }
            }
        }
    }


    public interface FetchMoreCallBack {
        void onClickMinId(String min_id, Status fetchStatus);

        void onClickMaxId(String max_id, Status fetchStatus);

        void autoFetch(String min_id, String max_id, Status status);
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusBinding binding;
        DrawerStatusHiddenBinding bindingHidden;
        DrawerStatusReportBinding bindingReport;
        DrawerStatusNotificationBinding bindingNotification;
        DrawerStatusArtBinding bindingArt;
        DrawerStatusPixelfedBinding bindingPixelfed;
        DrawerStatusFilteredBinding bindingFiltered;
        DrawerStatusFilteredHideBinding bindingFilteredHide;

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

        StatusViewHolder(DrawerStatusPixelfedBinding itemView) {
            super(itemView.getRoot());
            bindingPixelfed = itemView;
        }

        StatusViewHolder(DrawerStatusFilteredBinding itemView) {
            super(itemView.getRoot());
            bindingFiltered = itemView;
        }

        StatusViewHolder(DrawerStatusFilteredHideBinding itemView) {
            super(itemView.getRoot());
            bindingFilteredHide = itemView;
        }
    }


}
