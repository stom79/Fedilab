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

import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.updateCredential;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.net.MalformedURLException;
import java.net.URL;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityLoginPeertubeBinding;
import app.fedilab.android.mastodon.activities.BaseBarActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.helper.Helper;
import es.dmoral.toasty.Toasty;


public class LoginActivity extends BaseBarActivity {


    private static String client_id;
    private static String client_secret;
    private ActivityLoginPeertubeBinding binding;
    private String instance;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginPeertubeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            instance = b.getString(app.fedilab.android.mastodon.helper.Helper.ARG_INSTANCE, null);
        }
        if (instance == null) {
            finish();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.login);
        }

        binding.loginInstanceContainer.setVisibility(View.VISIBLE);

        binding.loginInstance.setText(instance);
        if (Helper.isTablet(LoginActivity.this)) {

            ViewGroup.LayoutParams layoutParamsI = binding.loginInstanceContainer.getLayoutParams();
            layoutParamsI.width = (int) Helper.convertDpToPixel(300, LoginActivity.this);
            binding.loginInstanceContainer.setLayoutParams(layoutParamsI);

            ViewGroup.LayoutParams layoutParamsU = binding.loginUidContainer.getLayoutParams();
            layoutParamsU.width = (int) Helper.convertDpToPixel(300, LoginActivity.this);
            binding.loginUidContainer.setLayoutParams(layoutParamsU);

            ViewGroup.LayoutParams layoutParamsP = binding.loginPasswdContainer.getLayoutParams();
            layoutParamsP.width = (int) Helper.convertDpToPixel(300, LoginActivity.this);
            binding.loginPasswdContainer.setLayoutParams(layoutParamsP);
        }


        binding.loginButton.setOnClickListener(v -> {
            if (binding.loginUid.getText() != null && binding.loginUid.getText().toString().contains("@") && !Patterns.EMAIL_ADDRESS.matcher(binding.loginUid.getText().toString().trim()).matches()) {
                Toasty.error(LoginActivity.this, getString(R.string.email_error)).show();
                return;
            }
            binding.loginButton.setEnabled(false);
            String instance;
            if (binding.loginInstance.getText() == null || binding.loginInstance.getText().toString().trim().length() == 0) {
                Toasty.error(LoginActivity.this, getString(R.string.not_valide_instance)).show();
                binding.loginButton.setEnabled(true);
                return;
            }
            instance = binding.loginInstance.getText().toString().trim().toLowerCase();

            if (instance.startsWith("http")) {
                try {
                    URL url = new URL(instance);
                    instance = url.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else if (instance.endsWith("/")) {
                try {
                    URL url = new URL("https://" + instance);
                    instance = url.getHost();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            if (!Patterns.WEB_URL.matcher("https://" + instance).matches()) {
                Toasty.error(LoginActivity.this, getString(R.string.not_valide_instance)).show();
                binding.loginButton.setEnabled(true);
                return;
            }
            String finalInstance = instance;
            new Thread(() -> connectToFediverse(finalInstance)).start();
        });
    }

    /**
     * Oauth process for Peertube
     *
     * @param finalInstance String
     */
    private void connectToFediverse(String finalInstance) {
        Oauth oauth = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).oauthClient(Helper.CLIENT_NAME_VALUE, Helper.WEBSITE_VALUE, Helper.OAUTH_SCOPES_PEERTUBE, Helper.WEBSITE_VALUE);
        if (oauth == null) {
            runOnUiThread(() -> {
                binding.loginButton.setEnabled(true);
                Toasty.error(LoginActivity.this, getString(R.string.client_error), Toast.LENGTH_LONG).show();
            });
            return;
        }
        client_id = oauth.getClient_id();
        client_secret = oauth.getClient_secret();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Helper.CLIENT_ID, client_id);
        editor.putString(Helper.CLIENT_SECRET, client_secret);
        editor.apply();
        OauthParams oauthParams = new OauthParams();
        oauthParams.setClient_id(client_id);
        oauthParams.setClient_secret(client_secret);
        oauthParams.setGrant_type("password");
        oauthParams.setScope("user");
        if (binding.loginUid.getText() != null) {
            oauthParams.setUsername(binding.loginUid.getText().toString().trim());
        }
        if (binding.loginPasswd.getText() != null) {
            oauthParams.setPassword(binding.loginPasswd.getText().toString());
        }
        try {
            Token token = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).manageToken(oauthParams);
            proceedLogin(token, finalInstance);
        } catch (final Exception e) {
            oauthParams.setUsername(binding.loginUid.getText().toString().toLowerCase().trim());
            Token token = null;
            try {
                token = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).manageToken(oauthParams);
            } catch (Error ex) {
                ex.printStackTrace();
            }
            proceedLogin(token, finalInstance);
        } catch (Error e) {
            runOnUiThread(() -> {
                Toasty.error(LoginActivity.this, e.getError() != null && !e.getError().isEmpty() ? e.getError() : getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                binding.loginButton.setEnabled(true);
            });

            e.printStackTrace();
        }
    }


    @SuppressLint("ApplySharedPref")
    private void proceedLogin(Token token, String host) {
        runOnUiThread(() -> {
            if (token != null) {
                //Update the account with the token;
                updateCredential(LoginActivity.this, token.getAccess_token(), client_id, client_secret, token.getRefresh_token(), host, null);
            } else {
                binding.loginButton.setEnabled(true);
            }
        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}