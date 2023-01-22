package app.fedilab.android.mastodon.client.endpoints;
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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048;
    private final File mFile;
    private final String mContentType;
    private final ProgressListener mListener;
    private final long mTotalToUpload;
    private long mLastTotalUploaded;

    public ProgressRequestBody(File file, String content_type, long lastTotalUploaded, long totalToUpload, ProgressListener listener) {
        mFile = file;
        mContentType = content_type;
        mListener = listener;
        mTotalToUpload = totalToUpload;
        mLastTotalUploaded = lastTotalUploaded;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(mContentType);
    }

    @Override
    public long contentLength() {
        return mFile.length();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        byte[] buffer = new byte[SEGMENT_SIZE];
        FileInputStream in = new FileInputStream(mFile);
        try {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                // update progress on UI thread
                handler.post(new ProgressUpdater(mLastTotalUploaded, mTotalToUpload));
                mLastTotalUploaded += read;
                sink.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            in.close();
        }
       /* Source source = null;
        try {
            source = Okio.source(mFile);

            long read;

            while ((read = source.read(bufferedSink.getBuffer(), SEGMENT_SIZE)) != -1) {
                mLastTotalUploaded += read;
                bufferedSink.flush();
                mListener.onProgressUpdate((int)(100 * mLastTotalUploaded / mTotalToUpload));

            }
        } finally {
            Util.closeQuietly(source);
        }*/
    }

    public interface ProgressListener {
        void onProgressUpdate(int percentage);
    }

    private class ProgressUpdater implements Runnable {
        private final long mUploaded;
        private final long mTotal;

        public ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            mListener.onProgressUpdate((int) (100 * mUploaded / mTotal));
        }
    }
}

