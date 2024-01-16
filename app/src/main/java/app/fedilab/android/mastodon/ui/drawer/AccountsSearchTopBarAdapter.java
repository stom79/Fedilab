package app.fedilab.android.mastodon.ui.drawer;
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

import static app.fedilab.android.BaseMainActivity.currentAccount;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.mastodon.activities.ProfileActivity;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.app.CachedBundle;
import app.fedilab.android.mastodon.helper.Helper;


public class AccountsSearchTopBarAdapter extends SimpleCursorAdapter {

    private final int layout;
    private final LayoutInflater inflater;
    private final List<Account> accountList;

    public AccountsSearchTopBarAdapter(Context context, List<Account> accounts, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.layout = layout;
        this.inflater = LayoutInflater.from(context);
        this.accountList = accounts;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        LinearLayoutCompat container = view.findViewById(R.id.account_container);
        container.setTag(cursor.getPosition());
        container.setOnClickListener(v -> {
            int position = (int) v.getTag();
            if (accountList != null && accountList.size() > position) {
                Intent intent = new Intent(context, ProfileActivity.class);
                Bundle args = new Bundle();
                args.putSerializable(Helper.ARG_ACCOUNT, accountList.get(position));
                new CachedBundle(context).insertBundle(args, currentAccount, bundleId -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong(Helper.ARG_INTENT_ID, bundleId);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                });
            }
        });
    }
}
