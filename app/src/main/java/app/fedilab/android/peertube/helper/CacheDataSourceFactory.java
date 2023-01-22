package app.fedilab.android.peertube.helper;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;


public class CacheDataSourceFactory implements DataSource.Factory {
    private static SimpleCache sDownloadCache;
    private final Context context;
    private final DefaultDataSourceFactory defaultDatasourceFactory;
    private final long maxFileSize;

    public CacheDataSourceFactory(Context context) {
        super();
        this.context = context;
        this.maxFileSize = 5 * 1024 * 1024;
        DefaultBandwidthMeter.Builder bandwidthMeterBuilder = new DefaultBandwidthMeter.Builder(context);
        DefaultBandwidthMeter bandwidthMeter = bandwidthMeterBuilder.build();
        defaultDatasourceFactory = new DefaultDataSourceFactory(this.context,
                bandwidthMeter,
                new DefaultHttpDataSourceFactory(Util.getUserAgent(context, null), bandwidthMeter));
    }

    public static SimpleCache getInstance(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        int video_cache = sharedpreferences.getInt(Helper.SET_VIDEO_CACHE, Helper.DEFAULT_VIDEO_CACHE_MB);
        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(video_cache * 1024 * 1024);
        ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);
        if (sDownloadCache == null)
            sDownloadCache = new SimpleCache(new File(context.getCacheDir(), "media"), evictor, exoDatabaseProvider);
        return sDownloadCache;
    }

    @Override
    public DataSource createDataSource() {
        SimpleCache simpleCache = getInstance(context);
        return new CacheDataSource(simpleCache, defaultDatasourceFactory.createDataSource(),
                new FileDataSource(), new CacheDataSink(simpleCache, maxFileSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null);
    }
}