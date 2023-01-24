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

import static app.fedilab.android.peertube.activities.PeertubeMainActivity.PICK_INSTANCE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityRegisterPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.AccountCreation;
import es.dmoral.toasty.Toasty;

public class PeertubeRegisterActivity extends BaseBarActivity {


    private String instance;
    private ActivityRegisterPeertubeBinding binding;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterPeertubeBinding.inflate(getLayoutInflater());
        View mainView = binding.getRoot();
        setContentView(mainView);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        binding.loginInstanceContainer.setVisibility(View.VISIBLE);
        binding.titleLoginInstance.setVisibility(View.VISIBLE);

        binding.username.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.username.getText() != null) {
                Pattern patternUsername = Pattern.compile("^[a-z0-9._]{1,50}$");
                Matcher matcherMaxId = patternUsername.matcher(binding.username.getText().toString());
                if (!matcherMaxId.matches()) {
                    binding.username.setError(getString(R.string.username_error));
                }
            }
        });


        binding.instanceHelp.setOnClickListener(v -> {
            Intent intent = new Intent(PeertubeRegisterActivity.this, InstancePickerActivity.class);
            startActivityForResult(intent, PICK_INSTANCE);
        });

        binding.email.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.email.getText() != null) {
                Pattern patternUsername = Patterns.EMAIL_ADDRESS;
                Matcher matcherMaxId = patternUsername.matcher(binding.email.getText().toString());
                if (!matcherMaxId.matches()) {
                    binding.email.setError(getString(R.string.email_error));
                }
            }
        });

        binding.password.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.password.getText() != null) {
                if (binding.password.getText().length() < 6) {
                    binding.password.setError(getString(R.string.password_length_error));
                }
            }
        });

        binding.passwordConfirm.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.passwordConfirm.getText() != null && binding.password.getText() != null) {
                if (binding.passwordConfirm.getText().toString().compareTo(binding.password.getText().toString()) != 0) {
                    binding.passwordConfirm.setError(getString(R.string.password));
                }
            }
        });
        setTextAgreement();
        binding.signup.setOnClickListener(view -> {
            binding.errorMessage.setVisibility(View.GONE);
            if (binding.username.getText() == null || binding.email.getText() == null || binding.password.getText() == null || binding.passwordConfirm.getText() == null || binding.username.getText().toString().trim().length() == 0 || binding.email.getText().toString().trim().length() == 0 ||
                    binding.password.getText().toString().trim().length() == 0 || binding.passwordConfirm.getText().toString().trim().length() == 0 || !binding.agreement.isChecked()) {
                Toasty.error(PeertubeRegisterActivity.this, getString(R.string.all_field_filled)).show();
                return;
            }

            if (!binding.password.getText().toString().trim().equals(binding.passwordConfirm.getText().toString().trim())) {
                Toasty.error(PeertubeRegisterActivity.this, getString(R.string.password_error)).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getText().toString().trim()).matches()) {
                Toasty.error(PeertubeRegisterActivity.this, getString(R.string.email_error)).show();
                return;
            }
            String[] emailArray = binding.email.getText().toString().split("@");


            if (binding.password.getText().toString().trim().length() < 8) {
                Toasty.error(PeertubeRegisterActivity.this, getString(R.string.password_too_short)).show();
                return;
            }
            if (binding.username.getText().toString().matches("[a-z0-9_]")) {
                Toasty.error(PeertubeRegisterActivity.this, getString(R.string.username_error)).show();
                return;
            }
            binding.signup.setEnabled(false);

            if (binding.loginInstance.getText() != null) {
                instance = binding.loginInstance.getText().toString();
            } else {
                instance = "";
            }
            binding.loginInstance.setOnFocusChangeListener((view1, focus) -> {
                if (!focus) {
                    setTextAgreement();
                }
            });
            if (instance != null) {
                instance = instance.toLowerCase().trim();
            }

            AccountCreation accountCreation = new AccountCreation();
            accountCreation.setEmail(binding.email.getText().toString().trim());
            accountCreation.setPassword(binding.password.getText().toString().trim());
            accountCreation.setPasswordConfirm(binding.passwordConfirm.getText().toString().trim());
            accountCreation.setUsername(binding.username.getText().toString().trim());
            accountCreation.setInstance(instance);

            new Thread(() -> {
                try {
                    APIResponse apiResponse = new RetrofitPeertubeAPI(PeertubeRegisterActivity.this, instance, null).createAccount(accountCreation);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Runnable myRunnable = () -> {
                        if (apiResponse.getError() != null) {
                            String errorMessage;
                            if (apiResponse.getError().getError() != null) {
                                try {
                                    String[] resp = apiResponse.getError().getError().split(":");
                                    if (resp.length == 2)
                                        errorMessage = apiResponse.getError().getError().split(":")[1];
                                    else if (resp.length == 3)
                                        errorMessage = apiResponse.getError().getError().split(":")[2];
                                    else
                                        errorMessage = getString(R.string.toast_error);
                                } catch (Exception e) {
                                    errorMessage = getString(R.string.toast_error);
                                }
                            } else {
                                errorMessage = getString(R.string.toast_error);
                            }
                            binding.errorMessage.setText(errorMessage);
                            binding.errorMessage.setVisibility(View.VISIBLE);
                            binding.signup.setEnabled(true);
                            return;
                        }

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PeertubeRegisterActivity.this);
                        dialogBuilder.setCancelable(false);
                        dialogBuilder.setPositiveButton(R.string.validate, (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        });
                        AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.setTitle(getString(R.string.account_created));
                        alertDialog.setMessage(getString(R.string.account_created_message, apiResponse.getStringData()));
                        alertDialog.show();
                    };
                    mainHandler.post(myRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        });

        setTitle(R.string.create_an_account);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_INSTANCE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                String instance = String.valueOf(data.getData());
                binding.loginInstance.setText(instance);
                binding.loginInstance.setSelection(instance.length());
                setTextAgreement();
            }
        }
    }

    private void setTextAgreement() {
        TextView agreement_text = findViewById(R.id.agreement_text);
        String tos = getString(R.string.tos);
        String serverrules = getString(R.string.server_rules);
        String content_agreement = null;
        agreement_text.setMovementMethod(null);
        agreement_text.setText(null);
        if (binding.loginInstance.getText() != null) {
            content_agreement = getString(R.string.agreement_check_peertube,
                    "<a href='https://" + binding.loginInstance.getText().toString() + "/about/instance#terms-section' >" + tos + "</a>"
            );
        }
        agreement_text.setMovementMethod(LinkMovementMethod.getInstance());
        if (content_agreement != null) {
            agreement_text.setText(Html.fromHtml(content_agreement));
        }
    }

}