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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestBuilder;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.DrawerStatusChatBinding;
import app.fedilab.android.databinding.LayoutMediaBinding;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.MediaHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;

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
                                            Status statusToDeal, Attachment attachment) {
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
        if (statusToDeal.media_attachments.get(0).meta != null && statusToDeal.media_attachments.get(0).meta.focus != null) {
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

        holder.binding.messageContent.setText(
                status.getSpanContent(context,
                        new WeakReference<>(holder.binding.messageContent),
                        () -> mRecyclerView.post(() -> notifyItemChanged(holder.getBindingAdapterPosition()))),
                TextView.BufferType.SPANNABLE);
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
        int textColor;
        if (status.account.id.equals(MainActivity.currentUserID)) {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_right_tail);
            textColor = R.attr.colorOnPrimary;
        } else {
            holder.binding.mainContainer.setBackgroundResource(R.drawable.bubble_left_tail);
            textColor = R.attr.colorOnSecondary;
        }
        holder.binding.date.setTextColor(ThemeHelper.getAttColor(context, textColor));
        holder.binding.messageContent.setTextColor(ThemeHelper.getAttColor(context, textColor));
        holder.binding.userName.setTextColor(ThemeHelper.getAttColor(context, textColor));

        if (status.media_attachments != null && status.media_attachments.size() > 0) {
            holder.binding.media.mediaContainer.setVisibility(View.VISIBLE);
            int mediaPosition = 1;

            int defaultHeight = (int) Helper.convertDpToPixel(300, context);
            if (measuredWidth > 0) {
                defaultHeight = (int) (measuredWidth * 3) / 4;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, defaultHeight);
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