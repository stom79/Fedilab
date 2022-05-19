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

import static app.fedilab.android.BaseMainActivity.admin;
import static app.fedilab.android.BaseMainActivity.api;
import static app.fedilab.android.BaseMainActivity.client_id;
import static app.fedilab.android.BaseMainActivity.client_secret;
import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.software;
import static app.fedilab.android.helper.Helper.PREF_USER_TOKEN;
import static app.fedilab.android.helper.MastodonHelper.REDIRECT_CONTENT_WEB;

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
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.entities.WellKnownNodeinfo;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.login.FragmentLoginMain;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.OauthVM;
import es.dmoral.toasty.Toasty;


public class LoginActivity extends BaseActivity {


    private final int PICK_IMPORT = 5557;
    private String oldSearch;
    private String autofilledInstance;
    private WellKnownNodeinfo.NodeInfo nodeInfo;
    private NavHostFragment host;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        setContentView(new FrameLayout(this));

        Helper.addFragment(getSupportFragmentManager(), android.R.id.content, new FragmentLoginMain(), null, null, null);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            autofilledInstance = b.getString("instance", null);
            admin = b.getBoolean("admin", false);
        }


        //The activity handles a redirect URI, it will extract token code and will proceed to authentication
        //That happens when the user wants to use an external browser
        if (getIntent() != null && getIntent().getData() != null && getIntent().getData().toString().contains(REDIRECT_CONTENT_WEB + "?code=")) {
            String url = getIntent().getData().toString();
            Matcher matcher = Helper.codePattern.matcher(url);
            if (!matcher.find()) {
                Toasty.error(LoginActivity.this, getString(R.string.toast_code_error), Toast.LENGTH_LONG).show();
                return;
            }
            String code = matcher.group(1);
            OauthVM oauthVM = new ViewModelProvider(LoginActivity.this).get(OauthVM.class);
            //We are dealing with a Mastodon API
            if (api == Account.API.MASTODON) {
                //API call to get the user token
                oauthVM.createToken(currentInstance, "authorization_code", client_id, client_secret, Helper.REDIRECT_CONTENT_WEB, Helper.OAUTH_SCOPES, code)
                        .observe(LoginActivity.this, tokenObj -> {
                            Account account = new Account();
                            account.client_id = BaseMainActivity.client_id;
                            account.client_secret = BaseMainActivity.client_secret;
                            account.token = tokenObj.token_type + " " + tokenObj.access_token;
                            account.api = api;
                            account.software = software;
                            account.instance = currentInstance;
                            //API call to retrieve account information for the new token
                            AccountsVM accountsVM = new ViewModelProvider(LoginActivity.this).get(AccountsVM.class);
                            accountsVM.getConnectedAccount(currentInstance, account.token).observe(LoginActivity.this, mastodonAccount -> {
                                account.mastodon_account = mastodonAccount;
                                new Thread(() -> {
                                    try {
                                        account.user_id = mastodonAccount.id;
                                        //update the database
                                        new Account(LoginActivity.this).insertOrUpdate(account);

                                        BaseMainActivity.currentToken = account.token;
                                        BaseMainActivity.currentUserID = account.user_id;
                                        api = Account.API.MASTODON;
                                        SharedPreferences.Editor editor = sharedpreferences.edit();
                                        editor.putString(PREF_USER_TOKEN, account.token);
                                        editor.commit();
                                        //The user is now aut
                                        //The user is now authenticated, it will be redirected to MainActivity
                                        Handler mainHandler = new Handler(Looper.getMainLooper());
                                        Runnable myRunnable = () -> {
                                            Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(mainActivity);
                                            finish();
                                        };
                                        mainHandler.post(myRunnable);
                                    } catch (DBException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            });
                        });
            }
        }
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