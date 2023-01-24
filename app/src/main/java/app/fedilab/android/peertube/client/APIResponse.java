package app.fedilab.android.peertube.client;
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

import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.BlockData.Block;
import app.fedilab.android.peertube.client.data.CaptionData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.CommentData;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.data.NotificationData;
import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.data.VideoPlaylistData.VideoPlaylist;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.OverviewVideo;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.Rating;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class APIResponse {

    private List<AccountData.PeertubeAccount> accounts = null;
    private List<ChannelData.Channel> channels = null;
    private String targetedId = null;
    private String actionReturn = null;
    private Rating rating;
    private OverviewVideo overviewVideo = null;
    private Map<String, List<PlaylistExist>> videoExistPlaylist = null;
    private List<VideoData.Video> peertubes = null;
    private List<CommentData.Comment> comments = null;
    private List<Block> muted;
    private List<VideoPlaylist> videoPlaylist;
    private CommentData.CommentThreadData commentThreadData;
    private List<NotificationData.Notification> peertubeNotifications = null;
    private List<PlaylistData.Playlist> playlists = null;
    private List<String> domains = null;
    private Map<String, Boolean> relationships = null;
    private List<CaptionData.Caption> captions = null;
    private Error error = null;
    private String since_id, max_id;
    private List<InstanceData.Instance> instances;
    private String stringData;
    private int statusCode;
    private String captionText;

    public List<AccountData.PeertubeAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountData.PeertubeAccount> accounts) {
        this.accounts = accounts;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public String getMax_id() {
        return max_id;
    }

    public void setMax_id(String max_id) {
        this.max_id = max_id;
    }

    public String getSince_id() {
        return since_id;
    }

    public void setSince_id(String since_id) {
        this.since_id = since_id;
    }


    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }


    public List<VideoData.Video> getPeertubes() {
        return peertubes;
    }

    public void setPeertubes(List<VideoData.Video> peertubes) {
        this.peertubes = peertubes;
    }


    public List<NotificationData.Notification> getPeertubeNotifications() {
        return peertubeNotifications;
    }

    public void setPeertubeNotifications(List<NotificationData.Notification> peertubeNotifications) {
        this.peertubeNotifications = peertubeNotifications;
    }


    public List<PlaylistData.Playlist> getPlaylists() {
        return playlists;
    }

    public void setPlaylists(List<PlaylistData.Playlist> playlists) {
        this.playlists = playlists;
    }


    public String getTargetedId() {
        return targetedId;
    }

    public void setTargetedId(String targetedId) {
        this.targetedId = targetedId;
    }


    public String getStringData() {
        return stringData;
    }

    public void setStringData(String stringData) {
        this.stringData = stringData;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, Boolean> getRelationships() {
        return relationships;
    }

    public void setRelationships(Map<String, Boolean> relationships) {
        this.relationships = relationships;
    }

    public List<InstanceData.Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<InstanceData.Instance> instances) {
        this.instances = instances;
    }

    public List<CaptionData.Caption> getCaptions() {
        return captions;
    }

    public void setCaptions(List<CaptionData.Caption> captions) {
        this.captions = captions;
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

    public List<ChannelData.Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelData.Channel> channels) {
        this.channels = channels;
    }

    public List<CommentData.Comment> getComments() {
        return comments;
    }

    public void setComments(List<CommentData.Comment> comments) {
        this.comments = comments;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public OverviewVideo getOverviewVideo() {
        return overviewVideo;
    }

    public void setOverviewVideo(OverviewVideo overviewVideo) {
        this.overviewVideo = overviewVideo;
    }

    public String getActionReturn() {
        return actionReturn;
    }

    public void setActionReturn(String actionReturn) {
        this.actionReturn = actionReturn;
    }

    public List<Block> getMuted() {
        return muted;
    }

    public void setMuted(List<Block> muted) {
        this.muted = muted;
    }

    public List<VideoPlaylist> getVideoPlaylist() {
        return videoPlaylist;
    }

    public void setVideoPlaylist(List<VideoPlaylist> videoPlaylist) {
        this.videoPlaylist = videoPlaylist;
    }

    public Map<String, List<PlaylistExist>> getVideoExistPlaylist() {
        return videoExistPlaylist;
    }

    public void setVideoExistPlaylist(Map<String, List<PlaylistExist>> videoExistPlaylist) {
        this.videoExistPlaylist = videoExistPlaylist;
    }

    public CommentData.CommentThreadData getCommentThreadData() {
        return commentThreadData;
    }

    public void setCommentThreadData(CommentData.CommentThreadData commentThreadData) {
        this.commentThreadData = commentThreadData;
    }
}
