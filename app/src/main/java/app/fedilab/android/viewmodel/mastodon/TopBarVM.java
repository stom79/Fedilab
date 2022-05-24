package app.fedilab.android.viewmodel.mastodon;
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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.client.entities.app.Pinned;
import app.fedilab.android.exception.DBException;

public class TopBarVM extends AndroidViewModel {

    private MutableLiveData<Pinned> pinnedMutableLiveData;

    public TopBarVM(@NonNull Application application) {
        super(application);
    }

    public LiveData<Pinned> getDBPinned() {
        pinnedMutableLiveData = new MutableLiveData<>();
        new Thread(() -> {
            Pinned pinned = new Pinned(getApplication().getApplicationContext());
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Pinned pinnedTimeline = null;
            try {
                pinnedTimeline = pinned.getPinned(BaseMainActivity.accountWeakReference.get());
            } catch (DBException e) {
                e.printStackTrace();
            }
            Pinned finalPinnedTimeline = pinnedTimeline;
            Runnable myRunnable = () -> pinnedMutableLiveData.setValue(finalPinnedTimeline);
            mainHandler.post(myRunnable);
        }).start();
        return pinnedMutableLiveData;
    }

}
