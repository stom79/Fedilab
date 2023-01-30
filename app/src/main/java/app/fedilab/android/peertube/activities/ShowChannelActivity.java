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

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.TypeOfConnection.SURFING;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.FOLLOW;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.MUTE;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPORT_ACCOUNT;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.UNFOLLOW;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.DataType.CHANNEL;
import static app.fedilab.android.peertube.helper.Helper.isLoggedIn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityShowChannelBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.ChannelData.Channel;
import app.fedilab.android.peertube.fragment.DisplayAccountsFragment;
import app.fedilab.android.peertube.fragment.DisplayVideosFragment;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.ChannelsVM;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import app.fedilab.android.peertube.viewmodel.RelationshipVM;
import app.fedilab.android.peertube.viewmodel.TimelineVM;
import es.dmoral.toasty.Toasty;


public class ShowChannelActivity extends BaseBarActivity {


    private Map<String, Boolean> relationship;
    private Channel channel;
    private action doAction;
    private String channelAcct;
    private boolean sepiaSearch;
    private String peertubeInstance;
    private ActivityShowChannelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShowChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("");
        Bundle b = getIntent().getExtras();
        binding.accountFollow.setEnabled(false);
        binding.accountPp.setBackgroundResource(R.drawable.account_pp_border);
        if (b != null) {
            channel = (Channel) b.getSerializable("channel");
            channelAcct = b.getString("channelId");
            sepiaSearch = b.getBoolean("sepia_search", false);
            peertubeInstance = b.getString("peertube_instance", null);

        } else {
            Toasty.error(ShowChannelActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ChannelsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(ChannelsVM.class);
        viewModel.get(sepiaSearch ? peertubeInstance : null, CHANNEL, channelAcct == null ? channel.getAcct() : channelAcct).observe(ShowChannelActivity.this, this::manageViewAccounts);
        manageChannel();


    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.main_account_peertube, menu);
        if (!Helper.isLoggedIn() || sepiaSearch) {
            menu.findItem(R.id.action_mute).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_mute) {
            PostActionsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(PostActionsVM.class);
            viewModel.post(MUTE, channel.getOwnerAccount().getAcct(), null).observe(ShowChannelActivity.this, apiResponse -> manageVIewPostActions(MUTE, apiResponse));
        } else if (item.getItemId() == R.id.action_report) {
            AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(ShowChannelActivity.this);
            LayoutInflater inflater1 = getLayoutInflater();
            View dialogView = inflater1.inflate(R.layout.popup_report_peertube, new LinearLayout(ShowChannelActivity.this), false);
            dialogBuilder.setView(dialogView);
            EditText report_content = dialogView.findViewById(R.id.report_content);
            dialogBuilder.setNeutralButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            dialogBuilder.setPositiveButton(R.string.report, (dialog, id) -> {
                if (report_content.getText().toString().trim().length() == 0) {
                    Toasty.info(ShowChannelActivity.this, getString(R.string.report_comment_size), Toasty.LENGTH_LONG).show();
                } else {
                    PostActionsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(PostActionsVM.class);
                    viewModel.post(REPORT_ACCOUNT, channel.getId(), report_content.getText().toString()).observe(ShowChannelActivity.this, apiResponse -> manageVIewPostActions(REPORT_ACCOUNT, apiResponse));
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        } else if (item.getItemId() == R.id.action_share && channel != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_via));
            String extra_text = channel.getUrl();
            sendIntent.putExtra(Intent.EXTRA_TEXT, extra_text);
            sendIntent.setType("text/plain");
            try {
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_with)));
            } catch (Exception e) {
                Toasty.error(ShowChannelActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
            }
        } else if (item.getItemId() == R.id.action_display_account) {
            Bundle b = new Bundle();
            Intent intent = new Intent(ShowChannelActivity.this, ShowAccountActivity.class);
            b.putSerializable("account", channel.getOwnerAccount());
            b.putString("accountAcct", channel.getOwnerAccount().getAcct());
            intent.putExtras(b);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void manageChannel() {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ShowChannelActivity.this);

        String accountIdRelation = channel.getAcct();
        if (isLoggedIn()) {
            RelationshipVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(RelationshipVM.class);
            List<String> uids = new ArrayList<>();
            uids.add(accountIdRelation);
            viewModel.get(uids).observe(ShowChannelActivity.this, this::manageVIewRelationship);
        }

        setTitle(channel.getAcct());

        binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.videos)));
        binding.accountViewpager.setOffscreenPageLimit(1);


        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        binding.accountViewpager.setAdapter(mPagerAdapter);
        ViewGroup.LayoutParams params = binding.accountTabLayout.getLayoutParams();
        params.height = 0;
        binding.accountTabLayout.setLayoutParams(params);
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
                        if (fragment != null) {
                            DisplayVideosFragment displayVideosFragment = ((DisplayVideosFragment) fragment);
                            displayVideosFragment.scrollToTop();
                        }
                        break;
                    case 1:
                        if (fragment != null) {
                            DisplayAccountsFragment displayAccountsFragment = ((DisplayAccountsFragment) fragment);
                            displayAccountsFragment.scrollToTop();
                        }
                        break;
                }
            }
        });

        binding.accountDn.setText(channel.getDisplayName());


        manageNotes(channel);
        Helper.loadAvatar(ShowChannelActivity.this, channel, binding.accountPp);
        //Follow button
        String target = channel.getAcct();

        binding.accountFollow.setOnClickListener(v -> {
            if (doAction == action.NOTHING) {
                Toasty.info(ShowChannelActivity.this, getString(R.string.nothing_to_do), Toast.LENGTH_LONG).show();
            } else if (doAction == action.FOLLOW) {
                binding.accountFollow.setEnabled(false);
                PostActionsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(PostActionsVM.class);
                viewModel.post(FOLLOW, target, null).observe(ShowChannelActivity.this, apiResponse -> manageVIewPostActions(FOLLOW, apiResponse));
            } else if (doAction == action.UNFOLLOW) {
                boolean confirm_unfollow = sharedpreferences.getBoolean(Helper.SET_UNFOLLOW_VALIDATION, true);
                if (confirm_unfollow) {
                    AlertDialog.Builder unfollowConfirm = new MaterialAlertDialogBuilder(ShowChannelActivity.this);
                    unfollowConfirm.setTitle(getString(R.string.unfollow_confirm));
                    unfollowConfirm.setMessage(channel.getAcct());
                    unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    unfollowConfirm.setPositiveButton(R.string.yes, (dialog, which) -> {
                        binding.accountFollow.setEnabled(false);
                        PostActionsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(PostActionsVM.class);
                        viewModel.post(UNFOLLOW, target, null).observe(ShowChannelActivity.this, apiResponse -> manageVIewPostActions(UNFOLLOW, apiResponse));
                        dialog.dismiss();
                    });
                    unfollowConfirm.show();
                } else {
                    binding.accountFollow.setEnabled(false);
                    PostActionsVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(PostActionsVM.class);
                    viewModel.post(UNFOLLOW, target, null).observe(ShowChannelActivity.this, apiResponse -> manageVIewPostActions(UNFOLLOW, apiResponse));
                }

            }
        });
    }


    public void manageVIewRelationship(APIResponse apiResponse) {

        if (apiResponse.getError() != null) {
            if (apiResponse.getError().getError().length() > 500) {
                Toasty.info(ShowChannelActivity.this, getString(R.string.remote_account), Toast.LENGTH_LONG).show();
            } else {
                Toasty.error(ShowChannelActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            }
            return;
        }
        this.relationship = apiResponse.getRelationships();
        manageButtonVisibility();

        invalidateOptionsMenu();

    }

    //Manages the visibility of the button
    private void manageButtonVisibility() {
        if (relationship == null || PeertubeMainActivity.typeOfConnection == SURFING || channel == null)
            return;
        binding.accountFollow.setEnabled(true);
        Boolean isFollowing = relationship.get(channel.getAcct());
        if (isFollowing != null && isFollowing) {
            binding.accountFollow.setText(R.string.action_unfollow);
            binding.accountFollow.setBackgroundTintList(ColorStateList.valueOf(Helper.getAttColor(ShowChannelActivity.this, R.attr.colorError)));
            doAction = action.UNFOLLOW;
        } else {
            binding.accountFollow.setText(R.string.action_follow);
            doAction = action.FOLLOW;
        }
        binding.accountFollow.setVisibility(View.VISIBLE);
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
            Toasty.error(ShowChannelActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        String target = channel.getAcct();
        //IF action is unfollow or mute, sends an intent to remove statuses
        if (statusAction == RetrofitPeertubeAPI.ActionType.UNFOLLOW) {
            Bundle b = new Bundle();
            b.putString("receive_action", apiResponse.getTargetedId());
            Intent intentBC = new Intent(Helper.RECEIVE_ACTION);
            intentBC.putExtras(b);
        }
        if (statusAction == RetrofitPeertubeAPI.ActionType.UNFOLLOW || statusAction == RetrofitPeertubeAPI.ActionType.FOLLOW) {
            RelationshipVM viewModel = new ViewModelProvider(ShowChannelActivity.this).get(RelationshipVM.class);
            List<String> uris = new ArrayList<>();
            uris.add(target);
            viewModel.get(uris).observe(ShowChannelActivity.this, this::manageVIewRelationship);
        } else if (statusAction == RetrofitPeertubeAPI.ActionType.MUTE) {
            Toasty.info(ShowChannelActivity.this, getString(R.string.muted_done), Toast.LENGTH_LONG).show();
        }
    }

    public void manageViewAccounts(APIResponse apiResponse) {
        if (apiResponse.getChannels() != null && apiResponse.getChannels().size() == 1) {
            Channel channel = apiResponse.getChannels().get(0);
            if (this.channel == null) {
                this.channel = channel;
                manageChannel();
            }
            if (channel.getOwnerAccount() != null) {
                this.channel.setOwnerAccount(channel.getOwnerAccount());
            }
            binding.subscriberCount.setText(getString(R.string.followers_count, Helper.withSuffix(channel.getFollowersCount())));
            binding.subscriberCount.setVisibility(View.VISIBLE);
            manageNotes(channel);
        }
    }

    private void manageNotes(Channel channel) {
        if (channel.getDescription() != null && channel.getDescription().compareTo("null") != 0 && channel.getDescription().trim().length() > 0) {
            SpannableString spannableString;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                spannableString = new SpannableString(Html.fromHtml(channel.getDescription(), FROM_HTML_MODE_LEGACY));
            else
                spannableString = new SpannableString(Html.fromHtml(channel.getDescription()));

            binding.accountNote.setText(spannableString, TextView.BufferType.SPANNABLE);
            binding.accountNote.setMovementMethod(LinkMovementMethod.getInstance());
            binding.accountNote.setVisibility(View.VISIBLE);
        } else {
            binding.accountNote.setVisibility(View.GONE);
        }
    }

    public enum action {
        FOLLOW,
        UNFOLLOW,
        NOTHING
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
            DisplayVideosFragment displayVideosFragment = new DisplayVideosFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.TIMELINE_TYPE, TimelineVM.TimelineType.CHANNEL_VIDEOS);
            bundle.putSerializable("channel", channel);
            bundle.putString("peertube_instance", channel.getHost());
            bundle.putBoolean("sepia_search", sepiaSearch);
            displayVideosFragment.setArguments(bundle);
            return displayVideosFragment;
        }


        @Override
        public int getCount() {
            return 1;
        }
    }

}
