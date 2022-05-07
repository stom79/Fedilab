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

import static app.fedilab.android.BaseMainActivity.admin;
import static app.fedilab.android.BaseMainActivity.api;
import static app.fedilab.android.BaseMainActivity.client_id;
import static app.fedilab.android.BaseMainActivity.client_secret;
import static app.fedilab.android.BaseMainActivity.currentInstance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.activities.WebviewConnectActivity;
import app.fedilab.android.client.entities.Account;
import app.fedilab.android.client.entities.InstanceSocial;
import app.fedilab.android.databinding.FragmentLoginMainBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.MastodonHelper;
import app.fedilab.android.viewmodel.mastodon.AppsVM;
import app.fedilab.android.viewmodel.mastodon.InstanceSocialVM;
import app.fedilab.android.viewmodel.mastodon.NodeInfoVM;
import es.dmoral.toasty.Toasty;

public class FragmentLoginMain extends Fragment {

    private static boolean client_id_for_webview = false;
    private FragmentLoginMainBinding binding;
    private boolean searchInstanceRunning = false;
    private String oldSearch;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentLoginMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.menuIcon.setOnClickListener(this::showMenu);
        binding.loginInstance.setOnItemClickListener((parent, view, position, id) -> oldSearch = parent.getItemAtPosition(position).toString().trim());
        binding.loginInstance.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 2 && !searchInstanceRunning) {
                    String query = s.toString().trim();
                    if (query.startsWith("http://")) {
                        query = query.replace("http://", "");
                    }
                    if (query.startsWith("https://")) {
                        query = query.replace("https://", "");
                    }
                    if (oldSearch == null || !oldSearch.equals(s.toString().trim())) {
                        searchInstanceRunning = true;
                        InstanceSocialVM instanceSocialVM = new ViewModelProvider(FragmentLoginMain.this).get(InstanceSocialVM.class);
                        instanceSocialVM.getInstances(query).observe(requireActivity(), instanceSocialList -> {
                            binding.loginInstance.setAdapter(null);
                            String[] instances = new String[instanceSocialList.instances.size()];
                            int j = 0;
                            for (InstanceSocial.Instance instance : instanceSocialList.instances) {
                                instances[j] = instance.name;
                                j++;
                            }
                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, instances);
                            binding.loginInstance.setAdapter(adapter);
                            if (binding.loginInstance.hasFocus() && !requireActivity().isFinishing())
                                binding.loginInstance.showDropDown();
                            if (oldSearch != null && oldSearch.equals(binding.loginInstance.getText().toString())) {
                                binding.loginInstance.dismissDropDown();
                            }

                            oldSearch = s.toString().trim();
                            searchInstanceRunning = false;
                        });
                    }
                }
            }
        });

        binding.noAccountA.setOnClickListener(v -> Helper.addFragment(
                getParentFragmentManager(), android.R.id.content, new FragmentLoginJoin(),
                null, null, FragmentLoginJoin.class.getName()));

        binding.continueButton.setOnClickListener(v -> {
            if (binding.loginInstance.getText() == null || binding.loginInstance.getText().toString().length() == 0) {
                binding.loginInstanceLayout.setError(getString(R.string.toast_error_instance));
                binding.loginInstanceLayout.setErrorEnabled(true);
                return;
            }
            currentInstance = binding.loginInstance.getText().toString().trim().toLowerCase();
            if (currentInstance.length() == 0) {
                return;
            }
            binding.continueButton.setEnabled(false);
            NodeInfoVM nodeInfoVM = new ViewModelProvider(requireActivity()).get(NodeInfoVM.class);
            nodeInfoVM.getNodeInfo(binding.loginInstance.getText().toString()).observe(requireActivity(), nodeInfo -> {
                binding.continueButton.setEnabled(true);
                if (nodeInfo != null && nodeInfo.software != null) {
                    BaseMainActivity.software = nodeInfo.software.name.toUpperCase();
                    if (nodeInfo.software.name.toUpperCase().trim().equals("PLEROMA") || nodeInfo.software.name.toUpperCase().trim().equals("MASTODON") || nodeInfo.software.name.toUpperCase().trim().equals("PIXELFED")) {
                        client_id_for_webview = true;
                        api = Account.API.MASTODON;
                        retrievesClientId(currentInstance);
                    } else {
                        client_id_for_webview = false;
                        if (nodeInfo.software.name.equals("PEERTUBE")) {
                            Toasty.error(requireActivity(), "Peertube is currently not supported", Toasty.LENGTH_LONG).show();
                        } else if (nodeInfo.software.name.equals("GNU")) {
                            Toasty.error(requireActivity(), "GNU is currently not supported", Toasty.LENGTH_LONG).show();
                        } else { //Fallback to Mastodon
                            client_id_for_webview = true;
                            api = Account.API.MASTODON;
                            retrievesClientId(currentInstance);
                        }
                    }
                } else { //Fallback to Mastodon
                    client_id_for_webview = true;
                    api = Account.API.MASTODON;
                    retrievesClientId(currentInstance);
                }
            });
        });
        return root;
    }

    private void showMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(requireActivity(), Helper.popupStyle()), binding.menuIcon);
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.main_login, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_about) {
                /* Todo: Open about page */
            } else if (itemId == R.id.action_privacy) {
                /* Todo: Open privacy page */
            } else if (itemId == R.id.action_proxy) {
                /* Todo: Open proxy settings */
            } else if (itemId == R.id.action_custom_tabs) {
                setMenuItemKeepOpen(item);
                /* Todo: Toggle custom tabs */
            } else if (itemId == R.id.action_import_data) {
                /* Todo: Import data */
            } else if (itemId == R.id.action_provider) {
                setMenuItemKeepOpen(item);
                /* Todo: Toggle security provider */
            }
            return false;
        });
        popupMenu.show();
    }

    private void setMenuItemKeepOpen(MenuItem item) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(requireContext()));
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void retrievesClientId(String instance) {
        if (client_id_for_webview) {
            if (!instance.startsWith("http://") && !instance.startsWith("https://")) {
                instance = "https://" + instance;
            }
            String host = instance;
            try {
                URL url = new URL(instance);
                host = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try {
                currentInstance = URLEncoder.encode(host, "utf-8");
            } catch (UnsupportedEncodingException e) {
                Toasty.error(requireActivity(), getString(R.string.client_error), Toast.LENGTH_LONG).show();
            }
            if (api == Account.API.MASTODON) {
                String scopes = Helper.OAUTH_SCOPES;
                if (admin) {
                    scopes = Helper.OAUTH_SCOPES_ADMIN;
                }
                AppsVM appsVM = new ViewModelProvider(requireActivity()).get(AppsVM.class);
                appsVM.createApp(currentInstance, getString(R.string.app_name),
                        client_id_for_webview ? Helper.REDIRECT_CONTENT_WEB : Helper.REDIRECT_CONTENT,
                        scopes,
                        Helper.WEBSITE_VALUE
                ).observe(requireActivity(), app -> {
                    client_id = app.client_id;
                    client_secret = app.client_secret;
                    String redirectUrl = MastodonHelper.authorizeURL(currentInstance, client_id, admin);
                    SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                    boolean embedded_browser = sharedpreferences.getBoolean(getString(R.string.SET_EMBEDDED_BROWSER), true);
                    if (embedded_browser) {
                        Intent i = new Intent(requireActivity(), WebviewConnectActivity.class);
                        i.putExtra("login_url", redirectUrl);
                        startActivity(i);
                        requireActivity().finish();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse(redirectUrl));
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            Toasty.error(requireActivity(), getString(R.string.toast_error), Toast.LENGTH_LONG).show();
                        }

                    }
                });
            }
        }
    }
}