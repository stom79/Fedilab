package app.fedilab.android.mastodon.ui.fragment.timeline;
/* Copyright 2026 Thomas Schneider
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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.FragmentProfileFeaturedBinding;
import app.fedilab.android.mastodon.activities.CollectionsActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Collection;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.AccountAdapter;
import app.fedilab.android.mastodon.ui.drawer.CollectionAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.CollectionsVM;

public class FragmentProfileFeatured extends Fragment implements CollectionAdapter.ActionOnCollection {

    private FragmentProfileFeaturedBinding binding;
    private String accountId;
    private boolean isOwnAccount;
    private CollectionsVM collectionsVM;
    private AccountsVM accountsVM;
    private boolean endorsementsLoaded;
    private boolean collectionsLoaded;
    private List<Collection> collectionList;
    private CollectionAdapter collectionAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileFeaturedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString(Helper.ARG_CACHED_ACCOUNT_ID);
        }
        isOwnAccount = accountId != null && accountId.equals(BaseMainActivity.currentUserID);

        accountsVM = new ViewModelProvider(this).get(AccountsVM.class);
        collectionsVM = new ViewModelProvider(this).get(CollectionsVM.class);

        binding.loader.setVisibility(View.VISIBLE);
        binding.mainContainer.setVisibility(View.GONE);

        accountsVM.getAccountEndorsements(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountId, "80", null, null)
                .observe(getViewLifecycleOwner(), this::displayEndorsedAccounts);

        collectionsVM.getAccountCollections(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, accountId)
                .observe(getViewLifecycleOwner(), this::displayCollections);
    }

    private void displayEndorsedAccounts(List<Account> accounts) {
        endorsementsLoaded = true;
        if (accounts != null && !accounts.isEmpty()) {
            binding.profilesLabel.setVisibility(View.VISIBLE);
            binding.profilesList.setVisibility(View.VISIBLE);
            AccountAdapter adapter = new AccountAdapter(accounts);
            binding.profilesList.setAdapter(adapter);
            binding.profilesList.setLayoutManager(new LinearLayoutManager(requireContext()));
            fetchRelationships(accounts, adapter);
        } else {
            binding.profilesLabel.setVisibility(View.GONE);
            binding.profilesList.setVisibility(View.GONE);
        }
        checkIfLoaded();
    }

    private void fetchRelationships(List<Account> accounts, AccountAdapter adapter) {
        List<String> ids = new ArrayList<>();
        for (Account account : accounts) {
            ids.add(account.id);
        }
        accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, ids)
                .observe(getViewLifecycleOwner(), relationShips -> {
                    if (relationShips != null) {
                        for (RelationShip relationShip : relationShips) {
                            if (relationShip == null || relationShip.id == null) {
                                continue;
                            }
                            for (Account account : accounts) {
                                if (account != null && account.id != null && account.id.compareToIgnoreCase(relationShip.id) == 0) {
                                    account.relationShip = relationShip;
                                }
                            }
                        }
                        adapter.notifyItemRangeChanged(0, accounts.size());
                    }
                });
    }

    private void displayCollections(List<Collection> collections) {
        collectionsLoaded = true;
        if (collections != null && !collections.isEmpty()) {
            binding.collectionsLabel.setVisibility(View.VISIBLE);
            binding.collectionsList.setVisibility(View.VISIBLE);
            collectionList = new ArrayList<>(collections);
            collectionAdapter = new CollectionAdapter(collectionList, isOwnAccount ? CollectionAdapter.MODE_OWN : CollectionAdapter.MODE_OTHER);
            collectionAdapter.actionOnCollection = this;
            binding.collectionsList.setAdapter(collectionAdapter);
            binding.collectionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        } else {
            binding.collectionsLabel.setVisibility(View.GONE);
            binding.collectionsList.setVisibility(View.GONE);
        }
        checkIfLoaded();
    }

    private void checkIfLoaded() {
        if (!endorsementsLoaded || !collectionsLoaded) {
            return;
        }
        binding.loader.setVisibility(View.GONE);
        binding.mainContainer.setVisibility(View.VISIBLE);
        if (binding.profilesLabel.getVisibility() == View.GONE && binding.collectionsLabel.getVisibility() == View.GONE) {
            binding.noContent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void click(Collection collection) {
        Intent intent = new Intent(requireContext(), CollectionsActivity.class);
        intent.putExtra(Helper.ARG_COLLECTION_ACCOUNT_ID, accountId);
        intent.putExtra(Helper.ARG_OWN_COLLECTIONS, isOwnAccount);
        intent.putExtra(Helper.ARG_COLLECTION_ID, collection.id);
        startActivity(intent);
    }

    @Override
    public void share(Collection collection) {
        if (collection.url != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, collection.url);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link)));
        }
    }

    @Override
    public void copyLink(Collection collection) {
        if (collection.url != null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("collection_url", collection.url);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                es.dmoral.toasty.Toasty.info(requireContext(), getString(R.string.copy_link), es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void manageAccounts(Collection collection) {
        Intent intent = new Intent(requireContext(), CollectionsActivity.class);
        intent.putExtra(Helper.ARG_COLLECTION_ACCOUNT_ID, accountId);
        intent.putExtra(Helper.ARG_OWN_COLLECTIONS, isOwnAccount);
        startActivity(intent);
    }

    @Override
    public void edit(Collection collection) {
        Intent intent = new Intent(requireContext(), CollectionsActivity.class);
        intent.putExtra(Helper.ARG_COLLECTION_ACCOUNT_ID, accountId);
        intent.putExtra(Helper.ARG_OWN_COLLECTIONS, isOwnAccount);
        startActivity(intent);
    }

    @Override
    public void removeMyself(Collection collection) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.action_remove_myself_from_collection);
        builder.setMessage(R.string.action_collection_confirm_delete);
        builder.setPositiveButton(R.string.action_remove_myself_from_collection, (dialog, which) -> {
            collectionsVM.revokeCurrentUserFromCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, collection.id, BaseMainActivity.currentUserID)
                    .observe(getViewLifecycleOwner(), success -> {
                        if (success != null && success && collectionList != null && collectionAdapter != null) {
                            int position = collectionList.indexOf(collection);
                            if (position >= 0) {
                                collectionList.remove(position);
                                collectionAdapter.notifyItemRemoved(position);
                            }
                            if (collectionList.isEmpty()) {
                                binding.collectionsLabel.setVisibility(View.GONE);
                                binding.collectionsList.setVisibility(View.GONE);
                                if (binding.profilesLabel.getVisibility() == View.GONE) {
                                    binding.noContent.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    });
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void delete(Collection collection) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.action_collection_delete);
        builder.setMessage(R.string.action_collection_confirm_delete);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            collectionsVM.deleteCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, collection.id);
            if (collectionList != null && collectionAdapter != null) {
                int position = collectionList.indexOf(collection);
                if (position >= 0) {
                    collectionList.remove(position);
                    collectionAdapter.notifyItemRemoved(position);
                }
            }
            if (collectionList == null || collectionList.isEmpty()) {
                binding.collectionsLabel.setVisibility(View.GONE);
                binding.collectionsList.setVisibility(View.GONE);
                if (binding.profilesLabel.getVisibility() == View.GONE) {
                    binding.noContent.setVisibility(View.VISIBLE);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
