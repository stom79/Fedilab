package app.fedilab.android.peertube.helper;
/* Copyright 2023 Thomas Schneider
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiHelper {


    //Emoji manager
    private static final Map<String, String> emoji = new HashMap<>();
    private static final Pattern SHORTNAME_PATTERN = Pattern.compile(":( |)([-+\\w]+):");

    /**
     * Converts emojis in input to unicode
     *
     * @param input String
     * @return String
     */
    public static String shortnameToUnicode(String input) {
        Matcher matcher = SHORTNAME_PATTERN.matcher(input);

        while (matcher.find()) {
            String unicode = emoji.get(matcher.group(2));
            if (unicode == null) {
                continue;
            }
            if (matcher.group(1).equals(" "))
                input = input.replace(": " + matcher.group(2) + ":", unicode);
            else
                input = input.replace(":" + matcher.group(2) + ":", unicode);
        }
        return input;
    }


    public static void fillMapEmoji(Context context) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("emoji.csv")));
            String line;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(",");
                String unicode = null;
                if (str.length == 2)
                    unicode = new String(new int[]{Integer.parseInt(str[1].replace("0x", "").trim(), 16)}, 0, 1);
                else if (str.length == 3)
                    unicode = new String(new int[]{Integer.parseInt(str[1].replace("0x", "").trim(), 16), Integer.parseInt(str[2].replace("0x", "").trim(), 16)}, 0, 2);
                else if (str.length == 4)
                    unicode = new String(new int[]{Integer.parseInt(str[1].replace("0x", "").trim(), 16), Integer.parseInt(str[2].replace("0x", "").trim(), 16), Integer.parseInt(str[3].replace("0x", "").trim(), 16)}, 0, 3);
                else if (str.length == 5)
                    unicode = new String(new int[]{Integer.parseInt(str[1].replace("0x", "").trim(), 16), Integer.parseInt(str[2].replace("0x", "").trim(), 16), Integer.parseInt(str[3].replace("0x", "").trim(), 16), Integer.parseInt(str[4].replace("0x", "").trim(), 16)}, 0, 4);
                if (unicode != null)
                    emoji.put(str[0], unicode);
            }
            br.close();
        } catch (IOException ignored) {
        }
    }


}
