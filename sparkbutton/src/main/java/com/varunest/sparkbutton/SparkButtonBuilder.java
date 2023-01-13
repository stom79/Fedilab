package com.varunest.sparkbutton;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.varunest.sparkbutton.helpers.Utils;

/**
 * @author varun on 07/07/16.
 */
public class SparkButtonBuilder {
    private final SparkButton sparkButton;
    private final Context context;

    public SparkButtonBuilder(Context context) {
        this.context = context;
        sparkButton = new SparkButton(context);
    }

    public SparkButtonBuilder setActiveImage(@DrawableRes int resourceId) {
        sparkButton.setActiveImage(resourceId);
        return this;
    }

    public SparkButtonBuilder setInactiveImage(@DrawableRes int resourceId) {
        sparkButton.setInactiveImage(resourceId);
        return this;
    }

    public SparkButtonBuilder setPrimaryColor(@ColorInt int color) {
        sparkButton.setPrimaryColor(color);
        return this;
    }

    public SparkButtonBuilder setSecondaryColor(int color) {
        sparkButton.setSecondaryColor(color);
        return this;
    }

    public SparkButtonBuilder setImageSizePx(int px) {
        sparkButton.setImageSize(px);
        return this;
    }

    public SparkButtonBuilder setImageSizeDp(int dp) {
        sparkButton.setImageSize(Utils.dpToPx(context, dp));
        return this;
    }

    public SparkButtonBuilder setAnimationSpeed(float speed) {
        sparkButton.setAnimationSpeed(speed);
        return this;
    }

    public SparkButton build() {
        sparkButton.init();
        return sparkButton;
    }
}
