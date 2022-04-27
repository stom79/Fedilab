package app.fedilab.android.broadcastreceiver;
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

import static app.fedilab.android.helper.Helper.RECEIVE_TOAST_CONTENT;
import static app.fedilab.android.helper.Helper.RECEIVE_TOAST_TYPE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import app.fedilab.android.helper.Helper;
import es.dmoral.toasty.Toasty;

public class ToastMessage extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle b = intent.getExtras();
        if (b != null) {
            String type = b.getString(RECEIVE_TOAST_TYPE, null);
            String content = b.getString(RECEIVE_TOAST_CONTENT, null);
            if (type != null && content != null) {
                switch (type) {
                    case Helper.RECEIVE_TOAST_TYPE_ERROR:
                        Toasty.error(context, content, Toasty.LENGTH_SHORT).show();
                        break;
                    case Helper.RECEIVE_TOAST_TYPE_WARNING:
                        Toasty.warning(context, content, Toasty.LENGTH_SHORT).show();
                        break;
                    case Helper.RECEIVE_TOAST_TYPE_INFO:
                        Toasty.info(context, content, Toasty.LENGTH_SHORT).show();
                        break;
                    case Helper.RECEIVE_TOAST_TYPE_SUCCESS:
                        Toasty.success(context, content, Toasty.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}
