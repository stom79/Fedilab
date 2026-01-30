package app.fedilab.android.mastodon.helper;
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

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.LayoutUnifiedEmojiPickerBinding;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.ui.drawer.EmojiAdapter;

public class UnifiedEmojiPicker {

    public interface OnUnicodeEmojiSelected {
        void onEmojiSelected(String unicode);
    }

    public interface OnCustomEmojiSelected {
        void onEmojiSelected(String shortcode, String url, String staticUrl);
    }

    public static void show(@NonNull Context context,
                            @NonNull View rootView,
                            @NonNull EditText editText,
                            @Nullable List<Emoji> customEmojis,
                            @Nullable OnUnicodeEmojiSelected unicodeCallback,
                            @Nullable OnCustomEmojiSelected customCallback) {

        boolean hasCustomEmojis = customEmojis != null && !customEmojis.isEmpty();

        BottomSheetDialog bottomSheet = new BottomSheetDialog(context);
        LayoutUnifiedEmojiPickerBinding binding = LayoutUnifiedEmojiPickerBinding.inflate(LayoutInflater.from(context));
        bottomSheet.setContentView(binding.getRoot());

        binding.emojiPickerView.setOnEmojiPickedListener(emojiViewItem -> {
            if (unicodeCallback != null) {
                unicodeCallback.onEmojiSelected(emojiViewItem.getEmoji());
            } else {
                editText.getText().insert(editText.getSelectionStart(), emojiViewItem.getEmoji());
            }
            bottomSheet.dismiss();
        });

        if (hasCustomEmojis) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.custom_emoji));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.standard_emoji));
            binding.buttonSearch.setVisibility(View.VISIBLE);

            setupCustomEmojiGrid(binding, customEmojis, customCallback, bottomSheet);

            binding.buttonSearch.setOnClickListener(v -> {
                if (binding.searchLayout.getVisibility() == View.VISIBLE) {
                    binding.searchLayout.setVisibility(View.GONE);
                    binding.searchEmoji.setText("");
                    setupCustomEmojiGrid(binding, customEmojis, customCallback, bottomSheet);
                } else {
                    binding.searchLayout.setVisibility(View.VISIBLE);
                    binding.searchEmoji.requestFocus();
                }
            });

            binding.searchEmoji.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim().toLowerCase();
                    if (query.isEmpty()) {
                        setupCustomEmojiGrid(binding, customEmojis, customCallback, bottomSheet);
                    } else {
                        List<Emoji> filtered = new ArrayList<>();
                        for (Emoji emoji : customEmojis) {
                            if (emoji.shortcode != null && emoji.shortcode.toLowerCase().contains(query)) {
                                filtered.add(emoji);
                            }
                        }
                        setupCustomEmojiGrid(binding, filtered, customCallback, bottomSheet);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        binding.gridview.setVisibility(View.VISIBLE);
                        binding.emojiPickerView.setVisibility(View.GONE);
                        binding.buttonSearch.setVisibility(View.VISIBLE);
                    } else {
                        binding.gridview.setVisibility(View.GONE);
                        binding.emojiPickerView.setVisibility(View.VISIBLE);
                        binding.buttonSearch.setVisibility(View.GONE);
                        binding.searchLayout.setVisibility(View.GONE);
                        binding.searchEmoji.setText("");
                    }
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
            binding.buttonSearch.setVisibility(View.GONE);
            binding.gridview.setVisibility(View.GONE);
            binding.emojiPickerView.setVisibility(View.VISIBLE);
        }

        bottomSheet.show();
        bottomSheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheet.getBehavior().setDraggable(false);
    }

    private static void setupCustomEmojiGrid(LayoutUnifiedEmojiPickerBinding binding,
                                              List<Emoji> emojis,
                                              OnCustomEmojiSelected customCallback,
                                              BottomSheetDialog bottomSheet) {
        binding.gridview.setAdapter(new EmojiAdapter(emojis));
        binding.gridview.setOnItemClickListener((parent, view, position, id) -> {
            if (position < emojis.size()) {
                Emoji emoji = emojis.get(position);
                if (customCallback != null) {
                    customCallback.onEmojiSelected(emoji.shortcode, emoji.url, emoji.static_url);
                }
                bottomSheet.dismiss();
            }
        });
    }
}
