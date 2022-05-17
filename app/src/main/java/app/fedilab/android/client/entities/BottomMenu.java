package app.fedilab.android.client.entities;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.R;
import app.fedilab.android.exception.DBException;
import app.fedilab.android.sqlite.Sqlite;


public class BottomMenu implements Serializable {
    private final SQLiteDatabase db;

    @SerializedName("id")
    public long id = -1;
    @SerializedName("instance")
    public String instance;
    @SerializedName("user_id")
    public String user_id;
    @SerializedName("bottom_menu")
    public List<MenuItem> bottom_menu;
    private Context context;

    public BottomMenu() {
        db = null;
    }

    public BottomMenu(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a list of MenuItem class
     *
     * @param menuItemList List of {@link MenuItem} to serialize
     * @return String serialized menuItemList list
     */
    public static String menuItemListToStringStorage(List<MenuItem> menuItemList) {
        Gson gson = new Gson();
        try {
            return gson.toJson(menuItemList);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a MenuItem List
     *
     * @param serializedMenuItem String serialized MenuItem list
     * @return List of {@link MenuItem}
     */
    public static List<MenuItem> restoreMenuItemFromString(String serializedMenuItem) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedMenuItem, new TypeToken<List<MenuItem>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

    public void hydrate(Account account, BottomNavigationView bottomNavigationView) {
        bottomNavigationView.getMenu().clear();
        BottomMenu bottomMenu;
        try {
            bottomMenu = getBottomMenu(account);
        } catch (DBException e) {
            bottomMenu = defaultBottomMenu();
        }
        for (BottomMenu.MenuItem menuItem : bottomMenu.bottom_menu) {
            if (menuItem.visible) {
                switch (menuItem.item_menu_type) {
                    case HOME:
                        bottomNavigationView.getMenu().add(Menu.NONE, R.id.nav_home, menuItem.position, context.getString(R.string.home_menu)).setIcon(R.drawable.ic_baseline_home_24);
                        break;
                    case LOCAL:
                        bottomNavigationView.getMenu().add(Menu.NONE, R.id.nav_local, menuItem.position, context.getString(R.string.local_menu)).setIcon(R.drawable.ic_baseline_people_alt_24);
                        break;
                    case PUBLIC:
                        bottomNavigationView.getMenu().add(Menu.NONE, R.id.nav_public, menuItem.position, context.getString(R.string.v_public)).setIcon(R.drawable.ic_baseline_public_24);
                        break;
                    case NOTIFICATION:
                        bottomNavigationView.getMenu().add(Menu.NONE, R.id.nav_notifications, menuItem.position, context.getString(R.string.notifications)).setIcon(R.drawable.ic_baseline_notifications_24);
                        break;
                    case DIRECT:
                        bottomNavigationView.getMenu().add(Menu.NONE, R.id.nav_privates, menuItem.position, context.getString(R.string.v_private)).setIcon(R.drawable.ic_baseline_mail_24);
                        break;
                }
            }
        }
    }

    /**
     * Insert or update instance
     *
     * @param bottomMenu {@link BottomMenu}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(BottomMenu bottomMenu) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (bottomMenu == null) {
            return -1;
        }
        boolean exists = bottomMenuExists(bottomMenu);
        long idReturned;
        if (exists) {
            idReturned = updateBottomMenu(bottomMenu);
        } else {
            idReturned = insertBottomMenu(bottomMenu);
        }
        return idReturned;
    }

    /**
     * Insert BottomMenu in db
     *
     * @param bottomMenu {@link BottomMenu}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertBottomMenu(BottomMenu bottomMenu) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, BaseMainActivity.currentInstance);
        values.put(Sqlite.COL_USER_ID, BaseMainActivity.currentUserID);
        values.put(Sqlite.COL_BOTTOM_MENU, menuItemListToStringStorage(bottomMenu.bottom_menu));
        //Inserts bottom
        try {
            return db.insertOrThrow(Sqlite.TABLE_BOTTOM_MENU, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * update bottomMenu in db
     *
     * @param bottomMenu {@link BottomMenu}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateBottomMenu(BottomMenu bottomMenu) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_BOTTOM_MENU, menuItemListToStringStorage(bottomMenu.bottom_menu));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_BOTTOM_MENU,
                    values, Sqlite.COL_INSTANCE + " =  ? AND " + Sqlite.COL_USER_ID + " = ?",
                    new String[]{bottomMenu.instance, bottomMenu.user_id});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the bottom menu for an account
     *
     * @param account Account
     * @return Pinned - {@link BottomMenu}
     */
    private BottomMenu getBottomMenu(Account account) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_BOTTOM_MENU, null, Sqlite.COL_INSTANCE + " = '" + account.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + account.user_id + "'", null, null, null, Sqlite.COL_ID + " DESC", "1");
            BottomMenu bottomMenu = cursorToBottomMenu(c);
            List<MenuItem> menuItemList = new ArrayList<>();
            if (bottomMenu != null) {
                for (MenuItem menuItem : bottomMenu.bottom_menu) {
                    if (menuItem.visible) {
                        menuItemList.add(menuItem.position, menuItem);
                    }
                }
                bottomMenu.bottom_menu = menuItemList;
            }
            if (bottomMenu == null) {
                bottomMenu = defaultBottomMenu();
            }
            return bottomMenu;
        } catch (Exception e) {
            return defaultBottomMenu();
        }
    }

