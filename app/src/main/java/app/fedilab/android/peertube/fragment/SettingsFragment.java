package app.fedilab.android.peertube.fragment;


import static app.fedilab.android.peertube.activities.PeertubeMainActivity.userMe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.avatarfirst.avatargenlib.AvatarGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.fedilab.android.R;
import app.fedilab.android.peertube.activities.MyAccountActivity;
import app.fedilab.android.peertube.activities.PeertubeMainActivity;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.entities.Error;
import app.fedilab.android.peertube.client.entities.UserSettings;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.helper.HelperInstance;
import app.fedilab.android.peertube.helper.ThemeHelper;
import es.dmoral.toasty.Toasty;

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

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main_preferences);
        createPref();
    }


    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        requireActivity();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        SharedPreferences.Editor editor = sharedpreferences.edit();

        if (key.compareTo(getString(R.string.set_video_mode_choice)) == 0) {
            ListPreference set_video_mode_choice = findPreference(getString(R.string.set_video_mode_choice));
            if (set_video_mode_choice != null) {
                switch (set_video_mode_choice.getValue()) {
                    case "0":
                        editor.putInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_NORMAL);
                        break;
                    case "1":
                        editor.putInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_MAGNET);
                        break;
                    case "2":
                        editor.putInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_WEBVIEW);
                        break;
                    case "3":
                        editor.putInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_TORRENT);
                        break;
                }

            }
        }
        if (key.compareTo(getString(R.string.set_theme_choice)) == 0) {
            ListPreference set_theme_choice = findPreference(getString(R.string.set_theme_choice));
            if (set_theme_choice != null) {
                int choice;
                switch (set_theme_choice.getValue()) {
                    case "0":
                        choice = Helper.LIGHT_MODE;
                        break;
                    case "1":
                        choice = Helper.DARK_MODE;
                        break;
                    default:
                        choice = Helper.DEFAULT_MODE;
                }
                editor.putInt(Helper.SET_THEME, choice);
                editor.apply();
                ThemeHelper.switchTo(choice);
            }
        }
        if (key.compareTo(getString(R.string.set_video_sensitive_choice)) == 0) {
            ListPreference set_video_sensitive_choice = findPreference(getString(R.string.set_video_sensitive_choice));
            if (set_video_sensitive_choice != null) {
                editor.putString(getString(R.string.set_video_sensitive_choice), set_video_sensitive_choice.getValue());
                editor.apply();
                if (Helper.isLoggedIn(getActivity())) {
                    new Thread(() -> {
                        UserSettings userSettings = new UserSettings();
                        userSettings.setNsfwPolicy(set_video_sensitive_choice.getValue());
                        try {
                            RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(getActivity());
                            api.updateUser(userSettings);
                            userMe.setNsfwPolicy(set_video_sensitive_choice.getValue());
                        } catch (Exception | Error e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        }
        if (key.compareTo(getString(R.string.set_video_quality_choice)) == 0) {
            ListPreference set_video_quality_choice = findPreference(getString(R.string.set_video_quality_choice));
            if (set_video_quality_choice != null) {
                switch (set_video_quality_choice.getValue()) {
                    case "0":
                        editor.putInt(Helper.SET_QUALITY_MODE, Helper.QUALITY_HIGH);
                        break;
                    case "1":
                        editor.putInt(Helper.SET_QUALITY_MODE, Helper.QUALITY_MEDIUM);
                        break;
                    case "2":
                        editor.putInt(Helper.SET_QUALITY_MODE, Helper.QUALITY_LOW);
                        break;
                }
            }
        }
        if (key.compareTo(getString(R.string.set_video_cache_choice)) == 0) {
            SeekBarPreference set_video_cache_choice = findPreference(getString(R.string.set_video_cache_choice));
            assert set_video_cache_choice != null;
            final int progress = set_video_cache_choice.getValue();
            set_video_cache_choice.setSummary(requireActivity().getString(R.string.video_cache_value, progress * 10));
            editor.putInt(Helper.SET_VIDEO_CACHE, progress * 10);
        }
        if (key.compareTo(getString(R.string.set_video_minimize_choice)) == 0) {
            SwitchPreference set_video_minimize_choice = findPreference(


                    getString(R.string.set_video_minimize_choice));
            assert set_video_minimize_choice != null;
            editor.putBoolean(getString(R.string.set_video_minimize_choice), set_video_minimize_choice.isChecked());
        }
        if (key.compareTo(getString(R.string.set_autoplay_choice)) == 0) {
            SwitchPreference set_autoplay_choice = findPreference(getString(R.string.set_autoplay_choice));
            assert set_autoplay_choice != null;
            editor.putBoolean(getString(R.string.set_autoplay_choice), set_autoplay_choice.isChecked());
            if (Helper.isLoggedIn(getActivity())) {
                new Thread(() -> {
                    UserSettings userSettings = new UserSettings();
                    userSettings.setAutoPlayVideo(set_autoplay_choice.isChecked());
                    try {
                        RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(getActivity());
                        api.updateUser(userSettings);
                    } catch (Exception | Error e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
        if (key.compareTo(getString(R.string.set_fullscreen_choice)) == 0) {
            SwitchPreference set_fullscreen_choice = findPreference(getString(R.string.set_fullscreen_choice));
            assert set_fullscreen_choice != null;
            editor.putBoolean(getString(R.string.set_fullscreen_choice), set_fullscreen_choice.isChecked());
        }
        if (key.compareTo(getString(R.string.set_autoplay_next_video_choice)) == 0) {
            SwitchPreference set_autoplay_next_video_choice = findPreference(getString(R.string.set_autoplay_next_video_choice));
            assert set_autoplay_next_video_choice != null;
            editor.putBoolean(getString(R.string.set_autoplay_next_video_choice), set_autoplay_next_video_choice.isChecked());
            if (Helper.isLoggedIn(getActivity())) {
                new Thread(() -> {
                    UserSettings userSettings = new UserSettings();
                    userSettings.setAutoPlayNextVideo(set_autoplay_next_video_choice.isChecked());
                    try {
                        RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(getActivity());
                        api.updateUser(userSettings);
                    } catch (Exception | Error e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
        if (key.compareTo(getString(R.string.set_play_screen_lock_choice)) == 0) {
            SwitchPreference set_play_screen_lock_choice = findPreference(getString(R.string.set_play_screen_lock_choice));
            assert set_play_screen_lock_choice != null;
            editor.putBoolean(getString(R.string.set_play_screen_lock_choice), set_play_screen_lock_choice.isChecked());
        }
        if (key.compareTo(getString(R.string.set_video_in_list_choice)) == 0) {
            SwitchPreference set_video_in_list_choice = findPreference(getString(R.string.set_video_in_list_choice));
            assert set_video_in_list_choice != null;
            editor.putBoolean(getString(R.string.set_video_in_list_choice), set_video_in_list_choice.isChecked());
            Intent intent = new Intent(requireActivity(), PeertubeMainActivity.class);
            startActivity(intent);
        }
        if (key.compareTo(getString(R.string.set_cast_choice)) == 0) {
            SwitchPreference set_cast_choice = findPreference(getString(R.string.set_cast_choice));
            assert set_cast_choice != null;
            editor.putInt(getString(R.string.set_cast_choice), set_cast_choice.isChecked() ? 1 : 0);
            Intent intentBC = new Intent(Helper.RECEIVE_CAST_SETTINGS);
            Bundle b = new Bundle();
            b.putInt("state_asked", set_cast_choice.isChecked() ? 1 : 0);
            intentBC.putExtras(b);
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intentBC);
        }
        if (key.compareTo(getString(R.string.set_video_language_choice)) == 0) {
            MultiSelectListPreference set_video_language_choice = findPreference(getString(R.string.set_video_language_choice));
            assert set_video_language_choice != null;
            editor.putStringSet(getString(R.string.set_video_language_choice), set_video_language_choice.getValues());
            if (Helper.isLoggedIn(getActivity())) {
                new Thread(() -> {
                    UserSettings userSettings = new UserSettings();
                    Set<String> language_choiceValues = set_video_language_choice.getValues();
                    userSettings.setVideoLanguages(new ArrayList<>(language_choiceValues));
                    try {
                        RetrofitPeertubeAPI api = new RetrofitPeertubeAPI(getActivity());
                        api.updateUser(userSettings);
                    } catch (Exception | Error e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
        editor.apply();
    }

    private void createPref() {
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.main_preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        FragmentActivity context = requireActivity();
        if (preferenceScreen == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            return;
        }

        //****** My Account ******

        Preference my_account = findPreference("my_account");
        if (my_account != null) {
            my_account.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireActivity(), MyAccountActivity.class));
                return false;
            });
            if (!Helper.isLoggedIn(getActivity()) || userMe == null) {
                my_account.setVisible(false);
            } else {
                my_account.setTitle(userMe.getUsername());
                my_account.setSummary(userMe.getEmail());
                Resources resources = getResources();
                Drawable defaultAvatar = ResourcesCompat.getDrawable(resources, R.drawable.missing_peertube, null);
                my_account.setIcon(defaultAvatar);
                String avatarUrl = null;
                BitmapDrawable avatar = null;
                if (userMe.getAccount().getAvatar() != null) {
                    avatarUrl = "https://" + HelperInstance.getLiveInstance(context) + userMe.getAccount().getAvatar().getPath();
                } else {
                    avatar = new AvatarGenerator.AvatarBuilder(context)
                            .setLabel(userMe.getAccount().getAcct())
                            .setAvatarSize(120)
                            .setTextSize(30)
                            .toSquare()
                            .setBackgroundColor(Helper.fetchAccentColor(context))
                            .build();
                }

                Glide.with(requireActivity())
                        .asDrawable()
                        .load(avatarUrl != null ? avatarUrl : avatar)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                my_account.setIcon(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });

            }
        }


        //****** App theme *******
        final SharedPreferences sharedpref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        ListPreference SET_THEME_BASE = findPreference(getString(R.string.SET_THEME_BASE));
        if (SET_THEME_BASE != null) {
            SET_THEME_BASE.getContext().setTheme(app.fedilab.android.mastodon.helper.Helper.dialogStyle());
        }
        ListPreference SET_THEME_DEFAULT_LIGHT = findPreference(getString(R.string.SET_THEME_DEFAULT_LIGHT));
        if (SET_THEME_DEFAULT_LIGHT != null) {
            SET_THEME_DEFAULT_LIGHT.getContext().setTheme(app.fedilab.android.mastodon.helper.Helper.dialogStyle());
        }
        ListPreference SET_THEME_DEFAULT_DARK = findPreference(getString(R.string.SET_THEME_DEFAULT_DARK));
        if (SET_THEME_DEFAULT_DARK != null) {
            SET_THEME_DEFAULT_DARK.getContext().setTheme(app.fedilab.android.mastodon.helper.Helper.dialogStyle());
        }


        //****** Video mode *******
        ListPreference set_video_mode_choice = findPreference(getString(R.string.set_video_mode_choice));
        List<String> array = Arrays.asList(getResources().getStringArray(R.array.settings_video_mode));
        CharSequence[] entries = array.toArray(new CharSequence[0]);
        CharSequence[] entryValues = new CharSequence[2];
        int video_mode = sharedpref.getInt(Helper.SET_VIDEO_MODE, Helper.VIDEO_MODE_NORMAL);
        entryValues[0] = String.valueOf(Helper.VIDEO_MODE_NORMAL);
        entryValues[1] = String.valueOf(Helper.VIDEO_MODE_WEBVIEW);
        if (set_video_mode_choice != null) {
            set_video_mode_choice.setEntries(entries);
            set_video_mode_choice.setEntryValues(entryValues);
            if (video_mode > Helper.VIDEO_MODE_WEBVIEW) {
                video_mode = Helper.VIDEO_MODE_NORMAL;
            }
            set_video_mode_choice.setValueIndex(video_mode);
        }

        //****** Video quality *******
        ListPreference set_video_quality_choice = findPreference(getString(R.string.set_video_quality_choice));
        List<String> arrayQuality = Arrays.asList(getResources().getStringArray(R.array.settings_video_quality));
        CharSequence[] entriesQuality = arrayQuality.toArray(new CharSequence[0]);
        CharSequence[] entryValuesQuality = new CharSequence[3];
        int video_quality = sharedpref.getInt(Helper.SET_QUALITY_MODE, Helper.QUALITY_HIGH);
        entryValuesQuality[0] = String.valueOf(Helper.QUALITY_HIGH);
        entryValuesQuality[1] = String.valueOf(Helper.QUALITY_MEDIUM);
        entryValuesQuality[2] = String.valueOf(Helper.QUALITY_LOW);
        if (set_video_quality_choice != null) {
            set_video_quality_choice.setEntries(entriesQuality);
            set_video_quality_choice.setEntryValues(entryValuesQuality);
            set_video_quality_choice.setValueIndex(video_quality);
        }
        //****** Video cache *******
        SeekBarPreference set_video_cache_choice = findPreference(getString(R.string.set_video_cache_choice));
        int video_cache = sharedpref.getInt(Helper.SET_VIDEO_CACHE, Helper.DEFAULT_VIDEO_CACHE_MB);
        assert set_video_cache_choice != null;
        set_video_cache_choice.setValue(video_cache / 10);

        //****** Minimized videos *******
        boolean minimized = sharedpref.getBoolean(getString(R.string.set_video_minimize_choice), true);
        SwitchPreference set_video_minimize_choice = findPreference(getString(R.string.set_video_minimize_choice));
        assert set_video_minimize_choice != null;
        set_video_minimize_choice.setChecked(minimized);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || !requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            set_video_minimize_choice.setVisible(false);
        }


        //****** Autoplay videos *******
        boolean autoplay = sharedpref.getBoolean(getString(R.string.set_autoplay_choice), true);
        SwitchPreference set_autoplay_choice = findPreference(getString(R.string.set_autoplay_choice));
        assert set_autoplay_choice != null;
        set_autoplay_choice.setChecked(autoplay);


        //****** Fullscreen videos *******
        boolean fullscreen = sharedpref.getBoolean(getString(R.string.set_fullscreen_choice), false);
        SwitchPreference set_fullscreen_choice = findPreference(getString(R.string.set_fullscreen_choice));
        assert set_fullscreen_choice != null;
        set_fullscreen_choice.setChecked(fullscreen);

        //****** Autoplay next videos *******
        boolean autoplayNextVideo = sharedpref.getBoolean(getString(R.string.set_autoplay_next_video_choice), false);
        SwitchPreference set_autoplay_next_video_choice = findPreference(getString(R.string.set_autoplay_next_video_choice));
        assert set_autoplay_next_video_choice != null;
        set_autoplay_next_video_choice.setChecked(autoplayNextVideo);


        //****** Screen lock *******
        boolean playScreenLock = sharedpref.getBoolean(getString(R.string.set_play_screen_lock_choice), false);
        SwitchPreference set_play_screen_lock_choice = findPreference(getString(R.string.set_play_screen_lock_choice));
        assert set_play_screen_lock_choice != null;
        set_play_screen_lock_choice.setChecked(playScreenLock);


        //****** Display videos in a list *******
        boolean videosInList = sharedpref.getBoolean(getString(R.string.set_video_in_list_choice), false);
        SwitchPreference set_video_in_list_choice = findPreference(getString(R.string.set_video_in_list_choice));
        assert set_video_in_list_choice != null;
        set_video_in_list_choice.setChecked(videosInList);

        //****** Allow Chromecast *******
        int cast = sharedpref.getInt(getString(R.string.set_cast_choice), 0);
        SwitchPreference set_cast_choice = findPreference(getString(R.string.set_cast_choice));
        assert set_cast_choice != null;
        set_cast_choice.setChecked(cast == 1);

        //****** Language filter  *********
        LinkedHashMap<String, String> languages = new LinkedHashMap<>(Helper.peertubeInformation.getLanguages());
        List<CharSequence> entriesLanguages = new ArrayList<>();
        List<CharSequence> valuesLanguages = new ArrayList<>();
        Iterator<Map.Entry<String, String>> it = languages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            entriesLanguages.add(pair.getValue());
            valuesLanguages.add(pair.getKey());
            it.remove();
        }
        MultiSelectListPreference set_video_language_choice = findPreference(getString(R.string.set_video_language_choice));
        Set<String> selection = sharedpref.getStringSet(getString(R.string.set_video_language_choice), null);
        assert set_video_language_choice != null;
        set_video_language_choice.setEntries(entriesLanguages.toArray(new CharSequence[]{}));
        set_video_language_choice.setEntryValues(valuesLanguages.toArray(new CharSequence[]{}));

        if (selection != null) {
            set_video_language_choice.setValues(selection);
        }

        //****** Display sensitive content *******
        ListPreference set_video_sensitive_choice = findPreference(getString(R.string.set_video_sensitive_choice));
        List<String> arraySensitive = new ArrayList<>();
        arraySensitive.add(getString(R.string.do_not_list));
        arraySensitive.add(getString(R.string.blur));
        arraySensitive.add(getString(R.string.display));
        CharSequence[] entriesSensitive = arraySensitive.toArray(new CharSequence[0]);
        CharSequence[] entryValuesSensitive = new CharSequence[3];
        String currentSensitive = sharedpref.getString(getString(R.string.set_video_sensitive_choice), Helper.BLUR);
        entryValuesSensitive[0] = Helper.DO_NOT_LIST.toLowerCase();
        entryValuesSensitive[1] = Helper.BLUR.toLowerCase();
        entryValuesSensitive[2] = Helper.DISPLAY.toLowerCase();
        int currentSensitivePosition = 0;
        for (CharSequence val : entryValuesSensitive) {
            if (val.equals(currentSensitive)) {
                break;
            }
            currentSensitivePosition++;
        }
        if (set_video_sensitive_choice != null) {
            set_video_sensitive_choice.setEntries(entriesSensitive);
            set_video_sensitive_choice.setEntryValues(entryValuesSensitive);
            set_video_sensitive_choice.setValueIndex(currentSensitivePosition);
        }
    }

}
