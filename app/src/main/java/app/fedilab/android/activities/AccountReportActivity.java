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


import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.AdminAccount;
import app.fedilab.android.client.entities.api.AdminReport;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.databinding.ActivityAdminReportBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.StatusReportAdapter;
import app.fedilab.android.viewmodel.mastodon.AdminVM;
import es.dmoral.toasty.Toasty;

public class AccountReportActivity extends BaseActivity {


    private String account_id;
    private AdminReport report;
    private ActivityAdminReportBinding binding;
    private AdminVM adminVM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyThemeBar(this);
        binding = ActivityAdminReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.cyanea_primary)));
        }

        report = null;
        AdminAccount targeted_account = null;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            account_id = b.getString(Helper.ARG_ACCOUNT_ID, null);
            targeted_account = (AdminAccount) b.getSerializable(Helper.ARG_ACCOUNT);
            report = (AdminReport) b.getSerializable(Helper.ARG_REPORT);
        }


        binding.allow.getBackground().setColorFilter(ContextCompat.getColor(AccountReportActivity.this, R.color.green_1), PorterDuff.Mode.MULTIPLY);
        binding.reject.getBackground().setColorFilter(ContextCompat.getColor(AccountReportActivity.this, R.color.red_1), PorterDuff.Mode.MULTIPLY);


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
            adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "none", null, null, binding.comment.getText().toString().trim(), binding.emailUser.isChecked());
            fillReport(accountAdmin, actionType.NONE);
        });
        binding.silence.setOnClickListener(view -> {
            if (!accountAdmin.silenced) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "silence", null, null, binding.comment.getText().toString().trim(), binding.emailUser.isChecked());
                accountAdmin.silenced = true;
                fillReport(accountAdmin, actionType.SILENCE);
            } else {
                adminVM.unsilence(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.UNSILENCE));
            }
        });
        binding.disable.setOnClickListener(view -> {
            if (!accountAdmin.disabled) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "disable", null, null, binding.comment.getText().toString().trim(), binding.emailUser.isChecked());
                accountAdmin.disabled = true;
                fillReport(accountAdmin, actionType.DISABLE);
            } else {
                adminVM.enable(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.ENABLE));
            }
        });

        binding.suspend.setOnClickListener(view -> {
            if (!accountAdmin.suspended) {
                adminVM.performAction(MainActivity.currentInstance, MainActivity.currentToken, account_id, "suspend", null, null, binding.comment.getText().toString().trim(), binding.emailUser.isChecked());
                accountAdmin.suspended = true;
                fillReport(accountAdmin, actionType.SUSPEND);
            } else {
                adminVM.unsuspend(MainActivity.currentInstance, MainActivity.currentToken, account_id).observe(this, account -> fillReport(account, actionType.UNSUSPEND));
            }
        });


        if (type != null) {
            String message = null;
            switch (type) {
                case SILENCE:
                    message = getString(R.string.account_silenced);
                    break;
                case UNSILENCE:
                    message = getString(R.string.account_unsilenced);
                    break;
                case DISABLE:
                    message = getString(R.string.account_disabled);
                    break;
                case ENABLE:
                    message = getString(R.string.account_undisabled);
                    break;
                case SUSPEND:
                    message = getString(R.string.account_suspended);
                    break;
                case UNSUSPEND:
                    message = getString(R.string.account_unsuspended);
                    break;
                case NONE:
                    message = getString(R.string.account_warned);
                    break;
                case APPROVE:
                    binding.allowRejectGroup.setVisibility(View.GONE);
                    message = getString(R.string.account_approved);
                    break;
                case REJECT:
                    binding.allowRejectGroup.setVisibility(View.GONE);
                    message = getString(R.string.account_rejected);
                    break;
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
        if (accountAdmin.ip == null || accountAdmin.ip.ip.trim().equals("")) {
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
        if (accountAdmin.domain == null || accountAdmin.domain.equals("null")) {
            binding.warn.setVisibility(View.VISIBLE);
            binding.emailUser.setVisibility(View.VISIBLE);
            binding.commentLabel.setVisibility(View.VISIBLE);
            binding.comment.setVisibility(View.VISIBLE);
            binding.recentIp.setText(accountAdmin.ip != null ? accountAdmin.ip.ip : "");
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
            switch (accountAdmin.role) {
                case "user":
                    binding.permissions.setText(getString(R.string.user));
                    break;
                case "moderator":
                    binding.permissions.setText(getString(R.string.moderator));
                    break;
                case "admin":
                    binding.permissions.setText(getString(R.string.administrator));
                    break;
            }
            if (accountAdmin.role.equals("admin") || accountAdmin.role.equals("moderator")) {
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

                    });
                } else {
                    adminVM.unassign(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {

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

                    });
                } else {
                    adminVM.resolved(MainActivity.currentInstance, MainActivity.currentToken, report.id).observe(this, adminReport -> {

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
