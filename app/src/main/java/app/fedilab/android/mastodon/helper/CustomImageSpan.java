package app.fedilab.android.mastodon.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.ReplacementSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.api.Emoji;



public class CustomImageSpan extends ReplacementSpan {
    private final WeakReference<View> viewWeakReference;
    private float scale;
    private Drawable imageDrawable;

    CustomImageSpan(WeakReference<View> viewWeakReference) {
        Context mContext = viewWeakReference.get().getContext();
        this.viewWeakReference = viewWeakReference;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        if (scale > 1.3f) {
            scale = 1.3f;
        }
    }

    public void makeEmoji(SpannableStringBuilder content, Emoji emoji, boolean animate) {
        if (emoji != null) {
            Matcher matcher = Pattern.compile(":" + emoji.shortcode + ":", Pattern.LITERAL)
                    .matcher(content);
            while (matcher.find()) {
                CustomImageSpan customEmoji = new CustomImageSpan(new WeakReference<>(viewWeakReference.get()));
                content.setSpan(customEmoji, matcher.start(), matcher.end(), 0);
                Glide.with(viewWeakReference.get())
                        .asDrawable()
                        .load(animate ? emoji.url : emoji.static_url)
                        .into(customEmoji.getTarget(animate));
            }
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

    public Target<Drawable> getTarget(boolean animate) {
        return new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                View view = viewWeakReference.get();

                if (animate && resource instanceof Animatable) {
                    Drawable.Callback drawableCallBack = resource.getCallback();
                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable drawable) {
                            if (drawableCallBack != null) {
                                drawableCallBack.invalidateDrawable(drawable);
                            }
                            if(view != null) {
                                view.invalidate();
                            }
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
                            if (drawableCallBack != null) {
                                drawableCallBack.scheduleDrawable(drawable, runnable, l);
                            }
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
                            if (drawableCallBack != null) {
                                drawableCallBack.unscheduleDrawable(drawable, runnable);
                            }
                        }
                    });
                    ((Animatable) resource).start();
                }
                imageDrawable = resource;
                if (view != null) {
                    view.invalidate();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
    }
}