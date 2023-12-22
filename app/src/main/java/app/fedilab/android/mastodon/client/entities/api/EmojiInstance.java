package app.fedilab.android.mastodon.client.entities.api;
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import app.fedilab.android.BaseMainActivity;
import app.fedilab.android.mastodon.client.endpoints.MastodonInstanceService;
import app.fedilab.android.mastodon.exception.DBException;
import app.fedilab.android.mastodon.helper.Helper;
import app.fedilab.android.mastodon.viewmodel.mastodon.InstancesVM;
import app.fedilab.android.sqlite.Sqlite;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class EmojiInstance implements Serializable {
    private final SQLiteDatabase db;
    @SerializedName("instance")
    public String instance;
    @SerializedName("emojiList")
    public List<Emoji> emojiList;
    private Context context;

    public EmojiInstance() {
        db = null;
    }

    public EmojiInstance(Context context) {
        //Creation of the DB with tables
        this.context = context;
        this.db = Sqlite.getInstance(context.getApplicationContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
    }

    /**
     * Serialized a list of Emoji class
     *
     * @param emojis List of {@link Emoji} to serialize
     * @return String serialized emoji list
     */
    public static String mastodonEmojiListToStringStorage(List<Emoji> emojis) {
        Gson gson = new Gson();
        try {
            return gson.toJson(emojis);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unserialized a Emoji List
     *
     * @param serializedEmojiList String serialized account
     * @return List of {@link Emoji}
     */
    public static List<Emoji> restoreEmojiListFromString(String serializedEmojiList) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(serializedEmojiList, new TypeToken<List<Emoji>>() {
            }.getType());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert or update emoji
     *
     * @param emojiInstance {@link EmojiInstance}
     * @return long - db id
     * @throws DBException exception with database
     */
    public long insertOrUpdate(EmojiInstance emojiInstance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        if (emojiInstance == null) {
            return -1;
        }
        boolean exists = emojiInstanceExist(emojiInstance);
        long idReturned;
        if (exists) {
            idReturned = updateEmojiInstance(emojiInstance);
        } else {
            idReturned = insertEmojiInstance(emojiInstance);
        }
        return idReturned;
    }

    /**
     * Check if emojis exists in db
     *
     * @param emojiInstance EmojiInstance {@link EmojiInstance}
     * @return boolean - emojiInstance exists
     * @throws DBException Exception
     */
    public boolean emojiInstanceExist(EmojiInstance emojiInstance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        Cursor mCount = db.rawQuery("select count(*) from " + Sqlite.TABLE_EMOJI_INSTANCE
                + " where " + Sqlite.COL_INSTANCE + " = '" + emojiInstance.instance + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return (count > 0);
    }

    /**
     * Insert emojis in db
     *
     * @param emojiInstance {@link EmojiInstance}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long insertEmojiInstance(EmojiInstance emojiInstance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_INSTANCE, emojiInstance.instance);
        values.put(Sqlite.COL_EMOJI_LIST, mastodonEmojiListToStringStorage(emojiInstance.emojiList));
        //Inserts token
        try {
            return db.insertOrThrow(Sqlite.TABLE_EMOJI_INSTANCE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * update emojis in db
     *
     * @param emojiInstance {@link EmojiInstance}
     * @return long - db id
     * @throws DBException exception with database
     */
    private long updateEmojiInstance(EmojiInstance emojiInstance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        ContentValues values = new ContentValues();
        values.put(Sqlite.COL_EMOJI_LIST, mastodonEmojiListToStringStorage(emojiInstance.emojiList));
        //Inserts token
        try {
            return db.update(Sqlite.TABLE_EMOJI_INSTANCE,
                    values, Sqlite.COL_INSTANCE + " =  ?",
                    new String[]{emojiInstance.instance});
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    private MastodonInstanceService init(String instance) {
        final OkHttpClient okHttpClient = Helper.myOkHttpClient(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + (instance != null ? IDN.toASCII(instance, IDN.ALLOW_UNASSIGNED) : null) + "/api/v1/")
                .addConverterFactory(GsonConverterFactory.create(Helper.getDateBuilder()))
                .client(okHttpClient)
                .build();
        return retrofit.create(MastodonInstanceService.class);
    }

    /**
     * Returns the emojis for an instance
     *
     * @param instance String
     * @return List<Emoji> - List of {@link Emoji}
     */
    public List<Emoji> getEmojiList(String instance) throws DBException {
        if (db == null) {
            throw new DBException("db is null. Wrong initialization.");
        }
        try {
            Cursor c = db.query(Sqlite.TABLE_EMOJI_INSTANCE, null, Sqlite.COL_INSTANCE + " = '" + instance + "'", null, null, null, null, "1");
            return cursorToEmojiList(c);
        } catch (Exception e) {
            MastodonInstanceService mastodonInstanceService = init(instance);
            Call<List<Emoji>> emojiCall = mastodonInstanceService.customEmoji();
            if (emojiCall != null) {
                try {
                    Response<List<Emoji>> emojiResponse = emojiCall.execute();
                    if (emojiResponse.isSuccessful()) {
                        return emojiResponse.body();
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * Restore emoji list from db
     *
     * @param c Cursor
     * @return List<Emoji>
     */
    private List<Emoji> cursorToEmojiList(Cursor c) {
        //No element found
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        //Take the first element
        c.moveToFirst();
        List<Emoji> emojiList = restoreEmojiListFromString(c.getString(c.getColumnIndexOrThrow(Sqlite.COL_EMOJI_LIST)));
        c.close();
        List<Emoji> filteredEmojis = new ArrayList<>();
        if (emojiList != null) {
            for (Emoji emoji : emojiList) {
                if (emoji.visible_in_picker) {
                    filteredEmojis.add(emoji);
                }
            }
        }
        return filteredEmojis;
    }
}
