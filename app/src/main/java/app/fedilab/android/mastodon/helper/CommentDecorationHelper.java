package app.fedilab.android.mastodon.helper;
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


import org.jetbrains.annotations.NotNull;

import java.util.List;

import app.fedilab.android.mastodon.client.entities.api.Status;


public class CommentDecorationHelper {

    /**
     * Get the indentation for the reply status with the id of original status
     *
     * @param replyToCommentId String - The id of the original status if it is a reply
     * @param statuses         List<Status> statuses
     * @param maxIndent        Maximum number of indents
     * @return int - indentation
     */
    public static int getIndentation(@NotNull String replyToCommentId, List<Status> statuses, int maxIndent) {
        return numberOfIndentation(0, replyToCommentId, statuses, maxIndent);
    }

    /**
     * Returns the indentation depending of the number of replies
     *
     * @param currentIndentation int - The current indentation (symbolize a margin from start)
     * @param replyToCommentId   String - The id of the original status if it is a reply
     * @param statuses           List<Status> statuses
     * @param maxIndent          Maximum number of indents
     * @return int - indentation
     */
    private static int numberOfIndentation(int currentIndentation, String replyToCommentId, List<Status> statuses, int maxIndent) {
        if (replyToCommentId == null) {
            return 0;
        } else {
            currentIndentation++;
        }
        String targetedComment = null;
        for (Status status : statuses) {
            if (status != null && status.id != null && replyToCommentId.compareTo(status.id) == 0) {
                targetedComment = status.in_reply_to_id;
                break;
            }
        }
        if (targetedComment != null) {
            return numberOfIndentation(currentIndentation, targetedComment, statuses, maxIndent);
        } else {
            if (currentIndentation == 0) {
                currentIndentation = 1;
            }
            return Math.min(currentIndentation, maxIndent);
        }
    }

}
