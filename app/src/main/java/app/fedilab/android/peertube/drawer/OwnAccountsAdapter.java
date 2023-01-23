package app.fedilab.android.peertube.drawer;
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.client.entities.app.BaseAccount;
import app.fedilab.android.peertube.helper.Helper;


public class OwnAccountsAdapter extends ArrayAdapter<BaseAccount> {

    private final List<BaseAccount> accounts;
    private final LayoutInflater layoutInflater;

    public OwnAccountsAdapter(Context context, List<BaseAccount> accounts) {
        super(context, android.R.layout.simple_list_item_1, accounts);
        this.accounts = accounts;
        layoutInflater = LayoutInflater.from(context);
    }


    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public BaseAccount getItem(int position) {
        return accounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        final BaseAccount account = accounts.get(position);
        final ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.drawer_account_owner, parent, false);
            holder = new ViewHolder();
            holder.account_pp = convertView.findViewById(R.id.account_pp);
            holder.account_un = convertView.findViewById(R.id.account_un);

            holder.account_container = convertView.findViewById(R.id.account_container);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.account_un.setText(String.format("@%s", account.peertube_account.getAcct()));
        //Profile picture
        Helper.loadAvatar(holder.account_pp.getContext(), account.peertube_account, holder.account_pp);
        return convertView;
    }


    private static class ViewHolder {
        ImageView account_pp;
        TextView account_un;
        LinearLayout account_container;
    }


}