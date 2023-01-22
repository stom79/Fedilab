package app.fedilab.android.peertube.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import org.jetbrains.annotations.NotNull;

import app.fedilab.android.peertube.R;

public class RoundedBackgroundSpan extends ReplacementSpan {

    private final int backgroundColor;
    private final int textColor;

    public RoundedBackgroundSpan(Context context) {
        super();
        backgroundColor = context.getResources().getColor(R.color.colorAccent);
        textColor = context.getResources().getColor(R.color.tag_color_text);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NotNull Paint paint) {
        RectF rect = new RectF(x, top + 2, x + measureText(paint, text, start, end), bottom - 1);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, 8, 8, paint);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x, y, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}
