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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Announcement;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.AnnouncementAdapter;
import app.fedilab.android.viewmodel.mastodon.AnnouncementsVM;


public class FragmentMastodonAnnouncement extends Fragment {


    private FragmentPaginationBinding binding;
    private AnnouncementsVM announcementsVM;
    private AnnouncementAdapter announcementAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.getRoot().setBackgroundColor(ThemeHelper.getBackgroundColor(requireActivity()));
        int c1 = getResources().getColor(R.color.cyanea_accent_reference);
        binding.swipeContainer.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.cyanea_primary_reference));
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );
        announcementsVM = new ViewModelProvider(FragmentMastodonAnnouncement.this).get(AnnouncementsVM.class);
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        announcementsVM.getAnnouncements(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                .observe(getViewLifecycleOwner(), this::initializeAnnouncementView);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        NotificationManager notificationManager = (NotificationManager) requireActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Helper.NOTIFICATION_USER_NOTIF);
    }

    /**
     * Intialize the view for announcements
     *
     * @param announcements List of {@link Announcement}
     */
    private void initializeAnnouncementView(List<Announcement> announcements) {

        binding.loader.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        if (announcements == null || announcements.size() == 0) {
            binding.noActionText.setText(R.string.no_announcements);
            binding.noAction.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            return;
        } else {
            binding.noAction.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }


        announcementAdapter = new AnnouncementAdapter(announcements);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(announcementAdapter);


        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            announcementsVM.getAnnouncements(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                    .observe(getViewLifecycleOwner(), this::initializeAnnouncementView);
        });

    }

    public void scrollToTop() {
        binding.recyclerView.scrollToPosition(0);
    }

}