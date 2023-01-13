package com.varunest.sparkbutton.helpers;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;


public class SparkAnimationView extends View {
    public static final Property<SparkAnimationView, Float> INNER_CIRCLE_RADIUS_PROGRESS =
            new Property<SparkAnimationView, Float>(Float.class, "innerCircleRadiusProgress") {
                @Override
                public Float get(SparkAnimationView object) {
                    return object.getInnerCircleRadiusProgress();
                }

                @Override
                public void set(SparkAnimationView object, Float value) {
                    object.setInnerCircleRadiusProgress(value);
                }
            };
    private static final int DOTS_COUNT = 12;
    private static final int OUTER_DOTS_POSITION_ANGLE = 360 / DOTS_COUNT;
    private static final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    public static final Property<SparkAnimationView, Float> DOTS_PROGRESS = new Property<SparkAnimationView, Float>(Float.class, "dotsProgress") {
        @Override
        public Float get(SparkAnimationView object) {
            return object.getCurrentProgress();
        }

        @Override
        public void set(SparkAnimationView object, Float value) {
            object.setCurrentProgress(value);
        }
    };
    public static final Property<SparkAnimationView, Float> OUTER_CIRCLE_RADIUS_PROGRESS =
            new Property<SparkAnimationView, Float>(Float.class, "outerCircleRadiusProgress") {
                @Override
                public Float get(SparkAnimationView object) {
                    return object.getOuterCircleRadiusProgress();
                }

                @Override
                public void set(SparkAnimationView object, Float value) {
                    object.setOuterCircleRadiusProgress(value);
                }
            };
    private final Paint[] dotsPaints = new Paint[4];
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int primaryColor = 0xFFFFC107;
    private int primaryColorDark = 0xFFFF9800;
    private int secondaryColor = 0xFFFF5722;
    private int secondaryColorDark = 0xFFF44336;
    private int centerX;
    private int centerY;
    private float maxOuterDotsRadius;
    private float maxInnerDotsRadius;
    private float maxDotSize;
    private float currentProgress = 0;
    private float currentRadius1 = 0;
    private float currentDotSize1 = 0;
    private float currentDotSize2 = 0;
    private float currentRadius2 = 0;
    private float outerCircleRadiusProgress = 0f;
    private float innerCircleRadiusProgress = 0f;
    private float maxCircleSize;

    public SparkAnimationView(Context context) {
        super(context);
        init();
    }

