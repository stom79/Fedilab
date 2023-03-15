package app.fedilab.android.peertube.activities;
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

import static com.google.android.exoplayer2.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO;
import static app.fedilab.android.BaseMainActivity.currentAccount;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.typeOfConnection;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.ADD_COMMENT;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.RATEVIDEO;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPLY;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPORT_VIDEO;
import static app.fedilab.android.peertube.helper.Helper.canMakeAction;
import static app.fedilab.android.peertube.helper.Helper.getAttColor;
import static app.fedilab.android.peertube.helper.Helper.isLoggedIn;
import static app.fedilab.android.peertube.helper.Helper.loadAvatar;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.vkay94.dtpv.youtube.YouTubeOverlay;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.varunest.sparkbutton.SparkButton;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.activities.BasePeertubeActivity;
import app.fedilab.android.databinding.ActivityPeertubeBinding;
import app.fedilab.android.databinding.PopupVideoInfoPeertubeBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.CacheDataSourceFactory;
import app.fedilab.android.mastodon.viewmodel.mastodon.StatusesVM;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.MenuItemVideo;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.CaptionData.Caption;
import app.fedilab.android.peertube.client.data.CommentData;
import app.fedilab.android.peertube.client.data.CommentData.Comment;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.client.data.PluginData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.File;
import app.fedilab.android.peertube.client.entities.MenuItemView;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.Report;
import app.fedilab.android.peertube.client.entities.UserSettings;
import app.fedilab.android.peertube.drawer.CommentListAdapter;
import app.fedilab.android.peertube.drawer.MenuAdapter;
import app.fedilab.android.peertube.drawer.MenuItemAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.viewmodel.CaptionsVM;
import app.fedilab.android.peertube.viewmodel.CommentVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import app.fedilab.android.peertube.viewmodel.SearchVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import app.fedilab.android.peertube.webview.MastalabWebChromeClient;
import app.fedilab.android.peertube.webview.MastalabWebViewClient;
import es.dmoral.toasty.Toasty;


public class PeertubeActivity extends BasePeertubeActivity implements CommentListAdapter.AllCommentRemoved, MenuAdapter.ItemClicked, MenuItemAdapter.ItemAction, Player.Listener {

    public static String video_id;
    public static List<String> playedVideos = new ArrayList<>();
    Uri captionURI;
    String captionLang;
    private String peertubeInstance, videoUuid;
    private ImageView fullScreenIcon;
    private boolean fullScreenMode;
    private int mode;
    private Map<String, List<PlaylistExist>> playlists;
    private boolean playInMinimized, autoPlay, autoFullscreen;
    private boolean onStopCalled;
    private List<Caption> captions;
    private String max_id;
    private boolean flag_loading;
    private boolean isMyVideo;
    private List<Comment> comments;
    private CommentListAdapter commentListAdapter;
    private CommentListAdapter commentReplyListAdapter;
    private boolean sepiaSearch;
    private ActivityPeertubeBinding binding;
    private List<Comment> commentsThread;
    private BroadcastReceiver mPowerKeyReceiver = null;
    private boolean isPlayInMinimized;
    private VideoData.Video nextVideo;
    private String show_more_content;
    private videoOrientation videoOrientationType;
    private int initialOrientation;
    private String currentResolution;
    private String currentCaption;
    private boolean isRemote;
    private boolean willPlayFromIntent;

    private Status status;

    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            activity.getWindow().getDecorView();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPeertubeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        videoOrientationType = videoOrientation.LANDSCAPE;
        max_id = "0";
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        String token = sharedpreferences.getString(PREF_USER_TOKEN, null);
        if (Helper.canMakeAction() && !sepiaSearch) {
            BaseAccount account = null;
            try {
                account = new app.fedilab.android.mastodon.client.entities.app.Account(PeertubeActivity.this).getAccountByToken(token);
            } catch (DBException e) {
                e.printStackTrace();
            }
            if (account != null && account.peertube_account != null) {
                loadAvatar(PeertubeActivity.this, account.peertube_account, binding.myPp);
            } else if (account != null && account.mastodon_account != null) {
                app.fedilab.android.mastodon.helper.Helper.loadPP(PeertubeActivity.this, binding.myPp, account);
            }
        }
        isRemote = false;

