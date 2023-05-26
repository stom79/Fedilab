package app.fedilab.android.ui.fragment;
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


import static android.app.Activity.RESULT_OK;
import static app.fedilab.android.activities.LoginActivity.apiLogin;
import static app.fedilab.android.activities.LoginActivity.client_idLogin;
import static app.fedilab.android.activities.LoginActivity.client_secretLogin;
import static app.fedilab.android.activities.LoginActivity.currentInstanceLogin;
import static app.fedilab.android.activities.LoginActivity.requestedAdmin;
import static app.fedilab.android.activities.LoginActivity.softwareLogin;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.net.MalformedURLException;
import java.net.URL;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentLoginMainBinding;
import app.fedilab.android.mastodon.activities.ProxyActivity;
import app.fedilab.android.mastodon.client.entities.app.Account;
import app.fedilab.android.mastodon.client.entities.app.InstanceSocial;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.helper.ZipHelper;
import app.fedilab.android.mastodon.viewmodel.mastodon.AppsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.InstanceSocialVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.NodeInfoVM;
import app.fedilab.android.peertube.activities.LoginActivity;
import es.dmoral.toasty.Toasty;

public class FragmentLoginMain extends Fragment {

    private static final int REQUEST_CODE = 5412;
    private final int PICK_IMPORT = 5557;
    private FragmentLoginMainBinding binding;
    private boolean searchInstanceRunning = false;
    private String oldSearch;
    private ActivityResultLauncher<String> permissionLauncher;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentLoginMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        InstanceSocialVM instanceSocialVM = new ViewModelProvider(FragmentLoginMain.this).get(InstanceSocialVM.class);
        binding.menuIcon.setOnClickListener(this::showMenu);
        binding.loginInstance.setOnItemClickListener((parent, view, position, id) -> oldSearch = parent.getItemAtPosition(position).toString().trim());

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                proceed();
            } else {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        });

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

                        instanceSocialVM.getInstances(query).observe(requireActivity(), instanceSocialList -> {
                            binding.loginInstance.setAdapter(null);
                            if (instanceSocialList.instances.isEmpty()) {
                                binding.loginInstance.dismissDropDown();
                            } else {
                                String[] instances = new String[instanceSocialList.instances.size()];
                                int j = 0;
                                for (InstanceSocial.Instance instance : instanceSocialList.instances) {
                                    instances[j] = instance.name;
                                    j++;
                                }
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(),
                                        android.R.layout.simple_list_item_1, instances);
                                binding.loginInstance.setAdapter(adapter);
                                if (binding.loginInstance.hasFocus() && !requireActivity().isFinishing())
                                    binding.loginInstance.showDropDown();
                            }
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
            currentInstanceLogin = binding.loginInstance.getText().toString().trim().toLowerCase();
            if (currentInstanceLogin.length() == 0) {
                return;
            }
            binding.continueButton.setEnabled(false);
            NodeInfoVM nodeInfoVM = new ViewModelProvider(requireActivity()).get(NodeInfoVM.class);
            String instance = binding.loginInstance.getText().toString().trim();
            nodeInfoVM.getNodeInfo(instance).observe(requireActivity(), nodeInfo -> {
                if (nodeInfo != null) {
                    BaseMainActivity.software = nodeInfo.software.name.toUpperCase();
                    switch (nodeInfo.software.name.toUpperCase().trim()) {
                        case "MASTODON":
                            apiLogin = Account.API.MASTODON;
                            break;
                        case "FRIENDICA":
                            apiLogin = Account.API.FRIENDICA;
                            break;
                        case "PIXELFED":
                            apiLogin = Account.API.PIXELFED;
                            break;
                        case "AKKOMA":
                        case "PLEROMA":
                            apiLogin = Account.API.PLEROMA;
                            break;
                        case "PEERTUBE":
                            apiLogin = Account.API.PEERTUBE;
                            break;
                        default:
                            apiLogin = Account.API.UNKNOWN;
                            break;
                    }
                    softwareLogin = nodeInfo.software.name.toUpperCase();
                } else {
                    apiLogin = Account.API.MASTODON;
                    softwareLogin = "MASTODON";
                }

                binding.continueButton.setEnabled(true);
                if (apiLogin != Account.API.PEERTUBE) {
                    retrievesClientId(currentInstanceLogin);
                } else {
                    Intent peertubeLogin = new Intent(requireActivity(), LoginActivity.class);
                    peertubeLogin.putExtra(Helper.ARG_INSTANCE, instance);
                    startActivity(peertubeLogin);
                    requireActivity().finish();
                }
            });
        });
        return root;
    }

    private void showMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(requireActivity(), binding.menuIcon);
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.main_login, popupMenu.getMenu());
        MenuItem adminTabItem = popupMenu.getMenu().findItem(R.id.action_request_admin);
        adminTabItem.setChecked(requestedAdmin);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean customTab = sharedpreferences.getBoolean(getString(R.string.SET_CUSTOM_TABS), true);
        popupMenu.getMenu().findItem(R.id.action_custom_tabs).setChecked(customTab);

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_proxy) {
                (new ProxyActivity()).show(requireActivity().getSupportFragmentManager(), null);
            } else if (itemId == R.id.action_request_admin) {

                item.setChecked(!item.isChecked());
                requestedAdmin = item.isChecked();
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
            } else if (itemId == R.id.action_import_data) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    proceed();
                }
            } else if (itemId == R.id.action_custom_tabs) {
                boolean newValue = !item.isChecked();
                item.setChecked(newValue);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(getString(R.string.SET_CUSTOM_TABS), newValue);
                editor.apply();
                return false;
            }
            return false;
        });
        popupMenu.show();
    }

    private void proceed() {
        Intent openFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        openFileIntent.setType("application/zip");
        String[] mimeTypes = new String[]{"application/zip"};
        openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        //noinspection deprecation
        startActivityForResult(
                Intent.createChooser(
                        openFileIntent,
                        getString(R.string.load_settings)), PICK_IMPORT);
    }

    private void retrievesClientId(String instance) {
        String oldInstance = instance;
        if (!instance.startsWith("http://") && !instance.startsWith("https://")) {
            instance = "https://" + instance;
        }
        String host;
        try {
            URL url = new URL(instance);
            host = url.getHost();
        } catch (MalformedURLException e) {
            host = oldInstance;
            e.printStackTrace();
        }

        currentInstanceLogin = host;
        String scopes = requestedAdmin ? Helper.OAUTH_SCOPES_ADMIN : Helper.OAUTH_SCOPES;
        AppsVM appsVM = new ViewModelProvider(requireActivity()).get(AppsVM.class);
        appsVM.createApp(currentInstanceLogin, getString(R.string.app_name),
                Helper.REDIRECT_CONTENT_WEB,
                scopes,
                Helper.WEBSITE_VALUE
        ).observe(requireActivity(), app -> {
            if (app != null) {
                client_idLogin = app.client_id;
                client_secretLogin = app.client_secret;
                String redirectUrl = MastodonHelper.authorizeURL(currentInstanceLogin, client_idLogin, requestedAdmin);
                SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                boolean customTab = sharedpreferences.getBoolean(getString(R.string.SET_CUSTOM_TABS), true);
                if (customTab) {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    int colorInt = ThemeHelper.getAttColor(requireActivity(), R.attr.statusBar);
                    CustomTabColorSchemeParams defaultColors = new CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(colorInt)
                            .build();
                    builder.setDefaultColorSchemeParams(defaultColors);
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.launchUrl(requireActivity(), Uri.parse(redirectUrl));
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
            } else {
                Toasty.error(requireActivity(), getString(R.string.client_error), Toasty.LENGTH_SHORT).show();
            }

        });
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMPORT && resultCode == RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toasty.error(requireActivity(), getString(R.string.toot_select_file_error), Toast.LENGTH_LONG).show();
                return;
            }
            Helper.createFileFromUri(requireActivity(), data.getData(), file -> ZipHelper.importData(requireActivity(), file));
        } else {
            Toasty.error(requireActivity(), getString(R.string.toot_select_file_error), Toast.LENGTH_LONG).show();
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent openFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openFileIntent.setType("application/zip");
                String[] mimeTypes = new String[]{"application/zip"};
                openFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(
                        Intent.createChooser(
                                openFileIntent,
                                getString(R.string.load_settings)), PICK_IMPORT);
            } else {
                Toasty.error(requireActivity(), getString(R.string.permission_missing), Toasty.LENGTH_SHORT).show();
            }
        }
    }
}