package app.fedilab.android.peertube.activities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import app.fedilab.android.peertube.BuildConfig;
import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.data.PluginData;
import app.fedilab.android.peertube.client.entities.AcadInstances;
import app.fedilab.android.peertube.client.entities.Oauth;
import app.fedilab.android.peertube.client.entities.OauthParams;
import app.fedilab.android.peertube.client.entities.Token;
import app.fedilab.android.peertube.client.entities.WellKnownNodeinfo;
import app.fedilab.android.peertube.client.mastodon.RetrofitMastodonAPI;
import app.fedilab.android.peertube.databinding.ActivityLoginBinding;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperAcadInstance;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.Theme;
import es.dmoral.toasty.Toasty;


public class LoginActivity extends BaseActivity {


    private static String client_id;
    private static String client_secret;
    private ActivityLoginBinding binding;
    private String acadInstance;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.setTheme(this, HelperInstance.getLiveInstance(this), false);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        SpannableString content_create;
        content_create = new SpannableString(getString(R.string.join_peertube));

        content_create.setSpan(new UnderlineSpan(), 0, content_create.length(), 0);
        content_create.setSpan(new ForegroundColorSpan(Helper.fetchAccentColor(LoginActivity.this)), 0, content_create.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.createAnAccountPeertube.setText(content_create, TextView.BufferType.SPANNABLE);

        binding.createAnAccountPeertube.setOnClickListener(v -> {
            Intent mainActivity = new Intent(LoginActivity.this, PeertubeRegisterActivity.class);
            Bundle b = new Bundle();
            mainActivity.putExtras(b);
            startActivity(mainActivity);
        });


        if (BuildConfig.full_instances && BuildConfig.instance_switcher) {
            binding.loginInstanceContainer.setVisibility(View.VISIBLE);
        }


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

        if (!BuildConfig.full_instances) {

            binding.loginUidContainer.setVisibility(View.GONE);
            binding.loginPasswdContainer.setVisibility(View.GONE);
            binding.loginInstanceContainer.setVisibility(View.GONE);
            binding.createAnAccountPeertube.setVisibility(View.GONE);
            binding.instancePickerTitle.setVisibility(View.VISIBLE);
            binding.instancePicker.setVisibility(View.VISIBLE);


            List<AcadInstances> acadInstances = AcadInstances.getInstances();
            String[] academiesKey = new String[acadInstances.size()];
            String[] academiesValue = new String[acadInstances.size()];
            String acad = HelperInstance.getLiveInstance(LoginActivity.this);
            int position = 0;
            int i = 0;
            for (AcadInstances ac : acadInstances) {
                academiesKey[i] = ac.getName();
                academiesValue[i] = ac.getUrl();
                if (ac.getUrl().compareTo(acad) == 0) {
                    position = i;
                }
                i++;
            }
            ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(LoginActivity.this,
                    android.R.layout.simple_spinner_dropdown_item, academiesKey);
            binding.instancePicker.setAdapter(adapterChannel);
            binding.instancePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    acadInstance = academiesValue[position];
                    binding.loginUidContainer.setVisibility(View.GONE);
                    binding.loginPasswdContainer.setVisibility(View.GONE);
                    binding.loginInstanceContainer.setVisibility(View.GONE);
                    binding.createAnAccountPeertube.setVisibility(View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            binding.instancePicker.setSelection(position, true);
        }
        if (BuildConfig.allow_remote_connections) {
            binding.loginInstance.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    if (binding.loginInstance.getText() != null) {
                        new Thread(() -> {
                            String testInstance = binding.loginInstance.getText().toString().trim();
                            if (testInstance.length() == 0) {
                                return;
                            }
                            WellKnownNodeinfo.NodeInfo instanceNodeInfo = null;
                            if (BuildConfig.allow_remote_connections) {
                                instanceNodeInfo = new RetrofitPeertubeAPI(LoginActivity.this, testInstance, null).getNodeInfo();
                            }
                            if (instanceNodeInfo != null &&
                                    (instanceNodeInfo.getSoftware().getName().toUpperCase().trim().compareTo("MASTODON") == 0 ||
                                            instanceNodeInfo.getSoftware().getName().toUpperCase().trim().compareTo("PLEROMA") == 0)
                            ) {
                                connectToFediverse(testInstance, instanceNodeInfo);
                            }
                        }).start();
                    }
                }
            });
        }

        binding.loginButton.setOnClickListener(v -> {
            if (!BuildConfig.full_instances && AcadInstances.isOpenId(acadInstance)) {
                new Thread(() -> {
                    try {
                        InstanceData.InstanceConfig instanceConfig = new RetrofitPeertubeAPI(LoginActivity.this).getConfigInstance();
                        PluginData.Plugin plugin = instanceConfig.getPlugin();
                        List<PluginData.PluginInfo> pluginInfos = plugin.getRegistered();
                        String openIdVersion = "0.0.7";
                        for (PluginData.PluginInfo pluginInfo : pluginInfos) {
                            if (pluginInfo.getName().toLowerCase().contains("openid")) {
                                openIdVersion = pluginInfo.getVersion();
                            }
                        }
                        Oauth oauth = new RetrofitPeertubeAPI(LoginActivity.this, acadInstance, null).oauthClient(null, null, null, null);
                        if (oauth == null) {
                            runOnUiThread(() -> {
                                binding.loginButton.setEnabled(true);
                                Toasty.error(LoginActivity.this, getString(R.string.client_error), Toast.LENGTH_LONG).show();
                            });
                            return;
                        }
                        client_id = oauth.getClient_id();
                        client_secret = oauth.getClient_secret();
                        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(Helper.CLIENT_ID, client_id);
                        editor.putString(Helper.CLIENT_SECRET, client_secret);
                        editor.apply();
                        Intent intent = new Intent(LoginActivity.this, WebviewConnectActivity.class);
                        Bundle b = new Bundle();
                        b.putString("url", "https://" + acadInstance + "/plugins/auth-openid-connect/" + openIdVersion + "/auth/openid-connect");
                        intent.putExtras(b);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            binding.loginButton.setEnabled(true);
                            Toasty.error(LoginActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                        });
                    }

                }).start();
            } else {
                if (binding.loginUid.getText() != null && binding.loginUid.getText().toString().contains("@") && !Patterns.EMAIL_ADDRESS.matcher(binding.loginUid.getText().toString().trim()).matches()) {
                    Toasty.error(LoginActivity.this, getString(R.string.email_error)).show();
                    return;
                }
                binding.loginButton.setEnabled(false);
                String instance;
                if (!BuildConfig.full_instances) {
                    String[] emailArray = binding.loginUid.getText().toString().split("@");
                    if (emailArray.length > 1 && !Arrays.asList(HelperAcadInstance.valideEmails).contains(emailArray[1])) {
                        Toasty.error(LoginActivity.this, getString(R.string.email_error_domain, emailArray[1])).show();
                        binding.loginButton.setEnabled(true);
                        return;
                    }

                    instance = HelperInstance.getLiveInstance(LoginActivity.this);
                } else {
                    if (binding.loginInstance.getText() == null || binding.loginInstance.getText().toString().trim().length() == 0) {
                        Toasty.error(LoginActivity.this, getString(R.string.not_valide_instance)).show();
                        binding.loginButton.setEnabled(true);
                        return;
                    }
                    instance = binding.loginInstance.getText().toString().trim().toLowerCase();
                }

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
                new Thread(() -> {
                    WellKnownNodeinfo.NodeInfo instanceNodeInfo = null;
                    if (BuildConfig.allow_remote_connections) {
                        instanceNodeInfo = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).getNodeInfo();
                    }
                    connectToFediverse(finalInstance, instanceNodeInfo);
                }).start();
            }
        });
    }

    /**
     * Oauth process for Peertube
     *
     * @param finalInstance String
     */
    private void connectToFediverse(String finalInstance, WellKnownNodeinfo.NodeInfo instanceNodeInfo) {
        Oauth oauth = null;
        String software;
        if (instanceNodeInfo != null) {
            software = instanceNodeInfo.getSoftware().getName().toUpperCase().trim();
            switch (software) {
                case "MASTODON":
                case "PLEROMA":
                    oauth = new RetrofitMastodonAPI(LoginActivity.this, finalInstance, null).oauthClient(Helper.CLIENT_NAME_VALUE, Helper.REDIRECT_CONTENT_WEB, Helper.OAUTH_SCOPES_MASTODON, Helper.WEBSITE_VALUE);
                    break;

                case "FRIENDICA":

                    break;

                default:
                    oauth = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).oauthClient(Helper.CLIENT_NAME_VALUE, Helper.WEBSITE_VALUE, Helper.OAUTH_SCOPES_PEERTUBE, Helper.WEBSITE_VALUE);
            }
        } else {
            oauth = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).oauthClient(Helper.CLIENT_NAME_VALUE, Helper.WEBSITE_VALUE, Helper.OAUTH_SCOPES_PEERTUBE, Helper.WEBSITE_VALUE);
            software = "PEERTUBE";
        }
        if (oauth == null) {
            runOnUiThread(() -> {
                binding.loginButton.setEnabled(true);
                Toasty.error(LoginActivity.this, getString(R.string.client_error), Toast.LENGTH_LONG).show();
            });
            return;
        }
        client_id = oauth.getClient_id();
        client_secret = oauth.getClient_secret();

        SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Helper.CLIENT_ID, client_id);
        editor.putString(Helper.CLIENT_SECRET, client_secret);
        editor.apply();
        OauthParams oauthParams = new OauthParams();
        oauthParams.setClient_id(client_id);
        oauthParams.setClient_secret(client_secret);
        oauthParams.setGrant_type("password");
        final boolean isMastodonAPI = software.compareTo("MASTODON") == 0 || software.compareTo("PLEROMA") == 0;
        if (software.compareTo("PEERTUBE") == 0) {
            oauthParams.setScope("user");
        } else if (isMastodonAPI) {
            oauthParams.setScope("read write follow");
        }
        if (binding.loginUid.getText() != null) {
            oauthParams.setUsername(binding.loginUid.getText().toString().trim());
        }
        if (binding.loginPasswd.getText() != null) {
            oauthParams.setPassword(binding.loginPasswd.getText().toString());
        }
        try {
            Token token = null;
            if (software.compareTo("PEERTUBE") == 0) {
                token = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).manageToken(oauthParams);
            } else if (isMastodonAPI) {
                Intent i = new Intent(LoginActivity.this, MastodonWebviewConnectActivity.class);
                i.putExtra("software", software);
                i.putExtra("instance", finalInstance);
                i.putExtra("client_id", client_id);
                i.putExtra("client_secret", client_secret);
                startActivity(i);
                return;
            }
            proceedLogin(token, finalInstance, software.compareTo("PEERTUBE") == 0 ? null : software);
        } catch (final Exception | Error e) {
            oauthParams.setUsername(binding.loginUid.getText().toString().toLowerCase().trim());
            try {
                if (software.compareTo("PEERTUBE") == 0) {
                    Token token = new RetrofitPeertubeAPI(LoginActivity.this, finalInstance, null).manageToken(oauthParams);
                    proceedLogin(token, finalInstance, software.compareTo("PEERTUBE") == 0 ? null : software);
                }
            } catch (Error error) {
                Error.displayError(LoginActivity.this, error);
                error.printStackTrace();
                runOnUiThread(() -> binding.loginButton.setEnabled(true));
            }
        }
    }


    @SuppressLint("ApplySharedPref")
    private void proceedLogin(Token token, String host, String software) {
        runOnUiThread(() -> {
            if (token != null) {
                boolean remote_account = software != null && software.toUpperCase().trim().compareTo("PEERTUBE") != 0;
                SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(Helper.PREF_KEY_OAUTH_TOKEN, token.getAccess_token());
                editor.putString(Helper.PREF_SOFTWARE, remote_account ? software : null);
                editor.putString(Helper.PREF_REMOTE_INSTANCE, remote_account ? host : null);
                if (!remote_account) {
                    editor.putString(Helper.PREF_INSTANCE, host);
                }
                editor.commit();
                //Update the account with the token;
                updateCredential(LoginActivity.this, token.getAccess_token(), client_id, client_secret, token.getRefresh_token(), host, software);
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