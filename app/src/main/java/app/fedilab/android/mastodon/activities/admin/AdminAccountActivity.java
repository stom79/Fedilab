package app.fedilab.android.mastodon.activities.admin;
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


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityAdminAccountBinding;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.mastodon.activities.InstanceProfileActivity;
import app.fedilab.android.mastodon.activities.MediaActivity;
import app.fedilab.android.mastodon.client.entities.api.Attachment;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminAccount;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminIp;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.SpannableHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AdminVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.NodeInfoVM;
import es.dmoral.toasty.Toasty;


public class AdminAccountActivity extends BaseActivity {


    private ScheduledExecutorService scheduledExecutorService;
    private ActivityAdminAccountBinding binding;
    private String account_id;
    private AdminVM adminVM;
    private AdminAccount adminAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        Bundle args = getIntent().getExtras();
        adminAccount = null;
        postponeEnterTransition();

        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(AdminAccountActivity.this).getBundle(bundleId, Helper.getCurrentAccount(AdminAccountActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }


    }


    private void initializeAfterBundle(Bundle bundle) {

        if (bundle != null) {
            adminAccount = (AdminAccount) bundle.getSerializable(Helper.ARG_ACCOUNT);
            account_id = bundle.getString(Helper.ARG_ACCOUNT_ID, null);
        }
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float scale = sharedpreferences.getFloat(getString(R.string.SET_FONT_SCALE), 1.1f);
        binding.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * 1.1f / scale);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        adminVM = new ViewModelProvider(AdminAccountActivity.this).get(AdminVM.class);
        if (account_id != null) {
            adminVM.getAccount(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, this::initializeView);
            return;
        }
        if (adminAccount != null && adminAccount.account != null) {
            initializeView(adminAccount);
        } else {
            Toasty.error(AdminAccountActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
        }

        binding.disableAction.setOnClickListener(v -> {
            if (adminAccount.disabled) {
                adminVM.enable(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount.disabled = false;
                            binding.disableAction.setText(R.string.disable);
                            binding.disabled.setText(R.string.no);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id, "disable ", null, null, null, null);
                adminAccount.disabled = true;
                binding.disableAction.setText(R.string.undisable);
                binding.disabled.setText(R.string.yes);
            }
        });

        binding.approveAction.setOnClickListener(v -> {
            if (adminAccount.approved) {
                adminVM.reject(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount = adminAccountResult;
                            initializeView(adminAccount);
                        });
            } else {
                adminVM.approve(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id);
                adminAccount.approved = true;
                initializeView(adminAccount);
            }
        });

        binding.silenceAction.setOnClickListener(v -> {
            if (adminAccount.silenced) {
                adminVM.unsilence(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount = adminAccountResult;
                            initializeView(adminAccount);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id, "silence", null, null, null, null);
                adminAccount.silenced = true;
                initializeView(adminAccount);
            }
        });

