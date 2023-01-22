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


@SuppressWarnings({"unused", "RedundantSuppression"})
public class MenuItemView {

    private int id;
    private String strId;
    private String label;
    private boolean selected;

    public MenuItemView() {
    }

    public MenuItemView(int id, String label) {
        this.id = id;
        this.label = label;
        selected = false;
    }

    public MenuItemView(int id, String label, boolean selected) {
        this.id = id;
        this.label = label;
        this.selected = selected;
    }

    public MenuItemView(String strId, String label, boolean selected) {
        this.strId = strId;
        this.label = label;
        this.selected = selected;
    }

    public MenuItemView(String strId, String label) {
        this.strId = strId;
        this.label = label;
        this.selected = false;
    }

    public MenuItemView(int id, String strId, String label, boolean selected) {
        this.id = id;
        this.strId = strId;
        this.label = label;
        this.selected = selected;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getStrId() {
        return strId;
    }

    public void setStrId(String strId) {
        this.strId = strId;
    }
}
