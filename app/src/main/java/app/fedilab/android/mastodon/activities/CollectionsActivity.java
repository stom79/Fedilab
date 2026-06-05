package app.fedilab.android.mastodon.activities;
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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.databinding.ActivityCollectionsBinding;
import app.fedilab.android.databinding.DrawerAccountBinding;
import app.fedilab.android.databinding.PopupAddCollectionBinding;
import app.fedilab.android.databinding.PopupManageAccountsListBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Collection;
import app.fedilab.android.mastodon.client.entities.api.RelationShip;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.ui.drawer.AccountAdapter;
import app.fedilab.android.mastodon.ui.drawer.CollectionAdapter;
import app.fedilab.android.mastodon.viewmodel.mastodon.AccountsVM;
import app.fedilab.android.mastodon.viewmodel.mastodon.CollectionsVM;
import es.dmoral.toasty.Toasty;


public class CollectionsActivity extends BaseBarActivity implements CollectionAdapter.ActionOnCollection {

    private ActivityCollectionsBinding binding;
    private CollectionsVM collectionsVM;
    private AccountsVM accountsVM;
    private ArrayList<Collection> collectionList;
    private CollectionAdapter collectionAdapter;
    private Collection currentCollection;
    private boolean isOwnCollections;
    private String targetAccountId;
    private boolean viewingDetail;
    private boolean openedDirectly;
    private int currentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCollectionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewingDetail = false;
        currentTab = 0;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String directCollectionId = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            targetAccountId = extras.getString(Helper.ARG_COLLECTION_ACCOUNT_ID);
            isOwnCollections = extras.getBoolean(Helper.ARG_OWN_COLLECTIONS, false);
            directCollectionId = extras.getString(Helper.ARG_COLLECTION_ID);
        }
        if (targetAccountId == null) {
            targetAccountId = BaseMainActivity.currentUserID;
            isOwnCollections = true;
        }

        collectionsVM = new ViewModelProvider(CollectionsActivity.this).get(CollectionsVM.class);
        accountsVM = new ViewModelProvider(CollectionsActivity.this).get(AccountsVM.class);

        if (isOwnCollections) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.collection_tab_created));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.collection_tab_featuring));
            binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    loadCollections();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        } else {
            binding.tabLayout.setVisibility(View.GONE);
        }

        if (directCollectionId != null) {
            openedDirectly = true;
            Collection directCollection = new Collection();
            directCollection.id = directCollectionId;
            click(directCollection);
        } else {
            loadCollections();
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewingDetail && !openedDirectly) {
                    viewingDetail = false;
                    setTitle(R.string.action_collections);
                    binding.tabLayout.setVisibility(isOwnCollections ? View.VISIBLE : View.GONE);
                    invalidateOptionsMenu();
                    loadCollections();
                } else {
                    finish();
                }
            }
        });
    }

    private void loadCollections() {
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.noContent.setVisibility(View.GONE);

        if (currentTab == 0) {
            collectionsVM.getAccountCollections(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, targetAccountId)
                    .observe(CollectionsActivity.this, this::displayCollections);
        } else {
            collectionsVM.getAccountInCollections(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, targetAccountId)
                    .observe(CollectionsActivity.this, this::displayCollections);
        }
    }

    private int getAdapterMode() {
        if (isOwnCollections && currentTab == 0) {
            return CollectionAdapter.MODE_OWN;
        } else if (isOwnCollections && currentTab == 1) {
            return CollectionAdapter.MODE_FEATURING;
        }
        return CollectionAdapter.MODE_OTHER;
    }

    private void displayCollections(List<Collection> collections) {
        binding.loader.setVisibility(View.GONE);
        if (collections != null && collections.size() > 0) {
            collectionList = new ArrayList<>(collections);
            collectionAdapter = new CollectionAdapter(collectionList, getAdapterMode());
            collectionAdapter.actionOnCollection = this;
            binding.recyclerView.setAdapter(collectionAdapter);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(CollectionsActivity.this));
            binding.recyclerView.setVisibility(View.VISIBLE);
        } else {
            binding.noContent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void click(Collection collection) {
        currentCollection = collection;
        viewingDetail = true;
        setTitle(collection.name);
        binding.tabLayout.setVisibility(View.GONE);
        invalidateOptionsMenu();
        showCollectionDetail(collection);
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
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("collection_url", collection.url);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toasty.info(CollectionsActivity.this, getString(R.string.copy_link), Toasty.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void manageAccounts(Collection collection) {
        currentCollection = collection;
        viewingDetail = true;
        setTitle(collection.name);
        binding.tabLayout.setVisibility(View.GONE);
        invalidateOptionsMenu();
        showCollectionDetail(collection);
        showAddAccountDialog();
    }

    @Override
    public void edit(Collection collection) {
        currentCollection = collection;
        showCreateEditDialog(collection);
    }

    @Override
    public void removeMyself(Collection collection) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(CollectionsActivity.this);
        builder.setMessage(R.string.action_remove_myself_from_collection);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            collectionsVM.revokeCurrentUserFromCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken,
                            collection.id, BaseMainActivity.currentUserID)
                    .observe(CollectionsActivity.this, success -> {
                        if (collectionList != null) {
                            int position = collectionList.indexOf(collection);
                            if (position >= 0) {
                                collectionList.remove(position);
                                collectionAdapter.notifyItemRemoved(position);
                            }
                        }
                        binding.recyclerView.setVisibility(collectionList != null && !collectionList.isEmpty() ? View.VISIBLE : View.GONE);
                        binding.noContent.setVisibility(collectionList != null && !collectionList.isEmpty() ? View.GONE : View.VISIBLE);
                    });
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void delete(Collection collection) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(CollectionsActivity.this);
        builder.setTitle(R.string.action_collection_delete);
        builder.setMessage(R.string.action_collection_confirm_delete);
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            collectionsVM.deleteCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, collection.id);
            if (collectionList != null) {
                int position = collectionList.indexOf(collection);
                if (position >= 0) {
                    collectionList.remove(position);
                    collectionAdapter.notifyItemRemoved(position);
                }
            }
            binding.recyclerView.setVisibility(collectionList != null && collectionList.size() > 0 ? View.VISIBLE : View.GONE);
            binding.noContent.setVisibility(collectionList != null && collectionList.size() > 0 ? View.GONE : View.VISIBLE);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showCollectionDetail(Collection collection) {
        binding.loader.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.noContent.setVisibility(View.GONE);

        collectionsVM.getCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, collection.id)
                .observe(CollectionsActivity.this, result -> {
                    binding.loader.setVisibility(View.GONE);
                    if (result != null && result.collection != null) {
                        setTitle(result.collection.name);
                    }
                    if (result != null && result.accounts != null && result.accounts.size() > 0) {
                        String ownerId = result.collection != null ? result.collection.account_id : null;
                        List<Account> filtered = new ArrayList<>();
                        for (Account account : result.accounts) {
                            if (ownerId == null || !ownerId.equals(account.id)) {
                                filtered.add(account);
                            }
                        }
                        if (!filtered.isEmpty()) {
                            AccountAdapter detailAdapter = new AccountAdapter(filtered);
                            binding.recyclerView.setAdapter(detailAdapter);
                            binding.recyclerView.setLayoutManager(new LinearLayoutManager(CollectionsActivity.this));
                            binding.recyclerView.setVisibility(View.VISIBLE);
                            fetchRelationships(filtered, detailAdapter);
                        } else {
                            binding.noContent.setText(R.string.no_collections);
                            binding.noContent.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.noContent.setText(R.string.no_collections);
                        binding.noContent.setVisibility(View.VISIBLE);
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_add_collection) {
            showCreateEditDialog(null);
        } else if (item.getItemId() == R.id.action_edit_collection && currentCollection != null) {
            showCreateEditDialog(currentCollection);
        } else if (item.getItemId() == R.id.action_delete_collection && currentCollection != null) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(CollectionsActivity.this);
            builder.setTitle(R.string.action_collection_delete);
            builder.setMessage(R.string.action_collection_confirm_delete);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                collectionsVM.deleteCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, currentCollection.id);
                if (collectionList != null) {
                    int position = collectionList.indexOf(currentCollection);
                    if (position >= 0) {
                        collectionList.remove(position);
                        collectionAdapter.notifyItemRemoved(position);
                    }
                }
                viewingDetail = false;
                setTitle(R.string.action_collections);
                binding.tabLayout.setVisibility(isOwnCollections ? View.VISIBLE : View.GONE);
                invalidateOptionsMenu();
                binding.recyclerView.setAdapter(collectionAdapter);
                binding.recyclerView.setVisibility(collectionList != null && collectionList.size() > 0 ? View.VISIBLE : View.GONE);
                binding.noContent.setVisibility(collectionList != null && collectionList.size() > 0 ? View.GONE : View.VISIBLE);
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
        } else if (item.getItemId() == R.id.action_add_account && currentCollection != null) {
            showAddAccountDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCreateEditDialog(Collection existingCollection) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(CollectionsActivity.this);
        PopupAddCollectionBinding popupBinding = PopupAddCollectionBinding.inflate(getLayoutInflater());
        dialogBuilder.setView(popupBinding.getRoot());

        if (existingCollection != null) {
            dialogBuilder.setTitle(R.string.action_collection_edit);
            popupBinding.collectionName.setText(existingCollection.name);
            if (existingCollection.description != null) {
                popupBinding.collectionDescription.setText(existingCollection.description);
            }
            if (existingCollection.tag != null) {
                popupBinding.collectionTag.setText(existingCollection.tag.name);
            }
            popupBinding.collectionSensitive.setChecked(existingCollection.sensitive);
            popupBinding.collectionDiscoverable.setChecked(existingCollection.discoverable);
        } else {
            dialogBuilder.setTitle(R.string.action_collection_add);
        }

        dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
            String name = popupBinding.collectionName.getText() != null ? popupBinding.collectionName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                popupBinding.collectionName.setError(getString(R.string.not_valid_collection_name));
                return;
            }
            String description = popupBinding.collectionDescription.getText() != null ? popupBinding.collectionDescription.getText().toString().trim() : null;
            String tagName = popupBinding.collectionTag.getText() != null ? popupBinding.collectionTag.getText().toString().trim() : null;
            boolean sensitive = popupBinding.collectionSensitive.isChecked();
            boolean discoverable = popupBinding.collectionDiscoverable.isChecked();

            if (description != null && description.isEmpty()) {
                description = null;
            }
            if (tagName != null && tagName.isEmpty()) {
                tagName = null;
            }

            if (existingCollection != null) {
                collectionsVM.updateCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken,
                                existingCollection.id, name, description, null, tagName, sensitive, discoverable)
                        .observe(CollectionsActivity.this, updatedCollection -> {
                            if (updatedCollection != null && collectionList != null) {
                                int position = collectionList.indexOf(existingCollection);
                                if (position >= 0) {
                                    collectionList.set(position, updatedCollection);
                                    collectionAdapter.notifyItemChanged(position);
                                }
                                currentCollection = updatedCollection;
                                setTitle(updatedCollection.name);
                            } else {
                                Toasty.error(CollectionsActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                            }
                        });
            } else {
                collectionsVM.createCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken,
                                name, description, null, tagName, sensitive, discoverable)
                        .observe(CollectionsActivity.this, newCollection -> {
                            if (newCollection != null) {
                                if (collectionList == null) {
                                    collectionList = new ArrayList<>();
                                }
                                if (collectionAdapter == null) {
                                    collectionAdapter = new CollectionAdapter(collectionList, getAdapterMode());
                                    collectionAdapter.actionOnCollection = this;
                                    binding.recyclerView.setAdapter(collectionAdapter);
                                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(CollectionsActivity.this));
                                }
                                collectionList.add(0, newCollection);
                                collectionAdapter.notifyItemInserted(0);
                                binding.noContent.setVisibility(View.GONE);
                                binding.recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                Toasty.error(CollectionsActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                            }
                        });
            }
            dialog.dismiss();
        });
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        dialogBuilder.create().show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showAddAccountDialog() {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(CollectionsActivity.this);
        PopupManageAccountsListBinding popupBinding = PopupManageAccountsListBinding.inflate(getLayoutInflater());
        dialogBuilder.setView(popupBinding.getRoot());
        popupBinding.loader.setVisibility(View.GONE);
        popupBinding.listTitle.setVisibility(View.GONE);

        popupBinding.searchAccount.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (popupBinding.searchAccount.length() > 0 && event.getRawX() >= (popupBinding.searchAccount.getRight() - popupBinding.searchAccount.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    popupBinding.searchAccount.setText("");
                }
            }
            return false;
        });

        popupBinding.searchAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    popupBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_close_24, 0);
                } else {
                    popupBinding.searchAccount.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_search_24, 0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() > 0) {
                    accountsVM.searchAccounts(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, s.toString(), 20, true, true)
                            .observe(CollectionsActivity.this, accounts -> {
                                if (accounts != null) {
                                    accounts.removeIf(a -> a.id != null && a.id.equals(BaseMainActivity.currentUserID));
                                    popupBinding.lvAccountsSearch.setVisibility(View.VISIBLE);
                                    popupBinding.lvAccountsCurrent.setVisibility(View.GONE);
                                    SearchAccountAdapter adapter = new SearchAccountAdapter(accounts);
                                    popupBinding.lvAccountsSearch.setAdapter(adapter);
                                    popupBinding.lvAccountsSearch.setLayoutManager(new LinearLayoutManager(CollectionsActivity.this));
                                }
                            });
                } else {
                    popupBinding.lvAccountsSearch.setVisibility(View.GONE);
                    popupBinding.lvAccountsCurrent.setVisibility(View.VISIBLE);
                }
            }
        });

        dialogBuilder.setPositiveButton(R.string.close, (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnDismissListener(d -> {
            if (currentCollection != null) {
                showCollectionDetail(currentCollection);
            }
        });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (!viewingDetail) {
            if (isOwnCollections && currentTab == 0) {
                getMenuInflater().inflate(R.menu.menu_main_collection, menu);
            }
        } else {
            if (isOwnCollections) {
                getMenuInflater().inflate(R.menu.menu_collection, menu);
            }
        }
        return true;
    }



    private void fetchRelationships(List<Account> accounts, AccountAdapter adapter) {
        List<String> ids = new ArrayList<>();
        for (Account account : accounts) {
            ids.add(account.id);
        }
        accountsVM.getRelationships(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, ids)
                .observe(CollectionsActivity.this, relationShips -> {
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

    private class SearchAccountAdapter extends RecyclerView.Adapter<SearchAccountAdapter.SearchViewHolder> {

        private final List<Account> accounts;

        SearchAccountAdapter(List<Account> accounts) {
            this.accounts = accounts;
        }

        @NonNull
        @Override
        public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DrawerAccountBinding itemBinding = DrawerAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new SearchViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
            Account account = accounts.get(position);
            holder.binding.displayName.setText(account.display_name != null ? account.display_name : account.username);
            holder.binding.username.setText(String.format("@%s", account.acct));
            holder.binding.bio.setVisibility(View.GONE);
            holder.binding.muteGroup.setVisibility(View.GONE);
            holder.binding.block.setVisibility(View.GONE);
            holder.binding.followAction.setVisibility(View.VISIBLE);
            holder.binding.followAction.setIconResource(R.drawable.ic_baseline_person_add_24);

            if (account.avatar != null) {
                Glide.with(holder.binding.avatar.getContext())
                        .load(account.avatar)
                        .placeholder(R.drawable.ic_person)
                        .into(holder.binding.avatar);
            }

            if (account.feature_approval != null && "denied".equals(account.feature_approval.current_user)) {
                holder.binding.followAction.setAlpha(0.5f);
                holder.binding.followAction.setOnClickListener(v ->
                        Toasty.warning(CollectionsActivity.this, getString(R.string.collection_feature_denied), Toasty.LENGTH_LONG).show());
            } else {
                holder.binding.followAction.setAlpha(1.0f);
                holder.binding.followAction.setOnClickListener(v -> {
                    if (currentCollection != null) {
                        collectionsVM.addAccountToCollection(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, currentCollection.id, account.id)
                                .observe(CollectionsActivity.this, item -> {
                                    if (item != null) {
                                        holder.binding.followAction.setIconResource(R.drawable.ic_baseline_check_24);
                                        holder.binding.followAction.setEnabled(false);
                                        Toasty.success(CollectionsActivity.this, getString(R.string.account_added_to_collection), Toasty.LENGTH_LONG).show();
                                    } else {
                                        Toasty.error(CollectionsActivity.this, getString(R.string.toast_error), Toasty.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        class SearchViewHolder extends RecyclerView.ViewHolder {
            DrawerAccountBinding binding;

            SearchViewHolder(DrawerAccountBinding itemView) {
                super(itemView.getRoot());
                binding = itemView;
            }
        }
    }
}
