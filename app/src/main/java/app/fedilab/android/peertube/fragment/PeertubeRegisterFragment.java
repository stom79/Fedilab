package app.fedilab.android.peertube.fragment;
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

import static app.fedilab.android.peertube.activities.PeertubeMainActivity.INSTANCE_ADDRESS;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentLoginRegisterPeertubeBinding;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.AccountCreation;
import es.dmoral.toasty.Toasty;

public class PeertubeRegisterFragment extends Fragment {


    private String instance;
    private FragmentLoginRegisterPeertubeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginRegisterPeertubeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        binding.loginInstanceLayout.setVisibility(View.VISIBLE);

        binding.loginUsername.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.loginUsername.getText() != null) {
                Pattern patternUsername = Pattern.compile("^[a-z0-9._]{1,50}$");
                Matcher matcherMaxId = patternUsername.matcher(binding.loginUsername.getText().toString());
                if (!matcherMaxId.matches()) {
                    binding.loginUsername.setError(getString(R.string.username_error));
                }
            }
        });

        binding.loginEmail.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.loginEmail.getText() != null) {
                Pattern patternUsername = Patterns.EMAIL_ADDRESS;
                Matcher matcherMaxId = patternUsername.matcher(binding.loginEmail.getText().toString());
                if (!matcherMaxId.matches()) {
                    binding.loginEmail.setError(getString(R.string.email_error));
                }
            }
        });

        binding.loginPassword.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.loginPassword.getText() != null) {
                if (binding.loginPassword.getText().length() < 6) {
                    binding.loginPassword.setError(getString(R.string.password_length_error));
                }
            }
        });

        binding.loginPasswordConfirm.setOnFocusChangeListener((view, focused) -> {
            if (!focused && binding.loginPasswordConfirm.getText() != null && binding.loginPassword.getText() != null) {
                if (binding.loginPasswordConfirm.getText().toString().compareTo(binding.loginPassword.getText().toString()) != 0) {
                    binding.loginPasswordConfirm.setError(getString(R.string.password));
                }
            }
        });
        setTextAgreement();
        binding.signup.setOnClickListener(view -> {
            binding.errorMessage.setVisibility(View.GONE);
            if (binding.loginUsername.getText() == null || binding.loginEmail.getText() == null || binding.loginPassword.getText() == null || binding.loginPasswordConfirm.getText() == null || binding.loginUsername.getText().toString().trim().length() == 0 || binding.loginEmail.getText().toString().trim().length() == 0 ||
                    binding.loginPassword.getText().toString().trim().length() == 0 || binding.loginPasswordConfirm.getText().toString().trim().length() == 0 || !binding.agreement.isChecked()) {
                Toasty.error(requireContext(), getString(R.string.all_field_filled)).show();
                return;
            }

            if (!binding.loginPassword.getText().toString().trim().equals(binding.loginPasswordConfirm.getText().toString().trim())) {
                Toasty.error(requireContext(), getString(R.string.password_error)).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(binding.loginEmail.getText().toString().trim()).matches()) {
                Toasty.error(requireContext(), getString(R.string.email_error)).show();
                return;
            }
            String[] emailArray = binding.loginEmail.getText().toString().split("@");


            if (binding.loginPassword.getText().toString().trim().length() < 8) {
                Toasty.error(requireContext(), getString(R.string.password_too_short)).show();
                return;
            }
            if (binding.loginUsername.getText().toString().matches("[a-z0-9_]")) {
                Toasty.error(requireContext(), getString(R.string.username_error)).show();
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
            accountCreation.setEmail(binding.loginEmail.getText().toString().trim());
            accountCreation.setPassword(binding.loginPassword.getText().toString().trim());
            accountCreation.setPasswordConfirm(binding.loginPasswordConfirm.getText().toString().trim());
            accountCreation.setUsername(binding.loginUsername.getText().toString().trim());
            accountCreation.setInstance(instance);

            new Thread(() -> {
                try {
                    APIResponse apiResponse = new RetrofitPeertubeAPI(requireContext(), instance, null).createAccount(accountCreation);
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

                        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireContext(), app.fedilab.android.mastodon.helper.Helper.dialogStyle());
                        dialogBuilder.setCancelable(false);
                        dialogBuilder.setPositiveButton(R.string.validate, (dialog, which) -> {
                            dialog.dismiss();
                            getParentFragmentManager().popBackStack();
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

        if (getArguments() != null) {
            String instance = getArguments().getString(INSTANCE_ADDRESS, null);
            binding.loginInstance.setText(instance);
            setTextAgreement();
        }

        return root;
    }

    private void setTextAgreement() {
        String tos = getString(R.string.tos);
        String serverrules = getString(R.string.server_rules);
        String content_agreement = null;
        binding.agreement.setMovementMethod(null);
        binding.agreement.setText(null);
        if (binding.loginInstance.getText() != null) {
            content_agreement = getString(R.string.agreement_check_peertube,
                    "<a href='https://" + binding.loginInstance.getText().toString() + "/about/instance#terms-section' >" + tos + "</a>"
            );
        }
        binding.agreement.setMovementMethod(LinkMovementMethod.getInstance());
        if (content_agreement != null) {
            binding.agreement.setText(Html.fromHtml(content_agreement));
        }
    }

}