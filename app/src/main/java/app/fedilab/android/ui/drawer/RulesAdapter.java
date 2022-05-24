package app.fedilab.android.ui.drawer;
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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.client.entities.api.Instance;
import app.fedilab.android.databinding.DrawerCheckboxBinding;

public class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.RuleViewHolder> {
    private final List<Instance.Rule> ruleList;

    public RulesAdapter(List<Instance.Rule> rules) {
        this.ruleList = rules;
    }

    public int getCount() {
        return ruleList.size();
    }

    public Instance.Rule getItem(int position) {
        return ruleList.get(position);
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DrawerCheckboxBinding itemBinding = DrawerCheckboxBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RuleViewHolder(itemBinding);
    }

    public List<String> getChecked() {
        List<String> checkedItems = new ArrayList<>();
        for (Instance.Rule rule : ruleList) {
            if (rule.isChecked) {
                checkedItems.add(rule.id);
            }
        }
        return checkedItems;
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        Instance.Rule rule = ruleList.get(position);
        holder.binding.checkbox.setText(rule.text);
        holder.binding.checkbox.setOnCheckedChangeListener((compoundButton, checked) -> rule.isChecked = checked);
    }

    @Override
    public int getItemCount() {
        return ruleList.size();
    }


    public static class RuleViewHolder extends RecyclerView.ViewHolder {
        DrawerCheckboxBinding binding;

        RuleViewHolder(DrawerCheckboxBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }
}
