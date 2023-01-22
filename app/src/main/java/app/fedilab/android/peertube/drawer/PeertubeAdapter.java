package app.fedilab.android.peertube.drawer;
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

import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.FOLLOW;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.UNFOLLOW;
import static app.fedilab.android.peertube.viewmodel.TimelineVM.TimelineType.MY_VIDEOS;
import static app.fedilab.android.peertube.viewmodel.TimelineVM.TimelineType.SEPIA_SEARCH;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.PeertubeActivity;
import app.fedilab.android.peertube.activities.PeertubeEditUploadActivity;
import app.fedilab.android.peertube.activities.ShowChannelActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.PlaylistData;
import app.fedilab.android.peertube.client.data.VideoData;
import app.fedilab.android.peertube.client.entities.PlaylistExist;
import app.fedilab.android.peertube.client.entities.Report;
import app.fedilab.android.peertube.databinding.DrawerPeertubeBinding;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;
import jp.wasabeef.glide.transformations.BlurTransformation;


public class PeertubeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<VideoData.Video> videos;
    public RelationShipListener relationShipListener;
    public PlaylistListener playlistListener;
    private Context context;
    private TimelineVM.TimelineType timelineType;
    private boolean sepiaSearch;
    private ChannelData.Channel forChannel;
    private AccountData.Account forAccount;

    public PeertubeAdapter(List<VideoData.Video> videos, TimelineVM.TimelineType timelineType, boolean sepiaSearch, ChannelData.Channel forChannel, AccountData.Account forAccount) {
        this.videos = videos;
        this.timelineType = timelineType;
        this.sepiaSearch = sepiaSearch || timelineType == SEPIA_SEARCH;
        this.forChannel = forChannel;
        this.forAccount = forAccount;
    }


    public PeertubeAdapter(List<VideoData.Video> videos) {
        this.videos = videos;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerPeertubeBinding itemBinding = DrawerPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {


        final ViewHolder holder = (ViewHolder) viewHolder;
        final VideoData.Video video = videos.get(position);

        if (video == null) {
            return;
        }
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        String userId = sharedpreferences.getString(Helper.PREF_KEY_ID, "");

        assert userId != null;

        boolean videoInList = sharedpreferences.getBoolean(context.getString(R.string.set_video_in_list_choice), false);

        boolean ownVideos;
        if (timelineType == TimelineVM.TimelineType.MY_VIDEOS) {
            ownVideos = true;
        } else {
            ownVideos = Helper.isVideoOwner(context, video);
        }


        String instance = null;
        if (sepiaSearch) {
            instance = video.getAccount().getHost();
        } else if (forChannel != null) {
            instance = forChannel.getHost();
        } else if (forAccount != null) {
            instance = forAccount.getHost();
        }


        holder.binding.peertubeAccountName.setText(video.getChannel().getAcct());
        Helper.loadAvatar(context, video.getChannel(), holder.binding.peertubeProfile);
        holder.binding.peertubeTitle.setText(video.getName());
        if (video.isLive()) {
            holder.binding.peertubeDuration.setText(R.string.live);
            holder.binding.peertubeDuration.setBackgroundResource(R.drawable.rounded_live);
        } else {
            holder.binding.peertubeDuration.setText(Helper.secondsToString(video.getDuration()));
            holder.binding.peertubeDuration.setBackgroundResource(R.drawable.rounded_corner);
        }


        holder.binding.peertubeDate.setText(String.format(" - %s", Helper.dateDiff(context, video.getCreatedAt())));
        holder.binding.peertubeViews.setText(context.getString(R.string.number_view_video, Helper.withSuffix(video.getViews())));

        boolean blur = sharedpreferences.getString(context.getString(R.string.set_video_sensitive_choice), Helper.BLUR).compareTo("blur") == 0 && video.isNsfw();
        if (videoInList) {
            Helper.loadGiF(context, instance, video.getThumbnailPath(), holder.binding.peertubeVideoImageSmall, blur);
            holder.binding.peertubeVideoImageSmall.setVisibility(View.VISIBLE);
            holder.binding.previewContainer.setVisibility(View.GONE);
        } else {
            loadImage(holder.binding.peertubeVideoImage, instance, video.getPreviewPath(), video.getThumbnailPath(), blur);
            holder.binding.peertubeVideoImageSmall.setVisibility(View.GONE);
            holder.binding.previewContainer.setVisibility(View.VISIBLE);
        }

        //For Overview Videos: boolean values for displaying title is managed in the fragment
        if (video.isHasTitle()) {
            holder.binding.headerTitle.setVisibility(View.VISIBLE);
            switch (video.getTitleType()) {
                case TAG:
                    holder.binding.headerTitle.setText(String.format("#%s", video.getTitle()));
                    break;
                case CHANNEL:
                case CATEGORY:
                    holder.binding.headerTitle.setText(String.format("%s", video.getTitle()));
                    break;
            }
        } else {
            holder.binding.headerTitle.setVisibility(View.GONE);
        }

        if (!ownVideos) {
            holder.binding.peertubeProfile.setOnClickListener(v -> {
                Intent intent = new Intent(context, ShowChannelActivity.class);
                Bundle b = new Bundle();
                b.putParcelable("channel", video.getChannel());
                b.putBoolean("sepia_search", sepiaSearch || forChannel != null);
                if (sepiaSearch || forChannel != null) {
                    b.putString("peertube_instance", video.getAccount().getHost());
                }
                intent.putExtras(b);
                context.startActivity(intent);
            });
        }


        holder.binding.moreActions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.moreActions);
            popup.getMenuInflater()
                    .inflate(R.menu.video_drawer_menu, popup.getMenu());
            if (timelineType == MY_VIDEOS) {
                popup.getMenu().findItem(R.id.action_report).setVisible(false);
                popup.getMenu().findItem(R.id.action_follow).setVisible(false);
            } else {
                popup.getMenu().findItem(R.id.action_edit).setVisible(false);
                if (relationShipListener == null || relationShipListener.getRelationShip() == null || relationShipListener.getRelationShip().size() == 0) {
                    popup.getMenu().findItem(R.id.action_follow).setVisible(false);
                } else {
                    popup.getMenu().findItem(R.id.action_follow).setVisible(true);
                    if (relationShipListener.getRelationShip().containsKey(video.getChannel().getAcct()) && relationShipListener.getRelationShip().get(video.getChannel().getAcct())) {
                        popup.getMenu().findItem(R.id.action_follow).setTitle(context.getString(R.string.action_unfollow));
                    } else {
                        popup.getMenu().findItem(R.id.action_follow).setTitle(context.getString(R.string.action_follow));
                    }
                }
            }
            popup.getMenu().findItem(R.id.action_playlist).setVisible(playlistListener != null && playlistListener.getPlaylist() != null && playlistListener.getPlaylist().size() != 0);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_follow) {
                    if (relationShipListener.getRelationShip().containsKey(video.getChannel().getAcct()) && relationShipListener.getRelationShip().get(video.getChannel().getAcct())) {
                        relationShipListener.getRelationShip().put(video.getChannel().getAcct(), false);
                        popup.getMenu().findItem(R.id.action_follow).setTitle(context.getString(R.string.action_follow));
                        boolean confirm_unfollow = sharedpreferences.getBoolean(Helper.SET_UNFOLLOW_VALIDATION, true);
                        if (confirm_unfollow) {
                            AlertDialog.Builder unfollowConfirm = new AlertDialog.Builder(context);
                            unfollowConfirm.setTitle(context.getString(R.string.unfollow_confirm));
                            unfollowConfirm.setMessage(video.getChannel().getAcct());
                            unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                            unfollowConfirm.setPositiveButton(R.string.yes, (dialog, which) -> {
                                PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                                viewModel.post(UNFOLLOW, video.getChannel().getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(UNFOLLOW, apiResponse));
                                dialog.dismiss();
                            });
                            unfollowConfirm.show();
                        } else {
                            PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                            viewModel.post(UNFOLLOW, video.getChannel().getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(UNFOLLOW, apiResponse));
                        }
                    } else {
                        relationShipListener.getRelationShip().put(video.getChannel().getAcct(), true);
                        popup.getMenu().findItem(R.id.action_follow).setTitle(context.getString(R.string.action_unfollow));
                        PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                        viewModel.post(FOLLOW, video.getChannel().getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(FOLLOW, apiResponse));
                    }
                } else if (itemId == R.id.action_playlist) {
                    PlaylistsVM viewModelOwnerPlaylist = new ViewModelProvider((ViewModelStoreOwner) context).get(PlaylistsVM.class);
                    viewModelOwnerPlaylist.manage(PlaylistsVM.action.GET_PLAYLISTS, null, null).observe((LifecycleOwner) context, apiResponse -> manageVIewPlaylists(video, apiResponse));
                } else if (itemId == R.id.action_edit) {
                    Intent intent = new Intent(context, PeertubeEditUploadActivity.class);
                    Bundle b = new Bundle();
                    b.putString("video_id", video.getUuid());
                    intent.putExtras(b);
                    context.startActivity(intent);
                } else if (itemId == R.id.action_report) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    LayoutInflater inflater1 = ((Activity) context).getLayoutInflater();
                    View dialogView = inflater1.inflate(R.layout.popup_report, new LinearLayout(context), false);
                    dialogBuilder.setView(dialogView);
                    EditText report_content = dialogView.findViewById(R.id.report_content);
                    dialogBuilder.setNeutralButton(R.string.cancel, (dialog2, id) -> dialog2.dismiss());
                    dialogBuilder.setPositiveButton(R.string.report, (dialog2, id) -> {
                        if (report_content.getText().toString().trim().length() == 0) {
                            Toasty.info(context, context.getString(R.string.report_comment_size), Toasty.LENGTH_LONG).show();
                        } else {
                            PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                            Report report = new Report();
                            Report.VideoReport videoReport = new Report.VideoReport();
                            videoReport.setId(video.getId());
                            report.setVideo(videoReport);
                            report.setReason(report_content.getText().toString());
                            viewModel.report(report).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.REPORT_VIDEO, apiResponse));
                            dialog2.dismiss();
                        }
                    });
                    AlertDialog alertDialog2 = dialogBuilder.create();
                    alertDialog2.show();
                }
                return true;
            });
            popup.show();
        });
        holder.binding.bottomContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, PeertubeActivity.class);
            Bundle b = new Bundle();
            b.putString("video_id", video.getId());
            b.putString("video_uuid", video.getUuid());
            b.putBoolean("isMyVideo", ownVideos);
            b.putBoolean("sepia_search", sepiaSearch);
            b.putParcelable("video", video);
            if (sepiaSearch) {
                b.putString("peertube_instance", video.getAccount().getHost());
            }
            intent.putExtras(b);
            context.startActivity(intent);
        });
        holder.binding.peertubeVideoImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, PeertubeActivity.class);
            Bundle b = new Bundle();
            b.putString("video_id", video.getId());
            b.putParcelable("video", video);
            b.putString("video_uuid", video.getUuid());
            b.putBoolean("isMyVideo", ownVideos);
            b.putBoolean("sepia_search", sepiaSearch);
            if (sepiaSearch) {
                b.putString("peertube_instance", video.getAccount().getHost());
            }
            intent.putExtras(b);
            context.startActivity(intent);
        });

    }


    public void manageVIewPlaylists(VideoData.Video video, APIResponse apiResponse) {
        if (apiResponse.getError() != null) {
            return;
        }
        if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.modify_playlists);

            List<PlaylistData.Playlist> ownerPlaylists = apiResponse.getPlaylists();
            if (ownerPlaylists == null) {
                return;
            }
            String[] label = new String[ownerPlaylists.size()];
            boolean[] checked = new boolean[ownerPlaylists.size()];
            int i = 0;
            List<PlaylistExist> playlistsForVideo = playlistListener.getPlaylist().get(video.getId());


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
                PlaylistsVM playlistsViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PlaylistsVM.class);
                if (isChecked) { //Add to playlist
                    playlistsViewModel.manage(PlaylistsVM.action.ADD_VIDEOS, ownerPlaylists.get(which), video.getUuid()).observe((LifecycleOwner) context, apiResponse3 -> addElement(ownerPlaylists.get(which).getId(), video.getId(), apiResponse3));
                } else { //Remove from playlist
                    String elementInPlaylistId = null;
                    for (PlaylistExist playlistExist : video.getPlaylistExists()) {
                        if (playlistExist.getPlaylistId().compareTo(ownerPlaylists.get(which).getId()) == 0) {
                            elementInPlaylistId = playlistExist.getPlaylistElementId();
                        }
                    }
                    playlistsViewModel.manage(PlaylistsVM.action.DELETE_VIDEOS, ownerPlaylists.get(which), elementInPlaylistId);
                    playlistListener.getPlaylist().remove(video.getId());
                }
            });
            builder.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void addElement(String playlistId, String videoId, APIResponse apiResponse) {
        if (apiResponse != null && apiResponse.getActionReturn() != null) {

            PlaylistExist playlistExist = new PlaylistExist();
            playlistExist.setPlaylistId(playlistId);
            playlistExist.setPlaylistElementId(apiResponse.getActionReturn());
            List<PlaylistExist> playlistExistList = playlistListener.getPlaylist().get(videoId);
            if (playlistExistList == null) {
                playlistExistList = new ArrayList<>();
            }
            playlistExistList.add(playlistExist);
            playlistListener.getPlaylist().put(videoId, playlistExistList);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, APIResponse apiResponse) {
        if (statusAction == RetrofitPeertubeAPI.ActionType.REPORT_VIDEO) {
            Toasty.success(context, context.getString(R.string.successful_video_report), Toasty.LENGTH_LONG).show();
        } else if (statusAction == RetrofitPeertubeAPI.ActionType.UNFOLLOW) {
            Bundle b = new Bundle();
            b.putString("receive_action", apiResponse.getTargetedId());
            Intent intentBC = new Intent(Helper.RECEIVE_ACTION);
            intentBC.putExtras(b);
        }
    }

    @SuppressLint("CheckResult")
    private void loadImage(ImageView target, String instance, String urlPreview, String thumbnail, boolean blur) {
        if (urlPreview == null || urlPreview.startsWith("null")) {
            urlPreview = thumbnail;
        }
        if (instance != null) {
            urlPreview = "https://" + instance + urlPreview;
            thumbnail = "https://" + instance + thumbnail;
        } else {
            urlPreview = "https://" + HelperInstance.getLiveInstance(context) + urlPreview;
            thumbnail = "https://" + HelperInstance.getLiveInstance(context) + thumbnail;
        }
        RequestBuilder<Drawable> requestBuilder = Glide.with(context)
                .asDrawable();
        if (blur) {
            requestBuilder.apply(new RequestOptions().transform(new BlurTransformation(50, 3), new CenterCrop(), new RoundedCorners(10)));
        } else {
            requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)));
        }
        final Drawable[] initialResource = new Drawable[1];
        String finalUrlPreview = urlPreview;
        requestBuilder.load(thumbnail).into(
                new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull final Drawable resource, Transition<? super Drawable> transition) {
                        target.setImageDrawable(resource);
                        initialResource[0] = resource;
                        RequestBuilder<Drawable> requestBuilder = Glide.with(context)
                                .asDrawable();
                        if (blur) {
                            requestBuilder.apply(new RequestOptions().transform(new BlurTransformation(50, 3), new CenterCrop(), new RoundedCorners(10)));
                        } else {
                            requestBuilder.apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)));
                        }
                        requestBuilder.load(finalUrlPreview).into(
                                new CustomTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull final Drawable resource, Transition<? super Drawable> transition) {
                                        target.setImageDrawable(resource);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                    }

                                    @Override
                                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                        target.setImageDrawable(initialResource[0]);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                }
        );

    }

    public interface RelationShipListener {
        Map<String, Boolean> getRelationShip();
    }

    public interface PlaylistListener {
        Map<String, List<PlaylistExist>> getPlaylist();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerPeertubeBinding binding;

        ViewHolder(DrawerPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}