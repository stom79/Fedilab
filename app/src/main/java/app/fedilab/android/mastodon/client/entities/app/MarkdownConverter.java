package app.fedilab.android.mastodon.client.entities.app;
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

import android.text.style.URLSpan;

import java.util.List;

public class MarkdownConverter {

    public List<MarkdownItem> markdownItems;

    public MarkdownItem getByPosition(int position) {
        if (markdownItems != null && markdownItems.size() > 0 && position < markdownItems.size()) {
            for (MarkdownItem markdownItem : markdownItems) {
                if (markdownItem.position == position) {
                    return markdownItem;
                }
            }
        }
        return null;
    }

    public static class MarkdownItem {
        public String code;
        public int position;
        public URLSpan urlSpan;
    }
}
