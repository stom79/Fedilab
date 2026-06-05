package app.fedilab.android.mastodon.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.SpannableStringBuilder;

import com.github.penfeizhou.animation.FrameAnimationDrawable;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;


import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.api.Emoji;
import app.fedilab.android.mastodon.client.entities.api.Status;


public class CustomEmoji extends ReplacementSpan {
    private final WeakReference<View> viewWeakReference;
    private float scale;
    private Drawable imageDrawable;
    private Drawable.Callback drawableCallback;
    private boolean callbackCalled;
    private boolean loadFailed;

    CustomEmoji(WeakReference<View> viewWeakReference) {
        Context mContext = viewWeakReference.get().getContext();
        this.viewWeakReference = viewWeakReference;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_EMOJI_SCALE), 1.1f);
        callbackCalled = false;
        loadFailed = false;
    }

    public SpannableStringBuilder makeEmoji(SpannableStringBuilder content, List<Emoji> emojiList, boolean animate, Status.Callback callback) {
        View view = viewWeakReference.get();
        if (view == null || emojiList == null || emojiList.isEmpty()) {
            return content;
        }
        int count = 1;
        for (Emoji emoji : emojiList) {
            Matcher matcher = Pattern.compile(":" + emoji.shortcode + ":", Pattern.LITERAL)
                    .matcher(content);
            while (matcher.find()) {
                CustomEmoji customEmoji = new CustomEmoji(new WeakReference<>(view));
                content.setSpan(customEmoji, matcher.start(), matcher.end(), 0);
                String emojiUrl = animate ? emoji.url : emoji.static_url;
                boolean isLastEmoji = count == emojiList.size() && !callbackCalled;
                customEmoji.loadEmoji(view, emojiUrl, animate, isLastEmoji ? callback : null);
            }
            count++;
        }
        return content;
    }

    private void onEmojiLoaded(Drawable resource, boolean animate, Status.Callback callback) {
        View view = viewWeakReference.get();

        if (animate && resource instanceof Animatable) {
            drawableCallback = new Drawable.Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable drawable) {
                    if (view != null) {
                        view.postInvalidate();
                    }
                }

                @Override
                public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
                    if (view != null) {
                        view.postDelayed(runnable, l);
                    }
                }

                @Override
                public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
                    if (view != null) {
                        view.removeCallbacks(runnable);
                    }
                }
            };
            resource.setCallback(drawableCallback);
            ((Animatable) resource).start();
        }
        imageDrawable = resource;
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.post(() -> {
                CharSequence text = tv.getText();
                tv.setText(text, TextView.BufferType.SPANNABLE);
            });
        } else if (view != null) {
            view.post(() -> {
                view.invalidate();
                view.requestLayout();
            });
        }
        if (callback != null && !callbackCalled) {
            callbackCalled = true;
            callback.emojiFetched();
        }
    }

    private void onEmojiLoadFailed() {
        loadFailed = true;
        View view = viewWeakReference.get();
        if (view != null) {
            view.post(() -> {
                view.invalidate();
                view.requestLayout();
            });
        }
    }

    public void loadEmoji(View view, String url, boolean animate, Status.Callback callback) {
        EmojiLoader.loadEmojiSpan(view, url, animate, new EmojiLoader.DrawableCallback() {
            @Override
            public void onLoaded(Drawable drawable, boolean shouldAnimate) {
                onEmojiLoaded(drawable, shouldAnimate, callback);
            }

            @Override
            public void onFailed() {
                onEmojiLoadFailed();
            }
        });
    }

    public static void stopAnimations(TextView textView) {
        if (textView == null) {
            return;
        }
        CharSequence text = textView.getText();
        if (text instanceof Spanned) {
            CustomEmoji[] spans = ((Spanned) text).getSpans(0, text.length(), CustomEmoji.class);
            for (CustomEmoji span : spans) {
                if (span.imageDrawable instanceof FrameAnimationDrawable) {
                    ((FrameAnimationDrawable<?>) span.imageDrawable).pause();
                } else if (span.imageDrawable instanceof Animatable && ((Animatable) span.imageDrawable).isRunning()) {
                    ((Animatable) span.imageDrawable).stop();
                }
            }
        }
    }

    public static void startAnimations(TextView textView) {
        if (textView == null) {
            return;
        }
        CharSequence text = textView.getText();
        if (text instanceof Spanned) {
            CustomEmoji[] spans = ((Spanned) text).getSpans(0, text.length(), CustomEmoji.class);
            for (CustomEmoji span : spans) {
                if (span.imageDrawable instanceof FrameAnimationDrawable) {
                    FrameAnimationDrawable<?> drawable = (FrameAnimationDrawable<?>) span.imageDrawable;
                    if (drawable.isPaused()) {
                        drawable.resume();
                    }
                } else if (span.imageDrawable instanceof Animatable && !((Animatable) span.imageDrawable).isRunning()) {
                    span.imageDrawable.setCallback(span.drawableCallback);
                    ((Animatable) span.imageDrawable).start();
                }
            }
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence charSequence, int i, int i1, @Nullable Paint.FontMetricsInt fontMetricsInt) {
        if (imageDrawable == null && loadFailed) {
            return (int) paint.measureText(charSequence, i, i1);
        }
        int emojiSize = (int) (paint.getTextSize() * scale);
        if (fontMetricsInt != null) {
            int defaultHeight = fontMetricsInt.descent - fontMetricsInt.ascent;
            if (emojiSize > defaultHeight) {
                fontMetricsInt.ascent = fontMetricsInt.descent - emojiSize;
                fontMetricsInt.top = fontMetricsInt.ascent;
            }
        }
        return emojiSize;
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
        } else if (loadFailed) {
            canvas.drawText(charSequence, start, end, x, y, paint);
        }
    }

}