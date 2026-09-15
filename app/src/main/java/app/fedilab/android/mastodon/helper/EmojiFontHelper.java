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

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EmojiFontHelper {

    public static final long FONT_SIZE = 10773992;
    private static final String FONT_URL = "https://files.fedilab.app/fedilab/emoji/NotoColorEmojiCompat-1.5.0.ttf";
    private static final String FONT_NAME = "NotoColorEmojiCompat-1.5.0.ttf";
    private static final String FONT_SHA256 = "c18d4022c22883f60ae74ad1d962f8b2b69440c6b0a0b55f0d4600165a1ca69a";

    public static File getFontFile(Context context) {
        return new File(context.getFilesDir(), FONT_NAME);
    }

    public static boolean isInstalled(Context context) {
        File fontFile = getFontFile(context);
        return fontFile.exists() && fontFile.length() == FONT_SIZE;
    }

    public static void init(Context context) {
        if (!isInstalled(context)) {
            EmojiCompat.init(context);
            return;
        }
        File fontFile = getFontFile(context);
        EmojiCompat.init(new EmojiCompat.Config(loaderCallback -> new Thread(() -> {
            try (InputStream inputStream = new FileInputStream(fontFile)) {
                loaderCallback.onLoaded(MetadataRepo.create(Typeface.createFromFile(fontFile), inputStream));
            } catch (Throwable throwable) {
                loaderCallback.onFailed(throwable);
            }
        }).start()) {
        });
    }

    public static void download(Context context, @NonNull Callback callback) {
        new Thread(() -> {
            boolean downloaded = false;
            File partialFile = new File(context.getFilesDir(), FONT_NAME + ".part");
            try {
                OkHttpClient okHttpClient = Helper.myOkHttpClient(context.getApplicationContext());
                Request request = new Request.Builder().url(FONT_URL).build();
                try (Response response = okHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        try (InputStream inputStream = response.body().byteStream();
                             FileOutputStream outputStream = new FileOutputStream(partialFile)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, read);
                            }
                        }
                        downloaded = FONT_SHA256.equals(sha256(partialFile)) && partialFile.renameTo(getFontFile(context));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!downloaded && partialFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                partialFile.delete();
            }
            boolean finalDownloaded = downloaded;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalDownloaded));
        }).start();
    }

    public static void remove(Context context) {
        //noinspection ResultOfMethodCallIgnored
        getFontFile(context).delete();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, read);
            }
        }
        StringBuilder digest = new StringBuilder();
        for (byte digestByte : messageDigest.digest()) {
            digest.append(String.format(Locale.US, "%02x", digestByte));
        }
        return digest.toString();
    }

    public interface Callback {
        void onResult(boolean success);
    }
}
