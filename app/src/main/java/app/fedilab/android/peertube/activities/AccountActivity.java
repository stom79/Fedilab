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

import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.badgeCount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityAccountPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.fragment.DisplayAccountsFragment;
import app.fedilab.android.peertube.fragment.DisplayChannelsFragment;
import app.fedilab.android.peertube.fragment.DisplayNotificationsFragment;
import app.fedilab.android.peertube.helper.Helper;


public class AccountActivity extends BaseBarActivity {


    private ActivityAccountPeertubeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountPeertubeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        SpannableString content_create = new SpannableString(getString(R.string.join_peertube));
        content_create.setSpan(new UnderlineSpan(), 0, content_create.length(), 0);
        content_create.setSpan(new ForegroundColorSpan(Helper.fetchAccentColor(AccountActivity.this)), 0, content_create.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);


        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(AccountActivity.this);
        String token = sharedpreferences.getString(PREF_USER_TOKEN, null);

        BaseAccount baseAccount = null;
        try {
            baseAccount = new Account(AccountActivity.this).getAccountByToken(token);
        } catch (DBException e) {
            e.printStackTrace();
        }
        if (baseAccount == null) {
            finish();
            return;
        }

        AccountData.PeertubeAccount account = baseAccount.peertube_account;

        setTitle(String.format("@%s@%s", account.getUsername(), baseAccount.instance));

        Helper.loadAvatar(AccountActivity.this, account, binding.profilePicture);
        binding.username.setText(String.format("@%s", account.getUsername()));
        binding.displayname.setText(account.getDisplayName());


        binding.editButton.setOnClickListener(v -> startActivity(new Intent(AccountActivity.this, MyAccountActivity.class)));


        TabLayout.Tab notificationTab = binding.accountTabLayout.newTab();
        if (Helper.isLoggedIn()) {
            if (badgeCount > 0) {
                binding.accountTabLayout.addTab(notificationTab.setText(getString(R.string.title_notifications) + " (" + badgeCount + ")"));
            } else {
                binding.accountTabLayout.addTab(notificationTab.setText(getString(R.string.title_notifications)));
            }
            binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.title_muted)));
            binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.title_channel)));

            binding.accountViewpager.setOffscreenPageLimit(3);


            binding.accountViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    TabLayout.Tab tab = binding.accountTabLayout.getTabAt(position);
                    if (tab != null)
                        tab.select();
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });


            binding.accountTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    binding.accountViewpager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    Fragment fragment = null;
                    if (binding.accountViewpager.getAdapter() != null)
                        fragment = (Fragment) binding.accountViewpager.getAdapter().instantiateItem(binding.accountViewpager, tab.getPosition());
                    switch (tab.getPosition()) {
                        case 0:
                            if (badgeCount > 0) {
                                android.app.AlertDialog.Builder builder;
                                builder = new android.app.AlertDialog.Builder(AccountActivity.this);
                                builder.setMessage(R.string.mark_all_notifications_as_read_confirm);
                                builder.setIcon(android.R.drawable.ic_dialog_alert)
                                        .setPositiveButton(R.string.mark_all_as_read, (dialog, which) -> {
                                            new Thread(() -> {
                                                new RetrofitPeertubeAPI(AccountActivity.this).markAllAsRead();
                                                Handler mainHandler = new Handler(Looper.getMainLooper());
                                                badgeCount = 0;
                                                Runnable myRunnable = () -> binding.accountTabLayout.getTabAt(0).setText(getString(R.string.title_notifications));
                                                mainHandler.post(myRunnable);
                                            }).start();

                                            dialog.dismiss();
                                        })
                                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                                        .show();
                            } else {
                                if (fragment != null) {
                                    DisplayNotificationsFragment displayNotificationsFragment = ((DisplayNotificationsFragment) fragment);
                                    displayNotificationsFragment.scrollToTop();
                                }
                            }
                            break;
                        case 1:
                            if (fragment != null) {
                                DisplayAccountsFragment displayAccountsFragment = ((DisplayAccountsFragment) fragment);
                                displayAccountsFragment.scrollToTop();
                            }
                            break;
                        case 2:
                            if (fragment != null) {
                                DisplayChannelsFragment displayChannelsFragment = ((DisplayChannelsFragment) fragment);
                                displayChannelsFragment.scrollToTop();
                            }
                            break;
                    }
                }
            });

            PagerAdapter mPagerAdapter = new AccountsPagerAdapter(getSupportFragmentManager());
            binding.accountViewpager.setAdapter(mPagerAdapter);
        } else {
            binding.accountTabLayout.setVisibility(View.GONE);
            binding.accountViewpager.setVisibility(View.GONE);
            binding.remoteAccount.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                binding.remoteAccount.setText(Html.fromHtml(getString(R.string.remote_account_from, baseAccount.software), Html.FROM_HTML_MODE_LEGACY));
            else
                binding.remoteAccount.setText(Html.fromHtml(getString(R.string.remote_account_from, baseAccount.software)));
        }
    }

    public void updateCounter() {
        if (badgeCount > 0) {
            binding.accountTabLayout.getTabAt(0).setText(getString(R.string.title_notifications) + " (" + badgeCount + ")");
        } else {
            binding.accountTabLayout.getTabAt(0).setText(getString(R.string.title_notifications));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_out_up, R.anim.slide_in_up_down);
    }

    /**
     * Pager adapter for three tabs (notifications, muted, blocked)
     */
    private static class AccountsPagerAdapter extends FragmentStatePagerAdapter {

        AccountsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            switch (position) {
                case 1:
                    DisplayAccountsFragment displayAccountsFragment = new DisplayAccountsFragment();
                    bundle.putSerializable("accountFetch", RetrofitPeertubeAPI.DataType.MUTED);
                    displayAccountsFragment.setArguments(bundle);
                    return displayAccountsFragment;
                case 2:
                    return new DisplayChannelsFragment();
                default:
                    return new DisplayNotificationsFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


}