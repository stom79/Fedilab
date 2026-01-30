package app.fedilab.android.misskey.client.entities;
/* Copyright 2026 Thomas Schneider
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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Poll;

public class MisskeyPoll implements Serializable {

    @SerializedName("multiple")
    public boolean multiple;
    @SerializedName("expiresAt")
    public Date expiresAt;
    @SerializedName("choices")
    public List<MisskeyPollChoice> choices;

    public static class MisskeyPollChoice implements Serializable {
        @SerializedName("text")
        public String text;
        @SerializedName("votes")
        public int votes;
        @SerializedName("isVoted")
        public boolean isVoted;
    }

    public Poll toPoll(String noteId) {
        Poll poll = new Poll();
        poll.id = noteId;
        poll.expires_at = this.expiresAt;
        poll.expired = this.expiresAt != null && this.expiresAt.before(new Date());
        poll.multiple = this.multiple;

        int totalVotes = 0;
        List<Integer> ownVotes = new ArrayList<>();

        if (this.choices != null) {
            poll.options = new ArrayList<>();
            for (int i = 0; i < this.choices.size(); i++) {
                MisskeyPollChoice choice = this.choices.get(i);
                Poll.PollItem option = new Poll.PollItem();
                option.title = choice.text;
                option.votes_count = choice.votes;
                poll.options.add(option);
                totalVotes += choice.votes;
                if (choice.isVoted) {
                    ownVotes.add(i);
                }
            }
        }

        poll.votes_count = totalVotes;
        poll.voters_count = totalVotes;
        poll.voted = !ownVotes.isEmpty();
        poll.own_votes = ownVotes;

        return poll;
    }
}