    public SparkAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparkAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SparkAnimationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        maxDotSize = Utils.dpToPx(getContext(), 4);
        for (int i = 0; i < dotsPaints.length; i++) {
            dotsPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2;
        centerY = h / 2;
        maxOuterDotsRadius = w / 2 - maxDotSize * 2;
        maxInnerDotsRadius = 0.8f * maxOuterDotsRadius;
        maxCircleSize = w / 4.3f;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawOuterDotsFrame(canvas);
        drawInnerDotsFrame(canvas);

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, outerCircleRadiusProgress * maxCircleSize, circlePaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerCircleRadiusProgress * (maxCircleSize + 1), maskPaint);
    }

    public void setMaxDotSize(int pxUnits) {
        maxDotSize = pxUnits;
    }

    private void drawOuterDotsFrame(Canvas canvas) {
        for (int i = 0; i < DOTS_COUNT; i++) {
            int cX = (int) (centerX + currentRadius1 * Math.cos(i * OUTER_DOTS_POSITION_ANGLE * Math.PI / 180));
            int cY = (int) (centerY + currentRadius1 * Math.sin(i * OUTER_DOTS_POSITION_ANGLE * Math.PI / 180));
            canvas.drawCircle(cX, cY, currentDotSize1, dotsPaints[i % dotsPaints.length]);
        }
    }

    private void drawInnerDotsFrame(Canvas canvas) {
        for (int i = 0; i < DOTS_COUNT; i++) {
            int cX = (int) (centerX + currentRadius2 * Math.cos((i * OUTER_DOTS_POSITION_ANGLE - 10) * Math.PI / 180));
            int cY = (int) (centerY + currentRadius2 * Math.sin((i * OUTER_DOTS_POSITION_ANGLE - 10) * Math.PI / 180));
            canvas.drawCircle(cX, cY, currentDotSize2, dotsPaints[(i + 1) % dotsPaints.length]);
        }
    }

    public float getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(float currentProgress) {
        this.currentProgress = currentProgress;

        updateInnerDotsPosition();
        updateOuterDotsPosition();
        updateDotsPaints();
        updateDotsAlpha();

        postInvalidate();
    }

    private void updateInnerDotsPosition() {
        if (currentProgress < 0.3f) {
            this.currentRadius2 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0, 0.3f, 0.f, maxInnerDotsRadius);
        } else {
            this.currentRadius2 = maxInnerDotsRadius;
        }

        if (currentProgress < 0.2) {
            this.currentDotSize2 = maxDotSize;
        } else if (currentProgress < 0.5) {
            this.currentDotSize2 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.2f, 0.5f, maxDotSize, 0.3 * maxDotSize);
        } else {
            this.currentDotSize2 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.5f, 1f, maxDotSize * 0.3f, 0);
        }

    }

    private void updateOuterDotsPosition() {
        if (currentProgress < 0.3f) {
            this.currentRadius1 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.0f, 0.3f, 0, maxOuterDotsRadius * 0.8f);
        } else {
            this.currentRadius1 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.3f, 1f, 0.8f * maxOuterDotsRadius, maxOuterDotsRadius);
        }

        if (currentProgress < 0.7) {
            this.currentDotSize1 = maxDotSize;
        } else {
            this.currentDotSize1 = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.7f, 1f, maxDotSize, 0);
        }
    }

    private void updateDotsPaints() {
        if (currentProgress < 0.5f) {
            float progress = (float) Utils.mapValueFromRangeToRange(currentProgress, 0f, 0.5f, 0, 1f);
            dotsPaints[0].setColor((Integer) argbEvaluator.evaluate(progress, primaryColor, primaryColorDark));
            dotsPaints[1].setColor((Integer) argbEvaluator.evaluate(progress, primaryColorDark, secondaryColor));
            dotsPaints[2].setColor((Integer) argbEvaluator.evaluate(progress, secondaryColor, secondaryColorDark));
            dotsPaints[3].setColor((Integer) argbEvaluator.evaluate(progress, secondaryColorDark, primaryColor));
        } else {
            float progress = (float) Utils.mapValueFromRangeToRange(currentProgress, 0.5f, 1f, 0, 1f);
            dotsPaints[0].setColor((Integer) argbEvaluator.evaluate(progress, primaryColorDark, secondaryColor));
            dotsPaints[1].setColor((Integer) argbEvaluator.evaluate(progress, secondaryColor, secondaryColorDark));
            dotsPaints[2].setColor((Integer) argbEvaluator.evaluate(progress, secondaryColorDark, primaryColor));
            dotsPaints[3].setColor((Integer) argbEvaluator.evaluate(progress, primaryColor, primaryColorDark));
        }
    }

    private void updateDotsAlpha() {
        float progress = (float) Utils.clamp(currentProgress, 0.6f, 1f);
        int alpha = (int) Utils.mapValueFromRangeToRange(progress, 0.6f, 1f, 255, 0);
        dotsPaints[0].setAlpha(alpha);
        dotsPaints[1].setAlpha(alpha);
        dotsPaints[2].setAlpha(alpha);
        dotsPaints[3].setAlpha(alpha);
    }

    public void setColors(int primaryColor, int secondaryColor) {
        this.primaryColor = primaryColor;
        this.primaryColorDark = Utils.darkenColor(primaryColor, 1.1f);
        this.secondaryColor = secondaryColor;
        this.secondaryColorDark = Utils.darkenColor(secondaryColor, 1.1f);
    }

    public float getInnerCircleRadiusProgress() {
        return innerCircleRadiusProgress;
    }

    public void setInnerCircleRadiusProgress(float innerCircleRadiusProgress) {
        this.innerCircleRadiusProgress = innerCircleRadiusProgress;
        postInvalidate();
    }

    private void updateCircleColor() {
        float colorProgress = (float) Utils.clamp(outerCircleRadiusProgress, 0.5, 1);
        colorProgress = (float) Utils.mapValueFromRangeToRange(colorProgress, 0.5f, 1f, 0f, 1f);
        this.circlePaint.setColor((Integer) argbEvaluator.evaluate(colorProgress, primaryColor, secondaryColor));
    }

    public float getOuterCircleRadiusProgress() {
        return outerCircleRadiusProgress;
    }

    public void setOuterCircleRadiusProgress(float outerCircleRadiusProgress) {
        this.outerCircleRadiusProgress = outerCircleRadiusProgress;
        updateCircleColor();
        postInvalidate();
    }
}