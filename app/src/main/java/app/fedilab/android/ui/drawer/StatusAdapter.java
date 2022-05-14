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


import static app.fedilab.android.BaseMainActivity.regex_home;
import static app.fedilab.android.BaseMainActivity.regex_local;
import static app.fedilab.android.BaseMainActivity.regex_public;
import static app.fedilab.android.BaseMainActivity.show_boosts;
import static app.fedilab.android.BaseMainActivity.show_replies;
import static app.fedilab.android.activities.ContextActivity.expand;

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
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.github.stom79.mytransl.MyTransL;
import com.github.stom79.mytransl.client.HttpsConnectionException;
import com.github.stom79.mytransl.client.Results;
import com.github.stom79.mytransl.translate.Params;
import com.github.stom79.mytransl.translate.Translate;
import com.varunest.sparkbutton.SparkButton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.ComposeActivity;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.activities.CustomSharingActivity;
import app.fedilab.android.activities.MediaActivity;
import app.fedilab.android.activities.ProfileActivity;
import app.fedilab.android.activities.ReportActivity;
import app.fedilab.android.activities.StatusInfoActivity;
import app.fedilab.android.client.entities.StatusDraft;
import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Poll;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.DrawerStatusArtBinding;
import app.fedilab.android.databinding.DrawerStatusBinding;
import app.fedilab.android.databinding.DrawerStatusHiddenBinding;
import app.fedilab.android.databinding.DrawerStatusNotificationBinding;
import app.fedilab.android.databinding.DrawerStatusReportBinding;
import app.fedilab.android.databinding.LayoutMediaBinding;
import app.fedilab.android.databinding.LayoutPollItemBinding;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonContext;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.SearchVM;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;
import jp.wasabeef.glide.transformations.BlurTransformation;


