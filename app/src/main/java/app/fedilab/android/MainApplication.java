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
import org.acra.ReportField;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import java.util.List;

import es.dmoral.toasty.Toasty;


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
        boolean custom_theme = sharedpreferences.getBoolean("use_custom_theme", false);
        boolean no_theme_set = sharedpreferences.getBoolean("no_theme_set", true);
        if (no_theme_set && !custom_theme) {
            list.get(0).apply(Cyanea.getInstance());
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean("no_theme_set", false);
            editor.apply();
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Toasty.Config.getInstance().apply();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(MainApplication.this);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.this);
        boolean send_crash_reports = sharedpreferences.getBoolean(getString(R.string.SET_SEND_CRASH_REPORTS), false);
        if (send_crash_reports) {
            ACRA.init(this, new CoreConfigurationBuilder()
                    //core configuration:
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportFormat(StringFormat.KEY_VALUE_LIST)
                    .withPluginConfigurations(
                            new MailSenderConfigurationBuilder()
                                    .withMailTo("hello@fedilab.app")
                                    .withReportAsFile(true)
                                    .withReportFileName("crash_report.txt")
                                    .withSubject("[Fedilab] - Crash Report " + BuildConfig.VERSION_CODE)
                                    .build(),
                            new DialogConfigurationBuilder()
                                    .withResIcon(R.mipmap.ic_launcher)
                                    .withText(getString(R.string.crash_title))
                                    .withCommentPrompt(getString(R.string.crash_message))
                                    .withResTheme(R.style.DialogDark)
                                    .withPositiveButtonText(getString(R.string.send_email))
                                    .withNegativeButtonText(getString(R.string.cancel))
                                    .build()
                    ).withReportContent(
                            ReportField.INSTALLATION_ID,
                            ReportField.APP_VERSION_CODE,
                            ReportField.ANDROID_VERSION,
                            ReportField.PHONE_MODEL,
                            ReportField.TOTAL_MEM_SIZE,
                            ReportField.AVAILABLE_MEM_SIZE,
                            ReportField.USER_CRASH_DATE,
                            ReportField.STACK_TRACE)
            );
        }
    }
}