        fullScreenMode = false;
        initialOrientation = getResources().getConfiguration().orientation;
        if (Helper.isTablet(PeertubeActivity.this)) {
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    2.0f
            );
            binding.videoContainer.setLayoutParams(param);
        } else {
            if (initialOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        0,
                        4.0f
                );
                binding.videoContainer.setLayoutParams(param);
            }
        }
        isPlayInMinimized = false;
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mode = sharedpreferences.getInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_NORMAL);

        Intent intent = getIntent();

        Bundle b = intent.getExtras();
        if (b != null) {
            peertubeInstance = b.getString("peertube_instance", HelperInstance.getLiveInstance(PeertubeActivity.this));
            videoUuid = b.getString("video_uuid", null);
            isMyVideo = b.getBoolean("isMyVideo", false);
            sepiaSearch = b.getBoolean("sepia_search", false);
            peertube = (VideoData.Video) b.getSerializable("video");
        }
        if (currentAccount != null && currentAccount.peertube_account != null) {
            binding.myAcct.setText(String.format("@%s@%s", currentAccount.peertube_account.getUsername(), currentAccount.instance));
        }

        willPlayFromIntent = manageIntentUrl(intent);

        if (Helper.isLoggedIn()) {
            binding.peertubePlaylist.setVisibility(View.VISIBLE);
        } else if (typeOfConnection == PeertubeMainActivity.TypeOfConnection.REMOTE_ACCOUNT) {
            binding.peertubeLikeCount.setVisibility(View.GONE);
            binding.peertubeDislikeCount.setVisibility(View.GONE);
            binding.peertubePlaylist.setVisibility(View.GONE);
            binding.peertubeReblog.setVisibility(View.VISIBLE);
            binding.peertubeFavorite.setVisibility(View.VISIBLE);
            binding.peertubeBookmark.setVisibility(View.VISIBLE);
        }

        binding.peertubeDescriptionMore.setOnClickListener(v -> {
            if (show_more_content != null && peertube != null) {
                if (binding.peertubeDescriptionMore.getText().toString().compareTo(getString(R.string.show_more)) == 0) {
                    binding.peertubeDescriptionMore.setText(getString(R.string.show_less));
                    binding.peertubeDescription.setText(show_more_content);
                } else {
                    binding.peertubeDescriptionMore.setText(getString(R.string.show_more));
                    binding.peertubeDescription.setText(peertube.getDescription());
                }
            }
        });
        if (!Helper.canMakeAction() || sepiaSearch) {
            binding.writeCommentContainer.setVisibility(View.GONE);
        }
        playInMinimized = sharedpreferences.getBoolean(getString(R.string.set_video_minimize_choice), true);
        autoPlay = sharedpreferences.getBoolean(getString(R.string.set_autoplay_choice), true);
        autoFullscreen = sharedpreferences.getBoolean(getString(R.string.set_fullscreen_choice), false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            playInMinimized = false;
        }

        if (peertube != null && peertube.isNsfw()) {
            binding.videoSensitive.setVisibility(View.VISIBLE);
        } else {
            binding.videoSensitive.setVisibility(View.INVISIBLE);
        }
        if (mode == Helper.VIDEO_MODE_WEBVIEW) {
            binding.webviewVideo.setVisibility(View.VISIBLE);
            binding.mediaVideo.setVisibility(View.GONE);
            binding.doubleTapPlayerView.setVisibility(View.GONE);
            WebView webview_video = Helper.initializeWebview(PeertubeActivity.this, R.id.webview_video, null);

            MastalabWebChromeClient mastalabWebChromeClient = new MastalabWebChromeClient(PeertubeActivity.this, webview_video, binding.mainMediaFrame, binding.videoLayout);
            mastalabWebChromeClient.setOnToggledFullscreen(fullscreen -> {
                if (fullscreen) {
                    binding.videoLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.videoLayout.setVisibility(View.GONE);
                }
                toogleFullscreen(fullscreen);
            });
            binding.webviewVideo.getSettings().setAllowFileAccess(true);
            binding.webviewVideo.setWebChromeClient(mastalabWebChromeClient);
            binding.webviewVideo.getSettings().setDomStorageEnabled(true);
            binding.webviewVideo.getSettings().setMediaPlaybackRequiresUserGesture(false);
            binding.webviewVideo.setWebViewClient(new MastalabWebViewClient(PeertubeActivity.this));
            binding.webviewVideo.loadUrl("https://" + peertubeInstance + "/videos/embed/" + videoUuid);
        } else {
            binding.webviewVideo.setVisibility(View.GONE);
            binding.loader.setVisibility(View.VISIBLE);
        }

        if (mode != Helper.VIDEO_MODE_WEBVIEW) {
            binding.doubleTapPlayerView.setControllerShowTimeoutMs(1000);
            binding.doubleTapPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            initControllerButtons();

            binding.doubleTapPlayerView
                    .setDoubleTapDelay(500);
            binding.doubleTapPlayerView.setDoubleTapEnabled(true);
            binding.doubleTapPlayerView.setControllerShowTimeoutMs(0);
            binding.mediaVideo.performListener(new YouTubeOverlay.PerformListener() {
                @Override
                public void onAnimationStart() {
                    binding.mediaVideo.setVisibility(View.VISIBLE);
                    binding.doubleTapPlayerView.setUseController(false);
                }

                @Override
                public void onAnimationEnd() {
                    binding.mediaVideo.setVisibility(View.GONE);
                    binding.doubleTapPlayerView.setUseController(true);
                }
            }).playerView(binding.doubleTapPlayerView).seekSeconds(10);
            binding.doubleTapPlayerView.setPlayer(player);
            binding.doubleTapPlayerView.controller(binding.mediaVideo);
            if (player != null)
                binding.mediaVideo.player(player);
        }
        flag_loading = true;
        comments = new ArrayList<>();

        binding.closeReply.setOnClickListener(v -> closeCommentThread());
        binding.closePost.setOnClickListener(v -> closePostComment());

        commentListAdapter = new CommentListAdapter(comments, isMyVideo || Helper.isVideoOwner(PeertubeActivity.this, peertube), false, peertubeInstance, sepiaSearch);
        commentListAdapter.allCommentRemoved = PeertubeActivity.this;
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(PeertubeActivity.this);
        binding.peertubeComments.setLayoutManager(mLayoutManager);
        binding.peertubeComments.setNestedScrollingEnabled(false);
        binding.peertubeComments.setAdapter(commentListAdapter);
        binding.peertubeComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flag_loading) {
                            CommentVM commentViewModel = new ViewModelProvider(PeertubeActivity.this).get(CommentVM.class);
                            commentViewModel.getThread(sepiaSearch ? peertubeInstance : null, videoUuid, max_id).observe(PeertubeActivity.this, apiresponse -> manageVIewComment(apiresponse));
                        }
                    }
                }
            }
        });
        if (!willPlayFromIntent && peertube != null && sepiaSearch && peertube.getEmbedUrl() != null && Helper.isLoggedIn()) {
            SearchVM viewModelSearch = new ViewModelProvider(PeertubeActivity.this).get(SearchVM.class);
            viewModelSearch.getVideos("0", peertube.getUuid()).observe(PeertubeActivity.this, this::manageVIewVideos);
        } else {
            playVideo();
        }

        registBroadcastReceiver();
        if (autoFullscreen && autoPlay) {
            openFullscreenDialog();
        }
        binding.postCommentButton.setOnClickListener(v -> {
            if (canMakeAction() && !sepiaSearch) {
                openPostComment(null, 0);
            } else {
                if (sepiaSearch) {
                    Toasty.info(PeertubeActivity.this, getString(R.string.federation_issue), Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.error(PeertubeActivity.this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void manageVIewVideos(APIResponse apiResponse) {
        if (apiResponse == null || apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0) {
            playVideo();
            return;
        }
        peertube = apiResponse.getPeertubes().get(0);
        if (peertube.isNsfw()) {
            binding.videoSensitive.setVisibility(View.VISIBLE);
        } else {
            binding.videoSensitive.setVisibility(View.INVISIBLE);
        }
        if (player != null && peertube.getUserHistory() != null) {
            player.seekTo(peertube.getUserHistory().getCurrentTime() * 1000);
        }
        sepiaSearch = false;
        playVideo();
    }

    public void manageVIewComment(APIResponse apiResponse) {
        flag_loading = false;
        if (apiResponse == null || (apiResponse.getError() != null)) {
            if (apiResponse == null)
                Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        int oldSize = comments.size();
        int newComments = 0;
        for (Comment comment : apiResponse.getComments()) {
            if (comment.getText() != null && comment.getText().trim().length() > 0) {
                comments.add(comment);
                newComments++;
            }
        }
        if (comments.size() > 0) {
            binding.peertubeComments.setVisibility(View.VISIBLE);
            commentListAdapter.notifyItemRangeInserted(oldSize, newComments);
        }
    }

    public void manageVIewCommentReply(Comment comment, APIResponse apiResponse) {
        if (apiResponse == null || apiResponse.getError() != null || apiResponse.getCommentThreadData() == null) {
            if (apiResponse == null || apiResponse.getError() == null)
                Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            else
                Toasty.error(PeertubeActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        List<CommentData.CommentThreadData> commentThreadDataList = apiResponse.getCommentThreadData().getChildren();
        commentsThread = generateCommentReply(commentThreadDataList, new ArrayList<>());
        comment.setInReplyToCommentId(null);
        comment.setTotalReplies(0);
        commentsThread.add(0, comment);
        commentReplyListAdapter = new CommentListAdapter(commentsThread, Helper.isVideoOwner(PeertubeActivity.this, peertube), true, peertubeInstance, sepiaSearch);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(PeertubeActivity.this);
        binding.peertubeReply.setLayoutManager(mLayoutManager);
        binding.peertubeReply.setNestedScrollingEnabled(false);
        binding.peertubeReply.setAdapter(commentReplyListAdapter);
        binding.peertubeReply.setVisibility(View.VISIBLE);
        if (commentsThread.size() > 0) {
            commentReplyListAdapter.notifyItemRangeInserted(0, commentsThread.size());
        }
    }

    private List<Comment> generateCommentReply(List<CommentData.CommentThreadData> commentThreadDataList, List<Comment> comments) {
        for (CommentData.CommentThreadData commentThreadData : commentThreadDataList) {
            if (commentThreadData.getComment().getText() != null && commentThreadData.getComment().getText().trim().length() > 0) {
                commentThreadData.getComment().setReply(true);
                comments.add(commentThreadData.getComment());
            }
            if (commentThreadData.getChildren() != null && commentThreadData.getChildren().size() > 0) {
                generateCommentReply(commentThreadData.getChildren(), comments);
            }
        }
        return comments;
    }


    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle b = intent.getExtras();
        if (b != null) {
            isRemote = false;
            peertubeInstance = b.getString("peertube_instance", HelperInstance.getLiveInstance(PeertubeActivity.this));
            videoUuid = b.getString("video_uuid", null);
            setRequestedOrientationCustom(initialOrientation);
            if (comments != null && comments.size() > 0) {
                int number = comments.size();
                comments.clear();
                commentListAdapter.notifyItemRangeRemoved(0, number);
            }
            playVideo();
        }
        willPlayFromIntent = manageIntentUrl(intent);
    }

    private boolean manageIntentUrl(Intent intent) {
        if (intent.getData() != null) { //Comes from a link
            String url = intent.getData().toString();
            Pattern link = Pattern.compile("(https?://[\\da-z.-]+\\.[a-z.]{2,10})/videos/watch/(\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})(\\?start=(\\d+[hH])?(\\d+[mM])?(\\d+[sS])?)?$");
            Matcher matcherLink = link.matcher(url);
            if (matcherLink.find()) {
                String instance = matcherLink.group(1);
                String uuid = matcherLink.group(2);
                String hour = matcherLink.group(4);
                String min = matcherLink.group(5);
                String sec = matcherLink.group(6);
                int hourInt, minInt, secInt;
                int totalSeconds = 0;
                if (hour != null) {
                    hourInt = Integer.parseInt(hour.replace("h", ""));
                    totalSeconds += 3600 * hourInt;
                }
                if (min != null) {
                    minInt = Integer.parseInt(min.replace("m", ""));
                    totalSeconds += 60 * minInt;
                }
                if (sec != null) {
                    secInt = Integer.parseInt(sec.replace("s", ""));
                    totalSeconds += secInt;
                }
                captionURI = null;
                captionLang = null;
                if (instance != null && uuid != null) {
                    peertubeInstance = instance.replace("https://", "").replace("http://", "");
                    sepiaSearch = true; // Sepia search flag is used because, at this time we don't know if the video is federated.
                    videoUuid = uuid;
                    peertube = new VideoData.Video();
                    peertube.setUuid(uuid);
                    peertube.setEmbedUrl(url);
                    if (totalSeconds > 0) {
                        VideoData.UserHistory userHistory = new VideoData.UserHistory();
                        userHistory.setCurrentTime(totalSeconds);
                        peertube.setUserHistory(userHistory);
                    }
                    TimelineVM viewModelTimeline = new ViewModelProvider(PeertubeActivity.this).get(TimelineVM.class);
                    viewModelTimeline.getVideo(peertubeInstance, peertube.getUuid(), false).observe(PeertubeActivity.this, this::manageVIewVideo);
                    if (player != null) {
                        player.release();
                    }
                    if (comments != null && comments.size() > 0) {
                        int number = comments.size();
                        comments.clear();
                        commentListAdapter.notifyItemRangeRemoved(0, number);
                    }
                    fetchComments();
                    isRemote = true;
                    return true;
                } else {
                    Helper.forwardToAnotherApp(PeertubeActivity.this, intent);
                    finish();
                }
            } else {
                Helper.forwardToAnotherApp(PeertubeActivity.this, intent);
                finish();
            }
        }
        return false;
    }

    private void playVideo() {
        if (status == null && typeOfConnection == PeertubeMainActivity.TypeOfConnection.REMOTE_ACCOUNT) {
            app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM searchVM = new ViewModelProvider(PeertubeActivity.this).get(app.fedilab.android.mastodon.viewmodel.mastodon.SearchVM.class);
            searchVM.search(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), videoUuid, null, "statuses", false, true, false, 0, null, null, 1)
                    .observe(PeertubeActivity.this, results -> {
                        if (results != null && results.statuses != null && results.statuses.size() > 0) {
                            status = results.statuses.get(0);
                        }
                    });
        }
        if (player != null) {
            player.release();
            player = new ExoPlayer.Builder(PeertubeActivity.this).build();
            binding.mediaVideo.player(player);
            binding.doubleTapPlayerView.setPlayer(player);
            binding.loader.setVisibility(View.GONE);
            player.setPlayWhenReady(autoPlay);
            if (autoPlay) {
                binding.doubleTapPlayerView.hideController();
            }
            captions = null;
        }
        currentResolution = null;
        show_more_content = null;
        currentCaption = "null";
        binding.peertubeDescriptionMore.setVisibility(View.GONE);

        if (autoFullscreen && autoPlay) {
            openFullscreenDialog();
        }

        TimelineVM feedsViewModel = new ViewModelProvider(PeertubeActivity.this).get(TimelineVM.class);
        if (!isRemote) {
            feedsViewModel.getVideo(sepiaSearch ? peertubeInstance : null, videoUuid, isMyVideo).observe(PeertubeActivity.this, this::manageVIewVideo);
        }
        CaptionsVM captionsViewModel = new ViewModelProvider(PeertubeActivity.this).get(CaptionsVM.class);
        captionsViewModel.getCaptions(sepiaSearch ? peertubeInstance : null, videoUuid).observe(PeertubeActivity.this, this::manageCaptions);

        //Post view count
        new Thread(() -> {
            String videoId = peertube != null ? peertube.getUuid() : videoUuid;
            new RetrofitPeertubeAPI(PeertubeActivity.this).postView(videoId);
        }).start();
        //manage plugin
        new Thread(() -> {
            String videoInstance = peertubeInstance != null ? peertubeInstance : peertube.getAccount().getHost();
            InstanceData.InstanceConfig instanceConfig = new RetrofitPeertubeAPI(PeertubeActivity.this, videoInstance, null).getConfigInstance();
            if (instanceConfig != null && instanceConfig.getPlugin() != null && instanceConfig.getPlugin().getRegistered() != null) {
                for (PluginData.PluginInfo pluginInfo : instanceConfig.getPlugin().getRegistered()) {
                    if (pluginInfo.getName().compareTo("player-watermark") == 0) {
                        PluginData.WaterMark getWaterMark = new RetrofitPeertubeAPI(PeertubeActivity.this, videoInstance, null).getWaterMark();
                        if (getWaterMark != null && getWaterMark.getDescription() != null && getWaterMark.getDescription().getWatermarkImageUrl() != null) {
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> {
                                Glide.with(binding.watermark.getContext())
                                        .asDrawable()
                                        .load(getWaterMark.getDescription().getWatermarkImageUrl())
                                        .into(binding.watermark);
                                binding.watermark.setVisibility(View.VISIBLE);
                            };
                            mainHandler.post(myRunnable);
                        }
                    }
                }
            }
        }).start();
        new Thread(() -> {
            try {
                RetrofitPeertubeAPI api;
                if (peertubeInstance != null) {
                    api = new RetrofitPeertubeAPI(PeertubeActivity.this, peertubeInstance, null);
                } else {
                    api = new RetrofitPeertubeAPI(PeertubeActivity.this);
                }
                VideoData.Description description = api.getVideoDescription(videoUuid);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (!isFinishing()) {
                        if (description == null) {
                            binding.peertubeDescriptionMore.setVisibility(View.GONE);
                            show_more_content = null;
                        } else {
                            if (!PeertubeActivity.this.isFinishing()) {
                                if (peertube != null && ((peertube.getDescription() == null && description.getDescription() != null && description.getDescription().trim().length() > 0) || (peertube.getDescription() != null && description.getDescription() != null
                                        && description.getDescription().compareTo(peertube.getDescription()) > 0))) {
                                    binding.peertubeDescriptionMore.setVisibility(View.VISIBLE);
                                    show_more_content = description.getDescription();
                                } else {
                                    binding.peertubeDescriptionMore.setVisibility(View.GONE);
                                    show_more_content = null;
                                }
                            }
                        }
                    }
                };
                mainHandler.post(myRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (playInMinimized && player != null) {
                finishAndRemoveTask();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reportAlert(RetrofitPeertubeAPI.ActionType type, AlertDialog alertDialog) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(PeertubeActivity.this);
        LayoutInflater inflater1 = getLayoutInflater();
        View dialogView = inflater1.inflate(R.layout.popup_report_peertube, new LinearLayout(PeertubeActivity.this), false);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setNeutralButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        EditText report_content = dialogView.findViewById(R.id.report_content);
        dialogBuilder.setPositiveButton(R.string.report, (dialog, id) -> {
            if (report_content.getText().toString().trim().length() == 0) {
                Toasty.info(PeertubeActivity.this, getString(R.string.report_comment_size), Toasty.LENGTH_LONG).show();
            } else {
                PostActionsVM viewModel = new ViewModelProvider(PeertubeActivity.this).get(PostActionsVM.class);
                if (type == RetrofitPeertubeAPI.ActionType.REPORT_VIDEO) {
                    Report report = new Report();
                    Report.VideoReport videoReport = new Report.VideoReport();
                    videoReport.setId(peertube.getId());
                    report.setVideo(videoReport);
                    report.setReason(report_content.getText().toString());
                    viewModel.report(report).observe(PeertubeActivity.this, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.REPORT_VIDEO, 0, apiResponse));
                    alertDialog.dismiss();
                    dialog.dismiss();
                } else if (type == RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT) {
                    Report report = new Report();
                    Report.AccountReport accountReport = new Report.AccountReport();
                    accountReport.setId(peertube.getAccount().getId());
                    report.setAccount(accountReport);
                    report.setReason(report_content.getText().toString());
                    viewModel.report(report).observe(PeertubeActivity.this, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT, 0, apiResponse));
                    alertDialog.dismiss();
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog2 = dialogBuilder.create();
        alertDialog2.show();
    }


    public void manageCaptions(APIResponse apiResponse) {
        if (apiResponse == null || (apiResponse.getError() != null) || apiResponse.getCaptions() == null || apiResponse.getCaptions().size() == 0) {
            return;
        }
        captions = apiResponse.getCaptions();
    }

    public void manageNextVideos(APIResponse apiResponse) {
        if (apiResponse == null || apiResponse.getError() != null || apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0) {
            return;
        }
        List<VideoData.Video> suggestedVideos = apiResponse.getPeertubes();
        for (VideoData.Video video : suggestedVideos) {
            if (!playedVideos.contains(video.getId())) {
                TimelineVM feedsViewModel = new ViewModelProvider(PeertubeActivity.this).get(TimelineVM.class);
                feedsViewModel.getVideo(null, suggestedVideos.get(0).getUuid(), false).observe(PeertubeActivity.this, this::nextVideoDetails);
                return;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void nextVideoDetails(APIResponse apiResponse) {
        if (apiResponse == null || (apiResponse.getError() != null) || apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0) {
            return;
        }
        int i = 0;
        while (i < (apiResponse.getPeertubes().size() - 1) && playedVideos.contains(apiResponse.getPeertubes().get(i).getId())) {
            i++;
        }
        nextVideo = apiResponse.getPeertubes().get(i);
        if (!playedVideos.contains(nextVideo.getId()) && player != null && nextVideo.getFileUrl(null, PeertubeActivity.this) != null) {
            MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(nextVideo.getFileUrl(null, PeertubeActivity.this))).build();
            player.addMediaItem(mediaItem);
        }
    }


    public void manageVIewVideo(APIResponse apiResponse) {
        if (!isRemote && apiResponse != null && apiResponse.getPeertubes() != null && apiResponse.getPeertubes().get(0).getErrorCode() == 1 && apiResponse.getPeertubes().get(0).getOriginUrl() != null) {
            String url = apiResponse.getPeertubes().get(0).getOriginUrl();
            Pattern link = Pattern.compile("(https?://[\\da-z.-]+\\.[a-z.]{2,10})/videos/watch/(\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})(\\?start=(\\d+[hH])?(\\d+[mM])?(\\d+[sS])?)?$");
            Matcher matcherLink = link.matcher(url);
            if (matcherLink.find()) {
                String instance = matcherLink.group(1);
                String uuid = matcherLink.group(2);
                String hour = matcherLink.group(4);
                String min = matcherLink.group(5);
                String sec = matcherLink.group(6);
                int hourInt, minInt, secInt;
                int totalSeconds = 0;
                if (hour != null) {
                    hourInt = Integer.parseInt(hour.replace("h", ""));
                    totalSeconds += 3600 * hourInt;
                }
                if (min != null) {
                    minInt = Integer.parseInt(min.replace("m", ""));
                    totalSeconds += 60 * minInt;
                }
                if (sec != null) {
                    secInt = Integer.parseInt(sec.replace("strue", ""));
                    totalSeconds += secInt;
                }
                captionURI = null;
                captionLang = null;
                if (instance != null && uuid != null) {
                    peertubeInstance = instance.replace("https://", "").replace("http://", "");
                    sepiaSearch = true; // Sepia search flag is used because, at this time we don't know if the video is federated.
                    videoUuid = uuid;
                    peertube = new VideoData.Video();
                    peertube.setUuid(uuid);
                    peertube.setEmbedUrl(url);
                    isRemote = true;
                    if (totalSeconds > 0) {
                        VideoData.UserHistory userHistory = new VideoData.UserHistory();
                        userHistory.setCurrentTime(totalSeconds);
                        peertube.setUserHistory(userHistory);
                    }
                    TimelineVM viewModelTimeline = new ViewModelProvider(PeertubeActivity.this).get(TimelineVM.class);
                    viewModelTimeline.getVideo(peertubeInstance, peertube.getUuid(), false).observe(PeertubeActivity.this, this::manageVIewVideo);
                }
            }
            return;
        }

        if (apiResponse != null && apiResponse.getPeertubes() != null && apiResponse.getPeertubes().size() > 0 && apiResponse.getPeertubes().get(0).getErrorMessage() != null) {
            Toasty.error(PeertubeActivity.this, apiResponse.getPeertubes().get(0).getErrorMessage(), Toast.LENGTH_LONG).show();
            binding.loader.setVisibility(View.GONE);
            return;
        }
        if (apiResponse == null || (apiResponse.getError() != null) || apiResponse.getPeertubes() == null || apiResponse.getPeertubes().size() == 0) {
            Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            binding.loader.setVisibility(View.GONE);
            return;
        }
        if (apiResponse.getPeertubes() == null || apiResponse.getPeertubes().get(0) == null || (!apiResponse.getPeertubes().get(0).isLive() && apiResponse.getPeertubes().get(0).getFileUrl(null, PeertubeActivity.this) == null)) {
            Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            binding.loader.setVisibility(View.GONE);
            return;
        }
        long position = -1;

        long previousPositionHistory = 0;
        if (peertube != null && peertube.getUserHistory() != null) {
            previousPositionHistory = peertube.getUserHistory().getCurrentTime();
        }
        peertube = apiResponse.getPeertubes().get(0);
        VideoData.UserHistory userHistory = new VideoData.UserHistory();
        userHistory.setCurrentTime(previousPositionHistory);
        peertube.setUserHistory(userHistory);


        PlayerControlView controlView = binding.doubleTapPlayerView.findViewById(R.id.exo_controller);
        DefaultTimeBar exo_progress = controlView.findViewById(R.id.exo_progress);
        TextView exo_duration = controlView.findViewById(R.id.exo_duration);
        TextView exo_position = controlView.findViewById(R.id.exo_position);
        TextView exo_live_badge = controlView.findViewById(R.id.exo_live_badge);
        if (peertube.isLive()) {
            exo_progress.setVisibility(View.INVISIBLE);
            exo_duration.setVisibility(View.GONE);
            exo_live_badge.setVisibility(View.VISIBLE);
            exo_live_badge.setText(R.string.live);
            exo_live_badge.setBackgroundResource(R.drawable.rounded_live);
            exo_position.setVisibility(View.GONE);
        } else {
            exo_progress.setVisibility(View.VISIBLE);
            exo_live_badge.setVisibility(View.GONE);
            exo_position.setVisibility(View.VISIBLE);
            exo_duration.setBackground(null);
        }

        if (peertube.getUserHistory() != null) {
            position = peertube.getUserHistory().getCurrentTime() * 1000;
        }
        if (peertube.getTags() != null && peertube.getTags().size() > 0) {
            SearchVM searchViewModel = new ViewModelProvider(PeertubeActivity.this).get(SearchVM.class);
            searchViewModel.searchNextVideos(peertube.getTags()).observe(PeertubeActivity.this, this::manageNextVideos);
        }
        if (sepiaSearch) {
            peertubeInstance = peertube.getAccount().getHost();
        }
        List<String> videoIds = new ArrayList<>();
        videoIds.add(peertube.getId());
        PlaylistsVM viewModel = new ViewModelProvider(this).get(PlaylistsVM.class);
        viewModel.videoExists(videoIds).observe(this, this::manageVIewPlaylist);

        if (!Helper.canMakeAction() || sepiaSearch) {
            binding.writeCommentContainer.setVisibility(View.GONE);
        }


        if (peertube.isNsfw()) {
            binding.videoSensitive.setVisibility(View.VISIBLE);
        } else {
            binding.videoSensitive.setVisibility(View.INVISIBLE);
        }

        binding.peertubePlaylist.setOnClickListener(v -> {
            PlaylistsVM viewModelOwnerPlaylist = new ViewModelProvider(PeertubeActivity.this).get(PlaylistsVM.class);
            viewModelOwnerPlaylist.manage(PlaylistsVM.action.GET_PLAYLISTS, null, null).observe(PeertubeActivity.this, this::manageVIewPlaylists);
        });

        binding.videoInformation.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(PeertubeActivity.this);

            PopupVideoInfoPeertubeBinding videoInfo = PopupVideoInfoPeertubeBinding.inflate(getLayoutInflater());

            LinkedHashMap<Integer, String> privaciesInit = new LinkedHashMap<>(peertubeInformation.getPrivacies());
            videoInfo.infoPrivacy.setText(privaciesInit.get(peertube.getPrivacy().getId()));
            LinkedHashMap<Integer, String> licenseInit = new LinkedHashMap<>(peertubeInformation.getLicences());
            videoInfo.infoLicense.setText(licenseInit.get(peertube.getLicence().getId()));
            LinkedHashMap<String, String> languageStr = new LinkedHashMap<>(peertubeInformation.getLanguages());
            videoInfo.infoLanguage.setText(languageStr.get(peertube.getLanguage().getId()));
            LinkedHashMap<Integer, String> categoryInit = new LinkedHashMap<>(peertubeInformation.getCategories());
            videoInfo.infoCategory.setText(categoryInit.get(peertube.getCategory().getId()));

            if (peertube.isLive()) {
                videoInfo.infoDuration.setText(R.string.live);
                videoInfo.infoDuration.setBackgroundResource(R.drawable.rounded_live);
                videoInfo.infoDuration.setBackgroundResource(R.drawable.rounded_live);
            } else {
                videoInfo.infoDuration.setText(Helper.secondsToString(peertube.getDuration()));
            }

            String format = DateFormat.getDateInstance(DateFormat.LONG).format(peertube.getPublishedAt());
            videoInfo.infoPublishedAt.setText(format);
            List<String> tags = peertube.getTags();
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                sb.append("#").append(tag).append(" ");
            }

            SpannableString spannableString = new SpannableString(sb.toString());
            for (String tag : tags) {
                String target = "#" + tag;
                if (spannableString.toString().contains(target)) {
                    for (int startPosition = -1; (startPosition = spannableString.toString().indexOf(target, startPosition + 1)) != -1; startPosition++) {
                        final int endPosition = startPosition + target.length();
                        if (endPosition <= spannableString.toString().length() && endPosition >= startPosition) {
                            spannableString.setSpan(new ClickableSpan() {
                                                        @Override
                                                        public void onClick(@NonNull View textView) {
                                                            Intent intent = new Intent(PeertubeActivity.this, SearchActivity.class);
                                                            Bundle b = new Bundle();
                                                            String search = tag.trim();
                                                            b.putString("search", search);
                                                            intent.putExtras(b);
                                                            startActivity(intent);
                                                        }

                                                        @Override
                                                        public void updateDrawState(@NonNull TextPaint ds) {
                                                            super.updateDrawState(ds);
                                                            ds.setUnderlineText(false);
                                                            ds.setColor(getResources().getColor(R.color.colorAccent));
                                                        }
                                                    },
                                    startPosition, endPosition,
                                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }
            videoInfo.infoTags.setText(spannableString, TextView.BufferType.SPANNABLE);
            videoInfo.infoTags.setMovementMethod(LinkMovementMethod.getInstance());
            dialogBuilder.setView(videoInfo.getRoot());
            dialogBuilder.setNeutralButton(R.string.close, (dialog, id) -> dialog.dismiss());
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        });

        fetchComments();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        setTitle(peertube.getName());

        binding.peertubeDescription.setText(peertube.getDescription());


        binding.peertubeTitle.setText(peertube.getName());
        binding.peertubeDislikeCount.setText(Helper.withSuffix(peertube.getDislikes()));
        binding.peertubeLikeCount.setText(Helper.withSuffix(peertube.getLikes()));
        binding.peertubeViewCount.setText(Helper.withSuffix(peertube.getViews()));
        loadAvatar(PeertubeActivity.this, peertube.getChannel(), binding.ppChannel);
        binding.ppChannel.setOnClickListener(v -> {
            Intent intent = new Intent(PeertubeActivity.this, ShowChannelActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("channel", peertube.getChannel());
            b.putBoolean("sepia_search", sepiaSearch);
            if (sepiaSearch) {
                b.putString("peertube_instance", peertube.getAccount().getHost());
            }
            intent.putExtras(b);
            startActivity(intent);
        });

        video_id = peertube.getId();

        changeColor();
        initResolution();


        binding.peertubeLikeCount.setOnClickListener(v -> {
            if (isLoggedIn() && !sepiaSearch) {
                String newState = peertube.getMyRating().equals("like") ? "none" : "like";
                PostActionsVM viewModelLike = new ViewModelProvider(PeertubeActivity.this).get(PostActionsVM.class);
                viewModelLike.post(RATEVIDEO, peertube.getId(), newState).observe(PeertubeActivity.this, apiResponse1 -> manageVIewPostActions(RATEVIDEO, 0, apiResponse1));
                peertube.setMyRating(newState);
                int count = Integer.parseInt(binding.peertubeLikeCount.getText().toString());
                if (newState.compareTo("none") == 0) {
                    count--;
                    if (count - 1 < 0) {
                        count = 0;
                    }
                } else {
                    count++;
                }
                binding.peertubeLikeCount.setText(String.valueOf(count));
                changeColor();
            } else {
                if (sepiaSearch) {
                    Toasty.info(PeertubeActivity.this, getString(R.string.federation_issue), Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.error(PeertubeActivity.this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.peertubeReblog.setOnClickListener(v -> {
            if (status != null) {
                boolean confirmBoost = sharedpreferences.getBoolean(getString(R.string.SET_NOTIF_VALIDATION), true);
                StatusesVM statusesVM = new ViewModelProvider(PeertubeActivity.this).get(StatusesVM.class);
                if (confirmBoost) {
                    AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(this);
                    if (status.reblogged) {
                        alt_bld.setMessage(getString(R.string.reblog_remove));
                    } else {
                        alt_bld.setMessage(getString(R.string.reblog_add));
                    }
                    alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (status.reblogged) {
                            statusesVM.unReblog(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                    .observe(PeertubeActivity.this, _status -> {
                                        if (_status != null) {
                                            status = _status;
                                        }
                                        manageVIewPostActionsMastodon(status);
                                    });
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.reblog(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id, null)
                                    .observe(PeertubeActivity.this, _status -> {
                                        if (_status != null) {
                                            status = _status;
                                        }
                                        manageVIewPostActionsMastodon(status);
                                    });
                        }
                        dialog.dismiss();
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                } else {
                    if (status.reblogged) {
                        statusesVM.unReblog(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                .observe(PeertubeActivity.this, _status -> {
                                    if (_status != null) {
                                        status = _status;
                                    }
                                    manageVIewPostActionsMastodon(status);
                                });
                    } else {
                        ((SparkButton) v).playAnimation();
                        statusesVM.reblog(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id, null)
                                .observe(PeertubeActivity.this, _status -> {
                                    if (_status != null) {
                                        status = _status;
                                    }
                                    manageVIewPostActionsMastodon(status);
                                });
                    }
                }
            }
        });

        binding.peertubeFavorite.setOnClickListener(v -> {
            if (status != null) {
                boolean confirmFav = sharedpreferences.getBoolean(getString(R.string.SET_NOTIF_VALIDATION_FAV), false);
                StatusesVM statusesVM = new ViewModelProvider(PeertubeActivity.this).get(StatusesVM.class);
                if (confirmFav) {
                    AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(PeertubeActivity.this);
                    if (status.favourited) {
                        alt_bld.setMessage(getString(R.string.favourite_remove));
                    } else {
                        alt_bld.setMessage(getString(R.string.favourite_add));
                    }
                    alt_bld.setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (status.favourited) {
                            statusesVM.unFavourite(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                    .observe(PeertubeActivity.this, _status -> {
                                        if (_status != null) {
                                            status = _status;
                                        }
                                        manageVIewPostActionsMastodon(status);
                                    });
                        } else {
                            ((SparkButton) v).playAnimation();
                            statusesVM.favourite(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                    .observe(PeertubeActivity.this, _status -> {
                                        if (_status != null) {
                                            status = _status;
                                        }
                                        manageVIewPostActionsMastodon(status);
                                    });
                        }
                        dialog.dismiss();
                    });
                    alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    AlertDialog alert = alt_bld.create();
                    alert.show();
                } else {
                    if (status.favourited) {
                        statusesVM.unFavourite(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                .observe(PeertubeActivity.this, _status -> {
                                    if (_status != null) {
                                        status = _status;
                                    }
                                    manageVIewPostActionsMastodon(status);
                                });
                    } else {
                        ((SparkButton) v).playAnimation();
                        statusesVM.favourite(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                                .observe(PeertubeActivity.this, _status -> {
                                    if (_status != null) {
                                        status = _status;
                                    }
                                    manageVIewPostActionsMastodon(status);
                                });
                    }
                }
            }
        });

        binding.peertubeBookmark.setOnClickListener(v -> {
            if (status != null) {
                StatusesVM statusesVM = new ViewModelProvider(PeertubeActivity.this).get(StatusesVM.class);
                if (status.bookmarked) {
                    statusesVM.unBookmark(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                            .observe(PeertubeActivity.this, _status -> {
                                if (_status != null) {
                                    status = _status;
                                }
                                manageVIewPostActionsMastodon(status);
                            });
                } else {
                    ((SparkButton) v).playAnimation();
                    statusesVM.bookmark(HelperInstance.getLiveInstance(PeertubeActivity.this), HelperInstance.getToken(), status.id)
                            .observe(PeertubeActivity.this, _status -> {
                                if (_status != null) {
                                    status = _status;
                                }
                                manageVIewPostActionsMastodon(status);
                            });
                }
            }
        });

        binding.peertubeDislikeCount.setOnClickListener(v -> {
            if (isLoggedIn() && !sepiaSearch) {
                String newState = peertube.getMyRating().equals("dislike") ? "none" : "dislike";
                PostActionsVM viewModelLike = new ViewModelProvider(PeertubeActivity.this).get(PostActionsVM.class);
                viewModelLike.post(RATEVIDEO, peertube.getId(), newState).observe(PeertubeActivity.this, apiResponse1 -> manageVIewPostActions(RATEVIDEO, 0, apiResponse1));
                peertube.setMyRating(newState);
                int count = Integer.parseInt(binding.peertubeDislikeCount.getText().toString());
                if (newState.compareTo("none") == 0) {
                    count--;
                    if (count - 1 < 0) {
                        count = 0;
                    }
                } else {
                    count++;
                }
                binding.peertubeDislikeCount.setText(String.valueOf(count));
                changeColor();
            } else {
                if (sepiaSearch) {
                    Toasty.info(PeertubeActivity.this, getString(R.string.federation_issue), Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.error(PeertubeActivity.this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
                }
            }
        });


        if (mode != Helper.VIDEO_MODE_WEBVIEW) {

            player = new ExoPlayer.Builder(PeertubeActivity.this).build();
            player.addListener(this);
            binding.mediaVideo.player(player);
            binding.doubleTapPlayerView.setPlayer(player);
            binding.loader.setVisibility(View.GONE);
            startStream(
                    apiResponse.getPeertubes().get(0),
                    null,
                    autoPlay, position, null, null, true);
            player.prepare();
            player.setPlayWhenReady(autoPlay);
            if (autoPlay) {
                binding.doubleTapPlayerView.hideController();
            }
        }


        binding.moreActions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(PeertubeActivity.this, binding.moreActions);
            popup.getMenuInflater()
                    .inflate(R.menu.main_video_peertube, popup.getMenu());

            if (!isMyVideo) {
                popup.getMenu().findItem(R.id.action_edit).setVisible(false);
            }

            MenuItem itemDownload = popup.getMenu().findItem(R.id.action_download);
            itemDownload.setEnabled(peertube != null && peertube.isDownloadEnabled());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_download) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (ContextCompat.checkSelfPermission(PeertubeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(PeertubeActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(PeertubeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Helper.EXTERNAL_STORAGE_REQUEST_CODE);
                        } else {
                            Helper.manageDownloads(PeertubeActivity.this, peertube.getFileDownloadUrl(null));
                        }
                    } else {
                        Helper.manageDownloads(PeertubeActivity.this, peertube.getFileDownloadUrl(null));
                    }
                } else if (itemId == R.id.action_share) {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_via));
                    boolean share_details = sharedpreferences.getBoolean(Helper.SET_SHARE_DETAILS, true);
                    String extra_text;
                    if (share_details) {
                        extra_text = "@" + peertube.getAccount().getAcct();
                        extra_text += "\r\n\r\n" + peertube.getName();
                        extra_text += "\n\n\uD83D\uDD17 https://" + peertube.getChannel().getHost() + "/videos/watch/" + peertube.getUuid() + "\r\n-\n";
                        final String contentToot;
                        if (peertube.getDescription() != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                contentToot = Html.fromHtml(peertube.getDescription(), Html.FROM_HTML_MODE_LEGACY).toString();
                            else
                                contentToot = Html.fromHtml(peertube.getDescription()).toString();
                        } else {
                            contentToot = "";
                        }
                        extra_text += contentToot;
                    } else {
                        extra_text = "https://" + peertube.getChannel().getHost() + "/videos/watch/" + peertube.getUuid();
                    }
                    sendIntent.putExtra(Intent.EXTRA_TEXT, extra_text);
                    sendIntent.setType("text/plain");
                    try {
                        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_with)));
                    } catch (Exception e) {
                        Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                    }
                } else if (itemId == R.id.action_edit) {
                    Intent intent = new Intent(PeertubeActivity.this, PeertubeEditUploadActivity.class);
                    Bundle b = new Bundle();
                    b.putString("video_id", peertube.getUuid());
                    intent.putExtras(b);
                    startActivity(intent);
                } else if (itemId == R.id.action_report) {
                    AlertDialog alertDialog;
                    AlertDialog.Builder dialogBuilder;
                    dialogBuilder = new MaterialAlertDialogBuilder(PeertubeActivity.this);
                    LayoutInflater inflater1 = getLayoutInflater();
                    View dialogView = inflater1.inflate(R.layout.popup_report_choice, new LinearLayout(PeertubeActivity.this), false);
                    dialogBuilder.setView(dialogView);

                    dialogBuilder.setNeutralButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                    alertDialog = dialogBuilder.create();
                    alertDialog.show();
                    dialogView.findViewById(R.id.report_video).setOnClickListener(v -> reportAlert(REPORT_VIDEO, alertDialog));
                    dialogView.findViewById(R.id.report_account).setOnClickListener(v -> reportAlert(REPORT_ACCOUNT, alertDialog));
                }
                return true;
            });
            popup.show();
        });
    }


    public void manageVIewPostActionsMastodon(Status status) {
        if (status != null) {
            this.status = status;
            changeColorMastodon();
            binding.peertubeFavorite.setText(String.valueOf(status.favourites_count));
            binding.peertubeReblog.setText(String.valueOf(status.reblogs_count));
        } else {
            Toasty.error(PeertubeActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
        }
    }


    private void changeColorMastodon() {
        Drawable reblog = ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_repeat_24);
        Drawable favorite = ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_star_24);
        Drawable bookmark = ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_bookmark_24);

        int color = getAttColor(this, android.R.attr.colorControlNormal);

        if (reblog != null) {
            reblog.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(reblog, color);
        }
        if (favorite != null) {
            favorite.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(favorite, color);
        }

        if (bookmark != null) {
            bookmark.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(bookmark, color);
        }

        if (reblog != null && status.reblogged) {
            reblog.setColorFilter(getResources().getColor(R.color.boost_icon), PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(reblog, getResources().getColor(R.color.boost_icon));
        }
        if (favorite != null && status.favourited) {
            favorite.setColorFilter(getResources().getColor(R.color.marked_icon), PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(favorite, getResources().getColor(R.color.marked_icon));
        }

        if (bookmark != null && status.bookmarked) {
            bookmark.setColorFilter(getResources().getColor(R.color.marked_icon), PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(bookmark, getResources().getColor(R.color.marked_icon));
        }

        binding.peertubeReblog.setCompoundDrawablesWithIntrinsicBounds(null, reblog, null, null);
        binding.peertubeFavorite.setCompoundDrawablesWithIntrinsicBounds(null, favorite, null, null);
        binding.peertubeBookmark.setCompoundDrawablesWithIntrinsicBounds(null, bookmark, null, null);
    }

    /**
     * Manage video to play with different factors
     *
     * @param video      VideoData.Video
     * @param resolution String the current resolution asked
     * @param autoPlay   boolean
     * @param position   int current position
     * @param subtitles  Uri uri for subtitles
     * @param lang       String ("en","fr", etc.)
     */
    private void stream(VideoData.Video video, String resolution, boolean autoPlay, long position, Uri subtitles, String lang) {
        videoURL = video.getFileUrl(resolution, PeertubeActivity.this);
        if (subtitles != null) {
            subtitlesStr = subtitles.toString();
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        int video_cache = sharedpreferences.getInt(Helper.SET_VIDEO_CACHE, Helper.DEFAULT_VIDEO_CACHE_MB);
        ProgressiveMediaSource videoSource = null;
        HlsMediaSource hlsMediaSource = null;
        SingleSampleMediaSource subtitleSource = null;
        DataSource.Factory dataSourceFactory;
        if (video_cache == 0 || video.isLive()) {
            dataSourceFactory = new DefaultDataSourceFactory(PeertubeActivity.this,
                    Util.getUserAgent(PeertubeActivity.this, null), null);

            if (subtitles != null) {
                MediaItem.Subtitle mediaSubtitle = new MediaItem.Subtitle(subtitles, MimeTypes.TEXT_VTT, lang);
                subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(mediaSubtitle, C.TIME_UNSET);
            }
            MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(videoURL)).build();
            if (videoURL != null && !videoURL.endsWith("m3u8")) {
                videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            } else {
                hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem);
            }
        } else {
            CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(PeertubeActivity.this);
            MediaItem mediaItem = new MediaItem.Builder().setUri(videoURL).build();
            if (subtitles != null) {
                MediaItem.Subtitle mediaSubtitle = new MediaItem.Subtitle(subtitles, MimeTypes.TEXT_VTT, lang, Format.NO_VALUE);
                subtitleSource = new SingleSampleMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaSubtitle, C.TIME_UNSET);
            }
            if (videoURL != null && !videoURL.endsWith("m3u8")) {
                videoSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem);
            } else {
                hlsMediaSource = new HlsMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(mediaItem);
            }
        }
        if (lang != null && subtitleSource != null && videoSource != null) {
            MergingMediaSource mergedSource =
                    new MergingMediaSource(videoSource, subtitleSource);
            player.setMediaSource(mergedSource);
        } else if (lang != null && subtitleSource != null) {
            MergingMediaSource mergedSource =
                    new MergingMediaSource(hlsMediaSource, subtitleSource);
            player.setMediaSource(mergedSource);
        } else if (videoSource != null) {
            player.setMediaSource(videoSource);
        } else {
            player.setMediaSource(hlsMediaSource);
        }
        player.prepare();
        if (position > 0) {
            player.seekTo(0, position);
        }
        player.setPlayWhenReady(autoPlay);
        if (autoPlay) {
            binding.doubleTapPlayerView.hideController();
        }
        // loadCast(video, videoURL, subtitles!=null?subtitles.toString():null);
    }

    private void fetchComments() {
        if (peertube.isCommentsEnabled()) {
            if (Helper.canMakeAction()) {
                binding.postCommentButton.setVisibility(View.VISIBLE);
            } else {
                binding.postCommentButton.setVisibility(View.GONE);
            }
            CommentVM commentViewModel = new ViewModelProvider(PeertubeActivity.this).get(CommentVM.class);
            commentViewModel.getThread(sepiaSearch ? peertubeInstance : null, videoUuid, max_id).observe(PeertubeActivity.this, this::manageVIewComment);
            if (Helper.canMakeAction() && !sepiaSearch) {
                binding.writeCommentContainer.setVisibility(View.VISIBLE);
            }
            binding.peertubeComments.setVisibility(View.VISIBLE);
            binding.noAction.setVisibility(View.GONE);
        } else {
            binding.postCommentButton.setVisibility(View.GONE);
            binding.peertubeComments.setVisibility(View.GONE);
            binding.writeCommentContainer.setVisibility(View.GONE);
            binding.noActionText.setText(getString(R.string.comment_no_allowed_peertube));
            binding.noAction.setVisibility(View.VISIBLE);
            binding.writeCommentContainer.setVisibility(View.GONE);
        }
    }

    private void startStream(VideoData.Video video, String resolution, boolean autoPlay, long position, Uri subtitles, String lang, boolean promptNSFW) {

        String videoURL = peertube.getFileUrl(resolution, PeertubeActivity.this);
        if (peertube != null && peertube.isLive() && videoURL == null) {
            View parentLayout = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(parentLayout, R.string.live_not_started, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.close, view -> finish());
            snackbar.show();
            return;
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        String nsfwAction = sharedpreferences.getString(getString(R.string.set_video_sensitive_choice), Helper.BLUR);
        if (promptNSFW && peertube != null && peertube.isNsfw() && (nsfwAction.compareTo(Helper.BLUR) == 0 || nsfwAction.compareTo(Helper.DO_NOT_LIST) == 0)) {
            AlertDialog alertDialog;
            AlertDialog.Builder dialogBuilder;
            dialogBuilder = new MaterialAlertDialogBuilder(PeertubeActivity.this);
            dialogBuilder.setTitle(R.string.nsfw_title_warning);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setMessage(R.string.nsfw_message_warning);
            dialogBuilder.setNegativeButton(R.string.no, (dialog, id) -> {
                dialog.dismiss();
                finish();
            });
            dialogBuilder.setPositiveButton(R.string.play, (dialog, id) -> {
                stream(video, resolution, autoPlay, position, subtitles, lang);
                dialog.dismiss();
            });
            alertDialog = dialogBuilder.create();
            alertDialog.show();
        } else {
            stream(video, resolution, autoPlay, position, subtitles, lang);
        }


    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (binding.minController.castMiniController.getVisibility() == View.VISIBLE) {
            return;
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mode != Helper.VIDEO_MODE_WEBVIEW) {
                openFullscreenDialog();
            }
            if (initialOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        0,
                        4.0f
                );
                binding.videoContainer.setLayoutParams(param);
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mode != Helper.VIDEO_MODE_WEBVIEW) {
                closeFullscreenDialog();
            }
            if (initialOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1.0f
                );
                binding.videoContainer.setLayoutParams(param);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
        if (player != null) {
            player.release();
        }
        unregisterReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        onStopCalled = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        onStopCalled = false;
        if (player != null && !player.isPlaying() && binding.minController.castMiniController.getVisibility() != View.VISIBLE) {
            player.setPlayWhenReady(autoPlay);
            if (autoPlay) {
                binding.doubleTapPlayerView.hideController();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            updateHistory(player.getCurrentPosition() / 1000);
        }
        if (player != null && (!isPlayInMinimized || !playInMinimized)) {
            player.setPlayWhenReady(false);
        } else if (playInMinimized && binding.minController.castMiniController.getVisibility() != View.VISIBLE) {
            enterVideoMode();
        }
    }

    private void registBroadcastReceiver() {
        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        mPowerKeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();
                if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    if (player != null) {
                        if (!sharedpreferences.getBoolean(getString(R.string.set_play_screen_lock_choice), false)) {
                            player.setPlayWhenReady(false);
                        } else {
                            player.setWakeMode(C.WAKE_MODE_NETWORK);
                        }
                    }
                }
            }
        };
        getApplicationContext().registerReceiver(mPowerKeyReceiver, theFilter);
    }

    private void unregisterReceiver() {
        if (mPowerKeyReceiver != null) {
            getApplicationContext().unregisterReceiver(mPowerKeyReceiver);
            mPowerKeyReceiver = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onUserLeaveHint() {
        enterVideoMode();
    }

    private void enterVideoMode() {
        if (playInMinimized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && player != null) {
            isPlayInMinimized = true;
            setRequestedOrientationCustom(initialOrientation);
            MediaSessionCompat mediaSession = new MediaSessionCompat(this, getPackageName());
            MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
            mediaSessionConnector.setPlayer(player);
            PlayerControlView controlView = binding.doubleTapPlayerView.findViewById(R.id.exo_controller);
            controlView.hide();
            binding.doubleTapPlayerView.setControllerAutoShow(false);
            mediaSession.setActive(true);
            PictureInPictureParams params = new PictureInPictureParams.Builder().build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onBackPressed() {

        if (binding.videoParamsSubmenu.getVisibility() == View.VISIBLE) {
            closeSubMenuMenuOptions();
            return;
        }
        if (binding.videoParams.getVisibility() == View.VISIBLE) {
            closeMainMenuOptions();
            return;
        }
        if (binding.postComment.getVisibility() == View.VISIBLE) {
            closePostComment();
            return;
        }
        if (binding.replyThread.getVisibility() == View.VISIBLE) {
            closeCommentThread();
            return;
        }

        if (fullScreenMode && player != null && player.isPlaying()) {
            player.setPlayWhenReady(false);
            return;
        }

        if (playInMinimized && player != null) {
            enterVideoMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode) {
            if (onStopCalled) {
                isPlayInMinimized = false;
                finishAndRemoveTask();
            }
        }
    }

    private void toogleFullscreen(boolean fullscreen) {

        if (fullscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Objects.requireNonNull(getSupportActionBar()).hide();
            binding.bottomVideo.setVisibility(View.GONE);
            Objects.requireNonNull(getSupportActionBar()).hide();
            if (videoOrientationType == videoOrientation.LANDSCAPE) {
                if (getResources().getConfiguration().orientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientationCustom(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            Objects.requireNonNull(getSupportActionBar()).show();
            binding.bottomVideo.setVisibility(View.VISIBLE);
            Objects.requireNonNull(getSupportActionBar()).show();
        }
    }

    private void openFullscreenDialog() {
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_fullscreen_exit_24));
        fullScreenMode = true;
        toogleFullscreen(true);
    }

    private void closeFullscreenDialog() {
        fullScreenMode = false;
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_fullscreen_24));
        toogleFullscreen(false);
    }

    public void openCommentThread(Comment comment) {

        CommentVM commentViewModel = new ViewModelProvider(PeertubeActivity.this).get(CommentVM.class);
        binding.peertubeReply.setVisibility(View.GONE);
        commentViewModel.getRepliesComment(sepiaSearch ? peertubeInstance : null, videoUuid, comment.getId()).observe(PeertubeActivity.this, apiResponse -> manageVIewCommentReply(comment, apiResponse));

        binding.replyThread.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                binding.peertubeInformationContainer.getWidth(),
                0,
                0,
                0);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.peertubeInformationContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animate.setDuration(500);
        binding.replyThread.startAnimation(animate);
    }


    public void openMainMenuOptions() {
        binding.videoParams.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        binding.doubleTapPlayerView.hideController();
        List<MenuItemVideo> menuItemVideos = new ArrayList<>();
        if (peertube.getAllFile(PeertubeActivity.this) != null && peertube.getAllFile(PeertubeActivity.this).size() > 0) {
            MenuItemVideo resolutionItem = new MenuItemVideo();
            resolutionItem.setIcon(R.drawable.ic_baseline_high_quality_24);
            resolutionItem.setTitle(getString(R.string.pickup_resolution));
            resolutionItem.setAction(MenuItemVideo.actionType.RESOLUTION);
            menuItemVideos.add(resolutionItem);

        }
        MenuItemVideo speedItem = new MenuItemVideo();
        speedItem.setIcon(R.drawable.ic_baseline_speed_24);
        speedItem.setTitle(getString(R.string.playback_speed));
        speedItem.setAction(MenuItemVideo.actionType.SPEED);
        menuItemVideos.add(speedItem);

        if (captions != null) {
            MenuItemVideo captionItem = new MenuItemVideo();
            captionItem.setIcon(R.drawable.ic_baseline_subtitles_24);
            captionItem.setTitle(getString(R.string.captions));
            captionItem.setAction(MenuItemVideo.actionType.CAPTION);
            menuItemVideos.add(captionItem);
        }

        MenuItemVideo autoNextItem = new MenuItemVideo();
        autoNextItem.setIcon(R.drawable.ic_baseline_play_arrow_24);
        autoNextItem.setTitle(getString(R.string.set_autoplay_next_video_settings));
        autoNextItem.setAction(MenuItemVideo.actionType.AUTONEXT);
        menuItemVideos.add(autoNextItem);

        MenuAdapter menuAdapter = new MenuAdapter(menuItemVideos);
        binding.mainOptionsVideo.setAdapter(menuAdapter);
        menuAdapter.itemClicked = this;
        binding.mainOptionsVideo.setLayoutManager(new LinearLayoutManager(PeertubeActivity.this));

        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                height,
                0);
        animate.setDuration(500);
        binding.videoParams.startAnimation(animate);
    }

    @Override
    public void onItemClicked(MenuItemVideo.actionType action) {
        binding.videoParamsSubmenu.setVisibility(View.VISIBLE);
        List<MenuItemView> items = new ArrayList<>();
        switch (action) {
            case RESOLUTION:
                binding.subMenuTitle.setText(R.string.pickup_resolution);
                int position = 0;
                for (File file : peertube.getFiles()) {
                    if (file.getResolutions() != null) {
                        if (file.getResolutions().getLabel().compareTo("0p") != 0) {
                            MenuItemView item = new MenuItemView();
                            item.setId(position);
                            item.setLabel(file.getResolutions().getLabel());
                            if (file.getResolutions().getLabel().compareTo(currentResolution) == 0) {
                                item.setSelected(true);
                            }
                            items.add(item);
                            position++;
                        }
                    }
                }
                break;
            case SPEED:
                binding.subMenuTitle.setText(R.string.playback_speed);
                items = new ArrayList<>();
                items.add(new MenuItemView(25, "0.25x", player.getPlaybackParameters().speed == 0.25));
                items.add(new MenuItemView(50, "0.5x", player.getPlaybackParameters().speed == 0.5));
                items.add(new MenuItemView(75, "0.75x", player.getPlaybackParameters().speed == 0.75));
                items.add(new MenuItemView(100, getString(R.string.normal), player.getPlaybackParameters().speed == 1));
                items.add(new MenuItemView(125, "1.25x", player.getPlaybackParameters().speed == 1.25));
                items.add(new MenuItemView(150, "1.5x", player.getPlaybackParameters().speed == 1.5));
                items.add(new MenuItemView(175, "1.75x", player.getPlaybackParameters().speed == 1.75));
                items.add(new MenuItemView(200, "2x", player.getPlaybackParameters().speed == 2.0));
                break;
            case CAPTION:
                binding.subMenuTitle.setText(R.string.pickup_captions);
                items = new ArrayList<>();
                items.add(new MenuItemView(-1, "null", getString(R.string.none), currentCaption.compareTo("null") == 0));
                int i = 0;
                for (Caption caption : captions) {
                    items.add(new MenuItemView(i, caption.getLanguage().getId(), caption.getLanguage().getLabel(), currentCaption.compareTo(caption.getLanguage().getId()) == 0));
                }
                break;
            case AUTONEXT:
                binding.subMenuTitle.setText(R.string.set_autoplay_next_video_settings);
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
                boolean autoplayNextVideo = sharedpreferences.getBoolean(getString(R.string.set_autoplay_next_video_choice), true);
                items.add(new MenuItemView(0, getString(R.string.no), !autoplayNextVideo));
                items.add(new MenuItemView(1, getString(R.string.yes), autoplayNextVideo));
                break;
        }
        MenuItemAdapter menuItemAdapter = new MenuItemAdapter(action, items);
        binding.subMenuRecycler.setAdapter(menuItemAdapter);
        menuItemAdapter.itemAction = this;
        binding.subMenuRecycler.setLayoutManager(new LinearLayoutManager(PeertubeActivity.this));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                height,
                0);
        animate.setDuration(500);
        binding.videoParamsSubmenu.startAnimation(animate);
    }

    @Override
    public void which(MenuItemVideo.actionType action, MenuItemView item) {
        closeMainMenuOptions();
        switch (action) {
            case RESOLUTION:
                String res = item.getLabel();
                binding.loader.setVisibility(View.VISIBLE);
                long position = player.getCurrentPosition();
                PlayerControlView controlView = binding.doubleTapPlayerView.findViewById(R.id.exo_controller);
                TextView resolution = controlView.findViewById(R.id.resolution);
                currentResolution = res;
                resolution.setText(String.format("%s", res));
                if (mode == Helper.VIDEO_MODE_NORMAL) {
                    if (player != null)
                        player.release();
                    player = new ExoPlayer.Builder(PeertubeActivity.this).build();
                    binding.mediaVideo.player(player);
                    binding.doubleTapPlayerView.setPlayer(player);
                    binding.loader.setVisibility(View.GONE);

                    startStream(
                            peertube,
                            res,
                            true, position, captionURI, captionLang, false);
                }
                break;
            case SPEED:
                int speed = item.getId();
                float ratio = (float) speed / 100;
                PlaybackParameters param = new PlaybackParameters(ratio);
                if (player != null) {
                    player.setPlaybackParameters(param);
                }
                break;
            case CAPTION:
                Caption captionToUse = null;
                for (Caption caption : captions) {
                    if (caption.getLanguage().getId().compareTo(item.getStrId()) == 0) {
                        captionToUse = caption;
                        break;
                    }
                }
                if (captionToUse != null) {
                    if (!sepiaSearch) {
                        captionURI = Uri.parse("https://" + HelperInstance.getLiveInstance(PeertubeActivity.this) + captionToUse.getCaptionPath());
                    } else {
                        captionURI = Uri.parse("https://" + peertubeInstance + captionToUse.getCaptionPath());
                    }
                } else {
                    captionURI = null;
                }
                currentCaption = item.getStrId();
                long newPosition = player.getCurrentPosition();

                if (player != null)
                    player.release();

                TrackSelector trackSelector = new DefaultTrackSelector(PeertubeActivity.this, new AdaptiveTrackSelection.Factory());
                player = new ExoPlayer.Builder(PeertubeActivity.this).setTrackSelector(trackSelector).build();
                binding.mediaVideo.player(player);
                binding.doubleTapPlayerView.setPlayer(player);
                captionLang = item.getStrId();
                startStream(
                        peertube,
                        null,
                        true,
                        newPosition,
                        captionURI,
                        captionLang,
                        false
                );
                break;
            case AUTONEXT:
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(getString(R.string.set_autoplay_next_video_choice), item.getId() == 1);
                editor.apply();
                if (Helper.isLoggedIn()) {
                    new Thread(() -> {
                        UserSettings userSettings = new UserSettings();
                        userSettings.setAutoPlayNextVideo(item.getId() == 1);
                        try {
                            RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(PeertubeActivity.this);
                            api.updateUser(userSettings);
                        } catch (Exception | Error e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;
        }
        closeSubMenuMenuOptions();
    }

    public void closeMainMenuOptions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                height);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.videoParams.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animate.setDuration(500);
        binding.videoParams.startAnimation(animate);
    }


    public void closeSubMenuMenuOptions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                height);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.videoParamsSubmenu.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animate.setDuration(500);
        binding.videoParamsSubmenu.startAnimation(animate);
    }


    private void sendComment(Comment comment, int position) {
        if (Helper.canMakeAction() && !sepiaSearch) {
            if (comment == null) {
                String commentStr = binding.addCommentWrite.getText() != null ? binding.addCommentWrite.getText().toString() : "";
                if (commentStr.trim().length() > 0) {
                    if (Helper.isLoggedIn()) {
                        PostActionsVM viewModelComment = new ViewModelProvider(PeertubeActivity.this).get(PostActionsVM.class);
                        viewModelComment.comment(ADD_COMMENT, peertube.getId(), null, commentStr).observe(PeertubeActivity.this, apiResponse1 -> manageVIewPostActions(ADD_COMMENT, 0, apiResponse1));
                    }
                    binding.addCommentWrite.setText("");
                }
            } else {
                String commentView = binding.addCommentWrite.getText() != null ? binding.addCommentWrite.getText().toString() : "";
                if (commentView.trim().length() > 0) {
                    if (Helper.isLoggedIn()) {
                        PostActionsVM viewModelComment = new ViewModelProvider(PeertubeActivity.this).get(PostActionsVM.class);
                        viewModelComment.comment(REPLY, peertube.getId(), comment.getId(), commentView).observe(PeertubeActivity.this, apiResponse1 -> manageVIewPostActions(REPLY, position, apiResponse1));
                    }
                    binding.addCommentWrite.setText("");
                }
            }
            closePostComment();
        } else {
            if (sepiaSearch) {
                Toasty.info(PeertubeActivity.this, getString(R.string.federation_issue), Toasty.LENGTH_SHORT).show();
            } else {
                Toasty.error(PeertubeActivity.this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void closeCommentThread() {
        binding.peertubeInformationContainer.setVisibility(View.VISIBLE);
        hideKeyboard(this);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                binding.replyThread.getWidth(),
                0,
                0);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.replyThread.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animate.setDuration(500);
        binding.replyThread.startAnimation(animate);
    }


    public void openPostComment(Comment comment, int position) {
        if (comment != null) {
            binding.replyContent.setVisibility(View.VISIBLE);
            AccountData.PeertubeAccount account = comment.getAccount();
            loadAvatar(PeertubeActivity.this, account, binding.commentAccountProfile);
            binding.commentAccountDisplayname.setText(account.getDisplayName());
            binding.commentAccountUsername.setText(account.getAcct());
            Spanned commentSpan;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                commentSpan = Html.fromHtml(comment.getText(), Html.FROM_HTML_MODE_COMPACT);
            else
                commentSpan = Html.fromHtml(comment.getText());
            binding.commentContent.setText(commentSpan);
            binding.commentDate.setText(Helper.dateDiff(PeertubeActivity.this, comment.getCreatedAt()));
        } else {
            binding.replyContent.setVisibility(View.GONE);
        }
        binding.postComment.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        TranslateAnimation animateComment = new TranslateAnimation(
                0,
                0,
                height,
                0);
        animateComment.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.peertubeInformationContainer.setVisibility(View.GONE);
                InputMethodManager inputMethodManager =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(
                        binding.addCommentWrite.getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
                binding.addCommentWrite.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animateComment.setDuration(500);
        binding.postComment.startAnimation(animateComment);
        if (comment != null) {
            binding.addCommentWrite.setText(String.format("@%s ", comment.getAccount().getAcct()));
        } else {
            binding.addCommentWrite.setText(String.format("@%s ", peertube.getAccount().getAcct()));
        }
        binding.addCommentWrite.setSelection(binding.addCommentWrite.getText() != null ? binding.addCommentWrite.getText().length() : 0);

        binding.send.setOnClickListener(null);
        binding.send.setOnClickListener(v -> sendComment(comment, position));
    }

    private void closePostComment() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        binding.peertubeInformationContainer.setVisibility(View.VISIBLE);
        hideKeyboard(this);
        TranslateAnimation animateComment = new TranslateAnimation(
                0,
                0,
                0,
                height);
        animateComment.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.postComment.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animateComment.setDuration(500);
        binding.postComment.startAnimation(animateComment);
    }


    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, int position, APIResponse apiResponse) {

        if (peertube.isCommentsEnabled() && statusAction == ADD_COMMENT) {
            if (apiResponse.getComments() != null && apiResponse.getComments().size() > 0) {
                comments.add(0, apiResponse.getComments().get(0));
                commentListAdapter.notifyItemInserted(0);
            }
        } else if (peertube.isCommentsEnabled() && statusAction == REPLY) {
            if (apiResponse.getComments() != null && apiResponse.getComments().size() > 0) {
                commentsThread.add(position + 1, apiResponse.getComments().get(0));
                commentReplyListAdapter.notifyItemInserted(position + 1);
            }
        } else if (statusAction == RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT) {
            Toasty.success(PeertubeActivity.this, getString(R.string.successful_report), Toasty.LENGTH_LONG).show();
        } else if (statusAction == RetrofitPeertubeAPI.ActionType.REPORT_VIDEO) {
            Toasty.success(PeertubeActivity.this, getString(R.string.successful_video_report), Toasty.LENGTH_LONG).show();
        }
    }


    private void initControllerButtons() {

        PlayerControlView controlView = binding.doubleTapPlayerView.findViewById(R.id.exo_controller);
        if (controlView == null) {
            return;
        }
        fullScreenIcon = controlView.findViewById(R.id.exo_fullscreen_icon);
        View fullScreenButton = controlView.findViewById(R.id.exo_fullscreen_button);
        if (fullScreenButton != null) {
            fullScreenButton.setOnClickListener(v -> {
                if (!fullScreenMode) {
                    openFullscreenDialog();
                } else {
                    closeFullscreenDialog();
                    setRequestedOrientationCustom(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            });
        }
        ImageButton playButton = controlView.findViewById(R.id.exo_play);
        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                if (autoFullscreen && !fullScreenMode) {
                    openFullscreenDialog();
                }
                player.setPlayWhenReady(true);
            });
        }
        View exo_next = controlView.findViewById(R.id.exo_next);
        if (exo_next != null) {
            exo_next.setOnClickListener(v -> playNextVideo());
        }

        View exoSettings = controlView.findViewById(R.id.exo_settings);
        if (exoSettings != null) {
            exoSettings.setOnClickListener(v -> {
                if (binding.videoParams.getVisibility() == View.VISIBLE) {
                    closeMainMenuOptions();
                } else {
                    openMainMenuOptions();
                }
            });
        }

    }

    private void setRequestedOrientationCustom(int orientationCustom) {
        setRequestedOrientation(orientationCustom);
        Handler handler = new Handler();
        handler.postDelayed(() -> setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR), 2000);
    }

    private void initResolution() {
        PlayerControlView controlView = binding.doubleTapPlayerView.findViewById(R.id.exo_controller);
        TextView resolution = controlView.findViewById(R.id.resolution);
        if (Helper.defaultFile(PeertubeActivity.this, peertube.getFiles()) != null) {
            currentResolution = Helper.defaultFile(PeertubeActivity.this, peertube.getFiles()).getResolutions().getLabel();
            if (peertube.getFiles() != null && peertube.getFiles().size() > 0) {
                resolution.setText(String.format("%s", currentResolution));
            } else {
                resolution.setVisibility(View.GONE);
            }
        } else {
            resolution.setVisibility(View.GONE);
        }
    }


    private void changeColor() {

        Drawable thumbUp = ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_thumb_up_alt_24);
        Drawable thumbDown = ContextCompat.getDrawable(PeertubeActivity.this, R.drawable.ic_baseline_thumb_down_alt_24);
        int color = getAttColor(this, android.R.attr.colorControlNormal);

        if (thumbUp != null) {
            thumbUp.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(thumbUp, color);
        }
        if (thumbDown != null) {
            thumbDown.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            DrawableCompat.setTint(thumbDown, color);
        }
        if (peertube.getMyRating() != null && peertube.getMyRating().compareTo("like") == 0) {
            if (thumbUp != null) {
                thumbUp.setColorFilter(getAttColor(PeertubeActivity.this, R.attr.colorPrimary), PorterDuff.Mode.SRC_ATOP);
                DrawableCompat.setTint(thumbUp, getAttColor(PeertubeActivity.this, R.attr.colorPrimary));
            }
        } else if (peertube.getMyRating() != null && peertube.getMyRating().compareTo("dislike") == 0) {
            if (thumbDown != null) {
                thumbDown.setColorFilter(getAttColor(PeertubeActivity.this, R.attr.colorError), PorterDuff.Mode.SRC_ATOP);
                DrawableCompat.setTint(thumbDown, getAttColor(PeertubeActivity.this, R.attr.colorError));
            }
        }
        binding.peertubeLikeCount.setCompoundDrawablesWithIntrinsicBounds(null, thumbUp, null, null);
        binding.peertubeDislikeCount.setCompoundDrawablesWithIntrinsicBounds(null, thumbDown, null, null);
    }

    public void manageVIewPlaylists(APIResponse apiResponse) {
        if (apiResponse == null || apiResponse.getError() != null || playlists == null || peertube == null) {
            return;
        }
        if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(PeertubeActivity.this);
            builder.setTitle(R.string.modify_playlists);

            List<PlaylistData.Playlist> ownerPlaylists = apiResponse.getPlaylists();
            if (ownerPlaylists == null) {
                return;
            }
            String[] label = new String[ownerPlaylists.size()];
            boolean[] checked = new boolean[ownerPlaylists.size()];
            int i = 0;
            List<PlaylistExist> playlistsForVideo = playlists.get(peertube.getId());


            for (PlaylistData.Playlist playlist : ownerPlaylists) {
                checked[i] = false;
                if (playlistsForVideo != null) {
                    for (PlaylistExist playlistExist : playlistsForVideo) {
                        if (playlistExist != null && playlistExist.getPlaylistId().compareTo(playlist.getId()) == 0) {
                            checked[i] = true;
                            break;
                        }
                    }
                }
                label[i] = playlist.getDisplayName();
                i++;
            }

            builder.setMultiChoiceItems(label, checked, (dialog, which, isChecked) -> {
                PlaylistsVM playlistsViewModel = new ViewModelProvider(PeertubeActivity.this).get(PlaylistsVM.class);
                if (isChecked) { //Add to playlist
                    playlistsViewModel.manage(PlaylistsVM.action.ADD_VIDEOS, ownerPlaylists.get(which), peertube.getUuid()).observe(PeertubeActivity.this, apiResponse3 -> addElement(ownerPlaylists.get(which).getId(), peertube.getId(), apiResponse3));
                } else { //Remove from playlist
                    String elementInPlaylistId = null;
                    for (PlaylistExist playlistExist : peertube.getPlaylistExists()) {
                        if (playlistExist.getPlaylistId().compareTo(ownerPlaylists.get(which).getId()) == 0) {
                            elementInPlaylistId = playlistExist.getPlaylistElementId();
                        }
                    }
                    playlistsViewModel.manage(PlaylistsVM.action.DELETE_VIDEOS, ownerPlaylists.get(which), elementInPlaylistId);
                    playlists.remove(peertube.getId());
                }
            });
            builder.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void manageVIewPlaylist(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getVideoExistPlaylist() == null) {
            return;
        }
        if (playlists == null) {
            playlists = new HashMap<>();
        }
        playlists.putAll(apiResponse.getVideoExistPlaylist());
        peertube.setPlaylistExists(playlists.get(peertube.getId()));

    }

    public void addElement(String playlistId, String videoId, APIResponse apiResponse) {
        if (apiResponse != null && apiResponse.getActionReturn() != null) {

            PlaylistExist playlistExist = new PlaylistExist();
            playlistExist.setPlaylistId(playlistId);
            playlistExist.setPlaylistElementId(apiResponse.getActionReturn());
            List<PlaylistExist> playlistExistList = playlists.get(videoId);
            if (playlistExistList == null) {
                playlistExistList = new ArrayList<>();
            }
            playlistExistList.add(playlistExist);
            playlists.put(videoId, playlistExistList);
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (MotionEvent.ACTION_UP == event.getAction()) {
            Rect viewRectParams = new Rect();
            binding.videoParams.getGlobalVisibleRect(viewRectParams);
            if (binding.videoParams.getVisibility() == View.VISIBLE && !viewRectParams.contains((int) event.getRawX(), (int) event.getRawY())) {
                closeMainMenuOptions();
                if (binding.videoParamsSubmenu.getVisibility() == View.VISIBLE) {
                    closeSubMenuMenuOptions();
                }
            }
            Rect viewRectParamsSub = new Rect();
            binding.videoParamsSubmenu.getGlobalVisibleRect(viewRectParamsSub);
            if (binding.videoParamsSubmenu.getVisibility() == View.VISIBLE && !viewRectParamsSub.contains((int) event.getRawX(), (int) event.getRawY())) {
                closeSubMenuMenuOptions();
                if (binding.videoParams.getVisibility() == View.VISIBLE) {
                    closeMainMenuOptions();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void updateHistory(long position) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        boolean storeInHistory = sharedpreferences.getBoolean(getString(R.string.set_store_in_history), true);
        if (Helper.isLoggedIn() && peertube != null && storeInHistory) {
            new Thread(() -> {
                try {
                    RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(PeertubeActivity.this);
                    api.updateHistory(peertube.getUuid(), position);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void onAllCommentRemoved() {
        binding.noActionText.setVisibility(View.VISIBLE);
    }

    private void playNextVideo() {
        if (nextVideo != null) {
            Intent intent = new Intent(PeertubeActivity.this, PeertubeActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("video", nextVideo);
            b.putString("video_id", nextVideo.getId());
            b.putString("video_uuid", nextVideo.getUuid());
            playedVideos.add(nextVideo.getId());
            b.putBoolean("sepia_search", sepiaSearch);
            intent.putExtras(b);
            startActivity(intent);
        }
    }

    @Override
    public void onMediaItemTransition(MediaItem mediaItem, int reason) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(PeertubeActivity.this);
        boolean autoplayNextVideo = sharedpreferences.getBoolean(getString(R.string.set_autoplay_next_video_choice), true);
        if (reason == MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            player.removeMediaItems(0, player.getMediaItemCount());
            if (!sepiaSearch && autoplayNextVideo) {
                playNextVideo();
            }
        }
    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        Player.Listener.super.onVideoSizeChanged(videoSize);
        if (videoSize.width < videoSize.height) {
            videoOrientationType = videoOrientation.PORTRAIT;
        } else {
            videoOrientationType = videoOrientation.LANDSCAPE;
        }
    }


    @Override
    public void onPlayerError(PlaybackException error) {
        Player.Listener.super.onPlayerError(error);
    }

    enum videoOrientation {
        LANDSCAPE,
        PORTRAIT
    }
}
