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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Filter;
import app.fedilab.android.databinding.ActivityFiltersBinding;
import app.fedilab.android.databinding.PopupAddFilterBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.FilterAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;

public class FilterActivity extends BaseActivity implements FilterAdapter.Delete {

    private ActivityFiltersBinding binding;
    private List<Filter> filterList;
    private FilterAdapter filterAdapter;

    /**
     * Method that allows to add or edit filter depending if Filter passing into params is null (null = insertion)
     *
     * @param context  - Context
     * @param filter   - {@link Filter}
     * @param listener - {@link FilterAdapter.FilterAction}
     */
    public static void addEditFilter(Context context, Filter filter, FilterAdapter.FilterAction listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, Helper.dialogStyle());
        PopupAddFilterBinding popupAddFilterBinding = PopupAddFilterBinding.inflate(LayoutInflater.from(context));
        AccountsVM accountsVM = new ViewModelProvider((ViewModelStoreOwner) context).get(AccountsVM.class);
        dialogBuilder.setView(popupAddFilterBinding.getRoot());
        ArrayAdapter<CharSequence> adapterResize = ArrayAdapter.createFromResource(Objects.requireNonNull(context),
                R.array.filter_expire, android.R.layout.simple_spinner_dropdown_item);
        popupAddFilterBinding.filterExpire.setAdapter(adapterResize);
        final int[] expire = {-1};
        popupAddFilterBinding.filterExpire.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent1, View view, int position1, long id) {
                switch (position1) {
                    case 0:
                        expire[0] = -1;
                        break;
                    case 1:
                        expire[0] = 3600;
                        break;
                    case 2:
                        expire[0] = 21600;
                        break;
                    case 3:
                        expire[0] = 43200;
                        break;
                    case 4:
                        expire[0] = 86400;
                        break;
                    case 5:
                        expire[0] = 604800;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent1) {
            }
        });
        if (filter != null) {
            popupAddFilterBinding.addPhrase.setText(filter.phrase);
            if (filter.context != null)
                for (String val : filter.context) {
                    switch (val) {
                        case "home":
                            popupAddFilterBinding.contextHome.setChecked(true);
                            break;
                        case "public":
                            popupAddFilterBinding.contextPublic.setChecked(true);
                            break;
                        case "notifications":
                            popupAddFilterBinding.contextNotification.setChecked(true);
                            break;
                        case "thread":
                            popupAddFilterBinding.contextConversation.setChecked(true);
                            break;
                    }
                }
            popupAddFilterBinding.contextWholeWord.setChecked(filter.whole_word);
            popupAddFilterBinding.contextDrop.setChecked(filter.irreversible);
        }


        AlertDialog alertDialog = dialogBuilder.setPositiveButton(R.string.validate, null)
                .setNegativeButton(R.string.cancel, null).create();
        alertDialog.setOnShowListener(dialogInterface -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                if (popupAddFilterBinding.addPhrase.getText() == null || popupAddFilterBinding.addPhrase.getText().toString().trim().length() == 0) {
                    popupAddFilterBinding.addPhrase.setError(context.getString(R.string.cannot_be_empty));
                    return;
                }
                if (!popupAddFilterBinding.contextConversation.isChecked() && !popupAddFilterBinding.contextHome.isChecked() && !popupAddFilterBinding.contextPublic.isChecked() && !popupAddFilterBinding.contextNotification.isChecked()) {
                    popupAddFilterBinding.contextDescription.setError(context.getString(R.string.cannot_be_empty));
                    return;
                }
                if (popupAddFilterBinding.addPhrase.getText() != null && popupAddFilterBinding.addPhrase.getText().toString().trim().length() > 0) {
                    Filter filterSent = new Filter();
                    ArrayList<String> contextFilter = new ArrayList<>();
                    if (popupAddFilterBinding.contextHome.isChecked())
                        contextFilter.add("home");
                    if (popupAddFilterBinding.contextPublic.isChecked())
                        contextFilter.add("public");
                    if (popupAddFilterBinding.contextNotification.isChecked())
                        contextFilter.add("notifications");
                    if (popupAddFilterBinding.contextConversation.isChecked())
                        contextFilter.add("thread");
                    filterSent.context = contextFilter;
                    filterSent.expires_at_sent = expire[0];
                    filterSent.phrase = popupAddFilterBinding.addPhrase.getText().toString();
                    filterSent.whole_word = popupAddFilterBinding.contextWholeWord.isChecked();
                    filterSent.irreversible = popupAddFilterBinding.contextDrop.isChecked();
                    if (filter != null) {
                        accountsVM.editFilter(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filter.id, filterSent.phrase, filterSent.context, filterSent.irreversible, filterSent.whole_word, filterSent.expires_at_sent)
                                .observe((LifecycleOwner) context, listener::callback);
                    } else {
                        accountsVM.addFilter(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, filterSent.phrase, filterSent.context, filterSent.irreversible, filterSent.whole_word, filterSent.expires_at_sent)
                                .observe((LifecycleOwner) context, listener::callback);
                    }
                    alertDialog.dismiss();
                }
            });
            Button buttonCancel = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonCancel.setOnClickListener(view -> alertDialog.dismiss());
        });
        alertDialog.setTitle(context.getString(R.string.action_update_filter));
        alertDialog.setOnDismissListener(dialogInterface -> {
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(popupAddFilterBinding.addPhrase.getWindowToken(), 0);
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        alertDialog.show();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityFiltersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        filterList = new ArrayList<>();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AccountsVM accountsVM = new ViewModelProvider(FilterActivity.this).get(AccountsVM.class);
        accountsVM.getFilters(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                .observe(FilterActivity.this, filters -> {
                    BaseMainActivity.mainFilters = filters;
                    if (filters != null && filters.size() > 0) {
                        filterList.addAll(filters);
                        filterAdapter = new FilterAdapter(filterList);
                        filterAdapter.delete = this;
                        binding.lvFilters.setAdapter(filterAdapter);
                        binding.lvFilters.setLayoutManager(new LinearLayoutManager(FilterActivity.this));
                    } else {
                        binding.lvFilters.setVisibility(View.GONE);
                        binding.noAction.setVisibility(View.VISIBLE);
                    }
                });

        binding.addFilter.setOnClickListener(v -> addEditFilter(FilterActivity.this, null, filter -> {
            if (filter != null) {
                filterList.add(0, filter);
                if (filterAdapter != null) {
                    filterAdapter.notifyItemInserted(0);
                } else {
                    filterAdapter = new FilterAdapter(filterList);
                    filterAdapter.delete = FilterActivity.this;
                    binding.lvFilters.setAdapter(filterAdapter);
                    binding.lvFilters.setLayoutManager(new LinearLayoutManager(FilterActivity.this));
                }
                binding.lvFilters.setVisibility(View.VISIBLE);
                binding.noAction.setVisibility(View.GONE);
            }
        }));

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
    public void allFiltersDeleted() {
        binding.lvFilters.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.VISIBLE);
    }
}
