package com.smarteist.autoimageslider.IndicatorView.draw.controller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smarteist.autoimageslider.IndicatorView.animation.type.BaseAnimation;
import com.smarteist.autoimageslider.IndicatorView.animation.type.ColorAnimation;
import com.smarteist.autoimageslider.IndicatorView.animation.type.FillAnimation;
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType;
import com.smarteist.autoimageslider.IndicatorView.animation.type.ScaleAnimation;
import com.smarteist.autoimageslider.IndicatorView.draw.data.Indicator;
import com.smarteist.autoimageslider.IndicatorView.draw.data.Orientation;
import com.smarteist.autoimageslider.IndicatorView.draw.data.RtlMode;
import com.smarteist.autoimageslider.IndicatorView.utils.DensityUtils;
import com.smarteist.autoimageslider.R;

public class AttributeController {

    private final Indicator indicator;

    public AttributeController(@NonNull Indicator indicator) {
        this.indicator = indicator;
    }

    public static RtlMode getRtlMode(int index) {
        switch (index) {
            case 0:
                return RtlMode.On;
            case 1:
                return RtlMode.Off;
            case 2:
                return RtlMode.Auto;
        }

        return RtlMode.Auto;
    }

    public void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PageIndicatorView, 0, 0);
        initCountAttribute(typedArray);
        initColorAttribute(typedArray);
        initAnimationAttribute(typedArray);
        initSizeAttribute(typedArray);
        typedArray.recycle();
    }

    private void initCountAttribute(@NonNull TypedArray typedArray) {
        int viewPagerId = typedArray.getResourceId(R.styleable.PageIndicatorView_piv_viewPager, View.NO_ID);
        boolean autoVisibility = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_autoVisibility, true);
        boolean dynamicCount = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_dynamicCount, false);
        int count = typedArray.getInt(R.styleable.PageIndicatorView_piv_count, Indicator.COUNT_NONE);

        if (count == Indicator.COUNT_NONE) {
            count = Indicator.DEFAULT_COUNT;
        }

        int position = typedArray.getInt(R.styleable.PageIndicatorView_piv_select, 0);
        if (position < 0) {
            position = 0;
        } else if (count > 0 && position > count - 1) {
            position = count - 1;
        }

        indicator.setViewPagerId(viewPagerId);
        indicator.setAutoVisibility(autoVisibility);
        indicator.setDynamicCount(dynamicCount);
        indicator.setCount(count);

        indicator.setSelectedPosition(position);
        indicator.setSelectingPosition(position);
        indicator.setLastSelectedPosition(position);
    }

    private void initColorAttribute(@NonNull TypedArray typedArray) {
        int unselectedColor = typedArray.getColor(R.styleable.PageIndicatorView_piv_unselectedColor, Color.parseColor(ColorAnimation.DEFAULT_UNSELECTED_COLOR));
        int selectedColor = typedArray.getColor(R.styleable.PageIndicatorView_piv_selectedColor, Color.parseColor(ColorAnimation.DEFAULT_SELECTED_COLOR));

        indicator.setUnselectedColor(unselectedColor);
        indicator.setSelectedColor(selectedColor);
    }

    private void initAnimationAttribute(@NonNull TypedArray typedArray) {
        boolean interactiveAnimation = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_interactiveAnimation, false);
        int animationDuration = typedArray.getInt(R.styleable.PageIndicatorView_piv_animationDuration, BaseAnimation.DEFAULT_ANIMATION_TIME);
        if (animationDuration < 0) {
            animationDuration = 0;
        }

        int animIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_animationType, IndicatorAnimationType.NONE.ordinal());
        IndicatorAnimationType animationType = getAnimationType(animIndex);

        int rtlIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_rtl_mode, RtlMode.Off.ordinal());
        RtlMode rtlMode = getRtlMode(rtlIndex);

        indicator.setAnimationDuration(animationDuration);
        indicator.setInteractiveAnimation(interactiveAnimation);
        indicator.setAnimationType(animationType);
        indicator.setRtlMode(rtlMode);
    }

    private void initSizeAttribute(@NonNull TypedArray typedArray) {
        int orientationIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_orientation, Orientation.HORIZONTAL.ordinal());
        Orientation orientation;

        if (orientationIndex == 0) {
            orientation = Orientation.HORIZONTAL;
        } else {
            orientation = Orientation.VERTICAL;
        }

        int radius = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_radius, DensityUtils.dpToPx(Indicator.DEFAULT_RADIUS_DP));
        if (radius < 0) {
            radius = 0;
        }

        int padding = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_padding, DensityUtils.dpToPx(Indicator.DEFAULT_PADDING_DP));
        if (padding < 0) {
            padding = 0;
        }

        float scaleFactor = typedArray.getFloat(R.styleable.PageIndicatorView_piv_scaleFactor, ScaleAnimation.DEFAULT_SCALE_FACTOR);
        if (scaleFactor < ScaleAnimation.MIN_SCALE_FACTOR) {
            scaleFactor = ScaleAnimation.MIN_SCALE_FACTOR;

        } else if (scaleFactor > ScaleAnimation.MAX_SCALE_FACTOR) {
            scaleFactor = ScaleAnimation.MAX_SCALE_FACTOR;
        }

        int stroke = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_strokeWidth, DensityUtils.dpToPx(FillAnimation.DEFAULT_STROKE_DP));
        if (stroke > radius) {
            stroke = radius;
        }

        if (indicator.getAnimationType() != IndicatorAnimationType.FILL) {
            stroke = 0;
        }

        indicator.setRadius(radius);
        indicator.setOrientation(orientation);
        indicator.setPadding(padding);
        indicator.setScaleFactor(scaleFactor);
        indicator.setStroke(stroke);
    }

    private IndicatorAnimationType getAnimationType(int index) {
        switch (index) {
            case 0:
                return IndicatorAnimationType.NONE;
            case 1:
                return IndicatorAnimationType.COLOR;
            case 2:
                return IndicatorAnimationType.SCALE;
            case 3:
                return IndicatorAnimationType.WORM;
            case 4:
                return IndicatorAnimationType.SLIDE;
            case 5:
                return IndicatorAnimationType.FILL;
            case 6:
                return IndicatorAnimationType.THIN_WORM;
            case 7:
                return IndicatorAnimationType.DROP;
            case 8:
                return IndicatorAnimationType.SWAP;
            case 9:
                return IndicatorAnimationType.SCALE_DOWN;
        }

        return IndicatorAnimationType.NONE;
    }

}