public class StatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Status> statusList;
    private final boolean minified;
    private Context context;
    private final Timeline.TimeLineEnum timelineType;

    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_VISIBLE = 1;
    public static final int STATUS_ART = 2;

    public StatusAdapter(List<Status> statuses, Timeline.TimeLineEnum timelineType, boolean minified) {
        this.statusList = statuses;
        this.timelineType = timelineType;
        this.minified = minified;
    }


    private static boolean isVisble(Timeline.TimeLineEnum timelineType, Status status) {
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

    /**
     * Manage status, this method is also reused in notifications timelines
     *
     * @param context          Context
     * @param statusesVM       StatusesVM - For handling actions in background to the correct activity
     * @param searchVM         SearchVM - For handling remote actions
     * @param holder           StatusViewHolder
     * @param adapter          RecyclerView.Adapter<RecyclerView.ViewHolder> - General adapter that can be for {@link StatusAdapter} or {@link NotificationAdapter}
     * @param statusList       List<Status>
     * @param notificationList List<Notification>
     * @param timelineType     Timeline.TimeLineEnum timelineTypeTimeline.TimeLineEnum
     * @param status           {@link Status}
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void statusManagement(Context context,
                                        StatusesVM statusesVM,
                                        SearchVM searchVM,
                                        StatusViewHolder holder,
                                        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter,
                                        List<Status> statusList,
                                        List<Notification> notificationList,
                                        Status status,
                                        Timeline.TimeLineEnum timelineType,
                                        boolean minified) {
        if (status == null) {
            return;
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean remote = timelineType == Timeline.TimeLineEnum.REMOTE;

        Status statusToDeal = status.reblog != null ? status.reblog : status;


        boolean expand_cw = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_CW), false);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);
        boolean display_card = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_CARD), false);
        boolean share_details = sharedpreferences.getBoolean(context.getString(R.string.SET_SHARE_DETAILS), true);
        boolean confirmFav = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION_FAV), false);
        boolean confirmBoost = sharedpreferences.getBoolean(context.getString(R.string.SET_NOTIF_VALIDATION), true);
        boolean fullAttachement = sharedpreferences.getBoolean(context.getString(R.string.SET_FULL_PREVIEW), false);
        boolean displayBookmark = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_BOOKMARK), false);

        int truncate_toots_size = sharedpreferences.getInt(context.getString(R.string.SET_TRUNCATE_TOOTS_SIZE), 0);
        boolean display_video_preview = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_VIDEO_PREVIEWS), true);
        boolean isModerator = sharedpreferences.getBoolean(Helper.PREF_IS_MODERATOR, false);
        boolean isAdmin = sharedpreferences.getBoolean(Helper.PREF_IS_ADMINISTRATOR, false);
        int theme_icons_color = -1;
        int theme_statuses_color = -1;
        int theme_boost_header_color = -1;
        int theme_text_color = -1;
        int theme_text_header_1_line = -1;
        int theme_text_header_2_line = -1;
        if (sharedpreferences.getBoolean("use_custom_theme", false)) {
            //Getting custom colors
            theme_icons_color = sharedpreferences.getInt("theme_icons_color", -1);
            theme_statuses_color = sharedpreferences.getInt("theme_statuses_color", -1);
            theme_boost_header_color = sharedpreferences.getInt("theme_boost_header_color", -1);
            theme_text_color = sharedpreferences.getInt("theme_text_color", -1);
            theme_text_header_1_line = sharedpreferences.getInt("theme_text_header_1_line", -1);
            theme_text_header_2_line = sharedpreferences.getInt("theme_text_header_2_line", -1);

        }

        if (theme_icons_color != -1) {
            Helper.changeDrawableColor(context, holder.binding.actionButtonReply, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.actionButtonMore, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_baseline_star_24, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_repeat, theme_icons_color);
            Helper.changeDrawableColor(context, holder.binding.visibility, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_star_outline, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_person, theme_icons_color);
            holder.binding.actionButtonFavorite.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonBookmark.setInActiveImageTintColor(theme_icons_color);
            holder.binding.actionButtonBoost.setInActiveImageTintColor(theme_icons_color);
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
        }
        if (theme_boost_header_color != -1 && status.reblog != null) {
            holder.binding.headerContainer.setBackgroundColor(theme_boost_header_color);
        } else {
            holder.binding.headerContainer.setBackgroundColor(0);
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
        }


        if (truncate_toots_size > 0) {
            holder.binding.statusContent.setMaxLines(truncate_toots_size);
            holder.binding.statusContent.setEllipsize(TextUtils.TruncateAt.END);
            Layout layout = holder.binding.statusContent.getLayout();
            if (layout != null) {
                int lines = layout.getLineCount();
                if (lines > truncate_toots_size) {
                    int ellipsisCount = layout.getEllipsisCount(lines - 1);
                    if (ellipsisCount > truncate_toots_size) {
                        holder.binding.toggleTruncate.setVisibility(View.VISIBLE);
                        holder.binding.toggleTruncate.setOnClickListener(v -> {
                            statusToDeal.isTruncated = !statusToDeal.isTruncated;
                            adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                        });
                        if (statusToDeal.isTruncated) {
                            holder.binding.statusContent.setMaxLines(5);
                        } else {
                            holder.binding.statusContent.setMaxLines(9999);
                        }
                    }
                }
            }
        }
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
        if (status.card != null && (display_card || status.isFocused) & status.card.description.trim().length() > 0) {
            if (status.card.width > status.card.height) {
                holder.binding.cardImageHorizontal.setVisibility(View.VISIBLE);
                holder.binding.cardImageVertical.setVisibility(View.GONE);
                Glide.with(context).load(status.card.image).into(holder.binding.cardImageHorizontal);
            } else {
                holder.binding.cardImageHorizontal.setVisibility(View.GONE);
                holder.binding.cardImageVertical.setVisibility(View.VISIBLE);
                Glide.with(context).load(status.card.image).into(holder.binding.cardImageVertical);
            }
            holder.binding.cardTitle.setText(status.card.title);
            holder.binding.cardDescription.setText(status.card.description);
            holder.binding.cardUrl.setText(Helper.transformURL(context, status.card.url));
            holder.binding.cardviewContainer.setOnClickListener(v -> Helper.openBrowser(context, Helper.transformURL(context, status.card.url)));
            holder.binding.card.setVisibility(View.VISIBLE);
        } else {
            holder.binding.card.setVisibility(View.GONE);
        }
        if (minified) {
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
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results.statuses != null && results.statuses.size() > 0) {
                                    Status fetchedStatus = statusList.get(0);
                                    statusesVM.bookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                            .observe((LifecycleOwner) context, _status -> {
                                                statusToDeal.bookmarked = _status.bookmarked;
                                                sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                                adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                            });
                                } else {
                                    Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    if (statusToDeal.bookmarked) {
                        statusesVM.unBookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                .observe((LifecycleOwner) context, _status -> {
                                    statusToDeal.bookmarked = _status.bookmarked;
                                    sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                });
                    } else {
                        ((SparkButton) v).playAnimation();
                        statusesVM.bookmark(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                .observe((LifecycleOwner) context, _status -> {
                                    statusToDeal.bookmarked = _status.bookmarked;
                                    sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                });
                    }
                }
            });
            holder.binding.actionButtonFavorite.setChecked(statusToDeal.favourited);
            holder.binding.statusUserInfo.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results.statuses != null && results.statuses.size() > 0) {
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
            holder.binding.statusBoosterAvatar.setOnClickListener(v -> {
                if (remote) {
                    Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                    searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                            .observe((LifecycleOwner) context, results -> {
                                if (results.statuses != null && results.statuses.size() > 0) {
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
                            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                    .observe((LifecycleOwner) context, results -> {
                                        if (results.statuses != null && results.statuses.size() > 0) {
                                            Status fetchedStatus = results.statuses.get(0);
                                            statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id, null)
                                                    .observe((LifecycleOwner) context, _status -> {
                                                        statusToDeal.reblogged = _status.reblogged;
                                                        statusToDeal.reblogs_count = _status.reblogs_count;
                                                        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                                    });
                                        } else {
                                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            if (statusToDeal.reblogged) {
                                statusesVM.unReblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> {
                                            statusToDeal.reblogged = _status.reblogged;
                                            statusToDeal.reblogs_count = _status.reblogs_count;
                                            sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                            adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                        });
                            } else {
                                ((SparkButton) v).playAnimation();
                                statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, null)
                                        .observe((LifecycleOwner) context, _status -> {
                                            statusToDeal.reblogged = _status.reblogged;
                                            statusToDeal.reblogs_count = _status.reblogs_count;
                                            sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                            adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                        });
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
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id, null)
                                                .observe((LifecycleOwner) context, _status -> {
                                                    statusToDeal.reblogged = _status.reblogged;
                                                    statusToDeal.reblogs_count = _status.reblogs_count;
                                                    sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                                });
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        if (statusToDeal.reblogged) {
                            statusesVM.unReblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> {
                                        statusToDeal.reblogged = _status.reblogged;
                                        statusToDeal.reblogs_count = _status.reblogs_count;
                                        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                    });
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.reblog(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id, null)
                                    .observe((LifecycleOwner) context, _status -> {
                                        statusToDeal.reblogged = _status.reblogged;
                                        statusToDeal.reblogs_count = _status.reblogs_count;
                                        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                    });
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
                            searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                    .observe((LifecycleOwner) context, results -> {
                                        if (results.statuses != null && results.statuses.size() > 0) {
                                            Status fetchedStatus = results.statuses.get(0);
                                            statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                                    .observe((LifecycleOwner) context, _status -> {
                                                        statusToDeal.favourited = _status.favourited;
                                                        statusToDeal.favourites_count = _status.favourites_count;
                                                        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                                    });
                                        } else {
                                            Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            if (status.favourited) {
                                statusesVM.unFavourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> {
                                            statusToDeal.favourited = _status.favourited;
                                            statusToDeal.favourites_count = _status.favourites_count;
                                            adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                        });
                            } else {
                                ((SparkButton) v).playAnimation();
                                statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                        .observe((LifecycleOwner) context, _status -> {
                                            statusToDeal.favourited = _status.favourited;
                                            statusToDeal.favourites_count = _status.favourites_count;
                                            sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                            adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                        });
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
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.id)
                                                .observe((LifecycleOwner) context, _status -> {
                                                    statusToDeal.favourited = _status.favourited;
                                                    statusToDeal.favourites_count = _status.favourites_count;
                                                    sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                                });
                                    } else {
                                        Toasty.info(context, context.getString(R.string.toast_error_search), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        if (status.favourited) {
                            statusesVM.unFavourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> {
                                        statusToDeal.favourited = _status.favourited;
                                        statusToDeal.favourites_count = _status.favourites_count;
                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                    });
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.favourite(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.id)
                                    .observe((LifecycleOwner) context, _status -> {
                                        statusToDeal.favourited = _status.favourited;
                                        statusToDeal.favourites_count = _status.favourites_count;
                                        sendAction(context, Helper.ARG_STATUS_ACTION, statusToDeal, null);
                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                    });
                        }
                    }
                }
            });
        }


        //--- ACCOUNT INFO ---
        MastodonHelper.loadPPMastodon(holder.binding.avatar, statusToDeal.account);
        holder.binding.displayName.setText(statusToDeal.account.span_display_name, TextView.BufferType.SPANNABLE);
        if (theme_text_header_1_line != -1) {
            holder.binding.displayName.setTextColor(theme_text_header_1_line);
        }
        holder.binding.username.setText(String.format("@%s", statusToDeal.account.acct));
        if (theme_text_header_2_line != -1) {
            holder.binding.username.setTextColor(theme_text_header_2_line);
        }
        if (status.isFocused) {
            holder.binding.statusInfo.setVisibility(View.VISIBLE);
            holder.binding.reblogsCount.setText(String.valueOf(status.reblogs_count));
            holder.binding.favoritesCount.setText(String.valueOf(status.favourites_count));
            holder.binding.time.setText(Helper.longDateToString(status.created_at));
            holder.binding.time.setVisibility(View.VISIBLE);
            holder.binding.dateShort.setVisibility(View.GONE);
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
            holder.binding.visibility.setImageResource(ressource);
            holder.binding.dateShort.setVisibility(View.GONE);
        } else {
            holder.binding.statusInfo.setVisibility(View.GONE);
            holder.binding.dateShort.setVisibility(View.VISIBLE);
            holder.binding.dateShort.setText(Helper.dateDiff(context, status.created_at));
            holder.binding.time.setVisibility(View.GONE);
        }

        //---- SPOILER TEXT -----

        if (statusToDeal.spoiler_text != null && !statusToDeal.spoiler_text.trim().isEmpty()) {
            if (expand_cw || expand) {
                holder.binding.spoilerExpand.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setText(statusToDeal.span_spoiler_text, TextView.BufferType.SPANNABLE);
                statusToDeal.isExpended = true;
                statusToDeal.isMediaDisplayed = true;
            } else {
                holder.binding.spoilerExpand.setOnClickListener(v -> {
                    statusToDeal.isExpended = !statusToDeal.isExpended;
                    statusToDeal.isMediaDisplayed = !statusToDeal.isMediaDisplayed;
                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                });
                holder.binding.spoilerExpand.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setVisibility(View.VISIBLE);
                holder.binding.spoiler.setText(statusToDeal.span_spoiler_text, TextView.BufferType.SPANNABLE);
            }

        } else {
            holder.binding.spoiler.setVisibility(View.GONE);
            holder.binding.spoilerExpand.setVisibility(View.GONE);
            holder.binding.spoiler.setText(null);
        }

        //--- BOOSTER INFO ---
        if (status.reblog != null) {
            MastodonHelper.loadPPMastodon(holder.binding.statusBoosterAvatar, status.account);
            holder.binding.statusBoosterInfo.setVisibility(View.VISIBLE);
        } else {
            holder.binding.statusBoosterInfo.setVisibility(View.GONE);
        }
        //--- BOOST VISIBILITY ---
        switch (status.visibility) {
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
        holder.binding.statusContent.setText(statusToDeal.span_content, TextView.BufferType.SPANNABLE);

        if (statusToDeal.translationContent != null) {
            holder.binding.containerTrans.setVisibility(View.VISIBLE);
            holder.binding.statusContentTranslated.setText(statusToDeal.span_translate, TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.containerTrans.setVisibility(View.GONE);
        }
        if (status.spoiler_text == null || status.spoiler_text.trim().isEmpty() || statusToDeal.isExpended) {
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
                LayoutMediaBinding layoutMediaBinding = LayoutMediaBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                RelativeLayout.LayoutParams lp;
                if (fullAttachement) {
                    lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
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
                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                        final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
                        if (timeout > 0) {
                            new CountDownTimer((timeout * 1000L), 1000) {
                                public void onTick(long millisUntilFinished) {
                                }

                                public void onFinish() {
                                    status.isMediaObfuscated = true;
                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
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
                if (!mediaObfuscated(statusToDeal) || expand_media) {
                    layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
                    Glide.with(layoutMediaBinding.media.getContext())
                            .load(statusToDeal.media_attachments.get(0).preview_url)
                            .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                            .into(layoutMediaBinding.media);
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
                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                });
                holder.binding.mediaContainer.addView(layoutMediaBinding.getRoot());
                holder.binding.mediaContainer.setVisibility(View.VISIBLE);
                holder.binding.attachmentsListContainer.setVisibility(View.GONE);
            } else { //Several media
                int mediaPosition = 1;
                for (Attachment attachment : statusToDeal.media_attachments) {
                    LayoutMediaBinding layoutMediaBinding = LayoutMediaBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                    RelativeLayout.LayoutParams lp;
                    if (fullAttachement) {
                        lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutMediaBinding.media.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    } else {
                        lp = new RelativeLayout.LayoutParams((int) Helper.convertDpToPixel(200, context), (int) Helper.convertDpToPixel(200, context));
                        layoutMediaBinding.media.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                    lp.setMargins(0, 0, (int) Helper.convertDpToPixel(5, context), 0);
                    if (!mediaObfuscated(statusToDeal) || expand_media) {
                        layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
                        Glide.with(layoutMediaBinding.media.getContext())
                                .load(attachment.preview_url)
                                .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                                .into(layoutMediaBinding.media);
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
                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                    });
                    holder.binding.attachmentsList.addView(layoutMediaBinding.getRoot());
                    mediaPosition++;
                }
                holder.binding.mediaContainer.setVisibility(View.GONE);
                holder.binding.attachmentsListContainer.setVisibility(View.VISIBLE);
            }
        } else {
            holder.binding.mediaContainer.setVisibility(View.GONE);
            holder.binding.attachmentsListContainer.setVisibility(View.GONE);
        }
        holder.binding.statusContent.setMovementMethod(LinkMovementMethod.getInstance());

        holder.binding.reblogInfo.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results.statuses != null && results.statuses.size() > 0) {
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
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results.statuses != null && results.statuses.size() > 0) {
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
                for (Poll.PollItem pollItem : statusToDeal.poll.options) {
                    @NonNull LayoutPollItemBinding pollItemBinding = LayoutPollItemBinding.inflate(inflater, holder.binding.poll.rated, true);
                    double value = ((double) (pollItem.votes_count * 100) / (double) statusToDeal.poll.voters_count);
                    pollItemBinding.pollItemPercent.setText(String.format("%s %%", (int) value));
                    if (theme_text_color != -1) {
                        pollItemBinding.pollItemPercent.setTextColor(theme_text_color);
                        pollItemBinding.pollItemText.setTextColor(theme_text_color);
                    }
                    pollItemBinding.pollItemText.setText(pollItem.span_title, TextView.BufferType.SPANNABLE);
                    pollItemBinding.pollItemValue.setProgress((int) value);
                    if (pollItem.votes_count == greaterValue) {
                        pollItemBinding.pollItemPercent.setTypeface(null, Typeface.BOLD);
                        pollItemBinding.pollItemText.setTypeface(null, Typeface.BOLD);
                    }
                    if (ownvotes != null && ownvotes.contains(j)) {
                        Drawable img = ContextCompat.getDrawable(context, R.drawable.ic_baseline_check_24);
                        assert img != null;
                        final float scale = context.getResources().getDisplayMetrics().density;
                        img.setColorFilter(ContextCompat.getColor(context, R.color.cyanea_accent_reference), PorterDuff.Mode.SRC_IN);
                        img.setBounds(0, 0, (int) (20 * scale + 0.5f), (int) (20 * scale + 0.5f));
                        pollItemBinding.pollItemText.setCompoundDrawables(null, null, img, null);
                    }
                    j++;
                }
            } else {
                holder.binding.poll.rated.setVisibility(View.GONE);
                holder.binding.poll.submitVote.setVisibility(View.VISIBLE);
                if (statusToDeal.poll.multiple) {
                    if ((holder.binding.poll.multipleChoice).getChildCount() > 0)
                        (holder.binding.poll.multipleChoice).removeAllViews();
                    for (Poll.PollItem pollOption : statusToDeal.poll.options) {
                        CheckBox cb = new CheckBox(context);
                        cb.setText(pollOption.span_title, TextView.BufferType.SPANNABLE);
                        holder.binding.poll.multipleChoice.addView(cb);
                    }
                    holder.binding.poll.multipleChoice.setVisibility(View.VISIBLE);
                    holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.GONE);
                } else {
                    if ((holder.binding.poll.singleChoiceRadioGroup).getChildCount() > 0)
                        (holder.binding.poll.singleChoiceRadioGroup).removeAllViews();
                    for (Poll.PollItem pollOption : statusToDeal.poll.options) {
                        RadioButton rb = new RadioButton(context);
                        rb.setText(pollOption.span_title, TextView.BufferType.SPANNABLE);
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
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results.statuses != null && results.statuses.size() > 0) {
                                        Status fetchedStatus = results.statuses.get(0);
                                        statusesVM.votePoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, fetchedStatus.poll.id, choice)
                                                .observe((LifecycleOwner) context, poll -> {
                                                    int i = 0;
                                                    for (Poll.PollItem item : statusToDeal.poll.options) {
                                                        poll.options.get(i).span_title = item.span_title;
                                                        i++;
                                                    }
                                                    statusToDeal.poll = poll;
                                                    adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
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
                                            poll.options.get(i).span_title = item.span_title;
                                            i++;
                                        }
                                        statusToDeal.poll = poll;
                                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
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
                            poll.options.get(i).span_title = item.span_title;
                            i++;
                        }
                        statusToDeal.poll = poll;
                        adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
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
        if (!minified) {
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
                        searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                                .observe((LifecycleOwner) context, results -> {
                                    if (results.statuses != null && results.statuses.size() > 0) {
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

            final String[] stringArrayConf;
            if (statusToDeal.visibility.equals("direct") || (statusToDeal.visibility.equals("private") && !isOwner))
                popup.getMenu().findItem(R.id.action_schedule_boost).setVisible(false);
            if (isOwner) {
                popup.getMenu().findItem(R.id.action_block).setVisible(false);
                popup.getMenu().findItem(R.id.action_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_report).setVisible(false);
                popup.getMenu().findItem(R.id.action_timed_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_block_domain).setVisible(false);
                stringArrayConf = context.getResources().getStringArray(R.array.more_action_owner_confirm);
            } else {
                popup.getMenu().findItem(R.id.action_redraft).setVisible(false);
                popup.getMenu().findItem(R.id.action_remove).setVisible(false);
                if (statusToDeal.account.acct.split("@").length < 2)
                    popup.getMenu().findItem(R.id.action_block_domain).setVisible(false);
                stringArrayConf = context.getResources().getStringArray(R.array.more_action_confirm);
            }
            boolean display_admin_statuses = sharedpreferences.getBoolean(context.getString(R.string.SET_DISPLAY_ADMIN_STATUSES) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, false);
            if (!display_admin_statuses) {
                popup.getMenu().findItem(R.id.action_admin).setVisible(false);
            }

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
                            int position = getPositionAsync(notificationList, statusList, statusToDeal);
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
                                context.startActivity(intent);
                                sendAction(context, Helper.ARG_STATUS_DELETED, statusToDeal, null);
                            });
                        }
                    });
                    builderInner.setMessage(statusToDeal.text);
                    builderInner.show();
                } else if (itemId == R.id.action_schedule_boost) {
                    MastodonHelper.scheduleBoost(context, MastodonHelper.ScheduleType.BOOST, statusToDeal, null, null);
                } else if (itemId == R.id.action_admin) {
                   /* Intent intent = new Intent(context, AccountReportActivity.class);
                    intent.putExtra(Helper.ARG_ACCOUNT, statusToDeal.account);
                    context.startActivity(intent);*/
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
                                int position = getPositionAsync(notificationList, statusList, status);
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
                    builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    builderInner.setPositiveButton(R.string.yes, (dialog, which) -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.account.id, null, null)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        statusToTranslate = Html.fromHtml(statusToDeal.content, Html.FROM_HTML_MODE_LEGACY).toString();
                    else
                        statusToTranslate = Html.fromHtml(statusToDeal.content).toString();
                    myTransL.translate(statusToTranslate, MyTransL.getLocale(), params, new Results() {
                        @Override
                        public void onSuccess(Translate translate) {
                            if (translate.getTranslatedContent() != null) {
                                statusToDeal.translationShown = true;
                                statusToDeal.translationContent = translate.getTranslatedContent();
                                new Thread(() -> {
                                    SpannableHelper.convertStatus(context.getApplicationContext(), statusToDeal);
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> adapter.notifyItemChanged(getPositionAsync(notificationList, statusList, statusToDeal));
                                    mainHandler.post(myRunnable);
                                }).start();
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
                    ClipData clip = ClipData.newPlainText(Helper.CLIP_BOARD, status.text);
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
                }
                return true;
            });
            popup.show();
        });
        holder.binding.actionButtonReply.setOnLongClickListener(v -> {
            CrossActionHelper.doCrossAction(context, CrossActionHelper.TypeOfCrossAction.REPLY_ACTION, null, statusToDeal);
            return true;
        });
        holder.binding.actionButtonReply.setOnClickListener(v -> {
            if (remote) {
                Toasty.info(context, context.getString(R.string.retrieve_remote_status), Toasty.LENGTH_SHORT).show();
                searchVM.search(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, statusToDeal.url, null, "statuses", false, true, false, 0, null, null, 1)
                        .observe((LifecycleOwner) context, results -> {
                            if (results.statuses != null && results.statuses.size() > 0) {
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
                context.startActivity(intent);
            }
        });
        //For reports

        if (holder.bindingReport != null) {
            holder.bindingReport.checkbox.setChecked(status.isChecked);
            holder.bindingReport.checkbox.setOnClickListener(v -> status.isChecked = !status.isChecked);
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

    /**
     * Will manage the current position of the element in the adapter. Action is async, and position might have changed
     *
     * @param notificationList List<Notification> - Not null when calling from notification adapter
     * @param statusList       ist<Status> statusList - Not null when calling from status adapter
     * @param status           Status - Current status
     * @return int - position in real time
     */
    public static int getPositionAsync(List<Notification> notificationList, List<Status> statusList, Status status) {
        int position = 0;
        if (statusList != null) {
            for (Status _status : statusList) {
                if (_status.id.compareTo(status.id) == 0) {
                    break;
                }
                position++;
            }
        } else if (notificationList != null) {
            for (Notification notification : notificationList) {
                if (notification.status != null && notification.status.id.compareTo(status.id) == 0) {
                    break;
                }
                position++;
            }
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (timelineType == Timeline.TimeLineEnum.ART) {
            return STATUS_ART;
        } else {
            return isVisble(timelineType, statusList.get(position)) ? STATUS_VISIBLE : STATUS_HIDDEN;
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
            statusManagement(context, statusesVM, searchVM, holder, this, statusList, null, status, timelineType, minified);
            if (holder.timer != null) {
                holder.timer.cancel();
                holder.timer = null;
            }
            if (status.emojis != null && status.emojis.size() > 0) {
                holder.timer = new Timer();
                holder.timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> holder.binding.statusContent.invalidate();
                        mainHandler.post(myRunnable);

                    }
                }, 100, 100);
            }
        } else if (viewHolder.getItemViewType() == STATUS_ART) {
            StatusViewHolder holder = (StatusViewHolder) viewHolder;
            MastodonHelper.loadPPMastodon(holder.bindingArt.artPp, status.account);
            Glide.with(holder.bindingArt.artMedia.getContext())
                    .load(status.art_attachment.preview_url)
                    .apply(new RequestOptions().transform(new RoundedCorners((int) Helper.convertDpToPixel(3, context))))
                    .into(holder.bindingArt.artMedia);
            holder.bindingArt.artAcct.setText(status.account.span_display_name, TextView.BufferType.SPANNABLE);
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
        if (holder instanceof StatusViewHolder && ((StatusViewHolder) holder).timer != null) {
            ((StatusViewHolder) holder).timer.cancel();
        }
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusBinding binding;
        DrawerStatusHiddenBinding bindingHidden;
        DrawerStatusReportBinding bindingReport;
        DrawerStatusNotificationBinding bindingNotification;
        DrawerStatusArtBinding bindingArt;
        Timer timer;

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
    }


}