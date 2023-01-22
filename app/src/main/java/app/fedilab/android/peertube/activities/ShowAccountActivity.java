package app.fedilab.android.peertube.activities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.MUTE;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.AccountData;
import app.fedilab.android.peertube.fragment.DisplayChannelsFragment;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.Theme;
import app.fedilab.android.peertube.viewmodel.AccountsVM;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class ShowAccountActivity extends BaseActivity {


    private ViewPager mPager;
    private TabLayout tabLayout;
    private TextView account_note, subscriber_count;
    private ImageView account_pp;
    private TextView account_dn;
    private AccountData.Account account;
    private String accountAcct;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.setTheme(this, HelperInstance.getLiveInstance(this), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_account);
        setTitle("");
        Bundle b = getIntent().getExtras();
        subscriber_count = findViewById(R.id.subscriber_count);
        account_pp = findViewById(R.id.account_pp);
        account_dn = findViewById(R.id.account_dn);
        account_pp.setBackgroundResource(R.drawable.account_pp_border);
        if (b != null) {
            account = b.getParcelable("account");
            accountAcct = b.getString("accountAcct");
        } else {
            Toasty.error(ShowAccountActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tabLayout = findViewById(R.id.account_tabLayout);
        account_note = findViewById(R.id.account_note);

        manageAccount();
        AccountsVM viewModel = new ViewModelProvider(ShowAccountActivity.this).get(AccountsVM.class);
        viewModel.getAccount(accountAcct == null ? account.getUsername() + "@" + account.getHost() : accountAcct).observe(ShowAccountActivity.this, this::manageViewAccounts);
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_account, menu);
        if (!Helper.isLoggedIn(ShowAccountActivity.this)) {
            menu.findItem(R.id.action_mute).setVisible(false);
        }
        menu.findItem(R.id.action_display_account).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_mute) {
            PostActionsVM viewModel = new ViewModelProvider(ShowAccountActivity.this).get(PostActionsVM.class);
            viewModel.post(MUTE, accountAcct == null ? account.getUsername() + "@" + account.getHost() : accountAcct, null).observe(ShowAccountActivity.this, apiResponse -> manageVIewPostActions(MUTE, apiResponse));
        } else if (item.getItemId() == R.id.action_report) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ShowAccountActivity.this);
            LayoutInflater inflater1 = getLayoutInflater();
            View dialogView = inflater1.inflate(R.layout.popup_report, new LinearLayout(ShowAccountActivity.this), false);
            dialogBuilder.setView(dialogView);
            EditText report_content = dialogView.findViewById(R.id.report_content);
            dialogBuilder.setNeutralButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            dialogBuilder.setPositiveButton(R.string.report, (dialog, id) -> {
                if (report_content.getText().toString().trim().length() == 0) {
                    Toasty.info(ShowAccountActivity.this, getString(R.string.report_comment_size), Toasty.LENGTH_LONG).show();
                } else {
                    PostActionsVM viewModel = new ViewModelProvider(ShowAccountActivity.this).get(PostActionsVM.class);
                    viewModel.post(REPORT_ACCOUNT, account.getId(), report_content.getText().toString()).observe(ShowAccountActivity.this, apiResponse -> manageVIewPostActions(REPORT_ACCOUNT, apiResponse));
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        } else if (item.getItemId() == R.id.action_share && account != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_via));
            String extra_text = account.getUrl();
            sendIntent.putExtra(Intent.EXTRA_TEXT, extra_text);
            sendIntent.setType("text/plain");
            try {
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_with)));
            } catch (Exception e) {
                Toasty.error(ShowAccountActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void manageAccount() {


        setTitle(account.getAcct());

        mPager = findViewById(R.id.account_viewpager);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.channels)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.videos)));
        mPager.setOffscreenPageLimit(2);

        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                TabLayout.Tab tab = tabLayout.getTabAt(position);
                if (tab != null)
                    tab.select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = null;
                if (mPager.getAdapter() != null)
                    fragment = (Fragment) mPager.getAdapter().instantiateItem(mPager, tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        if (fragment != null) {
                            DisplayChannelsFragment displayChannelsFragment = ((DisplayChannelsFragment) fragment);
                            displayChannelsFragment.scrollToTop();
                        }
                        break;
                    case 1:
                        if (fragment != null) {
                            DisplayVideosFragment displayVideosFragment = ((DisplayVideosFragment) fragment);
                            displayVideosFragment.scrollToTop();
                        }
                        break;
                }
            }
        });

        account_dn.setText(account.getDisplayName());

        manageNotes(account);
        Helper.loadAvatar(ShowAccountActivity.this, account, account_pp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, APIResponse apiResponse) {

        if (apiResponse.getError() != null) {
            Toasty.error(ShowAccountActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        if (statusAction == RetrofitPeertubeAPI.ActionType.MUTE) {
            Toasty.info(ShowAccountActivity.this, getString(R.string.muted_done), Toast.LENGTH_LONG).show();
        }
    }

    public void manageViewAccounts(APIResponse apiResponse) {
        if (apiResponse.getAccounts() != null && apiResponse.getAccounts().size() == 1) {
            AccountData.Account account = apiResponse.getAccounts().get(0);
            if (this.account == null) {
                this.account = account;
                manageAccount();
            }
            subscriber_count.setText(getString(R.string.followers_count, Helper.withSuffix(account.getFollowersCount())));
            subscriber_count.setVisibility(View.VISIBLE);
            manageNotes(account);
        }
    }

    private void manageNotes(AccountData.Account account) {
        if (account.getDescription() != null && account.getDescription().compareTo("null") != 0 && account.getDescription().trim().length() > 0) {
            SpannableString spannableString;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                spannableString = new SpannableString(Html.fromHtml(account.getDescription(), FROM_HTML_MODE_LEGACY));
            else
                spannableString = new SpannableString(Html.fromHtml(account.getDescription()));

            account_note.setText(spannableString, TextView.BufferType.SPANNABLE);
            account_note.setMovementMethod(LinkMovementMethod.getInstance());
            account_note.setVisibility(View.VISIBLE);
        } else {
            account_note.setVisibility(View.GONE);
        }
    }


    /**
     * Pager adapter for the 2 fragments
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {
                DisplayChannelsFragment displayChannelsFragment = new DisplayChannelsFragment();
                bundle.putString("name", account.getAcct());
                displayChannelsFragment.setArguments(bundle);
                return displayChannelsFragment;
            }
            DisplayVideosFragment displayVideosFragment = new DisplayVideosFragment();
            bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.ACCOUNT_VIDEOS);
            bundle.putParcelable("account", account);
            bundle.putString("peertube_instance", account.getHost());
            displayVideosFragment.setArguments(bundle);
            return displayVideosFragment;
        }


        @Override
        public int getCount() {
            return 2;
        }
    }

}
