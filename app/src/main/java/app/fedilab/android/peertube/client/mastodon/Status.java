package app.fedilab.android.peertube.client.mastodon;
/* Copyright 2021 Thomas Schneider
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

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import app.fedilab.android.peertube.client.data.CommentData;


@SuppressWarnings("unused")
public class Status {

    @SerializedName("id")
    private String id;
    @SerializedName("in_reply_to_id")
    private String inReplyToCommentId;
    @SerializedName("account")
    private MastodonAccount.Account account;
    @SerializedName("url")
    private String url;
    @SerializedName("content")
    private String text;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("reblogs_count")
    private int reblogsCount;
    @SerializedName("favourites_count")
    private int favouritesCount;
    @SerializedName("favourited")
    private boolean favourited;
    @SerializedName("reblogged")
    private boolean reblogged;
    @SerializedName("bookmarked")
    private boolean bookmarked;

    public static CommentData.Comment convertStatusToComment(Status status) {
        CommentData.Comment comment = new CommentData.Comment();
        comment.setAccount(MastodonAccount.convertToPeertubeAccount(status.getAccount()));
        comment.setCreatedAt(status.getCreatedAt());
        comment.setText(status.getText());
        return comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInReplyToCommentId() {
        return inReplyToCommentId;
    }

    public void setInReplyToCommentId(String inReplyToCommentId) {
        this.inReplyToCommentId = inReplyToCommentId;
    }

    public MastodonAccount.Account getAccount() {
        return account;
    }

    public void setAccount(MastodonAccount.Account account) {
        this.account = account;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getReblogsCount() {
        return reblogsCount;
    }

    public void setReblogsCount(int reblogsCount) {
        this.reblogsCount = reblogsCount;
    }

    public int getFavouriteCount() {
        return favouritesCount;
    }

    public boolean isFavourited() {
        return favourited;
    }

    public void setFavourited(boolean favourited) {
        this.favourited = favourited;
    }

    public boolean isReblogged() {
        return reblogged;
    }

    public void setReblogged(boolean reblogged) {
        this.reblogged = reblogged;
    }

    public int getFavouritesCount() {
        return favouritesCount;
    }

    public void setFavouritesCount(int favouritesCount) {
        this.favouritesCount = favouritesCount;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }
}
