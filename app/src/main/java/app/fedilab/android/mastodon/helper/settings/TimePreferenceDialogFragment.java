package app.fedilab.android.mastodon.helper.settings;
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

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import app.fedilab.android.R;

public class TimePreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private TimePicker mTimePicker;


    public static TimePreferenceDialogFragment newInstance(String key) {
        final TimePreferenceDialogFragment
                fragment = new TimePreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        mTimePicker = view.findViewById(R.id.time_picker);

        if (mTimePicker == null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'time_picker'");
        }

        String time = null;
        DialogPreference preference = getPreference();
        if (preference instanceof TimePreference) {
            time = ((TimePreference) preference).getTime();
        }

        // Set the time to the TimePicker
        if (time != null) {
            mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
            mTimePicker.setCurrentHour(TimePreference.getHour(time));
            mTimePicker.setCurrentMinute(TimePreference.getMinute(time));
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            // Get the current values from the TimePicker
            int hour = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            // Generate value to save
            String time = hour + ":" + minute;
            DialogPreference preference = getPreference();
            if (preference instanceof TimePreference) {
                TimePreference timePreference = ((TimePreference) preference);
                if (timePreference.callChangeListener(time)) {
                    timePreference.setTime(time);
                }
            }
        }
    }
}