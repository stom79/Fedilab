package app.fedilab.android.mastodon.ui.drawer;
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


import static app.fedilab.android.mastodon.ui.drawer.StatusAdapter.prepareRequestBuilder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerStatusChatBinding;
import app.fedilab.android.databinding.LayoutMediaBinding;
import app.fedilab.android.databinding.LayoutPollItemBinding;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Poll;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.LongClickLinkMovementMethod;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;

public class StatusDirectMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Status> statusList;
    private Context context;
    private RecyclerView mRecyclerView;
    private static float measuredWidth = -1;
    public StatusDirectMessageAdapter(List<Status> data) {
        this.statusList = data;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerStatusChatBinding itemBinding = DrawerStatusChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StatusChatViewHolder(itemBinding);
    }

    public static void loadAndAddAttachment(Context context, LayoutMediaBinding layoutMediaBinding,
                                            StatusChatViewHolder holder,
                                            RecyclerView.Adapter<RecyclerView.ViewHolder> adapter,
                                            int mediaPosition, float mediaW, float mediaH, float ratio,
                                            Status status, Attachment attachment) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final int timeout = sharedpreferences.getInt(context.getString(R.string.SET_NSFW_TIMEOUT), 5);
        boolean long_press_media = sharedpreferences.getBoolean(context.getString(R.string.SET_LONG_PRESS_STORE_MEDIA), false);
        boolean expand_media = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_MEDIA), false);

        LinearLayout.LayoutParams lp;

        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutMediaBinding.media.setScaleType(ImageView.ScaleType.CENTER_CROP);

        layoutMediaBinding.media.setLayoutParams(lp);

        float focusX = 0.f;
        float focusY = 0.f;
        if (status.media_attachments.get(0).meta != null && status.media_attachments.get(0).meta.focus != null) {
            focusX = status.media_attachments.get(0).meta.focus.x;
            focusY = status.media_attachments.get(0).meta.focus.y;
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

        RequestBuilder<Drawable> requestBuilder = prepareRequestBuilder(context, attachment, mediaW * ratio, mediaH * ratio, focusX, focusY, status.sensitive, false);
        if (!status.sensitive || expand_media) {
            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_24);
        } else {
            layoutMediaBinding.viewHide.setImageResource(R.drawable.ic_baseline_visibility_off_24);
        }
        requestBuilder.load(attachment.preview_url).into(layoutMediaBinding.media);
        if (status.sensitive) {
            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, ThemeHelper.getAttColor(context, R.attr.colorError));
        } else {
            Helper.changeDrawableColor(context, layoutMediaBinding.viewHide, R.color.white);
        }

        layoutMediaBinding.media.setOnClickListener(v -> {
            if (status.sensitive && !expand_media) {
                status.sensitive = false;
                int position = holder.getBindingAdapterPosition();
                adapter.notifyItemChanged(position);

                if (timeout > 0) {
                    new CountDownTimer((timeout * 1000L), 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            status.sensitive = true;
                            adapter.notifyItemChanged(position);
                        }
                    }.start();
                }
                return;
            }
            Intent mediaIntent = new Intent(context, MediaActivity.class);
            Bundle b = new Bundle();
            b.putInt(Helper.ARG_MEDIA_POSITION, mediaPosition);
            b.putSerializable(Helper.ARG_MEDIA_ARRAY, new ArrayList<>(status.media_attachments));
            mediaIntent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation((Activity) context, layoutMediaBinding.media, status.media_attachments.get(0).url);
            // start the new activity
            context.startActivity(mediaIntent, options.toBundle());
        });
        layoutMediaBinding.viewHide.setOnClickListener(v -> {
            status.sensitive = !status.sensitive;
            adapter.notifyItemChanged(holder.getBindingAdapterPosition());
        });

    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }


    @Override
    public int getItemCount() {
        return statusList.size();
    }

    public static class StatusChatViewHolder extends RecyclerView.ViewHolder {
        DrawerStatusChatBinding binding;

        StatusChatViewHolder(DrawerStatusChatBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        StatusChatViewHolder holder = (StatusChatViewHolder) viewHolder;
        Status status = statusList.get(position);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        holder.binding.messageContent.setText(
                status.getSpanContent(context,
                        new WeakReference<>(holder.binding.messageContent),
                        () -> mRecyclerView.post(() -> notifyItemChanged(holder.getBindingAdapterPosition()))),
                TextView.BufferType.SPANNABLE);
        holder.binding.messageContent.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        if (measuredWidth <= 0 && status.media_attachments != null && status.media_attachments.size() > 0) {
            holder.binding.media.mediaContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    holder.binding.media.mediaContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    measuredWidth = holder.binding.media.mediaContainer.getWidth();
                    notifyItemChanged(0, statusList.size());
                }
            });
        }
        MastodonHelper.loadPPMastodon(holder.binding.userPp, status.account);
        holder.binding.date.setText(Helper.longDateToString(status.created_at));
        //Owner account
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (status.account.id.equals(MainActivity.currentUserID)) {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_right_tail);

            layoutParams.setMargins((int) Helper.convertDpToPixel(50, context), (int) Helper.convertDpToPixel(12, context), 0, 0);
            holder.binding.date.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
            holder.binding.messageContent.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
            holder.binding.userName.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
        } else {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_left_tail);
            layoutParams.setMargins(0, (int) Helper.convertDpToPixel(12, context), (int) Helper.convertDpToPixel(50, context), 0);
            holder.binding.date.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.binding.messageContent.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.binding.userName.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
        holder.binding.mainContainer.setLayoutParams(layoutParams);

        int truncate_toots_size = sharedpreferences.getInt(context.getString(R.string.SET_TRUNCATE_TOOTS_SIZE), 0);
        if (truncate_toots_size > 0) {
            holder.binding.messageContent.setMaxLines(truncate_toots_size);
            holder.binding.messageContent.setEllipsize(TextUtils.TruncateAt.END);

            holder.binding.messageContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (holder.binding.messageContent.getLineCount() > 1) {
                        holder.binding.messageContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (holder.binding.messageContent.getLineCount() > truncate_toots_size) {
                            holder.binding.toggleTruncate.setVisibility(View.VISIBLE);
                            if (status.isTruncated) {
                                holder.binding.toggleTruncate.setText(R.string.display_toot_truncate);
                                holder.binding.toggleTruncate.setCompoundDrawables(null, null, ContextCompat.getDrawable(context, R.drawable.ic_display_more), null);
                            } else {
                                holder.binding.toggleTruncate.setText(R.string.hide_toot_truncate);
                                holder.binding.toggleTruncate.setCompoundDrawables(null, null, ContextCompat.getDrawable(context, R.drawable.ic_display_less), null);
                            }
                            holder.binding.toggleTruncate.setOnClickListener(v -> {
                                status.isTruncated = !status.isTruncated;
                                notifyItemChanged(holder.getBindingAdapterPosition());
                            });
                            if (status.isTruncated) {
                                holder.binding.messageContent.setMaxLines(truncate_toots_size);
                            } else {
                                holder.binding.messageContent.setMaxLines(9999);
                            }
                        } else {
                            holder.binding.toggleTruncate.setVisibility(View.GONE);
                        }
                    }
                }
            });
        } else {
            holder.binding.toggleTruncate.setVisibility(View.GONE);
        }

        final float scale = sharedpreferences.getFloat(context.getString(R.string.SET_FONT_SCALE), 1.1f);
        if (status.poll != null && status.poll.options != null) {

            holder.binding.poll.pollInfo.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
            holder.binding.poll.refresh.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
            StatusesVM statusesVM = new ViewModelProvider((ViewModelStoreOwner) context).get(StatusesVM.class);
            if (status.poll.voted || status.poll.expired) {
                holder.binding.poll.submitVote.setVisibility(View.GONE);
                holder.binding.poll.rated.setVisibility(View.VISIBLE);
                holder.binding.poll.multipleChoice.setVisibility(View.GONE);
                holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.GONE);
                int greaterValue = 0;
                for (Poll.PollItem pollItem : status.poll.options) {
                    if (pollItem.votes_count > greaterValue)
                        greaterValue = pollItem.votes_count;
                }
                holder.binding.poll.rated.removeAllViews();
                List<Integer> ownvotes = status.poll.own_votes;
                int j = 0;
                if (status.poll.voters_count == 0 && status.poll.votes_count > 0) {
                    status.poll.voters_count = status.poll.votes_count;
                }
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                for (Poll.PollItem pollItem : status.poll.options) {
                    @NonNull LayoutPollItemBinding pollItemBinding = LayoutPollItemBinding.inflate(inflater, holder.binding.poll.rated, true);
                    double value = Math.ceil((pollItem.votes_count * 100) / (double) status.poll.voters_count);
                    pollItemBinding.pollItemPercent.setText(String.format("%s %%", (int) value));
                    pollItemBinding.pollItemText.setText(
                            pollItem.getSpanTitle(context, status,
                                    new WeakReference<>(pollItemBinding.pollItemText)),
                            TextView.BufferType.SPANNABLE);
                    pollItemBinding.pollItemPercent.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
                    pollItemBinding.pollItemText.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
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

                if (status.poll.voters_count == 0 && status.poll.votes_count > 0) {
                    status.poll.voters_count = status.poll.votes_count;
                }
                holder.binding.poll.rated.setVisibility(View.GONE);
                holder.binding.poll.submitVote.setVisibility(View.VISIBLE);
                if (status.poll.multiple) {
                    if ((holder.binding.poll.multipleChoice).getChildCount() > 0)
                        (holder.binding.poll.multipleChoice).removeAllViews();
                    for (Poll.PollItem pollOption : status.poll.options) {
                        CheckBox cb = new CheckBox(context);
                        cb.setText(
                                pollOption.getSpanTitle(context, status,
                                        new WeakReference<>(cb)),
                                TextView.BufferType.SPANNABLE);
                        holder.binding.poll.multipleChoice.addView(cb);
                        cb.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
                    }
                    holder.binding.poll.multipleChoice.setVisibility(View.VISIBLE);
                    holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.GONE);
                } else {
                    if ((holder.binding.poll.singleChoiceRadioGroup).getChildCount() > 0)
                        (holder.binding.poll.singleChoiceRadioGroup).removeAllViews();
                    for (Poll.PollItem pollOption : status.poll.options) {
                        RadioButton rb = new RadioButton(context);
                        rb.setText(
                                pollOption.getSpanTitle(context, status,
                                        new WeakReference<>(rb)),
                                TextView.BufferType.SPANNABLE);
                        rb.setTextColor(ThemeHelper.getAttColor(context, R.attr.colorOnSecondary));
                        holder.binding.poll.singleChoiceRadioGroup.addView(rb);
                    }
                    holder.binding.poll.singleChoiceRadioGroup.setVisibility(View.VISIBLE);
                    holder.binding.poll.multipleChoice.setVisibility(View.GONE);
                }
                holder.binding.poll.submitVote.setVisibility(View.VISIBLE);
                holder.binding.poll.submitVote.setOnClickListener(v -> {
                    int[] choice;
                    if (status.poll.multiple) {
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

                    statusesVM.votePoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.poll.id, choice)
                            .observe((LifecycleOwner) context, poll -> {
                                if (poll != null) {
                                    int i = 0;
                                    for (Poll.PollItem item : status.poll.options) {
                                        if (item.span_title != null) {
                                            poll.options.get(i).span_title = item.span_title;
                                        } else {
                                            poll.options.get(i).span_title = new SpannableString(item.title);
                                        }
                                        i++;
                                    }
                                    status.poll = poll;
                                    notifyItemChanged(holder.getBindingAdapterPosition());
                                }
                            });

                });
            }
            holder.binding.poll.refreshPoll.setOnClickListener(v -> statusesVM.getPoll(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, status.poll.id)
                    .observe((LifecycleOwner) context, poll -> {
                        if (poll != null) {
                            //Store span elements
                            int i = 0;
                            for (Poll.PollItem item : status.poll.options) {
                                if (item.span_title != null) {
                                    poll.options.get(i).span_title = item.span_title;
                                } else {
                                    poll.options.get(i).span_title = new SpannableString(item.title);
                                }
                                i++;
                            }
                            status.poll = poll;
                            notifyItemChanged(holder.getBindingAdapterPosition());
                        }
                    }));
            holder.binding.poll.pollContainer.setVisibility(View.VISIBLE);
            String pollInfo = context.getResources().getQuantityString(R.plurals.number_of_voters, status.poll.voters_count, status.poll.voters_count);
            if (status.poll.expired) {
                pollInfo += " - " + context.getString(R.string.poll_finish_at, MastodonHelper.dateToStringPoll(status.poll.expires_at));
            } else {
                pollInfo += " - " + context.getString(R.string.poll_finish_in, MastodonHelper.dateDiffPoll(context, status.poll.expires_at));
            }
            holder.binding.poll.pollInfo.setText(pollInfo);
        } else {
            holder.binding.poll.pollContainer.setVisibility(View.GONE);
        }
        holder.binding.userName.setText(
                status.account.getSpanDisplayName(context,
                        new WeakReference<>(holder.binding.userName)),
                TextView.BufferType.SPANNABLE);

        if (status.media_attachments != null && status.media_attachments.size() > 0) {
            holder.binding.media.mediaContainer.setVisibility(View.VISIBLE);
            int mediaPosition = 1;

            int defaultHeight = (int) Helper.convertDpToPixel(300, context);
            if (measuredWidth > 0) {
                defaultHeight = (int) (measuredWidth * 3) / 4;
            }
            LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, defaultHeight);
            holder.binding.media.mediaContainer.setLayoutParams(lp);
            if (status.media_attachments.size() == 1) {
                holder.binding.media.media1Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media2Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.media3Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.media4Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.moreMedia.setVisibility(View.GONE);
            } else if (status.media_attachments.size() == 2) {
                holder.binding.media.media1Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media2Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media3Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.media4Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.moreMedia.setVisibility(View.GONE);
            } else if (status.media_attachments.size() == 3) {
                holder.binding.media.media1Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media2Container.mediaRoot.setVisibility(View.GONE);
                holder.binding.media.media3Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media4Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.moreMedia.setVisibility(View.GONE);
            } else if (status.media_attachments.size() == 4) {
                holder.binding.media.media1Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media2Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media3Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media4Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.moreMedia.setVisibility(View.GONE);
            } else if (status.media_attachments.size() > 4) {
                holder.binding.media.media1Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media2Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media3Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.media4Container.mediaRoot.setVisibility(View.VISIBLE);
                holder.binding.media.moreMedia.setVisibility(View.VISIBLE);
                holder.binding.media.moreMedia.setText(context.getString(R.string.more_media, "+" + (status.media_attachments.size() - 4)));
            }
            for (Attachment attachment : status.media_attachments) {
                LayoutMediaBinding layoutMediaBinding = null;
                if (mediaPosition == 1) {
                    layoutMediaBinding = holder.binding.media.media1Container;
                } else if (mediaPosition == 2 && status.media_attachments.size() == 3) {
                    layoutMediaBinding = holder.binding.media.media3Container;
                } else if (mediaPosition == 2) {
                    layoutMediaBinding = holder.binding.media.media2Container;
                } else if (mediaPosition == 3 && status.media_attachments.size() == 3) {
                    layoutMediaBinding = holder.binding.media.media4Container;
                } else if (mediaPosition == 3) {
                    layoutMediaBinding = holder.binding.media.media3Container;
                } else if (mediaPosition == 4) {
                    layoutMediaBinding = holder.binding.media.media4Container;
                }
                if (layoutMediaBinding != null) {
                    loadAndAddAttachment(context, layoutMediaBinding, holder, this, mediaPosition, -1.f, -1.f, -1.f, status, attachment);
                }


                mediaPosition++;
            }
        } else {
            holder.binding.media.mediaContainer.setVisibility(View.GONE);
        }
    }
}