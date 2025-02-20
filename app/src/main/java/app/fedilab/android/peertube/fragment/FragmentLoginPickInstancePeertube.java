package app.fedilab.android.peertube.fragment;
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


import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE_PEERTUBE_BROWSING;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.INSTANCE_ADDRESS;
import static app.fedilab.android.peertube.activities.PeertubeMainActivity.typeOfConnection;
import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;
import static app.fedilab.android.peertube.helper.Helper.recreatePeertubeActivity;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentLoginPickInstancePeertubeBinding;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.entities.InstanceParams;
import app.fedilab.android.peertube.client.entities.PeertubeInformation;
import app.fedilab.android.peertube.drawer.InstanceAdapter;
import app.fedilab.android.peertube.helper.RoundedBackgroundSpan;
import app.fedilab.android.peertube.sqlite.StoredInstanceDAO;
import app.fedilab.android.peertube.viewmodel.InstancesVM;
import app.fedilab.android.sqlite.Sqlite;
import es.dmoral.toasty.Toasty;


public class FragmentLoginPickInstancePeertube extends Fragment implements InstanceAdapter.ActionClick {


    boolean[] checkedItemsCategory;
    int[] itemsKeyCategory;
    String[] itemsLabelCategory;
    boolean[] checkedItemsLanguage;
    String[] itemsKeyLanguage;
    String[] itemsLabelLanguage;
    InstanceParams instanceParams;
    private InstancesVM viewModel;
    private FragmentLoginPickInstancePeertubeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginPickInstancePeertubeBinding.inflate(inflater, container, false);

        if (peertubeInformation == null || peertubeInformation.getLanguages() == null) {
            new Thread(() -> {
                peertubeInformation = new PeertubeInformation();
                peertubeInformation.setCategories(new LinkedHashMap<>());
                peertubeInformation.setLanguages(new LinkedHashMap<>());
                peertubeInformation.setLicences(new LinkedHashMap<>());
                peertubeInformation.setPrivacies(new LinkedHashMap<>());
                peertubeInformation.setPlaylistPrivacies(new LinkedHashMap<>());
                peertubeInformation.setTranslations(new LinkedHashMap<>());
                peertubeInformation = new RetrofitPeertubeAPI(requireActivity()).getPeertubeInformation();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = () -> {
                    if (peertubeInformation == null || peertubeInformation.getLanguages() == null) {
                        Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
                    } else {
                        initializeView();
                    }
                };
                mainHandler.post(myRunnable);

            }).start();
        } else {
            initializeView();
        }

