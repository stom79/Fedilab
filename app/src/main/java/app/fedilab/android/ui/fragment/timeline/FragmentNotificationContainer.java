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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentNotificationContainerBinding;
import app.fedilab.android.databinding.PopupNotificationSettingsBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.pageadapter.FedilabNotificationPageAdapter;
import app.fedilab.android.viewmodel.mastodon.NotificationsVM;
import es.dmoral.toasty.Toasty;


public class FragmentNotificationContainer extends Fragment {

    private FragmentNotificationContainerBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationContainerBinding.inflate(inflater, container, false);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        boolean display_all_notification = sharedpreferences.getBoolean(getString(R.string.SET_DISPLAY_ALL_NOTIFICATIONS_TYPE) + BaseMainActivity.currentUserID + BaseMainActivity.currentInstance, false);
        if (!display_all_notification) {
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.setTabMode(TabLayout.MODE_FIXED);
            binding.viewpager.setAdapter(new FedilabNotificationPageAdapter(requireActivity(), false));
        } else {
            binding.tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.tabLayout.addTab(binding.tabLayout.newTab());
            binding.viewpager.setAdapter(new FedilabNotificationPageAdapter(requireActivity(), true));
        }
        AtomicBoolean changes = new AtomicBoolean(false);
        binding.settings.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity(), Helper.dialogStyle());
            PopupNotificationSettingsBinding dialogView = PopupNotificationSettingsBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(dialogView.getRoot());

            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayMentions);
            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayFavourites);
            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayReblogs);
            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayPollResults);
            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayUpdatesFromPeople);
            ThemeHelper.changeButtonColor(requireActivity(), dialogView.displayFollows);
            DrawableCompat.setTintList(DrawableCompat.wrap(dialogView.displayAllCategories.getThumbDrawable()), ThemeHelper.getSwitchCompatThumbDrawable(requireActivity()));
            DrawableCompat.setTintList(DrawableCompat.wrap(dialogView.displayAllCategories.getTrackDrawable()), ThemeHelper.getSwitchCompatTrackDrawable(requireActivity()));
            dialogView.clearAllNotif.setOnClickListener(v1 -> {
                AlertDialog.Builder db = new AlertDialog.Builder(requireActivity(), Helper.dialogStyle());
                db.setTitle(R.string.delete_notification_ask_all);
                db.setMessage(R.string.delete_notification_all_warning);
                db.setPositiveButton(R.string.delete_all, (dialog, id) -> {
                    changes.set(true);
                    NotificationsVM notificationsVM = new ViewModelProvider(FragmentNotificationContainer.this).get(NotificationsVM.class);
                    notificationsVM.clearNotification(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                            .observe(getViewLifecycleOwner(), unused -> Toasty.info(requireActivity(), R.string.delete_notification_all, Toasty.LENGTH_LONG).show());
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
            dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> {
                if (changes.get()) {
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
                dialog.dismiss();
            });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
        });

        binding.tabLayout.setTabTextColors(ThemeHelper.getAttColor(requireActivity(), R.attr.mTextColor), ContextCompat.getColor(requireActivity(), R.color.cyanea_accent_dark_reference));
        binding.tabLayout.setTabIconTint(ThemeHelper.getColorStateList(requireActivity()));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = getParentFragmentManager().findFragmentByTag("f" + binding.viewpager.getCurrentItem());
                if (fragment instanceof FragmentMastodonNotification) {
                    FragmentMastodonNotification fragmentMastodonNotification = ((FragmentMastodonNotification) fragment);
                    fragmentMastodonNotification.scrollToTop();
                }
            }
        });
        new TabLayoutMediator(binding.tabLayout, binding.viewpager,
                (tab, position) -> {
                    binding.viewpager.setCurrentItem(tab.getPosition(), true);
                    if (!display_all_notification) {
                        switch (position) {
                            case 0:
                                tab.setText(getString(R.string.all));
                                break;
                            case 1:
                                tab.setText(getString(R.string.mention));
                                break;
                        }
                    } else {
                        switch (position) {
                            case 0:
                                tab.setText(getString(R.string.all));
                                break;
                            case 1:
                                tab.setIcon(R.drawable.ic_baseline_reply_24);
                                break;
                            case 2:
                                tab.setIcon(R.drawable.ic_baseline_star_24);
                                break;
                            case 3:
                                tab.setIcon(R.drawable.ic_repeat);
                                break;
                            case 4:
                                tab.setIcon(R.drawable.ic_baseline_poll_24);
                                break;
                            case 5:
                                tab.setIcon(R.drawable.ic_baseline_home_24);
                                break;
                            case 6:
                                tab.setIcon(R.drawable.ic_baseline_person_add_alt_1_24);
                                break;
                        }
                    }

                }
        ).attach();

        return binding.getRoot();
    }


    public void scrollToTop() {
        if (binding != null) {
            Fragment fragment = getParentFragmentManager().findFragmentByTag("f" + binding.viewpager.getCurrentItem());
            if (fragment instanceof FragmentMastodonNotification) {
                ((FragmentMastodonNotification) fragment).scrollToTop();
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

}
