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
import static app.fedilab.android.BaseMainActivity.instanceInfo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.Field;
import app.fedilab.android.databinding.AccountFieldItemBinding;
import app.fedilab.android.databinding.ActivityEditProfileBinding;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;

public class EditProfileActivity extends BaseBarActivity {

    public static final int PICK_MEDIA_AVATAR = 5705;
    public static final int PICK_MEDIA_HEADER = 5706;
    private ActivityEditProfileBinding binding;
    private AccountsVM accountsVM;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        new ViewModelProvider(EditProfileActivity.this).get(AccountsVM.class).getConnectedAccount(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                .observe(EditProfileActivity.this, account -> {
                    if (account != null) {
                        currentAccount.mastodon_account = account;
                        initializeView();
                    } else {
                        Helper.sendToastMessage(getApplication(), Helper.RECEIVE_TOAST_TYPE_ERROR, getString(R.string.toast_error));
                    }
                });
    }


    @SuppressWarnings("deprecation")
    private void initializeView() {
        //Hydrate values
        MastodonHelper.loadProfileMediaMastodon(EditProfileActivity.this, binding.bannerPp, currentAccount.mastodon_account, MastodonHelper.MediaAccountType.HEADER);
        MastodonHelper.loadPPMastodon(binding.accountPp, currentAccount.mastodon_account);
        binding.displayName.setText(currentAccount.mastodon_account.display_name);
        binding.acct.setText(String.format(Locale.getDefault(), "%s@%s", currentAccount.mastodon_account.acct, BaseMainActivity.currentInstance));
        String bio;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            bio = Html.fromHtml(currentAccount.mastodon_account.note, Html.FROM_HTML_MODE_LEGACY).toString();
        else
            bio = Html.fromHtml(currentAccount.mastodon_account.note).toString();
        binding.bio.setText(bio);
        if (currentAccount.mastodon_account.source != null) {
            binding.sensitive.setChecked(currentAccount.mastodon_account.source.sensitive);
            switch (currentAccount.mastodon_account.source.privacy) {
                case "public":
                    binding.visibilityPublic.setChecked(true);
                    break;
                case "unlisted":
                    binding.visibilityUnlisted.setChecked(true);
                    break;
                case "private":
                    binding.visibilityPrivate.setChecked(true);
                    break;
                case "direct":
                    binding.visibilityDirect.setChecked(true);
                    break;
            }
        } else {
            binding.sensitive.setVisibility(View.GONE);
            binding.visibilityGroup.setVisibility(View.GONE);
        }

        binding.bot.setChecked(currentAccount.mastodon_account.bot);
        binding.discoverable.setChecked(currentAccount.mastodon_account.discoverable);

        if (currentAccount.mastodon_account.locked) {
            binding.locked.setChecked(true);
        } else {
            binding.unlocked.setChecked(true);
        }
        List<Field> fields = currentAccount.mastodon_account.fields;
        if (fields != null && fields.size() > 0) {
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
                    AlertDialog.Builder deleteConfirm = new AlertDialog.Builder(EditProfileActivity.this);
                    deleteConfirm.setTitle(getString(R.string.delete_field));
                    deleteConfirm.setMessage(getString(R.string.delete_field_confirm));
                    deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                    deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                        binding.fieldsContainer.removeView(fieldItemBinding.getRoot());
                        if (binding.fieldsContainer.getChildCount() < 4) {
                            binding.fieldsContainer.setVisibility(View.VISIBLE);
                        } else {
                            binding.fieldsContainer.setVisibility(View.GONE);
                        }
                        dialog.dismiss();
                    });
                    deleteConfirm.create().show();
                });
                binding.fieldsContainer.addView(fieldItemBinding.getRoot());
            }

        }
        binding.addField.setOnClickListener(view -> {
            AccountFieldItemBinding fieldItemBinding = AccountFieldItemBinding.inflate(getLayoutInflater());
            fieldItemBinding.remove.setOnClickListener(v -> {
                AlertDialog.Builder deleteConfirm = new AlertDialog.Builder(EditProfileActivity.this);
                deleteConfirm.setTitle(getString(R.string.delete_field));
                deleteConfirm.setMessage(getString(R.string.delete_field_confirm));
                deleteConfirm.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                deleteConfirm.setPositiveButton(R.string.delete, (dialog, which) -> {
                    binding.fieldsContainer.removeView(fieldItemBinding.getRoot());
                    if (binding.fieldsContainer.getChildCount() < 4) {
                        binding.fieldsContainer.setVisibility(View.VISIBLE);
                    } else {
                        binding.fieldsContainer.setVisibility(View.GONE);
                    }
                    dialog.dismiss();
                });
                deleteConfirm.create().show();
            });
            binding.fieldsContainer.addView(fieldItemBinding.getRoot());
            if (binding.fieldsContainer.getChildCount() >= 4) {
                binding.addField.setVisibility(View.GONE);
            }
        });


        //Actions with the activity
        accountsVM = new ViewModelProvider(EditProfileActivity.this).get(AccountsVM.class);
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
                                currentAccount.mastodon_account = account;
                                Helper.recreateMainActivity(EditProfileActivity.this);
                                new Thread(() -> {
                                    try {
                                        new app.fedilab.android.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(currentAccount);
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
                            currentAccount.mastodon_account = account;
                            new Thread(() -> {
                                try {
                                    new app.fedilab.android.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(currentAccount);
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
        Bundle b = new Bundle();
        b.putBoolean(Helper.RECEIVE_REDRAW_PROFILE, true);
        b.putSerializable(Helper.ARG_ACCOUNT, account);
        Intent intentBD = new Intent(Helper.BROADCAST_DATA);
        intentBD.putExtras(b);
        LocalBroadcastManager.getInstance(EditProfileActivity.this).sendBroadcast(intentBD);
    }

    private Intent prepareIntent() {
        Intent intent;
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimetypes;
        long max_size = -1;
        if (instanceInfo.getMimeTypeImage().size() > 0) {
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
            field.name = ((TextInputEditText) binding.fieldsContainer.getChildAt(i).findViewById(R.id.name)).getText().toString().trim();
            field.value = ((TextInputEditText) binding.fieldsContainer.getChildAt(i).findViewById(R.id.value)).getText().toString().trim();
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
                            binding.displayName.getText().toString().trim(),
                            binding.bio.getText().toString(),
                            binding.locked.isChecked(),
                            getPrivacy(),
                            binding.sensitive.isChecked(),
                            null,
                            getFields()
                    )
                    .observe(EditProfileActivity.this, account -> {
                        if (account != null) {
                            currentAccount.mastodon_account = account;
                            new Thread(() -> {
                                try {
                                    new app.fedilab.android.client.entities.app.Account(EditProfileActivity.this).insertOrUpdate(currentAccount);
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
