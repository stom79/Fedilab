package app.fedilab.android.mastodon.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;


import app.fedilab.android.R;



public class CustomImageSpan extends ReplacementSpan {
    private float scale;
    private Drawable imageDrawable;

    CustomImageSpan(View view) {
        Context mContext = view.getContext();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        if (scale > 1.3f) {
            scale = 1.3f;
        }
    }


    @Override
    public int getSize(@NonNull Paint paint, CharSequence charSequence, int i, int i1, @Nullable Paint.FontMetricsInt fontMetricsInt) {
        if (fontMetricsInt != null) {
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            fontMetricsInt.top = (int) fontMetrics.top;
            fontMetricsInt.ascent = (int) fontMetrics.ascent;
            fontMetricsInt.descent = (int) fontMetrics.descent;
            fontMetricsInt.bottom = (int) fontMetrics.bottom;
        }
        return (int) (paint.getTextSize() * scale);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence charSequence, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        if (imageDrawable != null) {
            canvas.save();
            int emojiSize = (int) (paint.getTextSize() * scale);
            imageDrawable.setBounds(0, 0, emojiSize, emojiSize);
            int transY = bottom - imageDrawable.getBounds().bottom;
            transY -= (int) (paint.getFontMetrics().descent / 2);
            canvas.translate(x, (float) transY);
            imageDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public Target<Drawable> getTarget(View view, boolean animate) {
        return new CustomTarget<>() {

            @Override
            public void onStart() {
                super.onStart();
                if(imageDrawable instanceof Animatable) {
                    ((Animatable) imageDrawable).start();
                }
            }

            @Override
            public void onStop() {
                super.onStop();
                if(imageDrawable instanceof Animatable) {
                    ((Animatable) imageDrawable).stop();
                }
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                if (animate && resource instanceof Animatable) {
                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable drawable) {
                            view.invalidate();
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
                            view.postDelayed(runnable,l);
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
                            view.removeCallbacks(runnable);
                        }
                    });
                    ((Animatable) resource).start();
                }
                imageDrawable = resource;
                view.invalidate();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
    }
}