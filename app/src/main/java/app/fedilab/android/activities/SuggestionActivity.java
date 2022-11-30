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
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivitySuggestionsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonSuggestion;


public class SuggestionActivity extends BaseBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySuggestionsBinding binding = ActivitySuggestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Helper.addFragment(getSupportFragmentManager(), R.id.nav_host_fragment_suggestions, new FragmentMastodonSuggestion(), null, null, null);
    }


    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
