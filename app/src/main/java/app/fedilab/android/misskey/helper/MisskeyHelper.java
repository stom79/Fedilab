package app.fedilab.android.misskey.helper;
/* Copyright 2026 Thomas Schneider
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

import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.UUID;

public class MisskeyHelper {

    public static final String REDIRECT_URI = "fedilab://misskey-auth";

    private static final String MISSKEY_PERMISSIONS = "read:account,write:account,read:blocks,write:blocks," +
            "read:drive,write:drive,read:favorites,write:favorites,read:following,write:following," +
            "read:mutes,write:mutes,write:notes,read:notifications,write:notifications," +
            "read:reactions,write:reactions,write:votes";

    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public static String buildMiAuthUrl(String instance, String sessionId, String appName) {
        try {
            return "https://" + instance + "/miauth/" + sessionId +
                    "?name=" + URLEncoder.encode(appName, "UTF-8") +
                    "&callback=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                    "&permission=" + MISSKEY_PERMISSIONS;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
