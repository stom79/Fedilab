package app.fedilab.android.peertube.activities;
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

import static app.fedilab.android.peertube.activities.PeertubeMainActivity.PICK_INSTANCE_SURF;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.showRadioButtonDialogFullInstances;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityManageInstancesPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.drawer.AboutInstanceAdapter;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.sqlite.Sqlite;
import app.fedilab.android.peertube.sqlite.StoredInstanceDAO;
import app.fedilab.android.peertube.viewmodel.InfoInstanceVM;


public class ManageInstancesActivity extends BaseBarActivity implements AboutInstanceAdapter.AllInstancesRemoved {

    private ActivityManageInstancesPeertubeBinding binding;
    private List<InstanceData.AboutInstance> aboutInstances;
    private AboutInstanceAdapter aboutInstanceAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageInstancesPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.loader.setVisibility(View.VISIBLE);
        binding.noAction.setVisibility(View.GONE);
        binding.lvInstances.setVisibility(View.GONE);
        binding.actionButton.setOnClickListener(v -> showRadioButtonDialogFullInstances(ManageInstancesActivity.this, true));
        aboutInstances = new ArrayList<>();
        aboutInstanceAdapter = new AboutInstanceAdapter(this.aboutInstances);
        aboutInstanceAdapter.allInstancesRemoved = this;
        binding.lvInstances.setAdapter(aboutInstanceAdapter);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(ManageInstancesActivity.this);
        binding.lvInstances.setLayoutManager(layoutManager);
        InfoInstanceVM viewModelInfoInstance = new ViewModelProvider(ManageInstancesActivity.this).get(InfoInstanceVM.class);
        viewModelInfoInstance.getInstances().observe(ManageInstancesActivity.this, this::manageVIewInfoInstance);
    }

    private void manageVIewInfoInstance(List<InstanceData.AboutInstance> aboutInstances) {
        binding.loader.setVisibility(View.GONE);
        if (aboutInstances == null || aboutInstances.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.lvInstances.setVisibility(View.GONE);
            return;
        }
        binding.noAction.setVisibility(View.GONE);
        binding.lvInstances.setVisibility(View.VISIBLE);
        this.aboutInstances.addAll(aboutInstances);
        aboutInstanceAdapter.notifyItemRangeInserted(0, aboutInstances.size());

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_out_up, R.anim.slide_in_up_down);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_out_up, R.anim.slide_in_up_down);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_INSTANCE_SURF && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                new Thread(() -> {
                    String newInstance = data.getData().toString().trim().toLowerCase();
                    InstanceData.AboutInstance aboutInstance = new RetrofitPeertubeAPI(ManageInstancesActivity.this, newInstance, null).getAboutInstance();
                    SQLiteDatabase db = Sqlite.getInstance(getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                    new StoredInstanceDAO(ManageInstancesActivity.this, db).insertInstance(aboutInstance, newInstance);
                    runOnUiThread(() -> new Handler().post(() -> Helper.logoutNoRemoval(ManageInstancesActivity.this)));
                }).start();
            }
        }
    }

    @Override
    public void onAllInstancesRemoved() {
        binding.noAction.setVisibility(View.VISIBLE);
        binding.lvInstances.setVisibility(View.GONE);
    }
}
