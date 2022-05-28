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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.ActivityAboutBinding;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;


public class AboutActivity extends BaseActivity {


    private ActivityAboutBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            binding.aboutVersion.setText(getResources().getString(R.string.about_vesrion, version));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        binding.aboutCode.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://codeberg.org/tom79/Fedilab"));
        binding.aboutThekinrar.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://instances.social/"));
        binding.aboutLicense.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://www.gnu.org/licenses/quick-guide-gplv3.fr.html"));
        binding.aboutSupport.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://liberapay.com/tom79"));
        if (BuildConfig.DONATIONS) {
            binding.aboutSupport.setVisibility(View.VISIBLE);
        } else {
            binding.aboutSupport.setVisibility(View.GONE);
        }
        binding.aboutSupportPaypal.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://www.paypal.me/Mastalab"));


        if (BuildConfig.DONATIONS) {
            binding.aboutSupportPaypal.setVisibility(View.VISIBLE);
        } else {
            binding.aboutSupportPaypal.setVisibility(View.GONE);
        }
        binding.aboutWebsite.setOnClickListener(v -> Helper.openBrowser(AboutActivity.this, "https://fedilab.app"));
        CrossActionHelper.fetchRemoteAccount(AboutActivity.this, "@apps@toot.fedilab.app", new CrossActionHelper.Callback() {
            @Override
            public void federatedStatus(Status status) {

            }

            @Override
            public void federatedAccount(Account account) {
                if (account != null && account.username.equalsIgnoreCase("apps")) {
                    binding.developerTitle.setVisibility(View.VISIBLE);
                    binding.acccountContainer.setVisibility(View.VISIBLE);
                    MastodonHelper.loadPPMastodon(binding.accountPp, account);
                    binding.accountDn.setText(account.display_name);
                    binding.accountUn.setText(account.acct);
                    binding.accountPp.setOnClickListener(v -> {
                        Intent intent = new Intent(AboutActivity.this, ProfileActivity.class);
                        Bundle b = new Bundle();
                        b.putSerializable(Helper.ARG_ACCOUNT, account);
                        intent.putExtras(b);
                        ActivityOptionsCompat options = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(AboutActivity.this, binding.accountPp, getString(R.string.activity_porfile_pp));
                        startActivity(intent, options.toBundle());
                    });
                    AccountsVM accountsVM = new ViewModelProvider(AboutActivity.this).get(AccountsVM.class);
                    List<String> ids = new ArrayList<>();
                    ids.add(account.id);
                    accountsVM.getRelationships(MainActivity.currentInstance, MainActivity.currentToken, ids)
                            .observe(AboutActivity.this, relationShips -> {
                                if (relationShips != null && relationShips.size() > 0) {
                                    if (!relationShips.get(0).following) {
                                        binding.accountFollow.setVisibility(View.VISIBLE);
                                        binding.accountFollow.setOnClickListener(v -> accountsVM.follow(MainActivity.currentInstance, MainActivity.currentToken, account.id, true, false)
                                                .observe(AboutActivity.this, relationShip -> binding.accountFollow.setVisibility(View.GONE)));
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
