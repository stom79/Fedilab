package app.fedilab.android.mastodon.ui.drawer;
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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.fedilab.android.mastodon.helper.UnifiedEmojiPicker;

import java.lang.ref.WeakReference;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAnnouncementBinding;
import app.fedilab.android.mastodon.client.entities.api.Announcement;
import app.fedilab.android.mastodon.client.entities.api.Reaction;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AnnouncementsVM;


public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementHolder> {

    private final List<Announcement> announcements;
    private Context context;
    private AnnouncementsVM announcementsVM;

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

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedpreferences.getBoolean(context.getString(R.string.SET_CARDVIEW), false)) {
            holder.binding.cardviewContainer.setCardElevation(Helper.convertDpToPixel(5, context));
            holder.binding.dividerCard.setVisibility(View.GONE);
        }


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
        holder.binding.statusAddCustomEmoji.setOnClickListener(v -> {
            List<app.fedilab.android.mastodon.client.entities.api.Emoji> customEmojis =
                    emojis != null ? emojis.get(BaseMainActivity.currentInstance) : null;
            UnifiedEmojiPicker.show(context,
                    holder.binding.statusAddCustomEmoji,
                    holder.binding.layoutReactions.fakeEdittext,
                    customEmojis,
                    unicode -> {
                        boolean alreadyAdded = false;
                        for (Reaction reaction : announcement.reactions) {
                            if (reaction.name.compareTo(unicode) == 0) {
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
                            reaction.name = unicode;
                            announcement.reactions.add(0, reaction);
                            notifyItemChanged(position);
                        }
                        announcementsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AnnouncementsVM.class);
                        if (alreadyAdded) {
                            announcementsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, unicode);
                        } else {
                            announcementsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, unicode);
                        }
                    },
                    (shortcode, url, staticUrl) -> {
                        boolean alreadyAdded = false;
                        for (Reaction reaction : announcement.reactions) {
                            if (reaction.name.compareTo(shortcode) == 0) {
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
                            reaction.name = shortcode;
                            reaction.url = url;
                            reaction.static_url = staticUrl;
                            announcement.reactions.add(0, reaction);
                            notifyItemChanged(position);
                        }
                        announcementsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AnnouncementsVM.class);
                        if (alreadyAdded) {
                            announcementsVM.removeReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, shortcode);
                        } else {
                            announcementsVM.addReaction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, announcement.id, shortcode);
                        }
                    });
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
