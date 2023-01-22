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

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ItemStr implements Parcelable {

    public static final Creator<ItemStr> CREATOR = new Creator<ItemStr>() {
        @Override
        public ItemStr createFromParcel(Parcel source) {
            return new ItemStr(source);
        }

        @Override
        public ItemStr[] newArray(int size) {
            return new ItemStr[size];
        }
    };
    @SerializedName("id")
    private String id;
    @SerializedName("label")
    private String label;

    public ItemStr() {
    }


    protected ItemStr(Parcel in) {
        this.id = in.readString();
        this.label = in.readString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.label);
    }
}
