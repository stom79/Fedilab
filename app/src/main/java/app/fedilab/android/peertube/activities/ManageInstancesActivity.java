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

import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE_PEERTUBE_BROWSING;
import static app.fedilab.android.mastodon.helper.Helper.TAG;
import static app.fedilab.android.mastodon.helper.Helper.addFragment;
import static app.fedilab.android.peertube.helper.Helper.recreatePeertubeActivity;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityManageInstancesPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.entities.WellKnownNodeinfo;
import app.fedilab.android.peertube.drawer.AboutInstanceAdapter;
import app.fedilab.android.peertube.fragment.FragmentLoginPickInstancePeertube;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.sqlite.StoredInstanceDAO;
import app.fedilab.android.peertube.viewmodel.InfoInstanceVM;
import app.fedilab.android.sqlite.Sqlite;
import es.dmoral.toasty.Toasty;


public class ManageInstancesActivity extends BaseBarActivity implements AboutInstanceAdapter.InstanceActions {

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
        binding.actionButton.setOnClickListener(v -> {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
            AlertDialog.Builder alt_bld = new MaterialAlertDialogBuilder(this);
            alt_bld.setTitle(R.string.instance_choice);
            String instance = HelperInstance.getLiveInstance(this);
            final EditText input = new EditText(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            alt_bld.setView(input);
            input.setText(instance);
            alt_bld.setPositiveButton(R.string.validate,
                    (dialog, which) -> new Thread(() -> {
                        try {
                            String newInstance = input.getText().toString().trim();
                            if (!newInstance.startsWith("http")) {
                                newInstance = "http://" + newInstance;
                            }
                            URL url = new URL(newInstance);
                            newInstance = url.getHost();

                            WellKnownNodeinfo.NodeInfo instanceNodeInfo = new RetrofitPeertubeAPI(this, newInstance, null).getNodeInfo();
                            if (instanceNodeInfo.getSoftware() != null && instanceNodeInfo.getSoftware().getName().trim().toLowerCase().compareTo("peertube") == 0) {
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(PREF_USER_INSTANCE_PEERTUBE_BROWSING, newInstance);
                                editor.commit();
                                newInstance = newInstance.trim().toLowerCase();
                                InstanceData.AboutInstance aboutInstance = new RetrofitPeertubeAPI(this, newInstance, null).getAboutInstance();
                                SQLiteDatabase db = Sqlite.getInstance(this.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                                new StoredInstanceDAO(this, db).insertInstance(aboutInstance, newInstance);
                                this.runOnUiThread(() -> {
                                    dialog.dismiss();
                                    recreatePeertubeActivity(this);
                                    finish();
                                });
                            } else {
                                runOnUiThread(() -> Toasty.error(this, getString(R.string.not_valide_instance), Toast.LENGTH_LONG).show());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }).start());
            alt_bld.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            alt_bld.setNeutralButton(R.string.help, (dialog, which) -> {
                addFragment(
                        getSupportFragmentManager(), android.R.id.content, new FragmentLoginPickInstancePeertube(),
                        null, null, FragmentLoginPickInstancePeertube.class.getName());
            });
            AlertDialog alert = alt_bld.create();
            alert.show();
        });
        aboutInstances = new ArrayList<>();
        aboutInstanceAdapter = new AboutInstanceAdapter(this.aboutInstances);
        aboutInstanceAdapter.instanceActions = this;
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

    @Override
    public void onAllInstancesRemoved() {
        binding.noAction.setVisibility(View.VISIBLE);
        binding.lvInstances.setVisibility(View.GONE);
    }

    @Override
    public void onFinished() {
        recreatePeertubeActivity(this);
        finish();
    }
}
