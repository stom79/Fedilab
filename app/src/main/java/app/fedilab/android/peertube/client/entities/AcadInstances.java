package app.fedilab.android.peertube.client.entities;
/* Copyright 2021 Thomas Schneider
 *
 * This file is a part of TubeLab
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.fedilab.android.peertube.helper.HelperAcadInstance;

public class AcadInstances {

    private String name;
    private String url;
    private boolean openId;

    public static boolean isOpenId(String domain) {
        List<AcadInstances> instances = getInstances();
        for (AcadInstances acadInstance : instances) {
            if (acadInstance.getUrl().compareTo(domain) == 0) {
                return acadInstance.isOpenId();
            }
        }
        return false;
    }

    public static List<AcadInstances> getInstances() {
        List<AcadInstances> acadInstances = new ArrayList<>();

        LinkedHashMap<String, String> instancesMap = new LinkedHashMap<>(HelperAcadInstance.instances_themes);
        Iterator<Map.Entry<String, String>> it = instancesMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            AcadInstances acadInstance = new AcadInstances();
            acadInstance.name = pair.getKey();
            acadInstance.openId = true;
            acadInstance.url = pair.getValue();
            acadInstances.add(acadInstance);
            it.remove();
        }
        return acadInstances;
    }


    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isOpenId() {
        return openId;
    }

}
