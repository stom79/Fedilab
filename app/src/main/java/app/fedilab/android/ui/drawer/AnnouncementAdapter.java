package app.fedilab.android.ui.drawer;
/* Copyright 2022 Thomas Schneider
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
import static app.fedilab.android.BaseMainActivity.emojis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Announcement;
import app.fedilab.android.client.entities.api.Reaction;
import app.fedilab.android.databinding.DrawerAnnouncementBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.AnnouncementsVM;


public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementHolder> {

    private final List<Announcement> announcements;
    private Context context;
    private AnnouncementsVM announcementsVM;
    private AlertDialog alertDialogEmoji;

    public AnnouncementAdapter(List<Announcement> announcements) {
        this.announcements = announcements;
    }

    public int getCount() {
        return announcements.size();
    }

    public Announcement getItem(int position) {
        return announcements.get(position);
    }

    @NonNull
    @Override
    public AnnouncementHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerAnnouncementBinding itemBinding = DrawerAnnouncementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AnnouncementHolder(itemBinding);
    }


    @Override
    public void onBindViewHolder(@NonNull AnnouncementHolder holder, int position) {
        Announcement announcement = announcements.get(position);
        if (announcement.reactions != null && announcement.reactions.size() > 0) {
            ReactionAdapter reactionAdapter = new ReactionAdapter(announcement.id, announcement.reactions);
            holder.binding.layoutReactions.reactionsView.setAdapter(reactionAdapter);
            LinearLayoutManager layoutManager
                    = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.binding.layoutReactions.reactionsView.setLayoutManager(layoutManager);
        } else {
            holder.binding.layoutReactions.reactionsView.setAdapter(null);
        }
        holder.binding.content.setText(
                announcement.getSpanContent(context,
                        new WeakReference<>(holder.binding.content)),
                TextView.BufferType.SPANNABLE);
        if (announcement.starts_at != null) {
            String dateIni;
            String dateEnd;
            if (announcement.all_day) {
                dateIni = Helper.shortDateToString(announcement.starts_at);
                dateEnd = Helper.shortDateToString(announcement.ends_at);
            } else {
                dateIni = Helper.longDateToString(announcement.starts_at);
                dateEnd = Helper.longDateToString(announcement.ends_at);
            }
            String text = context.getString(R.string.action_announcement_from_to, dateIni, dateEnd);
            holder.binding.dates.setText(text);
            holder.binding.dates.setVisibility(View.VISIBLE);
        } else {
            holder.binding.dates.setVisibility(View.GONE);
        }
        holder.binding.layoutReactions.statusEmoji.setOnClickListener(v -> {
            EmojiManager.install(new EmojiOneProvider());
            final EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(holder.binding.layoutReactions.statusEmoji).setOnEmojiPopupDismissListener(() -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(holder.binding.layoutReactions.statusEmoji.getWindowToken(), 0);
            }).setOnEmojiClickListener((emoji, imageView) -> {
                String emojiStr = imageView.getUnicode();
                boolean alreadyAdded = false;
                for (Reaction reaction : announcement.reactions) {
                    if (reaction.name.compareTo(emojiStr) == 0) {
                        alreadyAdded = true;
                        reaction.count = (reaction.count - 1);
                        if (reaction.count == 0) {
                            announcement.reactions.remove(reaction);
                        }
                        notifyItemChanged(position);
                        break;
                    }
                }
                if (!alreadyAdded) {
                    Reaction reaction = new Reaction();
                    reaction.me = true;
                    reaction.count = 1;
                    reaction.name = emojiStr;
                    announcement.reactions.add(0, reaction);
                    notifyItemChanged(position);
                }
                announcementsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AnnouncementsVM.class);
                if (alreadyAdded) {
                    announcementsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, emojiStr);
                } else {
                    announcementsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, emojiStr);
                }
            })
                    .build(holder.binding.layoutReactions.fakeEdittext);
            emojiPopup.toggle();
        });
        holder.binding.layoutReactions.statusAddCustomEmoji.setOnClickListener(v -> {
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
                gridView.setOnItemClickListener((parent, view, index, id) -> {
                    String emojiStr = emojis.get(BaseMainActivity.currentInstance).get(index).shortcode;
                    String url = emojis.get(BaseMainActivity.currentInstance).get(index).url;
                    String static_url = emojis.get(BaseMainActivity.currentInstance).get(index).static_url;
                    boolean alreadyAdded = false;
                    for (Reaction reaction : announcement.reactions) {
                        if (reaction.name.compareTo(emojiStr) == 0) {
                            alreadyAdded = true;
                            reaction.count = (reaction.count - 1);
                            if (reaction.count == 0) {
                                announcement.reactions.remove(reaction);
                            }
                            notifyItemChanged(position);
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
                        announcement.reactions.add(0, reaction);
                        notifyItemChanged(position);
                    }
                    announcementsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AnnouncementsVM.class);
                    if (alreadyAdded) {
                        announcementsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, emojiStr);
                    } else {
                        announcementsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, emojiStr);
                    }
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
        });
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }


    static class AnnouncementHolder extends RecyclerView.ViewHolder {
        DrawerAnnouncementBinding binding;

        AnnouncementHolder(DrawerAnnouncementBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}