package app.fedilab.android.helper;
/* Copyright 2022 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.penfeizhou.animation.apng.APNGDrawable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.client.entities.api.Emoji;

public class CustomEmoji {


    public static void displayEmoji(Context context, List<Emoji> emojis, Spannable content, View view, String id, Callback listener) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        boolean animate = !sharedpreferences.getBoolean(view.getContext().getString(R.string.SET_DISABLE_ANIMATED_EMOJI), false);
        int count = 1;
        for (Emoji emoji : emojis) {
            int finalCount = count;
            Glide.with(view.getContext())
                    .asDrawable()
                    .load(animate ? emoji.url : emoji.static_url)
                    .into(
                            new CustomTarget<Drawable>() {
                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    if (finalCount == emojis.size()) {
                                        listener.allEmojisfound(id);
                                    }
                                }

                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    Matcher matcher = Pattern.compile(":" + emoji.shortcode + ":", Pattern.LITERAL)
                                            .matcher(content);
                                    while (matcher.find()) {
                                        ImageSpan imageSpan;
                                        resource.setBounds(0, 0, (int) Helper.convertDpToPixel(20, context), (int) Helper.convertDpToPixel(20, context));
                                        resource.setVisible(true, true);
                                        imageSpan = new ImageSpan(resource);
                                        content.setSpan(
                                                imageSpan, matcher.start(),
                                                matcher.end(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                    }
                                    if (animate && resource instanceof APNGDrawable) {
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
                                        ((APNGDrawable) resource).start();

                                    }
                                    if (finalCount == emojis.size()) {
                                        listener.allEmojisfound(id);
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            }
                    );
            count++;
        }
    }

    public interface Callback {
        void allEmojisfound(String id);
    }

}
