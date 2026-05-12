package app.fedilab.android.mastodon.helper;
/* Copyright 2023 Thomas Schneider
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


import static app.fedilab.android.mastodon.helper.Helper.mentionPatternALL;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.ui.drawer.EmojiSearchAdapter;


public class ComposeHelper {

    private static final int SEARCH_DEEP = 15;
    private static final Pattern EMOJI_PATTERN = Pattern.compile("^(.|\\s)*(:([\\w_]+))$");

    public static void setupEmojiAutocomplete(AutoCompleteTextView field, List<Emoji> emojisList, Context context) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (emojisList == null || emojisList.isEmpty()) {
                    return;
                }
                int cursor = field.getSelectionStart();
                if (cursor <= 0) {
                    return;
                }
                String textBeforeCursor = s.toString().substring(0, cursor);
                Matcher matcherEmoji = EMOJI_PATTERN.matcher(textBeforeCursor);
                if (matcherEmoji.matches()) {
                    String shortcode = matcherEmoji.group(3);
                    if (shortcode == null || shortcode.isEmpty()) {
                        return;
                    }
                    new Thread(() -> {
                        List<Emoji> emojisToDisplay = new ArrayList<>();
                        for (Emoji emoji : emojisList) {
                            if (emoji.shortcode.toLowerCase().contains(shortcode.toLowerCase())) {
                                emojisToDisplay.add(emoji);
                                if (emojisToDisplay.size() >= 10) {
                                    break;
                                }
                            }
                        }
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            int currentCursorPosition = field.getSelectionStart();
                            EmojiSearchAdapter emojisSearchAdapter = new EmojiSearchAdapter(context, emojisToDisplay);
                            field.setThreshold(1);
                            field.setAdapter(emojisSearchAdapter);
                            String oldContent = field.getText().toString();
                            String[] searchA = oldContent.substring(0, currentCursorPosition).split(":");
                            if (searchA.length > 0) {
                                String search = searchA[searchA.length - 1];
                                field.setOnItemClickListener((parent, view, position, id) -> {
                                    String shortcodeSelected = emojisToDisplay.get(position).shortcode;
                                    String deltaSearch = "";
                                    int searchLength = Math.min(currentCursorPosition, SEARCH_DEEP);
                                    if (currentCursorPosition - searchLength > 0 && currentCursorPosition < oldContent.length()) {
                                        deltaSearch = oldContent.substring(currentCursorPosition - searchLength, currentCursorPosition);
                                    } else if (currentCursorPosition >= oldContent.length()) {
                                        deltaSearch = oldContent.substring(currentCursorPosition - searchLength);
                                    }
                                    if (!search.isEmpty()) {
                                        deltaSearch = deltaSearch.replace(":" + search, "");
                                    }
                                    String newContent = oldContent.substring(0, currentCursorPosition - searchLength);
                                    newContent += deltaSearch;
                                    newContent += ":" + shortcodeSelected + ": ";
                                    int newPosition = newContent.length();
                                    if (currentCursorPosition < oldContent.length()) {
                                        newContent += oldContent.substring(currentCursorPosition);
                                    }
                                    field.setText(newContent);
                                    field.setSelection(newPosition);
                                    field.setAdapter(new EmojiSearchAdapter(context, new ArrayList<>()));
                                });
                            }
                        });
                    }).start();
                } else {
                    field.dismissDropDown();
                }
            }
        });
    }


    /**
     * Allows to split the toot by dot "." for sentences - adds number at the end automatically
     *
     * @param content  String initial content
     * @param maxChars int the max chars per toot
     * @return ArrayList<String> split toot
     */
    public static ArrayList<String> splitToots(String content, int maxChars) {
        String[] splitContent = content.split("\\s");


        ArrayList<String> mentions = new ArrayList<>();
        int mentionLength;
        StringBuilder mentionString = new StringBuilder();
        Matcher matcher = mentionPatternALL.matcher(content);
        while (matcher.find()) {
            String mentionLong = matcher.group(1);
            if (mentionLong != null) {
                if (!mentions.contains(mentionLong)) {
                    mentions.add(mentionLong);
                }
            }
            String mentionShort = matcher.group(2);
            if (mentionShort != null) {
                if (!mentions.contains(mentionShort)) {
                    mentions.add(mentionShort);
                }
            }
        }
        for (String mention : mentions) {
            mentionString.append(mention).append(" ");
        }
        mentionLength = countLength(mentionString.toString()) + 1;
        int maxCharsPerMessage = maxChars - mentionLength;
        int totalCurrent = 0;
        ArrayList<String> reply = new ArrayList<>();
        int index = 0;
        for (String s : splitContent) {
            if ((totalCurrent + s.length() + 1) < maxCharsPerMessage) {
                totalCurrent += (s.length() + 1);
            } else {
                if (content.length() > totalCurrent && totalCurrent > 0) {
                    String tempContent = content.substring(0, (totalCurrent));
                    content = content.substring(totalCurrent);

                    reply.add(index, tempContent);
                    index++;
                    totalCurrent = s.length() + 1;
                }
            }
        }
        if (totalCurrent > 0) {
            reply.add(index, content);
        }
        if (reply.size() > 1) {
            int i = 0;
            for (String r : reply) {
                if (mentions.size() > 0) {
                    String tmpMention = mentionString.toString();
                    for (String mention : mentions) {
                        if (r.contains(mention)) {
                            tmpMention = tmpMention.replace(mention, "");
                        }
                    }
                    reply.set(i, r + " " + tmpMention);
                } else {
                    reply.set(i, r);
                }
                i++;
            }
        }
        return reply;
    }


    /***
     * Returns the length used when composing a toot
     * @param mentions String containing mentions
     * @return int - characters used
     */
    public static int countLength(String mentions) {
        String contentCount = mentions;
        contentCount = contentCount.replaceAll("(?i)(^|[^/\\w])@(([a-z0-9_]+)@[a-z0-9.-]+[a-z0-9]+)", "$1@$3");
        Matcher matcherALink = Patterns.WEB_URL.matcher(contentCount);
        while (matcherALink.find()) {
            final String url = matcherALink.group(1);
            if (url != null) {
                contentCount = contentCount.replace(url, "abcdefghijklmnopkrstuvw");
            }
        }
        return contentCount.length();
    }
}
