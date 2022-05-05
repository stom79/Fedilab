package app.fedilab.android;
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


import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.jaredrummler.cyanea.Cyanea;
import com.jaredrummler.cyanea.prefs.CyaneaTheme;

import org.acra.ACRA;
import org.acra.annotation.AcraNotification;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.LimiterConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import java.util.List;

import es.dmoral.toasty.Toasty;


@AcraNotification(
        resIcon = R.mipmap.ic_launcher, resTitle = R.string.crash_title, resChannelName = R.string.set_crash_reports, resText = R.string.crash_message)

public class MainApplication extends MultiDexApplication {


    private static MainApplication app;

    public static MainApplication getApp() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.this);


        Cyanea.init(this, super.getResources());
        List<CyaneaTheme> list = CyaneaTheme.Companion.from(getAssets(), "themes/cyanea_themes.json");
        int theme = sharedpreferences.getInt(getString(R.string.SET_THEME), 0);
        boolean custom_theme = sharedpreferences.getBoolean("use_custom_theme", false);
        /*if (!custom_theme) {
            list.get(theme).apply(Cyanea.getInstance());
        }*/
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());


        boolean send_crash_reports = sharedpreferences.getBoolean(getString(R.string.SET_SEND_CRASH_REPORTS), false);
        if (send_crash_reports) {
            CoreConfigurationBuilder ACRABuilder = new CoreConfigurationBuilder(this);
            ACRABuilder.setBuildConfigClass(BuildConfig.class).setReportFormat(StringFormat.KEY_VALUE_LIST);
            int versionCode = BuildConfig.VERSION_CODE;
            ACRABuilder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class).setReportAsFile(true).setMailTo("hello@fedilab.app").setSubject("[Fedilab] - Crash Report " + versionCode).setEnabled(true);
            ACRABuilder.getPluginConfigurationBuilder(LimiterConfigurationBuilder.class).setEnabled(true);
            ACRA.init(this, ACRABuilder);
        }

        Toasty.Config.getInstance().apply();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(MainApplication.this);
    }
}
