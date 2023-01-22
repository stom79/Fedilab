package app.fedilab.android.activities;
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


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.BaseActivity;
import app.fedilab.android.mastodon.activities.ProxyActivity;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.AdminVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.OauthVM;
import app.fedilab.android.ui.fragment.FragmentLoginMain;
import es.dmoral.toasty.Toasty;


public class LoginActivity extends BaseActivity {


    public static Account.API apiLogin;
    public static String currentInstanceLogin, client_idLogin, client_secretLogin, softwareLogin;
    public static boolean requestedAdmin;


    @SuppressLint("ApplySharedPref")
    public void proceedLogin(Activity activity, Account account) {
        new Thread(() -> {
            try {
                //update the database
                new Account(activity).insertOrUpdate(account);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                BaseMainActivity.currentToken = account.token;
                BaseMainActivity.currentUserID = account.user_id;
                BaseMainActivity.api = Account.API.MASTODON;
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Helper.PREF_USER_TOKEN, account.token);
                editor.commit();
                //The user is now authenticated, it will be redirected to MainActivity
                Runnable myRunnable = () -> {
                    Intent mainActivity = new Intent(activity, MainActivity.class);
                    startActivity(mainActivity);
                    finish();
                };
                mainHandler.post(myRunnable);
            } catch (DBException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void manageItent(Intent intent) {

        if (intent != null && intent.getData() != null && intent.getData().toString().contains(MastodonHelper.REDIRECT_CONTENT_WEB + "?code=")) {
            String url = intent.getData().toString();
            Matcher matcher = Helper.codePattern.matcher(url);
            if (!matcher.find()) {
                Toasty.error(LoginActivity.this, getString(R.string.toast_code_error), Toast.LENGTH_LONG).show();
                return;
            }
            String code = matcher.group(1);
            OauthVM oauthVM = new ViewModelProvider(LoginActivity.this).get(OauthVM.class);
            //We are dealing with a Mastodon API
            //API call to get the user token
            String scope = requestedAdmin ? Helper.OAUTH_SCOPES_ADMIN : Helper.OAUTH_SCOPES;
            oauthVM.createToken(currentInstanceLogin, "authorization_code", client_idLogin, client_secretLogin, Helper.REDIRECT_CONTENT_WEB, scope, code)
                    .observe(LoginActivity.this, tokenObj -> {
                        if (tokenObj != null) {
                            Account account = new Account();
                            account.client_id = client_idLogin;
                            account.client_secret = client_secretLogin;
                            account.token = tokenObj.token_type + " " + tokenObj.access_token;
                            account.api = apiLogin;
                            account.software = softwareLogin;
                            account.instance = currentInstanceLogin;
                            //API call to retrieve account information for the new token
                            AccountsVM accountsVM = new ViewModelProvider(LoginActivity.this).get(AccountsVM.class);
                            accountsVM.getConnectedAccount(currentInstanceLogin, account.token).observe(LoginActivity.this, mastodonAccount -> {
                                if (mastodonAccount != null) {
                                    account.mastodon_account = mastodonAccount;
                                    account.user_id = mastodonAccount.id;
                                    //We check if user have really moderator rights
                                    if (requestedAdmin) {
                                        AdminVM adminVM = new ViewModelProvider(LoginActivity.this).get(AdminVM.class);
                                        adminVM.getAccount(account.instance, account.token, account.user_id).observe(LoginActivity.this, adminAccount -> {
                                            account.admin = adminAccount != null;
                                            proceedLogin(LoginActivity.this, account);
                                        });
                                    } else {
                                        proceedLogin(LoginActivity.this, account);
                                    }
                                } else {
                                    Toasty.error(LoginActivity.this, getString(R.string.toast_token), Toast.LENGTH_LONG).show();
                                }

                            });
                        } else {
                            Toasty.error(LoginActivity.this, getString(R.string.toast_token), Toast.LENGTH_LONG).show();
                        }

                    });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        manageItent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        setContentView(new FrameLayout(this));
        FragmentLoginMain fragmentLoginMain = new FragmentLoginMain();
        Helper.addFragment(getSupportFragmentManager(), android.R.id.content, fragmentLoginMain, null, null, null);
        //The activity handles a redirect URI, it will extract token code and will proceed to authentication
        //That happens when the user wants to use an external browser
        manageItent(getIntent());


    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_proxy) {
            (new ProxyActivity()).show(getSupportFragmentManager(), null);
        } else if (id == R.id.action_request_admin) {
            item.setChecked(!item.isChecked());
            requestedAdmin = item.isChecked();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }


}