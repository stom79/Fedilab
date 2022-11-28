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
import android.text.SpannableString;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityInstanceProfileBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import es.dmoral.toasty.Toasty;

public class InstanceProfileActivity extends BaseActivity {


    private String instance;
    private ActivityInstanceProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityInstanceProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Bundle b = getIntent().getExtras();
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        if (b != null)
            instance = b.getString(Helper.ARG_INSTANCE, null);
        if (instance == null) {
            finish();
        }
        Button close = findViewById(R.id.close);
        close.setOnClickListener(view -> finish());
        NodeInfoVM nodeInfoVM = new ViewModelProvider(InstanceProfileActivity.this).get(NodeInfoVM.class);
        nodeInfoVM.getNodeInfo(instance).observe(InstanceProfileActivity.this, nodeInfo -> {
            if (nodeInfo == null) {
                Toasty.error(InstanceProfileActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            binding.name.setText(instance);
            SpannableString descriptionSpan;
            binding.userCount.setText(Helper.withSuffix((nodeInfo.usage.users.total)));
            binding.statusCount.setText(Helper.withSuffix(((nodeInfo.usage.localPosts))));
            String softwareStr = nodeInfo.software.name + " - ";
            binding.software.setText(softwareStr);
            binding.version.setText(nodeInfo.software.version);
            binding.instanceContainer.setVisibility(View.VISIBLE);
            binding.loader.setVisibility(View.GONE);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
