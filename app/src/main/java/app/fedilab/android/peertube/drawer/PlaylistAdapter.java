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

import static app.fedilab.android.peertube.viewmodel.PlaylistsVM.action.GET_LIST_VIDEOS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import app.fedilab.android.peertube.BuildConfig;
import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.AllPlaylistsActivity;
import app.fedilab.android.peertube.activities.LocalPlaylistsActivity;
import app.fedilab.android.peertube.activities.MainActivity;
import app.fedilab.android.peertube.activities.PlaylistsActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.data.VideoPlaylistData;
import app.fedilab.android.peertube.databinding.DrawerPlaylistBinding;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.NotificationHelper;
import app.fedilab.android.peertube.helper.PlaylistExportHelper;
import app.fedilab.android.peertube.sqlite.ManagePlaylistsDAO;
import app.fedilab.android.peertube.sqlite.Sqlite;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import es.dmoral.toasty.Toasty;


public class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Playlist> playlists;
    private final boolean locale;
    public AllPlaylistRemoved allPlaylistRemoved;
    private Context context;

    public PlaylistAdapter(List<Playlist> lists, boolean locale) {
        this.playlists = lists;
        this.locale = locale;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerPlaylistBinding itemBinding = DrawerPlaylistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int position) {
        context = viewHolder.itemView.getContext();

        final ViewHolder holder = (ViewHolder) viewHolder;
        final Playlist playlist = playlists.get(position);
        String imgUrl;

        if (locale) {
            imgUrl = "https://" + playlist.getOwnerAccount().getHost() + playlist.getThumbnailPath();
        } else {
            imgUrl = playlist.getThumbnailPath();
        }
        Helper.loadGiF(context, imgUrl, holder.binding.previewPlaylist);

        holder.binding.previewTitle.setText(playlist.getDisplayName());
        if (playlist.getDescription() != null && playlist.getDescription().trim().compareTo("null") != 0 && playlist.getDescription().length() > 0) {
            holder.binding.previewDescription.setText(playlist.getDescription());
            holder.binding.previewDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.previewDescription.setVisibility(View.GONE);
        }
        holder.binding.previewVisibility.setText(playlist.getPrivacy().getLabel());

        holder.binding.playlistContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, locale ? LocalPlaylistsActivity.class : PlaylistsActivity.class);
            Bundle b = new Bundle();
            b.putParcelable("playlist", playlist);
            intent.putExtras(b);
            context.startActivity(intent);
        });

        if (playlist.getDisplayName().compareTo("Watch later") == 0) {
            holder.binding.playlistMore.setVisibility(View.GONE);
        } else {
            holder.binding.playlistMore.setVisibility(View.VISIBLE);
        }

        holder.binding.playlistMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.playlistMore);
            popup.getMenuInflater()
                    .inflate(R.menu.playlist_menu, popup.getMenu());
            if (!BuildConfig.full_instances) {
                popup.getMenu().findItem(R.id.action_export).setVisible(true);
            }
            if (locale) {
                popup.getMenu().findItem(R.id.action_export).setVisible(false);
                popup.getMenu().findItem(R.id.action_edit).setVisible(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getString(R.string.action_lists_delete) + ": " + playlist.getDisplayName());
                    builder.setMessage(context.getString(R.string.action_lists_confirm_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                playlists.remove(playlist);
                                notifyDataSetChanged();
                                if (!locale) {
                                    PlaylistsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PlaylistsVM.class);
                                    viewModel.manage(PlaylistsVM.action.DELETE_PLAYLIST, playlist, null).observe((LifecycleOwner) context, apiResponse -> manageVIewPlaylists(PlaylistsVM.action.DELETE_PLAYLIST, apiResponse));
                                } else {
                                    new Thread(() -> {
                                        SQLiteDatabase db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                                        new ManagePlaylistsDAO(context, db).removePlaylist(playlist.getUuid());
                                    }).start();
                                }
                                if (playlists.size() == 0) {
                                    allPlaylistRemoved.onAllPlaylistRemoved();
                                }
                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (itemId == R.id.action_edit) {
                    if (context instanceof AllPlaylistsActivity) {
                        ((AllPlaylistsActivity) context).manageAlert(playlist);
                    }
                } else if (itemId == R.id.action_export) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Helper.EXTERNAL_STORAGE_REQUEST_CODE);
                        } else {
                            doExport(playlist);
                        }
                    } else {
                        doExport(playlist);
                    }
                }
                return true;
            });
            popup.show();

        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    private void doExport(Playlist playlist) {
        new Thread(() -> {
            File file;
            RetrofitPeertubeAPI retrofitPeertubeAPI = new RetrofitPeertubeAPI(context);
            APIResponse apiResponse = retrofitPeertubeAPI.playlistAction(GET_LIST_VIDEOS, playlist.getId(), null, null, null);
            if (apiResponse != null) {
                List<VideoPlaylistData.VideoPlaylist> videos = apiResponse.getVideoPlaylist();
                VideoPlaylistData.VideoPlaylistExport videoPlaylistExport = new VideoPlaylistData.VideoPlaylistExport();
                videoPlaylistExport.setPlaylist(playlist);
                videoPlaylistExport.setUuid(playlist.getUuid());
                videoPlaylistExport.setAcct(MainActivity.userMe.getAccount().getAcct());
                videoPlaylistExport.setVideos(videos);

                String data = PlaylistExportHelper.playlistToStringStorage(videoPlaylistExport);


                File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "");

                if (!root.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    root.mkdirs();
                }
                String fileName = "playlist_" + playlist.getUuid() + ".tubelab";
                file = new File(root, fileName);
                FileWriter writer;
                try {
                    writer = new FileWriter(file);
                    writer.append(data);
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> Toasty.error(context, context.getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                    mainHandler.post(myRunnable);
                    return;
                }
                String urlAvatar = playlist.getThumbnailPath() != null ? HelperInstance.getLiveInstance(context) + playlist.getThumbnailPath() : null;
                FutureTarget<Bitmap> futureBitmapChannel = Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(urlAvatar != null ? urlAvatar : R.drawable.missing_peertube).submit();
                Bitmap icon = null;
                try {
                    icon = futureBitmapChannel.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                Intent mailIntent = new Intent(Intent.ACTION_SEND);
                mailIntent.setType("message/rfc822");
                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file);
                mailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_notification_subjet));
                mailIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.export_notification_body));
                mailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                mailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                NotificationHelper.notify_user(context.getApplicationContext(),
                        playlist.getOwnerAccount(), mailIntent, icon,
                        context.getString(R.string.export_notification_title),
                        context.getString(R.string.export_notification_content));
            }

        }).start();
    }


    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void manageVIewPlaylists(PlaylistsVM.action actionType, APIResponse apiResponse) {

    }

    public interface AllPlaylistRemoved {
        void onAllPlaylistRemoved();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerPlaylistBinding binding;

        ViewHolder(DrawerPlaylistBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }

}