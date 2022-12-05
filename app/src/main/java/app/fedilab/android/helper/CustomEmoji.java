package app.fedilab.android.helper;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Emoji;
import app.fedilab.android.client.entities.api.Status;


public class CustomEmoji extends ReplacementSpan {
    private final float scale;
    private final WeakReference<View> viewWeakReference;
    private Drawable imageDrawable;
    private boolean callbackCalled;

    CustomEmoji(WeakReference<View> viewWeakReference) {
        Context mContext = viewWeakReference.get().getContext();
        this.viewWeakReference = viewWeakReference;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        callbackCalled = false;
    }

    public SpannableStringBuilder makeEmoji(SpannableStringBuilder content, List<Emoji> emojiList, boolean animate, Status.Callback callback) {
        if (emojiList != null && emojiList.size() > 0) {
            int count = 1;
            for (Emoji emoji : emojiList) {
                Matcher matcher = Pattern.compile(":" + emoji.shortcode + ":", Pattern.LITERAL)
                        .matcher(content);
                while (matcher.find()) {
                    CustomEmoji customEmoji = new CustomEmoji(new WeakReference<>(viewWeakReference.get()));
                    content.setSpan(customEmoji, matcher.start(), matcher.end(), 0);
                    Glide.with(viewWeakReference.get())
                            .asDrawable()
                            .load(animate ? emoji.url : emoji.static_url)
                            .into(customEmoji.getTarget(animate, count == emojiList.size() && !callbackCalled ? callback : null));
                }
                count++;
            }
        }
        return content;
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
            transY -= paint.getFontMetrics().descent / 2;
            canvas.translate(x, (float) transY);
            imageDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public Target<Drawable> getTarget(boolean animate, Status.Callback callback) {
        return new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                View view = viewWeakReference.get();
                if (animate && resource instanceof Animatable) {
                    Drawable.Callback callback = resource.getCallback();
                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable drawable) {
                            if (callback != null) {
                                callback.invalidateDrawable(drawable);
                            }
                            view.invalidate();
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
                            if (callback != null) {
                                callback.scheduleDrawable(drawable, runnable, l);
                            }
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
                            if (callback != null) {
                                callback.unscheduleDrawable(drawable, runnable);
                            }
                        }
                    });
                    ((Animatable) resource).start();
                }
                imageDrawable = resource;
                if (view != null) {
                    view.invalidate();
                }
                if (callback != null) {
                    callbackCalled = true;
                    callback.emojiFetched();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
    }
}