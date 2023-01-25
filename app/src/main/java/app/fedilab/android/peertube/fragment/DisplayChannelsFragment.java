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
import static app.fedilab.android.peertube.activities.PeertubeUploadActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.AddChannelPeertubeBinding;
import app.fedilab.android.databinding.FragmentRecyclerviewPeertubeBinding;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData;
import app.fedilab.android.peertube.client.entities.ChannelParams;
import app.fedilab.android.peertube.drawer.ChannelListAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.SearchVM;
import es.dmoral.toasty.Toasty;


public class DisplayChannelsFragment extends Fragment implements ChannelListAdapter.AllChannelRemoved, ChannelListAdapter.EditAlertDialog {

    private static final int PICK_AVATAR = 467;
    private Context context;
    private ChannelListAdapter channelListAdapter;
    private List<ChannelData.Channel> channels;
    private String name;
    private FloatingActionButton action_button;
    private FragmentRecyclerviewPeertubeBinding binding;
    private AddChannelPeertubeBinding bindingDialog;
    private Uri inputData;
    private String search_peertube;
    private String max_id;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentRecyclerviewPeertubeBinding.inflate(LayoutInflater.from(context));
        Bundle bundle = this.getArguments();
        channels = new ArrayList<>();
        max_id = "0";
        if (bundle != null) {
            name = bundle.getString("name", null);
            search_peertube = bundle.getString("search_peertube", null);
        }

        if (getActivity() != null) {
            action_button = getActivity().findViewById(R.id.action_button);
            if (action_button != null) {
                action_button.setVisibility(View.VISIBLE);
                action_button.setOnClickListener(view -> manageAlert(null));
            }
        }

        binding.lvElements.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        channelListAdapter = new ChannelListAdapter(this.channels);
        channelListAdapter.allChannelRemoved = this;
        channelListAdapter.editAlertDialog = this;
        binding.lvElements.setAdapter(channelListAdapter);

        final LinearLayoutManager mLayoutManager;
        mLayoutManager = new LinearLayoutManager(context);
        binding.lvElements.setLayoutManager(mLayoutManager);

        binding.swipeContainer.setOnRefreshListener(this::pullToRefresh);

