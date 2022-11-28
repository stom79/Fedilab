package app.fedilab.android.ui.fragment.timeline;
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

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentNotificationContainerBinding;
import app.fedilab.android.databinding.PopupNotificationSettingsBinding;
import app.fedilab.android.ui.pageadapter.FedilabNotificationPageAdapter;
import app.fedilab.android.viewmodel.mastodon.NotificationsVM;
import es.dmoral.toasty.Toasty;


public class FragmentNotificationContainer extends Fragment {

    public static UpdateCounters update;
    private FragmentNotificationContainerBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationContainerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean display_all_notification = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_ALL_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, false);
        if (!display_all_notification) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.all)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.mention)));
            binding.tabLayout.setTabMode(TabLayout.MODE_FIXED);
            binding.viewpagerNotificationContainer.setAdapter(new FedilabNotificationPageAdapter(getChildFragmentManager(), false));
        } else {
            binding.tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.all)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_reply_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_star_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_repeat));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_poll_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_home_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_person_add_alt_1_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_edit_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_person_add_alt_1_24));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_baseline_report_24));
            binding.viewpagerNotificationContainer.setAdapter(new FedilabNotificationPageAdapter(getChildFragmentManager(), true));
        }
        AtomicBoolean changes = new AtomicBoolean(false);
        binding.settings.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity());
            PopupNotificationSettingsBinding dialogView = PopupNotificationSettingsBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(dialogView.getRoot());


            dialogView.clearAllNotif.setOnClickListener(v1 -> {
                AlertDialog.Builder db = new AlertDialog.Builder(requireActivity());
                db.setTitle(R.string.delete_notification_ask_all);
                db.setMessage(R.string.delete_notification_all_warning);
                db.setPositiveButton(R.string.delete_all, (dialog, id) -> {
                    changes.set(true);
                    NotificationsVM notificationsVM = new ViewModelProvider(FragmentNotificationContainer.this).get(NotificationsVM.class);
                    notificationsVM.clearNotification(BaseMainActivity.currentUserID, BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                            .observe(getViewLifecycleOwner(), unused -> {
                                Toasty.info(requireActivity(), R.string.delete_notification_all, Toasty.LENGTH_LONG).show();
                            });
                    dialog.dismiss();
                });
                db.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
                AlertDialog alertDialog = db.create();
                alertDialog.show();
            });

            boolean displayAllCategory = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_ALL_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, false);
            dialogView.displayAllCategories.setChecked(displayAllCategory);
            dialogView.displayAllCategories.setOnCheckedChangeListener((compoundButton, checked) -> {
                changes.set(true);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(getString(R.string.SET_DISPLAY_ALL_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, checked);
                editor.commit();
            });
            dialogView.displayMentions.setChecked(true);
            dialogView.displayFavourites.setChecked(true);
            dialogView.displayReblogs.setChecked(true);
            dialogView.displayPollResults.setChecked(true);
            dialogView.displayUpdatesFromPeople.setChecked(true);
            dialogView.displayFollows.setChecked(true);
            dialogView.displayUpdates.setChecked(true);
            dialogView.displaySignups.setChecked(true);
            dialogView.displayReports.setChecked(true);
            String excludedCategories = sharedpreferences.getString(getString(R.string.SET_EXCLUDED_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
            List<String> excludedCategoriesList = new ArrayList<>();
            if (excludedCategories != null) {
                String[] categoriesArray = excludedCategories.split("\\|");
                for (String category : categoriesArray) {
                    switch (category) {
                        case "mention":
                            excludedCategoriesList.add("mention");
                            dialogView.displayMentions.setChecked(false);
                            break;
                        case "favourite":
                            excludedCategoriesList.add("favourite");
                            dialogView.displayFavourites.setChecked(false);
                            break;
                        case "reblog":
                            excludedCategoriesList.add("reblog");
                            dialogView.displayReblogs.setChecked(false);
                            break;
                        case "poll":
                            excludedCategoriesList.add("poll");
                            dialogView.displayPollResults.setChecked(false);
                            break;
                        case "status":
                            excludedCategoriesList.add("status");
                            dialogView.displayUpdatesFromPeople.setChecked(false);
                            break;
                        case "follow":
                            excludedCategoriesList.add("follow");
                            dialogView.displayFollows.setChecked(false);
                            break;
                        case "update":
                            excludedCategoriesList.add("update");
                            dialogView.displayUpdates.setChecked(false);
                            break;
                        case "admin.sign_up":
                            excludedCategoriesList.add("admin.sign_up");
                            dialogView.displaySignups.setChecked(false);
                            break;
                        case "admin.report":
                            excludedCategoriesList.add("admin.report");
                            dialogView.displayReports.setChecked(false);
                            break;
                    }
                }
            }
            dialogView.displayTypesGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                changes.set(true);
                String notificationType = "";
                if (checkedId == R.id.display_mentions) {
                    notificationType = "mention";
                } else if (checkedId == R.id.display_favourites) {
                    notificationType = "favourite";
                } else if (checkedId == R.id.display_reblogs) {
                    notificationType = "reblog";
                } else if (checkedId == R.id.display_poll_results) {
                    notificationType = "poll";
                } else if (checkedId == R.id.display_updates_from_people) {
                    notificationType = "status";
                } else if (checkedId == R.id.display_follows) {
                    notificationType = "follow";
                } else if (checkedId == R.id.display_updates) {
                    notificationType = "update";
                } else if (checkedId == R.id.display_signups) {
                    notificationType = "admin.sign_up";
                } else if (checkedId == R.id.display_reports) {
                    notificationType = "admin.report";
                }
                if (isChecked) {
                    excludedCategoriesList.remove(notificationType);
                } else {
                    if (!excludedCategoriesList.contains(notificationType)) {
                        excludedCategoriesList.add(notificationType);
                    }
                }
            });

            dialogView.more.setOnClickListener(v1 -> {
                if (dialogView.clearAllNotif.getVisibility() == View.VISIBLE) {
                    dialogView.clearAllNotif.setVisibility(View.GONE);
                    ((MaterialButton) v1).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_expand_more_24, requireContext().getTheme()));
                } else {
                    dialogView.clearAllNotif.setVisibility(View.VISIBLE);
                    ((MaterialButton) v1).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_expand_less_24, requireContext().getTheme()));
                }
            });
            dialogBuilder.setOnDismissListener(dialogInterface -> doAction(changes.get(), excludedCategoriesList));
            dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> {
                dialog.dismiss();
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        });

        binding.viewpagerNotificationContainer.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewpagerNotificationContainer.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment;
                if (binding.viewpagerNotificationContainer.getAdapter() != null) {
                    fragment = (Fragment) binding.viewpagerNotificationContainer.getAdapter().instantiateItem(binding.viewpagerNotificationContainer, tab.getPosition());
                    if (fragment instanceof FragmentMastodonNotification) {
                        FragmentMastodonNotification fragmentMastodonNotification = ((FragmentMastodonNotification) fragment);
                        fragmentMastodonNotification.scrollToTop();
                    }
                }
            }
        });
    }


    private void doAction(boolean changed, List<String> excludedCategoriesList) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        if (changed) {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            if (excludedCategoriesList.size() > 0) {
                StringBuilder cat = new StringBuilder();
                for (String category : excludedCategoriesList) {
                    cat.append(category).append('|');
                }
                if (cat.toString().endsWith("|")) {
                    cat.setLength(cat.length() - 1);
                }
                editor.putString(getString(R.string.SET_EXCLUDED_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, cat.toString());
            } else {
                editor.putString(getString(R.string.SET_EXCLUDED_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, null);
            }
            editor.commit();
            ((BaseMainActivity) requireActivity()).refreshFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationManager mNotificationManager = (NotificationManager) requireActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && BaseMainActivity.currentAccount != null && BaseMainActivity.currentAccount.mastodon_account != null) {
            for (StatusBarNotification statusBarNotification : mNotificationManager.getActiveNotifications()) {
                if (statusBarNotification.getGroupKey().contains(BaseMainActivity.currentAccount.mastodon_account.acct + "@" + BaseMainActivity.currentAccount.instance)) {
                    mNotificationManager.cancel(statusBarNotification.getId());
                }
            }
        } else {
            mNotificationManager.cancelAll();
        }
    }

    public void scrollToTop() {
        if (binding != null) {
            FedilabNotificationPageAdapter fedilabNotificationPageAdapter = ((FedilabNotificationPageAdapter) binding.viewpagerNotificationContainer.getAdapter());
            if (fedilabNotificationPageAdapter != null) {
                FragmentMastodonNotification fragmentMastodonNotification = (FragmentMastodonNotification) fedilabNotificationPageAdapter.getCurrentFragment();
                if (fragmentMastodonNotification != null) {
                    fragmentMastodonNotification.scrollToTop();
                }
            }
        }
    }


    public interface UpdateCounters {
        void onUpdateNotification(int count);
    }
}
