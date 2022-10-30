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


import static app.fedilab.android.helper.PinnedTimelineHelper.sortListPositionAsc;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.MastodonList;
import app.fedilab.android.client.entities.app.PinnedTimeline;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityListBinding;
import app.fedilab.android.databinding.PopupAddListBinding;
import app.fedilab.android.databinding.PopupManageAccountsListBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.AccountListAdapter;
import app.fedilab.android.ui.drawer.MastodonListAdapter;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.viewmodel.mastodon.ReorderVM;
import app.fedilab.android.viewmodel.mastodon.TimelinesVM;
import es.dmoral.toasty.Toasty;


public class MastodonListActivity extends BaseActivity implements MastodonListAdapter.ActionOnList {


    AccountListAdapter accountsInListAdapter;
    private ActivityListBinding binding;
    private boolean canGoBack;
    private TimelinesVM timelinesVM;
    private MastodonList mastodonList;
    private ArrayList<MastodonList> mastodonListList;
    private MastodonListAdapter mastodonListAdapter;
    private AccountsVM accountsVM;
    private List<Account> accountsInList;
    private boolean flagLoading;
    private String max_id;
    private FragmentMastodonTimeline fragmentMastodonTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        canGoBack = false;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        flagLoading = false;
        max_id = null;
        accountsVM = new ViewModelProvider(MastodonListActivity.this).get(AccountsVM.class);
        timelinesVM = new ViewModelProvider(MastodonListActivity.this).get(TimelinesVM.class);
        timelinesVM.getLists(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                .observe(MastodonListActivity.this, mastodonLists -> {
                    ReorderVM reorderVM = new ViewModelProvider(MastodonListActivity.this).get(ReorderVM.class);
                    reorderVM.getPinned().observe(MastodonListActivity.this, pinned -> {
                        if (mastodonLists != null && mastodonLists.size() > 0) {
                            mastodonListList = new ArrayList<>(mastodonLists);
                            if (pinned != null) {
                                if (pinned.pinnedTimelines != null && pinned.pinnedTimelines.size() > 0) {
                                    for (PinnedTimeline pinnedTimeline : pinned.pinnedTimelines) {
                                        if (pinnedTimeline.type == Timeline.TimeLineEnum.LIST) {
                                            for (MastodonList mastodonList : mastodonLists) {
                                                if (mastodonList.id.equalsIgnoreCase(pinnedTimeline.mastodonList.id)) {
                                                    mastodonList.position = pinnedTimeline.position;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            sortListPositionAsc(mastodonListList);
                            mastodonListAdapter = new MastodonListAdapter(mastodonListList);
                            mastodonListAdapter.actionOnList = this;
                            binding.notContent.setVisibility(View.GONE);
                            binding.recyclerView.setAdapter(mastodonListAdapter);
                            binding.recyclerView.setLayoutManager(new LinearLayoutManager(MastodonListActivity.this));
                        } else {
                            binding.notContent.setVisibility(View.VISIBLE);
                        }
                    });
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_manage_users) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MastodonListActivity.this, Helper.dialogStyle());
            PopupManageAccountsListBinding popupManageAccountsListBinding = PopupManageAccountsListBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(popupManageAccountsListBinding.getRoot());
            popupManageAccountsListBinding.loader.setVisibility(View.VISIBLE);

            popupManageAccountsListBinding.searchAccount.setOnTouchListener((v, event) -> {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (popupManageAccountsListBinding.searchAccount.length() > 0 && event.getRawX() >= (popupManageAccountsListBinding.searchAccount.getRight() - popupManageAccountsListBinding.searchAccount.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        popupManageAccountsListBinding.searchAccount.setText("");
                    }
                }
                return false;
            });
            timelinesVM.getAccountsInList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id, null, null, 10)
                    .observe(MastodonListActivity.this, accounts -> {
                        popupManageAccountsListBinding.loader.setVisibility(View.GONE);
                        accountsInList = accounts;
                        if (accountsInList == null) {
                            accountsInList = new ArrayList<>();
                        }
                        if (accountsInList.size() > 0) {
                            max_id = accountsInList.get(accountsInList.size() - 1).id;
                            popupManageAccountsListBinding.noContent.setVisibility(View.GONE);
                            popupManageAccountsListBinding.lvAccountsCurrent.setVisibility(View.VISIBLE);
                        } else {
                            popupManageAccountsListBinding.noContent.setVisibility(View.VISIBLE);
                            popupManageAccountsListBinding.lvAccountsCurrent.setVisibility(View.GONE);
                        }
                        accountsInListAdapter = new AccountListAdapter(mastodonList, accountsInList, null);
                        popupManageAccountsListBinding.lvAccountsCurrent.setAdapter(accountsInListAdapter);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MastodonListActivity.this);
                        popupManageAccountsListBinding.lvAccountsCurrent.setLayoutManager(linearLayoutManager);
                        popupManageAccountsListBinding.lvAccountsCurrent.addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                                if (dy > 0) {
                                    int visibleItemCount = linearLayoutManager.getChildCount();
                                    int totalItemCount = linearLayoutManager.getItemCount();
                                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                                        if (!flagLoading) {
                                            flagLoading = true;
                                            timelinesVM.getAccountsInList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id, max_id, null, 10)
                                                    .observe(MastodonListActivity.this, accounts -> {
                                                        if (accounts != null && accounts.size() > 0) {
                                                            int position = accountsInList.size();
                                                            max_id = accountsInList.get(accounts.size() - 1).id;
                                                            accountsInList.addAll(accounts);
                                                            accountsInListAdapter.notifyItemRangeChanged(position, accounts.size());
                                                        }

                                                    });

                                        }
                                    }
                                }

                            }
                        });
                    });

