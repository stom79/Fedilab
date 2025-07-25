package app.fedilab.android.mastodon.helper;
/* Copyright 2021 Thomas Schneider
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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.api.Status;
import app.fedilab.android.mastodon.ui.drawer.StatusAdapter;

public class DividerDecoration extends RecyclerView.ItemDecoration {

    private final Context _mContext;
    private final List<Status> statusList;
    private final float fontScale;
    private final int indentationMax;
    private final List<Integer> colorList = Arrays.asList(
            R.color.decoration_1,
            R.color.decoration_2,
            R.color.decoration_3,
            R.color.decoration_4,
            R.color.decoration_5,
            R.color.decoration_6,
            R.color.decoration_7,
            R.color.decoration_8,
            R.color.decoration_9,
            R.color.decoration_10,
            R.color.decoration_11,
            R.color.decoration_12
    );

    public DividerDecoration(Context context, List<Status> statuses) {
        _mContext = context;
        statusList = statuses;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_mContext);
        fontScale = prefs.getFloat(_mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        indentationMax = prefs.getInt(_mContext.getString(R.string.SET_MAX_INDENTATION), 5);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewAdapterPosition();
        StatusAdapter statusAdapter = ((StatusAdapter) parent.getAdapter());
        if (statusAdapter != null && statusAdapter.getItemCount() > position && position > 0) {
            Status status = statusAdapter.getItem(position);
            int start = (int) Helper.convertDpToPixel(
                    6 * fontScale * CommentDecorationHelper.getIndentation(status.in_reply_to_id, statusList, indentationMax),
                    _mContext);

            if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                outRect.set(start, 0, 0, 0);
            } else {
                outRect.set(0, 0, start, 0);
            }
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, RecyclerView parent, @NonNull RecyclerView.State state) {
        int margin = (int) Helper.convertDpToPixel(12, _mContext);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(view);
            StatusAdapter statusAdapter = ((StatusAdapter) parent.getAdapter());
            if (statusAdapter != null && position >= 0) {
                Status status = statusAdapter.getItem(position);

                int indentation = Math.min(
                        CommentDecorationHelper.getIndentation(status.in_reply_to_id, statusList, indentationMax),
                        indentationMax);
                if (indentation > 0) {
                    Paint paint = new Paint();
                    paint.setDither(false);
                    paint.setStrokeWidth(Helper.convertDpToPixel(1.5F, _mContext));
                    paint.setStrokeCap(Paint.Cap.BUTT);
                    paint.setStrokeJoin(Paint.Join.MITER);

                    for (int j = 0; j < indentation; j++) {
                        float startPx = Helper.convertDpToPixel(6 * fontScale + 6 * fontScale * j, _mContext);
                        if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)
                            startPx = c.getWidth() - startPx;

                        float bottomPx = view.getBottom();
                        int color = colorList.get(j%(colorList.size()-1));
                        paint.setColor(ResourcesCompat.getColor(_mContext.getResources(), color, _mContext.getTheme()));
                        if (j == indentationMax - 1) {
                            paint.setPathEffect(new DashPathEffect(
                                    new float[]{Helper.convertDpToPixel(3, _mContext), Helper.convertDpToPixel(3, _mContext)},
                                    0));
                            bottomPx = bottomPx - view.getHeight() / 2F;
                        }

                        c.drawLine(startPx, view.getTop() - margin, startPx, bottomPx, paint);
                    }
                    int color = colorList.get((indentation-1)%colorList.size());
                    paint.setColor(ResourcesCompat.getColor(_mContext.getResources(), color, _mContext.getTheme()));

                    float startDp = 6 * fontScale * (indentation - 1) + 6 * fontScale;
                    float centerPx = view.getBottom() - view.getHeight() / 2F;
                    float endDp = startDp + 12 * fontScale;
                    float endPx = Helper.convertDpToPixel(endDp, _mContext);

                    float startPx = Helper.convertDpToPixel(startDp, _mContext);
                    if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                        startPx = c.getWidth() - startPx;
                        endPx = c.getWidth() - endPx;
                    }

                    if (i > 0) {
                        View aboveView = parent.getChildAt(i - 1);
                        float aboveViewLineTopPx = (aboveView.getTop() + aboveView.getHeight() / 2F) + Helper.convertDpToPixel(0.75F, _mContext);
                        float aboveViewLineBottomPx = parent.getChildAt(i - 1).getBottom();
                        c.drawLine(startPx, aboveViewLineTopPx, startPx, aboveViewLineBottomPx, paint);
                    }

                    c.drawLine(startPx, centerPx, endPx, centerPx, paint);
                }
            }
        }
    }
}
