package com.smarteist.autoimageslider.IndicatorView.draw.drawer;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.smarteist.autoimageslider.IndicatorView.animation.data.Value;
import com.smarteist.autoimageslider.IndicatorView.draw.data.Indicator;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.BasicDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.ColorDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.DropDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.FillDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.ScaleDownDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.ScaleDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.SlideDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.SwapDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.ThinWormDrawer;
import com.smarteist.autoimageslider.IndicatorView.draw.drawer.type.WormDrawer;

public class Drawer {

    private final BasicDrawer basicDrawer;
    private final ColorDrawer colorDrawer;
    private final ScaleDrawer scaleDrawer;
    private final WormDrawer wormDrawer;
    private final SlideDrawer slideDrawer;
    private final FillDrawer fillDrawer;
    private final ThinWormDrawer thinWormDrawer;
    private final DropDrawer dropDrawer;
    private final SwapDrawer swapDrawer;
    private final ScaleDownDrawer scaleDownDrawer;

    private int position;
    private int coordinateX;
    private int coordinateY;

    public Drawer(@NonNull Indicator indicator) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        basicDrawer = new BasicDrawer(paint, indicator);
        colorDrawer = new ColorDrawer(paint, indicator);
        scaleDrawer = new ScaleDrawer(paint, indicator);
        wormDrawer = new WormDrawer(paint, indicator);
        slideDrawer = new SlideDrawer(paint, indicator);
        fillDrawer = new FillDrawer(paint, indicator);
        thinWormDrawer = new ThinWormDrawer(paint, indicator);
        dropDrawer = new DropDrawer(paint, indicator);
        swapDrawer = new SwapDrawer(paint, indicator);
        scaleDownDrawer = new ScaleDownDrawer(paint, indicator);
    }

    public void setup(int position, int coordinateX, int coordinateY) {
        this.position = position;
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
    }

    public void drawBasic(@NonNull Canvas canvas, boolean isSelectedItem) {
        if (colorDrawer != null) {
            basicDrawer.draw(canvas, position, isSelectedItem, coordinateX, coordinateY);
        }
    }

    public void drawColor(@NonNull Canvas canvas, @NonNull Value value) {
        if (colorDrawer != null) {
            colorDrawer.draw(canvas, value, position, coordinateX, coordinateY);
        }
    }

    public void drawScale(@NonNull Canvas canvas, @NonNull Value value) {
        if (scaleDrawer != null) {
            scaleDrawer.draw(canvas, value, position, coordinateX, coordinateY);
        }
    }

    public void drawWorm(@NonNull Canvas canvas, @NonNull Value value) {
        if (wormDrawer != null) {
            wormDrawer.draw(canvas, value, coordinateX, coordinateY);
        }
    }

    public void drawSlide(@NonNull Canvas canvas, @NonNull Value value) {
        if (slideDrawer != null) {
            slideDrawer.draw(canvas, value, coordinateX, coordinateY);
        }
    }

    public void drawFill(@NonNull Canvas canvas, @NonNull Value value) {
        if (fillDrawer != null) {
            fillDrawer.draw(canvas, value, position, coordinateX, coordinateY);
        }
    }

    public void drawThinWorm(@NonNull Canvas canvas, @NonNull Value value) {
        if (thinWormDrawer != null) {
            thinWormDrawer.draw(canvas, value, coordinateX, coordinateY);
        }
    }

    public void drawDrop(@NonNull Canvas canvas, @NonNull Value value) {
        if (dropDrawer != null) {
            dropDrawer.draw(canvas, value, coordinateX, coordinateY);
        }
    }

    public void drawSwap(@NonNull Canvas canvas, @NonNull Value value) {
        if (swapDrawer != null) {
            swapDrawer.draw(canvas, value, position, coordinateX, coordinateY);
        }
    }

    public void drawScaleDown(@NonNull Canvas canvas, @NonNull Value value) {
        if (scaleDownDrawer != null) {
            scaleDownDrawer.draw(canvas, value, position, coordinateX, coordinateY);
        }
    }
}