            popupManageAccountsListBinding.searchAccount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (count > 0) {
                        popupManageAccountsListBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_close_24, 0);
                    } else {
                        popupManageAccountsListBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_search_24, 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s != null && s.length() > 0) {
                        accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, s.toString(), 20, true, true)
                                .observe(MastodonListActivity.this, accounts -> {
                                    popupManageAccountsListBinding.lvAccountsSearch.setVisibility(View.VISIBLE);
                                    popupManageAccountsListBinding.lvAccountsCurrent.setVisibility(View.GONE);
                                    AccountListAdapter accountListAdapter = new AccountListAdapter(mastodonList, accountsInList, accounts);
                                    popupManageAccountsListBinding.lvAccountsSearch.setAdapter(accountListAdapter);
                                    popupManageAccountsListBinding.lvAccountsSearch.setLayoutManager(new LinearLayoutManager(MastodonListActivity.this));
                                });
                    } else {
                        popupManageAccountsListBinding.lvAccountsSearch.setVisibility(View.GONE);
                        popupManageAccountsListBinding.lvAccountsCurrent.setVisibility(View.VISIBLE);
                    }
                }
            });

            dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
            dialogBuilder.create().show();
        } else if (item.getItemId() == R.id.action_delete && mastodonList != null) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(MastodonListActivity.this, Helper.dialogStyle());
            alt_bld.setTitle(R.string.action_lists_delete);
            alt_bld.setMessage(R.string.action_lists_confirm_delete);
            alt_bld.setPositiveButton(R.string.delete, (dialog, id) -> {
                timelinesVM.deleteList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, mastodonList.id);
                int position = 0;
                for (MastodonList mastodonListTmp : mastodonListList) {
                    if (mastodonListTmp.id.equalsIgnoreCase(mastodonList.id)) {
                        break;
                    }
                    position++;
                }
                mastodonListList.remove(position);
                mastodonListAdapter.notifyItemRemoved(position);
                ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.recyclerView, () -> {
                    canGoBack = false;
                    if (fragmentMastodonTimeline != null) {
                        fragmentMastodonTimeline.onDestroyView();
                    }
                    invalidateOptionsMenu();
                    setTitle(R.string.action_lists);
                });
                if (mastodonListList.size() == 0) {
                    binding.notContent.setVisibility(View.VISIBLE);
                } else {
                    binding.notContent.setVisibility(View.GONE);
                }
                Bundle b = new Bundle();
                b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
                Intent intentBD = new Intent(Helper.BROADCAST_DATA);
                b.putSerializable(Helper.RECEIVE_MASTODON_LIST, mastodonListList);
                intentBD.putExtras(b);
                LocalBroadcastManager.getInstance(MastodonListActivity.this).sendBroadcast(intentBD);
            });
            alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            AlertDialog alert = alt_bld.create();
            alert.show();
        } else if (item.getItemId() == R.id.action_add_list) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MastodonListActivity.this, Helper.dialogStyle());
            PopupAddListBinding popupAddListBinding = PopupAddListBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(popupAddListBinding.getRoot());
            popupAddListBinding.addList.setFilters(new InputFilter[]{new InputFilter.LengthFilter(255)});
            dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                if (popupAddListBinding.addList.getText() != null && popupAddListBinding.addList.getText().toString().trim().length() > 0) {
                    timelinesVM.createList(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, popupAddListBinding.addList.getText().toString().trim(), null)
                            .observe(MastodonListActivity.this, newMastodonList -> {
                                if (mastodonListList == null) {
                                    mastodonListList = new ArrayList<>();
                                }
                                if (newMastodonList != null && mastodonListAdapter != null) {
                                    mastodonListList.add(0, newMastodonList);
                                    mastodonListAdapter.notifyItemInserted(0);
                                } else {
                                    Toasty.error(MastodonListActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                                }
                                Bundle b = new Bundle();
                                b.putBoolean(Helper.RECEIVE_REDRAW_TOPBAR, true);
                                Intent intentBD = new Intent(Helper.BROADCAST_DATA);
                                b.putSerializable(Helper.RECEIVE_MASTODON_LIST, mastodonListList);
                                intentBD.putExtras(b);
                                LocalBroadcastManager.getInstance(MastodonListActivity.this).sendBroadcast(intentBD);
                            });
                    dialog.dismiss();
                } else {
                    popupAddListBinding.addList.setError(getString(R.string.not_valid_list_name));
                }

            });
            dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            dialogBuilder.create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void click(MastodonList mastodonList) {

        this.mastodonList = mastodonList;
        canGoBack = true;
        ThemeHelper.slideViewsToLeft(binding.recyclerView, binding.fragmentContainer, () -> {
            fragmentMastodonTimeline = new FragmentMastodonTimeline();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.ARG_LIST_ID, mastodonList.id);
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.LIST);
            setTitle(mastodonList.title);
            fragmentMastodonTimeline.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonTimeline);
            fragmentTransaction.commit();
            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (!canGoBack) {
            getMenuInflater().inflate(R.menu.menu_main_list, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_list, menu);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.recyclerView, () -> {
                if (fragmentMastodonTimeline != null) {
                    fragmentMastodonTimeline.onDestroyView();
                }
            });
            setTitle(R.string.action_lists);
            invalidateOptionsMenu();
        } else {
            super.onBackPressed();
        }
    }
}
