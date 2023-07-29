package app.fedilab.android.mastodon.helper;
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;

public class ImageListPreference extends ListPreference {

    private static final int DEFAULT_TINT = 0;
    private static final int DEFAULT_BACKGROUND_TINT = 0xFFFFFFFF;
    private final List<Integer> mImages;
    private int mErrorResource;
    private int mTintColor;
    private int mBackgroundColor;
    private boolean mUseCard;
    private int mCustomItemLayout;

    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mImages = new ArrayList<>();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ImageListPreference);

        try {
            int entryImagesArrayResource = array.getResourceId(R.styleable.ImageListPreference_ilp_entryImages, 0);
            String tintKey = array.getNonResourceString(R.styleable.ImageListPreference_ilp_tintKey);
            String backgroundKey = array.getNonResourceString(R.styleable.ImageListPreference_ilp_backgroundTint);

            mTintColor = array.getColor(R.styleable.ImageListPreference_ilp_tint, DEFAULT_TINT);
            mBackgroundColor = array.getColor(R.styleable.ImageListPreference_ilp_backgroundTint, 0);
            mErrorResource = array.getResourceId(R.styleable.ImageListPreference_ilp_errorImage, 0);
            mUseCard = array.getBoolean(R.styleable.ImageListPreference_ilp_useCard, false);
            mCustomItemLayout = array.getResourceId(R.styleable.ImageListPreference_ilp_itemLayout, 0);

            if (tintKey != null) {
                mTintColor = sharedPreferences.getInt(tintKey, mTintColor);
            }
            if (backgroundKey != null) {
                mBackgroundColor = sharedPreferences.getInt(backgroundKey, mBackgroundColor);
            }

            TypedArray images = context.getResources().obtainTypedArray(entryImagesArrayResource);

            for (int i = 0; i < images.length(); i++) {
                mImages.add(images.getResourceId(i, 0));
            }

            images.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onClick() {
        List<ImageListItem> items = new ArrayList<>();
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String launcher = sharedpreferences.getString(getContext().getString(R.string.SET_LOGO_LAUNCHER), "Bubbles");
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        int length = getEntries().length;
        for (int i = 0; i < length; i++) {
            int resource = 0;
            if (mImages.size() > i) {
                resource = mImages.get(i);
            }
            items.add(new ImageListItem(getEntries()[i], resource, String.valueOf(getEntryValues()[i]).equalsIgnoreCase(launcher)));
        }

        int layout = R.layout.imagelistpreference_item;
        if (mUseCard) {
            layout = R.layout.imagelistpreference_item_card;
        }
        if (mCustomItemLayout != 0) {
            layout = mCustomItemLayout;
        }

        ListAdapter adapter = new ImageListPreferenceAdapter(getContext(), layout, items);
        builder.setAdapter(adapter, (dialogInterface, which) -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(getContext().getString(R.string.SET_LOGO_LAUNCHER), String.valueOf(getEntryValues()[which]));
            editor.commit();
        });
        builder.create().show();

    }

    private static class ImageListItem {
        private final int resource;
        private final boolean isChecked;
        private final String name;

        ImageListItem(CharSequence name, int resource, boolean isChecked) {
            this(name.toString(), resource, isChecked);
        }

        ImageListItem(String name, int resource, boolean isChecked) {
            this.name = name;
            this.resource = resource;
            this.isChecked = isChecked;
        }
    }

    private static class ViewHolder {
        ImageView iconImage;
        TextView iconName;
        RadioButton radioButton;
    }

    private class ImageListPreferenceAdapter extends ArrayAdapter<ImageListItem> {
        private final List<ImageListItem> mItems;
        private final int mLayoutResource;

        ImageListPreferenceAdapter(Context context, int layoutResource, List<ImageListItem> items) {
            super(context, layoutResource, items);
            mLayoutResource = layoutResource;
            mItems = items;
        }

        @Override
        public @NonNull
        View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                try {
                    assert inflater != null;
                    convertView = inflater.inflate(mLayoutResource, parent, false);

                    holder = new ViewHolder();
                    holder.iconName = convertView.findViewById(R.id.imagelistpreference_text);
                    holder.iconImage = convertView.findViewById(R.id.imagelistpreference_image);
                    holder.radioButton = convertView.findViewById(R.id.imagelistpreference_radio);
                    convertView.setTag(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                    return super.getView(position, null, parent);
                }
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder == null) {
                return super.getView(position, convertView, parent);
            }

            ImageListItem item = mItems.get(position);

            holder.iconName.setText(item.name);

            if (item.resource != 0) {
                holder.iconImage.setImageResource(item.resource);
            } else {
                holder.iconImage.setImageResource(mErrorResource);
            }

            if (mTintColor != 0) {
                holder.iconImage.setColorFilter(mTintColor);
            }
            if (mBackgroundColor != 0) {
                holder.iconImage.setBackgroundColor(mBackgroundColor);
            }

            holder.radioButton.setChecked(item.isChecked);

            return convertView;
        }
    }
}