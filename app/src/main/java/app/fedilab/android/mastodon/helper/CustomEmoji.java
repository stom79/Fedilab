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
    private boolean callbackCalled;

    CustomEmoji(WeakReference<View> viewWeakReference) {
        Context mContext = viewWeakReference.get().getContext();
        this.viewWeakReference = viewWeakReference;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        scale = sharedpreferences.getFloat(mContext.getString(R.string.SET_FONT_SCALE), 1.1f);
        if (scale > 1.3f) {
            scale = 1.3f;
        }
        callbackCalled = false;
    }

    public SpannableStringBuilder makeEmoji(SpannableStringBuilder content, List<Emoji> emojiList, boolean animate, Status.Callback callback) {
        if (emojiList != null && !emojiList.isEmpty()) {
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

    public Target<Drawable> getTarget(boolean animate, Status.Callback callback) {
        return new CustomTarget<>() {

            @Override
            public void onStart() {
                if (imageDrawable instanceof Animatable) {
                    ((Animatable) imageDrawable).start();
                }
            }

            @Override
            public void onStop() {
                if (imageDrawable instanceof Animatable) {
                    ((Animatable) imageDrawable).stop();
                }
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                View view = viewWeakReference.get();

                if (animate && resource instanceof Animatable) {

                    resource.setCallback(new Drawable.Callback() {
                        @Override
                        public void invalidateDrawable(@NonNull Drawable drawable) {
                            if(view != null) {
                                view.invalidate();
                            }
                        }

                        @Override
                        public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long l) {
                            view.postDelayed(runnable, l);
                        }

                        @Override
                        public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
                            view.removeCallbacks(runnable);
                        }
                    });
                    ((Animatable) resource).start();
                }
                imageDrawable = resource;
                if(view != null) {
                    view.invalidate();
                }
                if (callback != null && !callbackCalled) {
                    callbackCalled = true;
                    callback.emojiFetched();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                View view = viewWeakReference.get();
                if (imageDrawable != null) {
                    if (imageDrawable instanceof Animatable) {
                        ((Animatable) imageDrawable).stop();
                        imageDrawable.setCallback(null);
                    }
                }
                imageDrawable = null;
                if(view != null) {
                    view.invalidate();
                }
            }
        };
    }
}