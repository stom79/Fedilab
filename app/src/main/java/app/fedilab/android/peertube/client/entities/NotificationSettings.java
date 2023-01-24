package app.fedilab.android.peertube.client.entities;
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

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class NotificationSettings {

    @SerializedName("abuseAsModerator")
    private int abuseAsModerator;
    @SerializedName("abuseNewMessage")
    private int abuseNewMessage;
    @SerializedName("abuseStateChange")
    private int abuseStateChange;
    @SerializedName("autoInstanceFollowing")
    private int autoInstanceFollowing;
    @SerializedName("blacklistOnMyVideo")
    private int blacklistOnMyVideo;
    @SerializedName("commentMention")
    private int commentMention;
    @SerializedName("myVideoImportFinished")
    private int myVideoImportFinished;
    @SerializedName("myVideoPublished")
    private int myVideoPublished;
    @SerializedName("newCommentOnMyVideo")
    private int newCommentOnMyVideo;
    @SerializedName("newFollow")
    private int newFollow;
    @SerializedName("newInstanceFollower")
    private int newInstanceFollower;
    @SerializedName("newUserRegistration")
    private int newUserRegistration;
    @SerializedName("newVideoFromSubscription")
    private int newVideoFromSubscription;
    @SerializedName("videoAutoBlacklistAsModerator")
    private int videoAutoBlacklistAsModerator;


    public int getAbuseAsModerator() {
        return abuseAsModerator;
    }

    public void setAbuseAsModerator(int abuseAsModerator) {
        this.abuseAsModerator = abuseAsModerator;
    }

    public int getAbuseNewMessage() {
        return abuseNewMessage;
    }

    public void setAbuseNewMessage(int abuseNewMessage) {
        this.abuseNewMessage = abuseNewMessage;
    }

    public int getAbuseStateChange() {
        return abuseStateChange;
    }

    public void setAbuseStateChange(int abuseStateChange) {
        this.abuseStateChange = abuseStateChange;
    }

    public int getAutoInstanceFollowing() {
        return autoInstanceFollowing;
    }

    public void setAutoInstanceFollowing(int autoInstanceFollowing) {
        this.autoInstanceFollowing = autoInstanceFollowing;
    }

    public int getBlacklistOnMyVideo() {
        return blacklistOnMyVideo;
    }

    public void setBlacklistOnMyVideo(int blacklistOnMyVideo) {
        this.blacklistOnMyVideo = blacklistOnMyVideo;
    }

    public int getCommentMention() {
        return commentMention;
    }

    public void setCommentMention(int commentMention) {
        this.commentMention = commentMention;
    }

    public int getMyVideoImportFinished() {
        return myVideoImportFinished;
    }

    public void setMyVideoImportFinished(int myVideoImportFinished) {
        this.myVideoImportFinished = myVideoImportFinished;
    }

    public int getMyVideoPublished() {
        return myVideoPublished;
    }

    public void setMyVideoPublished(int myVideoPublished) {
        this.myVideoPublished = myVideoPublished;
    }

    public int getNewCommentOnMyVideo() {
        return newCommentOnMyVideo;
    }

    public void setNewCommentOnMyVideo(int newCommentOnMyVideo) {
        this.newCommentOnMyVideo = newCommentOnMyVideo;
    }

    public int getNewFollow() {
        return newFollow;
    }

    public void setNewFollow(int newFollow) {
        this.newFollow = newFollow;
    }

    public int getNewInstanceFollower() {
        return newInstanceFollower;
    }

    public void setNewInstanceFollower(int newInstanceFollower) {
        this.newInstanceFollower = newInstanceFollower;
    }

    public int getNewUserRegistration() {
        return newUserRegistration;
    }

    public void setNewUserRegistration(int newUserRegistration) {
        this.newUserRegistration = newUserRegistration;
    }

    public int getNewVideoFromSubscription() {
        return newVideoFromSubscription;
    }

    public void setNewVideoFromSubscription(int newVideoFromSubscription) {
        this.newVideoFromSubscription = newVideoFromSubscription;
    }

    public int getVideoAutoBlacklistAsModerator() {
        return videoAutoBlacklistAsModerator;
    }

    public void setVideoAutoBlacklistAsModerator(int videoAutoBlacklistAsModerator) {
        this.videoAutoBlacklistAsModerator = videoAutoBlacklistAsModerator;
    }
}