        return binding.getRoot();
    }

    private void initializeView() {
        binding.loader.setVisibility(View.VISIBLE);


        String[] channelSensitive = new String[]{"do_not_list", "blur", "display", "no_opinion"};
        String[] channelSensitivesLabel = new String[]{getString(R.string.do_not_list), getString(R.string.blur), getString(R.string.display), getString(R.string.no_opinion)};
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(requireActivity(),
                android.R.layout.simple_spinner_dropdown_item, channelSensitivesLabel);
        binding.sensitive.setAdapter(adapterChannel);


        viewModel = new ViewModelProvider(this).get(InstancesVM.class);

        binding.sensitive.setSelection(1, false);
        binding.sensitive.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                instanceParams.setNsfwPolicy(channelSensitive[position]);
                binding.loader.setVisibility(View.VISIBLE);
                viewModel.getInstances(instanceParams).observe(getViewLifecycleOwner(), apiResponse -> manageVIewInstance(apiResponse));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (peertubeInformation != null && peertubeInformation.getLanguages() != null) {
            LinkedHashMap<String, String> languages = new LinkedHashMap<>(peertubeInformation.getLanguages());
            checkedItemsLanguage = new boolean[languages.size()];
            itemsLabelLanguage = new String[languages.size()];
            itemsKeyLanguage = new String[languages.size()];

            binding.pickupLanguages.setOnClickListener(v -> {
                AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());

                int i = 0;
                if (languages.size() > 0) {
                    Iterator<Map.Entry<String, String>> it = languages.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> pair = it.next();
                        itemsLabelLanguage[i] = pair.getValue();
                        checkedItemsLanguage[i] = false;
                        itemsKeyLanguage[i] = pair.getKey();
                        it.remove();
                        i++;
                    }
                }

                dialogBuilder.setMultiChoiceItems(itemsLabelLanguage, checkedItemsLanguage, (dialog, which, isChecked) -> {
                    // The user checked or unchecked a box
                    checkedItemsLanguage[which] = isChecked;
                });

                dialogBuilder.setOnDismissListener(dialogInterface -> {

                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    String between = "";
                    stringBuilder.append(between);
                    List<String> langs = new ArrayList<>();
                    int j = 0;
                    for (boolean itemcheked : checkedItemsLanguage) {
                        if (itemcheked) {
                            langs.add(itemsKeyLanguage[j]);
                            String lang = itemsLabelLanguage[j];
                            if (lang != null && lang.trim().toLowerCase().compareTo("null") != 0) {
                                if (between.length() == 0) between = "  ";
                                String tag = "  " + lang + "  ";
                                stringBuilder.append(tag);
                                stringBuilder.setSpan(new RoundedBackgroundSpan(requireContext()), stringBuilder.length() - tag.length(), stringBuilder.length() - tag.length() + tag.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                stringBuilder.append(" ");
                            }
                        }
                        j++;
                    }
                    instanceParams.setLanguagesOr(langs);
                    binding.languagesView.setText(stringBuilder, TextView.BufferType.SPANNABLE);
                    if (binding.languagesView.getText().toString().trim().length() > 0) {
                        binding.languagesView.setVisibility(View.VISIBLE);
                    } else {
                        binding.languagesView.setVisibility(View.GONE);
                    }
                    binding.loader.setVisibility(View.VISIBLE);
                    viewModel.getInstances(instanceParams).observe(getViewLifecycleOwner(), this::manageVIewInstance);
                });
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> dialog.dismiss());

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setTitle(getString(R.string.pickup_languages));
                alertDialog.show();
            });
        }

        if (peertubeInformation != null && peertubeInformation.getCategories() != null) {
            LinkedHashMap<Integer, String> categories = new LinkedHashMap<>(peertubeInformation.getCategories());
            checkedItemsCategory = new boolean[categories.size()];
            itemsLabelCategory = new String[categories.size()];
            itemsKeyCategory = new int[categories.size()];


            binding.pickupCategories.setOnClickListener(v -> {
                AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
                int i = 0;
                if (categories.size() > 0) {
                    Iterator<Map.Entry<Integer, String>> it = categories.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Integer, String> pair = it.next();
                        itemsLabelCategory[i] = pair.getValue();
                        itemsKeyCategory[i] = pair.getKey();
                        checkedItemsCategory[i] = false;
                        it.remove();
                        i++;
                    }
                }

                dialogBuilder.setMultiChoiceItems(itemsLabelCategory, checkedItemsCategory, (dialog, which, isChecked) -> {
                    // The user checked or unchecked a box
                    checkedItemsCategory[which] = isChecked;
                });

                dialogBuilder.setOnDismissListener(dialogInterface -> {
                    int j = 0;
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    String between = "";
                    stringBuilder.append(between);
                    List<Integer> cats = new ArrayList<>();
                    for (boolean itemcheked : checkedItemsCategory) {
                        if (itemcheked) {
                            cats.add(itemsKeyCategory[j]);
                            String cat = itemsLabelCategory[j];
                            if (cat != null && cat.trim().toLowerCase().compareTo("null") != 0) {
                                if (between.length() == 0) between = "  ";
                                String tag = "  " + cat + "  ";
                                stringBuilder.append(tag);
                                stringBuilder.setSpan(new RoundedBackgroundSpan(requireContext()), stringBuilder.length() - tag.length(), stringBuilder.length() - tag.length() + tag.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                stringBuilder.append(" ");
                            }
                        }
                        j++;
                    }
                    instanceParams.setCategoriesOr(cats);
                    binding.categoriesView.setText(stringBuilder, TextView.BufferType.SPANNABLE);
                    if (binding.categoriesView.getText().toString().trim().length() > 0) {
                        binding.categoriesView.setVisibility(View.VISIBLE);
                    } else {
                        binding.categoriesView.setVisibility(View.GONE);
                    }
                    binding.loader.setVisibility(View.VISIBLE);
                    viewModel.getInstances(instanceParams).observe(getViewLifecycleOwner(), this::manageVIewInstance);
                });
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> dialog.dismiss());

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setTitle(getString(R.string.pickup_categories));
                alertDialog.show();
            });
        }


        binding.loader.setVisibility(View.VISIBLE);

        instanceParams = new InstanceParams();
        instanceParams.setNsfwPolicy(channelSensitive[1]);
        viewModel.getInstances(instanceParams).observe(getViewLifecycleOwner(), this::manageVIewInstance);
    }


    public void manageVIewInstance(APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(requireContext(), apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        List<InstanceData.Instance> instances = apiResponse.getInstances();
        if ((instances == null || instances.size() == 0)) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.lvInstances.setVisibility(View.GONE);
        } else {
            binding.noAction.setVisibility(View.GONE);
            binding.lvInstances.setVisibility(View.VISIBLE);
            InstanceAdapter instanceAdapter = new InstanceAdapter(instances);
            instanceAdapter.actionClick = this;
            binding.lvInstances.setAdapter(instanceAdapter);
            binding.lvInstances.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    @Override
    public void instance(final String instance) {
        if (typeOfConnection == PeertubeMainActivity.TypeOfConnection.REMOTE_ACCOUNT) {
            new Thread(() -> {
                final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(PREF_USER_INSTANCE_PEERTUBE_BROWSING, instance);
                editor.commit();
                InstanceData.AboutInstance aboutInstance = new RetrofitPeertubeAPI(requireActivity(), instance, null).getAboutInstance();
                SQLiteDatabase db = Sqlite.getInstance(requireActivity(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                new StoredInstanceDAO(requireActivity(), db).insertInstance(aboutInstance, instance);
                requireActivity().runOnUiThread(() -> {
                    recreatePeertubeActivity(requireActivity());
                    requireActivity().recreate();
                });
            }).start();
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(INSTANCE_ADDRESS, instance);
            Helper.addFragment(
                    getParentFragmentManager(), android.R.id.content, new PeertubeRegisterFragment(),
                    bundle, null, PeertubeRegisterFragment.class.getName());
        }
    }
}
