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


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Suggestion;
import app.fedilab.android.client.entities.api.Suggestions;
import app.fedilab.android.databinding.FragmentPaginationBinding;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.SuggestionAdapter;
import app.fedilab.android.viewmodel.mastodon.AccountsVM;

public class FragmentMastodonSuggestion extends Fragment {


    private FragmentPaginationBinding binding;
    private AccountsVM accountsVM;
    private boolean flagLoading;
    private List<Suggestion> suggestions;
    private String max_id;
    private SuggestionAdapter suggestionAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        flagLoading = false;
        binding = FragmentPaginationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int c1 = ThemeHelper.getAttColor(requireActivity(), R.attr.colorAccent);
        binding.swipeContainer.setColorSchemeColors(
                c1, c1, c1
        );
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        accountsVM = new ViewModelProvider(FragmentMastodonSuggestion.this).get(AccountsVM.class);
        max_id = null;
        accountsVM.getSuggestions(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
    }


    public void scrollToTop() {
        binding.recyclerView.setAdapter(suggestionAdapter);
    }

    /**
     * Intialize the view for Suggestions
     *
     * @param suggestions {@link Suggestions}
     */
    private void initializeAccountCommonView(final Suggestions suggestions) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.noAction.setVisibility(View.GONE);
        binding.swipeContainer.setRefreshing(false);
        binding.swipeContainer.setOnRefreshListener(() -> {
            binding.swipeContainer.setRefreshing(true);
            flagLoading = false;
            max_id = null;
            accountsVM.getSuggestions(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, null)
                    .observe(getViewLifecycleOwner(), this::initializeAccountCommonView);
        });
        if (suggestions == null || suggestions.suggestions == null || suggestions.suggestions.size() == 0) {
            binding.noAction.setVisibility(View.VISIBLE);
            binding.noActionText.setText(R.string.no_accounts);
            return;
        }
        binding.recyclerView.setVisibility(View.VISIBLE);

        this.suggestions = suggestions.suggestions;
        suggestionAdapter = new SuggestionAdapter(this.suggestions);
        flagLoading = suggestions.pagination.max_id == null;
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(suggestionAdapter);
        max_id = suggestions.pagination.max_id;
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flagLoading) {
                            flagLoading = true;
                            binding.loadingNextElements.setVisibility(View.VISIBLE);
                            accountsVM.getSuggestions(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, max_id)
                                    .observe(getViewLifecycleOwner(), suggestionsPaginated -> {
                                        dealWithPagination(suggestionsPaginated);
                                    });
                        }
                    } else {
                        binding.loadingNextElements.setVisibility(View.GONE);
                    }
                }

            }
        });
    }


    /**
     * Update view and pagination when scrolling down
     *
     * @param suggestions_fetched Suggestions
     */
    private void dealWithPagination(Suggestions suggestions_fetched) {
        flagLoading = false;
        if (binding == null || !isAdded() || getActivity() == null) {
            return;
        }
        binding.loadingNextElements.setVisibility(View.GONE);
        if (this.suggestions != null && suggestions_fetched != null && suggestions_fetched.suggestions != null) {
            flagLoading = suggestions_fetched.pagination.max_id == null;
            int startId = this.suggestions.size();
            this.suggestions.addAll(suggestions_fetched.suggestions);
            max_id = suggestions_fetched.pagination.max_id;
            suggestionAdapter.notifyItemRangeInserted(startId, suggestions_fetched.suggestions.size());
        } else {
            flagLoading = true;
        }
    }
}