package app.fedilab.android.mastodon.helper;
/* Copyright 2026 Thomas Schneider
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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.github.penfeizhou.animation.apng.APNGDrawable;
import com.github.penfeizhou.animation.gif.GifDrawable;
import com.github.penfeizhou.animation.webp.WebPDrawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;

public class EmojiLoader {

    public static void loadEmoji(ImageView view, String url) {
        Glide.with(view)
                .asFile()
                .load(url)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File file, @Nullable Transition<? super File> transition) {
                        Drawable drawable = decodeFile(view.getResources(), file);
                        if (drawable instanceof Animatable) {
                            ((Animatable) drawable).start();
                        }
                        view.setImageDrawable(drawable);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public static void loadEmojiSpan(View view, String url, boolean animate, DrawableCallback callback) {
        if (!Helper.isValidContextForGlide(view.getContext())) {
            return;
        }

        // Store target in view tag to prevent garbage collection
        List<Target<?>> targets = (List<Target<?>>) view.getTag(R.id.custom_emoji_targets);
        if (targets == null) {
            targets = new ArrayList<>();
            view.setTag(R.id.custom_emoji_targets, targets);
        }

        CustomTarget<File> target = new CustomTarget<File>() {
            @Override
            public void onResourceReady(@NonNull File file, @Nullable Transition<? super File> transition) {
                Drawable drawable = decodeFile(view.getResources(), file);
                if (drawable != null) {
                    callback.onLoaded(drawable, animate);
                } else {
                    callback.onFailed();
                }
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                callback.onFailed();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };

        targets.add(target);

        Glide.with(view.getContext())
                .asFile()
                .load(url)
                .into(target);
    }

    private static Drawable decodeFile(Resources resources, File file) {
        int fileType = getFileType(file);
        if (fileType == TYPE_APNG) {
            return APNGDrawable.fromFile(file.getAbsolutePath());
        } else if (fileType == TYPE_PNG) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                return new BitmapDrawable(resources, bitmap);
            }
        } else if (fileType == TYPE_GIF_ANIMATED) {
            return GifDrawable.fromFile(file.getAbsolutePath());
        } else if (fileType == TYPE_GIF) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                return new BitmapDrawable(resources, bitmap);
            }
        } else if (fileType == TYPE_WEBP_ANIMATED) {
            return WebPDrawable.fromFile(file.getAbsolutePath());
        } else if (fileType == TYPE_WEBP) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                return new BitmapDrawable(resources, bitmap);
            }
        }
        // Fallback to BitmapDrawable for other formats (JPEG, etc.)
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap != null) {
            return new BitmapDrawable(resources, bitmap);
        }
        return null;
    }

    private static final int TYPE_UNKNOWN = 0;
    private static final int TYPE_PNG = 1;
    private static final int TYPE_APNG = 2;
    private static final int TYPE_GIF = 3;
    private static final int TYPE_GIF_ANIMATED = 4;
    private static final int TYPE_WEBP = 5;
    private static final int TYPE_WEBP_ANIMATED = 6;

    private static int getFileType(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[40];
            int bytesRead = fis.read(header);
            if (bytesRead < 12) {
                return TYPE_UNKNOWN;
            }
            // PNG signature
            if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
                if (isAPNG(file)) {
                    return TYPE_APNG;
                }
                return TYPE_PNG;
            }
            // GIF signature
            if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) {
                if (isAnimatedGif(file)) {
                    return TYPE_GIF_ANIMATED;
                }
                return TYPE_GIF;
            }
            // WebP signature
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                if (isAnimatedWebP(file)) {
                    return TYPE_WEBP_ANIMATED;
                }
                return TYPE_WEBP;
            }
        } catch (IOException ignored) {
        }
        return TYPE_UNKNOWN;
    }

    private static boolean isAPNG(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] signature = new byte[8];
            if (fis.read(signature) < 8) {
                return false;
            }
            byte[] chunkHeader = new byte[8];
            while (fis.read(chunkHeader) == 8) {
                String chunkType = new String(chunkHeader, 4, 4);
                if ("acTL".equals(chunkType)) {
                    return true;
                }
                if ("IDAT".equals(chunkType)) {
                    return false;
                }
                int length = ((chunkHeader[0] & 0xFF) << 24) | ((chunkHeader[1] & 0xFF) << 16)
                        | ((chunkHeader[2] & 0xFF) << 8) | (chunkHeader[3] & 0xFF);
                fis.skip(length + 4); // data + CRC
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static boolean isAnimatedGif(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            int imageCount = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead - 1; i++) {
                    if (i + 14 < bytesRead && buffer[i] == 0x21 && buffer[i + 1] == (byte) 0xFF) {
                        if (buffer[i + 2] == 0x0B) {
                            String app = new String(buffer, i + 3, 11);
                            if ("NETSCAPE2.0".equals(app) || "ANIMEXTS1.0".equals(app)) {
                                return true;
                            }
                        }
                    }
                    if (buffer[i] == 0x2C) {
                        imageCount++;
                        if (imageCount > 1) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static boolean isAnimatedWebP(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[32];
            if (fis.read(header) < 21) {
                return false;
            }
            if (header[12] == 'V' && header[13] == 'P' && header[14] == '8' && header[15] == 'X') {
                return (header[20] & 0x02) != 0;
            }
            fis.getChannel().position(12);
            byte[] chunkHeader = new byte[4];
            while (fis.read(chunkHeader) == 4) {
                String chunkType = new String(chunkHeader, 0, 4);
                if ("ANIM".equals(chunkType) || "ANMF".equals(chunkType)) {
                    return true;
                }
                // Read chunk size and skip
                byte[] sizeBytes = new byte[4];
                if (fis.read(sizeBytes) < 4) break;
                int size = (sizeBytes[0] & 0xFF) | ((sizeBytes[1] & 0xFF) << 8)
                        | ((sizeBytes[2] & 0xFF) << 16) | ((sizeBytes[3] & 0xFF) << 24);
                fis.skip(size + (size % 2));
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public interface DrawableCallback {
        void onLoaded(Drawable drawable, boolean animate);

        void onFailed();
    }
}