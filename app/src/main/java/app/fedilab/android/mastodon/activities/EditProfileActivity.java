package app.fedilab.android.mastodon.activities;
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


import static app.fedilab.android.BaseMainActivity.instanceInfo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.BuildConfig;
import app.fedilab.android.R;
import app.fedilab.android.databinding.AccountFeaturedHashtagItemBinding;
import app.fedilab.android.databinding.AccountFieldItemBinding;
import app.fedilab.android.databinding.ActivityEditProfileBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.FeaturedTag;
import app.fedilab.android.mastodon.client.entities.api.Field;
import app.fedilab.android.mastodon.client.entities.api.Tag;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;

public class EditProfileActivity extends BaseBarActivity {

    public static final int PICK_MEDIA_AVATAR = 5705;
    public static final int PICK_MEDIA_HEADER = 5706;
    private ActivityEditProfileBinding binding;
    private AccountsVM accountsVM;
    private static final int MAX_FIELDS = 4;
    private static final int MAX_FEATURED_HASHTAGS = 10;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.scrollContainer.setOnTouchListener((v, event) -> {

            binding.bio.getParent().requestDisallowInterceptTouchEvent(false);

            return false;
        });

        binding.bio.setOnTouchListener((v, event) -> {

            binding.bio.getParent().requestDisallowInterceptTouchEvent(true);

            return false;
        });
        accountsVM = new ViewModelProvider(EditProfileActivity.this).get(AccountsVM.class);

        accountsVM.getConnectedAccount(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                .observe(EditProfileActivity.this, account -> {
                    if (account != null) {
                        Helper.setCurrentAccountMastodonAccount(EditProfileActivity.this, account);
                        initializeView();
                    } else {
                        Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                    }
                });


    }

