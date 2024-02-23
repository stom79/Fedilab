package app.fedilab.android.mastodon.ui.drawer;
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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerAccountSearchBinding;
import app.fedilab.android.mastodon.client.entities.api.Account;
import app.fedilab.android.mastodon.client.entities.api.Field;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.helper.MastodonHelper;
import app.fedilab.android.mastodon.helper.PronounsHelper;


public class AccountsSearchAdapter extends ArrayAdapter<Account> implements Filterable {

    private final List<Account> accounts;
    private final List<Account> tempAccounts;
    private final List<Account> suggestions;

    private final Filter accountFilter = new Filter() {
        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Account account = (Account) resultValue;
            return "@" + account.acct;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                suggestions.addAll(tempAccounts);
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Account> c = (ArrayList<Account>) results.values;
            if (results.count > 0) {
                clear();
                addAll(c);
                notifyDataSetChanged();
            } else {
                clear();
                notifyDataSetChanged();
            }
        }
    };

    public AccountsSearchAdapter(Context context, List<Account> accounts) {
        super(context, android.R.layout.simple_list_item_1, accounts);
        this.accounts = accounts;
        this.tempAccounts = new ArrayList<>(accounts);
        this.suggestions = new ArrayList<>(accounts);
    }


    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public Account getItem(int position) {
        return accounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        final Account account = accounts.get(position);
        AccountSearchViewHolder holder;
        if (convertView == null) {
            DrawerAccountSearchBinding drawerAccountSearchBinding = DrawerAccountSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            holder = new AccountSearchViewHolder(drawerAccountSearchBinding);
            holder.view = drawerAccountSearchBinding.getRoot();
            holder.view.setTag(holder);
        } else {
            holder = (AccountSearchViewHolder) convertView.getTag();
        }

        holder.binding.accountUn.setText(String.format("@%s", account.acct));
        holder.binding.accountDn.setText(account.display_name);
        holder.binding.accountDn.setVisibility(View.VISIBLE);
        account.pronouns = null;
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean pronounsSupport = sharedpreferences.getBoolean(getContext().getString(R.string.SET_PRONOUNS_SUPPORT), true);
        if(pronounsSupport) {
            for (Field field : account.fields) {
                if (PronounsHelper.pronouns.contains(field.name.toLowerCase().trim())) {
                    account.pronouns = Helper.parseHtml(field.value);
                    break;
                }
            }
        }
        if (account.pronouns != null) {
            holder.binding.pronouns.setText(account.pronouns);
            holder.binding.pronouns.setVisibility(View.VISIBLE);
        } else {
            holder.binding.pronouns.setVisibility(View.GONE);
        }
        MastodonHelper.loadPPMastodon(holder.binding.accountPp, account);
        return holder.view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return accountFilter;
    }

    public static class AccountSearchViewHolder extends RecyclerView.ViewHolder {
        DrawerAccountSearchBinding binding;
        private View view;

        AccountSearchViewHolder(DrawerAccountSearchBinding itemView) {
            super(itemView.getRoot());
            this.view = itemView.getRoot();
            binding = itemView;
        }
    }

}
