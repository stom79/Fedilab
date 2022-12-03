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
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.InstanceSocial;
import app.fedilab.android.databinding.ActivityInstanceSocialBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.InstanceSocialVM;


public class InstanceHealthActivity extends BaseAlertDialogActivity {

    private ActivityInstanceSocialBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInstanceSocialBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        binding.close.setOnClickListener(view -> finish());

        SpannableString content = new SpannableString(binding.refInstance.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        binding.refInstance.setText(content);
        binding.refInstance.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instances.social"));
            startActivity(browserIntent);
        });

        checkInstance();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void checkInstance() {


        InstanceSocialVM instanceSocialVM = new ViewModelProvider(InstanceHealthActivity.this).get(InstanceSocialVM.class);
        instanceSocialVM.getInstances(BaseMainActivity.currentInstance.trim()).observe(InstanceHealthActivity.this, instanceSocialList -> {
            if (instanceSocialList != null && instanceSocialList.instances.size() > 0) {
                InstanceSocial.Instance instanceSocial = instanceSocialList.instances.get(0);
                if (instanceSocial.thumbnail != null && !instanceSocial.thumbnail.equals("null"))
                    Glide.with(InstanceHealthActivity.this)
                            .asBitmap()
                            .load(instanceSocial.thumbnail)
                            .into(binding.backGroundImage);
                binding.name.setText(instanceSocial.name);
                if (instanceSocial.up) {
                    binding.up.setText(R.string.is_up);
                    binding.up.setTextColor(ThemeHelper.getAttColor(this, R.attr.colorPrimary));
                } else {
                    binding.up.setText(R.string.is_down);
                    binding.up.setTextColor(ThemeHelper.getAttColor(this, R.attr.colorError));
                }
                binding.uptime.setText(getString(R.string.instance_health_uptime, (instanceSocial.uptime * 100)));
                if (instanceSocial.checked_at != null)
                    binding.checkedAt.setText(getString(R.string.instance_health_checkedat, Helper.dateToString(instanceSocial.checked_at)));
                binding.values.setText(getString(R.string.instance_health_indication, instanceSocial.version, Helper.withSuffix(instanceSocial.active_users), Helper.withSuffix(instanceSocial.statuses)));
                binding.instanceContainer.setVisibility(View.VISIBLE);
            } else {
                binding.instanceContainer.setVisibility(View.VISIBLE);
                binding.mainContainer.setVisibility(View.GONE);
                binding.noInstance.setVisibility(View.VISIBLE);
            }
            binding.loader.setVisibility(View.GONE);
        });
    }


}
