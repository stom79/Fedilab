package app.fedilab.android.client.entities.app;
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

import java.util.HashMap;
import java.util.List;

import app.fedilab.android.client.entities.Timeline;
import app.fedilab.android.client.mastodon.entities.Status;

public class SavedValues {

    public static HashMap<String, SavedValues> storedStates = new HashMap<>();
    public int position;
    public List<Status> statusList;

    /**
     * Retrieves saved values
     *
     * @param timeLineType - Timeline.TimeLineEnum
     * @param ident        - the name for pinned timeline
     * @return SavedValues
     */
    public static SavedValues getSavedValue(Timeline.TimeLineEnum timeLineType, String ident) {
        if (!canBestored(timeLineType)) {
            return null;
        }
        String key = timeLineType.getValue();
        if (ident != null) {
            key += "|" + ident;
        }
        return storedStates.get(key);
    }

    /**
     * Check if the current timeline can be stored
     *
     * @param timeLineType - Timeline.TimeLineEnum
     * @return boolean
     */
    private static boolean canBestored(Timeline.TimeLineEnum timeLineType) {
        return timeLineType == Timeline.TimeLineEnum.HOME || timeLineType == Timeline.TimeLineEnum.LOCAL || timeLineType == Timeline.TimeLineEnum.PUBLIC || timeLineType == Timeline.TimeLineEnum.REMOTE || timeLineType == Timeline.TimeLineEnum.LIST || timeLineType == Timeline.TimeLineEnum.TAG;
    }

    /**
     * @param position     - current position in timeline
     * @param timeLineType - Timeline.TimeLineEnum
     * @param statusList   - List<Status> to save
     * @param ident        - the name for pinned timeline
     */
    public static void storeTimeline(int position, Timeline.TimeLineEnum timeLineType, List<Status> statusList, String ident) {
        if (!canBestored(timeLineType)) {
            return;
        }
        String key = timeLineType.getValue();
        if (ident != null) {
            key += "|" + ident;
        }
        SavedValues savedValues = new SavedValues();
        savedValues.position = position;
        savedValues.statusList = statusList;
        storedStates.put(key, savedValues);
    }
}
