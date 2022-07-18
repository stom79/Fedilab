package app.fedilab.android.client.entities.api;
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

import android.content.Context;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;

import app.fedilab.android.R;
import app.fedilab.android.helper.SpannableHelper;

public class Field implements Serializable {
    @SerializedName("name")
    public String name;
    @SerializedName("value")
    public String value;
    @SerializedName("verified_at")
    public Date verified_at;

    //Some extra spannable element - They will be filled automatically when fetching the account
    private ForegroundColorSpan value_span;

    public synchronized Spannable getValueSpan(Context context, Account account, WeakReference<View> viewWeakReference) {

        if (verified_at != null && value != null) {
            value_span = new ForegroundColorSpan(ContextCompat.getColor(context, R.color.verified_text));
        }
        Spannable spannable = SpannableHelper.convert(context, value, null, account, null, true, viewWeakReference);
        if (value_span != null && spannable != null) {
            spannable.setSpan(value_span, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static class FieldParams implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("value")
        public String value;
    }
}
