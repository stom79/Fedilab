package app.fedilab.android.helper;
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


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;

import com.bumptech.glide.Glide;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.ScheduledBoost;
import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.client.mastodon.entities.Conversation;
import app.fedilab.android.client.mastodon.entities.Notification;
import app.fedilab.android.client.mastodon.entities.Pagination;
import app.fedilab.android.client.mastodon.entities.RelationShip;
import app.fedilab.android.client.mastodon.entities.ScheduledStatus;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.DatetimePickerBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.jobs.ScheduleBoostWorker;
import app.fedilab.android.ui.drawer.ComposeAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;

public class MastodonHelper {

    public static final String CLIENT_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String SCOPE = "scope";
    public static final String REDIRECT_CONTENT_WEB = "fedilab://backtofedilab";
    public static final String OAUTH_SCOPES = "read write follow push";
    public static final String OAUTH_SCOPES_ADMIN = "read write follow push admin:read admin:write";

    public static final int ACCOUNTS_PER_CALL = 40;
    public static final int STATUSES_PER_CALL = 40;
    public static final int NOTIFICATIONS_PER_CALL = 30;


    public static int accountsPerCall(Context _mContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_mContext);
        return sharedPreferences.getInt(_mContext.getString(R.string.SET_ACCOUNTS_PER_CALL), ACCOUNTS_PER_CALL);
    }

    public static int statusesPerCall(Context _mContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_mContext);
        return sharedPreferences.getInt(_mContext.getString(R.string.SET_STATUSES_PER_CALL), STATUSES_PER_CALL);
    }

    public static int notificationsPerCall(Context _mContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_mContext);
        return sharedPreferences.getInt(_mContext.getString(R.string.SET_NOTIFICATIONS_PER_CALL), NOTIFICATIONS_PER_CALL);
    }

    /**
     * Returns authorisation URL
     *
     * @param instance  String instance
     * @param client_id String client id
     * @param admin     boolean - Admin scope
     * @return String - Authorisation URL
     */
    public static String authorizeURL(@NonNull String instance, @NonNull String client_id, boolean admin) {
        String queryString = CLIENT_ID + "=" + client_id;
        queryString += "&" + REDIRECT_URI + "=" + Uri.encode(REDIRECT_CONTENT_WEB);
        queryString += "&" + RESPONSE_TYPE + "=code";
        if (admin) {
            queryString += "&" + SCOPE + "=" + OAUTH_SCOPES_ADMIN;
        } else {
            queryString += "&" + SCOPE + "=" + OAUTH_SCOPES;
        }
        return "https://" + instance + "/oauth/authorize" + "?" + queryString;
    }

    /* /**
     * Retrieve pagination from header
     *
     * @param headers Headers
     * @return Pagination
     */
   /* public static Pagination getPagination(Headers headers) {
        String link = headers.get("Link");
        Pagination pagination = new Pagination();
        if (link != null) {
            Pattern patternMaxId = Pattern.compile("max_id=([0-9a-zA-Z]+).*");
            Matcher matcherMaxId = patternMaxId.matcher(link);
            if (matcherMaxId.find()) {
                pagination.max_id = matcherMaxId.group(1);
            }
            Pattern patternSinceId = Pattern.compile("since_id=([0-9a-zA-Z]+).*");
            Matcher matcherSinceId = patternSinceId.matcher(link);
            if (matcherSinceId.find()) {
                pagination.since_id = matcherSinceId.group(1);
            }
            Pattern patternMinId = Pattern.compile("min_id=([0-9a-zA-Z]+).*");
            Matcher matcherMinId = patternMinId.matcher(link);
            if (matcherMinId.find()) {
                pagination.min_id = matcherMinId.group(1);
            }
        }
        return pagination;
    }*/

    public static Pagination getPaginationNotification(List<Notification> notificationList) {
        Pagination pagination = new Pagination();
        if (notificationList == null || notificationList.size() == 0) {
            return pagination;
        }
        pagination.max_id = notificationList.get(0).id;
        pagination.min_id = String.valueOf(Long.parseLong(notificationList.get(notificationList.size() - 1).id) - 1);
        return pagination;
    }

    public static Pagination getPaginationStatus(List<Status> statusList) {
        Pagination pagination = new Pagination();
        if (statusList == null || statusList.size() == 0) {
            return pagination;
        }
        pagination.max_id = statusList.get(0).id;
        pagination.min_id = statusList.get(statusList.size() - 1).id;
        return pagination;
    }

    public static Pagination getPaginationAccount(List<Account> accountList) {
        Pagination pagination = new Pagination();
        if (accountList == null || accountList.size() == 0) {
            return pagination;
        }
        pagination.max_id = accountList.get(0).id;
        pagination.min_id = accountList.get(accountList.size() - 1).id;
        return pagination;
    }

    public static Pagination getPaginationScheduledStatus(List<ScheduledStatus> scheduledStatusList) {
        Pagination pagination = new Pagination();
        if (scheduledStatusList == null || scheduledStatusList.size() == 0) {
            return pagination;
        }
        pagination.max_id = scheduledStatusList.get(0).id;
        pagination.min_id = scheduledStatusList.get(scheduledStatusList.size() - 1).id;
        return pagination;
    }

    public static Pagination getPaginationConversation(List<Conversation> conversationList) {
        Pagination pagination = new Pagination();
        if (conversationList == null || conversationList.size() == 0) {
            return pagination;
        }
        pagination.max_id = conversationList.get(0).id;
        pagination.min_id = conversationList.get(conversationList.size() - 1).id;
        return pagination;
    }

    public static void loadPPMastodon(ImageView view, app.fedilab.android.client.mastodon.entities.Account account) {
        loadProfileMediaMastodon(view, account, MediaAccountType.AVATAR);
    }

    public static void loadProfileMediaMastodon(ImageView view, app.fedilab.android.client.mastodon.entities.Account account, MediaAccountType type) {
        Context context = view.getContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean disableGif = sharedpreferences.getBoolean(context.getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = disableGif ? (type == MediaAccountType.AVATAR ? account.avatar_static : account.header_static) : (type == MediaAccountType.AVATAR ? account.avatar : account.header);
        @DrawableRes int placeholder = type == MediaAccountType.AVATAR ? R.drawable.ic_person : R.drawable.default_banner;
        if (disableGif || (!targetedUrl.endsWith(".gif"))) {
            Glide.with(view.getContext())
                    .asDrawable()
                    .load(targetedUrl)
                    .thumbnail(0.1f)
                    .placeholder(placeholder)
                    .into(view);
        } else {
            Glide.with(view.getContext())
                    .asGif()
                    .load(targetedUrl)
                    .thumbnail(0.1f)
                    .placeholder(placeholder)
                    .into(view);
        }
    }

    /**
     * Convert a date in String -> format yyyy-MM-dd HH:mm:ss
     *
     * @param date Date
     * @return String
     */
    public static String dateToStringPoll(Date date) {
        if (date == null)
            return null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(date);
    }

    /***
     * Returns a String depending of the date
     * @param context Context
     * @param dateEndPoll Date
     * @return String
     */
    public static String dateDiffPoll(Context context, Date dateEndPoll) {
        if (dateEndPoll == null) {
            return "";
        }
        Date now = new Date();
        long diff = dateEndPoll.getTime() - now.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return context.getResources().getQuantityString(R.plurals.date_day_polls, (int) days, (int) days);
        else if (hours > 0)
            return context.getResources().getQuantityString(R.plurals.date_hours_polls, (int) hours, (int) hours);
        else if (minutes > 0)
            return context.getResources().getQuantityString(R.plurals.date_minutes_polls, (int) minutes, (int) minutes);
        else {
            if (seconds < 0)
                seconds = 0;
            return context.getResources().getQuantityString(R.plurals.date_seconds_polls, (int) seconds, (int) seconds);
        }
    }

    /***
     * Returns the length used when composing a toot
     * @param composeViewHolder ComposeAdapter.ComposeViewHolder itemHolder for compose elements
     * @return int - characters used
     */
    public static int countLength(ComposeAdapter.ComposeViewHolder composeViewHolder) {
        String content = composeViewHolder.binding.content.getText().toString();
        String cwContent = composeViewHolder.binding.contentSpoiler.getText().toString();
        String contentCount = content;
        contentCount = contentCount.replaceAll("(?i)(^|[^/\\w])@(([a-z0-9_]+)@[a-z0-9.-]+[a-z0-9]+)", "$1@$3");
        Matcher matcherALink = Patterns.WEB_URL.matcher(contentCount);
        while (matcherALink.find()) {
            final String url = matcherALink.group(1);
            if (url != null) {
                contentCount = contentCount.replace(url, "abcdefghijklmnopkrstuvw");
            }
        }
        int contentLength = contentCount.length() - countWithEmoji(content);
        int cwLength = cwContent.length() - countWithEmoji(cwContent);
        return cwLength + contentLength;
    }

    /**
     * Length used by emoji displayed on the toot
     *
     * @param text String - The current text
     * @return int - Number of characters used by emoji
     */
    private static int countWithEmoji(String text) {
        int emojiCount = 0;
        for (int i = 0; i < text.length(); i++) {
            int type = Character.getType(text.charAt(i));
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                emojiCount++;
            }
        }
        return emojiCount / 2;
    }


    /**
     * Schedule a boost or timed mutes
     *
     * @param context Context
     * @param status  {@link Status}
     */
    public static void scheduleBoost(Context context, ScheduleType scheduleType, Status status, Account account, TimedMuted listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, Helper.dialogStyle());
        DatetimePickerBinding binding = DatetimePickerBinding.inflate(((Activity) context).getLayoutInflater());
        dialogBuilder.setView(binding.getRoot());
        final AlertDialog alertDialogBoost = dialogBuilder.create();
        binding.timePicker.setIs24HourView(true);
        //Buttons management
        binding.dateTimeCancel.setOnClickListener(v -> alertDialogBoost.dismiss());
        binding.dateTimeNext.setOnClickListener(v -> {
            binding.datePicker.setVisibility(View.GONE);
            binding.timePicker.setVisibility(View.VISIBLE);
            binding.dateTimePrevious.setVisibility(View.VISIBLE);
            binding.dateTimeNext.setVisibility(View.GONE);
            binding.dateTimeSet.setVisibility(View.VISIBLE);
        });
        binding.dateTimePrevious.setOnClickListener(v -> {
            binding.datePicker.setVisibility(View.VISIBLE);
            binding.timePicker.setVisibility(View.GONE);
            binding.dateTimePrevious.setVisibility(View.GONE);
            binding.dateTimeNext.setVisibility(View.VISIBLE);
            binding.dateTimeSet.setVisibility(View.GONE);
        });
        binding.dateTimeSet.setOnClickListener(v -> {
            int hour, minute;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = binding.timePicker.getHour();
                minute = binding.timePicker.getMinute();
            } else {
                hour = binding.timePicker.getCurrentHour();
                minute = binding.timePicker.getCurrentMinute();
            }
            Calendar calendar = new GregorianCalendar(binding.datePicker.getYear(),
                    binding.datePicker.getMonth(),
                    binding.datePicker.getDayOfMonth(),
                    hour,
                    minute);
            long time = calendar.getTimeInMillis();
            if ((time - new Date().getTime()) < 60000) {
                if (scheduleType == ScheduleType.BOOST) {
                    Toasty.warning(context, context.getString(R.string.toot_scheduled_date), Toast.LENGTH_LONG).show();
                } else {
                    Toasty.warning(context, context.getString(R.string.timed_mute_date_error), Toast.LENGTH_LONG).show();
                }
            } else {
                //Schedules the toot

                long delayToPass = (time - new Date().getTime());
                if (scheduleType == ScheduleType.BOOST) {
                    Data inputData = new Data.Builder()
                            .putString(Helper.ARG_INSTANCE, BaseMainActivity.currentInstance)
                            .putString(Helper.ARG_TOKEN, BaseMainActivity.currentToken)
                            .putString(Helper.ARG_USER_ID, BaseMainActivity.currentUserID)
                            .putString(Helper.ARG_STATUS_ID, status.reblog != null ? status.reblog.id : status.id)
                            .build();
                    OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ScheduleBoostWorker.class)
                            .setInputData(inputData)
                            .setInitialDelay(delayToPass, TimeUnit.MILLISECONDS)
                            .build();
                    ScheduledBoost scheduledBoost = new ScheduledBoost();
                    scheduledBoost.userId = BaseMainActivity.currentUserID;
                    scheduledBoost.statusId = status.reblog != null ? status.reblog.id : status.id;
                    scheduledBoost.scheduledAt = calendar.getTime();
                    scheduledBoost.instance = BaseMainActivity.currentInstance;
                    scheduledBoost.workerUuid = oneTimeWorkRequest.getId();
                    scheduledBoost.status = status.reblog != null ? status.reblog : status;
                    try {
                        new ScheduledBoost(context).insertScheduledBoost(scheduledBoost);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                    //Clear content
                    Toasty.info(context, context.getString(R.string.boost_scheduled), Toast.LENGTH_LONG).show();
                } else {
                    AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
                    String accountId;
                    String acct;
                    if (account == null) {
                        accountId = status.account.id;
                        acct = status.account.acct;
                    } else {
                        accountId = account.id;
                        acct = account.acct;
                    }
                    accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountId, true, (int) delayToPass)
                            .observe((LifecycleOwner) context, relationShip -> {
                                if (listener != null) {
                                    listener.onTimedMute(relationShip);
                                }
                            });
                    Toasty.info(context, context.getString(R.string.timed_mute_date, acct, Helper.dateToString(calendar.getTime())), Toast.LENGTH_LONG).show();
                }
                alertDialogBoost.dismiss();
            }
        });
        alertDialogBoost.show();
    }

    public enum MediaAccountType {
        AVATAR,
        HEADER
    }

    public enum visibility {
        @SerializedName("PUBLIC")
        PUBLIC("public"),
        @SerializedName("UNLISTED")
        UNLISTED("unlisted"),
        @SerializedName("PRIVATE")
        PRIVATE("private"),
        @SerializedName("DIRECT")
        DIRECT("direct");

        private final String value;

        visibility(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public enum ScheduleType {
        BOOST,
        TIMED_MUTED
    }

    public interface TimedMuted {
        void onTimedMute(RelationShip relationShip);
    }

}
