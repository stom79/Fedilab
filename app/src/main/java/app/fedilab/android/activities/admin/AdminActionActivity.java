package app.fedilab.android.activities.admin;
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

import static app.fedilab.android.activities.admin.AdminActionActivity.AdminEnum.ACCOUNT;
import static app.fedilab.android.activities.admin.AdminActionActivity.AdminEnum.DOMAIN;
import static app.fedilab.android.activities.admin.AdminActionActivity.AdminEnum.REPORT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.annotations.SerializedName;

import app.fedilab.android.R;
import app.fedilab.android.activities.BaseBarActivity;
import app.fedilab.android.client.entities.api.admin.AdminDomainBlock;
import app.fedilab.android.databinding.ActivityAdminActionsBinding;
import app.fedilab.android.databinding.PopupAdminFilterAccountsBinding;
import app.fedilab.android.databinding.PopupAdminFilterReportsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.admin.FragmentAdminAccount;
import app.fedilab.android.ui.fragment.admin.FragmentAdminDomain;
import app.fedilab.android.ui.fragment.admin.FragmentAdminReport;

public class AdminActionActivity extends BaseBarActivity {

    public static Boolean local = true, remote = true, active = true, pending = true, disabled = true, silenced = true, suspended = true, staff = null, orderByMostRecent = true;
    public static Boolean resolved = null, reportLocal = true, reportRemote = true;
    private ActivityAdminActionsBinding binding;
    private boolean canGoBack;
    private FragmentAdminReport fragmentAdminReport;
    private FragmentAdminAccount fragmentAdminAccount;
    private FragmentAdminDomain fragmentAdminDomain;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                AdminDomainBlock adminDomainBlock = (AdminDomainBlock) b.getSerializable(Helper.ARG_ADMIN_DOMAINBLOCK);
                AdminDomainBlock adminDomainBlockDelete = (AdminDomainBlock) b.getSerializable(Helper.ARG_ADMIN_DOMAINBLOCK_DELETE);
                if (adminDomainBlock != null && adminDomainBlock.domain != null && fragmentAdminDomain != null) {
                    fragmentAdminDomain.update(adminDomainBlock);
                }
                if (adminDomainBlockDelete != null && fragmentAdminDomain != null) {
                    fragmentAdminDomain.delete(adminDomainBlockDelete);
                }
            }

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminActionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(Helper.BROADCAST_DATA));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        canGoBack = false;
        binding.reports.setOnClickListener(v -> displayTimeline(REPORT));
        binding.accounts.setOnClickListener(v -> displayTimeline(ACCOUNT));
        binding.domains.setOnClickListener(v -> displayTimeline(DOMAIN));

    }

    private void displayTimeline(AdminEnum type) {
        canGoBack = true;
        if (type == REPORT) {
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
                fragmentAdminReport = new FragmentAdminReport();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
                bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + type.getValue());
                fragmentAdminReport.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragmentAdminReport);
                fragmentTransaction.commit();
            });
        } else if (type == ACCOUNT) {
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
                fragmentAdminAccount = new FragmentAdminAccount();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
                bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + type.getValue());
                fragmentAdminAccount.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragmentAdminAccount);
                fragmentTransaction.commit();
            });
        } else if (type == DOMAIN) {
            ThemeHelper.slideViewsToLeft(binding.buttonContainer, binding.fragmentContainer, () -> {
                fragmentAdminDomain = new FragmentAdminDomain();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, type);
                bundle.putString(Helper.ARG_VIEW_MODEL_KEY, "FEDILAB_" + type.getValue());
                fragmentAdminDomain.setArguments(bundle);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragmentAdminDomain);
                fragmentTransaction.commit();
            });
        }
        switch (type) {
            case REPORT:
                setTitle(R.string.reports);
                break;
            case ACCOUNT:
                setTitle(R.string.accounts);
                break;
            case DOMAIN:
                setTitle(R.string.domains);
                break;
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (canGoBack && fragmentAdminAccount != null) {
            getMenuInflater().inflate(R.menu.menu_admin_account, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            if (getTitle().toString().equalsIgnoreCase(getString(R.string.accounts))) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AdminActionActivity.this, Helper.dialogStyle());
                PopupAdminFilterAccountsBinding binding = PopupAdminFilterAccountsBinding.inflate(getLayoutInflater());
                alertDialogBuilder.setView(binding.getRoot());
                if (local != null && remote == null) {
                    binding.locationLocal.setChecked(true);
                } else if (remote != null && local == null) {
                    binding.locationRemote.setChecked(true);
                } else {
                    binding.locationAll.setChecked(true);
                }
                binding.location.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.location_all) {
                        local = true;
                        remote = true;
                    } else if (checkedId == R.id.location_local) {
                        local = true;
                        remote = null;
                    } else if (checkedId == R.id.location_remote) {
                        local = null;
                        remote = true;
                    }
                });
                if (pending != null && suspended == null && active == null) {
                    binding.moderationPending.setChecked(true);
                } else if (suspended != null && pending == null && active == null) {
                    binding.moderationSuspended.setChecked(true);
                } else if (active != null && pending == null && suspended == null) {
                    binding.moderationActive.setChecked(true);
                } else {
                    binding.moderationAll.setChecked(true);
                }
                binding.moderation.setOnCheckedChangeListener((group, checkedId) -> {
                    disabled = null;
                    silenced = null;
                    if (checkedId == R.id.moderation_all) {
                        active = true;
                        suspended = true;
                        pending = true;
                    } else if (checkedId == R.id.moderation_active) {
                        active = true;
                        suspended = null;
                        pending = null;
                    } else if (checkedId == R.id.moderation_suspended) {
                        active = null;
                        suspended = true;
                        pending = null;
                    } else if (checkedId == R.id.moderation_pending) {
                        active = null;
                        suspended = null;
                        pending = true;
                    }
                });
                if (staff != null) {
                    binding.permissionsStaff.setChecked(true);
                } else {
                    binding.permissionsAll.setChecked(true);
                }
                binding.permissions.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.permissions_all) {
                        staff = null;
                    } else if (checkedId == R.id.permissions_staff) {
                        staff = true;
                    }
                });
                if (orderByMostRecent != null) {
                    binding.orderByMostRecent.setChecked(true);
                } else {
                    binding.orderByLastActive.setChecked(true);
                }
                binding.orderBy.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.order_by_most_recent) {
                        orderByMostRecent = true;
                    } else if (checkedId == R.id.order_by_last_active) {
                        orderByMostRecent = null;
                    }
                });
                alertDialogBuilder.setPositiveButton(R.string.filter, (dialog, id) -> {
                    final FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                    ft1.detach(fragmentAdminAccount);
                    ft1.commit();
                    final FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                    ft2.attach(fragmentAdminAccount);
                    ft2.commit();
                    dialog.dismiss();
                });
                alertDialogBuilder.setNegativeButton(R.string.reset, (dialog, id) -> {
                    binding.locationAll.callOnClick();
                    binding.permissionsAll.callOnClick();
                    binding.moderationAll.callOnClick();
                    binding.orderByMostRecent.callOnClick();
                });
                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            } else {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AdminActionActivity.this, Helper.dialogStyle());
                PopupAdminFilterReportsBinding binding = PopupAdminFilterReportsBinding.inflate(getLayoutInflater());
                alertDialogBuilder.setView(binding.getRoot());
                if (resolved == null) {
                    binding.statusUnresolved.setChecked(true);
                } else {
                    binding.statusResolved.setChecked(true);
                }
                binding.status.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.status_resolved) {
                        resolved = true;
                    } else if (checkedId == R.id.status_unresolved) {
                        resolved = null;
                    }
                });
                if (reportLocal != null && reportRemote == null) {
                    binding.originLocal.setChecked(true);
                } else if (reportRemote != null && reportLocal == null) {
                    binding.originRemote.setChecked(true);
                } else {
                    binding.originAll.setChecked(true);
                }
                binding.origin.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.origin_all) {
                        reportLocal = true;
                        reportRemote = true;
                    } else if (checkedId == R.id.origin_local) {
                        reportLocal = true;
                        reportRemote = null;
                    } else if (checkedId == R.id.origin_remote) {
                        reportLocal = null;
                        reportRemote = true;
                    }
                });
                alertDialogBuilder.setPositiveButton(R.string.filter, (dialog, id) -> {
                    final FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                    ft1.detach(fragmentAdminReport);
                    ft1.commit();
                    final FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                    ft2.attach(fragmentAdminReport);
                    ft2.commit();
                    dialog.dismiss();
                });
                alertDialogBuilder.setNegativeButton(R.string.reset, (dialog, id) -> {
                    binding.originAll.callOnClick();
                    binding.statusUnresolved.callOnClick();
                });
                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.buttonContainer, () -> {
                if (fragmentAdminReport != null) {
                    fragmentAdminReport.onDestroyView();
                    fragmentAdminReport = null;
                }
                if (fragmentAdminAccount != null) {
                    fragmentAdminAccount.onDestroyView();
                    fragmentAdminAccount = null;
                }
                if (fragmentAdminDomain != null) {
                    fragmentAdminDomain.onDestroyView();
                    fragmentAdminDomain = null;
                }
                setTitle(R.string.administration);
                invalidateOptionsMenu();
            });
        } else {
            super.onBackPressed();
        }

    }

    public enum AdminEnum {
        @SerializedName("REPORT")
        REPORT("REPORT"),
        @SerializedName("ACCOUNT")
        ACCOUNT("ACCOUNT"),
        @SerializedName("DOMAIN")
        DOMAIN("DOMAIN");
        private final String value;

        AdminEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


}
