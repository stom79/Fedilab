package com.varunest.sparkbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.varunest.sparkbutton.helpers.SparkAnimationView;
import com.varunest.sparkbutton.helpers.Utils;

/**
 * @author varun 7th July 2016
 */
public class SparkButton extends FrameLayout implements View.OnClickListener {
    private static final DecelerateInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    private static final int INVALID_RESOURCE_ID = -1;
    private static final float ANIMATIONVIEW_SIZE_FACTOR = 3;
    private static final float DOTS_SIZE_FACTOR = .08f;
    int activeImageTint;
    int inActiveImageTint;
    private @DrawableRes
    int imageResourceIdActive = INVALID_RESOURCE_ID;
    private @DrawableRes
    int imageResourceIdInactive = INVALID_RESOURCE_ID;
    private @Px
    int imageSize;
    private @ColorInt
    int primaryColor;
    private @ColorInt
    int secondaryColor;
    private SparkAnimationView sparkAnimationView;
    private ImageView imageView;
    private float animationSpeed = 1;
    private boolean isChecked = false;
    private AnimatorSet animatorSet;
    private final Context context;

    SparkButton(Context context) {
        super(context);
        this.context = context;
    }

    public SparkButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromXML(attrs);
        this.context = context;
        init();
    }

    public SparkButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromXML(attrs);
        this.context = context;
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SparkButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromXML(attrs);
        this.context = context;
        init();
    }


    void init() {
        int animationViewSize = (int) (imageSize * ANIMATIONVIEW_SIZE_FACTOR);

        sparkAnimationView = new SparkAnimationView(getContext());
        LayoutParams dotsViewLayoutParams = new LayoutParams(animationViewSize, animationViewSize, Gravity.CENTER);
        sparkAnimationView.setLayoutParams(dotsViewLayoutParams);

        sparkAnimationView.setColors(secondaryColor, primaryColor);
        sparkAnimationView.setMaxDotSize((int) (imageSize * DOTS_SIZE_FACTOR));

        addView(sparkAnimationView);

        imageView = new AppCompatImageView(getContext());
        LayoutParams imageViewLayoutParams = new LayoutParams(imageSize, imageSize, Gravity.CENTER);
        imageView.setLayoutParams(imageViewLayoutParams);

        addView(imageView);

        if (imageResourceIdInactive != INVALID_RESOURCE_ID) {
            // should load inactive img first
            imageView.setImageResource(imageResourceIdInactive);
        } else if (imageResourceIdActive != INVALID_RESOURCE_ID) {
            imageView.setImageResource(imageResourceIdActive);
        } else {
            throw new IllegalArgumentException("One of Inactive/Active Image Resources is required!");
        }
      //  setOnTouchListener();
        setOnClickListener(this);
    }

    /**
     * Call this function to start spark animation
     */
    public void playAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
        }

        imageView.animate().cancel();
        imageView.setScaleX(0);
        imageView.setScaleY(0);
        sparkAnimationView.setInnerCircleRadiusProgress(0);
        sparkAnimationView.setOuterCircleRadiusProgress(0);
        sparkAnimationView.setCurrentProgress(0);

        animatorSet = new AnimatorSet();

        ObjectAnimator outerCircleAnimator = ObjectAnimator.ofFloat(sparkAnimationView, SparkAnimationView.OUTER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        outerCircleAnimator.setDuration((long) (250 / animationSpeed));
        outerCircleAnimator.setInterpolator(DECELERATE_INTERPOLATOR);

        ObjectAnimator innerCircleAnimator = ObjectAnimator.ofFloat(sparkAnimationView, SparkAnimationView.INNER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        innerCircleAnimator.setDuration((long) (200 / animationSpeed));
        innerCircleAnimator.setStartDelay((long) (200 / animationSpeed));
        innerCircleAnimator.setInterpolator(DECELERATE_INTERPOLATOR);

        ObjectAnimator starScaleYAnimator = ObjectAnimator.ofFloat(imageView, ImageView.SCALE_Y, 0.2f, 1f);
        starScaleYAnimator.setDuration((long) (350 / animationSpeed));
        starScaleYAnimator.setStartDelay((long) (250 / animationSpeed));
        starScaleYAnimator.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator starScaleXAnimator = ObjectAnimator.ofFloat(imageView, ImageView.SCALE_X, 0.2f, 1f);
        starScaleXAnimator.setDuration((long) (350 / animationSpeed));
        starScaleXAnimator.setStartDelay((long) (250 / animationSpeed));
        starScaleXAnimator.setInterpolator(OVERSHOOT_INTERPOLATOR);

        ObjectAnimator dotsAnimator = ObjectAnimator.ofFloat(sparkAnimationView, SparkAnimationView.DOTS_PROGRESS, 0, 1f);
        dotsAnimator.setDuration((long) (900 / animationSpeed));
        dotsAnimator.setStartDelay((long) (50 / animationSpeed));
        dotsAnimator.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);

        animatorSet.playTogether(
                outerCircleAnimator,
                innerCircleAnimator,
                starScaleYAnimator,
                starScaleXAnimator,
                dotsAnimator
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                sparkAnimationView.setInnerCircleRadiusProgress(0);
                sparkAnimationView.setOuterCircleRadiusProgress(0);
                sparkAnimationView.setCurrentProgress(0);
                imageView.setScaleX(1);
                imageView.setScaleY(1);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });

        animatorSet.start();
    }


    public @Px
    int getImageSize() {
        return imageSize;
    }

    public void setImageSize(@Px int imageSize) {
        this.imageSize = imageSize;
        if (imageView != null) {
            imageView.getLayoutParams().width = imageSize;
            imageView.getLayoutParams().height = imageSize;
            imageView.requestLayout();
        }
    }

    public @ColorInt
    int getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(@ColorInt int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public @ColorInt
    int getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(@ColorInt int secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    /**
     * @return Returns whether the button is checked (Active) or not.
     */
    public boolean isChecked() {
        return isChecked;
    }


    public void setInActiveImageTint(int inActiveImageTint) {
        this.inActiveImageTint = getColor(inActiveImageTint);
    }

    public void setInActiveImageTintColor(int inActiveImageTint) {
        this.inActiveImageTint = inActiveImageTint;
    }

    public void setActiveImageTint(int activeImageTint) {
        this.activeImageTint = getColor(activeImageTint);
    }

    public void setActiveImageTintColor(int activeImageTint) {
        this.activeImageTint = activeImageTint;
    }


    /**
     * Change Button State (Works only if both active and disabled image resource is defined)
     *
     * @param flag desired checked state of the button
     */
    public void setChecked(boolean flag) {
        isChecked = flag;
        imageView.setImageResource(isChecked ? imageResourceIdActive : imageResourceIdInactive);
        imageView.setColorFilter(isChecked ? activeImageTint : inActiveImageTint, PorterDuff.Mode.SRC_ATOP);
    }

    public void setInactiveImage(int inactiveResource) {
        this.imageResourceIdInactive = inactiveResource;
        imageView.setImageResource(isChecked ? imageResourceIdActive : imageResourceIdInactive);
    }

    public void setActiveImage(int activeResource) {
        this.imageResourceIdActive = activeResource;
        imageView.setImageResource(isChecked ? imageResourceIdActive : imageResourceIdInactive);
    }


    @Override
    public void onClick(View v) {
        if (imageResourceIdInactive != INVALID_RESOURCE_ID) {
            isChecked = !isChecked;

            imageView.setImageResource(isChecked ? imageResourceIdActive : imageResourceIdInactive);

            if (animatorSet != null) {
                animatorSet.cancel();
            }
            if (isChecked) {
                sparkAnimationView.setVisibility(VISIBLE);
                playAnimation();
            } else {
                sparkAnimationView.setVisibility(INVISIBLE);
            }
        } else {
            playAnimation();
        }
    }


    private int getColor(int id) {
        return ContextCompat.getColor(getContext(), id);
    }


    private void initFromXML(AttributeSet attr) {
        TypedArray a = getContext().obtainStyledAttributes(attr, R.styleable.SparkButton);
        imageSize = a.getDimensionPixelOffset(R.styleable.SparkButton_iconSize, Utils.dpToPx(getContext(), 50));
        imageResourceIdActive = a.getResourceId(R.styleable.SparkButton_activeImage, INVALID_RESOURCE_ID);
        imageResourceIdInactive = a.getResourceId(R.styleable.SparkButton_inactiveImage, INVALID_RESOURCE_ID);
        primaryColor = ContextCompat.getColor(getContext(), a.getResourceId(R.styleable.SparkButton_primaryColor, R.color.spark_primary_color));
        secondaryColor = ContextCompat.getColor(getContext(), a.getResourceId(R.styleable.SparkButton_secondaryColor, R.color.spark_secondary_color));
        animationSpeed = a.getFloat(R.styleable.SparkButton_animationSpeed, 1);
        // recycle typedArray
        a.recycle();
    }


}
