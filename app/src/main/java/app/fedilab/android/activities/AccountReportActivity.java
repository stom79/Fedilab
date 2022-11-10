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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.fedilab.android.R;
import app.fedilab.android.helper.Helper;
import es.dmoral.toasty.Toasty;

public class AccountReportActivity extends BaseActivity implements OnAdminActionInterface {

    TextView permissions, username, email, email_status, login_status, joined, recent_ip, comment_label;
    Button warn, disable, silence, suspend, allow, reject, assign, status;
    private String account_id;
    private CheckBox email_user;
    private EditText comment;
    private Report report;
    private Group allow_reject_group;
    private TextView email_label, recent_ip_label, joined_label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppAdminTheme);
        report = null;
        AccountAdmin targeted_account = null;
        Bundle b = getIntent().getExtras();

        if (b != null) {
            account_id = b.getString("account_id", null);
            targeted_account = b.getParcelable("targeted_account");
            report = b.getParcelable("report");
        }

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            View view = inflater.inflate(R.layout.simple_bar, new LinearLayout(AccountReportActivity.this), false);
            view.setBackground(new ColorDrawable(ContextCompat.getColor(AccountReportActivity.this, R.color.cyanea_primary)));
            actionBar.setCustomView(view, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ImageView toolbar_close = actionBar.getCustomView().findViewById(R.id.toolbar_close);
            TextView toolbar_title = actionBar.getCustomView().findViewById(R.id.toolbar_title);
            toolbar_close.setOnClickListener(v -> finish());
            toolbar_title.setText(String.format(getString(R.string.administration) + " %s", Helper.getLiveInstance(AccountReportActivity.this)));
        }
        setContentView(R.layout.activity_admin_report);

        warn = findViewById(R.id.warn);
        disable = findViewById(R.id.disable);
        silence = findViewById(R.id.silence);
        suspend = findViewById(R.id.suspend);
        allow = findViewById(R.id.allow);
        reject = findViewById(R.id.reject);
        status = findViewById(R.id.status);
        assign = findViewById(R.id.assign);
        email_label = findViewById(R.id.email_label);
        allow_reject_group = findViewById(R.id.allow_reject_group);
        allow_reject_group.setVisibility(View.GONE);
        allow.getBackground().setColorFilter(ContextCompat.getColor(AccountReportActivity.this, R.color.green_1), PorterDuff.Mode.MULTIPLY);
        reject.getBackground().setColorFilter(ContextCompat.getColor(AccountReportActivity.this, R.color.red_1), PorterDuff.Mode.MULTIPLY);
        comment_label = findViewById(R.id.comment_label);
        permissions = findViewById(R.id.permissions);
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        email_status = findViewById(R.id.email_status);
        login_status = findViewById(R.id.login_status);
        joined = findViewById(R.id.joined);
        recent_ip = findViewById(R.id.recent_ip);
        recent_ip_label = findViewById(R.id.recent_ip_label);
        joined_label = findViewById(R.id.joined_label);
        email_user = findViewById(R.id.email_user);
        comment = findViewById(R.id.comment);

        if (account_id == null && report == null && targeted_account == null) {
            Toasty.error(AccountReportActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
        }
        assign.setVisibility(View.GONE);
        status.setVisibility(View.GONE);
        if (account_id != null) {
            new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.GET_ONE_ACCOUNT, account_id, null, AccountReportActivity.this);
            return;
        }
        if (report != null) {
            targeted_account = report.getTarget_account();
            RecyclerView lv_statuses = findViewById(R.id.lv_statuses);

            ArrayList<String> contents = new ArrayList<>();
            for (Status status : report.getStatuses()) {
                contents.add(status.getContent());
            }
            lv_statuses.setLayoutManager(new LinearLayoutManager(this));
            StatusReportAdapter adapter = new StatusReportAdapter(contents);
            lv_statuses.setAdapter(adapter);

            Group statuses_group = findViewById(R.id.statuses_group);
            statuses_group.setVisibility(View.VISIBLE);
            if (MainActivity.social == UpdateAccountInfoAsyncTask.SOCIAL.PLEROMA) {
                new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.GET_ONE_ACCOUNT, report.getTarget_account().getUsername(), null, AccountReportActivity.this);
            }

        }
        if (targeted_account != null) {
            account_id = targeted_account.getId();
            fillReport(targeted_account);
            if (MainActivity.social == UpdateAccountInfoAsyncTask.SOCIAL.PLEROMA) {
                account_id = targeted_account.getUsername();
            }
        }


    }

    @Override
    public void onAdminAction(APIResponse apiResponse) {
        if (apiResponse.getError() != null) {
            if (apiResponse.getError().getStatusCode() == 403) {
                SharedPreferences sharedpreferences = getSharedPreferences(Helper.APP_PREFS, android.content.Context.MODE_PRIVATE);
                int theme = sharedpreferences.getInt(Helper.SET_THEME, Helper.THEME_DARK);
                int style;
                if (theme == Helper.THEME_DARK) {
                    style = R.style.DialogDark;
                } else if (theme == Helper.THEME_BLACK) {
                    style = R.style.DialogBlack;
                } else {
                    style = R.style.Dialog;
                }
                AlertDialog.Builder builderInner;
                builderInner = new AlertDialog.Builder(AccountReportActivity.this, style);
                builderInner.setTitle(R.string.reconnect_account);
                builderInner.setMessage(R.string.reconnect_account_message);
                builderInner.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builderInner.setPositiveButton(R.string.validate, (dialog, which) -> {
                    Intent intent = new Intent(AccountReportActivity.this, LoginActivity.class);
                    intent.putExtra("admin", true);
                    startActivity(intent);
                });
                builderInner.show();
            } else {
                Toasty.error(AccountReportActivity.this, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (apiResponse.getReports() != null && apiResponse.getReports().size() > 0) {
            report = apiResponse.getReports().get(0);
            fillReport(report.getTarget_account());
        } else if (apiResponse.getAccountAdmins() != null && apiResponse.getAccountAdmins().size() > 0) {
            if (MainActivity.social == UpdateAccountInfoAsyncTask.SOCIAL.PLEROMA) {
                account_id = apiResponse.getAccountAdmins().get(0).getUsername();
            }
            fillReport(apiResponse.getAccountAdmins().get(0));
        }

    }

    private void fillReport(AccountAdmin accountAdmin) {

        if (accountAdmin == null) {
            Toasty.error(AccountReportActivity.this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            return;
        }
        if (!accountAdmin.isApproved() && MainActivity.social != UpdateAccountInfoAsyncTask.SOCIAL.PLEROMA && (accountAdmin.getDomain() == null || accountAdmin.getDomain().equals("null"))) {
            allow_reject_group.setVisibility(View.VISIBLE);
        }

        reject.setOnClickListener(view -> {
            AdminAction adminAction = new AdminAction();
            adminAction.setType(REJECT);
            new PostAdminActionAsyncTask(AccountReportActivity.this, REJECT, account_id, adminAction, AccountReportActivity.this);
        });

        allow.setOnClickListener(view -> {
            AdminAction adminAction = new AdminAction();
            adminAction.setType(APPROVE);
            new PostAdminActionAsyncTask(AccountReportActivity.this, APPROVE, account_id, adminAction, AccountReportActivity.this);
        });

        warn.setOnClickListener(view -> {
            AdminAction adminAction = new AdminAction();
            adminAction.setType(NONE);
            adminAction.setSend_email_notification(email_user.isChecked());
            adminAction.setText(comment.getText().toString().trim());
            new PostAdminActionAsyncTask(AccountReportActivity.this, NONE, account_id, adminAction, AccountReportActivity.this);
        });


        if (!accountAdmin.isSilenced()) {
            silence.setText(getString(R.string.silence));
        } else {
            silence.setText(getString(R.string.unsilence));
        }
        silence.setOnClickListener(view -> {
            if (!accountAdmin.isSilenced()) {
                AdminAction adminAction = new AdminAction();
                adminAction.setType(SILENCE);
                adminAction.setSend_email_notification(email_user.isChecked());
                adminAction.setText(comment.getText().toString().trim());
                new PostAdminActionAsyncTask(AccountReportActivity.this, SILENCE, account_id, adminAction, AccountReportActivity.this);
            } else {
                new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.UNSILENCE, account_id, null, AccountReportActivity.this);
            }
        });

        if (!accountAdmin.isDisabled()) {
            disable.setText(getString(R.string.disable));
        } else {
            disable.setText(getString(R.string.undisable));
        }
        disable.setOnClickListener(view -> {
            if (!accountAdmin.isDisabled()) {
                AdminAction adminAction = new AdminAction();
                adminAction.setType(DISABLE);
                adminAction.setSend_email_notification(email_user.isChecked());
                adminAction.setText(comment.getText().toString().trim());
                new PostAdminActionAsyncTask(AccountReportActivity.this, DISABLE, account_id, adminAction, AccountReportActivity.this);
            } else {
                new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.ENABLE, account_id, null, AccountReportActivity.this);
            }
        });
        if (!accountAdmin.isSuspended()) {
            suspend.setText(getString(R.string.suspend));
        } else {
            suspend.setText(getString(R.string.unsuspend));
        }
        suspend.setOnClickListener(view -> {
            if (!accountAdmin.isSuspended()) {
                AdminAction adminAction = new AdminAction();
                adminAction.setType(SUSPEND);
                adminAction.setSend_email_notification(email_user.isChecked());
                adminAction.setText(comment.getText().toString().trim());
                new PostAdminActionAsyncTask(AccountReportActivity.this, SUSPEND, account_id, adminAction, AccountReportActivity.this);
            } else {
                new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.UNSUSPEND, account_id, null, AccountReportActivity.this);
            }
        });


        if (accountAdmin.getAction() != null) {
            String message = null;
            switch (accountAdmin.getAction()) {
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
                    allow_reject_group.setVisibility(View.GONE);
                    message = getString(R.string.account_approved);
                    break;
                case REJECT:
                    allow_reject_group.setVisibility(View.GONE);
                    message = getString(R.string.account_rejected);
                    break;
            }
            if (message != null) {
                Toasty.success(AccountReportActivity.this, message, Toast.LENGTH_LONG).show();
            }
            comment.setText("");
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(comment.getWindowToken(), 0);
        }

        if (accountAdmin.getAccount() != null) {
            username.setText(String.format("@%s", accountAdmin.getAccount().getAcct()));
        }

        email.setText(accountAdmin.getEmail());

        if (accountAdmin.getEmail() == null || accountAdmin.getEmail().trim().equals("")) {
            email.setVisibility(View.GONE);
            email_label.setVisibility(View.GONE);
        }
        if (accountAdmin.getIp() == null || accountAdmin.getIp().trim().equals("")) {
            recent_ip.setVisibility(View.GONE);
            recent_ip_label.setVisibility(View.GONE);
        }
        if (accountAdmin.getCreated_at() == null) {
            joined.setVisibility(View.GONE);
            joined_label.setVisibility(View.GONE);
        }
        if (accountAdmin.isDisabled()) {
            login_status.setText(getString(R.string.disabled));
        } else if (accountAdmin.isSilenced()) {
            login_status.setText(getString(R.string.silenced));
        } else if (accountAdmin.isSuspended()) {
            login_status.setText(getString(R.string.suspended));
        } else {
            login_status.setText(getString(R.string.active));
        }
        if (accountAdmin.getDomain() == null || accountAdmin.getDomain().equals("null")) {
            warn.setVisibility(View.VISIBLE);
            email_user.setVisibility(View.VISIBLE);
            comment_label.setVisibility(View.VISIBLE);
            comment.setVisibility(View.VISIBLE);
            recent_ip.setText(accountAdmin.getIp());
            disable.setVisibility(View.VISIBLE);
            suspend.setVisibility(View.VISIBLE);
        } else {
            warn.setVisibility(View.GONE);
            email_user.setVisibility(View.GONE);
            email_user.setChecked(false);
            comment.setVisibility(View.GONE);
            recent_ip.setText("-");
            permissions.setText("-");
            email.setText("-");
            disable.setVisibility(View.GONE);
            suspend.setVisibility(View.VISIBLE);
            comment_label.setVisibility(View.GONE);
        }

        if (accountAdmin.getRole() != null) {
            switch (accountAdmin.getRole()) {
                case "user":
                    permissions.setText(getString(R.string.user));
                    break;
                case "moderator":
                    permissions.setText(getString(R.string.moderator));
                    break;
                case "admin":
                    permissions.setText(getString(R.string.administrator));
                    break;
            }
            if (accountAdmin.getRole().equals("admin") || accountAdmin.getRole().equals("moderator")) {
                warn.setVisibility(View.GONE);
                suspend.setVisibility(View.GONE);
                silence.setVisibility(View.GONE);
                disable.setVisibility(View.GONE);
                email_user.setVisibility(View.GONE);
                email_user.setChecked(false);
                comment.setVisibility(View.GONE);
                comment_label.setVisibility(View.GONE);
            }
            email_status.setText(accountAdmin.isConfirmed() ? getString(R.string.confirmed) : getString(R.string.unconfirmed));
        }


        joined.setText(Helper.dateToString(accountAdmin.getCreated_at()));
        if (report != null) {
            assign.setVisibility(View.VISIBLE);
            status.setVisibility(View.VISIBLE);
            if (report.getAssigned_account() == null) {
                assign.setText(getString(R.string.assign_to_me));
            } else {
                assign.setText(getString(R.string.unassign));
            }
            assign.setOnClickListener(view -> {
                if (report.getAssigned_account() == null) {
                    new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.ASSIGN_TO_SELF, report.getId(), null, AccountReportActivity.this);
                } else {
                    new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.UNASSIGN, report.getId(), null, AccountReportActivity.this);
                }
            });
            if (report.isAction_taken()) {
                status.setText(getString(R.string.mark_unresolved));
            } else {
                status.setText(getString(R.string.mark_resolved));
            }
            status.setOnClickListener(view -> {
                if (report.isAction_taken()) {
                    new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.REOPEN, report.getId(), null, AccountReportActivity.this);
                } else {
                    new PostAdminActionAsyncTask(AccountReportActivity.this, API.adminAction.RESOLVE, report.getId(), null, AccountReportActivity.this);
                }
            });

        } else {
            assign.setVisibility(View.GONE);
            status.setVisibility(View.GONE);
        }
        if (MainActivity.social == UpdateAccountInfoAsyncTask.SOCIAL.PLEROMA) {
            email_user.setVisibility(View.GONE);
            email_user.setChecked(false);
            comment.setVisibility(View.INVISIBLE);
            comment_label.setVisibility(View.GONE);
            warn.setVisibility(View.GONE);
            silence.setVisibility(View.GONE);
            assign.setVisibility(View.GONE);
        }

    }
}