    @SuppressWarnings("deprecation")
    private void initializeView() {
        //Hydrate values
        MastodonHelper.loadProfileMediaMastodon(EditProfileActivity.this, binding.bannerPp, Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account, MastodonHelper.MediaAccountType.HEADER);
        MastodonHelper.loadPPMastodon(binding.accountPp, Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account);
        binding.displayName.setText(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.display_name);
        binding.acct.setText(String.format(Locale.getDefault(), "%s@%s", Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.acct, BaseMainActivity.currentInstance));
        String bio;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            bio = Html.fromHtml(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.note, Html.FROM_HTML_MODE_LEGACY).toString();
        else
            bio = Html.fromHtml(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.note).toString();
        binding.bio.setText(bio);

        accountsVM.getFeaturedTagsSuggestions(BaseMainActivity.currentInstance, BaseMainActivity.currentToken).observe(this, featuredTags -> {
            StringBuilder text = new StringBuilder(getString(R.string.no_feature_hashtag_suggestion));
            if(featuredTags != null && !featuredTags.isEmpty()) {
                text = new StringBuilder();
                for (Tag tag : featuredTags) {
                    text.append(String.format("#%s ", tag.name));
                }
            }
            binding.featuredHashtagsSuggestions.setText(text);
        });
        accountsVM.getAccountFeaturedTags(BaseMainActivity.currentInstance, BaseMainActivity.currentToken,  Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.id).observe(this, featuredTags -> {
            if(featuredTags != null && !featuredTags.isEmpty()) {
                for (FeaturedTag featuredTag : featuredTags) {
                    AccountFeaturedHashtagItemBinding featuredHashtagItemBinding = AccountFeaturedHashtagItemBinding.inflate(getLayoutInflater());
                    featuredHashtagItemBinding.name.setText(featuredTag.name);
                    featuredHashtagItemBinding.remove.setOnClickListener(v -> {
                        AlertDialog.Builder deleteConfirm = new MaterialAlertDialogBuilder(EditProfileActivity.this);
                        deleteConfirm.setTitle(getString(R.string.delete_featured_hashtag));
                        deleteConfirm.setMessage(getString(R.string.delete_featured_hashtag_confirm));
                        deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                        deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                            ((ViewGroup)featuredHashtagItemBinding.getRoot().getParent()).removeView(featuredHashtagItemBinding.getRoot());
                            if (binding.featuredHashtagsContainer.getChildCount() >= MAX_FEATURED_HASHTAGS) {
                                binding.addFeaturedHashtags.setVisibility(View.GONE);
                            } else {
                                binding.addFeaturedHashtags.setVisibility(View.VISIBLE);
                            }
                            dialog.dismiss();
                        });
                        deleteConfirm.create().show();
                    });
                    binding.featuredHashtagsContainer.addView(featuredHashtagItemBinding.getRoot());
                }
            }
            binding.addFeaturedHashtags.setOnClickListener(view -> {
                AccountFeaturedHashtagItemBinding featuredHashtagItemBinding = AccountFeaturedHashtagItemBinding.inflate(getLayoutInflater());
                featuredHashtagItemBinding.remove.setOnClickListener(v -> {
                    AlertDialog.Builder deleteConfirm = new MaterialAlertDialogBuilder(EditProfileActivity.this);
                    deleteConfirm.setTitle(getString(R.string.delete_featured_hashtag));
                    deleteConfirm.setMessage(getString(R.string.delete_featured_hashtag_confirm));
                    deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                        ((ViewGroup)featuredHashtagItemBinding.getRoot().getParent()).removeView(featuredHashtagItemBinding.getRoot());
                        if (binding.featuredHashtagsContainer.getChildCount() >= MAX_FEATURED_HASHTAGS) {
                            binding.addFeaturedHashtags.setVisibility(View.GONE);
                        } else {
                            binding.addFeaturedHashtags.setVisibility(View.VISIBLE);
                        }
                        dialog.dismiss();
                    });
                    deleteConfirm.create().show();
                });
                binding.featuredHashtagsContainer.addView(featuredHashtagItemBinding.getRoot());

                if (binding.featuredHashtagsContainer.getChildCount() >= MAX_FEATURED_HASHTAGS) {
                    binding.addFeaturedHashtags.setVisibility(View.GONE);
                } else {
                    binding.addFeaturedHashtags.setVisibility(View.VISIBLE);
                }
            });
        });
        if (Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.source != null) {
            binding.sensitive.setChecked(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.source.sensitive);
            switch (Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.source.privacy) {
                case "public" -> binding.visibilityPublic.setChecked(true);
                case "unlisted" -> binding.visibilityUnlisted.setChecked(true);
                case "private" -> binding.visibilityPrivate.setChecked(true);
                case "direct" -> binding.visibilityDirect.setChecked(true);
            }
        } else {
            binding.sensitive.setVisibility(View.GONE);
            binding.visibilityGroup.setVisibility(View.GONE);
        }

        binding.bot.setChecked(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.bot);
        binding.discoverable.setChecked(Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.discoverable);

        if (Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.locked) {
            binding.locked.setChecked(true);
        } else {
            binding.unlocked.setChecked(true);
        }
        List<Field> fields = Helper.getCurrentAccount(EditProfileActivity.this).mastodon_account.fields;
        if (fields != null && !fields.isEmpty()) {
            for (Field field : fields) {
                AccountFieldItemBinding fieldItemBinding = AccountFieldItemBinding.inflate(getLayoutInflater());
                fieldItemBinding.name.setText(field.name);
                String value;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    value = Html.fromHtml(field.value, Html.FROM_HTML_MODE_LEGACY).toString();
                else
                    value = Html.fromHtml(field.value).toString();
                fieldItemBinding.value.setText(value);
                fieldItemBinding.remove.setOnClickListener(v -> {
                    AlertDialog.Builder deleteConfirm = new MaterialAlertDialogBuilder(EditProfileActivity.this);
                    deleteConfirm.setTitle(getString(R.string.delete_field));
                    deleteConfirm.setMessage(getString(R.string.delete_field_confirm));
                    deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                        ((ViewGroup)fieldItemBinding.getRoot().getParent()).removeView(fieldItemBinding.getRoot());
                        if (binding.fieldsContainer.getChildCount() >= MAX_FIELDS) {
                            binding.addField.setVisibility(View.GONE);
                        } else {
                            binding.addField.setVisibility(View.VISIBLE);
                        }
                        dialog.dismiss();
                    });
                    deleteConfirm.create().show();
                });
                binding.fieldsContainer.addView(fieldItemBinding.getRoot());
            }
            if (binding.fieldsContainer.getChildCount() >= MAX_FIELDS) {
                binding.addField.setVisibility(View.GONE);
            } else {
                binding.addField.setVisibility(View.VISIBLE);
            }
        }
        binding.addField.setOnClickListener(view -> {
            AccountFieldItemBinding fieldItemBinding = AccountFieldItemBinding.inflate(getLayoutInflater());
            fieldItemBinding.remove.setOnClickListener(v -> {
                AlertDialog.Builder deleteConfirm = new MaterialAlertDialogBuilder(EditProfileActivity.this);
                deleteConfirm.setTitle(getString(R.string.delete_field));
                deleteConfirm.setMessage(getString(R.string.delete_field_confirm));
                deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                    ((ViewGroup)fieldItemBinding.getRoot().getParent()).removeView(fieldItemBinding.getRoot());
                    if (binding.fieldsContainer.getChildCount() >= MAX_FIELDS) {
                        binding.addField.setVisibility(View.GONE);
                    } else {
                        binding.addField.setVisibility(View.VISIBLE);
                    }
                    dialog.dismiss();
                });
                deleteConfirm.create().show();
            });
            binding.fieldsContainer.addView(fieldItemBinding.getRoot());

            if (binding.fieldsContainer.getChildCount() >= MAX_FIELDS) {
                binding.addField.setVisibility(View.GONE);
            } else {
                binding.addField.setVisibility(View.VISIBLE);
            }
        });
        binding.headerSelect.setOnClickListener(view -> startActivityForResult(prepareIntent(), EditProfileActivity.PICK_MEDIA_HEADER));

        binding.avatarSelect.setOnClickListener(view -> startActivityForResult(prepareIntent(), EditProfileActivity.PICK_MEDIA_AVATAR));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MEDIA_AVATAR && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                binding.avatarProgress.setVisibility(View.VISIBLE);
                Glide.with(EditProfileActivity.this)
                        .asDrawable()
                        .load(uri)
                        .thumbnail(0.1f)
                        .into(binding.accountPp);
                accountsVM.updateProfilePicture(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, uri, AccountsVM.UpdateMediaType.AVATAR)
                        .observe(EditProfileActivity.this, account -> {
                            if (account != null) {
                                sendBroadCast(account);
                                binding.avatarProgress.setVisibility(View.GONE);
                                Helper.setCurrentAccountMastodonAccount(EditProfileActivity.this, account);
                                Helper.recreateMainActivity(EditProfileActivity.this);
                                new Thread(() -> {
                                    try {
                                        new app.fedilab.android.mastodon.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(Helper.getCurrentAccount(EditProfileActivity.this));
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            } else {
                                Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                            }
                        });
            } else {
                Toasty.error(EditProfileActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
            }
        } else if (requestCode == PICK_MEDIA_HEADER && resultCode == RESULT_OK) {
            Glide.with(EditProfileActivity.this)
                    .asDrawable()
                    .load(data.getData())
                    .thumbnail(0.1f)
                    .into(binding.bannerPp);
            binding.headerProgress.setVisibility(View.VISIBLE);
            accountsVM.updateProfilePicture(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, data.getData(), AccountsVM.UpdateMediaType.HEADER)
                    .observe(EditProfileActivity.this, account -> {
                        if (account != null) {
                            sendBroadCast(account);
                            binding.headerProgress.setVisibility(View.GONE);
                            Helper.setCurrentAccountMastodonAccount(EditProfileActivity.this, account);
                            new Thread(() -> {
                                try {
                                    new app.fedilab.android.mastodon.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(Helper.getCurrentAccount(EditProfileActivity.this));
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                            Helper.recreateMainActivity(EditProfileActivity.this);
                        } else {
                            Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                        }
                    });
        }
    }

    private void sendBroadCast(Account account) {
        Bundle args = new Bundle();
        args.putBoolean(Helper.RECEIVE_REDRAW_PROFILE, true);
        args.putSerializable(Helper.ARG_ACCOUNT, account);
        new CachedBundle(EditProfileActivity.this).insertBundle(args, Helper.getCurrentAccount(EditProfileActivity.this), bundleId -> {
            Bundle bundle = new Bundle();
            bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
            Intent intentBD = new Intent(Helper.BROADCAST_DATA);
            intentBD.putExtras(bundle);
            intentBD.setPackage(BuildConfig.APPLICATION_ID);
            sendBroadcast(intentBD);
        });

    }

    private Intent prepareIntent() {
        Intent intent;
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimetypes;
        long max_size = -1;
        if (!instanceInfo.getMimeTypeImage().isEmpty()) {
            mimetypes = instanceInfo.getMimeTypeImage().toArray(new String[0]);
            max_size = instanceInfo.configuration.media_attachments.image_size_limit;
        } else {
            mimetypes = new String[]{"image/*"};
        }
        if (max_size > 0) {
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, max_size);
        }
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private String getPrivacy() {
        if (binding.visibilityPublic.isChecked()) {
            return "public";
        } else if (binding.visibilityUnlisted.isChecked()) {
            return "unlisted";
        } else if (binding.visibilityPrivate.isChecked()) {
            return "private";
        } else if (binding.visibilityDirect.isChecked()) {
            return "direct";
        }
        return null;
    }

    LinkedHashMap<Integer, Field.FieldParams> getFields() {
        LinkedHashMap<Integer, Field.FieldParams> fields = new LinkedHashMap<>();
        for (int i = 0; i < binding.fieldsContainer.getChildCount(); i++) {
            Field.FieldParams field = new Field.FieldParams();
            field.name = Objects.requireNonNull(((TextInputEditText) binding.fieldsContainer.getChildAt(i).findViewById(R.id.name)).getText()).toString().trim();
            field.value = Objects.requireNonNull(((TextInputEditText) binding.fieldsContainer.getChildAt(i).findViewById(R.id.value)).getText()).toString().trim();
            fields.put(i, field);
        }
        return fields;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            accountsVM.updateCredentials(BaseMainActivity.currentInstance, BaseMainActivity.currentToken,
                            binding.discoverable.isChecked(),
                            binding.bot.isChecked(),
                            Objects.requireNonNull(binding.displayName.getText()).toString().trim(),
                            Objects.requireNonNull(binding.bio.getText()).toString(),
                            binding.locked.isChecked(),
                            getPrivacy(),
                            binding.sensitive.isChecked(),
                            null,
                            getFields()
                    )
                    .observe(EditProfileActivity.this, account -> {
                        if (account != null) {
                            Helper.setCurrentAccountMastodonAccount(EditProfileActivity.this, account);
                            new Thread(() -> {
                                try {
                                    new app.fedilab.android.mastodon.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(Helper.getCurrentAccount(EditProfileActivity.this));
                                    sendBroadCast(account);
                                } catch (DBException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                            Toasty.success(EditProfileActivity.this, getString(R.string.profiled_updated), Toasty.LENGTH_LONG).show();
                            finish();
                        } else {
                            Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                        }

                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
