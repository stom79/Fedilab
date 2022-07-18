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


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.AdminAccount;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.databinding.ActivityAdminAccountBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.viewmodel.mastodon.AdminVM;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import es.dmoral.toasty.Toasty;


public class AdminAccountActivity extends BaseActivity {

    private AdminAccount adminAccount;
    private Account account;
    private ScheduledExecutorService scheduledExecutorService;
    private ActivityAdminAccountBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityAdminAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            adminAccount = (AdminAccount) b.getSerializable(Helper.ARG_ACCOUNT);
            if (adminAccount != null) {
                account = adminAccount.account;
            }
        }
        postponeEnterTransition();

        //Remove title
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setPopupTheme(Helper.popupStyle());
        if (account != null) {
            initializeView(account);
        } else {
            Toasty.error(AdminAccountActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeView(Account account) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(AdminAccountActivity.this);
        if (account == null) {
            Toasty.error(AdminAccountActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        binding.title.setText(String.format(Locale.getDefault(), "@%s", account.acct));

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
            for (AdminAccount.IP ip : adminAccount.ips) {
                lastActive.append(Helper.shortDateToString(ip.used_at)).append(" - ").append(ip.ip).append("\r\n");
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

        AdminVM adminVM = new ViewModelProvider(AdminAccountActivity.this).get(AdminVM.class);

        binding.disableAction.setOnClickListener(v -> {
            if (adminAccount.disabled) {
                adminVM.enable(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount.disabled = false;
                            binding.disableAction.setText(R.string.disable);
                            binding.disabled.setText(R.string.no);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, "disable ", null, null, null, null);
                adminAccount.disabled = true;
                binding.disableAction.setText(R.string.undisable);
                binding.disabled.setText(R.string.yes);
            }
        });

        binding.approveAction.setOnClickListener(v -> {
            if (adminAccount.approved) {
                adminVM.reject(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount.approved = false;
                            binding.approveAction.setText(R.string.approve);
                            binding.approved.setText(R.string.no);
                        });
            } else {
                adminVM.approve(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id);
                adminAccount.approved = true;
                binding.approveAction.setText(R.string.reject);
                binding.approved.setText(R.string.yes);
            }
        });

        binding.silenceAction.setOnClickListener(v -> {
            if (adminAccount.disabled) {
                adminVM.unsilence(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount.silenced = false;
                            binding.silenceAction.setText(R.string.silence);
                            binding.disabled.setText(R.string.no);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, "silence", null, null, null, null);
                adminAccount.silenced = true;
                binding.disableAction.setText(R.string.unsilence);
                binding.disabled.setText(R.string.yes);
            }
        });

        binding.suspendAction.setOnClickListener(v -> {
            if (adminAccount.disabled) {
                adminVM.unsuspend(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe(AdminAccountActivity.this, adminAccountResult -> {
                            adminAccount.suspended = false;
                            binding.suspendAction.setText(R.string.suspend);
                            binding.suspended.setText(R.string.no);
                        });
            } else {
                adminVM.performAction(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, "suspend", null, null, null, null);
                adminAccount.suspended = true;
                binding.disableAction.setText(R.string.unsuspend);
                binding.suspended.setText(R.string.yes);
            }
        });


        //Retrieve relationship with the connected account
        List<String> accountListToCheck = new ArrayList<>();
        accountListToCheck.add(account.id);
        //Animate emojis
        if (account.emojis != null && account.emojis.size() > 0) {
            boolean disableAnimatedEmoji = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
            if (!disableAnimatedEmoji) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> binding.accountDn.invalidate(), 0, 130, TimeUnit.MILLISECONDS);
            }
        }

        //Tablayout for timelines/following/followers

        boolean disableGif = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = disableGif ? account.avatar_static : account.avatar;
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
        MastodonHelper.loadProfileMediaMastodon(binding.bannerPp, account, MastodonHelper.MediaAccountType.HEADER);
        //Redraws icon for locked accounts
        final float scale = getResources().getDisplayMetrics().density;
        if (account.locked) {
            Drawable img = ContextCompat.getDrawable(AdminAccountActivity.this, R.drawable.ic_baseline_lock_24);
            assert img != null;
            img.setBounds(0, 0, (int) (16 * scale + 0.5f), (int) (16 * scale + 0.5f));
            binding.accountUn.setCompoundDrawables(null, null, img, null);
        } else {
            binding.accountUn.setCompoundDrawables(null, null, null, null);
        }
        //Peertube account watched by a Mastodon account
        //Bot account
        if (account.bot) {
            binding.accountBot.setVisibility(View.VISIBLE);
        }
        if (account.acct != null) {
            setTitle(account.acct);
        }


        final SpannableString content = new SpannableString(getString(R.string.disclaimer_full));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(AdminAccountActivity.this, R.color.cyanea_accent_reference)), 0, content.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        //This account was moved to another one
        if (account.moved != null) {
            binding.accountMoved.setVisibility(View.VISIBLE);
            Drawable imgTravel = ContextCompat.getDrawable(AdminAccountActivity.this, R.drawable.ic_baseline_card_travel_24);
            assert imgTravel != null;
            imgTravel.setBounds(0, 0, (int) (20 * scale + 0.5f), (int) (20 * scale + 0.5f));
            binding.accountMoved.setCompoundDrawables(imgTravel, null, null, null);
            //Retrieves content and make account names clickable
            SpannableString spannableString = SpannableHelper.moveToText(AdminAccountActivity.this, account);
            binding.accountMoved.setText(spannableString, TextView.BufferType.SPANNABLE);
            binding.accountMoved.setMovementMethod(LinkMovementMethod.getInstance());
        }


        binding.accountDn.setText(
                account.getSpanDisplayName(AdminAccountActivity.this,
                        new WeakReference<>(binding.accountDn),
                        id -> binding.accountDn.invalidate()),
                TextView.BufferType.SPANNABLE);
        binding.accountUn.setText(String.format("@%s", account.acct));
        binding.accountUn.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String account_id = account.acct;
            if (account_id.split("@").length == 1)
                account_id += "@" + BaseMainActivity.currentInstance;
            ClipData clip = ClipData.newPlainText("mastodon_account_id", "@" + account_id);
            Toasty.info(AdminAccountActivity.this, getString(R.string.account_id_clipbloard), Toast.LENGTH_SHORT).show();
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);
            return false;
        });

        MastodonHelper.loadPPMastodon(binding.accountPp, account);
        binding.accountPp.setOnClickListener(v -> {
            Intent intent = new Intent(AdminAccountActivity.this, MediaActivity.class);
            Bundle b = new Bundle();
            Attachment attachment = new Attachment();
            attachment.description = account.acct;
            attachment.preview_url = account.avatar;
            attachment.url = account.avatar;
            attachment.remote_url = account.avatar;
            attachment.type = "image";
            ArrayList<Attachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            b.putSerializable(Helper.ARG_MEDIA_ARRAY, attachments);
            b.putInt(Helper.ARG_MEDIA_POSITION, 1);
            intent.putExtras(b);
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(AdminAccountActivity.this, binding.accountPp, attachment.url);
            // start the new activity
            startActivity(intent, options.toBundle());
        });


        binding.accountDate.setText(Helper.shortDateToString(account.created_at));
        binding.accountDate.setVisibility(View.VISIBLE);

        String[] accountInstanceArray = account.acct.split("@");
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
                    Intent intent = new Intent(AdminAccountActivity.this, InstanceProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putString(Helper.ARG_INSTANCE, finalAccountInstance);
                    intent.putExtras(b);
                    startActivity(intent);

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
