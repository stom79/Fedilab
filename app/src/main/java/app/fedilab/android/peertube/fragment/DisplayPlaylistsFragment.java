package app.fedilab.android.peertube.fragment;
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

import static app.fedilab.android.mastodon.helper.Helper.dialogStyle;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.DataType.MY_CHANNELS;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.databinding.AddPlaylistPeertubeBinding;
import app.fedilab.android.databinding.FragmentPlaylistsPeertubeBinding;
import app.fedilab.android.peertube.activities.PlaylistsActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.entities.Item;
import app.fedilab.android.peertube.client.entities.PlaylistParams;
import app.fedilab.android.peertube.drawer.PlaylistAdapter;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import es.dmoral.toasty.Toasty;


public class DisplayPlaylistsFragment extends Fragment {


    private List<Playlist> playlists;
    private PlaylistAdapter playlistAdapter;
    private HashMap<Integer, String> privacyToSend;
    private HashMap<String, String> channelToSend;
    private HashMap<String, String> channels;
    private FragmentPlaylistsPeertubeBinding binding;
    private AddPlaylistPeertubeBinding bindingAlert;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //View for fragment is the same that fragment accounts
        binding = FragmentPlaylistsPeertubeBinding.inflate(getLayoutInflater());

        playlists = new ArrayList<>();

        binding.loader.setVisibility(View.VISIBLE);
        binding.loadingNextItems.setVisibility(View.GONE);
        playlists = new ArrayList<>();
        playlistAdapter = new PlaylistAdapter(playlists, false);
        binding.lvPlaylist.setAdapter(playlistAdapter);
        PlaylistsVM viewModel = new ViewModelProvider(this).get(PlaylistsVM.class);
        viewModel.manage(PlaylistsVM.action.GET_PLAYLISTS, null, null).observe(DisplayPlaylistsFragment.this.requireActivity(), apiResponse -> manageVIewPlaylists(PlaylistsVM.action.GET_PLAYLISTS, apiResponse));


        LinkedHashMap<Integer, String> privaciesInit = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        Map.Entry<Integer, String> entryInt = privaciesInit.entrySet().iterator().next();
        privacyToSend = new HashMap<>();
        privacyToSend.put(entryInt.getKey(), entryInt.getValue());
        LinkedHashMap<Integer, String> privacies = new LinkedHashMap<>(peertubeInformation.getPrivacies());
        //Populate privacies
        Iterator<Map.Entry<Integer, String>> it = privacies.entrySet().iterator();
        while (it.hasNext()) {
            it.remove();
        }

