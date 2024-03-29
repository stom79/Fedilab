package app.fedilab.android.mastodon.activities;
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


import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Objects;

import app.fedilab.android.R;
import app.fedilab.android.activities.MainActivity;
import app.fedilab.android.databinding.ActivityAdminReportBinding;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminAccount;
import app.fedilab.android.mastodon.client.entities.api.admin.AdminReport;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.ThemeHelper;
import app.fedilab.android.mastodon.ui.drawer.StatusReportAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AdminVM;
import es.dmoral.toasty.Toasty;

public class AccountReportActivity extends BaseBarActivity {


    private String account_id;
    private AdminReport report;
    private ActivityAdminReportBinding binding;
    private AdminVM adminVM;
    private AdminAccount targeted_account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        report = null;
        targeted_account = null;
        Bundle args = getIntent().getExtras();
        if (args != null) {
            long bundleId = args.getLong(Helper.ARG_INTENT_ID, -1);
            new CachedBundle(AccountReportActivity.this).getBundle(bundleId, Helper.getCurrentAccount(AccountReportActivity.this), this::initializeAfterBundle);
        } else {
            initializeAfterBundle(null);
        }
    }

    private void initializeAfterBundle(Bundle bundle) {

        if (bundle != null) {
            account_id = bundle.getString(Helper.ARG_ACCOUNT_ID, null);
            targeted_account = (AdminAccount) bundle.getSerializable(Helper.ARG_ACCOUNT);
            report = (AdminReport) bundle.getSerializable(Helper.ARG_REPORT);
        }

        binding.allow.getBackground().setColorFilter(ThemeHelper.getAttColor(this, R.attr.colorPrimary), PorterDuff.Mode.MULTIPLY);
        binding.reject.getBackground().setColorFilter(ThemeHelper.getAttColor(this, R.attr.colorError), PorterDuff.Mode.MULTIPLY);


        if (account_id == null && report == null && targeted_account == null) {
            Toasty.error(AccountReportActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
        }
        binding.assign.setVisibility(View.GONE);
        binding.status.setVisibility(View.GONE);
        adminVM = new ViewModelProvider(this).get(AdminVM.class);
        if (account_id != null) {
            adminVM.getAccount(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, null));
            return;
        }

        if (report != null) {
            ArrayList<String> contents = new ArrayList<>();
            for (Status status : report.statuses) {
                contents.add(status.content);
            }
            binding.lvStatuses.setLayoutManager(new LinearLayoutManager(this));
            StatusReportAdapter adapter = new StatusReportAdapter(contents);
            binding.lvStatuses.setAdapter(adapter);
            binding.statusesGroup.setVisibility(View.VISIBLE);
            targeted_account = report.target_account;

        }
        if (targeted_account != null) {
            account_id = targeted_account.id;
            fillReport(targeted_account, null);
            account_id = targeted_account.username;
        }

    }

    private void fillReport(AdminAccount accountAdmin, actionType type) {

        if (accountAdmin == null) {
            Toasty.error(AccountReportActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        if (!accountAdmin.approved && (accountAdmin.domain == null || accountAdmin.domain.equals("null"))) {
            binding.allowRejectGroup.setVisibility(View.VISIBLE);
        }

        if (!accountAdmin.silenced) {
            binding.silence.setText(getString(R.string.silence));
        } else {
            binding.silence.setText(getString(R.string.unsilence));
        }
        if (!accountAdmin.disabled) {
            binding.disable.setText(getString(R.string.disable));
        } else {
            binding.disable.setText(getString(R.string.undisable));
        }
        if (!accountAdmin.suspended) {
            binding.suspend.setText(getString(R.string.suspend));
        } else {
            binding.suspend.setText(getString(R.string.unsuspend));
        }

        binding.reject.setOnClickListener(view -> adminVM.reject(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.REJECT)));
        binding.allow.setOnClickListener(view -> adminVM.approve(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.APPROVE)));
        binding.warn.setOnClickListener(view -> {
            adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "none", null, null, Objects.requireNonNull(binding.comment.getText()).toString().trim(), binding.emailUser.isChecked());
            fillReport(accountAdmin, actionType.NONE);
        });
        binding.silence.setOnClickListener(view -> {
            if (!accountAdmin.silenced) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "silence", null, null, Objects.requireNonNull(binding.comment.getText()).toString().trim(), binding.emailUser.isChecked());
                accountAdmin.silenced = true;
                fillReport(accountAdmin, actionType.SILENCE);
            } else {
                adminVM.unsilence(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.UNSILENCE));
            }
        });
        binding.disable.setOnClickListener(view -> {
            if (!accountAdmin.disabled) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "disable", null, null, Objects.requireNonNull(binding.comment.getText()).toString().trim(), binding.emailUser.isChecked());
                accountAdmin.disabled = true;
                fillReport(accountAdmin, actionType.DISABLE);
            } else {
                adminVM.enable(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.ENABLE));
            }
        });

        binding.suspend.setOnClickListener(view -> {
            if (!accountAdmin.suspended) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "suspend", null, null, Objects.requireNonNull(binding.comment.getText()).toString().trim(), binding.emailUser.isChecked());
                accountAdmin.suspended = true;
                fillReport(accountAdmin, actionType.SUSPEND);
            } else {
                adminVM.unsuspend(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.UNSUSPEND));
            }
        });


        if (type != null) {
            String message = null;
            switch (type) {
                case SILENCE -> message = getString(R.string.account_silenced);
                case UNSILENCE -> message = getString(R.string.account_unsilenced);
                case DISABLE -> message = getString(R.string.account_disabled);
                case ENABLE -> message = getString(R.string.account_undisabled);
                case SUSPEND -> message = getString(R.string.account_suspended);
                case UNSUSPEND -> message = getString(R.string.account_unsuspended);
                case NONE -> message = getString(R.string.account_warned);
                case APPROVE -> {
                    binding.allowRejectGroup.setVisibility(View.GONE);
                    message = getString(R.string.account_approved);
                }
                case REJECT -> {
                    binding.allowRejectGroup.setVisibility(View.GONE);
                    message = getString(R.string.account_rejected);
                }
            }
            if (message != null) {
                Toasty.success(AccountReportActivity.this, message, Toast.LENGTH_LONG).show();
            }
            binding.comment.setText("");
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(binding.comment.getWindowToken(), 0);
        }

        if (accountAdmin.account != null) {
            binding.username.setText(String.format("@%s", accountAdmin.account.acct));
        }

        binding.email.setText(accountAdmin.email);

        if (accountAdmin.email == null || accountAdmin.email.trim().equals("")) {
            binding.email.setVisibility(View.GONE);
            binding.emailLabel.setVisibility(View.GONE);
        }
        if (accountAdmin.ip == null || accountAdmin.ip.trim().equals("")) {
            binding.recentIp.setVisibility(View.GONE);
            binding.recentIpLabel.setVisibility(View.GONE);
        }
        if (accountAdmin.created_at == null) {
            binding.joined.setVisibility(View.GONE);
            binding.joinedLabel.setVisibility(View.GONE);
        }
        if (accountAdmin.disabled) {
            binding.loginStatus.setText(getString(R.string.disabled));
        } else if (accountAdmin.silenced) {
            binding.loginStatus.setText(getString(R.string.silenced));
        } else if (accountAdmin.suspended) {
            binding.loginStatus.setText(getString(R.string.suspended));
        } else {
            binding.loginStatus.setText(getString(R.string.active));
        }
        if (accountAdmin.domain == null || accountAdmin.domain.trim().equalsIgnoreCase("null")) {
            binding.warn.setVisibility(View.VISIBLE);
            binding.emailUser.setVisibility(View.VISIBLE);
            binding.commentLabel.setVisibility(View.VISIBLE);
            binding.comment.setVisibility(View.VISIBLE);
            binding.recentIp.setText(accountAdmin.ip != null ? accountAdmin.ip : "");
            binding.disable.setVisibility(View.VISIBLE);
            binding.suspend.setVisibility(View.VISIBLE);
        } else {
            binding.warn.setVisibility(View.GONE);
            binding.emailUser.setVisibility(View.GONE);
            binding.emailUser.setChecked(false);
            binding.comment.setVisibility(View.GONE);
            binding.recentIp.setText("-");
            binding.permissions.setText("-");
            binding.email.setText("-");
            binding.disable.setVisibility(View.GONE);
            binding.suspend.setVisibility(View.VISIBLE);
            binding.commentLabel.setVisibility(View.GONE);
        }

        if (accountAdmin.role != null) {
            binding.permissions.setText(AdminAccount.permissions.get(accountAdmin.role.permissions));
            binding.permissions.setText(getString(R.string.user));
            if (accountAdmin.role.permissions == 1 || accountAdmin.role.permissions == 400) {
                binding.warn.setVisibility(View.GONE);
                binding.suspend.setVisibility(View.GONE);
                binding.silence.setVisibility(View.GONE);
                binding.disable.setVisibility(View.GONE);
                binding.emailUser.setVisibility(View.GONE);
                binding.emailUser.setChecked(false);
                binding.comment.setVisibility(View.GONE);
                binding.commentLabel.setVisibility(View.GONE);
            }
            binding.emailStatus.setText(accountAdmin.confirmed ? getString(R.string.confirmed) : getString(R.string.unconfirmed));
        }


        binding.joined.setText(Helper.dateToString(accountAdmin.created_at));
        if (report != null) {
            binding.assign.setVisibility(View.VISIBLE);
            binding.status.setVisibility(View.VISIBLE);
            if (report.assigned_account == null) {
                binding.assign.setText(getString(R.string.assign_to_me));
            } else {
                binding.assign.setText(getString(R.string.unassign));
            }
            binding.assign.setOnClickListener(view -> {
                if (report.assigned_account == null) {
                    adminVM.assignToSelf(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {
                        report = adminReport;
                        fillReport(accountAdmin, null);
                    });
                } else {
                    adminVM.unassign(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {
                        report = adminReport;
                        fillReport(accountAdmin, null);
                    });
                }
            });
            if (report.action_taken) {
                binding.status.setText(getString(R.string.mark_unresolved));
            } else {
                binding.status.setText(getString(R.string.mark_resolved));
            }
            binding.status.setOnClickListener(view -> {
                if (report.action_taken) {
                    adminVM.reopen(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {
                        report = adminReport;
                        fillReport(accountAdmin, null);
                    });
                } else {
                    adminVM.resolved(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {
                        report = adminReport;
                        fillReport(accountAdmin, null);
                    });
                }
            });

        } else {
            binding.assign.setVisibility(View.GONE);
            binding.status.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public enum actionType {
        ENABLE,
        APPROVE,
        REJECT,
        NONE,
        SILENCE,
        DISABLE,
        UNSILENCE,
        SUSPEND,
        UNSUSPEND,
        ASSIGN_TO_SELF,
        UNASSIGN,
        REOPEN,
        RESOLVE,
        GET_ACCOUNTS,
        GET_ONE_ACCOUNT,
        GET_REPORTS,
        GET_ONE_REPORT
    }
}
