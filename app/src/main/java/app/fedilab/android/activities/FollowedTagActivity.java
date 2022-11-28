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
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Tag;
import app.fedilab.android.client.entities.app.Timeline;
import app.fedilab.android.databinding.ActivityFollowedTagsBinding;
import app.fedilab.android.databinding.PopupAddFollowedTagtBinding;
import app.fedilab.android.helper.Helper;
import app.fedilab.android.helper.ThemeHelper;
import app.fedilab.android.ui.drawer.FollowedTagAdapter;
import app.fedilab.android.ui.fragment.timeline.FragmentMastodonTimeline;
import app.fedilab.android.viewmodel.mastodon.TagVM;
import es.dmoral.toasty.Toasty;


public class FollowedTagActivity extends BaseActivity implements FollowedTagAdapter.ActionOnTag {


    private ActivityFollowedTagsBinding binding;
    private boolean canGoBack;
    private TagVM tagVM;
    private Tag tag;
    private ArrayList<Tag> tagList;
    private FollowedTagAdapter followedTagAdapter;
    private FragmentMastodonTimeline fragmentMastodonTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFollowedTagsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        canGoBack = false;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        tagVM = new ViewModelProvider(FollowedTagActivity.this).get(TagVM.class);
        tagVM.followedTags(BaseMainActivity.currentInstance, BaseMainActivity.currentToken)
                .observe(FollowedTagActivity.this, tags -> {
                    if (tags != null && tags.tags != null && tags.tags.size() > 0) {
                        tagList = new ArrayList<>(tags.tags);
                        followedTagAdapter = new FollowedTagAdapter(tagList);
                        followedTagAdapter.actionOnTag = this;
                        binding.notContent.setVisibility(View.GONE);
                        binding.recyclerView.setAdapter(followedTagAdapter);
                        binding.recyclerView.setLayoutManager(new LinearLayoutManager(FollowedTagActivity.this));
                    } else {
                        binding.notContent.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_unfollow && tag != null) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(FollowedTagActivity.this);
            alt_bld.setTitle(R.string.action_unfollow_tag);
            alt_bld.setMessage(R.string.action_unfollow_tag_confirm);
            alt_bld.setPositiveButton(R.string.unfollow, (dialog, id) -> {
                tagVM.unfollow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, tag.name);
                int position = 0;
                for (Tag tagTmp : tagList) {
                    if (tagTmp.name.equalsIgnoreCase(tag.name)) {
                        break;
                    }
                    position++;
                }
                tagList.remove(position);
                followedTagAdapter.notifyItemRemoved(position);
                ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.recyclerView, () -> {
                    canGoBack = false;
                    if (fragmentMastodonTimeline != null) {
                        fragmentMastodonTimeline.onDestroyView();
                    }
                    invalidateOptionsMenu();
                    setTitle(R.string.action_lists);
                });
                if (tagList.size() == 0) {
                    binding.notContent.setVisibility(View.VISIBLE);
                } else {
                    binding.notContent.setVisibility(View.GONE);
                }
            });
            alt_bld.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            AlertDialog alert = alt_bld.create();
            alert.show();
        } else if (item.getItemId() == R.id.action_follow_tag) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(FollowedTagActivity.this);
            PopupAddFollowedTagtBinding popupAddFollowedTagtBinding = PopupAddFollowedTagtBinding.inflate(getLayoutInflater());
            dialogBuilder.setView(popupAddFollowedTagtBinding.getRoot());
            popupAddFollowedTagtBinding.addTag.setFilters(new InputFilter[]{new InputFilter.LengthFilter(255)});
            dialogBuilder.setPositiveButton(R.string.validate, (dialog, id) -> {
                if (popupAddFollowedTagtBinding.addTag.getText() != null && popupAddFollowedTagtBinding.addTag.getText().toString().trim().length() > 0) {
                    tagVM.follow(BaseMainActivity.currentInstance, BaseMainActivity.currentToken, popupAddFollowedTagtBinding.addTag.getText().toString().trim())
                            .observe(FollowedTagActivity.this, newTag -> {
                                if (tagList == null) {
                                    tagList = new ArrayList<>();
                                }
                                if (followedTagAdapter == null) {
                                    followedTagAdapter = new FollowedTagAdapter(tagList);
                                    followedTagAdapter.actionOnTag = this;
                                    binding.notContent.setVisibility(View.GONE);
                                    binding.recyclerView.setAdapter(followedTagAdapter);
                                    binding.recyclerView.setLayoutManager(new LinearLayoutManager(FollowedTagActivity.this));
                                }
                                if (newTag != null) {
                                    tagList.add(0, newTag);
                                    followedTagAdapter.notifyItemInserted(0);
                                } else {
                                    Toasty.error(FollowedTagActivity.this, getString(R.string.toast_feature_not_supported), Toasty.LENGTH_LONG).show();
                                }
                            });
                    dialog.dismiss();
                } else {
                    popupAddFollowedTagtBinding.addTag.setError(getString(R.string.not_valid_tag_name));
                }

            });
            dialogBuilder.setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
            dialogBuilder.create().show();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (!canGoBack) {
            getMenuInflater().inflate(R.menu.menu_main_followed_tag, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_followed_tag, menu);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            canGoBack = false;
            ThemeHelper.slideViewsToRight(binding.fragmentContainer, binding.recyclerView, () -> {
                if (fragmentMastodonTimeline != null) {
                    fragmentMastodonTimeline.onDestroyView();
                }
            });
            setTitle(R.string.action_lists);
            invalidateOptionsMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void click(Tag tag) {
        this.tag = tag;
        canGoBack = true;
        ThemeHelper.slideViewsToLeft(binding.recyclerView, binding.fragmentContainer, () -> {
            fragmentMastodonTimeline = new FragmentMastodonTimeline();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Helper.ARG_SEARCH_KEYWORD, tag.name);
            bundle.putSerializable(Helper.ARG_TIMELINE_TYPE, Timeline.TimeLineEnum.TAG);
            setTitle(tag.name);
            fragmentMastodonTimeline.setArguments(bundle);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragmentMastodonTimeline);
            fragmentTransaction.commit();
            invalidateOptionsMenu();
        });
    }
}
