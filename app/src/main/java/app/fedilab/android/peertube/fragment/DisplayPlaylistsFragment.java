package app.fedilab.android.peertube.fragment;
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

import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.DataType.MY_CHANNELS;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.app.Activity;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.activities.PlaylistsActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData.Account;
import app.fedilab.android.peertube.client.data.PlaylistData.Playlist;
import app.fedilab.android.peertube.client.entities.Item;
import app.fedilab.android.peertube.client.entities.PlaylistParams;
import app.fedilab.android.peertube.drawer.PlaylistAdapter;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.PlaylistsVM;
import es.dmoral.toasty.Toasty;


public class DisplayPlaylistsFragment extends Fragment {


    private Context context;
    private List<Playlist> playlists;
    private RelativeLayout mainLoader;
    private FloatingActionButton add_new;
    private PlaylistAdapter playlistAdapter;
    private RelativeLayout textviewNoAction;
    private HashMap<Integer, String> privacyToSend;
    private HashMap<String, String> channelToSend;
    private Spinner set_upload_channel;
    private Spinner set_upload_privacy;
    private HashMap<String, String> channels;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //View for fragment is the same that fragment accounts
        View rootView = inflater.inflate(R.layout.fragment_playlists, container, false);

        context = getContext();
        playlists = new ArrayList<>();


        RecyclerView lv_playlist = rootView.findViewById(R.id.lv_playlist);
        textviewNoAction = rootView.findViewById(R.id.no_action);
        mainLoader = rootView.findViewById(R.id.loader);
        RelativeLayout nextElementLoader = rootView.findViewById(R.id.loading_next_items);
        mainLoader.setVisibility(View.VISIBLE);
        nextElementLoader.setVisibility(View.GONE);
        playlists = new ArrayList<>();
        playlistAdapter = new PlaylistAdapter(playlists, false);
        lv_playlist.setAdapter(playlistAdapter);
        PlaylistsVM viewModel = new ViewModelProvider(this).get(PlaylistsVM.class);
        viewModel.manage(PlaylistsVM.action.GET_PLAYLISTS, null, null).observe(DisplayPlaylistsFragment.this.requireActivity(), apiResponse -> manageVIewPlaylists(PlaylistsVM.action.GET_PLAYLISTS, apiResponse));

        add_new = rootView.findViewById(R.id.add_new);

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

