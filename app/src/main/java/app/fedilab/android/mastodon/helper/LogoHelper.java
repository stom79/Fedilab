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

import static app.fedilab.android.BaseMainActivity.mLauncher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;

public class LogoHelper {


    public static int getNotificationIcon(Context context) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String logo = sharedpreferences.getString(context.getString(R.string.SET_LOGO_LAUNCHER), "Bubbles");
        switch (logo) {
            case "Fediverse":
                return R.drawable.ic_plain_fediverse;
            case "Hero":
                return R.drawable.ic_plain_hero;
            case "Atom":
                return R.drawable.ic_plain_atom;
            case "BrainCrash":
                return R.drawable.ic_plain_crash;
            case "Mastalab":
                return R.drawable.ic_plain_mastalab;
            default:
                return R.drawable.ic_plain_bubbles;
        }
    }

    public static int getMainLogo(Context context) {
        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String logo = sharedpreferences.getString(context.getString(R.string.SET_LOGO_LAUNCHER), "Bubbles");
        return getDrawable(logo);
    }


    public static int getDrawable(String value) {
        switch (value) {
            case "Fediverse":
                return R.drawable.fedilab_logo_fediverse;
            case "Hero":
                return R.drawable.fedilab_logo_hero;
            case "Atom":
                return R.drawable.fedilab_logo_atom;
            case "BrainCrash":
                return R.drawable.fedilab_logo_crash;
            case "Mastalab":
                return R.drawable.fedilab_logo_mastalab;
            case "BubblesUA":
                return R.drawable.fedilab_logo_bubbles_ua;
            case "BubblesPeaGreen":
                return R.drawable.fedilab_logo_bubbles_pea_green;
            case "BubblesPride":
                return R.drawable.fedilab_logo_bubbles_pride;
            case "BubblesPink":
                return R.drawable.fedilab_logo_bubbles_pink;
            case "BubblesPirate":
                return R.drawable.fedilab_logo_bubbles_pirate;
            default:
                return R.drawable.fedilab_logo_bubbles;
        }
    }

    public static void setDrawable(String value) {
        switch (value) {
            case "Fediverse":
                mLauncher = BaseMainActivity.iconLauncher.FEDIVERSE;
                return;
            case "Hero":
                mLauncher = BaseMainActivity.iconLauncher.HERO;
                break;
            case "Atom":
                mLauncher = BaseMainActivity.iconLauncher.ATOM;
                break;
            case "BrainCrash":
                mLauncher = BaseMainActivity.iconLauncher.BRAINCRASH;
                break;
            case "Mastalab":
                mLauncher = BaseMainActivity.iconLauncher.MASTALAB;
                break;
            case "BubblesUA":
                mLauncher = BaseMainActivity.iconLauncher.BUBBLESUA;
                break;
            case "BubblesPeaGreen":
                mLauncher = BaseMainActivity.iconLauncher.BUBBLESPEAGREEN;
                break;
            case "BubblesPride":
                mLauncher = BaseMainActivity.iconLauncher.BUBBLESPRIDE;
                break;
            case "BubblesPink":
                mLauncher = BaseMainActivity.iconLauncher.BUBBLESPINK;
                break;
            case "BubblesPirate":
                mLauncher = BaseMainActivity.iconLauncher.BUBBLESPIRATE;
                break;
            default:
                mLauncher = BaseMainActivity.iconLauncher.BUBBLES;
        }
    }
}
