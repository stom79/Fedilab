package app.fedilab.android.mastodon.activities;
/* Copyright 2022 Thomas Schneider
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


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityReorderTabsBinding;
import app.fedilab.android.databinding.PopupSearchInstanceBinding;
import app.fedilab.android.mastodon.client.entities.app.BottomMenu;
import app.fedilab.android.mastodon.client.entities.app.InstanceSocial;
import app.fedilab.android.mastodon.client.entities.app.Pinned;
import app.fedilab.android.mastodon.client.entities.app.PinnedTimeline;
import app.fedilab.android.mastodon.client.entities.app.RemoteInstance;
import app.fedilab.android.mastodon.client.entities.app.Timeline;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.PinnedTimelineHelper;
import app.fedilab.android.mastodon.helper.itemtouchhelper.OnStartDragListener;
import app.fedilab.android.mastodon.helper.itemtouchhelper.SimpleItemTouchHelperCallback;
import app.fedilab.android.mastodon.ui.drawer.ReorderBottomMenuAdapter;
import app.fedilab.android.mastodon.ui.drawer.ReorderTabAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.InstanceSocialVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.ReorderVM;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ReorderTimelinesActivity extends BaseBarActivity implements OnStartDragListener {


    private ItemTouchHelper touchHelper;
    private ReorderTabAdapter reorderTabAdapter;
    private ReorderBottomMenuAdapter reorderBottomMenuAdapter;
    private boolean searchInstanceRunning;
    private String oldSearch;
    private Pinned pinned;
    private BottomMenu bottomMenu;
    private ActivityReorderTabsBinding binding;
    private boolean changes;
    private boolean bottomChanges;
    private boolean update;

    public void setChanges(boolean changes) {
        this.changes = changes;
    }

    public void setBottomChanges(boolean bottomChanges) {
        this.bottomChanges = bottomChanges;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReorderTabsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        searchInstanceRunning = false;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ReorderTimelinesActivity.this);
        boolean singleBar = sharedpreferences.getBoolean(getString(R.string.SET_USE_SINGLE_TOPBAR), false);
        if (singleBar) {
            binding.titleBottom.setVisibility(View.GONE);
            binding.lvReorderBottom.setVisibility(View.GONE);
            binding.titleTop.setVisibility(View.GONE);
        }
        changes = false;
        bottomChanges = false;
        ReorderVM reorderVM = new ViewModelProvider(ReorderTimelinesActivity.this).get(ReorderVM.class);
        reorderVM.getPinned().observe(ReorderTimelinesActivity.this, _pinned -> {
            update = true;
            this.pinned = _pinned;
            if (this.pinned == null) {
                this.pinned = new Pinned();
                this.pinned.pinnedTimelines = new ArrayList<>();
                update = false;
            }
            PinnedTimelineHelper.sortPositionAsc(this.pinned.pinnedTimelines);
            reorderTabAdapter = new ReorderTabAdapter(this.pinned, ReorderTimelinesActivity.this);
            ItemTouchHelper.Callback callback =
                    new SimpleItemTouchHelperCallback(reorderTabAdapter);
            touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(binding.lvReorderTabs);
            binding.lvReorderTabs.setAdapter(reorderTabAdapter);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(ReorderTimelinesActivity.this);
            binding.lvReorderTabs.setLayoutManager(mLayoutManager);
        });
        reorderVM.getBottomMenu().observe(ReorderTimelinesActivity.this, _bottomMenu -> {
            this.bottomMenu = _bottomMenu;
            if (_bottomMenu == null) {
                this.bottomMenu = new BottomMenu(getApplicationContext()).defaultBottomMenu();
                this.bottomMenu.bottom_menu = new ArrayList<>();
            }
            PinnedTimelineHelper.sortMenuItem(this.bottomMenu.bottom_menu);
            reorderBottomMenuAdapter = new ReorderBottomMenuAdapter(this.bottomMenu, ReorderTimelinesActivity.this);
            ItemTouchHelper.Callback callback =
                    new SimpleItemTouchHelperCallback(reorderBottomMenuAdapter);
            touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(binding.lvReorderBottom);
            binding.lvReorderBottom.setAdapter(reorderBottomMenuAdapter);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(ReorderTimelinesActivity.this);
            binding.lvReorderBottom.setLayoutManager(mLayoutManager);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_add_timeline) {
            addInstance();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reorder, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void addInstance() {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(ReorderTimelinesActivity.this);
        PopupSearchInstanceBinding popupSearchInstanceBinding = PopupSearchInstanceBinding.inflate(getLayoutInflater());
        dialogBuilder.setView(popupSearchInstanceBinding.getRoot());
        TextWatcher textWatcher = autoComplete(popupSearchInstanceBinding);
        popupSearchInstanceBinding.searchInstance.addTextChangedListener(textWatcher);

        popupSearchInstanceBinding.setAttachmentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.twitter_accounts) {
                popupSearchInstanceBinding.searchInstance.setHint(R.string.list_of_twitter_accounts);
                popupSearchInstanceBinding.searchInstance.removeTextChangedListener(textWatcher);
            } else {
                popupSearchInstanceBinding.searchInstance.setHint(R.string.instance);
                popupSearchInstanceBinding.searchInstance.removeTextChangedListener(textWatcher);
                popupSearchInstanceBinding.searchInstance.addTextChangedListener(textWatcher);
            }
        });
        dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
            String instanceName = popupSearchInstanceBinding.searchInstance.getText().toString().trim().replace("@", "");
            new Thread(() -> {
                String url = null;
                boolean getCall = true;
                RequestBody formBody = new FormBody.Builder()
                        .build();
                if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.mastodon_instance) {
                    url = "https://" + instanceName + "/api/v1/timelines/public?local=true";
                } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.peertube_instance) {
                    url = "https://" + instanceName + "/api/v1/videos/";
                } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.pixelfed_instance) {
                    url = "https://" + instanceName + "/api/v1/timelines/public";
                } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.misskey_instance) {
                    url = "https://" + instanceName + "/api/notes/local-timeline";
                    getCall = false;
                } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.gnu_instance) {
                    url = "https://" + instanceName + "/api/statuses/public_timeline.json";
                } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.twitter_accounts) {
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ReorderTimelinesActivity.this);
                    String nitterHost = sharedpreferences.getString(getString(R.string.SET_NITTER_HOST), getString(R.string.DEFAULT_NITTER_HOST)).toLowerCase();
                    url = "https://" + nitterHost + "/" + instanceName.replaceAll("[ ]+", ",").replaceAll("\\s", "") + "/rss";
                }
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .proxy(Helper.getProxy(getApplication().getApplicationContext()))
                        .readTimeout(10, TimeUnit.SECONDS).build();
                Request request;
                if (url != null) {
                    if (getCall) {
                        request = new Request.Builder()
                                .url(url)
                                .build();
                    } else {
                        request = new Request.Builder()
                                .url(url)
                                .post(formBody)
                                .build();
                    }
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toasty.warning(ReorderTimelinesActivity.this, getString(R.string.toast_instance_unavailable), Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull final Response response) {
                            if (response.isSuccessful()) {
                                runOnUiThread(() -> {
                                    dialog.dismiss();
                                    RemoteInstance.InstanceType instanceType = null;
                                    if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.mastodon_instance) {
                                        instanceType = RemoteInstance.InstanceType.MASTODON;
                                    } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.peertube_instance) {
                                        instanceType = RemoteInstance.InstanceType.PEERTUBE;
                                    } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.pixelfed_instance) {
                                        instanceType = RemoteInstance.InstanceType.PIXELFED;
                                    } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.misskey_instance) {
                                        instanceType = RemoteInstance.InstanceType.MISSKEY;
                                    } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.gnu_instance) {
                                        instanceType = RemoteInstance.InstanceType.GNU;
                                    } else if (popupSearchInstanceBinding.setAttachmentGroup.getCheckedRadioButtonId() == R.id.twitter_accounts) {
                                        instanceType = RemoteInstance.InstanceType.NITTER;
                                    }
                                    RemoteInstance remoteInstance = new RemoteInstance();
                                    remoteInstance.type = instanceType;
                                    remoteInstance.host = instanceName;
                                    PinnedTimeline pinnedTimeline = new PinnedTimeline();
                                    pinnedTimeline.remoteInstance = remoteInstance;
                                    pinnedTimeline.displayed = true;
                                    pinnedTimeline.type = Timeline.TimeLineEnum.REMOTE;
                                    pinnedTimeline.position = pinned.pinnedTimelines.size();
                                    pinned.pinnedTimelines.add(pinnedTimeline);
                                    if (pinned.user_id == null || pinned.instance == null) {
                                        pinned.user_id = MainActivity.currentUserID;
                                        pinned.instance = MainActivity.currentInstance;
                                    }
                                    if (update) {
                                        try {
                                            new Pinned(ReorderTimelinesActivity.this).updatePinned(pinned);
                                        } catch (DBException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            new Pinned(ReorderTimelinesActivity.this).insertPinned(pinned);
                                        } catch (DBException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    reorderTabAdapter.notifyItemInserted(pinned.pinnedTimelines.size());
                                    Bundle b = new Bundle();
                                    b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
                                    Intent intentBD = new Intent(Helper.BROADCAST_DATA);
                                    intentBD.putExtras(b);
                                    LocalBroadcastManager.getInstance(ReorderTimelinesActivity.this).sendBroadcast(intentBD);
                                });
                            } else {
                                runOnUiThread(() -> Toasty.warning(ReorderTimelinesActivity.this, getString(R.string.toast_instance_unavailable), Toast.LENGTH_LONG).show());
                            }
                        }
                    });
                } else {
                    runOnUiThread(() -> Toasty.warning(ReorderTimelinesActivity.this, getString(R.string.toast_instance_unavailable), Toast.LENGTH_LONG).show());
                }
            }).start();
        });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setOnDismissListener(dialogInterface -> {
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(popupSearchInstanceBinding.searchInstance.getWindowToken(), 0);
        });
        if (alertDialog.getWindow() != null)
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();

        popupSearchInstanceBinding.searchInstance.setOnItemClickListener((parent, view1, position, id) -> oldSearch = parent.getItemAtPosition(position).toString().trim());


    }

    private TextWatcher autoComplete(PopupSearchInstanceBinding popupSearchInstanceBinding) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 2 && !searchInstanceRunning) {
                    String query = s.toString().trim();
                    if (query.startsWith("http://")) {
                        query = query.replace("http://", "");
                    }
                    if (query.startsWith("https://")) {
                        query = query.replace("https://", "");
                    }
                    if (oldSearch == null || !oldSearch.equals(s.toString().trim())) {
                        searchInstanceRunning = true;
                        InstanceSocialVM instanceSocialVM = new ViewModelProvider(ReorderTimelinesActivity.this).get(InstanceSocialVM.class);
                        instanceSocialVM.getInstances(query).observe(ReorderTimelinesActivity.this, instanceSocialList -> {
                            popupSearchInstanceBinding.searchInstance.setAdapter(null);
                            String[] instances = new String[instanceSocialList.instances.size()];
                            int j = 0;
                            for (InstanceSocial.Instance instance : instanceSocialList.instances) {
                                instances[j] = instance.name;
                                j++;
                            }
                            ArrayAdapter<String> arrayAdapter =
                                    new ArrayAdapter<>(ReorderTimelinesActivity.this, android.R.layout.simple_list_item_1, instances);
                            popupSearchInstanceBinding.searchInstance.setAdapter(arrayAdapter);
                            if (popupSearchInstanceBinding.searchInstance.hasFocus() && !isFinishing())
                                popupSearchInstanceBinding.searchInstance.showDropDown();
                            if (oldSearch != null && oldSearch.equals(popupSearchInstanceBinding.searchInstance.getText().toString())) {
                                popupSearchInstanceBinding.searchInstance.dismissDropDown();
                            }

                            oldSearch = s.toString().trim();
                            searchInstanceRunning = false;
                        });
                    }
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (changes) {
            //Update menu
            Bundle b = new Bundle();
            b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
            intentBD.putExtras(b);
            LocalBroadcastManager.getInstance(ReorderTimelinesActivity.this).sendBroadcast(intentBD);
        }
        if (bottomChanges) {
            Bundle b = new Bundle();
            b.putBoolean(Helper.RECEIVE_REDRAW_BOTTOM, true);
            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
            intentBD.putExtras(b);
            LocalBroadcastManager.getInstance(ReorderTimelinesActivity.this).sendBroadcast(intentBD);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }


    @Override
    public void onStop() {
        super.onStop();
    }


}
