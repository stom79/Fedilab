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

import static app.fedilab.android.activities.AdminActionActivity.AdminEnum.REPORT;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.annotations.SerializedName;

import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityAdminActionsBinding;
import app.fedilab.android.databinding.PopupAdminFilterAccountsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.fragment.admin.FragmentAdminAccount;
import app.fedilab.android.ui.fragment.admin.FragmentAdminReport;

public class AdminActionActivity extends BaseActivity {

    public static Boolean local = true, remote = true, active = true, pending = true, disabled = true, silenced = true, suspended = true, staff = null, orderByMostRecent = true;
    private ActivityAdminActionsBinding binding;
    private boolean canGoBack;
    private FragmentAdminReport fragmentAdminReport;
    private FragmentAdminAccount fragmentAdminAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityAdminActionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }
        canGoBack = false;
        binding.reports.setOnClickListener(v -> displayTimeline(REPORT));
        binding.accounts.setOnClickListener(v -> displayTimeline(AdminEnum.ACCOUNT));
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

        } else {

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

        }
        switch (type) {
            case REPORT:
                setTitle(R.string.reports);
                break;
            case ACCOUNT:
                setTitle(R.string.accounts);
                break;
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (canGoBack && getTitle().toString().equalsIgnoreCase(getString(R.string.accounts))) {
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.buttonContainer, () -> {
                if (fragmentAdminReport != null) {
                    fragmentAdminReport.onDestroyView();
                }
                if (fragmentAdminAccount != null) {
                    fragmentAdminAccount.onDestroyView();
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
        ACCOUNT("ACCOUNT");

        private final String value;

        AdminEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


}
