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


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityProxyBinding;


public class ProxyActivity extends DialogFragment {

    private ActivityProxyBinding binding;
    private int position;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityProxyBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setView(binding.getRoot());

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        //Enable proxy
        boolean enable_proxy = sharedpreferences.getBoolean(getString(R.string.SET_PROXY_ENABLED), false);
        binding.enableProxy.setChecked(enable_proxy);
        position = sharedpreferences.getInt(getString(R.string.SET_PROXY_TYPE), 0);
        String hostVal = sharedpreferences.getString(getString(R.string.SET_PROXY_HOST), "127.0.0.1");
        int portVal = sharedpreferences.getInt(getString(R.string.SET_PROXY_PORT), 8118);
        final String login = sharedpreferences.getString(getString(R.string.SET_PROXY_LOGIN), null);
        final String pwd = sharedpreferences.getString(getString(R.string.SET_PROXY_PASSWORD), null);
        if (hostVal.length() > 0) {
            binding.host.setText(hostVal);
        }
        binding.port.setText(String.valueOf(portVal));
        if (login != null && login.length() > 0) {
            binding.proxyLogin.setText(login);
        }
        if (pwd != null && binding.proxyPassword.length() > 0) {
            binding.proxyPassword.setText(pwd);
        }
        if (position == 1) binding.protocol.check(R.id.protocol_socks);
        binding.protocol.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.protocol_http)
                    position = 0;
                else
                    position = 1;
            }
        });

        materialAlertDialogBuilder.setPositiveButton(R.string.save, (dialog1, which) -> {
            String hostVal1 = binding.host.getText().toString().trim();
            String portVal1 = binding.port.getText().toString().trim();
            String proxy_loginVal = binding.proxyLogin.getText().toString().trim();
            String proxy_passwordVal = binding.proxyPassword.getText().toString().trim();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(getString(R.string.SET_PROXY_ENABLED), binding.enableProxy.isChecked());
            editor.putInt(getString(R.string.SET_PROXY_TYPE), position);
            editor.putString(getString(R.string.SET_PROXY_HOST), hostVal1);
            if (portVal1.matches("\\d+"))
                editor.putInt(getString(R.string.SET_PROXY_PORT), Integer.parseInt(portVal1));
            editor.putString(getString(R.string.SET_PROXY_LOGIN), proxy_loginVal);
            editor.putString(getString(R.string.SET_PROXY_PASSWORD), proxy_passwordVal);
            editor.apply();
        });
        materialAlertDialogBuilder.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        Dialog dialog = materialAlertDialogBuilder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
