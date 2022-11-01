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


import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityStatusHistoryBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.StatusHistoryAdapter;
import app.fedilab.android.viewmodel.mastodon.StatusesVM;
import es.dmoral.toasty.Toasty;


public class StatusHistoryActivity extends BaseActivity {

    public static Resources.Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        ActivityStatusHistoryBinding binding = ActivityStatusHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }

        Bundle b = getIntent().getExtras();
        String statusId;
        if (b != null) {
            statusId = b.getString(Helper.ARG_STATUS_ID);
        } else {
            finish();
            return;
        }
        StatusesVM statusesVM = new ViewModelProvider(StatusHistoryActivity.this).get(StatusesVM.class);
        statusesVM.getStatusHistory(MainActivity.currentInstance, MainActivity.currentToken, statusId).observe(this, statuses -> {
            if (statuses != null && statuses.size() > 0) {
                StatusHistoryAdapter statusHistoryAdapter = new StatusHistoryAdapter(statuses);
                binding.recyclerView.setAdapter(statusHistoryAdapter);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            } else {
                Toasty.error(this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                finish();
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

}