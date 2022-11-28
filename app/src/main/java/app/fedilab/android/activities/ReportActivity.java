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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Account;
import app.fedilab.android.client.entities.api.RelationShip;
import app.fedilab.android.client.entities.api.Status;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityReportBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.ui.drawer.RulesAdapter;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;
import es.dmoral.toasty.Toasty;


public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;
    private Status status;
    private Account account;
    private AccountsVM accountsVM;
    private RelationShip relationShip;
    private List<String> statusIds;
    private List<String> ruleIds;
    private String comment;
    private boolean forward;
    private FragmentMastodonTimeline fragment;
    private RulesAdapter rulesAdapter;
    private String category;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle b = getIntent().getExtras();
        if (b != null) {
            status = (Status) b.getSerializable(Helper.ARG_STATUS);
            account = (Account) b.getSerializable(Helper.ARG_ACCOUNT);
        }
        if (account == null && status != null) {
            account = status.account;
        }
        //The entry view
        show(binding.screenReason);

        setTitle(getString(R.string.report_title, "@" + account.acct));

        binding.val1.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val1);
        });
        binding.val2.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val2);
        });
        binding.val3.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val3);
        });
        binding.val4.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val4);
        });

        binding.val1Container.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val1);
        });
        binding.val2Container.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val2);
        });
        binding.val3Container.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val3);
        });
        binding.val4Container.setOnClickListener(v -> {
            binding.actionButton.setEnabled(true);
            setChecked(binding.val4);
        });


        accountsVM = new ViewModelProvider(ReportActivity.this).get(AccountsVM.class);
        binding.actionButton.setOnClickListener(v -> {
            if (binding.screenReason.getVisibility() == View.VISIBLE) {
                if (binding.val1.isChecked()) {
                    show(binding.screenIdontlike);
                    switchToIDontLike();
                } else if (binding.val2.isChecked()) {
                    show(binding.screenSpam);
                    switchToSpam();
                } else if (binding.val3.isChecked()) {
                    show(binding.screenViolateRules);
                    switchToRules();
                } else if (binding.val4.isChecked()) {
                    show(binding.screenSomethingElse);
                    switchToSomethingElse();
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void switchToRules() {
        rulesAdapter = new RulesAdapter(BaseMainActivity.instanceInfo.rules);
        binding.lvVr.setAdapter(rulesAdapter);
        binding.lvVr.setLayoutManager(new LinearLayoutManager(ReportActivity.this));
        binding.actionButton.setText(R.string.next);
        binding.actionButton.setOnClickListener(v -> {
            category = "violation";
            show(binding.screenSomethingElse);
            switchToSomethingElse();
        });
    }

    private void switchToIDontLike() {
        List<String> ids = new ArrayList<>();
        ids.add(account.id);
        binding.unfollowTitle.setText(getString(R.string.report_1_unfollow_title, "@" + account.acct));
        binding.muteTitle.setText(getString(R.string.report_1_mute_title, "@" + account.acct));
        binding.blockTitle.setText(getString(R.string.report_1_block_title, "@" + account.acct));
        accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, ids)
                .observe(ReportActivity.this, relationShips -> {
                    if (relationShips != null && relationShips.size() > 0) {
                        relationShip = relationShips.get(0);
                        if (relationShip.following) {
                            binding.groupUnfollow.setVisibility(View.VISIBLE);
                        } else {
                            binding.groupUnfollow.setVisibility(View.GONE);
                        }
                        if (relationShip.blocking) {
                            binding.groupBlock.setVisibility(View.GONE);
                        } else {
                            binding.groupBlock.setVisibility(View.VISIBLE);
                        }
                        if (relationShip.muting) {
                            binding.groupMute.setVisibility(View.GONE);
                        } else {
                            binding.groupMute.setVisibility(View.VISIBLE);
                        }
                    }
                    binding.actionUnfollow.setOnClickListener(v -> accountsVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                            .observe(ReportActivity.this, rsUnfollow -> {
                                if (rsUnfollow != null) {
                                    relationShip = rsUnfollow;
                                    Toasty.info(ReportActivity.this, getString(R.string.toast_unfollow), Toasty.LENGTH_LONG).show();
                                    binding.groupUnfollow.setVisibility(View.GONE);
                                }
                            }));
                    binding.actionMute.setOnClickListener(v -> accountsVM.mute(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, null, null)
                            .observe(ReportActivity.this, rsMute -> {
                                if (rsMute != null) {
                                    relationShip = rsMute;
                                    Toasty.info(ReportActivity.this, getString(R.string.toast_mute), Toasty.LENGTH_LONG).show();
                                    binding.groupMute.setVisibility(View.GONE);
                                }
                            }));
                    binding.actionBlock.setOnClickListener(v -> accountsVM.block(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id)
                            .observe(ReportActivity.this, rsBlock -> {
                                if (rsBlock != null) {
                                    relationShip = rsBlock;
                                    Toasty.info(ReportActivity.this, getString(R.string.toast_block), Toasty.LENGTH_LONG).show();
                                    binding.groupBlock.setVisibility(View.GONE);
                                }
                            }));

                });
        binding.actionButton.setText(R.string.done);
        binding.actionButton.setOnClickListener(v -> finish());
    }


    private void switchToSpam() {

        fragment = new FragmentMastodonTimeline();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.ACCOUNT_TIMELINE);
        bundle.putSerializable(Helper.ARG_ACCOUNT, account);
        //Set to display statuses with less options
        bundle.putBoolean(Helper.ARG_MINIFIED, true);
        if (status != null) {
            status.isChecked = true;
            bundle.putSerializable(Helper.ARG_STATUS_REPORT, status);
        }
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fram_spam_container, fragment);
        fragmentTransaction.commit();

        binding.actionButton.setText(R.string.next);
        binding.actionButton.setOnClickListener(v -> {
            category = "spam";
            switchToMoreInfo();
        });
    }

    private void switchToSomethingElse() {

        fragment = new FragmentMastodonTimeline();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.ACCOUNT_TIMELINE);
        bundle.putSerializable(Helper.ARG_ACCOUNT, account);
        //Set to display statuses with less options
        bundle.putBoolean(Helper.ARG_MINIFIED, true);
        if (status != null) {
            status.isChecked = true;
            bundle.putSerializable(Helper.ARG_STATUS_REPORT, status);
        }
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fram_se_container, fragment);
        fragmentTransaction.commit();

        binding.actionButton.setText(R.string.next);
        binding.actionButton.setOnClickListener(v -> {
            if (category == null) {
                category = "other";
            }
            switchToMoreInfo();
        });
    }

    private void switchToMoreInfo() {

        show(binding.screenMoreDetails);
        String[] domains = account.acct.split("@");
        if (domains.length > 1) {
            binding.forward.setOnCheckedChangeListener((compoundButton, checked) -> forward = checked);
            binding.forward.setText(getString(R.string.report_more_forward, domains[1]));
        } else {
            forward = false;
            binding.forwardBlock.setVisibility(View.GONE);
        }
        binding.actionButton.setText(R.string.done);
        binding.actionButton.setOnClickListener(v -> {
            if (rulesAdapter != null) {
                ruleIds = rulesAdapter.getChecked();
            }
            if (fragment != null) {
                statusIds = fragment.getCheckedStatusesId();
            }
            comment = binding.reportMessage.getText().toString();
            binding.actionButton.setEnabled(false);
            accountsVM.report(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, account.id, category, statusIds, ruleIds, comment, forward)
                    .observe(ReportActivity.this, report -> {
                        Toasty.success(ReportActivity.this, R.string.report_sent, Toasty.LENGTH_LONG).show();
                        finish();
                    });
        });
    }


    /**
     * Hide all views and display the wanted one
     *
     * @param linearLayoutCompat LinearLayoutCompat - One of the report page
     */
    private void show(LinearLayoutCompat linearLayoutCompat) {
        binding.screenReason.setVisibility(View.GONE);
        binding.screenIdontlike.setVisibility(View.GONE);
        binding.screenSpam.setVisibility(View.GONE);
        binding.screenViolateRules.setVisibility(View.GONE);
        binding.screenSomethingElse.setVisibility(View.GONE);
        binding.screenMoreDetails.setVisibility(View.GONE);
        linearLayoutCompat.setVisibility(View.VISIBLE);
    }

    private void setChecked(RadioButton radioButton) {
        binding.val1.setChecked(false);
        binding.val2.setChecked(false);
        binding.val3.setChecked(false);
        binding.val4.setChecked(false);
        radioButton.setChecked(true);
    }
}
