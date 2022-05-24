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


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Instance;
import app.fedilab.android.databinding.ActivityInstanceBinding;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.InstancesVM;
import es.dmoral.toasty.Toasty;


public class InstanceActivity extends BaseActivity {


    ActivityInstanceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeDialog(this);
        binding = ActivityInstanceBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        binding.close.setOnClickListener(view -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        InstancesVM instancesVM = new ViewModelProvider(InstanceActivity.this).get(InstancesVM.class);
        instancesVM.getInstance(BaseMainActivity.currentInstance).observe(InstanceActivity.this, instanceInfo -> {
            binding.instanceContainer.setVisibility(View.VISIBLE);
            binding.loader.setVisibility(View.GONE);
            if (instanceInfo == null || instanceInfo.info == null) {
                Toasty.error(InstanceActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                return;
            }
            Instance instance = instanceInfo.info;
            binding.instanceTitle.setText(instance.title);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                binding.instanceDescription.setText(Html.fromHtml(instance.description, Html.FROM_HTML_MODE_LEGACY));
            else
                binding.instanceDescription.setText(Html.fromHtml(instance.description));
            if (instance.description == null || instance.description.trim().length() == 0)
                binding.instanceDescription.setText(getString(R.string.instance_no_description));
            binding.instanceVersion.setText(instance.version);
            binding.instanceUri.setText(instance.uri);
            if (instance.email == null) {
                binding.instanceContact.hide();
            }
            Glide.with(InstanceActivity.this)
                    .asBitmap()
                    .load(instance.thumbnail)
                    .into(binding.backGroundImage);

            binding.instanceContact.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", instance.email, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Mastodon] - " + instance.uri);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
            });
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
