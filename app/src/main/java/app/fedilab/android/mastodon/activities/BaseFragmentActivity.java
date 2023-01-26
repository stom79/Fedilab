package app.fedilab.android.mastodon.activities;
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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import org.conscrypt.Conscrypt;

import java.security.Security;

import app.fedilab.android.mastodon.helper.Helper;


@SuppressLint("Registered")
public class BaseFragmentActivity extends FragmentActivity {


    static {
        EmojiManager.install(new EmojiOneProvider());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean patch_provider = true;
        try {
            SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
            patch_provider = sharedpreferences.getBoolean(Helper.SET_SECURITY_PROVIDER, true);
        } catch (Exception ignored) {
        }
        if (patch_provider) {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1);
            } catch (Exception ignored) {
            }
        }
        super.onCreate(savedInstanceState);
    }
}