        binding.suspendAction.setOnClickListener(v -> {
            if (adminAccount.suspended) {
                adminVM.unsuspend(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount = adminAccountResult;
                            initializeView(adminAccount);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, adminAccount.id, "suspend", null, null, null, null);
                adminAccount.suspended = true;
                initializeView(adminAccount);
            }
        });
    }

    private void initializeView(AdminAccount adminAccount) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(AdminAccountActivity.this);
        if (adminAccount == null) {
            Toasty.error(AdminAccountActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        binding.title.setText(String.format(Locale.getDefault(), "@%s", adminAccount.account.acct));

        // MastodonHelper.loadPPMastodon(binding.profilePicture, account);
        binding.appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {

            if (Math.abs(verticalOffset) - binding.appBar.getTotalScrollRange() == 0) {
                binding.profilePicture.setVisibility(View.VISIBLE);
                binding.title.setVisibility(View.VISIBLE);
            } else {
                binding.profilePicture.setVisibility(View.GONE);
                binding.title.setVisibility(View.GONE);
            }
        });

        binding.username.setText(String.format(Locale.getDefault(), "@%s", adminAccount.username));
        binding.domain.setText(adminAccount.domain);
        binding.email.setText(adminAccount.email);
        StringBuilder lastActive = new StringBuilder();
        if (adminAccount.ips != null) {
            int count = 0;
            for (AdminIp ip : adminAccount.ips) {
                lastActive.append(Helper.shortDateToString(ip.used_at)).append(" - ").append(ip.ip).append("\r\n");
                count++;
                if (count > 4) {
                    break;
                }
            }
        }
        if (lastActive.toString().trim().length() == 0) {
            binding.lastActiveContainer.setVisibility(View.GONE);
        }
        if (adminAccount.email == null || adminAccount.email.trim().length() == 0) {
            binding.emailContainer.setVisibility(View.GONE);
        }
        binding.lastActive.setText(lastActive.toString());
        binding.disabled.setText(adminAccount.disabled ? R.string.yes : R.string.no);
        binding.approved.setText(adminAccount.approved ? R.string.yes : R.string.no);
        binding.silenced.setText(adminAccount.silenced ? R.string.yes : R.string.no);
        binding.suspended.setText(adminAccount.suspended ? R.string.yes : R.string.no);

        binding.disableAction.setText(adminAccount.disabled ? R.string.undisable : R.string.disable);
        binding.approveAction.setText(adminAccount.approved ? R.string.reject : R.string.approve);
        binding.silenceAction.setText(adminAccount.silenced ? R.string.unsilence : R.string.silence);
        binding.suspendAction.setText(adminAccount.suspended ? R.string.unsuspend : R.string.suspend);


        //Animate emojis
        if (adminAccount.account.emojis != null && adminAccount.account.emojis.size() > 0) {
            boolean disableAnimatedEmoji = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
            if (!disableAnimatedEmoji) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> binding.accountDn.invalidate(), 0, 130, TimeUnit.MILLISECONDS);
            }
        }

        //Tablayout for timelines/following/followers

        boolean disableGif = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = disableGif ? adminAccount.account.avatar_static : adminAccount.account.avatar;
        Glide.with(AdminAccountActivity.this)
                .asDrawable()
                .dontTransform()
                .load(targetedUrl).into(
                        new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull final Drawable resource, Transition<? super Drawable> transition) {
                                binding.profilePicture.setImageDrawable(resource);
                                startPostponedEnterTransition();
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                binding.profilePicture.setImageResource(R.drawable.ic_person);
                                startPostponedEnterTransition();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        }
                );
        //Load header
        MastodonHelper.loadProfileMediaMastodon(AdminAccountActivity.this, binding.bannerPp, adminAccount.account, MastodonHelper.MediaAccountType.HEADER);
        //Redraws icon for locked accounts
        final float scale = getResources().getDisplayMetrics().density;
        if (adminAccount.account.locked) {
            Drawable img = ContextCompat.getDrawable(AdminAccountActivity.this, R.drawable.ic_baseline_lock_24);
            assert img != null;
            img.setBounds(0, 0, (int) (16 * scale + 0.5f), (int) (16 * scale + 0.5f));
            binding.accountUn.setCompoundDrawables(null, null, img, null);
        } else {
            binding.accountUn.setCompoundDrawables(null, null, null, null);
        }
        //Peertube account watched by a Mastodon account
        //Bot account
        if (adminAccount.account.bot) {
            binding.accountBot.setVisibility(View.VISIBLE);
        }
        if (adminAccount.account.acct != null) {
            setTitle(adminAccount.account.acct);
        }


        final SpannableString content = new SpannableString(getString(R.string.disclaimer_full));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        content.setSpan(new ForegroundColorSpan(ThemeHelper.getAttColor(this, R.attr.colorPrimary)), 0, content.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        //This account was moved to another one
        if (adminAccount.account.moved != null) {
            binding.accountMoved.setVisibility(View.VISIBLE);
            Drawable imgTravel = ContextCompat.getDrawable(AdminAccountActivity.this, R.drawable.ic_baseline_card_travel_24);
            assert imgTravel != null;
            imgTravel.setBounds(0, 0, (int) (20 * scale + 0.5f), (int) (20 * scale + 0.5f));
            binding.accountMoved.setCompoundDrawables(imgTravel, null, null, null);
            //Retrieves content and make account names clickable
            SpannableString spannableString = SpannableHelper.moveToText(AdminAccountActivity.this, adminAccount.account);
            binding.accountMoved.setText(spannableString, TextView.BufferType.SPANNABLE);
            binding.accountMoved.setMovementMethod(LinkMovementMethod.getInstance());
        }


        binding.accountDn.setText(
                adminAccount.account.getSpanDisplayNameEmoji(AdminAccountActivity.this,
                        new WeakReference<>(binding.accountDn)),
                TextView.BufferType.SPANNABLE);
        binding.accountUn.setText(String.format("@%s", adminAccount.account.acct));
        binding.accountUn.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String account_id = adminAccount.account.acct;
            if (account_id.split("@").length == 1)
                account_id += "@" + BaseMainActivity.currentInstance;
            ClipData clip = ClipData.newPlainText("mastodon_account_id", "@" + account_id);
            Toasty.info(AdminAccountActivity.this, getString(R.string.account_id_clipbloard), Toast.LENGTH_SHORT).show();
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);
            return false;
        });

        MastodonHelper.loadPPMastodon(binding.accountPp, adminAccount.account);
        binding.accountPp.setOnClickListener(v -> {
            Intent intent = new Intent(AdminAccountActivity.this, MediaActivity.class);
            Bundle args = new Bundle();
            Attachment attachment = new Attachment();
            attachment.description = adminAccount.account.acct;
            attachment.preview_url = adminAccount.account.avatar;
            attachment.url = adminAccount.account.avatar;
            attachment.remote_url = adminAccount.account.avatar;
            attachment.type = "image";
            ArrayList<Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            args.putSerializable(Helper.ARG_MEDIA_ARRAY, attachments);
            args.putInt(Helper.ARG_MEDIA_POSITION, 1);
            new CachedBundle(AdminAccountActivity.this).insertBundle(args, Helper.getCurrentAccount(AdminAccountActivity.this), bundleId -> {
                Bundle bundle = new Bundle();
                bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                intent.putExtras(bundle);
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(AdminAccountActivity.this, binding.accountPp, attachment.url);
                // start the new activity
                startActivity(intent, options.toBundle());
            });
        });


        binding.accountDate.setText(Helper.shortDateToString(adminAccount.created_at));
        binding.accountDate.setVisibility(View.VISIBLE);

        String[] accountInstanceArray = adminAccount.account.acct.split("@");
        String accountInstance = BaseMainActivity.currentInstance;
        if (accountInstanceArray.length > 1) {
            accountInstance = accountInstanceArray[1];
        }

        NodeInfoVM nodeInfoVM = new ViewModelProvider(AdminAccountActivity.this).get(NodeInfoVM.class);
        String finalAccountInstance = accountInstance;
        nodeInfoVM.getNodeInfo(accountInstance).observe(AdminAccountActivity.this, nodeInfo -> {
            if (nodeInfo != null && nodeInfo.software != null) {
                binding.instanceInfo.setText(nodeInfo.software.name);
                binding.instanceInfo.setVisibility(View.VISIBLE);

                binding.instanceInfo.setOnClickListener(v -> {
                    InstanceProfileActivity instanceProfileActivity = new InstanceProfileActivity();
                    Bundle b = new Bundle();
                    b.putString(Helper.ARG_INSTANCE, finalAccountInstance);
                    instanceProfileActivity.setArguments(b);
                    instanceProfileActivity.show(getSupportFragmentManager(), null);
                });
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
        super.onDestroy();
    }

    public enum action {
        FOLLOW,
        UNFOLLOW,
        BLOCK,
        UNBLOCK,
        NOTHING,
        MUTE,
        UNMUTE,
        REPORT,
        BLOCK_DOMAIN
    }


}