        if (add_new != null) {
            add_new.setOnClickListener(view -> {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                LayoutInflater inflater1 = ((Activity) context).getLayoutInflater();
                View dialogView = inflater1.inflate(R.layout.add_playlist, new LinearLayout(context), false);
                dialogBuilder.setView(dialogView);
                EditText display_name = dialogView.findViewById(R.id.display_name);
                EditText description = dialogView.findViewById(R.id.description);
                set_upload_channel = dialogView.findViewById(R.id.set_upload_channel);
                set_upload_privacy = dialogView.findViewById(R.id.set_upload_privacy);

                ChannelsVM viewModelC = new ViewModelProvider(this).get(ChannelsVM.class);
                viewModelC.get(MY_CHANNELS, null).observe(DisplayPlaylistsFragment.this.requireActivity(), this::manageVIewChannels);

                display_name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
                description.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});

                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {

                    if (display_name.getText() != null && display_name.getText().toString().trim().length() > 0) {

                        Playlist playlist = new Playlist();
                        playlist.setDisplayName(display_name.getText().toString().trim());
                        if (description.getText() != null && description.getText().toString().trim().length() > 0) {
                            playlist.setDescription(description.getText().toString().trim());
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
                            Toasty.error(context, context.getString(R.string.error_channel_mandatory), Toast.LENGTH_LONG).show();
                        } else {
                            if (privacyToSend != null) {
                                playlist.setPrivacy(privacyItem);
                            }
                            PlaylistParams playlistParams = new PlaylistParams();
                            playlistParams.setVideoChannelId(idChannel);
                            playlistParams.setDisplayName(playlist.getDisplayName());
                            playlistParams.setDescription(playlist.getDescription());
                            new Thread(() -> {
                                APIResponse apiResponse = new RetrofitPeertubeAPI(context).createOrUpdatePlaylist(PlaylistsVM.action.CREATE_PLAYLIST, null, playlistParams, null);
                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                Runnable myRunnable = () -> {
                                    if (getActivity() == null)
                                        return;
                                    playlist.setId(apiResponse.getActionReturn());
                                    playlists.add(0, playlist);
                                    playlistAdapter.notifyDataSetChanged();
                                };
                                mainHandler.post(myRunnable);
                                add_new.setEnabled(true);
                            }).start();

                            dialog.dismiss();
                            add_new.setEnabled(false);
                        }
                    } else {
                        Toasty.error(context, context.getString(R.string.error_display_name), Toast.LENGTH_LONG).show();
                    }

                });
                dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setTitle(getString(R.string.action_playlist_create));
                alertDialog.setOnDismissListener(dialogInterface -> {
                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    assert imm != null;
                    imm.hideSoftInputFromWindow(display_name.getWindowToken(), 0);
                });
                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                alertDialog.show();
            });
        }
        return rootView;
    }


    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }


    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void manageVIewPlaylists(PlaylistsVM.action actionType, APIResponse apiResponse) {
        mainLoader.setVisibility(View.GONE);
        add_new.setEnabled(true);
        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }

        if (actionType == PlaylistsVM.action.GET_PLAYLISTS) {
            if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
                this.playlists.addAll(apiResponse.getPlaylists());
                playlistAdapter.notifyDataSetChanged();
                textviewNoAction.setVisibility(View.GONE);
            } else {
                textviewNoAction.setVisibility(View.VISIBLE);
            }
        } else if (actionType == PlaylistsVM.action.CREATE_PLAYLIST) {
            if (apiResponse.getPlaylists() != null && apiResponse.getPlaylists().size() > 0) {
                Intent intent = new Intent(context, PlaylistsActivity.class);
                Bundle b = new Bundle();
                b.putParcelable("playlist", apiResponse.getPlaylists().get(0));
                intent.putExtras(b);
                context.startActivity(intent);
                this.playlists.add(0, apiResponse.getPlaylists().get(0));
                playlistAdapter.notifyDataSetChanged();
                textviewNoAction.setVisibility(View.GONE);
            } else {
                Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            }
        } else if (actionType == PlaylistsVM.action.DELETE_PLAYLIST) {
            if (this.playlists.size() == 0)
                textviewNoAction.setVisibility(View.VISIBLE);
        }
    }


    public void manageVIewChannels(APIResponse apiResponse) {
        if (apiResponse.getError() != null || apiResponse.getAccounts() == null || apiResponse.getAccounts().size() == 0) {
            if (apiResponse.getError() != null && apiResponse.getError().getError() != null)
                Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            else
                Toasty.error(context, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }

        //Populate channels
        List<Account> accounts = apiResponse.getAccounts();
        String[] channelName = new String[accounts.size() + 1];
        String[] channelId = new String[accounts.size() + 1];
        int i = 1;
        channelName[0] = "";
        channelId[0] = "";
        channels = new HashMap<>();
        for (Account account : accounts) {
            channels.put(account.getUsername(), account.getId());
            channelName[i] = account.getUsername();
            channelId[i] = account.getId();
            i++;
        }

        channelToSend = new HashMap<>();
        channelToSend.put(channelName[0], channelId[0]);
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, channelName);
        set_upload_channel.setAdapter(adapterChannel);

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

        ArrayAdapter<String> adapterPrivacies = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, privaciesA);
        set_upload_privacy.setAdapter(adapterPrivacies);

        //Manage privacies
        set_upload_privacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        set_upload_channel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
