package app.fedilab.android.activities;
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.StatusDraft;
import app.fedilab.android.client.mastodon.entities.Attachment;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.databinding.ActivityDraftsBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.StatusDraftAdapter;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;

public class DraftActivity extends BaseActivity implements StatusDraftAdapter.DraftActions {


    private ActivityDraftsBinding binding;
    private List<StatusDraft> statusDrafts;
    private StatusDraftAdapter statusDraftAdapter;
    private TimelinesVM timelinesVM;
    private LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityDraftsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setPopupTheme(Helper.popupStyle());
        binding.title.setText(R.string.drafts);
        binding.loader.setVisibility(View.VISIBLE);
        binding.lvStatus.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        timelinesVM = new ViewModelProvider(DraftActivity.this).get(TimelinesVM.class);
        timelinesVM.getDrafts(BaseMainActivity.accountWeakReference.get())
                .observe(DraftActivity.this, this::initializeDraftView);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_draft, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            AlertDialog.Builder unfollowConfirm = new AlertDialog.Builder(DraftActivity.this, Helper.dialogStyle());
            unfollowConfirm.setTitle(getString(R.string.delete_all));
            unfollowConfirm.setMessage(getString(R.string.remove_draft));
            unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            unfollowConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                new Thread(() -> {
                    if (statusDrafts != null) {
                        for (StatusDraft statusDraft : statusDrafts) {
                            //Check if there are media in the drafts
                            List<Attachment> attachments = new ArrayList<>();
                            if (statusDraft.statusDraftList != null) {
                                for (Status drafts : statusDraft.statusDraftList) {
                                    if (drafts.media_attachments != null && drafts.media_attachments.size() > 0) {
                                        attachments.addAll(drafts.media_attachments);
                                    }
                                }
                            }
                            //If there are media, we need to remove them first.
                            if (attachments.size() > 0) {
                                for (Attachment attachment : attachments) {
                                    if (attachment.local_path != null) {
                                        File fileToDelete = new File(attachment.local_path);
                                        if (fileToDelete.exists()) {
                                            //noinspection ResultOfMethodCallIgnored
                                            fileToDelete.delete();
                                        }
                                    }
                                }
                            }
                        }
                        try {
                            //Delete the draft
                            new StatusDraft(DraftActivity.this).removeAllDraft();
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            Runnable myRunnable = () -> statusDraftAdapter.draftActions.onAllDeleted();
                            mainHandler.post(myRunnable);
                        } catch (DBException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                dialog.dismiss();
            });
            unfollowConfirm.show();
            return true;
        }
        return true;
    }


    /**
     * Intialize the view for drafts
     *
     * @param statusDrafts {@link List<StatusDraft>}
     */
    private void initializeDraftView(List<StatusDraft> statusDrafts) {
        if (statusDrafts == null) {
            statusDrafts = new ArrayList<>();
        }
        binding.loader.setVisibility(View.GONE);
        if (statusDrafts.size() > 0) {
            binding.lvStatus.setVisibility(View.VISIBLE);
            this.statusDrafts = statusDrafts;
            statusDraftAdapter = new StatusDraftAdapter(this.statusDrafts);
            statusDraftAdapter.draftActions = this;
            mLayoutManager = new LinearLayoutManager(DraftActivity.this);
            binding.lvStatus.setLayoutManager(mLayoutManager);
            binding.lvStatus.setAdapter(statusDraftAdapter);
        } else {
            binding.noAction.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //We need to check if drafts changed (ie when coming back from the compose activity)
        if (statusDrafts != null && timelinesVM != null) {
            timelinesVM.getDrafts(BaseMainActivity.accountWeakReference.get())
                    .observe(DraftActivity.this, this::updateDrafts);
        }
    }

    private void updateDrafts(List<StatusDraft> statusDrafts) {
        if (statusDrafts == null) {
            statusDrafts = new ArrayList<>();
        }
        int currentPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (statusDrafts.size() > 0) {
            int count = this.statusDrafts.size();
            this.statusDrafts.clear();
            this.statusDrafts = new ArrayList<>();
            statusDraftAdapter.notifyItemRangeRemoved(0, count);
            this.statusDrafts = statusDrafts;
            statusDraftAdapter = new StatusDraftAdapter(this.statusDrafts);
            statusDraftAdapter.draftActions = this;
            mLayoutManager = new LinearLayoutManager(DraftActivity.this);
            binding.lvStatus.setLayoutManager(mLayoutManager);
            binding.lvStatus.setAdapter(statusDraftAdapter);
            if (currentPosition < this.statusDrafts.size()) {
                binding.lvStatus.scrollToPosition(currentPosition);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.lvStatus.setAdapter(null);
        binding = null;
    }

    @Override
    public void onAllDeleted() {
        binding.lvStatus.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.VISIBLE);
    }
}
