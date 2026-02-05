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
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        if (scale > 1.3f) {
            scale = 1.3f;
        }
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
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            tv.post(() -> {
                CharSequence text = tv.getText();
                tv.setText(text, android.widget.TextView.BufferType.SPANNABLE);
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

    @Override
    public int getSize(@NonNull Paint paint, CharSequence charSequence, int i, int i1, @Nullable Paint.FontMetricsInt fontMetricsInt) {
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

}