package app.fedilab.android.helper;
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
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.client.mastodon.entities.Status;
import app.fedilab.android.ui.drawer.ComposeAdapter;

public class DividerDecorationSimple extends RecyclerView.ItemDecoration {

    private final Context _mContext;
    private final List<Status> statusList;

    public DividerDecorationSimple(Context context, List<Status> statuses) {
        _mContext = context;
        statusList = statuses;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewAdapterPosition();
        ComposeAdapter composeAdapter = ((ComposeAdapter) parent.getAdapter());
        if (composeAdapter != null && composeAdapter.getItemCount() > position && position >= 0) {
            Status status = composeAdapter.getItem(position);
            if (status != null) {
                int start = (int) Helper.convertDpToPixel(
                        4 * CommentDecorationHelper.getIndentation(status.in_reply_to_id, statusList, 15),
                        _mContext);

                if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    outRect.set(start, 0, 0, 0);
                } else {
                    outRect.set(0, 0, start, 0);
                }
            }
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, RecyclerView parent, @NonNull RecyclerView.State state) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(view);
            ComposeAdapter composeAdapter = ((ComposeAdapter) parent.getAdapter());
            if (composeAdapter != null && composeAdapter.getItemCount() > position && position >= 0) {
                Status status = composeAdapter.getItem(position);

                if (status != null) {
                    int indentation = CommentDecorationHelper.getIndentation(status.in_reply_to_id, statusList, 15);

                    if (indentation > 0) {
                        Paint paint = new Paint();
                        paint.setDither(false);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(Helper.convertDpToPixel(2F, _mContext));
                        paint.setStrokeCap(Paint.Cap.ROUND);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setColor(ResourcesCompat.getColor(_mContext.getResources(), R.color.cyanea_accent, _mContext.getTheme()));
                        if (indentation == 15) {
                            paint.setPathEffect(new DashPathEffect(
                                    new float[]{Helper.convertDpToPixel(3, _mContext), Helper.convertDpToPixel(3, _mContext)},
                                    0));
                        }

                        float startDp = 12 + 4 * (indentation - 1);
                        if (i > 0) startDp = startDp - 6;

                        float endDp = startDp + 4;
                        if (i > 0) endDp = endDp + 4;

                        float startPx = Helper.convertDpToPixel(startDp, _mContext);
                        if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                            startPx = c.getWidth() - startPx;
                        }

                        float topPx = view.getTop() - Helper.convertDpToPixel(12, _mContext);

                        if (i > 0) {
                            View aboveView = parent.getChildAt(i - 1);
                            topPx = topPx - (aboveView.getHeight() / 2F);
                        }

                        float bottomPx = view.getBottom() - view.getHeight() / 2F;
                        float endPx = Helper.convertDpToPixel(endDp, _mContext);

                        Path path = new Path();
                        path.moveTo(startPx, topPx);
                        path.lineTo(startPx, bottomPx);
                        path.lineTo(endPx, bottomPx);

                        c.drawPath(path, paint);
                    }
                }
            }
        }
    }
}