    private BottomMenu defaultBottomMenu() {
        BottomMenu bottomMenu = new BottomMenu();
        bottomMenu.bottom_menu = new ArrayList<>();
        MenuItem menuItemHome = new MenuItem();
        menuItemHome.position = 0;
        menuItemHome.visible = true;
        menuItemHome.item_menu_type = ItemMenuType.HOME;
        bottomMenu.bottom_menu.add(menuItemHome);
        MenuItem menuItemLocal = new MenuItem();
        menuItemLocal.position = 1;
        menuItemLocal.visible = true;
        menuItemLocal.item_menu_type = ItemMenuType.LOCAL;
        bottomMenu.bottom_menu.add(menuItemLocal);
        MenuItem menuItemPublic = new MenuItem();
        menuItemPublic.position = 2;
        menuItemPublic.visible = true;
        menuItemPublic.item_menu_type = ItemMenuType.PUBLIC;
        bottomMenu.bottom_menu.add(menuItemPublic);
        MenuItem menuItemNotification = new MenuItem();
        menuItemNotification.position = 3;
        menuItemNotification.visible = true;
        menuItemNotification.item_menu_type = ItemMenuType.NOTIFICATION;
        bottomMenu.bottom_menu.add(menuItemNotification);
        MenuItem menuItemPrivate = new MenuItem();
        menuItemPrivate.position = 4;
        menuItemPrivate.visible = true;
        menuItemPrivate.item_menu_type = ItemMenuType.DIRECT;
        bottomMenu.bottom_menu.add(menuItemPrivate);
        return bottomMenu;
    }

    public boolean bottomMenuExists(BottomMenu bottomMenu) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_BOTTOM_MENU
                + " where " + Sqlite.COL_INSTANCE + " = '" + bottomMenu.instance + "' AND " + Sqlite.COL_USER_ID + " = '" + bottomMenu.user_id + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Restore pinned from db
     *
     * @param c Cursor
     * @return Pinned
     */
    private BottomMenu cursorToBottomMenu(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        BottomMenu bottomMenu = convertCursorToBottomMenu(c);
        //Close the cursor
        c.close();
        return bottomMenu;
    }

    /**
     * Read cursor and hydrate without closing it
     *
     * @param c - Cursor
     * @return BottomMenu
     */
    private BottomMenu convertCursorToBottomMenu(Cursor c) {
        BottomMenu bottomMenu = new BottomMenu();
        bottomMenu.id = c.getInt(c.getColumnIndexOrThrow(Sqlite.COL_ID));
        bottomMenu.instance = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_INSTANCE));
        bottomMenu.user_id = c.getString(c.getColumnIndexOrThrow(Sqlite.COL_USER_ID));
        bottomMenu.bottom_menu = restoreMenuItemFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_BOTTOM_MENU)));
        return bottomMenu;
    }

    public enum ItemMenuType {
        @SerializedName("HOME")
        HOME("HOME"),
        @SerializedName("DIRECT")
        DIRECT("DIRECT"),
        @SerializedName("NOTIFICATION")
        NOTIFICATION("NOTIFICATION"),
        @SerializedName("LOCAL")
        LOCAL("LOCAL"),
        @SerializedName("PUBLIC")
        PUBLIC("PUBLIC");
        private final String value;

        ItemMenuType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class MenuItem {
        @SerializedName("position")
        int position;
        @SerializedName("item_menu_type")
        ItemMenuType item_menu_type;
        @SerializedName("visible")
        boolean visible;
    }
}
