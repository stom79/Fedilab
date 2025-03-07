package app.fedilab.android.mastodon.ui.fragment.settings;
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

import static android.content.Context.POWER_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import org.unifiedpush.android.connector.UnifiedPush;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.PushHelper;
import app.fedilab.android.mastodon.helper.settings.TimePreference;
import app.fedilab.android.mastodon.helper.settings.TimePreferenceDialogFragment;
import es.dmoral.toasty.Toasty;


public class FragmentNotificationsSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String DIALOG_FRAGMENT_TAG = "TimePreference";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_notifications);
        createPref();
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        if (preference instanceof TimePreference) {
            final DialogFragment f = TimePreferenceDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void createPref() {

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.pref_notifications);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        //Theme for dialogs
        ListPreference SET_NOTIFICATION_TYPE = findPreference(getString(R.string.SET_NOTIFICATION_TYPE));
        if (SET_NOTIFICATION_TYPE != null) {
            SET_NOTIFICATION_TYPE.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_NOTIFICATION_DELAY_VALUE = findPreference(getString(R.string.SET_NOTIFICATION_DELAY_VALUE));
        if (SET_NOTIFICATION_DELAY_VALUE != null) {
            SET_NOTIFICATION_DELAY_VALUE.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_PUSH_DISTRIBUTOR = findPreference(getString(R.string.SET_PUSH_DISTRIBUTOR));
        if (SET_PUSH_DISTRIBUTOR != null) {
            SET_PUSH_DISTRIBUTOR.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_LED_COLOUR_VAL_N = findPreference(getString(R.string.SET_LED_COLOUR_VAL_N));
        if (SET_LED_COLOUR_VAL_N != null) {
            SET_LED_COLOUR_VAL_N.getContext().setTheme(Helper.dialogStyle());
        }
        ListPreference SET_NOTIFICATION_ACTION = findPreference(getString(R.string.SET_NOTIFICATION_ACTION));
        if (SET_NOTIFICATION_ACTION != null) {
            SET_NOTIFICATION_ACTION.getContext().setTheme(Helper.dialogStyle());
        }
        //---------


        if (preferenceScreen == null) {
            Toasty.error(requireActivity(), getString(R.string.toast_error), Toasty.LENGTH_SHORT).show();
            return;
        }


        String[] notificationValues = getResources().getStringArray(R.array.SET_NOTIFICATION_TYPE_VALUE);
        if (SET_NOTIFICATION_TYPE != null && SET_NOTIFICATION_TYPE.getValue().equals(notificationValues[2])) {
            PreferenceCategory notification_sounds = findPreference("notification_sounds");
            if (notification_sounds != null) {
                preferenceScreen.removePreference(notification_sounds);
            }
            PreferenceCategory notifications_enabled = findPreference("notifications_enabled");
            if (notifications_enabled != null) {
                preferenceScreen.removePreference(notifications_enabled);
            }
            PreferenceCategory notification_time_slot = findPreference("notification_time_slot");
            if (notification_time_slot != null) {
                preferenceScreen.removePreference(notification_time_slot);
            }
            if (SET_NOTIFICATION_DELAY_VALUE != null) {
                preferenceScreen.removePreferenceRecursively("SET_NOTIFICATION_DELAY_VALUE");
            }
            if (SET_PUSH_DISTRIBUTOR != null) {
                preferenceScreen.removePreferenceRecursively("SET_PUSH_DISTRIBUTOR");
            }
            return;
        } else if (SET_NOTIFICATION_TYPE != null && SET_NOTIFICATION_TYPE.getValue().equals(notificationValues[1])) {
            if (SET_PUSH_DISTRIBUTOR != null) {
                preferenceScreen.removePreferenceRecursively("SET_PUSH_DISTRIBUTOR");
            }
        } else {
            if (SET_NOTIFICATION_DELAY_VALUE != null) {
                preferenceScreen.removePreferenceRecursively("SET_NOTIFICATION_DELAY_VALUE");
            }
            if (SET_PUSH_DISTRIBUTOR != null) {
                List<String> distributors = UnifiedPush.getDistributors(requireActivity());
                SET_PUSH_DISTRIBUTOR.setValue(UnifiedPush.getAckDistributor(requireActivity()));
                SET_PUSH_DISTRIBUTOR.setEntries(distributors.toArray(new String[0]));
                SET_PUSH_DISTRIBUTOR.setEntryValues(distributors.toArray(new String[0]));
            }
        }

        Preference SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS = findPreference(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
        if (SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) requireActivity().getSystemService(POWER_SERVICE);
                String packageName = requireActivity().getPackageName();
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS.setOnPreferenceClickListener(preference -> {
                        Intent intent = new Intent();
                        String packageName1 = requireActivity().getPackageName();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName1));
                        startActivity(intent);
                        return false;
                    });
                } else {
                    preferenceScreen.removePreferenceRecursively(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
                }
            } else {
                preferenceScreen.removePreferenceRecursively(getString(R.string.SET_KEY_IGNORE_BATTERY_OPTIMIZATIONS));
            }
        }

        Preference button_mention = findPreference("button_mention");
        assert button_mention != null;
        button_mention.setOnPreferenceClickListener(preference -> {
            openSettings("channel_mention", getString(R.string.channel_notif_mention));
            return false;
        });
        Preference button_follow = findPreference("button_follow");
        assert button_follow != null;
        button_follow.setOnPreferenceClickListener(preference -> {
            openSettings("channel_follow", getString(R.string.channel_notif_follow));
            return false;
        });
        Preference button_reblog = findPreference("button_reblog");
        assert button_reblog != null;
        button_reblog.setOnPreferenceClickListener(preference -> {
            openSettings("channel_boost", getString(R.string.channel_notif_boost));
            return false;
        });
        Preference button_favourite = findPreference("button_favourite");
        assert button_favourite != null;
        button_favourite.setOnPreferenceClickListener(preference -> {
            openSettings("channel_favourite", getString(R.string.channel_notif_fav));
            return false;
        });
        Preference button_poll = findPreference("button_poll");
        assert button_poll != null;
        button_poll.setOnPreferenceClickListener(preference -> {
            openSettings("channel_poll", getString(R.string.channel_notif_poll));
            return false;
        });
        Preference button_status = findPreference("button_status");
        assert button_status != null;
        button_status.setOnPreferenceClickListener(preference -> {
            openSettings("channel_status", getString(R.string.channel_notif_status));
            return false;
        });
        Preference button_media = findPreference("button_media");
        assert button_media != null;
        button_media.setOnPreferenceClickListener(preference -> {
            openSettings("channel_media", getString(R.string.channel_notif_media));
            return false;
        });
    }

    private void createNotificationChannel(String name, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(name, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void openSettings(@NonNull String channel, String description) {

        createNotificationChannel(channel, description);
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
        } else {
            intent = new Intent();
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", requireActivity().getPackageName());
            intent.putExtra("app_uid", requireActivity().getApplicationInfo().uid);
        }
        startActivity(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            if (key.compareToIgnoreCase(getString(R.string.SET_NOTIFICATION_TYPE)) == 0) {
                createPref();
                String type = sharedPreferences.getString(key, "");
                if(type.equals("PUSH_NOTIFICATIONS")) {
                    PushHelper.startStreaming(requireActivity());
                }
            }
            if (key.compareToIgnoreCase(getString(R.string.SET_NOTIFICATION_DELAY_VALUE)) == 0) {
                createPref();
                PushHelper.setRepeat(requireActivity());
            }

            if (key.compareToIgnoreCase(getString(R.string.SET_PUSH_DISTRIBUTOR)) == 0) {
                String distributor = sharedPreferences.getString(key, "");
                UnifiedPush.saveDistributor(requireActivity(), distributor);
                PushHelper.startStreaming(requireActivity());
            }
        }
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


}
