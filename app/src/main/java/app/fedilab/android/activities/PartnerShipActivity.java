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
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.ActivityPartnershipBinding;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;


public class PartnerShipActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityPartnershipBinding binding = ActivityPartnershipBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView about_partnership = findViewById(R.id.about_partnership);
        about_partnership.setMovementMethod(LinkMovementMethod.getInstance());

        binding.mastohostLogo.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://masto.host"));
            startActivity(browserIntent);
        });
        binding.accountFollow.setBackgroundTintList(ThemeHelper.getButtonActionColorStateList(PartnerShipActivity.this));
        setTitle(R.string.action_partnership);
        binding.accountFollow.setImageResource(R.drawable.ic_baseline_person_add_24);
        CrossActionHelper.fetchRemoteAccount(PartnerShipActivity.this, "@mastohost@mastodon.social", new CrossActionHelper.Callback() {
            @Override
            public void federatedStatus(Status status) {

            }

            @Override
            public void federatedAccount(Account account) {
                if (account != null && account.username.equalsIgnoreCase("mastohost")) {
                    binding.acccountContainer.setVisibility(View.VISIBLE);
                    MastodonHelper.loadPPMastodon(binding.accountPp, account);
                    binding.accountDn.setText(account.display_name);
                    binding.accountUn.setText(account.acct);
                    binding.accountPp.setOnClickListener(v -> {
                        Intent intent = new Intent(PartnerShipActivity.this, ProfileActivity.class);
                        Bundle b = new Bundle();
                        b.putSerializable(Helper.ARG_ACCOUNT, account);
                        intent.putExtras(b);
                        ActivityOptionsCompat options = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(PartnerShipActivity.this, binding.accountPp, getString(R.string.activity_porfile_pp));
                        startActivity(intent, options.toBundle());
                    });
                    AccountsVM accountsVM = new ViewModelProvider(PartnerShipActivity.this).get(AccountsVM.class);
                    List<String> ids = new ArrayList<>();
                    ids.add(account.id);
                    accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, ids)
                            .observe(PartnerShipActivity.this, relationShips -> {
                                if (relationShips != null && relationShips.size() > 0) {
                                    if (!relationShips.get(0).following) {
                                        binding.accountFollow.setVisibility(View.VISIBLE);
                                        binding.accountFollow.setOnClickListener(v -> accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false)
                                                .observe(PartnerShipActivity.this, relationShip -> binding.accountFollow.setVisibility(View.GONE)));
                                    }
                                }
                            });
                }
            }
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