        loadChannels(max_id);
        return binding.getRoot();
    }

    private void loadChannels(String max_id) {
        if (search_peertube == null) {
            ChannelsVM viewModel = new ViewModelProvider(this).get(ChannelsVM.class);
            if (name != null) {
                viewModel.get(RetrofitPeertubeAPI.DataType.CHANNELS_FOR_ACCOUNT, name).observe(DisplayChannelsFragment.this.requireActivity(), this::manageViewChannels);
            } else {
                viewModel.get(RetrofitPeertubeAPI.DataType.MY_CHANNELS, null).observe(DisplayChannelsFragment.this.requireActivity(), this::manageViewChannels);
            }
        } else {
            SearchVM viewModelSearch = new ViewModelProvider(this).get(SearchVM.class);
            viewModelSearch.getChannels(max_id, search_peertube).observe(this.requireActivity(), this::manageViewChannels);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(context, getString(R.string.toot_select_image_error), Toast.LENGTH_LONG).show();
                return;
            }
            inputData = data.getData();
            Glide.with(context)
                    .load(inputData)
                    .thumbnail(0.1f)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(10)))
                    .into(bindingDialog.profilePicture);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity() != null) {
            View action_button = getActivity().findViewById(R.id.action_button);
            if (action_button != null) {
                action_button.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void scrollToTop() {
        binding.lvElements.setAdapter(channelListAdapter);
    }

    private void manageViewChannels(APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        binding.loadingNext.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            binding.swipeContainer.setRefreshing(false);
            return;
        }
        List<ChannelData.Channel> channels = apiResponse.getChannels();

        if ((channels == null || channels.size() == 0))
            binding.noAction.setVisibility(View.VISIBLE);
        else
            binding.noAction.setVisibility(View.GONE);
        max_id = String.valueOf(Integer.parseInt(max_id) + 20);
        if (channels != null && channels.size() > 0) {
            int currentPosition = this.channels.size();
            this.channels.addAll(channels);
            if (currentPosition == 0) {
                channelListAdapter = new ChannelListAdapter(this.channels);
                channelListAdapter.allChannelRemoved = DisplayChannelsFragment.this;
                channelListAdapter.editAlertDialog = DisplayChannelsFragment.this;
                binding.lvElements.setAdapter(channelListAdapter);
            } else {
                channelListAdapter.notifyItemRangeChanged(currentPosition, channels.size());
            }
        }
        binding.swipeContainer.setRefreshing(false);
    }

    public void pullToRefresh() {
        channels = new ArrayList<>();
        binding.swipeContainer.setRefreshing(true);
        loadChannels("0");
    }

    @Override
    public void onAllChannelRemoved() {
        binding.noAction.setVisibility(View.VISIBLE);
    }


    public void manageAlert(ChannelParams oldChannelValues) {


        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(context, dialogStyle());
        bindingDialog = AddChannelPeertubeBinding.inflate(LayoutInflater.from(context), null, false);
        dialogBuilder.setView(bindingDialog.getRoot());

        if (oldChannelValues != null) {
            bindingDialog.displayName.setText(oldChannelValues.getDisplayName());
            bindingDialog.name.setText(oldChannelValues.getName());
            bindingDialog.description.setText(oldChannelValues.getDescription());
            bindingDialog.name.setEnabled(false);
        }
        dialogBuilder.setPositiveButton(R.string.validate, null);
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        int position;
        if (oldChannelValues == null) {
            position = 0;
        } else {
            position = 0;
            for (ChannelData.Channel channel : channels) {
                if (channel.getName().compareTo(oldChannelValues.getName()) == 0) {
                    break;
                }
                position++;
            }
        }

        bindingDialog.selectFile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context,
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
        Helper.loadAvatar(context, channels.get(position), bindingDialog.profilePicture);
        int finalPosition = position;
        alertDialog.setOnShowListener(dialogInterface -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                if (bindingDialog.displayName.getText() != null && bindingDialog.displayName.getText().toString().trim().length() > 0 && bindingDialog.name.getText() != null && bindingDialog.name.getText().toString().trim().length() > 0) {

                    ChannelParams channelCreation = new ChannelParams();
                    channelCreation.setDisplayName(bindingDialog.displayName.getText().toString().trim());
                    channelCreation.setName(bindingDialog.name.getText().toString().trim());
                    if (bindingDialog.description.getText() != null && bindingDialog.description.getText().toString().trim().length() > 0) {
                        channelCreation.setDescription(bindingDialog.description.getText().toString().trim());
                    }
                    new Thread(() -> {
                        APIResponse apiResponse;
                        if (oldChannelValues == null) {
                            apiResponse = new RetrofitPeertubeAPI(context).createOrUpdateChannel(ChannelsVM.action.CREATE_CHANNEL, null, channelCreation, inputData);
                        } else {
                            apiResponse = new RetrofitPeertubeAPI(context).createOrUpdateChannel(ChannelsVM.action.UPDATE_CHANNEL, channelCreation.getName() + "@" + HelperInstance.getLiveInstance(context), channelCreation, inputData);
                        }
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        Runnable myRunnable = () -> {

                            if (oldChannelValues == null) {
                                ChannelData.Channel channel = new ChannelData.Channel();
                                channel.setId(apiResponse.getActionReturn());
                                channel.setName(channelCreation.getName());
                                channel.setDisplayName(channelCreation.getDisplayName());
                                channel.setDescription(channelCreation.getDescription());
                                channels.add(0, channel);
                                channelListAdapter.notifyItemInserted(0);
                            } else {
                                channels.get(finalPosition).setName(channelCreation.getName());
                                channels.get(finalPosition).setDisplayName(channelCreation.getDisplayName());
                                channels.get(finalPosition).setDescription(channelCreation.getDescription());
                                channelListAdapter.notifyItemChanged(finalPosition);
                            }
                            if (action_button != null) {
                                action_button.setEnabled(true);
                            }
                        };
                        mainHandler.post(myRunnable);
                    }).start();
                    alertDialog.dismiss();
                    if (action_button != null) {
                        action_button.setEnabled(false);
                    }
                } else {
                    Toasty.error(context, context.getString(R.string.error_display_name_channel), Toast.LENGTH_LONG).show();
                }
            });
        });
        if (oldChannelValues == null) {
            alertDialog.setTitle(getString(R.string.action_channel_create));
        } else {
            alertDialog.setTitle(getString(R.string.action_channel_edit));
        }
        alertDialog.setOnDismissListener(dialogInterface -> {
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(bindingDialog.displayName.getWindowToken(), 0);
        });
        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    @Override
    public void show(ChannelData.Channel channel) {
        ChannelParams oldChannelValues = new ChannelParams();
        oldChannelValues.setName(channel.getName());
        oldChannelValues.setDescription(channel.getDescription());
        oldChannelValues.setDisplayName(channel.getDisplayName());
        manageAlert(oldChannelValues);
    }
}
