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
import android.webkit.WebView;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import app.fedilab.android.mastodon.helper.ThemeHelper;
import es.dmoral.toasty.Toasty;


public class MainApplication extends MultiDexApplication {


    private static MainApplication app;
    private WebView webView;

    public static MainApplication getApp() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.this);
        try {
            webView = new WebView(this);
        } catch (Exception ignored) {
        }
        boolean dynamicColor = sharedpreferences.getBoolean(getString(R.string.SET_DYNAMICCOLOR), false);
        if (dynamicColor) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Toasty.Config.getInstance().apply();
        if (webView != null) {
            try {
                webView.destroy();
            } catch (Exception ignored) {
            }
        }
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(MainApplication.this);
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.this);
        boolean send_crash_reports = sharedpreferences.getBoolean(getString(R.string.SET_SEND_CRASH_REPORTS), false);
        String currentTheme = sharedpreferences.getString(getString(R.string.SET_THEME_BASE), getString(R.string.SET_DEFAULT_THEME));
        ThemeHelper.switchTo(currentTheme);
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
