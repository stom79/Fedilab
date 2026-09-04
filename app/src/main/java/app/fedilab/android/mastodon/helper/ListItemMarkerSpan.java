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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

/**
 * Original work from the Mastodon Android app: org.joinmastodon.android.ui.text.ListItemMarkerSpan
 */
public class ListItemMarkerSpan implements LeadingMarginSpan {

    private static final int MARKER_MARGIN = 32;
    private static final int MARKER_GAP = 8;

    private final String text;
    private final int margin;
    private final int gap;

    public ListItemMarkerSpan(Context context, String text) {
        this.text = text;
        this.margin = (int) Helper.convertDpToPixel(MARKER_MARGIN, context);
        this.gap = (int) Helper.convertDpToPixel(MARKER_GAP, context);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return margin;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        if (text instanceof Spanned spanned && spanned.getSpanStart(this) == start) {
            int level = spanned.getSpans(start, end, LeadingMarginSpan.class).length - 1;
            int textStart = margin * (level + 1);
            if (dir < 0) {
                c.drawText(this.text, layout.getWidth() - textStart + gap, baseline, p);
            } else {
                c.drawText(this.text, x + textStart - gap - p.measureText(this.text), baseline, p);
            }
        }
    }
}