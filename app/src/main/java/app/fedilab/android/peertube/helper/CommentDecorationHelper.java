package app.fedilab.android.peertube.helper;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */


import java.util.List;

import app.fedilab.android.peertube.client.data.CommentData;

public class CommentDecorationHelper {

    public static int getIndentation(String replyToCommentId, List<CommentData.Comment> comments) {
        return numberOfIndentation(0, replyToCommentId, comments);
    }


    private static int numberOfIndentation(int currentIdentation, String replyToCommentId, List<CommentData.Comment> comments) {

        String targetedComment = null;
        for (CommentData.Comment comment : comments) {
            if (replyToCommentId.compareTo(comment.getId()) == 0) {
                targetedComment = comment.getInReplyToCommentId();
                break;
            }
        }
        if (targetedComment != null) {
            currentIdentation++;
            return numberOfIndentation(currentIdentation, targetedComment, comments);
        } else {
            return Math.min(currentIdentation, 15);
        }
    }

}
