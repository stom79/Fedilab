package app.fedilab.android.ui.fragment.login;
/* Copyright 2021 Thomas Schneider
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


import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentLoginRegisterMastodonBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.AppsVM;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import app.fedilab.android.viewmodel.mastodon.OauthVM;

public class FragmentLoginRegisterMastodon extends Fragment {


    private FragmentLoginRegisterMastodonBinding binding;
    private NodeInfoVM nodeInfoVM;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Bundle args = getArguments();
        String instance = null;
        if (args != null) {
            instance = args.getString("instance", null);
        }

        binding = FragmentLoginRegisterMastodonBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        nodeInfoVM = new ViewModelProvider(requireActivity()).get(NodeInfoVM.class);
        if (instance != null) {
            binding.loginInstance.setText(instance.trim());
            binding.loginInstance.setEnabled(false);
            String tos = getString(R.string.tos);
            String serverrules = getString(R.string.server_rules);
            String content_agreement = getString(R.string.agreement_check,
                    "<a href='https://" + instance + "/about/more' >" + serverrules + "</a>",
                    "<a href='https://" + instance + "/terms' >" + tos + "</a>"
            );
            binding.agreementText.setMovementMethod(LinkMovementMethod.getInstance());
            binding.agreementText.setText(Html.fromHtml(content_agreement));
        } else {
            binding.loginInstance.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    nodeInfoVM.getNodeInfo(binding.loginInstance.getText().toString().trim()).observe(requireActivity(), nodeInfo -> {
                        if (nodeInfo != null) {
                            String tos = getString(R.string.tos);
                            String serverrules = getString(R.string.server_rules);
                            String content_agreement = getString(R.string.agreement_check,
                                    "<a href='https://" + binding.loginInstance.getText() + "/about/more' >" + serverrules + "</a>",
                                    "<a href='https://" + binding.loginInstance.getText() + "/terms' >" + tos + "</a>"
                            );
                            binding.agreementText.setMovementMethod(LinkMovementMethod.getInstance());
                            binding.agreementText.setText(Html.fromHtml(content_agreement));
                        } else {
                            binding.loginInstanceLayout.setError(getString(R.string.instance_not_valid));
                        }
                    });
                }
            });
        }

        binding.signup.setOnClickListener(v -> {
            boolean error = false;
            binding.loginUsernameLayout.setError(null);
            binding.loginEmailLayout.setError(null);
            binding.loginInstanceLayout.setError(null);
            binding.loginPasswordLayout.setError(null);
            binding.loginPasswordConfirmLayout.setError(null);

            if (binding.loginUsername.getText().toString().trim().length() == 0) {
                binding.loginUsernameLayout.setError(getString(R.string.cannot_be_empty));
                error = true;
            }
            if (binding.loginEmail.getText().toString().trim().length() == 0) {
                binding.loginEmailLayout.setError(getString(R.string.cannot_be_empty));
                error = true;
            }
            if (binding.loginInstance.getText().toString().trim().length() == 0) {
                binding.loginInstanceLayout.setError(getString(R.string.cannot_be_empty));
                error = true;
            } else {

                nodeInfoVM.getNodeInfo(binding.loginInstance.getText().toString()).observe(requireActivity(), nodeInfo -> {
                    if (nodeInfo == null || (nodeInfo.software.name.trim().toLowerCase().compareTo("mastodon") != 0 && nodeInfo.software.name.trim().toLowerCase().compareTo("pleroma") != 0)) {
                        binding.loginInstanceLayout.setError(getString(R.string.instance_not_valid));
                    }
                });
            }
            if (binding.loginPassword.getText().toString().trim().length() == 0) {
                binding.loginPasswordLayout.setError(getString(R.string.cannot_be_empty));
                error = true;
            }

            if (!binding.loginPassword.getText().toString().trim().equals(binding.loginPasswordConfirm.getText().toString().trim())) {
                binding.loginPasswordConfirmLayout.setError(getString(R.string.password_error));
                error = true;
            }
            if (binding.loginPassword.getText().toString().trim().length() < 8) {
                binding.loginPasswordLayout.setError(getString(R.string.password_too_short));
                error = true;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.loginEmail.getText().toString().trim()).matches()) {
                binding.loginEmailLayout.setError(getString(R.string.email_error));
                error = true;
            }
            if (binding.loginUsername.getText() == null || binding.loginUsername.getText().toString().trim().length() == 0) {
                binding.loginUsernameLayout.setError(getString(R.string.cannot_be_empty));
                error = true;
            }
            if (binding.loginUsername.getText().toString().matches("[a-zA-Z0-9_]")) {
                binding.loginUsernameLayout.setError(getString(R.string.username_error));
                error = true;
            }

            if (error) {
                return;
            }
            String registerInstance = binding.loginInstance.getText().toString().trim();
            AppsVM appsVM = new ViewModelProvider(requireActivity()).get(AppsVM.class);
            appsVM.createApp(registerInstance, getString(R.string.app_name),
                    Helper.REDIRECT_CONTENT_WEB,
                    Helper.OAUTH_SCOPES,
                    Helper.WEBSITE_VALUE
            ).observe(requireActivity(), app -> {
                OauthVM oauthVM = new ViewModelProvider(requireActivity()).get(OauthVM.class);
                oauthVM.createToken(registerInstance, "client_credentials", app.client_id, app.client_secret, null, Helper.APP_OAUTH_SCOPES, null)
                        .observe(requireActivity(), tokenObj -> {
                            AccountsVM accountsVM = new ViewModelProvider(requireActivity()).get(AccountsVM.class);
                            accountsVM.registerAccount(
                                    registerInstance,
                                    tokenObj.token_type + " " + tokenObj.access_token,
                                    binding.loginUsername.getText().toString().trim(),
                                    binding.loginEmail.getText().toString().trim(),
                                    binding.loginPassword.getText().toString().trim(),
                                    binding.agreement.isChecked(),
                                    Locale.getDefault().getLanguage(), null
                            ).observe(requireActivity(), token -> {
                                if (token != null) {
                                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity(), Helper.dialogStyle());
                                    dialogBuilder.setCancelable(false);
                                    dialogBuilder.setPositiveButton(R.string.validate, (dialog, which) -> {
                                        dialog.dismiss();
                                        requireActivity().onBackPressed();
                                        requireActivity().onBackPressed();
                                    });
                                    AlertDialog alertDialog = dialogBuilder.create();
                                    alertDialog.setTitle(getString(R.string.account_created));
                                    alertDialog.setMessage(getString(R.string.account_created_message, registerInstance));
                                    alertDialog.show();
                                    //Revoke the current token as we will not use it immediately.
                                    oauthVM.revokeToken(registerInstance, tokenObj.token_type + " " + tokenObj.access_token, app.client_id, app.client_secret);
                                }

                            });
                        });


            });

        });
        return root;
    }
}