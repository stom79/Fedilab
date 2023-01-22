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


import static app.fedilab.android.peertube.helper.Helper.peertubeInformation;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.R;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.client.entities.InstanceParams;
import app.fedilab.android.peertube.databinding.ActivityInstancePickerBinding;
import app.fedilab.android.peertube.drawer.InstanceAdapter;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.RoundedBackgroundSpan;
import app.fedilab.android.peertube.helper.Theme;
import app.fedilab.android.peertube.viewmodel.InstancesVM;
import es.dmoral.toasty.Toasty;


public class InstancePickerActivity extends BaseActivity {


    boolean[] checkedItemsCategory;
    int[] itemsKeyCategory;
    String[] itemsLabelCategory;
    boolean[] checkedItemsLanguage;
    String[] itemsKeyLanguage;
    String[] itemsLabelLanguage;
    InstanceParams instanceParams;
    private InstancesVM viewModel;
    private ActivityInstancePickerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.setTheme(this, HelperInstance.getLiveInstance(this), false);
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding = ActivityInstancePickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loader.setVisibility(View.VISIBLE);


        String[] channelSensitive = new String[]{"do_not_list", "blur", "display", "no_opinion"};
        String[] channelSensitivesLabel = new String[]{getString(R.string.do_not_list), getString(R.string.blur), getString(R.string.display), getString(R.string.no_opinion)};
        ArrayAdapter<String> adapterChannel = new ArrayAdapter<>(InstancePickerActivity.this,
                android.R.layout.simple_spinner_dropdown_item, channelSensitivesLabel);
        binding.sensitive.setAdapter(adapterChannel);


        viewModel = new ViewModelProvider(InstancePickerActivity.this).get(InstancesVM.class);

        binding.sensitive.setSelection(1, false);
        binding.sensitive.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                instanceParams.setNsfwPolicy(channelSensitive[position]);
                binding.loader.setVisibility(View.VISIBLE);
                viewModel.getInstances(instanceParams).observe(InstancePickerActivity.this, apiResponse -> manageVIewInstance(apiResponse));
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(InstancePickerActivity.this);

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
                                stringBuilder.setSpan(new RoundedBackgroundSpan(InstancePickerActivity.this), stringBuilder.length() - tag.length(), stringBuilder.length() - tag.length() + tag.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
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
                    viewModel.getInstances(instanceParams).observe(InstancePickerActivity.this, this::manageVIewInstance);
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(InstancePickerActivity.this);
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
                                stringBuilder.setSpan(new RoundedBackgroundSpan(InstancePickerActivity.this), stringBuilder.length() - tag.length(), stringBuilder.length() - tag.length() + tag.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
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
                    viewModel.getInstances(instanceParams).observe(InstancePickerActivity.this, this::manageVIewInstance);
                });
                dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> dialog.dismiss());

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setTitle(getString(R.string.pickup_categories));
                alertDialog.show();
            });
        }


        binding.loader.setVisibility(View.VISIBLE);

        setTitle(R.string.instances_picker);

        instanceParams = new InstanceParams();
        instanceParams.setNsfwPolicy(channelSensitive[1]);
        viewModel.getInstances(instanceParams).observe(InstancePickerActivity.this, this::manageVIewInstance);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void manageVIewInstance(APIResponse apiResponse) {
        binding.loader.setVisibility(View.GONE);
        if (apiResponse.getError() != null) {
            Toasty.error(InstancePickerActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        List<InstanceData.Instance> instances = apiResponse.getInstances();
        RecyclerView lv_instances = findViewById(R.id.lv_instances);
        if ((instances == null || instances.size() == 0)) {
            binding.noAction.setVisibility(View.VISIBLE);
            lv_instances.setVisibility(View.GONE);
        } else {
            binding.noAction.setVisibility(View.GONE);
            lv_instances.setVisibility(View.VISIBLE);
            InstanceAdapter instanceAdapter = new InstanceAdapter(instances);
            lv_instances.setAdapter(instanceAdapter);
            lv_instances.setLayoutManager(new LinearLayoutManager(InstancePickerActivity.this));
        }
    }
}
