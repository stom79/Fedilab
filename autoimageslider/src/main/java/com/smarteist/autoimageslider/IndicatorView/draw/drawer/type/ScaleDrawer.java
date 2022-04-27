package com.smarteist.autoimageslider.IndicatorView.draw.drawer.type;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.smarteist.autoimageslider.IndicatorView.animation.data.Value;
import com.smarteist.autoimageslider.IndicatorView.animation.data.type.ScaleAnimationValue;
import com.smarteist.autoimageslider.IndicatorView.draw.data.Indicator;

public class ScaleDrawer extends BaseDrawer {

    public ScaleDrawer(@NonNull Paint paint, @NonNull Indicator indicator) {
        super(paint, indicator);
    }

    public void draw(
            @NonNull Canvas canvas,
            @NonNull Value value,
            int position,
            int coordinateX,
            int coordinateY) {

        if (!(value instanceof ScaleAnimationValue)) {
            return;
        }

        ScaleAnimationValue v = (ScaleAnimationValue) value;
        float radius = indicator.getRadius();
        int color = indicator.getSelectedColor();

        int selectedPosition = indicator.getSelectedPosition();
        int selectingPosition = indicator.getSelectingPosition();
        int lastSelectedPosition = indicator.getLastSelectedPosition();

        if (indicator.isInteractiveAnimation()) {
            if (position == selectingPosition) {
                radius = v.getRadius();
                color = v.getColor();

            } else if (position == selectedPosition) {
                radius = v.getRadiusReverse();
                color = v.getColorReverse();
            }

        } else {
            if (position == selectedPosition) {
                radius = v.getRadius();
                color = v.getColor();

            } else if (position == lastSelectedPosition) {
                radius = v.getRadiusReverse();
                color = v.getColorReverse();
            }
        }

        paint.setColor(color);
        canvas.drawCircle(coordinateX, coordinateY, radius, paint);
    }
}
