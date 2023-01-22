package app.fedilab.android.mastodon.helper;
/* Copyright 2021 Thomas Schneider
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

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.PAINT_FLAGS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.util.Synthetic;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jp.wasabeef.glide.transformations.BitmapTransformation;

public class GlideFocus extends BitmapTransformation {


    private static final int VERSION = 1;
    private static final String ID = "app.fedilab.android.GlideFocus." + VERSION;
    private static final Set<String> MODELS_REQUIRING_BITMAP_LOCK =
            new HashSet<>(
                    Arrays.asList(
                            // Moto X gen 2
                            "XT1085",
                            "XT1092",
                            "XT1093",
                            "XT1094",
                            "XT1095",
                            "XT1096",
                            "XT1097",
                            "XT1098",
                            // Moto G gen 1
                            "XT1031",
                            "XT1028",
                            "XT937C",
                            "XT1032",
                            "XT1008",
                            "XT1033",
                            "XT1035",
                            "XT1034",
                            "XT939G",
                            "XT1039",
                            "XT1040",
                            "XT1042",
                            "XT1045",
                            // Moto G gen 2
                            "XT1063",
                            "XT1064",
                            "XT1068",
                            "XT1069",
                            "XT1072",
                            "XT1077",
                            "XT1078",
                            "XT1079"));
    private static final Lock BITMAP_DRAWABLE_LOCK =
            MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL) ? new ReentrantLock() : new NoLock();
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
    private final float focalX;
    private final float focalY;

    public GlideFocus(float focalX, float focalY) {
        this.focalX = focalX;
        this.focalY = focalY;
    }

    private static void applyMatrix(
            @NonNull Bitmap inBitmap, @NonNull Bitmap targetBitmap, Matrix matrix) {
        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(targetBitmap);
            canvas.drawBitmap(inBitmap, matrix, DEFAULT_PAINT);
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }
    }

    @NonNull
    private static Bitmap.Config getNonNullConfig(@NonNull Bitmap bitmap) {
        return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
    }

    // Avoids warnings in M+.
    private static void clear(Canvas canvas) {
        canvas.setBitmap(null);
    }

    @Override
    protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
                               @NonNull Bitmap inBitmap, int width, int height) {

        if (inBitmap.getWidth() == width && inBitmap.getHeight() == height) {
            return inBitmap;
        }
        // From ImageView/Bitmap.createScaledBitmap.
        final float scale;
        final float dx;
        final float dy;
        Matrix m = new Matrix();
        if (inBitmap.getWidth() * height > width * inBitmap.getHeight()) {
            scale = (float) height / (float) inBitmap.getHeight();
            dx = (width - inBitmap.getWidth() * scale) * 0.5f * (1 + focalX);
            dy = 0;
        } else {
            scale = (float) width / (float) inBitmap.getWidth();
            dx = 0;
            dy = (height - inBitmap.getHeight() * scale) * 0.5f * (1 + focalY);
        }

        m.setScale(scale, scale);
        m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

        Bitmap result = pool.get(width, height, getNonNullConfig(inBitmap));
        // We don't add or remove alpha, so keep the alpha setting of the Bitmap we were given.
        TransformationUtils.setAlpha(inBitmap, result);

        applyMatrix(inBitmap, result, m);
        return result;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update((ID + focalX + focalY).getBytes(CHARSET));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GlideFocus &&
                ((GlideFocus) o).focalX == focalX &&
                ((GlideFocus) o).focalY == focalY;
    }

    @Override
    public int hashCode() {
        return (int) (ID.hashCode() + focalX * 100000 + focalY * 1000);
    }


    @Override
    public String toString() {
        return "CropTransformation(width=" + focalX + ", height=" + focalY + ")";
    }

    private static final class NoLock implements Lock {

        @Synthetic
        NoLock() {
        }

        @Override
        public void lock() {
            // do nothing
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // do nothing
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
            // do nothing
        }

        @NonNull
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }
}