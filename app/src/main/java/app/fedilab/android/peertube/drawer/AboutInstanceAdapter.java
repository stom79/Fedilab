package app.fedilab.android.peertube.drawer;
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


import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;
import static app.fedilab.android.mastodon.helper.Helper.PREF_USER_INSTANCE_PEERTUBE_BROWSING;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAboutInstancePeertubeBinding;
import app.fedilab.android.peertube.client.data.InstanceData;
import app.fedilab.android.peertube.sqlite.StoredInstanceDAO;
import app.fedilab.android.sqlite.Sqlite;


public class AboutInstanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private final List<InstanceData.AboutInstance> aboutInstances;
    public InstanceActions instanceActions;
    private Context context;

    public AboutInstanceAdapter(List<InstanceData.AboutInstance> aboutInstances) {
        this.aboutInstances = aboutInstances;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return aboutInstances.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        DrawerAboutInstancePeertubeBinding itemBinding = DrawerAboutInstancePeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {

        context = viewHolder.itemView.getContext();

        final ViewHolder holder = (ViewHolder) viewHolder;

        final InstanceData.AboutInstance aboutInstance = aboutInstances.get(i);

        holder.binding.aboutInstanceHost.setText(aboutInstance.getHost());

        SpannableString spannableString;

        if (aboutInstance.getShortDescription() != null && aboutInstance.getShortDescription().trim().length() > 0) {
            if (aboutInstance.isTruncatedDescription()) {
                holder.binding.aboutInstanceDescription.setMaxLines(3);
                holder.binding.aboutInstanceDescription.setEllipsize(TextUtils.TruncateAt.END);

            } else {
                holder.binding.aboutInstanceDescription.setMaxLines(Integer.MAX_VALUE);
                holder.binding.aboutInstanceDescription.setEllipsize(null);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                spannableString = new SpannableString(Html.fromHtml(aboutInstance.getShortDescription(), FROM_HTML_MODE_LEGACY));
            else
                spannableString = new SpannableString(Html.fromHtml(aboutInstance.getShortDescription()));
            holder.binding.aboutInstanceDescription.setText(spannableString, TextView.BufferType.SPANNABLE);

            holder.binding.aboutInstanceDescription.setOnClickListener(v -> {
                aboutInstance.setTruncatedDescription(!aboutInstance.isTruncatedDescription());
                notifyItemChanged(i);
            });
            holder.binding.aboutInstanceDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.aboutInstanceDescription.setVisibility(View.GONE);
        }

        if (aboutInstance.getShortDescription() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                spannableString = new SpannableString(Html.fromHtml(aboutInstance.getShortDescription(), FROM_HTML_MODE_LEGACY));
            else
                spannableString = new SpannableString(Html.fromHtml(aboutInstance.getShortDescription()));
            holder.binding.aboutInstanceDescription.setText(spannableString, TextView.BufferType.SPANNABLE);
        }

        holder.binding.aboutInstanceName.setText(aboutInstance.getName());
        holder.binding.pickup.setOnClickListener(v -> {
            final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(PREF_USER_INSTANCE_PEERTUBE_BROWSING, aboutInstance.getHost());
            editor.commit();
            instanceActions.onFinished();
        });
        holder.binding.instanceMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.instanceMore);
            popup.getMenuInflater()
                    .inflate(R.menu.instance_menu_peertube, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_delete) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(R.string.delete_instance);
                    builder.setMessage(R.string.delete_instance_confirm);
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                new Thread(() -> {
                                    SQLiteDatabase db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
                                    new StoredInstanceDAO(context, db).removeInstance(aboutInstance.getHost());
                                    aboutInstances.remove(aboutInstance);
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> {
                                        notifyItemRemoved(i);
                                        if (aboutInstances.size() == 0) {
                                            instanceActions.onAllInstancesRemoved();
                                        }
                                    };
                                    mainHandler.post(myRunnable);
                                }).start();

                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                return true;
            });
            popup.show();
        });
    }

    public interface InstanceActions {
        void onAllInstancesRemoved();

        void onFinished();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        DrawerAboutInstancePeertubeBinding binding;

        ViewHolder(DrawerAboutInstancePeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }

    }

}