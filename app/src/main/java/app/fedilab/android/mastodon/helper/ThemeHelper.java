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

import static android.content.Context.WINDOW_SERVICE;
import static app.fedilab.android.BaseMainActivity.currentInstance;
import static app.fedilab.android.BaseMainActivity.currentNightMode;
import static app.fedilab.android.BaseMainActivity.currentUserID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import app.fedilab.android.R;


public class ThemeHelper {


    @ColorInt
    public static int getAttColor(Context context, @AttrRes int attColor) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attColor, typedValue, true);
        return typedValue.data;
    }


    public static int fetchAccentColor(Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }


    /**
     * Animate two views, the current view will be hidden to left
     *
     * @param viewToHide     View to hide
     * @param viewToShow     View to show
     * @param slideAnimation listener for the animation
     */
    public static void slideViewsToLeft(View viewToHide, View viewToShow, SlideAnimation slideAnimation) {

        TranslateAnimation animateHide = new TranslateAnimation(
                0,
                -viewToHide.getWidth(),
                0,
                0);
        TranslateAnimation animateShow = new TranslateAnimation(
                viewToShow.getWidth(),
                0,
                0,
                0);
        animateShow.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                viewToShow.setVisibility(View.VISIBLE);
                viewToHide.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewToHide.setVisibility(View.GONE);
                if (slideAnimation != null) {
                    slideAnimation.onAnimationEnded();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animateHide.setDuration(300);
        animateHide.setFillAfter(true);
        animateShow.setDuration(300);
        animateShow.setFillAfter(true);
        viewToHide.startAnimation(animateHide);
        viewToShow.startAnimation(animateShow);
    }

    /**
     * Animate two views, the current view will be hidden to right
     *
     * @param viewToHide     View to hide
     * @param viewToShow     View to show
     * @param slideAnimation listener for the animation
     */
    public static void slideViewsToRight(View viewToHide, View viewToShow, SlideAnimation slideAnimation) {

        TranslateAnimation animateHide = new TranslateAnimation(
                0,
                viewToHide.getWidth(),
                0,
                0);
        TranslateAnimation animateShow = new TranslateAnimation(
                -viewToShow.getWidth(),
                0,
                0,
                0);
        animateShow.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                viewToShow.setVisibility(View.VISIBLE);
                viewToHide.setVisibility(View.VISIBLE);
                if (slideAnimation != null) {
                    slideAnimation.onAnimationEnded();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewToHide.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animateHide.setDuration(300);
        animateShow.setDuration(300);
        viewToHide.requestLayout();
        viewToHide.startAnimation(animateHide);
        viewToShow.startAnimation(animateShow);
    }

    public static List<LinkedHashMap<String, String>> getContributorsTheme(Context context) {
        List<LinkedHashMap<String, String>> linkedHashMaps = new ArrayList<>();
        String[] list;
        try {
            list = context.getAssets().list("themes/contributors");
            for (String file : list) {
                InputStream is = context.getAssets().open("themes/contributors/" + file);
                LinkedHashMap<String, String> data = readCSVFile(is);
                linkedHashMaps.add(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return linkedHashMaps;
    }

    private static LinkedHashMap<String, String> readCSVFile(InputStream inputStream) {
        LinkedHashMap<String, String> readValues = new LinkedHashMap<>();
        if (inputStream != null) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    String[] line = sCurrentLine.split(",");
                    if (line.length > 1) {
                        String key = line[0];
                        String value = line[1];
                        readValues.put(key, value);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null) br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return readValues;
    }


    /**
     * Allow to set colors for no description on media
     *
     * @param context - Context
     * @return - ColorStateList
     */
    public static ColorStateList getNoDescriptionColorStateList(Context context) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_selected},
                new int[]{-android.R.attr.state_selected},
        };
        int[] colors = new int[]{
                ContextCompat.getColor(context, R.color.no_description),
                ContextCompat.getColor(context, R.color.no_description),
        };
        return new ColorStateList(states, colors);
    }


    /**
     * Allow to set colors for having description on media
     *
     * @param context - Context
     * @return - ColorStateList
     */
    public static ColorStateList getHavingDescriptionColorStateList(Context context) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_selected},
                new int[]{-android.R.attr.state_selected},
        };
        int[] colors = new int[]{
                ContextCompat.getColor(context, R.color.having_description),
                ContextCompat.getColor(context, R.color.having_description),
        };
        return new ColorStateList(states, colors);
    }


    /**
     * Allow to change font scale in activities
     *
     * @param activity      - Activity
     * @param configuration - Configuration
     */
    public static void adjustFontScale(Activity activity, Configuration configuration) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        configuration.fontScale = prefs.getFloat(activity.getString(R.string.SET_FONT_SCALE), 1.1f);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public static void switchTo(String themePref) {
        if (themes.LIGHT.name().equals(themePref) || themes.SOLARIZED_LIGHT.name().equals(themePref)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themes.DARK.name().equals(themePref) || themes.SOLARIZED_DARK.name().equals(themePref) || themes.DRACULA.name().equals(themePref) || themes.BLACK.name().equals(themePref)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }
        }
    }

    public static void applyThemeColor(Activity activity) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean dynamicColor = sharedpreferences.getBoolean(activity.getString(R.string.SET_DYNAMICCOLOR), false);
        boolean customAccentEnabled = sharedpreferences.getBoolean(activity.getString(R.string.SET_CUSTOM_ACCENT) + currentUserID + currentInstance, false);
        int customAccentLight = sharedpreferences.getInt(activity.getString(R.string.SET_CUSTOM_ACCENT_LIGHT_VALUE) + currentUserID + currentInstance, -1);
        int customAccentDark = sharedpreferences.getInt(activity.getString(R.string.SET_CUSTOM_ACCENT_DARK_VALUE) + currentUserID + currentInstance, -1);
        if (customAccentEnabled) {
            Bitmap bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            //Light theme enabled
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO && customAccentLight != -1) {
                canvas.drawColor(customAccentLight);
            } else if (customAccentDark != -1) {
                canvas.drawColor(customAccentDark);
            }
            DynamicColorsOptions.Builder builder = new DynamicColorsOptions.Builder();
            builder.setContentBasedSource(bmp);
            builder.setThemeOverlay(R.style.ThemeOverlay_Material3_DynamicColors_DayNight);
            DynamicColorsOptions dynamicColorsOptions = builder.build();
            DynamicColors.applyToActivityIfAvailable(activity, dynamicColorsOptions);
        } else if (dynamicColor) {
            DynamicColors.applyToActivityIfAvailable(activity);
        }
    }

    public enum themes {
        LIGHT,
        DARK,
        BLACK,
        DRACULA,
        SYSTEM,
        SOLARIZED_LIGHT,
        SOLARIZED_DARK
    }

    public interface SlideAnimation {
        void onAnimationEnded();
    }
}
