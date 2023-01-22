package app.fedilab.android.mastodon.helper;
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

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

import app.fedilab.android.R;


public class CacheDataSourceFactory implements DataSource.Factory {

    private static SimpleCache sDownloadCache;
    private final Context context;
    private final DefaultDataSourceFactory defaultDatasourceFactory;

    public CacheDataSourceFactory(Context context) {
        super();
        this.context = context;
        DefaultBandwidthMeter.Builder bandwidthMeterBuilder = new DefaultBandwidthMeter.Builder(context);
        DefaultBandwidthMeter bandwidthMeter = bandwidthMeterBuilder.build();
        DefaultHttpDataSource.Factory defaultHttpDataSource = new DefaultHttpDataSource.Factory();
        defaultDatasourceFactory = new DefaultDataSourceFactory(this.context,
                bandwidthMeter,
                defaultHttpDataSource);
    }

    public static synchronized SimpleCache getInstance(Context context) {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int video_cache = sharedpreferences.getInt(context.getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);

        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(((long) video_cache * 1024 * 1024));
        ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);

        if (sDownloadCache == null)
            sDownloadCache = new SimpleCache(new File(context.getCacheDir(), "media"), evictor, exoDatabaseProvider);
        return sDownloadCache;
    }

    @NonNull
    @Override
    public DataSource createDataSource() {
        SimpleCache simpleCache = getInstance(context);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int video_cache = sharedpreferences.getInt(context.getString(R.string.SET_VIDEO_CACHE), Helper.DEFAULT_VIDEO_CACHE_MB);
        return new CacheDataSource(simpleCache, defaultDatasourceFactory.createDataSource(),
                new FileDataSource(), new CacheDataSink(simpleCache, ((long) video_cache * 1024 * 1024)),
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null);
    }
}