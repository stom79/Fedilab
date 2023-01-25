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

import static app.fedilab.android.peertube.activities.PeertubeUploadActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityAllPlaylistPeertubeBinding;
import app.fedilab.android.databinding.AddPlaylistPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.entities.Item;
import app.fedilab.android.peertube.client.entities.PlaylistParams;
import app.fedilab.android.peertube.drawer.PlaylistAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import es.dmoral.toasty.Toasty;


public class AllPlaylistsActivity extends BaseBarActivity implements PlaylistAdapter.AllPlaylistRemoved {


    private static final int PICK_AVATAR = 467;
    PlaylistAdapter playlistAdapter;
    private HashMap<Integer, String> privacyToSend;
    private String idChannel;
    private List<Playlist> playlists;
    private Playlist playlistToEdit;
    private List<ChannelData.Channel> myChannels;
    private ChannelData.Channel selectedChannel;
    private AddPlaylistPeertubeBinding bindingDialog;
    private Uri inputData;
    private ActivityAllPlaylistPeertubeBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllPlaylistPeertubeBinding.inflate(getLayoutInflater());
        View viewRoot = binding.getRoot();
        setContentView(viewRoot);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.playlists);


        binding.loader.setVisibility(View.VISIBLE);
        binding.loadingNextItems.setVisibility(View.GONE);
        idChannel = null;

        PlaylistsVM viewModel = new ViewModelProvider(AllPlaylistsActivity.this).get(PlaylistsVM.class);
        viewModel.manage(PlaylistsVM.action.GET_PLAYLISTS, null, null).observe(AllPlaylistsActivity.this, apiResponse -> manageVIewPlaylists(PlaylistsVM.action.GET_PLAYLISTS, apiResponse));


        LinkedHashMap<Integer, String> privaciesInit = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        if (privaciesInit.size() > 0) {
            Map.Entry<Integer, String> entryInt = privaciesInit.entrySet().iterator().next();
            privacyToSend = new HashMap<>();
            privacyToSend.put(entryInt.getKey(), entryInt.getValue());
        }


        playlists = new ArrayList<>();
        playlistAdapter = new PlaylistAdapter(playlists, false);
        playlistAdapter.allPlaylistRemoved = this;
        binding.lvPlaylist.setAdapter(playlistAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(AllPlaylistsActivity.this);
        binding.lvPlaylist.setLayoutManager(mLayoutManager);
        binding.addNew.setOnClickListener(view -> manageAlert(null));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void manageVIewPlaylists(PlaylistsVM.action actionType, APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(AllPlaylistsActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        if (actionType == PlaylistsVM.action.GET_PLAYLISTS) {
            if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
                playlists.addAll(apiResponse.getPlaylists());
                playlistAdapter.notifyDataSetChanged();
                binding.noAction.setVisibility(View.GONE);
            } else {
                binding.noAction.setVisibility(View.VISIBLE);
            }
        }
    }

    public void manageAlert(Playlist playlistParam) {

        playlistToEdit = playlistParam;
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(AllPlaylistsActivity.this, app.fedilab.android.mastodon.helper.Helper.dialogStyle());
        bindingDialog = AddPlaylistPeertubeBinding.inflate(LayoutInflater.from(AllPlaylistsActivity.this), null, false);
        dialogBuilder.setView(bindingDialog.getRoot());

        dialogBuilder.setView(bindingDialog.getRoot());


        ChannelsVM viewModelC = new ViewModelProvider(AllPlaylistsActivity.this).get(ChannelsVM.class);
        viewModelC.get(RetrofitPeertubeAPI.DataType.MY_CHANNELS, null).observe(AllPlaylistsActivity.this, this::manageVIewChannels);

        bindingDialog.displayName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        bindingDialog.description.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});

        if (playlistToEdit != null) {
            bindingDialog.displayName.setText(playlistToEdit.getDisplayName());
            bindingDialog.description.setText(playlistToEdit.getDescription());
        }
        dialogBuilder.setPositiveButton(R.string.validate, null);
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();

        bindingDialog.selectFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(AllPlaylistsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AllPlaylistsActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {"image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, PICK_AVATAR);
        });
        Helper.loadGiF(AllPlaylistsActivity.this, playlistParam != null ? playlistParam.getThumbnailPath() : null, bindingDialog.profilePicture);
        alertDialog.setOnShowListener(dialogInterface -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                if (bindingDialog.displayName.getText() != null && bindingDialog.displayName.getText().toString().trim().length() > 0) {
                    PlaylistParams playlistElement = new PlaylistParams();
                    playlistElement.setDisplayName(bindingDialog.displayName.getText().toString().trim());
                    if (bindingDialog.description.getText() != null && bindingDialog.description.getText().toString().trim().length() > 0) {
                        playlistElement.setDescription(bindingDialog.description.getText().toString().trim());
                    }
                    playlistElement.setVideoChannelId(idChannel);
                    String label;
                    Map.Entry<Integer, String> privacyM = privacyToSend.entrySet().iterator().next();
                    Item privacyItem = new Item();
                    privacyItem.setId(privacyM.getKey());
                    privacyItem.setLabel(privacyM.getValue());
                    label = privacyM.getValue();
                    if ((label.trim().compareTo("Public") == 0 && (playlistElement.getVideoChannelId() == null || playlistElement.getVideoChannelId().trim().compareTo("null") == 0))) {
                        Toasty.error(AllPlaylistsActivity.this, getString(R.string.error_channel_mandatory), Toast.LENGTH_LONG).show();
                    } else {
                        if (privacyToSend != null) {
                            playlistElement.setPrivacy(privacyItem.getId());
                        }
                        new Thread(() -> {
                            String playlistId;
                            if (playlistToEdit == null) {
                                APIResponse apiResponse = new RetrofitPeertubeAPI(AllPlaylistsActivity.this).createOrUpdatePlaylist(PlaylistsVM.action.CREATE_PLAYLIST, null, playlistElement, inputData);
                                playlistId = apiResponse.getActionReturn();
                            } else {
                                playlistId = playlistToEdit.getId();
                                new RetrofitPeertubeAPI(AllPlaylistsActivity.this).createOrUpdatePlaylist(PlaylistsVM.action.UPDATE_PLAYLIST, playlistId, playlistElement, inputData);
                            }
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> {
                                Playlist playlist;
                                if (playlistToEdit == null) {
                                    playlist = new Playlist();
                                } else {
                                    playlist = playlistToEdit;
                                }
                                playlist.setId(playlistId);
                                playlist.setUuid(playlistId);
                                playlist.setDescription(playlistElement.getDescription());
                                playlist.setDisplayName(playlistElement.getDisplayName());
                                playlist.setVideoChannel(selectedChannel);
                                playlist.setPrivacy(privacyItem);
                                if (playlistToEdit == null) {
                                    playlists.add(playlist);
                                }
                                playlistAdapter.notifyDataSetChanged();
                            };
                            mainHandler.post(myRunnable);
                        }).start();
                        alertDialog.dismiss();
                    }
                } else {
                    Toasty.error(AllPlaylistsActivity.this, getString(R.string.error_display_name), Toast.LENGTH_LONG).show();
                }
            });
        });

        if (playlistToEdit == null) {
            alertDialog.setTitle(getString(R.string.action_playlist_create));
        } else {
            alertDialog.setTitle(getString(R.string.action_playlist_edit));
        }
        alertDialog.setOnDismissListener(dialogInterface -> {
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(bindingDialog.displayName.getWindowToken(), 0);
        });
        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(AllPlaylistsActivity.this, getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
                return;
            }
            inputData = data.getData();
            Glide.with(AllPlaylistsActivity.this)
                    .load(inputData)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(bindingDialog.profilePicture);

        }
    }

    public void manageVIewChannels(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getChannels() == null || apiResponse.getChannels().size() == 0) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(AllPlaylistsActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(AllPlaylistsActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }

        //Populate channels
        myChannels = apiResponse.getChannels();
        String[] channelName = new String[myChannels.size() + 1];
        String[] channelId = new String[myChannels.size() + 1];
        int i = 1;
        channelName[0] = "";
        channelId[0] = "null";

        for (ChannelData.Channel channel : myChannels) {
            channelName[i] = channel.getName();
            channelId[i] = channel.getId();
            i++;
        }

        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(AllPlaylistsActivity.this,
                android.R.layout.simple_spinner_dropdown_item, channelName);
        bindingDialog.setUploadChannel.setAdapter(adapterChannel);


        LinkedHashMap<String, String> translations = null;
        if (peertubeInformation.getTranslations() != null)
            translations = new LinkedHashMap<>(peertubeInformation.getTranslations());

        LinkedHashMap<Integer, String> privaciesInit = new LinkedHashMap<>(peertubeInformation.getPlaylistPrivacies());
        Map.Entry<Integer, String> entryInt = privaciesInit.entrySet().iterator().next();
        privacyToSend = new HashMap<>();
        privacyToSend.put(entryInt.getKey(), entryInt.getValue());
        LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPlaylistPrivacies());
        //Populate privacies
        String[] privaciesA = new String[privacies.size()];
        Iterator<Map.Entry<Integer, String>> it = privacies.entrySet().iterator();
        i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = it.next();
            if (translations == null || translations.size() == 0 || !translations.containsKey(pair.getValue()))
                privaciesA[i] = pair.getValue();
            else
                privaciesA[i] = translations.get(pair.getValue());
            it.remove();
            i++;
        }

        ArrayAdapter<String> adapterPrivacies = new ArrayAdapter<>(AllPlaylistsActivity.this,
                android.R.layout.simple_spinner_dropdown_item, privaciesA);
        bindingDialog.setUploadPrivacy.setAdapter(adapterPrivacies);

        if (playlistToEdit != null) {
            Item privacy = playlistToEdit.getPrivacy();
            if (privacy.getId() > 0) {
                bindingDialog.setUploadPrivacy.setSelection(privacy.getId() - 1);
            }
        } else {
            bindingDialog.setUploadPrivacy.setSelection(2);
        }

        //Manage privacies
        bindingDialog.setUploadPrivacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinkedHashMap<Integer, String> privaciesCheck = new LinkedHashMap<>(peertubeInformation.getPrivacies());
                Iterator<Map.Entry<Integer, String>> it = privaciesCheck.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Map.Entry<Integer, String> pair = it.next();
                    if (i == position) {
                        privacyToSend = new HashMap<>();
                        privacyToSend.put(pair.getKey(), pair.getValue());
                        break;
                    }
                    it.remove();
                    i++;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (playlistToEdit != null) {
            Item privacy = playlistToEdit.getPrivacy();

            if (privacy.getId() > 0) {
                bindingDialog.setUploadPrivacy.setSelection(privacy.getId() - 1);
            }
        }

        //Manage languages
        bindingDialog.setUploadChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idChannel = channelId[position];
                if (position > 0) {
                    selectedChannel = myChannels.get(position - 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (playlistToEdit != null) {
            int position = 0;
            int k = 1;
            for (ChannelData.Channel ac : myChannels) {
                if (playlistToEdit.getVideoChannel() != null && playlistToEdit.getVideoChannel().getId() != null && ac.getId().compareTo(playlistToEdit.getVideoChannel().getId()) == 0) {
                    position = k;
                    break;
                }
                k++;
            }
            bindingDialog.setUploadChannel.setSelection(position);
        }
    }

    @Override
    public void onAllPlaylistRemoved() {
        binding.noAction.setVisibility(View.VISIBLE);
    }
}
