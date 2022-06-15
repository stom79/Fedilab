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


import static app.fedilab.android.helper.MastodonHelper.REDIRECT_CONTENT_WEB;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.app.Account;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.login.FragmentLoginMain;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.AdminVM;
import app.fedilab.android.viewmodel.mastodon.OauthVM;
import es.dmoral.toasty.Toasty;


public class LoginActivity extends BaseActivity {


    private final int PICK_IMPORT = 5557;
    private boolean requestedAdmin;
    public static Account.API apiLogin;
    public static String currentInstanceLogin, client_idLogin, client_secretLogin, softwareLogin;

    private void manageItent(Intent intent) {
        if (intent != null && intent.getData() != null && intent.getData().toString().contains(REDIRECT_CONTENT_WEB + "?code=")) {
            String url = intent.getData().toString();
            Matcher matcher = Helper.codePattern.matcher(url);
            if (!matcher.find()) {
                Toasty.error(LoginActivity.this, getString(R.string.toast_code_error), Toast.LENGTH_LONG).show();
                return;
            }
            String code = matcher.group(1);
            OauthVM oauthVM = new ViewModelProvider(LoginActivity.this).get(OauthVM.class);

            //We are dealing with a Mastodon API
            if (apiLogin == Account.API.MASTODON) {
                //API call to get the user token
                String scope = requestedAdmin ? Helper.OAUTH_SCOPES_ADMIN : Helper.OAUTH_SCOPES;
                oauthVM.createToken(currentInstanceLogin, "authorization_code", client_idLogin, client_secretLogin, Helper.REDIRECT_CONTENT_WEB, scope, code)
                        .observe(LoginActivity.this, tokenObj -> {
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
                                account.mastodon_account = mastodonAccount;
                                account.user_id = mastodonAccount.id;
                                //We check if user have really moderator rights
                                if (requestedAdmin) {
                                    AdminVM adminVM = new ViewModelProvider(LoginActivity.this).get(AdminVM.class);
                                    adminVM.getAccount(account.instance, account.token, account.user_id).observe(LoginActivity.this, adminAccount -> {
                                        account.admin = adminAccount != null;
                                        WebviewConnectActivity.proceedLogin(LoginActivity.this, account);
                                    });
                                } else {
                                    WebviewConnectActivity.proceedLogin(LoginActivity.this, account);
                                }
                            });
                        });
            }
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
        ThemeHelper.applyTheme(this);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        setContentView(new FrameLayout(this));
        FragmentLoginMain fragmentLoginMain = new FragmentLoginMain();
        Helper.addFragment(getSupportFragmentManager(), android.R.id.content, fragmentLoginMain, null, null, null);
        requestedAdmin = false;
        //The activity handles a redirect URI, it will extract token code and will proceed to authentication
        //That happens when the user wants to use an external browser
        manageItent(getIntent());
    }

    public boolean requestedAdmin() {
        return requestedAdmin;
    }

    public boolean setAdmin(boolean askAdmin) {
        return requestedAdmin = askAdmin;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_login, menu);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        boolean embedded_browser = sharedpreferences.getBoolean(getString(R.string.SET_EMBEDDED_BROWSER), true);
        menu.findItem(R.id.action_custom_tabs).setChecked(!embedded_browser);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_proxy) {
            Intent intent = new Intent(LoginActivity.this, ProxyActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_custom_tabs) {
            item.setChecked(!item.isChecked());
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(getString(R.string.SET_EMBEDDED_BROWSER), !item.isChecked());
            editor.apply();
            return false;
        } else if (id == R.id.action_request_admin) {
            item.setChecked(!item.isChecked());
            requestedAdmin = item.isChecked();
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMPORT && resultCode == RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(LoginActivity.this, getString(R.string.toot_select_file_error), Toast.LENGTH_LONG).show();
                return;
            }
            //  String filename = Helper.getFilePathFromURI(LoginActivity.this, data.getData());
            //   Sqlite.importDB(LoginActivity.this, filename);
        } else {
            Toasty.error(LoginActivity.this, getString(R.string.toot_select_file_error), Toast.LENGTH_LONG).show();
        }
    }

}