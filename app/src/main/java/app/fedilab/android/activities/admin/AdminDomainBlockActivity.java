package app.fedilab.android.activities.admin;
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


import static app.fedilab.android.helper.Helper.BROADCAST_DATA;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.fedilab.android.R;
import app.fedilab.android.activities.BaseBarActivity;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.client.entities.api.admin.AdminDomainBlock;
import app.fedilab.android.databinding.ActivityAdminDomainblockBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.AdminVM;
import es.dmoral.toasty.Toasty;

public class AdminDomainBlockActivity extends BaseBarActivity {


    private final String[] severityChoices = {"silence", "suspend", "noop"};
    private AdminVM adminVM;
    private AdminDomainBlock adminDomainBlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityAdminDomainblockBinding binding = ActivityAdminDomainblockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            adminDomainBlock = (AdminDomainBlock) b.getSerializable(Helper.ARG_ADMIN_DOMAINBLOCK);
        }

        ArrayAdapter<CharSequence> adapterResize = ArrayAdapter.createFromResource(this,
                R.array.admin_block_severity, android.R.layout.simple_spinner_dropdown_item);
        binding.severity.setAdapter(adapterResize);

        if (adminDomainBlock != null) {
            binding.domain.setText(adminDomainBlock.domain);
            binding.domain.setEnabled(false);
            for (int i = 0; i < severityChoices.length; i++) {
                if (adminDomainBlock.severity.equalsIgnoreCase(severityChoices[i])) {
                    binding.severity.setSelection(i, false);
                    break;
                }
            }
            binding.obfuscate.setChecked(adminDomainBlock.obfuscate);
            binding.rejectMedia.setChecked(adminDomainBlock.reject_media);
            binding.rejectReports.setChecked(adminDomainBlock.reject_reports);
            binding.privateComment.setText(adminDomainBlock.private_comment);
            binding.publicComment.setText(adminDomainBlock.public_comment);
        } else {
            adminDomainBlock = new AdminDomainBlock();
        }

        binding.severity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                adminDomainBlock.severity = severityChoices[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        binding.obfuscate.setOnCheckedChangeListener((compoundButton, checked) -> adminDomainBlock.obfuscate = checked);
        binding.rejectMedia.setOnCheckedChangeListener((compoundButton, checked) -> adminDomainBlock.reject_media = checked);
        binding.rejectReports.setOnCheckedChangeListener((compoundButton, checked) -> adminDomainBlock.reject_reports = checked);
        adminVM = new ViewModelProvider(AdminDomainBlockActivity.this).get(AdminVM.class);
        binding.saveChanges.setOnClickListener(v -> {
            adminDomainBlock.domain = binding.domain.getText().toString().trim();
            adminDomainBlock.public_comment = binding.publicComment.getText().toString().trim();
            adminDomainBlock.private_comment = binding.privateComment.getText().toString().trim();
            adminVM.createOrUpdateDomainBlock(MainActivity.currentInstance, MainActivity.currentToken, adminDomainBlock)
                    .observe(AdminDomainBlockActivity.this, adminDomainBlockResult -> {
                                if (adminDomainBlockResult != null) {
                                    Toasty.success(AdminDomainBlockActivity.this, getString(R.string.saved_changes), Toasty.LENGTH_SHORT).show();
                                } else {
                                    Toasty.error(AdminDomainBlockActivity.this, getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                                }
                                Intent intent = new Intent(BROADCAST_DATA).putExtra(Helper.ARG_ADMIN_DOMAINBLOCK, adminDomainBlockResult);
                                LocalBroadcastManager.getInstance(AdminDomainBlockActivity.this).sendBroadcast(intent);
                                finish();
                            }
                    );
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_domain, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_delete) {
            if (adminDomainBlock.id != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdminDomainBlockActivity.this);
                builder.setMessage(getString(R.string.unblock_domain_confirm, adminDomainBlock.domain));
                builder
                        .setPositiveButton(R.string.unblock_domain, (dialog, which) -> {
                            adminVM.deleteDomain(MainActivity.currentInstance, MainActivity.currentToken, adminDomainBlock.id)
                                    .observe(AdminDomainBlockActivity.this, adminDomainBlockResult -> {
                                                Intent intent = new Intent(BROADCAST_DATA).putExtra(Helper.ARG_ADMIN_DOMAINBLOCK_DELETE, adminDomainBlock);
                                                LocalBroadcastManager.getInstance(AdminDomainBlockActivity.this).sendBroadcast(intent);
                                                finish();
                                            }
                                    );
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                finish();
            }

        }
        return true;
    }

}
