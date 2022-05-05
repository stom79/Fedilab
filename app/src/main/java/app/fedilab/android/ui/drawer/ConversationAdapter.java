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


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import app.fedilab.android.R;
import app.fedilab.android.activities.ContextActivity;
import app.fedilab.android.client.mastodon.entities.Account;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Conversation;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.DrawerConversationBinding;
import app.fedilab.android.databinding.ThumbnailBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;


public class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Conversation> conversationList;
    private Context context;
    private boolean isExpended = false;

    public ConversationAdapter(List<Conversation> conversations) {
        if (conversations == null) {
            conversations = new ArrayList<>();
        }
        this.conversationList = conversations;
    }

    public int getCount() {
        return conversationList.size();
    }

    public Conversation getItem(int position) {
        return conversationList.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerConversationBinding itemBinding = DrawerConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationHolder(itemBinding);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Conversation conversation = conversationList.get(position);
        ConversationHolder holder = (ConversationHolder) viewHolder;
        int theme_icons_color = -1;
        int theme_statuses_color = -1;
        int theme_text_color = -1;
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean("use_custom_theme", false)) {
            //Getting custom colors
            theme_icons_color = sharedpreferences.getInt("theme_icons_color", -1);
            theme_statuses_color = sharedpreferences.getInt("theme_statuses_color", -1);
            theme_text_color = sharedpreferences.getInt("theme_text_color", -1);

        }
        if (theme_icons_color != -1) {
            Helper.changeDrawableColor(context, R.drawable.ic_baseline_star_24, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_repeat, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_star_outline, theme_icons_color);
            Helper.changeDrawableColor(context, R.drawable.ic_person, theme_icons_color);
        }
        if (theme_statuses_color != -1) {
            holder.binding.container.setBackgroundColor(theme_statuses_color);
        }
        if (theme_text_color != -1) {
            holder.binding.statusContent.setTextColor(theme_text_color);
            holder.binding.spoiler.setTextColor(theme_text_color);
        }

        //--- Profile Pictures for participants ---
        holder.binding.participantsList.removeAllViews();
        for (Account account : conversation.accounts) {
            ImageView imageView = new ImageView(context);
            LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams((int) Helper.convertDpToPixel(20, context), (int) Helper.convertDpToPixel(20, context));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            lp.setMarginEnd((int) Helper.convertDpToPixel(6, context));
            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(lp);
            MastodonHelper.loadPPMastodon(imageView, account);
            holder.binding.participantsList.addView(imageView);
        }
        //---- SPOILER TEXT -----
        boolean expand_cw = sharedpreferences.getBoolean(context.getString(R.string.SET_EXPAND_CW), false);
        if (conversation.last_status.spoiler_text != null && !conversation.last_status.spoiler_text.trim().isEmpty()) {
            if (expand_cw || !conversation.last_status.sensitive) {
                isExpended = true;
            }
            holder.binding.spoilerExpand.setOnClickListener(v -> {
                isExpended = !isExpended;
                notifyItemChanged(position);
            });
            holder.binding.spoiler.setVisibility(View.VISIBLE);
            holder.binding.spoiler.setText(conversation.last_status.span_spoiler_text, TextView.BufferType.SPANNABLE);
        } else {
            holder.binding.spoiler.setVisibility(View.GONE);
            holder.binding.spoilerExpand.setVisibility(View.GONE);
            holder.binding.spoiler.setText(null);
        }
        //--- MAIN CONTENT ---
        holder.binding.statusContent.setText(conversation.last_status.span_content, TextView.BufferType.SPANNABLE);
        //--- DATE ---
        holder.binding.lastMessageDate.setText(Helper.dateDiff(context, conversation.last_status.created_at));

        holder.binding.statusContent.setOnClickListener(v -> {
            Intent intent = new Intent(context, ContextActivity.class);
            intent.putExtra(Helper.ARG_STATUS, conversation.last_status);
            context.startActivity(intent);
        });

        holder.binding.attachmentsListContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(context, ContextActivity.class);
                intent.putExtra(Helper.ARG_STATUS, conversation.last_status);
                context.startActivity(intent);
            }
            return false;
        });

        displayAttachments(holder, position);
        if (holder.timer != null) {
            holder.timer.cancel();
            holder.timer = null;
        }

        if (conversation.last_status.emojis != null && conversation.last_status.emojis.size() > 0) {
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
    }

    private void displayAttachments(ConversationAdapter.ConversationHolder holder, int position) {
        if (conversationList.get(position).last_status != null) {
            Status status = conversationList.get(position).last_status;
            holder.binding.attachmentsList.removeAllViews();
            List<Attachment> attachmentList = status.media_attachments;
            if (attachmentList != null && attachmentList.size() > 0) {
                for (Attachment attachment : attachmentList) {
                    ThumbnailBinding thumbnailBinding = ThumbnailBinding.inflate(LayoutInflater.from(context), holder.binding.attachmentsList, false);
                    thumbnailBinding.buttonPlay.setVisibility(View.GONE);
                    if (attachment.type.compareToIgnoreCase("image") == 0) {
                        Glide.with(thumbnailBinding.preview.getContext())
                                .load(attachment.preview_url)
                                .into(thumbnailBinding.preview);
                    } else if (attachment.type.compareToIgnoreCase("video") == 0 || attachment.type.compareToIgnoreCase("gifv") == 0) {
                        thumbnailBinding.buttonPlay.setVisibility(View.VISIBLE);
                        long interval = 2000;
                        RequestOptions options = new RequestOptions().frame(interval);
                        Glide.with(thumbnailBinding.preview.getContext()).asBitmap()
                                .load(attachment.preview_url)
                                .apply(options)
                                .into(thumbnailBinding.preview);
                    } else if (attachment.type.compareToIgnoreCase("audio") == 0) {
                        Glide.with(thumbnailBinding.preview.getContext())
                                .load(R.drawable.ic_baseline_audio_file_24)
                                .into(thumbnailBinding.preview);
                    } else {
                        Glide.with(thumbnailBinding.preview.getContext())
                                .load(R.drawable.ic_baseline_insert_drive_file_24)
                                .into(thumbnailBinding.preview);
                    }
                    holder.binding.attachmentsList.addView(thumbnailBinding.getRoot());
                }
                holder.binding.attachmentsList.setVisibility(View.VISIBLE);
            } else {
                holder.binding.attachmentsList.setVisibility(View.GONE);
            }
        } else {
            holder.binding.attachmentsList.setVisibility(View.GONE);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ConversationHolder && ((ConversationHolder) holder).timer != null) {
            ((ConversationHolder) holder).timer.cancel();
        }
    }

    static class ConversationHolder extends RecyclerView.ViewHolder {
        DrawerConversationBinding binding;
        Timer timer;

        ConversationHolder(DrawerConversationBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}