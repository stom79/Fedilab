package app.fedilab.android.peertube.client.entities;
/* Copyright 2020 Thomas Schneider
 *
 * This file is a part of TubeLab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * TubeLab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TubeLab; if not,
 * see <http://www.gnu.org/licenses>. */

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ViewsPerDay implements Parcelable {

    public static final Creator<ViewsPerDay> CREATOR = new Creator<ViewsPerDay>() {
        @Override
        public ViewsPerDay createFromParcel(Parcel in) {
            return new ViewsPerDay(in);
        }

        @Override
        public ViewsPerDay[] newArray(int size) {
            return new ViewsPerDay[size];
        }
    };
    @SerializedName("date")
    private Date date;
    @SerializedName("views")
    private int views;

    protected ViewsPerDay(Parcel in) {
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        views = in.readInt();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.date != null ? this.date.getTime() : -1);
        parcel.writeInt(views);
    }
}