        binding.addNew.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireActivity(), dialogStyle());
            bindingAlert = AddPlaylistPeertubeBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(bindingAlert.getRoot());

            ChannelsVM viewModelC = new ViewModelProvider(this).get(ChannelsVM.class);
            viewModelC.get(MY_CHANNELS, null).observe(DisplayPlaylistsFragment.this.requireActivity(), this::manageVIewChannels);

            bindingAlert.displayName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
            bindingAlert.description.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
            dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                if (bindingAlert.displayName.getText() != null && bindingAlert.displayName.getText().toString().trim().length() > 0) {

                    Playlist playlist = new Playlist();
                    playlist.setDisplayName(bindingAlert.displayName.getText().toString().trim());
                    if (bindingAlert.description.getText() != null && bindingAlert.description.getText().toString().trim().length() > 0) {
                        playlist.setDescription(bindingAlert.description.getText().toString().trim());
                    }
                    String idChannel = null;
                    if (channelToSend != null) {
                        Map.Entry<String, String> channelM = channelToSend.entrySet().iterator().next();
                        idChannel = channelM.getValue();
                    }
                    Map.Entry<Integer, String> privacyM = privacyToSend.entrySet().iterator().next();
                    Item privacyItem = new Item();
                    privacyItem.setLabel(privacyM.getValue());
                    privacyItem.setId(privacyM.getKey());
                    if (privacyItem.getLabel().equals("Public") && (playlist.getVideoChannel() == null)) {
                        Toasty.error(requireActivity(), getString(R.string.error_channel_mandatory), Toast.LENGTH_LONG).show();
                    } else {
                        if (privacyToSend != null) {
                            playlist.setPrivacy(privacyItem);
                        }
                        PlaylistParams playlistParams = new PlaylistParams();
                        playlistParams.setVideoChannelId(idChannel);
                        playlistParams.setDisplayName(playlist.getDisplayName());
                        playlistParams.setDescription(playlist.getDescription());
                        new Thread(() -> {
                            APIResponse apiResponse = new RetrofitPeertubeAPI(requireActivity()).createOrUpdatePlaylist(PlaylistsVM.action.CREATE_PLAYLIST, null, playlistParams, null);
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> {
                                if (getActivity() == null)
                                    return;
                                playlist.setId(apiResponse.getActionReturn());
                                playlists.add(0, playlist);
                                playlistAdapter.notifyDataSetChanged();
                            };
                            mainHandler.post(myRunnable);
                            binding.addNew.setEnabled(true);
                        }).start();

                        dialog.dismiss();
                        binding.addNew.setEnabled(false);
                    }
                } else {
                    Toasty.error(requireActivity(), getString(R.string.error_display_name), Toast.LENGTH_LONG).show();
                }

            });
            dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setTitle(getString(R.string.action_playlist_create));
            alertDialog.setOnDismissListener(dialogInterface -> {
                //Hide keyboard
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(bindingAlert.displayName.getWindowToken(), 0);
            });
            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
        });
        return binding.getRoot();
    }


    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }


    public void manageVIewPlaylists(PlaylistsVM.action actionType, APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        binding.addNew.setEnabled(true);
        if (apiResponse.getError() != null) {
            Toasty.error(requireActivity(), apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }

        if (actionType == PlaylistsVM.action.GET_PLAYLISTS) {
            if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
                this.playlists.addAll(apiResponse.getPlaylists());
                playlistAdapter.notifyDataSetChanged();
                binding.noAction.setVisibility(View.GONE);
            } else {
                binding.noAction.setVisibility(View.VISIBLE);
            }
        } else if (actionType == PlaylistsVM.action.CREATE_PLAYLIST) {
            if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
                Intent intent = new Intent(requireActivity(), PlaylistsActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("playlist", apiResponse.getPlaylists().get(0));
                intent.putExtras(b);
                startActivity(intent);
                this.playlists.add(0, apiResponse.getPlaylists().get(0));
                playlistAdapter.notifyDataSetChanged();
                binding.noAction.setVisibility(View.GONE);
            } else {
                Toasty.error(requireActivity(), apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            }
        } else if (actionType == PlaylistsVM.action.DELETE_PLAYLIST) {
            if (this.playlists.size() == 0)
                binding.noAction.setVisibility(View.VISIBLE);
        }
    }


    public void manageVIewChannels(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getAccounts() == null || apiResponse.getAccounts().size() == 0) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(requireActivity(), apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(requireActivity(), getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }

        //Populate channels
        List<AccountData.PeertubeAccount> accounts = apiResponse.getAccounts();
        String[] channelName = new String[accounts.size() + 1];
        String[] channelId = new String[accounts.size() + 1];
        int i = 1;
        channelName[0] = "";
        channelId[0] = "";
        channels = new HashMap<>();
        for (AccountData.PeertubeAccount account : accounts) {
            channels.put(account.getUsername(), account.getId());
            channelName[i] = account.getUsername();
            channelId[i] = account.getId();
            i++;
        }

        channelToSend = new HashMap<>();
        channelToSend.put(channelName[0], channelId[0]);
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, channelName);
        bindingAlert.setUploadChannel.setAdapter(adapterChannel);

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

        ArrayAdapter<String> adapterPrivacies = new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, privaciesA);
        bindingAlert.setUploadPrivacy.setAdapter(adapterPrivacies);

        //Manage privacies
        bindingAlert.setUploadPrivacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        //Manage languages
        bindingAlert.setUploadChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinkedHashMap<String, String> channelsCheck = new LinkedHashMap<>(channels);
                Iterator<Map.Entry<String, String>> it = channelsCheck.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    if (i == position) {
                        channelToSend = new HashMap<>();
                        channelToSend.put(pair.getKey(), pair.getValue());
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
    }
}
