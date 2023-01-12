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

import static app.fedilab.android.client.entities.app.Timeline.TimeLineEnum.ACCOUNT_DIRECTORY;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityDirectoryBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonAccount;


public class DirectoryActivity extends BaseBarActivity {

    private static boolean local = false;
    private static String order = "active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDirectoryBinding binding = ActivityDirectoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(Helper.ARG_DIRECTORY_LOCAL, local);
        bundle.putString(Helper.ARG_DIRECTORY_ORDER, order);
        bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, ACCOUNT_DIRECTORY);
        Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_directory, new FragmentMastodonAccount(), bundle, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_directory, menu);
        if (order.equals("active")) {
            menu.findItem(R.id.order_active).setChecked(true);
        } else {
            menu.findItem(R.id.order_new).setChecked(true);
        }
        menu.findItem(R.id.action_local).setChecked(local);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_local) {
            item.setChecked(!item.isChecked());
            local = item.isChecked();
        } else if (item.getItemId() == R.id.order_active) {
            order = "active";
        } else if (item.getItemId() == R.id.order_new) {
            order = "new";
        }
        recreate();
        return super.onOptionsItemSelected(item);
    }

}
