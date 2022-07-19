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


import static app.fedilab.android.BaseMainActivity.currentAccount;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.tabs.TabLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Attachment;
import app.fedilab.android.client.entities.api.Field;
import app.fedilab.android.client.entities.api.IdentityProof;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.api.RelationShip;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.RemoteInstance;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.client.entities.app.WellKnownNodeinfo;
import app.fedilab.android.databinding.ActivityProfileBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.CrossActionHelper;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.helper.SpannableHelper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.FieldAdapter;
import app.fedilab.android.ui.drawer.IdentityProofsAdapter;
import app.fedilab.android.ui.pageadapter.FedilabProfileTLPageAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import app.fedilab.android.viewmodel.mastodon.ReorderVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;
import es.dmoral.toasty.Toasty;


public class ProfileActivity extends BaseActivity {


    private RelationShip relationship;
    private Account account;
    private ScheduledExecutorService scheduledExecutorService;
    private action doAction;
    private AccountsVM accountsVM;
    private RecyclerView identityProofsRecycler;
    private List<IdentityProof> identityProofList;
    private ActivityProfileBinding binding;
    private String account_id;
    private String mention_str;
    private WellKnownNodeinfo.NodeInfo nodeInfo;

    private final BroadcastReceiver broadcast_data = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                Account accountReceived = (Account) b.getSerializable(Helper.ARG_ACCOUNT);
                if (b.getBoolean(Helper.RECEIVE_REDRAW_PROFILE, false) && accountReceived != null) {
                    if (account != null && accountReceived.id != null && account.id != null && accountReceived.id.equalsIgnoreCase(account.id)) {
                        initializeView(accountReceived);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        Bundle b = getIntent().getExtras();
        binding.accountFollow.setEnabled(false);
        if (b != null) {
            account = (Account) b.getSerializable(Helper.ARG_ACCOUNT);
            account_id = b.getString(Helper.ARG_USER_ID, null);
            mention_str = b.getString(Helper.ARG_MENTION, null);
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
        accountsVM = new ViewModelProvider(ProfileActivity.this).get(AccountsVM.class);
        if (account != null) {
            initializeView(account);
        } else if (account_id != null) {
            accountsVM.getAccount(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account_id).observe(ProfileActivity.this, fetchedAccount -> {
                account = fetchedAccount;
                initializeView(account);
            });
        } else if (mention_str != null) {
            accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mention_str, 1, true, false).observe(ProfileActivity.this, accounts -> {
                if (accounts != null && accounts.size() > 0) {
                    account = accounts.get(0);
                    initializeView(account);
                } else {
                    Toasty.error(ProfileActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            Toasty.error(ProfileActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
            finish();
        }
        LocalBroadcastManager.getInstance(ProfileActivity.this).registerReceiver(broadcast_data, new IntentFilter(Helper.BROADCAST_DATA));
    }

    private void initializeView(Account account) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this);
        if (account == null) {
            Toasty.error(ProfileActivity.this, getString(R.string.toast_error_loading_account), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        binding.title.setText(String.format(Locale.getDefault(), "@%s", account.acct));
        binding.headerEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

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


        //Retrieve relationship with the connected account
        List<String> accountListToCheck = new ArrayList<>();
        accountListToCheck.add(account.id);
        //Retrieve relation ship
        accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountListToCheck).observe(ProfileActivity.this, relationShips -> {
            if (relationShips != null && relationShips.size() > 0) {
                this.relationship = relationShips.get(0);
                updateAccount();
            }
        });
        //Retrieve identity proofs
        accountsVM.getIdentityProofs(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id).observe(ProfileActivity.this, identityProofs -> {
            this.identityProofList = identityProofs;
            updateAccount();
        });
        //Animate emojis
        if (account.emojis != null && account.emojis.size() > 0) {
            boolean disableAnimatedEmoji = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
            if (!disableAnimatedEmoji) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> binding.accountDn.invalidate(), 0, 130, TimeUnit.MILLISECONDS);
            }
        }
        binding.accountTabLayout.clearOnTabSelectedListeners();
        binding.accountTabLayout.removeAllTabs();
        //Tablayout for timelines/following/followers
        FedilabProfileTLPageAdapter fedilabProfileTLPageAdapter = new FedilabProfileTLPageAdapter(getSupportFragmentManager(), account);
        binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.status_cnt, Helper.withSuffix(account.statuses_count))));
        binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.following_cnt, Helper.withSuffix(account.following_count))));
        binding.accountTabLayout.addTab(binding.accountTabLayout.newTab().setText(getString(R.string.followers_cnt, Helper.withSuffix(account.followers_count))));
        binding.accountViewpager.setAdapter(fedilabProfileTLPageAdapter);
        binding.accountViewpager.setOffscreenPageLimit(3);
        binding.accountViewpager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.accountTabLayout));
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
            }
        });
        binding.accountTabLayout.setTabTextColors(ThemeHelper.getAttColor(ProfileActivity.this, R.attr.mTextColor), ContextCompat.getColor(ProfileActivity.this, R.color.cyanea_accent_dark_reference));
        binding.accountTabLayout.setTabIconTint(ThemeHelper.getColorStateList(ProfileActivity.this));
        boolean disableGif = sharedpreferences.getBoolean(getString(R.string.SET_DISABLE_GIF), false);
        String targetedUrl = disableGif ? account.avatar_static : account.avatar;
        Glide.with(ProfileActivity.this)
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
            Drawable img = ContextCompat.getDrawable(ProfileActivity.this, R.drawable.ic_baseline_lock_24);
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
        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ProfileActivity.this, R.color.cyanea_accent_reference)), 0, content.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.warningMessage.setText(content);
        binding.warningMessage.setOnClickListener(view -> {
            if (!account.url.toLowerCase().startsWith("http://") && !account.url.toLowerCase().startsWith("https://"))
                account.url = "http://" + account.url;
            Helper.openBrowser(ProfileActivity.this, account.url);
        });
        //Timed muted account
        if (account.mute_expires_at != null && account.mute_expires_at.after(new Date())) {
            binding.tempMute.setVisibility(View.VISIBLE);
            SpannableString content_temp_mute = new SpannableString(getString(R.string.timed_mute_profile, account.acct, account.mute_expires_at));
            content_temp_mute.setSpan(new UnderlineSpan(), 0, content_temp_mute.length(), 0);
            binding.tempMute.setText(content_temp_mute);
        }
        //This account was moved to another one
        if (account.moved != null) {
            binding.accountMoved.setVisibility(View.VISIBLE);
            Drawable imgTravel = ContextCompat.getDrawable(ProfileActivity.this, R.drawable.ic_baseline_card_travel_24);
            assert imgTravel != null;
            imgTravel.setBounds(0, 0, (int) (20 * scale + 0.5f), (int) (20 * scale + 0.5f));
            binding.accountMoved.setCompoundDrawables(imgTravel, null, null, null);
            //Retrieves content and make account names clickable
            SpannableString spannableString = SpannableHelper.moveToText(ProfileActivity.this, account);
            binding.accountMoved.setText(spannableString, TextView.BufferType.SPANNABLE);
            binding.accountMoved.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (account.acct != null && account.acct.contains("@"))
            binding.warningMessage.setVisibility(View.VISIBLE);
        else
            binding.warningMessage.setVisibility(View.GONE);


        //Fields for profile
        List<Field> fields = account.fields;
        if (fields != null && fields.size() > 0) {
            FieldAdapter fieldAdapter = new FieldAdapter(fields);
            binding.fieldsContainer.setAdapter(fieldAdapter);
            binding.fieldsContainer.setLayoutManager(new LinearLayoutManager(ProfileActivity.this));
        }

        binding.accountDn.setText(
                account.getSpanDisplayName(ProfileActivity.this,
                        new WeakReference<>(binding.accountDn)),
                TextView.BufferType.SPANNABLE);

        binding.accountUn.setText(String.format("@%s", account.acct));
        binding.accountUn.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String account_id = account.acct;
            if (account_id.split("@").length == 1)
                account_id += "@" + BaseMainActivity.currentInstance;
            ClipData clip = ClipData.newPlainText("mastodon_account_id", "@" + account_id);
            Toasty.info(ProfileActivity.this, getString(R.string.account_id_clipbloard), Toast.LENGTH_SHORT).show();
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);
            return false;
        });
        binding.accountNote.setText(
                account.getSpanNote(ProfileActivity.this,
                        new WeakReference<>(binding.accountNote)),
                TextView.BufferType.SPANNABLE);
        binding.accountNote.setMovementMethod(LinkMovementMethod.getInstance());

        MastodonHelper.loadPPMastodon(binding.accountPp, account);
        binding.accountPp.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MediaActivity.class);
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
                    .makeSceneTransitionAnimation(ProfileActivity.this, binding.accountPp, attachment.url);
            // start the new activity
            startActivity(intent, options.toBundle());
        });


        binding.accountNotification.setOnClickListener(v -> {
            if (relationship != null && relationship.followed_by) {
                relationship.notifying = !relationship.notifying;
                accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, relationship.showing_reblogs, relationship.notifying)
                        .observe(ProfileActivity.this, relationShip -> {
                            this.relationship = relationShip;
                            updateAccount();
                        });
            }
        });

        binding.accountFollow.setOnClickListener(v -> {
            if (doAction == action.NOTHING) {
                Toasty.info(ProfileActivity.this, getString(R.string.nothing_to_do), Toast.LENGTH_LONG).show();
            } else if (doAction == action.FOLLOW) {
                binding.accountFollow.setEnabled(false);
                accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false)
                        .observe(ProfileActivity.this, relationShip -> {
                            this.relationship = relationShip;
                            updateAccount();
                        });
            } else if (doAction == action.UNFOLLOW) {
                boolean confirm_unfollow = sharedpreferences.getBoolean(getString(R.string.SET_UNFOLLOW_VALIDATION), true);
                if (confirm_unfollow) {
                    AlertDialog.Builder unfollowConfirm = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                    unfollowConfirm.setTitle(getString(R.string.unfollow_confirm));
                    unfollowConfirm.setMessage(account.acct);
                    unfollowConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    unfollowConfirm.setPositiveButton(R.string.yes, (dialog, which) -> {
                        binding.accountFollow.setEnabled(false);
                        accountsVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                                .observe(ProfileActivity.this, relationShip -> {
                                    this.relationship = relationShip;
                                    updateAccount();
                                });
                        dialog.dismiss();
                    });
                    unfollowConfirm.show();
                } else {
                    binding.accountFollow.setEnabled(false);
                    binding.accountFollow.setEnabled(false);
                    accountsVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                            .observe(ProfileActivity.this, relationShip -> {
                                this.relationship = relationShip;
                                updateAccount();
                            });
                }

            } else if (doAction == action.UNBLOCK) {
                binding.accountFollow.setEnabled(false);
                accountsVM.unblock(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                        .observe(ProfileActivity.this, relationShip -> {
                            this.relationship = relationShip;
                            updateAccount();
                        });
            }
        });
        binding.accountNotification.setOnLongClickListener(v -> {
            CrossActionHelper.doCrossAction(ProfileActivity.this, CrossActionHelper.TypeOfCrossAction.FOLLOW_ACTION, account, null);
            return false;
        });

        binding.accountDate.setText(Helper.shortDateToString(account.created_at));
        binding.accountDate.setVisibility(View.VISIBLE);

        String[] accountInstanceArray = account.acct.split("@");
        String accountInstance = BaseMainActivity.currentInstance;
        if (accountInstanceArray.length > 1) {
            accountInstance = accountInstanceArray[1];
        }

        NodeInfoVM nodeInfoVM = new ViewModelProvider(ProfileActivity.this).get(NodeInfoVM.class);
        String finalAccountInstance = accountInstance;
        nodeInfoVM.getNodeInfo(accountInstance).observe(ProfileActivity.this, nodeInfo -> {
            this.nodeInfo = nodeInfo;
            if (nodeInfo != null && nodeInfo.software != null) {
                binding.instanceInfo.setText(nodeInfo.software.name);
                binding.instanceInfo.setVisibility(View.VISIBLE);

                binding.instanceInfo.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, InstanceProfileActivity.class);
                    Bundle b = new Bundle();
                    b.putString(Helper.ARG_INSTANCE, finalAccountInstance);
                    intent.putExtras(b);
                    startActivity(intent);

                });
            }
        });

    }


    /***
     * This methode is called to update the view once an action has been performed
     */
    private void updateAccount() {

        //The value for account is from same server so id can be used
        if (account.id.equals(currentAccount.user_id)) {
            binding.accountFollow.setVisibility(View.GONE);
            binding.headerEditProfile.setVisibility(View.VISIBLE);
            binding.headerEditProfile.bringToFront();
        }
        //Manage indentity proofs if not yet displayed

        if (identityProofList != null && identityProofList.size() > 0) {
            ImageView identity_proofs_indicator = findViewById(R.id.identity_proofs_indicator);
            identity_proofs_indicator.setVisibility(View.VISIBLE);
            //Recyclerview for identity proof has not been inflated yet
            if (identityProofsRecycler == null) {
                identity_proofs_indicator.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                    identityProofsRecycler = new RecyclerView(ProfileActivity.this);
                    LinearLayoutManager mLayoutManager = new LinearLayoutManager(ProfileActivity.this);
                    identityProofsRecycler.setLayoutManager(mLayoutManager);
                    IdentityProofsAdapter identityProofsAdapter = new IdentityProofsAdapter(identityProofList);
                    identityProofsRecycler.setAdapter(identityProofsAdapter);
                    builder.setView(identityProofsRecycler);
                    builder
                            .setTitle(R.string.identity_proofs)
                            .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                            .show();
                });
            }
        }
        binding.accountFollow.setBackgroundTintList(ThemeHelper.getButtonActionColorStateList(ProfileActivity.this));
        binding.accountFollow.setEnabled(true);
        //Visibility depending of the relationship
        if (relationship != null) {
            if (relationship.blocked_by) {
                binding.accountFollow.setImageResource(R.drawable.ic_baseline_person_add_24);
                binding.accountFollow.setVisibility(View.VISIBLE);
                binding.accountFollow.setEnabled(false);
                binding.accountFollow.setContentDescription(getString(R.string.action_disabled));
            }

            if (relationship.requested) {
                binding.accountFollowRequest.setVisibility(View.VISIBLE);
                binding.accountFollow.setImageResource(R.drawable.ic_baseline_hourglass_full_24);
                binding.accountFollow.setVisibility(View.VISIBLE);
                binding.accountFollow.setContentDescription(getString(R.string.follow_request));
                doAction = action.UNFOLLOW;
            }
            if (relationship.followed_by) {
                binding.accountFollowedBy.setVisibility(View.VISIBLE);
            } else {
                binding.accountFollowedBy.setVisibility(View.GONE);
            }
            if (relationship.following) {
                binding.accountFollow.setImageResource(R.drawable.ic_baseline_person_remove_24);
                binding.accountFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProfileActivity.this, R.color.red_1)));
                doAction = action.UNFOLLOW;
                binding.accountFollow.setContentDescription(getString(R.string.action_unfollow));
                binding.accountFollow.setVisibility(View.VISIBLE);
            } else if (relationship.blocking) {
                binding.accountFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProfileActivity.this, R.color.red_1)));
                binding.accountFollow.setImageResource(R.drawable.ic_baseline_lock_open_24);
                doAction = action.UNBLOCK;
                binding.accountFollow.setVisibility(View.VISIBLE);
                binding.accountFollow.setContentDescription(getString(R.string.action_unblock));
            } else {
                binding.accountFollow.setImageResource(R.drawable.ic_baseline_person_add_24);
                doAction = action.FOLLOW;
                binding.accountFollow.setVisibility(View.VISIBLE);
                binding.accountFollow.setContentDescription(getString(R.string.action_follow));
            }
            if (!relationship.following) {
                binding.accountNotification.setVisibility(View.GONE);
            } else {
                binding.accountNotification.setVisibility(View.VISIBLE);
            }
            if (relationship.notifying) {
                binding.accountNotification.setImageResource(R.drawable.ic_baseline_notifications_active_24);
            } else {
                binding.accountNotification.setImageResource(R.drawable.ic_baseline_notifications_off_24);
            }
            //Account note
            if (relationship.note == null || relationship.note.trim().isEmpty()) {
                binding.personalNote.setText(R.string.note_for_account);
            } else {
                binding.personalNote.setText(relationship.note);
            }
            binding.personalNote.setOnClickListener(view -> {
                AlertDialog.Builder builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                builderInner.setTitle(R.string.note_for_account);
                EditText input = new EditText(ProfileActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                input.setLayoutParams(lp);
                input.setSingleLine(false);
                input.setText(relationship.note);
                input.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                builderInner.setView(input);
                builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builderInner.setPositiveButton(R.string.validate, (dialog, which) -> {
                    String notes = input.getText().toString().trim();
                    binding.personalNote.setText(notes);
                    if (relationship != null) {
                        relationship.note = notes;
                    }
                    accountsVM.updateNote(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, notes);
                    dialog.dismiss();
                });
                builderInner.show();
            });
        }

        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.activity_profile, menu);
        if (account != null) {
            final boolean isOwner = account.id != null && BaseMainActivity.currentUserID != null && account.id.compareToIgnoreCase(BaseMainActivity.currentUserID) == 0;
            String[] splitAcct = account.acct.split("@");
            //check if user is from the same instance
            if (splitAcct.length <= 1) { //If yes, these entries must be hidden
                menu.findItem(R.id.action_follow_instance).setVisible(false);
                menu.findItem(R.id.action_block_instance).setVisible(false);
            }
            if (isOwner) {
                menu.findItem(R.id.action_block).setVisible(false);
                menu.findItem(R.id.action_report).setVisible(false);
                menu.findItem(R.id.action_mute).setVisible(false);
                menu.findItem(R.id.action_mention).setVisible(false);
                menu.findItem(R.id.action_follow_instance).setVisible(false);
                menu.findItem(R.id.action_block_instance).setVisible(false);
                menu.findItem(R.id.action_hide_boost).setVisible(false);
                menu.findItem(R.id.action_endorse).setVisible(false);
                menu.findItem(R.id.action_direct_message).setVisible(false);
                menu.findItem(R.id.action_add_to_list).setVisible(false);
            } else {
                menu.findItem(R.id.action_block).setVisible(true);
                menu.findItem(R.id.action_mute).setVisible(true);
                menu.findItem(R.id.action_mention).setVisible(true);
            }
            //Update menu title depending of relationship
            if (relationship != null) {
                if (!relationship.following) {
                    menu.findItem(R.id.action_hide_boost).setVisible(false);
                    menu.findItem(R.id.action_endorse).setVisible(false);
                }
                if (relationship.blocking) {
                    menu.findItem(R.id.action_block).setTitle(R.string.action_unblock);
                }
                if (relationship.muting) {
                    menu.findItem(R.id.action_mute).setTitle(R.string.action_unmute);
                }
                if (relationship.endorsed) {
                    menu.findItem(R.id.action_endorse).setTitle(R.string.unendorse);
                } else {
                    menu.findItem(R.id.action_endorse).setTitle(R.string.endorse);
                }
                if (relationship.showing_reblogs) {
                    menu.findItem(R.id.action_hide_boost).setTitle(getString(R.string.hide_boost, account.username));
                } else {
                    menu.findItem(R.id.action_hide_boost).setTitle(getString(R.string.show_boost, account.username));
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        String[] splitAcct = account.acct.split("@");
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this);
        AlertDialog.Builder builderInner;
        final boolean isOwner = account.id.compareToIgnoreCase(BaseMainActivity.currentUserID) == 0;
        final String[] stringArrayConf;
        if (isOwner) {
            stringArrayConf = getResources().getStringArray(R.array.more_action_owner_confirm);
        } else {
            stringArrayConf = getResources().getStringArray(R.array.more_action_confirm);
        }
        action doActionAccount;
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_follow_instance) {
            String finalInstanceName = splitAcct[1];
            ReorderVM reorderVM = new ViewModelProvider(ProfileActivity.this).get(ReorderVM.class);
            //Get pinned instances
            reorderVM.getPinned().observe(ProfileActivity.this, pinned -> {
                boolean alreadyPinned = false;
                boolean present = true;
                if (pinned == null) {
                    pinned = new Pinned();
                    pinned.pinnedTimelines = new ArrayList<>();
                    present = false;
                } else {
                    for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                        if (pinnedTimeline.remoteInstance != null && pinnedTimeline.remoteInstance.host.compareToIgnoreCase(finalInstanceName) == 0) {
                            alreadyPinned = true;
                            break;
                        }
                    }
                }
                if (alreadyPinned) {
                    Toasty.info(ProfileActivity.this, getString(R.string.toast_instance_already_added), Toast.LENGTH_LONG).show();
                    return;
                }
                RemoteInstance.InstanceType instanceType;
                if (nodeInfo != null) {
                    if (nodeInfo.software.name.compareToIgnoreCase("peertube") == 0) {
                        instanceType = RemoteInstance.InstanceType.PEERTUBE;
                    } else if (nodeInfo.software.name.compareToIgnoreCase("pixelfed") == 0) {
                        instanceType = RemoteInstance.InstanceType.PIXELFED;
                    } else if (nodeInfo.software.name.compareToIgnoreCase("misskey") == 0) {
                        instanceType = RemoteInstance.InstanceType.MISSKEY;
                    } else if (nodeInfo.software.name.compareToIgnoreCase("gnu") == 0) {
                        instanceType = RemoteInstance.InstanceType.GNU;
                    } else {
                        instanceType = RemoteInstance.InstanceType.MASTODON;
                    }
                } else {
                    instanceType = RemoteInstance.InstanceType.MASTODON;
                }
                RemoteInstance remoteInstance = new RemoteInstance();
                remoteInstance.type = instanceType;
                remoteInstance.host = finalInstanceName;
                PinnedTimeline pinnedTimeline = new PinnedTimeline();
                pinnedTimeline.remoteInstance = remoteInstance;
                pinnedTimeline.displayed = true;
                pinnedTimeline.type = Timeline.TimeLineEnum.REMOTE;
                pinnedTimeline.position = pinned.pinnedTimelines.size();
                pinned.pinnedTimelines.add(pinnedTimeline);
                Pinned finalPinned = pinned;
                boolean finalPresent = present;
                new Thread(() -> {
                    try {
                        if (finalPresent) {
                            new Pinned(ProfileActivity.this).updatePinned(finalPinned);
                        } else {
                            new Pinned(ProfileActivity.this).insertPinned(finalPinned);
                        }
                        runOnUiThread(() -> {
                            Bundle b = new Bundle();
                            b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
                            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
                            intentBD.putExtras(b);
                            LocalBroadcastManager.getInstance(ProfileActivity.this).sendBroadcast(intentBD);
                        });
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }).start();

            });
            return true;
        } else if (itemId == R.id.action_filter) {
            AlertDialog.Builder filterTagDialog = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
            Set<String> featuredTagsSet = sharedpreferences.getStringSet(getString(R.string.SET_FEATURED_TAGS), null);
            List<String> tags = new ArrayList<>();
            if (featuredTagsSet != null) {
                tags = new ArrayList<>(featuredTagsSet);
            }
            tags.add(0, getString(R.string.no_tags));
            String[] tagsString = tags.toArray(new String[0]);
            List<String> finalTags = tags;
            String tag = sharedpreferences.getString(getString(R.string.SET_FEATURED_TAG_ACTION), null);
            int checkedposition = 0;
            int i = 0;
            for (String _t : tags) {
                if (_t.equals(tag))
                    checkedposition = i;
                i++;
            }
            filterTagDialog.setSingleChoiceItems(tagsString, checkedposition, (dialog, item1) -> {
                String tag1;
                if (item1 == 0) {
                    tag1 = null;
                } else {
                    tag1 = finalTags.get(item1);
                }
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(getString(R.string.SET_FEATURED_TAG_ACTION), tag1);
                editor.apply();
                dialog.dismiss();
            });
            filterTagDialog.show();
            return true;
        } else if (itemId == R.id.action_endorse) {
            if (relationship != null)
                if (relationship.endorsed) {
                    accountsVM.endorse(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                            .observe(ProfileActivity.this, relationShip -> this.relationship = relationShip);
                } else {
                    accountsVM.unendorse(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                            .observe(ProfileActivity.this, relationShip -> this.relationship = relationShip);
                }
            return true;
        } else if (itemId == R.id.action_hide_boost) {
            if (relationship != null)
                if (relationship.showing_reblogs) {
                    accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, false, relationship.notifying)
                            .observe(ProfileActivity.this, relationShip -> this.relationship = relationShip);
                } else {
                    accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, relationship.notifying)
                            .observe(ProfileActivity.this, relationShip -> this.relationship = relationShip);
                }
            return true;
        } else if (itemId == R.id.action_direct_message) {
            Intent intent = new Intent(ProfileActivity.this, ComposeActivity.class);
            Bundle b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT_MENTION, account);
            b.putString(Helper.ARG_VISIBILITY, "direct");
            intent.putExtras(b);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_add_to_list) {
            TimelinesVM timelinesVM = new ViewModelProvider(ProfileActivity.this).get(TimelinesVM.class);
            timelinesVM.getLists(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                    .observe(ProfileActivity.this, mastodonLists -> {
                        if (mastodonLists == null || mastodonLists.size() == 0) {
                            Toasty.info(ProfileActivity.this, getString(R.string.action_lists_empty), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        accountsVM.getListContainingAccount(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                                .observe(ProfileActivity.this, mastodonListUserIs -> {
                                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                                    builderSingle.setTitle(getString(R.string.action_lists_add_to));
                                    builderSingle.setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss());
                                    String[] listsId = new String[mastodonLists.size()];
                                    String[] listsArray = new String[mastodonLists.size()];
                                    boolean[] presentArray = new boolean[mastodonLists.size()];
                                    int i = 0;
                                    List<String> userIds = new ArrayList<>();
                                    userIds.add(account.id);
                                    for (MastodonList mastodonList : mastodonLists) {
                                        listsArray[i] = mastodonList.title;
                                        presentArray[i] = false;
                                        listsId[i] = mastodonList.id;
                                        for (MastodonList mastodonListPresent : mastodonListUserIs) {
                                            if (mastodonList.id.equalsIgnoreCase(mastodonListPresent.id)) {
                                                presentArray[i] = true;
                                                break;
                                            }
                                        }
                                        i++;
                                    }
                                    builderSingle.setMultiChoiceItems(listsArray, presentArray, (dialog, which, isChecked) -> {
                                        if (!relationship.following) {
                                            accountsVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, true, false)
                                                    .observe(ProfileActivity.this, newRelationShip -> {
                                                        relationship = newRelationShip;
                                                        updateAccount();
                                                        if (isChecked) {
                                                            timelinesVM.addAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, listsId[which], userIds);
                                                        } else {
                                                            timelinesVM.deleteAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, listsId[which], userIds);
                                                        }
                                                    });
                                        } else {
                                            if (isChecked) {
                                                timelinesVM.addAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, listsId[which], userIds);
                                            } else {
                                                timelinesVM.deleteAccountsList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, listsId[which], userIds);
                                            }
                                        }

                                    });
                                    builderSingle.show();
                                });

                    });
            return true;
        } else if (itemId == R.id.action_open_browser) {
            if (account.url != null) {
                if (!account.url.toLowerCase().startsWith("http://") && !account.url.toLowerCase().startsWith("https://"))
                    account.url = "http://" + account.url;
                Helper.openBrowser(ProfileActivity.this, account.url);
            }
            return true;
        } else if (itemId == R.id.action_mention) {
            Intent intent;
            Bundle b;
            intent = new Intent(ProfileActivity.this, ComposeActivity.class);
            b = new Bundle();
            b.putSerializable(Helper.ARG_ACCOUNT_MENTION, account);
            intent.putExtras(b);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_mute) {
            if (relationship.muting) {
                builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                builderInner.setTitle(stringArrayConf[4]);
                doActionAccount = action.UNMUTE;
            } else {
                builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
                builderInner.setTitle(stringArrayConf[0]);
                doActionAccount = action.MUTE;
            }
        } else if (itemId == R.id.action_report) {
            builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
            builderInner.setTitle(R.string.report_account);
            //Text for report
            EditText input = new EditText(ProfileActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            input.setLayoutParams(lp);
            builderInner.setView(input);
            builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builderInner.setPositiveButton(R.string.yes, (dialog, which) -> {
                String comment = null;
                if (input.getText() != null)
                    comment = input.getText().toString();
                accountsVM.report(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, null, null, null, comment, false);
                dialog.dismiss();
            });
            builderInner.show();
            return true;
        } else if (itemId == R.id.action_block) {
            builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
            if (relationship.blocking) {
                builderInner.setTitle(stringArrayConf[5]);
                doActionAccount = action.UNBLOCK;
            } else {
                builderInner.setTitle(stringArrayConf[1]);
                doActionAccount = action.BLOCK;
            }
        } else if (itemId == R.id.action_block_instance) {
            builderInner = new AlertDialog.Builder(ProfileActivity.this, Helper.dialogStyle());
            doActionAccount = action.BLOCK_DOMAIN;
            String domain = account.acct.split("@")[1];
            builderInner.setMessage(getString(R.string.block_domain_confirm_message, domain));
        } else {
            return true;
        }
        builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builderInner.setPositiveButton(R.string.yes, (dialog, which) -> {
            String target;
            if (item.getItemId() == R.id.action_block_instance) {
                target = account.acct.split("@")[1];
            } else {
                target = account.id;
            }
            switch (doActionAccount) {
                case MUTE:
                    accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, target, true, 0)
                            .observe(ProfileActivity.this, relationShip -> {
                                this.relationship = relationShip;
                                updateAccount();
                            });
                    break;
                case UNMUTE:
                    accountsVM.unmute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, target)
                            .observe(ProfileActivity.this, relationShip -> {
                                this.relationship = relationShip;
                                updateAccount();
                            });
                    break;
                case BLOCK:
                    accountsVM.block(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, target)
                            .observe(ProfileActivity.this, relationShip -> {
                                this.relationship = relationShip;
                                updateAccount();
                            });
                    break;
                case UNBLOCK:
                    accountsVM.unblock(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, target)
                            .observe(ProfileActivity.this, relationShip -> {
                                this.relationship = relationShip;
                                updateAccount();
                            });
                    break;
                case BLOCK_DOMAIN:
                    accountsVM.addDomainBlocks(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, target);
                    break;
            }
            dialog.dismiss();
        });
        builderInner.show();
        return true;
    }

    @Override
    public void onDestroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
        LocalBroadcastManager.getInstance(ProfileActivity.this).unregisterReceiver(broadcast_data);
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
