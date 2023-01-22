package app.fedilab.android.mastodon.services;
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


import android.content.Context;

import app.fedilab.android.mastodon.client.entities.app.StatusDraft;
import app.fedilab.android.mastodon.jobs.ComposeWorker;

public class ThreadMessageService {

    public ThreadMessageService(Context context, String instance, String userId, String token, StatusDraft statusDraft, String scheduledDate, String editMessageId) {
        ComposeWorker.DataPost dataPost = new ComposeWorker.DataPost();
        dataPost.instance = instance;
        dataPost.userId = userId;
        dataPost.token = token;
        dataPost.scheduledDate = scheduledDate;
        dataPost.statusDraft = statusDraft;
        dataPost.statusEditId = editMessageId;
        ComposeWorker.publishMessage(context, dataPost);
    }
}
