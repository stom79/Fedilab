package app.fedilab.android.services;
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

import static app.fedilab.android.services.PostMessageService.publishMessage;

import android.content.Context;

import app.fedilab.android.client.entities.app.StatusDraft;

public class ThreadMessageService {

    public ThreadMessageService(Context context, String instance, String token, StatusDraft statusDraft, String scheduledDate) {
        PostMessageService.DataPost dataPost = new PostMessageService.DataPost();
        dataPost.instance = instance;
        dataPost.token = token;
        dataPost.scheduledDate = scheduledDate;
        dataPost.statusDraft = statusDraft;
        publishMessage(context, dataPost);
    }
}
