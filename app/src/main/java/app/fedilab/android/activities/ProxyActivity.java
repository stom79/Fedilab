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


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.preference.PreferenceManager;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityProxyBinding;


public class ProxyActivity extends BaseAlertDialogActivity {

    private ActivityProxyBinding binding;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(ProxyActivity.this);
        binding = ActivityProxyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        //Enable proxy
        boolean enable_proxy = sharedpreferences.getBoolean(getString(R.string.SET_PROXY_ENABLED), false);
        binding.enableProxy.setChecked(enable_proxy);
        position = 0;
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
        ArrayAdapter<CharSequence> adapterTrans = ArrayAdapter.createFromResource(ProxyActivity.this,
                R.array.proxy_type_choice, android.R.layout.simple_spinner_dropdown_item);
        binding.type.setAdapter(adapterTrans);
        binding.type.setSelection(sharedpreferences.getInt(getString(R.string.SET_PROXY_TYPE), 0), false);
        binding.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int p, long id) {
                position = p;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.setProxySave.setOnClickListener(view -> {
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
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
